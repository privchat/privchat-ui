package com.netonstream.privchat.ui.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ContactSearchTest {

    private fun fields(name: String, alias: String = "", username: String = "") = listOf(
        SearchField.DisplayName to name,
        SearchField.Alias to alias,
        SearchField.Username to username,
    )

    /** 命中备注时，返回的必须是**备注**字段和备注里的区间。 */
    @Test
    fun aliasHitKeepsItsOwnFieldAndRange() {
        val hit = assertNotNull(ContactSearch.bestHit(fields("唐子齐", alias = "子齐和他的朋友"), "pengyou"))
        assertEquals(SearchField.Alias, hit.field)
        assertEquals("子齐和他的朋友", hit.text)
        assertEquals("朋友", hit.text.substring(hit.match.ranges[0].first, hit.match.ranges[0].last + 1))
    }

    /** 🔴 字段不能拼成一个长串搜：那样会造出跨字段的假命中。 */
    @Test
    fun doesNotMatchAcrossFieldBoundaries() {
        // 昵称以「王」结尾、备注以「总」开头；拼起来能凑出 wangzong，分开就不该命中。
        assertNull(ContactSearch.bestHit(fields("小王", alias = "总部对接"), "xiaowangzongbu"))
    }

    /** 昵称就叫「朋友」的，排在备注里提到「朋友」的前面。 */
    @Test
    fun betterMatchKindWinsOverFieldPriority() {
        val onName = assertNotNull(ContactSearch.bestHit(fields("朋友"), "pengyou"))
        val onAlias = assertNotNull(ContactSearch.bestHit(fields("唐子齐", alias = "子齐和他的朋友"), "pengyou"))
        // 「pengyou」对「朋友」是全拼命中，不是原文相等——Exact 只给字面相同的情况。
        assertEquals(MatchKind.FullPinyin, onName.match.kind)
        assertEquals(true, ContactSearch.rankOf(onName) < ContactSearch.rankOf(onAlias))
    }

    /** 同一条记录里，昵称与备注都命中时取更精确的那个。 */
    @Test
    fun picksTheBestFieldWithinOneRecord() {
        val hit = assertNotNull(ContactSearch.bestHit(fields("朋友", alias = "很久以前的朋友"), "pengyou"))
        assertEquals(SearchField.DisplayName, hit.field)
    }

    /** 空字段直接跳过，不该在结果里冒出空白副标题。 */
    @Test
    fun blankFieldsAreSkipped() {
        assertNull(ContactSearch.bestHit(fields("", alias = "  "), "a"))
    }

    /** 没权限看到 username 时，调用方不传这个字段，自然搜不到。 */
    @Test
    fun usernameOnlyMatchesWhenProvided() {
        assertNull(ContactSearch.bestHit(listOf(SearchField.DisplayName to "张三"), "zs001"))
        val hit = ContactSearch.bestHit(fields("张三", username = "zs001"), "zs001")
        assertEquals(SearchField.Username, assertNotNull(hit).field)
    }
}
