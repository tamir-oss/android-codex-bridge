#!/data/data/com.termux/files/usr/bin/bash
# Read-only environment check for Codex on Android.
# This script reports state; it does not install packages or change settings.

set -u

PASS=0
WARN=0
FAIL=0

if [ -t 1 ]; then
  GREEN='\033[32m'; YELLOW='\033[33m'; RED='\033[31m'; RESET='\033[0m'
else
  GREEN=''; YELLOW=''; RED=''; RESET=''
fi

pass() { PASS=$((PASS + 1)); printf "%b[OK]%b %s\n" "$GREEN" "$RESET" "$1"; }
warn() { WARN=$((WARN + 1)); printf "%b[WARN]%b %s\n" "$YELLOW" "$RESET" "$1"; }
fail() { FAIL=$((FAIL + 1)); printf "%b[FAIL]%b %s\n" "$RED" "$RESET" "$1"; }
info() { printf "[INFO] %s\n" "$1"; }

command_version() {
  command -v "$1" >/dev/null 2>&1 || return 1
  case "$1" in
    codex) codex --version 2>/dev/null | head -n 1 ;;
    node) node --version 2>/dev/null | head -n 1 ;;
    npm) npm --version 2>/dev/null | head -n 1 ;;
    git) git --version 2>/dev/null | head -n 1 ;;
    python) python --version 2>&1 | head -n 1 ;;
    ssh) ssh -V 2>&1 | head -n 1 ;;
    *) command -v "$1" ;;
  esac
}

android_shell() {
  if command -v rish >/dev/null 2>&1; then
    timeout 5 rish -c "$1" 2>/dev/null
  else
    return 127
  fi
}

printf '%s\n' 'Codex on Android — read-only environment check'
printf '%s\n' '================================================'

if [ "$(uname -o 2>/dev/null || true)" = "Android" ] || [ -d /data/data/com.termux ]; then
  pass 'Running in an Android/Termux environment'
else
  warn 'This does not look like a standard Termux environment'
fi

if command -v getprop >/dev/null 2>&1; then
  RELEASE=$(getprop ro.build.version.release 2>/dev/null || true)
  API=$(getprop ro.build.version.sdk 2>/dev/null || true)
  DEVICE=$(getprop ro.product.manufacturer 2>/dev/null || true)
  MODEL=$(getprop ro.product.model 2>/dev/null || true)
  info "Android ${RELEASE:-unknown} (API ${API:-unknown}), ${DEVICE:-unknown} ${MODEL:-unknown}"
else
  warn 'Android properties are not available from this shell'
fi

for tool in rish codex node npm git python ssh; do
  if VERSION=$(command_version "$tool"); then
    pass "$tool: $VERSION"
  else
    if [ "$tool" = rish ] || [ "$tool" = codex ]; then
      fail "$tool is not available"
    else
      warn "$tool is not available"
    fi
  fi
done

if command -v rish >/dev/null 2>&1; then
  RISH_UID=$(android_shell 'id -u' | tr -d '\r\n' || true)
  if [ "$RISH_UID" = 2000 ]; then
    pass 'rish can reach the Android shell (uid 2000)'
  elif [ -n "$RISH_UID" ]; then
    warn "rish responded with uid $RISH_UID; expected Android shell uid 2000"
  else
    fail 'rish is present but did not return an Android shell identity'
  fi
else
  fail 'Skipping rish bridge test'
fi

if command -v rish >/dev/null 2>&1; then
  for package in moe.shizuku.privileged.api com.termux com.openai.chatgpt; do
    if android_shell "pm path $package" | grep -q '^package:'; then
      pass "Android package found: $package"
    else
      warn "Android package not found or not readable: $package"
    fi
  done
fi

if command -v sshd >/dev/null 2>&1; then
  if pgrep -f '[s]shd' >/dev/null 2>&1; then
    info 'OpenSSH server process is running'
  else
    info 'OpenSSH is installed; sshd is not running (this is optional)'
  fi
fi

printf '\nSummary: %d OK, %d warnings, %d failures\n' "$PASS" "$WARN" "$FAIL"
if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0
