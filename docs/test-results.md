# Companion verification status

Last updated: 2026-09-11.

Reference device: Nothing A059P (`Asteroids`), Android 16 / API 36, 1080x2392, non-root. The companion was installed over authorized USB ADB; runtime control is designed to stay local to the phone.

## Built

- Companion Android application and visible test activity.
- User-enabled accessibility service.
- Authenticated IPv4 loopback transport at `127.0.0.1:8765`.
- Termux `android-ui` JSON client and private token pairing.
- One `android-local-control` Codex skill routing accessibility and Shizuku operations. Migration preserves the existing shell executable and backs up previous instructions outside skill discovery.
- Reproducible local build/install scripts and uninstall instructions.

## Verified in practice

- APK build and v1/v2/v3 signature verification.
- Ten host-side protocol/token, migration-preservation and USB-state parser tests.
- Unified skill installed on the reference phone; the original `phone-control` SHA-256 remained identical and a fresh Shizuku status check succeeded. Previous skill instructions were archived outside active skill discovery.
- Service state, unlocked state and active package readback.
- Invalid-token rejection followed by successful authenticated reconnect.
- Manual accessibility-service disablement: `android-ui status` returned `CONNECTION_UNAVAILABLE` with connection refused.
- Manual service re-enable: the existing token authenticated successfully, the safe UI suite passed, and ChatGPT foreground was verified (`reconnected` phase, 2026-09-11).
- Structured discovery of text fields, buttons and supported actions.
- Exact Hebrew text: `שלום עולם`.
- Exact English text: `Hello Android`.
- Exact mixed text: `שלום Codex 123`.
- Button click followed by a fresh visible-result snapshot.
- Scroll followed by discovery of the bottom control.
- Stale-node rejection after the UI changed.
- Changed-window rejection after Android Settings replaced the test activity.
- Existing `phone-control status` after installation: Shizuku returned uid 2000 (`shell`), screen size, unlocked state and active window.

No real account, message, form submission, payment or deletion was used for testing.

## Still requiring a manual device transition

- Lock the phone, confirm the companion refuses control, unlock normally and confirm reconnect.
- Disconnect USB and run the safe test from Termux to prove the computer is not a runtime dependency.
- Broader application and manufacturer compatibility.

Do not describe an item in this final section as verified until the corresponding device transition has been observed.
