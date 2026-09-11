# Demo and video script

This sequence demonstrates the idea without exposing authentication material.

## Recording checklist

1. Show the Android phone and state that it is not rooted.
2. Show Developer options and USB debugging only as a general setup step. Do not show pairing codes or account prompts.
3. Open Termux and run `bash scripts/check-environment.sh`.
4. Show the successful `rish`/Shizuku check and explain that the result is Android `shell`, not root.
5. Show Codex CLI installed in Termux.
6. In the ChatGPT app, speak a harmless request such as asking Codex to list the project directory.
7. Show the request reaching the local Codex instance and the resulting read-only output.
8. Explain the path: ChatGPT voice → authenticated bridge → local Codex → Termux → rish/Shizuku → Android.
9. On the companion's test screen only, inspect the UI and enter harmless Hebrew text with `android-ui set-text`, then show the fresh verification result.
10. Explain that the accessibility path is local-only, separately authenticated and removable.
11. End with the limitations: setup and sensitive permissions still require explicit user action.

## Do not record

- verification codes
- passwords, tokens, API keys, cookies, or private hostnames
- personal notifications or private files
- destructive commands
- a claim that this is official OpenAI support


## Control extensions

See [control extensions](control-extensions.md) for recording usage, API compatibility, verification limits and the companion development handoff.
