package com.netonstream.privchat.ui.sticker

import androidx.compose.runtime.*
import com.netonstream.privchat.ui.pag.*
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.layout.onGloballyPositioned
import com.tencent.kuikly.lifecycle.Lifecycle
import com.tencent.kuikly.lifecycle.LifecycleEventObserver
import com.tencent.kuikly.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

/** Optional inline PAG only. Never changes the immutable fallback message payload or opens effects. */
@Composable
fun PublishedSticker(sticker: CatalogSticker, modifier: Modifier, onRetry: () -> Unit = {}) {
    val catalog = StickerUiBridge.catalog
    val session = catalog?.session
    val resourceGenerations = catalog?.offlineResourceGenerations?.collectAsState()?.value
    val generation = sticker.pag?.let { resourceGenerations?.get(it.url) } ?: 0L
    key(catalog, session, sticker.stickerId, sticker.imageUrl, sticker.pag) {
        var visible by remember { mutableStateOf(false) }
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState == Lifecycle.State.RESUMED) }
        DisposableEffect(lifecycle) {
            val observer = LifecycleEventObserver { _, _ -> resumed = lifecycle.currentState == Lifecycle.State.RESUMED }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
        Box(modifier.onGloballyPositioned { coordinates ->
            visible = coordinates.hasVisibleStickerBounds()
        }) {
            val pag = sticker.pag
            val retry = rememberStickerPagRetryState(generation)
            val active = visible && resumed
            val appearance = remember(active) { Any() }
            var firstFrame by remember(retry) { mutableStateOf(false) }
            val host = PagRuntimeUiBridge.host
            val canPlay = session != null && active && retry.canAttempt(appearance) && pag?.validFor(PagUsage.STICKER) == true && PagRuntimeUiBridge.supports(pag.profile)
            if (canPlay && host != null) key(appearance, retry) {
                val owner = remember { Any() }
                val requestId = remember { "sticker-${nextStickerPagRequest()}" }
                var admitted by remember { mutableStateOf(false) }
                val live = remember { booleanArrayOf(true) }
                fun current() = live[0] && StickerUiBridge.catalog === catalog && catalog?.session == session
                DisposableEffect(owner) {
                    live[0] = true
                    admitted = PagRuntimeUiBridge.budget.acquire(owner, PagUsage.STICKER)
                    if (!admitted) retry.admissionDenied(appearance)
                    onDispose { live[0] = false; firstFrame = false; PagRuntimeUiBridge.budget.release(owner) }
                }
                if (admitted) {
                    LaunchedEffect(requestId) {
                        delay(5000)
                        if (current() && !firstFrame) retry.resourceFailed()
                    }
                    host(PagPlaybackRequest(requestId, pag, PagUsage.STICKER, loop = true), Modifier.fillMaxSize()) { event ->
                        if (current() && event.requestId == requestId) when (event.type) {
                            PagEventType.FRAME -> firstFrame = true
                            PagEventType.ERROR -> retry.resourceFailed()
                            PagEventType.REDUCED_MOTION -> { PagRuntimeUiBridge.reducedMotion = true }
                            PagEventType.COMPLETE -> retry.resourceFailed()
                            PagEventType.READY -> Unit
                        }
                    }
                }
            }
            // Keep the native surface visible so its visibility gate can start rendering.
            // The fallback sits ABOVE it until frame confirmation, never alpha-hiding the host.
            if (!canPlay || !firstFrame) PublishedStickerImage(sticker.imageUrl, sticker.label, Modifier.fillMaxSize(), onRetry)
        }
    }
}

private var stickerPagSequence = 0L
private fun nextStickerPagRequest(): Long = ++stickerPagSequence
