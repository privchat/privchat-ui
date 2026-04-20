package com.netonstream.privchat.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import om.netonstream.privchat.sdk.dto.MessageEntry
import om.netonstream.privchat.sdk.dto.MessageStatus
import com.netonstream.privchat.ui.models.*
import com.netonstream.privchat.ui.utils.Formatter
import com.netonstream.privchat.ui.voice.VoicePlayback
import com.gearui.theme.Theme
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.typography.Typography
import com.gearui.components.image.GearImage
import com.gearui.components.image.ImageFit
import com.gearui.components.image.ImageShape
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
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.collectAsState
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.unit.dp

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
) {
    val colors = Theme.colors
    val textColor = if (isSelf) colors.onBubbleSelf else colors.onBubbleOther
    val secondaryTextColor = if (isSelf) colors.onBubbleSelf.copy(alpha = 0.7f) else colors.textSecondary

    val parsed = message.parsedContent

    Column(modifier = modifier.padding(10.dp)) {
        // 根据消息类型渲染内容
        when (parsed.type) {
            MessageType.TEXT -> TextContent(parsed, textColor)
            MessageType.IMAGE -> ImageContent(parsed, message)
            MessageType.VIDEO -> VideoContent(parsed, message)
            MessageType.VOICE -> VoiceContent(parsed, message, isSelf, textColor)
            MessageType.FILE -> FileContent(parsed, textColor, secondaryTextColor)
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
            )
        }
    }
}

/**
 * 文本消息
 */
@Composable
private fun TextContent(
    parsed: ParsedContent,
    textColor: Color,
) {
    Text(
        text = parsed.text ?: "",
        style = Typography.BodyMedium,
        color = textColor,
    )
}

/**
 * 图片消息
 * 优先使用本地规范路径（thumb.webp / payload.{ext}），回退到远程 URL
 */
@Composable
private fun ImageContent(
    parsed: ParsedContent,
    message: MessageEntry,
) {
    val width = parsed.width?.coerceIn(80, 200) ?: 150
    val height = parsed.height?.coerceIn(80, 250) ?: 200
    // 本地路径优先：缩略图 > 媒体文件 > 远程 URL
    val imageModel = message.localThumbnailPath?.let { "file://$it" }
        ?: message.localMediaPath?.let { "file://$it" }
        ?: parsed.thumbnailUrl
        ?: parsed.attachmentUrl

    Box(
        modifier = Modifier
            .size(width.dp, height.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        if (!imageModel.isNullOrBlank()) {
            Image(
                painter = rememberAsyncImagePainter(model = imageModel),
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
 * 优先使用本地规范路径（thumb.webp），回退到远程 URL
 */
@Composable
private fun VideoContent(
    parsed: ParsedContent,
    message: MessageEntry,
) {
    val width = parsed.width?.coerceIn(80, 200) ?: 150
    val height = parsed.height?.coerceIn(80, 250) ?: 200
    val videoThumb = message.localThumbnailPath?.let { "file://$it" }
        ?: parsed.thumbnailUrl
        ?: parsed.attachmentUrl

    Box(
        modifier = Modifier
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
 * 文件消息
 */
@Composable
private fun FileContent(
    parsed: ParsedContent,
    textColor: Color,
    secondaryTextColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 文件图标
        Text(
            text = "📄",
            style = Typography.TitleMedium,
        )
        HorizontalSpacer(8.dp)
        Column {
            Text(
                text = parsed.fileName ?: "文件",
                style = Typography.BodyMedium,
                color = textColor,
                maxLines = 1,
            )
            if (parsed.fileSize != null && parsed.fileSize > 0) {
                VerticalSpacer(2.dp)
                Text(
                    text = Formatter.fileSize(parsed.fileSize),
                    style = Typography.Label,
                    color = secondaryTextColor,
                )
            }
        }
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
            )
        }
    }
}

/**
 * 消息状态图标
 *
 * 优先级（高→低）：已读 > 已送达 > 已发送 > 发送中/失败
 */
@Composable
private fun MessageStatusIcon(
    status: MessageStatus,
    color: Color,
    isReadByPts: Boolean = false,
    delivered: Boolean = false,
) {
    val (icon, iconColor) = when {
        status == MessageStatus.Failed -> "❗" to Theme.colors.danger
        status == MessageStatus.Pending || status == MessageStatus.Sending -> "⏳" to color
        isReadByPts || status == MessageStatus.Read -> "✓✓" to Theme.colors.primary
        delivered -> "✓✓" to color
        else -> "✓" to color
    }

    Text(
        text = icon,
        style = Typography.Label,
        color = iconColor,
    )
}
