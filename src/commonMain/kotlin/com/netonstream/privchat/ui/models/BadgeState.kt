package com.netonstream.privchat.ui.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.netonstream.privchat.sdk.dto.ChannelListEntry
import com.netonstream.privchat.ui.PrivChat

/**
 * 全局角标统一真源（P6-2，CLIENT_GLOBAL_STATE §24）。
 *
 * 收口此前 App 的**两套未读口径**（图标/会话 Tab 用 `notifications`-only vs 会话行 `maxOf` 三者）
 * 与**两套好友申请源**（旧 `friendRequests` isNotEmpty vs `receivedFriendRequests` status==0）。
 *
 * 口径（GPT 拍板）：
 * - conversationUnread = max(notifications, messages, mentions) = [ChannelListEntry.unreadCount]
 * - totalUnread = 所有**非静音**会话 conversationUnread 之和（静音会话不计入总数/图标角标）
 * - friendRequests = 收到的待处理好友申请数（唯一源 `receivedFriendRequests` 里 status==0）
 * - groupRequests/system/wallet：SDK 未暴露群审批 / 暂无通知角标 → 先 0（预留字段）
 *
 * 图标角标、底部会话 Tab、联系人红点全部读同一个 BadgeState，口径一致。
 */
data class BadgeState(
    val totalUnread: Int,
    val conversationUnread: Map<ULong, Int>,
    val friendRequests: Int,
    val groupRequests: Int = 0,
    val systemNotifications: Int = 0,
    val walletNotifications: Int = 0,
)

/** 纯函数派生（可测、无副作用）。totalUnread 排除静音会话，单会话未读用 maxOf 三计数。 */
fun badgeStateOf(channels: List<ChannelListEntry>, friendRequests: Int): BadgeState =
    BadgeState(
        totalUnread = channels.filterNot { it.isMuted }.sumOf { it.unreadCount },
        conversationUnread = channels.associate { it.channelId to it.unreadCount },
        friendRequests = friendRequests,
    )

/** Compose 统一入口：读 PrivChat.channels + receivedFriendRequests(status==0) → 单一 BadgeState。 */
@Composable
fun rememberBadgeState(): BadgeState {
    val channels by PrivChat.channels.collectAsState()
    val received by PrivChat.receivedFriendRequests.collectAsState()
    return badgeStateOf(channels, received.count { it.status.toInt() == 0 })
}
