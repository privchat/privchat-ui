package com.netonstream.privchat.ui.call

import com.netonstream.privchat.sdk.dto.SdkEventEnvelope
import com.netonstream.privchat.sdk.dto.call.CallMediaKind
import com.netonstream.privchat.sdk.dto.call.CallSnapshot
import kotlinx.coroutines.flow.StateFlow

/** Host owns permissions, audio routing and native video presentation. Never log credentials. */
data class CallVideoBounds(val x: Float, val y: Float, val width: Float, val height: Float)

interface CallMedia {
    val available: Boolean
    suspend fun connect(call: CallSnapshot, url: String, token: String)
    suspend fun disconnect()
    suspend fun setMicrophone(enabled: Boolean)
    suspend fun setCamera(enabled: Boolean)
    suspend fun setSpeaker(enabled: Boolean)
    fun showVideo(bounds: CallVideoBounds) {}
    fun dismissVideo() {}
}

data class CallUiState(
    val available: Boolean = false,
    val call: CallSnapshot? = null,
    val incoming: Boolean = false,
    val busy: Boolean = false,
    val joined: Boolean = false,
    val microphoneEnabled: Boolean = false,
    val cameraEnabled: Boolean = false,
    val speakerEnabled: Boolean = false,
    val error: Throwable? = null,
)

/** All entry points are called on the host UI dispatcher. One instance per authenticated session. */
interface CallCoordinator {
    val state: StateFlow<CallUiState>
    suspend fun start(channelId: ULong, mediaKind: CallMediaKind, inviteeIds: List<ULong> = emptyList())
    suspend fun accept()
    suspend fun reject()
    suspend fun hangUp()
    suspend fun setMicrophone(enabled: Boolean)
    suspend fun setCamera(enabled: Boolean)
    suspend fun setSpeaker(enabled: Boolean)
    fun showVideo(bounds: CallVideoBounds)
    fun dismissVideo()
    fun onEvent(envelope: SdkEventEnvelope)
    fun onMediaEvent(callId: ULong, type: String, message: String? = null)
    fun onForegroundOrReconnect()
    fun clearError()
    /** Invalidate before replacing the coordinator or authenticating another account. */
    fun close()
}
