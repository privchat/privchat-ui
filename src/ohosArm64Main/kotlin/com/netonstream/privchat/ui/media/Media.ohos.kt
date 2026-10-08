package com.netonstream.privchat.ui.media

// Opening a file in another app and saving to the gallery need ArkTS host APIs
// (Want / photoAccessHelper) that are not bridged yet.

actual object MediaOpener {
    actual fun open(localPath: String, mimeType: String?): Boolean = false
}

actual object MediaSaver {
    actual suspend fun saveImage(localPath: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Saving to the gallery is not supported on HarmonyOS yet"))
}
