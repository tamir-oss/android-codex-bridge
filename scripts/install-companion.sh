#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
APK="$ROOT_DIR/companion/build/android-codex-bridge-companion.apk"

adb devices -l
PHONE_SERIAL=$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')
if [ -z "$PHONE_SERIAL" ]; then
    printf 'NO_READY_DEVICE\n' >&2
    exit 2
fi
adb -s "$PHONE_SERIAL" shell id
adb -s "$PHONE_SERIAL" shell '
  dumpsys window policy | grep "mKeyguardUnlocked=" | head -1
  dumpsys window | grep -E "mCurrentFocus|mFocusedApp" | head -2
'
if [ ! -f "$APK" ]; then
    printf 'Build the companion first: bash scripts/build-companion.sh\n' >&2
    exit 1
fi

adb -s "$PHONE_SERIAL" install -r "$APK"
adb -s "$PHONE_SERIAL" shell monkey \
    -p com.tamir.androidcodexbridge \
    -c android.intent.category.LAUNCHER 1 >/dev/null
printf 'Installed and opened the companion. Enable its service manually in Accessibility settings.\n'
