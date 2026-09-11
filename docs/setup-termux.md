# Termux setup guide

This guide describes the non-root path used by the proof of concept. Device menus differ between manufacturers. Follow Android's prompts and keep every authorization visible.

## 1. Bootstrap from a computer with ADB

The first setup used a computer to reach the phone:

1. Open **Developer options** on the phone.
2. Enable **USB debugging**.
3. Connect the phone to the computer and approve the ADB authorization prompt.
4. Confirm the device from the computer with `adb devices`.
5. Use the computer-side Codex session to transfer the project and install the command-line tools.

ADB is the bootstrap channel. It is separate from the later local `rish`/Shizuku bridge.

## 2. Install the Android components

- Install [Termux](https://termux.dev/en/).
- Install [Shizuku](https://shizuku.rikka.app/download/) from a trusted distribution.
- Start Shizuku using the method supported by the Android version and device. The [official Shizuku setup guide](https://shizuku.rikka.app/guide/setup/) describes wireless debugging on Android 11+ and the computer/ADB path for older unrooted devices.
- Authorize Termux inside Shizuku.
- Export or install the `rish` bridge according to the Shizuku instructions.

Wireless-debugging startup normally has to be repeated after a reboot because of Android system limits.

## 3. Install Termux tools

Run the following in Termux and review the package list before confirming:

```sh
pkg update
pkg install git nodejs-lts python openssh
termux-setup-storage
```

Install Codex CLI using the current official Codex documentation and the authentication method available to your account. Do not paste a password, verification code, session token, or API key into a public issue or commit.

## 4. Verify the local bridge

From the repository directory:

```sh
bash scripts/check-environment.sh
bash scripts/diagnose.sh
```

The expected non-root bridge test is an Android `shell` identity (uid 2000). A different identity or a missing response means that Shizuku or Termux authorization needs attention.

## 5. Connect the local Codex instance

Start Codex CLI in Termux, then add that Codex instance to the ChatGPT app using the normal authenticated bridge flow. The verification code belongs only in the private connection flow. Never put it in this repository.

## 6. What still requires the user

The assistant cannot safely or reliably enable Developer options, approve ADB, enter pairing codes, authorize Shizuku, or grant sensitive Android permissions without visible user action. This project guides those steps and verifies their result.

## 7. Add structured UI and Unicode text entry

This optional layer is installed alongside `rish`/Shizuku. Build and install the APK from an authorized computer:

```sh
bash scripts/build-companion.sh
bash scripts/install-companion.sh
```

On the phone, open the companion and use its button to open Accessibility settings. Explicitly enable **Codex local UI control** after reviewing Android's warning.

Then, from the repository in Termux:

```sh
bash scripts/install-termux-client.sh
bash scripts/install-codex-skill.sh
```

Copy the pairing token with the visible companion button and immediately run `android-ui pair-from-clipboard`. The command stores the token privately and clears the clipboard. Verify with `android-ui status`, then follow the inspect/action/inspect loop in [control extensions](control-extensions.md).

The skill installer consolidates both tools under the existing `android-local-control` skill and preserves its shell executable. It archives the older accessibility-only skill under `~/.codex/skill-backups/`. This is an upgrade path for an existing `phone-control` installation, not an installer for the original shell backend.

USB is used only for build/install/debug. Once the APK, client and skill are installed and paired, `android-ui` communicates entirely inside the phone over authenticated loopback.


## Control extensions

See [control extensions](control-extensions.md) for recording usage, API compatibility, companion commands and verification limits.
