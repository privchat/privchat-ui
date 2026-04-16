package com.netonstream.privchat.ui.utils

import com.netonstream.privchat.ui.common.base.currentTimeMillis
import kotlin.math.pow
import kotlin.math.round

/**
 * 格式化工具
 *
 * 统一的时间、文件大小等格式化方法
 */
object Formatter {

    private const val SECOND = 1000L
    private const val MINUTE = 60 * SECOND
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR

    // ========== 时间格式化 ==========

    /**
     * 格式化会话列表时间
     *
     * - 今天：HH:mm
     * - 昨天：昨天
     * - 本周：周X
     * - 更早：MM-dd
     *
     * @param timestamp 时间戳（毫秒）
     */
    fun conversationTime(timestamp: Long): String {
        if (timestamp <= 0) return ""

        val now = currentTimeMillis()
        val todayStart = now - (now % DAY)
        val yesterdayStart = todayStart - DAY
        val weekStart = todayStart - 6 * DAY

        return when {
            timestamp >= todayStart -> formatHHmm(timestamp)
            timestamp >= yesterdayStart -> "昨天"
            timestamp >= weekStart -> getDayOfWeek(timestamp)
            else -> formatMMdd(timestamp)
        }
    }

    /**
     * 格式化会话列表时间（ULong 版本）
     */
    fun conversationTime(timestamp: ULong): String = conversationTime(timestamp.toLong())

    /**
     * 格式化消息时间
     *
     * @param timestamp 时间戳（毫秒）
     * @return HH:mm
     */
    fun messageTime(timestamp: Long): String {
        if (timestamp <= 0) return ""
        return formatHHmm(timestamp)
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
     * @param timestamp 时间戳（毫秒）
     */
    fun messageSeparatorTime(timestamp: Long): String {
        if (timestamp <= 0) return ""

        val now = currentTimeMillis()
        val todayStart = now - (now % DAY)
        val yesterdayStart = todayStart - DAY

        val time = formatHHmm(timestamp)

        return when {
            timestamp >= todayStart -> "今天 $time"
            timestamp >= yesterdayStart -> "昨天 $time"
            else -> "${formatMMdd(timestamp)} $time"
        }
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

    private fun formatHHmm(timestamp: Long): String {
        // 使用简单计算，避免平台差异
        val totalMinutes = timestamp / MINUTE
        val hours = ((totalMinutes / 60) % 24).toInt()
        val minutes = (totalMinutes % 60).toInt()
        return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
    }

    private fun formatMMdd(timestamp: Long): String {
        // 简化实现，使用近似计算
        val days = timestamp / DAY
        // 简单的日期估算（不考虑闰年等，仅用于显示）
        val dayOfYear = (days % 365).toInt()
        val month = (dayOfYear / 30 + 1).coerceIn(1, 12)
        val day = (dayOfYear % 30 + 1).coerceIn(1, 31)
        return "${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }

    private fun getDayOfWeek(timestamp: Long): String {
        val days = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
        // 1970-01-01 是周四，索引为 4
        val dayIndex = ((timestamp / DAY + 4) % 7).toInt()
        return days[dayIndex]
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

// currentTimeMillis 从 com.netonstream.privchat.ui.common.base 导入
