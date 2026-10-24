package com.netonstream.privchat.ui.sticker

import androidx.compose.runtime.*

/** One resource failure per committed generation; admission denial lasts only for that appearance. */
@Stable
internal class StickerPagRetryState {
    private var resourceFailed by mutableStateOf(false)
    private var deniedAppearance by mutableStateOf<Any?>(null)
    fun canAttempt(appearance: Any): Boolean = !resourceFailed && deniedAppearance !== appearance
    fun admissionDenied(appearance: Any) { deniedAppearance = appearance }
    fun resourceFailed() { resourceFailed = true }
}

@Composable
internal fun rememberStickerPagRetryState(generation: Long): StickerPagRetryState =
    remember(generation) { StickerPagRetryState() }
