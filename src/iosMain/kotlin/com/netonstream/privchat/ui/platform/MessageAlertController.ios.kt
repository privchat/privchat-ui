package com.netonstream.privchat.ui.platform

import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate

actual object MessageAlertController {

    /** iOS 系统通用 SMS 提示音 ID（"sms-received1.caf"），无需服务器配合即可播放。 */
    private const val SOUND_ID_SMS_RECEIVED: UInt = 1003u

    actual fun playIncomingMessageAlert() {
        AudioServicesPlaySystemSound(SOUND_ID_SMS_RECEIVED)
        AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
    }
}
