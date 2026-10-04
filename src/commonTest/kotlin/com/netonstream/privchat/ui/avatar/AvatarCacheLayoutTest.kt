package com.netonstream.privchat.ui.avatar

import kotlin.test.Test
import kotlin.test.assertEquals

class AvatarCacheLayoutTest {
    private fun match(names: List<String>, vararg uids: String, mtime: Map<String, Long> = emptyMap()) =
        AvatarCacheLayout.matchUserAvatarFiles(names, uids.toSet()) { mtime[it] ?: 0L }

    @Test
    fun findsEachUidFromOneListing() {
        val names = listOf("12-aa.img", "34-bb.img", "56-cc.img", "notes.txt")
        assertEquals(mapOf("12" to "12-aa.img", "56" to "56-cc.img"), match(names, "12", "56", "78"))
    }

    @Test
    fun keepsTheNewestWhenOldVersionsRemain() {
        val names = listOf("12-old.img", "12-new.img")
        val mtime = mapOf("12-old.img" to 100L, "12-new.img" to 200L)
        assertEquals(mapOf("12" to "12-new.img"), match(names, "12", mtime = mtime))
    }

    @Test
    fun fallsBackToTheLegacyName() {
        assertEquals(mapOf("12" to "12.img"), match(listOf("12.img"), "12"))
    }

    @Test
    fun ignoresGeneratedInitialsAndOtherUidsWithTheSamePrefix() {
        // "12.gen-…" is a generated letter avatar; "123-…" belongs to another user.
        val names = listOf("12.gen-1f.img", "123-xx.img")
        assertEquals(emptyMap(), match(names, "12"))
    }
}
