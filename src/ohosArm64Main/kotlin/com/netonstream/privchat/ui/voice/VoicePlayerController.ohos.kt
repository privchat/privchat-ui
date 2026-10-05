package com.netonstream.privchat.ui.voice

// Playback needs the ArkTS media player, not bridged yet. Report the error so the
// bubble leaves its playing state instead of spinning.
actual object VoicePlayerController {
    actual fun play(source: String, onComplete: () -> Unit, onError: (String) -> Unit) {
        onError("Voice playback is not supported on HarmonyOS yet")
    }

    actual fun stop() {}
}
