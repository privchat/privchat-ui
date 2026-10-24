package com.netonstream.privchat.ui.group

import com.netonstream.privchat.ui.error.UserFacingError
import com.netonstream.privchat.ui.i18n.PrivChatI18n

/** Existing IM protocol codes and existing language packs; raw details never reach the UI. */
object GroupUiError {
    fun message(error: Throwable?, fallback: String): String = when (UserFacingError.serverReasonCode(error)) {
        10004 -> PrivChatI18n.current.platformErrPermissionDenied
        10100 -> PrivChatI18n.current.platformErrProfileFieldInvalid
        else -> UserFacingError.message(error, fallback)
    }
}
