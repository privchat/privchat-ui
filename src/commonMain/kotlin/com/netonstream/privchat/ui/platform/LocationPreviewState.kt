package com.netonstream.privchat.ui.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException

/** One attempt per appearance or explicit retry. Native authorization remains owned by the lease. */
internal class LocationPreviewState(
    private val lease: LocationPreviewBridge.Registration,
    private val location: LocationPresentation,
    private val widthPx: Int,
    private val heightPx: Int,
) {
    private enum class Phase { Pending, Loading, Ready, Failed }
    private var phase by mutableStateOf(Phase.Pending)
    var attempt by mutableStateOf(0L)
        private set
    var image by mutableStateOf<String?>(null)
        private set

    val canRetry: Boolean
        get() = phase == Phase.Failed && LocationPreviewBridge.registration === lease

    fun retry(): Boolean {
        if (!canRetry) return false
        phase = Phase.Pending
        attempt++
        return true
    }

    suspend fun load() {
        if (phase != Phase.Pending || LocationPreviewBridge.registration !== lease) return
        phase = Phase.Loading
        try {
            val result = LocationPreviewBridge.load(lease, location, widthPx, heightPx)
            if (LocationPreviewBridge.registration === lease) {
                image = result
                phase = if (result == null) Phase.Failed else Phase.Ready
            }
        } catch (cancelled: CancellationException) {
            if (LocationPreviewBridge.registration === lease) phase = Phase.Failed
            throw cancelled
        }
    }
}
