package com.netonstream.privchat.ui.call

import androidx.compose.runtime.*
import com.gearui.components.actionsheet.ActionSheet
import com.netonstream.privchat.sdk.dto.call.CallMediaKind
import com.netonstream.privchat.sdk.dto.call.CallState
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.avatar.AvatarText
import com.netonstream.privchat.ui.error.UserFacingError
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.netonstream.privchat.ui.i18n.channelLabel
import com.netonstream.privchat.ui.models.displayName
import com.tencent.kuikly.compose.BackHandler
import com.tencent.kuikly.compose.extension.MakeKuiklyComposeNode
import com.tencent.kuikly.compose.extension.setEvent
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.layout.onGloballyPositioned
import com.tencent.kuikly.compose.ui.layout.positionInRoot
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.platform.LocalFocusManager
import com.tencent.kuikly.compose.ui.platform.LocalSoftwareKeyboardController
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.event.Event
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

/** Presentation only. Every operation still passes through the session's call coordinator. */
class KrCallScreen : DeclarativeBaseView<KrCallScreenAttr, KrCallScreenEvent>() {
    override fun viewName() = "KrCallScreen"
    override fun createAttr() = KrCallScreenAttr()
    override fun createEvent() = KrCallScreenEvent()
}
class KrCallScreenAttr : Attr() {
    fun state(json: String) { setProp("state", json) }
}
class KrCallScreenEvent : Event() {
    fun onAction(handler: (Any?) -> Unit) { register("onAction", handler) }
}

@Composable
internal fun NativeCallOverlay(coordinator: CallCoordinator) {
    val state by coordinator.state.collectAsState()
    val channels by PrivChat.channels.collectAsState()
    val strings = PrivChatI18n.strings
    val labels = strings.dCall
    val scope = rememberCoroutineScope()
    val call = state.call
    if (call == null && state.error == null && !state.busy) return
    // A phone-style page cannot be dismissed into an invisible, still active call.
    BackHandler { }
    val channelId = call?.channelId ?: state.presentation?.channelId
    val presentation = state.presentation?.takeIf { it.channelId == channelId }
    val channel = channels.firstOrNull { it.channelId == channelId }
    val name = channelId?.let { labels.channelLabel(it, channel?.displayName?.takeIf(String::isNotBlank)
        ?: presentation?.name?.takeIf(String::isNotBlank)) } ?: labels.entry
    val video = (call?.mediaKind ?: presentation?.mediaKind) == CallMediaKind.VIDEO
    val outgoing = !state.incoming && (call?.state == CallState.RINGING ||
        (!state.joined && (call != null || (state.busy && presentation != null))))
    val status = when {
        call == null && state.error != null -> ""
        state.incoming -> if (video) labels.incomingVideoInvitation else labels.incomingVoiceInvitation
        call?.state == CallState.RINGING -> labels.waitingForAnswer
        state.mediaConnected -> labels.active
        else -> labels.connecting
    }
    val failure = state.error?.let {
        when ((it as? CallException)?.reason) {
            CallFailure.UNAVAILABLE -> labels.unavailable
            CallFailure.DISABLED -> labels.disabled
            CallFailure.PARTICIPANT_LIMIT -> labels.participantLimit
            CallFailure.BUSY -> labels.busy
            CallFailure.MEDIA -> labels.mediaError
            CallFailure.CONNECTION -> labels.connectionFailed
            CallFailure.ROUTING -> labels.routingError
            null -> UserFacingError.message(it, strings.networkError)
        }
    }
    val callId = call?.callId?.toString().orEmpty()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(coordinator, callId) {
        focus.clearFocus(force = true)
        keyboard?.hide()
        ActionSheet.dismiss()
    }
    val actions = buildJsonArray {
        fun action(id: String, label: String, icon: String, tone: String = "normal", selected: Boolean = false, enabled: Boolean = true) {
            add(buildJsonObject {
                put("id", id); put("label", label); put("icon", icon)
                put("tone", tone); put("selected", selected)
                put("enabled", enabled)
            })
        }
        if (outgoing) {
            action("microphone", if (state.microphoneEnabled) labels.microphoneIsOn else labels.microphoneIsOff,
                if (state.microphoneEnabled) "mic" else "micOff", selected = state.microphoneEnabled)
            action("end", strings.cancel, "end", "danger")
            action("speaker", if (state.speakerEnabled) labels.speakerIsOn else labels.speakerIsOff,
                if (state.speakerEnabled) "speaker" else "speakerOff", selected = state.speakerEnabled)
        } else if (state.joined) {
            action("microphone", if (state.microphoneEnabled) labels.mute else labels.unmute,
                if (state.microphoneEnabled) "mic" else "micOff", selected = state.microphoneEnabled)
            action("speaker", if (state.speakerEnabled) labels.speakerOff else labels.speakerOn,
                "speaker", selected = state.speakerEnabled)
            if (video) action("camera", if (state.cameraEnabled) labels.cameraOff else labels.cameraOn,
                "camera", selected = state.cameraEnabled)
        }
        if (!outgoing && (call != null || state.busy)) {
            action("end", if (state.incoming) labels.declineIncoming else labels.hangUp, "end", "danger")
            if (!state.joined && (state.incoming || (!state.busy && state.error != null))) {
                action("accept", if (state.incoming) labels.accept else strings.retry, "answer", "success", enabled = !state.busy)
            }
        }
        if (failure != null && call == null && !state.busy) action("confirm", strings.confirm, "close")
    }
    val json = buildJsonObject {
        put("callId", callId); put("name", name)
        put("layout", if (state.incoming) "incoming" else if (outgoing) "outgoing" else "standard")
        put("ring", state.incoming && !state.busy && call?.state == CallState.RINGING)
        put("ringback", !state.incoming && call?.state == CallState.RINGING)
        put("videoBackground", state.joined && video && !outgoing && !state.incoming)
        put("expiresAtMs", call?.expiresAtMs ?: 0L)
        put("hint", if (outgoing && !state.speakerEnabled) labels.earpieceHint else "")
        put("initials", AvatarText.initialsOf(name = name))
        put("avatar", channel?.peerAvatarUrl?.takeIf(String::isNotBlank)
            ?: channel?.avatarUrl?.takeIf(String::isNotBlank) ?: presentation?.avatarUrl.orEmpty())
        put("kind", if (video) labels.video else labels.audio)
        put("status", status); put("error", failure.orEmpty()); put("actions", actions)
    }.toString()
    val onAction = rememberUpdatedState<(Any?) -> Unit> { raw ->
        val event = runCatching { Json.parseToJsonElement(raw.toString()).jsonObject }.getOrNull()
        val id = event?.get("id")?.jsonPrimitive?.contentOrNull
        val latest = coordinator.state.value
        if (CallUiBridge.coordinator === coordinator && event?.get("callId")?.jsonPrimitive?.contentOrNull == callId &&
            latest.call?.callId?.toString().orEmpty() == callId &&
            actions.any { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull == id && it.jsonObject["enabled"]?.jsonPrimitive?.booleanOrNull != false }) {
            scope.launch {
                when (id) {
                    "end" -> if (latest.incoming) coordinator.reject() else coordinator.hangUp()
                    "accept" -> coordinator.accept()
                    "microphone" -> coordinator.setMicrophone(!latest.microphoneEnabled)
                    "speaker" -> coordinator.setSpeaker(!latest.speakerEnabled)
                    "camera" -> coordinator.setCamera(!latest.cameraEnabled)
                    "confirm" -> coordinator.clearError()
                }
            }
        }
    }
    val density = LocalDensity.current.density
    var videoBounds by remember(coordinator, callId) { mutableStateOf<CallVideoBounds?>(null) }
    if (state.joined && video) {
        DisposableEffect(coordinator) { onDispose { coordinator.dismissVideo() } }
        LaunchedEffect(videoBounds) { videoBounds?.let(coordinator::showVideo) }
    }
    // Seal Compose hit testing as well as native input, so background taps cannot focus chat.
    BoxWithConstraints(Modifier.fillMaxSize().setEvent("touchDown") { }.pointerInput(Unit) {
        awaitPointerEventScope { while (true) awaitPointerEvent() }
    }) {
        MakeKuiklyComposeNode<KrCallScreen>(factory = { KrCallScreen() }, modifier = Modifier.fillMaxSize(),
            viewInit = { getViewEvent().onAction { onAction.value(it) }; getViewAttr().state(json) },
            viewUpdate = { it.getViewAttr().state(json) })
        if (state.joined && video) Box(Modifier.fillMaxSize().onGloballyPositioned {
            val p = it.positionInRoot()
            videoBounds = CallVideoBounds(p.x / density, p.y / density, it.size.width / density, it.size.height / density)
        })
    }
}
