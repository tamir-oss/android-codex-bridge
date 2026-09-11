#!/data/data/com.termux/files/usr/bin/python
"""Non-destructive real-device checks for the companion's own test activity."""

import importlib.util
import os
from pathlib import Path
import socket
import subprocess
import sys
import time


ROOT = Path(__file__).resolve().parents[1]
CLIENT_PATH = ROOT / "scripts" / "companion-control.py"
SPEC = importlib.util.spec_from_file_location("companion_control", CLIENT_PATH)
CLIENT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(CLIENT)

TEST_PACKAGE = "com.tamir.androidcodexbridge"
TEST_ACTIVITY = TEST_PACKAGE + "/.MainActivity"


def rish(command):
    environment = dict(os.environ)
    environment["RISH_APPLICATION_ID"] = "com.termux"
    completed = subprocess.run(
        ["rish", "-c", command],
        capture_output=True,
        text=True,
        timeout=10,
        env=environment,
        check=False,
    )
    if completed.returncode:
        raise RuntimeError(completed.stderr.strip() or "rish command failed")
    return completed.stdout.strip()


def call(command, args=None):
    response = CLIENT.request(command, args or {})
    if not response.get("ok"):
        error = response.get("error", {})
        raise RuntimeError(error.get("code", "UNKNOWN") + ": " + error.get("message", ""))
    return response["result"]


def snapshot():
    result = call("snapshot", {"include_text": True})
    if result.get("package") != TEST_PACKAGE:
        raise RuntimeError("Unexpected active package: " + str(result.get("package")))
    return result


def node_by_view_id(result, suffix):
    for node in result["nodes"]:
        view_id = node.get("view_id") or ""
        if view_id.endswith("/" + suffix):
            return node
    raise RuntimeError("Node not found: " + suffix)


def node_with_action(result, action):
    for node in result["nodes"]:
        if action in node.get("actions", []):
            return node
    raise RuntimeError("No node supports action: " + action)


def set_text(view_id, value):
    current = snapshot()
    node = node_by_view_id(current, view_id)
    result = call("perform", {"node": node["id"], "action": "set_text", "text": value})
    if not result.get("accepted") or not result.get("verified"):
        raise RuntimeError("Text entry was not verified for " + view_id)


def visible_text(result, expected):
    return any(node.get("text") == expected for node in result["nodes"])


def assert_invalid_auth_rejected():
    client = socket.create_connection(CLIENT.LOOPBACK_ADDRESS, timeout=3)
    try:
        client.sendall(CLIENT.encode_request("status", token="0" * 64))
        data = bytearray()
        while not data.endswith(b"\n"):
            chunk = client.recv(65536)
            if not chunk:
                break
            data.extend(chunk)
        response = CLIENT.decode_response(bytes(data))
    finally:
        client.close()
    if response.get("ok") or response.get("error", {}).get("code") != "UNAUTHORIZED":
        raise RuntimeError("Invalid local authentication was not rejected")


def main():
    rish("am start -n " + TEST_ACTIVITY)
    time.sleep(1)
    status = call("status")
    if status.get("locked") or status.get("package") != TEST_PACKAGE:
        raise RuntimeError("Companion test activity is not active and unlocked")
    print("PASS status and active-window check")
    assert_invalid_auth_rejected()
    if call("status").get("package") != TEST_PACKAGE:
        raise RuntimeError("Authenticated reconnect did not return to the test activity")
    print("PASS invalid-auth rejection and authenticated reconnect")

    first = snapshot()
    for view_id in ("input_hebrew", "input_english", "input_mixed", "button_primary"):
        node_by_view_id(first, view_id)
    print("PASS structured fields and button discovery")

    set_text("input_hebrew", "שלום עולם")
    print("PASS Hebrew ACTION_SET_TEXT")
    set_text("input_english", "Hello Android")
    print("PASS English ACTION_SET_TEXT")
    set_text("input_mixed", "שלום Codex 123")
    print("PASS mixed ACTION_SET_TEXT")

    before_click = snapshot()
    button = node_by_view_id(before_click, "button_primary")
    call("perform", {"node": button["id"], "action": "click"})
    after_click = snapshot()
    if not visible_text(after_click, "הכפתור הופעל; שדות שאינם ריקים: 3"):
        raise RuntimeError("Button result was not visible in the fresh snapshot")
    print("PASS button click and fresh-result verification")

    scroll_node = node_with_action(after_click, "scroll_forward")
    call("perform", {"node": scroll_node["id"], "action": "scroll_forward"})
    after_scroll = snapshot()
    if not visible_text(after_scroll, "כפתור בתחתית המסך"):
        raise RuntimeError("Bottom control was not found after scrolling")
    print("PASS scroll and post-scroll inspection")

    stale_node = None
    for _ in range(3):
        stale_snapshot = snapshot()
        candidate = node_by_view_id(stale_snapshot, "input_hebrew")
        current_button = node_by_view_id(stale_snapshot, "button_clear")
        clear_response = CLIENT.request(
            "perform", {"node": current_button["id"], "action": "click"})
        if clear_response.get("ok"):
            stale_node = candidate
            break
        if clear_response.get("error", {}).get("code") != "STALE_NODE":
            raise RuntimeError("Clear action failed before stale-node test")
        time.sleep(0.2)
    if stale_node is None:
        raise RuntimeError("Screen did not settle for stale-node test")
    stale_response = CLIENT.request("perform", {
        "node": stale_node["id"], "action": "set_text", "text": "must not be entered"
    })
    if stale_response.get("ok") or stale_response.get("error", {}).get("code") != "STALE_NODE":
        raise RuntimeError("Stale-node rejection did not occur")
    print("PASS stale-node rejection")

    window_snapshot = snapshot()
    old_node = node_by_view_id(window_snapshot, "input_hebrew")
    rish("am start -a android.settings.SETTINGS")
    time.sleep(1)
    changed = CLIENT.request("perform", {
        "node": old_node["id"], "action": "set_text", "text": "must not be entered"
    })
    if changed.get("ok") or changed.get("error", {}).get("code") not in {
        "STALE_NODE", "STALE_WINDOW"
    }:
        raise RuntimeError("Changed-window rejection did not occur")
    print("PASS changed-window rejection")

    rish("am start -n " + TEST_ACTIVITY)
    print("PASS all safe on-device companion checks")


if __name__ == "__main__":
    try:
        main()
    except (OSError, RuntimeError, subprocess.TimeoutExpired) as error:
        print("FAIL " + str(error), file=sys.stderr)
        sys.exit(1)
