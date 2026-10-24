package com.netonstream.privchat.ui.pag

/** Only populated from an authorized, immutable Platform representation mapping. */
data class PagRepresentation(
    val resourceId: String,
    val url: String,
    val sha256: String,
    val size: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val profile: String = STICKER_PROFILE,
) {
    fun validFor(usage: PagUsage): Boolean {
        val sticker = usage == PagUsage.STICKER
        return resourceId.isNotBlank() && trustedUrlShape(url) &&
            sha256.length == 64 && sha256.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' } &&
            size in 1..(if (sticker) 500L * 1024 else 2L * 1024 * 1024) &&
            width in 1..(if (sticker) 512 else 2048) && height in 1..(if (sticker) 512 else 2048) &&
            width.toLong() * height <= 8_388_608L &&
            durationMs in 1..(if (sticker) 3000L else 8000L) &&
            profile == if (sticker) STICKER_PROFILE else EFFECT_PROFILE
    }

    companion object {
        const val STICKER_PROFILE = "pag-sticker-v1"
        const val EFFECT_PROFILE = "pag-effect-v1"

        // Origin authorization belongs to the resolver. This only rejects executable/local URLs.
        fun trustedUrlShape(url: String): Boolean {
            if (url.length > 4096 || url.any { it.isWhitespace() || it.isISOControl() || it == '\\' || it == '#' }) return false
            val scheme = url.substringBefore("://", "").lowercase()
            if (scheme != "https" && scheme != "http") return false
            val authority = url.substringAfter("://").substringBefore('/').substringBefore('?')
            if (authority.isEmpty() || authority.any { it == '@' || it == '%' }) return false
            val host: String
            val port: String?
            if (authority.startsWith('[')) {
                val end = authority.indexOf(']')
                if (end < 2) return false
                host = authority.substring(0,end+1).lowercase()
                val tail = authority.substring(end+1)
                if (tail.isNotEmpty() && !tail.startsWith(':')) return false
                port = tail.takeIf { it.isNotEmpty() }?.drop(1)
                if (host.drop(1).dropLast(1).any { it !in "0123456789abcdef:." }) return false
            } else {
                if (authority.count { it == ':' } > 1) return false
                host = authority.substringBefore(':').lowercase()
                port = authority.substringAfter(':', "").takeIf { ':' in authority }
                if (host.isEmpty() || host.split('.').any { part -> part.isEmpty() || !part.first().isLetterOrDigit() || !part.last().isLetterOrDigit() || part.any { !it.isLetterOrDigit() && it != '-' } || part.any { it.code > 127 } }) return false
            }
            if (port != null && (port.isEmpty() || port.any { it !in '0'..'9' } || port.toIntOrNull() !in 1..65535)) return false
            return scheme == "https" || host in setOf("127.0.0.1", "localhost", "[::1]")
        }
    }
}

enum class PagUsage(val wire: String) { STICKER("sticker"), EFFECT("effect") }
enum class PagHealth { UNAVAILABLE, INITIALIZING, HEALTHY, DEGRADED, DISABLED }

data class PagCapabilities(
    val compiled: Boolean = false,
    val initialized: Boolean = false,
    val supportedProfiles: Set<String> = emptySet(),
    val health: PagHealth = PagHealth.UNAVAILABLE,
) {
    fun supports(profile: String): Boolean = compiled && initialized && health == PagHealth.HEALTHY && profile in supportedProfiles
}

data class PagPlaybackRequest(
    val requestId: String,
    val resource: PagRepresentation,
    val usage: PagUsage,
    val loop: Boolean,
)

enum class PagEventType(val wire: String) {
    READY("ready"), FRAME("frame"), COMPLETE("complete"), ERROR("error"), REDUCED_MOTION("reducedMotion")
}
data class PagEvent(val requestId: String, val type: PagEventType, val code: String? = null)

/** Shared sticker/effect PAG admission. Native hosts still enforce their measured GPU limits. */
class PagPlaybackBudget(maxPlayers: Int = 4) {
    private val limit = maxPlayers.coerceIn(1, 4)
    private val owners = mutableMapOf<Any, PagUsage>()
    fun acquire(owner: Any, usage: PagUsage): Boolean {
        owners[owner]?.let { return it == usage }
        if (owners.size >= limit || (usage == PagUsage.EFFECT && PagUsage.EFFECT in owners.values)) return false
        owners[owner] = usage
        return true
    }
    fun release(owner: Any) { owners.remove(owner) }
    fun clear() { owners.clear() }
    val size: Int get() = owners.size
}
