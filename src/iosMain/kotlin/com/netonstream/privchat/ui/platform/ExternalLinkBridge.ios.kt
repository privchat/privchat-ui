package com.netonstream.privchat.ui.platform

import com.netonstream.privchat.ui.utils.CoordinateConverter
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual object ExternalLinkBridge {
    actual fun openUri(uri: String): Boolean {
        val url = NSURL.URLWithString(uri) ?: return false
        val app = UIApplication.sharedApplication
        if (!app.canOpenURL(url)) return false
        @Suppress("DEPRECATION")
        app.openURL(url)
        return true
    }

    actual fun openMap(latitude: Double, longitude: Double, coordinateSystem: String?, label: String?): Boolean {
        // Apple Maps 按 WGS-84：先把 gcj02/bd09 转成 WGS-84，再 maps://?ll=lat,lng[&q=label]。
        val (lat, lng) = CoordinateConverter.toWgs84(latitude, longitude, coordinateSystem)
        val base = "maps://?ll=$lat,$lng"
        if (!label.isNullOrBlank()) {
            val q = label.replace(" ", "%20")
            if (openUri("$base&q=$q")) return true
        }
        return openUri(base)
    }
}
