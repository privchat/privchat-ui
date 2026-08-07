package com.netonstream.privchat.ui.error

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * 附件超限必须变成一句「文件太大」，而不是通用的「发送失败」。
 *
 * 用户反馈「大视频发不出去」时，客户端给的提示是「发送失败」——于是同一个视频被反复重试。
 * 服务端其实在签发上传 token 那一步就明确拒绝了（`ErrorCode::FileTooLarge` = 20602，
 * 发生在任何字节上传之前），只是这个原因在客户端被通用文案盖掉了。
 */
class AttachmentTooLargeMessageTest {

    /** SDK 的 `Error::Server` 渲染成 `server error: reason_code={code} message={...}`。 */
    private fun serverError(code: Int) =
        RuntimeException("server error: reason_code=$code message=文件大小超过限制（最大 100 MB）")

    @Test
    fun the_too_large_code_becomes_a_specific_message() {
        val shown = UserFacingError.message(serverError(20602), "发送失败")
        assertEquals("文件超过大小限制，无法发送", shown)
    }

    @Test
    fun the_servers_own_text_never_reaches_the_screen() {
        // 服务端文案只有中文，直出会让其它语言的用户看到中文。
        val shown = UserFacingError.message(serverError(20602), "发送失败")
        assertFalse(shown.contains("100 MB"), "服务端原文不上屏，文案由客户端本地化")
    }

    @Test
    fun other_server_errors_still_fall_back() {
        assertEquals("发送失败", UserFacingError.message(serverError(20004), "发送失败"))
    }
}
