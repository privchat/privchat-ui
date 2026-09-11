package com.netonstream.privchat.ui.platform

import platform.UIKit.UISelectionFeedbackGenerator

actual object HapticBridge {

    // 复用一个实例：每次新建都要重新"预热"马达，第一下会明显迟。
    private val generator by lazy { UISelectionFeedbackGenerator() }

    actual fun selectionChanged() {
        generator.prepare()
        generator.selectionChanged()
    }
}
