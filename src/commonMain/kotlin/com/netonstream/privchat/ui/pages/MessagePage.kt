package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import com.netonstream.privchat.sdk.ConnectionState
import com.netonstream.privchat.sdk.dto.ChannelListEntry
import com.netonstream.privchat.sdk.dto.ContentMessageType
import com.netonstream.privchat.sdk.dto.GroupMemberEntry
import com.netonstream.privchat.sdk.dto.MessageEntry
import com.netonstream.privchat.sdk.dto.MessageStatus
import com.netonstream.privchat.sdk.dto.PresenceEntry
import com.netonstream.privchat.sdk.dto.contentType
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.models.*
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.components.DefaultMessageReactions
import com.netonstream.privchat.ui.components.MessageAction
import com.netonstream.privchat.ui.components.MessageActionKind
import com.netonstream.privchat.ui.components.MessageActionPolicy
import com.netonstream.privchat.ui.components.MessageActionsMenu
import com.netonstream.privchat.ui.components.MessageContent
import com.netonstream.privchat.ui.platform.ClipboardBridge
import com.tencent.kuikly.compose.foundation.gestures.detectTapGestures
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.offlineStatus
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.onlineStatus
import com.netonstream.privchat.ui.utils.Formatter
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.theme.Theme
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.primitives.ScrollView
import com.gearui.foundation.typography.IconSizes
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
import com.gearui.components.actionsheet.ActionSheet
import com.gearui.components.dialog.ConfirmDialog
import com.gearui.components.dialog.Dialog
import com.gearui.components.toast.Toast
import com.gearui.components.swiper.Swiper
import com.gearui.components.swiper.SwiperNavigation
import com.gearui.components.swiper.SwiperIndicatorPosition
import com.gearui.runtime.LocalGearRuntimeEnvironment
import com.tencent.kuikly.compose.ui.platform.LocalSoftwareKeyboardController
import com.tencent.kuikly.compose.ui.platform.LocalFocusManager
import com.tencent.kuikly.compose.ui.focus.FocusRequester
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.layout.ExperimentalLayoutApi
import com.tencent.kuikly.compose.foundation.layout.FlowRow
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
import com.tencent.kuikly.compose.animation.core.animateFloatAsState
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
    // 附件发送回调：在 picker 返回、准备阶段开始时调用 `onPrepStart(label)` 弹出全屏 loading。
    onSendImage: (suspend (ULong, Int, onPrepStart: (String) -> Unit) -> Result<ULong>)? = null, // 相册
    onSendCamera: (suspend (ULong, Int, onPrepStart: (String) -> Unit) -> Result<ULong>)? = null, // 相机
    onSendFile: (suspend (ULong, Int, onPrepStart: (String) -> Unit) -> Result<ULong>)? = null,
    onVoiceStart: (() -> Boolean)? = null,
    onVoiceCancel: (() -> Unit)? = null,
    onSendVoice: (suspend (ULong, Int, durationMs: Long) -> Result<ULong>)? = null,
    onRequestForward: ((MessageEntry) -> Unit)? = null,
    onVideoPreview: ((MessageEntry) -> Unit)? = null,
    onImagePreview: ((MessageEntry) -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val messages by PrivChat.messages.collectAsState()
    val messageReactions by PrivChat.messageReactions.collectAsState()
    val currentUserId by PrivChat.currentUserId.collectAsState()
    val presences by PrivChat.presences.collectAsState()
    val peerReadPtsMap by PrivChat.peerReadPtsByChannel.collectAsState()
    val peerReadPts = peerReadPtsMap[channel.channelId]
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val runtimeEnv = LocalGearRuntimeEnvironment.current
    val sortedMessages = messages
    // REPLY_SPEC §4.3：按 server_message_id 建立索引，引用气泡渲染时 O(1) 定位原消息。
    val messagesByServerId = remember(sortedMessages) {
        val map = HashMap<String, MessageEntry>(sortedMessages.size)
        for (m in sortedMessages) {
            val sid = m.serverMessageId ?: continue
            map[sid.toString()] = m
        }
        map
    }
    // peer_user_id：优先从 channel 字段取，channel_member 表为空时回退到 dmPeerUserId()
    var peerUserId by remember(channel.channelId) { mutableStateOf(channel.peerUserId) }
    var initialPositioned by remember(channel.channelId) { mutableStateOf(false) }
    var hasInitialLoadCompleted by remember(channel.channelId) { mutableStateOf(false) }
    // 输入文本
    var inputText by remember { mutableStateOf(PrivChat.getDraft(channel.channelId) ?: "") }
    // UX-10：@ 提及选择器（仅群聊）。mentionQuery=null 时隐藏 picker；
    // mentionSpans 记录每段 `@name ` 的区间（含尾随空格），用于原子删除与回填 userId。
    var mentionQuery by remember(channel.channelId) { mutableStateOf<String?>(null) }
    val mentionSpans = remember(channel.channelId) { mutableStateListOf<MentionSpan>() }
    // REPLY_SPEC：长按【回复】后进入回复态；onSend 发送时把 serverMessageId 透传给 SDK。
    var pendingReply by remember(channel.channelId) { mutableStateOf<MessageEntry?>(null) }
    // REPLY_SPEC §4.3：点击引用摘要后滚动到原消息并短暂高亮；800ms 后自动清除。
    var highlightMessageId by remember(channel.channelId) { mutableStateOf<ULong?>(null) }
    LaunchedEffect(highlightMessageId) {
        if (highlightMessageId != null) {
            delay(800)
            highlightMessageId = null
        }
    }
    val allGroupMembers by PrivChat.groupMembers.collectAsState()
    val groupMembersForChannel = remember(allGroupMembers, channel.channelId) {
        if (channel.isDm) emptyList() else allGroupMembers.filter { it.channelId == channel.channelId }
    }
    // Typing 节流：记录上次发送 typing 的时间戳（毫秒）
    var lastTypingSentMs by remember { mutableStateOf(0L) }
    // 当前页面是否已经上报过“正在输入”
    var typingActive by remember { mutableStateOf(false) }
    var panelMode by remember(channel.channelId) { mutableStateOf(InputPanelMode.NONE) }
    val hasOpenInputPanel = panelMode != InputPanelMode.NONE
    var voiceRecordingState by remember { mutableStateOf(VoiceRecordingState.IDLE) }
    // 媒体预处理（复制原图/生成缩略图/压缩视频/复制文件）进行中时，全屏 loading 遮罩。
    var mediaPrepBusy by remember { mutableStateOf(false) }
    var mediaPrepLabel by remember { mutableStateOf("") }
    var recordingStartMs by remember { mutableStateOf(0L) }

    // UX-7 未读分隔线：进入会话时快照 unreadCount，并在消息首次填充后锚定到首条未读的 message id。
    // 锚点只计算一次（进入会话那一瞬间）；后续收到新消息时分隔线位置保持稳定，直到退出会话。
    val initialUnreadSnapshot = remember(channel.channelId) { channel.unreadCount }
    var unreadDividerAnchorId by remember(channel.channelId) { mutableStateOf<ULong?>(null) }
    var unreadDividerAnchorResolved by remember(channel.channelId) { mutableStateOf(false) }

    // UX-8 新消息浮动气泡：用户滚到历史区时累计新消息数，点击胶囊回到底部。
    var newMsgBubbleCount by remember(channel.channelId) { mutableStateOf(0) }
    var lastSeenLastId by remember(channel.channelId) { mutableStateOf<ULong?>(null) }

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
                        listState.animateScrollToItem(currentMessages.size - 1)
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

    // 批量加载当前会话消息的 reactions（消息列表变化时增量刷新）
    val messageIdsKey = remember(messages) { messages.map { it.id } }
    LaunchedEffect(messageIdsKey) {
        if (messageIdsKey.isEmpty()) return@LaunchedEffect
        withContext(Dispatchers.Default) {
            PrivChat.client.reactionsBatch(channel.channelId, messageIdsKey)
        }.onSuccess { PrivChat.mergeMessageReactions(it) }
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

    // UX-9：输入变化后 400ms 防抖 flush。避免高频键入每次都穿透到 SharedPreferences / NSUserDefaults。
    // onDispose 分支依旧会在退出会话时做一次无条件兜底 flush。
    LaunchedEffect(channel.channelId, inputText) {
        delay(400L)
        PrivChat.saveDraft(channel.channelId, inputText.takeIf { it.isNotBlank() })
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

    // 用 localMessageId（若存在）作为 LazyColumn item key：出站消息在 placeholder → 服务端确认时
    // DB id 会变（旧 placeholder 行被删、真实行被新建），但 localMessageId 保持不变，
    // 用它做 key 可以让 LazyColumn 复用同一个 slot，避免状态更新时重建 row。
    fun MessageEntry.stableKey(): ULong =
        localMessageId?.takeIf { it > 0uL } ?: id

    // UX-7 未读分隔线：首次获取到消息列表后，按 unreadCount 向前回推定位到首条未读，并记住 id。
    // 只解析一次；之后列表变化（新消息到达、上拉刷新）都不重算。
    LaunchedEffect(channel.channelId, sortedMessages.size) {
        if (unreadDividerAnchorResolved) return@LaunchedEffect
        if (sortedMessages.isEmpty()) return@LaunchedEffect
        if (initialUnreadSnapshot > 0 && sortedMessages.size >= initialUnreadSnapshot) {
            val firstUnreadIndex = sortedMessages.size - initialUnreadSnapshot
            unreadDividerAnchorId = sortedMessages[firstUnreadIndex].id
        }
        unreadDividerAnchorResolved = true
    }

    // 首次进入直接定位到底部，并在定位完成前隐藏列表，避免看到"从上滚到下"。
    LaunchedEffect(channel.channelId, sortedMessages.lastOrNull()?.id, sortedMessages.size) {
        if (sortedMessages.isEmpty()) return@LaunchedEffect
        val lastIndex = sortedMessages.size - 1
        if (!initialPositioned) {
            delay(16)
            listState.scrollToItem(lastIndex)
            initialPositioned = true
            lastSeenLastId = sortedMessages[lastIndex].id
            return@LaunchedEffect
        }
        // 收到新消息时：若用户已在底部附近（距底部 ≤ 3 条），自动滚到底部
        // 注意：用 sortedMessages.size 而非 layoutInfo.totalItemsCount，
        // 因为 layoutInfo 在新消息刚加入时可能尚未更新（stale），导致误判。
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        val nearBottom = sortedMessages.size - 1 - lastVisible <= 3
        val newLastId = sortedMessages[lastIndex].id
        if (nearBottom) {
            listState.animateScrollToItem(lastIndex)
            newMsgBubbleCount = 0
        } else if (newLastId != lastSeenLastId && lastSeenLastId != null) {
            // UX-8：历史区有新消息时累加计数。仅统计对方消息（自己发送的会随 onSend 自动滚底）。
            val lastSeenIdx = sortedMessages.indexOfFirst { it.id == lastSeenLastId }
            val delta = if (lastSeenIdx >= 0) sortedMessages.size - 1 - lastSeenIdx else 1
            newMsgBubbleCount += delta.coerceAtLeast(1)
        }
        lastSeenLastId = newLastId
        // 用户在会话中收到新消息时，即时上报已读
        if (hasInitialLoadCompleted) {
            runCatching {
                withContext(Dispatchers.Default) {
                    PrivChat.client.markChannelRead(channel.channelId, channel.channelType)
                }
            }
        }
    }

    // 对方"正在输入"气泡更新时自动滚到底部。
    // 用最新的 typing 心跳时间戳作为 LaunchedEffect key——只要对方每次心跳都会
    // 刷新这个值，哪怕气泡短暂隐藏后又重新出现，也能触发新一轮滚动。
    // （此前用 hasTypingBubble Boolean 作 key，_typingUsers 不主动清理过期条目，
    // 气泡视觉消失时 Boolean 仍然是 true，下一次对方输入无状态跳变，滚动不触发。）
    val typingMapForScroll by PrivChat.typingUserIds.collectAsState()
    val latestPeerTypingMs = typingMapForScroll[channel.channelId]?.values?.maxOrNull() ?: 0L
    LaunchedEffect(channel.channelId, latestPeerTypingMs) {
        if (latestPeerTypingMs <= 0L || sortedMessages.isEmpty()) return@LaunchedEffect
        // 仅当用户已在底部附近时才跟随 typing 气泡滚动，否则对方心跳会把用户的上滑手势强行拽回底部。
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        val totalItems = sortedMessages.size // 不含 typing 气泡本身
        if (totalItems - 1 - lastVisible <= 3) {
            listState.animateScrollToItem(sortedMessages.size)
        }
    }

    // 键盘弹起时滚到底部，避免消息被遮挡
    val currentBottomInset = runtimeEnv.safeArea.bottom
    var baselineBottomInset by remember(channel.channelId) { mutableStateOf(currentBottomInset.value) }
    LaunchedEffect(currentBottomInset) {
        if (currentBottomInset.value < baselineBottomInset) baselineBottomInset = currentBottomInset.value
        val keyboardVisible = currentBottomInset.value > baselineBottomInset + 80f
        if (keyboardVisible && sortedMessages.isNotEmpty()) {
            listState.animateScrollToItem(sortedMessages.size - 1)
        }
    }

    // 表情/+ 面板弹起时同样滚到底部（面板是应用内的 layout 变化，没有系统 inset 事件）
    LaunchedEffect(hasOpenInputPanel) {
        if (hasOpenInputPanel && sortedMessages.isNotEmpty()) {
            listState.animateScrollToItem(sortedMessages.size - 1)
        }
    }

    val truncatedTitle = channel.displayName.let { if (it.length > 15) it.take(15) + "..." else it }
    val peerPresence = peerUserId?.let { presences[it] }

    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
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
                        // UX-5 浮动日期胶囊：根据首条可见消息的日期显示；停止滚动 1.5 秒后淡出。
                        val firstVisibleIdx by remember {
                            derivedStateOf { listState.firstVisibleItemIndex }
                        }
                        val isScrolling by remember {
                            derivedStateOf { listState.isScrollInProgress }
                        }
                        var dateHeaderVisible by remember(channel.channelId) { mutableStateOf(false) }
                        LaunchedEffect(isScrolling) {
                            if (isScrolling) {
                                dateHeaderVisible = true
                            } else {
                                delay(1500)
                                dateHeaderVisible = false
                            }
                        }
                        val headerLabel = remember(firstVisibleIdx, sortedMessages) {
                            sortedMessages.getOrNull(firstVisibleIdx)
                                ?.let { Formatter.messageDateLabel(it.timestamp) }
                                .orEmpty()
                        }

                        GearLazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(if (initialPositioned || sortedMessages.isEmpty()) 1f else 0f),
                            state = listState,
                        ) {
                            items(
                                count = sortedMessages.size,
                                key = { sortedMessages[it].stableKey().toLong() },
                            ) { index ->
                                val message = sortedMessages[index]
                                val isSelf = currentUserId?.let { message.isSelf(it) } ?: false
                                val previous = if (index > 0) sortedMessages[index - 1] else null

                                // UX-11 时间合并 + UX-5 跨日分隔线（日期优先于时间）
                                MessageGroupDivider(previous = previous, current = message)

                                if (unreadDividerAnchorId == message.id && initialUnreadSnapshot > 0) {
                                    UnreadDivider(count = initialUnreadSnapshot)
                                }

                                MessageRow(
                                    message = message,
                                    isSelf = isSelf,
                                    showAvatar = !channel.isDm || !isSelf,
                                    channelDisplayName = channel.displayName,
                                    onAvatarClick = if (!isSelf) onAvatarClick else null,
                                    onAvatarLongPress = if (!channel.isDm && !isSelf) { userId, name ->
                                        val ins = appendMention(inputText, name, userId)
                                        inputText = ins.text
                                        mentionSpans.add(ins.span)
                                        mentionQuery = null
                                    } else null,
                                    peerReadPts = peerReadPts,
                                    reactions = messageReactions[message.id].orEmpty(),
                                    selfUserId = currentUserId,
                                    onRequestForward = onRequestForward,
                                    onVideoPreview = onVideoPreview,
                                    onImagePreview = onImagePreview,
                                    onReply = { target ->
                                        if (target.serverMessageId == null) {
                                            Toast.error("原消息尚未发送")
                                        } else {
                                            pendingReply = target
                                        }
                                    },
                                    replyLookup = { serverId -> messagesByServerId[serverId] },
                                    senderLabelOf = { uid ->
                                        when {
                                            uid == currentUserId -> "我"
                                            channel.isDm -> channel.displayName.ifBlank { uid.toString() }
                                            else -> groupMembersForChannel
                                                .firstOrNull { it.userId == uid }
                                                ?.let { it.remark.ifBlank { it.name } }
                                                ?.takeIf { it.isNotBlank() }
                                                ?: uid.toString()
                                        }
                                    },
                                    onReplyClick = { target ->
                                        val idx = sortedMessages.indexOf(target)
                                        if (idx >= 0) {
                                            scope.launch {
                                                listState.animateScrollToItem(idx)
                                                highlightMessageId = target.id
                                            }
                                        }
                                    },
                                    isHighlighted = highlightMessageId == message.id,
                                )
                            }
                            item {
                                TypingBubble(
                                    channelId = channel.channelId,
                                    peerName = channel.displayName,
                                )
                            }
                        }

                        if (dateHeaderVisible && headerLabel.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 8.dp),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                FloatingDateHeader(label = headerLabel)
                            }
                        }
                    }
                }
            }
            // UX-8 新消息浮动气泡：用户在历史区时显示于右下角，点击回到底部。
            if (newMsgBubbleCount > 0 && voiceRecordingState == VoiceRecordingState.IDLE) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 16.dp, bottom = 12.dp),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    NewMessagesBubble(
                        count = newMsgBubbleCount,
                        onClick = {
                            scope.launch {
                                if (sortedMessages.isNotEmpty()) {
                                    listState.animateScrollToItem(sortedMessages.size - 1)
                                }
                                newMsgBubbleCount = 0
                            }
                        },
                    )
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

        // REPLY_SPEC §4.1：回复态窄条，锚定在输入栏上方；右侧 × 清除。
        pendingReply?.let { reply ->
            ReplyBar(
                message = reply,
                channelDisplayName = channel.displayName,
                onDismiss = { pendingReply = null },
            )
        }

        // UX-10：@ 提及选择器（仅群聊），锚定在输入栏上方。
        if (!channel.isDm) {
            val query = mentionQuery
            val filteredMembers = remember(query, groupMembersForChannel, currentUserId) {
                if (query == null) emptyList()
                else groupMembersForChannel
                    .asSequence()
                    .filter { it.userId != currentUserId }
                    .filter { query.isEmpty() || matchMemberQuery(it, query) }
                    .toList()
            }
            if (query != null && filteredMembers.isNotEmpty()) {
                MentionPicker(
                    members = filteredMembers,
                    onPick = { member ->
                        val displayName = member.remark.ifBlank { member.name }
                        val ins = replaceMentionQuery(inputText, displayName, member.userId)
                        inputText = ins.text
                        mentionSpans.add(ins.span)
                        mentionQuery = null
                    },
                )
            }
        }

        // 输入框
        MessageInputBar(
            text = inputText,
            onTextChange = { rawNewText ->
                // UX-10：把用户编辑与已有 mention 区间做 diff 合并——触碰到任一 span 时整段抹除，
                // 其它编辑保持不变；等价于 WeChat 的"pill 原子删除"但不需要富文本输入。
                val (newText, newSpans) = resolveMentionEdit(inputText, rawNewText, mentionSpans.toList())
                inputText = newText
                if (newSpans != mentionSpans.toList()) {
                    mentionSpans.clear()
                    mentionSpans.addAll(newSpans)
                }
                mentionQuery = computeMentionQuery(newText, channel.isDm)
                // 节流发送 typing：文本非空且距离上次发送超过 1 秒（与接收侧 5s 过期窗口对齐，
                // 确保用户持续输入时对端始终能收到心跳，不会因中间某次发送被延迟而误判停止）
                if (newText.isNotBlank()) {
                    val now = currentTimeMillis()
                    if (now - lastTypingSentMs > 1_000L) {
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
                            listState.animateScrollToItem(currentMessages.size - 1)
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
                panelMode = InputPanelMode.NONE
                scope.launch {
                    try {
                        val result = onSendImage?.invoke(channel.channelId, channel.channelType) { label ->
                            if (label.isEmpty()) {
                                // SDK 告知文件+缩略图已就绪，关掉 loading；紧随其后的 DB 写入会 emit 气泡。
                                mediaPrepBusy = false
                            } else {
                                mediaPrepLabel = label
                                mediaPrepBusy = true
                            }
                        }
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
                    } finally {
                        mediaPrepBusy = false
                    }
                }
            },
            onPickCamera = {
                panelMode = InputPanelMode.NONE
                scope.launch {
                    try {
                        val result = onSendCamera?.invoke(channel.channelId, channel.channelType) { label ->
                            if (label.isEmpty()) {
                                mediaPrepBusy = false
                            } else {
                                mediaPrepLabel = label
                                mediaPrepBusy = true
                            }
                        }
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
                    } finally {
                        mediaPrepBusy = false
                    }
                }
            },
            onPickFile = {
                panelMode = InputPanelMode.NONE
                scope.launch {
                    try {
                        val result = onSendFile?.invoke(channel.channelId, channel.channelType) { label ->
                            if (label.isEmpty()) {
                                mediaPrepBusy = false
                            } else {
                                mediaPrepLabel = label
                                mediaPrepBusy = true
                            }
                        }
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
                    } finally {
                        mediaPrepBusy = false
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
                    val replyTargetServerId = pendingReply?.serverMessageId
                    val mentionUserIds = mentionSpans.map { it.userId }.distinct()
                    val needOptions = replyTargetServerId != null || mentionUserIds.isNotEmpty()
                    inputText = ""
                    mentionQuery = null
                    mentionSpans.clear()
                    pendingReply = null
                    // UX-9：成功调用发送路径后立刻清掉持久草稿；onDispose 只在退出会话时兜底。
                    PrivChat.saveDraft(channel.channelId, null)
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
                            // 仅当带回复 / @ 时走 options 路径（SDK 直接发，不经 onSendText 拦截）。
                            // sendTextMessage 内部会先插入 optimistic 消息到 UI，再异步 FFI
                            val result = if (needOptions) {
                                withContext(Dispatchers.Default) {
                                    PrivChat.client.sendText(
                                        channel.channelId,
                                        channel.channelType,
                                        text,
                                        com.netonstream.privchat.sdk.dto.SendMessageOptions(
                                            inReplyToMessageId = replyTargetServerId,
                                            mentions = mentionUserIds,
                                        ),
                                    )
                                }
                            } else {
                                onSendText?.invoke(channel.channelId, channel.channelType, text)
                                    ?: withContext(Dispatchers.Default) {
                                        PrivChat.client.sendText(channel.channelId, channel.channelType, text)
                                    }
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
                            listState.animateScrollToItem(lastIndex)
                        }
                    }
                }
            },
            replyPending = pendingReply != null,
        )
    }
    Dialog.Host(visible = mediaPrepBusy, dismissOnOutside = false) {
        Box(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            com.gearui.components.loading.Loading(
                text = mediaPrepLabel.ifBlank { "处理中…" },
            )
        }
    }
    // UX-3 / UX-4：文本内联实体点击后的 ActionSheet 通过全局单例弹出，
    // 必须有一个 Host 挂载在页面根部才能接收显示请求。
    ActionSheet.Host()
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
    onAvatarLongPress: ((ULong, String) -> Unit)? = null,
    peerReadPts: ULong? = null,
    reactions: List<com.netonstream.privchat.sdk.dto.ReactionChip> = emptyList(),
    selfUserId: ULong? = null,
    onRequestForward: ((MessageEntry) -> Unit)? = null,
    onVideoPreview: ((MessageEntry) -> Unit)? = null,
    onImagePreview: ((MessageEntry) -> Unit)? = null,
    onReply: ((MessageEntry) -> Unit)? = null,
    replyLookup: ((String) -> MessageEntry?)? = null,
    senderLabelOf: ((ULong) -> String)? = null,
    onReplyClick: ((MessageEntry) -> Unit)? = null,
    isHighlighted: Boolean = false,
) {
    val colors = Theme.colors
    val strings = PrivChatI18n.strings
    val parsed = message.parsedContent
    val scope = rememberCoroutineScope()
    var showRetryDialog by remember(message.id) { mutableStateOf(false) }
    val onFailedClick: (() -> Unit)? = if (isSelf && message.status == MessageStatus.Failed) {
        { showRetryDialog = true }
    } else {
        null
    }

    if (showRetryDialog) {
        ConfirmDialog(
            visible = true,
            title = "重新发送",
            message = "是否重新发送这条消息？",
            confirmText = "重新发送",
            cancelText = "取消",
            onConfirm = {
                showRetryDialog = false
                scope.launch {
                    withContext(Dispatchers.Default) {
                        PrivChat.client.retryMessage(message.id)
                    }.onFailure { error ->
                        Toast.error(error.message ?: strings.networkError)
                    }
                }
            },
            onCancel = { showRetryDialog = false },
        )
    }

    // 撤回 / 系统消息走整行居中布局；其余走常规气泡。
    if (message.renderType() != RenderType.BUBBLE) {
        val ctx = MessageActionPolicy.Context(
            message = message,
            isSelf = isSelf,
            nowMs = currentTimeMillis(),
        )
        if (MessageActionPolicy.isMenuAvailable(ctx)) {
            // 撤回消息：允许长按本地删除（Policy 只会返回 DeleteLocal 一项）。
            // 外层 Box 保证系统气泡始终水平居中（MessageActionsWrapper 自身有 widthIn 限制）。
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                MessageActionsWrapper(message = message, isSelf = isSelf, onRequestForward = onRequestForward, onReply = onReply) {
                    SystemMessageRow(message = message)
                }
            }
        } else {
            SystemMessageRow(message = message)
        }
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
            val peerAvatarName = channelDisplayName.ifBlank { message.fromUid.toString() }
            val avatarModifier = if (onAvatarClick != null || onAvatarLongPress != null) {
                Modifier.pointerInput(message.id) {
                    detectTapGestures(
                        onTap = { onAvatarClick?.invoke(message.fromUid) },
                        onLongPress = { onAvatarLongPress?.invoke(message.fromUid, peerAvatarName) },
                    )
                }
            } else {
                Modifier
            }
            Box(modifier = avatarModifier) {
                ChatAvatar(
                    url = null, // TODO: 从用户信息获取
                    name = peerAvatarName,
                    size = AvatarSpecs.Size.small,
                )
            }
            HorizontalSpacer(8.dp)
        }

        // 消息气泡（长按弹出动作菜单） + reactions
        Column(
            horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start,
        ) {
            // REPLY_SPEC §4.3：命中高亮时在气泡之上叠加一层主题色蒙版，800ms 后淡出。
            val flashAlpha by animateFloatAsState(
                targetValue = if (isHighlighted) 0.25f else 0f,
                animationSpec = tween(durationMillis = 220),
            )
            val bubbleShape = RoundedCornerShape(
                topStart = if (isSelf) 16.dp else 4.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = if (isSelf) 4.dp else 16.dp,
            )
            MessageActionsWrapper(message = message, isSelf = isSelf, onRequestForward = onRequestForward, onReply = onReply) {
                Box(
                    modifier = Modifier
                        .clip(bubbleShape)
                        .background(if (isSelf) colors.bubbleSelf else colors.bubbleOther),
                ) {
                    Column {
                        message.replyToServerMessageId?.let { replyId ->
                            val original = replyLookup?.invoke(replyId)
                            ReplyQuoteBanner(
                                original = original,
                                isSelf = isSelf,
                                senderLabelOf = senderLabelOf,
                                onClick = if (original != null && onReplyClick != null) {
                                    { onReplyClick.invoke(original) }
                                } else null,
                            )
                        }
                        MessageContent(
                            message = message,
                            isSelf = isSelf,
                            peerReadPts = peerReadPts,
                            onFailedClick = onFailedClick,
                            onVideoPreview = onVideoPreview,
                            onImagePreview = onImagePreview,
                        )
                    }
                    if (flashAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(bubbleShape)
                                .alpha(flashAlpha)
                                .background(colors.primary),
                        )
                    }
                }
            }
            if (reactions.isNotEmpty()) {
                VerticalSpacer(4.dp)
                MessageReactionsRow(
                    message = message,
                    reactions = reactions,
                    selfUserId = selfUserId,
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
 * 气泡下方的 reaction chips 行：按 emoji 聚合为胶囊芯片，
 * 显示表情+数量；点击切换自己的反应（已反应则取消，未反应则添加）。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageReactionsRow(
    message: MessageEntry,
    reactions: List<com.netonstream.privchat.sdk.dto.ReactionChip>,
    selfUserId: ULong?,
) {
    val colors = Theme.colors
    val strings = PrivChatI18n.strings
    val scope = rememberCoroutineScope()

    suspend fun refreshReactions() {
        val updated = withContext(Dispatchers.Default) {
            PrivChat.client.reactions(message.channelId, message.id)
        }
        updated.onSuccess { PrivChat.setMessageReactions(message.id, it) }
    }

    FlowRow(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .widthIn(max = 260.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        reactions.forEach { chip ->
            val selfReacted = selfUserId != null && chip.userIds.contains(selfUserId)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceVariant)
                    .clickable {
                        scope.launch {
                            val result = withContext(Dispatchers.Default) {
                                if (selfReacted) {
                                    PrivChat.client.removeReaction(message.id, chip.emoji)
                                } else {
                                    PrivChat.client.addReaction(message.id, chip.emoji)
                                }
                            }
                            result.onSuccess { refreshReactions() }
                                .onFailure { error ->
                                    Toast.error(error.message ?: strings.networkError)
                                }
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = chip.emoji,
                    style = Typography.BodySmall,
                    color = colors.textPrimary,
                )
                Text(
                    text = chip.count.toString(),
                    style = Typography.Label,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

/**
 * 输入状态指示器
 *
 * 显示"对方正在输入..."提示，自动过滤已过期的 typing 事件
 */
@Composable
private fun TypingBubble(channelId: ULong, peerName: String) {
    val typingMap by PrivChat.typingUserIds.collectAsState()
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(typingMap) {
        if (typingMap[channelId]?.isNotEmpty() == true) {
            delay(5_000)
            tick++
        }
    }
    val activeUsers = remember(typingMap, tick) { PrivChat.activeTypingUsers(channelId) }

    if (activeUsers.isEmpty()) {
        VerticalSpacer(4.dp)
        return
    }

    val colors = Theme.colors

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
            name = peerName.ifBlank { "?" },
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
 * 消息分组分隔线（UX-11 时间合并 + UX-5 跨日日期分隔线）。
 *
 * 规则：
 * - 首条消息或跨自然日 → "今天 HH:mm" / "昨天 HH:mm" / "M月d日 HH:mm" / "yyyy年M月d日 HH:mm"
 * - 同日 + 与上一条间隔 > 5 分钟 → "HH:mm"
 * - 同日 + 间隔 ≤ 5 分钟 → 不渲染
 */
@Composable
private fun MessageGroupDivider(previous: MessageEntry?, current: MessageEntry) {
    val prevTs = previous?.timestamp?.toLong() ?: 0L
    val currTs = current.timestamp.toLong()
    val crossDay = previous == null || !Formatter.isSameLocalDay(prevTs, currTs)
    val label = when {
        crossDay -> Formatter.messageSeparatorTime(currTs)
        (currTs - prevTs) > 5 * 60_000L -> Formatter.messageTime(currTs)
        else -> null
    } ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            style = Typography.Caption,
            color = Theme.colors.textSecondary,
        )
    }
}

/**
 * 浮动日期胶囊（UX-5）。
 *
 * 显示于消息列表顶部，跟随首条可见消息的日期更新；停止滚动 1.5 秒后淡出。
 */
@Composable
private fun FloatingDateHeader(label: String) {
    val colors = Theme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.textSecondary.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = Typography.Caption,
            color = Color.White,
        )
    }
}

/**
 * 未读消息分隔线（UX-7）。
 * 居中灰色横条 + 文字：`── 以下为未读消息 (N) ──`。
 */
@Composable
private fun UnreadDivider(count: Int) {
    val colors = Theme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .background(colors.textSecondary.copy(alpha = 0.35f))
                .padding(vertical = 0.5.dp),
        )
        HorizontalSpacer(8.dp)
        Text(
            text = "以下为未读消息 ($count)",
            style = Typography.Caption,
            color = colors.textSecondary,
        )
        HorizontalSpacer(8.dp)
        Box(
            modifier = Modifier
                .weight(1f)
                .background(colors.textSecondary.copy(alpha = 0.35f))
                .padding(vertical = 0.5.dp),
        )
    }
}

/**
 * 新消息浮动气泡（UX-8）。
 * 用户在历史区滚动时显示，点击回到底部并清空计数。
 */
@Composable
private fun NewMessagesBubble(count: Int, onClick: () -> Unit) {
    val colors = Theme.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.dp, colors.textSecondary.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$count 条新消息",
            style = Typography.Label,
            color = colors.primary,
        )
        HorizontalSpacer(6.dp)
        Icon(
            name = Icons.keyboard_arrow_down,
            tint = colors.primary,
            size = IconSizes.Default.medium,
        )
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
    replyPending: Boolean = false,
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
    // REPLY_SPEC §4.3：进入回复态后自动聚焦文本输入并弹键盘；语音模式不触发，避免打断录音体验。
    LaunchedEffect(replyPending) {
        if (replyPending && !voiceMode) {
            pendingAutoFocus = true
        }
    }
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
                        onClick = { onSend() },
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

/**
 * 给消息气泡包一层长按菜单：统一处理 Policy 查询、回调 dispatch、reaction 展开。
 *
 * 撤回消息走 [SystemMessageRow]、正常气泡走常规渲染路径，
 * 两条路径共用同一套 action dispatcher 逻辑，因此抽到这个 helper。
 */
@Composable
private fun MessageActionsWrapper(
    message: MessageEntry,
    isSelf: Boolean,
    onRequestForward: ((MessageEntry) -> Unit)? = null,
    onReply: ((MessageEntry) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val strings = PrivChatI18n.strings
    val scope = rememberCoroutineScope()
    val ctx = MessageActionPolicy.Context(
        message = message,
        isSelf = isSelf,
        nowMs = currentTimeMillis(),
    )
    val menuActions = MessageActionPolicy.menuActions(ctx).map { kind ->
        kind.toMessageAction(message) {
            when (kind) {
                MessageActionKind.Copy -> {
                    when (message.contentType()) {
                        ContentMessageType.TEXT -> {
                            val text = message.parsedContent.text.orEmpty()
                            if (text.isNotEmpty()) {
                                ClipboardBridge.setText(text)
                                Toast.success("已复制")
                            }
                        }
                        ContentMessageType.LINK -> {
                            val url = message.parsedContent.linkUrl
                                ?: message.parsedContent.text.orEmpty()
                            if (url.isNotEmpty()) {
                                ClipboardBridge.setText(url)
                                Toast.success("已复制")
                            }
                        }
                        else -> { /* Policy 不会派发到其他类型 */ }
                    }
                }
                MessageActionKind.SaveImage -> Toast.show("保存图片功能即将支持")
                MessageActionKind.Recall -> {
                    scope.launch {
                        if (message.status == MessageStatus.Failed) {
                            withContext(Dispatchers.Default) {
                                PrivChat.client.deleteMessageLocal(message.id)
                            }.onSuccess { PrivChat.removeMessage(message.id) }
                                .onFailure { error ->
                                    Toast.error(error.message ?: strings.networkError)
                                }
                        } else {
                            withContext(Dispatchers.Default) {
                                PrivChat.client.revokeMessage(message.id)
                            }.onFailure { error ->
                                Toast.error(error.message ?: strings.networkError)
                            }
                        }
                    }
                }
                MessageActionKind.DeleteLocal -> {
                    scope.launch {
                        withContext(Dispatchers.Default) {
                            PrivChat.client.deleteMessageLocal(message.id)
                        }.onSuccess { PrivChat.removeMessage(message.id) }
                            .onFailure { error ->
                                Toast.error(error.message ?: strings.networkError)
                            }
                    }
                }
                MessageActionKind.Forward -> {
                    val handler = onRequestForward
                    if (handler != null) {
                        handler(message)
                    } else {
                        Toast.show("转发功能即将支持")
                    }
                }
                MessageActionKind.Reply -> {
                    val handler = onReply
                    if (handler != null) handler(message) else Toast.show("回复功能即将支持")
                }
                MessageActionKind.Select -> Toast.show("多选功能即将支持")
            }
        }
    }

    val canReact = MessageActionPolicy.canReact(ctx)
    val reactions = if (canReact) DefaultMessageReactions else emptyList()
    val onReaction: ((String) -> Unit)? = if (canReact) {
        { emoji ->
            scope.launch {
                val result = withContext(Dispatchers.Default) {
                    PrivChat.client.addReaction(message.id, emoji)
                }
                result.onSuccess {
                    // 刷新单条消息的 reactions 列表，驱动气泡下方 chips 更新。
                    withContext(Dispatchers.Default) {
                        PrivChat.client.reactions(message.channelId, message.id)
                    }.onSuccess { chips -> PrivChat.setMessageReactions(message.id, chips) }
                }.onFailure { error ->
                    Toast.error(error.message ?: strings.networkError)
                }
            }
        }
    } else {
        null
    }

    MessageActionsMenu(
        actions = menuActions,
        modifier = Modifier.widthIn(max = 260.dp),
        reactions = reactions,
        onReaction = onReaction,
        isSelf = isSelf,
        pointerInputKey = message.id,
        bubble = content,
    )
}

/**
 * 将 Policy 产出的 [MessageActionKind] 映射为带文案 / icon / 回调的 [MessageAction]。
 *
 * 文案对 pending/sending 消息会改用"取消发送"以贴合语义（DeleteLocal 枚举同时承担"本地删除"和
 * "取消发送"两种状态，靠 [MessageEntry.status] 区分）。
 */
private fun MessageActionKind.toMessageAction(
    message: MessageEntry,
    onClick: () -> Unit,
): MessageAction = when (this) {
    MessageActionKind.Reply ->
        MessageAction(label = "回复", icon = Icons.reply, onClick = onClick)
    MessageActionKind.Copy ->
        MessageAction(label = "复制文字", icon = Icons.content_copy, onClick = onClick)
    MessageActionKind.SaveImage ->
        MessageAction(label = "保存图片", icon = Icons.download, onClick = onClick)
    MessageActionKind.Recall ->
        MessageAction(label = "撤回", icon = Icons.autorenew, onClick = onClick)
    MessageActionKind.Forward ->
        MessageAction(label = "转发", icon = Icons.forward, onClick = onClick)
    MessageActionKind.DeleteLocal -> {
        val label = when (message.status) {
            MessageStatus.Pending, MessageStatus.Sending -> "取消发送"
            else -> "本地删除"
        }
        MessageAction(label = label, icon = Icons.delete, danger = true, onClick = onClick)
    }
    MessageActionKind.Select ->
        MessageAction(label = "选择", icon = Icons.check_box_outline_blank, onClick = onClick)
}

// ==================== REPLY_SPEC 辅助 ====================

/**
 * REPLY_SPEC §4.2：按内容类型生成回复态摘要文案。
 * 撤回状态优先兜底（直接显示"该消息已撤回"）。
 */
private fun summarizeForReply(message: MessageEntry): String {
    if (message.isRevoked) return "该消息已撤回"
    return when (message.contentType()) {
        ContentMessageType.TEXT -> {
            val t = message.parsedContent.text.orEmpty()
            if (t.length > 40) t.take(40) + "…" else t
        }
        ContentMessageType.IMAGE -> "[图片]"
        ContentMessageType.VIDEO -> "[视频]"
        ContentMessageType.VOICE -> {
            val secs = message.parsedContent.duration
            if (secs != null && secs > 0) "[语音 ${secs}s]" else "[语音]"
        }
        ContentMessageType.FILE -> {
            val name = message.parsedContent.fileName.orEmpty()
            if (name.isNotBlank()) "[文件] $name" else "[文件]"
        }
        ContentMessageType.LINK -> message.parsedContent.linkTitle
            ?: message.parsedContent.linkUrl
            ?: "[链接]"
        ContentMessageType.STICKER -> "[表情]"
        ContentMessageType.CONTACT_CARD -> "[联系人]"
        ContentMessageType.LOCATION -> "[位置]"
        ContentMessageType.FORWARD -> "[转发]"
        ContentMessageType.SYSTEM, null -> "[消息]"
    }
}

/**
 * REPLY_SPEC §4.1：输入栏上方的引用摘要窄条。
 * 左侧 2px 主题色竖条 + 两行摘要；右侧 × 清除按钮。
 */
@Composable
private fun ReplyBar(
    message: MessageEntry,
    channelDisplayName: String,
    onDismiss: () -> Unit,
) {
    val colors = Theme.colors
    val senderLabel = channelDisplayName.ifBlank { message.fromUid.toString() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(32.dp)
                .background(colors.primary),
        )
        HorizontalSpacer(8.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "回复 $senderLabel",
                style = Typography.Label,
                color = colors.textSecondary,
            )
            Text(
                text = summarizeForReply(message),
                style = Typography.BodySmall,
                color = colors.textPrimary,
            )
        }
        HorizontalSpacer(8.dp)
        Box(
            modifier = Modifier
                .size(24.dp)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(name = Icons.close, size = 16.dp, tint = colors.textSecondary)
        }
    }
}

/**
 * REPLY_SPEC §4.3：气泡顶部的引用窄条。
 * 2px 主题色竖条 + 两行：第一行被引用方名字，第二行摘要（原消息缺失时降级为占位文案）。
 */
@Composable
private fun ReplyQuoteBanner(
    original: MessageEntry?,
    isSelf: Boolean,
    senderLabelOf: ((ULong) -> String)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = Theme.colors
    val summary = original?.let { summarizeForReply(it) } ?: "该消息已失效"
    val senderText = original?.let { senderLabelOf?.invoke(it.fromUid) ?: it.fromUid.toString() }
    val foreground = if (isSelf) colors.onPrimary else colors.textPrimary
    val secondary = if (isSelf) colors.onPrimary else colors.textSecondary
    val rowModifier = Modifier
        .padding(start = 10.dp, end = 10.dp, top = 8.dp)
        .fillMaxWidth()
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(if (senderText != null) 30.dp else 16.dp)
                .background(if (isSelf) colors.onPrimary else colors.primary),
        )
        HorizontalSpacer(6.dp)
        Column(modifier = Modifier.weight(1f)) {
            if (senderText != null) {
                Text(
                    text = senderText,
                    style = Typography.Label,
                    color = secondary,
                )
            }
            Text(
                text = summary,
                style = Typography.BodySmall,
                color = foreground,
            )
        }
    }
}

// ==================== UX-10 @ 提及工具函数 ====================

/**
 * 一条 `@name ` 的区间记录；`end` 为 exclusive，涵盖尾随空格，用于原子删除和偏移追踪。
 */
private data class MentionSpan(val start: Int, val end: Int, val userId: ULong)

/** 一次插入操作的产出：更新后的文本与新增 span。*/
private data class MentionInsertion(val text: String, val span: MentionSpan)

/**
 * 从当前输入文本末尾推断 @ 提及查询串：最后一个 `@` 必须位于行首或紧邻空白后，
 * 且其后的子串中不含空白；否则视作非提及上下文（例如邮箱 `a@b`）。
 */
private fun computeMentionQuery(text: String, isDm: Boolean): String? {
    if (isDm) return null
    val atIdx = text.lastIndexOf('@')
    if (atIdx < 0) return null
    if (atIdx > 0 && !text[atIdx - 1].isWhitespace()) return null
    val query = text.substring(atIdx + 1)
    if (query.any { it.isWhitespace() }) return null
    return query
}

/** 把输入尾部的 `@query` 片段替换为 `@<name> `（保留触发符，便于对方阅读）。*/
private fun replaceMentionQuery(text: String, name: String, userId: ULong): MentionInsertion {
    val atIdx = text.lastIndexOf('@')
    val prefix = if (atIdx < 0) text else text.substring(0, atIdx)
    val newText = "$prefix@$name "
    val spanStart = prefix.length
    return MentionInsertion(newText, MentionSpan(spanStart, newText.length, userId))
}

/** 头像长按直接追加 `@name `；若输入框末尾非空白，先补一个空格。*/
private fun appendMention(text: String, name: String, userId: ULong): MentionInsertion {
    val prefix = if (text.isEmpty() || text.last().isWhitespace()) text else "$text "
    val newText = "$prefix@$name "
    return MentionInsertion(newText, MentionSpan(prefix.length, newText.length, userId))
}

/**
 * 把用户编辑后的文本与旧文本/旧 span 做 diff 合并：
 * - 编辑未触碰任何 span → 原样应用，仅按增量偏移后续 span。
 * - 编辑落在 span 区间内（哪怕只咬了一口）→ 把整段 span 从 *旧文本* 中摘掉，
 *   本次用户的局部编辑一并丢弃；产生"一次 backspace 擦除整条 @mention"的手感。
 *
 * 之所以用 diff 而不是依赖光标位置，是因为 AutoResizeTextarea 只吐 `String`；
 * 只能靠新旧文本的公共前后缀推断变更区间。覆盖"末尾退格"主场景足矣。
 */
private fun resolveMentionEdit(
    oldText: String,
    newText: String,
    oldSpans: List<MentionSpan>,
): Pair<String, List<MentionSpan>> {
    if (oldText == newText) return newText to oldSpans
    val minLen = minOf(oldText.length, newText.length)
    var p = 0
    while (p < minLen && oldText[p] == newText[p]) p++
    var s = 0
    while (s < minLen - p && oldText[oldText.length - 1 - s] == newText[newText.length - 1 - s]) s++
    val changeEndOld = oldText.length - s
    val delta = newText.length - oldText.length
    val damaged = oldSpans.filter { it.end > p && it.start < changeEndOld }
    if (damaged.isEmpty()) {
        val shifted = oldSpans.map { span ->
            if (span.end <= p) span
            else MentionSpan(span.start + delta, span.end + delta, span.userId)
        }
        return newText to shifted
    }
    var output = oldText
    for (span in damaged.sortedByDescending { it.start }) {
        output = output.removeRange(span.start, span.end)
    }
    val survivors = oldSpans
        .filter { it !in damaged }
        .map { span ->
            val removedBefore = damaged
                .filter { it.end <= span.start }
                .sumOf { it.end - it.start }
            MentionSpan(span.start - removedBefore, span.end - removedBefore, span.userId)
        }
    return output to survivors
}

/** 在备注/昵称上做前缀匹配（忽略大小写）。*/
private fun matchMemberQuery(member: GroupMemberEntry, query: String): Boolean {
    val q = query.lowercase()
    return member.name.lowercase().contains(q) || member.remark.lowercase().contains(q)
}

/**
 * @ 提及选择器：垂直列表锚定在输入栏上方。
 *
 * 列表高度受限，支持滚动；每项点击后由父级替换输入文本并关闭 picker。
 */
@Composable
private fun MentionPicker(
    members: List<GroupMemberEntry>,
    onPick: (GroupMemberEntry) -> Unit,
) {
    val colors = Theme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .background(colors.surface)
            .border(width = 1.dp, color = colors.divider, shape = RoundedCornerShape(0.dp)),
    ) {
        ScrollView(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                members.forEach { member ->
                    val displayName = member.remark.ifBlank { member.name }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(member) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ChatAvatar(
                            url = member.avatar.takeIf { it.isNotBlank() },
                            name = displayName,
                            size = AvatarSpecs.Size.small,
                        )
                        HorizontalSpacer(10.dp)
                        Text(
                            text = displayName,
                            style = Typography.BodyMedium,
                            color = colors.textPrimary,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.divider),
                    )
                }
            }
        }
    }
}
