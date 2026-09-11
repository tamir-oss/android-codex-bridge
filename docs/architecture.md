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

## Structured-UI companion

The optional companion is a separate Android application with a user-enabled `AccessibilityService`. It complements the shell path for UI element discovery, supported accessibility actions and Unicode text entry:

```text
Codex in Termux
  -> android-ui client
  -> TCP 127.0.0.1:8765 with per-request token
  -> companion AccessibilityService
  -> fresh Android accessibility node
```

The listener binds explicitly to IPv4 loopback and is not reachable through Wi-Fi, cellular data, Tailscale or another device. Android requires the application to declare network permission even for this local socket. A random 256-bit token is generated in app-private storage; Termux receives it only after the user copies it in the visible companion screen. The client saves it as mode 0600 and clears the clipboard after pairing.

The service snapshots the active window, returns opaque short-lived node IDs, and records only their in-memory path and fingerprint. Before a UI mutation it verifies unlock state, snapshot generation, package, window, class, view ID and bounds. Locked status requests still succeed; locked snapshots contain only state metadata and no nodes or content. Termux background work is outside the companion's management. If focusing an editable field changes its bounds, the service re-identifies exactly one matching view ID before using `ACTION_SET_TEXT`. It then reads the field again to verify the result. Screen text and typed text are not written to logs by default.

The companion is removable and independent. Disabling its accessibility service or uninstalling the app leaves Termux, Codex, Shizuku, `rish`, the keyboard and the existing control tool intact.

## Initial bootstrap

The first setup used a computer. Developer options and USB debugging were enabled on the phone, the computer was authorized through ADB, and the computer-side ChatGPT/Codex session installed the project and command-line tools. Shizuku was then installed and configured on the phone, with ChatGPT guiding the setup.

## Remote-to-local progression

The design was discovered in two steps:

1. Codex CLI was installed on a headless VPS and reached from the ChatGPT app with an authenticated verification flow.
2. The same idea was applied locally by installing Codex in Termux and connecting it to Android control tools through `rish` and Shizuku.

## Permission boundary

The bridge runs with the permissions granted to Termux and Shizuku. Developer options, USB debugging, wireless debugging, pairing, and sensitive permissions remain Android-controlled steps. The project must not describe this path as root access or as a way to bypass Android security.


## Control extensions

See [control extensions](control-extensions.md) for recording usage, API compatibility, companion commands, build/install steps and verification limits.
