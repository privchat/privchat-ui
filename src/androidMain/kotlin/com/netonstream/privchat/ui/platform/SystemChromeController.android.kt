package com.netonstream.privchat.ui.platform

import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

actual object SystemChromeController {

    private var activity: Activity? = null
    private var previousBehavior: Int? = null

    /** 由宿主 Activity 在 onCreate 中调用，注入 Activity 引用。 */
    fun register(activity: Activity) {
        this.activity = activity
    }

    fun unregister() {
        this.activity = null
        previousBehavior = null
    }

    actual fun setSystemBarsHidden(hidden: Boolean) {
        val act = activity ?: return
        act.runOnUiThread {
            val window = act.window ?: return@runOnUiThread
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (hidden) {
                if (previousBehavior == null) {
                    previousBehavior = controller.systemBarsBehavior
                }
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
                previousBehavior?.let { controller.systemBarsBehavior = it }
                previousBehavior = null
            }
        }
    }
}
