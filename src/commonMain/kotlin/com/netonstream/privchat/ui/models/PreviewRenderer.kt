package com.netonstream.privchat.ui.models

import com.netonstream.privchat.sdk.dto.ChannelListEntry
import com.netonstream.privchat.sdk.dto.LatestChannelEvent
import com.netonstream.privchat.sdk.dto.MessageEntry
import com.netonstream.privchat.ui.i18n.PrivChatStrings

/**
 * 会话列表 / 消息预览渲染**单点入口**（架构归正后唯一渲染源）。
 *
 * 设计原则（参见 SYSTEM_MESSAGE_SPEC §3 + §4 + 架构归正讨论）：
 * 1. **永不**返回原始 JSON 给用户看——任何 messageType 都映射到本地化标签或模板渲染
 * 2. 所有标签都走 [PrivChatStrings]（i18n 唯一来源，Rust SDK 不再硬编码）
 * 3. 系统消息按 `template + refs` 渲染，与气泡渲染器共享同一个模板系统
 * 4. 不依赖 Composable / 无副作用——纯函数，便于测试 & 各处复用
 *
 * 使用：
 * ```
 * val strings = PrivChatI18n.strings
 * val text = strings.previewOf(channel)   // 会话列表
 * val text = strings.previewOf(message)   // 消息列表
 * ```
 */

/**
 * 从会话列表 entry 渲染最后一条消息的预览文本。
 *
 * 取 [LatestChannelEvent] 里的 `messageType` + `isRevoked` + `content`，
 * 用 [PrivChatStrings] 做本地化渲染。null event 返回空串。
 */
fun PrivChatStrings.previewOf(channel: ChannelListEntry): String {
    val event = channel.latestEvent ?: return ""
    return renderPreview(
        strings = this,
        messageType = event.messageType,
        isRevoked = event.isRevoked,
        rawContent = event.content,
    )
}

/**
 * 从消息 entry 渲染预览（用于转发预览、回复摘要等）。
 *
 * 撤回状态从 [MessageEntry.isRevoked] 取，类型从 messageType 取，
 * 内容透过 [parsedContent] 的解析结果。
 */
fun PrivChatStrings.previewOf(message: MessageEntry): String {
    val parsed = message.parsedContent
    return renderPreview(
        strings = this,
        messageType = message.messageType,
        isRevoked = message.isRevoked,
        rawContent = message.content,
        parsed = parsed,
    )
}

/**
 * 统一渲染逻辑——所有类型分支在这里收敛。
 *
 * @param rawContent 原始 content（TEXT 时是文本，其他类型是结构化 JSON）
 * @param parsed 可选的预解析结果；不传时按 [rawContent] 重新解析
 */
private fun renderPreview(
    strings: PrivChatStrings,
    messageType: Int?,
    isRevoked: Boolean,
    rawContent: String,
    parsed: ParsedContent? = null,
): String {
    if (isRevoked) return strings.previewRecalled

    // 优先以协议 messageType 分派；未给（如 SDK 同步未就绪）时回退到 content 嗅探
    val type = when (messageType) {
        null -> parseMessageType(rawContent)
        else -> parseMessageType(messageType, rawContent, "")
    }
    val effectiveParsed = parsed ?: when {
        // 重用 parse 逻辑，但要保证给到 content 字符串
        rawContent.isNotEmpty() -> parseMessageContent(rawContent)
        else -> ParsedContent(type = type)
    }

    return when (type) {
        MessageType.TEXT -> effectiveParsed.text?.takeIf { it.isNotBlank() } ?: rawContent.trim()

        MessageType.IMAGE -> strings.previewImage

        MessageType.VIDEO -> strings.previewVideo

        MessageType.VOICE -> {
            val dur = effectiveParsed.duration
            if (dur != null && dur > 0) strings.previewVoiceWithDuration.replace("{0}", dur.toString())
            else strings.previewVoice
        }

        MessageType.FILE -> {
            val name = effectiveParsed.fileName?.takeIf { it.isNotBlank() }
            if (name != null) strings.previewFileWithName.replace("{0}", name)
            else strings.previewFile
        }

        MessageType.STICKER -> strings.previewSticker

        MessageType.LOCATION -> {
            val addr = effectiveParsed.address?.takeIf { it.isNotBlank() }
            if (addr != null) strings.previewLocationWithAddress.replace("{0}", addr)
            else strings.previewLocation
        }

        MessageType.LINK -> {
            val title = effectiveParsed.linkTitle?.takeIf { it.isNotBlank() }
                ?: effectiveParsed.linkUrl?.takeIf { it.isNotBlank() }
                ?: ""
            strings.previewLink.replace("{0}", title)
        }

        MessageType.SYSTEM -> renderSystemPreview(strings, effectiveParsed, rawContent)

        MessageType.UNKNOWN -> strings.previewUnknown
    }
}

/**
 * 系统消息预览：走 [PrivChatStrings.systemTemplates] + refs 占位符替换，
 * 跟气泡 `SystemMessageRow` / `SystemTemplateText` 使用同一套模板规则。
 *
 * 渲染失败（没有 template / 没有 refs / key 未命中且非字面模板）→ 兜底
 * `previewSystemFallback`。**任何情况下都不会返回原始 JSON**。
 */
private fun renderSystemPreview(
    strings: PrivChatStrings,
    parsed: ParsedContent,
    rawContent: String,
): String {
    val template = parsed.systemTemplate
    if (template == null) {
        // 老的纯文本系统消息（少数遗留）：parsed.text 在新版解析里不再 fallback 到 content；
        // 这里再保守判一下，若 text 是一个像 JSON 的串，直接兜底，避免 JSON 漏出去。
        val text = parsed.text?.takeIf { it.isNotBlank() && !looksLikeJson(it) }
        return text ?: strings.previewSystemFallback
    }

    val refs = parsed.systemRefs ?: emptyList()
    val isI18nKey = template.contains('.') &&
        template.all { it.isLowerCase() || it.isDigit() || it == '.' || it == '_' }
    val effective = if (isI18nKey) strings.systemTemplates[template] ?: return strings.previewSystemFallback
        else template

    // 渲染：{i} 单值替换 + {n+} 列表展开
    val regex = Regex("\\{(\\d+)(\\+)?\\}")
    val result = StringBuilder()
    var cursor = 0
    regex.findAll(effective).forEach { match ->
        if (match.range.first > cursor) {
            result.append(effective.substring(cursor, match.range.first))
        }
        val startIdx = match.groupValues[1].toIntOrNull() ?: -1
        val isList = match.groupValues[2] == "+"
        when {
            startIdx < 0 -> result.append(match.value)
            isList -> {
                val tail = if (startIdx < refs.size) refs.subList(startIdx, refs.size) else emptyList()
                tail.forEachIndexed { i, ref ->
                    if (i > 0) result.append(strings.systemListSeparator)
                    result.append(ref.text)
                }
            }
            else -> {
                val ref = refs.getOrNull(startIdx)
                result.append(ref?.text ?: match.value)
            }
        }
        cursor = match.range.last + 1
    }
    if (cursor < effective.length) result.append(effective.substring(cursor))
    val rendered = result.toString()
    return rendered.ifBlank { strings.previewSystemFallback }
}

/**
 * 启发式判断字符串是否像 JSON——只在系统消息兜底路径使用，避免 raw JSON 漏到 UI。
 * 容错宽松：以 `{` 开头 + 包含 `"` 的就当 JSON。
 */
private fun looksLikeJson(s: String): Boolean {
    val t = s.trim()
    return (t.startsWith("{") && t.contains("\"")) || (t.startsWith("[") && t.contains("\""))
}
