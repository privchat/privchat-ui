package com.netonstream.privchat.ui.components

import androidx.compose.runtime.Composable
import com.gearui.primitives.Avatar
import com.gearui.foundation.AvatarSpecs
import com.gearui.theme.Theme
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

/**
 * 聊天头像组件
 *
 * 封装 GearUI Avatar，添加聊天特定功能：
 * - 未读消息角标（仅在 unreadCount > 0 时显示）
 * - 免打扰状态显示小红点
 *
 * @param url 头像 URL
 * @param name 名称（用于显示首字母）
 * @param size 头像大小
 * @param unreadCount 未读消息数
 * @param isMuted 是否免打扰
 * @param isOnline 是否在线
 * @param modifier Modifier
 */
@Composable
fun ChatAvatar(
    url: String?,
    name: String,
    size: Dp = AvatarSpecs.Size.medium,
    unreadCount: Int = 0,
    isMuted: Boolean = false,
    isOnline: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = Theme.colors
    val normalizedName = name.trim()
    val avatarText = normalizedName
        .firstOrNull { !it.isDigit() }
        ?.uppercaseChar()
        ?.toString()
        ?: normalizedName.lastOrNull()?.toString()?.uppercase()
        ?: "?"

    Box(modifier = modifier) {
        Avatar(
            // image 参数需要 Painter 类型，暂时使用 text 显示
            text = avatarText,
            size = size,
            // 方形圆角头像
            radius = AvatarSpecs.squareRadius,
            // 未读消息数：免打扰时不显示数字，正常显示数字，0 时不显示
            badgeCount = if (isMuted || unreadCount <= 0) null else unreadCount,
            // 小红点：免打扰有未读时显示
            badgeDot = isMuted && unreadCount > 0,
        )

        if (isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(if (size <= AvatarSpecs.Size.small) 9.dp else 10.dp)
                    .clip(CircleShape)
                    .background(colors.onlineStatus)
                    .border(2.dp, colors.surface, CircleShape)
            )
        }
    }
}

/**
 * 群头像组件
 *
 * 简化版本，直接使用单个头像
 *
 * @param url 群头像 URL
 * @param name 群名称
 * @param size 头像大小
 * @param unreadCount 未读消息数
 * @param isMuted 是否免打扰
 * @param modifier Modifier
 */
@Composable
fun GroupAvatar(
    url: String?,
    name: String,
    size: Dp = AvatarSpecs.Size.medium,
    unreadCount: Int = 0,
    isMuted: Boolean = false,
    isOnline: Boolean = false,
    modifier: Modifier = Modifier,
) {
    ChatAvatar(
        url = url,
        name = name,
        size = size,
        unreadCount = unreadCount,
        isMuted = isMuted,
        isOnline = isOnline,
        modifier = modifier,
    )
}
