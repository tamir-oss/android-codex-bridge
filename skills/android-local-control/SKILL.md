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

Before using a backend, run its `status` command. Confirm the device is unlocked, the backend is available and the intended app is active. Accessibility and Shizuku availability are independent: failure of one does not prove failure of the other. A locked phone must be unlocked normally by the user.

For accessibility actions, use `android-ui inspect` to select a fresh node by package, view ID, class, bounds and supported actions. Use `inspect --no-text` when text is unnecessary. IDs are short-lived; after any UI change or `STALE_NODE`/`STALE_WINDOW` response, inspect again before choosing a new target. Never retry a consequential action solely because its response was lost.

After acting, read a new snapshot or screenshot and check the intended result. An accepted click or scroll alone is not proof of its effect. For `set-text`, require exact readback verification; report unsupported fields or verification failures instead of substituting blind typing or another keyboard.

The existing shell tool supports `status`, `apps`, `open PACKAGE`, `home`, `back`, `recents`, `tap X Y`, `swipe X1 Y1 X2 Y2 [MS]`, `text ASCII_TEXT`, `screenshot`, `ui`, and `shell 'ANDROID SHELL COMMAND'`. The shell command runs as Android uid 2000, not as Termux or root. Screenshot paths are private local files; view the image before interpreting it and use actual device dimensions for coordinates.

After a phone-control task, including an incomplete task, return ChatGPT (`com.openai.chatgpt`) to the foreground with `phone-control open com.openai.chatgpt` and verify focus with `status`, unless the user requested a different final app. If the phone is locked or access is unavailable, explain why restoration was not possible.

## Recovery and boundaries

- `CONNECTION_UNAVAILABLE`: the companion may be disabled. The user enables **Codex local UI control** in Android Accessibility settings. Do not bypass consent.
- `UNAUTHORIZED`: the user copies the token in the companion, then `android-ui pair-from-clipboard` stores it privately and clears the clipboard. Never display or commit the token.
- Shizuku requires its own running service and Termux authorization, and may need manual startup after reboot. Do not promise unattended recovery.
- Both device-control paths work locally without USB or Wi-Fi. The ChatGPT/Codex connection has separate network requirements.
- Keep screen content and typed text out of persistent logs. Request authorization for messages, purchases, deletions or account/security changes outside the current request.
- Preserve the keyboard, existing shell tool and unrelated services. Root-only private app data, lock bypass and protected captures remain outside these tools' permissions.
