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
     * 可点击文本（链接/手机号/@提及）在**对方气泡与系统消息**上的颜色。
     *
     * 不能用 `primary`：那是「主按钮底色」，暗色主题下是近白，跟气泡文字色撞在一起。
     *
     * 也不再直接等于 `info`。去掉下划线之后颜色成了唯一的「这是可点的」提示，判据就从
     * 「链接 vs 背景」变成了还要加一条「链接 vs 同段正文 ≥ 3:1」——`info` 的暗色值
     * `#60A5FA` 对白正文只有 2.44，压深到 `#3B82F6` 才够（4.62 / 3.52）。亮色的
     * `#2563EB` 两条都过（4.70 / 3.85），沿用。
     *
     * 之所以在这里定值而不是去改 gearui 的 `info`：`info` 是冻结 token，还被状态提示等
     * 处消费，为聊天气泡的判据去动它会波及无关组件。
     */
    val Colors.messageLinkOther: Color
        get() = if (isDarkTheme) Color(0xFF3B82F6) else Color(0xFF2563EB)

    /**
     * 可点击文本在**自己气泡**上的颜色。
     *
     * 自己气泡是品牌主色铺满的（Weey 黄 `#FFD238` / PrivChat 蓝 `#0046BE`），
     * 所以这里不能用固定的一个蓝：深蓝落在 PrivChat 蓝底上对比度 1.20（看不见），
     * 浅蓝落在 Weey 黄底上 1.25（同样看不见）。按气泡自身亮度选深/浅。
     *
     * 🔴 已知残留：品牌色饱和度太高时蓝没有余量，两条判据无法同时满足——
     * Weey 黄底 4.64/2.97，PrivChat 蓝底 4.45/1.73（后者链接跟白正文几乎分不开）。
     * 微信/Telegram 在自己气泡上都保留了下划线正是因为这个。要彻底解决只有两条路：
     * 自己气泡上保留一个非颜色提示，或者别让自己气泡整块铺品牌色。
     */
    val Colors.messageLinkSelf: Color
        get() {
            val b = chatColors.bubbleSelf
            val lum = 0.2126f * b.red + 0.7152f * b.green + 0.0722f * b.blue
            return if (lum > 0.5f) Color(0xFF1D4ED8) else Color(0xFF93C5FD)
        }

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
