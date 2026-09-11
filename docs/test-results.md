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
- Fifteen host-side protocol/token, awake-CLI validation/routing, migration-preservation and USB-state parser/retry tests.
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

## Temporary screen-awake lease (v0.3)

- Built and installed without adding Android permissions or changing timeout settings.
- Real-device start, renewal, concurrent-start rejection, wrong-ID rejection, explicit release, abandoned-lease expiry, stale renewal rejection and Unicode input with the overlay passed.
- Android WindowManager attributed its `mHoldScreenWindow` to `CodexTaskAwake`; USB stay-awake was off. This confirms the visible overlay, not a charger setting, held the screen.
- A 310-second idle run passed with the configured timeout unchanged at 300,000 ms and charger stay-awake disabled. Screen remained interactive/unlocked, then release removed the hold-screen window and ChatGPT foreground was verified. The idle interval was on Android Settings after leaving the harmless test screen; no account was used. The reusable test now explicitly returns to its own activity before the idle interval.
- Manual Power-button lock passed (2026-09-11): the lease was revoked, stale renewal and a new start while locked were rejected, authenticated status/redacted inspection remained available, UI mutation required unlock, and local Termux computation completed. Fresh Android diagnostics showed keyguard locked, `mHoldScreenWindow=null`, and the device dozing.
- Normal user unlock passed (2026-09-11): the lease did not reactivate automatically, the authenticated safe UI suite passed with fresh snapshots and Unicode entry/click/scroll, and ChatGPT foreground was verified. No service restart or new pairing was needed.
- The integration is skill/tool-driven with bounded expiry, not a built-in Codex task lifecycle hook.

## USB-disconnected verification

Disconnected-test preparation initially failed closed with `USB_STATE_UNAVAILABLE` while the cable was still attached: USB dumps were intermittently incomplete through the rish subprocess capture. The probe now filters the current state on Android before transport, accommodates whitespace trimming and briefly allows output delivery before process teardown. It retries at most five read-only samples and still fails if none is valid.

- The subsequent real unplugged run passed on 2026-09-11. The authenticated safe UI suite ran in Termux with USB reported disconnected both before and after its actions. Hebrew, English, mixed text, element click/scroll and stale/window checks passed; ChatGPT foreground was verified. The user reconnected the cable only to retrieve the result. This verifies local device-control operation without USB, not a new test of the ChatGPT remote connection or offline AI inference.

## Still unverified

- Broader application and manufacturer compatibility.
- Full phone reboot and unattended recovery of Shizuku/Termux; long-term Android background-process survival.
- The combined long screen-awake retention test with USB unplugged (retention and unplugged UI operation were tested separately).

Do not describe these remaining items as verified without dedicated evidence.
