package com.netonstream.privchat.ui.common.base

import androidx.compose.runtime.Composable
import com.gearui.theme.Theme

/**
 * PrivChat 状态组件基类
 *
 * 所有 PrivChat 组件的基础接口，提供:
 * - 平台感知的构建器选择 (mobile/desktop/tablet)
 * - 主题数据监听
 * - 屏幕适配方法
 *
 * 对标: TencentCloudChatState
 */
interface PrivChatStateWidget {
    /**
     * 默认构建器 (移动端)
     */
    @Composable
    fun defaultBuilder()

    /**
     * 桌面端构建器
     */
    @Composable
    fun desktopBuilder() {
        defaultBuilder()
    }

    /**
     * 平板构建器
     */
    @Composable
    fun tabletBuilder() {
        desktopBuilder()
    }
}

/**
 * 平台类型枚举
 */
enum class DeviceScreenType {
    MOBILE,
    TABLET,
    DESKTOP
}

/**
 * 获取当前设备类型
 */
expect fun getDeviceScreenType(): DeviceScreenType

/**
 * 获取当前时间戳（毫秒）
 * 跨平台实现
 */
expect fun currentTimeMillis(): Long

/**
 * 本地日期时间（年月日时分秒 + 星期）
 * 纯数据结构，不依赖任何平台日期类
 */
data class LocalDateTimeInfo(
    val year: Int,
    val month: Int,      // 1-12
    val day: Int,        // 1-31
    val hour: Int,       // 0-23
    val minute: Int,     // 0-59
    val second: Int,     // 0-59
    val dayOfWeek: Int,  // 1=周一 ... 7=周日 (ISO 8601)
)

/**
 * 将 UTC 毫秒时间戳转换为指定时区的本地日期时间
 *
 * @param epochMillis UTC 毫秒时间戳
 * @param timeZoneId IANA 时区 ID，如 "Asia/Shanghai"、"Asia/Ho_Chi_Minh"
 * @return 该时区下的本地日期时间
 */
expect fun epochMillisToLocalDateTime(epochMillis: Long, timeZoneId: String): LocalDateTimeInfo

/**
 * 获取系统默认时区 ID（IANA 格式）
 */
expect fun systemDefaultTimeZoneId(): String

/**
 * PrivChat 组件构建器
 *
 * 根据设备类型自动选择对应的构建器
 */
@Composable
fun PrivChatComponentBuilder(
    widget: PrivChatStateWidget
) {
    val colors = Theme.colors

    when (getDeviceScreenType()) {
        DeviceScreenType.MOBILE -> widget.defaultBuilder()
        DeviceScreenType.TABLET -> widget.tabletBuilder()
        DeviceScreenType.DESKTOP -> widget.desktopBuilder()
    }
}
