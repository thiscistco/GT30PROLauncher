#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TC="${TOOLCHAIN:-$HOME/toolchain}"
JAVA="${JAVA_HOME:-$TC/venv/lib/python3.11/site-packages/jdk4py/java-runtime}/bin/java"
ANDROID="${ANDROID_JAR:-$TC/android-platforms/android-35/android.jar}"
TOOLS="${TOOLSJAR:-$TC/npm/dataslope-tools-jar-1.0.0/tools.jar}"
DIR="$(mktemp -d)"; trap 'rm -rf "$DIR"' EXIT
# Explicit fake Android sources shadow runtime stubs during these JVM-only lifecycle tests.
mkdir -p "$DIR/stub-src/java/lang/invoke" "$DIR/stub" "$DIR/classes"
cat > "$DIR/stub-src/java/lang/invoke/LambdaMetafactory.java" <<'STUB'
package java.lang.invoke;
public final class LambdaMetafactory {
 public static CallSite metafactory(MethodHandles.Lookup c,String n,MethodType t,MethodType s,MethodHandle h,MethodType i) throws LambdaConversionException { throw new UnsupportedOperationException(); }
 public static CallSite altMetafactory(MethodHandles.Lookup c,String n,MethodType t,Object... args) throws LambdaConversionException { throw new UnsupportedOperationException(); }
}
STUB
JAVAC=("$JAVA" -Dsun.boot.class.path="$DIR/stub:$ANDROID" -cp "$TOOLS" com.sun.tools.javac.Main -source 8 -target 8 -bootclasspath "$DIR/stub:$ANDROID")
"${JAVAC[@]}" -d "$DIR/stub" "$DIR/stub-src/java/lang/invoke/LambdaMetafactory.java"
"${JAVAC[@]}" -d "$DIR/classes" $(find tests/voice-stubs -name '*.java') app/src/main/java/com/gt30pro/launcher/VoiceReplies.java tests/VoiceRepliesTest.java
"$JAVA" -cp "$DIR/classes" com.gt30pro.launcher.VoiceRepliesTest
