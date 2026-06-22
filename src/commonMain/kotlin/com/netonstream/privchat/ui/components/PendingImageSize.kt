package com.netonstream.privchat.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * 发送中(pending)图片消息的占位比例兜底。
 *
 * 背景：附件消息上传完成后 metadata 才带 width/height，发送中本地行没有尺寸，
 * 气泡会退化竖向默认。这里从本地图片文件**一次性**读出「展示方向」尺寸（已应用 EXIF），
 * 供 [attachmentBubbleSize] 算比例。
 *
 * 关键：不走 `painter.intrinsicSize`——在 Kuikly/iOS 下那是 native bridge 调用、
 * 图未加载时会抛未捕获异常导致整个 App crash。这里用平台 API 同步读图片头（轻量），
 * 用 `remember(path)` 缓存，只读一次，不引入重组循环。读取失败返回 null（退回默认占位）。
 *
 * @return (width, height) 展示方向像素；path 为 null 或读取失败时返回 null。
 */
@Composable
fun rememberPendingImageSize(vararg paths: String?): Pair<Int, Int>? {
    val candidates = paths.filterNotNull().filter { it.isNotBlank() }
    if (candidates.isEmpty()) return null
    return remember(candidates) {
        // 按渲染优先级依次尝试（缩略图在前，原图在后）：pending 早期 payload 原图可能还没
        // 落盘（resolve_attachment_path 返回 None），但 thumb.webp 已生成且是渲染所用的图。
        candidates.firstNotNullOfOrNull { p ->
            runCatching { decodeLocalImageDisplaySize(p) }.getOrNull()
        }
    }
}

/** 读取本地图片文件的展示方向尺寸（应用 EXIF orientation）。失败返回 null。 */
internal expect fun decodeLocalImageDisplaySize(path: String): Pair<Int, Int>?
