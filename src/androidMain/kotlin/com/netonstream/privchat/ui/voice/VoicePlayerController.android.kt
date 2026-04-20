package com.netonstream.privchat.ui.voice

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper

actual object VoicePlayerController {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null

    actual fun play(source: String, onComplete: () -> Unit, onError: (String) -> Unit) {
        stop()
        val mp = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnCompletionListener {
                if (player === it) {
                    player = null
                    it.release()
                    mainHandler.post { onComplete() }
                }
            }
            setOnErrorListener { mp, what, extra ->
                if (player === mp) {
                    player = null
                    mp.release()
                    mainHandler.post { onError("MediaPlayer error what=$what extra=$extra") }
                }
                true
            }
        }
        player = mp
        try {
            mp.setDataSource(source)
            mp.setOnPreparedListener { it.start() }
            mp.prepareAsync()
        } catch (e: Exception) {
            player = null
            mp.release()
            mainHandler.post { onError(e.message ?: e::class.simpleName ?: "unknown") }
        }
    }

    actual fun stop() {
        val mp = player ?: return
        player = null
        try { if (mp.isPlaying) mp.stop() } catch (_: Exception) {}
        mp.release()
    }
}
