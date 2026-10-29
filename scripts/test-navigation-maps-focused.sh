#!/usr/bin/env bash
# Cached compiler only: no Gradle, downloads, application build or device operations.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1"
OUT="${NAVIGATION_MAP_TEST_OUT:-$ROOT/build/navigation-map-focused}"
mkdir -p "$OUT/classes"
jar() {
    local file
    file=$(find "$CACHE/$1" -name '*.jar' ! -name '*-all.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' -print -quit)
    test -n "$file" || { echo "Missing cached dependency: $1" >&2; exit 1; }
    printf '%s' "$file"
}
stdlib=$(jar org.jetbrains.kotlin/kotlin-stdlib/2.1.21)
annotations=$(jar org.jetbrains/annotations/13.0)
compiler="$(jar org.jetbrains.kotlin/kotlin-compiler-embeddable/2.1.21):$stdlib:$annotations"
compiler+=":$(jar org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.10.2)"
compiler+=":$(jar org.jetbrains.kotlin/kotlin-script-runtime/2.1.21):$(jar org.jetbrains.kotlin/kotlin-reflect/1.6.10):$(jar org.jetbrains.intellij.deps/trove4j/1.0.20200330)"
cp="$stdlib:$annotations:${ANDROID_HOME:-$HOME/Library/Android/sdk}/platforms/android-36/android.jar"
cp+=":$(jar org.jetbrains.kotlin/kotlin-test/2.1.21):$(jar org.jetbrains.kotlin/kotlin-test-junit/2.1.21)"
cp+=":$(jar junit/junit/4.13.2):$(jar org.hamcrest/hamcrest-core/1.3)"
BASE="$ROOT/src/commonMain/kotlin/com/netonstream/privchat/ui"
common=("$BASE/platform/ExternalLinkBridge.kt" "$BASE/platform/NavigationMapUrls.kt" "$BASE/utils/CoordinateConverter.kt")
actual="$ROOT/src/androidMain/kotlin/com/netonstream/privchat/ui/platform/ExternalLinkBridge.android.kt"
tests="$ROOT/src/androidUnitTest/kotlin/com/netonstream/privchat/ui/platform/NavigationMapUrlsTest.kt"
shasum -a 256 "${common[@]}" "$actual" "$tests" "$ROOT/scripts/test-navigation-maps-focused.sh" > "$OUT/source-manifest.sha256"
common_csv=$(IFS=,; echo "${common[*]}")
java -Xmx512m -cp "$compiler" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
    -no-stdlib -no-reflect -jvm-target 1.8 -Xmulti-platform -Xexpect-actual-classes \
    -Xcommon-sources="$common_csv" -classpath "$cp" -d "$OUT/classes" \
    "${common[@]}" "$actual" "$tests"
java -Xmx128m -cp "$OUT/classes:$cp" org.junit.runner.JUnitCore \
    com.netonstream.privchat.ui.platform.NavigationMapUrlsTest | tee "$OUT/test.log"
