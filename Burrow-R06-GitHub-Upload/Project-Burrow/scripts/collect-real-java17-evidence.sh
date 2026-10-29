#!/usr/bin/env bash
# BUR-0007: RUN ONLY ON AUTHORIZED JDK17 HOST WITH REAL RUNELITE REPOSITORY ACCESS.
# No synthetic API stubs, game/client processes, accounts, or bank reads.
set -Eeuo pipefail
cd "$(dirname "$0")/.."
ROOT=$(pwd -P)
EVIDENCE_DIR="${BURROW_EVIDENCE_DIR:-$ROOT/../BUR-0007-JDK17-REAL-BUILD-EVIDENCE}"
mkdir -p "$EVIDENCE_DIR"
{
    printf 'UTC: '; date -u +'%Y-%m-%dT%H:%M:%SZ'
    printf 'PROJECT: %s\n' "$ROOT"
    printf 'JAVA_HOME: %s\n' "${JAVA_HOME:-UNSET}"
    printf 'No client launch, plugin install or real bank read requested.\n'
} > "$EVIDENCE_DIR/environment.txt"
# This checks the real java, javac and jar executables. 21 --release 17 never passes.
set +e
bash scripts/require-java17.sh >"$EVIDENCE_DIR/jdk17-gate.log" 2>&1
GATE_EXIT=$?
set -e
printf '%s\n' "$GATE_EXIT" > "$EVIDENCE_DIR/jdk17-gate.exit"
if [[ "$GATE_EXIT" -ne 0 ]]; then
    printf 'BUR-0007 BLOCKED: actual JDK17 gate returned %s\n' "$GATE_EXIT" >&2
    exit "$GATE_EXIT"
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
    "$JAVA_HOME/bin/java" -version >"$EVIDENCE_DIR/java17-version.log" 2>&1
    "$JAVA_HOME/bin/javac" -version >"$EVIDENCE_DIR/javac17-version.log" 2>&1
else
    java -version >"$EVIDENCE_DIR/java17-version.log" 2>&1
    javac -version >"$EVIDENCE_DIR/javac17-version.log" 2>&1
fi
if [[ -x ./gradlew ]]; then
    GRADLE=(./gradlew)
elif command -v gradle >/dev/null 2>&1; then
    GRADLE=(gradle)
else
    echo 'BUR-0007 BLOCKED: real Gradle distribution/wrapper unavailable' | tee "$EVIDENCE_DIR/blocker.txt" >&2
    exit 18
fi
set +e
"${GRADLE[@]}" --version >"$EVIDENCE_DIR/gradle-version.log" 2>&1
GV=$?
set -e
printf '%s\n' "$GV" >"$EVIDENCE_DIR/gradle-version.exit"
if [[ "$GV" -ne 0 ]] || ! grep -Eq '^[[:space:]]*(Launcher JVM|JVM):[[:space:]]*17([.]|[[:space:]]|$)' "$EVIDENCE_DIR/gradle-version.log"; then
    echo 'BUR-0007 BLOCKED: Gradle JVM must actually be 17' | tee "$EVIDENCE_DIR/blocker.txt" >&2
    exit 17
fi
set +e
bash scripts/build-offline.sh >"$EVIDENCE_DIR/offline-java17.log" 2>&1
O=$?
set -e
printf '%s\n' "$O" >"$EVIDENCE_DIR/offline-java17.exit"
if [[ "$O" -ne 0 ]]; then echo "BUR-0007 BLOCKED: offline JDK17 build failed: $O" >&2; exit "$O"; fi
set +e
"${GRADLE[@]}" --no-daemon --stacktrace --info clean test jar >"$EVIDENCE_DIR/real-gradle-build.log" 2>&1
B=$?
set -e
printf '%s\n' "$B" >"$EVIDENCE_DIR/real-gradle-build.exit"
if [[ "$B" -ne 0 ]]; then echo "BUR-0007 BLOCKED: genuine Gradle/RuneLite build failed: $B" >&2; exit "$B"; fi
"${GRADLE[@]}" --no-daemon dependencyInsight --dependency net.runelite:client --configuration compileClasspath >"$EVIDENCE_DIR/runelite-resolved-dependency.log" 2>&1 || :
if [[ ! -d build/libs ]] || [[ -z "$(find build/libs -maxdepth 1 -name '*.jar' -print -quit)" ]]; then
    echo 'BUR-0007 BLOCKED: Gradle returned success without a developer JAR' | tee "$EVIDENCE_DIR/blocker.txt" >&2
    exit 20
fi
mkdir -p "$EVIDENCE_DIR/compiled-jars"
cp build/libs/*.jar "$EVIDENCE_DIR/compiled-jars/"
(cd "$EVIDENCE_DIR/compiled-jars" && sha256sum ./*.jar) >"$EVIDENCE_DIR/jar-sha256s.txt"
if [[ -d build/test-results/test ]]; then cp -a build/test-results/test "$EVIDENCE_DIR/junit-xml"; fi
printf 'GRADLE_COMPLETED_ON_JDK17__RUNE_LITE_CLIENT_NOT_STARTED__QA_AND_RELEASE_PENDING\n' > "$EVIDENCE_DIR/RESULT.txt"
echo "BUR-0007: JDK17 real Gradle build tasks PASS. Not native QA or release. Evidence: $EVIDENCE_DIR"
