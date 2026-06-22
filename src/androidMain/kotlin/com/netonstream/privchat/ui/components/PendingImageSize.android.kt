package com.netonstream.privchat.ui.components

import android.graphics.BitmapFactory
import android.media.ExifInterface

/**
 * BitmapFactory 只解码图片头（inJustDecodeBounds）拿原始像素宽高，再用 EXIF orientation
 * 校正成展示方向（旋转 90/270 时交换宽高）。`android.media.ExifInterface` 属 Android SDK，
 * 无需额外依赖。仅取比例用。
 */
internal actual fun decodeLocalImageDisplaySize(path: String): Pair<Int, Int>? {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, opts)
    var w = opts.outWidth
    var h = opts.outHeight
    if (w <= 0 || h <= 0) return null
    val orientation = runCatching {
        ExifInterface(path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val swap = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
        orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
        orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
        orientation == ExifInterface.ORIENTATION_TRANSVERSE
    return if (swap) h to w else w to h
}
