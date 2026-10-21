#!/usr/bin/env bash
# Compile only call production sources and pure tests; no app/framework/FFI link.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="$ROOT/../privchat-sdk-kotlin/sdk"
KONAN="${CALL_KONAN_HOME:-$HOME/.konan/kotlin-native-prebuilt-macos-aarch64-2.1.21}"
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1"
OUT="$ROOT/build/call-tests-ios"
mkdir -p "$OUT"
libs=()
for entry in kotlinx-coroutines-core:1.10.2 kotlinx-coroutines-test:1.10.2 kotlinx-serialization-core:1.8.1 kotlinx-serialization-json:1.8.1 atomicfu:0.28.0; do
    artifact="${entry%:*}"; version="${entry#*:}"
    while IFS= read -r file; do libs+=(-library "$file"); done < <(find "$CACHE/org.jetbrains.kotlinx/$artifact-iossimulatorarm64/$version" -name '*.klib')
done
plugin="$(find "$CACHE/org.jetbrains.kotlin/kotlin-serialization-compiler-plugin-embeddable/2.1.21" -name '*.jar' | head -1)"
"$KONAN/bin/kotlinc-native" \
    -target ios_simulator_arm64 -language-version 2.1 -produce library -Xplugin="$plugin" \
    "${libs[@]}" -o "$OUT/call-tests-lib" \
    "$SDK/src/commonMain/kotlin/com/netonstream/privchat/sdk/dto/call/CallModels.kt" \
    "$SDK/src/commonMain/kotlin/com/netonstream/privchat/sdk/dto/call/CallPeerModels.kt" \
    "$SDK/src/commonMain/kotlin/com/netonstream/privchat/sdk/dto/call/CallHistory.kt" \
    "$SDK/src/commonMain/kotlin/com/netonstream/privchat/sdk/dto/SdkEventEnvelope.kt" \
    "$SDK/src/commonMain/kotlin/com/netonstream/privchat/sdk/dto/SyncState.kt" \
    "$SDK/src/commonMain/kotlin/com/netonstream/privchat/sdk/call/CallControl.kt" \
    "$ROOT/src/commonMain/kotlin/com/netonstream/privchat/ui/call/CallContracts.kt" \
    "$ROOT/src/commonMain/kotlin/com/netonstream/privchat/ui/call/DefaultCallCoordinator.kt" \
    "$ROOT/src/commonMain/kotlin/com/netonstream/privchat/ui/call/CallPeerSession.kt" \
    "$ROOT/src/commonMain/kotlin/com/netonstream/privchat/ui/i18n/PrivChatCallStrings.kt" \
    "$ROOT/src/commonTest/kotlin/com/netonstream/privchat/ui/call/CallCoordinatorTest.kt" \
    "$ROOT/src/commonTest/kotlin/com/netonstream/privchat/ui/call/CallHistoryTest.kt" \
    "$SDK/src/commonTest/kotlin/com/netonstream/privchat/sdk/call/CallControlTest.kt"
"$KONAN/bin/kotlinc-native" -target ios_simulator_arm64 -generate-test-runner \
    "${libs[@]}" -Xinclude="$OUT/call-tests-lib.klib" -o "$OUT/call-tests"
xcrun simctl spawn "${SIMULATOR_UDID:-booted}" "$OUT/call-tests.kexe"
