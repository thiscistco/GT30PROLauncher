#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -z "${JSON_JAVA_SOURCE:-}" ]]; then echo 'Note backup test skipped (set JSON_JAVA_SOURCE)'; exit 0; fi
TC="${TOOLCHAIN:-$HOME/toolchain}"
JAVA="${JAVA_HOME:-$TC/venv/lib/python3.11/site-packages/jdk4py/java-runtime}/bin/java"
ANDROID="${ANDROID_JAR:-$TC/android-platforms/android-35/android.jar}"
TOOLS="${TOOLSJAR:-$TC/npm/dataslope-tools-jar-1.0.0/tools.jar}"
DIR="$(mktemp -d)"; trap 'rm -rf "$DIR"' EXIT
mkdir -p "$DIR/com/gt30pro/launcher"
cat > "$DIR/com/gt30pro/launcher/NoteStore.java" <<'STUB'
package com.gt30pro.launcher;
final class NoteStore {
 static final class Note {
  final long id,updated; final String title,body,tag,tags,format; final int pinned,state,previousState,color;
  Note(long id,String title,String body,long updated,int pinned,int state,int previousState,String tag,int color) {
   this(id,title,body,updated,pinned,state,previousState,tag,color,null,null);
  }
  Note(long id,String title,String body,long updated,int pinned,int state,int previousState,String tag,int color,String tags,String format) {
   this.id=id;this.title=title;this.body=body;this.updated=updated;this.pinned=pinned;this.state=state;this.previousState=previousState;this.tag=tag;this.color=color;this.tags=tags;this.format=format;
  }
 }
}
STUB
"$JAVA" -Dsun.boot.class.path="$ANDROID" -cp "$TOOLS" com.sun.tools.javac.Main -source 8 -target 8 -bootclasspath "$ANDROID" -d "$DIR" "$JSON_JAVA_SOURCE"/*.java "$DIR/com/gt30pro/launcher/NoteStore.java" app/src/main/java/com/gt30pro/launcher/NoteText.java app/src/main/java/com/gt30pro/launcher/NoteTags.java app/src/main/java/com/gt30pro/launcher/NoteFormats.java app/src/main/java/com/gt30pro/launcher/NoteBackup.java tests/NoteBackupTest.java tests/NoteTagsFormatsTest.java
"$JAVA" -cp "$DIR" com.gt30pro.launcher.NoteBackupTest
"$JAVA" -cp "$DIR" com.gt30pro.launcher.NoteTagsFormatsTest
