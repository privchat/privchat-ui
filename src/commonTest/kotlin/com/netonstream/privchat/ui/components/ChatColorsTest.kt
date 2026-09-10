package com.netonstream.privchat.ui.components

import com.gearui.theme.Themes
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.chatColors
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.otherBubbleStyle
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.selfBubbleStyle
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.systemMessageStyle
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

    /**
     * 三个聊天表面都必须自洽：正文和链接对各自底色都过 4.5。
     *
     * 底色由品牌决定（Weey 是黄、PrivChat 是蓝、无品牌回退中性），**我们不为了链接改底色**，
     * 所以这条要对内置的两套主题都成立。
     */
    @Test
    fun everyChatSurfaceIsReadable() {
        listOf(Themes.Light.colors, Themes.Dark.colors).forEach { colors ->
            listOf(
                "自己气泡" to colors.selfBubbleStyle,
                "对方气泡" to colors.otherBubbleStyle,
                "系统消息" to colors.systemMessageStyle,
            ).forEach { (name, style) ->
                assertTrue(style.textOnBackground >= 4.5, "$name：正文对底仅 ${style.textOnBackground}")
                assertTrue(style.linkOnBackground >= 4.5, "$name：链接对底仅 ${style.linkOnBackground}")
            }
        }
    }

    /**
     * 系统消息不再用全局 mutedForeground。
     *
     * 浅色下 `#8A8A8E` 对 `#F4F4F5` 只有 3.13——系统消息是要读的内容，这是不达标的。
     * 弱化改由字号/居中/底纹承担。这条同时守住「不要顺手把全局弱化文字调深」。
     */
    @Test
    fun systemMessageDoesNotBorrowTheMutedForeground() {
        listOf(Themes.Light.colors, Themes.Dark.colors).forEach { colors ->
            assertTrue(
                contrast(colors.systemMessageStyle.text, colors.muted) >= 4.5,
                "系统消息正文对底不足 4.5",
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


    private fun luminance(c: com.tencent.kuikly.compose.ui.graphics.Color): Float =
        (c.red + c.green + c.blue) / 3f
}
