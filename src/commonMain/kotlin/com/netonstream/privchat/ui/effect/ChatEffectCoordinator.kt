package com.netonstream.privchat.ui.effect

import com.netonstream.privchat.ui.pag.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** UI-thread owned. No SDK observers, network requests, playback queues or inferred origins. */
class ChatEffectCoordinator(
    private val normalizeNfc: (String) -> String?,
    private val nowEpochMs: () -> Long,
    private val nowMonotonicMs: () -> Long,
    private val runtimeAvailable: (String) -> Boolean = { false },
) {
    private val state = MutableStateFlow<ChatEffectPlayback?>(null)
    val active: StateFlow<ChatEffectPlayback?> = state.asStateFlow()
    private var owner: ChatEffectSession? = null
    private var snapshot: ChatEffectSnapshot? = null
    private var highestRevision = -1L
    private var channel: String? = null
    private var kind = ChatEffectChannelKind.DIRECT
    private var visible = false
    private var foreground = false
    private var enabled = false
    private var reducedMotion = false
    private var sequence = 0L
    private val seenMessages = linkedMapOf<Pair<String, String>, Long>()
    private val dedup = linkedMapOf<DedupKey, Long>()
    private var lastAdmittedAt: Long? = null
    private val rules = linkedMapOf<String, Long>()

    private data class DedupKey(val session: ChatEffectSession, val channelId: String, val messageId: String, val ruleId: String, val ruleVersion: Int)

    fun session(value: ChatEffectSession?) {
        if (owner == value) return
        state.value = null
        owner = value?.takeIf { it.platform.isNotBlank() && it.account != 0uL }
        snapshot = null
        highestRevision = -1
        channel = null
        visible = false
        foreground = false
        enabled = false
        seenMessages.clear(); dedup.clear(); lastAdmittedAt = null; rules.clear()
    }

    fun setVisible(channelId: String?, channelKind: ChatEffectChannelKind = ChatEffectChannelKind.DIRECT, visible: Boolean = true, foreground: Boolean = true) {
        val next = channelId?.takeIf(::serverId)
        if (channel != next || kind != channelKind || !visible || !foreground) state.value = null
        channel = next
        kind = channelKind
        this.visible = visible && next != null
        this.foreground = foreground
    }

    fun setting(enabled: Boolean, reducedMotion: Boolean = this.reducedMotion) {
        this.enabled = enabled
        this.reducedMotion = reducedMotion
        if (!enabled || reducedMotion) state.value = null
    }

    fun setForeground(value: Boolean) {
        foreground = value
        if (!value) state.value = null
    }

    fun replaceRules(value: ChatEffectSnapshot) {
        if (value.session != owner || value.revision < highestRevision) return
        if (value.revision == highestRevision) {
            if (value != snapshot) { snapshot = null; state.value = null }
            return
        }
        highestRevision = value.revision
        val valid = value.schemaVersion == 1 && value.revision >= 0 && value.rules.size <= 512 && value.resources.size <= 512 &&
            value.rules.map { it.ruleId }.toSet().size == value.rules.size &&
            value.resources.map { it.effectId to it.effectVersion }.toSet().size == value.resources.size &&
            value.rules.sumOf { it.keywords.size } <= 4096
        snapshot = value.takeIf { valid }
        tick()
        state.value?.let { current ->
            if (snapshot?.rules?.none { it == current.rule } != false || snapshot?.resources?.none { it == current.resource } != false) state.value = null
        }
    }

    fun offer(candidate: ChatEffectCandidate): Boolean {
        tick()
        val session = owner ?: return false
        if (candidate.session != session || !serverId(candidate.channelId) || !serverId(candidate.serverMessageId) || !serverId(candidate.senderId)) return false
        if (candidate.origin != ChatEffectOrigin.LOCAL_SEND_CONFIRMED && candidate.origin != ChatEffectOrigin.LIVE_RECEIVE) return false
        if (candidate.origin == ChatEffectOrigin.LIVE_RECEIVE && candidate.senderId == session.account.toString()) return false
        if (candidate.origin == ChatEffectOrigin.LOCAL_SEND_CONFIRMED && candidate.senderId != session.account.toString()) return false
        if (!candidate.plainText || candidate.forwarded || candidate.quoted || candidate.text.length > 4096) return false
        val wall = nowEpochMs()
        val mono = nowMonotonicMs()
        if (candidate.occurredAtMs <= 0 || candidate.occurredAtMs > wall || wall - candidate.occurredAtMs > 5000) return false
        prune(seenMessages, mono)
        val message = candidate.channelId to candidate.serverMessageId
        if (message in seenMessages) return false
        seenMessages[message] = mono // Consumed even when hidden/disabled/busy: never replay on resume.
        if (!visible || !foreground || !enabled || reducedMotion || candidate.channelId != channel) return false
        val config = snapshot?.takeIf { it.expiresAtMs > wall } ?: return false
        val text = normalized(candidate.text) ?: return false
        val rule = config.rules.asSequence().filter {
            it.ruleId.isNotBlank() && it.ruleId.length <= 128 && it.ruleVersion > 0 && it.effectId.isNotBlank() && it.effectId.length <= 128 && it.effectVersion > 0 &&
                it.placement == ChatEffectPlacement.FULLSCREEN && it.expiresAtMs > wall && it.maxEventAgeMs > 0 &&
                wall - candidate.occurredAtMs <= minOf(5000, it.maxEventAgeMs) && it.ruleId !in config.revokedRuleIds &&
                (it.channelScope == ChatEffectChannelScope.ALL || it.channelScope.name == kind.name) &&
                it.keywords.any { keyword -> keyword.length <= 4096 && normalized(keyword) == text }
        }.sortedWith(compareByDescending<ChatEffectRule> { it.priority }.thenBy { it.ruleId }).firstOrNull() ?: return false
        val key = DedupKey(session, candidate.channelId, candidate.serverMessageId, rule.ruleId, rule.ruleVersion)
        prune(dedup, mono)
        if (key in dedup) return false
        dedup[key] = mono // Before admission/loading: errors and cancellations are not retried.
        if (state.value != null || cooling(lastAdmittedAt, mono, 10_000) || cooling(rules[rule.ruleId], mono, maxOf(30_000, rule.cooldownMs))) return false
        val resource = config.resources.firstOrNull { it.effectId == rule.effectId && it.effectVersion == rule.effectVersion } ?: return false
        if (resource.pag.resourceId in config.revokedResourceIds || !resource.pag.validFor(PagUsage.EFFECT) || !runCatching { runtimeAvailable(resource.pag.profile) }.getOrDefault(false)) return false
        lastAdmittedAt = mono
        rules[rule.ruleId] = mono
        trim(rules)
        state.value = ChatEffectPlayback(
            requestId = "effect-${++sequence}", session = session, channelId = candidate.channelId,
            serverMessageId = candidate.serverMessageId, rule = rule, resource = resource,
            admittedAtMonotonicMs = mono,
            loadDeadlineMonotonicMs = mono + minOf(5000, rule.maxEventAgeMs) - (wall-candidate.occurredAtMs),
        )
        return true
    }

    fun onPagEvent(event: PagEvent) {
        tick()
        val current = state.value?.takeIf { it.requestId == event.requestId } ?: return
        when (event.type) {
            PagEventType.READY -> Unit
            PagEventType.FRAME -> if (current.firstFrameAtMonotonicMs == null) state.value = current.copy(firstFrameAtMonotonicMs = nowMonotonicMs())
            PagEventType.COMPLETE, PagEventType.ERROR -> state.value = null
            PagEventType.REDUCED_MOTION -> { reducedMotion = true; state.value = null }
        }
    }

    fun tick() {
        val current = state.value ?: return
        val wall = nowEpochMs()
        val mono = nowMonotonicMs()
        val config = snapshot
        val expired = if (current.firstFrameAtMonotonicMs == null) mono > current.loadDeadlineMonotonicMs
            else mono - current.firstFrameAtMonotonicMs >= current.resource.pag.durationMs
        if (current.session != owner || current.channelId != channel || !visible || !foreground || !enabled || reducedMotion ||
            mono < current.admittedAtMonotonicMs || expired || config == null || config.expiresAtMs <= wall || current.rule.expiresAtMs <= wall ||
            current.rule.ruleId in config.revokedRuleIds || current.resource.pag.resourceId in config.revokedResourceIds ||
            !runCatching { runtimeAvailable(current.resource.pag.profile) }.getOrDefault(false)) state.value = null
    }

    fun cancel(requestId: String? = active.value?.requestId) { if (state.value?.requestId == requestId) state.value = null }
    fun close() { session(null) }

    private fun normalized(value: String): String? = runCatching { normalizeNfc(value)?.trim()?.takeIf { it.isNotEmpty() } }.getOrNull()
    private fun cooling(last: Long?, now: Long, duration: Long): Boolean = last != null && (now < last || now-last < duration)
    private fun <K> prune(map: MutableMap<K,Long>, now: Long) {
        map.keys.filter { key -> map.getValue(key).let { now < it || now-it > 120_000 } }.forEach(map::remove)
        trim(map)
    }
    private fun <K> trim(map: MutableMap<K,Long>) { while (map.size >= 2048) map.remove(map.keys.first()) }

    companion object {
        fun serverId(value: String): Boolean = value.isNotEmpty() && value[0] in '1'..'9' && value.all { it in '0'..'9' } && value.toULongOrNull() != null
    }
}
