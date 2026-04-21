package com.netonstream.privchat.ui.media

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.netonstream.privchat.sdk.dto.MessageEntry
import com.netonstream.privchat.sdk.dto.SdkEventEnvelope

/**
 * 附件下载 UI 态（字节级进度）。与 Rust SDK `MediaDownloadState` 对齐。
 */
sealed class MediaDownloadState {
    object Idle : MediaDownloadState()
    data class Downloading(val bytes: ULong, val total: ULong?) : MediaDownloadState()
    data class Paused(val bytes: ULong, val total: ULong?) : MediaDownloadState()
    data class Done(val path: String) : MediaDownloadState()
    data class Failed(val code: UInt, val message: String) : MediaDownloadState()
}

/**
 * 附件下载 UI 侧状态仓库 + 控制器路由。
 *
 * - 状态来源于 SDK 的 `media_download_state_changed` 事件，App 层订阅 SDK 事件流后调用
 *   [onSdkEvent] 把事件喂给本仓库；UI 只订阅 [states] 即可。
 * - 点击气泡触发的 start/pause/resume/cancel 统一走 [Controller]，由 App 层在 SDK 初始化时
 *   [setController] 注入，控制器内部直接调用 SDK API。
 */
object MediaDownloadManager {
    private val _states = MutableStateFlow<Map<ULong, MediaDownloadState>>(emptyMap())
    val states: StateFlow<Map<ULong, MediaDownloadState>> = _states.asStateFlow()

    private var controller: Controller? = null

    fun setController(impl: Controller?) {
        controller = impl
    }

    fun stateFor(messageId: ULong): MediaDownloadState =
        _states.value[messageId] ?: MediaDownloadState.Idle

    /** App 层消费 SDK 事件流时调用，把下载事件投递到 UI 状态仓库。 */
    fun onSdkEvent(envelope: SdkEventEnvelope) {
        val payload = envelope.event
        if (payload.type.lowercase() != "media_download_state_changed") return
        val messageId = payload.messageId ?: return
        val kind = payload.mediaDownloadStateKind ?: return
        val next: MediaDownloadState = when (kind.lowercase()) {
            "idle" -> MediaDownloadState.Idle
            "downloading" -> MediaDownloadState.Downloading(
                bytes = payload.mediaDownloadBytes ?: 0uL,
                total = payload.mediaDownloadTotal,
            )
            "paused" -> MediaDownloadState.Paused(
                bytes = payload.mediaDownloadBytes ?: 0uL,
                total = payload.mediaDownloadTotal,
            )
            "done" -> MediaDownloadState.Done(payload.mediaDownloadPath ?: "")
            "failed" -> MediaDownloadState.Failed(
                code = payload.errorCode ?: 0u,
                message = payload.reason ?: "download failed",
            )
            else -> return
        }
        _states.value = _states.value + (messageId to next)
    }

    fun start(message: MessageEntry) {
        val c = controller ?: return
        c.start(message)
    }

    fun pause(messageId: ULong) {
        controller?.pause(messageId)
    }

    fun resume(messageId: ULong) {
        controller?.resume(messageId)
    }

    fun cancel(messageId: ULong) {
        controller?.cancel(messageId)
    }

    /** 消息重新从 DB 载入（media_downloaded=true，localMediaPath 有值）后清理内存态，避免残留。 */
    fun clearIfDone(messageId: ULong, localPath: String?) {
        if (localPath.isNullOrBlank()) return
        val current = _states.value[messageId]
        if (current is MediaDownloadState.Done) {
            _states.value = _states.value - messageId
        }
    }

    /**
     * 点击气泡时由 UI 调用的统一控制器。
     * 所有 start/pause/resume/cancel 都不会阻塞 UI 线程，
     * 实现方应在 SDK 所在协程 scope 中异步处理。
     */
    interface Controller {
        fun start(message: MessageEntry)
        fun pause(messageId: ULong)
        fun resume(messageId: ULong)
        fun cancel(messageId: ULong)
    }
}
