#!/usr/bin/env bash
# Membuat keystore debug & release bila belum ada (untuk build lokal/CI).
# PERINGATAN: untuk rilis ke Play Store, gunakan keystore milik Anda sendiri.
set -euo pipefail

TC="${TOOLCHAIN:-$HOME/toolchain}"
JAVA_HOME="${JAVA_HOME:-$TC/venv/lib/python3.11/site-packages/jdk4py/java-runtime}"
KEYTOOL="$JAVA_HOME/bin/keytool"
mkdir -p "$HOME/.android"

if [ ! -f "$HOME/.android/debug.keystore" ]; then
  "$KEYTOOL" -genkeypair -v -keystore "$HOME/.android/debug.keystore" \
    -storetype JKS -storepass android -alias androiddebugkey -keypass android \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US"
fi

if [ ! -f "$HOME/.android/gt30pro-release.keystore" ]; then
  "$KEYTOOL" -genkeypair -v -keystore "$HOME/.android/gt30pro-release.keystore" \
    -storetype JKS -storepass gt30pro -alias gt30pro -keypass gt30pro \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=GT30 PRO Launcher,O=GT30Pro,C=ID"
fi

echo "Keystore siap di $HOME/.android/"
