package com.netonstream.privchat.ui.media

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHPhotoLibrary
import kotlin.coroutines.resume

actual object MediaSaver {
    actual suspend fun saveImage(localPath: String): Result<Unit> {
        val authorized = ensureAuthorized()
        if (!authorized) return Result.failure(IllegalStateException("用户未授权访问相册"))

        return suspendCancellableCoroutine { cont ->
            val url = NSURL.fileURLWithPath(localPath)
            PHPhotoLibrary.sharedPhotoLibrary().performChanges(
                changeBlock = {
                    PHAssetChangeRequest.creationRequestForAssetFromImageAtFileURL(url)
                },
                completionHandler = { success: Boolean, error: NSError? ->
                    if (success) {
                        cont.resume(Result.success(Unit))
                    } else {
                        val message = error?.localizedDescription ?: "保存失败"
                        cont.resume(Result.failure(IllegalStateException(message)))
                    }
                }
            )
        }
    }

    private suspend fun ensureAuthorized(): Boolean = suspendCancellableCoroutine { cont ->
        // iOS 14+ 走 addOnly 权限分级；这里用宽松判定，权限弹窗由系统处理。
        PHPhotoLibrary.requestAuthorization { status ->
            cont.resume(status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited)
        }
    }
}
