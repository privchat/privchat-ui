package com.netonstream.privchat.ui.sticker

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** UI-thread owned, process-wide admission queue. Waiting views render a static first frame. */
internal class StickerPlaybackSlots(private val limit: Int = 4) {
    private val candidates = mutableStateListOf<Any>()

    fun enter(owner: Any) { if (owner !in candidates) candidates.add(owner) }
    fun leave(owner: Any) { candidates.remove(owner) }
    fun canAnimate(owner: Any): Boolean = candidates.indexOf(owner) in 0 until limit
    val isEmpty: Boolean get() = candidates.isEmpty()

    fun rotate() {
        if (candidates.size <= limit) return
        repeat(limit) { candidates.add(candidates.removeAt(0)) }
    }
}

/** One timer for all pages; static catalog entries cannot permanently monopolize admission. */
internal object StickerPlaybackCoordinator {
    private val slots = StickerPlaybackSlots()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var rotation: Job? = null

    fun enter(owner: Any) {
        slots.enter(owner)
        if (rotation == null) rotation = scope.launch {
            while (true) {
                delay(3_000)
                slots.rotate()
            }
        }
    }

    fun leave(owner: Any) {
        slots.leave(owner)
        if (slots.isEmpty) {
            rotation?.cancel()
            rotation = null
        }
    }

    fun canAnimate(owner: Any): Boolean = slots.canAnimate(owner)
}
