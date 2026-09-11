---
name: android-local-control
description: Operate Android locally from Codex in Termux. Use the authenticated accessibility companion for Hebrew and Unicode text or identified screen elements, and the existing Shizuku/rish tool for app launch, navigation, screenshots, gestures and Android shell tasks.
---

# Local Android control

You run on the phone. This skill routes between two complementary local tools:

- `android-ui`: authenticated accessibility inspection, element focus/click/scroll and Unicode `ACTION_SET_TEXT`.
- `scripts/phone-control` relative to this skill: existing Shizuku/rish shell tool. On the reference phone its full path is `/data/data/com.termux/files/home/.codex/skills/android-local-control/scripts/phone-control`; it may not be on PATH.

## Choose a tool

| Task | Preferred tool |
| --- | --- |
| Hebrew, English or mixed text in a supported editable field | `android-ui set-text NODE 'שלום Codex 123'` |
| Identify and click/focus/scroll an accessibility element | `android-ui inspect`, then `click`, `focus`, `scroll-forward` or `scroll-backward` |
| Open an app, Home/Back/Recents, screenshot or Android shell command | `phone-control` |
| UI XML, coordinate taps or swipes when usable accessibility elements are absent | `phone-control ui`, `tap`, `swipe`, verified against a fresh screenshot |
| ASCII text where explicitly suitable | `phone-control text ASCII_TEXT`; never use this for Hebrew or silently transliterate |

## Inspect, act, verify

Before using a backend, run its `status` command. Check availability and choose the policy for the requested operation. Accessibility and Shizuku availability are independent: failure of one does not prove failure of the other.

While locked, keep authenticated status checks and authorized Termux background work available. `android-ui inspect` returns `inspection_scope: status_only`, redacted metadata and no nodes, even if text was requested. This is a successful limited inspection, not lost connectivity. Do not interpret an empty node list as an empty app.

UI mutations (typing, node click/focus/scroll and coordinate navigation) require an unlocked, freshly verified target window. The companion returns `UI_REQUIRES_UNLOCK` for these actions while locked. Ask the user to unlock only when the requested operation actually needs screen interaction; do not stop unrelated local development, computations or already authorized background jobs just because the screen locked. Android may still suspend background processes; the companion does not manage or guarantee their lifetime. Shizuku shell access remains independently available within its Android permissions; do not use it to bypass lock protection or redirect UI actions to keyguard.

For accessibility actions, use `android-ui inspect` to select a fresh node by package, view ID, class, bounds and supported actions. Use `inspect --no-text` when text is unnecessary. IDs are short-lived; after any UI change or `STALE_NODE`/`STALE_WINDOW` response, inspect again before choosing a new target. Never retry a consequential action solely because its response was lost.

After acting, read a new snapshot or screenshot and check the intended result. An accepted click or scroll alone is not proof of its effect. For `set-text`, require exact readback verification; report unsupported fields or verification failures instead of substituting blind typing or another keyboard.

The existing shell tool supports `status`, `apps`, `open PACKAGE`, `home`, `back`, `recents`, `tap X Y`, `swipe X1 Y1 X2 Y2 [MS]`, `text ASCII_TEXT`, `screenshot`, `ui`, and `shell 'ANDROID SHELL COMMAND'`. The shell command runs as Android uid 2000, not as Termux or root. Screenshot paths are private local files; view the image before interpreting it and use actual device dimensions for coordinates.

After a phone-control task, including an incomplete task, return ChatGPT (`com.openai.chatgpt`) to the foreground with `phone-control open com.openai.chatgpt` and verify focus with `status`, unless the user requested a different final app. If the phone is locked or access is unavailable, explain why restoration was not possible.

## Temporary screen-awake lease

For an authorized task requiring UI interaction, when automatic screen-off could interrupt it, call `android-ui awake-start --seconds 120` while the phone is unlocked. Save the returned `result.lease_id`. A small non-touchable badge indicates the lease; it does not change the target app or keyboard. Inspect fresh nodes after starting it.

Renew with `android-ui awake-renew LEASE_ID --seconds 120` only while actively doing that UI task, before expiry. Use up to 600 seconds for a bounded long step; do not run an unbounded background renewal loop. Release with `android-ui awake-stop LEASE_ID` in cleanup on success, failure, interruption, or waiting for user input. Scripted sequences should use `try/finally` or a shell `trap` to release. Check `status.screen_awake.active` afterwards. The service expires an abandoned lease independently of the client.

This is a tool-driven workflow, not a native Codex task-completion hook. Never claim automatic integration with every Codex turn. Screen-off/manual lock revokes the lease, and stale lease IDs cannot renew it. Do not automatically reacquire after a user's manual screen-off; wait for normal unlock and renewed user intent. Do not wake/unlock, disable security, alter global timeouts, or acquire this lease for file/build/network-only background tasks. Screen-on uses battery and does not guarantee Android will preserve Termux or its network connection.

## Recovery and boundaries

- `CONNECTION_UNAVAILABLE`: the companion may be disabled. The user enables **Codex local UI control** in Android Accessibility settings. Do not bypass consent.
- `UNAUTHORIZED`: the user copies the token in the companion, then `android-ui pair-from-clipboard` stores it privately and clears the clipboard. Never display or commit the token.
- Shizuku requires its own running service and Termux authorization, and may need manual startup after reboot. Do not promise unattended recovery.
- Both device-control paths work locally without USB or Wi-Fi. The ChatGPT/Codex connection has separate network requirements.
- Keep screen content and typed text out of persistent logs. Request authorization for messages, purchases, deletions or account/security changes outside the current request.
- Preserve the keyboard, existing shell tool and unrelated services. Root-only private app data, lock bypass and protected captures remain outside these tools' permissions.
