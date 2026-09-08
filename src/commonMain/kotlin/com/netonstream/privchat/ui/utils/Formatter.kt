package com.netonstream.privchat.ui.utils

import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.common.base.LocalDateTimeInfo
import com.netonstream.privchat.ui.common.base.currentTimeMillis
import com.netonstream.privchat.ui.common.base.epochMillisToLocalDateTime
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.netonstream.privchat.ui.i18n.withArgs
import kotlin.math.pow
import kotlin.math.round

/**
 * 格式化工具
 *
 * 统一的时间、文件大小等格式化方法。
 *
 * 时区策略：
 * - 所有时间戳均为 UTC 毫秒，数据层不做任何转换
 * - 格式化时按 PrivChat.timeZoneId 指定的时区解释并显示
 * - App 层可通过 PrivChat.setTimeZone("Asia/Ho_Chi_Minh") 切换
 */
object Formatter {

    // ========== 时间格式化 ==========

    /**
     * 格式化会话列表时间
     *
     * - 今天：HH:mm
     * - 昨天：昨天
     * - 本周内（7天内）：周X
     * - 更早：MM-dd
     *
     * @param timestamp UTC 时间戳（毫秒）
     */
    fun conversationTime(timestamp: Long): String {
        if (timestamp <= 0) return ""

        val zone = PrivChat.timeZoneId
        val now = currentTimeMillis()
        val nowLocal = epochMillisToLocalDateTime(now, zone)
        val tsLocal = epochMillisToLocalDateTime(timestamp, zone)

        // 同一天
        if (nowLocal.year == tsLocal.year && nowLocal.month == tsLocal.month && nowLocal.day == tsLocal.day) {
            return formatHHmm(tsLocal)
        }

        // 昨天：日期差1天（简化：用 now 减一天的开始判断）
        val yesterdayLocal = epochMillisToLocalDateTime(now - 86_400_000L, zone)
        if (yesterdayLocal.year == tsLocal.year && yesterdayLocal.month == tsLocal.month && yesterdayLocal.day == tsLocal.day) {
            return PrivChatI18n.current.timeYesterday
        }

        // 本周内（7天以内）
        val sevenDaysAgo = now - 7 * 86_400_000L
        if (timestamp >= sevenDaysAgo) {
            return getDayOfWeek(tsLocal.dayOfWeek)
        }

        // 更早
        return formatMMdd(tsLocal)
    }

    /**
     * 格式化会话列表时间（ULong 版本）
     */
    fun conversationTime(timestamp: ULong): String = conversationTime(timestamp.toLong())

    /**
     * 格式化消息时间
     *
     * @param timestamp UTC 时间戳（毫秒）
     * @return HH:mm
     */
    fun messageTime(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val zone = PrivChat.timeZoneId
        val local = epochMillisToLocalDateTime(timestamp, zone)
        return formatHHmm(local)
    }

    /**
     * 格式化消息时间（ULong 版本）
     */
    fun messageTime(timestamp: ULong): String = messageTime(timestamp.toLong())

    /**
     * 格式化消息分隔符时间
     *
     * - 今天：今天 HH:mm
     * - 昨天：昨天 HH:mm
     * - 本年：MM月dd日 HH:mm
     * - 更早：yyyy年MM月dd日 HH:mm
     *
     * @param timestamp UTC 时间戳（毫秒）
     */
    fun messageSeparatorTime(timestamp: Long): String {
        if (timestamp <= 0) return ""

        val zone = PrivChat.timeZoneId
        val now = currentTimeMillis()
        val nowLocal = epochMillisToLocalDateTime(now, zone)
        val tsLocal = epochMillisToLocalDateTime(timestamp, zone)
        val time = formatHHmm(tsLocal)

        // 今天
        if (nowLocal.year == tsLocal.year && nowLocal.month == tsLocal.month && nowLocal.day == tsLocal.day) {
            return dateWithTime(PrivChatI18n.current.timeToday, time)
        }

        // 昨天
        val yesterdayLocal = epochMillisToLocalDateTime(now - 86_400_000L, zone)
        if (yesterdayLocal.year == tsLocal.year && yesterdayLocal.month == tsLocal.month && yesterdayLocal.day == tsLocal.day) {
            return dateWithTime(PrivChatI18n.current.timeYesterday, time)
        }

        // 同年
        if (nowLocal.year == tsLocal.year) {
            return dateWithTime(monthDay(tsLocal, pad = true), time)
        }

        // 不同年
        return dateWithTime(yearMonthDay(tsLocal, pad = true), time)
    }

    /**
     * 格式化消息分隔符时间（ULong 版本）
     */
    fun messageSeparatorTime(timestamp: ULong): String = messageSeparatorTime(timestamp.toLong())

    /**
     * 日期标签（不含时间），用于 UX-5 浮动日期头 & 跨日期分隔线。
     *
     * - 今天 / 昨天
     * - 本周内：周X
     * - 同年：M月d日
     * - 跨年：yyyy年M月d日
     */
    fun messageDateLabel(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val zone = PrivChat.timeZoneId
        val now = currentTimeMillis()
        val nowLocal = epochMillisToLocalDateTime(now, zone)
        val tsLocal = epochMillisToLocalDateTime(timestamp, zone)
        if (nowLocal.year == tsLocal.year && nowLocal.month == tsLocal.month && nowLocal.day == tsLocal.day) {
            return PrivChatI18n.current.timeToday
        }
        val yesterdayLocal = epochMillisToLocalDateTime(now - 86_400_000L, zone)
        if (yesterdayLocal.year == tsLocal.year && yesterdayLocal.month == tsLocal.month && yesterdayLocal.day == tsLocal.day) {
            return PrivChatI18n.current.timeYesterday
        }
        if (timestamp >= now - 7 * 86_400_000L) {
            return getDayOfWeek(tsLocal.dayOfWeek)
        }
        if (nowLocal.year == tsLocal.year) {
            return monthDay(tsLocal, pad = false)
        }
        return yearMonthDay(tsLocal, pad = false)
    }

    fun messageDateLabel(timestamp: ULong): String = messageDateLabel(timestamp.toLong())

    /**
     * 完整日期时间 `YYYY-MM-DD HH:mm`（本地时区）。用于账单/提现/绑卡等**需要精确到分钟且
     * 不能相对化**的记录场景——"昨天"对一笔提现申请没有意义。0/负值返回 "-"。
     */
    fun absoluteDateTime(timestamp: Long): String {
        if (timestamp <= 0) return "-"
        val t = epochMillisToLocalDateTime(timestamp, PrivChat.timeZoneId)
        fun p(v: Int) = if (v < 10) "0$v" else "$v"
        return "${t.year}-${p(t.month)}-${p(t.day)} ${p(t.hour)}:${p(t.minute)}"
    }

    fun absoluteDateTime(timestamp: ULong): String = absoluteDateTime(timestamp.toLong())

    /**
     * 两个时间戳是否属于同一本地日。用于 UX-11 时间合并分组的跨日判断。
     */
    fun isSameLocalDay(a: Long, b: Long): Boolean {
        if (a <= 0 || b <= 0) return false
        val zone = PrivChat.timeZoneId
        val la = epochMillisToLocalDateTime(a, zone)
        val lb = epochMillisToLocalDateTime(b, zone)
        return la.year == lb.year && la.month == lb.month && la.day == lb.day
    }

    fun isSameLocalDay(a: ULong, b: ULong): Boolean = isSameLocalDay(a.toLong(), b.toLong())

    /** 好友申请页行内短日期——`dd/MM` 格式（零填充）。 */
    fun friendRequestShortDate(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val local = epochMillisToLocalDateTime(timestamp, PrivChat.timeZoneId)
        return "${local.day.toString().padStart(2, '0')}/${local.month.toString().padStart(2, '0')}"
    }

    fun friendRequestShortDate(timestamp: ULong): String = friendRequestShortDate(timestamp.toLong())

    /**
     * 好友申请页"我发送的"行的相对时间——`刚刚 / N 分钟前 / N 小时前 /
     * N 天前 / MM-dd / yyyy-MM-dd`。中文兜底（与 Formatter 其他方法一致）。
     */
    fun friendRequestRelativeShort(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val now = currentTimeMillis()
        val diffSec = (now - timestamp) / 1000L
        return when {
            diffSec < 60 -> PrivChatI18n.current.relativeJustNow
            diffSec < 3600 -> PrivChatI18n.current.relativeMinutesAgo.withArgs(diffSec / 60)
            diffSec < 86_400 -> PrivChatI18n.current.relativeHoursAgo.withArgs(diffSec / 3600)
            diffSec < 7 * 86_400 -> PrivChatI18n.current.relativeDaysAgo.withArgs(diffSec / 86_400)
            else -> {
                val local = epochMillisToLocalDateTime(timestamp, PrivChat.timeZoneId)
                val nowLocal = epochMillisToLocalDateTime(now, PrivChat.timeZoneId)
                if (nowLocal.year == local.year) {
                    formatMMdd(local)
                } else {
                    "${local.year}-${formatMMdd(local)}"
                }
            }
        }
    }

    fun friendRequestRelativeShort(timestamp: ULong): String =
        friendRequestRelativeShort(timestamp.toLong())

    /**
     * 好友申请"收到的"列表的月份分组判断：当前 local-year/month 是 ts 所在月吗？
     *
     * UI 据此把当月申请归到"{年}年{月}月"标题下，其余归到"更早"。
     */
    fun isInCurrentLocalMonth(timestamp: Long): Boolean {
        if (timestamp <= 0) return false
        val zone = PrivChat.timeZoneId
        val now = currentTimeMillis()
        val nowLocal = epochMillisToLocalDateTime(now, zone)
        val tsLocal = epochMillisToLocalDateTime(timestamp, zone)
        return nowLocal.year == tsLocal.year && nowLocal.month == tsLocal.month
    }

    fun isInCurrentLocalMonth(timestamp: ULong): Boolean = isInCurrentLocalMonth(timestamp.toLong())

    /** 当前 local 月份的展示标签（zh-flavor），用作"收到的"列表当月分组标题。 */
    fun currentLocalMonthLabel(): String {
        val nowLocal = epochMillisToLocalDateTime(currentTimeMillis(), PrivChat.timeZoneId)
        return PrivChatI18n.current.dateYearMonthPattern
            .replace("{y}", nowLocal.year.toString())
            .replace("{m}", nowLocal.month.toString())
    }

    /**
     * 在线状态「最近在线」相对时长展示（presence 真源，lastSeen 为 UTC 毫秒）。
     *
     * - < 1 分钟：刚刚（[justNow]）
     * - < 1 小时：N 分钟前在线（[minutesAgo]）
     * - < 1 天：N 小时前在线（[hoursAgo]）
     * - < 7 天：N 天前在线（[daysAgo]）
     * - 更早：回退到 [conversationTime]（日期）
     *
     * 文案模板由调用方传入（含 `%d` 占位），保持 4 语言一致。
     *
     * @param lastSeen UTC 毫秒时间戳；<=0 时返回 null（调用方退化为「离线」）
     */
    fun presenceLastSeen(
        lastSeen: Long,
        justNow: String,
        minutesAgo: String,
        hoursAgo: String,
        daysAgo: String,
    ): String? {
        if (lastSeen <= 0L) return null
        // 防御:老 server 下发 Unix 秒(契约是 UTC 毫秒),秒值会被格式化成 1970-01-21。
        @Suppress("NAME_SHADOWING")
        val lastSeen = if (lastSeen < 1_000_000_000_000L) lastSeen * 1000 else lastSeen
        val now = currentTimeMillis()
        val diffSec = (now - lastSeen) / 1000L
        return when {
            diffSec < 60 -> justNow
            diffSec < 3600 -> minutesAgo.replace("%d", (diffSec / 60).toString())
            diffSec < 86_400 -> hoursAgo.replace("%d", (diffSec / 3600).toString())
            diffSec < 7 * 86_400 -> daysAgo.replace("%d", (diffSec / 86_400).toString())
            else -> conversationTime(lastSeen)
        }
    }

    // ========== 时长格式化 ==========

    /**
     * 格式化时长（秒）
     *
     * @param seconds 秒数
     * @return mm:ss 或 H:mm:ss
     */
    fun duration(seconds: Int): String {
        if (seconds <= 0) return "0:00"

        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60

        return if (h > 0) {
            "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
        } else {
            "$m:${s.toString().padStart(2, '0')}"
        }
    }

    /**
     * 格式化语音时长
     *
     * @param seconds 秒数
     * @return X" 或 X'X"
     */
    fun voiceDuration(seconds: Int): String {
        if (seconds <= 0) return "0\""

        val m = seconds / 60
        val s = seconds % 60

        return if (m > 0) {
            "$m'$s\""
        } else {
            "$s\""
        }
    }

    // ========== 文件大小格式化 ==========

    /**
     * 格式化文件大小
     *
     * @param bytes 字节数
     * @return 如 1.5 MB
     */
    fun fileSize(bytes: Long): String {
        return when {
            bytes < 0 -> "0 B"
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> {
                val mb = bytes.toDouble() / (1024 * 1024)
                if (mb >= 100) "${mb.toInt()} MB"
                else "${formatDecimal(mb, 1)} MB"
            }

            else -> {
                val gb = bytes.toDouble() / (1024 * 1024 * 1024)
                "${formatDecimal(gb, 2)} GB"
            }
        }
    }

    /**
     * 格式化文件大小（ULong 版本）
     */
    fun fileSize(bytes: ULong): String = fileSize(bytes.toLong())

    // ========== 数量格式化 ==========

    /**
     * 格式化未读数
     *
     * @param count 未读数
     * @param max 最大显示数，超过显示 max+
     * @return 如 99+
     */
    fun unreadCount(count: Int, max: Int = 99): String {
        return when {
            count <= 0 -> ""
            count > max -> "$max+"
            else -> count.toString()
        }
    }

    /**
     * 格式化未读数（UInt 版本）
     */
    fun unreadCount(count: UInt, max: Int = 99): String = unreadCount(count.toInt(), max)

    // ========== 私有辅助方法 ==========

    private fun formatHHmm(local: LocalDateTimeInfo): String {
        return "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
    }

    private fun formatMMdd(local: LocalDateTimeInfo): String {
        return "${local.month.toString().padStart(2, '0')}-${local.day.toString().padStart(2, '0')}"
    }

    private fun getDayOfWeek(isoDayOfWeek: Int): String {
        val s = PrivChatI18n.current
        val days = listOf(
            s.timeMonday, s.timeTuesday, s.timeWednesday, s.timeThursday,
            s.timeFriday, s.timeSaturday, s.timeSunday,
        )
        return days.getOrElse(isoDayOfWeek - 1) { s.timeMonday }
    }

    /** 「几月几日」——顺序交给语言包，这里只填数字。 */
    private fun monthDay(local: LocalDateTimeInfo, pad: Boolean): String =
        PrivChatI18n.current.dateMonthDayPattern
            .replace("{m}", local.month.toString().let { if (pad) it.padStart(2, '0') else it })
            .replace("{d}", local.day.toString().let { if (pad) it.padStart(2, '0') else it })

    private fun yearMonthDay(local: LocalDateTimeInfo, pad: Boolean): String =
        PrivChatI18n.current.dateYearMonthDayPattern
            .replace("{y}", local.year.toString())
            .replace("{m}", local.month.toString().let { if (pad) it.padStart(2, '0') else it })
            .replace("{d}", local.day.toString().let { if (pad) it.padStart(2, '0') else it })

    private fun dateWithTime(date: String, time: String): String =
        PrivChatI18n.current.dateTimePattern.replace("{date}", date).replace("{time}", time)

    private fun formatDecimal(value: Double, digits: Int): String {
        val factor = 10.0.pow(digits.toDouble())
        val rounded = round(value * factor) / factor
        val raw = rounded.toString()
        val dot = raw.indexOf('.')
        if (dot < 0) return raw
        val decimals = raw.length - dot - 1
        if (decimals >= digits) return raw
        return raw + "0".repeat(digits - decimals)
    }
}
