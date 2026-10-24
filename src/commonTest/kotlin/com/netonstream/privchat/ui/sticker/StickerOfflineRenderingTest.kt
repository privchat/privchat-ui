package com.netonstream.privchat.ui.sticker

import kotlinx.coroutines.test.runTest
import kotlin.test.*

class StickerOfflineRenderingTest {
    private val owner = StickerSession("https://one/app", 7u, 1)
    private val original = "https://one/app/sticker/resource/get/10001"
    private val image = StickerCachedImage(original, "file:///verified/account7/10001.png", "a".repeat(64), "10001")
    private inner class Catalog : StickerCatalog {
        override var session: StickerSession? = owner
        var hit: StickerCachedImage? = image
        var reads = 0
        var rows = emptyList<StickerPack>()
        var onResolve: () -> Unit = {}
        override suspend fun cachedImage(originalUrl: String): StickerCachedImage? { reads++; onResolve(); return hit }
        override suspend fun installed() = rows
        override suspend fun store() = rows
        override suspend fun install(packId: String) = error("test must not create installation")
        override suspend fun uninstall(packId: String) = error("unused")
        override suspend fun reorder(packIds: List<String>) = Unit
        override suspend fun resolve(stickerId: String): CatalogSticker = error("unused")
    }
    @Test fun painterSourceChangesButCatalogAndSendIdentityDoNot() = runTest {
        val catalog = Catalog()
        val sticker = CatalogSticker("10001", original, "smile", 256, 128)
        val file = stickerRenderSource(catalog, owner, sticker.imageUrl)
        assertEquals(image.fileUrl, file)
        assertEquals(original, sticker.imageUrl)
        assertEquals("10001", sticker.stickerId)
        assertEquals(image.fileUrl, StickerImagePolicy.originalUrl(StickerImagePolicy.renderUrl(file)))
    }
    @Test fun missingMismatchOrInvalidCacheRecordFallsBackToOriginal() = runTest {
        val catalog = Catalog()
        for (bad in listOf(null, image.copy(originalUrl = "$original/other"), image.copy(sha256 = "bad"),
            image.copy(sha256 = "g".repeat(64)), image.copy(fileUrl = "https://other/10001.png"))) {
            catalog.hit = bad
            assertEquals(original, stickerRenderSource(catalog, owner, original))
        }
    }
    @Test fun platformAccountEpochAndLogoutNeverUseAnotherOwnersCachedFile() = runTest {
        val catalog = Catalog()
        for (session in listOf(null, owner.copy(platform = "https://two/app"), owner.copy(account = 8u), owner.copy(epoch = 2))) {
            catalog.session = session
            assertEquals(original, stickerRenderSource(catalog, owner, original))
        }
        assertEquals(0, catalog.reads)
        catalog.session = owner
        catalog.onResolve = { catalog.session = owner.copy(epoch = 2) }
        assertEquals(original, stickerRenderSource(catalog, owner, original))
    }
    @Test fun offlineReadinessIsNotInstallationTruth() = runTest {
        val catalog = Catalog()
        val pack = StickerPack("smileys", emptyMap(), 2, emptyList(), offlineReadiness = StickerOfflineReadiness.READY)
        catalog.rows = listOf(pack)
        val state = StickerPanelState(catalog, owner)
        state.refreshStore()
        assertEquals(StickerOfflineReadiness.UNAVAILABLE, state.offlineReadiness("smileys"))
        assertFalse(state.canSend(pack))
        for (readiness in StickerOfflineReadiness.entries) {
            catalog.rows = listOf(pack.copy(offlineReadiness = readiness))
            state.refreshInstalled()
            assertEquals(readiness, state.offlineReadiness("smileys"))
            assertTrue(state.canSend(pack))
        }
        catalog.rows = emptyList()
        state.refreshInstalled()
        assertFalse(state.canSend(pack))
        assertEquals(StickerOfflineReadiness.UNAVAILABLE, state.offlineReadiness("smileys"))
    }
}
