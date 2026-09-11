package com.netonstream.privchat.ui.i18n

/**
 * 汉字读音查询。数据见 [CjkPinyinData]（由脚本生成）。
 *
 * 一个字可能有多个读音，**查询时全部参与匹配**（`单` 既是 dan 也是 shan）；
 * 而分组排序只用第一个（最常用的那个），否则同一个人会同时出现在两组里。
 */
internal object Pinyin {

    private const val CJK_START = 0x4E00
    private const val CJK_END = 0x9FFF

    private val syllables: List<String> by lazy { PINYIN_SYLLABLES.split('|') }

    /** 多音字的其余读音。首次用到时才解析——大多数会话根本不搜人。 */
    private val alternates: Map<Char, List<String>> by lazy {
        buildMap {
            PINYIN_ALTERNATES.split(';').forEach { entry ->
                if (entry.isEmpty()) return@forEach
                val colon = entry.indexOf(':')
                if (colon != 1) return@forEach
                put(entry[0], entry.substring(colon + 1).split(','))
            }
        }
    }

    fun isHanzi(c: Char): Boolean = c.code in CJK_START..CJK_END

    /** 首选读音；无读音（或非汉字）返回 null。 */
    fun primaryOf(c: Char): String? {
        if (!isHanzi(c)) return null
        val offset = (c.code - CJK_START) * 2
        val hi = PINYIN_PRIMARY[offset]
        if (hi == '.') return null
        val index = (hi - 'A') * 26 + (PINYIN_PRIMARY[offset + 1] - 'A')
        return syllables.getOrNull(index)
    }

    /** 全部读音，首选在前。 */
    fun readingsOf(c: Char): List<String> {
        val primary = primaryOf(c) ?: return emptyList()
        val alt = alternates[c] ?: return listOf(primary)
        return listOf(primary) + alt
    }

    /**
     * 整串的首选拼音，用于排序。非汉字原样保留。
     *
     * 排序必须用它而不是原文：同在 L 组的「刘(liu)」要排在「李(li)」之后，
     * 按码点排出来是反的。
     */
    fun sortKeyOf(text: String): String = buildString {
        text.forEach { c ->
            val p = primaryOf(c)
            if (p != null) append(p) else append(c.lowercaseChar())
        }
    }
}
