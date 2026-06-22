package com.netonstream.privchat.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.netonstream.privchat.sdk.dto.MessageEntry
import com.netonstream.privchat.sdk.dto.MessageStatus
import com.netonstream.privchat.ui.media.MediaDownloadManager
import com.netonstream.privchat.ui.media.MediaDownloadState
import com.netonstream.privchat.ui.media.MediaOpener
import com.netonstream.privchat.ui.models.*
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.avatar.PrivChatAvatar
import com.netonstream.privchat.ui.platform.ClipboardBridge
import com.netonstream.privchat.ui.platform.ExternalLinkBridge
import com.netonstream.privchat.ui.utils.Formatter
import com.netonstream.privchat.ui.utils.MessageEntityDetector
import com.netonstream.privchat.ui.voice.VoicePlayback
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.messageTextOther
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.messageTextSelf
import com.gearui.components.actionsheet.ActionSheet
import com.gearui.components.actionsheet.ActionSheetItem
import com.gearui.components.toast.Toast
import com.gearui.theme.Theme
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.typography.Typography
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
import com.tencent.kuikly.compose.ui.text.style.TextDecoration
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
) {
    val colors = Theme.colors
    val textColor = if (isSelf) colors.messageTextSelf else colors.messageTextOther
    val secondaryTextColor = if (isSelf) colors.messageTextSelf.copy(alpha = 0.7f) else colors.mutedForeground

    val parsed = message.parsedContent
    // [TRACE] 排查 Bug2：气泡右下角时间+状态不显示。footer 只在 parsed.type == SYSTEM 时
    // 被跳过——但走到这里说明已经按 BUBBLE 渲染（RenderType.BUBBLE）。如果 parsed.type 是
    // SYSTEM，意味着 contentType() != SYSTEM 但 parseMessageType(messageType, content, extra)
    // 还是把这条消息归到 SYSTEM——典型成因：messageType 落在 [0..10] 之外（fromValue 返 null →
    // BUBBLE 渲染），又走 else 分支扫到 content 里的 "type":"tip" / "system" 标志。
    if (parsed.type == MessageType.SYSTEM) {
        println("[MsgNoFooter] id=${message.id} svr=${message.serverMessageId} status=${message.status} mtype=${message.messageType} parsedType=${parsed.type} text=[${parsed.text}] content=[${message.content}] extra=[${message.extra}] revoked=${message.isRevoked}")
    }

    // 图片气泡尺寸：父级算一次，ImageContent 渲染与下方 footer 宽度共用，确保发送中
    // 状态/时间贴着图片右下角对齐（pending 图比例已修正后，footer 不能再用旧的默认宽）。
    val imageBubbleSize = if (parsed.type == MessageType.IMAGE) {
        rememberImageBubbleSize(parsed, message)
    } else {
        null
    }

    Column(modifier = modifier.padding(10.dp)) {
        // 根据消息类型渲染内容
        when (parsed.type) {
            MessageType.TEXT -> TextContent(parsed, textColor)
            MessageType.IMAGE -> ImageContent(parsed, message, imageBubbleSize!!, onImagePreview)
            MessageType.VIDEO -> VideoContent(parsed, message, onVideoPreview)
            MessageType.VOICE -> VoiceContent(parsed, message, isSelf, textColor)
            MessageType.FILE -> FileContent(parsed, message, textColor, secondaryTextColor)
            MessageType.STICKER -> StickerContent(parsed)
            MessageType.LOCATION -> LocationContent(parsed, textColor, secondaryTextColor)
            MessageType.LINK -> LinkContent(parsed, textColor, secondaryTextColor)
            MessageType.CONTACT -> ContactContent(parsed, textColor, secondaryTextColor, onContactClick)
            MessageType.SYSTEM -> {
                // 系统消息由 MessageRow 在 row 级早返回 SystemMessageRow 渲染，
                // 不会走到这里；留空分支以保持 when 穷尽。
                // 撤回（isRevoked）同样在 row 级被 RenderType.REVOKED 拦截。
            }

            MessageType.UNKNOWN -> UnknownContent(textColor)
        }

        // 消息时间和状态（系统消息除外）
        if (parsed.type != MessageType.SYSTEM) {
            VerticalSpacer(4.dp)
            // 图片/视频：footer 宽度对齐图片外框（与 ImageContent/VideoContent 同一 attachmentBubbleSize），
            // 时间/状态贴着图片右下角，不再被 fillMaxWidth 推到屏幕边。
            val mediaWidthDp = when (parsed.type) {
                MessageType.IMAGE -> imageBubbleSize?.first
                MessageType.VIDEO ->
                    attachmentBubbleSize(parsed.width, parsed.height).first
                else -> null
            }
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
            )
        }
    }
}

/**
 * 文本消息。
 *
 * UX-3 / UX-4：在客户端识别 URL / 电话 / 邮箱，并把命中的区间渲染为带下划线、
 * 可点击的 span（Kuikly AnnotatedString + `LinkAnnotation.Clickable`）。
 * 点击任意实体弹出 `ActionSheet`，按类型分派 打开/拨号/发短信/发邮件/复制 等动作。
 * 若文本中未识别到任何实体，仍走 gearui 的 `Text(String)`，避免无谓的 AnnotatedString 开销。
 */
@Composable
private fun TextContent(
    parsed: ParsedContent,
    textColor: Color,
) {
    // 渲染前 trim 头尾空白：发送方常误带前后换行 / 空格，气泡内显示时去掉以贴合 IM 习惯。
    val text = (parsed.text ?: "").trim()
    val entities = remember(text) { MessageEntityDetector.detect(text) }
    if (entities.isEmpty()) {
        Text(
            text = text,
            style = Typography.BodyMedium,
            color = textColor,
        )
        return
    }

    val bodyStyle = Typography.BodyMedium
    val linkColor = Theme.colors.primary
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            color = linkColor,
            textDecoration = TextDecoration.Underline,
        ),
    )

    val annotated = buildAnnotatedString {
        var cursor = 0
        entities.forEachIndexed { index, entity ->
            if (entity.start > cursor) {
                append(text.substring(cursor, entity.start))
            }
            withLink(
                LinkAnnotation.Clickable(
                    tag = "entity-$index",
                    styles = linkStyle,
                    linkInteractionListener = LinkInteractionListener { showEntityActionSheet(entity) },
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
 * 依赖页面根部已挂载 `ActionSheet.Host()`（见 MessagePage）。
 */
private fun showEntityActionSheet(entity: MessageEntityDetector.Entity) {
    when (entity.type) {
        MessageEntityDetector.Type.URL -> {
            val url = entity.text
            ActionSheet.showList(
                items = listOf(
                    ActionSheetItem(label = "打开链接"),
                    ActionSheetItem(label = "复制链接"),
                ),
                description = url,
                onSelected = { _, index ->
                    when (index) {
                        0 -> if (!ExternalLinkBridge.openUri(url)) Toast.error("无法打开链接")
                        1 -> {
                            ClipboardBridge.setText(url)
                            Toast.success("已复制")
                        }
                    }
                },
            )
        }

        MessageEntityDetector.Type.PHONE -> {
            val phone = entity.text
            val normalized = phone.filter { it.isDigit() || it == '+' }
            ActionSheet.showList(
                items = listOf(
                    ActionSheetItem(label = "拨号"),
                    ActionSheetItem(label = "发送短信"),
                    ActionSheetItem(label = "复制号码"),
                ),
                description = phone,
                onSelected = { _, index ->
                    when (index) {
                        0 -> if (!ExternalLinkBridge.openUri("tel:$normalized")) Toast.error("无法打开拨号面板")
                        1 -> if (!ExternalLinkBridge.openUri("sms:$normalized")) Toast.error("无法打开短信")
                        2 -> {
                            ClipboardBridge.setText(phone)
                            Toast.success("已复制")
                        }
                    }
                },
            )
        }

        MessageEntityDetector.Type.EMAIL -> {
            val email = entity.text
            ActionSheet.showList(
                items = listOf(
                    ActionSheetItem(label = "发送邮件"),
                    ActionSheetItem(label = "复制邮箱"),
                ),
                description = email,
                onSelected = { _, index ->
                    when (index) {
                        0 -> if (!ExternalLinkBridge.openUri("mailto:$email")) Toast.error("无法打开邮件")
                        1 -> {
                            ClipboardBridge.setText(email)
                            Toast.success("已复制")
                        }
                    }
                },
            )
        }
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
            Text(text = icon, style = Typography.TitleMedium, color = Color.White)
        }
        if (percent != null) {
            VerticalSpacer(4.dp)
            Text(text = percent, style = Typography.Label, color = Color.White)
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
 * 队列里 process_outbound_file 跑完才落到 files/{id}/），但此刻 message.content 就是用户
 * 选的原图本地路径（占位时写入）。按 thumb → media → content 兜底，取第一个能解码的。
 * content 为 "[图片]"/JSON 时不是路径，跳过。有 metadata 时传 null → 不解码。
 */
@Composable
private fun rememberImageBubbleSize(parsed: ParsedContent, message: MessageEntry): Pair<Int, Int> {
    val needLocalSize = parsed.width == null || parsed.height == null
    val contentPath = message.content.takeIf { it.startsWith("/") }
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
    // thumb_status=3: 协议层无缩略图，直接渲染类型化静态占位
    val thumbModel = if (message.thumbStatus == 3) {
        null
    } else {
        message.localThumbnailPath?.let { "file://$it" }
            ?: message.localMediaPath?.let { "file://$it" }
            ?: parsed.thumbnailUrl
            ?: parsed.attachmentUrl
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
                contentDescription = "图片",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            GearImage(
                painter = null,
                placeholderText = "图片",
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
    // thumb_status=3: 协议层无缩略图，跳过所有远程/本地 URL，直接走类型化占位
    val videoThumb = if (message.thumbStatus == 3) {
        null
    } else {
        message.localThumbnailPath?.let { "file://$it" }
            ?: parsed.thumbnailUrl
            ?: parsed.attachmentUrl
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
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (!videoThumb.isNullOrBlank()) {
            Image(
                painter = rememberAsyncImagePainter(model = videoThumb),
                contentDescription = "视频",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            GearImage(
                painter = null,
                placeholderText = "视频",
                fit = ImageFit.COVER,
                shape = ImageShape.ROUNDED,
                cornerRadius = 8.dp,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 播放按钮
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "▶",
                style = Typography.HeadlineMedium,
                color = Color.White,
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
                    style = Typography.Label,
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
                style = Typography.BodyMedium,
                color = textColor,
            )
            HorizontalSpacer(8.dp)
            VoiceWaveIcon(isPlaying = isPlaying, tint = textColor, facing = WaveFacing.LEFT)
        } else {
            VoiceWaveIcon(isPlaying = isPlaying, tint = textColor, facing = WaveFacing.RIGHT)
            HorizontalSpacer(8.dp)
            Text(
                text = Formatter.voiceDuration(duration),
                style = Typography.BodyMedium,
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
                text = parsed.fileName ?: "文件",
                style = Typography.BodyMedium,
                color = textColor,
                maxLines = 1,
            )
            val sub = fileSubtitle(parsed.fileSize, state, hasLocal)
            if (sub != null) {
                VerticalSpacer(2.dp)
                Text(
                    text = sub,
                    style = Typography.Label,
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
            style = Typography.TitleMedium,
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
            listOfNotNull(sizeStr, pct?.let { "已暂停 $it" } ?: "已暂停").joinToString(" · ")
        }
        is MediaDownloadState.Failed -> listOfNotNull(sizeStr, "下载失败").joinToString(" · ")
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
            contentDescription = parsed.text ?: "表情",
            modifier = Modifier.size(100.dp),
            contentScale = ContentScale.Fit,
        )
    } else {
        Text(
            text = parsed.text ?: "😀",
            style = Typography.DisplayMedium,
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
    val title = parsed.locationName?.takeIf { it.isNotBlank() }
        ?: parsed.address?.takeIf { it.isNotBlank() }
        ?: "位置"
    val clickMod = if (lat != null && lng != null) {
        Modifier.clickable {
            ExternalLinkBridge.openMap(lat, lng, parsed.coordinateSystem, title.takeIf { it != "位置" })
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
                Text(text = "📍", style = Typography.TitleLarge)
            }
        }
        VerticalSpacer(6.dp)
        Text(
            text = title,
            style = Typography.BodyMedium,
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
                style = Typography.Label,
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
@Composable
private fun LinkContent(
    parsed: ParsedContent,
    textColor: Color,
    secondaryTextColor: Color,
) {
    val url = parsed.linkUrl
    val title = parsed.linkTitle?.takeIf { it.isNotBlank() } ?: url ?: ""
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
            Text(text = title, style = Typography.BodyMedium, color = textColor, maxLines = 2)
        }
        if (desc != null) {
            VerticalSpacer(2.dp)
            Text(text = desc, style = Typography.BodySmall, color = secondaryTextColor, maxLines = 2)
        }
        if (!url.isNullOrBlank()) {
            VerticalSpacer(4.dp)
            Text(text = url, style = Typography.Label, color = secondaryTextColor, maxLines = 1)
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
        ?: uid?.let { "用户 #$it" }
        ?: "用户名片"
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
                style = Typography.BodyMedium,
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
        Text(text = "个人名片", style = Typography.Label, color = secondaryTextColor)
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
        text = "[不支持的消息类型]",
        style = Typography.BodyMedium,
        color = textColor,
    )
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
) {
    Row(
        modifier = if (mediaWidthDp != null) Modifier.width(mediaWidthDp.dp) else Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 时间
        Text(
            text = Formatter.messageTime(timestamp),
            style = Typography.Label,
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
) {
    val (icon, label, iconColor) = when {
        status == MessageStatus.Failed -> Triple("❗", "发送失败 · 重试", Theme.colors.destructive)
        status == MessageStatus.Pending || status == MessageStatus.Sending ->
            Triple("⏳", "发送中", color)
        isReadByPts || status == MessageStatus.Read ->
            Triple("✓✓", "已读", Theme.colors.primary)
        delivered -> Triple("✓✓", "已送达", color)
        else -> Triple("✓", "已发送", color)
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
            style = Typography.Label,
            color = iconColor,
        )
        HorizontalSpacer(3.dp)
        Text(
            text = label,
            style = Typography.Label,
            color = iconColor,
        )
    }
}
