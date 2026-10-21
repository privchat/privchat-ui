package com.netonstream.privchat.ui.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.ceil

/**
 * UI-thread owned, local-only snapshot bridge. Install for server + account scope after consent.
 * Reinstall on account/provider/policy changes; uninstall BEFORE logout or scope changes.
 * Hosts must bound native work, honor cancellation, preserve attribution, and return a PNG data URI.
 * Requested dimensions are proportionally enlarged to at least 256 x 160 physical pixels.
 * At most 16 distinct in-flight requests per lease; native work is serialized and duplicates share
 * the same flight. All calls (including load) must run on the same UI dispatcher. Each native call
 * has a 20-second deadline after acquiring its slot. No result cache or file/HTTP URI is accepted;
 * native image-renderer memory caches
 * must also be cleared by the host when disposing the account's render context.
 */
object LocationPreviewBridge {
    class Registration internal constructor(
        val ownerKey: String,
        internal val loader: suspend (LocationPresentation, Int, Int) -> String?,
    ) {
        internal val job = SupervisorJob()
        internal val slots = Semaphore(1)
        internal val flights = mutableMapOf<Request, Flight>()
        internal fun close() {
            job.cancel()
            flights.clear()
        }
    }

    internal data class Request(val location: LocationPresentation, val width: Int, val height: Int)
    internal class Flight(val result: Deferred<String?>, var consumers: Int = 0)

    var registration: Registration? by mutableStateOf(null)
        private set

    fun install(
        ownerKey: String,
        load: suspend (LocationPresentation, Int, Int) -> String?,
    ): Registration {
        require(ownerKey.isNotBlank())
        registration?.close()
        return Registration(ownerKey, load).also { registration = it }
    }

    /** An old host cannot uninstall a newer host's registration. */
    fun uninstall(lease: Registration) {
        lease.close()
        if (registration === lease) registration = null
    }

    suspend fun load(
        lease: Registration,
        location: LocationPresentation,
        widthPx: Int,
        heightPx: Int,
    ): String? {
        if (registration !== lease || widthPx !in 1..4096 || heightPx !in 1..4096) return null
        if (!location.latitude.isFinite() || location.latitude !in -90.0..90.0 ||
            !location.longitude.isFinite() || location.longitude !in -180.0..180.0) return null
        val scale = maxOf(1.0, 256.0 / widthPx, 160.0 / heightPx)
        val renderWidth = ceil(widthPx * scale).toInt()
        val renderHeight = ceil(heightPx * scale).toInt()
        if (renderWidth > 4096 || renderHeight > 4096) return null
        val key = Request(location, renderWidth, renderHeight)
        val flight = lease.flights[key] ?: run {
            if (lease.flights.size >= MAX_FLIGHTS) return null
            // Keep the caller's UI dispatcher, but not its Job: one departing consumer must not
            // cancel a shared snapshot still needed by another consumer.
            val scope = CoroutineScope(currentCoroutineContext().minusKey(Job) + lease.job)
            Flight(scope.async(start = CoroutineStart.LAZY) {
                lease.slots.withPermit {
                    if (registration !== lease) return@withPermit null
                    try {
                        withTimeoutOrNull(20_000) { lease.loader(location, renderWidth, renderHeight) }
                            ?.takeIf {
                                it.startsWith(PNG_PREFIX) && it.length > PNG_PREFIX.length &&
                                    it.length <= MAX_URI_CHARS
                            }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                }
            }).also { lease.flights[key] = it }
        }
        flight.consumers++
        try {
            val image = flight.result.await()
            return image.takeIf { registration === lease }
        } finally {
            // No suspension: cleanup also runs when the consumer has been cancelled.
            flight.consumers--
            if (flight.consumers == 0) {
                if (lease.flights[key] === flight) lease.flights.remove(key)
                flight.result.cancel()
            }
        }
    }

    private const val PNG_PREFIX = "data:image/png;base64,"
    private const val MAX_URI_CHARS = 8 * 1024 * 1024
    private const val MAX_FLIGHTS = 16
}
