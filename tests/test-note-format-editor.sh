#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -z "${JSON_JAVA_SOURCE:-}" ]]; then echo 'Note format editor test skipped (set JSON_JAVA_SOURCE)'; exit 0; fi
TC="${TOOLCHAIN:-$HOME/toolchain}"; JAVA="${JAVA_HOME:-$TC/venv/lib/python3.11/site-packages/jdk4py/java-runtime}/bin/java"; ANDROID="${ANDROID_JAR:-$TC/android-platforms/android-35/android.jar}"; TOOLS="${TOOLSJAR:-$TC/npm/dataslope-tools-jar-1.0.0/tools.jar}"
DIR="$(mktemp -d)"; trap 'rm -rf "$DIR"' EXIT
mkdir -p "$DIR/src/android/graphics" "$DIR/src/android/text/style" "$DIR/src/android/text"
cat > "$DIR/src/android/graphics/Typeface.java" <<'STUB'
package android.graphics; public class Typeface {public static final int BOLD=1,ITALIC=2;}
STUB
cat > "$DIR/src/android/text/Spannable.java" <<'STUB'
package android.text; public interface Spannable extends CharSequence {int SPAN_EXCLUSIVE_EXCLUSIVE=33; <T>T[] getSpans(int a,int b,Class<T> t); int getSpanStart(Object s); int getSpanEnd(Object s); void setSpan(Object s,int a,int b,int flags); void removeSpan(Object s);}
STUB
cat > "$DIR/src/android/text/Editable.java" <<'STUB'
package android.text; public interface Editable extends Spannable {}
STUB
cat > "$DIR/src/android/text/style/StyleSpan.java" <<'STUB'
package android.text.style; public class StyleSpan {private final int style; public StyleSpan(int style){this.style=style;} public int getStyle(){return style;}}
STUB
cat > "$DIR/src/android/text/style/UnderlineSpan.java" <<'STUB'
package android.text.style; public class UnderlineSpan {}
STUB
"$JAVA" -Dsun.boot.class.path="$ANDROID" -cp "$TOOLS" com.sun.tools.javac.Main -source 8 -target 8 -bootclasspath "$ANDROID" -d "$DIR" "$JSON_JAVA_SOURCE"/*.java "$DIR"/src/android/graphics/*.java "$DIR"/src/android/text/*.java "$DIR"/src/android/text/style/*.java app/src/main/java/com/gt30pro/launcher/NoteFormats.java app/src/main/java/com/gt30pro/launcher/NoteFormatEditor.java tests/NoteFormatEditorTest.java
"$JAVA" -cp "$DIR" com.gt30pro.launcher.NoteFormatEditorTest
