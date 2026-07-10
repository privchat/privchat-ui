package com.netonstream.privchat.ui.avatar

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGContextFillRect
import platform.CoreGraphics.CGContextSetFillColorWithColor
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSDictionary
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.writeToFile
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.drawAtPoint
import platform.UIKit.drawInRect
import platform.UIKit.sizeWithAttributes

@OptIn(ExperimentalForeignApi::class)
actual object AvatarBitmapRenderer {

    actual suspend fun renderInitials(
        initials: String,
        bgArgb: Int,
        fgArgb: Int,
        sizePx: Int,
        outPath: String,
    ): Boolean = withContext(Dispatchers.Default) {
        render(sizePx, outPath) { s ->
            fillRect(bgArgb, 0.0, 0.0, s, s)
            drawCenteredText(initials, fgArgb, s / 2.0, s / 2.0, s * 0.4)
        }
    }

    actual suspend fun renderCollage(
        cells: List<CollageCell>,
        sizePx: Int,
        outPath: String,
    ): Boolean = withContext(Dispatchers.Default) {
        render(sizePx, outPath) { s ->
            fillRect(COLLAGE_BG_ARGB, 0.0, 0.0, s, s)
            val pad = s * 0.09
            val gap = s * 0.02
            val cell = (s - 2 * pad - 2 * gap) / 3.0
            for (i in 0 until 9) {
                val r = i / 3
                val c = i % 3
                val left = pad + c * (cell + gap)
                val top = pad + r * (cell + gap)
                drawCell(cells.getOrElse(i) { CollageCell.Empty }, left, top, cell)
            }
        }
    }

    actual fun fileExists(path: String): Boolean =
        NSFileManager.defaultManager.fileExistsAtPath(path)

    /** 建 image context → 绘制 → 取 PNG → 落盘。writeToFile(atomically) 内部即临时文件
     *  + rename 原子换入；但**不建父目录**，需先 createDirectory，且 **必须以 writeToFile
     *  的返回值为准**（否则父目录缺失时静默失败却被当成成功，UI 会加载到不存在的文件而空白）。 */
    private inline fun render(sizePx: Int, outPath: String, draw: (Double) -> Unit): Boolean {
        if (sizePx <= 0) return false
        val s = sizePx.toDouble()
        return runCatching {
            UIGraphicsBeginImageContextWithOptions(CGSizeMake(s, s), true, 1.0)
            val ok = try {
                draw(s)
                val image = UIGraphicsGetImageFromCurrentImageContext()
                val png = image?.let { UIImagePNGRepresentation(it) }
                if (png == null) {
                    false
                } else {
                    val parent = outPath.substringBeforeLast('/')
                    NSFileManager.defaultManager.createDirectoryAtPath(parent, true, null, null)
                    png.writeToFile(outPath, atomically = true)
                }
            } finally {
                UIGraphicsEndImageContext()
            }
            ok
        }.getOrDefault(false)
    }

    private fun drawCell(spec: CollageCell, left: Double, top: Double, size: Double) {
        when (spec) {
            is CollageCell.Image -> {
                val img = UIImage.imageWithContentsOfFile(spec.filePath)
                if (img == null) {
                    fillRect(AvatarPalette.DEFAULT_BACKGROUND_ARGB, left, top, size, size)
                    drawCenteredText(
                        "?", AvatarPalette.DEFAULT_FOREGROUND_ARGB,
                        left + size / 2.0, top + size / 2.0, size * 0.5,
                    )
                } else {
                    img.drawInRect(CGRectMake(left, top, size, size))
                }
            }
            is CollageCell.Initials -> {
                fillRect(spec.bgArgb, left, top, size, size)
                drawCenteredText(spec.text, spec.fgArgb, left + size / 2.0, top + size / 2.0, size * 0.5)
            }
            CollageCell.Empty -> fillRect(COLLAGE_EMPTY_ARGB, left, top, size, size)
        }
    }

    private fun fillRect(argb: Int, x: Double, y: Double, w: Double, h: Double) {
        val ctx = UIGraphicsGetCurrentContext() ?: return
        CGContextSetFillColorWithColor(ctx, argbColor(argb).CGColor)
        CGContextFillRect(ctx, CGRectMake(x, y, w, h))
    }

    private fun drawCenteredText(text: String, fgArgb: Int, cx: Double, cy: Double, fontSize: Double) {
        if (text.isEmpty()) return
        val attrs: NSDictionary = mapOf(
            NSFontAttributeName to UIFont.boldSystemFontOfSize(fontSize),
            NSForegroundColorAttributeName to argbColor(fgArgb),
        ) as NSDictionary
        val ns = text as NSString
        @Suppress("UNCHECKED_CAST")
        ns.sizeWithAttributes(attrs as Map<Any?, *>).useContents {
            ns.drawAtPoint(CGPointMake(cx - width / 2.0, cy - height / 2.0), attrs)
        }
    }

    private fun argbColor(argb: Int): UIColor {
        val a = ((argb ushr 24) and 0xFF) / 255.0
        val r = ((argb ushr 16) and 0xFF) / 255.0
        val g = ((argb ushr 8) and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0
        return UIColor(red = r, green = g, blue = b, alpha = a)
    }

    private val COLLAGE_BG_ARGB: Int = 0xFFD9DCE0.toInt()
    private val COLLAGE_EMPTY_ARGB: Int = 0xFFEDEFF2.toInt()
}
