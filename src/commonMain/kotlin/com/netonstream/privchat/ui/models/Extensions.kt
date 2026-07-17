package com.netonstream.privchat.ui.models

import com.netonstream.privchat.sdk.dto.*
import com.netonstream.privchat.sdk.dto.MessageStatus
import com.netonstream.privchat.ui.i18n.PrivChatStrings

/**
 * SDK 类型扩展函数
 *
 * 为 SDK 数据结构添加 UI 层便利方法
 */

// ========== ChannelListEntry 扩展 ==========

/** 显示名称(DM 对端为系统用户时按语言本地化,判定见 [SystemUser]) */
val ChannelListEntry.displayName: String
    get() = SystemUser.channelTitle(this)

/** 是否有未读消息 */
val ChannelListEntry.hasUnread: Boolean
    get() = unreadCount > 0

/** 未读消息数（转为 Int 方便 UI 使用） */
val ChannelListEntry.unreadCount: Int
    get() = maxOf(messages.toInt(), notifications.toInt(), mentions.toInt())

/** 是否群聊 */
val ChannelListEntry.isGroup: Boolean
    get() = !isDm

/**
 * 最后消息**纯字符串预览**（兼容老调用方；不带 i18n 渲染能力）。
 *
 * **不推荐**新代码直接用这个。新代码应该用 [previewText]（在调用处传 PrivChatStrings）
 * 拿到本地化的预览文案——架构归正后 SDK 不再做"[图片]"等改写，preview 渲染 100%
 * 在 UI 层完成，参见 `SYSTEM_MESSAGE_SPEC` 与 `lastMessagePreviewLocalized`。
 *
 * 老路径只返回 SDK 已投影的 display text；调用方仍应优先使用本地化版本。
 */
@Deprecated(
    "Use ChannelListEntry.lastMessagePreviewLocalized(strings) to get i18n preview.",
    ReplaceWith("lastMessagePreviewLocalized(strings)"),
)
val ChannelListEntry.lastMessagePreview: String
    get() = latestEvent?.body?.text ?: ""

/**
 * 最后消息**本地化预览**（架构归正后的唯一推荐入口）。
 *
 * - 走 [PrivChatStrings] 字典（多语言），不再硬编码中文
 * - 系统消息按 `template + refs` 模板化渲染（spec/SYSTEM_MESSAGE_SPEC）
 * - 撤回 → `previewRecalled`；图片 → `previewImage`...
 * - **永不**返回原始 JSON
 *
 * 实现见 `PreviewRenderer.previewOf(ChannelListEntry)`。
 */
fun ChannelListEntry.lastMessagePreviewLocalized(strings: PrivChatStrings): String =
    strings.previewOf(this)

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

/** 显示名称（统一走 [UserDisplay.of]：备注 > 昵称 > username > uid；系统用户本地化）。 */
val FriendEntry.displayName: String
    get() = UserDisplay.of(
        username = username,
        nickname = nickname,
        remark = remark,
        userId = userId.toLong(),
        userType = userType.toInt(),
    )

/**
 * 头像首字母（用于无头像时显示）。
 * **不要在新代码里直接读此 property**——业务页面只调
 * [com.netonstream.privchat.ui.avatar.PrivChatAvatar]；这里保留供旧 call site 使用，
 * 内部统一走 [com.netonstream.privchat.ui.avatar.AvatarText.initialsOf]。
 */
val FriendEntry.avatarLetter: String
    get() = com.netonstream.privchat.ui.avatar.AvatarText.initialsOf(
        name = remark ?: nickname,
        username = username,
    )

// ========== UserEntry 扩展 ==========

/** 显示名称（统一走 [UserDisplay.of]；系统用户本地化）。 */
val UserEntry.displayName: String
    get() = UserDisplay.of(
        username = username,
        nickname = nickname,
        userId = userId.toLong(),
        userType = userType.toInt(),
    )

/** 头像首字母（同 [FriendEntry.avatarLetter] 说明）。 */
val UserEntry.avatarLetter: String
    get() = com.netonstream.privchat.ui.avatar.AvatarText.initialsOf(
        name = nickname,
        username = username,
    )

// ========== GroupEntry 扩展 ==========

/** 显示名称（P6-1：收口到 [GroupDisplay.titleOf]，与会话列表/群资料页同一 fallback 规则）。 */
val GroupEntry.displayName: String
    get() = GroupDisplay.titleOf(name)

/** 头像首字母（同 [FriendEntry.avatarLetter] 说明）。 */
val GroupEntry.avatarLetter: String
    get() = com.netonstream.privchat.ui.avatar.AvatarText.initialsOf(name = name)

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

/** 显示名称（统一走 [UserDisplay.of]：备注 > name(昵称) > uid）。 */
val GroupMemberEntry.displayName: String
    get() = UserDisplay.of(
        username = null,
        nickname = name,
        remark = remark,
        userId = userId.toLong(),
    )

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
