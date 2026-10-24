package com.netonstream.privchat.ui.pag

import androidx.compose.runtime.*
import com.tencent.kuikly.compose.ui.Modifier

typealias PagComposableHost = @Composable (PagPlaybackRequest, Modifier, (PagEvent) -> Unit) -> Unit

/** Optional by construction: no native runtime is assumed until the parent installs it. */
object PagRuntimeUiBridge {
    var host: PagComposableHost? by mutableStateOf(null)
    var capabilities: PagCapabilities by mutableStateOf(PagCapabilities())
    var animationsEnabled: Boolean by mutableStateOf(true)
    var reducedMotion: Boolean by mutableStateOf(false)
    val budget = PagPlaybackBudget()
    fun supports(profile: String): Boolean = host != null && animationsEnabled && !reducedMotion && capabilities.supports(profile)
}
