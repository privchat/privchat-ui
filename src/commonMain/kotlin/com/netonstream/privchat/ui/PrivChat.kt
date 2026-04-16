package com.netonstream.privchat.ui

import om.netonstream.privchat.sdk.PrivchatClient
import om.netonstream.privchat.sdk.ConnectionState
import om.netonstream.privchat.sdk.dto.*
import com.netonstream.privchat.ui.models.ChannelLocalState
import com.netonstream.privchat.ui.models.UIState
import com.netonstream.privchat.ui.common.base.currentTimeMillis
import com.netonstream.privchat.ui.common.base.systemDefaultTimeZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * PrivChat UI 入口
 *
 * 极简设计：
 * - 直接使用 SDK 数据类型，零转换
 * - 通过 StateFlow 暴露数据，UI 层响应式更新
 * - 本地状态独立管理（草稿、播放状态等）
 *
 * ## 使用方式
 * ```kotlin
 * // 1. 注入 SDK 客户端
 * PrivChat.init(client)
 *
 * // 2. UI 层订阅数据
 * val channels by PrivChat.channels.collectAsState()
 *
 * // 3. 通过 client 操作
 * PrivChat.client.sendText(channelId, channelType, "hello")
 * ```
 */
object PrivChat {

    const val VERSION = "0.2.0"

    // SDK 版本信息（从 FFI 获取，初始化时设置）
    private var _sdkVersion: String = ""
    private var _sdkGitSha: String = ""
    private var _sdkBuildTime: String = ""

    /** Rust SDK 版本 */
    val sdkVersion: String get() = _sdkVersion

    /** Rust SDK Git Commit SHA */
    val sdkGitSha: String get() = _sdkGitSha

    /** Rust SDK 构建时间 */
    val sdkBuildTime: String get() = _sdkBuildTime

    /** 设置 SDK 版本信息（由业务层在初始化时调用） */
    fun setSdkVersionInfo(version: String, gitSha: String, buildTime: String) {
        _sdkVersion = version
        _sdkGitSha = gitSha
        _sdkBuildTime = buildTime
    }

    // ========== 时区配置 ==========

    /**
     * 显示时区 ID（IANA 格式，如 "Asia/Shanghai"、"Asia/Ho_Chi_Minh"）
     *
     * - 仅影响 UI 展示层（Formatter）的时间格式化
     * - UTC 时间戳在数据层始终保持原样，不做任何转换
     * - 默认值：设备系统时区
     *
     * App 层可在初始化时或用户切换语言/时区时调用 setTimeZone() 修改
     */
    private var _timeZoneId: String = systemDefaultTimeZoneId()

    /** 当前显示时区 ID */
    val timeZoneId: String get() = _timeZoneId

    /**
     * 设置显示时区
     *
     * @param zoneId IANA 时区 ID，如 "Asia/Shanghai"、"Asia/Ho_Chi_Minh"、"America/New_York"
     */
    fun setTimeZone(zoneId: String) {
        _timeZoneId = zoneId
    }

    /**
     * 重置为设备系统时区
     */
    fun resetTimeZone() {
        _timeZoneId = systemDefaultTimeZoneId()
    }

    // ========== SDK 客户端 ==========

    private var _client: PrivchatClient? = null

    /** SDK 客户端（需要业务层注入） */
    val client: PrivchatClient
        get() = _client ?: error("PrivChat not initialized. Call PrivChat.init(client) first.")

    /** 是否已初始化 */
    val isInitialized: Boolean
        get() = _client != null

    // ========== 连接状态 ==========

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _currentUserId = MutableStateFlow<ULong?>(null)
    val currentUserId: StateFlow<ULong?> = _currentUserId.asStateFlow()

    private val _currentUserName = MutableStateFlow<String?>(null)
    val currentUserName: StateFlow<String?> = _currentUserName.asStateFlow()

    // ========== 频道/会话数据（直接用 SDK 类型） ==========

    private val _channels = MutableStateFlow<List<ChannelListEntry>>(emptyList())

    private data class ChannelUnreadWatermark(
        val latestTs: ULong,
        val eventType: String?,
        val content: String?,
    )

    /**
     * 本地“已读清零”水位。
     * key = channelId, value = 执行 clear 时该会话可见的 latest event 快照
     *
     * 用于抵抗会话列表异步刷新时旧 unread 回弹：
     * - 若 SDK 返回的会话 latest event 仍与该水位一致，视为旧快照，保持 unread=0
     * - 若 latest event 变化，则恢复服务端 unread 投影
     */
    private val channelUnreadClearWatermark = mutableMapOf<ULong, ChannelUnreadWatermark>()

    /** 频道列表 */
    val channels: StateFlow<List<ChannelListEntry>> = _channels.asStateFlow()

    private val _currentChannelId = MutableStateFlow<ULong?>(null)

    /** 当前打开的频道 ID */
    val currentChannelId: StateFlow<ULong?> = _currentChannelId.asStateFlow()

    // ========== 消息数据（直接用 SDK 类型） ==========

    private val _messages = MutableStateFlow<List<MessageEntry>>(emptyList())
    private val _messagesByChannel = MutableStateFlow<Map<ULong, List<MessageEntry>>>(emptyMap())

    /** 当前频道的消息列表 */
    val messages: StateFlow<List<MessageEntry>> = _messages.asStateFlow()

    // ========== 好友数据（直接用 SDK 类型） ==========

    private val _friends = MutableStateFlow<List<FriendEntry>>(emptyList())

    /** 好友列表 */
    val friends: StateFlow<List<FriendEntry>> = _friends.asStateFlow()

    private val _friendRequests = MutableStateFlow<List<FriendPendingEntry>>(emptyList())

    /** 好友申请列表 */
    val friendRequests: StateFlow<List<FriendPendingEntry>> = _friendRequests.asStateFlow()

    // ========== 群组数据（直接用 SDK 类型） ==========

    private val _groups = MutableStateFlow<List<GroupEntry>>(emptyList())

    /** 群组列表 */
    val groups: StateFlow<List<GroupEntry>> = _groups.asStateFlow()

    private val _groupMembers = MutableStateFlow<List<GroupMemberEntry>>(emptyList())

    /** 当前群的成员列表 */
    val groupMembers: StateFlow<List<GroupMemberEntry>> = _groupMembers.asStateFlow()

    // ========== 对端已读水位（per-channel） ==========

    private val _peerReadPtsByChannel = MutableStateFlow<Map<ULong, ULong>>(emptyMap())

    /** 对端已读 pts 水位（key = channelId） */
    val peerReadPtsByChannel: StateFlow<Map<ULong, ULong>> = _peerReadPtsByChannel.asStateFlow()

    /** 更新对端已读水位（单调递增，取 max） */
    fun updatePeerReadPts(channelId: ULong, pts: ULong) {
        val current = _peerReadPtsByChannel.value
        val old = current[channelId] ?: 0uL
        if (pts > old) {
            _peerReadPtsByChannel.value = current.toMutableMap().apply { this[channelId] = pts }
        }
    }

    /** 获取指定频道的对端已读 pts */
    fun peerReadPts(channelId: ULong): ULong? {
        return _peerReadPtsByChannel.value[channelId]
    }

    // ========== 消息送达状态 patch ==========

    /**
     * 标记指定消息为已送达。
     * 同时 patch _messagesByChannel 和当前 _messages（如果是当前会话）。
     */
    fun markMessageDelivered(channelId: ULong, serverMessageId: ULong) {
        val currentByChannel = _messagesByChannel.value
        val channelMessages = currentByChannel[channelId] ?: return

        var patched = false
        val updated = channelMessages.map { msg ->
            if (msg.serverMessageId == serverMessageId && !msg.delivered) {
                patched = true
                msg.copy(delivered = true)
            } else {
                msg
            }
        }
        if (!patched) return

        _messagesByChannel.value = currentByChannel.toMutableMap().apply {
            this[channelId] = updated
        }
        if (_currentChannelId.value == channelId) {
            _messages.value = updated
        }
    }

    // ========== 本地状态（UI 层独有） ==========

    private val _channelLocalStates = MutableStateFlow<Map<ULong, ChannelLocalState>>(emptyMap())

    /** 频道本地状态（草稿等） */
    val channelLocalStates: StateFlow<Map<ULong, ChannelLocalState>> = _channelLocalStates.asStateFlow()

    private val _uiState = MutableStateFlow(UIState())

    /** UI 全局状态 */
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()

    // ========== 初始化 ==========

    /**
     * 初始化 PrivChat
     *
     * @param client SDK 客户端实例
     */
    fun init(client: PrivchatClient) {
        _client = client
    }

    /**
     * 重置所有状态
     */
    fun reset() {
        _connectionState.value = ConnectionState.Disconnected
        _currentUserId.value = null
        _currentUserName.value = null
        _channels.value = emptyList()
        channelUnreadClearWatermark.clear()
        _currentChannelId.value = null
        _messages.value = emptyList()
        _messagesByChannel.value = emptyMap()
        _friends.value = emptyList()
        _friendRequests.value = emptyList()
        _groups.value = emptyList()
        _groupMembers.value = emptyList()
        _presences.value = emptyMap()
        _peerReadPtsByChannel.value = emptyMap()
        _channelLocalStates.value = emptyMap()
        _uiState.value = UIState()
        _typingUsers.value = emptyMap()
    }

    // ========== 状态更新方法 ==========

    /** 更新连接状态 */
    fun updateConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }

    /** 更新当前用户 ID */
    fun updateCurrentUserId(userId: ULong?) {
        _currentUserId.value = userId
    }

    /** 更新当前用户名 */
    fun updateCurrentUserName(name: String?) {
        _currentUserName.value = name
    }

    /** 更新频道列表 */
    fun updateChannels(list: List<ChannelListEntry>) {
        val previousByChannelId = _channels.value.associateBy { it.channelId }
        val currentChannel = _currentChannelId.value
        val normalized = list.map { channel ->
            var next = channel
            val previous = previousByChannelId[next.channelId]
            if (isReadCursorPreviewNoise(next)) {
                // 读游标同步事件不应作为“最后一条消息”展示，同时清理未读计数。
                next = previous?.let {
                    next.copy(
                        lastTs = it.lastTs,
                        latestEvent = it.latestEvent,
                    )
                } ?: next.copy(
                    latestEvent = next.latestEvent?.copy(content = ""),
                )
                next = forceUnreadCleared(next)
            }
            val latestTs = next.latestEvent?.timestamp ?: next.lastTs
            val watermark = channelUnreadClearWatermark[next.channelId]
            val isCurrentChannel = currentChannel != null && currentChannel == next.channelId
            if (watermark != null) {
                if (latestTs < watermark.latestTs || isCurrentChannel || isSameWatermarkEvent(next, watermark)) {
                    next = forceUnreadCleared(next)
                } else {
                    channelUnreadClearWatermark.remove(next.channelId)
                }
            } else if (isCurrentChannel) {
                next = forceUnreadCleared(next)
            }
            next
        }
        _channels.value = normalized.sortedWith(
            compareByDescending<ChannelListEntry> { it.isFavourite }
                .thenByDescending { it.latestEvent?.timestamp ?: it.lastTs }
        )
    }

    /** 添加或更新单个频道（保持 watermark / 当前会话已读状态） */
    fun upsertChannel(channel: ChannelListEntry) {
        val current = _channels.value.toMutableList()
        val index = current.indexOfFirst { it.channelId == channel.channelId }
        var next = channel
        // 与 updateChannels 保持一致：对 read-cursor 噪声和 watermark 做相同处理
        if (isReadCursorPreviewNoise(next)) {
            val previous = if (index >= 0) current[index] else null
            next = previous?.let {
                next.copy(lastTs = it.lastTs, latestEvent = it.latestEvent)
            } ?: next.copy(latestEvent = next.latestEvent?.copy(content = ""))
            next = forceUnreadCleared(next)
        }
        val latestTs = next.latestEvent?.timestamp ?: next.lastTs
        val watermark = channelUnreadClearWatermark[next.channelId]
        val isCurrentChannel = _currentChannelId.value != null && _currentChannelId.value == next.channelId
        if (watermark != null) {
            if (latestTs < watermark.latestTs || isCurrentChannel || isSameWatermarkEvent(next, watermark)) {
                next = forceUnreadCleared(next)
            } else {
                channelUnreadClearWatermark.remove(next.channelId)
            }
        } else if (isCurrentChannel) {
            next = forceUnreadCleared(next)
        }
        if (index >= 0) {
            current[index] = next
        } else {
            current.add(0, next)
        }
        _channels.value = current.sortedWith(
            compareByDescending<ChannelListEntry> { it.isFavourite }
                .thenByDescending { it.latestEvent?.timestamp ?: it.lastTs }
        )
    }

    /** 本地更新会话预览（发送消息后立即反映到会话列表） */
    fun touchChannelLatestMessage(
        channelId: ULong,
        content: String,
        timestamp: ULong,
    ) {
        val current = _channels.value.toMutableList()
        val index = current.indexOfFirst { it.channelId == channelId }
        if (index < 0) return
        val channel = current[index]
        current[index] = channel.copy(
            lastTs = timestamp,
            latestEvent = LatestChannelEvent(
                eventType = "message",
                content = content,
                timestamp = timestamp
            )
        )
        _channels.value = current.sortedWith(
            compareByDescending<ChannelListEntry> { it.isFavourite }
                .thenByDescending { it.latestEvent?.timestamp ?: it.lastTs }
        )
    }

    /** 本地清理会话未读数（进入会话时立即生效） */
    fun clearChannelUnread(channelId: ULong) {
        val channels = _channels.value.toMutableList()
        val index = channels.indexOfFirst { it.channelId == channelId }
        if (index < 0) return
        val channel = channels[index]
        channelUnreadClearWatermark[channelId] = buildUnreadWatermark(channel)
        if (
            channel.notifications == 0u &&
            channel.messages == 0u &&
            channel.mentions == 0u &&
            !channel.markedUnread
        ) {
            return
        }
        channels[index] = forceUnreadCleared(channel)
        _channels.value = channels
    }

    private fun buildUnreadWatermark(channel: ChannelListEntry): ChannelUnreadWatermark {
        val event = channel.latestEvent
        return ChannelUnreadWatermark(
            latestTs = event?.timestamp ?: channel.lastTs,
            eventType = event?.eventType?.trim()?.lowercase(),
            content = event?.content?.trim(),
        )
    }

    private fun isSameWatermarkEvent(
        channel: ChannelListEntry,
        watermark: ChannelUnreadWatermark,
    ): Boolean {
        val event = channel.latestEvent
        val latestTs = event?.timestamp ?: channel.lastTs
        if (latestTs != watermark.latestTs) {
            return false
        }
        val eventType = event?.eventType?.trim()?.lowercase()
        val content = event?.content?.trim()
        return eventType == watermark.eventType && content == watermark.content
    }

    private fun forceUnreadCleared(channel: ChannelListEntry): ChannelListEntry {
        return channel.copy(
            notifications = 0u,
            messages = 0u,
            mentions = 0u,
            markedUnread = false
        )
    }

    private fun isReadCursorPreviewNoise(channel: ChannelListEntry): Boolean {
        val eventType = channel.latestEvent?.eventType?.lowercase()?.trim().orEmpty()
        if (eventType == "channel_read_cursor_updated" || eventType == "channel_read_cursor") {
            return true
        }
        val content = channel.latestEvent?.content?.lowercase()?.trim().orEmpty()
        if (content.isBlank()) return false
        return content == "[channel read cursor updated]" ||
            content == "channel read cursor updated" ||
            content.contains("channel read cursor updated")
    }

    /** 隐藏频道：从 UI 列表移除并清除消息缓存 */
    fun removeChannel(channelId: ULong) {
        _channels.value = _channels.value.filter { it.channelId != channelId }
        _messagesByChannel.value = _messagesByChannel.value.toMutableMap().apply { remove(channelId) }
        if (_currentChannelId.value == channelId) {
            _currentChannelId.value = null
            _messages.value = emptyList()
        }
    }

    /** 设置当前频道 */
    fun setCurrentChannel(channelId: ULong?) {
        _currentChannelId.value = channelId
        if (channelId == null) {
            _messages.value = emptyList()
        } else {
            _messages.value = _messagesByChannel.value[channelId].orEmpty()
        }
    }

    /** 读取某个频道当前缓存的消息（不切换当前会话）。 */
    fun cachedMessages(channelId: ULong): List<MessageEntry> {
        return _messagesByChannel.value[channelId].orEmpty()
    }

    /** 更新指定频道的消息列表（与已有缓存合并，避免覆盖乐观插入的消息） */
    fun updateMessages(channelId: ULong, list: List<MessageEntry>) {
        val existing = _messagesByChannel.value[channelId].orEmpty()
        val merged = normalizeMessages(existing + list)
        _messagesByChannel.value = _messagesByChannel.value.toMutableMap().apply {
            this[channelId] = merged
        }
        if (_currentChannelId.value == channelId) {
            _messages.value = merged
        }
    }

    /** 预热某个会话的消息缓存，不切换当前会话。与已有缓存合并，避免覆盖乐观插入的消息。 */
    fun primeChannelMessages(channelId: ULong, list: List<MessageEntry>) {
        val existing = _messagesByChannel.value[channelId].orEmpty()
        val merged = normalizeMessages(existing + list)
        _messagesByChannel.value = _messagesByChannel.value.toMutableMap().apply {
            this[channelId] = merged
        }
        if (_currentChannelId.value == channelId) {
            _messages.value = merged
        }
    }

    /** 添加新消息 */
    fun addMessage(message: MessageEntry) {
        val normalized = normalizeMessages(_messages.value + message)
        _messages.value = normalized
        _currentChannelId.value?.let { channelId ->
            _messagesByChannel.value = _messagesByChannel.value.toMutableMap().apply {
                this[channelId] = normalized
            }
        }
    }

    /** 添加多条消息（用于加载历史） */
    fun prependMessages(list: List<MessageEntry>) {
        val normalized = normalizeMessages(list + _messages.value)
        _messages.value = normalized
        _currentChannelId.value?.let { channelId ->
            _messagesByChannel.value = _messagesByChannel.value.toMutableMap().apply {
                this[channelId] = normalized
            }
        }
    }

    /** 更新单条消息 */
    fun updateMessage(message: MessageEntry) {
        upsertChannelMessage(message)
    }

    /**
     * 按消息所属会话更新消息缓存（RX 主路径）。
     * - 永远写入 messagesByChannel[channelId]
     * - 仅当该频道是当前会话时，刷新当前 messages 列表
     */
    fun upsertChannelMessage(message: MessageEntry) {
        val channelId = message.channelId

        // ★ 消息强收敛：任意远端真实消息一旦进入会话显示模型，立即清除发送者 typing
        //   同时记录 suppressUntil 时间戳，3 秒内忽略迟到 typing 事件
        val nowMs = currentTimeMillis()
        val currentUserId = _currentUserId.value
        if (currentUserId != null && message.fromUid != currentUserId) {
            suppressTypingFor(channelId, message.fromUid, suppressUntilMs = nowMs + 3_000L)
        }

        val currentByChannel = _messagesByChannel.value.toMutableMap()
        val channelMessages = currentByChannel[channelId].orEmpty().toMutableList()

        // 如果新消息携带 localMessageId，先移除用 localMessageId 作为 id 的占位消息
        val localMsgId = message.localMessageId
        if (localMsgId != null && localMsgId > 0uL && message.id != localMsgId) {
            val placeholderIndex = channelMessages.indexOfFirst { it.id == localMsgId }
            if (placeholderIndex >= 0) {
                channelMessages.removeAt(placeholderIndex)
            }
        }

        val index = channelMessages.indexOfFirst { it.id == message.id }
        if (index >= 0) {
            channelMessages[index] = choosePreferredMessage(channelMessages[index], message)
        } else {
            channelMessages.add(message)
        }
        val normalized = normalizeMessages(channelMessages)
        currentByChannel[channelId] = normalized
        _messagesByChannel.value = currentByChannel
        if (_currentChannelId.value == channelId) {
            _messages.value = normalized
        }
    }

    /** 移除消息 */
    fun removeMessage(messageId: ULong) {
        val normalized = _messages.value.filter { it.id != messageId }
        _messages.value = normalized
        _currentChannelId.value?.let { channelId ->
            _messagesByChannel.value = _messagesByChannel.value.toMutableMap().apply {
                this[channelId] = normalized
            }
        }
    }

    private fun normalizeMessages(source: List<MessageEntry>): List<MessageEntry> {
        if (source.isEmpty()) return emptyList()
        val dedup = linkedMapOf<ULong, MessageEntry>()
        val serverIdToLocalId = mutableMapOf<ULong, ULong>()
        // localMessageId → dedup key: 用于追踪哪些占位消息（id == localMessageId）还在 dedup 中
        val localMsgIdToKey = mutableMapOf<ULong, ULong>()
        source.forEach { candidate ->
            // 1. 如果 candidate 携带 localMessageId 且 id != localMessageId，说明是真实消息，
            //    需要移除之前用 localMessageId 作为 id 的占位消息
            val localMsgId = candidate.localMessageId
            if (localMsgId != null && localMsgId > 0uL && candidate.id != localMsgId) {
                dedup.remove(localMsgId)
                localMsgIdToKey.remove(localMsgId)
            }

            // 2. serverMessageId 去重（原有逻辑）
            val serverId = candidate.serverMessageId
            if (serverId != null && serverId > 0uL) {
                val existedLocalId = serverIdToLocalId[serverId]
                if (existedLocalId != null && existedLocalId != candidate.id) {
                    val existed = dedup[existedLocalId]
                    if (existed != null) {
                        val preferred = choosePreferredMessage(existed, candidate)
                        dedup.remove(existedLocalId)
                        dedup[preferred.id] = preferred
                        serverIdToLocalId[serverId] = preferred.id
                        return@forEach
                    }
                } else {
                    serverIdToLocalId[serverId] = candidate.id
                }
            }

            // 3. id 去重
            val existing = dedup[candidate.id]
            dedup[candidate.id] = if (existing == null) candidate else choosePreferredMessage(existing, candidate)

            // 4. 记录占位消息（id == localMessageId 的消息）
            if (localMsgId != null && localMsgId > 0uL && candidate.id == localMsgId) {
                localMsgIdToKey[localMsgId] = candidate.id
            }
        }
        return dedup.values.sortedWith(
            compareBy<MessageEntry> { it.timestamp }
                .thenBy { it.serverMessageId ?: 0uL }
                .thenBy { it.id },
        )
    }

    private fun choosePreferredMessage(a: MessageEntry, b: MessageEntry): MessageEntry {
        if (a.id != b.id) return if (a.timestamp >= b.timestamp) a else b
        if (a.isRevoked != b.isRevoked) return if (b.isRevoked) b else a
        val statusA = statusRank(a.status)
        val statusB = statusRank(b.status)
        if (statusA != statusB) return if (statusA >= statusB) a else b
        if ((a.serverMessageId ?: 0uL) != (b.serverMessageId ?: 0uL)) {
            return if ((a.serverMessageId ?: 0uL) >= (b.serverMessageId ?: 0uL)) a else b
        }
        // When all criteria are equal, prefer the incoming message (b) —
        // it may carry updated fields such as localThumbnailPath / thumbStatus.
        return if (a.timestamp > b.timestamp) a else b
    }

    private fun statusRank(status: MessageStatus): Int = when (status) {
        MessageStatus.Pending -> 0
        MessageStatus.Sending -> 1
        MessageStatus.Failed -> 2
        MessageStatus.Sent -> 3
        MessageStatus.Read -> 4
    }

    /** 更新好友列表 */
    fun updateFriends(list: List<FriendEntry>) {
        _friends.value = list
    }

    /** 更新好友申请列表 */
    fun updateFriendRequests(list: List<FriendPendingEntry>) {
        _friendRequests.value = list
    }

    /** 更新群组列表 */
    fun updateGroups(list: List<GroupEntry>) {
        _groups.value = list
    }

    /** 更新群成员列表 */
    fun updateGroupMembers(list: List<GroupMemberEntry>) {
        _groupMembers.value = list
    }

    // ========== 本地状态管理 ==========

    /** 保存草稿 */
    fun saveDraft(channelId: ULong, text: String?, replyTo: ULong? = null) {
        val current = _channelLocalStates.value.toMutableMap()
        if (text.isNullOrBlank() && replyTo == null) {
            current.remove(channelId)
        } else {
            current[channelId] = ChannelLocalState(
                channelId = channelId,
                draftText = text,
                draftReplyTo = replyTo
            )
        }
        _channelLocalStates.value = current
    }

    /** 获取草稿 */
    fun getDraft(channelId: ULong): String? {
        return _channelLocalStates.value[channelId]?.draftText
    }

    /** 获取草稿回复的消息 ID */
    fun getDraftReplyTo(channelId: ULong): ULong? {
        return _channelLocalStates.value[channelId]?.draftReplyTo
    }

    /** 保存输入模式（语音/文字） */
    fun saveVoiceMode(channelId: ULong, voiceMode: Boolean) {
        val current = _channelLocalStates.value.toMutableMap()
        val existing = current[channelId]
        current[channelId] = (existing ?: ChannelLocalState(channelId = channelId)).copy(voiceMode = voiceMode)
        _channelLocalStates.value = current
    }

    /** 获取输入模式（语音/文字） */
    fun getVoiceMode(channelId: ULong): Boolean {
        return _channelLocalStates.value[channelId]?.voiceMode ?: false
    }

    /** 进入多选模式 */
    fun enterMultiSelectMode() {
        _uiState.value = _uiState.value.copy(
            isMultiSelectMode = true,
            selectedMessageIds = emptySet()
        )
    }

    /** 退出多选模式 */
    fun exitMultiSelectMode() {
        _uiState.value = _uiState.value.copy(
            isMultiSelectMode = false,
            selectedMessageIds = emptySet()
        )
    }

    /** 切换消息选中状态 */
    fun toggleMessageSelection(messageId: ULong) {
        val current = _uiState.value.selectedMessageIds.toMutableSet()
        if (current.contains(messageId)) {
            current.remove(messageId)
        } else {
            current.add(messageId)
        }
        _uiState.value = _uiState.value.copy(selectedMessageIds = current)
    }

    /** 进入搜索模式 */
    fun enterSearchMode() {
        _uiState.value = _uiState.value.copy(isSearchMode = true, searchKeyword = "")
    }

    /** 退出搜索模式 */
    fun exitSearchMode() {
        _uiState.value = _uiState.value.copy(isSearchMode = false, searchKeyword = "")
    }

    /** 更新搜索关键词 */
    fun updateSearchKeyword(keyword: String) {
        _uiState.value = _uiState.value.copy(searchKeyword = keyword)
    }

    // ========== 事件处理辅助方法 ==========

    /** 更新单个频道（别名） */
    fun updateChannel(channel: ChannelListEntry) = upsertChannel(channel)

    /** 添加好友 */
    fun addFriend(friend: FriendEntry) {
        val current = _friends.value.toMutableList()
        val index = current.indexOfFirst { it.userId == friend.userId }
        if (index >= 0) {
            current[index] = friend
        } else {
            current.add(friend)
        }
        _friends.value = current
    }

    /** 移除好友 */
    fun removeFriend(userId: ULong) {
        _friends.value = _friends.value.filter { it.userId != userId }
    }

    /** 添加好友申请 */
    fun addFriendRequest(request: FriendPendingEntry) {
        val current = _friendRequests.value.toMutableList()
        val index = current.indexOfFirst { it.fromUserId == request.fromUserId }
        if (index >= 0) {
            current[index] = request
        } else {
            current.add(0, request)
        }
        _friendRequests.value = current
    }

    /** 更新群组 */
    fun updateGroup(group: GroupEntry) {
        val current = _groups.value.toMutableList()
        val index = current.indexOfFirst { it.groupId == group.groupId }
        if (index >= 0) {
            current[index] = group
        } else {
            current.add(group)
        }
        _groups.value = current
    }

    /** 更新群成员 */
    fun updateGroupMember(member: GroupMemberEntry) {
        val current = _groupMembers.value.toMutableList()
        val index = current.indexOfFirst { it.userId == member.userId && it.channelId == member.channelId }
        if (index >= 0) {
            current[index] = member
        } else {
            current.add(member)
        }
        _groupMembers.value = current
    }

    // ========== 在线状态 ==========

    private val _presences = MutableStateFlow<Map<ULong, PresenceEntry>>(emptyMap())

    /** 用户在线状态 */
    val presences: StateFlow<Map<ULong, PresenceEntry>> = _presences.asStateFlow()

    /** 更新在线状态 */
    fun updatePresence(presence: PresenceEntry) {
        val current = _presences.value.toMutableMap()
        current[presence.userId] = presence
        _presences.value = current
    }

    /** 获取用户是否在线 */
    fun isUserOnline(userId: ULong): Boolean {
        return _presences.value[userId]?.isOnline == true
    }

    // ========== 输入状态（Typing） ==========

    /** typing 事件有效期（毫秒），超过此时间视为已停止输入 */
    private const val TYPING_EXPIRE_MS = 5_000L

    /**
     * 频道内正在输入的用户及其最后 typing 时间戳。
     * key = channelId, value = Map<userId, timestampMs>
     */
    private val _typingUsers = MutableStateFlow<Map<ULong, Map<ULong, Long>>>(emptyMap())

    /**
     * 收到对方消息后，suppress 该用户 typing 事件的截止时间。
     * key = channelId, value = Map<userId, suppressUntilMs>
     */
    private val _typingSuppressUntil = MutableStateFlow<Map<ULong, Map<ULong, Long>>>(emptyMap())

    /** 当前频道正在输入的用户 ID 列表（自动过滤过期 & suppress 条目） */
    val typingUserIds: StateFlow<Map<ULong, Map<ULong, Long>>> = _typingUsers.asStateFlow()

    /**
     * 标记某用户的 typing 为过时状态（消息到达时调用）。
     * 1. 立即清除该用户 typing 状态
     * 2. 设置 suppressUntil 截止时间，短窗口内忽略迟到 typing
     */
    fun suppressTypingFor(channelId: ULong, userId: ULong, suppressUntilMs: Long) {
        // 清除 typing 状态
        val current = _typingUsers.value.toMutableMap()
        val channelMap = current[channelId]?.toMutableMap()
        if (channelMap != null) {
            channelMap.remove(userId)
            if (channelMap.isEmpty()) current.remove(channelId) else current[channelId] = channelMap
            _typingUsers.value = current
        }
        // 设置 suppress 截止时间
        val suppress = _typingSuppressUntil.value.toMutableMap()
        val sMap = suppress[channelId]?.toMutableMap() ?: mutableMapOf()
        sMap[userId] = suppressUntilMs
        suppress[channelId] = sMap
        _typingSuppressUntil.value = suppress
    }

    /**
     * 收到远端 typing 事件时调用。
     * 若在 suppress 窗口内或已过期，直接忽略。
     */
    fun onRemoteTyping(channelId: ULong, userId: ULong) {
        val now = currentTimeMillis()

        // 检查 suppress 窗口
        val suppressUntil = _typingSuppressUntil.value[channelId]?.get(userId) ?: 0L
        if (now < suppressUntil) return

        // 检查过期
        val lastTypingAt = _typingUsers.value[channelId]?.get(userId) ?: 0L
        if (lastTypingAt > 0 && now - lastTypingAt >= TYPING_EXPIRE_MS) return

        // 接受
        val current = _typingUsers.value.toMutableMap()
        val cm = current[channelId]?.toMutableMap() ?: mutableMapOf()
        cm[userId] = now
        current[channelId] = cm
        _typingUsers.value = current
    }

    /**
     * 获取指定频道当前正在输入的用户列表（过滤已过期的）
     */
    fun activeTypingUsers(channelId: ULong): List<ULong> {
        val now = currentTimeMillis()
        val channelMap = _typingUsers.value[channelId] ?: return emptyList()
        return channelMap.entries
            .filter { now - it.value < TYPING_EXPIRE_MS }
            .map { it.key }
    }

    /**
     * 清除指定频道的 typing 状态（离开聊天页面时调用）
     */
    fun clearTyping(channelId: ULong) {
        val current = _typingUsers.value.toMutableMap()
        current.remove(channelId)
        _typingUsers.value = current
    }
}
