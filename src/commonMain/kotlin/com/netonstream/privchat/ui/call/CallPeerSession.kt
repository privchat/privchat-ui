package com.netonstream.privchat.ui.call

import com.netonstream.privchat.sdk.call.CallControl
import com.netonstream.privchat.sdk.dto.call.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.Json

/** One device-owned call, not a second consumer of the SDK event stream. No payload logging. */
internal class CallPeerSession(
    private val callId: ULong,
    private var config: CallPeerConfig,
    private val control: CallControl,
    private val scope: CoroutineScope,
    private val commandId: () -> String,
    private val isCurrent: () -> Boolean,
    private val connected: () -> Boolean,
    private val receive: suspend (CallPeerEvent) -> Unit,
    private val renewed: suspend (CallJoinResponse) -> Unit,
    private val failed: suspend () -> Unit,
    private val nowMs: (() -> Long)?,
) {
    private val lifetime = SupervisorJob(scope.coroutineContext[Job])
    private val jobs = CoroutineScope(scope.coroutineContext + lifetime)
    private val outbound = Channel<CallPeerSignalRequest>(64)
    private var started = false
    private var closed = false
    private var cursor = 0uL
    private var refresh: Job? = null
    private var renewalTimer: Job? = null
    private fun live() = !closed && isCurrent()

    fun start() {
        if (started || !live()) return
        started = true
        jobs.launch {
            for (request in outbound) {
                if (!live()) break
                try { retry { control.peerSignal(request).getOrThrow() } }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) { fail(); break }
            }
        }
        jobs.launch {
            var failures = 0
            while (live()) {
                try {
                    val response = withTimeout(5_000) { control.peerEvents(CallPeerEventsRequest(callId, cursor)).getOrThrow() }
                    if (!live()) break
                    require(response.nextSeq >= cursor)
                    for (event in response.events.sortedBy { it.seq }) {
                        if (!live()) return@launch
                        if (event.seq <= cursor) continue
                        require(event.seq <= response.nextSeq && event.revision > 0u)
                        if (event.fromUserId != config.selfUserId) {
                            require(event.fromUserId == config.peerUserId)
                            receive(event) // Native deduplicates seq and queues ICE behind matching remote SDP.
                        }
                        if (!live()) return@launch
                        cursor = event.seq
                    }
                    cursor = response.nextSeq
                    failures = 0
                } catch (timeout: TimeoutCancellationException) { failures++ }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) { failures++ }
                if (failures >= 3) { fail(); break }
                delay(if (failures > 0) 500L * failures else if (connected()) 2_000 else 500)
            }
        }
        scheduleRenewal()
    }

    /** May be queued during connectPeer setup, before the poll loop starts. */
    fun send(message: String?) {
        if (!live()) return
        try {
            require(message != null && message.length <= 524_288)
            val signal = Json.decodeFromString<CallPeerSignal>(message)
            require(signal.revision > 0u)
            require(signal.kind != CallPeerSignalKind.OFFER || config.offerer)
            require(signal.kind != CallPeerSignalKind.ANSWER || !config.offerer)
            val request = CallPeerSignalRequest(commandId(), callId, signal.revision, signal.kind, signal.payload)
            check(outbound.trySend(request).isSuccess)
        } catch (failure: Exception) { fail() }
    }

    fun refreshCredentials() {
        if (!live() || refresh?.isActive == true) return
        refresh = jobs.launch {
            try {
                val request = CallMutationRequest(commandId(), callId)
                val result = retry { control.join(request).getOrThrow() }
                if (!live()) return@launch
                val next = requireNotNull(result.peer)
                require(result.call.callId == callId && result.call.transport == CallTransport.WEBRTC)
                require(next.selfUserId == config.selfUserId && next.peerUserId == config.peerUserId && next.offerer == config.offerer)
                require(nonExpiring(next) || (next.credentialExpiresAtMs > 0 &&
                    (nowMs == null || next.credentialExpiresAtMs > nowMs.invoke())))
                renewed(result)
                if (!live()) return@launch
                config = next
                scheduleRenewal()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { fail() }
        }
    }

    private fun scheduleRenewal() {
        renewalTimer?.cancel()
        renewalTimer = null
        if (nonExpiring(config)) return
        val clock = nowMs ?: return // Factory injects the platform clock; pure tests can inject virtual time.
        val remaining = config.credentialExpiresAtMs - clock()
        if (remaining <= 0) { fail(); return }
        renewalTimer = jobs.launch {
            delay((remaining - minOf(30_000, remaining / 4)).coerceAtLeast(1_000))
            if (live()) refreshCredentials()
        }
    }

    // Zero means no expiring credentials only when the server supplies no TURN relay.
    private fun nonExpiring(peer: CallPeerConfig): Boolean =
        peer.credentialExpiresAtMs == 0L && peer.iceServers.none { server ->
            server.urls.any { it.startsWith("turn:", ignoreCase = true) || it.startsWith("turns:", ignoreCase = true) }
        }

    private suspend fun <T> retry(action: suspend () -> T): T {
        repeat(3) { attempt ->
            if (!live()) throw CancellationException("Call replaced")
            try { return withTimeout(5_000) { action() } }
            catch (timeout: TimeoutCancellationException) { if (attempt == 2) throw IllegalStateException("Peer operation timed out") }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { if (attempt == 2) throw failure }
            delay(250L shl attempt)
        }
        error("Unreachable")
    }

    private fun fail() {
        if (!live()) return
        close()
        scope.launch { if (isCurrent()) failed() }
    }

    fun close() {
        if (closed) return
        closed = true
        outbound.cancel()
        lifetime.cancel()
    }
}
