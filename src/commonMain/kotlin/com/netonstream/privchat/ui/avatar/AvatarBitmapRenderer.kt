package com.netonstream.privchat.ui.avatar

/**
 * 客户端位图管道（AVATAR_CACHE_SPEC §5.2/§5.3）：把「无头像用户的 initials 色块」和
 * 「群九宫格」合成成 **正方形 PNG** 落盘到绝对路径，UI 端统一按 `file://` 加载并自行裁剪
 * （与下载的头像照片同构），不再每次运行时用 Compose 逐格绘制。
 *
 * - Compose 无关，IO 线程调用；配色/首字复用同包的 [AvatarPalette] / [AvatarText]
 *   （三端统一，Rust 侧不做中文字体栅格化）；
 * - 输出是满幅正方形（背景铺满 + 内容居中），圆角由 UI 层裁切，位图不烘焙圆角。
 */
expect object AvatarBitmapRenderer {
    /** initials 色块 PNG（[bgArgb] 铺满 + [initials] 居中 [fgArgb] 字）→ [outPath]，成功 true。 */
    suspend fun renderInitials(
        initials: String,
        bgArgb: Int,
        fgArgb: Int,
        sizePx: Int,
        outPath: String,
    ): Boolean

    /**
     * 九宫格 PNG → [outPath]。[cells] 按行优先最多 9 个（固定 3×3，不足补空格），布局参数
     * 与运行时 `GroupCollageAvatar` 对齐（pad 9% / gap 2% / 直角）。成功 true。
     */
    suspend fun renderCollage(
        cells: List<CollageCell>,
        sizePx: Int,
        outPath: String,
    ): Boolean

    /** 文件是否存在（缓存命中判定）。 */
    fun fileExists(path: String): Boolean
}

/** 九宫格单格：一张本地图片 / 一个 initials 色块 / 空位。 */
sealed interface CollageCell {
    /** 成员已有本地头像文件（下载或生成的 PNG）。 */
    data class Image(val filePath: String) : CollageCell
    /** 成员无头像：画 initials 色块。 */
    data class Initials(val text: String, val bgArgb: Int, val fgArgb: Int) : CollageCell
    /** 空位（浅灰块）。 */
    data object Empty : CollageCell
}
