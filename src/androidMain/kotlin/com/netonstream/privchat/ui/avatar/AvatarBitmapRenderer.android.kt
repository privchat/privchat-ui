package com.netonstream.privchat.ui.avatar

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual object AvatarBitmapRenderer {

    actual suspend fun renderInitials(
        initials: String,
        bgArgb: Int,
        fgArgb: Int,
        sizePx: Int,
        outPath: String,
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(bgArgb)
            drawCenteredText(canvas, initials, fgArgb, sizePx / 2f, sizePx / 2f, sizePx * 0.4f)
            writePng(bmp, outPath)
            bmp.recycle()
            true
        }.getOrDefault(false)
    }

    actual suspend fun renderCollage(
        cells: List<CollageCell>,
        sizePx: Int,
        outPath: String,
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(COLLAGE_BG_ARGB)
            val pad = sizePx * 0.09f
            val gap = sizePx * 0.02f
            val cell = (sizePx - 2 * pad - 2 * gap) / 3f
            for (i in 0 until 9) {
                val r = i / 3
                val c = i % 3
                val left = pad + c * (cell + gap)
                val top = pad + r * (cell + gap)
                drawCell(canvas, cells.getOrElse(i) { CollageCell.Empty }, left, top, cell)
            }
            writePng(bmp, outPath)
            bmp.recycle()
            true
        }.getOrDefault(false)
    }

    actual fun fileExists(path: String): Boolean = File(path).exists()

    private fun drawCell(canvas: Canvas, spec: CollageCell, left: Float, top: Float, size: Float) {
        when (spec) {
            is CollageCell.Image -> {
                val src = BitmapFactory.decodeFile(spec.filePath)
                if (src == null) {
                    drawInitialsCell(
                        canvas, "?",
                        AvatarPalette.DEFAULT_BACKGROUND_ARGB,
                        AvatarPalette.DEFAULT_FOREGROUND_ARGB,
                        left, top, size,
                    )
                } else {
                    canvas.drawBitmap(
                        src, null,
                        RectF(left, top, left + size, top + size),
                        Paint(Paint.FILTER_BITMAP_FLAG),
                    )
                    src.recycle()
                }
            }
            is CollageCell.Initials ->
                drawInitialsCell(canvas, spec.text, spec.bgArgb, spec.fgArgb, left, top, size)
            CollageCell.Empty ->
                canvas.drawRect(
                    left, top, left + size, top + size,
                    Paint().apply { color = COLLAGE_EMPTY_ARGB },
                )
        }
    }

    private fun drawInitialsCell(
        canvas: Canvas,
        text: String,
        bgArgb: Int,
        fgArgb: Int,
        left: Float,
        top: Float,
        size: Float,
    ) {
        canvas.drawRect(left, top, left + size, top + size, Paint().apply { color = bgArgb })
        drawCenteredText(canvas, text, fgArgb, left + size / 2f, top + size / 2f, size * 0.5f)
    }

    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        fgArgb: Int,
        cx: Float,
        cy: Float,
        textSize: Float,
    ) {
        if (text.isEmpty()) return
        val paint = Paint().apply {
            color = fgArgb
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            this.textSize = textSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val m = paint.fontMetrics
        canvas.drawText(text, cx, cy - (m.ascent + m.descent) / 2f, paint)
    }

    /** `.part` 临时文件 + rename 原子换入，避免读到半写文件。 */
    private fun writePng(bmp: Bitmap, outPath: String) {
        val file = File(outPath)
        file.parentFile?.mkdirs()
        val tmp = File("$outPath.part")
        tmp.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }

    // 与 web 九宫格一致的外底 / 空位色。
    private val COLLAGE_BG_ARGB: Int = 0xFFD9DCE0.toInt()
    private val COLLAGE_EMPTY_ARGB: Int = 0xFFEDEFF2.toInt()
}
