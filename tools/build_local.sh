#!/usr/bin/env bash
# Build lokal GT30 PRO Launcher tanpa Gradle:
#   aapt2 compile/link -> javac -> d8 -> inject dex -> sign (v1+v2+v3) -> verifikasi
#
# Output: dist/GT30PROLauncher-v<versi>-debug.apk dan -release.apk
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TC="${TOOLCHAIN:-$HOME/toolchain}"
JAVA_HOME="${JAVA_HOME:-$TC/venv/lib/python3.11/site-packages/jdk4py/java-runtime}"
AAPT2="${AAPT2:-$TC/npm/aaptjs3-2.0.2/bin/x64/linux/aapt2}"
D8JAR="${D8JAR:-$TC/npm/d8-jvm.jar}"
TOOLSJAR="${TOOLSJAR:-$TC/npm/dataslope-tools-jar-1.0.0/tools.jar}"
ANDROID_JAR="${ANDROID_JAR:-$TC/android-platforms/android-35/android.jar}"
APKSIGN_LIB="${APKSIGN_LIB:-$TC/npm/apk_sign_ts-1.0.1}"

PKG="com.gt30pro.launcher"
VERSION_NAME="0.1.123"
VERSION_CODE=124
MIN_SDK=26
TARGET_SDK=35

BUILD="$ROOT/build"
DIST="$ROOT/dist"

for f in "$AAPT2" "$D8JAR" "$TOOLSJAR" "$ANDROID_JAR" "$JAVA_HOME/bin/java"; do
  [ -e "$f" ] || { echo "ERROR: toolchain tidak ditemukan: $f (jalankan tools/bootstrap_toolchain.sh)"; exit 1; }
done

rm -rf "$BUILD"
mkdir -p "$BUILD/gen" "$BUILD/classes" "$BUILD/dex" "$BUILD/compiled" "$DIST"

echo "==> [1/7] aapt2 compile resources"
"$AAPT2" compile --dir "$ROOT/app/src/main/res" -o "$BUILD/compiled/res.zip"

echo "==> [2/7] aapt2 link (base APK + R.java)"
sed "s#<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">#<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" package=\"$PKG\">#" \
  "$ROOT/app/src/main/AndroidManifest.xml" > "$BUILD/AndroidManifest.xml"
"$AAPT2" link -o "$BUILD/base.apk" \
  -I "$ANDROID_JAR" \
  --manifest "$BUILD/AndroidManifest.xml" \
  -R "$BUILD/compiled/res.zip" \
  -A "$ROOT/app/src/main/assets" \
  --java "$BUILD/gen" \
  --min-sdk-version $MIN_SDK --target-sdk-version $TARGET_SDK \
  --version-code $VERSION_CODE --version-name "$VERSION_NAME" \
  --auto-add-overlay

echo "==> [3/7] javac (via tools.jar; stub LambdaMetafactory karena android.jar tidak memuatnya)"
STUB="$BUILD/stub"
mkdir -p "$STUB/src/java/lang/invoke" "$STUB/out"
cat > "$STUB/src/java/lang/invoke/LambdaMetafactory.java" <<'STUBEOF'
package java.lang.invoke;

/** Stub kompilasi saja (class + static) - Android mendesugar lambda saat runtime. */
public final class LambdaMetafactory {
    private LambdaMetafactory() {}

    public static CallSite metafactory(MethodHandles.Lookup caller,
                                       String invokedName,
                                       MethodType invokedType,
                                       MethodType samMethodType,
                                       MethodHandle implMethod,
                                       MethodType instantiatedMethodType) throws LambdaConversionException {
        throw new UnsupportedOperationException("compile-time stub");
    }

    public static CallSite altMetafactory(MethodHandles.Lookup caller,
                                          String invokedName,
                                          MethodType invokedType,
                                          Object... args) throws LambdaConversionException {
        throw new UnsupportedOperationException("compile-time stub");
    }
}
STUBEOF
JAVAC=("$JAVA_HOME/bin/java" -Dsun.boot.class.path="$STUB/out:$ANDROID_JAR" -cp "$TOOLSJAR"
       com.sun.tools.javac.Main -source 8 -target 8 -encoding UTF-8
       -bootclasspath "$STUB/out:$ANDROID_JAR")
"${JAVAC[@]}" -d "$STUB/out" "$STUB/src/java/lang/invoke/LambdaMetafactory.java"
find "$ROOT/app/src/main/java" "$BUILD/gen" -name '*.java' > "$BUILD/sources.txt"
"${JAVAC[@]}" -d "$BUILD/classes" @"$BUILD/sources.txt"

echo "==> [4/7] d8 (dex)"
"$JAVA_HOME/bin/java" -cp "$D8JAR" com.android.tools.r8.D8 \
  --release --min-api $MIN_SDK --lib "$ANDROID_JAR" \
  --output "$BUILD/dex" $(find "$BUILD/classes" -name '*.class')

echo "==> [5/7] Inject classes.dex ke APK"
(cd "$BUILD/dex" && zip -q -j "$BUILD/base.apk" classes.dex)

echo "==> [6/7] Signing (v1+v2+v3)"
bash "$ROOT/tools/gen_keystores.sh"
KEYSTORE_DEBUG="${KEYSTORE_DEBUG:-$HOME/.android/debug.keystore}"
KEYSTORE_RELEASE="${KEYSTORE_RELEASE:-$HOME/.android/gt30pro-release.keystore}"
APKSIGN_LIB="$APKSIGN_LIB" node "$ROOT/tools/sign_apk.js" \
  "$BUILD/base.apk" "$DIST/GT30PROLauncher-v$VERSION_NAME-debug.apk" "$KEYSTORE_DEBUG" android
APKSIGN_LIB="$APKSIGN_LIB" node "$ROOT/tools/sign_apk.js" \
  "$BUILD/base.apk" "$DIST/GT30PROLauncher-v$VERSION_NAME-release.apk" "$KEYSTORE_RELEASE" "${RELEASE_PASSWORD:-gt30pro}"

echo "==> [7/7] Verifikasi"
for apk in "$DIST"/GT30PROLauncher-v$VERSION_NAME-*.apk; do
  echo "--- $(basename "$apk")"
  python3 - "$apk" <<'PYEOF'
import sys, zipfile
apk = sys.argv[1]
data = open(apk, 'rb').read()
print("  APK Sig Block 42 (v2/v3):", "OK" if b'APK Sig Block 42' in data else "TIDAK ADA")
with zipfile.ZipFile(apk) as z:
    names = z.namelist()
    checks = ['classes.dex', 'AndroidManifest.xml', 'resources.arsc', 'META-INF/MANIFEST.MF', 'assets/wallpaper_gt30pro.png', 'assets/wallpaper_amber_glow.png', 'assets/circuit_paths.json']
    for f in checks:
        print("  %-32s %s" % (f + ":", "OK" if f in names else "HILANG"))
    sigs = [n for n in names if n.startswith('META-INF/') and n.rsplit('.', 1)[-1] in ('RSA', 'DSA', 'EC')]
    print("  %-32s %s" % ("Sertifikat v1 (META-INF/*.RSA):", sigs[0] if sigs else "TIDAK ADA"))
print("  ukuran: %.2f MB" % (len(data) / 1e6))
PYEOF
done

echo
echo "Selesai. APK ada di: $DIST/"
ls -la "$DIST"
