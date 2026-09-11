# Codex on Android — Development & Device Control Without Root

A Codex environment running inside your Android phone through Termux, combining local development tools with device control through rish and Shizuku, without root.

Develop projects, run commands and local services, and interact with the phone from the same Codex environment. The ChatGPT bridge provides a conversational interface, including voice; setup and diagnostics help establish and maintain that environment.

The aim is to make the fullest practical use of Android control available without root, within the permissions granted on each device. The current proof of concept does not establish that every possible non-root capability has been implemented.

This is an independent community project and an experimental setup. It is not an official OpenAI product or an OpenAI-supported Android distribution.

## Why this exists

The project started with a Codex CLI installation on a headless VPS. The ChatGPT app could connect to that Codex instance with a verification code, making it possible to control the VPS from the phone even though the VPS had no graphical interface.

That led to the next question: if the ChatGPT app can reach Codex on a remote machine, can the same pattern reach Codex running locally on the phone?

Before the Android setup could be bootstrapped, Developer options had to be enabled on the phone and USB debugging had to be authorized. Once ADB access from the computer was approved, ChatGPT/Codex on the computer could operate the phone, transfer the project, and install the required tools. Shizuku was installed and configured on the phone with that guidance.

Codex CLI was installed in Termux. The local Codex process was then connected to Android control tools through `rish` and Shizuku. After the local Codex instance was added to the ChatGPT app through the authenticated bridge, the phone appeared under the connection label `localhost`. This label alone does not establish that all ChatGPT traffic stays on the device or that the connection works offline. Voice commands in the ChatGPT app could then reach the Codex process inside Termux, and that process could perform Android actions through the Shizuku bridge.

The resulting path is:

```text
ChatGPT app (conversation / voice)
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

## A local development and device-control environment

The intended experience is to work with Codex running on the phone: develop software, manage project files and processes, and perform authorized Android actions. Users can work in Termux or reach the connected environment through ChatGPT, including by voice.

In the original phone setup, this included opening apps, inspecting the screen, navigating a browser, and managing a local development server. These examples describe the existing proof of concept; this repository does not yet package all of those control tools for a fresh installation.

## What this repository currently provides

This repository contains documentation, an environment checker, diagnostics, and an interactive setup checklist. The checklist offers guidance and a settings-opening action; it is not an automatic installer or repair engine.

The next product work should package and verify the local Codex environment and its device-control tools, then make onboarding and connection recovery easier. A graphical companion can show environment health and guide setup.

Installation supports the product. Success means Codex can perform verified development tasks and authorized device actions on the phone, with the ChatGPT connection available as a way to work with it.

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

Android control proof of concept demonstrated on one device. This repository currently distributes documentation and setup diagnostics; a reproducible control-tool package, guided installation and recovery, and broader device testing remain future work.
