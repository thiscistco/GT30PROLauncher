#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TC="${TOOLCHAIN:-$HOME/toolchain}"
JAVA="${JAVA_HOME:-$TC/venv/lib/python3.11/site-packages/jdk4py/java-runtime}/bin/java"
ANDROID="${ANDROID_JAR:-$TC/android-platforms/android-35/android.jar}"
TOOLS="${TOOLSJAR:-$TC/npm/dataslope-tools-jar-1.0.0/tools.jar}"
DIR="$(mktemp -d)"
trap 'rm -rf "$DIR"' EXIT
"$JAVA" -Dsun.boot.class.path="$ANDROID" -cp "$TOOLS" com.sun.tools.javac.Main -source 8 -target 8 -bootclasspath "$ANDROID" -d "$DIR" app/src/main/java/com/gt30pro/launcher/ClockTime.java tests/ClockTimeTest.java
"$JAVA" -cp "$DIR" com.gt30pro.launcher.ClockTimeTest
