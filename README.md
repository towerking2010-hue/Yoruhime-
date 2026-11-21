# YoruHime Reborn

A clean-room Android companion app centered on Ayaka Tōjō (東条 絢香). The legacy application is preserved separately; Reborn uses its own \`:reborn\` module and application ID (\`com.yoruhime.reborn\`).

## Product direction

**Ayaka is the interface.** The Home destination is an immersive, full-screen character stage. Chat is a temporary bottom sheet that appears only when requested; the text keyboard appears only when the user focuses the composer. There is no permanently visible half-screen chat panel.

Ayaka's personality remains mature, elegant, caring, protective, emotionally perceptive and warmly affectionate. Her default voice is composed, with occasional playful tsundere teasing. When the user is distressed, empathy takes priority over teasing.

Target device: Samsung Galaxy A31 / Android 12. Keep rendering light, avoid background camera use, and minimize battery, memory and thermal load.

## Architecture

- Native Android shell: Kotlin, Jetpack Compose and Material 3.
- Immersive Home surface: full-size local WebView avatar stage with native Compose controls and on-demand conversation sheet.
- Avatar runtime: Vite-built Three.js + \`@pixiv/three-vrm\`, bundled locally and served through AndroidX WebViewAssetLoader.
- Avatar diagnostics: JavaScript-to-native status bridge, visible loading/error state, WebView render-process recovery and retry.
- Conversation: coroutine-based OpenRouter \`openrouter/free\` requests; network work runs off the UI thread.
- Local data: SQLite chat history and explicitly saved memories.
- Secret storage: OpenRouter key encrypted with Android Keystore AES-GCM.
- Voice input/output: Android SpeechRecognizer and Android TextToSpeech.
- Camera: user-initiated Android camera capture; local preview and a separate explicit confirmation before any image upload.
- Care tools: focus timer, mood check-in, bond counter and user-scheduled bedtime reminder.

## Current avatar status

The bundled VRM is pixiv's \`AvatarSample_B\`, a temporary licensed sample—not a custom Ayaka model. The runtime currently provides basic idle motion, blinking and a tap-triggered happy expression. It does not yet provide Ayaka's final character design, a rich motion library, expressive dialogue-to-body action selection or a custom voice.

The avatar stage is now full-screen. The WebView reports startup and failure status to the native layer rather than silently leaving an empty stage. This diagnostic feedback is intended to make the next Galaxy A31 test actionable; it is not a claim that the model has been verified on that phone.

## Reference projects and what we adapt

### Project Riko
Repository: https://github.com/rayenfeng/riko_project (MIT)

Useful ideas: separate speech / brain / voice / avatar responsibilities; one clear conversational turn pipeline; configurable character personality; VRM camera framing based on the avatar's actual head/body bounds; lip-sync as a distinct presentation concern.

We adapt the architecture, not Riko's character identity or voice. The public repository is a PC-oriented Python project, so its desktop audio dependencies are not copied into the Android app.

### OpenSakura
Repository: https://github.com/zeekk0/OpenSakura (MIT harness)

Useful ideas: emotion state as structured data rather than a single mood label; emotion decay over time; per-user memory notes with explicit ownership; a separate action controller that maps a reply's tone to a small set of named motions; a clean boundary between the companion brain and avatar presentation.

We adapt those patterns to Ayaka and Android. Sakura's character design, name, likeness, voice model and voice assets are not included or reused. Its GPU-heavy Windows voice stack is not suitable for the Galaxy A31 target.

### Original YoruHime identity
Riko and OpenSakura are technical/personality references, not templates to copy. Ayaka keeps her own identity, boundaries, dialogue style and visual design.

## Implemented in this branch

- New Compose design system and separate Home / Memories / Care / Settings destinations.
- Full-screen avatar-first Home experience; conversation opens as an on-demand bottom sheet.
- Persistent local chat history, separate user/assistant bubbles, clear-chat control and scroll-to-latest.
- OpenRouter \`openrouter/free\` route only when the user supplies a key.
- Lightweight offline conversational fallback when no key is configured or a cloud request fails. This is not a full local language model.
- User-entered API key encrypted with Android Keystore; remove-key control.
- Explicit user-created memories, deletion and approved-memory context injection.
- Camera capture, local preview and explicit confirmation before image upload.
- Runtime-permission-gated speech recognition and Android TTS.
- 15/25/45/60-minute focus timer, daily mood check-in and conversation-based bond counter.
- Daily bedtime notification scheduling with inexact alarms, notification permission handling, cancellation and reboot restoration.
- Dedicated GitHub Actions workflow for unit-test task and debug APK assembly.

## Not yet release-ready

- Distinctive custom Ayaka VRM with verified redistribution rights.
- More facial expressions, idle variations, touch reactions, emotion-to-expression mapping and dialogue-to-motion action selection.
- A consistent Ayaka voice that is free to use and licensed for redistribution.
- Verified vision support across whichever free model OpenRouter routes to.
- Persistent mood history and a more meaningful, user-controlled relationship progression.
- Automated unit/UI tests and physical Galaxy A31 QA.
- Device validation of WebGL rendering, model scale/framing, keyboard and bottom-sheet behavior, rotation, permissions, low-memory recovery, background/foreground transitions and battery use.

## Non-negotiable constraints

- No paid AI fallback, subscriptions, credits, hidden charges or automatic top-ups.
- Never commit API credentials.
- No background camera operation; camera starts only after explicit user action.
- Never send a photo until the user confirms.
- Never silently save personal details; memories remain user-controlled.
- Keep network work off the main thread.
- Preserve the legacy project and its data; do not force a destructive migration.
- A successful CI build is not proof of physical-device reliability.

## Build

The workflow runs:

\`\`\`bash
gradle :reborn:testDebugUnitTest :reborn:assembleDebug
\`\`\`

The resulting debug APK is uploaded as the \`yoruhime-reborn-debug-apk\` workflow artifact. The application ID is \`com.yoruhime.reborn\`, so it installs separately from the legacy app. Test on Android 12 before treating it as a release candidate.

See [AYAKA_ARCHITECTURE.md](AYAKA_ARCHITECTURE.md) for the detailed interaction and component design.
