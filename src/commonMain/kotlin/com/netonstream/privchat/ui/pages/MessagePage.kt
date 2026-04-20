package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import om.netonstream.privchat.sdk.ConnectionState
import om.netonstream.privchat.sdk.dto.ChannelListEntry
import om.netonstream.privchat.sdk.dto.MessageEntry
import om.netonstream.privchat.sdk.dto.MessageStatus
import om.netonstream.privchat.sdk.dto.PresenceEntry
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.models.*
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.components.MessageAction
import com.netonstream.privchat.ui.components.MessageActionsMenu
import com.netonstream.privchat.ui.components.MessageContent
import com.tencent.kuikly.compose.foundation.gestures.detectTapGestures
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.offlineStatus
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.onlineStatus
import com.netonstream.privchat.ui.utils.Formatter
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.theme.Theme
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.primitives.ScrollView
import com.gearui.foundation.typography.Typography
import com.gearui.foundation.AvatarSpecs
import com.gearui.primitives.HorizontalSpacer
import com.gearui.primitives.VerticalSpacer
import com.tencent.kuikly.compose.ui.unit.Dp
import com.gearui.primitives.composite.Card
import com.gearui.components.navbar.NavBar
import com.gearui.components.navbar.NavBarItem
import com.gearui.components.icon.Icons
import com.gearui.foundation.primitives.Icon
import com.gearui.components.input.Input
import com.gearui.components.input.InputSize
import com.gearui.components.textarea.AutoResizeTextarea
import com.gearui.components.button.Button
import com.gearui.components.button.ButtonTheme
import com.gearui.components.button.ButtonSize
import com.gearui.components.empty.EmptyState
import com.gearui.components.swiper.Swiper
import com.gearui.components.swiper.SwiperNavigation
import com.gearui.components.swiper.SwiperIndicatorPosition
import com.gearui.runtime.LocalGearRuntimeEnvironment
import com.tencent.kuikly.compose.ui.platform.LocalSoftwareKeyboardController
import com.tencent.kuikly.compose.ui.platform.LocalFocusManager
import com.tencent.kuikly.compose.ui.focus.FocusRequester
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.draw.alpha
import com.tencent.kuikly.compose.ui.zIndex
import com.tencent.kuikly.compose.ui.unit.dp
import com.netonstream.privchat.ui.common.base.currentTimeMillis
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.input.pointer.changedToUp
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.foundation.gestures.awaitEachGesture
import com.tencent.kuikly.compose.foundation.gestures.awaitFirstDown
import com.tencent.kuikly.compose.animation.core.rememberInfiniteTransition
import com.tencent.kuikly.compose.animation.core.animateFloat
import com.tencent.kuikly.compose.animation.core.infiniteRepeatable
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.animation.core.RepeatMode
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.ULong
import kotlin.math.PI
import kotlin.math.sin

private enum class InputPanelMode {
    NONE,
    EMOJI,
    PLUS,
}

private enum class VoiceRecordingState {
    IDLE,
    RECORDING,
    CANCEL_ZONE,
}

/** 录制时长不足此时长（ms）静默丢弃 */
private const val VOICE_MIN_DURATION_MS = 1000L

/** 最长录制时长（ms），超过自动发送 */
private const val VOICE_MAX_DURATION_MS = 60_000L

private fun logMessageInputBar(message: String) {
    println("[MessageInputBar] $message")
}

/**
 * 聊天页面
 *
 * 直接使用 SDK 的数据类型
 *
 * @param channel 频道信息（SDK 类型）
 * @param onBack 返回回调
 * @param onProfileClick 点击频道详情回调
 * @param modifier Modifier
 */
@Composable
fun MessagePage(
    channel: ChannelListEntry,
    onBack: () -> Unit,
    onProfileClick: () -> Unit = {},
    onAvatarClick: ((ULong) -> Unit)? = null,
    networkStatusBar: (@Composable () -> Unit)? = null,
    onLoadMessages: (suspend (ULong, Int) -> Result<List<MessageEntry>>)? = null,
    onMarkRead: (suspend (ULong, Int) -> Result<Unit>)? = null,
    onSendText: (suspend (ULong, Int, String) -> Result<ULong>)? = null,
    onSendImage: (suspend (ULong, Int) -> Result<ULong>)? = null, // 相册
    onSendCamera: (suspend (ULong, Int) -> Result<ULong>)? = null, // 相机
    onSendFile: (suspend (ULong, Int) -> Result<ULong>)? = null,
    onVoiceStart: (() -> Boolean)? = null,
    onVoiceCancel: (() -> Unit)? = null,
    onSendVoice: (suspend (ULong, Int, durationMs: Long) -> Result<ULong>)? = null,
    onError: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val messages by PrivChat.messages.collectAsState()
    val currentUserId by PrivChat.currentUserId.collectAsState()
    val presences by PrivChat.presences.collectAsState()
    val peerReadPtsMap by PrivChat.peerReadPtsByChannel.collectAsState()
    val peerReadPts = peerReadPtsMap[channel.channelId]
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val runtimeEnv = LocalGearRuntimeEnvironment.current
    val sortedMessages = messages
    // peer_user_id：优先从 channel 字段取，channel_member 表为空时回退到 dmPeerUserId()
    var peerUserId by remember(channel.channelId) { mutableStateOf(channel.peerUserId) }
    var initialPositioned by remember(channel.channelId) { mutableStateOf(false) }
    var hasInitialLoadCompleted by remember(channel.channelId) { mutableStateOf(false) }
    // 输入文本
    var inputText by remember { mutableStateOf(PrivChat.getDraft(channel.channelId) ?: "") }
    // Typing 节流：记录上次发送 typing 的时间戳（毫秒）
    var lastTypingSentMs by remember { mutableStateOf(0L) }
    // 当前页面是否已经上报过“正在输入”
    var typingActive by remember { mutableStateOf(false) }
    var panelMode by remember(channel.channelId) { mutableStateOf(InputPanelMode.NONE) }
    val hasOpenInputPanel = panelMode != InputPanelMode.NONE
    var voiceRecordingState by remember { mutableStateOf(VoiceRecordingState.IDLE) }
    var recordingStartMs by remember { mutableStateOf(0L) }

    // 60秒自动停止
    LaunchedEffect(voiceRecordingState) {
        if (voiceRecordingState == VoiceRecordingState.RECORDING) {
            delay(VOICE_MAX_DURATION_MS)
            if (voiceRecordingState != VoiceRecordingState.IDLE) {
                val durationMs = currentTimeMillis() - recordingStartMs
                voiceRecordingState = VoiceRecordingState.IDLE
                scope.launch {
                    onSendVoice?.invoke(channel.channelId, channel.channelType, durationMs)
                    delay(50)
                    val currentMessages = PrivChat.messages.value
                    if (currentMessages.isNotEmpty()) {
                        listState.scrollToItem(currentMessages.size - 1)
                    }
                }
            }
        }
    }

    // 加载消息 + 订阅 typing 事件
    LaunchedEffect(channel.channelId) {
        PrivChat.setCurrentChannel(channel.channelId)
        val cachedBeforeLoad = PrivChat.cachedMessages(channel.channelId)
        if (cachedBeforeLoad.isNotEmpty()) {
            PrivChat.updateMessages(channel.channelId, cachedBeforeLoad)
        }
        PrivChat.clearChannelUnread(channel.channelId)
        val result = onLoadMessages?.invoke(channel.channelId, channel.channelType)
            ?: withContext(Dispatchers.Default) {
                PrivChat.client.getMessagesByType(channel.channelId, channel.channelType, 50u, null)
            }
        result.onSuccess { list ->
            PrivChat.updateMessages(channel.channelId, list)
        }
        // 加载对端已读水位（cold start）
        runCatching {
            withContext(Dispatchers.Default) {
                PrivChat.client.getPeerReadPts(channel.channelId, channel.channelType)
                    .getOrNull()
                    ?.let { PrivChat.updatePeerReadPts(channel.channelId, it) }
            }
        }
        hasInitialLoadCompleted = true
        // 标记已读
        runCatching {
            onMarkRead?.invoke(channel.channelId, channel.channelType)
                ?: withContext(Dispatchers.Default) {
                    PrivChat.client.markChannelRead(channel.channelId, channel.channelType)
                }
        }
        // 订阅频道实时状态（typing 等）
        runCatching {
            withContext(Dispatchers.Default) {
                PrivChat.client.subscribeChannel(channel.channelId, channel.channelType.toUByte())
            }
        }
        if (channel.isDm) {
            // channel_member 表为空时 peerUserId 可能是 null，回退到 dmPeerUserId()
            if (peerUserId == null) {
                runCatching {
                    withContext(Dispatchers.Default) {
                        PrivChat.client.dmPeerUserId(channel.channelId)
                            .getOrNull()
                            ?.let { peerUserId = it }
                    }
                }
            }
            val uid = peerUserId
            if (uid != null) {
                runCatching {
                    withContext(Dispatchers.Default) {
                        PrivChat.client.fetchPresence(listOf(uid))
                            .getOrNull()
                            ?.firstOrNull()
                            ?.let { PrivChat.updatePresence(it) }
                    }
                }
            }
        }
    }

    // 重连后重新拉取对端在线状态
    val connectionState by PrivChat.connectionState.collectAsState()
    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.Connected && channel.isDm) {
            val uid = peerUserId ?: run {
                PrivChat.client.dmPeerUserId(channel.channelId)
                    .getOrNull()
                    ?.also { peerUserId = it }
            }
            if (uid != null) {
                runCatching {
                    withContext(Dispatchers.Default) {
                        PrivChat.client.fetchPresence(listOf(uid))
                            .getOrNull()
                            ?.firstOrNull()
                            ?.let { PrivChat.updatePresence(it) }
                    }
                }
            }
        }
    }

    // 保存草稿 + 取消订阅 typing 事件
    DisposableEffect(Unit) {
        onDispose {
            PrivChat.saveDraft(channel.channelId, inputText.takeIf { it.isNotBlank() })
            PrivChat.clearTyping(channel.channelId)
            PrivChat.setCurrentChannel(null)
            if (typingActive || inputText.isNotBlank()) {
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.Default) {
                            PrivChat.client.stopTyping(channel.channelId)
                        }
                    }
                }
            }
            // 取消订阅（best-effort，fire-and-forget）
            scope.launch {
                runCatching {
                    withContext(Dispatchers.Default) {
                        PrivChat.client.unsubscribeChannel(channel.channelId, channel.channelType.toUByte())
                    }
                }
            }
        }
    }

    // 首次进入直接定位到底部，并在定位完成前隐藏列表，避免看到"从上滚到下"。
    LaunchedEffect(channel.channelId, sortedMessages.lastOrNull()?.id, sortedMessages.size) {
        if (sortedMessages.isEmpty()) return@LaunchedEffect
        val lastIndex = sortedMessages.size - 1
        if (!initialPositioned) {
            delay(16)
            listState.scrollToItem(lastIndex)
            initialPositioned = true
            return@LaunchedEffect
        }
        // 收到新消息时：若用户已在底部附近（距底部 ≤ 3 条），自动滚到底部
        // 注意：用 sortedMessages.size 而非 layoutInfo.totalItemsCount，
        // 因为 layoutInfo 在新消息刚加入时可能尚未更新（stale），导致误判。
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (sortedMessages.size - 1 - lastVisible <= 3) {
            listState.scrollToItem(lastIndex)
        }
        // 用户在会话中收到新消息时，即时上报已读
        if (hasInitialLoadCompleted) {
            runCatching {
                withContext(Dispatchers.Default) {
                    PrivChat.client.markChannelRead(channel.channelId, channel.channelType)
                }
            }
        }
    }

    // 对方"正在输入"气泡出现时自动滚到底部，确保用户能看到
    val hasTypingBubble = PrivChat.activeTypingUsers(channel.channelId).isNotEmpty()
    LaunchedEffect(channel.channelId, hasTypingBubble) {
        if (hasTypingBubble && sortedMessages.isNotEmpty()) {
            listState.scrollToItem(sortedMessages.size)
        }
    }

    // 键盘弹起时滚到底部，避免消息被遮挡
    val currentBottomInset = runtimeEnv.safeArea.bottom
    var baselineBottomInset by remember(channel.channelId) { mutableStateOf(currentBottomInset.value) }
    LaunchedEffect(currentBottomInset) {
        if (currentBottomInset.value < baselineBottomInset) baselineBottomInset = currentBottomInset.value
        val keyboardVisible = currentBottomInset.value > baselineBottomInset + 80f
        if (keyboardVisible && sortedMessages.isNotEmpty()) {
            listState.scrollToItem(sortedMessages.size - 1)
        }
    }

    val truncatedTitle = channel.displayName.let { if (it.length > 15) it.take(15) + "..." else it }
    val peerPresence = peerUserId?.let { presences[it] }

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (hasOpenInputPanel) {
                    Modifier.clickable {
                        panelMode = InputPanelMode.NONE
                    }
                } else {
                    Modifier
                }
            ),
    ) {
        // 顶部导航栏
        NavBar(
            title = if (channel.isDm) "" else truncatedTitle,
            useDefaultBack = true,
            onBackClick = onBack,
            titleWidget = if (channel.isDm) {
                {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = truncatedTitle,
                            style = Typography.TitleMedium,
                            color = Theme.colors.textPrimary,
                        )
                        if (peerPresence?.isOnline == true) {
                            HorizontalSpacer(6.dp)
                            Text(
                                text = strings.presenceOnline,
                                style = Typography.Label,
                                color = Theme.colors.onlineStatus,
                            )
                        }
                    }
                }
            } else null,
            rightItems = listOf(
                NavBarItem(icon = Icons.more_horiz, onClick = onProfileClick)
            ),
        )
        networkStatusBar?.invoke()

        Box(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    if (!hasInitialLoadCompleted && messages.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize())
                    } else if (messages.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            EmptyState(
                                message = "暂无聊天内容",
                            )
                        }
                    } else {
                        GearLazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(if (initialPositioned || sortedMessages.isEmpty()) 1f else 0f),
                            state = listState,
                        ) {
                            items(sortedMessages.size) { index ->
                                val message = sortedMessages[index]
                                val isSelf = currentUserId?.let { message.isSelf(it) } ?: false

                                MessageRow(
                                    message = message,
                                    isSelf = isSelf,
                                    showAvatar = !channel.isDm || !isSelf,
                                    channelDisplayName = channel.displayName,
                                    onAvatarClick = if (!isSelf) onAvatarClick else null,
                                    peerReadPts = peerReadPts,
                                )
                            }
                            item {
                                TypingBubble(channelId = channel.channelId)
                            }
                        }
                    }
                }
            }
            // 录音浮层（覆盖在消息列表上方）
            if (voiceRecordingState != VoiceRecordingState.IDLE) {
                VoiceRecordingOverlay(
                    recordingState = voiceRecordingState,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // 输入框
        MessageInputBar(
            text = inputText,
            onTextChange = { newText ->
                inputText = newText
                // 节流发送 typing：文本非空且距离上次发送超过 3 秒
                if (newText.isNotBlank()) {
                    val now = currentTimeMillis()
                    if (now - lastTypingSentMs > 3_000L) {
                        lastTypingSentMs = now
                        typingActive = true
                        scope.launch {
                            runCatching {
                                withContext(Dispatchers.Default) {
                                    PrivChat.client.sendTyping(channel.channelId)
                                }
                            }
                        }
                    }
                } else if (typingActive) {
                    typingActive = false
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.Default) {
                                PrivChat.client.stopTyping(channel.channelId)
                            }
                        }
                    }
                }
            },
            panelMode = panelMode,
            onPanelModeChange = { panelMode = it },
            initialVoiceMode = PrivChat.getVoiceMode(channel.channelId),
            onVoiceModeChange = { PrivChat.saveVoiceMode(channel.channelId, it) },
            voiceRecordingState = voiceRecordingState,
            onVoiceRecordStart = {
                val canRecord = onVoiceStart?.invoke() ?: true
                if (canRecord) {
                    recordingStartMs = currentTimeMillis()
                    voiceRecordingState = VoiceRecordingState.RECORDING
                }
            },
            onVoiceRecordZoneChange = { inCancel ->
                if (voiceRecordingState != VoiceRecordingState.IDLE) {
                    voiceRecordingState = if (inCancel) VoiceRecordingState.CANCEL_ZONE
                                         else VoiceRecordingState.RECORDING
                }
            },
            onVoiceRecordEnd = {
                val durationMs = currentTimeMillis() - recordingStartMs
                voiceRecordingState = VoiceRecordingState.IDLE
                if (durationMs >= VOICE_MIN_DURATION_MS) {
                    scope.launch {
                        onSendVoice?.invoke(channel.channelId, channel.channelType, durationMs)
                        delay(50)
                        val currentMessages = PrivChat.messages.value
                        if (currentMessages.isNotEmpty()) {
                            listState.scrollToItem(currentMessages.size - 1)
                        }
                    }
                }
            },
            onVoiceRecordCancel = {
                voiceRecordingState = VoiceRecordingState.IDLE
                onVoiceCancel?.invoke()
            },
            safeAreaBottom = runtimeEnv.safeArea.bottom,
            onPickImage = {
                scope.launch {
                    try {
                        val result = onSendImage?.invoke(channel.channelId, channel.channelType)
                        result?.onFailure { e ->
                            val message = e.message ?: strings.networkError
                            if (!message.contains("cancel", ignoreCase = true) && !message.contains("取消")) {
                                onError?.invoke(message)
                            }
                        }
                    } catch (e: Exception) {
                        val message = e.message ?: strings.networkError
                        if (!message.contains("cancel", ignoreCase = true) && !message.contains("取消")) {
                            onError?.invoke(message)
                        }
                    }
                }
            },
            onPickCamera = {
                scope.launch {
                    try {
                        val result = onSendCamera?.invoke(channel.channelId, channel.channelType)
                        result?.onFailure { e ->
                            val message = e.message ?: strings.networkError
                            if (!message.contains("cancel", ignoreCase = true) && !message.contains("取消")) {
                                onError?.invoke(message)
                            }
                        }
                    } catch (e: Exception) {
                        val message = e.message ?: strings.networkError
                        if (!message.contains("cancel", ignoreCase = true) && !message.contains("取消")) {
                            onError?.invoke(message)
                        }
                    }
                }
            },
            onPickFile = {
                scope.launch {
                    try {
                        val result = onSendFile?.invoke(channel.channelId, channel.channelType)
                        result?.onFailure { e ->
                            val message = e.message ?: strings.networkError
                            if (!message.contains("cancel", ignoreCase = true) && !message.contains("取消")) {
                                onError?.invoke(message)
                            }
                        }
                    } catch (e: Exception) {
                        val message = e.message ?: strings.networkError
                        if (!message.contains("cancel", ignoreCase = true) && !message.contains("取消")) {
                            onError?.invoke(message)
                        }
                    }
                }
            },
            onLocation = {
                onError?.invoke("位置功能即将支持")
            },
            onRedPacket = {
                onError?.invoke("红包功能即将支持")
            },
            onContact = {
                onError?.invoke("联系人功能即将支持")
            },
            onSend = {
                if (inputText.isNotBlank()) {
                    val text = inputText
                    inputText = ""
                    if (typingActive) {
                        typingActive = false
                        scope.launch {
                            runCatching {
                                withContext(Dispatchers.Default) {
                                    PrivChat.client.stopTyping(channel.channelId)
                                }
                            }
                        }
                    }
                    scope.launch {
                        try {
                            // sendTextMessage 内部会先插入 optimistic 消息到 UI，再异步 FFI
                            val result = onSendText?.invoke(channel.channelId, channel.channelType, text)
                                ?: withContext(Dispatchers.Default) {
                                    PrivChat.client.sendText(channel.channelId, channel.channelType, text)
                                }
                            result.onFailure { error ->
                                if (error is CancellationException) return@onFailure
                                val message = error.message.orEmpty()
                                if (message.contains("left the composition", ignoreCase = true) ||
                                    message.contains("cancel", ignoreCase = true)
                                ) {
                                    return@onFailure
                                }
                                onError?.invoke(error.message ?: strings.networkError)
                            }
                        } catch (_: CancellationException) {
                            // 用户在发送过程中离开页面，scope 被取消，忽略即可
                        } catch (e: Exception) {
                            val message = e.message.orEmpty()
                            if (!message.contains("left the composition", ignoreCase = true) &&
                                !message.contains("cancel", ignoreCase = true)
                            ) {
                                onError?.invoke(e.message ?: strings.networkError)
                            }
                        }
                    }
                    // optimistic 消息已同步插入 UI，等一帧布局后滚动
                    scope.launch {
                        delay(50)
                        val currentMessages = PrivChat.messages.value
                        val lastIndex = (currentMessages.size - 1).coerceAtLeast(0)
                        if (currentMessages.isNotEmpty()) {
                            listState.scrollToItem(lastIndex)
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun DmPresenceStatus(
    presence: PresenceEntry?,
) {
    val strings = PrivChatI18n.strings
    val colors = Theme.colors
    val statusText = presenceStatusText(presence, strings)
    val statusColor = if (presence?.isOnline == true) colors.onlineStatus else colors.offlineStatus

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (statusText != null) {
            Text(
                text = statusText,
                style = Typography.Label,
                color = statusColor,
            )
        }
    }
}

private fun presenceStatusText(
    presence: PresenceEntry?,
    strings: com.netonstream.privchat.ui.i18n.PrivChatStrings,
): String? {
    if (presence == null) return null
    if (presence.isOnline) return strings.presenceOnline
    val lastSeen = presence.lastSeen ?: return strings.presenceOffline
    if (lastSeen <= 0L) return strings.presenceOffline
    return "${strings.presenceLastSeenPrefix} ${Formatter.conversationTime(lastSeen)}"
}

private fun String.dropLastGraphemeCluster(): String {
    if (isEmpty()) return this

    var clusterStart = previousCodePointStart(length)
    clusterStart = consumeTrailingEmojiContinuations(clusterStart)

    while (clusterStart > 0) {
        val previousStart = previousCodePointStart(clusterStart)
        if (codePointAtIndex(previousStart) != 0x200D) break
        clusterStart = consumeTrailingEmojiContinuations(previousStart)
    }

    if (clusterStart > 0) {
        val previousStart = previousCodePointStart(clusterStart)
        val currentCp = codePointAtIndex(clusterStart)
        val previousCp = codePointAtIndex(previousStart)
        if (currentCp.isRegionalIndicator() && previousCp.isRegionalIndicator()) {
            clusterStart = previousStart
        }
    }

    return substring(0, clusterStart)
}

private fun String.consumeTrailingEmojiContinuations(startIndex: Int): Int {
    var currentStart = startIndex
    while (currentStart > 0) {
        val cp = codePointAtIndex(currentStart)
        if (!cp.isGraphemeContinuation()) break
        currentStart = previousCodePointStart(currentStart)
    }
    return currentStart
}

private fun String.previousCodePointStart(index: Int): Int {
    if (index <= 0) return 0
    var cursor = index - 1
    if (cursor > 0 && this[cursor].isLowSurrogateChar() && this[cursor - 1].isHighSurrogateChar()) {
        cursor -= 1
    }
    return cursor
}

private fun String.codePointAtIndex(index: Int): Int {
    val current = this[index]
    return if (
        current.isHighSurrogateChar() &&
        index + 1 < length &&
        this[index + 1].isLowSurrogateChar()
    ) {
        ((current.code - 0xD800) shl 10) + (this[index + 1].code - 0xDC00) + 0x10000
    } else {
        current.code
    }
}

private fun Char.isHighSurrogateChar(): Boolean = this in '\uD800'..'\uDBFF'

private fun Char.isLowSurrogateChar(): Boolean = this in '\uDC00'..'\uDFFF'

private fun Int.isRegionalIndicator(): Boolean = this in 0x1F1E6..0x1F1FF

private fun Int.isGraphemeContinuation(): Boolean =
    this == 0x200D ||
        this == 0x20E3 ||
        this in 0x0300..0x036F ||
        this in 0x1AB0..0x1AFF ||
        this in 0x1DC0..0x1DFF ||
        this in 0x20D0..0x20FF ||
        this in 0xFE00..0xFE0F ||
        this in 0xFE20..0xFE2F ||
        this in 0x1F3FB..0x1F3FF ||
        this in 0xE0020..0xE007F ||
        this in 0xE0100..0xE01EF

/**
 * 消息行
 */
@Composable
private fun MessageRow(
    message: MessageEntry,
    isSelf: Boolean,
    showAvatar: Boolean = true,
    channelDisplayName: String = "",
    onAvatarClick: ((ULong) -> Unit)? = null,
    peerReadPts: ULong? = null,
) {
    val colors = Theme.colors
    val parsed = message.parsedContent

    // 撤回 / 系统消息走整行居中布局；其余走常规气泡。
    if (message.renderType() != RenderType.BUBBLE) {
        SystemMessageRow(message = message)
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        // 对方头像
        if (!isSelf && showAvatar) {
            Box(
                modifier = if (onAvatarClick != null) Modifier.clickable { onAvatarClick(message.fromUid) } else Modifier
            ) {
                ChatAvatar(
                    url = null, // TODO: 从用户信息获取
                    name = channelDisplayName.ifBlank { message.fromUid.toString() },
                    size = AvatarSpecs.Size.small,
                )
            }
            HorizontalSpacer(8.dp)
        }

        // 消息气泡（长按弹出动作菜单）
        MessageActionsMenu(
            actions = listOf(
                MessageAction(label = "回复", icon = Icons.reply) {},
                MessageAction(label = "复制", icon = Icons.content_copy) {},
                MessageAction(label = "撤回", icon = Icons.autorenew) {},
                MessageAction(label = "转发", icon = Icons.forward) {},
                MessageAction(label = "删除", icon = Icons.delete, danger = true) {},
                MessageAction(label = "选择", icon = Icons.check_box_outline_blank) {},
            ),
            modifier = Modifier.widthIn(max = 260.dp),
            onReaction = {},
            isSelf = isSelf,
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isSelf) 16.dp else 4.dp,
                            topEnd = 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = if (isSelf) 4.dp else 16.dp,
                        )
                    )
                    .background(if (isSelf) colors.bubbleSelf else colors.bubbleOther),
            ) {
                MessageContent(
                    message = message,
                    isSelf = isSelf,
                    peerReadPts = peerReadPts,
                )
            }
        }

        // 自己头像（可选）
        if (isSelf && showAvatar) {
            HorizontalSpacer(8.dp)
            ChatAvatar(
                url = null,
                name = "我",
                size = AvatarSpecs.Size.small,
            )
        }
    }
}

/**
 * 系统消息行
 */
@Composable
private fun SystemMessageRow(
    message: MessageEntry,
) {
    val strings = PrivChatI18n.strings
    val colors = Theme.colors
    val parsed = message.parsedContent

    // 文案按状态决定：撤回 → 本地化"消息已撤回"；系统 → 使用解析后文案。
    val text = if (message.isRevoked) strings.messageRevoked else (parsed.text ?: "")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(colors.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                text = text,
                style = Typography.Label,
                color = colors.textSecondary,
            )
        }
    }
}

/**
 * 输入状态指示器
 *
 * 显示"对方正在输入..."提示，自动过滤已过期的 typing 事件
 */
@Composable
private fun TypingBubble(channelId: ULong) {
    val typingMap by PrivChat.typingUserIds.collectAsState()
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(typingMap) {
        if (typingMap[channelId]?.isNotEmpty() == true) {
            delay(5_000)
            tick++
        }
    }
    val activeUsers = remember(typingMap, tick) { PrivChat.activeTypingUsers(channelId) }

    println("[PrivChat][TYPING] TypingBubble recompose: channelId=$channelId activeUsers=$activeUsers mapSize=${typingMap[channelId]?.size}")

    if (activeUsers.isEmpty()) {
        VerticalSpacer(4.dp)
        return
    }

    val colors = Theme.colors

    // 三个点逐步上下弹跳动画
    val infiniteTransition = rememberInfiniteTransition()
    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse)
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 150), RepeatMode.Reverse)
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 300), RepeatMode.Reverse)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 64.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        ChatAvatar(
            url = null,
            name = "?",
            size = AvatarSpecs.Size.small,
        )
        HorizontalSpacer(8.dp)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(colors.bubbleOther)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf(dot1, dot2, dot3).forEach { anim ->
                    val offsetY = (-6f * sin(anim * PI.toFloat())).dp
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .offset(y = offsetY)
                            .clip(RoundedCornerShape(50))
                            .background(colors.onBubbleOther.copy(alpha = 0.5f + 0.5f * anim))
                    )
                }
            }
        }
    }
}

/**
 * 消息输入栏
 */
@Composable
private fun MessageInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    panelMode: InputPanelMode,
    onPanelModeChange: (InputPanelMode) -> Unit,
    initialVoiceMode: Boolean = false,
    onVoiceModeChange: (Boolean) -> Unit = {},
    voiceRecordingState: VoiceRecordingState = VoiceRecordingState.IDLE,
    onVoiceRecordStart: () -> Unit = {},
    onVoiceRecordZoneChange: (inCancelZone: Boolean) -> Unit = {},
    onVoiceRecordEnd: () -> Unit = {},
    onVoiceRecordCancel: () -> Unit = {},
    safeAreaBottom: Dp = 0.dp,
    loading: Boolean = false,
    onPickImage: () -> Unit = {},
    onPickCamera: () -> Unit = {},
    onPickFile: () -> Unit = {},
    onLocation: () -> Unit = {},
    onRedPacket: () -> Unit = {},
    onContact: () -> Unit = {},
    onSend: () -> Unit,
) {
    val colors = Theme.colors
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var voiceMode by remember { mutableStateOf(initialVoiceMode) }
    var pendingPanelMode by remember { mutableStateOf<InputPanelMode?>(null) }
    var displayedPanelMode by remember { mutableStateOf(InputPanelMode.NONE) }
    var reservePanelHost by remember { mutableStateOf(false) }
    var overlayReservedPanelHost by remember { mutableStateOf(false) }
    var inputFocused by remember { mutableStateOf(false) }
    val inputFocusRequester = remember { FocusRequester() }
    var pendingAutoFocus by remember { mutableStateOf(false) }
    var collapsedBottomInset by remember { mutableStateOf(safeAreaBottom) }
    var lastKeyboardHeight by remember { mutableStateOf(0f) }
    val plusActions = remember {
        listOf(
            PlusAction(Icons.image, "相册", onPickImage),
            PlusAction(Icons.camera_alt, "相机", onPickCamera),
            PlusAction(Icons.flag, "位置", onLocation),
            PlusAction(Icons.mail, "红包", onRedPacket),
            PlusAction(Icons.attach_file, "文件", onPickFile),
            PlusAction(Icons.contacts, "联系人", onContact),
        )
    }
    val plusPages = remember(plusActions) { plusActions.chunked(8) }
    val emojis = remember {
        listOf(
            "😀", "😁", "😂", "🤣", "😊", "😉", "😍", "🥰",
            "😘", "😋", "😎", "🤩", "🥳", "🤗", "🤔", "🫡",
            "😴", "😮", "😢", "😭", "😤", "😡", "🤯", "🥺",
            "👍", "👎", "👌", "✌️", "🙏", "👏", "🙌", "🤝",
            "💪", "👀", "🎉", "🎂", "❤️", "💔", "💕", "💯",
            "🔥", "✨", "🌹", "🌞", "🌙", "⭐", "☕", "🍺",
            "🍎", "🍉", "⚽", "🏀", "🎮", "🎵", "🎁", "📷",
            "📍", "🚗", "✈️", "⌛", "✅", "❌", "❓", "❗"
        )
    }
    val inputControlHeight = 40.dp
    val panelHostHeight = 228.dp
    val panelTopSpacing = 8.dp
    val keyboardVisibleThreshold = 80f
    val rawKeyboardVisible = safeAreaBottom.value > (collapsedBottomInset.value + keyboardVisibleThreshold)
    // 对键盘消失信号做 150ms 防抖，避免切换 app 时系统短暂重置 inset 导致布局闪烁
    var keyboardVisible by remember { mutableStateOf(rawKeyboardVisible) }
    LaunchedEffect(rawKeyboardVisible) {
        if (rawKeyboardVisible) {
            keyboardVisible = true
        } else {
            delay(150)
            keyboardVisible = false
        }
    }
    val hostVisible = displayedPanelMode != InputPanelMode.NONE || pendingPanelMode != null || reservePanelHost
    // 过渡期间用键盘高度撑起面板区域，使输入框位置保持不动；panel 显示时 bottom padding 切换为 collapsed inset
    val effectivePanelHeight = ((lastKeyboardHeight - panelTopSpacing.value).coerceAtLeast(panelHostHeight.value)).dp
    val effectiveBottomPadding = if (hostVisible) collapsedBottomInset else safeAreaBottom

    fun closeAllPanels() {
        logMessageInputBar(
            "closeAllPanels panelMode=$panelMode displayed=$displayedPanelMode pending=$pendingPanelMode reserve=$reservePanelHost overlayReserve=$overlayReservedPanelHost inset=${safeAreaBottom.value}"
        )
        pendingPanelMode = null
        reservePanelHost = false
        overlayReservedPanelHost = false
        displayedPanelMode = InputPanelMode.NONE
        onPanelModeChange(InputPanelMode.NONE)
    }

    fun requestPanel(targetMode: InputPanelMode) {
        logMessageInputBar(
            "requestPanel target=$targetMode panelMode=$panelMode displayed=$displayedPanelMode pending=$pendingPanelMode reserve=$reservePanelHost overlayReserve=$overlayReservedPanelHost focused=$inputFocused inset=${safeAreaBottom.value}"
        )
        if (!keyboardVisible) {
            // 无键盘：直接切换面板，不需要 overlay 过渡
            pendingPanelMode = null
            reservePanelHost = false
            overlayReservedPanelHost = false
            displayedPanelMode = targetMode
            onPanelModeChange(targetMode)
            return
        }
        reservePanelHost = true
        overlayReservedPanelHost = true
        pendingPanelMode = targetMode
        displayedPanelMode = InputPanelMode.NONE
        onPanelModeChange(InputPanelMode.NONE)
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    fun transitionPanelToKeyboard() {
        logMessageInputBar(
            "transitionPanelToKeyboard panelMode=$panelMode displayed=$displayedPanelMode pending=$pendingPanelMode reserve=$reservePanelHost overlayReserve=$overlayReservedPanelHost focused=$inputFocused inset=${safeAreaBottom.value}"
        )
        reservePanelHost = true
        overlayReservedPanelHost = true
        pendingPanelMode = null
        displayedPanelMode = InputPanelMode.NONE
        onPanelModeChange(InputPanelMode.NONE)
    }

    LaunchedEffect(panelMode) {
        // 仅当面板实际显示时才响应外部关闭，避免干扰表情→键盘的过渡流程
        if (panelMode == InputPanelMode.NONE && displayedPanelMode != InputPanelMode.NONE) {
            closeAllPanels()
        }
    }

    LaunchedEffect(safeAreaBottom) {
        logMessageInputBar(
            "safeAreaBottom=${safeAreaBottom.value} collapsedBaseline=${collapsedBottomInset.value} keyboardVisible=$keyboardVisible panelMode=$panelMode displayed=$displayedPanelMode pending=$pendingPanelMode reserve=$reservePanelHost overlayReserve=$overlayReservedPanelHost focused=$inputFocused"
        )
        val kbHeight = (safeAreaBottom.value - collapsedBottomInset.value).coerceAtLeast(0f)
        if (kbHeight > lastKeyboardHeight) {
            lastKeyboardHeight = kbHeight
        }
        if (safeAreaBottom.value < collapsedBottomInset.value) {
            collapsedBottomInset = safeAreaBottom
        }
    }

    LaunchedEffect(pendingPanelMode, keyboardVisible) {
        val targetMode = pendingPanelMode ?: return@LaunchedEffect
        if (!keyboardVisible) {
            logMessageInputBar(
                "pendingResolved target=$targetMode keyboardVisible=$keyboardVisible inset=${safeAreaBottom.value}"
            )
            displayedPanelMode = targetMode
            reservePanelHost = false
            overlayReservedPanelHost = false
            pendingPanelMode = null
            onPanelModeChange(targetMode)
        }
    }

    LaunchedEffect(inputFocused, keyboardVisible, reservePanelHost, displayedPanelMode, pendingPanelMode) {
        if (pendingPanelMode != null) return@LaunchedEffect
        if (displayedPanelMode != InputPanelMode.NONE) return@LaunchedEffect
        if (!reservePanelHost) return@LaunchedEffect
        if (inputFocused && keyboardVisible) {
            logMessageInputBar(
                "releaseReserveForKeyboard focused=$inputFocused keyboardVisible=$keyboardVisible inset=${safeAreaBottom.value}"
            )
            reservePanelHost = false
            overlayReservedPanelHost = false
        }
    }

    LaunchedEffect(panelMode, displayedPanelMode, pendingPanelMode, reservePanelHost, overlayReservedPanelHost, inputFocused, keyboardVisible, hostVisible) {
        logMessageInputBar(
            "state panelMode=$panelMode displayed=$displayedPanelMode pending=$pendingPanelMode reserve=$reservePanelHost overlayReserve=$overlayReservedPanelHost hostVisible=$hostVisible focused=$inputFocused keyboardVisible=$keyboardVisible inset=${safeAreaBottom.value}"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface),
    ) {
        // 顶部分隔线（与 BottomNavBar 一致）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.divider),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
            // 左侧：语音/键盘切换
                CircleIconButton(
                    icon = if (voiceMode) Icons.chat else Icons.mic,
                    onClick = {
                        val enteringVoice = !voiceMode
                        voiceMode = enteringVoice
                        onVoiceModeChange(enteringVoice)
                        closeAllPanels()
                        if (enteringVoice) {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                        } else {
                            pendingAutoFocus = true
                        }
                    }
                )

                HorizontalSpacer(8.dp)

                if (voiceMode) {
                    val isInCancelZone = voiceRecordingState == VoiceRecordingState.CANCEL_ZONE
                    val isRecording = voiceRecordingState != VoiceRecordingState.IDLE
                    val btnBackground = when {
                        isInCancelZone -> Color(0xFFE53935)
                        isRecording -> colors.primary
                        else -> colors.surfaceVariant
                    }
                    val btnText = when {
                        isInCancelZone -> "松开 取消"
                        isRecording -> "松开 发送"
                        else -> "按住 说话"
                    }
                    val btnTextColor = if (isInCancelZone) Color.White
                                      else if (isRecording) colors.onPrimary
                                      else colors.textPrimary
                    // 取消阈值：手指上滑 60dp
                    val cancelThresholdPx = with(LocalDensity.current) { 60.dp.toPx() }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(inputControlHeight)
                            .clip(RoundedCornerShape(8.dp))
                            .background(btnBackground)
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    onVoiceRecordStart()
                                    var inCancel = false
                                    try {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull {
                                                it.id == down.id
                                            } ?: break
                                            if (change.changedToUp()) {
                                                if (inCancel) onVoiceRecordCancel()
                                                else onVoiceRecordEnd()
                                                break
                                            }
                                            val deltaY = change.position.y - down.position.y
                                            val nowInCancel = deltaY < -cancelThresholdPx
                                            if (nowInCancel != inCancel) {
                                                inCancel = nowInCancel
                                                onVoiceRecordZoneChange(inCancel)
                                            }
                                        }
                                    } catch (_: CancellationException) {
                                        onVoiceRecordCancel()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = btnText,
                            style = Typography.BodyMedium,
                            color = btnTextColor,
                        )
                    }
                } else {
                    AutoResizeTextarea(
                        value = text,
                        onValueChange = onTextChange,
                        placeholder = "输入消息",
                        modifier = Modifier.weight(1f),
                        maxLines = 8,
                        autoFocus = pendingAutoFocus,
                        focusRequester = inputFocusRequester,
                        onFocusChanged = { focused ->
                            logMessageInputBar(
                                "focusChanged focused=$focused panelMode=$panelMode displayed=$displayedPanelMode pending=$pendingPanelMode reserve=$reservePanelHost overlayReserve=$overlayReservedPanelHost inset=${safeAreaBottom.value}"
                            )
                            inputFocused = focused
                            if (focused) {
                                pendingAutoFocus = false
                                if (displayedPanelMode != InputPanelMode.NONE || pendingPanelMode != null) {
                                    transitionPanelToKeyboard()
                                }
                            }
                        },
                    )
                }

                HorizontalSpacer(8.dp)

                // 右侧：表情
                CircleIconButton(
                    icon = Icons.favorite_border,
                    onClick = {
                    if (panelMode == InputPanelMode.EMOJI || displayedPanelMode == InputPanelMode.EMOJI) {
                        closeAllPanels()
                    } else {
                        requestPanel(InputPanelMode.EMOJI)
                        }
                        if (voiceMode) voiceMode = false
                    }
                )

                HorizontalSpacer(8.dp)

                // 右侧：有文字时显示发送按钮，否则显示 ➕
                if (text.isNotBlank()) {
                    Button(
                        text = "发送",
                        theme = ButtonTheme.PRIMARY,
                        size = ButtonSize.SMALL,
                        disabled = loading,
                        loading = loading,
                        onClick = {
                            // 先锁住键盘，再发送（发送会清空 text 触发 recompose，可能导致焦点丢失）
                            if (displayedPanelMode == InputPanelMode.NONE) {
                                keyboardController?.show()
                            }
                            onSend()
                        },
                    )
                } else {
                CircleIconButton(
                    icon = Icons.add,
                    onClick = {
                    if (panelMode == InputPanelMode.PLUS || displayedPanelMode == InputPanelMode.PLUS) {
                        closeAllPanels()
                    } else {
                        requestPanel(InputPanelMode.PLUS)
                        }
                        if (voiceMode) voiceMode = false
                    }
                )
                }
            }
        }

        if (displayedPanelMode != InputPanelMode.NONE || pendingPanelMode != null || reservePanelHost) {
            if (overlayReservedPanelHost && displayedPanelMode == InputPanelMode.NONE) {
                // 过渡期间用键盘等高的占位，防止输入框位置跳变
                VerticalSpacer(panelTopSpacing)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(effectivePanelHeight)
                        .alpha(0f)
                )
            } else {
                VerticalSpacer(panelTopSpacing)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(panelHostHeight)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.background)
                        .clickable { }
                ) {
                    if (displayedPanelMode == InputPanelMode.EMOJI) {
                        BoxWithConstraints(
                            modifier = Modifier.fillMaxSize()
                        ) {
                        val emojiCellMinWidth = 48.dp
                        val emojiCellSpacing = 3.dp
                        val emojiColumns = (((maxWidth.value + emojiCellSpacing.value) /
                            (emojiCellMinWidth.value + emojiCellSpacing.value)).toInt())
                            .coerceIn(1, 7)
                        val emojiRows = remember(emojis, emojiColumns) { emojis.chunked(emojiColumns) }
                        val emojiGridHorizontalPadding = emojiCellSpacing
                        val emojiGridVerticalPadding = emojiCellSpacing
                        val emojiActionInset = 44.dp
                        val emojiContentWidth =
                            maxWidth - (emojiGridHorizontalPadding * 2) - (emojiCellSpacing * (emojiColumns - 1))
                        val emojiCellSize = emojiContentWidth / emojiColumns

                        ScrollView(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = emojiGridHorizontalPadding,
                                        end = emojiGridHorizontalPadding,
                                        top = emojiGridVerticalPadding,
                                        bottom = emojiGridVerticalPadding + emojiActionInset,
                                    )
                            ) {
                                emojiRows.forEachIndexed { rowIndex, rowEmojis ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(emojiCellSpacing),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        rowEmojis.forEach { emoji ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(emojiCellSize)
                                                    .clickable { onTextChange(text + emoji) },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(text = emoji, style = Typography.HeadlineSmall)
                                            }
                                        }
                                        repeat((emojiColumns - rowEmojis.size).coerceAtLeast(0)) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                    if (rowIndex < emojiRows.lastIndex) VerticalSpacer(emojiCellSpacing)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 10.dp, bottom = 8.dp),
                            contentAlignment = Alignment.BottomEnd,
                        ) {
                            Button(
                                text = "删除",
                                theme = ButtonTheme.DEFAULT,
                                size = ButtonSize.SMALL,
                                onClick = {
                                    if (text.isNotEmpty()) onTextChange(text.dropLastGraphemeCluster())
                                },
                            )
                        }
                        }
                    }

                    if (displayedPanelMode == InputPanelMode.PLUS) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                        ) {
                            Swiper(
                                itemCount = plusPages.size.coerceAtLeast(1),
                                loop = false,
                                autoPlay = false,
                                navigation = if (plusPages.size > 1) SwiperNavigation.DOTS else SwiperNavigation.NONE,
                                indicatorPosition = SwiperIndicatorPosition.OUTSIDE_BOTTOM,
                                height = 180.dp,
                            ) { pageIndex ->
                                val pageActions = plusPages.getOrElse(pageIndex) { emptyList() }
                                Column(modifier = Modifier.fillMaxSize()) {
                                    for (row in 0..1) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            for (col in 0..3) {
                                                val idx = row * 4 + col
                                                val action = pageActions.getOrNull(idx)
                                                if (action != null) {
                                                    PlusActionItem(
                                                        icon = action.icon,
                                                        text = action.text,
                                                        onClick = action.onClick,
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.width(78.dp))
                                                }
                                            }
                                        }
                                        if (row == 0) VerticalSpacer(12.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        VerticalSpacer(8.dp)
        Box(modifier = Modifier.padding(bottom = effectiveBottomPadding))
    }
}

/**
 * 录音浮层
 */
@Composable
private fun VoiceRecordingOverlay(
    recordingState: VoiceRecordingState,
    modifier: Modifier = Modifier,
) {
    val isCancel = recordingState == VoiceRecordingState.CANCEL_ZONE
    val cardColor = if (isCancel) Color(0xFFE53935) else Color(0xFF4CAF50)

    // 波形动画
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavePhase",
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000)),
        )

        // 卡片：左侧麦克风，右侧波形
        Row(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(cardColor)
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左：麦克风图标
            Icon(
                name = if (isCancel) Icons.delete else Icons.mic,
                size = 56.dp,
                tint = Color.White,
            )
            HorizontalSpacer(24.dp)
            // 右：9 根波形柱
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(48.dp),
            ) {
                val barCount = 9
                val phaseStep = (2 * PI / barCount).toFloat()
                repeat(barCount) { i ->
                    val height = if (isCancel) 8f else {
                        ((sin((wavePhase + i * phaseStep).toDouble()) * 0.45 + 0.55) * 44).toFloat()
                            .coerceAtLeast(6f)
                    }
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(height.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xCCFFFFFF)),
                    )
                }
            }
        }
    }
}

@Composable
private fun CircleIconButton(
    icon: String,
    onClick: () -> Unit,
) {
    val colors = Theme.colors
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            name = icon,
            size = 18.dp,
            tint = colors.textPrimary,
        )
    }
}

private data class PlusAction(
    val icon: String,
    val text: String,
    val onClick: () -> Unit,
)

@Composable
private fun PlusActionItem(
    icon: String,
    text: String,
    onClick: () -> Unit,
) {
    val colors = Theme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(96.dp).clickable { onClick() },
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(name = icon, size = 22.dp, tint = colors.textPrimary)
        }
        VerticalSpacer(6.dp)
        Text(text = text, style = Typography.Label, color = colors.textSecondary)
    }
}
