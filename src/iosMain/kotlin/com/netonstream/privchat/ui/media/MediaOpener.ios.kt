package com.netonstream.privchat.ui.media

import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual object MediaOpener {
    actual fun open(localPath: String, mimeType: String?): Boolean {
        val url = NSURL.fileURLWithPath(localPath)
        val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return false
        dispatch_async(dispatch_get_main_queue()) {
            var top = root
            while (top.presentedViewController != null) {
                top = top.presentedViewController!!
            }
            val sheet = UIActivityViewController(
                activityItems = listOf(url),
                applicationActivities = null,
            )
            top.presentViewController(sheet, true, completion = null)
        }
        return true
    }
}
