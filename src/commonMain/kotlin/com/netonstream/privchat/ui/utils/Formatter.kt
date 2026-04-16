package com.netonstream.privchat.ui.utils

import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.common.base.LocalDateTimeInfo
import com.netonstream.privchat.ui.common.base.currentTimeMillis
import com.netonstream.privchat.ui.common.base.epochMillisToLocalDateTime
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
            return "昨天"
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
            return "今天 $time"
        }

        // 昨天
        val yesterdayLocal = epochMillisToLocalDateTime(now - 86_400_000L, zone)
        if (yesterdayLocal.year == tsLocal.year && yesterdayLocal.month == tsLocal.month && yesterdayLocal.day == tsLocal.day) {
            return "昨天 $time"
        }

        // 同年
        if (nowLocal.year == tsLocal.year) {
            return "${tsLocal.month.toString().padStart(2, '0')}月${tsLocal.day.toString().padStart(2, '0')}日 $time"
        }

        // 不同年
        return "${tsLocal.year}年${tsLocal.month.toString().padStart(2, '0')}月${tsLocal.day.toString().padStart(2, '0')}日 $time"
    }

    /**
     * 格式化消息分隔符时间（ULong 版本）
     */
    fun messageSeparatorTime(timestamp: ULong): String = messageSeparatorTime(timestamp.toLong())

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

    /**
     * 格式化成员数
     *
     * @param count 成员数
     * @return 如 1.2万
     */
    fun memberCount(count: Int): String {
        return when {
            count < 0 -> "0"
            count < 10000 -> count.toString()
            count < 100000 -> "${formatDecimal(count / 10000.0, 1)}万"
            else -> "${count / 10000}万"
        }
    }

    /**
     * 格式化成员数（UInt 版本）
     */
    fun memberCount(count: UInt): String = memberCount(count.toInt())

    // ========== 私有辅助方法 ==========

    private fun formatHHmm(local: LocalDateTimeInfo): String {
        return "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
    }

    private fun formatMMdd(local: LocalDateTimeInfo): String {
        return "${local.month.toString().padStart(2, '0')}-${local.day.toString().padStart(2, '0')}"
    }

    private fun getDayOfWeek(isoDayOfWeek: Int): String {
        val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        return days.getOrElse(isoDayOfWeek - 1) { "周一" }
    }

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
