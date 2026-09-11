# Compatibility notes

Compatibility is not established for every Android device. Reports should include the Android release, API level, manufacturer, model, Termux source, Shizuku version, and startup method.

| Capability | Current status |
| --- | --- |
| Termux on Android | Verified on one device |
| Codex CLI in Termux | Verified on one device |
| rish bridge | Verified on one device |
| Shizuku without root | Verified on one device |
| ChatGPT app to local Codex bridge | Verified in the project setup |
| Accessibility companion build/install | Verified with Debian Android SDK tools and USB ADB |
| Structured UI inspection and click/scroll | Verified on Nothing A059P, Android 16/API 36 |
| Hebrew/English/mixed `ACTION_SET_TEXT` | Verified on companion test fields |
| Runtime after USB disconnect | Requires final on-device check; USB is not part of the design |
| Android 11+ wireless debugging path | Documented; needs more device testing |
| Android 10 and below computer/ADB path | Documented; needs more device testing |
| Automatic setup on every manufacturer | Not claimed |

Known variables include battery optimization, vendor changes to Developer options, permission monitoring, and whether the device stops Shizuku after reboot. The setup assistant should report these conditions instead of silently changing them.

The companion declares minSdk 23 and targetSdk 28 so it can be built with the minimal SDK available in the reference environment. Actual cross-version compatibility is not yet established. Apps may omit accessibility view IDs, suppress text, expose virtual elements, reject `ACTION_SET_TEXT`, or protect secure surfaces; the client reports these cases instead of falling back to blind typing.


## Control extensions

See [control extensions](control-extensions.md) for recording usage, API compatibility, verification limits and the companion test matrix.
