#!/usr/bin/env python3
"""User-driven lifecycle checks; only built-in test fields are modified."""
import argparse
import contextlib
import importlib.util
import io
import json
import os
from pathlib import Path
import re
import tempfile
import time

ROOT = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location('device_test', ROOT / 'test-companion-on-device.py')
TEST = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(TEST)


def usb_connected(dump):
    # First connected field is the current USB device state in dumpsys usb.
    match = re.search(r'^\s+connected=(true|false)\s*$', dump, re.MULTILINE)
    if not match:
        raise RuntimeError('USB_STATE_UNAVAILABLE')
    return match.group(1) == 'true'


def restore_chatgpt():
    status = TEST.call('status')
    if status.get('locked'):
        return 'device_locked'
    TEST.rish('am start -n com.openai.chatgpt/.MainActivity')
    for _ in range(10):
        if TEST.call('status').get('package') == 'com.openai.chatgpt':
            return 'verified'
        time.sleep(0.3)
    return 'not_verified'


def check(phase, wait_seconds):
    if phase == 'disabled':
        try:
            TEST.CLIENT.request('status')
        except ConnectionRefusedError:
            return ['service_connection_refused']
        raise RuntimeError('EXPECTED_SERVICE_DISABLED')

    if phase == 'usb-detached':
        deadline = time.monotonic() + wait_seconds
        while usb_connected(TEST.rish('dumpsys usb')):
            if time.monotonic() >= deadline:
                raise RuntimeError('USB_STILL_CONNECTED')
            time.sleep(2)

    status = TEST.call('status')
    if phase == 'locked':
        if not status.get('locked'):
            raise RuntimeError('EXPECTED_DEVICE_LOCKED')
        for command, args in (
            ('snapshot', {'include_text': False}),
            ('perform', {'node': 'invalid-lock-probe', 'action': 'focus'}),
        ):
            reply = TEST.CLIENT.request(command, args)
            if reply.get('ok') or reply.get('error', {}).get('code') != 'DEVICE_LOCKED':
                raise RuntimeError('LOCK_REJECTION_FAILED')
        return ['locked_state', 'snapshot_rejected', 'action_rejected']

    if status.get('locked'):
        raise RuntimeError('DEVICE_LOCKED')
    # Output contains only test result labels; no UI text or credentials are persisted.
    with contextlib.redirect_stdout(io.StringIO()):
        TEST.main()
    results = ['authenticated_connection', 'unlocked', 'safe_ui_suite']
    if phase == 'usb-detached':
        if usb_connected(TEST.rish('dumpsys usb')):
            raise RuntimeError('USB_RECONNECTED_DURING_TEST')
        results.append('usb_disconnected_before_and_after')
    return results


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('phase', choices=['disabled', 'reconnected', 'locked', 'unlocked', 'usb-detached'])
    parser.add_argument('--wait-seconds', type=int, default=0, choices=range(0, 301))
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    report = {'phase': args.phase, 'ok': False, 'time_utc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())}
    try:
        report['checks'] = check(args.phase, args.wait_seconds)
        report['ok'] = True
    except Exception as error:
        # Never persist arbitrary exception text containing remote output.
        report['error_type'] = type(error).__name__
        message = str(error)
        report['error'] = message if re.fullmatch('[A-Z_]+', message) else 'CHECK_FAILED'
    finally:
        if args.phase not in ('locked', 'disabled'):
            try:
                report['chatgpt_foreground'] = restore_chatgpt()
            except Exception:
                report['chatgpt_foreground'] = 'unavailable'
    encoded = json.dumps(report, indent=2) + '\n'
    if args.report:
        args.report.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
        descriptor, name = tempfile.mkstemp(prefix='.lifecycle-', dir=args.report.parent)
        with os.fdopen(descriptor, 'w') as output:
            output.write(encoded)
        os.replace(name, args.report)
    print(encoded, end='')
    return 0 if report['ok'] else 2


if __name__ == '__main__':
    raise SystemExit(main())
