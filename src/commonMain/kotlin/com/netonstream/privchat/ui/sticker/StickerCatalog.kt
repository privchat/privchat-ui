package com.netonstream.privchat.ui.sticker

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

data class CatalogSticker(
    val stickerId: String,
    val imageUrl: String,
    val label: String,
    val width: Int,
    val height: Int,
)

data class StickerPack(
    val packId: String,
    val names: Map<String, String>,
    val publicationState: Int,
    val stickers: List<CatalogSticker>,
) {
    fun canSend(installed: Boolean): Boolean = installed && publicationState in 2..3
    fun displayName(language: String): String = names[language]
        ?: names.entries.firstOrNull { it.key.substringBefore('-') == language.substringBefore('-') }?.value
        ?: names["en"] ?: names.values.firstOrNull() ?: packId
}

data class StickerSession(val platform: String, val account: ULong, val epoch: Int)

/** Account-owned installation state; resources and immutable sticker IDs are not installations. */
interface StickerCatalog {
    val session: StickerSession?
    suspend fun store(): List<StickerPack>
    suspend fun installed(): List<StickerPack>
    suspend fun install(packId: String)
    suspend fun uninstall(packId: String)
    suspend fun reorder(packIds: List<String>)
    suspend fun resolve(stickerId: String): CatalogSticker
}

object StickerUiBridge {
    var catalog: StickerCatalog? by androidx.compose.runtime.mutableStateOf(null)
}
