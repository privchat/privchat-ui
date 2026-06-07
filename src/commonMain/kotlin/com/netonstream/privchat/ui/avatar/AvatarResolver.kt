package com.netonstream.privchat.ui.avatar

import androidx.compose.runtime.Composable
import com.gearui.theme.Theme
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
 * 头像展示规则中心。Compose-aware：会读 [Theme.colors]，所以必须在 `@Composable` 里调用。
 *
 * - [name] 为业务侧最常见的「显示名」（用户 nickname / 群名称），优先级最高
 * - [username] / [userId] 是兜底来源，避免出现「?」头像
 * - [avatarUrl] 透传给上层组件；**Phase 1 不接远程图加载**，调用方拿到也只是占位
 * - [isGroup] 暂时只用于元信息标记；视觉差异（边角、占位 icon）留给后续阶段
 */
@Composable
fun rememberAvatarResolved(
    name: String?,
    username: String? = null,
    avatarUrl: String? = null,
    userId: Long? = null,
    isGroup: Boolean = false,
): AvatarResolved {
    val initials = AvatarText.initialsOf(name = name, username = username, fallbackId = userId)
    val displayName = name?.trim()?.takeIf { it.isNotEmpty() }
        ?: username?.trim()?.takeIf { it.isNotEmpty() }
        ?: userId?.toString()
        ?: ""
    // 当前规则：所有 fallback 头像统一走 muted / mutedForeground。
    // 这两个 token 在 light / dark theme 下都有合适对比度（gearui Avatar 的默认配色一致），
    // bitmap 端的 AvatarPalette.DEFAULT_* 常量是 light 版本，跟这里来源同一份设计 token。
    val colors = Theme.colors
    return AvatarResolved(
        displayName = displayName,
        initials = initials,
        backgroundColor = colors.muted,
        foregroundColor = colors.mutedForeground,
        avatarUrl = avatarUrl,
        isGroup = isGroup,
    )
}
