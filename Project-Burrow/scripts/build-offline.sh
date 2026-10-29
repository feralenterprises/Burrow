#!/usr/bin/env bash
# BUR-0008 R06: Java 17-only, self-contained tests that do not depend on JUnit.
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/require-java17.sh
mkdir -p build/offline-main build/offline-test build/previews
find src/main/java -name '*.java' ! -path '*/runelite/*' | sort > build/MAIN-SOURCES.txt
# Enumerate actual dependency-free entry points. Naming patterns are unsafe:
# NavigationLifecycleTest.java imports JUnit even though it lacks 'JUnit' in its name.
TEST_SOURCES=(
  src/test/java/gg/feral/burrow/core/BurrowCoreTest.java
  src/test/java/gg/feral/burrow/core/BurrowP1RegressionTest.java
  src/test/java/gg/feral/burrow/ui/BurrowHitboxTest.java
)
for src in "${TEST_SOURCES[@]}"; do
  if [[ ! -f "$src" ]]; then
    printf 'BURROW-OFFLINE-BLOCKED: required regression source missing: %s\n' "$src" >&2
    exit 19
  fi
done
printf '%s\n' "${TEST_SOURCES[@]}" > build/TEST-SOURCES.txt
"$JAVAC_BIN" --release 17 -encoding UTF-8 -d build/offline-main @build/MAIN-SOURCES.txt
"$JAVAC_BIN" --release 17 -encoding UTF-8 -cp build/offline-main -d build/offline-test @build/TEST-SOURCES.txt
: > build/offline-test-results.txt
for test_class in \
  gg.feral.burrow.core.BurrowCoreTest \
  gg.feral.burrow.core.BurrowP1RegressionTest \
  gg.feral.burrow.ui.BurrowHitboxTest; do
  "$JAVA_BIN" -ea -Djava.awt.headless=true -cp build/offline-main:build/offline-test "$test_class" | tee -a build/offline-test-results.txt
done
"$JAVA_BIN" -Djava.awt.headless=true -cp build/offline-main gg.feral.burrow.preview.RenderPreview build/previews
"$JAR_BIN" --create --file build/BURROW-OFFLINE-PREVIEW.jar --main-class gg.feral.burrow.preview.BurrowStandalone -C build/offline-main .
echo 'Offline preview: actual Java 17 only; NOT a RuneLite plugin JAR.'
