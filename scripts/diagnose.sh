#!/data/data/com.termux/files/usr/bin/bash
# Print a small, intentionally non-sensitive diagnostic report.

set -u
ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)

printf '%s\n' 'Codex on Android — diagnostic report'
printf '%s\n' '===================================='
printf 'Generated (UTC): '; date -u '+%Y-%m-%dT%H:%M:%SZ' 2>/dev/null || printf 'unknown\n'
printf 'Working tree: %s\n' "$ROOT_DIR"
printf '\n'

if command -v getprop >/dev/null 2>&1; then
  printf 'Android release: '; getprop ro.build.version.release 2>/dev/null || printf 'unknown\n'
  printf 'Android API: '; getprop ro.build.version.sdk 2>/dev/null || printf 'unknown\n'
  printf 'Manufacturer: '; getprop ro.product.manufacturer 2>/dev/null || printf 'unknown\n'
  printf 'Model: '; getprop ro.product.model 2>/dev/null || printf 'unknown\n'
fi

printf '\nTools:\n'
for tool in rish codex node npm git python ssh; do
  if command -v "$tool" >/dev/null 2>&1; then
    printf '  %-8s %s\n' "$tool" "$(command -v "$tool")"
  else
    printf '  %-8s missing\n' "$tool"
  fi
done

printf '\nBridge:\n'
if command -v rish >/dev/null 2>&1; then
  printf '  rish uid: '
  timeout 5 rish -c 'id -u' 2>/dev/null | tr -d '\r' | head -n 1 || printf 'unavailable\n'
else
  printf '  rish unavailable\n'
fi

if command -v rish >/dev/null 2>&1; then
  printf '\nAndroid packages:\n'
  for package in moe.shizuku.privileged.api com.termux com.openai.chatgpt; do
    if timeout 5 rish -c "pm path $package" 2>/dev/null | grep -q '^package:'; then
      printf '  %s: present\n' "$package"
    else
      printf '  %s: unavailable\n' "$package"
    fi
  done
fi

printf '\nEnvironment check:\n'
bash "$ROOT_DIR/scripts/check-environment.sh" || true
