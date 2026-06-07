package com.netonstream.privchat.ui.avatar

/**
 * 头像 fallback 调色板的**唯一**来源。
 *
 * 设计取向：fallback 头像配色**不跟随 light/dark theme**，永远是同一套（浅灰底 + 深灰字）。
 * 理由：
 * - bitmap 渲染（QR 中心头像）保存到相册后跟应用 theme 解耦，theme 切了图也不会变；
 *   如果 Compose 端跟着 theme 变色，dark theme 下两条路径就视觉割裂
 * - 业务头像在 Compose 与 bitmap 间必须一致是整个 avatar 包的硬不变式（spec §3）
 *
 * Compose 端通过 [AvatarResolver] 转 `Color`；Bitmap 端（QrPngRenderer 等）直接用 ARGB int。
 */
object AvatarPalette {
    /** fallback 背景（浅灰，Compose ARGB / Android `Color.rgb` 通用）。 */
    const val DEFAULT_BACKGROUND_ARGB: Int = 0xFFF4F4F5.toInt()

    /** fallback 前景（深灰）。 */
    const val DEFAULT_FOREGROUND_ARGB: Int = 0xFF52525B.toInt()
}
