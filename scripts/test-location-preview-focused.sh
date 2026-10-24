#!/usr/bin/env bash
# Real production bridge/state and cached Compose API; no Gradle, app, device or simulator.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1"
OUT="${LOCATION_PREVIEW_TEST_OUT:-$ROOT/build/location-preview-focused}"
mkdir -p "$OUT/classes" "$OUT/ui-classes"
jar() {
    local file
    file=$(find "$CACHE/$1" -name '*.jar' ! -name '*-all.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' -print -quit)
    test -n "$file" || { echo "Missing cached dependency: $1" >&2; exit 1; }
    printf '%s' "$file"
}
aar() {
    local file
    file=$(find "$CACHE/$1" -name '*.aar' -print -quit)
    test -n "$file" || { echo "Missing cached AAR: $1" >&2; exit 1; }
    unzip -p "$file" classes.jar > "$OUT/$2.jar"
    printf '%s' "$OUT/$2.jar"
}
stdlib=$(jar org.jetbrains.kotlin/kotlin-stdlib/2.1.21)
annotations=$(jar org.jetbrains/annotations/13.0)
coroutines=$(jar org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.10.2)
compiler="$(jar org.jetbrains.kotlin/kotlin-compiler-embeddable/2.1.21):$stdlib:$annotations:$coroutines"
compiler+=":$(jar org.jetbrains.kotlin/kotlin-script-runtime/2.1.21):$(jar org.jetbrains.kotlin/kotlin-reflect/1.6.10):$(jar org.jetbrains.intellij.deps/trove4j/1.0.20200330)"
runtime="$OUT/compose-runtime-desktop-1.7.3.jar"
if ! test -f "$runtime"; then
    cached="$ROOT/build/sticker-packs-focused/compose-runtime-desktop-1.7.3.jar"
    if test -f "$cached"; then cp "$cached" "$runtime"
    else
        curl --fail --silent --show-error --location --max-time 60 \
            https://repo.maven.apache.org/maven2/org/jetbrains/compose/runtime/runtime-desktop/1.7.3/runtime-desktop-1.7.3.jar \
            -o "$runtime.download"
        echo "76e3250547b9ca3cb1a49c272933b25cb3bccbc9ba12775dd6161a63d9aa7cf1  $runtime.download" | shasum -a 256 -c -
        mv "$runtime.download" "$runtime"
    fi
fi
echo "76e3250547b9ca3cb1a49c272933b25cb3bccbc9ba12775dd6161a63d9aa7cf1  $runtime" | shasum -a 256 -c -
cp="$stdlib:$annotations:$coroutines:$runtime:$(jar androidx.collection/collection-jvm/1.4.0)"
cp+=":$(jar org.jetbrains.kotlinx/kotlinx-coroutines-test-jvm/1.10.2):$(jar org.jetbrains.kotlin/kotlin-test/2.1.21):$(jar org.jetbrains.kotlin/kotlin-test-junit/2.1.21):$(jar junit/junit/4.13.2):$(jar org.hamcrest/hamcrest-core/1.3)"
BASE="$ROOT/src/commonMain/kotlin/com/netonstream/privchat/ui"
sources=("$BASE/platform/LocationUiBridge.kt" "$BASE/platform/LocationPreviewBridge.kt"
    "$BASE/platform/LocationPreviewState.kt" "$BASE/components/MessageContent.kt"
    "$ROOT/src/androidUnitTest/kotlin/com/netonstream/privchat/ui/platform/LocationPreviewBridgeTest.kt"
    "$ROOT/src/androidUnitTest/kotlin/com/netonstream/privchat/ui/platform/LocationPreviewStateTest.kt")
shasum -a 256 "${sources[@]}" "$ROOT/scripts/test-location-preview-focused.sh" > "$OUT/source-manifest.sha256"
java -Xmx512m -cp "$compiler" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
    -no-stdlib -no-reflect -jvm-target 1.8 -classpath "$cp" -d "$OUT/classes" \
    "${sources[0]}" "${sources[1]}" "${sources[2]}" "${sources[4]}" "${sources[5]}"
java -Xmx256m -cp "$OUT/classes:$cp" org.junit.runner.JUnitCore \
    com.netonstream.privchat.ui.platform.LocationPreviewBridgeTest \
    com.netonstream.privchat.ui.platform.LocationPreviewStateTest | tee "$OUT/test.log"

# Typecheck the actual card integration, not replacement UI functions or state stubs.
ui="$ROOT/build/intermediates/compile_library_classes_jar/debug/bundleLibCompileToJarDebug/classes.jar"
gear="$ROOT/../gearui-kit/gearui-kit/build/intermediates/compile_library_classes_jar/debug/bundleLibCompileToJarDebug/classes.jar"
sdk="$ROOT/../privchat-sdk-kotlin/sdk/build/intermediates/compile_library_classes_jar/debug/bundleLibCompileToJarDebug/classes.jar"
for file in "$ui" "$gear" "$sdk"; do test -f "$file"; done
api="$stdlib:$annotations:$coroutines:$ui:$gear:$sdk:${ANDROID_HOME:-$HOME/Library/Android/sdk}/platforms/android-36/android.jar"
api+=":$(aar androidx.compose.runtime/runtime-android/1.7.6 compose-runtime)"
api+=":$(aar com.tencent.kuikly-open/compose-android/2.28.0-2.1.21 kuikly-compose)"
api+=":$(aar com.tencent.kuikly-open/core-android/2.28.0-2.1.21 kuikly-core)"
api+=":$(aar com.tencent.kuikly-open/core-render-android/2.28.0-2.1.21 kuikly-renderer)"
api+=":$(jar org.jetbrains.kotlinx/kotlinx-serialization-json-jvm/1.8.1):$(jar org.jetbrains.kotlinx/kotlinx-serialization-core-jvm/1.8.1)"
java -Xmx768m -cp "$compiler" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
    -no-stdlib -no-reflect -jvm-target 1.8 -Xfriend-paths="$ui" \
    -Xplugin="$(jar org.jetbrains.kotlin/kotlin-compose-compiler-plugin-embeddable/2.1.21)" \
    -classpath "$api" -d "$OUT/ui-classes" "${sources[0]}" "${sources[1]}" "${sources[2]}" "${sources[3]}"
shasum -a 256 -c "$OUT/source-manifest.sha256" > "$OUT/source-verification.log"
echo 'PASS: focused preview recovery and real MessageContent Compose API compile; not native acceptance.'
