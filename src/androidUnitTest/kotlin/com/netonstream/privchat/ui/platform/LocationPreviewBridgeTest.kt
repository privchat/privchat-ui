package com.netonstream.privchat.ui.platform

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocationPreviewBridgeTest {
    private val point = LocationPresentation(39.9, 116.4, "wgs84", null)
    private val png = "data:image/png;base64,iVBORw0KGgo="

    @Test fun duplicatesShareAndOneCancellationDoesNotStopOtherConsumer() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        var calls = 0
        var stopped = false
        val lease = LocationPreviewBridge.install("server:1") { _, width, height ->
            calls++
            assertEquals(320, width)
            assertEquals(160, height)
            started.complete(Unit)
            try { finish.await(); png } finally { stopped = true }
        }
        try {
            val first = async(start = CoroutineStart.UNDISPATCHED) {
                LocationPreviewBridge.load(lease, point, 220, 110)
            }
            started.await()
            val second = async(start = CoroutineStart.UNDISPATCHED) {
                LocationPreviewBridge.load(lease, point, 220, 110)
            }
            first.cancelAndJoin()
            assertFalse(stopped)
            finish.complete(Unit)
            assertEquals(png, second.await())
            assertEquals(1, calls)
            assertTrue(lease.flights.isEmpty())
        } finally { LocationPreviewBridge.uninstall(lease) }
    }

    @Test fun differentRequestsAreSerializedAndResultsAreNotCached() = runBlocking {
        val firstStarted = CompletableDeferred<Unit>()
        val finishFirst = CompletableDeferred<Unit>()
        var calls = 0
        var active = 0
        var maximum = 0
        val lease = LocationPreviewBridge.install("server:1") { _, _, _ ->
            calls++; active++; maximum = maxOf(maximum, active)
            try {
                if (calls == 1) { firstStarted.complete(Unit); finishFirst.await() }
                png
            } finally { active-- }
        }
        try {
            val first = async { LocationPreviewBridge.load(lease, point, 320, 160) }
            firstStarted.await()
            val second = async(start = CoroutineStart.UNDISPATCHED) {
                LocationPreviewBridge.load(lease, point.copy(latitude = 40.0), 320, 160)
            }
            yield()
            assertEquals(1, calls)
            finishFirst.complete(Unit)
            assertEquals(png, first.await())
            assertEquals(png, second.await())
            assertEquals(1, maximum)
            assertEquals(png, LocationPreviewBridge.load(lease, point, 320, 160))
            assertEquals(3, calls)
        } finally { LocationPreviewBridge.uninstall(lease) }
    }

    @Test fun lastConsumerCancelsNativeWork() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val stopped = CompletableDeferred<Unit>()
        val lease = LocationPreviewBridge.install("server:1") { _, _, _ ->
            started.complete(Unit)
            try { awaitCancellation() } finally { stopped.complete(Unit) }
        }
        try {
            val consumer = async { LocationPreviewBridge.load(lease, point, 320, 160) }
            started.await()
            consumer.cancelAndJoin()
            stopped.await()
            assertTrue(lease.flights.isEmpty())
        } finally { LocationPreviewBridge.uninstall(lease) }
    }

    @Test fun replacementCancelsRunningAndQueuedWorkEvenForSameOwner() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val stopped = CompletableDeferred<Unit>()
        var calls = 0
        val old = LocationPreviewBridge.install("server:1") { _, _, _ ->
            calls++; started.complete(Unit)
            try { awaitCancellation() } finally { stopped.complete(Unit) }
        }
        val first = async { LocationPreviewBridge.load(old, point, 320, 160) }
        started.await()
        val queued = async(start = CoroutineStart.UNDISPATCHED) {
            LocationPreviewBridge.load(old, point.copy(latitude = 40.0), 320, 160)
        }
        val replacement = LocationPreviewBridge.install("server:1") { _, _, _ -> png }
        try {
            stopped.await(); first.join(); queued.join()
            assertTrue(first.isCancelled)
            assertTrue(queued.isCancelled)
            assertEquals(1, calls)
            LocationPreviewBridge.uninstall(old)
            assertTrue(LocationPreviewBridge.registration === replacement)
            assertNull(LocationPreviewBridge.load(old, point, 320, 160))
            assertEquals(png, LocationPreviewBridge.load(replacement, point, 320, 160))
        } finally { LocationPreviewBridge.uninstall(replacement) }
    }

    @Test fun queueIsBoundedAndOverflowDoesNotEnterNativeLoader() = runBlocking {
        val started = CompletableDeferred<Unit>()
        var calls = 0
        val lease = LocationPreviewBridge.install("server:1") { _, _, _ ->
            calls++; started.complete(Unit); awaitCancellation()
        }
        try {
            val consumers = (0 until 16).map { index ->
                async(start = CoroutineStart.UNDISPATCHED) {
                    LocationPreviewBridge.load(lease, point.copy(latitude = index.toDouble()), 320, 160)
                }
            }
            started.await()
            assertEquals(16, lease.flights.size)
            assertNull(LocationPreviewBridge.load(lease, point.copy(latitude = 30.0), 320, 160))
            assertEquals(1, calls)
            consumers.forEach { it.cancelAndJoin() }
            assertTrue(lease.flights.isEmpty())
        } finally { LocationPreviewBridge.uninstall(lease) }
    }

    @Test fun failureAndNonLocalUrisFallBackWithoutPoisoningLaterRequests() = runBlocking {
        var calls = 0
        val lease = LocationPreviewBridge.install("server:1") { _, _, _ ->
            when (++calls) {
                1 -> error("native failure")
                2 -> "https://example.invalid/preview.png"
                else -> png
            }
        }
        try {
            assertNull(LocationPreviewBridge.load(lease, point, 320, 160))
            assertNull(LocationPreviewBridge.load(lease, point, 320, 160))
            assertEquals(png, LocationPreviewBridge.load(lease, point, 320, 160))
        } finally { LocationPreviewBridge.uninstall(lease) }
    }
}
