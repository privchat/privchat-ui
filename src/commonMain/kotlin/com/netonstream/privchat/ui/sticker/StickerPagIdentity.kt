package com.netonstream.privchat.ui.sticker

/** A bare current-catalog ID is not enough provenance for upgrading a historical message. */
internal fun canResolveStickerPag(stickerId: String?, originalUrl: String?, platform: String?): Boolean {
    if (stickerId.isNullOrEmpty() || stickerId[0] !in '1'..'9' || stickerId.any { it !in '0'..'9' } || stickerId.toULongOrNull() == null || platform.isNullOrBlank()) return false
    return originalUrl == platform.trimEnd('/') + "/sticker/resource/get/" + stickerId
}
