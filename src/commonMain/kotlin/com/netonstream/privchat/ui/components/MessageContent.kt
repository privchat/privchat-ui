package com.netonstream.privchat.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.netonstream.privchat.sdk.dto.MessageEntry
import com.netonstream.privchat.sdk.dto.MessageTextEntity
import com.netonstream.privchat.sdk.dto.MessageTextEntityType
import com.netonstream.privchat.sdk.dto.MessageStatus
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.netonstream.privchat.ui.i18n.withArgs
import com.netonstream.privchat.ui.media.MediaDownloadManager
import com.netonstream.privchat.ui.media.MediaDownloadState
import com.netonstream.privchat.ui.media.MediaOpener
import com.netonstream.privchat.ui.models.*
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.avatar.PrivChatAvatar
import com.netonstream.privchat.ui.platform.ClipboardBridge
import com.netonstream.privchat.ui.platform.ExternalLinkBridge
import com.netonstream.privchat.ui.utils.Formatter
import com.netonstream.privchat.ui.voice.VoicePlayback
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.messageLinkOther
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.messageLinkSelf
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.messageTextOther
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.messageTextSelf
import com.gearui.components.actionsheet.ActionSheet
import com.gearui.components.actionsheet.ActionSheetItem
import com.gearui.components.toast.Toast
import com.gearui.theme.Theme
import com.gearui.foundation.primitives.Text
import com.gearui.components.icon.Icons
import com.gearui.foundation.primitives.Icon
import com.gearui.foundation.typography.IconSizes
import com.gearui.components.image.GearImage
import com.gearui.components.image.ImageFit
import com.gearui.components.image.ImageShape
import com.gearui.components.loading.Loading
import com.gearui.components.loading.LoadingSize
import com.gearui.primitives.HorizontalSpacer
import com.gearui.primitives.VerticalSpacer
import com.tencent.kuikly.compose.animation.core.LinearEasing
import com.tencent.kuikly.compose.animation.core.RepeatMode
import com.tencent.kuikly.compose.animation.core.animateFloat
import com.tencent.kuikly.compose.animation.core.infiniteRepeatable
import com.tencent.kuikly.compose.animation.core.rememberInfiniteTransition
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.detectTapGestures
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.collectAsState
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.text.LinkAnnotation
import com.tencent.kuikly.compose.ui.text.LinkInteractionListener
import com.tencent.kuikly.compose.ui.text.SpanStyle
import com.tencent.kuikly.compose.ui.text.TextLinkStyles
import com.tencent.kuikly.compose.ui.text.buildAnnotatedString
import com.tencent.kuikly.compose.ui.text.withLink
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.material3.Text as KuiklyText

/**
 * 消息内容渲染组件
 *
 * 根据消息类型渲染不同的内容
 * 直接使用 SDK 的 MessageEntry 类型
 *
 * @param message 消息（SDK 类型）
 * @param isSelf 是否自己发送
 * @param modifier Modifier
 */
@Composable
fun MessageContent(
    message: MessageEntry,
    isSelf: Boolean,
    modifier: Modifier = Modifier,
    peerReadPts: ULong? = null,
    onFailedClick: (() -> Unit)? = null,
    onVideoPreview: ((MessageEntry) -> Unit)? = null,
    onImagePreview: ((MessageEntry) -> Unit)? = null,
    onContactClick: ((ULong) -> Unit)? = null,
    // 点红包卡片（传 redPacketId）→ 宿主打开红包详情/领取。null = 只读降级（BUILTIN）。
    onRedPacketClick: ((String) -> Unit)? = null,
    // 点转账卡片（传 transferId）→ 宿主打开转账详情。null = 只读。
    onMoneyTransferClick: ((String) -> Unit)? = null,
    // 会话展示名（DM 场景即对端昵称）：资金卡片用它显示「转账给 X」/「X 向你转账」，避免裸 uid。
    channelDisplayName: String = "",
    // 红包卡片实时状态解析（由会话页据领取/抢完系统消息推导）：0/未知=可领取，1=我已领取，2=已抢完/过期。
    redPacketStatusOf: ((String) -> Int)? = null,
    /**
     * 点击 @ 提及。**不要复用 [onContactClick]**：那个在自己发的消息上会被置空
     * （「不能点自己的头像」），@ 却是指向别人的，跟着一起失效就变成了复制。
     * 第一个参数是 SDK 解析出的 userId，可能为 null，此时由上层按名字兜底。
     */
    onMentionClick: ((ULong?, String) -> Unit)? = null,
) {
    val colors = Theme.colors
    val textColor = if (isSelf) colors.messageTextSelf else colors.messageTextOther
    val parsed = message.parsedContent
    // 🔴 媒体气泡（图片/视频）背景透明，footer 直接落在页面白底上：自己发的消息若沿用
    // 深色气泡里的浅色 `messageTextSelf`，时间/发送中/进度就是白字白底——什么都看不见。
    // 媒体气泡一律用 mutedForeground，与对方消息一致。
    val secondaryTextColor = if (
        isSelf && parsed.type != MessageType.IMAGE && parsed.type != MessageType.VIDEO
    ) colors.messageTextSelf.copy(alpha = 0.7f) else colors.mutedForeground
    // [TRACE] 排查 Bug2：气泡右下角时间+状态不显示。footer 只在 parsed.type == SYSTEM 时
    // 被跳过——但走到这里说明已经按 BUBBLE 渲染（RenderType.BUBBLE）。如果 parsed.type 是
    // SYSTEM，意味着 contentType() != SYSTEM 但 parseMessageType(messageType, content, extra)
    // 还是把这条消息归到 SYSTEM——典型成因：messageType 落在 [0..10] 之外（fromValue 返 null →
    // BUBBLE 渲染），又走 else 分支扫到 content 里的 "type":"tip" / "system" 标志。
    if (parsed.type == MessageType.SYSTEM) {
        println("[MsgNoFooter] id=${message.id} svr=${message.serverMessageId} status=${message.status} mtype=${message.messageType} parsedType=${parsed.type} text=[${parsed.text}] revoked=${message.isRevoked}")
    }

    // 图片气泡尺寸：父级算一次，ImageContent 渲染与下方 footer 宽度共用，确保发送中
    // 状态/时间贴着图片右下角对齐（pending 图比例已修正后，footer 不能再用旧的默认宽）。
    val imageBubbleSize = if (parsed.type == MessageType.IMAGE) {
        rememberImageBubbleSize(parsed, message)
    } else {
        null
    }

    // 媒体（图片/视频）气泡背景透明、内容自带圆角铺满，不能再套 10dp 内边距 —— 否则透明的
    // 顶部 padding 会把图片往下推，使图片顶部比发送者头像低、不对齐（文字气泡有深色背景，
    // 10dp 是气泡内边距、气泡顶仍对齐头像）。媒体用 0 padding，图片顶与头像对齐。
    val isMediaBubble = parsed.type == MessageType.IMAGE || parsed.type == MessageType.VIDEO
    // 资金卡片（红包/转账）是独立卡片，自带底色/圆角/内边距，外层不再套气泡内边距。
    val isMoneyCard = parsed.type == MessageType.RED_PACKET || parsed.type == MessageType.MONEY_TRANSFER
    Column(modifier = modifier.padding(if (isMediaBubble || isMoneyCard) 0.dp else 10.dp)) {
        // 根据消息类型渲染内容
        when (parsed.type) {
            MessageType.TEXT -> TextContent(
                text = message.body.text,
                entities = message.body.entities,
                textColor = textColor,
                isSelf = isSelf,
                onMentionClick = onMentionClick,
            )
            MessageType.IMAGE -> ImageContent(parsed, message, imageBubbleSize!!, onImagePreview)
            MessageType.VIDEO -> VideoContent(parsed, message, onVideoPreview)
            MessageType.VOICE -> VoiceContent(parsed, message, isSelf, textColor)
            MessageType.FILE -> FileContent(parsed, message, textColor, secondaryTextColor)
            MessageType.STICKER -> StickerContent(parsed)
            MessageType.LOCATION -> LocationContent(parsed, textColor, secondaryTextColor)
            MessageType.LINK -> LinkContent(parsed, linkColorFor(isSelf), secondaryTextColor)
            MessageType.CONTACT -> ContactContent(parsed, textColor, secondaryTextColor, onContactClick)
            MessageType.RED_PACKET -> RedPacketMessageView(parsed, redPacketStatusOf, onRedPacketClick)
            MessageType.MONEY_TRANSFER -> MoneyTransferMessageView(parsed, isSelf, channelDisplayName, onMoneyTransferClick)
            MessageType.SYSTEM -> {
                // 系统消息由 MessageRow 在 row 级早返回 SystemMessageRow 渲染，
                // 不会走到这里；留空分支以保持 when 穷尽。
                // 撤回（isRevoked）同样在 row 级被 RenderType.REVOKED 拦截。
            }

            MessageType.UNKNOWN -> UnknownContent(textColor)
        }

        // 消息时间和状态（系统消息 + 资金卡片除外）。资金卡片由服务端注入天然是 Sent 态，
        // 不显示「发送中/发送失败」，也不在卡片下压时间/状态行——保持独立卡片的干净视觉。
        if (parsed.type != MessageType.SYSTEM && !isMoneyCard) {
            VerticalSpacer(4.dp)
            // 图片/视频：footer 宽度对齐图片外框（与 ImageContent/VideoContent 同一 attachmentBubbleSize），
            // 时间/状态贴着图片右下角，不再被 fillMaxWidth 推到屏幕边。
            val mediaWidthDp = when (parsed.type) {
                MessageType.IMAGE -> imageBubbleSize?.first
                MessageType.VIDEO ->
                    attachmentBubbleSize(parsed.width, parsed.height).first
                else -> null
            }
            // 上传进度按**本地消息 id** 取：SDK 发进度时带的就是它。
            val uploads = com.netonstream.privchat.ui.runtime.ClientRuntime.uploads
                .collectAsState().value
            val uploadPercent = uploads
                .fractionOf(message.id.toString())
                ?.let { (it * 100).toInt().coerceIn(0, 100) }
            // 百分比之外再给字节量：光看 "37%" 判断不了是卡住了还是文件本来就大，
            // "2.1MB / 5.6MB" 一眼能看出还剩多少、走没走动。
            val uploadBytes = uploads.inFlight[message.id.toString()]
                ?.let { (sent, total) -> "${humanBytes(sent)} / ${humanBytes(total)}" }
            MessageFooter(
                timestamp = message.timestamp,
                status = message.status,
                isSelf = isSelf,
                secondaryTextColor = secondaryTextColor,
                messagePts = message.pts,
                peerReadPts = peerReadPts,
                delivered = message.delivered,
                onFailedClick = onFailedClick,
                mediaWidthDp = mediaWidthDp,
                uploadPercent = uploadPercent,
                uploadBytes = uploadBytes,
            )
        } else if (isMoneyCard) {
            // 资金卡片：只显示时间，不显示发送中/发送失败/已读状态（服务端注入天然 Sent）。
            VerticalSpacer(4.dp)
            Row(
                modifier = Modifier.width(MoneyCardWidth),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = Formatter.messageTime(message.timestamp),
                    style = Theme.typography.label,
                    color = secondaryTextColor,
                )
            }
        }
    }
}

/**
 * 文本消息。
 *
 * UX-3 / UX-4：使用 Rust SDK 识别的 URL / 电话 / 连续数字 / 邮箱 / 提及，并把命中的区间渲染为带下划线、
 * 可点击的 span（Kuikly AnnotatedString + `LinkAnnotation.Clickable`）。
 * 点击任意实体弹出 `ActionSheet`，按类型分派 打开/拨号/发短信/发邮件/复制 等动作。
 * 若文本中未识别到任何实体，仍走 gearui 的 `Text(String)`，避免无谓的 AnnotatedString 开销。
 */
@Composable
private fun TextContent(
    text: String,
    entities: List<MessageTextEntity>,
    textColor: Color,
    isSelf: Boolean,
    onMentionClick: ((ULong?, String) -> Unit)?,
) {
    val safeEntities = remember(text, entities) { validTextEntities(text, entities) }
    if (safeEntities.isEmpty()) {
        Text(
            text = text,
            style = Theme.typography.bodyMedium,
            color = textColor,
        )
        return
    }

    val bodyStyle = Theme.typography.bodyMedium
    // 可点击的一律用 link 蓝，不加下划线。
    //
    // 自己气泡曾经用正文色 + 下划线（下划线是唯一提示）。现在两种气泡都靠颜色表达
    // 「这是可点的」，所以链接色要同时满足：与所在气泡底 ≥4.5（看得清）、与同段正文
    // ≥3:1（认得出）。自己/对方气泡的底色完全不同，必须分别取值，见
    // PrivChatThemeExtension.messageLinkSelf / messageLinkOther 的注释与实测数字。
    val linkColor = linkColorFor(isSelf)
    val linkStyle = TextLinkStyles(style = SpanStyle(color = linkColor))

    val annotated = buildAnnotatedString {
        var cursor = 0
        safeEntities.forEachIndexed { index, entity ->
            if (entity.start > cursor) {
                append(text.substring(cursor, entity.start))
            }
            withLink(
                LinkAnnotation.Clickable(
                    tag = "entity-$index",
                    styles = linkStyle,
                    linkInteractionListener = LinkInteractionListener {
                        showEntityActionSheet(entity, onMentionClick)
                    },
                ),
            ) {
                append(entity.text)
            }
            cursor = entity.end
        }
        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }

    KuiklyText(
        text = annotated,
        color = textColor,
        fontSize = bodyStyle.fontSize,
        fontWeight = bodyStyle.fontWeight,
        lineHeight = bodyStyle.lineHeight,
    )
}

/**
 * 按实体类型弹出 ActionSheet；调用方只需传入一个 `Entity`，
 * 所有 “打开链接 / 拨号 / 发短信 / 发邮件 / 复制” 都封装在这里。
 * ActionSheet 宿主由 gearui `App()` 全局挂载，页面无需（也不得）自行挂 Host。
 */
private fun showEntityActionSheet(
    entity: MessageTextEntity,
    onMentionClick: ((ULong?, String) -> Unit)?,
) {
    when (entity.type) {
        MessageTextEntityType.Url -> {
            val url = entity.value
            ActionSheet.showList(
                items = listOf(
                    ActionSheetItem(label = PrivChatI18n.current.linkOpen),
                    ActionSheetItem(label = PrivChatI18n.current.linkCopy),
                ),
                description = url,
                onSelected = { _, index ->
                    when (index) {
                        0 -> if (!ExternalLinkBridge.openUri(url)) Toast.error(PrivChatI18n.current.linkOpenFailed)
                        1 -> {
                            ClipboardBridge.setText(url)
                            Toast.success(PrivChatI18n.current.messageCopied)
                        }
                    }
                },
            )
        }

        MessageTextEntityType.Phone -> {
            val phone = entity.text
            val normalized = entity.value
            ActionSheet.showList(
                items = listOf(
                    ActionSheetItem(label = PrivChatI18n.current.phoneDial),
                    ActionSheetItem(label = PrivChatI18n.current.phoneSms),
                    ActionSheetItem(label = PrivChatI18n.current.phoneCopy),
                ),
                description = phone,
                onSelected = { _, index ->
                    when (index) {
                        0 -> if (!ExternalLinkBridge.openUri("tel:$normalized")) Toast.error(PrivChatI18n.current.phoneDialFailed)
                        1 -> if (!ExternalLinkBridge.openUri("sms:$normalized")) Toast.error(PrivChatI18n.current.phoneSmsFailed)
                        2 -> {
                            ClipboardBridge.setText(phone)
                            Toast.success(PrivChatI18n.current.messageCopied)
                        }
                    }
                },
            )
        }

        MessageTextEntityType.Number -> {
            ActionSheet.showList(
                items = listOf(ActionSheetItem(label = PrivChatI18n.current.phoneCopy)),
                description = entity.text,
                onSelected = { _, _ ->
                    ClipboardBridge.setText(entity.value)
                    Toast.success(PrivChatI18n.current.messageCopied)
                },
            )
        }

        MessageTextEntityType.Email -> {
            val email = entity.value
            ActionSheet.showList(
                items = listOf(
                    ActionSheetItem(label = PrivChatI18n.current.emailSend),
                    ActionSheetItem(label = PrivChatI18n.current.emailCopy),
                ),
                description = email,
                onSelected = { _, index ->
                    when (index) {
                        0 -> if (!ExternalLinkBridge.openUri("mailto:$email")) Toast.error(PrivChatI18n.current.emailOpenFailed)
                        1 -> {
                            ClipboardBridge.setText(email)
                            Toast.success(PrivChatI18n.current.messageCopied)
                        }
                    }
                },
            )
        }

        MessageTextEntityType.Mention -> {
            // 🔴 点 @ 要打开这个人的资料，复制只是没人接手时的兜底。
            //
            // 这里原来还要求 `entity.userId != null` 才跳转，可 userId 是 SDK 按「第 N 个 @
            // 对应 mentioned_user_ids 的第 N 项」位置匹配出来的：发送端没带这个数组（例如
            // 手打的 @、或别的客户端没填）时它就是 null，于是所有 @ 一律变成"复制"。
            // 现在把 userId 和名字一起交给上层，由会话页拿群成员按名字兜底解析。
            if (onMentionClick != null) {
                onMentionClick(entity.userId, entity.value)
            } else {
                ClipboardBridge.setText(entity.text)
                Toast.success(PrivChatI18n.current.messageCopied)
            }
        }

        MessageTextEntityType.Unknown -> Unit
    }
}

/** Invalid or overlapping SDK spans must never hide message text or crash rendering. */
internal fun validTextEntities(
    text: String,
    entities: List<MessageTextEntity>,
): List<MessageTextEntity> {
    var cursor = 0
    return entities
        .sortedWith(compareBy<MessageTextEntity> { it.start }.thenBy { it.end })
        .filter { entity ->
            val valid = entity.start >= cursor &&
                entity.type != MessageTextEntityType.Unknown &&
                entity.start >= 0 &&
                entity.end > entity.start &&
                entity.end <= text.length &&
                text.substring(entity.start, entity.end) == entity.text
            if (valid) cursor = entity.end
            valid
        }
}

/**
 * Telegram 风格的附件下载气泡包装。
 *
 * UI 规则：
 * - 已下载（localMediaPath 非空）：无遮罩，点击用 MediaOpener 打开；
 * - 未下载（Idle / Failed）：覆盖半透明圆形按钮显示 ↓ / ↻，点击触发 start；
 * - 下载中（Downloading）：覆盖圆形按钮显示 × + 百分比，点击触发 pause；
 * - 暂停（Paused）：覆盖圆形按钮显示 ↓ + 百分比，点击触发 resume。
 *
 * 下载完成后 SDK 会发 `media_download_state_changed(done)` + 刷新消息条目，
 * 新的 localMediaPath 写回后本 composable 会自动回到"无遮罩"分支。
 */
@Composable
private fun MediaDownloadBubble(
    message: MessageEntry,
    onOpen: ((String) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val downloadStates by MediaDownloadManager.states.collectAsState()
    val state = downloadStates[message.id] ?: MediaDownloadState.Idle

    LaunchedEffect(message.localMediaPath) {
        MediaDownloadManager.clearIfDone(message.id, message.localMediaPath)
    }

    val hasLocal = !message.localMediaPath.isNullOrBlank()
    val showOverlay = !hasLocal && state !is MediaDownloadState.Done

    fun openLocal(path: String) {
        if (onOpen != null) onOpen(path) else MediaOpener.open(path, message.mimeType)
    }

    Box(
        modifier = Modifier.clickable {
            when {
                hasLocal -> openLocal(message.localMediaPath!!)
                state is MediaDownloadState.Done -> openLocal(state.path)
                state is MediaDownloadState.Downloading -> MediaDownloadManager.pause(message.id)
                state is MediaDownloadState.Paused -> MediaDownloadManager.resume(message.id)
                else -> MediaDownloadManager.start(message)
            }
        }
    ) {
        content()
        if (showOverlay) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                DownloadBadge(state = state)
            }
        }
    }
}

@Composable
private fun DownloadBadge(state: MediaDownloadState) {
    val icon = when (state) {
        is MediaDownloadState.Downloading -> "×"
        is MediaDownloadState.Paused -> "↓"
        is MediaDownloadState.Failed -> "↻"
        else -> "↓"
    }
    val percent = when (state) {
        is MediaDownloadState.Downloading -> percentText(state.bytes, state.total)
        is MediaDownloadState.Paused -> percentText(state.bytes, state.total)
        else -> null
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            if (state is MediaDownloadState.Downloading) {
                Loading(size = LoadingSize.SMALL, color = Color.White)
            }
            Text(text = icon, style = Theme.typography.titleMedium, color = Color.White)
        }
        if (percent != null) {
            VerticalSpacer(4.dp)
            Text(text = percent, style = Theme.typography.label, color = Color.White)
        }
    }
}

private fun percentText(bytes: ULong, total: ULong?): String? {
    val t = total ?: return null
    if (t == 0uL) return null
    val pct = (bytes.toDouble() / t.toDouble() * 100.0).toInt().coerceIn(0, 100)
    return "$pct%"
}

/**
 * 图片消息
 *
 * 气泡里显示缩略图（本地 thumb > 本地 payload > 远程 thumb > 远程原图）；
 * 点击走 [onImagePreview] 进入全屏预览页查看大图，不经过 MediaDownloadBubble。
 * 预览页优先使用本地原图，否则交给远程 URL（由 Coil 异步解码）。
 */
/**
 * 图片/视频气泡显示尺寸：按原图宽高比缩放进 maxW×maxH 范围，保持比例不失真。
 * 旧逻辑对 width/height 各自独立 coerceIn，会把任意图都压成方块/失真；尺寸缺失
 * （加密前客户端未带 width/height，或老消息无尺寸）时回退竖向默认 150×200。
 */
private fun attachmentBubbleSize(width: Int?, height: Int?): Pair<Int, Int> {
    val iw = width ?: 0
    val ih = height ?: 0
    if (iw <= 0 || ih <= 0) return 150 to 200
    val maxW = 200f
    val maxH = 250f
    val minSide = 80f
    val ratio = iw.toFloat() / ih.toFloat()
    var w = maxW
    var h = w / ratio
    if (h > maxH) { h = maxH; w = h * ratio }
    if (w < minSide) { w = minSide; h = w / ratio }
    if (h < minSide) { h = minSide; w = h * ratio }
    return w.toInt().coerceAtLeast(1) to h.toInt().coerceAtLeast(1)
}

/**
 * 图片气泡显示尺寸（width,height dp）。父级、[ImageContent] 与 footer 共用同一结果，
 * 保证发送中状态/时间与图片右边对齐。
 *
 * 比例优先 metadata width/height；发送中(pending)本地图 metadata 还没回写时，从本地图片
 * 一次性读「展示方向」尺寸（applyExif）。不走 painter.intrinsicSize（Kuikly 下那是 native
 * 调用、图未加载会抛 → crash）。
 *
 * pending(status=Sending)阶段 localThumbnailPath/localMediaPath 都还是 null（缩略图要等
 * 队列里 process_outbound_file 跑完才落到 files/{id}/），但此刻 message.body.text 就是用户
 * 选的原图本地路径（占位时写入）。按 thumb → media → content 兜底，取第一个能解码的。
 * content 为 "[图片]"/JSON 时不是路径，跳过。有 metadata 时传 null → 不解码。
 */
@Composable
private fun rememberImageBubbleSize(parsed: ParsedContent, message: MessageEntry): Pair<Int, Int> {
    val needLocalSize = parsed.width == null || parsed.height == null
    val contentPath = message.body.text.takeIf { it.startsWith("/") }
    val localImageSize = rememberPendingImageSize(
        if (needLocalSize) message.localThumbnailPath else null,
        if (needLocalSize) message.localMediaPath else null,
        if (needLocalSize) contentPath else null,
    )
    val effWidth = parsed.width ?: localImageSize?.first
    val effHeight = parsed.height ?: localImageSize?.second
    return attachmentBubbleSize(effWidth, effHeight)
}

@Composable
private fun ImageContent(
    parsed: ParsedContent,
    message: MessageEntry,
    bubbleSize: Pair<Int, Int>,
    onImagePreview: ((MessageEntry) -> Unit)? = null,
) {
    val (width, height) = bubbleSize
    // 只用**本地已下载**的文件渲染。
    //
    // 附件在服务端是加密存储的（ATTACHMENT_ENCRYPTION_SPEC）：metadata 里的 url /
    // thumbnail_url 指向密文 blob，直接喂给图片加载器一定失败——实测拉下来的字节
    // 既不是 PNG 也不是 JPEG。而一旦把这种地址当成"有图"，就连占位都不会走，气泡
    // 整个是空的，用户看到的就是一片空白。
    //
    // 冻结的分层也是这么写的：UI 由 message_type + media_downloaded + thumb_status
    // 驱动，不消费 content/extra 里的远程 URL。下载/解密是 SDK 的事，UI 只在拿到
    // 本地路径后渲染，否则显示类型化占位。
    val thumbModel = message.localThumbnailPath?.let { "file://$it" }
        ?: message.localMediaPath?.let { "file://$it" }

    // 「滚到哪儿就加载哪儿」：LazyColumn 里这个 composable 只有进入可视区（含预取）
    // 才会组合，所以这里就是「可见」的时机。缩略图的自动下载原本只挂在消息入站
    // 路径上，历史翻页拉回来的从来不触发——表现就是历史图片要点一下才出来。
    // ensure 语义：已经有了 / 协议层确无缩略图，SDK 侧直接返回。
    if (thumbModel.isNullOrBlank()) {
        LaunchedEffect(message.id) {
            MediaDownloadManager.ensureThumbnail(message.id)
        }
    }

    // UX-2：图片气泡点击预览 + 长按弹菜单。二合一 detectTapGestures 避免与外层长按冲突。
    val menuTrigger = LocalMessageMenuTrigger.current
    val gestureMod = if (onImagePreview != null || menuTrigger != null) {
        Modifier.pointerInput(message.id) {
            detectTapGestures(
                onTap = { onImagePreview?.invoke(message) },
                onLongPress = { menuTrigger?.invoke() },
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = gestureMod
            .size(width.dp, height.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        if (!thumbModel.isNullOrBlank()) {
            Image(
                painter = rememberAsyncImagePainter(model = thumbModel),
                contentDescription = PrivChatI18n.current.a11yImage,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            // 没有本地资源:类型化占位,文案走 i18n(不硬编码中文)。
            // 绝不留空气泡——空白让用户以为消息坏了,占位至少说明"这是一张图"。
            GearImage(
                painter = null,
                placeholderText = PrivChatI18n.strings.previewImage,
                fit = ImageFit.COVER,
                shape = ImageShape.ROUNDED,
                cornerRadius = 8.dp,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * 视频消息
 *
 * 气泡显示缩略图 + 播放按钮；点击无论下载与否都进入全屏预览页。
 * 下载/loading/播放切换全部由 VideoPreviewPage 内部接管，本组件不再叠加下载遮罩。
 */
@Composable
private fun VideoContent(
    parsed: ParsedContent,
    message: MessageEntry,
    onVideoPreview: ((MessageEntry) -> Unit)? = null,
) {
    val (width, height) = attachmentBubbleSize(parsed.width, parsed.height)
    // 同图片:只用本地已下载的文件。远程 url 指向加密 blob,渲染器解不开。
    //
    // 🔴 **不能退回 `localMediaPath`**：那是视频文件本身（.mov/.mp4），图片解码器解不开。
    // 退回它的后果不是报错，是画出一片**透明**——气泡只剩一个孤零零的播放按钮。
    // 而且这个坑只在「看过一次之后」才现形：没下载时 localMediaPath 是 null，走 else
    // 分支还有占位底色；看完一次 media 落了盘，反而把底色弄没了。
    // 视频的图像来源只有一个：缩略图。
    val videoThumb = message.localThumbnailPath?.let { "file://$it" }

    // 🔴 与 ImageContent 一样，没有本地封面就触发一次下载。
    //
    // 这里原来只读 localThumbnailPath，没有就渲染灰底——**发送端**有本地文件所以
    // 封面正常，**接收端**永远是一个灰方块加播放键，而且链路上不报任何错。图片那条
    // 早就接了 ensureThumbnail，视频这条一直没跟上。
    //
    // ensure 语义：已经有了 / 协议层确无缩略图，SDK 侧直接返回，重复调用无副作用。
    if (videoThumb.isNullOrBlank()) {
        LaunchedEffect(message.id) {
            MediaDownloadManager.ensureThumbnail(message.id)
        }
    }

    // UX-2：视频气泡点击预览 + 长按弹菜单。
    val menuTrigger = LocalMessageMenuTrigger.current
    val gestureMod = if (onVideoPreview != null || menuTrigger != null) {
        Modifier.pointerInput(message.id) {
            detectTapGestures(
                onTap = { onVideoPreview?.invoke(message) },
                onLongPress = { menuTrigger?.invoke() },
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = gestureMod
            .size(width.dp, height.dp)
            .clip(RoundedCornerShape(8.dp))
            // 底色恒在：缩略图缺失、还没解码完、解码失败，任何一种情况下气泡都得是
            // 一个成形的方块，而不是一个浮在聊天背景上的播放按钮。
            .background(Theme.colors.muted),
        contentAlignment = Alignment.Center,
    ) {
        if (!videoThumb.isNullOrBlank()) {
            Image(
                painter = rememberAsyncImagePainter(model = videoThumb),
                contentDescription = PrivChatI18n.current.a11yVideo,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        // 缩略图缺失时不再垫占位组件：外层 Box 已有底色，播放按钮 + 时长角标足以
        // 说明「这是条视频」。占位文字会从播放按钮后面透出来，看着像渲染叠错了层。

        // 播放按钮
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                name = Icons.play_fill,
                size = 26.dp,
                tint = Color.White,
            )
        }

        // 时长
        if (parsed.duration != null && parsed.duration > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = Formatter.duration(parsed.duration),
                    style = Theme.typography.label,
                    color = Color.White,
                )
            }
        }
    }
}

/**
 * 语音消息（微信风格）
 *
 * - 点击切换播放/停止，单路播放（新点击会停掉旧的）
 * - 自己发送：波纹图标在右，气泡整体右对齐
 * - 对方发送：波纹图标在左
 * - 播放中：三根竖条做 1s 的循环动画
 * - 气泡宽度按时长动态调整：最短 72dp，每秒 +4dp，最长 200dp
 */
@Composable
private fun VoiceContent(
    parsed: ParsedContent,
    message: MessageEntry,
    isSelf: Boolean,
    textColor: Color,
) {
    val duration = parsed.duration ?: 0
    val width = (72 + (duration.coerceAtLeast(1) * 4).coerceAtMost(128)).dp

    val playing by VoicePlayback.playingMessageId.collectAsState()
    val isPlaying = playing == message.id

    val source = message.localMediaPath?.let { "file://$it" }
        ?: parsed.attachmentUrl

    Row(
        modifier = Modifier
            .width(width)
            .clickable { VoicePlayback.toggle(message.id, source) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start,
    ) {
        if (isSelf) {
            Text(
                text = Formatter.voiceDuration(duration),
                style = Theme.typography.bodyMedium,
                color = textColor,
            )
            HorizontalSpacer(8.dp)
            VoiceWaveIcon(isPlaying = isPlaying, tint = textColor, facing = WaveFacing.LEFT)
        } else {
            VoiceWaveIcon(isPlaying = isPlaying, tint = textColor, facing = WaveFacing.RIGHT)
            HorizontalSpacer(8.dp)
            Text(
                text = Formatter.voiceDuration(duration),
                style = Theme.typography.bodyMedium,
                color = textColor,
            )
        }
    }
}

private enum class WaveFacing { LEFT, RIGHT }

/**
 * 微信语音的三根竖条波纹。播放中循环动画，静止时显示中等高度。
 * facing=RIGHT 时高度从左到右递增（喇叭开口向右，用于对方气泡）；
 * facing=LEFT 时反向（用于自己气泡）。
 */
@Composable
private fun VoiceWaveIcon(
    isPlaying: Boolean,
    tint: Color,
    facing: WaveFacing,
) {
    val baseHeights = listOf(6.dp, 10.dp, 14.dp)
    val heights = if (facing == WaveFacing.RIGHT) baseHeights else baseHeights.asReversed()

    // rememberInfiniteTransition 必须在顶层稳定调用；仅在 isPlaying 为 true 时读取 phase
    val transition = rememberInfiniteTransition(label = "voice-wave")
    val animatedPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "voice-wave-phase",
    )
    val phase: Float = if (isPlaying) animatedPhase else 1.5f

    Row(verticalAlignment = Alignment.CenterVertically) {
        heights.forEachIndexed { index, h ->
            val scale: Float = if (isPlaying) {
                val local = (phase - index + 3f) % 3f
                0.4f + 0.6f * (1f - kotlin.math.abs(local - 1.5f) / 1.5f)
            } else {
                1f
            }
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((h.value * scale).dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(tint),
            )
            if (index != heights.lastIndex) HorizontalSpacer(2.dp)
        }
    }
}


/**
 * 文件消息：左侧图标按钮即为下载/暂停/恢复控件，Telegram 风格。
 */
@Composable
private fun FileContent(
    parsed: ParsedContent,
    message: MessageEntry,
    textColor: Color,
    secondaryTextColor: Color,
) {
    val downloadStates by MediaDownloadManager.states.collectAsState()
    val state = downloadStates[message.id] ?: MediaDownloadState.Idle

    LaunchedEffect(message.localMediaPath) {
        MediaDownloadManager.clearIfDone(message.id, message.localMediaPath)
    }

    val hasLocal = !message.localMediaPath.isNullOrBlank()

    Row(
        modifier = Modifier.clickable {
            when {
                hasLocal -> MediaOpener.open(message.localMediaPath!!, message.mimeType)
                state is MediaDownloadState.Done -> MediaOpener.open(state.path, message.mimeType)
                state is MediaDownloadState.Downloading -> MediaDownloadManager.pause(message.id)
                state is MediaDownloadState.Paused -> MediaDownloadManager.resume(message.id)
                else -> MediaDownloadManager.start(message)
            }
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FileLeadingBadge(hasLocal = hasLocal, state = state)
        HorizontalSpacer(8.dp)
        Column {
            Text(
                text = parsed.fileName ?: PrivChatI18n.current.a11yFile,
                style = Theme.typography.bodyMedium,
                color = textColor,
                maxLines = 1,
            )
            val sub = fileSubtitle(parsed.fileSize, state, hasLocal)
            if (sub != null) {
                VerticalSpacer(2.dp)
                Text(
                    text = sub,
                    style = Theme.typography.label,
                    color = secondaryTextColor,
                )
            }
        }
    }
}

@Composable
private fun FileLeadingBadge(
    hasLocal: Boolean,
    state: MediaDownloadState,
) {
    val icon = when {
        hasLocal -> "📄"
        state is MediaDownloadState.Done -> "📄"
        state is MediaDownloadState.Downloading -> "×"
        state is MediaDownloadState.Paused -> "↓"
        state is MediaDownloadState.Failed -> "↻"
        else -> "↓"
    }
    val bgColor = if (hasLocal || state is MediaDownloadState.Done) {
        Color.Transparent
    } else {
        Theme.colors.primary.copy(alpha = 0.9f)
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        if (state is MediaDownloadState.Downloading) {
            Loading(size = LoadingSize.SMALL, color = Color.White)
        }
        Text(
            text = icon,
            style = Theme.typography.titleMedium,
            color = if (hasLocal || state is MediaDownloadState.Done) null else Color.White,
        )
    }
}

private fun fileSubtitle(
    fileSize: Long?,
    state: MediaDownloadState,
    hasLocal: Boolean,
): String? {
    if (hasLocal || state is MediaDownloadState.Done) {
        return fileSize?.takeIf { it > 0 }?.let { Formatter.fileSize(it) }
    }
    val sizeStr = fileSize?.takeIf { it > 0 }?.let { Formatter.fileSize(it) }
    return when (state) {
        is MediaDownloadState.Downloading -> {
            val pct = percentText(state.bytes, state.total)
            listOfNotNull(sizeStr, pct).joinToString(" · ").ifBlank { null }
        }
        is MediaDownloadState.Paused -> {
            val pct = percentText(state.bytes, state.total)
            listOfNotNull(
                sizeStr,
                pct?.let { PrivChatI18n.current.downloadPausedAt.withArgs(it) } ?: PrivChatI18n.current.downloadPaused,
            ).joinToString(" · ")
        }
        is MediaDownloadState.Failed -> listOfNotNull(sizeStr, PrivChatI18n.current.downloadFailed).joinToString(" · ")
        else -> sizeStr
    }
}

/**
 * 表情/贴纸消息
 */
@Composable
private fun StickerContent(
    parsed: ParsedContent,
) {
    if (parsed.attachmentUrl != null) {
        Image(
            painter = rememberAsyncImagePainter(model = parsed.attachmentUrl),
            contentDescription = parsed.text ?: PrivChatI18n.current.a11ySticker,
            modifier = Modifier.size(100.dp),
            contentScale = ContentScale.Fit,
        )
    } else {
        Text(
            text = parsed.text ?: "😀",
            style = Theme.typography.displayMedium,
        )
    }
}

/**
 * 位置消息（协议 LocationMetadata，Phase 2 已扩展）。
 *
 * 展示优先级：POI name > address > lat/lng 兜底。点击 → 系统地图，按 coordinate_system
 * 转成 WGS-84 再打开（gcj02/bd09 不转会偏几百米）。缩略图：协议是 thumbnail_file_id，
 * 接收端用 server 解析后的 thumbnailUrl 显示；只有 file_id 拿不到 url 时退视觉占位块。
 */
@Composable
private fun LocationContent(
    parsed: ParsedContent,
    textColor: Color,
    secondaryTextColor: Color,
) {
    val lat = parsed.latitude
    val lng = parsed.longitude
    // 先算「有没有真实地名」，再决定显示什么：地图 App 的标注只该收真实地名，
    // 之前那句 `title.takeIf { it != "位置" }` 是拿兜底文案当哨兵值——文案一翻译就失效。
    val placeName = parsed.locationName?.takeIf { it.isNotBlank() }
        ?: parsed.address?.takeIf { it.isNotBlank() }
    val title = placeName ?: PrivChatI18n.current.locationFallbackTitle
    val clickMod = if (lat != null && lng != null) {
        Modifier.clickable {
            ExternalLinkBridge.openMap(lat, lng, parsed.coordinateSystem, placeName)
        }
    } else {
        Modifier
    }
    val thumb = parsed.thumbnailUrl?.takeIf { it.isNotBlank() }
    Column(modifier = clickMod.width(220.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Theme.colors.muted),
            contentAlignment = Alignment.Center,
        ) {
            if (thumb != null) {
                // 发送端生成的静态地图缩略图（server 已把 thumbnail_file_id 解析成 url）。
                Image(
                    painter = rememberAsyncImagePainter(model = thumb),
                    contentDescription = title,
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(text = "📍", style = Theme.typography.titleLarge)
            }
        }
        VerticalSpacer(6.dp)
        Text(
            text = title,
            style = Theme.typography.bodyMedium,
            color = textColor,
            maxLines = 2,
        )
        // 副行：有 POI 名时显示地址；否则显示坐标兜底。
        val subtitle = parsed.locationName?.takeIf { it.isNotBlank() }
            ?.let { parsed.address?.takeIf { a -> a.isNotBlank() } }
            ?: lat?.let { la -> lng?.let { lo -> "${fmtCoord(la)}, ${fmtCoord(lo)}" } }
        if (subtitle != null) {
            VerticalSpacer(2.dp)
            Text(
                text = subtitle,
                style = Theme.typography.label,
                color = secondaryTextColor,
                maxLines = 2,
            )
        }
    }
}

private fun fmtCoord(v: Double): String = ((v * 100000).toLong() / 100000.0).toString()

/**
 * 链接卡片消息（协议 LinkMetadata：url / title / description / thumbnail）。
 *
 * 缩略图由发送端 SDK 预览钩子生成（server 不爬，接收端不爬）。Phase 1 只渲染：
 * 缩略图可用 → 图 + 标题 + 描述 + url；不可用 → 纯文本卡片。点击 → 外部浏览器。
 */
/** 可点击文本/卡片的颜色：自己与对方气泡底色不同，取值也不同。 */
@Composable
private fun linkColorFor(isSelf: Boolean): Color =
    if (isSelf) Theme.colors.messageLinkSelf else Theme.colors.messageLinkOther

@Composable
private fun LinkContent(
    parsed: ParsedContent,
    linkColor: Color,
    secondaryTextColor: Color,
) {
    val url = parsed.linkUrl
    // 没有真标题时用 url 顶上，但下面那行 url 就要跳过——否则同一个网址印两遍。
    val realTitle = parsed.linkTitle?.takeIf { it.isNotBlank() }
    val title = realTitle ?: url ?: ""
    val desc = parsed.linkDescription?.takeIf { it.isNotBlank() }
    val thumb = parsed.thumbnailUrl?.takeIf { it.isNotBlank() }
    val clickMod = if (!url.isNullOrBlank()) {
        Modifier.clickable { ExternalLinkBridge.openUri(url) }
    } else {
        Modifier
    }
    Column(modifier = clickMod.width(240.dp)) {
        if (thumb != null) {
            Image(
                painter = rememberAsyncImagePainter(model = thumb),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
            VerticalSpacer(6.dp)
        }
        if (title.isNotBlank()) {
            // 整块卡片是可点的，标题用链接色，不用正文色。
            Text(text = title, style = Theme.typography.bodyMedium, color = linkColor, maxLines = 2)
        }
        if (desc != null) {
            VerticalSpacer(2.dp)
            Text(text = desc, style = Theme.typography.bodySmall, color = secondaryTextColor, maxLines = 2)
        }
        // 标题就是 url 时不再重复一行。
        if (!url.isNullOrBlank() && realTitle != null) {
            VerticalSpacer(4.dp)
            Text(text = url, style = Theme.typography.label, color = secondaryTextColor, maxLines = 1)
        }
    }
}

/**
 * 名片消息（协议 ContactCardMetadata：只有 user_id，user_id 是权威引用）。
 *
 * user_id 是主路径。当前用本地 friends 缓存解析头像/昵称，但这只覆盖「分享的是我的好友」；
 * 群成员 / 陌生人 / 已注销用户查不到 → 降级「用户 #id」。
 * TODO: friends 缓存只是 best-effort，后续应收敛到统一 UserResolver.resolve(userId)
 * （覆盖 user cache / friends / channel members）。
 * 点击 → 复用 onContactClick（= MessagePage.onAvatarClick → 用户资料页）。
 */
@Composable
private fun ContactContent(
    parsed: ParsedContent,
    textColor: Color,
    secondaryTextColor: Color,
    onContactClick: ((ULong) -> Unit)?,
) {
    val uid = parsed.contactUserId
    val friends by PrivChat.friends.collectAsState()
    val friend = remember(friends, uid) { uid?.let { id -> friends.firstOrNull { it.userId == id } } }
    val displayName = friend?.remark?.takeIf { it.isNotBlank() }
        ?: friend?.nickname?.takeIf { it.isNotBlank() }
        ?: parsed.contactName?.takeIf { it.isNotBlank() }
        ?: friend?.username
        ?: uid?.let { PrivChatI18n.current.contactCardUnnamed.withArgs(it) }
        ?: PrivChatI18n.current.contactCardFallback
    val avatarUrl = friend?.avatarUrl ?: parsed.contactAvatarUrl
    val clickMod = if (uid != null && onContactClick != null) {
        Modifier.clickable { onContactClick(uid) }
    } else {
        Modifier
    }
    Column(modifier = clickMod.width(220.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PrivChatAvatar(
                name = displayName,
                username = friend?.username,
                avatarUrl = avatarUrl,
                userId = uid?.toLong(),
                size = 44.dp,
            )
            HorizontalSpacer(10.dp)
            Text(
                text = displayName,
                style = Theme.typography.bodyMedium,
                color = textColor,
                maxLines = 1,
            )
        }
        VerticalSpacer(8.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Theme.colors.border),
        )
        VerticalSpacer(6.dp)
        Text(text = PrivChatI18n.current.contactCardLabel, style = Theme.typography.label, color = secondaryTextColor)
    }
}

/**
 * 未知类型消息
 */
@Composable
private fun UnknownContent(
    textColor: Color,
) {
    Text(
        text = PrivChatI18n.current.unsupportedContent,
        style = Theme.typography.bodyMedium,
        color = textColor,
    )
}

// ─────────────── Money Message 标准卡片（RP-ferry） ───────────────
// 只渲染 payload 展示快照 + 交互回调；资金真相在 platform，UI 不碰余额/领取入账。

// 资金卡片统一为独立卡片（不再被普通文本气泡包裹）：固定宽度 + 圆角 + 品牌底色 + 左图标 + 右文案栈。
private val MoneyCardWidth = 232.dp
private val RedPacketColor = Color(0xFFE5533D)
private val TransferColor = Color(0xFFF5A623)

/**
 * 红包卡片（独立卡片）。标题=祝福语，副标题=普通/拼手气，状态=快照（未领取/已抢完/已过期）。
 * 「已领取」等 per-viewer 结果以详情页 platform API 为准，这里只展示订单级快照，不本地猜最终资金态。
 * onOpen != null → 点击进入红包详情/领取；null → 只读降级（BUILTIN）。
 */
@Composable
private fun RedPacketMessageView(
    parsed: ParsedContent,
    redPacketStatusOf: ((String) -> Int)?,
    onOpen: ((String) -> Unit)?,
) {
    val refId = parsed.moneyRefId
    val clickable = refId != null && onOpen != null
    val title = parsed.moneyTitle?.takeIf { it.isNotBlank() } ?: PrivChatI18n.current.redPacketDefaultTitle
    val subtitle = when (parsed.moneyType) {
        1 -> PrivChatI18n.current.redPacketLucky
        0 -> PrivChatI18n.current.redPacketNormal
        else -> null
    }
    // 实时状态优先（会话页据领取/抢完系统消息推导）：2=已抢完/过期、1=我已领取；否则回退 content 快照。
    val liveStatus = refId?.let { redPacketStatusOf?.invoke(it) } ?: 0
    val statusText = when {
        liveStatus == 2 -> PrivChatI18n.current.redPacketDrained
        liveStatus == 1 -> PrivChatI18n.current.redPacketClaimed
        parsed.moneyStatus == "finished" -> PrivChatI18n.current.redPacketDrained
        parsed.moneyStatus == "expired" || parsed.moneyStatus == "refunding" -> PrivChatI18n.current.redPacketExpired
        onOpen != null -> PrivChatI18n.current.redPacketClaim
        else -> PrivChatI18n.current.redPacketUnsupportedVersion
    }
    MoneyCardScaffold(icon = Icons.gift, bg = RedPacketColor, refId = refId, clickable = clickable, onOpen = onOpen) {
        Text(text = title, style = Theme.typography.bodyMedium, color = Color.White)
        subtitle?.let {
            VerticalSpacer(3.dp)
            Text(text = it, style = Theme.typography.label, color = Color.White.copy(alpha = 0.85f))
        }
        VerticalSpacer(5.dp)
        Text(text = statusText, style = Theme.typography.label, color = Color.White.copy(alpha = 0.95f))
    }
}

/**
 * 转账卡片（独立卡片）。转账即时到账、无需接收确认。视角相关：发送方「转账给 X / 已到账」、
 * 接收方「X 向你转账 / 已存入余额」；退款态「转账 / 已退回」。counterpartyName（DM 即对端昵称）
 * 为空时退化为「转账」，绝不显示裸 uid。点击进入转账详情。null → 只读。
 */
@Composable
private fun MoneyTransferMessageView(
    parsed: ParsedContent,
    isSelf: Boolean,
    counterpartyName: String,
    onOpen: ((String) -> Unit)?,
) {
    val refId = parsed.moneyRefId
    val clickable = refId != null && onOpen != null
    val refunded = parsed.moneyStatus == "refunded"
    val peer = counterpartyName.takeIf { it.isNotBlank() }
    val title = when {
        refunded || peer == null -> PrivChatI18n.current.transferTitle
        isSelf -> PrivChatI18n.current.transferToPeer.withArgs(peer)
        else -> PrivChatI18n.current.transferFromPeer.withArgs(peer)
    }
    val statusText = when {
        refunded -> PrivChatI18n.current.transferRefunded
        isSelf -> PrivChatI18n.current.transferReceived
        else -> PrivChatI18n.current.transferCredited
    }
    MoneyCardScaffold(icon = Icons.wallet, bg = TransferColor, refId = refId, clickable = clickable, onOpen = onOpen) {
        Text(text = title, style = Theme.typography.bodyMedium, color = Color.White)
        parsed.moneyAmountText?.takeIf { it.isNotBlank() }?.let {
            VerticalSpacer(4.dp)
            Text(text = it, style = Theme.typography.titleLarge, color = Color.White)
        }
        VerticalSpacer(5.dp)
        Text(text = statusText, style = Theme.typography.label, color = Color.White.copy(alpha = 0.95f))
    }
}

/** 资金卡片外壳：固定宽度独立卡片 + 左图标 + 右文案列（内容由 [content] 提供）。 */
@Composable
private fun MoneyCardScaffold(
    /**
     * An `Icons.*` key, not a glyph.
     *
     * These were 🧧 and 💸 rendered through `Text(color = Color.White)`, which
     * is a no-op: a colour emoji ignores text colour. So the red packet drew
     * its own red on the card's red (#E5533D) and neither mark could follow
     * the card. They also drew a different picture on every platform. Same
     * defect the kit's own `check_emoji_as_icon` guard describes — it just did
     * not cover this repo.
     */
    icon: String,
    bg: Color,
    refId: String?,
    clickable: Boolean,
    onOpen: ((String) -> Unit)?,
    content: @Composable () -> Unit,
) {
    val base = Modifier
        .width(MoneyCardWidth)
        .clip(RoundedCornerShape(12.dp))
        .background(bg)
    Row(
        modifier = (if (clickable) base.clickable { onOpen!!.invoke(refId!!) } else base)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(name = icon, size = IconSizes.Default.xl, tint = Color.White)
        HorizontalSpacer(12.dp)
        Column(modifier = Modifier.weight(1f)) { content() }
    }
}

/**
 * 消息底部（时间 + 状态）
 */
@Composable
private fun MessageFooter(
    timestamp: ULong,
    status: MessageStatus,
    isSelf: Boolean,
    secondaryTextColor: Color,
    messagePts: ULong? = null,
    peerReadPts: ULong? = null,
    delivered: Boolean = false,
    onFailedClick: (() -> Unit)? = null,
    // 媒体（图片/视频）气泡：footer 宽度对齐图片外框宽度（dp），时间/状态右对齐到图片右边缘，
    // 不再 fillMaxWidth 把整列撑到屏幕边。null = 文本等普通气泡，沿用 fillMaxWidth。
    mediaWidthDp: Int? = null,
    uploadPercent: Int? = null,
    uploadBytes: String? = null,
) {
    Row(
        modifier = if (mediaWidthDp != null) Modifier.width(mediaWidthDp.dp) else Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 时间
        Text(
            text = Formatter.messageTime(timestamp),
            style = Theme.typography.label,
            color = secondaryTextColor,
        )

        // 发送状态（仅自己的消息显示）
        if (isSelf) {
            HorizontalSpacer(4.dp)
            // 已读投影：message.pts <= peerReadPts 时视为已读（优先级最高）
            val isReadByPts = messagePts != null && peerReadPts != null && messagePts <= peerReadPts
            MessageStatusIcon(
                status = status,
                color = secondaryTextColor,
                isReadByPts = isReadByPts,
                delivered = delivered,
                onFailedClick = onFailedClick,
                uploadPercent = uploadPercent,
                uploadBytes = uploadBytes,
            )
        }
    }
}

/**
 * 消息状态图标 + 文案
 *
 * 优先级（高→低）：已读 > 已送达 > 已发送 > 发送中/失败
 * 失败态文字可点击触发重试。
 */
@Composable
private fun MessageStatusIcon(
    status: MessageStatus,
    color: Color,
    isReadByPts: Boolean = false,
    delivered: Boolean = false,
    onFailedClick: (() -> Unit)? = null,
    /** 上传进度百分比；null = 不在上传（或没有进度可报）。 */
    uploadPercent: Int? = null,
    uploadBytes: String? = null,
) {
    val (icon, label, iconColor) = when {
        status == MessageStatus.Failed -> Triple("❗", PrivChatI18n.current.statusSendFailedRetry, Theme.colors.destructive)
        // 🔴 上传中的媒体：把百分比摆出来。
        //
        // 大文件在弱网下「发送中」可能停留好几分钟，只有一个不动的 ⏳ 时，用户没法
        // 区分「在传」和「卡死了」——他们的应对是长按重发，于是刚传上去的部分全白费。
        // 有数字在动，等待才是可以忍受的。
        (status == MessageStatus.Pending || status == MessageStatus.Sending) &&
            uploadPercent != null ->
            Triple(
                "⏳",
                if (uploadBytes != null) "$uploadBytes · $uploadPercent%"
                else PrivChatI18n.current.statusUploading.withArgs(uploadPercent),
                color,
            )
        status == MessageStatus.Pending || status == MessageStatus.Sending ->
            Triple("⏳", PrivChatI18n.current.statusSendingShort, color)
        // 「已读」与时间同色。它不是需要抢注意力的状态——消息已经送到了，读者没有任何
        // 动作要做；把它挑成强调色只会让每条自己发的消息末尾都有一处高对比色块在跳。
        // 需要强调的只有「发送失败」（destructive），那才要用户处理。
        isReadByPts || status == MessageStatus.Read ->
            Triple("✓✓", PrivChatI18n.current.statusRead, color)
        delivered -> Triple("✓✓", PrivChatI18n.current.statusDelivered, color)
        else -> Triple("✓", PrivChatI18n.current.statusSent, color)
    }

    val modifier = if (status == MessageStatus.Failed && onFailedClick != null) {
        Modifier.clickable { onFailedClick() }
    } else {
        Modifier
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = icon,
            style = Theme.typography.label,
            color = iconColor,
        )
        HorizontalSpacer(3.dp)
        Text(
            text = label,
            style = Theme.typography.label,
            color = iconColor,
        )
    }
}

/// 字节数转人读格式。上传进度用，所以只到 MB 就够——再大的附件本来也传不动。
private fun humanBytes(n: Long): String = when {
    n >= 1024L * 1024L -> {
        val mb = n.toDouble() / (1024.0 * 1024.0)
        val one = (mb * 10).toLong()
        "${one / 10}.${one % 10}MB"
    }
    n >= 1024L -> "${n / 1024}KB"
    else -> "${n}B"
}
