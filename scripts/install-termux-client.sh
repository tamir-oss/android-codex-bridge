#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SOURCE="$ROOT_DIR/scripts/companion-control.py"
TARGET="$PREFIX/bin/android-ui"

if [ ! -f "$SOURCE" ]; then
    printf 'Client source not found: %s\n' "$SOURCE" >&2
    exit 1
fi

install -m 700 "$SOURCE" "$TARGET"
printf 'Installed %s\n' "$TARGET"
printf 'Run: android-ui status\n'
