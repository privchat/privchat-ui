package com.netonstream.privchat.ui.platform

actual object SystemChromeController {
    actual fun setSystemBarsHidden(hidden: Boolean) {
        // iOS 下 overlay 自身已能覆盖到安全区外，暂不需要额外操作
    }
}
