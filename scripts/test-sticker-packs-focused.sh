#!/usr/bin/env bash
# Shared sticker-pack state tests: real cached Compose runtime, no Gradle/app/native build.
# No Gradle, Android Looper, app/native build, device or simulator is used.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1"
OUT="$ROOT/build/sticker-packs-focused"
mkdir -p "$OUT/classes"

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
for module in core compose; do
    source="$(find "$CACHE/com.tencent.kuikly-open/$module-android/2.28.0-2.1.21" -name '*.aar' -print -quit 2>/dev/null)"
    test -n "$source" || { echo "Missing cached Kuikly $module 2.28.0-2.1.21 AAR" >&2; exit 1; }
    unzip -p "$source" classes.jar > "$OUT/kuikly-$module.jar"
    cp+=":$OUT/kuikly-$module.jar"
done

# The Android runtime initializes Looper on a desktop JVM. Use the real desktop
# Compose runtime for snapshot state, not replacement TextFieldState/Buffer stubs.
runtime="$OUT/compose-runtime-desktop-1.7.3.jar"
sha=76e3250547b9ca3cb1a49c272933b25cb3bccbc9ba12775dd6161a63d9aa7cf1
if ! test -f "$runtime"; then
    curl --fail --silent --show-error --location --max-time 60 \
        https://repo.maven.apache.org/maven2/org/jetbrains/compose/runtime/runtime-desktop/1.7.3/runtime-desktop-1.7.3.jar \
        -o "$runtime.download"
    echo "$sha  $runtime.download" | shasum -a 256 -c -
    mv "$runtime.download" "$runtime"
fi
echo "$sha  $runtime" | shasum -a 256 -c -
cp+=":$runtime:$(jar androidx.collection/collection-jvm/1.4.0)"

cp+=":$(jar org.jetbrains.kotlinx/kotlinx-coroutines-test-jvm/1.10.2)"
gear="$ROOT/../gearui-kit/gearui-kit/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar"
test -f "$gear" || { echo "Missing existing compiled GearUI API jar: $gear" >&2; exit 1; }
cp+=":$gear"
BASE="$ROOT/src/commonMain/kotlin/com/netonstream/privchat/ui"
sources=(
    "$BASE/pag/PagContracts.kt"
    "$BASE/sticker/StickerCatalog.kt"
    "$BASE/sticker/StickerPanelState.kt"
    "$BASE/sticker/StickerPanelTestTags.kt"
    "$BASE/i18n/PrivChatStickerPanelStrings.kt"
    "$ROOT/src/commonTest/kotlin/com/netonstream/privchat/ui/sticker/StickerPanelStateTest.kt"
    "$ROOT/src/commonTest/kotlin/com/netonstream/privchat/ui/sticker/StickerPackPanelTest.kt"
)
shasum -a 256 "${sources[@]}" > "$OUT/state-source-hashes.txt"
java -Xmx768m -cp "$compiler" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
    -Xplugin="$(jar org.jetbrains.kotlin/kotlin-compose-compiler-plugin-embeddable/2.1.21)" \
    -no-stdlib -no-reflect -jvm-target 1.8 -classpath "$cp" -d "$OUT/classes" "${sources[@]}"
java -Xmx512m -cp "$OUT/classes:$cp" org.junit.runner.JUnitCore \
    com.netonstream.privchat.ui.sticker.StickerPanelStateTest \
    com.netonstream.privchat.ui.sticker.StickerPackPanelTest | tee "$OUT/test.log"
shasum -a 256 -c "$OUT/state-source-hashes.txt" >/dev/null
