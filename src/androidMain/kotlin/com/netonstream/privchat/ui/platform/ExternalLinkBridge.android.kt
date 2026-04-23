package com.netonstream.privchat.ui.platform

import android.content.Context
import android.content.Intent
import android.net.Uri

actual object ExternalLinkBridge {

    private var appContext: Context? = null

    fun register(context: Context) {
        appContext = context.applicationContext
    }

    fun unregister() {
        appContext = null
    }

    actual fun openUri(uri: String): Boolean {
        val ctx = appContext ?: return false
        val parsed = runCatching { Uri.parse(uri) }.getOrNull() ?: return false
        val intent = Intent(Intent.ACTION_VIEW, parsed).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { ctx.startActivity(intent) }.isSuccess
    }
}
