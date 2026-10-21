package com.netonstream.privchat.ui.sticker

import androidx.compose.runtime.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Store availability must never gate an already installed pack. */
internal class StickerPanelState(private val catalog: StickerCatalog, private val owner: StickerSession) {
    private val installationMutex = Mutex()
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

    private fun checkSession() {
        if (catalog.session != owner) throw CancellationException("sticker session changed")
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
    fun canSend(pack: StickerPack): Boolean = !busy && installedReady &&
        pack.canSend(installed.any { it.packId == pack.packId }) && catalog.session == owner

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
