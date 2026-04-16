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
