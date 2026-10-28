package com.netonstream.privchat.ui.call

import com.netonstream.privchat.sdk.call.CallControl
import com.netonstream.privchat.sdk.dto.SdkEventEnvelope
import com.netonstream.privchat.sdk.dto.SdkEventPayload
import com.netonstream.privchat.sdk.dto.call.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CallCoordinatorTest {
    @Test fun terminalSnapshotCancelsPendingNativeConnectWithoutWaitingForItsTimeout() = runTest {
        for (transport in listOf(CallTransport.LIVEKIT, CallTransport.WEBRTC)) {
            val api = Control().apply { call = snapshot().copy(transport = transport) }
            val gate = CompletableDeferred<Unit>()
            val media = Media().apply { this.gate = gate }
            val c = DefaultCallCoordinator(api, 1u, media, backgroundScope)
            c.onEvent(event(api.call))
            val accepting = launch { c.accept() }
            try {
                runCurrent()
                assertEquals(1, media.connections + media.peerConnections)
                assertFalse(accepting.isCompleted)
                c.onEvent(event(api.call.copy(version = 9u, state = CallState.ENDED)))
                runCurrent()
                assertNull(c.state.value.call)
                assertTrue(accepting.isCompleted, "Terminal authority must cancel native setup, not hold the global media lock until its timeout")
                assertTrue(media.disconnections > 0)
                assertEquals(listOf("join"), api.mutations.map { it.first }, "Do not mutate an already terminal server call")
            } finally {
                accepting.cancel(); gate.complete(Unit); c.close(); runCurrent()
            }
        }
    }

    @Test fun ringingDeadlineCancelsPendingNativeConnectWithoutServerMutation() = runTest {
        val api = Control().apply { call = snapshot(initiator = 1u, self = CallParticipantState.JOINED).copy(expiresAtMs = 1_000) }
        val gate = CompletableDeferred<Unit>()
        val media = Media().apply { this.gate = gate }
        val c = DefaultCallCoordinator(api, 1u, media, backgroundScope, nowMs = { testScheduler.currentTime })
        val starting = launch { c.start(9u, CallMediaKind.AUDIO) }
        try {
            runCurrent(); assertEquals(1, media.connections)
            advanceTimeBy(1_001); runCurrent()
            assertNull(c.state.value.call)
            assertTrue(starting.isCompleted, "Expired ringing must release an unfinished native setup")
            assertTrue(media.disconnections > 0)
            assertEquals(listOf("join"), api.mutations.map { it.first })
        } finally {
            starting.cancel(); gate.complete(Unit); c.close(); runCurrent()
        }
    }

    @Test fun absentActiveReconciliationCancelsPendingNativeConnect() = runTest {
        val api = Control().apply { activeGate = CompletableDeferred(CallActiveResponse(emptyList())) }
        val gate = CompletableDeferred<Unit>()
        val media = Media().apply { this.gate = gate }
        val c = DefaultCallCoordinator(api, 1u, media, backgroundScope)
        c.onEvent(event(api.call))
        val accepting = launch { c.accept() }
        try {
            runCurrent(); assertEquals(1, media.connections)
            c.onForegroundOrReconnect(); runCurrent()
            assertNull(c.state.value.call)
            assertTrue(accepting.isCompleted, "Server reconciliation cannot leave an orphan native setup holding media ownership")
            assertTrue(media.disconnections > 0)
        } finally {
            accepting.cancel(); gate.complete(Unit); c.close(); runCurrent()
        }
    }

    private fun snapshot(version: ULong = 1u, self: CallParticipantState = CallParticipantState.INVITED,
                         state: CallState = CallState.RINGING, initiator: ULong = 2u) = CallSnapshot(
        41u, 9u, initiator, CallMediaKind.AUDIO, state, version = version, createdAtMs = 1,
        expiresAtMs = 50_000, participants = listOf(CallParticipant(1u, self), CallParticipant(2u, CallParticipantState.JOINED)),
    )
    private fun event(call: CallSnapshot) = SdkEventEnvelope(1u, 0, SdkEventPayload(
        type = "rpc_notification_received", route = "call/state",
        body = Json.encodeToString(CallResponse(call)).encodeToByteArray(),
    ))

    private class Media : CallMedia {
        override var available = true
        var connections = 0
        var disconnections = 0
        var peerConnections = 0
        val peerEvents = mutableListOf<CallPeerEvent>()
        val peerConfigs = mutableListOf<CallPeerConfig>()
        var peerReceiveFailures = 0
        var peerSetupEvent: (() -> Unit)? = null
        val microphoneCalls = mutableListOf<Boolean>()
        val cameraCalls = mutableListOf<Boolean>()
        val speakerCalls = mutableListOf<Boolean>()
        var speakerFailure = false
        var cameraFailure = false
        var microphoneFailure = false
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun connect(call: CallSnapshot, url: String, token: String) { connections++; gate?.await() }
        override suspend fun connectPeer(call: CallSnapshot, peer: CallPeerConfig) { peerConnections++; peerSetupEvent?.invoke(); gate?.await() }
        override suspend fun receivePeerSignal(event: CallPeerEvent) {
            if (peerReceiveFailures-- > 0) error("retry native delivery")
            peerEvents += event
        }
        override suspend fun updatePeerConfig(peer: CallPeerConfig) { peerConfigs += peer }
        override suspend fun disconnect() { disconnections++ }
        override suspend fun setMicrophone(enabled: Boolean) {
            microphoneCalls += enabled
            if (microphoneFailure) error("permission denied")
        }
        override suspend fun setCamera(enabled: Boolean) {
            cameraCalls += enabled
            if (cameraFailure) error("simulator camera unavailable")
        }
        override suspend fun setSpeaker(enabled: Boolean) {
            speakerCalls += enabled
            if (speakerFailure) error("audio routing failed")
        }
    }

    private inner class Control : CallControl {
        var call = snapshot()
        var activeGate: CompletableDeferred<CallActiveResponse>? = null
        var joinGate: CompletableDeferred<CallJoinResponse>? = null
        var inviteGate: CompletableDeferred<CallResponse>? = null
        var mutationFailure = false
        var enabled = true
        var capabilitiesFailure = false
        var activeCalls = 0
        var beats = 0
        var heartbeatFailures = 0
        var heartbeatHook: (() -> Unit)? = null
        var peer = CallPeerConfig(1u, 2u, false, listOf(CallIceServer(listOf("turn:example.invalid"), "user", "SECRET")), 10_000)
        val journal = mutableListOf<CallPeerEvent>()
        val polls = mutableListOf<CallPeerEventsRequest>()
        val signals = mutableListOf<CallPeerSignalRequest>()
        var signalFailures = 0
        var peerPollGate: CompletableDeferred<CallPeerEventsResponse>? = null
        val mutations = mutableListOf<Pair<String, CallMutationRequest>>()
        val invitations = mutableListOf<CallInviteRequest>()
        override suspend fun capabilities(): Result<CallCapabilitiesResponse> =
            if (capabilitiesFailure) Result.failure(IllegalStateException("unknown route"))
            else Result.success(CallCapabilitiesResponse(enabled, 8u, 4u, 30u))
        override suspend fun active(): Result<CallActiveResponse> {
            activeCalls++
            return Result.success(activeGate?.await() ?: CallActiveResponse(listOf(call)))
        }
        override suspend fun get(callId: ULong) = Result.success(CallResponse(call))
        override suspend fun invite(request: CallInviteRequest): Result<CallResponse> {
            invitations += request
            return Result.success(inviteGate?.await() ?: CallResponse(call))
        }
        override suspend fun join(request: CallMutationRequest): Result<CallJoinResponse> {
            mutations += "join" to request
            return Result.success(joinGate?.await() ?: CallJoinResponse(
                call.copy(version = call.version + 1u, participants = call.participants.map {
                    if (it.userId == 1uL) it.copy(state = CallParticipantState.JOINED) else it
                }), "wss://media.invalid", "secret", 50_000, peer = peer.takeIf { call.transport == CallTransport.WEBRTC },
            ))
        }
        private fun finish(action: String, request: CallMutationRequest): Result<CallResponse> {
            mutations += action to request
            if (mutationFailure) return Result.failure(IllegalStateException("offline"))
            return Result.success(CallResponse(call.copy(version = 9u, state = CallState.ENDED)))
        }
        override suspend fun reject(request: CallMutationRequest) = finish("reject", request)
        override suspend fun cancel(request: CallMutationRequest) = finish("cancel", request)
        override suspend fun leave(request: CallMutationRequest) = finish("leave", request)
        override suspend fun end(request: CallMutationRequest) = finish("end", request)
        override suspend fun heartbeat(request: CallMutationRequest): Result<CallResponse> {
            beats++
            heartbeatHook?.invoke()
            if (heartbeatFailures-- > 0) return Result.failure(IllegalStateException("transient"))
            return Result.success(CallResponse(call))
        }
        override suspend fun peerSignal(request: CallPeerSignalRequest): Result<Unit> {
            signals += request
            return if (signalFailures-- > 0) Result.failure(IllegalStateException("transient")) else Result.success(Unit)
        }
        override suspend fun peerEvents(request: CallPeerEventsRequest): Result<CallPeerEventsResponse> {
            polls += request
            return Result.success(peerPollGate?.await() ?: CallPeerEventsResponse(journal.filter { it.seq > request.afterSeq }, journal.lastOrNull()?.seq ?: request.afterSeq))
        }
    }

    @Test fun rejectUsesServerAndNeverConnectsMedia() = runTest {
        val api = Control(); val media = Media(); val c = DefaultCallCoordinator(api, 1u, media, backgroundScope)
        c.onEvent(event(api.call)); assertTrue(c.state.value.incoming)
        c.reject()
        assertEquals("reject", api.mutations.single().first)
        assertNull(c.state.value.call); assertEquals(0, media.connections)
        c.onEvent(event(api.call)); assertNull(c.state.value.call)
        c.close(); runCurrent()
    }

    @Test fun outgoingRingingHangupCancelsRatherThanLeaves() = runTest {
        val api = Control().apply { call = snapshot(initiator = 1u, self = CallParticipantState.JOINED) }
        val media = Media(); val c = DefaultCallCoordinator(api, 1u, media, backgroundScope)
        c.start(9u, CallMediaKind.AUDIO)
        assertTrue(c.state.value.joined)
        c.hangUp()
        assertEquals("cancel", api.mutations.last().first)
        assertNull(c.state.value.call); assertTrue(media.disconnections > 0)
        c.close(); runCurrent()
    }

    @Test fun terminalPushWinsOverLateJoinResponse() = runTest {
        val api = Control().apply { joinGate = CompletableDeferred() }
        val media = Media(); val c = DefaultCallCoordinator(api, 1u, media, backgroundScope)
        c.onEvent(event(api.call)); val accept = launch { c.accept() }; runCurrent()
        c.onEvent(event(api.call.copy(version = 9u, state = CallState.ENDED)))
        api.joinGate!!.complete(CallJoinResponse(snapshot(2u, CallParticipantState.JOINED, CallState.ACTIVE), "url", "token", 10))
        accept.join()
        assertNull(c.state.value.call); assertEquals(0, media.connections)
        c.close(); runCurrent()
    }

    @Test fun rejectWhileJoinInFlightNeverConnectsLateMedia() = runTest {
        val api = Control().apply { joinGate = CompletableDeferred() }
        val media = Media(); val c = DefaultCallCoordinator(api, 1u, media, backgroundScope)
        c.onEvent(event(api.call)); val accept = launch { c.accept() }; runCurrent()
        val reject = launch { c.reject() }; runCurrent()
        assertEquals(listOf("join"), api.mutations.map { it.first })
        api.joinGate!!.complete(CallJoinResponse(snapshot(2u, CallParticipantState.JOINED, CallState.ACTIVE), "url", "token", 10))
        accept.join(); reject.join()
        assertEquals("leave", api.mutations.last().first)
        assertNull(c.state.value.call); assertEquals(0, media.connections)
        c.close(); runCurrent()
    }

    @Test fun failedMutationRetryKeepsCommandId() = runTest {
        val api = Control().apply { mutationFailure = true }
        val c = DefaultCallCoordinator(api, 1u, Media(), backgroundScope)
        c.onEvent(event(api.call)); c.reject(); assertNotNull(c.state.value.error)
        api.mutationFailure = false; c.reject()
        assertEquals(api.mutations[0].second.commandId, api.mutations[1].second.commandId)
        assertNull(c.state.value.call)
        c.close(); runCurrent()
    }

    @Test fun lowerVersionCannotUndoAuthoritativeState() = runTest {
        val api = Control(); val c = DefaultCallCoordinator(api, 1u, Media(), backgroundScope)
        c.onEvent(event(snapshot(8u, CallParticipantState.JOINED, CallState.ACTIVE)))
        c.onEvent(event(snapshot(3u)))
        assertNull(c.state.value.call); assertFalse(c.state.value.incoming)
        c.close(); runCurrent()
    }

    @Test fun answeredOnOtherDeviceDismissesIncomingWithoutEndingRemoteCall() = runTest {
        val api = Control(); val media = Media()
        val c = DefaultCallCoordinator(api, 1u, media, backgroundScope)
        c.onEvent(event(api.call)); assertTrue(c.state.value.incoming)
        val answered = snapshot(8u, CallParticipantState.JOINED, CallState.ACTIVE)
        c.onEvent(event(answered)); runCurrent()
        assertNull(c.state.value.call)
        c.onEvent(event(api.call)); c.onEvent(event(answered))
        api.call = answered
        c.onForegroundOrReconnect(); runCurrent()
        c.hangUp(); c.reject(); c.accept()
        assertNull(c.state.value.call); assertNull(c.state.value.error)
        assertTrue(api.mutations.isEmpty()); assertEquals(0, media.connections)
        c.close(); runCurrent()
    }

    @Test fun otherDeviceWinsWhileLocalJoinIsPending() = runTest {
        val api = Control().apply { joinGate = CompletableDeferred() }
        val media = Media(); val c = DefaultCallCoordinator(api, 1u, media, backgroundScope)
        c.onEvent(event(api.call))
        val accepting = launch { c.accept() }; runCurrent()
        val answered = snapshot(8u, CallParticipantState.JOINED, CallState.ACTIVE)
        c.onEvent(event(answered))
        api.joinGate!!.completeExceptionally(IllegalStateException("owned by another device"))
        accepting.join(); runCurrent()
        assertNull(c.state.value.call); assertNotNull(c.state.value.error)
        c.hangUp(); c.reject(); c.onEvent(event(api.call))
        assertNull(c.state.value.call)
        assertEquals(listOf("join"), api.mutations.map { it.first })
        assertEquals(0, media.connections)
        c.close(); runCurrent()
    }

    @Test fun notificationRejectsPublishForeignUserAndPreservesLocalJoinedCall() = runTest {
        val api = Control(); val media = Media()
        val c = DefaultCallCoordinator(api, 1u, media, backgroundScope)
        c.onEvent(SdkEventEnvelope(1u, 0, SdkEventPayload("subscription_message_received", topic = "call/state",
            payload = Json.encodeToString(CallResponse(api.call)).encodeToByteArray())))
        assertNull(c.state.value.call)
        c.onEvent(event(api.call)); c.accept(); runCurrent()
        val accepted = c.state.value.call!!
        c.onEvent(event(accepted.copy(version = 20u, transport = CallTransport.WEBRTC,
            participants = listOf(CallParticipant(2u, CallParticipantState.JOINED)))))
        assertEquals(accepted, c.state.value.call); assertNull(c.state.value.error)
        c.onEvent(event(accepted.copy(version = 3u, state = CallState.ACTIVE)))
        c.onEvent(event(accepted.copy(version = 1u, transport = CallTransport.WEBRTC)))
        c.onEvent(event(api.call))
        assertEquals(3uL, c.state.value.call?.version); assertTrue(c.state.value.joined)
        assertNull(c.state.value.error)
        assertFalse(c.state.value.incoming); assertEquals(1, media.connections)
        c.close(); runCurrent()
    }

    @Test fun expiredRingingNeverAppearsAndMissingTerminalPacketUsesLocalDeadline() = runTest {
        val api = Control().apply { call = snapshot().copy(expiresAtMs = 1_000) }
        val media = Media()
        val c = DefaultCallCoordinator(api, 1u, media, backgroundScope, nowMs = { testScheduler.currentTime })
        c.onEvent(event(api.call)); assertTrue(c.state.value.incoming)
        advanceTimeBy(1_001); runCurrent()
        assertNull(c.state.value.call); assertTrue(api.mutations.isEmpty())
        assertEquals(1, api.activeCalls)
        c.onEvent(event(api.call)); c.onEvent(event(api.call.copy(version = 2u)))
        assertNull(c.state.value.call); assertNull(c.state.value.error)
        assertEquals(0, media.connections)
        c.close(); runCurrent()
        val late = DefaultCallCoordinator(api, 1u, Media(), backgroundScope, nowMs = { testScheduler.currentTime })
        late.onEvent(event(api.call)); assertNull(late.state.value.call)
        late.start(9u, CallMediaKind.AUDIO)
        assertNull(late.state.value.call); assertFalse(late.state.value.busy)
        assertTrue(api.mutations.isEmpty())
        late.close(); runCurrent()
    }

    @Test fun serverActiveSnapshotCancelsRingingDeadline() = runTest {
        val api = Control().apply { call = snapshot().copy(expiresAtMs = 1_000) }
        val c = DefaultCallCoordinator(api, 1u, Media(), backgroundScope, nowMs = { testScheduler.currentTime })
        c.onEvent(event(api.call)); c.accept(); runCurrent()
        c.onEvent(event(c.state.value.call!!.copy(version = 3u, state = CallState.ACTIVE)))
        advanceTimeBy(1_001); runCurrent()
        assertEquals(CallState.ACTIVE, c.state.value.call?.state); assertTrue(c.state.value.joined)
        assertEquals(listOf("join"), api.mutations.map { it.first })
        c.close(); runCurrent()
    }

    @Test fun oldRingingDeadlineCannotClearNewCall() = runTest {
        val api = Control().apply { call = snapshot().copy(expiresAtMs = 1_000) }
        val c = DefaultCallCoordinator(api, 1u, Media(), backgroundScope, nowMs = { testScheduler.currentTime })
        c.onEvent(event(api.call)); runCurrent()
        c.onEvent(event(api.call.copy(version = 2u, state = CallState.ENDED)))
        val second = api.call.copy(callId = 42u, expiresAtMs = 5_000)
        c.onEvent(event(second)); advanceTimeBy(1_001); runCurrent()
        assertEquals(second, c.state.value.call); assertTrue(api.mutations.isEmpty())
        c.close(); runCurrent()
    }

    @Test fun accountEpochInvalidatesLateInviteAndEvents() = runTest {
        var sameSession = true
        val api = Control().apply { inviteGate = CompletableDeferred() }
        val media = Media(); val c = DefaultCallCoordinator(api, 1u, media, backgroundScope, { sameSession })
        val invite = launch { c.start(9u, CallMediaKind.AUDIO) }; runCurrent()
        sameSession = false; c.close()
        api.inviteGate!!.complete(CallResponse(api.call)); invite.join()
        c.onEvent(event(api.call)); c.onMediaEvent(41u, "failed", "secret")
        assertEquals(CallUiState(), c.state.value); assertEquals(0, media.connections)
        c.close(); runCurrent()
    }

    @Test fun staleActiveResponseDoesNotClearNewPush() = runTest {
        val api = Control().apply { activeGate = CompletableDeferred() }
        val c = DefaultCallCoordinator(api, 1u, Media(), backgroundScope)
        c.onForegroundOrReconnect(); runCurrent()
        c.onEvent(event(api.call))
        api.activeGate!!.complete(CallActiveResponse(emptyList())); runCurrent()
        assertEquals(41uL, c.state.value.call?.callId)
        c.close(); runCurrent()
    }

    @Test fun capabilitiesGateAvailableEntrypoints() = runTest {
        val api = Control().apply { enabled = false }
        val c = DefaultCallCoordinator(api, 1u, Media(), backgroundScope)
        assertFalse(c.state.value.available)
        c.onForegroundOrReconnect(); runCurrent(); assertFalse(c.state.value.available)
        assertEquals(0, api.activeCalls); assertNull(c.state.value.error)
        api.enabled = true; c.onForegroundOrReconnect(); runCurrent(); assertTrue(c.state.value.available)
        c.close(); runCurrent()
    }

    @Test fun heartbeatOnlyWhileLocallyJoinedAndStopsAfterEnd() = runTest {
        val api = Control(); val c = DefaultCallCoordinator(api, 1u, Media(), backgroundScope, heartbeatIntervalMs = 1_000)
        c.onEvent(event(api.call)); advanceTimeBy(5_000); runCurrent(); assertEquals(0, api.beats)
        c.accept(); runCurrent(); advanceTimeBy(1_001); runCurrent(); assertEquals(1, api.beats)
        c.onEvent(event(api.call.copy(version = 9u, state = CallState.ENDED)))
        runCurrent(); advanceTimeBy(5_000); runCurrent(); assertEquals(1, api.beats)
        c.close(); runCurrent()
    }

    @Test fun olderServerDoesNotShowErrorOrQueryActiveAtLogin() = runTest {
        val api = Control().apply { capabilitiesFailure = true }
        val c = DefaultCallCoordinator(api, 1u, Media(), backgroundScope)
        c.onForegroundOrReconnect(); runCurrent()
        assertFalse(c.state.value.available); assertNull(c.state.value.error); assertEquals(0, api.activeCalls)
        c.close(); runCurrent()
    }

    @Test fun cancellingPendingInvitePreventsMediaAndCancelsServerCall() = runTest {
        val api = Control().apply {
            call = snapshot(initiator = 1u, self = CallParticipantState.JOINED)
            inviteGate = CompletableDeferred()
        }
        val media = Media(); val c = DefaultCallCoordinator(api, 1u, media, backgroundScope)
        val start = launch { c.start(9u, CallMediaKind.AUDIO) }; runCurrent()
        c.hangUp(); api.inviteGate!!.complete(CallResponse(api.call)); start.join()
        assertEquals(listOf("cancel"), api.mutations.map { it.first }); assertEquals(0, media.connections)
        assertNull(c.state.value.call)
        c.close(); runCurrent()
    }

    @Test fun closeDisconnectsEvenWhenHostScopeWasCancelled() = runTest {
        val host = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val media = Media(); val c = DefaultCallCoordinator(Control(), 1u, media, host)
        c.onEvent(event(snapshot())); c.accept(); assertTrue(c.state.value.joined)
        host.cancel(); c.close(); runCurrent()
        assertEquals(CallUiState(), c.state.value); assertEquals(1, media.disconnections)
    }

    @Test fun transientHeartbeatFailureRecoversWithoutReconnect() = runTest {
        val api = Control().apply { heartbeatFailures = 1 }
        val c = DefaultCallCoordinator(api, 1u, Media(), backgroundScope, heartbeatIntervalMs = 1_000)
        c.onEvent(event(api.call)); c.accept(); runCurrent()
        advanceTimeBy(2_001); runCurrent()
        assertEquals(2, api.beats); assertTrue(c.state.value.joined); assertNull(c.state.value.error)
        advanceTimeBy(1_001); runCurrent(); assertEquals(3, api.beats)
        c.close(); runCurrent()
    }

    @Test fun repeatedReconnectReconciliationDoesNotStarveHeartbeat() = runTest {
        val api = Control()
        val c = DefaultCallCoordinator(api, 1u, Media(), backgroundScope, heartbeatIntervalMs = 1_000)
        c.onEvent(event(api.call)); c.accept(); runCurrent()
        repeat(10) {
            advanceTimeBy(500)
            c.onForegroundOrReconnect(); runCurrent()
        }
        assertEquals(10, api.activeCalls)
        assertEquals(5, api.beats)
        assertTrue(c.state.value.joined)
        c.close(); runCurrent()
    }

    @Test fun reconnectReconciliationPreservesHeartbeatRetryBudget() = runTest {
        val api = Control().apply { heartbeatFailures = 20 }
        val media = Media()
        val c = DefaultCallCoordinator(api, 1u, media, backgroundScope, heartbeatIntervalMs = 1_000)
        c.onEvent(event(api.call)); c.accept(); runCurrent()
        advanceTimeBy(1_000); runCurrent()
        assertEquals(1, api.beats)
        c.onForegroundOrReconnect(); runCurrent()
        advanceTimeBy(1_000); runCurrent()
        assertEquals(2, api.beats)
        c.onForegroundOrReconnect(); runCurrent()
        advanceTimeBy(2_000); runCurrent()
        assertEquals(3, api.beats)
        assertFalse(c.state.value.joined)
        assertEquals(1, media.disconnections)
        advanceTimeBy(10_000); runCurrent()
        assertEquals(3, api.beats)
        c.close(); runCurrent()
    }

    @Test fun newCallStartsItsOwnHeartbeatAfterPreviousCallEnds() = runTest {
        val api = Control()
        val c = DefaultCallCoordinator(api, 1u, Media(), backgroundScope, heartbeatIntervalMs = 1_000)
        c.onEvent(event(api.call)); c.accept(); runCurrent()
        advanceTimeBy(1_000); runCurrent(); assertEquals(1, api.beats)
        c.onEvent(event(api.call.copy(version = 9u, state = CallState.ENDED))); runCurrent()
        api.call = api.call.copy(callId = 42u)
        c.onEvent(event(api.call)); c.accept(); runCurrent()
        advanceTimeBy(1_000); runCurrent()
        assertEquals(2, api.beats)
        assertEquals(42uL, c.state.value.call?.callId)
        assertTrue(c.state.value.joined)
        c.close(); runCurrent()
    }

    @Test fun repeatedHeartbeatFailureStopsAfterBoundedRetries() = runTest {
        val api = Control().apply { heartbeatFailures = 20 }
        val media = Media(); val c = DefaultCallCoordinator(api, 1u, media, backgroundScope, heartbeatIntervalMs = 1_000)
        c.onEvent(event(api.call)); c.accept(); runCurrent()
        advanceTimeBy(10_000); runCurrent()
        assertEquals(3, api.beats); assertFalse(c.state.value.joined); assertEquals(1, media.disconnections)
        advanceTimeBy(100_000); runCurrent(); assertEquals(3, api.beats)
        c.close(); runCurrent()
    }

    @Test fun closeDuringConnectDisconnectsLateRoom() = runTest {
        val api = Control(); val media = Media().apply { gate = CompletableDeferred() }
        val c = DefaultCallCoordinator(api, 1u, media, backgroundScope)
        c.onEvent(event(api.call)); val accept = launch { c.accept() }; runCurrent()
        c.close(); media.gate!!.complete(Unit); accept.join(); runCurrent()
        assertEquals(CallUiState(), c.state.value); assertEquals(1, media.disconnections)
    }

    @Test fun successfulJoinActuallyEnablesMicrophoneAndCamera() = runTest {
        val api = Control().apply { call = snapshot().copy(mediaKind = CallMediaKind.VIDEO) }
        val media = Media(); val c = DefaultCallCoordinator(api, 1u, media, backgroundScope)
        c.onEvent(event(api.call)); c.accept()
        assertEquals(listOf(true), media.microphoneCalls); assertEquals(listOf(true), media.cameraCalls)
        assertEquals(listOf(true), media.speakerCalls); assertTrue(c.state.value.speakerEnabled)
        assertTrue(c.state.value.joined); assertTrue(c.state.value.microphoneEnabled); assertTrue(c.state.value.cameraEnabled)
        c.close(); runCurrent()
    }

    @Test fun cameraPermissionFailureKeepsConnectedWithoutFalseCameraState() = runTest {
        val api = Control().apply { call = snapshot().copy(mediaKind = CallMediaKind.VIDEO) }
        val media = Media().apply { cameraFailure = true }
        val c = DefaultCallCoordinator(api, 1u, media, backgroundScope)
        c.onEvent(event(api.call)); c.accept()
        assertTrue(c.state.value.joined); assertTrue(c.state.value.microphoneEnabled)
        assertFalse(c.state.value.cameraEnabled); assertNotNull(c.state.value.error)
        assertEquals(listOf(true), media.cameraCalls)
        c.close(); runCurrent()
    }

    @Test fun microphonePermissionFailureTearsDownInsteadOfClaimingSuccess() = runTest {
        val api = Control(); val media = Media().apply { microphoneFailure = true }
        val c = DefaultCallCoordinator(api, 1u, media, backgroundScope)
        c.onEvent(event(api.call)); c.accept()
        assertFalse(c.state.value.joined); assertFalse(c.state.value.microphoneEnabled)
        assertNotNull(c.state.value.error); assertEquals(1, media.disconnections)
        assertEquals("leave", api.mutations.last().first)
        c.close(); runCurrent()
    }

    @Test fun delayedConnectFailureCannotFinishOrSetErrorOnNextCall() = runTest {
        val api = Control(); val media = Media().apply { gate = CompletableDeferred() }
        val c = DefaultCallCoordinator(api, 1u, media, backgroundScope)
        val first = api.call
        c.onEvent(event(first)); val accept = launch { c.accept() }; runCurrent()
        c.onEvent(event(first.copy(version = 9u, state = CallState.ENDED)))
        val second = first.copy(callId = 42u)
        c.onEvent(event(second))
        media.gate!!.completeExceptionally(IllegalStateException("late A connect failure"))
        accept.join(); runCurrent()
        assertEquals(second, c.state.value.call); assertNull(c.state.value.error)
        assertTrue(api.mutations.none { it.first != "join" })
        c.close(); runCurrent()
    }

    @Test fun queuedDisconnectCallbackCannotFinishOrSetErrorOnNextCall() = runTest {
        val api = Control(); val c = DefaultCallCoordinator(api, 1u, Media(), backgroundScope)
        val first = api.call
        c.onEvent(event(first)); c.accept()
        c.onMediaEvent(first.callId, "disconnected", "old A")
        c.onEvent(event(first.copy(version = 9u, state = CallState.ENDED)))
        val second = first.copy(callId = 42u)
        c.onEvent(event(second)); runCurrent()
        assertEquals(second, c.state.value.call); assertNull(c.state.value.error)
        assertTrue(api.mutations.none { it.first != "join" })
        c.close(); runCurrent()
    }

    @Test fun queuedHeartbeatFailureCannotFinishOrSetErrorOnNextCall() = runTest {
        val api = Control().apply { heartbeatFailures = 3 }
        val c = DefaultCallCoordinator(api, 1u, Media(), backgroundScope, heartbeatIntervalMs = 1_000)
        val first = api.call
        val second = first.copy(callId = 42u)
        api.heartbeatHook = {
            if (api.beats == 3) launch {
                c.onEvent(event(first.copy(version = 9u, state = CallState.ENDED)))
                c.onEvent(event(second))
            }
        }
        c.onEvent(event(first)); c.accept(); runCurrent()
        advanceTimeBy(5_000); runCurrent()
        assertEquals(3, api.beats); assertEquals(second, c.state.value.call)
        assertNull(c.state.value.error); assertTrue(api.mutations.none { it.first != "join" })
        c.close(); runCurrent()
    }

    @Test fun audioJoinExplicitlyDisablesSpeaker() = runTest {
        val api = Control(); val media = Media(); val c = DefaultCallCoordinator(api, 1u, media, backgroundScope)
        c.onEvent(event(api.call)); c.accept()
        assertEquals(listOf(false), media.speakerCalls)
        assertFalse(c.state.value.speakerEnabled); assertTrue(c.state.value.joined)
        c.close(); runCurrent()
    }

    @Test fun peerSetupDoesNotClaimConnectionAndReplaysOfferSentBeforeJoin() = runTest {
        val api = Control().apply {
            call = snapshot().copy(transport = CallTransport.WEBRTC)
            journal += CallPeerEvent(1u, 1u, CallPeerSignalKind.OFFER, "raw-offer", 2u)
        }
        val media = Media(); val c = DefaultCallCoordinator(api, 1u, media, backgroundScope)
        c.onEvent(event(api.call)); c.accept(); runCurrent()
        assertEquals(1, media.peerConnections); assertEquals(0, media.connections)
        assertTrue(c.state.value.joined); assertFalse(c.state.value.mediaConnected)
        assertEquals(0uL, api.polls.first().afterSeq); assertEquals(api.journal, media.peerEvents)
        c.onMediaEvent(41u, "connected"); assertTrue(c.state.value.mediaConnected)
        c.onMediaEvent(41u, "reconnecting"); assertFalse(c.state.value.mediaConnected)
        c.close(); runCurrent()
    }

    @Test fun livekitNeverPollsPeerJournal() = runTest {
        val api = Control(); val media = Media(); val c = DefaultCallCoordinator(api, 1u, media, backgroundScope)
        c.onEvent(event(api.call)); c.accept(); runCurrent(); advanceTimeBy(2_500); runCurrent()
        assertEquals(1, media.connections); assertEquals(0, media.peerConnections); assertTrue(api.polls.isEmpty())
        c.close(); runCurrent()
    }

    @Test fun nativeReceiveFailureReplaysUnacknowledgedSignalAndCloseCancelsPolling() = runTest {
        val api = Control().apply {
            call = snapshot().copy(transport = CallTransport.WEBRTC)
            journal += CallPeerEvent(1u, 1u, CallPeerSignalKind.OFFER, "raw-offer", 2u)
            journal += CallPeerEvent(2u, 1u, CallPeerSignalKind.ICE, "candidate-json", 2u)
        }
        val media = Media().apply { peerReceiveFailures = 1 }
        val c = DefaultCallCoordinator(api, 1u, media, backgroundScope)
        c.onEvent(event(api.call)); c.accept(); runCurrent(); advanceTimeBy(501); runCurrent()
        assertEquals(listOf(0uL, 0uL), api.polls.take(2).map { it.afterSeq })
        assertEquals(api.journal, media.peerEvents)
        c.close(); runCurrent(); val polls = api.polls.size
        advanceTimeBy(10_000); runCurrent(); assertEquals(polls, api.polls.size)
    }

    @Test fun signalsGeneratedDuringSetupAreQueuedAndRetryWithSameCommandId() = runTest {
        val api = Control().apply {
            call = snapshot(initiator = 1u, self = CallParticipantState.JOINED).copy(transport = CallTransport.WEBRTC)
            peer = peer.copy(offerer = true); signalFailures = 1
        }
        val media = Media(); val c = DefaultCallCoordinator(api, 1u, media, backgroundScope)
        media.peerSetupEvent = {
            c.onMediaEvent(41u, "peer_signal", """{"revision":1,"kind":"offer","payload":"raw-SDP"}""")
            c.onMediaEvent(41u, "peer_signal", """{"revision":1,"kind":"ice","payload":"candidate-json"}""")
        }
        c.start(9u, CallMediaKind.AUDIO); runCurrent(); advanceTimeBy(251); runCurrent()
        assertEquals(3, api.signals.size)
        assertEquals(api.signals[0], api.signals[1])
        assertEquals(listOf(CallPeerSignalKind.OFFER, CallPeerSignalKind.OFFER, CallPeerSignalKind.ICE), api.signals.map { it.kind })
        c.close(); runCurrent()
    }

    @Test fun oldPeerPollAndCallbackCannotReachNewCall() = runTest {
        val api = Control().apply { call = snapshot().copy(transport = CallTransport.WEBRTC); peerPollGate = CompletableDeferred() }
        val media = Media(); val c = DefaultCallCoordinator(api, 1u, media, backgroundScope)
        val first = api.call
        c.onEvent(event(first)); c.accept(); runCurrent()
        c.onEvent(event(first.copy(version = 9u, state = CallState.ENDED)))
        val second = snapshot().copy(callId = 42u)
        c.onEvent(event(second))
        api.peerPollGate!!.complete(CallPeerEventsResponse(listOf(CallPeerEvent(1u, 1u, CallPeerSignalKind.OFFER, "old", 2u)), 1u))
        c.onMediaEvent(41u, "peer_signal", """{"revision":1,"kind":"answer","payload":"old"}""")
        c.onMediaEvent(41u, "connected"); runCurrent()
        assertEquals(second, c.state.value.call); assertFalse(c.state.value.mediaConnected)
        assertTrue(media.peerEvents.isEmpty()); assertTrue(api.signals.isEmpty()); assertNull(c.state.value.error)
        c.close(); runCurrent()
    }

    @Test fun noTurnZeroExpiryStaysConnectedWithoutAutomaticRenewal() = runTest {
        for (servers in listOf(emptyList(), listOf(CallIceServer(listOf("stun:example.invalid"))))) {
            val api = Control().apply {
                call = snapshot(state = CallState.ACTIVE).copy(transport = CallTransport.WEBRTC)
                peer = peer.copy(iceServers = servers, credentialExpiresAtMs = 0)
            }
            val media = Media()
            val c = DefaultCallCoordinator(api, 1u, media, backgroundScope, nowMs = { testScheduler.currentTime })
            c.onEvent(event(api.call)); c.accept(); runCurrent()
            assertTrue(c.state.value.joined); assertNull(c.state.value.error)
            c.onMediaEvent(41u, "connected")
            advanceTimeBy(120_000); runCurrent()
            assertTrue(c.state.value.mediaConnected); assertNull(c.state.value.error)
            assertEquals(1, api.mutations.count { it.first == "join" })
            assertEquals(1, media.peerConnections); assertEquals(0, media.connections)
            c.close(); runCurrent()
        }
    }

    @Test fun renewalCanDisableTurnAndReturnZeroExpiry() = runTest {
        val api = Control().apply { call = snapshot(state = CallState.ACTIVE).copy(transport = CallTransport.WEBRTC) }
        val media = Media()
        val c = DefaultCallCoordinator(api, 1u, media, backgroundScope, nowMs = { testScheduler.currentTime })
        c.onEvent(event(api.call)); c.accept(); runCurrent()
        api.peer = api.peer.copy(iceServers = emptyList(), credentialExpiresAtMs = 0)
        c.onMediaEvent(41u, "peer_credentials_required"); runCurrent()
        assertEquals(listOf(api.peer), media.peerConfigs)
        c.onMediaEvent(41u, "connected")
        advanceTimeBy(120_000); runCurrent()
        assertNull(c.state.value.error); assertTrue(c.state.value.mediaConnected)
        assertEquals(2, api.mutations.count { it.first == "join" })
        assertEquals(0, media.connections)
        c.close(); runCurrent()
    }

    @Test fun noTurnConnectionFailureIsExplicitAndNeverFallsBackToLivekit() = runTest {
        for (timeout in listOf(false, true)) {
            val api = Control().apply {
                call = snapshot(state = CallState.ACTIVE).copy(transport = CallTransport.WEBRTC)
                peer = peer.copy(iceServers = emptyList(), credentialExpiresAtMs = 0)
            }
            val media = Media()
            val c = DefaultCallCoordinator(api, 1u, media, backgroundScope, nowMs = { testScheduler.currentTime })
            c.onEvent(event(api.call)); c.accept(); runCurrent()
            assertTrue(c.state.value.joined); assertFalse(c.state.value.mediaConnected)
            if (timeout) advanceTimeBy(30_001) else c.onMediaEvent(41u, "failed", "private native detail")
            runCurrent()
            assertNull(c.state.value.call)
            assertEquals(CallFailure.CONNECTION, (c.state.value.error as? CallException)?.reason)
            assertEquals("建立通话失败", com.netonstream.privchat.ui.i18n.zhCall.connectionFailed)
            assertEquals(0, media.connections); assertEquals(1, media.peerConnections)
            c.close(); runCurrent()
        }
    }

    @Test fun turnCredentialsRefreshWithoutRebuildingPeerOrReusingJoinIntent() = runTest {
        val api = Control().apply { call = snapshot().copy(transport = CallTransport.WEBRTC) }
        val media = Media(); val c = DefaultCallCoordinator(api, 1u, media, backgroundScope, nowMs = { testScheduler.currentTime })
        c.onEvent(event(api.call)); c.accept(); runCurrent()
        api.peer = api.peer.copy(credentialExpiresAtMs = 30_000)
        advanceTimeBy(7_501); runCurrent()
        val joins = api.mutations.filter { it.first == "join" }
        assertEquals(2, joins.size); assertNotEquals(joins[0].second.commandId, joins[1].second.commandId)
        assertTrue(joins.all { it.second.supportsPeer })
        assertEquals(1, media.peerConnections); assertEquals(listOf(api.peer), media.peerConfigs)
        assertTrue(c.state.value.joined)
        c.close(); runCurrent()
    }

    @Test fun callTransportCannotChangeOnLaterSnapshot() = runTest {
        val api = Control(); val c = DefaultCallCoordinator(api, 1u, Media(), backgroundScope)
        c.onEvent(event(api.call))
        c.onEvent(event(api.call.copy(version = 2u, transport = CallTransport.WEBRTC)))
        assertEquals(CallTransport.LIVEKIT, c.state.value.call?.transport)
        c.close(); runCurrent()
    }

    @Test fun calleeRestartWaitHasFixedDeadlineAndCannotHangSilently() = runTest {
        val api = Control().apply { call = snapshot().copy(transport = CallTransport.WEBRTC) }
        val c = DefaultCallCoordinator(api, 1u, Media(), backgroundScope)
        c.onEvent(event(api.call)); c.accept(); runCurrent()
        c.onMediaEvent(41u, "connected"); c.onMediaEvent(41u, "peer_restart_needed"); runCurrent()
        assertFalse(c.state.value.mediaConnected)
        advanceTimeBy(20_000); c.onMediaEvent(41u, "peer_restart_needed"); runCurrent()
        advanceTimeBy(10_001); runCurrent()
        assertNull(c.state.value.call); assertNotNull(c.state.value.error)
        assertTrue(api.signals.none { it.kind == CallPeerSignalKind.OFFER })
        c.close(); runCurrent()
    }

    @Test fun recoveredConnectionCancelsCalleeRestartDeadline() = runTest {
        val api = Control().apply { call = snapshot().copy(transport = CallTransport.WEBRTC) }
        val c = DefaultCallCoordinator(api, 1u, Media(), backgroundScope)
        c.onEvent(event(api.call)); c.accept(); runCurrent()
        c.onMediaEvent(41u, "peer_restart_needed"); runCurrent()
        advanceTimeBy(5_000); c.onMediaEvent(41u, "connected")
        advanceTimeBy(30_000); runCurrent()
        assertTrue(c.state.value.mediaConnected); assertNotNull(c.state.value.call); assertNull(c.state.value.error)
        c.close(); runCurrent()
    }

    @Test fun initialSpeakerFailureIsNonfatalAndDoesNotClaimSuccess() = runTest {
        val api = Control().apply { call = snapshot().copy(mediaKind = CallMediaKind.VIDEO) }
        val media = Media().apply { speakerFailure = true }
        val c = DefaultCallCoordinator(api, 1u, media, backgroundScope)
        c.onEvent(event(api.call)); c.accept()
        assertEquals(listOf(true), media.speakerCalls)
        assertTrue(c.state.value.joined); assertFalse(c.state.value.speakerEnabled)
        assertEquals(CallFailure.ROUTING, (c.state.value.error as? CallException)?.reason)
        assertEquals(0, media.disconnections)
        c.close(); runCurrent()
    }
}
