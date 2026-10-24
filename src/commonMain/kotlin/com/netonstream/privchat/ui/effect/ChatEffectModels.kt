package com.netonstream.privchat.ui.effect

import com.netonstream.privchat.ui.pag.PagRepresentation

data class ChatEffectSession(val platform: String, val account: ULong, val epoch: Int = 0)
enum class ChatEffectOrigin { LOCAL_SEND_CONFIRMED, LIVE_RECEIVE, HISTORY, RECONNECT_CATCHUP, LOCAL_REPLAY, UNKNOWN }
enum class ChatEffectChannelKind { DIRECT, GROUP }
enum class ChatEffectChannelScope { ALL, DIRECT, GROUP }
enum class ChatEffectPlacement { FULLSCREEN, LOCAL, BUBBLE }

data class ChatEffectCandidate(
    val origin: ChatEffectOrigin,
    val session: ChatEffectSession,
    val channelId: String,
    val serverMessageId: String,
    val senderId: String,
    val text: String,
    val occurredAtMs: Long,
    val plainText: Boolean = true,
    val forwarded: Boolean = false,
    val quoted: Boolean = false,
)

data class ChatEffectResource(val effectId: String, val effectVersion: Int, val pag: PagRepresentation)

data class ChatEffectRule(
    val ruleId: String,
    val ruleVersion: Int,
    val effectId: String,
    val effectVersion: Int,
    val keywords: List<String>,
    val channelScope: ChatEffectChannelScope = ChatEffectChannelScope.ALL,
    val placement: ChatEffectPlacement = ChatEffectPlacement.FULLSCREEN,
    val priority: Int = 0,
    val cooldownMs: Long = 30_000,
    val maxEventAgeMs: Long = 5_000,
    val expiresAtMs: Long = Long.MAX_VALUE,
)

data class ChatEffectSnapshot(
    val session: ChatEffectSession,
    val revision: Long,
    val expiresAtMs: Long,
    val rules: List<ChatEffectRule>,
    val resources: List<ChatEffectResource>,
    val schemaVersion: Int = 1,
    val revokedRuleIds: Set<String> = emptySet(),
    val revokedResourceIds: Set<String> = emptySet(),
)

interface ChatEffectCatalog {
    val session: ChatEffectSession?
    suspend fun snapshot(): ChatEffectSnapshot
}

data class ChatEffectPlayback(
    val requestId: String,
    val session: ChatEffectSession,
    val channelId: String,
    val serverMessageId: String,
    val rule: ChatEffectRule,
    val resource: ChatEffectResource,
    val admittedAtMonotonicMs: Long,
    val loadDeadlineMonotonicMs: Long,
    val firstFrameAtMonotonicMs: Long? = null,
)
