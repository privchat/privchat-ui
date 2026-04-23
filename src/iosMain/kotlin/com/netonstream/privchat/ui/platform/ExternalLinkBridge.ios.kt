package com.netonstream.privchat.ui.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual object ExternalLinkBridge {
    actual fun openUri(uri: String): Boolean {
        val url = NSURL.URLWithString(uri) ?: return false
        val app = UIApplication.sharedApplication
        if (!app.canOpenURL(url)) return false
        @Suppress("DEPRECATION")
        app.openURL(url)
        return true
    }
}
