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

## Hebrew input and structured UI companion — pending

No companion accessibility APK has been built or enabled. Use a laptop with an Android SDK and authorized USB ADB for its build/install/debug cycle. Runtime must remain usable after USB is disconnected. Do not replace the existing rish tool or alter the current keyboard as a shortcut.

Implementation handoff:

1. Build an isolated companion with a visible test activity containing Hebrew/English text fields and buttons. Define an authenticated local-only command transport before exposing control; reject unauthorized callers and do not create an unauthenticated exported receiver or HTTP endpoint.
2. Add an explicitly enabled AccessibilityService that returns structured nodes and supported actions, supports ACTION_SET_TEXT on eligible editable nodes, and reports unsupported operations. Avoid storing screen content or typed text in logs.
3. Address nodes by current window and node identity, revalidate focus before every action, and reject stale nodes, locked screens and unsupported actions. Report completion only after fresh inspection.
4. Preserve existing enabled services and the chosen keyboard. Provide a clear disable/uninstall path. Accessibility enablement must follow Android's user-facing consent flow.
5. Verify Hebrew, English and mixed text, wrong/stale focus, service disabled, reconnect, lock/unlock and USB disconnect on the test activity before using another app. Never send a message as a typing test.
6. Run existing Shizuku status/UI/screenshot checks before and after. Publish source, reproducible build steps and actual test results before calling the companion complete.

Android action reference: https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo.AccessibilityAction
