# privchat-ui

[English](./README.md) | [简体中文](./README.zh-Hans.md)

The shared UI layer of PrivChat: every screen and chat component, written once
in Kotlin Multiplatform and used unchanged by the Android and iOS apps.

- Coordinates: `com.netonstream.privchat:privchat-ui` (composite build only, not yet published)
- Targets: Android, iOS (arm64 / simulator arm64 / x64)
- Built on [GearUI Kit](https://github.com/gearui/gearui-kit) for UI and
  [privchat-sdk-kotlin](https://github.com/privchat/privchat-sdk-kotlin) for data

## Where it sits

```
┌──────────────────────────────────────────────────────────┐
│  host app              the shipping app: login, startup,  │
│                        navigation shell, push, settings   │
├──────────────────────────────────────────────────────────┤
│  privchat-ui  (this)   pages + chat components + runtime  │
│                        state; owns nothing about network  │
├────────────────────────────┬─────────────────────────────┤
│  gearui-kit                │  privchat-sdk-kotlin         │
│  design system, tokens,    │  PrivchatClient, DTOs,       │
│  72 components, i18n,      │  events — a UniFFI binding   │
│  overlay + safe-area       │  over the Rust SDK           │
│  runtime                   │                              │
├────────────────────────────┴─────────────────────────────┤
│  KuiklyUI (Compose Multiplatform renderer)  ·  privchat-sdk (Rust)  │
└──────────────────────────────────────────────────────────┘
```

Two rules follow from that picture and the code is arranged to keep them true:

1. **privchat-ui talks to the network only through `PrivchatClient`.** It never
   opens a socket, never parses a wire message, never decides when to reconnect.
   The Rust SDK does all of that; the Kotlin binding exposes it; this layer
   renders it.
2. **privchat-ui depends on GearUI Kit, not on KuiklyUI.** `build.gradle.kts`
   declares no KuiklyUI dependency at all. Buttons, inputs, lists, sheets,
   toasts, dialogs, theming, spacing, i18n plumbing — all of it comes from
   `com.gearui.*`. The Compose primitives that appear in page code (`Modifier`,
   layout, animation under `com.tencent.kuikly.compose.*`) are the ones GearUI
   Kit re-exports as its own `api` surface; they arrive transitively and would
   follow the kit if it ever swapped renderers.

## How the SDK is used

The SDK is reached through one façade, `PrivChat`, and one runtime store,
`ClientRuntime`. Pages never hold a client of their own.

### `PrivChat` — data façade

`com.netonstream.privchat.ui.PrivChat` is a singleton that the app injects a
client into once, then everything else reads from it.

```kotlin
// app layer, once the SDK client exists
PrivChat.init(client)          // client: com.netonstream.privchat.sdk.PrivchatClient
```

It exposes the SDK's own DTOs as `StateFlow`s — **no mapping layer, no view
models of its own**. What the SDK calls a `ChannelListEntry`, `MessageEntry`,
`FriendEntry`, `GroupEntry` or `PresenceEntry` is exactly what a page collects:

```kotlin
val channels   by PrivChat.channels.collectAsState()      // List<ChannelListEntry>
val messages   by PrivChat.messages.collectAsState()      // List<MessageEntry>
val friends    by PrivChat.friends.collectAsState()       // List<FriendEntry>
val groups     by PrivChat.groups.collectAsState()        // List<GroupEntry>
val presences  by PrivChat.presences.collectAsState()     // Map<ULong, PresenceEntry>
val me         by PrivChat.currentUserId.collectAsState()
```

Writes go straight to the client, which is exposed for exactly this:

```kotlin
PrivChat.client.sendText(channelId, channelType, "hello")
PrivChat.client.forwardMessage(...)
```

The one thing `PrivChat` adds on top of the SDK is **local UI state the SDK
has no business knowing**: input drafts and reply-to per channel (persisted
through the platform `DraftStore` and restored on cold start), the "read
watermark" that zeroes an unread badge before the server has caught up,
in-flight image sizes, voice playback position, and the like. Those live in
`channelLocalStates` and `uiState`.

`PrivChat.reset()` clears everything on logout / account switch so nothing
leaks between users.

### `ClientRuntime` — connectivity, sync and send-queue truth

`com.netonstream.privchat.ui.runtime.ClientRuntime` is the **single source of
truth for the three stability chains**: connectivity, sync, and the outbound
send queue. The app layer feeds it SDK events; pages subscribe to its
`StateFlow`s and nothing else.

```
SDK event ──▶ host app's event handler ──▶ ClientRuntime.on…()
                                                              │
   ConnectivityState / SyncState / SendQueueState  ◀──────────┘
                     │
                     ▼
   pages read these; none of them inspects the SDK connection itself
```

Events routed in: `connection_state_changed`, `network_hint_changed`,
`resume_sync_*`, `sync_entities_applied`, `message_send_status_changed`,
`outbound_queue_updated`, `forced_logout`.

The rule that makes this worth having: **a page must never decide on its own
whether it is "offline", "syncing" or "sending"**. Before this store existed
each page had its own guess and they disagreed on screen. Now the conversation
list's title turns into "Reconnecting…" and the message page's send button
greys out from the same boolean.

Errors cross the boundary as a sealed `ClientRuntimeError`
(`NetworkUnavailable`, `GatewayDisconnected`, `AuthExpired`, `ServerBusy`,
`SyncFailed`, `Unknown`) and reach the user only through
`userFacingMessage(strings)`, which returns localized copy. Raw reasons and
exception text are logged, never shown.

### Pages take callbacks, not the SDK

A page's signature is data in, intents out:

```kotlin
@Composable
fun ConversationPage(
    onChannelClick: (ChannelListEntry) -> Unit,
    onGlobalSearch: () -> Unit = {},
    onCreateGroup: () -> Unit = {},
    statusTitle: String? = null,       // "Reconnecting…" shown in place of the title
    statusBusy: Boolean = false,       // spinner beside it while still trying
    onPinChannel: (suspend (ULong, Boolean) -> Result<Boolean>)? = null,
    ...
)
```

Navigation is the app's decision, so it arrives as lambdas. Data comes from
`PrivChat` flows collected inside the page. Anything that needs the SDK to do
work is either a call on `PrivChat.client` or a suspend callback the app
supplies. Pages available today: conversation list, message thread, contacts,
friend requests, friend / chat / group settings, group create / invite /
members / approval, user profile, global search, forward picker, and edit
dialogs for names and remarks.

## How GearUI Kit is used

privchat-ui is a **consumer** of GearUI Kit, in the sense the kit's own README
means: it depends on the published components and tokens and does not fork or
restyle them.

- **Components**: `NavBar`, `Tabs`, `Input`, `AutoResizeTextarea`, `Button`,
  `Checkbox`, `Switch`, `Cell`, `SearchBar`, `SwipeCell`, `Swiper`,
  `ActionSheet`, `Dialog` / `ConfirmDialog`, `ContextMenu`, `Toast`,
  `Loading`, `EmptyState`, `GearImage`, `Icons` — 176 imports from
  `com.gearui.components` across the codebase. Chat-specific pieces (message
  bubbles, the actions menu, the avatar with presence dot, media thumbnails,
  voice bars) are composed from those rather than written from scratch.
- **Tokens over literals**: colours come from `Theme.colors`, type from
  `Typography`, spacing from `Spacing`, radii from `Theme.shapes` — the same
  four axes the kit gates in its own CI. A theme change in the kit re-skins the
  app. (Honest footnote: about twenty `Color(0x…)` literals survive in five
  files — mostly chat bubble and collage-avatar tints. They are debt, not
  design; the kit's colour guard does not run on this repo yet.)
- **Runtime**: `PageScaffold` consumes safe area; sheets and toasts go through
  the kit's `OverlayRoot`; nothing here reads `safeAreaInsets` by hand.
- **i18n**: `PrivChatI18nProvider` plugs into the kit's language pipeline. It
  reads `LocalLanguageTag` that the kit's `App` already provides and resolves
  the matching `PrivChatStrings` pack (zh-Hans, zh-Hant, en, vi ship built in).
  Apps set the language **once**, on `App(languageTag = …)`; this provider
  must not be handed a tag of its own. Strings are split into domains so no
  single class hits Android's DEX / 255-parameter limits.

On the `com.tencent.kuikly.compose.*` imports for `Modifier`, layout containers
and animation: those are not a second dependency. GearUI Kit exposes
`com.tencent.kuikly-open:compose` as `api`, so they are part of what "depending
on GearUI Kit" means; privchat-ui declares nothing of KuiklyUI itself.

## Wiring it into an app

The host app composes the three layers like this — this is lifted from a
real app, not invented for the README:

```kotlin
// 1. SDK client is created by the app (device id, server, storage path…)
val client = PrivchatClient(...)
PrivChat.init(client)

// 2. Route SDK events into the runtime store (events carry a string `type`)
fun handleSdkEvent(envelope: SdkEventEnvelope) {
    when (envelope.event.type.lowercase()) {
        "connection_state_changed" ->
            envelope.event.toState?.let { ClientRuntime.onConnectionStateChanged(it) }
        "network_hint_changed" -> {
            val offline = envelope.event.toNetworkHint.equals("offline", ignoreCase = true)
            ClientRuntime.onNetworkReachableChanged(!offline)
        }
        "forced_logout" -> ClientRuntime.onAuthExpired()
        // resume_sync_* / sync_entities_applied / message_send_status_changed / …
    }
}

// 3. Compose root: GearUI App → PrivChat i18n → your pages
App(
    themeMode = settings.themeMode,
    isSystemDark = isSystemDark,
    languageTag = settings.languageTag,       // the ONLY place language is set
) {
    PrivChatI18nProvider {
        ConversationPage(
            onChannelClick = { navigate(it) },
            statusTitle = runtimeTitle,        // derived from ClientRuntime
            statusBusy = runtimeBusy,
        )
    }
}
```

`PrivChatI18nProvider` sits **inside** `App` and outside every page.

## Repository layout

```
src/commonMain/kotlin/com/netonstream/privchat/ui/
├── PrivChat.kt            data façade over the SDK (StateFlows + local UI state)
├── runtime/               ClientRuntime: connectivity / sync / send-queue truth
├── pages/                 one file per screen
├── components/            chat-specific composites (bubbles, actions menu, avatar)
├── models/                display models and small helpers over SDK DTOs
├── state/                 group / approval stores
├── i18n/                  PrivChatStrings domains + language packs (zh-Hans, zh-Hant, en, vi)
├── platform/              expect declarations: clipboard, drafts, links, alerts, system chrome
├── media/  voice/  avatar/  forward/  error/
└── common/                base widgets, cross-platform adapters, utils
src/androidMain, src/iosMain   the actuals for platform/
src/commonTest                 pages / runtime / components / error tests
```

## Building

This repo is consumed as a Gradle **composite build**. `settings.gradle.kts`
substitutes both dependencies for sibling checkouts:

```
com.gearui:gearui-kit           → ../gearui-kit
com.netonstream.privchat:sdk    → ../privchat-sdk-kotlin
```

So the three repositories must sit next to each other. The SDK's Rust FFI is
built by the SDK's own Gradle wiring; you do not build `.so` / `.a` files here.

```bash
./gradlew compileDebugKotlinAndroid          # Android
./gradlew compileKotlinIosSimulatorArm64     # iOS
./gradlew testDebugUnitTest                  # commonTest via the Android unit-test runner
./gradlew iosSimulatorArm64Test              # commonTest as Kotlin/Native on the simulator
```

There is no JVM target, so `commonTest` runs through one of those two.

## Related

- [GearUI Kit](https://github.com/gearui/gearui-kit) — the design system this is built on
- [privchat-sdk-kotlin](https://github.com/privchat/privchat-sdk-kotlin) — the Kotlin SDK binding
