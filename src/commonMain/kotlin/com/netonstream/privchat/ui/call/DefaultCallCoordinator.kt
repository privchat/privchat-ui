package com.netonstream.privchat.ui.call

import com.netonstream.privchat.sdk.call.CallControl
import com.netonstream.privchat.sdk.call.callStateSnapshot
import com.netonstream.privchat.sdk.dto.SdkEventEnvelope
import com.netonstream.privchat.sdk.dto.call.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class CallFailure { UNAVAILABLE, DISABLED, PARTICIPANT_LIMIT, BUSY, MEDIA, ROUTING, CONNECTION }
class CallException(val reason: CallFailure) : Exception(reason.name)

/** Scope runs on a single UI dispatcher. close() cleanup survives scope cancellation. Never collects SDK events. */
class DefaultCallCoordinator(
    private val control: CallControl,
    private val userId: ULong,
    private val media: CallMedia,
    private val scope: CoroutineScope,
    private val isCurrentSession: () -> Boolean = { true },
    private val heartbeatIntervalMs: Long = 15_000,
    private val commandId: () -> String = ::newCallCommandId,
    private val nowMs: (() -> Long)? = null,
) : CallCoordinator {
    private val mutableState = MutableStateFlow(CallUiState())
    override val state: StateFlow<CallUiState> = mutableState.asStateFlow()
    private var epoch = 0L
    private var closed = false
    private var revision = 0L
    private val snapshots = mutableMapOf<ULong, CallSnapshot>()
    private val stopping = mutableSetOf<ULong>()
    private val finishing = mutableSetOf<ULong>()
    private val commands = mutableMapOf<String, String>()
    private var heartbeat: Job? = null
    private var heartbeatCallId: ULong? = null
    private var resync: Job? = null
    private var joining: ULong? = null
    private var joinRpc: CompletableDeferred<Unit>? = null
    private data class MediaJoinFlight(val callId: ULong, val job: Job)
    private var mediaJoinFlight: MediaJoinFlight? = null
    private var cancelStart = false
    private var leaseMs = 60_000L
    private var peerSession: CallPeerSession? = null
    private var peerCallId: ULong? = null
    private var peerRecovery: Job? = null
    private var ringingDeadline: Job? = null

    init { require(heartbeatIntervalMs >= 1_000) }

    private fun current(ticket: Long) = !closed && ticket == epoch && isCurrentSession()
    private fun currentCall(callId: ULong, ticket: Long) = current(ticket) && state.value.call?.callId == callId
    private fun errorIfCurrent(callId: ULong, ticket: Long, failure: Throwable) {
        if (currentCall(callId, ticket)) error(failure)
    }
    private suspend fun finishIfCurrent(callId: ULong, ticket: Long, failure: Throwable) {
        // Check inside the executing coroutine, not merely before scheduling it.
        if (!currentCall(callId, ticket)) return
        error(failure)
        finish()
    }
    private fun error(error: Throwable) { if (current(epoch)) mutableState.value = state.value.copy(error = error) }
    private fun id(key: String) = commands.getOrPut(key, commandId)
    private fun idle() = CallUiState(available = state.value.available, error = state.value.error)
    override fun clearError() { if (!closed) mutableState.value = state.value.copy(error = null) }

    override suspend fun start(channelId: ULong, mediaKind: CallMediaKind, inviteeIds: List<ULong>) {
        if (!current(epoch)) return
        if (state.value.call != null || state.value.busy) { error(CallException(CallFailure.BUSY)); return }
        val ticket = epoch
        val startRevision = revision
        var startedCallId: ULong? = null
        fun ownsStartUi() = current(ticket) && (startedCallId?.let { state.value.call?.callId == it }
            ?: (revision == startRevision && state.value.call == null))
        cancelStart = false
        mutableState.value = state.value.copy(busy = true, error = null)
        val key = "invite:$channelId:$mediaKind:${inviteeIds.sorted().joinToString()}"
        try {
            checkMedia()
            val caps = control.capabilities().getOrThrow()
            if (!current(ticket)) return
            leaseMs = caps.heartbeatLeaseSeconds.toLong().coerceIn(10, 300) * 1_000
            mutableState.value = state.value.copy(available = caps.enabled && media.available)
            if (cancelStart) return
            if (state.value.call != null) throw CallException(CallFailure.BUSY)
            if (!caps.enabled) throw CallException(CallFailure.DISABLED)
            val limit = if (mediaKind == CallMediaKind.AUDIO) caps.audioParticipantLimit else caps.videoParticipantLimit
            // Empty invitees delegates eligible-peer selection and large-group validation to the server.
            if (limit < 2u || (inviteeIds.isNotEmpty() && inviteeIds.distinct().size.toLong() + 1 > limit.toLong())) {
                throw CallException(CallFailure.PARTICIPANT_LIMIT)
            }
            val result = control.invite(CallInviteRequest(id(key), channelId, mediaKind, inviteeIds)).getOrThrow()
            if (!current(ticket)) return
            commands.remove(key)
            startedCallId = result.call.callId
            apply(result.call, localInvite = true)
            if (state.value.call == null) mutableState.value = state.value.copy(busy = false)
            if (state.value.call?.callId == result.call.callId) {
                if (cancelStart) hangUp() else join(result.call, ticket)
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { if (ownsStartUi()) error(failure) }
        finally { if (ownsStartUi()) mutableState.value = state.value.copy(busy = false) }
    }

    override suspend fun accept() {
        val call = state.value.call ?: return
        if (!current(epoch) || state.value.busy || state.value.joined || call.callId in stopping) return
        val ticket = epoch
        mutableState.value = state.value.copy(busy = true, error = null)
        try { checkMedia(); join(call, ticket) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) {
            errorIfCurrent(call.callId, ticket, failure)
            if (currentCall(call.callId, ticket) && !state.value.joined &&
                state.value.call?.participants?.any { it.userId == userId && it.state == CallParticipantState.JOINED } == true) {
                mutableState.value = idle()
            }
        }
        finally { if (currentCall(call.callId, ticket)) mutableState.value = state.value.copy(busy = false) }
    }

    private fun checkMedia() { if (!media.available) throw CallException(CallFailure.UNAVAILABLE) }

    private suspend fun join(call: CallSnapshot, ticket: Long) {
        joining = call.callId
        val rpcDone = CompletableDeferred<Unit>()
        joinRpc = rpcDone
        try {
            val key = "join:${call.callId}"
            val response = try {
                withTimeout(30_000) { control.join(CallMutationRequest(id(key), call.callId)).getOrThrow() }
                    .also {
                        if (current(ticket)) { commands.remove(key); apply(it.call) }
                    }
            } finally { rpcDone.complete(Unit) }
            if (!current(ticket)) return
            if (!canConnect(call.callId, ticket)) return
            try { withMediaJoin(call.callId) {
                mediaLock.withLock {
                    if (!canConnect(call.callId, ticket)) return@withLock
                    owner?.let { previous -> previous.media.disconnect() }
                    owner = this
                    ownedCall = call.callId
                    require(response.call.transport == call.transport)
                    if (call.transport == CallTransport.WEBRTC) {
                        val peer = requireNotNull(response.peer)
                        require(peer.selfUserId == userId && peer.peerUserId != userId)
                        require(peer.offerer == (call.initiatorId == userId))
                        require(call.participants.any { it.userId == peer.peerUserId })
                        stopPeer(call.callId)
                        peerCallId = call.callId
                        peerSession = CallPeerSession(
                            call.callId, peer, control, scope, commandId,
                            isCurrent = { canConnect(call.callId, ticket) },
                            connected = { state.value.mediaConnected },
                            receive = { event ->
                                mediaLock.withLock {
                                    if (!canConnect(call.callId, ticket) || owner !== this || ownedCall != call.callId) {
                                        throw CancellationException("Call replaced")
                                    }
                                    media.receivePeerSignal(event)
                                }
                            },
                            renewed = { result ->
                                if (currentCall(call.callId, ticket)) apply(result.call)
                                mediaLock.withLock {
                                    if (!canConnect(call.callId, ticket) || owner !== this || ownedCall != call.callId) {
                                        throw CancellationException("Call replaced")
                                    }
                                    media.updatePeerConfig(requireNotNull(result.peer))
                                }
                            },
                            failed = { finishIfCurrent(call.callId, ticket, CallException(CallFailure.CONNECTION)) },
                            nowMs = nowMs,
                        )
                        try { media.connectPeer(response.call, peer) }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (failure: Exception) { throw CallException(CallFailure.CONNECTION) }
                    } else {
                        require(response.url.isNotBlank() && response.token.isNotBlank())
                        media.connect(response.call, response.url, response.token)
                    }
                    val microphoneEnabled = state.value.microphoneEnabled
                    if (canConnect(call.callId, ticket)) media.setMicrophone(microphoneEnabled)
                    var speakerEnabled = false
                    if (canConnect(call.callId, ticket)) {
                        val initialSpeaker = state.value.speakerEnabled
                        try { media.setSpeaker(initialSpeaker); speakerEnabled = initialSpeaker }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (failure: Exception) { errorIfCurrent(call.callId, ticket, CallException(CallFailure.ROUTING)) }
                    }
                    var cameraEnabled = false
                    if (canConnect(call.callId, ticket) && call.mediaKind == CallMediaKind.VIDEO) {
                        try { media.setCamera(true); cameraEnabled = true }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (failure: Exception) { errorIfCurrent(call.callId, ticket, CallException(CallFailure.MEDIA)) }
                    }
                    if (!canConnect(call.callId, ticket)) {
                        media.disconnect()
                        owner = null
                        ownedCall = null
                        return@withLock
                    }
                    mutableState.value = state.value.copy(
                        joined = true, incoming = false,
                        microphoneEnabled = microphoneEnabled, cameraEnabled = cameraEnabled,
                        speakerEnabled = speakerEnabled,
                    )
                }
                if (canConnect(call.callId, ticket) && state.value.joined) {
                    startHeartbeat(call.callId, ticket)
                    if (peerCallId == call.callId) peerSession?.start()
                    if (call.transport == CallTransport.WEBRTC && state.value.call?.state == CallState.ACTIVE) {
                        awaitPeerRecovery(call.callId, ticket)
                    }
                }
            } } catch (cancelled: CancellationException) {
                // This device's join RPC succeeded. Cancellation must release its server claim too.
                withContext(NonCancellable) {
                    withTimeoutOrNull(5_000) {
                        if (currentCall(call.callId, ticket)) finish() else detach(call.callId)
                    }
                }
                throw cancelled
            }
            catch (failure: Exception) {
                finishIfCurrent(call.callId, ticket, failure as? CallException ?: CallException(CallFailure.MEDIA))
            }
        } finally {
            rpcDone.complete(Unit)
            if (joinRpc === rpcDone) { joinRpc = null; joining = null }
        }
    }

    private suspend fun withMediaJoin(callId: ULong, block: suspend () -> Unit) = coroutineScope {
        val flight = MediaJoinFlight(callId, currentCoroutineContext().job)
        mediaJoinFlight = flight
        try { block() }
        finally { if (mediaJoinFlight === flight) mediaJoinFlight = null }
    }

    private fun cancelMediaJoin(callId: ULong) {
        // Removing presentation must not leave native setup holding the shared media lock.
        mediaJoinFlight?.takeIf { it.callId == callId }?.job?.cancel()
    }

    private fun canConnect(callId: ULong, ticket: Long): Boolean =
        current(ticket) && callId !in stopping && state.value.call?.let {
            it.callId == callId && it.state != CallState.ENDED &&
                it.participants.any { p -> p.userId == userId && p.state == CallParticipantState.JOINED }
        } == true

    override suspend fun reject() = finish()
    override suspend fun hangUp() {
        if (state.value.call == null && state.value.busy) cancelStart = true
        finish()
    }

    private suspend fun finish() {
        val call = state.value.call ?: return
        if (!current(epoch) || !finishing.add(call.callId)) return
        val ticket = epoch
        stopping += call.callId
        ringingDeadline?.cancel()
        stopPeer(call.callId)
        heartbeat?.cancel()
        mutableState.value = state.value.copy(busy = true, joined = false, mediaConnected = false)
        try {
            mediaJoinFlight?.takeIf { it.callId == call.callId && it.job !== currentCoroutineContext().job }
                ?.job?.cancelAndJoin()
            detach(call.callId)
            if (joining == call.callId) joinRpc?.await()
            if (!current(ticket)) return
            val latest = state.value.call?.takeIf { it.callId == call.callId } ?: return
            val action = when {
                latest.initiatorId == userId && latest.state == CallState.RINGING -> "cancel"
                latest.participants.any { it.userId == userId && it.state == CallParticipantState.INVITED } -> "reject"
                else -> "leave"
            }
            val key = "$action:${call.callId}"
            val request = CallMutationRequest(id(key), call.callId)
            val response = when (action) {
                "reject" -> control.reject(request)
                "cancel" -> control.cancel(request)
                else -> control.leave(request)
            }.getOrThrow()
            if (!current(ticket)) return
            commands.remove(key)
            apply(response.call)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) {
            if (currentCall(call.callId, ticket)) {
                error(failure)
                // Mutations may lose a race with a server timeout/end. Reconcile once, never poll.
                val latest = control.get(call.callId).getOrNull()
                if (current(ticket) && latest != null) apply(latest.call)
            }
        }
        finally {
            finishing.remove(call.callId)
            if (currentCall(call.callId, ticket)) mutableState.value = state.value.copy(busy = false)
        }
    }

    private suspend fun detach(callId: ULong) {
        stopPeer(callId)
        val ticket = epoch
        mediaLock.withLock {
            if (owner === this && ownedCall == callId) {
                try { media.disconnect() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) { errorIfCurrent(callId, ticket, CallException(CallFailure.MEDIA)) }
                finally { owner = null; ownedCall = null }
            }
        }
    }

    private fun apply(call: CallSnapshot, localInvite: Boolean = false) {
        if (!current(epoch)) return
        if (call.participants.none { it.userId == userId }) return
        val previous = snapshots[call.callId]
        if (previous != null && (call.version < previous.version || previous.state == CallState.ENDED)) return
        if (previous != null && previous.transport != call.transport) {
            errorIfCurrent(call.callId, epoch, CallException(CallFailure.MEDIA))
            return
        }
        if (previous?.version == call.version && (state.value.call != null || call.callId in stopping)) return
        snapshots[call.callId] = call
        revision++
        val self = call.participants.first { it.userId == userId }
        val terminal = call.state == CallState.ENDED || self.state == CallParticipantState.LEFT || self.state == CallParticipantState.REJECTED
        val activeId = state.value.call?.callId
        if (!terminal && awaitsRingingDeadline(call) && nowMs?.invoke()?.let { call.expiresAtMs <= it } == true) {
            expireRinging(call.callId, epoch)
            return
        }
        // Joined on another device: this device must not offer controls that end that call.
        if (!terminal && self.state == CallParticipantState.JOINED && !localInvite &&
            joining != call.callId && !(activeId == call.callId && state.value.joined)) {
            if (activeId == call.callId) {
                ringingDeadline?.cancel()
                mutableState.value = idle()
            }
            return
        }
        if (terminal) {
            stopping += call.callId
            stopPeer(call.callId)
            cancelMediaJoin(call.callId)
            if (activeId == call.callId) {
                ringingDeadline?.cancel()
                heartbeat?.cancel()
                mutableState.value = idle()
                scope.launch(NonCancellable) { detach(call.callId) }
            }
        } else if ((activeId == null && call.callId !in stopping) || activeId == call.callId) {
            mutableState.value = state.value.copy(
                call = call,
                incoming = self.state == CallParticipantState.INVITED && call.initiatorId != userId,
                error = if (activeId == null) null else state.value.error,
                microphoneEnabled = if (activeId == null) true else state.value.microphoneEnabled,
                speakerEnabled = if (activeId == null) call.mediaKind == CallMediaKind.VIDEO else state.value.speakerEnabled,
            )
            scheduleRingingDeadline(call)
            if (call.transport == CallTransport.WEBRTC && call.state == CallState.ACTIVE && state.value.joined) {
                awaitPeerRecovery(call.callId, epoch)
            }
        }
    }

    override fun onEvent(envelope: SdkEventEnvelope) {
        if (!current(epoch)) return
        envelope.event.callStateSnapshot()?.fold(onSuccess = { apply(it) }, onFailure = ::error)
    }

    override fun onForegroundOrReconnect() {
        if (!current(epoch) || resync?.isActive == true) return
        val ticket = epoch
        val before = revision
        resync = scope.launch {
            try {
                val caps = control.capabilities().getOrThrow()
                if (!current(ticket)) return@launch
                leaseMs = caps.heartbeatLeaseSeconds.toLong().coerceIn(10, 300) * 1_000
                mutableState.value = state.value.copy(available = caps.enabled && media.available)
                if (!caps.enabled && state.value.call == null) return@launch
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                if (current(ticket)) {
                    mutableState.value = state.value.copy(available = false)
                    if (state.value.call == null) return@launch
                    error(failure)
                }
            }
            if (!current(ticket)) return@launch
            try {
                val active = control.active().getOrThrow()
                if (!current(ticket)) return@launch
                val old = state.value.call
                val unchanged = before == revision
                active.calls.sortedBy { it.createdAtMs }.forEach { apply(it) }
                // An event arriving during active() is newer evidence than an absent entry.
                if (unchanged && old != null && active.calls.none { it.callId == old.callId }) {
                    stopping += old.callId
                    cancelMediaJoin(old.callId)
                    heartbeat?.cancel()
                    mutableState.value = idle()
                    detach(old.callId)
                    active.calls.sortedBy { it.createdAtMs }.forEach { apply(it) }
                }
                state.value.call?.let {
                    if (state.value.joined && it.callId !in stopping) startHeartbeat(it.callId, ticket)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { if (current(ticket)) error(failure) }
        }
    }

    private fun startHeartbeat(callId: ULong, ticket: Long) {
        // Reconciliation must not postpone renewal or reset an in-flight retry budget.
        if (heartbeat?.isActive == true && heartbeatCallId == callId) return
        heartbeat?.cancel()
        heartbeatCallId = callId
        heartbeat = scope.launch {
            while (canConnect(callId, ticket) && state.value.joined) {
                delay(minOf(heartbeatIntervalMs, leaseMs / 4))
                if (!canConnect(callId, ticket) || !state.value.joined) break
                val request = CallMutationRequest(commandId(), callId)
                var lastFailure: Throwable? = null
                for (attempt in 0..2) {
                    if (!canConnect(callId, ticket) || !state.value.joined) return@launch
                    try {
                        val response = withTimeout(minOf(5_000, leaseMs / 10)) { control.heartbeat(request).getOrThrow() }
                        if (current(ticket)) apply(response.call)
                        lastFailure = null
                        break
                    } catch (timeout: TimeoutCancellationException) { lastFailure = IllegalStateException("Call heartbeat timed out") }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (failure: Exception) { lastFailure = failure }
                    if (attempt < 2) delay(minOf(1_000L shl attempt, leaseMs / 20 * (attempt + 1)))
                }
                if (lastFailure != null) {
                    if (currentCall(callId, ticket)) {
                        // finish cancels this heartbeat; dispatch cleanup in the host scope instead.
                        val failure = lastFailure
                        scope.launch { finishIfCurrent(callId, ticket, failure) }
                    }
                    break
                }
            }
        }
    }

    override fun onMediaEvent(callId: ULong, type: String, message: String?) {
        if (!current(epoch) || state.value.call?.callId != callId || callId in stopping) return
        if (type == "peer_signal") {
            if (peerCallId == callId) peerSession?.send(message)
            return
        }
        if (type == "peer_credentials_required") {
            if (peerCallId == callId) {
                mutableState.value = state.value.copy(mediaConnected = false)
                awaitPeerRecovery(callId, epoch)
                peerSession?.refreshCredentials()
            }
            return
        }
        if (type == "peer_restart_needed") {
            if (peerCallId == callId) {
                mutableState.value = state.value.copy(mediaConnected = false)
                awaitPeerRecovery(callId, epoch)
            }
            return
        }
        if (type == "connected" || type == "reconnecting") {
            if (owner === this && ownedCall == callId) {
                mutableState.value = state.value.copy(mediaConnected = type == "connected")
                if (type == "connected") { peerRecovery?.cancel(); peerRecovery = null }
                else if (peerCallId == callId) awaitPeerRecovery(callId, epoch)
            }
            return
        }
        if (type == "route") return // Never retain/log raw native statistics (candidate addresses may be sensitive).
        if (type == "error" || type == "failed" || type == "disconnected") {
            val ticket = epoch
            val reason = if (peerCallId == callId) CallFailure.CONNECTION else CallFailure.MEDIA
            scope.launch { finishIfCurrent(callId, ticket, CallException(reason)) }
        }
    }

    private suspend fun changeMedia(allowPending: Boolean = false, action: suspend () -> Unit, update: (CallUiState) -> CallUiState) {
        val callId = state.value.call?.callId ?: return
        val ticket = epoch
        if (!currentCall(callId, ticket) || callId in stopping || (!state.value.joined && !allowPending)) return
        try {
            mediaLock.withLock {
                if (!currentCall(callId, ticket) || callId in stopping) return@withLock
                if (state.value.joined) {
                    if (!canConnect(callId, ticket) || owner !== this || ownedCall != callId) return@withLock
                    action()
                } else if (!allowPending) return@withLock
                if (currentCall(callId, ticket) && callId !in stopping) mutableState.value = update(state.value)
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { errorIfCurrent(callId, ticket, CallException(CallFailure.MEDIA)) }
    }

    override suspend fun setMicrophone(enabled: Boolean) = changeMedia(true, { media.setMicrophone(enabled) }) { it.copy(microphoneEnabled = enabled) }
    override suspend fun setCamera(enabled: Boolean) {
        if (state.value.call?.mediaKind != CallMediaKind.VIDEO) return
        changeMedia(action = { media.setCamera(enabled) }) { it.copy(cameraEnabled = enabled) }
    }
    override suspend fun setSpeaker(enabled: Boolean) = changeMedia(true, { media.setSpeaker(enabled) }) { it.copy(speakerEnabled = enabled) }
    override fun showVideo(bounds: CallVideoBounds) {
        if (!current(epoch) || !state.value.joined || state.value.call?.mediaKind != CallMediaKind.VIDEO) return
        if (!listOf(bounds.x, bounds.y, bounds.width, bounds.height).all { it.isFinite() } || bounds.width <= 0 || bounds.height <= 0) return
        try { media.showVideo(bounds) }
        catch (failure: Exception) { error(CallException(CallFailure.MEDIA)) }
    }
    override fun dismissVideo() {
        try { media.dismissVideo() }
        catch (failure: Exception) { error(CallException(CallFailure.MEDIA)) }
    }

    override fun close() {
        if (closed) return
        closed = true
        epoch++
        mediaJoinFlight?.job?.cancel()
        heartbeat?.cancel()
        resync?.cancel()
        ringingDeadline?.cancel()
        peerSession?.close()
        peerRecovery?.cancel()
        peerRecovery = null
        peerSession = null
        peerCallId = null
        val callId = if (owner === this) ownedCall else state.value.call?.callId
        mutableState.value = CallUiState()
        if (callId != null) scope.launch(NonCancellable) { detach(callId) }
        snapshots.clear()
        commands.clear()
    }

    private fun awaitsRingingDeadline(call: CallSnapshot): Boolean = call.state != CallState.ENDED &&
        (call.state == CallState.RINGING || call.participants.any {
            it.userId == userId && it.state == CallParticipantState.INVITED
        })

    private fun scheduleRingingDeadline(call: CallSnapshot) {
        ringingDeadline?.cancel()
        ringingDeadline = null
        val clock = nowMs ?: return
        if (!awaitsRingingDeadline(call)) return
        val ticket = epoch
        ringingDeadline = scope.launch {
            delay((call.expiresAtMs - clock()).coerceAtLeast(0))
            expireRinging(call.callId, ticket)
        }
    }

    private fun expireRinging(callId: ULong, ticket: Long) {
        if (!currentCall(callId, ticket)) return
        val call = snapshots[callId] ?: return
        if (!awaitsRingingDeadline(call) || nowMs?.invoke()?.let { it < call.expiresAtMs } != false) return
        // Local presentation deadline only: retain the authoritative snapshot and send no mutation.
        heartbeat?.cancel()
        stopPeer(callId)
        cancelMediaJoin(callId)
        mutableState.value = idle()
        revision++
        scope.launch(NonCancellable) { detach(callId) }
        onForegroundOrReconnect()
    }

    private fun stopPeer(callId: ULong) {
        if (peerCallId != callId) return
        peerRecovery?.cancel()
        peerRecovery = null
        peerSession?.close()
        peerSession = null
        peerCallId = null
    }

    /** Callee never creates an offer. Wait for the native caller restart, with a fixed deadline. */
    private fun awaitPeerRecovery(callId: ULong, ticket: Long) {
        if (!canConnect(callId, ticket) || state.value.mediaConnected || peerRecovery?.isActive == true) return
        peerRecovery = scope.launch {
            delay(30_000)
            if (currentCall(callId, ticket) && !state.value.mediaConnected) {
                // finish() cancels peerRecovery; keep teardown in a separately guarded coroutine.
                scope.launch { finishIfCurrent(callId, ticket, CallException(CallFailure.CONNECTION)) }
            }
        }
    }

    companion object {
        // Shared across account coordinators: late native connect/cleanup cannot touch a new room.
        private val mediaLock = Mutex()
        private var owner: DefaultCallCoordinator? = null
        private var ownedCall: ULong? = null
    }
}

@OptIn(ExperimentalUuidApi::class)
private fun newCallCommandId(): String = Uuid.random().toString()
