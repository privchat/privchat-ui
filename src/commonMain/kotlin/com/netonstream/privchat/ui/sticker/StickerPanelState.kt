package com.netonstream.privchat.ui.sticker

import androidx.compose.runtime.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Store availability must never gate an already installed pack. */
internal class StickerPanelState(private val catalog: StickerCatalog, private val owner: StickerSession) {
    private val installationMutex = Mutex()
    private val favoritesMutex = Mutex()
    var installed by mutableStateOf<List<StickerPack>>(emptyList()); private set
    var store by mutableStateOf<List<StickerPack>>(emptyList()); private set
    var installedReady by mutableStateOf(false); private set
    var storeReady by mutableStateOf(false); private set
    var installedLoading by mutableStateOf(false); private set
    var storeLoading by mutableStateOf(false); private set
    var installedError by mutableStateOf<Throwable?>(null); private set
    var storeError by mutableStateOf<Throwable?>(null); private set
    var actionError by mutableStateOf<Throwable?>(null); private set
    var busy by mutableStateOf(false); private set
    var selectedPackId by mutableStateOf<String?>(null); private set
    var favorites by mutableStateOf<List<CatalogSticker>>(emptyList()); private set
    var favoritesLoading by mutableStateOf(false); private set
    var favoritesError by mutableStateOf<Throwable?>(null); private set
    var searchResult by mutableStateOf(StickerSearchResult(emptyList(), emptyList())); private set
    var searchLoading by mutableStateOf(false); private set
    var searchError by mutableStateOf<Throwable?>(null); private set
    private var searchRevision = 0
    val defaultPack: StickerPack? get() = installed.firstOrNull { it.defaultInstall }
        ?: installed.firstOrNull { it.packId == "apple-smileys-emotion" }
    val selectedPack: StickerPack? get() = installed.firstOrNull { it.packId == selectedPackId }
        ?: installed.firstOrNull()
    fun offlineReadiness(packId: String): StickerOfflineReadiness =
        installed.firstOrNull { it.packId == packId }?.offlineReadiness ?: StickerOfflineReadiness.UNAVAILABLE

    fun selectPack(packId: String) {
        if (installed.any { it.packId == packId }) selectedPackId = packId
    }

    suspend fun install(packId: String) = perform(changesInstallation = true) {
        if (installed.none { it.packId == packId }) catalog.install(packId)
    }
    suspend fun uninstall(packId: String) = perform(changesInstallation = true) { catalog.uninstall(packId) }
    suspend fun move(packId: String, delta: Int) = perform(changesInstallation = true) {
        val ids = installed.map { it.packId }.toMutableList()
        val from = ids.indexOf(packId)
        val to = from + delta
        if (from >= 0 && delta in listOf(-1, 1) && to in ids.indices) {
            ids.add(to, ids.removeAt(from))
            catalog.reorder(ids)
        }
    }

    private fun checkSession() {
        if (catalog.session != owner) throw CancellationException("sticker session changed")
    }
    fun dismissActionError() { actionError = null }

    suspend fun refreshFavorites() = favoritesMutex.withLock { loadFavorites() }

    private suspend fun loadFavorites() {
        checkSession()
        favoritesLoading = true
        favoritesError = null
        try {
            val value = catalog.favorites()
            checkSession()
            favorites = value.distinctBy { it.stickerId }
        } catch (cancel: CancellationException) { throw cancel }
        catch (failure: Exception) { favoritesError = failure }
        finally { favoritesLoading = false }
    }

    suspend fun removeFavorite(stickerId: String) = perform {
        favoritesMutex.withLock {
            checkSession()
            catalog.removeFavorite(stickerId)
            checkSession()
            loadFavorites()
        }
    }

    suspend fun search(query: String) {
        checkSession()
        val revision = ++searchRevision
        searchResult = StickerSearchResult(emptyList(), emptyList())
        searchError = null
        searchLoading = query.isNotBlank()
        if (query.isBlank()) return
        try {
            val value = catalog.search(query.trim().take(100))
            checkSession()
            if (revision == searchRevision) searchResult = value
        } catch (cancel: CancellationException) { throw cancel }
        catch (failure: Exception) { if (revision == searchRevision) searchError = failure }
        finally { if (revision == searchRevision) searchLoading = false }
    }
    suspend fun refreshInstalled() {
        installationMutex.withLock { loadInstalled() }
    }

    private suspend fun loadInstalled() {
        checkSession()
        installedLoading = true
        installedError = null
        try {
            val value = catalog.installed()
            checkSession()
            installed = value
            if (value.none { it.packId == selectedPackId }) selectedPackId = value.firstOrNull()?.packId
            installedReady = true
        } catch (cancel: CancellationException) { throw cancel }
        catch (failure: Exception) { installedError = failure }
        finally { installedLoading = false }
    }
    suspend fun refreshStore() {
        if (storeLoading) return
        checkSession()
        storeLoading = true
        storeError = null
        try {
            val value = catalog.store()
            checkSession()
            store = value
            storeReady = true
        } catch (cancel: CancellationException) { throw cancel }
        catch (failure: Exception) { storeError = failure }
        finally { storeLoading = false }
    }
    fun canSendPackForAction(pack: StickerPack): Boolean = installedReady &&
        pack.canSend(installed.any { it.packId == pack.packId }) && catalog.session == owner
    fun canSend(pack: StickerPack): Boolean = !busy && canSendPackForAction(pack)

    suspend fun perform(changesInstallation: Boolean = false, action: suspend () -> Unit) {
        if (busy) return
        checkSession()
        busy = true
        actionError = null
        try {
            if (changesInstallation) {
                // A pre-mutation GET must finish before the write; always read again afterward.
                installationMutex.withLock {
                    checkSession()
                    installedReady = false
                    try {
                        action()
                        checkSession()
                    } catch (cancel: CancellationException) { throw cancel }
                    catch (failure: Exception) { actionError = failure }
                    loadInstalled()
                }
            } else {
                action()
                checkSession()
            }
        } catch (cancel: CancellationException) { throw cancel }
        catch (failure: Exception) { actionError = failure }
        finally { busy = false }
    }
}

/** A returned success owns the reservation until the timeline consumes it. */
internal suspend fun <T> withStickerSendReservation(
    reserve: () -> Unit, release: () -> Unit, send: suspend () -> Result<T>,
): Result<T> {
    reserve()
    var accepted = false
    try { return send().also { accepted = it.isSuccess } }
    finally { if (!accepted) release() }
}

/** Without message provenance, never resolve a foreign platform's numeric ID. */
internal fun canResolveStickerFallback(originalUrl: String?, platform: String?): Boolean {
    if (originalUrl.isNullOrBlank() || platform.isNullOrBlank()) return false
    val prefix = platform.trimEnd('/') + "/sticker/resource/get/"
    return originalUrl.startsWith(prefix) && originalUrl.length > prefix.length
}
