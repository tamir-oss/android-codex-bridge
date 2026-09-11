# Troubleshooting

## `rish` is missing

Confirm that Shizuku is installed and running, Termux is authorized inside Shizuku, and the `rish` bridge files were exported to Termux. Run the check again:

```sh
bash scripts/check-environment.sh
```

## `rish` returns an error or the bridge stops

Shizuku may have stopped after a reboot or may have been restricted by battery management. Start Shizuku again using the supported Android method, then re-authorize Termux if needed. Keep Developer options and the required debugging setting enabled while troubleshooting.

## ADB does not show the phone

Unlock the phone, verify USB debugging, accept the authorization dialog, and run `adb devices` again on the computer. Try a data-capable cable and a different USB mode if the device is listed as unauthorized or offline.

## Codex is installed but cannot reach Android

Check the chain in order: Codex command available, Termux process running, `rish` available, Shizuku running, and Termux authorized. The ChatGPT-to-Codex bridge is a separate connection and must also be authenticated.

## The ChatGPT app cannot see the local Codex instance

Confirm that the local Codex process is running and that the authenticated bridge was added to the same ChatGPT account. Re-enter the connection flow without sharing the verification code with anyone. Check that the endpoint is local to the phone and that no stale process is holding the port.

## Black screen or stale web content

Capture a screenshot, return to the normal view, and restart only the affected local process. Do not delete project files while diagnosing. Record Android version, device model, browser version, and the last safe action in an issue after removing private information.

## `android-ui` reports `CONNECTION_UNAVAILABLE`

Open the companion and confirm its status says the accessibility service is active. If not, open Android Accessibility settings and enable **Codex local UI control** manually. Android or vendor battery management may stop or disable services after an update or reboot; do not bypass the consent screen.

Confirm the client is installed with `command -v android-ui`. The service uses only `127.0.0.1:8765`, so Wi-Fi, Tailscale and ADB do not affect this local connection.

## `android-ui` reports `UNAUTHORIZED`

The app token and Termux token no longer match, often after token rotation or app data removal. In the companion, copy the pairing token, then run `android-ui pair-from-clipboard` in Termux. Do not display the token. The pairing command clears the clipboard after storing it privately.

## `STALE_NODE`, `STALE_WINDOW` or `WINDOW_CHANGED`

The screen changed after inspection. This is a safety rejection, not a reason to reuse the old node. Run `android-ui status`, inspect again, choose the element from the fresh result and retry only if the package and visible context are still correct.

## Text entry is unsupported or not verified

Some apps do not expose editable accessibility nodes or reject `ACTION_SET_TEXT`. Do not replace the keyboard or use blind coordinate typing as an automatic fallback. Focus the correct visible field, inspect again, and report the structured error if the app still lacks support.

## Remove only the companion

Disable its accessibility service first, then uninstall package `com.tamir.androidcodexbridge`. Remove `$PREFIX/bin/android-ui` and `~/.config/android-codex-bridge/token` only if desired. Keep the unified `android-local-control` skill, Termux, Shizuku, `rish` and the existing phone-control tool. The retired accessibility-only skill is backed up outside skill discovery in `~/.codex/skill-backups/`.


## Control extensions

See [control extensions](control-extensions.md) for recording usage, API compatibility, companion commands and verification limits.
