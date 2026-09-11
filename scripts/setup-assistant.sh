#!/data/data/com.termux/files/usr/bin/bash
# Interactive, non-destructive setup guide. It never enables debugging or
# grants permissions silently; those steps remain explicit Android actions.

set -u
ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)

usage() {
  cat <<'EOF'
Usage: bash scripts/setup-assistant.sh [--check] [--print]

  --check  run the read-only environment check and exit
  --print  print the manual checklist without opening Android settings
EOF
}

open_settings() {
  if ! command -v rish >/dev/null 2>&1; then
    printf 'rish is not available, so Android settings cannot be opened from this script.\n'
    return 1
  fi
  timeout 5 rish -c "am start $1" >/dev/null 2>&1 || {
    printf 'Could not open Android settings automatically. Open them manually.\n'
    return 1
  }
  return 0
}

print_checklist() {
  cat <<'EOF'

Manual checklist
---------------
1. Enable Developer options on the Android phone.
2. Enable USB debugging, then authorize the computer in the Android prompt.
3. Install Termux from a trusted source.
4. Install and start Shizuku, then authorize Termux when prompted.
5. Confirm that rish is exported to Termux.
6. Install Codex CLI and the development tools you need.
7. Add the local Codex instance to the ChatGPT app through its authenticated bridge flow.
8. Run: bash scripts/check-environment.sh

The script does not enter pairing codes, enable debugging, install APKs, or
grant permissions for you. Those actions stay visible and user-approved.
EOF
}

for arg in "$@"; do
  case "$arg" in
    --check) bash "$ROOT_DIR/scripts/check-environment.sh"; exit $? ;;
    --print) print_checklist; exit 0 ;;
    -h|--help) usage; exit 0 ;;
    *) printf 'Unknown option: %s\n' "$arg" >&2; usage >&2; exit 2 ;;
  esac
done

printf '%s\n' 'Codex on Android — setup assistant'
printf '%s\n' '=================================='
printf '%s\n' 'This assistant guides setup and leaves security-sensitive actions to you.'
printf '\n'
print_checklist

if [ ! -t 0 ]; then
  exit 0
fi

printf '\nOpen Developer options now? [y/N] '
read -r answer
case "$answer" in
  y|Y|yes|YES) open_settings 'android.settings.DEVELOPMENT_SETTINGS' || true ;;
  *) printf 'Skipping settings.\n' ;;
esac

printf '\nRun the environment check now? [Y/n] '
read -r answer
case "$answer" in
  n|N|no|NO) printf 'Skipping check.\n' ;;
  *) bash "$ROOT_DIR/scripts/check-environment.sh" || true ;;
esac

