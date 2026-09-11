# Codex on Android: Local ChatGPT Bridge

Run Codex CLI inside Termux on an Android phone and reach it from the ChatGPT app through an authenticated local bridge. The bridge lets a voice conversation in ChatGPT reach a Codex agent running on the same phone, which can then use Android control tools through Shizuku.

This is an independent community project and an experimental setup. It is not an official OpenAI product or an OpenAI-supported Android distribution.

## Why this exists

The project started with a Codex CLI installation on a headless VPS. The ChatGPT app could connect to that Codex instance with a verification code, making it possible to control the VPS from the phone even though the VPS had no graphical interface.

Before the Android setup could be bootstrapped, Developer options had to be enabled on the phone and USB debugging had to be authorized. Once ADB access from the computer was approved, ChatGPT/Codex on the computer could operate the phone, transfer the project, and install the required tools. Shizuku was installed and configured on the phone with that guidance.

That led to the next question: if the ChatGPT app can reach Codex on a remote machine, can the same pattern reach Codex running locally on the phone?

Codex CLI was installed in Termux. The local Codex process was then connected to Android control tools through `rish` and Shizuku. After the local Codex instance was added to the ChatGPT app through the authenticated bridge, the phone appeared as a local endpoint (`localhost`). Voice commands in the ChatGPT app could then reach the Codex process inside Termux, and that process could perform Android actions through the Shizuku bridge.

The resulting path is:

```text
ChatGPT app (voice)
        |
        v
Authenticated Codex bridge
        |
        v
Codex CLI in Termux (same Android phone)
        |
        v
rish -> Shizuku -> Android shell / Android APIs
```

## What has been verified

The proof of concept has been exercised on one non-root Android phone. The tested environment includes:

- Termux
- Shizuku
- the `rish` Shizuku shell bridge
- Codex CLI
- Node.js and npm
- Git and Python
- the ChatGPT app connection to the local Codex endpoint

The local `rish` bridge returned an Android `shell` identity during a read-only check. This confirms an authenticated shell-level bridge, not root access. Reproducibility across Android versions and device manufacturers has not yet been established.

## Prerequisites

The exact setup depends on the device and Android version. Expect to perform some steps manually:

1. An Android phone without root is sufficient for the non-root path.
2. Install Termux from a trusted source and allow the storage permission it needs.
3. Install Shizuku from an official distribution.
4. Enable Developer options and Wireless debugging, or use the ADB setup path from a computer.
5. Pair and start Shizuku, then authorize Termux when requested.
6. Install Codex CLI and the command-line tools needed by the project.
7. Connect the local Codex instance to the ChatGPT app using the normal authenticated bridge flow.

Do not publish a verification code, password, session token, API key, or private host details in this repository.

## Current scope

This repository documents and validates the setup. It does not claim that Android can enable every required system setting automatically. Developer options, wireless debugging, Shizuku startup, package installation, and sensitive permissions may still require explicit user action.

The repository includes a first setup assistant that checks the environment, explains what is missing, can open the Developer options screen when requested, and verifies each step. It guides the user rather than silently bypassing Android security. A graphical Android setup app can build on this behavior later.

## Try the read-only checker

From the repository directory in Termux:

```sh
bash scripts/check-environment.sh
bash scripts/diagnose.sh
```

For an interactive checklist that leaves security-sensitive actions to the user:

```sh
bash scripts/setup-assistant.sh
```

See [the setup guide](docs/setup-termux.md), [the architecture notes](docs/architecture.md), [compatibility notes](docs/compatibility.md), [troubleshooting](docs/troubleshooting.md), and the [demo script](docs/demo-script.md).

## Safety

Codex can execute commands with the permissions available to its bridge. Review commands before allowing them to run, keep Shizuku authorization limited to trusted applications, and never share authentication material in issues, screenshots, videos, or commits.

## Status

MVP on one device. The repository has setup documentation, compatibility notes, a read-only checker, a diagnostic report, a guided checklist, and a shell-syntax CI check. Broader device compatibility and a graphical Android setup app remain future work.
