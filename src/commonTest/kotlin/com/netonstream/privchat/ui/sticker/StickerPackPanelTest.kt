package com.netonstream.privchat.ui.sticker

import com.netonstream.privchat.ui.i18n.stickerPanelStrings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/** Installation authority is supplied by the catalog. This fixture is test-only, never a UI store. */
class StickerPackPanelTest {
    private val owner = StickerSession("https://one/app", 7u, 1)
    private val packs = (0 until 21).map { index ->
        val id = (index + 1).toString()
        val count = when (index) { 0 -> 171; 1, 2 -> 190; else -> 189 }
        StickerPack(id, mapOf("en" to if (index == 0) "Smileys" else "Pack $id"), 2,
            (0 until count).map { item ->
                val stickerId = (index * 256 + item + 1).toString()
                CatalogSticker(stickerId, "https://one/app/sticker/resource/get/$stickerId", "sticker $stickerId", 128, 128)
            }, "https://one/app/covers/$id.png", defaultInstall = index == 0)
    }

    private inner class Catalog : StickerCatalog {
        override var session: StickerSession? = owner
        var rows = listOf(packs.first())
        val resources = packs.flatMap { it.stickers }.associateBy { it.stickerId }
        val writes = mutableListOf<String>()
        var reads = 0
        var installedRead: suspend () -> List<StickerPack> = { rows }
        var storeRead: suspend () -> List<StickerPack> = { packs }
        var afterWrite: () -> Unit = {}
        var reorderResult: ((List<String>) -> List<StickerPack>)? = null
        override suspend fun installed(): List<StickerPack> { reads++; return installedRead() }
        override suspend fun store() = storeRead()
        override suspend fun install(packId: String) {
            writes += "install:$packId"
            if (rows.none { it.packId == packId }) rows = rows + packs.single { it.packId == packId }
            afterWrite()
        }
        override suspend fun uninstall(packId: String) {
            writes += "uninstall:$packId"
            rows = rows.filterNot { it.packId == packId }
            afterWrite()
        }
        override suspend fun reorder(packIds: List<String>) {
            writes += "order:${packIds.joinToString(",")}"
            rows = reorderResult?.invoke(packIds) ?: packIds.map { id -> rows.single { it.packId == id } }
            afterWrite()
        }
        override suspend fun resolve(stickerId: String) = resources.getValue(stickerId)
    }

    @Test fun onlyBackendDefaultIsInstalledAndStoreDoesNotAutomaticallyInstallOtherPacks() = runTest {
        val catalog = Catalog()
        val state = StickerPanelState(catalog, owner)
        state.refreshInstalled(); state.refreshStore()
        assertEquals(21, state.store.size)
        assertEquals(3953, packs.sumOf { it.stickers.size })
        assertTrue(packs.all { it.stickers.size <= 256 && !it.coverImageUrl.isNullOrBlank() })
        assertEquals(listOf("1"), state.installed.map { it.packId })
        assertEquals(171, state.selectedPack?.stickers?.size)
        assertTrue(state.canSend(packs.first()))
        packs.drop(1).forEach { assertFalse(state.canSend(it)) }
        state.selectPack("2")
        assertEquals("1", state.selectedPackId)
        assertTrue(catalog.writes.isEmpty())
    }

    @Test fun defaultFlagAloneNeverBypassesServerInstallationAuthority() = runTest {
        val catalog = Catalog()
        catalog.rows = emptyList()
        val state = StickerPanelState(catalog, owner)
        state.refreshInstalled(); state.refreshStore()
        assertTrue(state.store.first().defaultInstall)
        assertTrue(state.installed.isEmpty())
        assertNull(state.selectedPack)
        assertFalse(state.canSend(packs.first()))
        assertTrue(catalog.writes.isEmpty())
    }

    @Test fun installAndUninstallAreRereadAndHistoryIdentityRemainsResolvable() = runTest {
        val catalog = Catalog()
        val state = StickerPanelState(catalog, owner)
        state.refreshInstalled()
        val history = packs[1].stickers.first()
        state.install("2")
        assertEquals(listOf("1", "2"), state.installed.map { it.packId })
        state.selectPack("2")
        assertTrue(state.canSend(packs[1]))
        state.uninstall("2")
        assertEquals(listOf("install:2", "uninstall:2"), catalog.writes)
        assertEquals(3, catalog.reads)
        assertEquals(listOf("1"), state.installed.map { it.packId })
        assertEquals("1", state.selectedPackId)
        assertFalse(state.canSend(packs[1]))
        assertEquals(history, catalog.resolve(history.stickerId))
        val reopened = StickerPanelState(catalog, owner)
        reopened.refreshInstalled()
        assertEquals(listOf("1"), reopened.installed.map { it.packId })
    }

    @Test fun upDownOrderIsPersistedAndRecoveredByNewPanel() = runTest {
        val catalog = Catalog()
        catalog.rows = packs.take(3)
        val state = StickerPanelState(catalog, owner)
        state.refreshInstalled(); state.selectPack("2")
        state.move("2", -1)
        assertEquals(listOf("2", "1", "3"), state.installed.map { it.packId })
        assertEquals("2", state.selectedPackId)
        state.move("2", 1)
        assertEquals(listOf("1", "2", "3"), state.installed.map { it.packId })
        assertEquals(listOf("order:2,1,3", "order:1,2,3"), catalog.writes)
        val reopened = StickerPanelState(catalog, owner)
        reopened.refreshInstalled()
        assertEquals(listOf("1", "2", "3"), reopened.installed.map { it.packId })
    }

    @Test fun reconciliationUsesServerOrderRatherThanOptimisticClientOrder() = runTest {
        val catalog = Catalog()
        catalog.rows = packs.take(3)
        catalog.reorderResult = { packs.take(3).reversed() }
        val state = StickerPanelState(catalog, owner)
        state.refreshInstalled(); state.move("2", -1)
        assertEquals(listOf("3", "2", "1"), state.installed.map { it.packId })
    }

    @Test fun lateInstalledAndStoreResponsesCannotCrossAccountPlatformOrEpoch() = runTest {
        for (newOwner in listOf(owner.copy(account = 8u), owner.copy(platform = "https://two/app"), owner.copy(epoch = 2), null)) {
            val catalog = Catalog()
            val state = StickerPanelState(catalog, owner)
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            catalog.installedRead = { started.complete(Unit); release.await(); packs }
            val load = async { runCatching { state.refreshInstalled() } }
            started.await(); catalog.session = newOwner; release.complete(Unit)
            assertTrue(load.await().exceptionOrNull() is CancellationException)
            assertFalse(state.installedReady)
            assertTrue(state.installed.isEmpty())
            assertNull(state.selectedPack)
            catalog.session = owner
            catalog.storeRead = { catalog.session = newOwner; packs }
            assertFailsWith<CancellationException> { state.refreshStore() }
            assertFalse(state.storeReady)
            assertTrue(state.store.isEmpty())
        }
    }

    @Test fun accountSwitchDuringMutationCannotApplyOldAccountReadback() = runTest {
        val catalog = Catalog()
        val state = StickerPanelState(catalog, owner)
        state.refreshInstalled()
        catalog.afterWrite = { catalog.session = owner.copy(account = 8u) }
        assertFailsWith<CancellationException> { state.install("2") }
        assertEquals(1, catalog.reads)
        assertFalse(state.installedReady)
        assertFalse(state.busy)
        assertFalse(state.canSend(packs.first()))
    }

    @Test fun modelDefaultsRemainCompatibleAndAllFourLanguagesHaveManagementLabels() {
        val legacy = StickerPack("legacy", emptyMap(), 2, emptyList())
        assertNull(legacy.coverImageUrl)
        assertFalse(legacy.defaultInstall)
        for (lang in listOf("en", "zh-CN", "zh-Hant", "vi")) {
            val strings = stickerPanelStrings(lang)
            assertTrue(strings.manage.isNotBlank())
            assertTrue(strings.moveDown.isNotBlank())
            assertTrue("171" in strings.count(171))
        }
        assertEquals("171 張貼圖", stickerPanelStrings("zh-TW").count(171))
    }
}
