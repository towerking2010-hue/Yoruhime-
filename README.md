# YoruHime Reborn

A clean-room Android rebuild of YoruHime, centered on Ayaka Tōjō (東条 絢香). The previous application module is excluded from the new Gradle build; this branch uses a new Compose UI and a separate application ID.

## Product vision

Ayaka is mature, elegant, caring, protective, emotionally intelligent, and warmly affectionate. Her default voice is composed, with occasional playful tsundere teasing. When the user is distressed, empathy takes priority over teasing.

Target device: Samsung Galaxy A31 / Android 12. The app must avoid continuous heavy rendering, background camera use, and unnecessary battery or memory consumption.

## Current clean architecture

- New Android module: `:reborn`
- Package: `com.yoruhime.reborn`
- Application ID: `com.yoruhime.reborn`
- Kotlin + Jetpack Compose + Material 3
- Local SQLite store for chat history and explicitly saved memories
- Android Keystore AES-GCM encryption for the OpenRouter API key
- Coroutine-based network requests on IO
- Dedicated build workflow: `.github/workflows/reborn-build.yml`

## Implemented in this branch

- New visual system, launcher icon, four separate destinations: Home, Memories, Care, Settings.
- Bundled offline VRM character stage using pixiv/VRoid AvatarSample_B (VRM 1.0), loaded locally through AndroidX WebViewAssetLoader and Three.js / @pixiv/three-vrm.
- Lightweight 30 FPS character rendering, subtle idle sway/breathing motion, automatic blinking, and tap-triggered happy expression. The current model is a licensed sample placeholder, not the final custom Ayaka design.
- Chat UI with separate user/assistant bubbles, history persistence, clear-chat action, and scroll-to-latest.
- Ayaka chat through the OpenRouter `openrouter/free` route when the user supplies a key.
- Offline conversational fallback when no key is configured or the network request fails.
- User-entered OpenRouter key encrypted with Android Keystore; remove-key control.
- Explicit user-created memories stored locally, editable by deletion, and supplied to AI context only as user-approved notes.
- Camera capture through Android's camera intent, local preview, and a separate user-confirmed action to send a resized photo to the free vision route.
- Speech recognition with runtime microphone permission.
- Android text-to-speech for reading the latest Ayaka reply.
- Focus timer with 15, 25, 45, and 60-minute presets.
- Daily mood check-in.
- Conversation-based bond level indicator.

## Not yet complete / must not be represented as complete

- Replace the licensed VRoid sample with a distinctive custom Ayaka model and validate the final appearance, model scale, expression mapping, and touch reactions on a physical Galaxy A31.
- Reliable device-tested image vision across free models (free model capabilities can vary).
- Bedtime notification scheduling and notification-permission flow.
- Persistent mood history and richer bond progression/rewards.
- Automated unit and UI tests for core flows.
- Physical-device QA on the Galaxy A31, including avatar rendering/performance, rotation, keyboard, permission denial, network failure, low-memory behavior, and background/foreground transitions.

## Non-negotiable constraints

- No paid AI fallback, subscriptions, credits, hidden charges, or automatic top-ups.
- Never commit API credentials.
- Camera only starts after an explicit user action; no background camera operation.
- Images are not sent until the user explicitly taps the photo-send action.
- Memory is user-controlled; never silently save personal details.
- Keep AI/network work off the main thread.
- No destructive migration or removal of the legacy project history.
- A green CI build is not proof of physical-device reliability.

## Build and artifact

The dedicated workflow runs:

```bash
gradle :reborn:testDebugUnitTest :reborn:assembleDebug
```

On success, it uploads `yoruhime-reborn-debug-apk`. The new application ID means it installs separately from the legacy app. Test on Android 12 before treating it as a release candidate.
