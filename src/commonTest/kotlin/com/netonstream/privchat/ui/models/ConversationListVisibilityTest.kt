package com.netonstream.privchat.ui.models

import com.netonstream.privchat.sdk.dto.ChannelListEntry
import com.netonstream.privchat.sdk.dto.LatestChannelEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 新设备登录时频道实体先于聊天记录到达：此时列出会话，会冒出一排只有名字、没有预览的空行，
 * 角标还会算上这些看不见内容的会话的未读。会话列表与角标只认「本地已有聊天记录」的会话，
 * 补历史落库后它们再出现。
 */
class ConversationListVisibilityTest {

    private fun channel(
        id: ULong,
        unread: UInt = 0u,
        muted: Boolean = false,
        withHistory: Boolean = true,
    ) = ChannelListEntry(
        channelId = id,
        channelType = 1,
        name = "c$id",
        lastTs = 1_000uL,
        notifications = unread,
        messages = unread,
        mentions = 0u,
        markedUnread = unread > 0u,
        isFavourite = false,
        isLowPriority = muted,
        avatarUrl = null,
        isDm = true,
        isEncrypted = false,
        memberCount = 2u,
        topic = null,
        latestEvent = if (withHistory) {
            LatestChannelEvent(eventType = "message", content = "hi", timestamp = 1_000uL)
        } else {
            null
        },
    )

    @Test
    fun aConversationWithoutLocalMessagesIsNotListed() {
        assertFalse(channel(1u, withHistory = false).hasLocalHistory)
        assertFalse(channel(1u, withHistory = false).isListedInConversations(draftText = null))
    }

    @Test
    fun aConversationWithLocalMessagesIsListed() {
        assertTrue(channel(1u).hasLocalHistory)
        assertTrue(channel(1u).isListedInConversations(draftText = null))
    }

    @Test
    fun aDraftKeepsAnEmptyConversationListed() {
        // 用户在空会话里打了字又退出来：和微信一样，带草稿的会话要留在列表里。
        assertTrue(channel(1u, withHistory = false).isListedInConversations(draftText = "写到一半"))
        assertFalse(channel(1u, withHistory = false).isListedInConversations(draftText = "   "))
    }

    @Test
    fun badgeIgnoresUnreadOfConversationsNotYetHydrated() {
        // 服务端已同步了未读数、但本地还没拉到记录的会话：列表里看不到它，角标就不能算它，
        // 否则用户对着一个找不到来源的红点。补齐记录后它出现在列表里，角标随之计入。
        val state = badgeStateOf(
            listOf(
                channel(1u, unread = 3u),
                channel(2u, unread = 5u, withHistory = false),
                channel(3u, unread = 7u, muted = true),
            ),
            friendRequests = 0,
        )
        assertEquals(3, state.totalUnread)
    }
}
