@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.netonstream.privchat.ui.platform

import com.netonstream.privchat.ui.utils.CoordinateConverter
import platform.Foundation.NSURL
import platform.Foundation.NSBundle
import platform.Foundation.NSThread
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKMapItem
import platform.MapKit.MKPlacemark
import platform.MapKit.MKLaunchOptionsDirectionsModeKey
import platform.MapKit.MKLaunchOptionsDirectionsModeDriving
import platform.UIKit.UIApplication

actual object ExternalLinkBridge {
    private fun tencentKey(): String? =
        (NSBundle.mainBundle.objectForInfoDictionaryKey(NavigationMapUrls.tencentKeyMetadata) as? String)
            ?.takeIf { it.isNotBlank() }

    actual fun installedNavigationMaps(): List<NavigationMapApp> {
        if (!NSThread.isMainThread) return emptyList()
        val names = mapOf("apple" to "Apple Maps", "google" to "Google Maps", "amap" to "AMap",
            "baidu" to "Baidu Maps", "tencent" to "Tencent Maps")
        return NavigationMapUrls.iosSchemes.mapNotNull { (id, scheme) ->
            if (id == "tencent" && tencentKey() == null) return@mapNotNull null
            val url = NSURL.URLWithString("$scheme://") ?: return@mapNotNull null
            if (UIApplication.sharedApplication.canOpenURL(url)) NavigationMapApp(id, names.getValue(id)) else null
        }
    }

    actual fun navigateTo(appId: String, latitude: Double, longitude: Double, crs: String?, label: String?): Boolean {
        if (!NSThread.isMainThread || appId !in NavigationMapUrls.iosSchemes) return false
        if (appId == "apple") {
            val (lat, lng) = NavigationMapUrls.wgsPoint(latitude, longitude, crs) ?: return false
            val url = NSURL.URLWithString("maps://") ?: return false
            if (!UIApplication.sharedApplication.canOpenURL(url)) return false
            val item = MKMapItem(placemark = MKPlacemark(coordinate = CLLocationCoordinate2DMake(lat, lng), addressDictionary = null))
            item.name = label
            return item.openInMapsWithLaunchOptions(mapOf(MKLaunchOptionsDirectionsModeKey to MKLaunchOptionsDirectionsModeDriving))
        }
        val key = if (appId == "tencent") tencentKey() else null
        val url = NavigationMapUrls.url(true, appId, latitude, longitude, crs, label, key) ?: return false
        return openUri(url)
    }
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
        if (!NavigationMapUrls.validPoint(latitude, longitude)) return false
        // Apple Maps 按 WGS-84：先把 gcj02/bd09 转成 WGS-84，再 maps://?ll=lat,lng[&q=label]。
        val (lat, lng) = CoordinateConverter.toWgs84(latitude, longitude, coordinateSystem)
        val base = "maps://?ll=$lat,$lng"
        if (!label.isNullOrBlank()) {
            val q = NavigationMapUrls.encode(label)
            if (openUri("$base&q=$q")) return true
        }
        return openUri(base)
    }
}
