#!/usr/bin/env python3
"""Harmless task-awake checks. Never locks, wakes or changes Android settings."""
import argparse
import importlib.util
from pathlib import Path
import time

spec = importlib.util.spec_from_file_location("ui_test", Path(__file__).with_name("test-companion-on-device.py"))
ui = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ui)


def rejected(args, code):
    result = ui.CLIENT.request("awake", args)
    assert not result["ok"] and result["error"]["code"] == code, code


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--idle-seconds", type=int, default=0, choices=range(0, 551), metavar="0..550")
    args = parser.parse_args()
    lease = None
    try:
        ui.rish("am start -n " + ui.TEST_ACTIVITY)
        time.sleep(1)
        before = ui.call("status")
        assert not before["locked"] and not before["screen_awake"]["active"]
        for value in (0, 601, "120", 5.5):
            rejected({"action": "start", "seconds": value}, "AWAKE_SECONDS_OUT_OF_RANGE")
        result = ui.call("awake", {"action": "start", "seconds": 10})
        lease = result["lease_id"]
        time.sleep(1)
        state = ui.call("status")
        assert state["screen_awake"]["active"] and state["package"] == ui.TEST_PACKAGE
        rejected({"action": "start", "seconds": 10}, "AWAKE_LEASE_BUSY")
        rejected({"action": "stop", "lease_id": "wrong"}, "STALE_AWAKE_LEASE")
        ui.call("awake", {"action": "renew", "lease_id": lease, "seconds": 20})
        assert ui.call("status")["screen_awake"]["remaining_ms"] > 15000
        ui.set_text("input_hebrew", "שלום Codex 123")
        print("PASS lease start, renew, isolation and UI text with overlay", flush=True)
        ui.call("awake", {"action": "stop", "lease_id": lease})
        lease = None
        assert not ui.call("status")["screen_awake"]["active"]
        print("PASS explicit release", flush=True)
        lease = ui.call("awake", {"action": "start", "seconds": 5})["lease_id"]
        time.sleep(7)
        state = ui.call("status")["screen_awake"]
        assert not state["active"] and state["last_end"] == "expired"
        rejected({"action": "renew", "lease_id": lease, "seconds": 10}, "STALE_AWAKE_LEASE")
        lease = None
        print("PASS abandoned lease expiry and stale renewal rejection", flush=True)
        if args.idle_seconds:
            # Close the keyboard; its own window must not account for the screen staying on.
            ui.rish("input keyevent 4")
            ui.rish("am start -n " + ui.TEST_ACTIVITY)
            time.sleep(1)
            assert ui.call("status")["package"] == ui.TEST_PACKAGE
            lease = ui.call("awake", {"action": "start", "seconds": args.idle_seconds + 30})["lease_id"]
            print("READY idle retention test", flush=True)
            time.sleep(args.idle_seconds)
            state = ui.call("status")
            assert not state["locked"] and state["screen_interactive"] and state["screen_awake"]["active"]
            print("PASS idle screen retention", flush=True)
    finally:
        if lease:
            ui.CLIENT.request("awake", {"action": "stop", "lease_id": lease})
        if not ui.call("status")["locked"]:
            ui.rish("am start -n com.openai.chatgpt/.MainActivity")
            time.sleep(1)
            assert ui.call("status")["package"] == "com.openai.chatgpt"
            print("PASS ChatGPT foreground restored", flush=True)


if __name__ == "__main__":
    main()
