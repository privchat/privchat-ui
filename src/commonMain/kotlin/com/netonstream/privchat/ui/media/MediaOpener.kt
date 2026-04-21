package com.netonstream.privchat.ui.media

/**
 * 打开已下载到本地规范目录的媒体文件（图片预览内联不需要，此处主要面向视频/文件）。
 * 平台实现：
 * - Android: FileProvider + Intent.ACTION_VIEW
 * - iOS: UIDocumentInteractionController 或 UIActivityViewController（V1 用 share sheet 占位）
 */
expect object MediaOpener {
    /**
     * 打开本地文件。
     * @param localPath 绝对路径
     * @param mimeType 可选；若平台需要（如 Android），会据此决定 ACTION_VIEW 的 dataType
     * @return 是否成功发起打开动作；调用方据此决定是否提示“无法打开”
     */
    fun open(localPath: String, mimeType: String?): Boolean
}
