package com.netonstream.privchat.ui.utils

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 坐标系转换：WGS-84（GPS/Apple/Google）/ GCJ-02（高德/腾讯，中国"火星坐标"）/
 * BD-09（百度）互转。
 *
 * 用途：消息协议是 provider-neutral 的——location 消息带 `coordinate_system`
 * （wgs84/gcj02/bd09）。接收端打开系统地图（Apple/Google 按 WGS-84）前，必须把
 * 非 WGS-84 坐标转成 WGS-84，否则会偏移几百米。
 *
 * 算法是中国测绘偏移的通用公式（境外坐标不偏移，直接返回）。GCJ-02→WGS-84 用
 * 一次反向近似（误差米级，足够打开地图定位）。
 */
object CoordinateConverter {

    private const val PI = 3.1415926535897932384626
    private const val A = 6378245.0 // 长半轴
    private const val EE = 0.00669342162296594323 // 偏心率平方
    private val X_PI = PI * 3000.0 / 180.0

    /** 将任意来源坐标统一转成 WGS-84（供 Apple/Google/系统地图使用）。 */
    fun toWgs84(latitude: Double, longitude: Double, coordinateSystem: String?): Pair<Double, Double> =
        when (coordinateSystem?.lowercase()) {
            "gcj02", "gcj-02" -> gcj02ToWgs84(latitude, longitude)
            "bd09", "bd-09", "bd09ll" -> {
                val (gLat, gLng) = bd09ToGcj02(latitude, longitude)
                gcj02ToWgs84(gLat, gLng)
            }
            // wgs84 / null / unknown → 原样（best-effort，不假装转换）
            else -> latitude to longitude
        }

    private fun outOfChina(lat: Double, lng: Double): Boolean =
        lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271

    /** WGS-84 → GCJ-02。 */
    fun wgs84ToGcj02(lat: Double, lng: Double): Pair<Double, Double> {
        if (outOfChina(lat, lng)) return lat to lng
        var dLat = transformLat(lng - 105.0, lat - 35.0)
        var dLng = transformLng(lng - 105.0, lat - 35.0)
        val radLat = lat / 180.0 * PI
        var magic = sin(radLat)
        magic = 1 - EE * magic * magic
        val sqrtMagic = sqrt(magic)
        dLat = dLat * 180.0 / (A * (1 - EE) / (magic * sqrtMagic) * PI)
        dLng = dLng * 180.0 / (A / sqrtMagic * cos(radLat) * PI)
        return (lat + dLat) to (lng + dLng)
    }

    /** GCJ-02 → WGS-84（一次反向近似，误差米级）。 */
    fun gcj02ToWgs84(lat: Double, lng: Double): Pair<Double, Double> {
        if (outOfChina(lat, lng)) return lat to lng
        val (gLat, gLng) = wgs84ToGcj02(lat, lng)
        return (lat * 2 - gLat) to (lng * 2 - gLng)
    }

    /** BD-09 → GCJ-02。 */
    fun bd09ToGcj02(lat: Double, lng: Double): Pair<Double, Double> {
        val x = lng - 0.0065
        val y = lat - 0.006
        val z = sqrt(x * x + y * y) - 0.00002 * sin(y * X_PI)
        val theta = atan2(y, x) - 0.000003 * cos(x * X_PI)
        return (z * sin(theta)) to (z * cos(theta))
    }

    private fun transformLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * PI) + 320.0 * sin(y * PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun transformLng(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }
}
