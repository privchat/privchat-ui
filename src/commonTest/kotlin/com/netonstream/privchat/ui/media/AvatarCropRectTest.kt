package com.netonstream.privchat.ui.media

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
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

    /** 没缩放没位移 = 整张图（图片本来就以 Crop 铺满取景框）。 */
    @Test
    fun untouched_selects_the_whole_framed_image() {
        val r = computeCropRect(scale = 1f, offsetX = 0f, offsetY = 0f, framePx = 1000f)
        assertClose(0f, r.x, "x")
        assertClose(0f, r.y, "y")
        assertClose(1f, r.size, "size")
    }

    /** 放大 2 倍 = 只取中间一半，且仍然居中。 */
    @Test
    fun zooming_in_takes_a_centred_half() {
        val r = computeCropRect(scale = 2f, offsetX = 0f, offsetY = 0f, framePx = 1000f)
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
        val centred = computeCropRect(2f, offsetX = 0f, offsetY = 0f, framePx = 1000f)
        val panned = computeCropRect(2f, offsetX = 200f, offsetY = 0f, framePx = 1000f)
        assertTrue(
            panned.x < centred.x,
            "图片右移后裁剪区没有左移: ${centred.x} -> ${panned.x}",
        )
        // 放大后的图边长 = 1000*2 = 2000，位移 200px = 0.1 比例。
        assertClose(0.25f - 0.1f, panned.x, "x")
    }

    /** 裁剪区永远落在图内：位移再大也不会裁到图像之外。 */
    @Test
    fun the_crop_never_leaves_the_image() {
        for (off in listOf(-99_999f, -1000f, 1000f, 99_999f)) {
            val r = computeCropRect(3f, offsetX = off, offsetY = off, framePx = 800f)
            assertTrue(r.x >= 0f && r.x + r.size <= 1f + 1e-4f, "x 越界: $r")
            assertTrue(r.y >= 0f && r.y + r.size <= 1f + 1e-4f, "y 越界: $r")
        }
    }

    /**
     * 还没布局完就点确认，退回整图而不是算出一个垃圾值。
     *
     * viewportPx=0 会让换算除以 0；这条保证那一刻点确认得到的是"等价于不裁剪"，
     * 而不是 NaN 一路传到 FFI。
     */
    @Test
    fun confirming_before_layout_falls_back_to_the_whole_image() {
        val r = computeCropRect(scale = 2f, offsetX = 50f, offsetY = 50f, framePx = 0f)
        assertEquals(0f, r.x)
        assertEquals(0f, r.y)
        assertEquals(1f, r.size)
    }
}
