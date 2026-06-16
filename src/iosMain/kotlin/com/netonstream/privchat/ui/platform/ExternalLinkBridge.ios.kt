package com.netonstream.privchat.ui.platform

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

    actual fun openMap(latitude: Double, longitude: Double, label: String?): Boolean {
        // Apple Maps：maps://?ll=lat,lng[&q=label]。label 含特殊字符解析失败时退回纯坐标。
        val base = "maps://?ll=$latitude,$longitude"
        if (!label.isNullOrBlank()) {
            val q = label.replace(" ", "%20")
            if (openUri("$base&q=$q")) return true
        }
        return openUri(base)
    }
}
