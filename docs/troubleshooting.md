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

