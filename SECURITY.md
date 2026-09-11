# Security

This project connects a conversational interface to a local coding agent and Android control tools. Treat every bridge as a privileged development connection.

## Never commit

- verification codes
- passwords or session tokens
- API keys or cookies
- private VPS addresses or credentials
- screenshots containing account or device secrets

## Review before running

Review commands that install packages, change settings, access files, send messages, or delete data. Shizuku is not root, but it can still expose powerful Android operations to an authorized application.

## Structured-UI companion

The companion accepts commands only on `127.0.0.1:8765` and requires a random 256-bit token on every request. Do not change the bind address to all interfaces, expose the port through ADB forwarding, Tailscale or a tunnel, or publish the token. Pair only through the visible in-app copy action and `android-ui pair-from-clipboard`.

Treat accessibility snapshots as sensitive because they may contain text from the visible screen. The service and client do not create logs by default; callers must not persist screen or typed content unless the user explicitly requests a safe artifact. Keep the phone locked when local control should be unavailable and disable the service when it is not needed.

The accessibility service does not grant root and must not be used to bypass lock screens, secure windows, permission dialogs or application protections. Consequential actions still require the user's action-time authorization.

## Reporting

Do not publish a secret or an exploit in a public issue. Remove sensitive material and report the problem privately to the repository owner.
