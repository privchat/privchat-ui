package com.netonstream.privchat.ui.emoji

import kotlin.test.*

class AppleEmojiTest {
    @Test fun panelDeletionPreservesEmojiClustersAndSurroundingUnicode() {
        val clusters = listOf("👩🏽‍💻", "👨‍👩‍👧‍👦", "🇸🇬", "1️⃣", "#️⃣", "👍🏿", "❤️", "e\u0301", "\r\n",
            "🏴\uDB40\uDC67\uDB40\uDC62\uDB40\uDC65\uDB40\uDC6E\uDB40\uDC67\uDB40\uDC7F")
        for (cluster in clusters) {
            assertEquals("prefix中", ("prefix中" + cluster).dropLastEmojiCluster(), cluster)
            assertEquals("", cluster.dropLastEmojiCluster(), cluster)
            assertEquals(cluster, (cluster + "x").dropLastEmojiCluster(), cluster)
        }
        assertEquals("", "".dropLastEmojiCluster())
        assertEquals("ab", "abc".dropLastEmojiCluster())
        assertEquals("🇸🇬", "🇸🇬🇨".dropLastEmojiCluster())
        assertEquals("🇸🇬", "🇸🇬🇨🇳".dropLastEmojiCluster())
        assertEquals("a", "a\uD800".dropLastEmojiCluster())
    }

    @Test fun exactLookupKeepsUnknownSequencesAndPresentationAliasesUnmodified() {
        val entry = AppleEmojiEntry("❤️", "Symbols", listOf("❤"), "red heart")
        val catalog = AppleEmojiCatalog("test", listOf(AppleEmojiGroup("Symbols", "Symbols")), listOf(entry))
        assertEquals(entry, catalog.resolve("❤️"))
        assertEquals(entry, catalog.resolve("❤"))
        for (unknown in listOf("text", "❤️‍🛸", "❤︎", "prefix❤️", "🫩")) assertNull(catalog.resolve(unknown), unknown)
        assertEquals("❤", catalog.entries.single().aliases.single())
        assertEquals("❤️", catalog.entries.single().unicode)
    }

    @Test fun invalidCatalogIsRejectedAndAmbiguousAliasesAreNotGuessed() {
        val group = AppleEmojiGroup("g", "Group")
        assertFailsWith<IllegalArgumentException> { AppleEmojiCatalog("v", listOf(group), listOf(AppleEmojiEntry("\uD800", "g"))) }
        assertFailsWith<IllegalArgumentException> { AppleEmojiCatalog("v", listOf(group), listOf(AppleEmojiEntry("😀", "missing"))) }
        assertFailsWith<IllegalArgumentException> { AppleEmojiCatalog.fromManifestChunks("v", listOf("bad\trow")) }
        val catalog = AppleEmojiCatalog("v", listOf(group), listOf(AppleEmojiEntry("😀", "g", listOf("alias")), AppleEmojiEntry("😁", "g", listOf("alias"))))
        assertNull(catalog.resolve("alias"))
    }

    @Test fun completeGeneratedCollectionRetainsEveryCategoryEntryAndAlias() {
        val catalog = AppleEmojiCatalog.fromManifestChunks("18.0", AppleEmojiCatalogData.chunks())
        assertEquals(3953, catalog.entries.size)
        assertEquals(10, catalog.groups.size)
        assertEquals(1272, catalog.entries.sumOf { it.aliases.size })
        assertEquals(catalog.entries, catalog.groups.flatMap { catalog.entriesIn(it.id) })
        for (entry in catalog.entries) {
            assertEquals(entry, catalog.resolve(entry.unicode))
            for (alias in entry.aliases) assertEquals(entry, catalog.resolve(alias))
        }
        for (group in catalog.groups) assertTrue(catalog.entriesIn(group.id).isNotEmpty(), group.id)
        assertTrue(catalog.entries.any { it.unicode == "👩🏽‍💻" })
        assertTrue(catalog.entries.any { it.unicode == "🇸🇬" })
        assertTrue(catalog.entries.any { it.unicode == "1️⃣" })
    }

    @Test fun testIdentifiersEncodeWholeSequencesWithoutUtf16Splitting() {
        assertEquals("apple-emoji-sequence-1f469-1f3fd-200d-1f4bb", AppleEmojiTestTags.entry("👩🏽‍💻"))
        assertEquals("apple-emoji-sequence-1f1f8-1f1ec", AppleEmojiTestTags.entry("🇸🇬"))
        assertEquals("apple-emoji-sequence-31-fe0f-20e3", AppleEmojiTestTags.entry("1️⃣"))
    }

    @Test fun allCategoriesHaveFourLocalizedAccessibilityNamesAndEmojiIcons() {
        val catalog = AppleEmojiCatalog.fromManifestChunks("18.0", AppleEmojiCatalogData.chunks())
        assertEquals(AppleEmojiCategoryIds.all, catalog.groups.map { it.id })
        for (id in AppleEmojiCategoryIds.all) {
            val en = appleEmojiCategoryUi(id, "en").accessibilityLabel
            for (lang in listOf("zh-CN", "zh-Hant", "vi")) {
                val ui = appleEmojiCategoryUi(id, lang)
                assertNotEquals(en, ui.accessibilityLabel)
                assertNotNull(catalog.resolve(ui.icon))
            }
        }
        assertEquals("旗幟", appleEmojiCategoryUi("flags", "zh-TW").accessibilityLabel)
        assertEquals("Other", appleEmojiCategoryUi("future", "unknown").accessibilityLabel)
    }
}
