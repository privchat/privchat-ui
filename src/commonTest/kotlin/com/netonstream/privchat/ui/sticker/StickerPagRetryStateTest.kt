package com.netonstream.privchat.ui.sticker

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StickerPagRetryStateTest {
    private class EmptyApplier : AbstractApplier<Unit>(Unit) {
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun onClear() = Unit
    }
    @Test fun realCompositionResourceFailureRetriesOnlyForChangedGenerationOrOwner() = runTest {
        val clock = BroadcastFrameClock()
        val recomposer = Recomposer(coroutineContext + clock)
        val job = backgroundScope.launch(clock) { recomposer.runRecomposeAndApplyChanges() }
        val composition = Composition(EmptyApplier(), recomposer)
        var generation by mutableStateOf(0L)
        var resource by mutableStateOf("original-resource-sha")
        var owner by mutableStateOf(StickerSession("https://one/app", 7u, 1))
        var active by mutableStateOf(true)
        var unrelatedRefresh by mutableStateOf(0)
        var current: StickerPagRetryState? = null
        var appearance: Any? = null
        var allowed = false
        fun frame() {
            Snapshot.sendApplyNotifications(); runCurrent(); clock.sendFrame(1); runCurrent()
        }
        try {
            composition.setContent {
                unrelatedRefresh
                key(owner, resource) {
                    val retry = rememberStickerPagRetryState(generation)
                    val visibleAppearance = remember(active) { Any() }
                    current = retry; appearance = visibleAppearance
                    allowed = active && retry.canAttempt(visibleAppearance)
                }
            }
            frame(); assertTrue(allowed)
            val failed = current!!; failed.resourceFailed(); frame(); assertFalse(allowed)
            repeat(3) { unrelatedRefresh++; frame(); assertSame(failed, current); assertFalse(allowed) }
            active = false; frame(); active = true; frame()
            assertSame(failed, current); assertFalse(allowed)
            generation++; frame()
            assertNotSame(failed, current); assertTrue(allowed)
            val replacement = current!!
            // An event holding an old generation can only mutate the abandoned state object.
            failed.resourceFailed(); frame(); assertSame(replacement, current); assertTrue(allowed)
            owner = owner.copy(account = 8u, epoch = 2); frame()
            assertNotSame(replacement, current); assertTrue(allowed)
            replacement.resourceFailed(); frame(); assertTrue(allowed)
            current!!.resourceFailed(); frame(); assertFalse(allowed)
            resource = "updated-resource-sha"; frame(); assertTrue(allowed)
            assertNotNull(appearance)
        } finally { composition.dispose(); recomposer.close(); job.cancel() }
    }
    @Test fun realCompositionBudgetDenialRetriesOncePerAppearanceNotBudgetReleaseOrRefresh() = runTest {
        val clock = BroadcastFrameClock()
        val recomposer = Recomposer(coroutineContext + clock)
        val job = backgroundScope.launch(clock) { recomposer.runRecomposeAndApplyChanges() }
        val composition = Composition(EmptyApplier(), recomposer)
        var active by mutableStateOf(true)
        var unrelated by mutableStateOf(0)
        var attempts = 0
        var retry: StickerPagRetryState? = null
        var appearance: Any? = null
        var allowed = false
        fun frame() { Snapshot.sendApplyNotifications(); runCurrent(); clock.sendFrame(1); runCurrent() }
        try {
            composition.setContent {
                unrelated
                val state = rememberStickerPagRetryState(0)
                val token = remember(active) { Any() }
                retry = state; appearance = token
                allowed = active && state.canAttempt(token)
                if (allowed) DisposableEffect(token) {
                    attempts++; state.admissionDenied(token)
                    onDispose { /* A budget release is deliberately not a retry trigger. */ }
                }
            }
            frame(); frame(); assertEquals(1, attempts); assertFalse(allowed)
            repeat(4) { unrelated++; frame(); assertEquals(1, attempts); assertFalse(allowed) }
            active = false; frame(); active = true; frame(); frame()
            assertEquals(2, attempts); assertFalse(allowed)
            retry!!.resourceFailed(); frame()
            active = false; frame(); active = true; frame(); frame()
            assertEquals(2, attempts); assertFalse(allowed)
            assertNotNull(appearance)
        } finally { composition.dispose(); recomposer.close(); job.cancel() }
    }
}
