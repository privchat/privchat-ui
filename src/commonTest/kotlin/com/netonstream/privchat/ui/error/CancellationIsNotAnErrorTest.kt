package com.netonstream.privchat.ui.error

import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 2026-08-07 生产反馈：会话列表偶发弹出英文报错框
 * 「The coroutine scope left the composition」。
 *
 * 那不是错误，是用户按了返回键：UI 调用跑在 Composable 的 scope 上，页面销毁时
 * scope 被取消，上游 `runCatching` 把 CancellationException 当失败捕获，再写进全局
 * 的 `_errorMessage`——而弹窗宿主活在 App 顶层，不跟着页面一起销毁。于是「退出页面」
 * 变成了下一个页面上的一个框。
 *
 * 这里钉两条：**取消要被识别出来**，以及**框架英文绝不能上屏**。
 */
class CancellationIsNotAnErrorTest {

    @Test
    fun the_compose_scope_message_is_recognised_as_cancellation() {
        assertTrue(
            UserFacingError.isCancellationMessage("The coroutine scope left the composition"),
            "就是这句话弹到了用户脸上",
        )
        assertTrue(UserFacingError.isCancellation(IllegalStateException("The coroutine scope left the composition")))
    }

    @Test
    fun a_cancellation_exception_is_recognised_even_without_a_message() {
        assertTrue(UserFacingError.isCancellation(CancellationException(null as String?)))
    }

    @Test
    fun cancellation_shows_nothing_at_all() {
        assertNull(
            UserFacingError.ofMessage("The coroutine scope left the composition"),
            "取消应当什么都不显示，而不是换一句文案继续弹",
        )
    }

    @Test
    fun a_real_business_message_still_goes_through() {
        val msg = "红包已被领完"
        assertFalse(UserFacingError.isCancellationMessage(msg))
        assertEquals(msg, UserFacingError.ofMessage(msg))
    }

    @Test
    fun a_genuine_failure_is_not_mistaken_for_cancellation() {
        assertFalse(UserFacingError.isCancellationMessage("server error: reason_code=10007"))
    }
}
