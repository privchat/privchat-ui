package com.netonstream.privchat.ui.avatar

/**
 * 头像 fallback 调色板的**唯一来源**。Compose 端通过 [AvatarResolver] 取主题敏感版本，
 * Bitmap 渲染（QR 中心头像等无法读 [com.gearui.theme.Theme]）直接用这里的 ARGB 常量，
 * 保证两条管道颜色一致。
 *
 * 当前固定按 light theme 的 `muted` / `mutedForeground` 取色——QR 等 bitmap 产物多在
 * 白底 / 相册里看，统一一套 light token 视觉更稳定；远程头像加载是下一阶段。
 */
object AvatarPalette {
    /** light theme `muted` (0xFFF4F4F5)。Compose ARGB 与 Android `Color.rgb` 通用。 */
    const val DEFAULT_BACKGROUND_ARGB: Int = 0xFFF4F4F5.toInt()

    /** light theme `mutedForeground` (0xFF52525B)。 */
    const val DEFAULT_FOREGROUND_ARGB: Int = 0xFF52525B.toInt()
}
