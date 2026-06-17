package com.netonstream.privchat.ui.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.netonstream.privchat.ui.utils.CoordinateConverter

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

    actual fun openMap(latitude: Double, longitude: Double, coordinateSystem: String?, label: String?): Boolean {
        // 统一转 WGS-84 再喂 geo:（系统地图/Google 按 WGS-84）。
        val (lat, lng) = CoordinateConverter.toWgs84(latitude, longitude, coordinateSystem)
        // geo:lat,lng?q=lat,lng(label) —— 系统选择默认地图 App。
        val q = if (label.isNullOrBlank()) "$lat,$lng" else "$lat,$lng(${Uri.encode(label)})"
        return openUri("geo:$lat,$lng?q=$q")
    }
}
