# Compatibility notes

Compatibility is not established for every Android device. Reports should include the Android release, API level, manufacturer, model, Termux source, Shizuku version, and startup method.

| Capability | Current status |
| --- | --- |
| Termux on Android | Verified on one device |
| Codex CLI in Termux | Verified on one device |
| rish bridge | Verified on one device |
| Shizuku without root | Verified on one device |
| ChatGPT app to local Codex bridge | Verified in the project setup |
| Android 11+ wireless debugging path | Documented; needs more device testing |
| Android 10 and below computer/ADB path | Documented; needs more device testing |
| Automatic setup on every manufacturer | Not claimed |

Known variables include battery optimization, vendor changes to Developer options, permission monitoring, and whether the device stops Shizuku after reboot. The setup assistant should report these conditions instead of silently changing them.


## Control extensions

See [control extensions](control-extensions.md) for recording usage, API compatibility, verification limits and the companion development handoff.
