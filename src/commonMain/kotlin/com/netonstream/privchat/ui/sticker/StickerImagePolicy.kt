package com.netonstream.privchat.ui.sticker

/** Storage validation is for new resources, never a gate for historical rendering. */
object StickerImagePolicy {
    const val MAX_STORED_AXIS = 512
    const val DECODE_AXIS = 512
    const val RENDER_SUFFIX = "#privchat-sticker"
    const val STATIC_SUFFIX = "#privchat-sticker-static"

    fun acceptsNewResource(width: Int, height: Int): Boolean =
        width in 1..MAX_STORED_AXIS && height in 1..MAX_STORED_AXIS

    fun decodeSize(width: Int, height: Int): Pair<Int, Int> {
        require(width > 0 && height > 0)
        val scale = minOf(1.0, DECODE_AXIS.toDouble() / maxOf(width, height))
        return maxOf(1, (width * scale).toInt()) to maxOf(1, (height * scale).toInt())
    }

    // Transport-only hint to native adapters. Never persist this in catalog/message URLs.
    fun renderUrl(url: String, animate: Boolean = true): String = url + if (animate) RENDER_SUFFIX else STATIC_SUFFIX

    fun originalUrl(renderUrl: String): String = when {
        renderUrl.endsWith(STATIC_SUFFIX) -> renderUrl.removeSuffix(STATIC_SUFFIX)
        renderUrl.endsWith(RENDER_SUFFIX) -> renderUrl.removeSuffix(RENDER_SUFFIX)
        else -> renderUrl
    }
}
