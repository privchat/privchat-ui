package com.netonstream.privchat.ui.error

import com.netonstream.privchat.ui.i18n.PrivChatStringPacks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Neton 应用服务端只回机器码（spec ERROR_CODE_SPEC）：客户端按机器码 / 字段码 / 数字类别给文案，
 * 服务端 message 永远不上屏。
 */
class PlatformErrorMessageTest {

    private val en = PrivChatStringPacks.English

    private class Err(
        override val code: Int,
        override val machineCode: String?,
        override val fieldErrors: List<PlatformFieldError> = emptyList(),
    ) : RuntimeException("platform failed: code=$code message=$machineCode"), PlatformApiError

    @Test
    fun machine_codes_map_to_specific_copy() {
        assertEquals(en.signInDoneToday, PlatformErrorMessage.describe(Err(10204, "ALREADY_SIGNED_IN_TODAY"), en))
        assertEquals(en.platformErrSmsCodeExpired, PlatformErrorMessage.describe(Err(10100, "SMS_CODE_EXPIRED"), en))
        assertEquals(en.platformErrInviteAlreadyBound, PlatformErrorMessage.describe(Err(10100, "INVITE_ALREADY_BOUND"), en))
        assertEquals(en.platformErrBalanceInsufficient, PlatformErrorMessage.describe(Err(10205, "WALLET_AVAILABLE_BALANCE_INSUFFICIENT"), en))
        assertEquals(en.loginErrInvalidCredentials, PlatformErrorMessage.describe(Err(10011, "INVALID_CREDENTIALS"), en))
    }

    @Test
    fun entry_rate_limits_share_one_message() {
        assertEquals(en.platformErrRateLimited, PlatformErrorMessage.describe(Err(10300, "SMS_SEND_RATE_LIMITED"), en))
        assertEquals(en.platformErrRateLimited, PlatformErrorMessage.describe(Err(10300, "RATE_LIMITED"), en))
        // 用户名 30 天限改有自己的说法，不被通用限流吞掉。
        assertEquals(en.settingsUsernameErrorRateLimited, PlatformErrorMessage.describe(Err(10300, "USERNAME_RATE_LIMITED"), en))
    }

    @Test
    fun validation_failed_speaks_about_the_field_with_params() {
        val e = Err(
            10100,
            "VALIDATION_FAILED",
            listOf(PlatformFieldError("nickname", "TOO_SHORT", mapOf("min" to 2L, "max" to 32L))),
        )
        assertEquals("Must be 2–32 characters.", PlatformErrorMessage.describe(e, en))
        val required = Err(10100, "VALIDATION_FAILED", listOf(PlatformFieldError("mobile", "REQUIRED")))
        assertEquals(en.platformErrFieldRequired, PlatformErrorMessage.describe(required, en))
    }

    @Test
    fun module_codes_in_field_errors_use_the_module_copy() {
        val e = Err(10100, "VALIDATION_FAILED", listOf(PlatformFieldError("mobile", "INVALID_MOBILE")))
        assertEquals(en.authPhoneInvalid, PlatformErrorMessage.describe(e, en))
    }

    @Test
    fun unknown_codes_fall_back_to_the_numeric_category() {
        assertEquals(en.platformErrConflict, PlatformErrorMessage.describe(Err(10205, "SOMETHING_NEW"), en))
        assertEquals(en.platformErrNotFound, PlatformErrorMessage.describe(Err(10201, "SOMETHING_NEW"), en))
        assertEquals(en.platformErrSessionExpired, PlatformErrorMessage.describe(Err(401, null), en))
        // 参数错 / 系统错没有可说的：交给调用方的动作文案。
        assertNull(PlatformErrorMessage.describe(Err(10100, "VALIDATION_FAILED"), en))
        assertNull(PlatformErrorMessage.describe(Err(4, "INTERNAL_ERROR"), en))
    }

    @Test
    fun sentences_are_not_machine_codes() {
        assertNull(PlatformErrorMessage.machineCodeOrNull("Already signed in today"))
        assertNull(PlatformErrorMessage.machineCodeOrNull("no_access_token"))
        assertEquals("ALREADY_SIGNED_IN_TODAY", PlatformErrorMessage.machineCodeOrNull("ALREADY_SIGNED_IN_TODAY"))
    }

    @Test
    fun user_facing_error_never_shows_the_machine_code() {
        assertEquals(PrivChatStringPacks.Chinese.platformErrRedPacketDrained.isNotBlank(), true)
        val shown = UserFacingError.message(Err(10205, "RED_PACKET_DRAINED"), "fallback")
        assertEquals(false, shown.contains("RED_PACKET_DRAINED"))
        assertEquals("fallback", UserFacingError.message(Err(4, "INTERNAL_ERROR"), "fallback"))
    }
}
