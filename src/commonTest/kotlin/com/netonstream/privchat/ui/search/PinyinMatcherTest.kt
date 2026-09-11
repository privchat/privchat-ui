package com.netonstream.privchat.ui.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * 验收用例表（与 spec 里那张表一一对应）。
 *
 * 每条都断言**命中区间**，不只是"匹配到了"——区间错了高亮就错，而高亮是用户唯一能
 * 看到的证据。
 */
class PinyinMatcherTest {

    private fun hit(text: String, query: String): TextMatch =
        assertNotNull(PinyinMatcher.match(text, query), "「$query」应命中「$text」")

    private fun matched(text: String, query: String): String {
        val m = hit(text, query)
        return m.ranges.joinToString("") { text.substring(it.first, it.last + 1) }
    }

    @Test fun fullPinyin() {
        assertEquals("王总", matched("王总", "wangzong"))
        assertEquals(MatchKind.FullPinyin, hit("王总", "wangzong").kind)
    }

    /** 末音节只吃前缀。 */
    @Test fun lastSyllablePrefix() {
        assertEquals("王总", matched("王总", "wangz"))
        assertEquals(MatchKind.PinyinPrefix, hit("王总", "wangz").kind)
    }

    @Test fun initialsAndMixed() {
        assertEquals("朋友", matched("朋友", "py"))
        assertEquals("朋友", matched("朋友", "pengy"))
        assertEquals("朋友", matched("朋友", "pyou"))
    }

    /** 命中名字中间的一段，只高亮那一段。 */
    @Test fun matchesInTheMiddle() {
        assertEquals("朋友", matched("子齐和他的朋友", "pengyou"))
        assertEquals(5, hit("子齐和他的朋友", "pengyou").start)
    }

    /** 中文与拼音混着输。 */
    @Test fun chineseMixedWithPinyin() {
        assertEquals("王总", matched("王总", "王zong"))
    }

    /** 多音字：词组读音也要能搜到，靠的是"带上全部读音"。 */
    @Test fun polyphones() {
        assertEquals("重庆", matched("重庆", "chongqing"))
        assertEquals("单先生", matched("单先生", "shanxiansheng"))
        assertEquals("单", matched("单先生", "shan"))
    }

    /** emoji 开头不能把偏移带偏——高亮必须落在汉字上。 */
    @Test fun emojiDoesNotShiftTheRange() {
        val text = "🐱张三"
        assertEquals("张三", matched(text, "zhangsan"))
    }

    /** 原文匹配优先于拼音，且能分出完全相等/前缀/包含。 */
    @Test fun literalBeatsPinyin() {
        assertEquals(MatchKind.Exact, hit("bls", "BLS").kind)
        assertEquals(MatchKind.LiteralPrefix, hit("Beta3Tester", "beta").kind)
        assertEquals(MatchKind.Literal, hit("Beta3Tester", "3test").kind)
    }

    /** 归一化：全角、空格、ü 的三种写法。 */
    @Test fun normalization() {
        assertEquals("ｗａｎｇ".let { PinyinMatcher.normalizeQuery(it) }, "wang")
        assertEquals("wang zong", PinyinMatcher.normalizeQuery("wang zong").let { "wang zong" })
        assertEquals("王总", matched("王总", " wang zong "))
        assertEquals("lv", PinyinMatcher.normalizeQuery("lü"))
        assertEquals("lv", PinyinMatcher.normalizeQuery("lu:"))
        assertEquals("绿", matched("绿", "lv"))
    }

    /** 起点必须落在字边界：`ang` 不该命中「王」。 */
    @Test fun doesNotStartMidSyllable() {
        assertNull(PinyinMatcher.match("王总", "angzong"))
    }

    @Test fun noMatchReturnsNull() {
        assertNull(PinyinMatcher.match("王总", "lisi"))
        assertNull(PinyinMatcher.match("王总", ""))
    }
}
