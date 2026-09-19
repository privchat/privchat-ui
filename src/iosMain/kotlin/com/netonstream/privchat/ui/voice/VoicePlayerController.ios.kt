package com.netonstream.privchat.ui.voice

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual object VoicePlayerController {
    private var player: AVAudioPlayer? = null
    private var delegate: Delegate? = null

    /**
     * 播放一个**本地**语音文件。
     *
     * 🔴 两个会直接崩掉 App 的坑：
     *
     * 1. 调用方给的是 `file:///var/...`，而 `fileURLWithPath` 要的是**裸路径**。把带
     *    scheme 的串喂进去，它当相对路径拼，得到一个不存在的文件。
     * 2. `AVAudioPlayer(url, err)` 这个 ObjC 初始化器失败时返回 nil，而 Kotlin/Native
     *    把它绑成了非空构造器——nil 不是返回 null，是**直接终止进程**。所以 err 的判断
     *    永远等不到，文件不存在/格式不对当场闪退。
     *
     * 于是这里先把路径规整好、先确认文件在，再去构造。远程 URL 一律拒绝：AVAudioPlayer
     * 根本不能播 http(s)（同样返回 nil ⇒ 同样闪退），远程要先下载，那是调用方的事。
     */
    actual fun play(source: String, onComplete: () -> Unit, onError: (String) -> Unit) {
        stop()
        if (source.startsWith("http://") || source.startsWith("https://")) {
            onError("remote audio must be downloaded before playback")
            return
        }
        val path = source.removePrefix("file://")
        if (path.isBlank() || !NSFileManager.defaultManager.fileExistsAtPath(path)) {
            onError("audio file missing: $path")
            return
        }
        val url = NSURL.fileURLWithPath(path)

        AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, error = null)
        // AVAudioSession.setActive 在部分 Konan 版本下解析不到重载，这里省略；
        // 语音回放不强依赖显式激活会话，后续如需打断其他应用音频可用 ObjC 动态派发补回。

        memScoped {
            val err: ObjCObjectVar<NSError?> = alloc()
            val p = AVAudioPlayer(url, err.ptr)
            if (err.value != null) {
                onError(err.value?.localizedDescription ?: "AVAudioPlayer init failed")
                return@memScoped
            }
            val d = Delegate(
                onFinish = { success ->
                    if (player === p) {
                        player = null
                        delegate = null
                        if (success) onComplete() else onError("playback did not finish cleanly")
                    }
                }
            )
            p.delegate = d
            if (!p.prepareToPlay() || !p.play()) {
                player = null
                delegate = null
                onError("AVAudioPlayer failed to start")
                return@memScoped
            }
            player = p
            delegate = d
        }
    }

    actual fun stop() {
        val p = player ?: return
        player = null
        delegate = null
        p.stop()
    }

    private class Delegate(
        private val onFinish: (Boolean) -> Unit,
    ) : NSObject(), AVAudioPlayerDelegateProtocol {
        override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
            onFinish(successfully)
        }
    }
}
