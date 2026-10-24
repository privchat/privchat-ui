package com.netonstream.privchat.ui.emoji

object AppleEmojiTestTags {
    const val Picker = "apple-emoji-picker"
    const val Input = "apple-emoji-input"
    const val Delete = "apple-emoji-delete"
    const val Toggle = "apple-emoji-toggle"
    fun category(index: Int) = "apple-emoji-category-$index"
    fun entry(unicode: String): String {
        val parts = mutableListOf<String>()
        var i = 0
        while (i < unicode.length) {
            val c = unicode[i++]
            val cp = if (c.isHighSurrogate() && i < unicode.length && unicode[i].isLowSurrogate()) {
                ((c.code - 0xD800) shl 10) + unicode[i++].code - 0xDC00 + 0x10000
            } else c.code
            parts += cp.toString(16)
        }
        return "apple-emoji-sequence-${parts.joinToString("-")}"
    }
}
