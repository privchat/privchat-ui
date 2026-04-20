package com.netonstream.privchat.ui.platform

import platform.UIKit.UIPasteboard

actual object ClipboardBridge {
    actual fun setText(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }
}
