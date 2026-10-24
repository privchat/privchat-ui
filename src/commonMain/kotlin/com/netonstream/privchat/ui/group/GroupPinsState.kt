package com.netonstream.privchat.ui.group

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.netonstream.privchat.sdk.dto.GroupPinnedMessageView
import com.netonstream.privchat.sdk.dto.MessageEntry
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

object GroupPinPolicy {
    // Frozen protocol wire roles: Member=0, Owner=1, Admin=2.
    private const val MEMBER = 0
    private const val OWNER = 1
    private const val ADMIN = 2

    fun canPin(role: Int?): Boolean = role == OWNER || role == ADMIN

    fun canUnpin(actorId: ULong?, actorRole: Int?, pinnedBy: ULong, pinnerRole: Int?): Boolean =
        actorId != null && (actorRole == OWNER ||
            (actorRole == ADMIN && (pinnedBy == actorId || pinnerRole == MEMBER || pinnerRole == ADMIN)))

    fun sourceAnchor(channelId: ULong, serverId: ULong, window: List<MessageEntry>?): MessageEntry? =
        window?.firstOrNull { it.channelId == channelId && it.serverMessageId == serverId }
}

class GroupPinsState(private val channelId: ULong) {
    var messages: List<GroupPinnedMessageView> by mutableStateOf(emptyList())
        private set
    var hidden by mutableStateOf(false)
        private set
    private var readEpoch = 0L

    fun hide() { hidden = true }
    fun selectionSnapshot(): List<GroupPinnedMessageView> = messages.toList()

    suspend fun refresh(read: suspend () -> Result<List<GroupPinnedMessageView>>) {
        val epoch = ++readEpoch
        val result = read()
        currentCoroutineContext().ensureActive()
        if (epoch == readEpoch) result.onSuccess { pins ->
            messages = pins.filter { it.channelId == channelId && it.messageId > 0uL }
        }
    }
}

object PinTestTags {
    const val Bar = "group-pins-bar"
    const val Open = "group-pins-open"
    const val Hide = "group-pins-hide"
    const val Unpin = "group-pins-unpin"
    fun source(messageId: ULong) = "group-pin-source-$messageId"
}
