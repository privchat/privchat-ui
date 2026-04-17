package com.netonstream.privchat.ui.common.base

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitWeekday
import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.timeZoneWithName

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

/**
 * iOS 平台：UTC 毫秒 → 指定时区的本地日期时间
 */
actual fun epochMillisToLocalDateTime(epochMillis: Long, timeZoneId: String): LocalDateTimeInfo {
    val date = NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0)
    val calendar = NSCalendar.currentCalendar.copy() as NSCalendar
    calendar.timeZone = NSTimeZone.timeZoneWithName(timeZoneId) ?: NSTimeZone.localTimeZone
    val units = NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or
        NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond or NSCalendarUnitWeekday
    val components = calendar.components(units, date)
    // NSCalendar weekday: 1=Sunday, 2=Monday, ..., 7=Saturday → ISO: 1=Monday ... 7=Sunday
    val isoWeekday = when (components.weekday.toInt()) {
        1 -> 7  // Sunday
        else -> components.weekday.toInt() - 1
    }
    return LocalDateTimeInfo(
        year = components.year.toInt(),
        month = components.month.toInt(),
        day = components.day.toInt(),
        hour = components.hour.toInt(),
        minute = components.minute.toInt(),
        second = components.second.toInt(),
        dayOfWeek = isoWeekday,
    )
}

/**
 * iOS 平台：获取系统默认时区 ID
 */
actual fun systemDefaultTimeZoneId(): String = NSTimeZone.localTimeZone.name
