#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
APP_DIR="$ROOT_DIR/companion"
BUILD_DIR="$APP_DIR/build"
SDK_ROOT=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/usr/lib/android-sdk}}
ANDROID_JAR=${ANDROID_JAR:-"$SDK_ROOT/platforms/android-23/android.jar"}
BUILD_TOOLS=${ANDROID_BUILD_TOOLS:-"$SDK_ROOT/build-tools/debian"}
AAPT=${AAPT:-"$BUILD_TOOLS/aapt"}
DX=${DX:-"$BUILD_TOOLS/dx"}
ZIPALIGN=${ZIPALIGN:-"$BUILD_TOOLS/zipalign"}
APKSIGNER=${APKSIGNER:-"$BUILD_TOOLS/apksigner"}
KEYSTORE=${ANDROID_CODEX_KEYSTORE:-"$ROOT_DIR/local/android-codex-bridge.keystore"}

for path in "$ANDROID_JAR" "$AAPT" "$DX" "$ZIPALIGN" "$APKSIGNER"; do
    if [ ! -e "$path" ]; then
        printf 'Required Android build tool not found: %s\n' "$path" >&2
        exit 1
    fi
done

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/gen" "$BUILD_DIR/classes" "$ROOT_DIR/local"

"$AAPT" package -f -m \
    -J "$BUILD_DIR/gen" \
    -M "$APP_DIR/AndroidManifest.xml" \
    -S "$APP_DIR/res" \
    -I "$ANDROID_JAR"

find "$APP_DIR/src" "$BUILD_DIR/gen" -name '*.java' -print | sort > "$BUILD_DIR/sources.list"
javac -encoding UTF-8 -source 8 -target 8 \
    -classpath "$ANDROID_JAR" \
    -d "$BUILD_DIR/classes" \
    @"$BUILD_DIR/sources.list"

"$DX" --dex --output="$BUILD_DIR/classes.dex" "$BUILD_DIR/classes"
"$AAPT" package -f \
    -M "$APP_DIR/AndroidManifest.xml" \
    -S "$APP_DIR/res" \
    -I "$ANDROID_JAR" \
    -F "$BUILD_DIR/companion-unsigned.apk"
(
    cd "$BUILD_DIR"
    "$AAPT" add companion-unsigned.apk classes.dex >/dev/null
)
"$ZIPALIGN" -f 4 "$BUILD_DIR/companion-unsigned.apk" "$BUILD_DIR/companion-aligned.apk"

if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair -noprompt \
        -keystore "$KEYSTORE" \
        -storepass android \
        -keypass android \
        -alias android-codex-bridge \
        -keyalg RSA \
        -keysize 3072 \
        -validity 3650 \
        -dname 'CN=Android Codex Bridge Local Build,O=android-codex-bridge'
    chmod 600 "$KEYSTORE"
fi

"$APKSIGNER" sign \
    --ks "$KEYSTORE" \
    --ks-key-alias android-codex-bridge \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "$BUILD_DIR/android-codex-bridge-companion.apk" \
    "$BUILD_DIR/companion-aligned.apk"
"$APKSIGNER" verify --verbose "$BUILD_DIR/android-codex-bridge-companion.apk"
printf '%s\n' "$BUILD_DIR/android-codex-bridge-companion.apk"
