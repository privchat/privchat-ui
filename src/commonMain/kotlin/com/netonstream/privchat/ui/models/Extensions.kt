package com.netonstream.privchat.ui.models

import com.netonstream.privchat.sdk.dto.*
import com.netonstream.privchat.sdk.dto.MessageStatus

/**
 * SDK 类型扩展函数
 *
 * 为 SDK 数据结构添加 UI 层便利方法
 */

// ========== ChannelListEntry 扩展 ==========

/** 显示名称 */
val ChannelListEntry.displayName: String
    get() = name

/** 是否有未读消息 */
val ChannelListEntry.hasUnread: Boolean
    get() = unreadCount > 0

/** 未读消息数（转为 Int 方便 UI 使用） */
val ChannelListEntry.unreadCount: Int
    get() = maxOf(messages.toInt(), notifications.toInt(), mentions.toInt())

/** 是否群聊 */
val ChannelListEntry.isGroup: Boolean
    get() = !isDm

/** 最后消息预览文本 */
val ChannelListEntry.lastMessagePreview: String
    get() = latestEvent?.content ?: ""

/** 最后消息时间戳 */
val ChannelListEntry.lastMessageTime: ULong
    get() = latestEvent?.timestamp ?: lastTs

/** 是否置顶（isFavourite 的别名） */
val ChannelListEntry.isPinned: Boolean
    get() = isFavourite

/** 是否免打扰（isLowPriority 的别名） */
val ChannelListEntry.isMuted: Boolean
    get() = isLowPriority

// ========== MessageEntry 扩展 ==========

/** 是否自己发送的消息 */
fun MessageEntry.isSelf(currentUserId: ULong): Boolean = fromUid == currentUserId

/** 是否发送中 */
val MessageEntry.isSending: Boolean
    get() = status == MessageStatus.Pending || status == MessageStatus.Sending

/** 是否发送失败 */
val MessageEntry.isFailed: Boolean
    get() = status == MessageStatus.Failed

/** 是否已读 */
val MessageEntry.isRead: Boolean
    get() = status == MessageStatus.Read

/** 是否已发送成功 */
val MessageEntry.isSent: Boolean
    get() = status == MessageStatus.Sent || status == MessageStatus.Read

/** 消息时间戳（转为 Long） */
val MessageEntry.time: Long
    get() = timestamp.toLong()

// ========== FriendEntry 扩展 ==========

/** 显示名称（备注优先，其次昵称，最后用户名） */
val FriendEntry.displayName: String
    get() = remark?.takeIf { it.isNotBlank() }
        ?: nickname?.takeIf { it.isNotBlank() }
        ?: username

/** 头像首字母（用于无头像时显示） */
val FriendEntry.avatarLetter: String
    get() = displayName.firstOrNull()?.uppercase() ?: "?"

// ========== UserEntry 扩展 ==========

/** 显示名称 */
val UserEntry.displayName: String
    get() = nickname?.takeIf { it.isNotBlank() } ?: username

/** 头像首字母 */
val UserEntry.avatarLetter: String
    get() = displayName.firstOrNull()?.uppercase() ?: "?"

// ========== GroupEntry 扩展 ==========

/** 显示名称 */
val GroupEntry.displayName: String
    get() = name ?: "群聊"

/** 头像首字母 */
val GroupEntry.avatarLetter: String
    get() = displayName.firstOrNull()?.uppercase() ?: "G"

// ========== GroupMemberEntry 扩展 ==========

/** 是否群主 */
val GroupMemberEntry.isOwner: Boolean
    get() = role == 2

/** 是否管理员 */
val GroupMemberEntry.isAdmin: Boolean
    get() = role == 1

/** 是否普通成员 */
val GroupMemberEntry.isMember: Boolean
    get() = role == 0

/** 显示名称（备注优先） */
val GroupMemberEntry.displayName: String
    get() = remark.takeIf { it.isNotBlank() } ?: name

/** 角色名称 */
val GroupMemberEntry.roleName: String
    get() = when (role) {
        2 -> "群主"
        1 -> "管理员"
        else -> "成员"
    }

// ========== LatestChannelEvent 扩展 ==========

/** 事件时间（转为 Long） */
val LatestChannelEvent.time: Long
    get() = timestamp.toLong()

// ========== AttachmentInfo 扩展 ==========

/** 是否是图片 */
val AttachmentInfo.isImage: Boolean
    get() = mimeType.startsWith("image/")

/** 是否是视频 */
val AttachmentInfo.isVideo: Boolean
    get() = mimeType.startsWith("video/")

/** 是否是音频 */
val AttachmentInfo.isAudio: Boolean
    get() = mimeType.startsWith("audio/")

/** 文件大小（转为 Long） */
val AttachmentInfo.fileSize: Long
    get() = size.toLong()

/** 时长秒数（转为 Int） */
val AttachmentInfo.durationSeconds: Int
    get() = duration?.toInt() ?: 0
