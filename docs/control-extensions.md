# Non-root control extensions

## Bounded screen recording

Run from Termux:

```sh
python scripts/record-screen.py --seconds 10
```

The tool requires Python, `rish`, authorized Shizuku shell access, shared-storage access, and an unlocked phone. It records the visible screen without audio for 1–60 seconds at 720x1280, then stops automatically. It validates MP4 file-type, movie metadata and media-data boxes; this is structural validation, not a decoder/playback test. Secure app surfaces may remain black. Do not record credentials or publish private recordings.

Files are saved privately under `~/.codex/phone-artifacts/` with mode 600. A randomly named transfer is briefly created in shared Download storage and removed after copying. Errors are surfaced; root, network listeners and persistent background services are not used. An interrupted/lost bridge may require checking for a leftover transfer; screenrecord still has its time limit. The original phone-control script is unchanged. This device also has a `phone-record --seconds 10` shortcut.

A three-second real-device recording produced a finalized MP4. Duration validation rejects zero. Cross-device behavior and playback are not yet verified.

## Termux:API

Install the Android add-on from the same signing source as Termux, and the separate `termux-api` CLI package. Never uninstall a working Termux installation to resolve a signature mismatch.

This device's installed Termux signer matched the F-Droid Termux:API 0.53.0 signer; the GitHub candidate differed and was not installed. F-Droid 0.53.0 and CLI package 0.59.1-1 were installed without replacing Termux. Android Package Manager accepted the APK. `openssl-tool` was added to inspect certificates; existing packages were not upgraded.

Verified on this device: battery status returned JSON, sensor listing returned JSON, and TTS engine enumeration returned one engine. Sensor samples, audio playback and other API calls were not tested.

Useful low-impact checks:

```sh
timeout 15 termux-battery-status
timeout 15 termux-sensor -l
timeout 15 termux-tts-engines
```

Availability of an executable does not establish that every API works. Permission-dependent camera, location, microphone, contacts and SMS functions are not part of the verification. Grant only permissions needed for a requested function.

Official source and compatibility guidance: https://github.com/termux/termux-api
F-Droid distribution: https://f-droid.org/en/packages/com.termux.api/

## Hebrew input and structured UI companion

The companion under `companion/` is an isolated Android app with a visible test activity and an explicitly enabled accessibility service. It supplements the existing `rish`/Shizuku tool; it does not replace it or change the active keyboard.

### Build and install

The reference build uses Java, Android SDK platform 23, `aapt`, `dx`, `zipalign` and `apksigner`. Override their paths with the environment variables documented in `scripts/build-companion.sh` when the SDK layout differs.

```sh
bash scripts/build-companion.sh
bash scripts/install-companion.sh
```

The build creates a local development signing key under ignored `local/` storage and emits:

```text
companion/build/android-codex-bridge-companion.apk
```

On the phone, open the companion and tap **פתיחת הגדרות נגישות**. Android must show and the user must approve the accessibility service. The installer never enables it silently.

From the repository in Termux, install the client and Codex skill:

```sh
bash scripts/install-termux-client.sh
bash scripts/install-codex-skill.sh
```

There is one skill, `android-local-control`, routing both backends. The skill installer requires the existing `android-local-control/scripts/phone-control` tool and never overwrites it. It backs up the previous instructions under `~/.codex/skill-backups/`, installs the unified instructions, and moves the retired `android-structured-ui` skill outside skill discovery. Start a fresh Codex task to pick up the changed skill inventory; no backend restart is needed for a Markdown instruction change.

In the companion tap **העתק מפתח צימוד ל־Termux**, then immediately run:

```sh
android-ui pair-from-clipboard
android-ui status
```

The pairing command validates a 64-character hexadecimal token, stores it only in `~/.config/android-codex-bridge/token` with mode 0600, and clears the clipboard. Do not print or share the token.

### Commands

Inspect the current screen:

```sh
android-ui inspect
android-ui inspect --no-text
```

Every node includes an opaque ID, class, optional Android view ID, bounds, state and supported actions. Use an ID only from the latest inspection and act immediately:

```sh
android-ui set-text n123-4 'שלום Codex 123'
android-ui click n124-9
android-ui focus n125-2
android-ui scroll-forward n126-0
android-ui scroll-backward n127-0
```

The exact IDs above are examples; they deliberately expire when the UI changes. Before a UI-changing action, verify `android-ui status` reports `locked: false` and the intended package. After it, inspect again and verify the visible result. `--compact` produces one-line JSON for an agent. Accepted clicks and scrolls return `verified: false` until the caller inspects their visible result; command acceptance alone is not completion.

### Lock policy by operation

`status` remains available while locked. `inspect` also succeeds but returns only device/service state, `inspection_scope: status_only`, `redacted: true`, and `nodes: []`; it does not inspect underlying apps, notifications or credential fields. All current node mutations need an unlocked window and return `UI_REQUIRES_UNLOCK` otherwise. A lock observed during an action makes verification incomplete; inspect after normal unlock before deciding whether a retry is appropriate.

The companion does not stop or manage background work in Termux. Authorized file processing, development and background services can continue subject to Android scheduling and permissions. Shizuku remains an independent backend. This policy neither grants root nor bypasses Android keyguard. Full phone reboot and unattended background persistence are separate concerns.

`set-text` uses Android `ACTION_SET_TEXT`, so Hebrew, English and mixed text do not pass through `input text` and do not require replacing the keyboard. If the app does not expose an editable field with `ACTION_SET_TEXT`, the command returns `SET_TEXT_UNSUPPORTED`. Other important errors include `UI_REQUIRES_UNLOCK`, `NO_ACTIVE_WINDOW`, `STALE_NODE`, `STALE_WINDOW`, `FOCUS_CHANGED`, `UNAUTHORIZED` and `CONNECTION_UNAVAILABLE`.

### Temporary task screen-awake control

Companion v0.3 adds an authenticated, task-scoped lease. Use it only for screen tasks that need an unlocked UI; development and file-processing tasks do not need the display kept on.

```sh
android-ui awake-start --seconds 120
# Use result.lease_id from that response, not this placeholder:
android-ui awake-renew LEASE_ID --seconds 120
android-ui awake-stop LEASE_ID
android-ui status
```

Each start/renew accepts an integer 5–600 seconds (CLI default 120). Only one lease may exist; another start returns `AWAKE_LEASE_BUSY`. Renewal/release require the lease ID in addition to normal request authentication. Status reports active state and remaining milliseconds but not its ID. An expired/revoked/wrong ID returns `STALE_AWAKE_LEASE`. Start cannot wake a sleeping or locked phone.

The service owns a small, visible, non-focusable and non-touchable accessibility overlay with `FLAG_KEEP_SCREEN_ON`. It adds no permission, permanent setting, keyboard, network service, root or Shizuku dependency. The overlay is removed on explicit release, expiry, screen-off, lock detection, service interruption/destruction, or process death. A monotonic deadline and independent one-second watchdog handle an abandoned client. No lease survives restart. Manual lock is never dismissed. The indicator can visually cover a small part of an app; release when no longer needed.

The unified skill starts/renews/releases the lease around UI work; scripted workflows must release in `finally`/`trap`. This is not a native Codex completion hook: if the agent forgets cleanup or loses its connection, expiry is the fallback, not instant end-of-turn release. Do not continuously renew during idle/user-approval waits. Keeping a display lit consumes battery and does not guarantee Termux background lifetime.

Reference: [Android window flags](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_KEEP_SCREEN_ON). Android's ordinary activity `keepScreenOn` stops applying when that activity is backgrounded; this companion uses a visible accessibility window and requires real-device validation. Do not infer support on every vendor from a successful build.

Run `python scripts/test-awake-on-device.py` for start, renewal, ownership, Unicode entry with the overlay, release, expiry and stale-renewal checks. Optional `--idle-seconds 310` tests past the reference device's five-minute screen timeout without changing it. Do not touch the device during that idle interval. Manual screen-off/re-enable and USB-disconnected checks remain separate user-driven tests.

For manual lock precedence, start `python scripts/check-companion-lifecycle.py awake-locked --wait-seconds 120`, then press Power yourself within that interval. The probe verifies lease revocation, locked-start/renewal rejection and the operation-scoped lock policy. It never wakes or unlocks the phone.

### Security model

- The service binds only to `127.0.0.1:8765`; it is not exposed to LAN, cellular data, Tailscale or the public internet.
- Every request requires a random 256-bit token. Failed authentication is delayed and rejected.
- Android itself protects the exported accessibility service with `BIND_ACCESSIBILITY_SERVICE` and the user-facing consent flow.
- Node references are in memory only. They are tied to a snapshot generation, package, window and element fingerprint.
- Status and redacted metadata inspection remain available while locked. UI changes require unlock, with lock state rechecked immediately before dispatch. Lock observation invalidates cached node IDs.
- Screen content and text entered are not logged. Successful text responses return only character count and verification status.
- Token rotation is visible and confirmed in the companion screen. Rotating invalidates the Termux client until it is paired again.

Loopback plus authentication was chosen because Android 16 SELinux prevented Termux from connecting to the app's abstract Unix-domain socket in the reference device. A local TCP socket works across the app sandbox boundary while remaining unreachable from other machines.

### Test status

On the reference Nothing A059P running Android 16, the following passed on the companion's own harmless test screen:

- service state and active-window checks;
- invalid-token rejection and a following authenticated reconnect;
- structured field and button discovery;
- Hebrew, English and mixed `ACTION_SET_TEXT` with exact readback;
- button click with fresh visible-result verification;
- scrolling followed by fresh inspection;
- stale-node rejection and changed-window rejection;
- multiple independent client connections;
- the pre-existing Shizuku `phone-control status` check after installation.

No real account, form submission or message was used. Manual service disablement returned connection refused. After the user re-enabled it, the existing token reconnected and the safe UI suite passed. The remaining manual transitions are normal lock/unlock and the final run after unplugging USB. They must be recorded as untested until exercised; the design has no ADB or computer dependency at runtime.

Run the safe on-device test from the repository in Termux:

```sh
python scripts/test-companion-on-device.py
```

Lifecycle checks are explicitly user-driven; they never toggle accessibility, lock or unlock the device:

```sh
python scripts/check-companion-lifecycle.py disabled
# After manually re-enabling the service:
python scripts/check-companion-lifecycle.py reconnected
# Start, then lock normally within 120 seconds; do not unlock until results are checked:
python scripts/check-companion-lifecycle.py locked --wait-seconds 120 \
  --report "$HOME/.codex/phone-artifacts/companion-lock-test.json"
# After normal user unlock:
python scripts/check-companion-lifecycle.py unlocked
# Start, then unplug within 120 seconds; keep the phone unlocked:
python scripts/check-companion-lifecycle.py usb-detached --wait-seconds 120 \
  --report "$HOME/.codex/phone-artifacts/companion-usb-test.json"
```

The last check reads the current Android USB connection state through local Shizuku before and after running the harmless test suite inside Termux. Unknown USB state is a failure, never assumed to mean disconnected. Reports contain result labels only. After unlocked tests the runner attempts to restore ChatGPT and reports whether the foreground was verified. This does not test or require a full phone reboot.

### Disable and remove

1. In Android Accessibility settings, turn off **Codex local UI control**.
2. Uninstall the Android app normally, or from an authorized computer run `adb uninstall com.tamir.androidcodexbridge`.
3. In Termux, remove only `$PREFIX/bin/android-ui` and `~/.config/android-codex-bridge/token` if no longer wanted. Keep the unified `android-local-control` skill and its existing shell tool; it remains useful with Shizuku alone. Previous skill instructions are recoverable from `~/.codex/skill-backups/`.

These steps do not remove Termux, Codex, Shizuku, `rish`, the existing phone-control skill or the keyboard.

Android action reference: https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo.AccessibilityAction
