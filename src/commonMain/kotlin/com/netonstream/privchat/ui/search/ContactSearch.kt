package com.netonstream.privchat.ui.search

/**
 * 命中来自哪个字段。**必须跟着结果一起传到 UI**。
 *
 * 没有它就只能在界面上显示一个名字，用户看不出"我搜的词到底命中了什么"；
 * 更糟的是，备注命中时若把偏移套到昵称上，高亮会落在完全无关的字上。
 */
enum class SearchField {
    /** 主标题用的名字（备注 > 昵称 的最终结果）。 */
    DisplayName,
    /** 好友备注 / 群昵称等"我给对方起的名字"。 */
    Alias,
    /** 账号名。只在当前用户有权看到时才参与搜索（PROFILE_VISIBILITY §D1）。 */
    Username,
}

/** 一个字段上的命中。 */
data class FieldHit(
    val field: SearchField,
    /** 命中的那段原文所属的完整字段值——高亮和副标题都渲染它，不要另取。 */
    val text: String,
    val match: TextMatch,
)

/**
 * 多字段搜索：在若干字段上找出**最好的一条**命中。
 *
 * 🔴 字段分开匹配，不能拼成一个长串。
 *
 * 把「昵称 + 备注 + 账号名」拼起来搜，会造出跨字段的假命中（昵称结尾 + 备注开头拼成
 * 查询词），而且拼完就再也说不清命中的是哪个字段、区间该落在哪。
 */
object ContactSearch {

    /**
     * [fields] 按优先级给出，同层匹配时排在前面的胜出。
     *
     * 返回 null = 这条记录不该出现在结果里。
     */
    fun bestHit(fields: List<Pair<SearchField, String>>, query: String): FieldHit? {
        var best: FieldHit? = null
        var bestFieldRank = Int.MAX_VALUE
        fields.forEachIndexed { index, (field, text) ->
            if (text.isBlank()) return@forEachIndexed
            val match = PinyinMatcher.match(text, query) ?: return@forEachIndexed
            val current = FieldHit(field, text, match)
            if (best == null || better(current, index, best!!, bestFieldRank)) {
                best = current
                bestFieldRank = index
            }
        }
        return best
    }

    /**
     * 排名：先看匹配方式（原文 > 全拼 > 首字母），再看字段优先级，最后看命中位置。
     *
     * 匹配方式压过字段优先级是有意的：昵称就叫「朋友」的人，应该排在备注里提到「朋友」
     * 的人前面。
     */
    private fun better(a: FieldHit, aFieldRank: Int, b: FieldHit, bFieldRank: Int): Boolean {
        if (a.match.kind != b.match.kind) return a.match.kind.ordinal < b.match.kind.ordinal
        if (aFieldRank != bFieldRank) return aFieldRank < bFieldRank
        return a.match.start < b.match.start
    }

    /**
     * 结果排序键：匹配方式 → 字段 → 命中位置。
     *
     * 同一层里的最终顺序由调用方再补一个稳定项（名字、实体 id），否则两条完全同分的
     * 记录每次刷新都可能互换位置。
     */
    fun rankOf(hit: FieldHit): Int = hit.match.kind.ordinal * 1000 + hit.field.ordinal * 100 +
        hit.match.start.coerceAtMost(99)
}
