package com.netonstream.privchat.ui.sticker

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

data class CatalogSticker(
    val stickerId: String,
    val imageUrl: String,
    val label: String,
    val width: Int,
    val height: Int,
    val pag: com.netonstream.privchat.ui.pag.PagRepresentation? = null,
)

data class StickerPack(
    val packId: String,
    val names: Map<String, String>,
    val publicationState: Int,
    val stickers: List<CatalogSticker>,
    val coverImageUrl: String? = null,
    val defaultInstall: Boolean = false,
    val version: Int = 0,
    val versionRowId: String? = null,
    val archive: StickerPackArchive? = null,
    val offlineReadiness: StickerOfflineReadiness = StickerOfflineReadiness.UNAVAILABLE,
) {
    fun canSend(installed: Boolean): Boolean = installed && publicationState in 2..3
    fun displayName(language: String): String = names[language]
        ?: names.entries.firstOrNull { it.key.substringBefore('-') == language.substringBefore('-') }?.value
        ?: names["en"] ?: names.values.firstOrNull() ?: packId
}

data class StickerSession(val platform: String, val account: ULong, val epoch: Int)

/** Resource availability only; this never creates an installation relationship. */
enum class StickerOfflineReadiness { UNAVAILABLE, NOT_READY, READY, FAILED }
data class StickerPackArchive(val versionRowId: String, val sha256: String, val size: Long)
data class StickerCachedImage(val originalUrl: String, val fileUrl: String, val sha256: String, val stickerId: String)

/** Account-owned installation state; resources and immutable sticker IDs are not installations. */
interface StickerCatalog {
    val session: StickerSession?
    val offlineChanges: kotlinx.coroutines.flow.StateFlow<Long>? get() = null
    /** Per original resource URL; changes only after verified local bytes are successfully committed. */
    val offlineResourceGenerations: kotlinx.coroutines.flow.StateFlow<Map<String, Long>>? get() = null
    suspend fun cachedImage(originalUrl: String): StickerCachedImage? = null
    suspend fun store(): List<StickerPack>
    suspend fun installed(): List<StickerPack>
    suspend fun install(packId: String)
    suspend fun uninstall(packId: String)
    suspend fun reorder(packIds: List<String>)
    suspend fun resolve(stickerId: String): CatalogSticker
}

/** Resolve only the painter source. Message/outbox/catalog identities keep the original URL. */
internal suspend fun stickerRenderSource(catalog: StickerCatalog?, owner: StickerSession?, originalUrl: String): String {
    if (owner == null || catalog?.session != owner) return originalUrl
    val cached = catalog.cachedImage(originalUrl) ?: return originalUrl
    return cached.fileUrl.takeIf {
        catalog.session == owner && cached.originalUrl == originalUrl && it.startsWith("file://") &&
            cached.sha256.length == 64 && cached.sha256.all { c -> c in "0123456789abcdefABCDEF" }
    } ?: originalUrl
}

object StickerUiBridge {
    var catalog: StickerCatalog? by androidx.compose.runtime.mutableStateOf(null)
}
