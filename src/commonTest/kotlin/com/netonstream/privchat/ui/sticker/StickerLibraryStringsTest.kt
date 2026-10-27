package com.netonstream.privchat.ui.sticker

import com.netonstream.privchat.ui.i18n.stickerLibraryStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StickerLibraryStringsTest {
    @Test fun localizedLibraryCopyHasEveryRequiredField() {
        for (language in listOf("en", "zh", "zh-Hant", "vi", "ja", "es")) {
            val strings = stickerLibraryStrings(language)
            assertTrue(listOf(
                strings.search, strings.default, strings.favorites, strings.addPacks,
                strings.back, strings.manage, strings.installed, strings.searchHint,
                strings.emptyFavorites, strings.emptySearch, strings.favorite, strings.removeFavorite,
            ).all { it.isNotBlank() }, language)
            assertFalse(strings.favorite == strings.removeFavorite, language)
        }
    }

    @Test fun regionalLanguagesAndFallbackFollowAppResolution() {
        assertEquals(stickerLibraryStrings("en"), stickerLibraryStrings("unknown"))
        assertEquals(stickerLibraryStrings("zh-Hant"), stickerLibraryStrings("zh-TW"))
        assertEquals(stickerLibraryStrings("zh-Hant"), stickerLibraryStrings("zh-HK"))
        assertEquals(stickerLibraryStrings("zh"), stickerLibraryStrings("zh-Hans"))
        assertEquals(stickerLibraryStrings("ja"), stickerLibraryStrings("ja-JP"))
        assertEquals(stickerLibraryStrings("es"), stickerLibraryStrings("es-MX"))
        assertEquals(stickerLibraryStrings("vi"), stickerLibraryStrings("vi-VN"))
    }

    @Test fun favoriteProvenanceRequiresStableIdAndExactPlatformResource() {
        val platform = "https://one.example/app"
        assertTrue(canResolveStickerPag("42", "$platform/sticker/resource/get/42", platform))
        assertTrue(canResolveStickerPag("42", "$platform/sticker/resource/get/42", "$platform/"))
        for (id in listOf(null, "", "0", "042", "-42", "42 ", "18446744073709551616")) {
            assertFalse(canResolveStickerPag(id, "$platform/sticker/resource/get/$id", platform))
        }
        for (url in listOf(null, "file:///42.png", "https://other.example/app/sticker/resource/get/42",
            "$platform/sticker/resource/get/43", "$platform/sticker/resource/get/42?source=other")) {
            assertFalse(canResolveStickerPag("42", url, platform))
        }
        assertFalse(canResolveStickerPag("42", "$platform/sticker/resource/get/42", null))
    }
}
