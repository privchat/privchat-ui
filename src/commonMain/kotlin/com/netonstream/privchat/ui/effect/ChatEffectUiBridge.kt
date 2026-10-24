package com.netonstream.privchat.ui.effect

import androidx.compose.runtime.*

/** Parent owns catalog/session/SDK origin adaptation and account-scoped preference persistence. */
object ChatEffectUiBridge {
    var coordinator: ChatEffectCoordinator? by mutableStateOf(null)
    var catalog: ChatEffectCatalog? by mutableStateOf(null)
}
