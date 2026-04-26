package com.netonstream.privchat.ui.media

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual object MediaSaver {
    private var appContext: Context? = null
    private const val ALBUM_NAME = "PrivChat"

    fun register(context: Context) {
        appContext = context.applicationContext
    }

    fun unregister() {
        appContext = null
    }

    actual suspend fun saveImage(localPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        val ctx = appContext ?: return@withContext Result.failure(IllegalStateException("MediaSaver 未注册"))
        val src = File(localPath)
        if (!src.exists()) return@withContext Result.failure(IllegalStateException("源文件不存在: $localPath"))

        val ext = src.extension.lowercase().ifEmpty { "jpg" }
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "image/jpeg"
        val displayName = "PrivChat_${System.currentTimeMillis()}.$ext"

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = ctx.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, mime)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM_NAME")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("MediaStore.insert 返回 null")
                try {
                    resolver.openOutputStream(uri).use { out ->
                        requireNotNull(out) { "openOutputStream 返回 null" }
                        src.inputStream().use { it.copyTo(out) }
                    }
                    val finish = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                    resolver.update(uri, finish, null, null)
                } catch (t: Throwable) {
                    runCatching { resolver.delete(uri, null, null) }
                    throw t
                }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val albumDir = File(picturesDir, ALBUM_NAME).apply { if (!exists()) mkdirs() }
                val target = File(albumDir, displayName)
                src.inputStream().use { input ->
                    target.outputStream().use { input.copyTo(it) }
                }
                MediaScannerConnection.scanFile(ctx, arrayOf(target.absolutePath), arrayOf(mime), null)
            }
            Unit
        }
    }
}
