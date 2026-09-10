package com.netonstream.privchat.ui.common.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.gearui.theme.Theme
import com.gearui.theme.ThemeMode
import com.gearui.foundation.color.ContentStyle
import com.gearui.foundation.color.resolveContentStyle
import com.gearui.theme.Colors
import com.tencent.kuikly.compose.ui.graphics.Color
import kotlin.math.pow

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

    /**
     * 前景色按底色亮度自动取黑或白（WCAG 相对亮度近似）。
     *
     * 品牌主色可能是浅黄(#FFD238)、深绿(微信那种)、深红——浅底必须配黑字、
     * 深底必须配白字，写死任何一种都会在另一类品牌上变成低对比甚至不可读。
     * 0.6 的阈值把三类品牌都分对：黄 0.82 → 黑字；绿 0.57 / 红 0.23 / 蓝 0.12 → 白字。
     */
    private fun contentColorOn(background: Color): Color {
        val luminance = 0.2126f * background.red + 0.7152f * background.green + 0.0722f * background.blue
        return if (luminance > 0.6f) Color(0xFF0A0A0A) else Color(0xFFFFFFFF)
    }

    val Colors.chatColors: ChatColors
        get() {
            val isDark = isDarkTheme
            // 自己的气泡在亮暗两种模式下都用品牌主色（微信绿不分模式，同理）。
            //
            // 判据是 primary 的亮度而不是模式：BUILTIN（无品牌覆盖）的暗色主题 primary
            // 是原厂近白（#FAFAFA），拿来当气泡就是一块刺眼白板——亮度落在两端说明这是
            // 原厂中性主题，退回中性灰气泡；落在中段说明是真品牌色（Weey 黄 0.82、
            // 深红约 0.23），照用。
            val primaryLum =
                0.2126f * primary.red + 0.7152f * primary.green + 0.0722f * primary.blue
            val primaryIsBranded = primaryLum in 0.05f..0.9f
            return if (isDark && !primaryIsBranded) {
                ChatColors(
                    bubbleSelf = Color(0xFF3F3F46),
                    onBubbleSelf = Color(0xFFFAFAFA),
                    bubbleOther = Color(0xFF1A1C24),
                    onBubbleOther = Color(0xFFFAFAFA),
                )
            } else if (isDark) {
                ChatColors(
                    bubbleSelf = primary,
                    onBubbleSelf = contentColorOn(primary),
                    bubbleOther = Color(0xFF1A1C24),
                    onBubbleOther = Color(0xFFFAFAFA),
                )
            } else {
                // 自己的气泡=品牌主色（白标：Weey 黄 / PrivChat 蓝），
                // 文字随主色亮度取黑或白。气泡内的时间、「✓✓ 已读」、发送进度都由
                // onBubbleSelf 派生（见 MessageContent），所以两者必须成对切换。
                ChatColors(
                    bubbleSelf = primary,
                    onBubbleSelf = contentColorOn(primary),
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
     * 聊天各表面的内容样式：把**实际底色**交给 gearui 的解析器，拿回正文色/链接色/是否需要强调。
     *
     * 品牌决定气泡底色（微信绿、Telegram 蓝、闲鱼黄、Weey 黄……），我们**永远不为了链接去
     * 改品牌底色**；正文和链接一律按底色的实际对比度推导，所以深色主题里的浅黄气泡照样是
     * 深色字。判据是对比度，不是颜色名，也不是「现在是浅色还是深色模式」。
     *
     * 这套映射留在 PrivChat 这边：`bubbleSelf`/`bubbleOther`/`systemMessage` 是聊天组件的
     * 概念，gearui 只提供通用的「背景 → 内容色」能力，不该把业务命名塞进框架核心。
     */
    val Colors.selfBubbleStyle: ContentStyle
        get() = resolveContentStyle(chatColors.bubbleSelf)

    val Colors.otherBubbleStyle: ContentStyle
        get() = resolveContentStyle(chatColors.bubbleOther)

    /**
     * 系统消息是**独立表面**，不继承品牌气泡色。
     *
     * 它的正文也不再用全局 `mutedForeground`：浅色下 `#8A8A8E` 对 `#F4F4F5` 只有 3.13，
     * 达不到普通正文的 4.5——系统消息是需要阅读的内容。弱化改由字号/居中/底纹承担，
     * 不靠把字调灰。（全局 `mutedForeground` 本身不动，那是另一份盘点。）
     */
    val Colors.systemMessageStyle: ContentStyle
        get() = resolveContentStyle(muted)

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
