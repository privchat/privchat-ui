package com.netonstream.privchat.ui.components

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.UIImage

/**
 * `UIImage(contentsOfFile:)` 解码时会消费 EXIF orientation，`.size` 即「展示方向」尺寸
 * （横拍照片就是宽>高），正好是气泡比例需要的。只为取比例用，一次性、由上层 remember 缓存。
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun decodeLocalImageDisplaySize(path: String): Pair<Int, Int>? {
    val img = UIImage(contentsOfFile = path) ?: return null
    return img.size.useContents {
        val w = width.toInt()
        val h = height.toInt()
        if (w > 0 && h > 0) w to h else null
    }
}
