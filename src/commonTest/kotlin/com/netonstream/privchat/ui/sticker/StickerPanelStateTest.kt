package com.netonstream.privchat.ui.sticker

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.*

class StickerPanelStateTest {
    private val owner = StickerSession("https://one/app", 7u, 1)
    private val pack = StickerPack("classic", emptyMap(), 3, emptyList())

    private inner class Catalog : StickerCatalog {
        override var session: StickerSession? = owner
        var rows = listOf(pack)
        var getInstalled: suspend () -> List<StickerPack> = { rows }
        var getStore: suspend () -> List<StickerPack> = { emptyList() }
        override suspend fun installed() = getInstalled()
        override suspend fun store() = getStore()
        override suspend fun install(packId: String) { rows = listOf(pack) }
        override suspend fun uninstall(packId: String) { rows = emptyList() }
        override suspend fun reorder(packIds: List<String>) = Unit
        override suspend fun resolve(stickerId: String): CatalogSticker = error("unused")
    }

    @Test fun storeFailureDoesNotBlockInstalledAndExplicitRefreshFindsCrossDeviceChanges() = runTest {
        val catalog = Catalog()
        val state = StickerPanelState(catalog, owner)
        catalog.getStore = { error("store unavailable") }
        state.refreshInstalled()
        state.refreshStore()
        assertNotNull(state.storeError)
        assertTrue(state.installedReady)
        assertTrue(state.canSend(pack))
        catalog.rows = emptyList()
        state.refreshInstalled()
        assertTrue(state.installed.isEmpty())
        assertFalse(state.canSend(pack))
    }

    @Test fun latePreMutationGetCannotRestoreUninstalledPack() = runTest {
        val catalog = Catalog()
        val state = StickerPanelState(catalog, owner)
        state.refreshInstalled()
        val getStarted = CompletableDeferred<Unit>()
        val releaseGet = CompletableDeferred<Unit>()
        var reads = 0
        catalog.getInstalled = {
            reads++
            val snapshot = catalog.rows
            if (reads == 1) {
                getStarted.complete(Unit)
                releaseGet.await()
            }
            snapshot
        }
        val refresh = launch { state.refreshInstalled() }
        getStarted.await()
        var mutationRan = false
        val uninstall = launch {
            state.perform(changesInstallation = true) {
                mutationRan = true
                catalog.uninstall(pack.packId)
            }
        }
        yield()
        assertFalse(mutationRan)
        assertFalse(state.canSend(pack))
        releaseGet.complete(Unit)
        refresh.join()
        uninstall.join()
        assertTrue(mutationRan)
        assertEquals(2, reads)
        assertTrue(state.installedReady)
        assertTrue(state.installed.isEmpty())
        assertFalse(state.canSend(pack))
    }

    @Test fun failedMutationReloadsAuthorityAndDoesNotPermanentlyDisableExistingPack() = runTest {
        val catalog = Catalog()
        val state = StickerPanelState(catalog, owner)
        state.refreshInstalled()
        var reads = 0
        catalog.getInstalled = { reads++; catalog.rows }
        state.perform(changesInstallation = true) { error("write rejected") }
        assertEquals(1, reads)
        assertNotNull(state.actionError)
        assertTrue(state.installedReady)
        assertTrue(state.canSend(pack))
        // A server may have committed before the response was lost.
        state.perform(changesInstallation = true) {
            catalog.uninstall(pack.packId)
            error("response lost")
        }
        assertEquals(2, reads)
        assertTrue(state.installed.isEmpty())
        assertFalse(state.canSend(pack))
    }

    @Test fun failedReconciliationStaysDisabledUntilSuccessfulRefresh() = runTest {
        val catalog = Catalog()
        val state = StickerPanelState(catalog, owner)
        state.refreshInstalled()
        catalog.getInstalled = { error("offline") }
        state.perform(changesInstallation = true) { error("unknown write result") }
        assertNotNull(state.installedError)
        assertFalse(state.canSend(pack))
        catalog.getInstalled = { catalog.rows }
        state.refreshInstalled()
        assertTrue(state.canSend(pack))
    }

    @Test fun lateGetCannotCrossPlatformOrSessionEpoch() = runTest {
        for (newOwner in listOf(owner.copy(platform = "https://two/app"), owner.copy(epoch = 2))) {
            val catalog = Catalog()
            val state = StickerPanelState(catalog, owner)
            catalog.getInstalled = { catalog.session = newOwner; listOf(pack) }
            assertFailsWith<CancellationException> { state.refreshInstalled() }
            assertFalse(state.installedReady)
            assertFalse(state.installedLoading)
            assertFalse(state.canSend(pack))
        }
    }

    @Test fun cancellationAndFailureReturnReservationButSuccessTransfersOwnership() = runTest {
        var pending = 0
        assertFailsWith<CancellationException> {
            withStickerSendReservation<ULong>({ pending++ }, { pending-- }) {
                throw CancellationException("not enqueued")
            }
        }
        assertEquals(0, pending)
        withStickerSendReservation<ULong>({ pending++ }, { pending-- }) {
            Result.failure(IllegalStateException("rejected"))
        }
        assertEquals(0, pending)
        withStickerSendReservation({ pending++ }, { pending-- }) { Result.success(1uL) }
        assertEquals(1, pending)
    }

    @Test fun historyFallbackRequiresVerifiablePlatformResourceOrigin() {
        assertTrue(canResolveStickerFallback("https://one/app/sticker/resource/get/1", owner.platform))
        for (url in listOf(null, "", "https://two/app/sticker/resource/get/1",
            "https://one.evil/app/sticker/resource/get/1", "https://cdn.example/1.png")) {
            assertFalse(canResolveStickerFallback(url, owner.platform))
        }
        assertFalse(canResolveStickerFallback("https://one/app/sticker/resource/get/1", null))
    }
}
