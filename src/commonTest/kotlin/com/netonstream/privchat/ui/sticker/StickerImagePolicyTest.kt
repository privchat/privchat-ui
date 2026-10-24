package com.netonstream.privchat.ui.sticker

import kotlin.test.*

class StickerImagePolicyTest {
    @Test fun newResourcesAllowRectanglesButLimitEachAxis() {
        assertTrue(StickerImagePolicy.acceptsNewResource(512, 1))
        assertTrue(StickerImagePolicy.acceptsNewResource(128, 512))
        for ((w, h) in listOf(513 to 128, 128 to 513, 0 to 512, 512 to -1)) {
            assertFalse(StickerImagePolicy.acceptsNewResource(w, h))
        }
    }

    @Test fun decodeBoundsDoNotRejectOrDistortLegacyResources() {
        assertEquals(512 to 256, StickerImagePolicy.decodeSize(2048, 1024))
        assertEquals(128 to 512, StickerImagePolicy.decodeSize(512, 2048))
        assertEquals(512 to 512, StickerImagePolicy.decodeSize(Int.MAX_VALUE, Int.MAX_VALUE))
        assertEquals(1 to 512, StickerImagePolicy.decodeSize(1, Int.MAX_VALUE))
        assertEquals(123 to 321, StickerImagePolicy.decodeSize(123, 321))
        assertFailsWith<IllegalArgumentException> { StickerImagePolicy.decodeSize(0, 512) }
    }

    @Test fun renderHintIsReversibleWithoutChangingStoredOrSignedUrl() {
        for (url in listOf("https://cdn.test/1?signature=abc", "https://cdn.test/1#old")) {
            assertEquals(url, StickerImagePolicy.renderUrl(url).removeSuffix(StickerImagePolicy.RENDER_SUFFIX))
            assertEquals(url, StickerImagePolicy.renderUrl(url, false).removeSuffix(StickerImagePolicy.STATIC_SUFFIX))
        }
    }
}
