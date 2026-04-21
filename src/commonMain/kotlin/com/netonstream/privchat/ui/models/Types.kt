package com.netonstream.privchat.ui.models

import com.netonstream.privchat.sdk.dto.*
import com.netonstream.privchat.sdk.dto.MessageStatus

/**
 * SDK 类型别名
 *
 * 直接使用 SDK 数据结构，零转换
 */

// 频道/会话
typealias Channel = ChannelListEntry
typealias ChannelEvent = LatestChannelEvent

// 消息
typealias Message = MessageEntry
typealias Attachment = AttachmentInfo

// 用户/好友
typealias User = UserEntry
typealias Friend = FriendEntry
typealias FriendRequest = FriendPendingEntry

// 群组
typealias Group = GroupEntry
typealias GroupMember = GroupMemberEntry

// 状态
typealias MsgStatus = MessageStatus
