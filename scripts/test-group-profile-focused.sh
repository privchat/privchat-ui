#!/usr/bin/env bash
# Real shared production state/policy tests; optional real-API Compose compile.
# No Gradle, app/native build, device, simulator, SDK transport or DB is started.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1"
OUT="$ROOT/build/group-profile-focused"
mkdir -p "$OUT/classes" "$OUT/ui-classes"
jar() {
    local result
    result="$(find "$CACHE/$1" -name '*.jar' ! -name '*-all.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' -print -quit 2>/dev/null)"
    test -n "$result" || { echo "Missing cached dependency: $1" >&2; exit 1; }
    printf '%s' "$result"
}
stdlib="$(jar org.jetbrains.kotlin/kotlin-stdlib/2.1.21)"
annotations="$(jar org.jetbrains/annotations/13.0)"
coroutines="$(jar org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.10.2)"
compiler="$(jar org.jetbrains.kotlin/kotlin-compiler-embeddable/2.1.21):$stdlib:$annotations:$coroutines"
compiler+=":$(jar org.jetbrains.kotlin/kotlin-script-runtime/2.1.21):$(jar org.jetbrains.kotlin/kotlin-reflect/1.6.10):$(jar org.jetbrains.intellij.deps/trove4j/1.0.20200330)"
cp="$stdlib:$annotations:$coroutines:$(jar org.jetbrains.kotlin/kotlin-test/2.1.21):$(jar org.jetbrains.kotlin/kotlin-test-junit/2.1.21):$(jar junit/junit/4.13.2):$(jar org.hamcrest/hamcrest-core/1.3)"
cp+=":$(jar org.jetbrains.kotlinx/kotlinx-coroutines-test-jvm/1.10.2):$(jar org.jetbrains.kotlinx/kotlinx-serialization-json-jvm/1.8.1):$(jar org.jetbrains.kotlinx/kotlinx-serialization-core-jvm/1.8.1)"
for module in core compose core-render; do
    source="$(find "$CACHE/com.tencent.kuikly-open/$module-android/2.28.0-2.1.21" -name '*.aar' -print -quit 2>/dev/null)"
    test -n "$source" || { echo "Missing cached Kuikly $module AAR" >&2; exit 1; }
    unzip -p "$source" classes.jar > "$OUT/kuikly-$module.jar"
    cp+=":$OUT/kuikly-$module.jar"
done
runtime="$OUT/compose-runtime-desktop-1.7.3.jar"
sha=76e3250547b9ca3cb1a49c272933b25cb3bccbc9ba12775dd6161a63d9aa7cf1
if ! test -f "$runtime"; then
    for cached in "$ROOT/build/apple-emoji-focused/compose-runtime-desktop-1.7.3.jar" "$ROOT/build/sticker-packs-focused/compose-runtime-desktop-1.7.3.jar"; do
        if test -f "$cached"; then cp "$cached" "$runtime"; break; fi
    done
fi
if ! test -f "$runtime"; then
    curl --fail --silent --show-error --location --max-time 60 \
        https://repo.maven.apache.org/maven2/org/jetbrains/compose/runtime/runtime-desktop/1.7.3/runtime-desktop-1.7.3.jar \
        -o "$runtime.download"
    echo "$sha  $runtime.download" | shasum -a 256 -c -
    mv "$runtime.download" "$runtime"
fi
echo "$sha  $runtime" | shasum -a 256 -c -
cp+=":$runtime:$(jar androidx.collection/collection-jvm/1.4.0)"
ui="$ROOT/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar"
gear="$ROOT/../gearui-kit/gearui-kit/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar"
sdk="$ROOT/../privchat-sdk-kotlin/sdk/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar"
for required in "$ui" "$gear" "$sdk"; do
    test -f "$required" || { echo "Missing existing compiled real API jar: $required" >&2; exit 1; }
done
cp+=":$ui:$gear:$sdk"
BASE="$ROOT/src/commonMain/kotlin/com/netonstream/privchat/ui"
sources=("$BASE"/group/*.kt "$BASE/components/MessageActionPolicy.kt")
if [[ "${1:-test}" == compile ]]; then
    cp+=":${ANDROID_HOME:-$HOME/Library/Android/sdk}/platforms/android-36/android.jar"
    sources+=("$BASE/pages/MessagePage.kt" "$BASE/pages/ChatSettingsPage.kt" "$BASE/pages/GroupNameEditPage.kt")
    # Existing MessagePage dependencies are source-only in this dirty workspace;
    # compile them unchanged, rather than pretending the older API jar contains them.
    sources+=("$BASE"/emoji/*.kt "$BASE/i18n/PrivChatEmojiStrings.kt" "$BASE/sticker/StickerPanelTestTags.kt")
    output="$OUT/ui-classes"
else
    sources+=("$ROOT/src/commonTest/kotlin/com/netonstream/privchat/ui/group/GroupProfileTest.kt")
    output="$OUT/classes"
fi
shasum -a 256 "${sources[@]}" > "$OUT/${1:-test}-sources.sha256"
java -Xmx1024m -cp "$compiler" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
    -Xplugin="$(jar org.jetbrains.kotlin/kotlin-compose-compiler-plugin-embeddable/2.1.21)" \
    -Xfriend-paths="$ui" -no-stdlib -no-reflect -jvm-target 1.8 \
    -classpath "$cp" -d "$output" "${sources[@]}"
if [[ "${1:-test}" == compile ]]; then
    echo 'PASS: real-API production pin/group Compose compile (not rendering).'
else
    java -Xmx512m -cp "$output:$cp" org.junit.runner.JUnitCore \
        com.netonstream.privchat.ui.group.GroupProfileTest \
        com.netonstream.privchat.ui.group.GroupDescriptionEditorTest \
        com.netonstream.privchat.ui.group.GroupPinsStateTest | tee "$OUT/test.log"
fi
shasum -a 256 -c "$OUT/${1:-test}-sources.sha256" >/dev/null
