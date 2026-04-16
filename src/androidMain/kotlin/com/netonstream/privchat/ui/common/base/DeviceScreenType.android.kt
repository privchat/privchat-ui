package com.netonstream.privchat.ui.common.base

/**
 * Android 平台获取设备类型
 */
actual fun getDeviceScreenType(): DeviceScreenType {
    // Android 默认返回移动端
    // TODO: 可以通过 Configuration 检测平板或桌面
    return DeviceScreenType.MOBILE
}

/**
 * Android 平台获取当前时间戳（毫秒）
 */
actual fun currentTimeMillis(): Long = System.currentTimeMillis()
