package com.netonstream.privchat.ui.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

actual object ClipboardBridge {

    private var appContext: Context? = null

    fun register(context: Context) {
        appContext = context.applicationContext
    }

    fun unregister() {
        appContext = null
    }

    actual fun setText(text: String) {
        val ctx = appContext ?: return
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText("privchat_message", text))
    }
}
