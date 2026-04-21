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
import platform.Foundation.NSURL
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual object VoicePlayerController {
    private var player: AVAudioPlayer? = null
    private var delegate: Delegate? = null

    actual fun play(source: String, onComplete: () -> Unit, onError: (String) -> Unit) {
        stop()
        val url = if (source.startsWith("http://") || source.startsWith("https://")) {
            NSURL.URLWithString(source)
        } else {
            NSURL.fileURLWithPath(source)
        } ?: run {
            onError("invalid url")
            return
        }

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
