package com.netonstream.privchat.ui.common.base

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * iOS 平台获取设备类型
 */
actual fun getDeviceScreenType(): DeviceScreenType {
    // iOS 默认返回移动端
    // TODO: 可以通过 UIDevice 检测是否是 iPad
    return DeviceScreenType.MOBILE
}

/**
 * iOS 平台获取当前时间戳（毫秒）
 */
actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
