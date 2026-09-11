# Architecture

## Runtime path

```text
ChatGPT app (voice)
        |
        v
Authenticated Codex bridge
        |
        v
Codex CLI in Termux
        |
        v
rish -> Shizuku -> Android shell / Android APIs
```

The ChatGPT app is the conversational and voice interface. The authenticated bridge exposes the selected Codex instance to that app. In this project the selected instance runs locally in Termux on the Android phone, so the endpoint appears as `localhost`.

Codex runs inside Termux and uses the tools available to its process. `rish` is the Shizuku shell bridge exported to Termux. Shizuku provides authorized Android operations without requiring root; it does not turn the device into a rooted device.

## Initial bootstrap

The first setup used a computer. Developer options and USB debugging were enabled on the phone, the computer was authorized through ADB, and the computer-side ChatGPT/Codex session installed the project and command-line tools. Shizuku was then installed and configured on the phone, with ChatGPT guiding the setup.

## Remote-to-local progression

The design was discovered in two steps:

1. Codex CLI was installed on a headless VPS and reached from the ChatGPT app with an authenticated verification flow.
2. The same idea was applied locally by installing Codex in Termux and connecting it to Android control tools through `rish` and Shizuku.

## Permission boundary

The bridge runs with the permissions granted to Termux and Shizuku. Developer options, USB debugging, wireless debugging, pairing, and sensitive permissions remain Android-controlled steps. The project must not describe this path as root access or as a way to bypass Android security.

