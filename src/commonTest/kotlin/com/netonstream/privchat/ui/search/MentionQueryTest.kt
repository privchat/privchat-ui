package com.netonstream.privchat.ui.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MentionQueryTest {

    private fun typed(old: String, new: String) = MentionQuery.of(old, new, isDm = false)

    /** 刚敲下 @：片段为空，面板该弹。 */
    @Test fun typingAtOpensWithEmptyQuery() {
        assertEquals("", typed("hi ", "hi @"))
        assertEquals("", typed("", "@"))
    }

    @Test fun typingAfterAtBuildsTheQuery() {
        assertEquals("bl", typed("hi @b", "hi @bl"))
    }

    /** 🔴 在句子中间插 @：查询串是 @ 与**光标**之间，不是到文本末尾。 */
    @Test fun insertingAtInTheMiddleDoesNotSwallowTheRest() {
        assertEquals("", typed("hello world", "hello @world"))
    }

    /** 🔴 提及选完之后继续在末尾打字，不该再被当成提及片段。 */
    @Test fun typingAfterACompletedMentionIsNotAQuery() {
        // "@张三 " 之后打「的消息」——@ 与光标之间有空白
        assertNull(typed("@张三 ", "@张三 的"))
        // 没有空格的情况：@ 与光标之间是「张三的」，但那是在**末尾**继续打字，
        // 面板由调用方按"片段匹配不到人"再关一次（见 MessagePage 的可见条件）。
        assertEquals("张三的", typed("@张三", "@张三的"))
    }

    /** 邮箱不是提及。 */
    @Test fun emailIsNotAMention() {
        assertNull(typed("a@b.co", "a@b.com"))
    }

    /** 片段里不能有空白。 */
    @Test fun whitespaceEndsTheFragment() {
        assertNull(typed("hi @bls", "hi @bls "))
    }

    /** 私聊没有提及。 */
    @Test fun directChannelsNeverMention() {
        assertNull(MentionQuery.of("hi ", "hi @", isDm = true))
    }

    /** 删字也要跟着收：从 `@ab` 删到 `@a`，片段是 `a`。 */
    @Test fun deletingShrinksTheFragment() {
        assertEquals("a", typed("hi @ab", "hi @a"))
        assertEquals("", typed("hi @a", "hi @"))
        assertNull(typed("hi @", "hi "))
    }

    /** 输入法一次上屏多个字。 */
    @Test fun imeCommitsSeveralCharsAtOnce() {
        assertEquals("张三", typed("@", "@张三"))
    }
}
