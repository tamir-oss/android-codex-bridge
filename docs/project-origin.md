# Project origin

The idea began with a practical problem: a VPS had no graphical interface, so Codex CLI was installed there. The ChatGPT app could connect to that Codex instance by using a verification code, and the VPS could then be controlled from the phone.

That raised a second question: if the phone can reach Codex on the VPS, can Codex also run inside the phone and be reached through the same kind of bridge?

The Android bootstrap started by enabling Developer options and USB debugging. After the computer was authorized through ADB, ChatGPT/Codex on the computer could control the phone and install the project and command-line tools. Shizuku was installed and configured on the phone with that assistance.

Codex was installed in Termux. Termux was connected to Android through `rish` and Shizuku. The local Codex instance was then added to the ChatGPT app through the authenticated bridge. Because the app and Codex were on the same phone, the local endpoint appeared as `localhost`.

The result is a voice-driven path from the ChatGPT app to Codex inside Termux, and from Codex through `rish` and Shizuku to Android. This repository documents that proof of concept and the plan to make setup easier for other users.

