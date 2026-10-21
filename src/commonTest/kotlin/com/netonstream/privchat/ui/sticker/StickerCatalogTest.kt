package com.netonstream.privchat.ui.sticker

import kotlin.test.*

class StickerCatalogTest {
    @Test fun historyConsumesTypedIdentityWithoutParsingExtraOrUrl() {
        val message = com.netonstream.privchat.sdk.dto.MessageEntry(
            id = 1u, serverMessageId = 2u, localMessageId = null, channelId = 3u,
            channelType = 1, fromUid = 4u, content = "not JSON", extra = "deliberately not JSON",
            status = com.netonstream.privchat.sdk.dto.MessageStatus.Sent, timestamp = 5u, messageType = 6,
            body = com.netonstream.privchat.sdk.dto.MessageContent(
                com.netonstream.privchat.sdk.dto.MessageContentKind.Sticker, "",
                stickerId = "9007199254740993", attachmentUrl = "https://cdn.example/not-an-id.png",
            ),
        )
        val parsed = com.netonstream.privchat.ui.models.parseMessageContent(message)
        assertEquals("9007199254740993", parsed.stickerId)
        assertEquals("https://cdn.example/not-an-id.png", parsed.attachmentUrl)
    }

    @Test fun offlineInstalledPacksRemainSendable() {
        for (state in listOf(2, 3)) {
            val pack = StickerPack("classic", emptyMap(), state, emptyList())
            assertTrue(pack.canSend(installed = true))
            assertFalse(pack.canSend(installed = false))
        }
        assertFalse(StickerPack("draft", emptyMap(), 1, emptyList()).canSend(true))
        assertFalse(StickerPack("unknown", emptyMap(), 4, emptyList()).canSend(true))
    }

    @Test fun sessionIdentityIncludesPlatformAccountAndEpoch() {
        val owner = StickerSession("https://one/app", 7u, 1)
        assertNotEquals(owner, owner.copy(platform = "https://two/app"))
        assertNotEquals(owner, owner.copy(account = 8u))
        assertNotEquals(owner, owner.copy(epoch = 2))
    }

    @Test fun namesRemainCatalogData() {
        val pack = StickerPack("classic", mapOf("zh-CN" to "经典", "en" to "Classic"), 2, emptyList())
        assertEquals("经典", pack.displayName("zh-CN"))
        assertEquals("Classic", pack.displayName("vi"))
    }
}
