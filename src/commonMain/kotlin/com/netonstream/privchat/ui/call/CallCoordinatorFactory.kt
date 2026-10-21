package com.netonstream.privchat.ui.call

import com.netonstream.privchat.sdk.PrivchatClient
import com.netonstream.privchat.sdk.call.createCallControl
import kotlinx.coroutines.CoroutineScope
import com.netonstream.privchat.ui.common.base.currentTimeMillis

fun createCallCoordinator(
    client: PrivchatClient,
    userId: ULong,
    media: CallMedia,
    scope: CoroutineScope,
    isCurrentSession: () -> Boolean,
): CallCoordinator = DefaultCallCoordinator(createCallControl(client), userId, media, scope, isCurrentSession, nowMs = ::currentTimeMillis)
