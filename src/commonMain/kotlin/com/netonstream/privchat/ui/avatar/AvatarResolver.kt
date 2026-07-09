package com.netonstream.privchat.ui.avatar

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ui.graphics.Color

/**
 * 头像渲染的展示态：所有业务页面只能通过 [PrivChatAvatar] 间接拿到这个结果，
 * 不允许在调用点自己算 initials / 配色。Bitmap 渲染端走 [AvatarText] +
 * [AvatarPalette]，跟这里来源同构。
 */
data class AvatarResolved(
    val displayName: String,
    val initials: String,
    val backgroundColor: Color,
    val foregroundColor: Color,
    val avatarUrl: String?,
    val isGroup: Boolean,
)

/**
 * 头像展示规则中心。
 *
 * - [name] 业务侧最常见的「显示名」（用户 nickname / 群名称），优先级最高
 * - [username] / [userId] 是兜底来源，避免出现「?」头像
 * - [avatarUrl] 透传给上层组件；[PrivChatAvatar] 非空时异步加载远程图覆盖 initials 色块，
 *   加载中 / 失败回退 initials
 * - [isGroup] 暂时只用于元信息标记；视觉差异留给后续阶段
 * - [seed] hash 色种子（三端统一：成员/DM 用 `"u:<uid>"`，群兜底 `"g:<channelId>"`）；
 *   不传时按 userId → 显示名依次兜底
 *
 * 配色走 [AvatarPalette] 的 per-identity hash 色，**不**跟 [com.gearui.theme.Theme] 联动。
 * 理由见 [AvatarPalette]。
 */
@Composable
fun rememberAvatarResolved(
    name: String?,
    username: String? = null,
    avatarUrl: String? = null,
    userId: Long? = null,
    isGroup: Boolean = false,
    seed: String? = null,
): AvatarResolved {
    val initials = AvatarText.initialsOf(name = name, username = username, fallbackId = userId)
    val displayName = name?.trim()?.takeIf { it.isNotEmpty() }
        ?: username?.trim()?.takeIf { it.isNotEmpty() }
        ?: userId?.toString()
        ?: ""
    // seed 优先级：调用方显式 seed > "u:<uid>" > 显示名（与 Web/H5 端规则一致）
    val hashSeed = seed?.takeIf { it.isNotEmpty() }
        ?: userId?.let { "u:$it" }
        ?: displayName
    return AvatarResolved(
        displayName = displayName,
        initials = initials,
        backgroundColor = Color(AvatarPalette.hashBackgroundArgb(hashSeed)),
        foregroundColor = Color(AvatarPalette.HASH_FOREGROUND_ARGB),
        avatarUrl = avatarUrl,
        isGroup = isGroup,
    )
}
