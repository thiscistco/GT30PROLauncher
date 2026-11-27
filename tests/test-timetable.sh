#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -z "${JSON_JAVA_SOURCE:-}" ]]; then echo 'Time Table JVM test skipped (set JSON_JAVA_SOURCE to JSON-java 20180813 source directory)'; exit 0; fi
TC="${TOOLCHAIN:-$HOME/toolchain}"
JAVA="${JAVA_HOME:-$TC/venv/lib/python3.11/site-packages/jdk4py/java-runtime}/bin/java"
ANDROID="${ANDROID_JAR:-$TC/android-platforms/android-35/android.jar}"
TOOLS="${TOOLSJAR:-$TC/npm/dataslope-tools-jar-1.0.0/tools.jar}"
DIR="$(mktemp -d)"
trap 'rm -rf "$DIR"' EXIT
"$JAVA" -Dsun.boot.class.path="$ANDROID" -cp "$TOOLS" com.sun.tools.javac.Main -source 8 -target 8 -bootclasspath "$ANDROID" -d "$DIR" "$JSON_JAVA_SOURCE"/*.java app/src/main/java/com/gt30pro/launcher/TimeTableData.java tests/TimeTableDataTest.java
"$JAVA" -cp "$DIR" com.gt30pro.launcher.TimeTableDataTest
