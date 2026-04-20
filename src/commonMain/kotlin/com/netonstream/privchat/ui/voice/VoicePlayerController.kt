package com.netonstream.privchat.ui.voice

/**
 * 语音播放器（expect/actual）。
 *
 * 只支持单路播放：任何新 [play] 会停止当前正在播放的实例。
 * 播放源为本地文件路径或远程 URL（http/https）；平台实现负责识别 scheme。
 */
expect object VoicePlayerController {
    /**
     * 开始播放。
     *
     * @param source 本地文件绝对路径或远程 URL
     * @param onComplete 自然播放结束时在主线程回调
     * @param onError 发生错误时在主线程回调（message 为可选诊断信息）
     */
    fun play(source: String, onComplete: () -> Unit, onError: (String) -> Unit)

    /** 停止当前播放；未在播放时为 no-op，不会触发 [onComplete]。 */
    fun stop()
}
