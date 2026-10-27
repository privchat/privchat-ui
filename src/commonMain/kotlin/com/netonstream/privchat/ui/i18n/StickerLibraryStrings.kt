package com.netonstream.privchat.ui.i18n

import com.gearui.i18n.resolveLanguagePack

/** Sticker library copy only; Unicode emoji categories remain separate. */
data class StickerLibraryStrings(
    val search: String,
    val default: String,
    val favorites: String,
    val addPacks: String,
    val back: String,
    val manage: String,
    val installed: String,
    val searchHint: String,
    val emptyFavorites: String,
    val emptySearch: String,
    val favorite: String,
    val removeFavorite: String,
)

private val enStickerLibrary = StickerLibraryStrings(
    search = "Search", default = "Default", favorites = "Favorites", addPacks = "Add sticker packs",
    back = "Back", manage = "Manage", installed = "Installed", searchHint = "Search stickers and packs",
    emptyFavorites = "No favorite stickers yet. Touch and hold a received sticker to add it.",
    emptySearch = "No stickers or packs found", favorite = "Add to favorites", removeFavorite = "Remove from favorites",
)
private val zhStickerLibrary = StickerLibraryStrings(
    search = "搜索", default = "默认", favorites = "收藏", addPacks = "添加表情包",
    back = "返回", manage = "管理", installed = "已添加", searchHint = "搜索表情和表情包",
    emptyFavorites = "还没有收藏的表情，长按收到的表情即可收藏。",
    emptySearch = "未找到表情或表情包", favorite = "收藏", removeFavorite = "取消收藏",
)
private val hantStickerLibrary = StickerLibraryStrings(
    search = "搜尋", default = "預設", favorites = "收藏", addPacks = "新增貼圖包",
    back = "返回", manage = "管理", installed = "已新增", searchHint = "搜尋貼圖和貼圖包",
    emptyFavorites = "還沒有收藏的貼圖，長按收到的貼圖即可收藏。",
    emptySearch = "找不到貼圖或貼圖包", favorite = "收藏", removeFavorite = "取消收藏",
)
private val viStickerLibrary = StickerLibraryStrings(
    search = "Tìm kiếm", default = "Mặc định", favorites = "Yêu thích", addPacks = "Thêm bộ nhãn dán",
    back = "Quay lại", manage = "Quản lý", installed = "Đã thêm", searchHint = "Tìm nhãn dán và bộ nhãn dán",
    emptyFavorites = "Chưa có nhãn dán yêu thích. Nhấn giữ nhãn dán nhận được để thêm.",
    emptySearch = "Không tìm thấy nhãn dán hoặc bộ nhãn dán", favorite = "Thêm vào yêu thích", removeFavorite = "Xóa khỏi yêu thích",
)
private val jaStickerLibrary = StickerLibraryStrings(
    search = "検索", default = "デフォルト", favorites = "お気に入り", addPacks = "スタンプパックを追加",
    back = "戻る", manage = "管理", installed = "追加済み", searchHint = "スタンプやパックを検索",
    emptyFavorites = "お気に入りのスタンプはありません。受信したスタンプを長押しして追加できます。",
    emptySearch = "スタンプやパックが見つかりません", favorite = "お気に入りに追加", removeFavorite = "お気に入りから削除",
)
private val esStickerLibrary = StickerLibraryStrings(
    search = "Buscar", default = "Predeterminados", favorites = "Favoritos", addPacks = "Añadir paquetes de stickers",
    back = "Volver", manage = "Gestionar", installed = "Añadidos", searchHint = "Buscar stickers y paquetes",
    emptyFavorites = "Aún no hay stickers favoritos. Mantén pulsado un sticker recibido para añadirlo.",
    emptySearch = "No se encontraron stickers ni paquetes", favorite = "Añadir a favoritos", removeFavorite = "Quitar de favoritos",
)

private val stickerLibraryPacks = mapOf(
    "en" to enStickerLibrary, "vi" to viStickerLibrary, "ja" to jaStickerLibrary, "es" to esStickerLibrary,
    "zh" to zhStickerLibrary, "zh-Hans" to zhStickerLibrary,
    "zh-Hant" to hantStickerLibrary, "zh-TW" to hantStickerLibrary,
    "zh-HK" to hantStickerLibrary, "zh-MO" to hantStickerLibrary,
)

fun stickerLibraryStrings(languageTag: String): StickerLibraryStrings =
    resolveLanguagePack(languageTag, stickerLibraryPacks, "en")
