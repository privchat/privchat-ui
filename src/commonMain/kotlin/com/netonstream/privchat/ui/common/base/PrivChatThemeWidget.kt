package com.netonstream.privchat.ui.common.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.gearui.theme.Theme
import com.gearui.theme.ThemeMode
import com.gearui.theme.Colors
import com.tencent.kuikly.compose.ui.graphics.Color

data class ChatColors(
    val bubbleSelf: Color,
    val onBubbleSelf: Color,
    val bubbleOther: Color,
    val onBubbleOther: Color,
)

/**
 * PrivChat 主题扩展
 *
 * 基于 GearTheme 的聊天专用主题扩展
 *
 * 对标: TencentCloudChatThemeWidget
 */
object PrivChatThemeExtension {
    /**
     * 主题明暗判定。
     *
     * **不能拿 background 跟某个具体色值比**：gearui 把暗色阶梯从 zinc(`#09090B`) 换成中性
     * (`#0A0A0A`) 后，那种写法就静默失效了——气泡退回亮色配色，而 `Theme.colors.*` 仍是暗色
     * 值，于是「白色 primary 画在浅色气泡上」，手机号/网址/@提及整段隐形。
     * 按亮度判断对任何主题（含品牌覆盖色）都成立。
     */
    private val Colors.isDarkTheme: Boolean
        get() = (background.red + background.green + background.blue) < 1.5f

    val Colors.chatColors: ChatColors
        get() {
            val isDark = isDarkTheme
            return if (isDark) {
                ChatColors(
                    bubbleSelf = Color(0xFF3F3F46),
                    onBubbleSelf = Color(0xFFFAFAFA),
                    bubbleOther = Color(0xFF1A1C24),
                    onBubbleOther = Color(0xFFFAFAFA),
                )
            } else {
                ChatColors(
                    bubbleSelf = Color(0xFF18181B),
                    onBubbleSelf = Color(0xFFFFFFFF),
                    bubbleOther = Color(0xFFF4F4F5),
                    onBubbleOther = Color(0xFF09090B),
                )
            }
        }

    /**
     * 消息气泡颜色 - 发送方
     */
    val Colors.messageBubbleSelf: Color
        get() = chatColors.bubbleSelf

    /**
     * 消息气泡颜色 - 接收方
     */
    val Colors.messageBubbleOther: Color
        get() = chatColors.bubbleOther

    /**
     * 消息文本颜色 - 发送方
     */
    val Colors.messageTextSelf: Color
        get() = chatColors.onBubbleSelf

    /**
     * 消息文本颜色 - 接收方
     */
    val Colors.messageTextOther: Color
        get() = chatColors.onBubbleOther

    /**
     * 对方气泡里的链接/手机号/@提及色。
     *
     * 不能用 `primary`：那是「主按钮底色」，暗色主题下是近白，跟气泡文字色撞在一起。
     * `info` 是链接语义色，亮暗两套值都是照着可读性调的。
     */
    val Colors.messageLinkOther: Color
        get() = info

    /**
     * 未读消息数量徽章
     */
    val Colors.unreadBadge: Color
        get() = destructive

    /**
     * 在线状态颜色
     */
    val Colors.onlineStatus: Color
        get() = success

    /**
     * 离线状态颜色
     */
    val Colors.offlineStatus: Color
        get() = mutedForeground

    /**
     * 忙碌状态颜色
     */
    val Colors.busyStatus: Color
        get() = destructive

    /**
     * 输入框背景
     */
    val Colors.inputAreaBackground: Color
        get() = muted

    /**
     * 会话项背景
     */
    val Colors.conversationItemBackground: Color
        get() = surface

    /**
     * 会话项选中背景
     */
    val Colors.conversationItemSelectedBackground: Color
        get() = muted

    /**
     * 时间戳颜色
     */
    val Colors.timestamp: Color
        get() = mutedForeground
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
