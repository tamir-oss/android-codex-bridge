# Contributing

Issues and pull requests are welcome. This project is still a proof of concept, so reproducible device reports are especially useful.

## Before opening an issue

Run:

```sh
bash scripts/check-environment.sh
bash scripts/diagnose.sh
```

Copy the output, after removing account names, hostnames, IP addresses, tokens, and any other private information. Include the Android version, device manufacturer, and whether Shizuku was started through wireless debugging or a computer.

## Changes

- Keep setup steps explicit and reversible.
- Do not add code that bypasses Android security prompts.
- Do not commit credentials, verification codes, session tokens, or private screenshots.
- Test shell scripts in Termux where possible.
- Run `python3 -m unittest discover -s tests -v` for protocol changes.
- For companion changes, verify an APK signature and exercise the harmless built-in test activity before trying another app.
- Never add an unauthenticated or non-loopback control listener.
- Explain device-specific behavior in the documentation.
