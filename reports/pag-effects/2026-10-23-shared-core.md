# Shared PAG / Chat Effect Core Handoff

Date: 2026-10-23. Implementation scope: shared UI contracts, keyword admission,
optional composable bridge, native-view wrapper, and inline sticker fallback.
No commits or pushes. No SDKManager, App root/init, MessagePage, settings,
native implementation, backend, build configuration or specification edits by
this task. Concurrent image-runtime and parent integration changes were preserved.

## Parent Call Contract

`com.netonstream.privchat.ui.effect`:

- `ChatEffectSession(platform: String, account: ULong, epoch: Int = 0)`.
- `ChatEffectCandidate(origin, session, channelId, serverMessageId, senderId,
  text, occurredAtMs, plainText = true, forwarded = false, quoted = false)`.
  Channel/message/sender IDs are canonical positive decimal u64 strings.
- `ChatEffectOrigin`: LOCAL_SEND_CONFIRMED, LIVE_RECEIVE, HISTORY,
  RECONNECT_CATCHUP, LOCAL_REPLAY, UNKNOWN. Only the first two are eligible.
- `ChatEffectRule`, `ChatEffectResource`, `ChatEffectSnapshot`, `ChatEffectCatalog`
  are typed inputs; no backend requests or rule hardcoding in shared UI.
- `ChatEffectCoordinator(normalizeNfc, nowEpochMs, nowMonotonicMs,
  runtimeAvailable = { false })` is UI-thread-owned. NFC is required/injected;
  a null result skips matching rather than pretending to normalize.
- `session(ChatEffectSession?)`, `setVisible(channelId, channelKind = DIRECT,
  visible = true, foreground = true)`, `setting(enabled, reducedMotion)`,
  `setForeground(Boolean)`, `replaceRules(snapshot)`, `offer(candidate): Boolean`,
  `onPagEvent(event)`, `tick()`, `cancel(requestId)`, `close()`.
- `active: StateFlow<ChatEffectPlayback?>` is the single current request, not a
  queue. `ChatEffectOverlay(coordinator, modifier)` renders it. Parent guards the
  overlay by exact page channel; parent root binding owns route/foreground.
- `ChatEffectUiBridge.coordinator/catalog` are optional parent-installed inputs.

Call `session` before applying that account's preference, visible channel and
snapshot. Session changes clear rules/current playback/seen caches and reset
enabled=false. SDK provenance adaptation and persisted preferences remain parent
responsibilities. A new catalog snapshot never scans/replays existing messages.

`com.netonstream.privchat.ui.pag`:

- `PagRepresentation(resourceId, url, sha256, size, width, height, durationMs,
  profile = "pag-sticker-v1")` matches the agreed optional sticker wire mapping.
  Effects explicitly use `pag-effect-v1`.
- `PagCapabilities(compiled, initialized, supportedProfiles, health)` defaults
  unavailable. `PagRuntimeUiBridge.host` defaults null. Health must be HEALTHY
  and the exact profile present to admit playback.
- `PagRuntimeUiBridge.animationsEnabled/reducedMotion` are shared optional
  runtime gates, not the account's independent chat-effect preference.
- Remote HTTPS, or explicit HTTP loopback (`localhost`, `127.0.0.1`, `[::1]`),
  with valid optional ports only. No userinfo, encoded authority, fragments,
  malformed port, local/data/script URL, or deceptive loopback suffix. Resolver
  authorization is still required; URL shape checks are not origin permission.
- One shared PAG admission budget, capped at four PAG players and one effect.
  This counts PAG instances, not measured GPU bytes or total cross-codec memory.

`com.netonstream.privchat.app.pag`:

- `installPagRuntime(capabilities)` is callable from parent initialization after
  native registration/capability validation. This task does not call it at root.
- `PagRuntimeHost(request, modifier, onEvent)` hosts `KrPagView` through
  `MakeKuiklyComposeNode`, not a duplicate renderer.
- Single prop `configuration` JSON: `url, sha256, bytes, usage, requestId, loop,
  width, height, durationMs`. Usage is `sticker` or `effect`.
- `onPagEvent` accepts JSON string/native dictionary containing requestId and
  `ready/frame/complete/error/reducedMotion`, optional code. It never parses a
  dictionary's non-JSON `toString()`.
- Disposal invalidates callbacks before clearing native configuration; blank
  configuration plus native node destruction must cancel/release native work.
  Late mismatched request IDs are ignored. Parent/native agents own actual
  parser/hash/download/render/mute/reduce-motion behavior and capability probing.

Parent owns platform expect/actual NFC and capability functions in
`app.platform.effect`; this task did not add competing declarations there.

## Admission And Lifecycle Behavior

- Only explicit confirmed-local sends or live remote receives, exact active
  Platform/account/epoch/channel, ordinary non-forwarded/non-quoted text.
- Live self-echo/other-self-device sends skipped; local confirmations must be
  authored by this account. No history/reconnect inference from timestamps.
- Real NFC callback then trim/full exact matching. No substring, regex,
  automatic case folding, or server text upload. Priority descending then rule
  ID ascending is deterministic. Only FULLSCREEN placement is implemented.
- Five-second eligibility/loading deadline; READY does not count as a first
  frame or restart the deadline. FRAME received after expiry cannot revive it.
- Monotonic page cooldown at least 10 seconds; same-rule cooldown at least
  30 seconds. Remote configuration cannot lower these safeguards.
- Candidates consumed while hidden, disabled, busy or unavailable never queue
  for foreground/resume. Matching dedup is registered before resource admission;
  failure/cancellation does not auto-retry. Bounded 2048-entry short-term caches;
  history filtering remains authoritative after cache expiry/restart.
- Expiry, unknown schema, immutable revision violation, revocation, route/session
  change, runtime loss, user disable, Reduce Motion, error or playback timeout
  remove active playback. Current-request ID scoping prevents stale view cleanup
  from cancelling a newer channel's request.
- Overlay local lifecycle only gates its own lease. It never writes root
  foreground or performs an unscoped outer disposal cancel. No pointer handlers,
  focus handlers, Toasts or message mutations in the decoration. Native views
  must separately guarantee real touch pass-through and transparent composition.

## Sticker Compatibility

`CatalogSticker.pag` defaults null. Existing constructors, fallback image IDs,
type-6 message payloads and image URLs remain usable. `PublishedSticker` is an
optional inline PAG surface over the existing `PublishedStickerImage`; no effect
rules, full-screen display or installation requirement is attached to stickers.
The existing image-runtime implementation was not overwritten.

`StickerPanel` passes the typed catalog item but still sends the original
`StickerMessagePayload(stickerId, imageUrl)`. `MessageContent` resolves optional
PAG only with an exact same-Platform original fallback path and the same returned
immutable sticker ID; foreign/CDN/ambiguous provenance preserves the historical
image. Initial representation lookup does not replace the original fallback URL;
explicit image retry can still use the same verified resource resolver. Failed
or unsupported PAG remains on PNG/WebP without rewriting message state.

Visibility uses Kuikly's ancestor/root-clipped `boundsInWindow`; lifecycle and
capability gates remove native nodes. Fallback remains until a FRAME event, with
a five-second loading timeout. These are source-level wiring facts, not evidence
of real PAG pixels, animation smoothness or surface destruction on a device.

## Executed Focused Tests

```sh
cd privchat-ui
bash scripts/test-pag-effect-focused.sh
```

Actual output after final gate/test changes:

```text
JUnit version 4.13.2
..............
Time: 0.096
OK (14 tests)
```

The script compiles current shared production contracts/coordinator/protocol,
sticker identity policy and actual app event-payload adapter, then executes
assertions. No test stubs for those production sources, no skipped scenarios,
Gradle/app/FFI link, native engine, device or simulator. Toolchain: Kotlin JVM
compiler 2.1.21, Java 17.0.15, coroutines 1.10.2, serialization 1.8.1, JUnit 4.13.2.
Focused test sources live in `src/focusedTest`, because real JVM NFC is used and
there is no new JVM target in the multiplatform root build.

Covered: real Java NFC, exact/non-substring matching, all explicit origins,
self-device and malformed u64 rejection (including >2^53 preservation), exact
session/channel, no-queue resume, deadline and READY-vs-FRAME semantics, late
callbacks, 10s/30s cooldown, deterministic priority/scope, revocation/revision,
runtime loss, Reduce Motion, close, URL attack cases, native JSON keys/event
types, shared budget/no second effect, and historical sticker identity isolation.

`git diff --check` passed for shared UI and scoped app wrapper files. The first
script launch selected Kotlin's metadata `-all.jar`; jar filtering was corrected
before the actual successful 14-test executions. That setup failure is not
counted as a passed test.

## Follow-Up Verification And Review Fixes

Parent follow-up review identified three additional issues, now fixed:

- Page cooldown used a channel-keyed map. It is now one coordinator/account
  admission timestamp, preserved when switching rooms. The new room-switch test
  proves 9999ms refusal / 10000ms admission with another rule, while the original
  rule still observes its independent 30000ms cooldown. Session changes reset it.
- Initial effect canvas maximum is now 2048 per axis to match the conservative
  iOS profile; 2048 is accepted and 2049 rejected. Sticker remains 512 per axis.
- The initial sticker surface used alpha=0 before FRAME. That prevented iOS's
  native visibility gate from starting at all. Alpha hiding was removed: native
  surface remains visible, and the fallback image is composed ABOVE it until
  FRAME confirmation. This fixes the source-level startup interlock; actual
  first-frame pixels still require native runtime evidence.

Final follow-up execution:

```sh
bash scripts/test-pag-effect-focused.sh
# JUnit 4.13.2: OK (15 tests), 0.12 seconds
bash scripts/compile-pag-ui-focused.sh
# PASS: real shared PAG/Effects/Sticker Compose API compile; not a device/rendering test.
```

The second script uses Kotlin/Compose compiler 2.1.21, cached Kuikly 2.28.0 APIs,
Compose runtime Android 1.7.6 and existing SDK/GearUI/UI compiled API jars. It
compiles the actual owned shared bridges/overlay/sticker surfaces and app native
wrapper, including current StickerPanel/MessageContent. Existing image policy /
playback/source dependencies are read but not modified. It does not use substitute
Composable or Kuikly stubs. No blocking compile/API errors; one non-blocking
redundant nullable-condition warning at PublishedSticker.kt:37 remains.

Logs and exact source list/pre/post SHA-256 verification are under
`build/pag-effect-focused/ui-compile.log`, `ui-sources.txt` and
`ui-source-hashes.txt`. This is a dirty-workspace compile against existing API
artifacts, not a commit-pinned full app build. Output footprint approximately
10 MiB, no Gradle/FFI/native renderer/build pipeline started. A transient disk-full
failure creating a test log prevented that attempt from starting; the subsequent
direct-output core invocation executed all 15 tests successfully. No other task's
artifacts were deleted.

## Remaining Integration Evidence

This task did not run a whole app/KMP build or actual native PAG rendering; the
focused Compose API compile above is narrower. Parent integration/native-agent evidence must cover: optional build
on/off, native registration, official libpag parse/render, hash/size checks,
transparent output, mute/audio rejection, touch pass-through, actual Reduce
Motion, teardown/late callbacks, account switching, frame timing, GPU/memory
budget and artifact-size increment. Actual cross-codec/global GPU byte admission
is not proved by the small shared PAG instance counter. Do not mark four-end
runtime or performance acceptance complete using the JVM policy suite alone.

## Files Owned By This Task

- UI `pag/PagContracts.kt`, `PagProtocol.kt`, `PagRuntimeUiBridge.kt`.
- UI `effect/ChatEffectModels.kt`, `ChatEffectCoordinator.kt`,
  `ChatEffectUiBridge.kt`, `ChatEffectOverlay.kt`.
- UI `sticker/PublishedSticker.kt`, `StickerPagIdentity.kt`; narrow edits to
  `StickerCatalog.kt`, `StickerPanel.kt`, `components/MessageContent.kt`.
- UI `src/focusedTest/.../PagEffectFocusedTest.kt`,
  `scripts/test-pag-effect-focused.sh`, `scripts/compile-pag-ui-focused.sh`, this report.
- App common `pag/KrPagView.kt`, `PagRuntimeHost.kt`, `PagEventPayload.kt`.

Other concurrent files, including app `PagRepresentationWire.kt`, platform NFC /
catalog binding, settings, i18n, native hosts and image-runtime changes, are not
claimed as this task's modifications.

## Simulator Crash Follow-Up: Root Geometry And IPC Payload

- Replaced both sticker `boundsInWindow` calls in `PublishedSticker` and
  `PublishedStickerImage` (also consumed by StickerPanel) with the new
  `StickerVisibility.hasVisibleStickerBounds` helper. It checks attachment,
  requests ancestor-clipped bounds in root coordinates and intersects the root
  viewport, without calling Kuikly's unsupported window-coordinate conversion.
  Other concurrent image-runtime changes were preserved.
- App `PagEventPayload` now handles the real Kuikly `JSONObject` with strict
  `opt` value types, including direct fields and nested `data` JSON/object.
  Numeric identifiers/types are not silently coerced into strings.
- Executed `bash scripts/test-pag-effect-focused.sh`: **18 tests passed**.
  Tests use the real cached Kuikly JSONObject and geometry APIs; fake layout
  coordinates deliberately throw on window conversions and verify clipped,
  offscreen, non-finite and detached geometry.
- Executed `bash scripts/compile-pag-ui-focused.sh`: **PASS**, source hashes
  stable; only the existing redundant nullable-condition warning remains.
- No native host edits, simulator automation, full app rebuild or rendering
  acceptance was performed in this follow-up. UI agent must rebuild its existing
  simulator harness before rechecking the reported startup crash and callbacks.

## Transparent Sticker Loading Label Follow-Up

The parent reported four passing real-PAG simulator cases, but its screenshot
showed fallback artwork obscured by a persistent Loading label. This follow-up
only changes `PublishedStickerImage`: Empty/Loading no longer add text above
transparent artwork; actual Error still exposes localized retry and invokes both
the caller retry and painter restart.

Inspected the real cached 2.28.0-2.1.21 `KuiklyPainter` bytecode and local Kuikly
source: Success requires both native loadSuccess and loadResolution; onSuccess
uses the same state publisher, not a separate native completion API. No synthetic
success, timer or framework modification was introduced. The exact cause of the
missing/late state transition remains unproven without callback tracing.

Executed the focused JVM suite again: **18 tests passed**. This suite covers
core/protocol/geometry regression, not the loading label's rendered appearance.
The simulator harness needs a fresh build/screenshot to verify that appearance
and an actual failed URL to exercise retry; no simulator was driven here.
The real Compose/Kuikly API focused compile also passed with stable source
hashes; only the previously noted redundant nullable-condition warning remains.
