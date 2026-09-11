package com.netonstream.privchat.ui.i18n

/**
 * 名字 → A–Z 索引字母（中文按拼音首字母，英文按首字母，其余归 `#`）。
 *
 * 为什么需要它：分组过去用的是"显示名首字符"，于是每个中文名各自成组——
 * 索引条会变成一长串汉字，而不是 A–Z。微信是按拼音归组的，这里对齐。
 *
 * 表是离线生成的（见 [CJK_PINYIN_INITIALS]）。要重新生成：
 * ```
 * pip3 install pypinyin
 * # 对 U+4E00..U+9FFF 逐字取 Style.FIRST_LETTER，大写；无读音写 '#'
 * ```
 *
 * **已知取舍**：按字查表，没有词库，所以多音姓氏（单、仇、曾、朴…）按常用读音归组。
 * 要更准就得把词表也带进来，那是另一个量级的体积。
 */
object PinyinIndex {

    private const val CJK_START = 0x4E00
    private const val CJK_END = 0x9FFF

    /** `#` 排在 A–Z 之后：符号、数字、emoji 开头的名字都沉到底部，与微信一致。 */
    const val OTHER = '#'

    /**
     * 取索引字母。取的是**首个有效字符**——名字前面的空格不该把人踢进 `#`，
     * 但名字真的以符号开头（"…"、emoji）就该进 `#`。
     */
    fun initialOf(name: String): Char {
        val first = name.trim().firstOrNull() ?: return OTHER
        return when {
            first in 'a'..'z' -> first.uppercaseChar()
            first in 'A'..'Z' -> first
            first.code in CJK_START..CJK_END -> CJK_PINYIN_INITIALS[first.code - CJK_START]
            else -> OTHER
        }
    }

    /** 排序键：A–Z 在前且保持字母序，`#` 恒在最后。 */
    fun sortKey(letter: Char): Int = if (letter == OTHER) Int.MAX_VALUE else letter.code

    /**
     * 按索引字母分组并排序，`#` 落在最后。
     *
     * 组内按显示名排序，且**忽略大小写**：ASCII 里大写全部小于小写，按原样排会排成
     * BarNick / Beta3Tester / bls —— 同一个字母下先出现所有大写开头的，再出现小写的。
     * 不排序则更糟：顺序取决于成员列表的到达顺序，同一个群每次打开都可能不一样。
     */
    fun <T> group(items: List<T>, nameOf: (T) -> String): List<Pair<Char, List<T>>> =
        items
            .groupBy { initialOf(nameOf(it)) }
            .entries
            .sortedBy { sortKey(it.key) }
            .map { (letter, list) -> letter to list.sortedBy { nameOf(it).lowercase() } }
}
