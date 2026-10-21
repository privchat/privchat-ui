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
    private fun snapshot(version: ULong = 1u, self: CallParticipantState = CallParticipantState.INVITED,
                         state: CallState = CallState.RINGING, initiator: ULong = 2u) = CallSnapshot(
        41u, 9u, initiator, CallMediaKind.AUDIO, state, version = version, createdAtMs = 1,
        expiresAtMs = 50_000, participants = listOf(CallParticipant(1u, self), CallParticipant(2u, CallParticipantState.JOINED)),
    )
    private fun event(call: CallSnapshot) = SdkEventEnvelope(1u, 0, SdkEventPayload(
        type = "subscription_message_received", topic = "call/state",
        payload = Json.encodeToString(CallResponse(call)).encodeToByteArray(),
    ))

    private class Media : CallMedia {
        override var available = true
        var connections = 0
        var disconnections = 0
        val microphoneCalls = mutableListOf<Boolean>()
        val cameraCalls = mutableListOf<Boolean>()
        val speakerCalls = mutableListOf<Boolean>()
        var speakerFailure = false
        var cameraFailure = false
        var microphoneFailure = false
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun connect(call: CallSnapshot, url: String, token: String) { connections++; gate?.await() }
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
                }), "wss://media.invalid", "secret", 50_000,
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
        assertEquals(8uL, c.state.value.call?.version); assertFalse(c.state.value.incoming)
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
