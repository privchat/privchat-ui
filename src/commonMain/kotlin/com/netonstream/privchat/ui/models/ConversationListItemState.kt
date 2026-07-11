package com.netonstream.privchat.ui.models

import com.netonstream.privchat.sdk.dto.ChannelListEntry
import com.netonstream.privchat.sdk.dto.PresenceEntry
import com.netonstream.privchat.ui.avatar.AvatarModel

/**
 * 会话列表项聚合 ViewState（CLIENT_GLOBAL_STATE_AND_IDENTITY_STORE_SPEC §7 P3.5）。
 *
 * 会话列表 UI 只消费本对象，不再各自：拼标题（系统用户本地化）、拼 peer 头像、判 presence。
 * - 标题走统一规则（[ChannelListEntry.displayName] → [SystemUser.channelTitle]，系统用户本地化）。
 * - DM peer 头像走统一 [AvatarModel]（带 userId → PrivChatAvatar 内部 local-first）；群头像走九宫格。
 * - presence 只读 [PrivChat.presences]（服务端真源，不另存）。
 * 预览文案仍由调用处的本地化 renderer 提供（`lastMessagePreviewLocalized`）。
 */
data class ConversationListItemState(
    val channelId: ULong,
    val channelType: Int,
    val isGroup: Boolean,
    val title: String,
    /** DM peer 头像 model（group 为 null，UI 用九宫格）。 */
    val avatar: AvatarModel?,
    val peerUserId: ULong?,
    val unreadCount: Int,
    val isMuted: Boolean,
    val isPinned: Boolean,
    val isOnline: Boolean,
    val lastMessageTime: ULong,
) {
    companion object {
        /**
         * [presences] 由调用处 `collectAsState()` 传入（Compose 可响应），保证在线态变化重组。
         * 标题走 [ChannelListEntry.displayName]（内部读 [SystemUser] 的 Compose State，系统用户本地化
         * 会在 resolveUid 异步就绪后重组刷新）——**故本函数不要缓存进 remember**，每次重组重算（很轻）。
         */
        fun from(
            entry: ChannelListEntry,
            presences: Map<ULong, PresenceEntry>,
        ): ConversationListItemState {
            val isGroup = entry.isGroup
            val peer = entry.peerUserId
            val online = peer?.let { presences[it]?.isOnline } == true
            val avatar = if (!isGroup && peer != null) {
                AvatarModel(
                    userId = peer.toLong(),
                    displayName = entry.displayName,
                    // localPath 交给 PrivChatAvatar 按 userId 内部 local-first 解析；这里只带 remote 作下载源。
                    remoteUrl = entry.avatarUrl,
                    seed = "u:$peer",
                )
            } else {
                null
            }
            return ConversationListItemState(
                channelId = entry.channelId,
                channelType = entry.channelType,
                isGroup = isGroup,
                title = entry.displayName,
                avatar = avatar,
                peerUserId = peer,
                unreadCount = entry.unreadCount,
                isMuted = entry.isMuted,
                isPinned = entry.isPinned,
                isOnline = online,
                lastMessageTime = entry.lastMessageTime,
            )
        }
    }
}
