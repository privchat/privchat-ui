package com.netonstream.privchat.ui.search

/**
 * 判断输入框的一次编辑是不是"用户刚敲下了一个 @"——面板只由这一下打开。
 *
 * 🔴 面板**只在敲下 @ 的那一刻**弹一次，之后输入框里再怎么编辑都不再弹。
 *
 * 曾经的做法是"取最后一个 @ 到光标之间的串当查询词，能匹配到人就开面板"，于是面板会在
 * 用户根本没在提及谁的时候自己冒出来：
 * - 句中插一个 @，后面那串被当成查询词；
 * - 提及选完又接着打字（`@张三的消息`），整串被当成查询词；
 * - 往回退格，删着删着剩下的片段恰好又匹配上了，面板重新弹出来。
 *
 * 而且面板一开就会收键盘，"自己冒出来"就等于"打字打到一半键盘没了"。
 *
 * 敲下 @ 之后的筛选交给面板自己的搜索框：那里有分组和索引条，比在输入框里盲敲片段好用，
 * 也不需要把输入框的光标状态和面板的查询状态两头同步。
 */
internal object MentionQuery {

    /**
     * 这次编辑新插入的字符在新文本中的位置；不是"单纯插入"则返回 null。
     *
     * 输入组件只给字符串、不给 selection，所以改动区间由新旧文本的公共前后缀反推。
     * 删除、替换、输入法一次上屏多字都不算——它们都不该开面板。
     */
    fun singleInsertIndex(old: String, new: String): Int? {
        if (new.length != old.length + 1) return null
        var prefix = 0
        while (prefix < old.length && old[prefix] == new[prefix]) prefix++
        // 前缀之后的部分必须原样右移一位，否则就是"删一段又插一段"的替换。
        for (i in prefix until old.length) {
            if (old[i] != new[i + 1]) return null
        }
        return prefix
    }

    /**
     * 是否应当弹出 @ 选人面板，以及触发符 @ 在新文本中的下标。
     *
     * @ 必须在行首或紧跟空白：`a@b.com` 这种邮箱不是提及。
     */
    fun triggerIndex(oldText: String, newText: String, isDm: Boolean): Int? {
        if (isDm) return null
        val at = singleInsertIndex(oldText, newText) ?: return null
        if (newText[at] != '@') return null
        if (at > 0 && !newText[at - 1].isWhitespace()) return null
        return at
    }
}
