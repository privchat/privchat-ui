#!/usr/bin/env bash
# Reproduce 19 tests: 6 catalog + 5 boundary + 8 real Kuikly editor regressions.
# No Gradle, Android Looper, app/native build, device or simulator is used.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1"
OUT="$ROOT/build/apple-emoji-focused"
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

BASE="$ROOT/src/commonMain/kotlin/com/netonstream/privchat/ui"
sources=(
    "$BASE/i18n/PrivChatEmojiStrings.kt"
    "$BASE/emoji/AppleEmojiEditorController.kt"
    "$BASE/emoji/AppleEmojiCategories.kt"
    "$BASE/emoji/AppleEmojiCatalog.kt"
    "$BASE/emoji/AppleEmojiCatalogData.kt"
    "$BASE/emoji/EmojiTextEdit.kt"
    "$BASE/emoji/AppleEmojiTestTags.kt"
    "$ROOT/src/commonTest/kotlin/com/netonstream/privchat/ui/emoji/AppleEmojiTest.kt"
    "$ROOT/src/commonTest/kotlin/com/netonstream/privchat/ui/emoji/EmojiTextEditBoundaryTest.kt"
    "$ROOT/src/focusedTest/kotlin/com/netonstream/privchat/ui/emoji/AppleEmojiEditorFocusedTest.kt"
)
shasum -a 256 "${sources[@]}" > "$OUT/source-hashes.txt"
java -Xmx768m -cp "$compiler" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
    -Xplugin="$(jar org.jetbrains.kotlin/kotlin-compose-compiler-plugin-embeddable/2.1.21)" \
    -no-stdlib -no-reflect -jvm-target 1.8 -classpath "$cp" -d "$OUT/classes" "${sources[@]}"
java -Xmx512m -cp "$OUT/classes:$cp" org.junit.runner.JUnitCore \
    com.netonstream.privchat.ui.emoji.AppleEmojiTest \
    com.netonstream.privchat.ui.emoji.EmojiTextEditBoundaryTest \
    com.netonstream.privchat.ui.emoji.AppleEmojiEditorFocusedTest | tee "$OUT/test.log"
shasum -a 256 -c "$OUT/source-hashes.txt" >/dev/null
