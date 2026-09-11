package com.netonstream.privchat.ui.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MentionQueryTest {

    private fun trigger(old: String, new: String) = MentionQuery.triggerIndex(old, new, isDm = false)

    @Test
    fun opensWhenAtIsTyped() {
        assertEquals(0, trigger("", "@"))
        assertEquals(6, trigger("hello ", "hello @"))
    }

    @Test
    fun opensWhenAtIsInsertedMidSentence() {
        // 面板要认得这个 @ 的位置：选中成员后只能替换它本身，不能把后半句吃掉。
        assertEquals(3, trigger("hi world", "hi @world"))
    }

    @Test
    fun doesNotOpenWhileTypingAfterTheAt() {
        // 用户报的 bug：@ 后面每敲一个字都重开面板，等于打字打到一半键盘被收走。
        assertNull(trigger("@", "@b"))
        assertNull(trigger("@b", "@bl"))
        assertNull(trigger("hi @bls", "hi @blsh"))
    }

    @Test
    fun doesNotOpenWhileDeleting() {
        // 往回退格，删到只剩 @ 或剩下的片段恰好能匹配上，都不该把面板重新弹出来。
        assertNull(trigger("@bls", "@bl"))
        assertNull(trigger("hi @b", "hi @"))
        assertNull(trigger("hi @", "hi "))
    }

    @Test
    fun doesNotOpenForEmailLikeAt() {
        assertNull(trigger("mail me at a", "mail me at a@"))
    }

    @Test
    fun doesNotOpenOnMultiCharacterImeCommit() {
        // 输入法一次上屏多字（含 @）不是"敲下 @"，不开面板。
        assertNull(trigger("", "@ab"))
        assertNull(trigger("hi", "hi @x"))
    }

    @Test
    fun doesNotOpenOnReplacement() {
        // 选中一段再输入：长度虽然 +1，但不是单纯插入。
        assertNull(trigger("abc", "@xyz"))
    }

    @Test
    fun neverOpensInDm() {
        assertNull(MentionQuery.triggerIndex("", "@", isDm = true))
    }

    @Test
    fun singleInsertIndexRejectsNonInserts() {
        assertNull(MentionQuery.singleInsertIndex("ab", "ab"))
        assertNull(MentionQuery.singleInsertIndex("abc", "ab"))
        assertEquals(1, MentionQuery.singleInsertIndex("ac", "abc"))
    }
}
