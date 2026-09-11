package com.netonstream.privchat.ui.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

actual object HapticBridge {

    private var appContext: Context? = null

    fun register(context: Context) {
        appContext = context.applicationContext
    }

    fun unregister() {
        appContext = null
    }

    private val vibrator: Vibrator?
        get() {
            val ctx = appContext ?: return null
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }

    actual fun selectionChanged() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // TICK 是系统给"跨过一格"准备的最轻的一档，比自己定时长更贴近平台手感，
                // 也会跟随用户在系统设置里关掉触感的选择。
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(12)
            }
        }
    }
}
