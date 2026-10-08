package com.netonstream.privchat.ui.components

// No image decoder below ArkTS; the bubble falls back to its default ratio until the
// real size arrives with the message metadata.
internal actual fun decodeLocalImageDisplaySize(path: String): Pair<Int, Int>? = null
