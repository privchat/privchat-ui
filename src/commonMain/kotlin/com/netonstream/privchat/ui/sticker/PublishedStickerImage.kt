package com.netonstream.privchat.ui.sticker

import androidx.compose.runtime.*
import com.gearui.foundation.primitives.Text
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.tencent.kuikly.compose.coil3.AsyncImagePainter
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.layout.onGloballyPositioned
import com.tencent.kuikly.lifecycle.Lifecycle
import com.tencent.kuikly.lifecycle.LifecycleEventObserver
import com.tencent.kuikly.lifecycle.compose.LocalLifecycleOwner

@Composable
internal fun PublishedStickerImage(url: String, label: String, modifier: Modifier, onRetry: () -> Unit = {}) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState == Lifecycle.State.RESUMED) }
    var visible by remember(url) { mutableStateOf(false) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState == Lifecycle.State.RESUMED
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    Box(modifier.onGloballyPositioned { coordinates ->
        visible = coordinates.hasVisibleStickerBounds()
    }, contentAlignment = Alignment.Center) {
        // Removing the native image view stops playback and releases its decoder on page exit,
        // backgrounding and scroll clipping, including non-lazy sticker panel rows.
        if (visible && resumed) ActiveStickerImage(url, label, onRetry)
    }
}

@Composable
private fun ActiveStickerImage(url: String, label: String, onRetry: () -> Unit) {
    val strings = PrivChatI18n.strings
    val catalog = StickerUiBridge.catalog
    val session = catalog?.session
    val cacheRevision = catalog?.offlineChanges?.collectAsState()?.value
    var source by remember(catalog, session, url, cacheRevision) { mutableStateOf<String?>(null) }
    LaunchedEffect(catalog, session, url, cacheRevision) {
        source = stickerRenderSource(catalog, session, url)
    }
    // Do not start a remote request while the account's verified local source is being resolved.
    val resolvedSource = source ?: return
    val owner = remember { Any() }
    DisposableEffect(owner) {
        StickerPlaybackCoordinator.enter(owner)
        onDispose { StickerPlaybackCoordinator.leave(owner) }
    }
    val painter = rememberAsyncImagePainter(StickerImagePolicy.renderUrl(resolvedSource, StickerPlaybackCoordinator.canAnimate(owner)))
    val state by painter.state.collectAsState()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(painter, contentDescription = label, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        // Native pixels may be visible before the painter receives both success and resolution.
        // Never cover transparent sticker artwork with a loading label while that state lags.
        if (state is AsyncImagePainter.State.Error) {
            Text(strings.retry, modifier = Modifier.clickable {
                onRetry()
                painter.restart()
            })
        }
    }
}
