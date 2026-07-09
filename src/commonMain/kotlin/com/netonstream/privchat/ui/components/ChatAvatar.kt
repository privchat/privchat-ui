package com.netonstream.privchat.ui.components

import androidx.compose.runtime.Composable
import com.gearui.foundation.avatar.AvatarSizeTokens
import com.netonstream.privchat.ui.avatar.PrivChatAvatar
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.Dp

/**
 * 历史名兼容层：保留以避免一次性改 11 个 call site。
 * **新代码请用 [PrivChatAvatar]**；这里只是 thin wrapper，不允许再加任何 initials / 配色逻辑。
 */
@Composable
fun ChatAvatar(
    url: String?,
    name: String,
    size: Dp = AvatarSizeTokens.Medium.size,
    unreadCount: Int = 0,
    isMuted: Boolean = false,
    isOnline: Boolean = false,
    seed: String? = null,
    userId: Long? = null,
    modifier: Modifier = Modifier,
) {
    PrivChatAvatar(
        name = name,
        avatarUrl = url,
        userId = userId,
        size = size,
        unreadCount = unreadCount,
        isMuted = isMuted,
        isOnline = isOnline,
        seed = seed,
        modifier = modifier,
    )
}

/**
 * 历史名兼容层：保留以避免一次性改群头像 call site。
 * **新代码请用 [PrivChatAvatar]** 并显式传 `isGroup = true`。
 */
@Composable
fun GroupAvatar(
    url: String?,
    name: String,
    size: Dp = AvatarSizeTokens.Medium.size,
    unreadCount: Int = 0,
    isMuted: Boolean = false,
    isOnline: Boolean = false,
    seed: String? = null,
    modifier: Modifier = Modifier,
) {
    PrivChatAvatar(
        name = name,
        avatarUrl = url,
        size = size,
        isGroup = true,
        unreadCount = unreadCount,
        isMuted = isMuted,
        isOnline = isOnline,
        seed = seed,
        modifier = modifier,
    )
}
