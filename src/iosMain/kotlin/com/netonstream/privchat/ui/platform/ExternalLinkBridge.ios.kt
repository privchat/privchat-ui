package com.netonstream.privchat.ui.platform

import com.netonstream.privchat.ui.utils.CoordinateConverter
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual object ExternalLinkBridge {
    /**
     * 🔴 必须用 `openURL:options:completionHandler:`。单参数的 `openURL:` 是 iOS 10 就废弃的
     * 同步接口，新系统上调了什么也不发生——登录页的协议链接、关于页的入口、tel:/mailto:
     * 全都像死按钮，而返回值还是 true（推送设置跳转踩过同一个坑）。
     */
    actual fun openUri(uri: String): Boolean {
        val url = NSURL.URLWithString(uri) ?: return false
        val app = UIApplication.sharedApplication
        if (!app.canOpenURL(url)) return false
        app.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
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
