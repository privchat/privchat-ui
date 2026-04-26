package com.netonstream.privchat.ui.platform

/**
 * 收到入站消息时的本地提示音 + 震动。
 *
 * 与 APNs/FCM 解耦——前台 socket 推到的消息也要能让用户察觉。
 * 调用方负责判断是否应该提示（当前会话内的入站不应触发）。
 */
expect object MessageAlertController {
    /** 播放系统消息提示音 + 短震动；失败静默忽略。 */
    fun playIncomingMessageAlert()
}
