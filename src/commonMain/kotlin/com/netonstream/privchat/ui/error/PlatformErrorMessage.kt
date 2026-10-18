package com.netonstream.privchat.ui.error

import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.netonstream.privchat.ui.i18n.PrivChatStrings
import com.netonstream.privchat.ui.i18n.withArgs

/**
 * 字段级错误（信封 `errors[]` 的一项）。`code` 是机器码（框架内建 `REQUIRED` / `TOO_SHORT` …
 * 或模块码 `INVALID_MOBILE` …）；`params` 是渲染用的约束值（目前 `min` / `max`）。
 */
data class PlatformFieldError(
    val field: String,
    val code: String,
    val params: Map<String, Long> = emptyMap(),
)

/**
 * Neton 应用服务端（PLATFORM 模式：account / member / payment / system / infra …）返回的业务错误。
 *
 * 与 PrivChat 自己的 IM 服务端（`privchat-protocol::ErrorCode`，见 [UserFacingError.serverReasonCode]）
 * 是两套体系，别混。
 *
 * - [code]：`NetonErrorCode` 数字类别（客户端判断类别只看它）。客户端自己构造的本地错误可能用
 *   401 表示「没有可用 token」。
 * - [machineCode]：信封 `message`，`UPPER_SNAKE` 机器码（spec ERROR_CODE_SPEC）；不是机器码时为 null。
 * - [fieldErrors]：信封 `errors[]`。
 */
interface PlatformApiError {
    val code: Int
    val machineCode: String?
    val fieldErrors: List<PlatformFieldError>
}

/**
 * Neton 应用服务端错误 → 本地化文案（spec ERROR_CODE_SPEC §1.6）。
 *
 * 顺序：总括码（`VALIDATION_FAILED` / `INVALID_PARAMS`）且带字段错误时先说字段 → 机器码 →
 * 其它字段错误 → 数字类别。都认不出返回 null，由调用方给动作文案（「操作失败」之类）；
 * 服务端 message 只是机器码，任何情况下都不上屏。
 *
 * 映射表与 privchat-web / privchat-h5 的 `describePlatformError` 同一份。
 */
object PlatformErrorMessage {

    private val MACHINE_CODE = Regex("^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$")

    /** 信封 `message` 是机器码才返回它；旧服务端的句子 / 本地拼的诊断串返回 null。 */
    fun machineCodeOrNull(message: String?): String? =
        message?.trim()?.takeIf { MACHINE_CODE.matches(it) }

    fun describe(error: PlatformApiError, strings: PrivChatStrings = PrivChatI18n.current): String? {
        val machine = error.machineCode
        if ((machine == VALIDATION_FAILED || machine == INVALID_PARAMS) && error.fieldErrors.isNotEmpty()) {
            firstField(error.fieldErrors, strings)?.let { return it }
        }
        machine?.let { forMachineCode(it, strings) }?.let { return it }
        firstField(error.fieldErrors, strings)?.let { return it }
        return forCategory(error.code, strings)
    }

    /**
     * 只有一个 `message` 的回包（例如机器人菜单的 IM transfer 回包 `{code, message}`，code 是
     * registry 段位码不是 `NetonErrorCode`）：message 是已知机器码才给文案，否则 null 交给动作文案。
     */
    fun forServerMessage(message: String?, strings: PrivChatStrings = PrivChatI18n.current): String? =
        machineCodeOrNull(message)?.let { forMachineCode(it, strings) }

    /** [throwable] 是 [PlatformApiError] 时的便捷入口。 */
    fun describeThrowable(throwable: Throwable?, strings: PrivChatStrings = PrivChatI18n.current): String? =
        (throwable as? PlatformApiError)?.let { describe(it, strings) }

    private fun firstField(fields: List<PlatformFieldError>, strings: PrivChatStrings): String? =
        fields.firstNotNullOfOrNull { forField(it, strings) }

    fun forField(field: PlatformFieldError, strings: PrivChatStrings = PrivChatI18n.current): String? {
        val min = field.params["min"]
        val max = field.params["max"]
        return when (field.code) {
            "REQUIRED" -> strings.platformErrFieldRequired
            "TOO_SHORT" -> when {
                min != null && max != null -> strings.platformErrFieldLengthRange.withArgs(min, max)
                min != null -> strings.platformErrFieldTooShort.withArgs(min)
                else -> null
            }
            "TOO_LONG" -> when {
                min != null && max != null -> strings.platformErrFieldLengthRange.withArgs(min, max)
                max != null -> strings.platformErrFieldTooLong.withArgs(max)
                else -> null
            }
            "TOO_SMALL" -> min?.let { strings.platformErrFieldTooSmall.withArgs(it) }
            "TOO_LARGE" -> max?.let { strings.platformErrFieldTooLarge.withArgs(it) }
            "INVALID_FORMAT" -> strings.platformErrFieldInvalidFormat
            "INVALID_EMAIL" -> strings.platformErrFieldInvalidEmail
            else -> forMachineCode(field.code, strings)
        }
    }

    fun forMachineCode(code: String, strings: PrivChatStrings = PrivChatI18n.current): String? = when (code) {
        // ── 会话 / 权限 / 资源（框架 + 各模块）──
        in SESSION_MACHINE_CODES -> strings.platformErrSessionExpired
        "PERMISSION_DENIED", "IP_NOT_ALLOWED", "ACCOUNT_DELETION_DISABLED", "FILE_FORBIDDEN",
        -> strings.platformErrPermissionDenied
        "RESOURCE_NOT_FOUND", "ACCOUNT_NOT_FOUND", "WITHDRAW_ORDER_NOT_FOUND", "PAY_TRANSFER_NOT_FOUND",
        "FILE_NOT_FOUND", "FILE_DELETED", "ADDRESS_NOT_FOUND",
        -> strings.platformErrNotFound
        "LOCK_NOT_ACQUIRED", "INVITE_CODE_GENERATION_CONFLICT", "RED_PACKET_CLAIM_CONFLICT",
        "WITHDRAW_CONCURRENT_MODIFIED", "WALLET_RECHARGE_CONCURRENT_MODIFIED",
        -> strings.platformErrBusy
        "RATE_LIMITED" -> strings.platformErrRateLimited
        "SERVICE_UNAVAILABLE", "TIMEOUT", "REQUEST_TIMEOUT", "ROUTE_NOT_FOUND", "METHOD_NOT_ALLOWED",
        "SOCIAL_LOGIN_UNAVAILABLE",
        -> strings.platformErrServiceUnavailable
        "PAYLOAD_TOO_LARGE" -> strings.platformErrRequestTooLarge

        // ── account / system：登录、验证码 ──
        "INVALID_CREDENTIALS" -> strings.loginErrInvalidCredentials
        "ACCOUNT_DISABLED" -> strings.loginErrAccountDisabled
        "ACCOUNT_DELETED" -> strings.platformErrAccountDeleted
        "INVALID_MOBILE", "MOBILE_INVALID" -> strings.authPhoneInvalid
        "MOBILE_IN_USE", "MOBILE_ALREADY_TAKEN" -> strings.bindMobileErrTaken
        "MOBILE_ALREADY_BOUND" -> strings.bindMobileErrBound
        "SMS_CODE_REQUIRED" -> strings.platformErrSmsCodeRequired
        "SMS_CODE_INVALID" -> strings.smsCodeError
        "SMS_CODE_EXPIRED" -> strings.platformErrSmsCodeExpired
        "SMS_CODE_ATTEMPTS_EXCEEDED", "SMS_CODE_LOCKED" -> strings.platformErrSmsCodeAttemptsExceeded
        "VERIFICATION_TOO_FREQUENT" -> strings.platformErrVerificationTooFrequent
        "VERIFICATION_DAILY_LIMIT" -> strings.platformErrVerificationDailyLimit
        "VERIFICATION_UNAVAILABLE", "VERIFICATION_CHANNEL_UNAVAILABLE", "VERIFICATION_SEND_FAILED",
        "VERIFICATION_DAILY_BUDGET_EXHAUSTED", "SMS_NOT_CONFIGURED",
        -> strings.platformErrVerificationUnavailable
        "LOGIN_LOCKED" -> strings.platformErrLoginLocked

        // ── account：注册、资料、举报 ──
        "USERNAME_TAKEN" -> strings.settingsUsernameErrorTaken
        "USERNAME_RESERVED" -> strings.settingsUsernameErrorReserved
        "INVALID_USERNAME_FORMAT" -> strings.settingsUsernameErrorInvalidFormat
        "USERNAME_RATE_LIMITED" -> strings.settingsUsernameErrorRateLimited
        "PASSWORD_TOO_SHORT" -> strings.registerErrPasswordTooShort
        "PASSWORD_TOO_LONG" -> strings.platformErrPasswordTooLong
        "REGISTER_MODE_DISABLED", "UNSUPPORTED_REGISTER_MODE" -> strings.platformErrRegisterModeDisabled
        "INVALID_NICKNAME_LENGTH" -> strings.setNicknameError
        "INVALID_BIO_LENGTH" -> strings.platformErrBioTooLong
        "INVALID_GENDER", "INVALID_BIRTHDAY_FORMAT" -> strings.platformErrProfileFieldInvalid
        "INVALID_AVATAR_FILE_ID", "AVATAR_FILE_NOT_FOUND", "AVATAR_FILE_INACTIVE",
        "INVALID_AVATAR_BUSINESS_TYPE", "AVATAR_FILE_NO_URL",
        -> strings.platformErrAvatarInvalid
        "REPORT_DESCRIPTION_TOO_LONG" -> strings.platformErrReportDescriptionTooLong

        // ── member：邀请码、签到 ──
        "INVITE_CODE_INVALID", "INVITE_CODE_UNSUPPORTED" -> strings.inviteErrInvalid
        "INVITE_ALREADY_BOUND" -> strings.platformErrInviteAlreadyBound
        "ALREADY_SIGNED_IN_TODAY" -> strings.signInDoneToday
        "SIGN_IN_CASH_REWARD_UNAVAILABLE" -> strings.platformErrSignInRewardUnavailable

        // ── payment：钱包、红包、转账、提现、银行卡 ──
        "AMOUNT_INVALID" -> strings.redPacketErrAmount
        "WALLET_NOT_FOUND" -> strings.platformErrWalletNotFound
        "WALLET_BALANCE_INSUFFICIENT", "WALLET_AVAILABLE_BALANCE_INSUFFICIENT" -> strings.platformErrBalanceInsufficient
        "TRANSFER_TO_SELF" -> strings.platformErrTransferToSelf
        "RED_PACKET_COUNT_INVALID" -> strings.platformErrRedPacketCountInvalid
        "RED_PACKET_AMOUNT_BELOW_COUNT" -> strings.redPacketErrTooSmall
        "RED_PACKET_NOT_FOUND" -> strings.platformErrRedPacketNotFound
        "RED_PACKET_EXPIRED" -> strings.platformErrRedPacketExpired
        "RED_PACKET_DRAINED" -> strings.platformErrRedPacketDrained
        "RED_PACKET_ALREADY_CLAIMED" -> strings.platformErrRedPacketAlreadyClaimed
        "RED_PACKET_NOT_CLAIMABLE" -> strings.platformErrRedPacketNotClaimable
        "RED_PACKET_CLAIM_FORBIDDEN" -> strings.platformErrRedPacketClaimForbidden
        "BANK_CARD_HOLDER_NAME_REQUIRED", "BANK_CARD_BANK_NAME_REQUIRED", "BANK_CARD_NUMBER_REQUIRED",
        -> strings.platformErrBankCardInfoRequired
        "BANK_CARD_NUMBER_INVALID" -> strings.platformErrBankCardNumberInvalid
        "BANK_CARD_NOT_FOUND" -> strings.platformErrBankCardNotFound
        "BANK_CARD_HAS_PENDING_WITHDRAWALS" -> strings.platformErrBankCardHasPendingWithdrawals
        "WITHDRAW_STATUS_NOT_ALLOWED" -> strings.platformErrWithdrawStatusNotAllowed
        "PAY_CHANNEL_UNAVAILABLE" -> strings.platformErrPayChannelUnavailable

        // ── infra：上传 ──
        "FILE_TOO_LARGE" -> strings.platformErrFileTooLarge
        "INVALID_MIME_TYPE" -> strings.platformErrFileTypeNotAllowed
        "EMPTY_FILE" -> strings.platformErrFileEmpty

        // ── privchat：扫码登录（/platform/qr-login/*）──
        "QR_SCENE_NOT_FOUND", "QR_TOKEN_MISMATCH" -> strings.qrLoginCodeExpired
        "QR_SCENE_STATE_INVALID" -> strings.qrLoginCodeUsed
        "QR_CONFIRM_TOKEN_INVALID" -> strings.qrLoginConfirmExpired
        "QR_SCANNER_MISMATCH" -> strings.qrLoginScannerMismatch
        "QR_LOGIN_UNAVAILABLE" -> strings.platformErrServiceUnavailable

        // ── privchat：频道 transfer 回包（机器人菜单走 IM transfer，回包 message 同为机器码）──
        "PRIVCHAT_CHANNEL_NOT_BOUND", "PRIVCHAT_CHANNEL_DISABLED", "PRIVCHAT_TRANSFER_DISABLED",
        "PRIVCHAT_SERVICE_NOT_FOUND", "PRIVCHAT_SERVICE_DISABLED", "PRIVCHAT_ROUTE_SERVICE_MISMATCH",
        "PRIVCHAT_ROUTE_NOT_SUPPORTED", "PRIVCHAT_BOT_BINDING_MISSING", "PRIVCHAT_BOT_DISABLED",
        -> strings.botServiceUnavailable
        "PRIVCHAT_HANDLER_UNAVAILABLE", "PRIVCHAT_HANDLER_FAILED", "PRIVCHAT_CALLBACK_FAILED",
        "PRIVCHAT_RESPONSE_TOO_LARGE",
        -> strings.platformErrServiceUnavailable

        // 各入口的 @RateLimit 机器码（LOGIN_RATE_LIMITED / SMS_SEND_RATE_LIMITED …）。
        else -> if (code.endsWith("_RATE_LIMITED")) strings.platformErrRateLimited else null
    }

    /** 数字类别兜底（`NetonErrorCode` 段位）。参数类 / 系统类没有可说的，返回 null 交给动作文案。 */
    fun forCategory(code: Int, strings: PrivChatStrings = PrivChatI18n.current): String? = when (code) {
        // 401：客户端本地「没有可用 token」。
        in SESSION_CATEGORY_CODES -> strings.platformErrSessionExpired
        10004, 10008 -> strings.platformErrPermissionDenied
        10007 -> strings.loginErrAccountDisabled
        10011 -> strings.loginErrInvalidCredentials
        10106 -> strings.platformErrRequestTooLarge
        10201, 10203 -> strings.platformErrNotFound
        10200, 10202, 10205 -> strings.platformErrConflict
        10204 -> strings.platformErrDuplicate
        10301, 10302 -> strings.platformErrDailyLimit
        in 10300..10399 -> strings.platformErrRateLimited
        3, 5 -> strings.platformErrServiceUnavailable
        else -> null
    }

    /**
     * 会话已失效、需要重新登录的错误（机器码或数字类别任一命中）。给需要「先登录再操作」专用文案的入口用
     * （例如扫码登录），免得各处再抄一份码表。
     */
    fun isSessionExpired(error: PlatformApiError): Boolean =
        error.machineCode in SESSION_MACHINE_CODES || error.code in SESSION_CATEGORY_CODES

    /** 会话类机器码（框架 + account / member / privchat 模块；DEVICE_ID_REQUIRED = 刷新时缺签发设备，只能重新登录）。 */
    private val SESSION_MACHINE_CODES = setOf(
        "AUTH_REQUIRED", "INVALID_REFRESH_TOKEN", "NOT_A_REFRESH_TOKEN", "INVALID_REFRESH_TOKEN_SUBJECT",
        "REFRESH_TOKEN_EXPIRED", "REFRESH_TOKEN_REVOKED", "LOGIN_REQUIRED", "REFRESH_TOKEN_INVALID",
        "DEVICE_ID_REQUIRED",
    )

    /** 会话类数字类别；401 = 客户端本地「没有可用 token」。 */
    private val SESSION_CATEGORY_CODES = setOf(401, 10000, 10001, 10002, 10003, 10005, 10006, 10009, 10010)

    const val VALIDATION_FAILED = "VALIDATION_FAILED"
    const val INVALID_PARAMS = "INVALID_PARAMS"
}
