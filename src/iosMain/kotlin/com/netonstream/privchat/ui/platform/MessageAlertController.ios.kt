package com.netonstream.privchat.ui.platform

import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual object MessageAlertController {

    /** iOS 系统通用 SMS 提示音 ID（"sms-received1.caf"），无需服务器配合即可播放。 */
    private const val SOUND_ID_SMS_RECEIVED: UInt = 1003u

    /**
     * 同一时间窗口内只响一次，避免离线追平时一次推 100 条消息就响 100 声。
     * 取 3 秒覆盖典型批量到达；普通节奏聊天也合并到一次提示。
     */
    private const val MIN_INTERVAL_MS: Long = 3_000L
    private var lastPlayedAtMs: Long = 0L

    actual fun playIncomingMessageAlert() {
        val now = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
        if (now - lastPlayedAtMs < MIN_INTERVAL_MS) return
        lastPlayedAtMs = now
        AudioServicesPlaySystemSound(SOUND_ID_SMS_RECEIVED)
        AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
    }
}
