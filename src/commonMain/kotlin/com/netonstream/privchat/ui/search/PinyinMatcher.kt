package com.netonstream.privchat.ui.search

import com.netonstream.privchat.ui.i18n.Pinyin

/**
 * 匹配方式。**顺序即排名**：靠前的更精确，排序时先按它分层。
 *
 * 分层排序比"命中位置越靠前越好"更对：搜 `pengyou` 时，昵称就叫「朋友」的人应该排在
 * 备注里含「朋友」的人前面，哪怕后者的命中位置更靠前。
 */
enum class MatchKind {
    /** 原文完全相等（忽略大小写）。 */
    Exact,
    /** 原文前缀。 */
    LiteralPrefix,
    /** 原文包含。 */
    Literal,
    /** 全拼：每个字都吃掉了完整音节。 */
    FullPinyin,
    /** 全拼但最后一个音节只吃了前缀（`wangz` → 王总）。 */
    PinyinPrefix,
    /** 混拼或首字母（`py`、`pengy`、`pyou`）。 */
    Initials,
}

/**
 * 一次命中。[ranges] 是**原文**的区间，UI 据此高亮。
 *
 * 🔴 区间必须是原文坐标，不能是拼音串的下标。拿拼音下标去截中文，高亮会错位到别的字上，
 * 而且会切开代理对。
 */
data class TextMatch(
    val ranges: List<IntRange>,
    val kind: MatchKind,
    /** 命中起点，用于同层内排序。 */
    val start: Int,
)

/**
 * 拼音 / 原文匹配器。纯逻辑，不依赖 UI。
 *
 * 支持：原文子串、全拼连写、末音节前缀、首字母、中英与拼音混合。
 * **不支持**（有意）：模糊纠错、跳字匹配——那两样会让"为什么这条会出现"无法解释。
 */
object PinyinMatcher {

    /**
     * 查询串归一化：小写、去空白、全角转半角、`ü` 的三种写法统一成 `v`。
     *
     * 只处理拼音输入相关的这几类，不无条件剥离所有变音符号——别的语言的姓名里，
     * 变音符号是字母的一部分，剥掉会把不同的名字搅成一个。
     */
    fun normalizeQuery(raw: String): String = buildString {
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            when {
                c == 'ü' || c == 'Ü' -> append('v')
                c == 'u' && i + 1 < raw.length && raw[i + 1] == ':' -> {
                    append('v'); i++
                }
                c.isWhitespace() -> Unit
                c.code in 0xFF01..0xFF5E -> append((c.code - 0xFEE0).toChar().lowercaseChar())
                else -> append(c.lowercaseChar())
            }
            i++
        }
    }

    fun match(text: String, rawQuery: String): TextMatch? {
        val query = normalizeQuery(rawQuery)
        if (query.isEmpty() || text.isEmpty()) return null

        // 1) 原文匹配优先：它最精确，也最好解释。
        val lower = text.lowercase()
        val literal = lower.indexOf(query)
        if (literal >= 0) {
            val kind = when {
                lower.length == query.length -> MatchKind.Exact
                literal == 0 -> MatchKind.LiteralPrefix
                else -> MatchKind.Literal
            }
            return TextMatch(listOf(literal..(literal + query.length - 1)), kind, literal)
        }

        // 2) 拼音匹配：从每个字边界起试一次。起点必须是字边界——允许从音节中间起，
        //    `ang` 就能命中「王」，命中原因没法向用户解释。
        for (start in text.indices) {
            val consumed = consume(text, start, query, 0, fullSoFar = true)
            if (consumed != null) {
                val (end, kind) = consumed
                return TextMatch(listOf(start..(end - 1)), kind, start)
            }
        }
        return null
    }

    /**
     * 从 [text] 的 [at] 位置起吃掉 [query] 的 [from] 之后的部分。
     *
     * 返回（结束位置，匹配方式）；吃不完返回 null。回溯：一个字有多个读音，
     * 而且"整吃音节"与"只吃首字母"两条路都要试。名字很短，指数展开不是问题。
     */
    private fun consume(
        text: String,
        at: Int,
        query: String,
        from: Int,
        fullSoFar: Boolean,
    ): Pair<Int, MatchKind>? {
        if (from >= query.length) {
            return at to if (fullSoFar) MatchKind.FullPinyin else MatchKind.Initials
        }
        if (at >= text.length) return null

        val c = text[at]
        if (!Pinyin.isHanzi(c)) {
            // 非汉字逐字符比：名字里夹的字母、数字要能连着匹配（`王zong`、`iOS16Pro`）。
            if (c.lowercaseChar() != query[from]) return null
            return consume(text, at + 1, query, from + 1, fullSoFar)
        }

        // 查询里直接敲了汉字（`王zong`）：那一个字必须原样对上，不走读音。
        if (Pinyin.isHanzi(query[from])) {
            if (c != query[from]) return null
            return consume(text, at + 1, query, from + 1, fullSoFar)
        }

        val readings = Pinyin.readingsOf(c)
        if (readings.isEmpty()) return null
        val remaining = query.length - from

        // a) 整吃一个音节
        for (syllable in readings) {
            if (remaining >= syllable.length && query.regionMatches(from, syllable, 0, syllable.length)) {
                consume(text, at + 1, query, from + syllable.length, fullSoFar)?.let { return it }
            }
        }
        // b) 查询在这个字上用完了，且只覆盖音节的一部分 → 末音节前缀
        for (syllable in readings) {
            if (remaining < syllable.length && syllable.startsWith(query.substring(from))) {
                return (at + 1) to if (fullSoFar) MatchKind.PinyinPrefix else MatchKind.Initials
            }
        }
        // c) 只吃首字母
        for (syllable in readings) {
            if (query[from] == syllable[0]) {
                consume(text, at + 1, query, from + 1, fullSoFar = false)?.let { return it }
            }
        }
        return null
    }
}
