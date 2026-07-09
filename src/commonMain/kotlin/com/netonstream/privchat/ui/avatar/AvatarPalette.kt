package com.netonstream.privchat.ui.avatar

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 头像 fallback 调色板的**唯一**来源。
 *
 * 设计取向（2026-07 评审拍板）：fallback 头像配色改为 **per-identity hash 色**——
 * 深色底 + 白字，同一身份三端（Web / H5 / App）颜色一致。规则：
 * - seed：有 userId 用 `"u:<uid>"`（十进制），否则用显示名；群整体兜底 `"g:<channelId>"`
 * - FNV-1a 32bit over UTF-8 bytes → hue = h % 360 → HSL(hue, 0.62, 0.36)
 * - 校验点：h=0 → 约 RGB(149, 35, 35)
 *
 * 配色依旧**不跟随 light/dark theme**（bitmap 渲染产物与 app theme 解耦的硬不变式，spec §3）。
 * [DEFAULT_BACKGROUND_ARGB] / [DEFAULT_FOREGROUND_ARGB] 保留给 bitmap 管道
 * （QrPngRenderer 等）作为无 seed 时的兜底。
 *
 * Compose 端通过 [AvatarResolver] 转 `Color`；Bitmap 端直接用 ARGB int。
 */
object AvatarPalette {
    /** fallback 背景（浅灰；bitmap 管道无 seed 时兜底）。 */
    const val DEFAULT_BACKGROUND_ARGB: Int = 0xFFF4F4F5.toInt()

    /** fallback 前景（深灰；bitmap 管道无 seed 时兜底）。 */
    const val DEFAULT_FOREGROUND_ARGB: Int = 0xFF52525B.toInt()

    /** hash 色底上的前景固定白字。 */
    const val HASH_FOREGROUND_ARGB: Int = 0xFFFFFFFF.toInt()

    /** per-identity fallback 背景色：FNV-1a(seed) → HSL(hue, 0.62, 0.36)。 */
    fun hashBackgroundArgb(seed: String): Int {
        val hue = (fnv1a32(seed) % 360u).toFloat()
        return hslToArgb(hue, 0.62f, 0.36f)
    }

    /** FNV-1a 32bit（UTF-8 bytes），与 Web/H5 端实现逐字节一致。 */
    private fun fnv1a32(seed: String): UInt {
        var h = 0x811c9dc5u
        for (byte in seed.encodeToByteArray()) {
            h = h xor byte.toUByte().toUInt()
            h *= 0x01000193u
        }
        return h
    }

    /** 标准 HSL → ARGB（alpha 固定 0xFF）。h ∈ [0,360)，s / l ∈ [0,1]。 */
    private fun hslToArgb(h: Float, s: Float, l: Float): Int {
        val c = (1f - abs(2f * l - 1f)) * s
        val hp = h / 60f
        val x = c * (1f - abs(hp % 2f - 1f))
        val (r1, g1, b1) = when {
            hp < 1f -> Triple(c, x, 0f)
            hp < 2f -> Triple(x, c, 0f)
            hp < 3f -> Triple(0f, c, x)
            hp < 4f -> Triple(0f, x, c)
            hp < 5f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val m = l - c / 2f
        val r = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
