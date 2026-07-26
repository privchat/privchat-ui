package com.netonstream.privchat.ui.error

import com.netonstream.privchat.sdk.SdkError
import com.netonstream.privchat.ui.i18n.PrivChatI18n

/**
 * Central mapper from a raw [Throwable] to a **safe, user-facing** message.
 *
 * Why this exists: several screens used to surface `throwable.message` directly
 * (e.g. `setError("发送验证码失败：${it.message}")`). On a network failure Ktor's
 * message is something like `Failed to connect to /124.156.166.105:8080`, which
 * both leaks server infrastructure (IP/port) and is meaningless to end users.
 *
 * Rules:
 * - The raw detail is written to the log (for diagnostics), never returned to UI.
 * - Recognized transport failures map to the localized [PrivChatStrings.networkError].
 * - Everything else falls back to the caller-provided, human-readable [fallback]
 *   (an action-specific message such as "发送验证码失败，请稍后重试").
 */
object UserFacingError {

    /**
     * @param throwable the caught error (nullable for `Result.exceptionOrNull()` call sites)
     * @param fallback  action-specific, user-readable message shown when the error is
     *                  not a recognized transport failure. MUST NOT contain raw exception text.
     */
    fun message(throwable: Throwable?, fallback: String): String {
        logRaw(throwable)
        // 结构化错误优先于字符串嗅探：SDK 已给出类型时不要再猜文案。
        typedMessage(throwable)?.let { return it }
        return if (isTransportFailure(throwable)) PrivChatI18n.current.networkError else fallback
    }

    /**
     * SDK 结构化错误 → 本地化文案。原始英文（含 `current: New` 之类内部状态名）
     * 只进日志，绝不上屏。
     */
    private fun typedMessage(throwable: Throwable?): String? = when (throwable) {
        is SdkError.SessionNotReady -> PrivChatI18n.current.connectionNotReady
        is SdkError.AttachmentSourceMissing -> PrivChatI18n.current.messageAttachmentSourceMissing
        else -> null
    }

    /**
     * String variant for call sites that already hold a raw message (e.g. an error
     * `StateFlow<String?>`). Transport-failure strings are replaced with the localized
     * network hint; anything else is returned unchanged (it is typically an app-authored
     * business message, not a raw stack detail). Blank/null passes through untouched.
     */
    fun ofMessage(message: String?): String? {
        if (message.isNullOrBlank()) return message
        return if (isTransportFailureMessage(message)) {
            println("[UserFacingError] transport failure message hidden: $message")
            PrivChatI18n.current.networkError
        } else {
            message
        }
    }

    /**
     * Server business error code carried in the SDK error message
     * (`Error::Server` renders as `server error: reason_code=<code> message=<...>`).
     * Lets screens map specific protocol codes (ERROR_CODE_SPEC) to precise
     * localized copy instead of a generic fallback. Null when absent.
     */
    fun serverReasonCode(throwable: Throwable?): Int? {
        val m = throwable?.message ?: return null
        val marker = "reason_code="
        val idx = m.indexOf(marker)
        if (idx < 0) return null
        return m.drop(idx + marker.length).takeWhile { it.isDigit() }.toIntOrNull()
    }

    /** True if the error looks like a connectivity / timeout problem (cross-platform, best-effort). */
    fun isTransportFailure(throwable: Throwable?): Boolean =
        isTransportFailureMessage(throwable?.message)

    fun isTransportFailureMessage(message: String?): Boolean {
        val m = (message ?: "").lowercase()
        if (m.isBlank()) return false
        return NETWORK_MARKERS.any { m.contains(it) }
    }

    private fun logRaw(throwable: Throwable?) {
        // Matches the app's println-tag logging convention; keeps the raw detail off-screen.
        println("[UserFacingError] ${throwable?.let { it::class.simpleName } ?: "null"}: ${throwable?.message}")
    }

    // Lowercased substrings covering Ktor/OkHttp (Android), Darwin (iOS) and the SDK's own
    // "网络已断开" transport errors. Intentionally broad; false positives only downgrade a
    // message to the generic network hint, never leak details.
    private val NETWORK_MARKERS = listOf(
        "failed to connect",
        "connection refused",
        "connection reset",
        "connection abort",
        "software caused connection abort",
        "unable to resolve host",
        "no address associated with hostname",
        "network is unreachable",
        "could not connect",
        "connect timed out",
        "timeout",
        "timed out",
        "unreachable",
        "socketexception",
        "connectexception",
        "unknownhostexception",
        "网络已断开",
        "网络连接",
        "网络错误",
    )
}
