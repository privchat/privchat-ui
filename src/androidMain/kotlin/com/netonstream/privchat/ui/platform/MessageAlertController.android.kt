package com.netonstream.privchat.ui.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

actual object MessageAlertController {

    /**
     * 同一时间窗口内只响一次，避免离线追平时一次推 100 条消息就响 100 声。
     * 取 3 秒覆盖典型批量到达；普通节奏聊天也合并到一次提示，与 WeChat 一致。
     */
    private const val MIN_INTERVAL_MS: Long = 3_000L
    private var lastPlayedAtMs: Long = 0L

    private var appContext: Context? = null

    fun register(context: Context) {
        appContext = context.applicationContext
    }

    fun unregister() {
        appContext = null
    }

    actual fun playIncomingMessageAlert() {
        val ctx = appContext ?: return
        val now = SystemClock.elapsedRealtime()
        if (now - lastPlayedAtMs < MIN_INTERVAL_MS) return
        lastPlayedAtMs = now
        playSound(ctx)
        vibrate(ctx)
    }

    private fun playSound(ctx: Context) {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ?: return
            val ringtone = RingtoneManager.getRingtone(ctx, uri) ?: return
            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone.play()
        }
    }

    private fun vibrate(ctx: Context) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(180L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(180L)
            }
        }
    }
}
