#!/usr/bin/env bash
# Compile the real Android calendar classes with Android stubs; run pure logic using JSON-java.
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -z "${JSON_JAVA_SOURCE:-}" ]]; then echo 'Calendar event JVM test skipped (set JSON_JAVA_SOURCE)'; exit 0; fi
TC="${TOOLCHAIN:-$HOME/toolchain}"
JAVA="${JAVA_HOME:-$TC/venv/lib/python3.11/site-packages/jdk4py/java-runtime}/bin/java"
ANDROID="${ANDROID_JAR:-$TC/android-platforms/android-35/android.jar}"
TOOLS="${TOOLSJAR:-$TC/npm/dataslope-tools-jar-1.0.0/tools.jar}"
DIR="$(mktemp -d)"; trap 'rm -rf "$DIR"' EXIT
mkdir -p "$DIR/src/java/lang/invoke" "$DIR/stub" "$DIR/classes"
cat > "$DIR/src/java/lang/invoke/LambdaMetafactory.java" <<'STUB'
package java.lang.invoke;
public final class LambdaMetafactory {
 public static CallSite metafactory(MethodHandles.Lookup caller,String invokedName,MethodType invokedType,MethodType samMethodType,MethodHandle implMethod,MethodType instantiatedMethodType) throws LambdaConversionException { throw new UnsupportedOperationException(); }
 public static CallSite altMetafactory(MethodHandles.Lookup caller,String invokedName,MethodType invokedType,Object... args) throws LambdaConversionException { throw new UnsupportedOperationException(); }
}
STUB
JAVAC=("$JAVA" -Dsun.boot.class.path="$DIR/stub:$ANDROID" -cp "$TOOLS" com.sun.tools.javac.Main -source 8 -target 8 -bootclasspath "$DIR/stub:$ANDROID")
"${JAVAC[@]}" -d "$DIR/stub" "$DIR/src/java/lang/invoke/LambdaMetafactory.java"
# A lightweight receiver stub is sufficient for the static logic under test.
mkdir -p "$DIR/src/com/gt30pro/launcher"
echo 'package com.gt30pro.launcher; public class CalendarReminderReceiver extends android.content.BroadcastReceiver { public void onReceive(android.content.Context c, android.content.Intent i) {} }' > "$DIR/src/com/gt30pro/launcher/CalendarReminderReceiver.java"
"${JAVAC[@]}" -d "$DIR/classes" "$JSON_JAVA_SOURCE"/*.java "$DIR/src/com/gt30pro/launcher/CalendarReminderReceiver.java" app/src/main/java/com/gt30pro/launcher/CalendarDates.java app/src/main/java/com/gt30pro/launcher/CalendarRecurrence.java app/src/main/java/com/gt30pro/launcher/CalendarEventLogic.java app/src/main/java/com/gt30pro/launcher/CalendarReminders.java tests/CalendarEventLogicTest.java
"$JAVA" -cp "$DIR/classes:$ANDROID" com.gt30pro.launcher.CalendarEventLogicTest
