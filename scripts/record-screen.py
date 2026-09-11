#!/usr/bin/env python3
"""Bounded, silent Android screen recording through an authorized Shizuku shell."""
import argparse
import os
import re
from pathlib import Path
import shlex
import struct
import subprocess
import sys
import uuid


def remote(command, timeout=15):
    p = subprocess.run(['rish', '-c', command], capture_output=True, text=True,
                       timeout=timeout, env={**os.environ, 'RISH_APPLICATION_ID': 'com.termux'})
    if p.returncode:
        raise RuntimeError(p.stderr.strip() or 'Shizuku command failed')
    return p.stdout.strip()


def valid_mp4(path):
    """Require file type, finalized movie metadata and nonempty media data."""
    size = path.stat().st_size
    boxes = set()
    with path.open('rb') as f:
        while f.tell() < size:
            start = f.tell()
            header = f.read(8)
            if len(header) != 8:
                return False
            length, kind = struct.unpack('>I4s', header)
            minimum = 8
            if length == 1:
                extra = f.read(8)
                if len(extra) != 8:
                    return False
                length = struct.unpack('>Q', extra)[0]
                minimum = 16
            elif length == 0:
                length = size - start
            if length < minimum or start + length > size:
                return False
            if length > minimum:
                boxes.add(kind)
            f.seek(start + length)
    return {b'ftyp', b'moov', b'mdat'} <= boxes


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--seconds', type=int, default=10, choices=range(1, 61), metavar='1..60')
    args = parser.parse_args()
    if remote('id -u') != '2000':
        raise RuntimeError('Expected authorized Shizuku shell uid 2000; no recording started')
    if not re.search(r'mKeyguardUnlocked=\s*true\b', remote('dumpsys window policy')):
        raise RuntimeError('Unlock status not confirmed; no recording started')
    name = 'codex-record-' + uuid.uuid4().hex + '.mp4'
    transfer = '/sdcard/Download/' + name
    local_transfer = Path('/storage/emulated/0/Download') / name
    outdir = Path.home() / '.codex/phone-artifacts'
    outdir.mkdir(mode=0o700, parents=True, exist_ok=True)
    target = outdir / name
    try:
        remote('screenrecord --size 720x1280 --bit-rate 2000000 --time-limit '
               + str(args.seconds) + ' ' + shlex.quote(transfer), timeout=args.seconds + 20)
        with local_transfer.open('rb') as source, target.open('xb') as dest:
            os.chmod(target, 0o600)
            import shutil
            shutil.copyfileobj(source, dest)
        if not valid_mp4(target):
            target.unlink()
            raise RuntimeError('Recording did not produce a finalized MP4')
        print(target)
    finally:
        # Only remove the unique transfer created by this invocation.
        remote('rm -f ' + shlex.quote(transfer))


if __name__ == '__main__':
    try:
        main()
    except (RuntimeError, OSError, subprocess.TimeoutExpired) as exc:
        print(str(exc), file=sys.stderr)
        sys.exit(1)
