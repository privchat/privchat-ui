package com.netonstream.privchat.ui.avatar

import androidx.compose.runtime.Composable
import com.gearui.foundation.avatar.AvatarSizeTokens
import com.gearui.primitives.Avatar
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.onlineStatus
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.gearui.theme.Theme

/**
 * PrivChat 业务**唯一**头像入口。所有业务页面（消息、联系人、个人资料、群、二维码…）只用这个。
 *
 * 直接调 [com.gearui.primitives.Avatar] 被视为分裂规则——initials / 配色 / radius 三件套必须经过
 * [rememberAvatarResolved] 统一解析，bitmap 渲染（QR 中心头像）通过 [AvatarText] +
 * [AvatarPalette] 共享同一来源。
 *
 * @param name 用户/群的显示名（nickname / groupName 最常用）
 * @param username 兜底来源；name 为空时使用
 * @param avatarUrl Phase 1 **暂未启用远程图加载**（透传以便 call site 数据模型不动）
 * @param userId 最末兜底来源（取末 2 位）
 * @param size 头像尺寸；默认 [AvatarSizeTokens.Medium]
 * @param radius 圆角（默认 6dp 方圆角，跟既有 [ChatAvatar] 视觉一致）；群头像传同值不区分
 * @param isGroup 元信息标记；视觉差异留给下阶段
 * @param unreadCount 未读 badge 数量（旧 [ChatAvatar] 用法保留）
 * @param isMuted 免打扰时不显示数字、只显示小红点（旧 [ChatAvatar] 用法保留）
 * @param isOnline 在线小绿点（旧 [ChatAvatar] 用法保留）
 * @param seed hash 色种子（`"u:<uid>"` / `"g:<channelId>"`）；不传时由 resolver 按 userId/名字兜底
 */
@Composable
fun PrivChatAvatar(
    name: String?,
    username: String? = null,
    avatarUrl: String? = null,
    userId: Long? = null,
    size: Dp = AvatarSizeTokens.Medium.size,
    radius: Dp = 6.dp,
    isGroup: Boolean = false,
    unreadCount: Int = 0,
    isMuted: Boolean = false,
    isOnline: Boolean = false,
    seed: String? = null,
    modifier: Modifier = Modifier,
) {
    val resolved = rememberAvatarResolved(
        name = name,
        username = username,
        avatarUrl = avatarUrl,
        userId = userId,
        isGroup = isGroup,
        seed = seed,
    )
    val colors = Theme.colors
    // 强制把 fallback 配色固定到 [AvatarPalette]，不让 gearui Avatar 回退到 Theme.colors.muted——
    // 否则 dark theme 下「我」tab 头像会变暗色，而 QR bitmap 头像永远是 light 色（保存到相册的
    // 图片跟 app theme 解耦），两条管道视觉就割裂了。
    Box(modifier = modifier) {
        Avatar(
            text = resolved.initials,
            size = size,
            radius = radius,
            backgroundColor = resolved.backgroundColor,
            contentColor = resolved.foregroundColor,
            badgeCount = if (isMuted || unreadCount <= 0) null else unreadCount,
            badgeDot = isMuted && unreadCount > 0,
        )
        if (isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(if (size <= AvatarSizeTokens.Small.size) 9.dp else 10.dp)
                    .clip(CircleShape)
                    .background(colors.onlineStatus)
                    .border(2.dp, colors.surface, CircleShape),
            )
        }
    }
}
