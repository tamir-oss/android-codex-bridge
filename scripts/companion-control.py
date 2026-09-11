#!/usr/bin/env python3
"""Termux client for the authenticated local Android accessibility companion."""

import argparse
import json
import socket
import sys
import uuid
import os
from pathlib import Path
import re
import subprocess


LOOPBACK_ADDRESS = ("127.0.0.1", 8765)
PROTOCOL_VERSION = 1
MAX_RESPONSE_BYTES = 1024 * 1024
TOKEN_FILE = Path.home() / ".config" / "android-codex-bridge" / "token"


def encode_request(command, args=None, request_id=None, token=""):
    request = {
        "version": PROTOCOL_VERSION,
        "id": request_id or str(uuid.uuid4()),
        "command": command,
        "args": args or {},
        "token": token,
    }
    return (json.dumps(request, ensure_ascii=False, separators=(",", ":")) + "\n").encode("utf-8")


def decode_response(data):
    if not data.endswith(b"\n"):
        raise ValueError("Companion closed the connection without a complete response")
    response = json.loads(data.decode("utf-8"))
    if not isinstance(response, dict) or response.get("version") != PROTOCOL_VERSION:
        raise ValueError("Companion returned an unsupported response")
    return response


def request(command, args=None, timeout=10.0):
    token = load_token()
    client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    client.settimeout(timeout)
    try:
        client.connect(LOOPBACK_ADDRESS)
        client.sendall(encode_request(command, args, token=token))
        chunks = bytearray()
        while not chunks.endswith(b"\n"):
            chunk = client.recv(65536)
            if not chunk:
                break
            chunks.extend(chunk)
            if len(chunks) > MAX_RESPONSE_BYTES:
                raise ValueError("Companion response exceeded the safety limit")
        return decode_response(bytes(chunks))
    finally:
        client.close()


def connection_error(message):
    return {
        "version": PROTOCOL_VERSION,
        "id": None,
        "ok": False,
        "error": {
            "code": "CONNECTION_UNAVAILABLE",
            "message": message,
            "hint": "Enable the companion accessibility service and pair this client locally.",
        },
    }


def load_token():
    try:
        token = TOKEN_FILE.read_text(encoding="ascii").strip()
    except FileNotFoundError as error:
        raise ConnectionError(
            "No local token. In the companion tap Copy, then run android-ui pair-from-clipboard.") from error
    if not re.fullmatch(r"[0-9a-f]{64}", token):
        raise ConnectionError("The stored local token is invalid; pair again")
    return token


def save_token(token):
    if not re.fullmatch(r"[0-9a-f]{64}", token):
        raise ValueError("Clipboard does not contain a valid pairing token")
    TOKEN_FILE.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
    TOKEN_FILE.write_text(token + "\n", encoding="ascii")
    os.chmod(TOKEN_FILE, 0o600)


def pair_from_clipboard():
    completed = subprocess.run(
        ["termux-clipboard-get"], capture_output=True, text=True, timeout=10, check=False)
    if completed.returncode:
        raise RuntimeError(completed.stderr.strip() or "Could not read the Termux clipboard")
    save_token(completed.stdout.strip())
    subprocess.run(["termux-clipboard-set", ""], timeout=10, check=False,
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    return {
        "version": PROTOCOL_VERSION,
        "id": None,
        "ok": True,
        "result": {"paired": True, "token_file_mode": "0600", "clipboard_cleared": True},
    }


def build_parser():
    parser = argparse.ArgumentParser(
        description="Structured Android UI control through the local accessibility companion")
    parser.add_argument("--compact", action="store_true", help="print compact JSON")
    sub = parser.add_subparsers(dest="command", required=True)

    sub.add_parser("pair-from-clipboard",
                   help="store a token copied explicitly from the companion, then clear the clipboard")
    sub.add_parser("status", help="show service, lock and active-window state")
    for action in ("start", "renew", "stop"):
        awake = sub.add_parser("awake-" + action, help="bounded task-only screen-awake lease")
        if action != "start":
            awake.add_argument("lease_id", help="lease id returned by awake-start")
        if action != "stop":
            awake.add_argument("--seconds", type=int, default=120, choices=range(5, 601),
                               metavar="5..600", help="expiry unless explicitly renewed (default 120)")
    inspect = sub.add_parser("inspect", help="read a fresh structured UI snapshot")
    inspect.add_argument("--no-text", action="store_true",
                         help="omit visible text and content descriptions")

    for name, action in (
        ("click", "click"),
        ("focus", "focus"),
        ("scroll-forward", "scroll_forward"),
        ("scroll-backward", "scroll_backward"),
    ):
        command = sub.add_parser(name)
        command.add_argument("node", help="node id from the most recent inspect")
        command.set_defaults(action=action)

    set_text = sub.add_parser("set-text", help="set Unicode text on a supported editable node")
    set_text.add_argument("node", help="node id from the most recent inspect")
    set_text.add_argument("text", help="Unicode text; quote it as one shell argument")
    return parser


def main(argv=None):
    args = build_parser().parse_args(argv)
    if args.command == "pair-from-clipboard":
        try:
            response = pair_from_clipboard()
        except (OSError, RuntimeError, ValueError, subprocess.TimeoutExpired) as error:
            response = connection_error(str(error))
    elif args.command == "status":
        command, payload = "status", {}
    elif args.command.startswith("awake-"):
        command, payload = "awake", {"action": args.command[6:]}
        if hasattr(args, "lease_id"):
            payload["lease_id"] = args.lease_id
        if hasattr(args, "seconds"):
            payload["seconds"] = args.seconds
    elif args.command == "inspect":
        command, payload = "snapshot", {"include_text": not args.no_text}
    elif args.command == "set-text":
        command = "perform"
        payload = {"node": args.node, "action": "set_text", "text": args.text}
    else:
        command = "perform"
        payload = {"node": args.node, "action": args.action}

    if args.command != "pair-from-clipboard":
        try:
            response = request(command, payload)
        except (ConnectionError, FileNotFoundError, socket.timeout, OSError, ValueError, json.JSONDecodeError) as error:
            response = connection_error(str(error))

    if args.compact:
        print(json.dumps(response, ensure_ascii=False, separators=(",", ":")))
    else:
        print(json.dumps(response, ensure_ascii=False, indent=2))
    return 0 if response.get("ok") else 2


if __name__ == "__main__":
    sys.exit(main())
