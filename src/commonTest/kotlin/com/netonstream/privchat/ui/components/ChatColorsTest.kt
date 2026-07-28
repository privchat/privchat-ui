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

    private fun luminance(c: com.tencent.kuikly.compose.ui.graphics.Color): Float =
        (c.red + c.green + c.blue) / 3f
}
