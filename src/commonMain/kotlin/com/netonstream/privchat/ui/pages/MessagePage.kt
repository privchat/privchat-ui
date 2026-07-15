package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import com.netonstream.privchat.sdk.ConnectionState
import com.netonstream.privchat.sdk.dto.ChannelListEntry
import com.netonstream.privchat.ui.error.UserFacingError
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
import com.netonstream.privchat.ui.media.MediaDownloadManager
import com.netonstream.privchat.ui.media.MediaDownloadState
import com.netonstream.privchat.ui.media.MediaSaver
import com.netonstream.privchat.ui.platform.ClipboardBridge
import com.tencent.kuikly.compose.foundation.gestures.detectTapGestures
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.offlineStatus
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.onlineStatus
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.messageBubbleOther
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.messageBubbleSelf
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.messageTextOther
import com.netonstream.privchat.ui.utils.Formatter
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.theme.Theme
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.primitives.ScrollView
import com.gearui.foundation.typography.IconSizes
import com.gearui.foundation.typography.Typography
import com.gearui.foundation.avatar.AvatarSizeTokens
import com.gearui.primitives.HorizontalSpacer
import com.gearui.primitives.VerticalSpacer
import com.tencent.kuikly.compose.ui.unit.Dp
import com.gearui.primitives.composite.Card
import com.gearui.components.navbar.NavBar
import com.gearui.components.navbar.NavBarItem
import com.gearui.components.contextmenu.ContextMenu
import com.gearui.components.contextmenu.ContextMenuItem
import com.gearui.components.icon.Icons
import com.gearui.components.popover.PopoverPlacement
import com.gearui.foundation.primitives.Icon
import com.gearui.components.input.Input
import com.gearui.components.input.InputSize
import com.gearui.components.textarea.AutoResizeTextarea
import com.gearui.components.button.Button
import com.gearui.components.button.ButtonTheme
import com.gearui.components.button.ButtonSize
import com.gearui.components.empty.EmptyState
import com.gearui.components.actionsheet.ActionSheet
import com.gearui.components.actionsheet.ActionSheetItem
import com.netonstream.privchat.sdk.dto.BotMenu
import com.netonstream.privchat.sdk.dto.BotMenuAction
import com.netonstream.privchat.sdk.dto.BotMenuItem
import com.netonstream.privchat.sdk.dto.SendMessageOptions
import com.netonstream.privchat.ui.platform.ExternalLinkBridge
import com.netonstream.privchat.ui.utils.BotMenuController
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.gearui.components.dialog.ConfirmDialog
import com.gearui.components.dialog.Dialog
import com.gearui.components.toast.Toast
import com.gearui.components.swiper.Swiper
import com.gearui.components.swiper.SwiperNavigation
import com.gearui.components.swiper.SwiperIndicatorPosition
import com.gearui.runtime.LocalRuntimeEnvironment
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
import com.tencent.kuikly.compose.ui.text.LinkAnnotation
import com.tencent.kuikly.compose.ui.text.LinkInteractionListener
import com.tencent.kuikly.compose.ui.text.SpanStyle
import com.tencent.kuikly.compose.ui.text.TextLinkStyles
import com.tencent.kuikly.compose.ui.text.buildAnnotatedString
import com.tencent.kuikly.compose.ui.text.withLink
import com.tencent.kuikly.compose.material3.Text as KuiklyText
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
 * 拉 / 弹 bot 菜单。BOT_INTERACTION_SPEC §3.2-§3.3：菜单获取走 transfer，
 * 命中缓存秒开；失败 toast 不阻塞聊天。
 */
private fun showBotMenu(
    channel: ChannelListEntry,
    scope: kotlinx.coroutines.CoroutineScope,
    onError: ((String) -> Unit)?,
) {
    BotMenuController.getCached(channel.channelId)?.let { menu ->
        presentBotMenuSheet(channel, menu, scope, onError)
        return
    }
    Toast.show("加载菜单…")
    scope.launch {
        BotMenuController.loadOrCached(
            channelId = channel.channelId,
            scope = scope,
        ) { route, body, timeoutMs ->
            withContext(Dispatchers.Default) {
                PrivChat.client.transfer(channel.channelId, route, body, timeoutMs)
            }
        }.fold(
            onSuccess = { menu -> presentBotMenuSheet(channel, menu, scope, onError) },
            onFailure = { e ->
                onError?.invoke("菜单加载失败：${e.message ?: "未知错误"}")
            },
        )
    }
}

private fun presentBotMenuSheet(
    channel: ChannelListEntry,
    menu: BotMenu,
    scope: kotlinx.coroutines.CoroutineScope,
    onError: ((String) -> Unit)?,
) {
    if (menu.items.isEmpty()) {
        Toast.show("当前会话没有可用菜单")
        return
    }
    ActionSheet.showList(
        items = menu.items.map { ActionSheetItem(label = it.title) },
        onSelected = { _, index ->
            val item = menu.items.getOrNull(index) ?: return@showList
            dispatchBotMenuAction(item, channel, scope, onError)
        },
    )
}

/**
 * BOT_INTERACTION_SPEC §4：menu action 三类分发。
 * 严格按 spec 强约束：transfer 不入 timeline；message 走 SendMessage；web 打开 URL。
 */
private fun dispatchBotMenuAction(
    item: BotMenuItem,
    channel: ChannelListEntry,
    scope: kotlinx.coroutines.CoroutineScope,
    onError: ((String) -> Unit)?,
) {
    when (val action = item.action) {
        is BotMenuAction.Transfer -> dispatchTransferAction(item, action, channel, scope, onError)
        is BotMenuAction.Message -> dispatchMessageAction(item, action, channel, scope, onError)
        is BotMenuAction.Web -> dispatchWebAction(action, channel, scope, onError)
    }
}

private val botActionJson = Json { ignoreUnknownKeys = true }

private fun dispatchTransferAction(
    item: BotMenuItem,
    action: BotMenuAction.Transfer,
    channel: ChannelListEntry,
    scope: kotlinx.coroutines.CoroutineScope,
    onError: ((String) -> Unit)?,
) {
    if (!action.route.startsWith("bot/")) {
        onError?.invoke("非法 transfer route：${action.route}")
        return
    }
    val bodyBytes = action.body
        ?.let { botActionJson.encodeToString(JsonObject.serializer(), it).encodeToByteArray() }
        ?: ByteArray(0)
    Toast.show("处理中…")
    scope.launch {
        val result = withContext(Dispatchers.Default) {
            PrivChat.client.transfer(channel.channelId, action.route, bodyBytes, 0u)
        }
        result.fold(
            onSuccess = { reply ->
                if (reply.isOk) {
                    val preview = reply.data.takeIf { it.isNotEmpty() }
                        ?.decodeToString()
                        ?.takeIf { it.length <= 200 }
                    Toast.success(preview ?: "已完成")
                } else {
                    onError?.invoke("[${reply.code}] ${reply.message.ifBlank { PrivChatI18n.current.operationFailed }}")
                }
            },
            onFailure = { e ->
                onError?.invoke("调用失败：${e.message ?: "未知错误"}")
            },
        )
    }
}

private fun dispatchMessageAction(
    item: BotMenuItem,
    action: BotMenuAction.Message,
    channel: ChannelListEntry,
    scope: kotlinx.coroutines.CoroutineScope,
    onError: ((String) -> Unit)?,
) {
    val text = action.text.ifBlank {
        onError?.invoke("菜单消息内容为空：${item.id}")
        return
    }
    // BOT_INTERACTION_SPEC §5.1：from_menu / menu_item_id / command 必须透传到
    // SendMessageRequest.metadata；这里走 extraJson 进入 SDK，server 持久化后
    // 由 application 端 BotMessageEventHandler 解析。
    val extra = buildJsonObject {
        put("from_menu", JsonPrimitive(true))
        put("menu_item_id", JsonPrimitive(item.id))
        action.metadata?.forEach { (k, v) -> put(k, v) }
    }
    val options = SendMessageOptions(
        extraJson = botActionJson.encodeToString(JsonObject.serializer(), extra),
    )
    scope.launch {
        val result = withContext(Dispatchers.Default) {
            PrivChat.client.sendText(
                channel.channelId,
                channel.channelType,
                text,
                options,
            )
        }
        result.onFailure { e ->
            onError?.invoke("发送失败：${e.message ?: "未知错误"}")
        }
    }
}

private fun dispatchWebAction(
    action: BotMenuAction.Web,
    channel: ChannelListEntry,
    scope: kotlinx.coroutines.CoroutineScope,
    onError: ((String) -> Unit)?,
) {
    if (!action.url.startsWith("https://")) {
        // BOT_INTERACTION_SPEC §8.2：v1 强制 HTTPS。
        onError?.invoke("仅允许 HTTPS 链接")
        return
    }
    val prefetch = action.prefetchSignedUrlRoute
    if (prefetch.isNullOrBlank()) {
        if (!ExternalLinkBridge.openUri(action.url)) {
            onError?.invoke("无法打开链接")
        }
        return
    }
    if (!prefetch.startsWith("bot/")) {
        onError?.invoke("非法 prefetch route：$prefetch")
        return
    }
    // 先走 transfer 拿一次性 signed URL（reply.data 是 JSON {"url": "..."}）。
    Toast.show("准备中…")
    scope.launch {
        val result = withContext(Dispatchers.Default) {
            PrivChat.client.transfer(channel.channelId, prefetch, ByteArray(0), 0u)
        }
        result.fold(
            onSuccess = { reply ->
                if (!reply.isOk) {
                    onError?.invoke("[${reply.code}] ${reply.message.ifBlank { "签名失败" }}")
                    return@fold
                }
                val signed = runCatching {
                    botActionJson
                        .parseToJsonElement(reply.data.decodeToString())
                        .jsonObject["url"]
                        ?.jsonPrimitive
                        ?.contentOrNull
                }.getOrNull()
                val target = signed?.takeIf { it.startsWith("https://") } ?: action.url
                if (!ExternalLinkBridge.openUri(target)) {
                    onError?.invoke("无法打开链接")
                }
            },
            onFailure = { e ->
                onError?.invoke("准备链接失败：${e.message ?: "未知错误"}")
            },
        )
    }
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
    /** 会话内搜索：从「…」菜单进入，上层 push GlobalSearch(scopeChannel=当前会话)。 */
    onSearchMessages: (() -> Unit)? = null,
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
    onReportMessage: ((MessageEntry) -> Unit)? = null,
    onVideoPreview: ((MessageEntry) -> Unit)? = null,
    onImagePreview: ((MessageEntry) -> Unit)? = null,
    // Money Message（PLATFORM-only）：非空才在 + 菜单显示入口；BUILTIN 传 null 隐藏。
    // 宿主(App) money-first：先 platform /app/red-packet|money-transfer/send，再 SDK 发消息。
    onRedPacket: (() -> Unit)? = null,
    onMoneyTransfer: (() -> Unit)? = null,
    // 点红包卡片（传 redPacketId）→ 宿主打开红包详情/领取。null=只读降级。
    onRedPacketClick: ((String) -> Unit)? = null,
    // 点转账卡片（传 transferId）→ 宿主打开转账详情。
    onMoneyTransferClick: ((String) -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    /** 搜索命中跳转：以该 server_message_id 为锚打开会话（spec §5 jump-to-message） */
    initialFocusMessageId: ULong? = null,
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
    val runtimeEnv = LocalRuntimeEnvironment.current
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
    // 红包卡片实时状态：扫描会话内的领取/抢完/过期系统消息，推导每个红包的展示态。
    // 2=已抢完/过期、1=当前用户已领取；无匹配=0（可领取，回退卡片快照）。卡片据此显示「已领取/已被抢完」。
    val redPacketStatusMap = remember(sortedMessages, currentUserId) {
        val map = HashMap<String, Int>()
        val myId = currentUserId?.toString()
        for (m in sortedMessages) {
            val p = m.parsedContent
            if (p.type != MessageType.SYSTEM) continue
            val tpl = p.systemTemplate ?: continue
            val rpId = p.systemRefs?.firstOrNull { it.type == "red_packet" }?.targetId ?: continue
            when {
                tpl.contains("抢完") || tpl.contains("过期") -> map[rpId] = 2
                tpl.contains("领取") -> {
                    val claimer = p.systemRefs?.firstOrNull { it.type == "user" }?.targetId
                    if (myId != null && claimer == myId && (map[rpId] ?: 0) < 1) map[rpId] = 1
                }
            }
        }
        map
    }
    // peer_user_id 由 channel 同步直接下发并持久化到 channel row，UI 直接读取，
    // 不再回退 channel_member / dmPeerUserId() 推断。
    val peerUserId = channel.peerUserId
    // BOT_INTERACTION_SPEC §3.1：私聊对端 user_type ∈ {1=System, 2=Bot} 时显示菜单入口。
    // peerUserType 异步解析（getUserProfileLocalFirst → 本地优先，未知再拉服务端）。
    var peerUserType by remember(channel.channelId) { mutableStateOf<Short?>(null) }
    var initialPositioned by remember(channel.channelId) { mutableStateOf(false) }
    // spec §5：跳转模式下抑制"初始滚到底部"，直到 focus 定位完成或降级——否则
    // around 回灌 messages 触发的初始滚底会把 anchor 定位冲掉（消息多的会话尤其明显）。
    var jumpPending by remember(channel.channelId) { mutableStateOf(initialFocusMessageId != null) }
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
    // spec §5：跳转锚（本地行 id）。窗口装载后由下方 effect 消费：滚动 + 高亮。
    var pendingFocusLocalId by remember(channel.channelId) { mutableStateOf<ULong?>(null) }
    // spec §5：跳转窗口就绪后定位 anchor（复用 reply 的滚动+高亮原语），一次性消费。
    LaunchedEffect(messages, pendingFocusLocalId) {
        val focusId = pendingFocusLocalId ?: return@LaunchedEffect
        val idx = messages.indexOfFirst { it.id == focusId }
        if (idx >= 0) {
            listState.animateScrollToItem(idx)
            highlightMessageId = focusId
            pendingFocusLocalId = null
            // 已定位到 anchor：声明初始定位完成 + 解除滚底抑制，后续新消息走常规近底逻辑。
            initialPositioned = true
            jumpPending = false
        }
    }
    val allGroupMembers by PrivChat.groupMembers.collectAsState()
    val groupMembersForChannel = remember(allGroupMembers, channel.channelId) {
        if (channel.isDm) emptyList() else allGroupMembers.filter { it.channelId == channel.channelId }
    }
    // 群消息置顶：仅群主/管理员可操作（canPin），所有成员可见置顶条。
    // 当前用户在该群的角色（role: 2=群主 1=管理员 0=成员）。
    val canPinMessages = remember(groupMembersForChannel, currentUserId, channel.isDm) {
        if (channel.isDm) false
        else groupMembersForChannel.firstOrNull { it.userId == currentUserId }
            ?.let { it.isOwner || it.isAdmin } ?: false
    }
    // 已置顶消息列表（来自服务端 groupPinnedMessages，群聊 channelId == groupId）。
    var pinnedMessages by remember(channel.channelId) {
        mutableStateOf<List<com.netonstream.privchat.sdk.dto.GroupPinnedMessageView>>(emptyList())
    }
    val pinnedMessageIds = remember(pinnedMessages) { pinnedMessages.map { it.messageId }.toSet() }
    // 加载/刷新置顶列表（封装供初始加载与置顶操作后调用）。
    val refreshPinnedMessages: suspend () -> Unit = refresh@{
        if (channel.isDm) return@refresh
        withContext(Dispatchers.Default) {
            PrivChat.client.groupPinnedMessages(channel.channelId)
        }.onSuccess { pinnedMessages = it }
    }
    LaunchedEffect(channel.channelId) {
        if (!channel.isDm) refreshPinnedMessages()
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
        if (initialFocusMessageId != null) {
            // spec §5：搜索命中跳转——先 around 回填服务端上下文，再从本地按显示排序
            // 读窗口渲染（本地重查是渲染真源）；anchor 不可见（撤回/删除/越权）时降级
            // 为常规最近窗口并提示。
            val focusWindow = withContext(Dispatchers.Default) {
                PrivChat.client.getMessagesAround(
                    channel.channelId, channel.channelType, initialFocusMessageId, 25u, 25u,
                ).fold(
                    onSuccess = {
                        PrivChat.client.getLocalMessagesAround(
                            channel.channelId, channel.channelType, initialFocusMessageId,
                        ).getOrNull()
                    },
                    onFailure = { null },
                )
            }
            if (!focusWindow.isNullOrEmpty()) {
                PrivChat.updateMessages(channel.channelId, focusWindow)
                val anchorRow = focusWindow.firstOrNull { it.serverMessageId == initialFocusMessageId }
                if (anchorRow != null) {
                    pendingFocusLocalId = anchorRow.id
                } else {
                    jumpPending = false // 兜底：窗口里找不到锚，别卡住初始滚底
                }
            } else {
                jumpPending = false // anchor 不可见，降级常规窗口 → 恢复初始滚底
                onError?.invoke(strings.globalSearchAnchorMissing)
                val result = onLoadMessages?.invoke(channel.channelId, channel.channelType)
                    ?: withContext(Dispatchers.Default) {
                        PrivChat.client.getMessagesByType(channel.channelId, channel.channelType, 50u, null)
                    }
                result.onSuccess { list -> PrivChat.updateMessages(channel.channelId, list) }
            }
        } else {
            val result = onLoadMessages?.invoke(channel.channelId, channel.channelType)
                ?: withContext(Dispatchers.Default) {
                    PrivChat.client.getMessagesByType(channel.channelId, channel.channelType, 50u, null)
                }
            result.onSuccess { list ->
                PrivChat.updateMessages(channel.channelId, list)
            }
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
                // BOT_INTERACTION_SPEC §3.1：拉对端 user_type，决定输入栏菜单按钮是否显示。
                // 用 getUserProfileLocalFirst 而不是 listUsersByIds：前者本地没记录时会自动
                // 从 server 拉一次并 upsert 本地 users 表，避免 bot/system 等还没缓存的对端
                // 因为本地查空导致菜单按钮 / 昵称都缺失。
                runCatching {
                    withContext(Dispatchers.Default) {
                        PrivChat.client.getUserProfileLocalFirst(uid)
                            .getOrNull()
                            ?.let { peerUserType = it.userType }
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

    // 重连恢复（presence 是订阅态，不是轮询态）：server 重启 / 网络抖动后，服务端 subscribe
    // 注册表清空、对端 presence 可能错过 online 事件。客户端必须在重连后：① 重新 subscribe
    // 频道（恢复 presence_changed / typing 的 channel-published 通道）② 重新 fetch 一次对端
    // 当前 presence。不能依赖用户重新进会话。
    val connectionState by PrivChat.connectionState.collectAsState()
    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.Connected) {
            // 频道订阅恢复（typing + presence_changed 都走 channel publish）
            runCatching {
                withContext(Dispatchers.Default) {
                    PrivChat.client.subscribeChannel(channel.channelId, channel.channelType.toUByte())
                }
            }
            if (channel.isDm) {
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
    }

    // 低频兜底校准（非主路径）：presence 主要靠 server push presence_changed 实时更新，这里只是
    // 防御 push 丢失 / subscriber 注册漂移的弱兜底，60s 一次，不要降到 10/15s 当主机制。
    if (channel.isDm) {
        LaunchedEffect(channel.channelId) {
            while (true) {
                delay(60_000L)
                val uid = peerUserId ?: continue
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
            if (jumpPending) return@LaunchedEffect // 跳转模式：让 focus 定位接管，别滚到底部
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
    val keyboardVisible = runtimeEnv.keyboard.visible
    LaunchedEffect(keyboardVisible) {
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

    // 标题：名称部分 15 字截断；群聊在名称后追加成员数「名称 (N)」（人数不参与截断）
    val truncatedName = channel.displayName.let { if (it.length > 15) it.take(15) + "..." else it }
    // SDK ChannelListEntry.memberCount 目前无数据源(恒 0),用九宫格成员预览缓存兜底
    val groupCount = if (channel.isDm) 0
    else maxOf(channel.memberCount.toInt(), com.netonstream.privchat.ui.avatar.groupMemberPreviewCount(channel.channelId))
    val truncatedTitle = if (!channel.isDm && groupCount > 0) {
        "$truncatedName ($groupCount)"
    } else {
        truncatedName
    }
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
        // 顶部导航栏；点中央昵称：DM → 跳用户/好友资料（复用 onAvatarClick），群 → 群信息页（onProfileClick）
        // 右上 "…" 仍走 onProfileClick（保持原行为，作为"更多/设置"入口）
        val onTitleClick: () -> Unit = {
            if (channel.isDm) {
                peerUserId?.let { onAvatarClick?.invoke(it) }
            } else {
                onProfileClick()
            }
        }
        NavBar(
            title = "",
            useDefaultBack = true,
            onBackClick = onBack,
            titleWidget = {
                // presence 为真源：DM 标题下展示在线 / 「N 分钟前在线」相对时长（离线时用 muted 色）。
                val presenceText = if (channel.isDm) presenceStatusText(peerPresence, strings) else null
                Column(
                    modifier = Modifier.clickable(onClick = onTitleClick),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = truncatedTitle,
                        style = Typography.TitleMedium,
                        color = Theme.colors.foreground,
                    )
                    if (presenceText != null) {
                        Text(
                            text = presenceText,
                            style = Typography.Label,
                            color = if (peerPresence?.isOnline == true) {
                                Theme.colors.onlineStatus
                            } else {
                                Theme.colors.mutedForeground
                            },
                        )
                    }
                }
            },
            rightWidget = {
                // 「…」菜单：搜索聊天记录（会话内）+ 聊天/群详情（原 onProfileClick 入口）。
                ContextMenu(
                    placement = PopoverPlacement.BOTTOM_RIGHT,
                    items = buildList {
                        if (onSearchMessages != null) {
                            add(
                                ContextMenuItem(
                                    label = strings.globalSearchPlaceholder,
                                    icon = Icons.search,
                                    onClick = { onSearchMessages() },
                                )
                            )
                        }
                        add(
                            ContextMenuItem(
                                label = strings.chatSettingsTitle,
                                icon = Icons.info,
                                onClick = onProfileClick,
                            )
                        )
                    },
                ) { onOpen ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clickable(onClick = onOpen),
                    ) {
                        Icon(name = Icons.more_horiz, size = 24.dp, tint = Theme.colors.foreground)
                    }
                }
            },
        )
        networkStatusBar?.invoke()

        // 置顶条：群聊有置顶消息时展示最新一条预览；群主/管理员可一键取消置顶。
        // 不做跳转原消息（产品定稿）。普通成员只读。
        if (!channel.isDm && pinnedMessages.isNotEmpty()) {
            val latestPinned = pinnedMessages.first()
            val pinnedMsg = sortedMessages.firstOrNull {
                it.serverMessageId == latestPinned.messageId
            }
            PinnedMessagesBar(
                count = pinnedMessages.size,
                preview = pinnedMsg?.let {
                    strings.previewOf(it)
                } ?: strings.pinnedMessagesTitle,
                canManage = canPinMessages,
                onUnpin = {
                    scope.launch {
                        withContext(Dispatchers.Default) {
                            PrivChat.client.groupPinMessage(
                                groupId = channel.channelId,
                                channelId = channel.channelId,
                                messageId = latestPinned.messageId,
                                pinned = false,
                            )
                        }.onSuccess {
                            refreshPinnedMessages()
                            Toast.success(strings.messageUnpinSuccess)
                        }.onFailure { error ->
                            Toast.error(UserFacingError.message(error, strings.networkError))
                        }
                    }
                },
            )
        }

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
                                key = { sortedMessages[it].id.toLong() },
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
                                    redPacketStatusOf = { redPacketStatusMap[it] ?: 0 },
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
                                    onReportMessage = onReportMessage,
                                    onVideoPreview = onVideoPreview,
                                    onImagePreview = onImagePreview,
                                    onRedPacketClick = onRedPacketClick,
                                    onMoneyTransferClick = onMoneyTransferClick,
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
                                    canPin = canPinMessages,
                                    isPinned = pinnedMessageIds.contains(message.serverMessageId),
                                    onPinMessage = { target, pin ->
                                        val serverId = target.serverMessageId
                                        if (serverId == null) {
                                            Toast.error("原消息尚未发送")
                                        } else {
                                            scope.launch {
                                                withContext(Dispatchers.Default) {
                                                    // 群聊 channelId == groupId（同值）。
                                                    PrivChat.client.groupPinMessage(
                                                        groupId = channel.channelId,
                                                        channelId = channel.channelId,
                                                        messageId = serverId,
                                                        pinned = pin,
                                                    )
                                                }.onSuccess {
                                                    refreshPinnedMessages()
                                                    Toast.success(
                                                        if (pin) strings.messagePinSuccess
                                                        else strings.messageUnpinSuccess
                                                    )
                                                }.onFailure { error ->
                                                    Toast.error(UserFacingError.message(error, strings.networkError))
                                                }
                                            }
                                        }
                                    },
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
            systemSafeAreaBottom = runtimeEnv.safeArea.bottom,
            keyboardHeight = runtimeEnv.keyboard.height,
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
            onRedPacket = { onRedPacket?.invoke() },
            onMoneyTransfer = { onMoneyTransfer?.invoke() },
            moneyEnabled = onRedPacket != null,
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
            // BOT_INTERACTION_SPEC §3.1：DM 对端 user_type ∈ {1=System, 2=Bot} 时显示菜单按钮。
            showMenuButton = channel.isDm
                && peerUserType?.let { it == 1.toShort() || it == 2.toShort() } == true,
            onMenuClick = {
                showBotMenu(
                    channel = channel,
                    scope = scope,
                    onError = onError,
                )
            },
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

/**
 * 群置顶消息条：展示最新一条置顶消息预览（不做跳转）。
 * 群主/管理员（[canManage]）可点右侧图标一键取消置顶；普通成员只读。
 */
@Composable
private fun PinnedMessagesBar(
    count: Int,
    preview: String,
    canManage: Boolean,
    onUnpin: () -> Unit,
) {
    val colors = Theme.colors
    val strings = PrivChatI18n.strings
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(name = Icons.bookmark, size = IconSizes.Default.small, tint = colors.primary)
        HorizontalSpacer(8.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (count > 1) "${strings.pinnedMessagesTitle} ($count)" else strings.pinnedMessagesTitle,
                style = Typography.Label,
                color = colors.primary,
            )
            Text(
                text = preview,
                style = Typography.BodySmall,
                color = colors.foreground,
                maxLines = 1,
            )
        }
        if (canManage) {
            HorizontalSpacer(8.dp)
            Box(modifier = Modifier.clickable(onClick = onUnpin)) {
                Icon(name = Icons.close, size = IconSizes.Default.small, tint = colors.mutedForeground)
            }
        }
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
    // presence 为真源：离线时展示「N 分钟/小时/天前在线」相对时长（< 1 分钟回退到「最近在线」）。
    return Formatter.presenceLastSeen(
        lastSeen = lastSeen,
        justNow = strings.presenceLastSeenPrefix,
        minutesAgo = strings.presenceOfflineMinutesAgo,
        hoursAgo = strings.presenceOfflineHoursAgo,
        daysAgo = strings.presenceOfflineDaysAgo,
    ) ?: strings.presenceOffline
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
    onReportMessage: ((MessageEntry) -> Unit)? = null,
    onVideoPreview: ((MessageEntry) -> Unit)? = null,
    onImagePreview: ((MessageEntry) -> Unit)? = null,
    onRedPacketClick: ((String) -> Unit)? = null,
    onMoneyTransferClick: ((String) -> Unit)? = null,
    redPacketStatusOf: ((String) -> Int)? = null,
    onReply: ((MessageEntry) -> Unit)? = null,
    replyLookup: ((String) -> MessageEntry?)? = null,
    senderLabelOf: ((ULong) -> String)? = null,
    onReplyClick: ((MessageEntry) -> Unit)? = null,
    isHighlighted: Boolean = false,
    /** 群主/管理员可在该群置顶消息（DM / 普通成员为 false）。 */
    canPin: Boolean = false,
    /** 该消息当前是否已被置顶。 */
    isPinned: Boolean = false,
    /** 置顶/取消置顶回调（pinned=true 置顶，false 取消）。 */
    onPinMessage: ((MessageEntry, Boolean) -> Unit)? = null,
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
                        Toast.error(UserFacingError.message(error, strings.networkError))
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
                MessageActionsWrapper(message = message, isSelf = isSelf, onRequestForward = onRequestForward, onReply = onReply, onReportMessage = onReportMessage) {
                    SystemMessageRow(message = message, onUserClick = onAvatarClick, onRedPacketClick = onRedPacketClick)
                }
            }
        } else {
            SystemMessageRow(message = message, onUserClick = onAvatarClick, onRedPacketClick = onRedPacketClick)
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
                    size = AvatarSizeTokens.Small.size,
                    userId = message.fromUid.toLong(),
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
            // 图片/视频是「全幅媒体」气泡：内容本身带圆角铺满，不应再套 self/other 气泡底色，
            // 否则深色主题下 outgoing 黑底会在图片四周露出黑边。媒体气泡背景透明。
            val isMediaBubble = parsed.type == MessageType.IMAGE || parsed.type == MessageType.VIDEO
            // 资金卡片（红包/转账）是独立卡片，自带底色/圆角，不套黑色文本气泡。
            val isMoneyCard = parsed.type == MessageType.RED_PACKET || parsed.type == MessageType.MONEY_TRANSFER
            val bubbleBackground = when {
                isMediaBubble || isMoneyCard -> Color.Transparent
                isSelf -> colors.messageBubbleSelf
                else -> colors.messageBubbleOther
            }
            MessageActionsWrapper(
                message = message,
                isSelf = isSelf,
                onRequestForward = onRequestForward,
                onReply = onReply,
                onReportMessage = onReportMessage,
                canPin = canPin,
                isPinned = isPinned,
                onPinMessage = onPinMessage,
            ) {
                Box(
                    // 媒体气泡（透明底 + 0 padding）不能套外层圆角 clip：图片本身在 ImageContent 内
                    // 已有圆角，外层 clip 会把贴边的 footer（时间/状态）右下角切掉。文字气泡保留圆角。
                    modifier = Modifier
                        .then(if (isMediaBubble || isMoneyCard) Modifier else Modifier.clip(bubbleShape))
                        .background(bubbleBackground),
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
                            onContactClick = onAvatarClick,
                            onRedPacketClick = onRedPacketClick,
                            onMoneyTransferClick = onMoneyTransferClick,
                            channelDisplayName = channelDisplayName,
                            redPacketStatusOf = redPacketStatusOf,
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
                size = AvatarSizeTokens.Small.size,
                userId = selfUserId?.toLong(),
            )
        }
    }
}

/**
 * 系统消息行（spec/05-feature/SYSTEM_MESSAGE_SPEC）。
 *
 * 文案优先级：
 * 1. 撤回状态 → `strings.messageRevoked` 本地化兜底
 * 2. 协议 `template + refs` → 走 [SystemTemplateText]（AnnotatedString，user 类型可点击）
 * 3. 都没有 → 渲染 `parsed.text` 原文（向后兼容老的纯文本系统消息）
 */
@Composable
private fun SystemMessageRow(
    message: MessageEntry,
    onUserClick: ((ULong) -> Unit)? = null,
    onRedPacketClick: ((String) -> Unit)? = null,
) {
    val strings = PrivChatI18n.strings
    val colors = Theme.colors
    val parsed = message.parsedContent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(colors.muted)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            when {
                message.isRevoked -> Text(
                    text = strings.messageRevoked,
                    style = Typography.Label,
                    color = colors.mutedForeground,
                )
                parsed.systemTemplate != null -> SystemTemplateText(
                    template = parsed.systemTemplate,
                    refs = parsed.systemRefs ?: emptyList(),
                    templateDict = strings.systemTemplates,
                    listSeparator = strings.systemListSeparator,
                    textColor = colors.mutedForeground,
                    linkColor = colors.primary,
                    onUserClick = onUserClick,
                    onRedPacketClick = onRedPacketClick,
                )
                else -> Text(
                    text = parsed.text ?: "",
                    style = Typography.Label,
                    color = colors.mutedForeground,
                )
            }
        }
    }
}

/**
 * 系统消息模板渲染（spec/05-feature/SYSTEM_MESSAGE_SPEC §4.1）。
 *
 * 占位符两种：
 * - `{i}`  → 替换为 `refs[i].text`
 * - `{n+}` → 列表展开：消费 `refs[n..]`，元素之间用 [listSeparator] 拼接（处理"X 邀请 A、B、C 加入了群聊"这种不定数量）
 *
 * `type == "user"` 的 ref 渲染为可点击 [LinkAnnotation.Clickable]，点击调 [onUserClick]
 * （target_id 是 ULong 字符串）；其它类型作纯文本渲染。
 *
 * i18n key 判定（spec §4.1）：含点号且全为 `[a-z0-9._]` 视为 key，否则字面量。
 * 异常路径（key 未命中、占位越界、target_id 非数字）一律降级为字面文本，不抛错。
 */
@Composable
private fun SystemTemplateText(
    template: String,
    refs: List<com.netonstream.privchat.ui.models.MessageRef>,
    templateDict: Map<String, String>,
    listSeparator: String,
    textColor: Color,
    linkColor: Color,
    onUserClick: ((ULong) -> Unit)?,
    onRedPacketClick: ((String) -> Unit)? = null,
) {
    val isI18nKey = template.contains('.') &&
        template.all { it.isLowerCase() || it.isDigit() || it == '.' || it == '_' }
    val effective = if (isI18nKey) templateDict[template] ?: template else template

    val linkStyle = TextLinkStyles(style = SpanStyle(color = linkColor))

    val annotated = buildAnnotatedString {
        // 占位符正则：`{i}` 或 `{i+}`
        val regex = Regex("\\{(\\d+)(\\+)?\\}")
        var cursor = 0
        regex.findAll(effective).forEach { match ->
            // append literal slice before placeholder
            if (match.range.first > cursor) {
                append(effective.substring(cursor, match.range.first))
            }
            val startIdx = match.groupValues[1].toIntOrNull() ?: -1
            val isList = match.groupValues[2] == "+"

            if (startIdx < 0) {
                append(match.value)
            } else if (isList) {
                // {n+}: refs[n..] joined with listSeparator
                val tail = if (startIdx < refs.size) refs.subList(startIdx, refs.size) else emptyList()
                tail.forEachIndexed { i, ref ->
                    if (i > 0) append(listSeparator)
                    appendRefSpan(ref, linkStyle, onUserClick, onRedPacketClick)
                }
            } else {
                // {i}: single ref
                val ref = refs.getOrNull(startIdx)
                if (ref == null) {
                    append(match.value) // 越界：保留占位符字面，不抛错
                } else {
                    appendRefSpan(ref, linkStyle, onUserClick, onRedPacketClick)
                }
            }
            cursor = match.range.last + 1
        }
        if (cursor < effective.length) {
            append(effective.substring(cursor))
        }
    }

    KuiklyText(
        text = annotated,
        color = textColor,
        fontSize = Typography.Label.fontSize,
        fontWeight = Typography.Label.fontWeight,
        lineHeight = Typography.Label.lineHeight,
    )
}

/**
 * 把一个 [com.netonstream.privchat.ui.models.MessageRef] 渲染为 AnnotatedString 片段。
 *
 * `user` 类型加 [LinkAnnotation.Clickable] 包裹（点击触发 [onUserClick]）；
 * `red_packet` 类型点击触发 [onRedPacketClick]（target_id = redPacketId，跳红包详情，RP-7-B2）；其他类型纯文本。
 * Spec §4 + MESSAGE_REF_SPEC §3：未知类型降级为纯文本。
 */
private fun com.tencent.kuikly.compose.ui.text.AnnotatedString.Builder.appendRefSpan(
    ref: com.netonstream.privchat.ui.models.MessageRef,
    linkStyle: TextLinkStyles,
    onUserClick: ((ULong) -> Unit)?,
    onRedPacketClick: ((String) -> Unit)? = null,
) {
    when (ref.type) {
        "user" -> {
            val targetUid = ref.targetId.toULongOrNull()
            if (targetUid != null && onUserClick != null) {
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "user-${ref.targetId}",
                        styles = linkStyle,
                        linkInteractionListener = LinkInteractionListener { onUserClick(targetUid) },
                    ),
                ) {
                    append(ref.text)
                }
            } else {
                append(ref.text)
            }
        }
        "red_packet" -> {
            if (ref.targetId.isNotBlank() && onRedPacketClick != null) {
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "red_packet-${ref.targetId}",
                        styles = linkStyle,
                        linkInteractionListener = LinkInteractionListener { onRedPacketClick(ref.targetId) },
                    ),
                ) {
                    append(ref.text)
                }
            } else {
                append(ref.text)
            }
        }
        else -> append(ref.text)
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
                    .background(colors.muted)
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
                                    Toast.error(UserFacingError.message(error, strings.networkError))
                                }
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = chip.emoji,
                    style = Typography.BodySmall,
                    color = colors.foreground,
                )
                Text(
                    text = chip.count.toString(),
                    style = Typography.Label,
                    color = colors.mutedForeground,
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
            size = AvatarSizeTokens.Small.size,
            userId = activeUsers.firstOrNull()?.toLong(),
        )
        HorizontalSpacer(8.dp)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(colors.messageBubbleOther)
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
                            .background(colors.messageTextOther.copy(alpha = 0.5f + 0.5f * anim))
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
            color = Theme.colors.mutedForeground,
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
            .background(colors.mutedForeground.copy(alpha = 0.55f))
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
                .background(colors.mutedForeground.copy(alpha = 0.35f))
                .padding(vertical = 0.5.dp),
        )
        HorizontalSpacer(8.dp)
        Text(
            text = "以下为未读消息 ($count)",
            style = Typography.Caption,
            color = colors.mutedForeground,
        )
        HorizontalSpacer(8.dp)
        Box(
            modifier = Modifier
                .weight(1f)
                .background(colors.mutedForeground.copy(alpha = 0.35f))
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
            .border(1.dp, colors.mutedForeground.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
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
    systemSafeAreaBottom: Dp = 0.dp,
    keyboardHeight: Dp = 0.dp,
    loading: Boolean = false,
    onPickImage: () -> Unit = {},
    onPickCamera: () -> Unit = {},
    onPickFile: () -> Unit = {},
    onLocation: () -> Unit = {},
    onRedPacket: () -> Unit = {},
    onMoneyTransfer: () -> Unit = {},
    moneyEnabled: Boolean = false,
    onContact: () -> Unit = {},
    onSend: () -> Unit,
    replyPending: Boolean = false,
    // BOT_INTERACTION_SPEC §3.1：bot/system/official 会话在输入栏最前面显示菜单按钮。
    showMenuButton: Boolean = false,
    onMenuClick: () -> Unit = {},
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
    var lastKeyboardHeight by remember { mutableStateOf(0f) }
    val plusActions = remember(moneyEnabled) {
        buildList {
            add(PlusAction(Icons.image, "相册", onPickImage))
            add(PlusAction(Icons.camera_alt, "相机", onPickCamera))
            add(PlusAction(Icons.flag, "位置", onLocation))
            // 红包/转账 PLATFORM-only：moneyEnabled 才显示（BUILTIN 隐藏入口）。
            if (moneyEnabled) {
                add(PlusAction(Icons.mail, "红包", onRedPacket))
                add(PlusAction(Icons.mail, "转账", onMoneyTransfer))
            }
            add(PlusAction(Icons.attach_file, "文件", onPickFile))
            add(PlusAction(Icons.contacts, "联系人", onContact))
        }
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
    val rawKeyboardVisible = keyboardHeight > 0.dp
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
    val effectiveBottomPadding = if (hostVisible) {
        systemSafeAreaBottom
    } else {
        maxOf(systemSafeAreaBottom, keyboardHeight)
    }

    fun closeAllPanels() {
        logMessageInputBar(
            "closeAllPanels panelMode=$panelMode displayed=$displayedPanelMode pending=$pendingPanelMode reserve=$reservePanelHost overlayReserve=$overlayReservedPanelHost systemBottom=${systemSafeAreaBottom.value} keyboard=${keyboardHeight.value}"
        )
        pendingPanelMode = null
        reservePanelHost = false
        overlayReservedPanelHost = false
        displayedPanelMode = InputPanelMode.NONE
        onPanelModeChange(InputPanelMode.NONE)
    }

    fun requestPanel(targetMode: InputPanelMode) {
        logMessageInputBar(
            "requestPanel target=$targetMode panelMode=$panelMode displayed=$displayedPanelMode pending=$pendingPanelMode reserve=$reservePanelHost overlayReserve=$overlayReservedPanelHost focused=$inputFocused systemBottom=${systemSafeAreaBottom.value} keyboard=${keyboardHeight.value}"
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
            "transitionPanelToKeyboard panelMode=$panelMode displayed=$displayedPanelMode pending=$pendingPanelMode reserve=$reservePanelHost overlayReserve=$overlayReservedPanelHost focused=$inputFocused systemBottom=${systemSafeAreaBottom.value} keyboard=${keyboardHeight.value}"
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

    LaunchedEffect(keyboardHeight) {
        logMessageInputBar(
            "systemBottom=${systemSafeAreaBottom.value} keyboardHeight=${keyboardHeight.value} keyboardVisible=$keyboardVisible panelMode=$panelMode displayed=$displayedPanelMode pending=$pendingPanelMode reserve=$reservePanelHost overlayReserve=$overlayReservedPanelHost focused=$inputFocused"
        )
        if (keyboardHeight.value > lastKeyboardHeight) {
            lastKeyboardHeight = keyboardHeight.value
        }
    }

    LaunchedEffect(pendingPanelMode, keyboardVisible) {
        val targetMode = pendingPanelMode ?: return@LaunchedEffect
        if (!keyboardVisible) {
            logMessageInputBar(
                "pendingResolved target=$targetMode keyboardVisible=$keyboardVisible keyboard=${keyboardHeight.value}"
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
                "releaseReserveForKeyboard focused=$inputFocused keyboardVisible=$keyboardVisible keyboard=${keyboardHeight.value}"
            )
            reservePanelHost = false
            overlayReservedPanelHost = false
        }
    }


    LaunchedEffect(panelMode, displayedPanelMode, pendingPanelMode, reservePanelHost, overlayReservedPanelHost, inputFocused, keyboardVisible, hostVisible) {
        logMessageInputBar(
            "state panelMode=$panelMode displayed=$displayedPanelMode pending=$pendingPanelMode reserve=$reservePanelHost overlayReserve=$overlayReservedPanelHost hostVisible=$hostVisible focused=$inputFocused keyboardVisible=$keyboardVisible systemBottom=${systemSafeAreaBottom.value} keyboard=${keyboardHeight.value}"
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
                .background(colors.border),
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
            // 最左：bot/system/official 会话的菜单按钮（BOT_INTERACTION_SPEC §3.1）
            if (showMenuButton) {
                CircleIconButton(
                    icon = Icons.menu,
                    onClick = {
                        closeAllPanels()
                        onMenuClick()
                    }
                )
                HorizontalSpacer(8.dp)
            }

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
                        else -> colors.muted
                    }
                    val btnText = when {
                        isInCancelZone -> "松开 取消"
                        isRecording -> "松开 发送"
                        else -> "按住 说话"
                    }
                    val btnTextColor = if (isInCancelZone) Color.White
                                      else if (isRecording) colors.primaryForeground
                                      else colors.foreground
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
                                "focusChanged focused=$focused panelMode=$panelMode displayed=$displayedPanelMode pending=$pendingPanelMode reserve=$reservePanelHost overlayReserve=$overlayReservedPanelHost keyboard=${keyboardHeight.value}"
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
            .background(colors.muted)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            name = icon,
            size = 18.dp,
            tint = colors.foreground,
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
                .background(colors.muted),
            contentAlignment = Alignment.Center,
        ) {
            Icon(name = icon, size = 22.dp, tint = colors.foreground)
        }
        VerticalSpacer(6.dp)
        Text(text = text, style = Typography.Label, color = colors.mutedForeground)
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
    onReportMessage: ((MessageEntry) -> Unit)? = null,
    canPin: Boolean = false,
    isPinned: Boolean = false,
    onPinMessage: ((MessageEntry, Boolean) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val strings = PrivChatI18n.strings
    val scope = rememberCoroutineScope()
    val ctx = MessageActionPolicy.Context(
        message = message,
        isSelf = isSelf,
        nowMs = currentTimeMillis(),
        canPin = canPin,
        isPinned = isPinned,
    )
    val menuActions = MessageActionPolicy.menuActions(ctx).map { kind ->
        kind.toMessageAction(message, strings) {
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
                MessageActionKind.SaveImage -> {
                    scope.launch {
                        val localPath = resolveLocalImagePath(message)
                        if (localPath == null) {
                            Toast.error("保存失败：图片未下载完成")
                            return@launch
                        }
                        Toast.show("正在保存…")
                        val result = withContext(Dispatchers.Default) {
                            MediaSaver.saveImage(localPath)
                        }
                        result.fold(
                            onSuccess = { Toast.success("已保存到相册") },
                            onFailure = { Toast.error(UserFacingError.message(it, PrivChatI18n.current.saveFailed)) },
                        )
                    }
                }
                MessageActionKind.Recall -> {
                    scope.launch {
                        if (message.status == MessageStatus.Failed) {
                            withContext(Dispatchers.Default) {
                                PrivChat.client.deleteMessageLocal(message.id)
                            }.onSuccess { PrivChat.removeMessage(message.id) }
                                .onFailure { error ->
                                    Toast.error(UserFacingError.message(error, strings.networkError))
                                }
                        } else {
                            withContext(Dispatchers.Default) {
                                PrivChat.client.revokeMessage(message.id)
                            }.onFailure { error ->
                                Toast.error(UserFacingError.message(error, strings.networkError))
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
                                Toast.error(UserFacingError.message(error, strings.networkError))
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
                MessageActionKind.Pin -> onPinMessage?.invoke(message, true)
                MessageActionKind.Unpin -> onPinMessage?.invoke(message, false)
                MessageActionKind.Select -> Toast.show("多选功能即将支持")
                MessageActionKind.Report -> {
                    val handler = onReportMessage
                    if (handler != null) handler(message) else Toast.show("举报功能暂不可用")
                }
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
                    Toast.error(UserFacingError.message(error, strings.networkError))
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
    strings: com.netonstream.privchat.ui.i18n.PrivChatStrings,
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
    MessageActionKind.Pin ->
        MessageAction(label = strings.messagePin, icon = Icons.bookmark, onClick = onClick)
    MessageActionKind.Unpin ->
        MessageAction(label = strings.messageUnpin, icon = Icons.bookmark_border, onClick = onClick)
    MessageActionKind.DeleteLocal -> {
        val label = when (message.status) {
            MessageStatus.Pending, MessageStatus.Sending -> "取消发送"
            else -> "本地删除"
        }
        MessageAction(label = label, icon = Icons.delete, danger = true, onClick = onClick)
    }
    MessageActionKind.Select ->
        MessageAction(label = "选择", icon = Icons.check_box_outline_blank, onClick = onClick)
    MessageActionKind.Report ->
        MessageAction(label = "举报", icon = Icons.flag, danger = true, onClick = onClick)
}

/**
 * 解析消息对应的本地原图绝对路径：
 * - 已有 [MessageEntry.localMediaPath] 直接返回；
 * - 否则尝试拉一次最新的 PrivChat.messages 缓存（覆盖刚下载完未刷的场景）；
 * - 仍没有则 trigger MediaDownloadManager.start，并最多等 30s 直到 Done。
 *
 * 不阻塞 UI；失败返回 null，调用方负责 Toast 提示。
 */
private suspend fun resolveLocalImagePath(message: MessageEntry): String? {
    message.localMediaPath?.takeIf { it.isNotBlank() }?.let { return it }

    PrivChat.messages.value.firstOrNull { it.id == message.id }
        ?.localMediaPath?.takeIf { it.isNotBlank() }
        ?.let { return it }

    MediaDownloadManager.start(message)
    val timeoutMs = 30_000L
    val pollMs = 250L
    var waited = 0L
    while (waited < timeoutMs) {
        val state = MediaDownloadManager.states.value[message.id]
        if (state is MediaDownloadState.Done) {
            return state.path.takeIf { it.isNotBlank() }
        }
        if (state is MediaDownloadState.Failed) return null
        PrivChat.messages.value.firstOrNull { it.id == message.id }
            ?.localMediaPath?.takeIf { it.isNotBlank() }
            ?.let { return it }
        delay(pollMs)
        waited += pollMs
    }
    return null
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
        ContentMessageType.RED_PACKET -> "[红包]"
        ContentMessageType.MONEY_TRANSFER -> "[转账]"
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
            .background(colors.muted)
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
                color = colors.mutedForeground,
            )
            Text(
                text = summarizeForReply(message),
                style = Typography.BodySmall,
                color = colors.foreground,
            )
        }
        HorizontalSpacer(8.dp)
        Box(
            modifier = Modifier
                .size(24.dp)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(name = Icons.close, size = 16.dp, tint = colors.mutedForeground)
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
    val foreground = if (isSelf) colors.primaryForeground else colors.foreground
    val secondary = if (isSelf) colors.primaryForeground else colors.mutedForeground
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
                .background(if (isSelf) colors.primaryForeground else colors.primary),
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
            .border(width = 1.dp, color = colors.border, shape = RoundedCornerShape(0.dp)),
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
                            size = AvatarSizeTokens.Small.size,
                            userId = member.userId.toLong(),
                        )
                        HorizontalSpacer(10.dp)
                        Text(
                            text = displayName,
                            style = Typography.BodyMedium,
                            color = colors.foreground,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.border),
                    )
                }
            }
        }
    }
}
