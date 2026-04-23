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
import com.netonstream.privchat.ui.platform.ClipboardBridge
import com.netonstream.privchat.ui.platform.ExternalLinkBridge
import com.netonstream.privchat.ui.utils.Formatter
import com.netonstream.privchat.ui.utils.MessageEntityDetector
import com.netonstream.privchat.ui.voice.VoicePlayback
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
) {
    val colors = Theme.colors
    val textColor = if (isSelf) colors.onBubbleSelf else colors.onBubbleOther
    val secondaryTextColor = if (isSelf) colors.onBubbleSelf.copy(alpha = 0.7f) else colors.textSecondary

    val parsed = message.parsedContent

    Column(modifier = modifier.padding(10.dp)) {
        // 根据消息类型渲染内容
        when (parsed.type) {
            MessageType.TEXT -> TextContent(parsed, textColor)
            MessageType.IMAGE -> ImageContent(parsed, message, onImagePreview)
            MessageType.VIDEO -> VideoContent(parsed, message, onVideoPreview)
            MessageType.VOICE -> VoiceContent(parsed, message, isSelf, textColor)
            MessageType.FILE -> FileContent(parsed, message, textColor, secondaryTextColor)
            MessageType.STICKER -> StickerContent(parsed)
            MessageType.LOCATION -> LocationContent(parsed, textColor, secondaryTextColor)
            // LINK 占位：Link 气泡 UI 待设计；此分支保持 when 穷尽，同时降级为文本显示避免空白气泡。
            MessageType.LINK -> TextContent(
                parsed.copy(text = parsed.linkTitle ?: parsed.linkUrl ?: "", type = MessageType.TEXT),
                textColor,
            )
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
            MessageFooter(
                timestamp = message.timestamp,
                status = message.status,
                isSelf = isSelf,
                secondaryTextColor = secondaryTextColor,
                messagePts = message.pts,
                peerReadPts = peerReadPts,
                delivered = message.delivered,
                onFailedClick = onFailedClick,
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
    val text = parsed.text ?: ""
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
@Composable
private fun ImageContent(
    parsed: ParsedContent,
    message: MessageEntry,
    onImagePreview: ((MessageEntry) -> Unit)? = null,
) {
    val width = parsed.width?.coerceIn(80, 200) ?: 150
    val height = parsed.height?.coerceIn(80, 250) ?: 200
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
    val width = parsed.width?.coerceIn(80, 200) ?: 150
    val height = parsed.height?.coerceIn(80, 250) ?: 200
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
 * 位置消息
 */
@Composable
private fun LocationContent(
    parsed: ParsedContent,
    textColor: Color,
    secondaryTextColor: Color,
) {
    Column {
        Text(
            text = "📍 位置",
            style = Typography.BodyMedium,
            color = textColor,
        )
        if (!parsed.address.isNullOrBlank()) {
            VerticalSpacer(4.dp)
            Text(
                text = parsed.address,
                style = Typography.BodySmall,
                color = secondaryTextColor,
                maxLines = 2,
            )
        }
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
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
        status == MessageStatus.Failed -> Triple("❗", "发送失败 · 重试", Theme.colors.danger)
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
