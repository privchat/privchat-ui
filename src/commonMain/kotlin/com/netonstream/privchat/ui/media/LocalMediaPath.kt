package com.netonstream.privchat.ui.media

import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.sdk.dto.MessageEntry
import kotlinx.coroutines.delay

/**
 * 解析一条媒体消息的**本地文件绝对路径**，需要时先下载。
 *
 * 顺序：消息自己带的 `localMediaPath` → 最新一份 `PrivChat.messages`（覆盖"刚下完还没刷
 * 到这个 composable"）→ 触发 [MediaDownloadManager.start] 并轮询到 Done。失败/超时返回
 * null，由调用方决定怎么提示。
 *
 * 独立成一个函数而不是各页面各写一遍：图片预览、视频预览、语音播放要的都是这套等待，
 * 而"等下载"的坑（Failed 要立刻返回、Done 之外还要看消息行被刷新这条旁路）不该重复实现。
 */
suspend fun awaitLocalMediaPath(
    message: MessageEntry,
    timeoutMs: Long = 30_000L,
): String? {
    message.localMediaPath?.takeIf { it.isNotBlank() }?.let { return it }

    PrivChat.messages.value.firstOrNull { it.id == message.id }
        ?.localMediaPath?.takeIf { it.isNotBlank() }
        ?.let { return it }

    MediaDownloadManager.start(message)
    val pollMs = 250L
    var waited = 0L
    while (waited < timeoutMs) {
        when (val state = MediaDownloadManager.states.value[message.id]) {
            is MediaDownloadState.Done -> return state.path.takeIf { it.isNotBlank() }
            is MediaDownloadState.Failed -> return null
            else -> Unit
        }
        PrivChat.messages.value.firstOrNull { it.id == message.id }
            ?.localMediaPath?.takeIf { it.isNotBlank() }
            ?.let { return it }
        delay(pollMs)
        waited += pollMs
    }
    return null
}
