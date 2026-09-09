package com.netonstream.privchat.ui.components

import com.gearui.theme.Themes
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.chatColors
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.messageLink
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
            val delta = luminance(colors.messageLink) - luminance(colors.chatColors.bubbleOther)
            assertTrue(
                delta * delta > 0.04f,
                "链接色与对方气泡背景亮度过近（差 $delta），实体文本会隐形",
            )
        }
    }

    /**
     * 🔴 这里原本还有一条「链接色必须与同段正文 ≥3:1」（WCAG 1.4.1，去掉下划线后颜色是
     * 唯一提示）。**它和「全局同一个链接色」不可兼得，已经量过了，不是没测出来。**
     *
     * 深色主题里三种底色分别配中灰字（系统消息）、白字（对方气泡）、黑字或白字（自己气泡，
     * 取决于品牌主色明暗）。一个颜色要同时和这三种正文各拉开 3:1，luminance 窗口是空集。
     * 把整条蓝色阶都试过：无品牌深色最好是「底 5.79 / 正文 1.43」，Weey 深色是
     * 「底 3.43 / 正文 1.92」——没有一个点两项都过。
     *
     * 取舍是**底色可读性优先**：先保证链接本身看得见，再尽量和正文分开。剩下的缺口只有
     * 两条真出路：深色下给链接补一个非颜色提示（下划线），或者自己气泡别整块铺品牌色。
     * 两条都是产品决定，不是调色能解决的。
     */
    /**
     * 全局只有一个链接色：同一屏上系统消息里的人名和气泡里的链接必须同色。
     *
     * 这条挡的是「为了各自的底色把链接色拆成两个值」——那样每一处单看都更清楚，
     * 但用户同时看到两种蓝，而它们表达的是同一件事。
     */
    @Test
    fun oneLinkColourAcrossEverySurface() {
        listOf(Themes.Light.colors, Themes.Dark.colors).forEach { colors ->
            // 系统消息底(muted)、对方气泡、自己气泡——同一个值必须在三处都还看得见。
            listOf(colors.muted, colors.chatColors.bubbleOther, colors.chatColors.bubbleSelf)
                .forEach { bg ->
                    assertTrue(
                        contrast(colors.messageLink, bg) >= 3.0,
                        "链接色在某个底色上对比不足 3:1（底=$bg）",
                    )
                }
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
            val delta = luminance(colors.messageLink) - luminance(colors.muted)
            assertTrue(
                delta * delta > 0.04f,
                "链接色与系统消息底色亮度过近（差 $delta），人名会糊在背景里",
            )
        }
    }

    private fun luminance(c: com.tencent.kuikly.compose.ui.graphics.Color): Float =
        (c.red + c.green + c.blue) / 3f
}
