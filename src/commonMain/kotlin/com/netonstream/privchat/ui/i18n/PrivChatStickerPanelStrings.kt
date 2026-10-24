package com.netonstream.privchat.ui.i18n

import com.gearui.i18n.resolveLanguagePack

data class PrivChatStickerPanelStrings(val manage: String, val moveDown: String, val countTemplate: String) {
    fun count(value: Int): String = countTemplate.replace("{count}", value.toString())
}

private val enStickerPanel = PrivChatStickerPanelStrings("Manage", "Move down", "{count} stickers")
private val zhStickerPanel = PrivChatStickerPanelStrings("管理", "下移", "{count} 张贴纸")
private val hantStickerPanel = PrivChatStickerPanelStrings("管理", "下移", "{count} 張貼圖")
private val viStickerPanel = PrivChatStickerPanelStrings("Quản lý", "Chuyển xuống", "{count} nhãn dán")
private val stickerPanelPacks = mapOf(
    "en" to enStickerPanel, "vi" to viStickerPanel,
    "zh" to zhStickerPanel, "zh-Hans" to zhStickerPanel,
    "zh-Hant" to hantStickerPanel, "zh-TW" to hantStickerPanel,
    "zh-HK" to hantStickerPanel, "zh-MO" to hantStickerPanel,
)

fun stickerPanelStrings(languageTag: String): PrivChatStickerPanelStrings =
    resolveLanguagePack(languageTag, stickerPanelPacks, "en")
