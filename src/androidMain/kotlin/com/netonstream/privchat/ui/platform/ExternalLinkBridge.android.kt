package com.netonstream.privchat.ui.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

    private fun handlers(context: Context, intent: Intent) = runCatching {
        context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .filter { it.activityInfo?.enabled == true && it.activityInfo.exported && it.activityInfo.applicationInfo.enabled }
    }.getOrDefault(emptyList())

    private fun tencentKey(context: Context): String? = runCatching {
        context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            .metaData?.getString(NavigationMapUrls.tencentKeyMetadata)
    }.getOrNull()?.takeIf { it.isNotBlank() }

    actual fun installedNavigationMaps(): List<NavigationMapApp> {
        val ctx = appContext ?: return emptyList()
        val generic = handlers(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0")))
        return NavigationMapUrls.androidPackages.mapNotNull { (id, pkg) ->
            if (id == "tencent" && tencentKey(ctx) == null) return@mapNotNull null
            val probe = when (id) {
                "google" -> "google.navigation:q=0,0"
                "amap" -> "androidamap://navi"
                "baidu" -> "baidumap://map/navi"
                "tencent" -> "qqmap://map/routeplan"
                else -> return@mapNotNull null
            }
            val activity = (generic + handlers(ctx, Intent(Intent.ACTION_VIEW, Uri.parse(probe)).setPackage(pkg)))
                .firstOrNull { it.activityInfo.packageName == pkg } ?: return@mapNotNull null
            NavigationMapApp(id, activity.loadLabel(ctx.packageManager).toString())
        }
    }

    actual fun navigateTo(appId: String, latitude: Double, longitude: Double, crs: String?, label: String?): Boolean {
        val ctx = appContext ?: return false
        val pkg = NavigationMapUrls.androidPackages[appId] ?: return false
        val key = if (appId == "tencent") tencentKey(ctx) else null
        val url = NavigationMapUrls.url(false, appId, latitude, longitude, crs, label, key) ?: return false
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (handlers(ctx, intent).none { it.activityInfo.packageName == pkg }) return false
        return runCatching { ctx.startActivity(intent) }.isSuccess
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
        if (!NavigationMapUrls.validPoint(latitude, longitude)) return false
        // 统一转 WGS-84 再喂 geo:（系统地图/Google 按 WGS-84）。
        val (lat, lng) = CoordinateConverter.toWgs84(latitude, longitude, coordinateSystem)
        // geo:lat,lng?q=lat,lng(label) —— 系统选择默认地图 App。
        val q = if (label.isNullOrBlank()) "$lat,$lng" else "$lat,$lng(${Uri.encode(label)})"
        return openUri("geo:$lat,$lng?q=$q")
    }
}
