package com.netonstream.privchat.ui.platform

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LocationPreviewStateTest {
    private val point = LocationPresentation(37.0, -122.0, "wgs84", null)
    private val png = "data:image/png;base64,iVBORw0KGgo="
    private fun state(lease: LocationPreviewBridge.Registration) = LocationPreviewState(lease, point, 220, 110)

    @Test fun failureStaysIdleUntilOneExplicitRetryAndSuccessRemovesRetry() = runBlocking {
        var calls = 0
        val lease = LocationPreviewBridge.install("server:1") { selected, width, height ->
            assertEquals(point, selected); assertEquals(320, width); assertEquals(160, height)
            if (++calls == 1) null else png
        }
        try {
            val state = state(lease)
            assertFalse(state.retry())
            state.load(); assertTrue(state.canRetry); assertNull(state.image)
            repeat(5) { state.load(); yield() }
            assertEquals(1, calls); assertEquals(0L, state.attempt)
            assertTrue(state.retry()); assertFalse(state.retry())
            assertFalse(state.canRetry); assertEquals(1L, state.attempt)
            state.load()
            assertEquals(png, state.image); assertFalse(state.canRetry); assertFalse(state.retry())
            state.load(); assertEquals(2, calls)
        } finally { LocationPreviewBridge.uninstall(lease) }
    }

    @Test fun repeatedNativeFailureNeverSchedulesItsOwnRetry() = runBlocking {
        var calls = 0
        val lease = LocationPreviewBridge.install("server:1") { _, _, _ -> calls++; error("native failure") }
        try {
            val state = state(lease)
            repeat(3) { attempt ->
                if (attempt > 0) assertTrue(state.retry())
                state.load(); assertTrue(state.canRetry); assertNull(state.image)
                repeat(5) { state.load(); yield() }
                assertEquals(attempt + 1, calls)
            }
        } finally { LocationPreviewBridge.uninstall(lease) }
    }

    @Test fun retryRechecksHostAuthorizationWithoutSwitchingTheLease() = runBlocking {
        var authorized = true
        var checks = 0
        var authorizedCalls = 0
        val lease = LocationPreviewBridge.install("server:1:GOOGLE") { _, _, _ ->
            checks++
            if (!authorized) null else { authorizedCalls++; null }
        }
        try {
            val state = state(lease); state.load()
            authorized = false
            assertTrue(state.retry()); state.load()
            assertEquals(2, checks); assertEquals(1, authorizedCalls)
            assertTrue(LocationPreviewBridge.registration === lease)
            assertNull(state.image); assertTrue(state.canRetry)
        } finally { LocationPreviewBridge.uninstall(lease) }
    }

    @Test fun revokedLeaseCannotRetryOrLoadAndOldUninstallDoesNotAffectReplacement() = runBlocking {
        var oldCalls = 0
        var newCalls = 0
        val old = LocationPreviewBridge.install("server:1") { _, _, _ -> oldCalls++; null }
        val oldState = state(old); oldState.load()
        val replacement = LocationPreviewBridge.install("server:1") { _, _, _ -> newCalls++; png }
        try {
            assertFalse(oldState.canRetry); assertFalse(oldState.retry()); oldState.load()
            LocationPreviewBridge.uninstall(old)
            assertTrue(LocationPreviewBridge.registration === replacement)
            val newState = state(replacement); newState.load()
            assertEquals(png, newState.image); assertEquals(1, oldCalls); assertEquals(1, newCalls)
        } finally { LocationPreviewBridge.uninstall(replacement) }
    }

    @Test fun accountReplacementRejectsLateNonCancellableResult() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val old = LocationPreviewBridge.install("server:1") { _, _, _ ->
            started.complete(Unit); withContext(NonCancellable) { finish.await(); png }
        }
        val oldState = state(old)
        val pending = launch { oldState.load() }
        started.await()
        val replacement = LocationPreviewBridge.install("server:2") { _, _, _ -> png }
        try {
            finish.complete(Unit); pending.join()
            assertNull(oldState.image); assertFalse(oldState.canRetry); assertFalse(oldState.retry())
            val newState = state(replacement); newState.load(); assertEquals(png, newState.image)
        } finally { LocationPreviewBridge.uninstall(replacement) }
    }

    @Test fun twoCardsShareBothInitialFailureAndExplicitRetry() = runBlocking {
        var calls = 0
        val started = CompletableDeferred<Unit>()
        val initial = CompletableDeferred<Unit>()
        val retry = CompletableDeferred<Unit>()
        val lease = LocationPreviewBridge.install("server:1") { _, _, _ ->
            calls++
            if (calls == 1) { started.complete(Unit); initial.await(); null } else { retry.await(); png }
        }
        try {
            val a = state(lease); val b = state(lease)
            val first = launch { a.load() }; started.await()
            val second = launch(start = CoroutineStart.UNDISPATCHED) { b.load() }
            initial.complete(Unit); first.join(); second.join()
            assertEquals(1, calls); assertTrue(a.retry()); assertTrue(b.retry())
            val ar = launch(start = CoroutineStart.UNDISPATCHED) { a.load() }
            val br = launch(start = CoroutineStart.UNDISPATCHED) { b.load() }
            retry.complete(Unit); ar.join(); br.join()
            assertEquals(2, calls); assertEquals(png, a.image); assertEquals(png, b.image)
        } finally { LocationPreviewBridge.uninstall(lease) }
    }

    @Test fun cancellingOneRetryConsumerDoesNotCancelOtherCard() = runBlocking {
        var calls = 0
        var stopped = false
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val lease = LocationPreviewBridge.install("server:1") { _, _, _ ->
            if (++calls == 1) null else {
                started.complete(Unit)
                try { finish.await(); png } finally { stopped = true }
            }
        }
        try {
            val a = state(lease); a.load(); assertTrue(a.retry())
            val first = launch { a.load() }; started.await()
            val b = state(lease)
            val second = launch(start = CoroutineStart.UNDISPATCHED) { b.load() }
            first.cancelAndJoin(); assertFalse(stopped)
            finish.complete(Unit); second.join()
            assertEquals(png, b.image); assertEquals(2, calls); assertTrue(lease.flights.isEmpty())
        } finally { LocationPreviewBridge.uninstall(lease) }
    }

    @Test fun nativeDeadlineBecomesRetryableWithoutAutomaticRetry() = runTest {
        var calls = 0
        val lease = LocationPreviewBridge.install("server:1") { _, _, _ ->
            if (++calls == 1) awaitCancellation() else png
        }
        try {
            val state = state(lease)
            val pending = launch { state.load() }; runCurrent()
            advanceTimeBy(20_001); runCurrent(); pending.join()
            assertTrue(state.canRetry); assertNull(state.image); assertEquals(1, calls)
            advanceTimeBy(60_000); runCurrent(); assertEquals(1, calls)
            assertTrue(state.retry()); state.load(); assertEquals(png, state.image)
            assertEquals(2, calls)
        } finally { LocationPreviewBridge.uninstall(lease) }
    }

    @Test fun nonLocalUriRequiresANewExplicitAttempt() = runBlocking {
        var calls = 0
        val lease = LocationPreviewBridge.install("server:1") { _, _, _ ->
            if (++calls == 1) "https://example.invalid/map.png" else png
        }
        try {
            val state = state(lease); state.load()
            assertNull(state.image); assertTrue(state.canRetry)
            assertTrue(state.retry()); state.load(); assertEquals(png, state.image)
        } finally { LocationPreviewBridge.uninstall(lease) }
    }

    @Test fun lastConsumerCancellationPropagatesAndAllowsOnlyExplicitRecovery() = runBlocking {
        var calls = 0
        val started = CompletableDeferred<Unit>()
        val stopped = CompletableDeferred<Unit>()
        val lease = LocationPreviewBridge.install("server:1") { _, _, _ ->
            if (++calls == 1) {
                started.complete(Unit)
                try { awaitCancellation() } finally { stopped.complete(Unit) }
            } else png
        }
        try {
            val state = state(lease)
            val pending = async { state.load() }
            started.await(); pending.cancelAndJoin(); stopped.await()
            assertTrue(pending.isCancelled); assertTrue(state.canRetry)
            state.load(); assertEquals(1, calls)
            assertTrue(state.retry()); state.load(); assertEquals(png, state.image)
        } finally { LocationPreviewBridge.uninstall(lease) }
    }
}
