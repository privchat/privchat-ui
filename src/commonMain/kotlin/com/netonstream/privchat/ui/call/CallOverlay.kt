package com.netonstream.privchat.ui.call

import androidx.compose.runtime.*
import com.gearui.components.button.Button
import com.gearui.components.button.ButtonSize
import com.gearui.components.button.ButtonTheme
import com.gearui.components.actionsheet.ActionSheet
import com.gearui.components.actionsheet.ActionSheetItem
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.primitives.ScrollView
import com.gearui.theme.Theme
import com.netonstream.privchat.sdk.dto.call.CallMediaKind
import com.netonstream.privchat.sdk.dto.call.CallState
import com.netonstream.privchat.sdk.dto.call.CallSnapshot
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.models.displayName
import com.netonstream.privchat.ui.i18n.channelLabel
import com.netonstream.privchat.ui.i18n.initiatorLabel
import com.netonstream.privchat.ui.error.UserFacingError
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.layout.onGloballyPositioned
import com.tencent.kuikly.compose.ui.layout.positionInRoot
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch

/** Mount once above navigation, inside the app's theme/i18n providers. */
@Composable
fun CallOverlay(coordinator: CallCoordinator) {
    val platform = com.tencent.kuikly.compose.ui.platform.LocalConfiguration.current
    if (platform.isAndroid || platform.isIOS) {
        NativeCallOverlay(coordinator)
        return
    }
    val state by coordinator.state.collectAsState()
    val strings = PrivChatI18n.strings
    val labels = strings.dCall
    val scope = rememberCoroutineScope()
    val call = state.call
    if (call == null && state.error == null && !state.busy) return
    val joinedVideo = state.joined && call?.mediaKind == CallMediaKind.VIDEO
    var videoBounds by remember(coordinator, call?.callId) { mutableStateOf<CallVideoBounds?>(null) }
    val density = LocalDensity.current.density
    if (joinedVideo) {
        DisposableEffect(coordinator) { onDispose { coordinator.dismissVideo() } }
        LaunchedEffect(coordinator, videoBounds) { videoBounds?.let(coordinator::showVideo) }
    }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = if (joinedVideo) Alignment.BottomCenter else Alignment.TopCenter) {
      if (joinedVideo) {
          Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(maxHeight / 2).onGloballyPositioned { layout ->
              val position = layout.positionInRoot()
              videoBounds = CallVideoBounds(position.x / density, position.y / density,
                  layout.size.width / density, layout.size.height / density)
          })
      }
      // Native video reserves the top half. Controls must remain below it, including on small screens.
      ScrollView(Modifier.fillMaxWidth()
          .heightIn(max = if (joinedVideo) maxHeight / 2 else maxHeight)
          .padding(horizontal = 12.dp, vertical = if (joinedVideo) 8.dp else 56.dp)) {
        Column(
            Modifier.fillMaxWidth().background(Theme.colors.background, RoundedCornerShape(16.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (call != null) {
                CallIdentity(call)
                Text(if (call.mediaKind == CallMediaKind.VIDEO) labels.video else labels.audio, style = Theme.typography.titleMedium)
                Text(when {
                    state.incoming -> labels.incoming
                    call.state == CallState.RINGING -> labels.ringing
                    !state.mediaConnected -> labels.connecting
                    else -> labels.active
                })
                if (!state.joined) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!state.busy && (state.incoming || state.error != null)) {
                            Button(text = if (state.incoming) labels.accept else strings.retry, size = ButtonSize.SMALL,
                                onClick = { scope.launch { coordinator.accept() } })
                        }
                        // Keep decline/hang-up available while join is in flight.
                        Button(text = if (state.incoming) labels.reject else labels.hangUp, size = ButtonSize.SMALL,
                            theme = ButtonTheme.DANGER, onClick = {
                                scope.launch { if (state.incoming) coordinator.reject() else coordinator.hangUp() }
                            })
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(text = if (state.microphoneEnabled) labels.mute else labels.unmute, size = ButtonSize.SMALL,
                            onClick = { scope.launch { coordinator.setMicrophone(!state.microphoneEnabled) } })
                        Button(text = if (state.speakerEnabled) labels.speakerOff else labels.speakerOn, size = ButtonSize.SMALL,
                            onClick = { scope.launch { coordinator.setSpeaker(!state.speakerEnabled) } })
                    }
                    if (call.mediaKind == CallMediaKind.VIDEO) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(text = if (state.cameraEnabled) labels.cameraOff else labels.cameraOn, size = ButtonSize.SMALL,
                                onClick = { scope.launch { coordinator.setCamera(!state.cameraEnabled) } })
                            Button(text = labels.showVideo, size = ButtonSize.SMALL,
                                onClick = { videoBounds?.let(coordinator::showVideo) })
                        }
                    }
                    Button(text = labels.hangUp, size = ButtonSize.SMALL, theme = ButtonTheme.DANGER,
                        onClick = { scope.launch { coordinator.hangUp() } })
                }
            } else if (state.busy) {
                Text(strings.loading)
                Button(text = strings.cancel, size = ButtonSize.SMALL, onClick = { scope.launch { coordinator.hangUp() } })
            }
            state.error?.let { failure ->
                Text(when ((failure as? CallException)?.reason) {
                    CallFailure.UNAVAILABLE -> labels.unavailable
                    CallFailure.DISABLED -> labels.disabled
                    CallFailure.PARTICIPANT_LIMIT -> labels.participantLimit
                    CallFailure.BUSY -> labels.busy
                    CallFailure.MEDIA -> labels.mediaError
                    CallFailure.CONNECTION -> labels.connectionFailed
                    CallFailure.ROUTING -> labels.routingError
                    null -> UserFacingError.message(failure, strings.networkError)
                })
                Button(text = strings.confirm, size = ButtonSize.SMALL, onClick = coordinator::clearError)
            }
        }
      }
    }
}

@Composable
private fun CallIdentity(call: CallSnapshot) {
    val channels by PrivChat.channels.collectAsState()
    val knownUsers by PrivChat.knownUsers.collectAsState()
    val members by PrivChat.groupMembers.collectAsState()
    val friends by PrivChat.friends.collectAsState()
    val currentUserId by PrivChat.currentUserId.collectAsState()
    val currentUserName by PrivChat.currentUserName.collectAsState()
    val labels = PrivChatI18n.strings.dCall
    val channel = channels.firstOrNull { it.channelId == call.channelId }
    val initiatorName = when {
        members.any { it.channelId == call.channelId && it.userId == call.initiatorId } ->
            PrivChat.displayNameOf(call.initiatorId, call.channelId)
        else -> friends.firstOrNull { it.userId == call.initiatorId }?.displayName
            ?: knownUsers[call.initiatorId]?.let { PrivChat.displayNameOf(call.initiatorId) }
            ?: currentUserName?.takeIf { currentUserId == call.initiatorId }
            ?: channel?.takeIf { it.isDm && it.peerUserId == call.initiatorId }?.displayName
    }
    Text(labels.channelLabel(call.channelId, channel?.displayName), style = Theme.typography.titleMedium)
    Text(labels.initiatorLabel(call.initiatorId, initiatorName))
}

@Composable
fun rememberChatCallAction(channelId: ULong): (() -> Unit)? {
    val coordinator = CallUiBridge.coordinator ?: return null
    val state by coordinator.state.collectAsState()
    if (!state.available) return null
    val labels = PrivChatI18n.strings.dCall
    val scope = rememberCoroutineScope()
    return {
        val current = coordinator.state.value
        ActionSheet.showList(
            items = listOf(
                ActionSheetItem(label = labels.video, disabled = current.busy || current.call != null),
                ActionSheetItem(label = labels.audio, disabled = current.busy || current.call != null),
            ),
            onSelected = { _, index ->
                val latest = coordinator.state.value
                if (latest.available && !latest.busy && latest.call == null) {
                    scope.launch { coordinator.start(channelId, if (index == 0) CallMediaKind.VIDEO else CallMediaKind.AUDIO) }
                }
            },
        )
    }
}
