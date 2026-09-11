package com.netonstream.privchat.ui.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PinyinIndexTest {

    /** 中文按拼音首字母，不是按字符本身——这正是换掉旧分组的理由。 */
    @Test
    fun chineseGoesToItsPinyinInitial() {
        assertEquals('Z', PinyinIndex.initialOf("张三"))
        assertEquals('L', PinyinIndex.initialOf("李工"))
        assertEquals('W', PinyinIndex.initialOf("王五"))
        assertEquals('Q', PinyinIndex.initialOf("钱多多"))
        assertEquals('O', PinyinIndex.initialOf("欧阳锋"))
    }

    @Test
    fun asciiUsesItsOwnLetterCaseInsensitively() {
        assertEquals('B', PinyinIndex.initialOf("bls"))
        assertEquals('B', PinyinIndex.initialOf("Beta3Tester"))
        assertEquals('M', PinyinIndex.initialOf("Miokirukira"))
    }

    /** 符号、数字、emoji、空名字全归 `#`。 */
    @Test
    fun everythingElseFallsToHash() {
        assertEquals('#', PinyinIndex.initialOf("123"))
        assertEquals('#', PinyinIndex.initialOf("——某人"))
        assertEquals('#', PinyinIndex.initialOf("🐱猫"))
        assertEquals('#', PinyinIndex.initialOf(""))
        assertEquals('#', PinyinIndex.initialOf("   "))
    }

    /** 前导空格不该把人踢进 `#`。 */
    @Test
    fun leadingWhitespaceIsIgnored() {
        assertEquals('Z', PinyinIndex.initialOf("  张三"))
        assertEquals('A', PinyinIndex.initialOf(" alice"))
    }

    /** 🔴 `#` 必须排在 A–Z **之后**，不能因为 '#' < 'A' 就跑到最前面。 */
    @Test
    fun hashSortsLast() {
        val grouped = PinyinIndex.group(listOf("张三", "alice", "123", "李四")) { it }
        assertEquals(listOf('A', 'L', 'Z', '#'), grouped.map { it.first })
    }

    @Test
    fun membersInsideAGroupAreSortedByName() {
        val grouped = PinyinIndex.group(listOf("zoe", "amy", "adam")) { it }
        assertEquals(listOf("adam", "amy"), grouped.first { it.first == 'A' }.second)
    }

    /** 大小写不参与排序：否则 ASCII 里大写全小于小写，一组里会先大写后小写。 */
    @Test
    fun sortingInsideAGroupIgnoresCase() {
        val grouped = PinyinIndex.group(listOf("bls", "BarNick", "Beta3Tester")) { it }
        assertEquals(
            listOf("BarNick", "Beta3Tester", "bls"),
            grouped.first { it.first == 'B' }.second,
        )
    }

    /** 表要覆盖整个基本区，越界访问会崩在用户脸上。 */
    @Test
    fun tableCoversTheWholeBasicBlock() {
        assertEquals(0x9FFF - 0x4E00 + 1, CJK_PINYIN_INITIALS.length)
        assertTrue(CJK_PINYIN_INITIALS.all { it in 'A'..'Z' || it == '#' })
    }
}

class PinyinSortTest {
    /** 🔴 组内必须按全拼排：按码点排会把「刘」排到「李」前面。 */
    @Test
    fun chineseSortsByFullPinyinNotCodePoint() {
        val grouped = PinyinIndex.group(listOf("刘一", "李四", "兰花")) { it }
        assertEquals(listOf("兰花", "李四", "刘一"), grouped.first { it.first == 'L' }.second)
    }

    @Test
    fun asciiStillSortsCaseInsensitively() {
        val grouped = PinyinIndex.group(listOf("bls", "BarNick", "Beta3Tester")) { it }
        assertEquals(listOf("BarNick", "Beta3Tester", "bls"), grouped.first { it.first == 'B' }.second)
    }
}
