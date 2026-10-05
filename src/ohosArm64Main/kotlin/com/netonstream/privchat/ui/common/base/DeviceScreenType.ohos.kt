@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.netonstream.privchat.ui.common.base

import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.value
import kotlinx.cinterop.set
import kotlinx.cinterop.LongVar
import platform.posix.CLOCK_REALTIME
import platform.posix.clock_gettime
import platform.posix.getenv
import platform.posix.localtime_r
import platform.posix.readlink
import platform.posix.setenv
import platform.posix.timespec
import platform.posix.tm
import platform.posix.tzset
import platform.posix.unsetenv

actual fun getDeviceScreenType(): DeviceScreenType = DeviceScreenType.MOBILE

actual fun currentTimeMillis(): Long = memScoped {
    val ts = alloc<timespec>()
    clock_gettime(CLOCK_REALTIME, ts.ptr)
    ts.tv_sec * 1000L + ts.tv_nsec / 1_000_000L
}

/** The zone the process already runs in; [epochMillisToLocalDateTime] uses it as is. */
private const val LOCAL_ZONE = "Local"

actual fun systemDefaultTimeZoneId(): String {
    getenv("TZ")?.toKString()?.takeIf { it.isNotBlank() }?.let { return it }
    // /etc/localtime -> .../zoneinfo/Area/City on systems that keep tzdata as files.
    memScoped {
        val buf = allocArray<ByteVar>(256)
        val n = readlink("/etc/localtime", buf, 255u)
        if (n > 0) {
            buf[n.toInt()] = 0
            val target = buf.toKString()
            val idx = target.indexOf("zoneinfo/")
            if (idx >= 0) return target.substring(idx + "zoneinfo/".length)
        }
    }
    return LOCAL_ZONE
}

actual fun epochMillisToLocalDateTime(epochMillis: Long, timeZoneId: String): LocalDateTimeInfo {
    val useLocal = timeZoneId == LOCAL_ZONE || timeZoneId == systemDefaultTimeZoneId()
    val previousTz = if (useLocal) null else getenv("TZ")?.toKString()
    if (!useLocal) {
        setenv("TZ", timeZoneId, 1)
        tzset()
    }
    try {
        return memScoped {
            val seconds = alloc<LongVar>()
            seconds.value = epochMillis / 1000L
            val t = alloc<tm>()
            localtime_r(seconds.ptr, t.ptr)
            LocalDateTimeInfo(
                year = t.tm_year + 1900,
                month = t.tm_mon + 1,
                day = t.tm_mday,
                hour = t.tm_hour,
                minute = t.tm_min,
                second = t.tm_sec,
                // tm_wday: 0=Sunday → ISO 1=Monday … 7=Sunday
                dayOfWeek = if (t.tm_wday == 0) 7 else t.tm_wday,
            )
        }
    } finally {
        if (!useLocal) {
            if (previousTz != null) setenv("TZ", previousTz, 1) else unsetenv("TZ")
            tzset()
        }
    }
}
