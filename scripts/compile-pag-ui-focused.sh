#!/usr/bin/env bash
# Compile real owned Compose surfaces against cached real APIs, without Gradle/app/FFI builds.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1"
OUT="$ROOT/build/pag-effect-focused"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
jar() { find "$CACHE/$1" -name '*.jar' ! -name '*-all.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' -print -quit; }
aar() {
    local source
    source="$(find "$CACHE/$1" -name '*.aar' -print -quit)"
    test -n "$source" || { echo "Missing cached AAR: $1" >&2; exit 1; }
    unzip -p "$source" classes.jar > "$OUT/$2.jar"
    printf '%s' "$OUT/$2.jar"
}
mkdir -p "$OUT/ui-classes"
stdlib="$(jar org.jetbrains.kotlin/kotlin-stdlib/2.1.21)"
annotations="$(jar org.jetbrains/annotations/13.0)"
coroutines="$(jar org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.10.2)"
compiler="$(jar org.jetbrains.kotlin/kotlin-compiler-embeddable/2.1.21):$stdlib:$annotations:$coroutines"
compiler+=":$(jar org.jetbrains.kotlin/kotlin-script-runtime/2.1.21):$(jar org.jetbrains.kotlin/kotlin-reflect/1.6.10):$(jar org.jetbrains.intellij.deps/trove4j/1.0.20200330)"
runtime="$(aar androidx.compose.runtime/runtime-android/1.7.6 compose-runtime)"
compose="$(aar com.tencent.kuikly-open/compose-android/2.28.0-2.1.21 kuikly-compose)"
core="$(aar com.tencent.kuikly-open/core-android/2.28.0-2.1.21 kuikly-core)"
renderer="$(aar com.tencent.kuikly-open/core-render-android/2.28.0-2.1.21 kuikly-renderer)"
ui="$ROOT/build/intermediates/compile_library_classes_jar/debug/bundleLibCompileToJarDebug/classes.jar"
gear="$ROOT/../gearui-kit/gearui-kit/build/intermediates/compile_library_classes_jar/debug/bundleLibCompileToJarDebug/classes.jar"
sdk="$ROOT/../privchat-sdk-kotlin/sdk/build/intermediates/compile_library_classes_jar/debug/bundleLibCompileToJarDebug/classes.jar"
for required in "$ui" "$gear" "$sdk"; do test -f "$required" || { echo "Missing existing compiled API dependency: $required" >&2; exit 1; }; done
cp="$stdlib:$annotations:$coroutines:$runtime:$compose:$core:$renderer:$ui:$gear:$sdk:$SDK/platforms/android-36/android.jar"
cp+=":$(jar org.jetbrains.kotlinx/kotlinx-serialization-json-jvm/1.8.1):$(jar org.jetbrains.kotlinx/kotlinx-serialization-core-jvm/1.8.1)"
BASE="$ROOT/src/commonMain/kotlin/com/netonstream/privchat/ui"
APP="$ROOT/../privchat-app/privchat/src/commonMain/kotlin/com/netonstream/privchat/app/pag"
sources=("$BASE"/pag/*.kt "$BASE"/effect/*.kt
    "$BASE/sticker/StickerCatalog.kt" "$BASE/sticker/PublishedSticker.kt" "$BASE/sticker/StickerPagIdentity.kt"
    "$BASE/sticker/PublishedStickerImage.kt" "$BASE/sticker/StickerPlaybackSlots.kt" "$BASE/sticker/StickerImagePolicy.kt" "$BASE/sticker/StickerVisibility.kt"
    "$BASE/sticker/StickerPanelState.kt" "$BASE/sticker/StickerPanel.kt" "$BASE/components/MessageContent.kt"
    "$APP/KrPagView.kt" "$APP/PagRuntimeHost.kt" "$APP/PagEventPayload.kt")
printf '%s\n' "${sources[@]}" > "$OUT/ui-sources.txt"
shasum -a 256 "${sources[@]}" > "$OUT/ui-source-hashes.txt"
java -Xmx1024m -cp "$compiler" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
    -no-stdlib -no-reflect -jvm-target 1.8 -Xfriend-paths="$ui" \
    -Xplugin="$(jar org.jetbrains.kotlin/kotlin-compose-compiler-plugin-embeddable/2.1.21)" \
    -classpath "$cp" -d "$OUT/ui-classes" "${sources[@]}"
shasum -a 256 -c "$OUT/ui-source-hashes.txt" >/dev/null
echo "PASS: real shared PAG/Effects/Sticker Compose API compile; not a device/rendering test."
