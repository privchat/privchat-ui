package com.netonstream.privchat.ui.error

import com.netonstream.privchat.sdk.SdkError
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import kotlin.coroutines.cancellation.CancellationException

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
        // 取消不是错误——见 [isCancellation]。调用方应先用 isCancellation 过滤；万一漏了，
        // 至少不要把框架的英文内部消息当成一条业务失败推给用户。
        if (isCancellation(throwable)) return fallback
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
        // 取消不是错误：返回 null，调用方据此**什么都不显示**（而不是换个文案继续弹）。
        if (isCancellationMessage(message)) {
            println("[UserFacingError] cancellation is not an error, dropped: $message")
            return null
        }
        return if (isTransportFailureMessage(message)) {
            println("[UserFacingError] transport failure message hidden: $message")
            PrivChatI18n.current.networkError
        } else {
            message
        }
    }

    /**
     * 协程取消不是错误。
     *
     * UI 发起的调用跑在 Composable 的 scope 上，用户退出页面时这个 scope 会被取消，
     * `runCatching` / `Result` 会把 [CancellationException] 当成一次失败捕获。但错误
     * 出口（`_errorMessage`）是全局的、不随页面销毁，于是「退出页面」这个完全正常的
     * 操作会变成下一个页面上的一个报错框——2026-08-07 生产反馈里会话列表偶发弹出
     * `The coroutine scope left the composition` 就是这么来的。
     *
     * 字符串嗅探是必要的兜底：跨 FFI/Result 边界回来的取消经常已经丢了异常类型，
     * 只剩一句消息文本。
     */
    fun isCancellation(throwable: Throwable?): Boolean {
        if (throwable == null) return false
        if (throwable is CancellationException) return true
        return isCancellationMessage(throwable.message)
    }

    fun isCancellationMessage(message: String?): Boolean {
        val m = (message ?: "").lowercase()
        if (m.isBlank()) return false
        return CANCELLATION_MARKERS.any { m.contains(it) }
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

    // Compose/Kuikly 与 kotlinx.coroutines 在取消时给出的消息。全小写匹配。
    // "The coroutine scope left the composition" 是 rememberCoroutineScope 随 Composable
    // 一起销毁时抛的那一条——用户按了返回键，仅此而已。
    private val CANCELLATION_MARKERS = listOf(
        "left the composition",
        "cancellationexception",
        "job was cancelled",
        "parent job is cancelled",
        "was cancelled",
        "scope was closed",
        "coroutine scope",
    )
}
