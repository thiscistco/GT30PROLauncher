#!/usr/bin/env bash
# Bootstrap toolchain build lokal & CI (tanpa Android SDK/Gradle):
#   - aapt2      : npm aaptjs3
#   - d8 (dexer) : npm d8-termux
#   - signing    : npm apk_sign_ts
#   - JDK        : pypi jdk4py
#   - android.jar: github.com/Sable/android-platforms (sparse clone)
set -euo pipefail

TC="${TOOLCHAIN:-$HOME/toolchain}"
mkdir -p "$TC/npm"
cd "$TC/npm"

echo "==> Unduh paket npm (aaptjs3, apk_sign_ts, dataslope-tools-jar)"
npm pack aaptjs3@2.0.2 apk_sign_ts@1.0.1 dataslope-tools-jar@1.0.0 >/dev/null
for t in *.tgz; do
  d="${t%.tgz}"
  [ -d "$d" ] || { mkdir -p "$d" && tar -xzf "$t" -C "$d" --strip-components=1; }
done

echo "==> Dependensi npm apk_sign_ts (jks-js, node-forge, pako)"
npm install --prefix "$TC/npm" --no-save \
  $(python3 -c "
import json
d = json.load(open('$TC/npm/apk_sign_ts-1.0.1/package.json'))
print(' '.join(k + '@' + v for k, v in d.get('dependencies', {}).items()))") >/dev/null

echo "==> d8 (dexer JVM) dari repo eprendre/tingshu"
if [ ! -f "$TC/npm/d8-jvm.jar" ]; then
  curl -sL -H "Accept: application/vnd.github.raw" \
    ${GITHUB_TOKEN:+-H "Authorization: Bearer $GITHUB_TOKEN"} \
    "https://api.github.com/repos/eprendre/tingshu/contents/dx/lib/d8.jar" \
    -o "$TC/npm/d8-jvm.jar"
fi

echo "==> Python venv + jdk4py (JDK) + pillow"
if [ ! -x "$TC/venv/bin/python" ]; then
  python3 -m venv "$TC/venv"
fi
"$TC/venv/bin/pip" install -q jdk4py pillow

echo "==> android.jar API 35 (sparse clone)"
if [ ! -f "$TC/android-platforms/android-35/android.jar" ]; then
  rm -rf "$TC/android-platforms"
  git clone --depth 1 --filter=blob:none --sparse https://github.com/Sable/android-platforms "$TC/android-platforms"
  git -C "$TC/android-platforms" sparse-checkout set android-35
fi

echo "==> Selesai. Toolchain di: $TC"
