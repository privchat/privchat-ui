package com.netonstream.privchat.ui.platform

/**
 * 跨平台的外部链接打开桥接。支持任意 URI scheme：
 * - https:// / http://（系统浏览器）
 * - tel:（拨号面板，不自动拨出）
 * - sms:（短信编辑器）
 * - mailto:（邮件草稿）
 *
 * Android 使用 `Intent.ACTION_VIEW`；iOS 使用 `UIApplication.openURL`。
 */
expect object ExternalLinkBridge {
    fun openUri(uri: String): Boolean

    /**
     * 在系统地图打开经纬度坐标（Android `geo:`→默认地图 App / iOS Apple Maps）。
     * [label] 是可选标注名。打不开返回 false（UI 静默忽略）。
     */
    fun openMap(latitude: Double, longitude: Double, label: String?): Boolean
}
