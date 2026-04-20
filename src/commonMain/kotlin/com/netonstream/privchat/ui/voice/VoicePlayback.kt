package com.netonstream.privchat.ui.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 单路语音播放状态。UI 层用 [playingMessageId] 驱动气泡图标切换。
 */
object VoicePlayback {
    private val _playingMessageId = MutableStateFlow<ULong?>(null)
    val playingMessageId: StateFlow<ULong?> = _playingMessageId.asStateFlow()

    /**
     * 点击气泡：同一条消息再次点击 → 停止；不同条 → 停止旧的，播新的。
     *
     * @param source 本地绝对路径或 http(s) URL；为 null/blank 则直接忽略
     */
    fun toggle(messageId: ULong, source: String?) {
        val current = _playingMessageId.value
        if (current == messageId) {
            VoicePlayerController.stop()
            _playingMessageId.value = null
            return
        }
        if (source.isNullOrBlank()) return

        VoicePlayerController.stop()
        _playingMessageId.value = messageId
        VoicePlayerController.play(
            source = source,
            onComplete = {
                if (_playingMessageId.value == messageId) {
                    _playingMessageId.value = null
                }
            },
            onError = {
                if (_playingMessageId.value == messageId) {
                    _playingMessageId.value = null
                }
            },
        )
    }

    /** 外部强制停止（离开页面等场景使用）。 */
    fun stop() {
        VoicePlayerController.stop()
        _playingMessageId.value = null
    }
}
