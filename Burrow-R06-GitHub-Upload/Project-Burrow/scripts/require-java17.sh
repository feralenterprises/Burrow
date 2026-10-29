#!/usr/bin/env bash
# Source this script before Java compilation or launching any Burrow Java test.
# BUR-0006: compatibility requires an actual JDK 17 javac + Java 17 runtime.
set -euo pipefail
if [[ -n "${JAVA_HOME:-}" ]]; then
  JAVA_BIN="$JAVA_HOME/bin/java"
  JAVAC_BIN="$JAVA_HOME/bin/javac"
  JAR_BIN="$JAVA_HOME/bin/jar"
else
  JAVA_BIN="$(command -v java || true)"
  JAVAC_BIN="$(command -v javac || true)"
  JAR_BIN="$(command -v jar || true)"
fi
if [[ ! -x "$JAVA_BIN" || ! -x "$JAVAC_BIN" || ! -x "$JAR_BIN" ]]; then
  echo 'BURROW-JAVA17-BLOCKED: java, javac and jar must exist in a JDK 17 installation (set JAVA_HOME).' >&2
  return 17 2>/dev/null || exit 17
fi
JAVA_VERSION="$($JAVA_BIN -version 2>&1 | head -n 1)"
JAVAC_VERSION="$($JAVAC_BIN -version 2>&1 | head -n 1)"
if [[ ! "$JAVA_VERSION" =~ \"17([.\"]|$) || ! "$JAVAC_VERSION" =~ (^|[[:space:]])17([.]|$) ]]; then
  printf '%s\n' 'BURROW-JAVA17-BLOCKED: actual JDK 17 compiler and Java 17 runtime required; --release 17 under a newer JDK is not accepted.' "Detected runtime: $JAVA_VERSION" "Detected compiler: $JAVAC_VERSION" >&2
  return 17 2>/dev/null || exit 17
fi
export JAVA_BIN JAVAC_BIN JAR_BIN
printf 'BURROW-JAVA17-VERIFIED: %s / %s\n' "$JAVA_VERSION" "$JAVAC_VERSION"
