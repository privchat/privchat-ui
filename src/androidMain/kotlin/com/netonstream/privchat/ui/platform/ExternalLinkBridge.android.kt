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

    actual fun openMap(latitude: Double, longitude: Double, label: String?): Boolean {
        // geo:lat,lng?q=lat,lng(label) —— 系统选择默认地图 App（高德/百度/Google 等）。
        val q = if (label.isNullOrBlank()) "$latitude,$longitude"
        else "$latitude,$longitude(${Uri.encode(label)})"
        return openUri("geo:$latitude,$longitude?q=$q")
    }
}
