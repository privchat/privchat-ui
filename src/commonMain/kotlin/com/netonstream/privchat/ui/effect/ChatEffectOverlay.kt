package com.netonstream.privchat.ui.effect

import androidx.compose.runtime.*
import com.netonstream.privchat.ui.pag.*
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.lifecycle.Lifecycle
import com.tencent.kuikly.lifecycle.LifecycleEventObserver
import com.tencent.kuikly.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

/** Decorative only: no pointer input, focus, clicks, notifications, or message mutations. */
@Composable
fun ChatEffectOverlay(coordinator: ChatEffectCoordinator, modifier: Modifier = Modifier) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState == Lifecycle.State.RESUMED) }
    DisposableEffect(lifecycle, coordinator) {
        val observer = LifecycleEventObserver { _, _ -> resumed = lifecycle.currentState == Lifecycle.State.RESUMED }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val playback by coordinator.active.collectAsState()
    val current = playback ?: return
    key(current.requestId) {
        val owner = remember { Any() }
        var admitted by remember { mutableStateOf(false) }
        val host = PagRuntimeUiBridge.host
        val available = resumed && PagRuntimeUiBridge.supports(current.resource.pag.profile)
        DisposableEffect(owner, available, host) {
            admitted = available && PagRuntimeUiBridge.budget.acquire(owner, PagUsage.EFFECT)
            if (!admitted) coordinator.cancel(current.requestId) // No queue or deferred resume.
            onDispose {
                PagRuntimeUiBridge.budget.release(owner)
                coordinator.cancel(current.requestId)
            }
        }
        LaunchedEffect(current.requestId) {
            while (coordinator.active.value?.requestId == current.requestId) {
                coordinator.tick()
                delay(50)
            }
        }
        if (admitted && host != null) {
            host(PagPlaybackRequest(current.requestId, current.resource.pag, PagUsage.EFFECT, loop = false), modifier.fillMaxSize()) { event ->
                if (event.requestId == current.requestId) {
                    if (event.type == PagEventType.REDUCED_MOTION) PagRuntimeUiBridge.reducedMotion = true
                    coordinator.onPagEvent(event)
                }
            }
        }
    }
}
