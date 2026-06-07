package com.netonstream.privchat.ui.avatar

/**
 * 头像 fallback 文字（initials）的**唯一来源**。Compose 和 bitmap 渲染都从这里取，
 * 杜绝业务页面自己 `name.firstOrNull()?.uppercase()` 的散装实现。
 *
 * 规则（写死）：
 * 1. 优先 [name]，其次 [username]，其次 [fallbackId] 取末 2 位
 * 2. 全空 → "?"
 * 3. 首字符是 CJK / 假名 / 谚文 → **取 1 个字符**（贴近微信观感）
 * 4. 否则按空白切词，取前 2 个词的首字母拼起来；单词不够拿原串前 2 字符；最后 uppercase
 *
 * Phase 1 不接远程图加载；当头像 url 缺失或加载失败时全部回落到 initials。
 */
object AvatarText {

    /** 头像 fallback 中显示「?」时的常量，给调用方比较用。 */
    const val UNKNOWN: String = "?"

    fun initialsOf(
        name: String?,
        username: String? = null,
        fallbackId: Long? = null,
    ): String {
        val source = name?.trim()?.takeIf { it.isNotEmpty() }
            ?: username?.trim()?.takeIf { it.isNotEmpty() }
            ?: fallbackId?.toString()?.takeLast(2)
            ?: return UNKNOWN
        return compute(source)
    }

    private fun compute(s: String): String {
        val first = s.first()
        if (isCjk(first.code)) return first.toString()
        val words = s.split(WHITESPACE).filter { it.isNotEmpty() }
        val initials = words.take(2)
            .mapNotNull { it.firstOrNull()?.toString() }
            .joinToString("")
        return initials.ifBlank { s.take(2) }.uppercase()
    }

    // CJK Unified / Extension A / Hiragana / Katakana / Hangul Syllables
    private fun isCjk(cp: Int): Boolean =
        cp in 0x4E00..0x9FFF ||
            cp in 0x3400..0x4DBF ||
            cp in 0x3040..0x309F ||
            cp in 0x30A0..0x30FF ||
            cp in 0xAC00..0xD7AF

    private val WHITESPACE = Regex("\\s+")
}
