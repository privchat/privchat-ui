package com.netonstream.privchat.ui.search

/**
 * 从输入框的一次编辑里推断"用户正在输入的 @ 提及片段"。
 *
 * 🔴 判据是**光标位置**，不是"文本里最后一个 @"。
 *
 * 输入组件只给字符串、不给 selection，所以光标由"这次编辑改了哪一段"反推：
 * 掐掉首尾的公共部分，剩下那段的末尾就是光标。打字和输入法上屏都是一次连续修改，
 * 这个反推是准的。
 *
 * 只看"最后一个 @ 到末尾"会在两种常见输入上误弹面板：
 * - 在句子中间插一个 @（`hello world` → `hello @world`），后面那串被当成查询词；
 * - 提及已经选好之后又在末尾继续打字（`@张三的消息`），整串也被当成查询词。
 */
internal object MentionQuery {

    /**
     * 一次编辑之后的光标位置。
     *
     * 前缀 + 后缀都相同的部分不算改动；新文本去掉公共后缀的位置就是光标。
     */
    fun caretAfterEdit(old: String, new: String): Int {
        var prefix = 0
        while (prefix < old.length && prefix < new.length && old[prefix] == new[prefix]) prefix++
        var suffix = 0
        while (
            suffix < old.length - prefix &&
            suffix < new.length - prefix &&
            old[old.length - 1 - suffix] == new[new.length - 1 - suffix]
        ) {
            suffix++
        }
        return new.length - suffix
    }

    /**
     * 返回 `@` 与光标之间的查询串；不在提及上下文里返回 null。
     *
     * 规则：`@` 必须在行首或紧跟空白（`a@b.com` 这种邮箱不算），且 `@` 与光标之间不含空白。
     */
    fun of(oldText: String, newText: String, isDm: Boolean): String? {
        if (isDm) return null
        val caret = caretAfterEdit(oldText, newText).coerceIn(0, newText.length)
        if (caret == 0) return null
        val atIdx = newText.lastIndexOf('@', startIndex = caret - 1)
        if (atIdx < 0) return null
        if (atIdx > 0 && !newText[atIdx - 1].isWhitespace()) return null
        val query = newText.substring(atIdx + 1, caret)
        if (query.any { it.isWhitespace() }) return null
        return query
    }
}
