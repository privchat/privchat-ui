package com.netonstream.privchat.ui.components

import com.gearui.theme.Themes
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.chatColors
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.messageLinkOther
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * 聊天配色必须跟随 gearui 的明暗主题。
 *
 * 回归背景：暗色判定曾写成 `background == Color(0xFF09090B)`。gearui 后来把暗色阶梯换成中性
 * `#0A0A0A`，判定静默失效——气泡退回亮色（浅底），而 `Theme.colors.primary` 仍是暗色的近白，
 * 于是用 primary 画的手机号 / 网址 / @提及在浅气泡上整段隐形。
 */
class ChatColorsTest {

    /** 两个内置主题必须各自解析到自己那套气泡色，不能混用。 */
    @Test
    fun chatColorsFollowThemeBrightness() {
        val light = Themes.Light.colors.chatColors
        val dark = Themes.Dark.colors.chatColors

        assertNotEquals(light.bubbleOther, dark.bubbleOther, "暗色主题必须解析出自己的对方气泡色")
        assertNotEquals(light.onBubbleOther, dark.onBubbleOther, "暗色主题必须解析出自己的对方气泡文字色")

        assertTrue(luminance(light.bubbleOther) > luminance(light.onBubbleOther), "亮色：浅气泡深字")
        assertTrue(luminance(dark.bubbleOther) < luminance(dark.onBubbleOther), "暗色：深气泡浅字")
    }

    /** 链接色要跟对方气泡背景拉开对比，否则实体文本看不见。 */
    @Test
    fun linkColorContrastsWithOtherBubble() {
        listOf(Themes.Light.colors, Themes.Dark.colors).forEach { colors ->
            val delta = luminance(colors.messageLinkOther) - luminance(colors.chatColors.bubbleOther)
            assertTrue(
                delta * delta > 0.04f,
                "链接色与对方气泡背景亮度过近（差 $delta），实体文本会隐形",
            )
        }
    }

    /**
     * 链接**不再有下划线**，颜色是「这是可点的」唯一提示，所以还要跟同一段正文分得开。
     *
     * WCAG 1.4.1：仅靠颜色传递信息时，需与周围文字有 ≥3:1 的对比（手机上没有 hover
     * 可以补第二个提示）。这条挡的是「把链接调成好看的浅蓝，结果跟白正文糊成一片」。
     */
    @Test
    fun linkColorIsDistinguishableFromBodyText() {
        listOf(Themes.Light.colors, Themes.Dark.colors).forEach { colors ->
            assertTrue(
                contrast(colors.messageLinkOther, colors.chatColors.onBubbleOther) >= 3.0,
                "对方气泡：链接色与正文色对比不足 3:1，去掉下划线后认不出哪段可点",
            )
        }
    }

    /** WCAG 相对亮度对比度。luminance() 那个平均值只够比"谁更亮"，判阈值要用这个。 */
    private fun contrast(
        a: com.tencent.kuikly.compose.ui.graphics.Color,
        b: com.tencent.kuikly.compose.ui.graphics.Color,
    ): Double {
        fun ch(v: Float): Double {
            val d = v.toDouble()
            return if (d <= 0.04045) d / 12.92 else Math.pow((d + 0.055) / 1.055, 2.4)
        }
        fun rel(c: com.tencent.kuikly.compose.ui.graphics.Color) =
            0.2126 * ch(c.red) + 0.7152 * ch(c.green) + 0.0722 * ch(c.blue)
        val la = rel(a)
        val lb = rel(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    /**
     * 系统消息（"X 邀请 Y 加入了群聊"）里的人名也是链接，底色是 `muted` 而不是气泡色。
     *
     * 这行曾经用 `primary`：Weey 的品牌主色是黄色，浅色主题下人名在浅灰底上几乎看不清。
     * 链接色必须与所在容器背景拉开对比，跟气泡那条判据同源。
     */
    @Test
    fun linkColorContrastsWithSystemMessageBackground() {
        listOf(Themes.Light.colors, Themes.Dark.colors).forEach { colors ->
            val delta = luminance(colors.messageLinkOther) - luminance(colors.muted)
            assertTrue(
                delta * delta > 0.04f,
                "链接色与系统消息底色亮度过近（差 $delta），人名会糊在背景里",
            )
        }
    }

    private fun luminance(c: com.tencent.kuikly.compose.ui.graphics.Color): Float =
        (c.red + c.green + c.blue) / 3f
}
