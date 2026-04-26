package com.netonstream.privchat.ui.media

/**
 * 把已下载到本地的媒体文件保存到系统相册。
 *
 * - Android: MediaStore.Images（API 29+ scoped storage） / Pictures/PrivChat/<filename>
 * - iOS: PHPhotoLibrary，落到用户照片库
 */
expect object MediaSaver {
    /**
     * 保存图片到相册。
     * @param localPath 已下载完成的本地原图绝对路径
     * @return 成功返回 Unit；失败返回带错误信息的 Result
     */
    suspend fun saveImage(localPath: String): Result<Unit>
}
