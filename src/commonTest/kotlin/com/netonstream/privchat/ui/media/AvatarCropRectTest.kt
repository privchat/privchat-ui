package com.netonstream.privchat.ui.media

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 取景框 → 归一化裁剪矩形的换算。
 *
 * 这段换算是整条头像链路里最容易错、又最难靠肉眼验的一环：错了不会崩，只是裁出来的
 * 头像偏一点，而"偏一点"在手机上看常常看不出来，直到用户换了张构图讲究的照片。
 * 所以把几何关系钉成用例。
 */
class AvatarCropRectTest {

    private fun assertClose(expected: Float, actual: Float, what: String) {
        assertTrue(
            abs(expected - actual) < 1e-4f,
            "$what: 期望 ~$expected，实际 $actual",
        )
    }

    /** 方图不动 = 整张图。 */
    @Test
    fun untouched_square_selects_the_whole_image() {
        val r = computeCropRect(1f, 0f, 0f, framePx = 1000f, srcWidth = 800f, srcHeight = 800f)
        assertClose(0f, r.x, "x")
        assertClose(0f, r.y, "y")
        assertClose(1f, r.size, "size")
    }

    /**
     * 🔴 竖图不动 = **正中间**那块正方形，不是顶部。
     *
     * x 按宽度归一、y 按高度归一（FFI 契约），两者分母不同。曾经 y 也按短边算，
     * 1440x3200 的截图确认后裁的是图片顶部，跟取景框里看到的完全不是一块。
     */
    @Test
    fun untouched_portrait_selects_the_centre_not_the_top() {
        val r = computeCropRect(1f, 0f, 0f, framePx = 1000f, srcWidth = 1440f, srcHeight = 3200f)
        assertClose(0f, r.x, "x")
        assertClose(1f, r.size, "size")
        // 居中：(3200-1440)/2 / 3200
        assertClose(0.275f, r.y, "y")
    }

    /** 放大 2 倍 = 只取中间一半，且仍然居中。 */
    @Test
    fun zooming_in_takes_a_centred_half() {
        val r = computeCropRect(2f, 0f, 0f, framePx = 1000f, srcWidth = 800f, srcHeight = 800f)
        assertClose(0.5f, r.size, "size")
        assertClose(0.25f, r.x, "x")
        assertClose(0.25f, r.y, "y")
    }

    /**
     * 图片右移 = 取景框相对图片左移，裁剪区应当往左走。
     *
     * 符号错了会让裁剪区朝反方向跑——用户把人脸拖进框里，裁出来的却是另一边。
     */
    @Test
    fun panning_the_image_right_moves_the_crop_left() {
        val centred = computeCropRect(2f, 0f, 0f, 1000f, 800f, 800f)
        val panned = computeCropRect(2f, 200f, 0f, 1000f, 800f, 800f)
        assertTrue(
            panned.x < centred.x,
            "图片右移后裁剪区没有左移: ${centred.x} -> ${panned.x}",
        )
        // 屏幕位移 200px ÷ 总缩放系数(1000*2/800=2.5) = 图内 80px = 0.1 宽度比例。
        assertClose(0.25f - 0.1f, panned.x, "x")
    }

    /** 裁剪区永远落在图内：位移再大也不会裁到图像之外。 */
    @Test
    fun the_crop_never_leaves_the_image() {
        for (off in listOf(-99_999f, -1000f, 1000f, 99_999f)) {
            val r = computeCropRect(3f, off, off, framePx = 800f, srcWidth = 1440f, srcHeight = 3200f)
            val wFrac = 1440f / 1440f * r.size // size 相对短边(=宽)，换成宽度比例
            val hFrac = 1440f * r.size / 3200f
            assertTrue(r.x >= 0f && r.x + wFrac <= 1f + 1e-4f, "x 越界: $r")
            assertTrue(r.y >= 0f && r.y + hFrac <= 1f + 1e-4f, "y 越界: $r")
        }
    }

    /**
     * 还没布局完就点确认，退回"整图居中正方形"而不是算出一个垃圾值。
     *
     * framePx=0 会让换算除以 0；这条保证那一刻点确认得到的是"等价于不裁剪"，
     * 而不是 NaN 一路传到 FFI。
     */
    @Test
    fun confirming_before_layout_falls_back_to_the_centred_square() {
        val r = computeCropRect(2f, 50f, 50f, framePx = 0f, srcWidth = 1440f, srcHeight = 3200f)
        assertClose(0f, r.x, "x")
        assertClose(0.275f, r.y, "y")
        assertClose(1f, r.size, "size")
    }
}
