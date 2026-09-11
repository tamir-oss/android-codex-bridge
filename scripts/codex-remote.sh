#!/data/data/com.termux/files/usr/bin/bash
# Manual lifecycle for the existing authenticated Codex Remote connection.
set -euo pipefail
action=${1:-start}
case "$action" in start|restart|status|stop) ;; *) echo 'Usage: codex-remote [start|restart|status|stop]' >&2; exit 2;; esac
: "${PREFIX:?Run this command inside Termux}"
codex_bin=$(command -v codex)
if [[ "$action" == status ]]; then exec "$codex_bin" app-server daemon version; fi
if [[ "$action" == stop ]]; then exec "$codex_bin" app-server daemon stop; fi
command -v proot >/dev/null
command -v python >/dev/null
command -v flock >/dev/null
test -s "$PREFIX/etc/resolv.conf"
test -s "$PREFIX/etc/tls/cert.pem"
umask 077
state_dir="$HOME/.local/state/android-codex-bridge/remote"
mkdir -p "$state_dir"
chmod 700 "$state_dir"
exec 9>"$state_dir/start.lock"
flock -n 9 || { echo 'Another startup is in progress.' >&2; exit 1; }
if [[ "$action" == restart ]]; then
  "$codex_bin" app-server daemon stop >/dev/null
elif "$codex_bin" app-server daemon version | python -c 'import json,sys; sys.exit(0 if json.load(sys.stdin).get("status")=="running" else 1)'; then
  echo 'Codex is already running. Check localhost in ChatGPT; use restart if its connection is failing.'
  exit 0
fi
export SSL_CERT_FILE="$PREFIX/etc/tls/cert.pem"
export SSL_CERT_DIR="$PREFIX/etc/tls/certs"
# Map only the missing resolver file. Broad /usr bindings interfere with Android's linker.
# Do not use termux-chroot: its kill-on-exit option terminates daemon children.
: >"$state_dir/start-result.log"
nohup proot -b "$PREFIX/etc/resolv.conf:/etc/resolv.conf" \
  "$codex_bin" remote-control start --json \
  >"$state_dir/start-result.log" 2>&1 </dev/null 9>&- &
python - "$state_dir/start-result.log" <<'PY'
import json, pathlib, sys, time
p = pathlib.Path(sys.argv[1])
deadline = time.monotonic() + 45
while time.monotonic() < deadline:
    text = p.read_text(errors='replace') if p.exists() else ''
    for line in text.splitlines():
        try:
            result = json.loads(line)
        except ValueError:
            continue
        if isinstance(result, dict) and 'status' in result:
            print(json.dumps({key: result[key] for key in
                              ('mode', 'status', 'serverName', 'timedOut') if key in result}))
            sys.exit(0 if result.get('status') == 'connected' else 1)
    if any(line.startswith('Error:') for line in text.splitlines()):
        print('Remote connection failed. Diagnostic output is stored privately.', file=sys.stderr)
        sys.exit(1)
    time.sleep(0.5)
print('Connection not confirmed within 45 seconds. Check status and the private startup log.', file=sys.stderr)
sys.exit(1)
PY
