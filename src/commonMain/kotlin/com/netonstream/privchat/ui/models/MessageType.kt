package com.netonstream.privchat.ui.models

import com.netonstream.privchat.sdk.dto.MessageEntry

/**
 * 消息内容类型枚举（仅映射协议内容类型）。
 *
 * 注意：撤回是状态 overlay（`MessageEntry.isRevoked`），不是内容类型——
 * 不在此枚举内。撤回的显示层分派与文案由 `RenderType` / `isRevoked` 负责。
 */
enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    VOICE,
    FILE,
    STICKER,
    LOCATION,
    LINK,
    SYSTEM,
    UNKNOWN
}

// privchat-protocol::message::ContentMessageType
// 编号按业务优先级（0=文字 最常用 … 10=转发 最少用），与 Rust 协议保持一致。
private const val PROTOCOL_TEXT = 0
private const val PROTOCOL_VOICE = 1
private const val PROTOCOL_IMAGE = 2
private const val PROTOCOL_VIDEO = 3
private const val PROTOCOL_FILE = 4
private const val PROTOCOL_SYSTEM = 5
private const val PROTOCOL_STICKER = 6
private const val PROTOCOL_CONTACT_CARD = 7
private const val PROTOCOL_LOCATION = 8
private const val PROTOCOL_LINK = 9
private const val PROTOCOL_FORWARD = 10

/**
 * 解析后的消息内容
 */
data class ParsedContent(
    val type: MessageType,
    val text: String? = null,
    val attachmentUrl: String? = null,
    val thumbnailUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val duration: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    // LINK 专用：url 必有；title/description/thumbnailUrl 由 SDK 宿主预览回调填充（未注册则全为空）。
    val linkUrl: String? = null,
    val linkTitle: String? = null,
    val linkDescription: String? = null,
)

/**
 * 从消息内容解析消息类型
 *
 * SDK 的 content 是 JSON 格式，包含 type 字段
 * 格式示例：{"type":"text","text":"hello"}
 *          {"type":"image","url":"...","width":100,"height":100}
 */
fun parseMessageType(content: String): MessageType {
    return try {
        when {
            content.contains("\"type\":\"text\"") ||
                    content.contains("\"type\": \"text\"") -> MessageType.TEXT

            content.contains("\"type\":\"image\"") ||
                    content.contains("\"type\": \"image\"") -> MessageType.IMAGE

            content.contains("\"type\":\"video\"") ||
                    content.contains("\"type\": \"video\"") -> MessageType.VIDEO

            content.contains("\"type\":\"voice\"") ||
                    content.contains("\"type\": \"voice\"") ||
                    content.contains("\"type\":\"audio\"") ||
                    content.contains("\"type\": \"audio\"") -> MessageType.VOICE

            content.contains("\"type\":\"file\"") ||
                    content.contains("\"type\": \"file\"") -> MessageType.FILE

            content.contains("\"type\":\"sticker\"") ||
                    content.contains("\"type\": \"sticker\"") ||
                    content.contains("\"type\":\"emoji\"") ||
                    content.contains("\"type\": \"emoji\"") -> MessageType.STICKER

            content.contains("\"type\":\"location\"") ||
                    content.contains("\"type\": \"location\"") -> MessageType.LOCATION

            content.contains("\"type\":\"link\"") ||
                    content.contains("\"type\": \"link\"") -> MessageType.LINK

            content.contains("\"type\":\"system\"") ||
                    content.contains("\"type\": \"system\"") ||
                    content.contains("\"type\":\"tip\"") ||
                    content.contains("\"type\": \"tip\"") -> MessageType.SYSTEM

            // 默认当作纯文本处理
            else -> MessageType.TEXT
        }
    } catch (e: Exception) {
        MessageType.UNKNOWN
    }
}

/**
 * 从协议 message_type 优先解析消息类型；仅在未知时回退 content/extra。
 */
fun parseMessageType(messageType: Int, content: String, extra: String): MessageType {
    return when (messageType) {
        PROTOCOL_TEXT -> MessageType.TEXT
        PROTOCOL_IMAGE -> MessageType.IMAGE
        PROTOCOL_FILE -> inferFileOrVideo(content, extra)
        PROTOCOL_VOICE -> MessageType.VOICE
        PROTOCOL_VIDEO -> MessageType.VIDEO
        PROTOCOL_SYSTEM -> MessageType.SYSTEM
        PROTOCOL_LOCATION -> MessageType.LOCATION
        PROTOCOL_CONTACT_CARD -> MessageType.FILE
        PROTOCOL_STICKER -> MessageType.STICKER
        PROTOCOL_LINK -> MessageType.LINK
        PROTOCOL_FORWARD -> MessageType.TEXT
        else -> {
            val fromContent = parseMessageType(content)
            if (fromContent != MessageType.TEXT || content.contains("\"type\"")) {
                fromContent
            } else {
                parseMessageType(extra)
            }
        }
    }
}

/**
 * 解析消息内容
 *
 * 简化的 JSON 解析，提取常用字段
 */
fun parseMessageContent(content: String): ParsedContent {
    val type = parseMessageType(content)

    return when (type) {
        MessageType.TEXT -> ParsedContent(
            type = type,
            text = extractJsonString(content, "text") ?: content
        )

        MessageType.IMAGE -> ParsedContent(
            type = type,
            attachmentUrl = extractJsonString(content, "url"),
            thumbnailUrl = extractJsonString(content, "thumbnail")
                ?: extractJsonString(content, "thumbnailUrl"),
            width = extractJsonInt(content, "width"),
            height = extractJsonInt(content, "height")
        )

        MessageType.VIDEO -> ParsedContent(
            type = type,
            attachmentUrl = extractJsonString(content, "url"),
            thumbnailUrl = extractJsonString(content, "thumbnail")
                ?: extractJsonString(content, "snapshot"),
            duration = extractJsonInt(content, "duration"),
            width = extractJsonInt(content, "width"),
            height = extractJsonInt(content, "height")
        )

        MessageType.VOICE -> ParsedContent(
            type = type,
            attachmentUrl = extractJsonString(content, "url"),
            duration = extractJsonInt(content, "duration")
        )

        MessageType.FILE -> ParsedContent(
            type = type,
            attachmentUrl = extractJsonString(content, "url"),
            fileName = extractJsonString(content, "filename")
                ?: extractJsonString(content, "name"),
            fileSize = extractJsonLong(content, "size")
        )

        MessageType.LOCATION -> ParsedContent(
            type = type,
            latitude = extractJsonDouble(content, "latitude")
                ?: extractJsonDouble(content, "lat"),
            longitude = extractJsonDouble(content, "longitude")
                ?: extractJsonDouble(content, "lng"),
            address = extractJsonString(content, "address")
                ?: extractJsonString(content, "name")
        )

        MessageType.STICKER -> ParsedContent(
            type = type,
            attachmentUrl = extractJsonString(content, "url"),
            text = extractJsonString(content, "name")
                ?: extractJsonString(content, "emoji")
        )

        MessageType.SYSTEM -> ParsedContent(
            type = type,
            text = extractJsonString(content, "text")
                ?: extractJsonString(content, "tip")
                ?: content
        )

        MessageType.LINK -> ParsedContent(
            type = type,
            linkUrl = extractJsonString(content, "url"),
            linkTitle = extractJsonString(content, "title"),
            linkDescription = extractJsonString(content, "description"),
            thumbnailUrl = extractJsonString(content, "thumbnail_url")
                ?: extractJsonString(content, "thumbnail")
        )

        MessageType.UNKNOWN -> ParsedContent(
            type = type,
            text = content
        )
    }
}

/**
 * 基于 MessageEntry（包含协议 messageType/extra）解析消息内容。
 *
 * 仅做内容类型解析，不读取 `isRevoked` 状态——撤回的显示与文案由 `RenderType.REVOKED`
 * 与 `textPreview`（看 `isRevoked`）负责。避免状态层污染内容解析。
 */
fun parseMessageContent(message: MessageEntry): ParsedContent {
    val content = message.content
    val extra = message.extra
    val type = parseMessageType(message.messageType, content, extra)

    return when (type) {
        MessageType.TEXT -> ParsedContent(
            type = type,
            text = extractPayloadContent(content)
                ?: extractJsonString(content, "text")
                ?: content
        )

        MessageType.IMAGE -> ParsedContent(
            type = type,
            attachmentUrl = extractMediaUrl(content, extra),
            thumbnailUrl = extractThumbnailUrl(content, extra),
            fileName = extractFileName(content, extra),
            fileSize = extractFileSize(content, extra),
            width = extractJsonInt(content, "width") ?: extractJsonInt(extra, "width"),
            height = extractJsonInt(content, "height") ?: extractJsonInt(extra, "height"),
        )

        MessageType.VIDEO -> ParsedContent(
            type = type,
            attachmentUrl = extractMediaUrl(content, extra),
            thumbnailUrl = extractThumbnailUrl(content, extra),
            fileName = extractFileName(content, extra),
            fileSize = extractFileSize(content, extra),
            duration = extractJsonInt(content, "duration") ?: extractJsonInt(extra, "duration"),
            width = extractJsonInt(content, "width") ?: extractJsonInt(extra, "width"),
            height = extractJsonInt(content, "height") ?: extractJsonInt(extra, "height"),
        )

        MessageType.VOICE -> ParsedContent(
            type = type,
            attachmentUrl = extractMediaUrl(content, extra),
            duration = extractJsonInt(content, "duration") ?: extractJsonInt(extra, "duration")
        )

        MessageType.FILE -> ParsedContent(
            type = type,
            attachmentUrl = extractMediaUrl(content, extra),
            fileName = extractFileName(content, extra),
            fileSize = extractFileSize(content, extra),
        )

        MessageType.LOCATION -> ParsedContent(
            type = type,
            latitude = extractJsonDouble(content, "latitude")
                ?: extractJsonDouble(content, "lat")
                ?: extractJsonDouble(extra, "latitude")
                ?: extractJsonDouble(extra, "lat"),
            longitude = extractJsonDouble(content, "longitude")
                ?: extractJsonDouble(content, "lng")
                ?: extractJsonDouble(extra, "longitude")
                ?: extractJsonDouble(extra, "lng"),
            address = extractJsonString(content, "address")
                ?: extractJsonString(content, "name")
                ?: extractJsonString(extra, "address")
                ?: extractJsonString(extra, "name")
        )

        MessageType.STICKER -> ParsedContent(
            type = type,
            attachmentUrl = extractMediaUrl(content, extra),
            text = extractJsonString(content, "name")
                ?: extractJsonString(content, "emoji")
                ?: extractJsonString(extra, "name")
                ?: extractJsonString(extra, "emoji")
        )

        MessageType.LINK -> ParsedContent(
            type = type,
            linkUrl = extractJsonString(content, "url")
                ?: extractJsonString(extra, "url"),
            linkTitle = extractJsonString(content, "title")
                ?: extractJsonString(extra, "title"),
            linkDescription = extractJsonString(content, "description")
                ?: extractJsonString(extra, "description"),
            thumbnailUrl = extractThumbnailUrl(content, extra)
        )

        MessageType.SYSTEM -> ParsedContent(
            type = type,
            text = extractPayloadContent(content)
                ?: extractJsonString(content, "text")
                ?: extractJsonString(content, "tip")
                ?: content
        )

        MessageType.UNKNOWN -> ParsedContent(
            type = type,
            text = extractPayloadContent(content) ?: content
        )
    }
}

/**
 * MessageEntry 扩展：获取解析后的内容
 */
val MessageEntry.parsedContent: ParsedContent
    get() = parseMessageContent(this)

/**
 * MessageEntry 扩展：获取纯文本预览。
 *
 * 撤回文案由状态（`isRevoked`）决定，与内容类型无关——
 * 否则按内容类型映射预览。
 */
val MessageEntry.textPreview: String
    get() {
        if (isRevoked) return "撤回了一条消息"
        val parsed = parsedContent
        return when (parsed.type) {
            MessageType.TEXT -> parsed.text ?: ""
            MessageType.IMAGE -> "[图片]"
            MessageType.VIDEO -> "[视频]"
            MessageType.VOICE -> "[语音] ${parsed.duration ?: 0}\""
            MessageType.FILE -> "[文件] ${parsed.fileName ?: ""}"
            MessageType.STICKER -> "[表情]"
            MessageType.LOCATION -> "[位置] ${parsed.address ?: ""}"
            MessageType.LINK -> "[链接] ${parsed.linkTitle ?: parsed.linkUrl ?: ""}"
            MessageType.SYSTEM -> parsed.text ?: "[系统消息]"
            MessageType.UNKNOWN -> "[消息]"
        }
    }

// ========== 简易 JSON 提取工具 ==========

private fun extractJsonString(json: String, key: String): String? {
    val patterns = listOf(
        "\"$key\":\"([^\"]*?)\"",
        "\"$key\": \"([^\"]*?)\"",
        "\"$key\":\"([^\"]*)\""
    )
    for (pattern in patterns) {
        val regex = Regex(pattern)
        val match = regex.find(json)
        if (match != null) {
            return match.groupValues[1]
        }
    }
    return null
}

private fun inferFileOrVideo(content: String, extra: String): MessageType {
    val mime = (
        extractJsonString(content, "mime_type")
            ?: extractJsonString(extra, "mime_type")
            ?: ""
        ).lowercase()
    return when {
        mime.startsWith("video/") -> MessageType.VIDEO
        mime.startsWith("image/") -> MessageType.IMAGE
        else -> MessageType.FILE
    }
}

private fun extractPayloadContent(content: String): String? {
    return extractJsonString(content, "content")
        ?.takeIf { it.isNotBlank() }
        ?: extractJsonString(content, "text")?.takeIf { it.isNotBlank() }
}

private fun normalizeUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    if (url.startsWith("/")) return "file://$url"
    return url
}

private fun extractMediaUrl(content: String, extra: String): String? {
    return normalizeUrl(extractJsonString(content, "file_url"))
        ?: normalizeUrl(extractJsonString(content, "url"))
        ?: normalizeUrl(extractJsonString(extra, "file_url"))
        ?: normalizeUrl(extractJsonString(extra, "url"))
        ?: content.takeIf { it.startsWith("/") }?.let { "file://$it" }
        ?: content.takeIf { it.startsWith("file://") }
}

private fun extractThumbnailUrl(content: String, extra: String): String? {
    return normalizeUrl(extractJsonString(content, "thumbnail_url"))
        ?: normalizeUrl(extractJsonString(content, "thumbnail"))
        ?: normalizeUrl(extractJsonString(content, "thumbnailUrl"))
        ?: normalizeUrl(extractJsonString(extra, "thumbnail_url"))
        ?: normalizeUrl(extractJsonString(extra, "thumbnail"))
        ?: normalizeUrl(extractJsonString(extra, "thumbnailUrl"))
        ?: extractMediaUrl(content, extra)
}

private fun extractFileName(content: String, extra: String): String? {
    val fromMeta = extractJsonString(content, "filename")
        ?: extractJsonString(content, "name")
        ?: extractJsonString(extra, "filename")
        ?: extractJsonString(extra, "name")
        ?: extractPayloadContent(content)
    return fromMeta
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.substringAfterLast('/')
        ?.substringAfterLast('\\')
}

private fun extractFileSize(content: String, extra: String): Long? {
    return extractJsonLong(content, "file_size")
        ?: extractJsonLong(content, "size")
        ?: extractJsonLong(extra, "file_size")
        ?: extractJsonLong(extra, "size")
}

private fun extractJsonInt(json: String, key: String): Int? {
    val patterns = listOf(
        "\"$key\":(\\d+)",
        "\"$key\": (\\d+)"
    )
    for (pattern in patterns) {
        val regex = Regex(pattern)
        val match = regex.find(json)
        if (match != null) {
            return match.groupValues[1].toIntOrNull()
        }
    }
    return null
}

private fun extractJsonLong(json: String, key: String): Long? {
    val patterns = listOf(
        "\"$key\":(\\d+)",
        "\"$key\": (\\d+)"
    )
    for (pattern in patterns) {
        val regex = Regex(pattern)
        val match = regex.find(json)
        if (match != null) {
            return match.groupValues[1].toLongOrNull()
        }
    }
    return null
}

private fun extractJsonDouble(json: String, key: String): Double? {
    val patterns = listOf(
        "\"$key\":([\\d.]+)",
        "\"$key\": ([\\d.]+)"
    )
    for (pattern in patterns) {
        val regex = Regex(pattern)
        val match = regex.find(json)
        if (match != null) {
            return match.groupValues[1].toDoubleOrNull()
        }
    }
    return null
}
