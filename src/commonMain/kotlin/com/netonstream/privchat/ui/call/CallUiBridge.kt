package com.netonstream.privchat.ui.call

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.netonstream.privchat.sdk.dto.SdkEventEnvelope

object CallUiBridge {
    var coordinator: CallCoordinator? by mutableStateOf(null)
    fun onEvent(envelope: SdkEventEnvelope) { coordinator?.onEvent(envelope) }
    fun onForegroundOrReconnect() { coordinator?.onForegroundOrReconnect() }
}
