package com.netonstream.privchat.ui.models

/**
 * 本地状态
 *
 * SDK 不管理的 UI 层状态
 */

/**
 * 频道本地状态
 *
 * @param channelId 频道 ID
 * @param draftText 草稿文本
 * @param draftReplyTo 草稿回复的消息 ID
 * @param lastReadMessageId 本地记录的最后已读消息 ID
 */
data class ChannelLocalState(
    val channelId: ULong,
    val draftText: String? = null,
    val draftReplyTo: ULong? = null,
    val lastReadMessageId: ULong? = null,
    val voiceMode: Boolean = false,
)

/**
 * 消息本地状态
 *
 * @param messageId 消息 ID
 * @param isPlaying 是否正在播放（语音/视频）
 * @param playProgress 播放进度 0.0 ~ 1.0
 * @param downloadProgress 下载进度 0.0 ~ 1.0，null 表示未在下载
 * @param uploadProgress 上传进度 0.0 ~ 1.0，null 表示未在上传
 * @param isSelected 是否被选中（多选模式）
 */
data class MessageLocalState(
    val messageId: ULong,
    val isPlaying: Boolean = false,
    val playProgress: Float = 0f,
    val downloadProgress: Float? = null,
    val uploadProgress: Float? = null,
    val isSelected: Boolean = false,
)

/**
 * UI 全局状态
 *
 * @param isMultiSelectMode 是否处于多选模式
 * @param selectedMessageIds 已选中的消息 ID 列表
 * @param isSearchMode 是否处于搜索模式
 * @param searchKeyword 搜索关键词
 */
data class UIState(
    val isMultiSelectMode: Boolean = false,
    val selectedMessageIds: Set<ULong> = emptySet(),
    val isSearchMode: Boolean = false,
    val searchKeyword: String = "",
)
