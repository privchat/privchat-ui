#!/usr/bin/env bash
# Real shared gate/protocol tests on the JVM; no Gradle, app, FFI, device or simulator.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP="$ROOT/../privchat-app/privchat/src/commonMain/kotlin/com/netonstream/privchat/app/pag"
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1"
OUT="$ROOT/build/pag-effect-focused"
jar() { find "$CACHE/$1" -name '*.jar' ! -name '*-all.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -1; }
stdlib="$(jar org.jetbrains.kotlin/kotlin-stdlib/2.1.21)"
annotations="$(jar org.jetbrains/annotations/13.0)"
coroutines="$(jar org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.10.2)"
json="$(jar org.jetbrains.kotlinx/kotlinx-serialization-json-jvm/1.8.1)"
serialization="$(jar org.jetbrains.kotlinx/kotlinx-serialization-core-jvm/1.8.1)"
testlib="$(jar org.jetbrains.kotlin/kotlin-test/2.1.21)"
testjunit="$(jar org.jetbrains.kotlin/kotlin-test-junit/2.1.21)"
junit="$(jar junit/junit/4.13.2)"
hamcrest="$(jar org.hamcrest/hamcrest-core/1.3)"
compiler="$(jar org.jetbrains.kotlin/kotlin-compiler-embeddable/2.1.21):$stdlib:$annotations:$coroutines"
compiler+=":$(jar org.jetbrains.kotlin/kotlin-script-runtime/2.1.21):$(jar org.jetbrains.kotlin/kotlin-reflect/1.6.10):$(jar org.jetbrains.intellij.deps/trove4j/1.0.20200330)"
cp="$stdlib:$annotations:$coroutines:$json:$serialization:$testlib:$testjunit:$junit:$hamcrest"
mkdir -p "$OUT/classes"
for module in core compose; do
  source="$(find "$CACHE/com.tencent.kuikly-open/$module-android/2.28.0-2.1.21" -name '*.aar' -print -quit)"
  test -n "$source"
  unzip -p "$source" classes.jar > "$OUT/focused-$module.jar"
  cp+=":$OUT/focused-$module.jar"
done
BASE="$ROOT/src/commonMain/kotlin/com/netonstream/privchat/ui"
java -Xmx768m -cp "$compiler" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -no-stdlib -no-reflect -jvm-target 1.8 -classpath "$cp" -d "$OUT/classes" \
  "$BASE/pag/PagContracts.kt" "$BASE/pag/PagProtocol.kt" \
  "$BASE/effect/ChatEffectModels.kt" "$BASE/effect/ChatEffectCoordinator.kt" \
  "$BASE/sticker/StickerPagIdentity.kt" "$BASE/sticker/StickerVisibility.kt" "$APP/PagEventPayload.kt" \
  "$ROOT/src/focusedTest/kotlin/com/netonstream/privchat/ui/effect/"*.kt
java -Xmx512m -cp "$OUT/classes:$cp" org.junit.runner.JUnitCore com.netonstream.privchat.ui.effect.PagEffectFocusedTest com.netonstream.privchat.ui.effect.StickerVisibilityFocusedTest
