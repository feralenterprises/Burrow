#!/usr/bin/env bash
# Real dependency build only: no synthetic stub code allowed in the classpath.
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/require-java17.sh
if [[ -x ./gradlew ]]; then
  GRADLE=(./gradlew)
elif command -v gradle >/dev/null 2>&1; then
  GRADLE=(gradle)
else
  echo 'BURROW-REAL-HOST-BLOCKED: real Gradle distribution/wrapper unavailable; cannot resolve RuneLite client artifacts.' >&2
  exit 18
fi
"${GRADLE[@]}" --no-daemon --stacktrace clean test jar
