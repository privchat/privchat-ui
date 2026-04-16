package com.netonstream.privchat.ui.common.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.gearui.theme.Theme
import com.gearui.theme.ThemeMode
import com.gearui.theme.GearColors

/**
 * PrivChat 主题扩展
 *
 * 基于 GearTheme 的聊天专用主题扩展
 *
 * 对标: TencentCloudChatThemeWidget
 */
object PrivChatThemeExtension {
    /**
     * 消息气泡颜色 - 发送方
     */
    val GearColors.messageBubbleSelf: com.tencent.kuikly.compose.ui.graphics.Color
        get() = primary

    /**
     * 消息气泡颜色 - 接收方
     */
    val GearColors.messageBubbleOther: com.tencent.kuikly.compose.ui.graphics.Color
        get() = surfaceVariant

    /**
     * 消息文本颜色 - 发送方
     */
    val GearColors.messageTextSelf: com.tencent.kuikly.compose.ui.graphics.Color
        get() = onPrimary

    /**
     * 消息文本颜色 - 接收方
     */
    val GearColors.messageTextOther: com.tencent.kuikly.compose.ui.graphics.Color
        get() = textPrimary

    /**
     * 未读消息数量徽章
     */
    val GearColors.unreadBadge: com.tencent.kuikly.compose.ui.graphics.Color
        get() = danger

    /**
     * 在线状态颜色
     */
    val GearColors.onlineStatus: com.tencent.kuikly.compose.ui.graphics.Color
        get() = success

    /**
     * 离线状态颜色
     */
    val GearColors.offlineStatus: com.tencent.kuikly.compose.ui.graphics.Color
        get() = textPlaceholder

    /**
     * 忙碌状态颜色
     */
    val GearColors.busyStatus: com.tencent.kuikly.compose.ui.graphics.Color
        get() = danger

    /**
     * 输入框背景
     */
    val GearColors.inputAreaBackground: com.tencent.kuikly.compose.ui.graphics.Color
        get() = surfaceVariant

    /**
     * 会话项背景
     */
    val GearColors.conversationItemBackground: com.tencent.kuikly.compose.ui.graphics.Color
        get() = surface

    /**
     * 会话项选中背景
     */
    val GearColors.conversationItemSelectedBackground: com.tencent.kuikly.compose.ui.graphics.Color
        get() = primaryLight

    /**
     * 时间戳颜色
     */
    val GearColors.timestamp: com.tencent.kuikly.compose.ui.graphics.Color
        get() = textSecondary
}

/**
 * PrivChat 主题提供者
 *
 * 包装 GearTheme，提供聊天专用的主题配置
 */
@Composable
fun PrivChatTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val mode = if (darkTheme) ThemeMode.Dark else ThemeMode.Light

    Theme(mode = mode) {
        content()
    }
}
