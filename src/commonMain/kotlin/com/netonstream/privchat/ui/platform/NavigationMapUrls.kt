package com.netonstream.privchat.ui.platform

import com.netonstream.privchat.ui.utils.CoordinateConverter

internal object NavigationMapUrls {
    val androidPackages = mapOf(
        "google" to "com.google.android.apps.maps",
        "amap" to "com.autonavi.minimap",
        "baidu" to "com.baidu.BaiduMap",
        "tencent" to "com.tencent.map",
    )
    val iosSchemes = mapOf(
        "apple" to "maps", "google" to "comgooglemaps", "amap" to "iosamap",
        "baidu" to "baidumap", "tencent" to "qqmap",
    )
    const val tencentKeyMetadata = "PrivChatTencentMapsUriKey"

    fun validPoint(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0

    fun normalizedCrs(crs: String?): String? = when (crs?.lowercase()) {
        null, "wgs84", "wgs-84" -> "wgs84"
        "gcj02", "gcj-02" -> "gcj02"
        "bd09", "bd-09", "bd09ll" -> "bd09ll"
        else -> null
    }

    fun wgsPoint(latitude: Double, longitude: Double, crs: String?): Pair<Double, Double>? {
        if (!validPoint(latitude, longitude)) return null
        val source = normalizedCrs(crs) ?: return null
        return CoordinateConverter.toWgs84(latitude, longitude, source)
            .takeIf { validPoint(it.first, it.second) }
    }

    // RFC 3986 component encoding, including UTF-8 labels and reserved query delimiters.
    fun encode(value: String): String = buildString {
        val hex = "0123456789ABCDEF"
        for (byte in value.encodeToByteArray()) {
            val b = byte.toInt() and 255
            if (b in 65..90 || b in 97..122 || b in 48..57 || b == 45 || b == 46 || b == 95 || b == 126) {
                append(b.toChar())
            } else { append('%'); append(hex[b ushr 4]); append(hex[b and 15]) }
        }
    }

    fun url(ios: Boolean, appId: String, latitude: Double, longitude: Double, crs: String?,
            label: String?, tencentKey: String? = null): String? {
        if (!validPoint(latitude, longitude)) return null
        val source = normalizedCrs(crs) ?: return null
        val name = encode(label?.takeIf { it.isNotBlank() } ?: "$latitude,$longitude")
        return when (appId) {
            "google" -> {
                val (lat, lng) = wgsPoint(latitude, longitude, source) ?: return null
                if (ios) "comgooglemaps://?daddr=$lat,$lng&directionsmode=driving"
                else "google.navigation:q=$lat,$lng&mode=d"
            }
            "amap" -> {
                val (lat, lng) = if (source == "bd09ll") CoordinateConverter.bd09ToGcj02(latitude, longitude)
                    else latitude to longitude
                if (!validPoint(lat, lng)) return null
                val dev = if (source == "wgs84") 1 else 0
                "${if (ios) "iosamap" else "androidamap"}://navi?sourceApplication=PrivChat&poiname=$name&lat=$lat&lon=$lng&dev=$dev&style=2"
            }
            "baidu" -> {
                // Baidu direction binds the destination name and coordinates in one value.
                // A pipe in a label is a nested grammar delimiter, not escapable by URL encoding.
                val title = label?.takeIf { it.isNotBlank() && '|' !in it }
                val destination = if (title == null) "$latitude,$longitude"
                    else "name:$title|latlng:$latitude,$longitude"
                val origin = encode("\u6211\u7684\u4f4d\u7f6e") // Baidu's current-location token.
                "baidumap://map/direction?origin=$origin&destination=${encode(destination)}&coord_type=$source&mode=driving&src=${if (ios) "ios" else "andr"}.privchat.app"
            }
            "tencent" -> {
                // Tencent's official native URI contract requires a real developer key, not an app-name placeholder.
                val key = tencentKey?.takeIf { it.isNotBlank() } ?: return null
                val (lat, lng) = when (source) {
                    "gcj02" -> latitude to longitude
                    "bd09ll" -> CoordinateConverter.bd09ToGcj02(latitude, longitude)
                    else -> CoordinateConverter.wgs84ToGcj02(latitude, longitude)
                }
                if (!validPoint(lat, lng)) return null
                "qqmap://map/routeplan?type=drive&fromcoord=CurrentLocation&tocoord=$lat,$lng&to=$name&referer=${encode(key)}"
            }
            else -> null
        }
    }
}
