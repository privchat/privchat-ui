package com.netonstream.privchat.ui.common.base

import java.util.Calendar
import java.util.TimeZone

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

/**
 * Android 平台：UTC 毫秒 → 指定时区的本地日期时间
 */
actual fun epochMillisToLocalDateTime(epochMillis: Long, timeZoneId: String): LocalDateTimeInfo {
    val cal = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId))
    cal.timeInMillis = epochMillis
    val dow = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
    }
    return LocalDateTimeInfo(
        year = cal.get(Calendar.YEAR),
        month = cal.get(Calendar.MONTH) + 1,
        day = cal.get(Calendar.DAY_OF_MONTH),
        hour = cal.get(Calendar.HOUR_OF_DAY),
        minute = cal.get(Calendar.MINUTE),
        second = cal.get(Calendar.SECOND),
        dayOfWeek = dow,
    )
}

/**
 * Android 平台：获取系统默认时区 ID
 */
actual fun systemDefaultTimeZoneId(): String = TimeZone.getDefault().id
