#!/usr/bin/env bash
# Real Compose row identity regression. Native hit-test geometry requires separate App UI evidence.
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
out=${CONVERSATION_COMPOSE_TEST_OUT:-"$root/build/conversation-compose-identity"}
mkdir -p "$out/deps" "$out/classes"
cache="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1"
jar() {
  local group=$1 artifact=$2 version=$3 found="" target
  if [[ -d "$cache/$group/$artifact/$version" ]]; then
    found=$(find "$cache/$group/$artifact/$version" -name "$artifact-$version.jar" -print -quit)
  fi
  if [[ -n "$found" ]]; then printf '%s' "$found"; return; fi
  target="$out/deps/$artifact-$version.jar"
  if [[ ! -s "$target" ]]; then
    curl --fail --location --silent --show-error \
      "https://repo.maven.apache.org/maven2/${group//.//}/$artifact/$version/$artifact-$version.jar" \
      -o "$target.partial"
    mv "$target.partial" "$target"
  fi
  printf '%s' "$target"
}
grep -Fq 'items(filteredChannels.size, key = { filteredChannels[it].channelId.toString() })' "$root/src/commonMain/kotlin/com/netonstream/privchat/ui/pages/ConversationPage.kt"
stdlib=$(jar org.jetbrains.kotlin kotlin-stdlib 2.1.21)
annotations=$(jar org.jetbrains annotations 13.0)
coroutines=$(jar org.jetbrains.kotlinx kotlinx-coroutines-core-jvm 1.10.2)
compiler="$(jar org.jetbrains.kotlin kotlin-compiler-embeddable 2.1.21):$stdlib:$annotations:$coroutines:$(jar org.jetbrains.kotlin kotlin-script-runtime 2.1.21):$(jar org.jetbrains.kotlin kotlin-reflect 1.6.10):$(jar org.jetbrains.intellij.deps trove4j 1.0.20200330)"
classpath="$stdlib:$annotations:$coroutines:$(jar org.jetbrains.compose.runtime runtime-desktop 1.7.3):$(jar androidx.collection collection-jvm 1.4.0):$(jar org.jetbrains.kotlinx atomicfu-jvm 0.25.0):$(jar org.jetbrains.kotlin kotlin-test 2.1.21):$(jar org.jetbrains.kotlin kotlin-test-junit 2.1.21):$(jar junit junit 4.13.2):$(jar org.hamcrest hamcrest-core 1.3)"
java -Xmx512m -cp "$compiler" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -no-stdlib -no-reflect -jvm-target 17 \
  -Xplugin="$(jar org.jetbrains.kotlin kotlin-compose-compiler-plugin-embeddable 2.1.21)" \
  -classpath "$classpath" -d "$out/classes" \
  "$root/src/composeBoundaryTest/kotlin/com/netonstream/privchat/ui/pages/ConversationRowIdentityTest.kt"
java -Xmx256m -cp "$out/classes:$classpath" org.junit.runner.JUnitCore \
  com.netonstream.privchat.ui.pages.ConversationRowIdentityTest
