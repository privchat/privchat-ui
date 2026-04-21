package com.netonstream.privchat.ui.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

actual object MediaOpener {
    private var appContext: Context? = null

    fun register(context: Context) {
        appContext = context.applicationContext
    }

    fun unregister() {
        appContext = null
    }

    actual fun open(localPath: String, mimeType: String?): Boolean {
        val ctx = appContext ?: return false
        val file = File(localPath)
        if (!file.exists()) return false

        val authority = "${ctx.packageName}.fileprovider"
        val uri: Uri = runCatching {
            FileProvider.getUriForFile(ctx, authority, file)
        }.getOrElse { return false }

        val resolvedMime = mimeType?.takeIf { it.isNotBlank() }
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())
            ?: "*/*"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, resolvedMime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { ctx.startActivity(Intent.createChooser(intent, null).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }.isSuccess
    }
}
