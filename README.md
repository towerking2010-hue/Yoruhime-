# YoruHime (夜姫)

**YoruHime** is a native Android companion app built with Kotlin and Jetpack Compose. Its companion is **Ayaka Tōjou (東条 絢香)**: a mature, caring, gently strict *mommy-tsundere* who balances warmth with occasional playful teasing.

The project targets mid-range Android devices, including the Samsung Galaxy A31 running Android 12.

## Project principles

- **Ayaka stays Ayaka.** Her core identity is warm, protective, emotionally perceptive, capable, and occasionally tsundere. Teasing never overrides a serious or distressed moment.
- **Free cloud only.** Cloud chat uses OpenRouter's `openrouter/free` router. YoruHime does not configure paid model fallbacks, subscriptions, purchases, or automatic top-ups.
- **Honest offline mode.** Local dialogue works without a network connection. It does not pretend to perform cloud-level reasoning or image analysis.
- **Privacy and control.** Chat history and explicit memory notes are stored locally. The OpenRouter key is encrypted with Android Keystore. Camera capture is user-triggered.
- **Preserve user data.** Room migrations are used; destructive migration fallbacks must not be added.
- **Performance first.** Avoid unnecessary background camera use, perpetual high-cost animation, and repeated WebView recreation.

## Current feature set

| Feature | Current implementation |
|---|---|
| Companion avatar | Existing bundled Live2D model rendered in a WebView using Pixi/WebGL, with expressions, idle motion, gaze and touch reactions |
| Chat | Jetpack Compose chat UI, Room-backed history, recent conversation context |
| Cloud AI | OpenRouter OpenAI-compatible chat API, strict `openrouter/free` routing |
| Offline chat | Local in-character response bank with explicit messaging when a request needs cloud reasoning |
| Memory | Explicit remember/forget commands, locally stored notes, review/edit/delete/clear controls |
| API key | User-supplied OpenRouter key encrypted at rest using AES-GCM and Android Keystore |
| Voice | Android TextToSpeech and speech-recognition input; TTS initializes lazily |
| Camera | CameraX preview and still-image capture; captured images can be sent for cloud vision |
| Focus | Pomodoro sessions (15, 25, 45, 60 minutes), break, interruption reaction and bond experience |
| Bond | Local progression and study/touch/chat experience |
| Bedtime | User-configurable daily WorkManager reminder, defaulting to 11:00 PM |
| Avatar direction | Renderer-neutral Ayaka animation director and Live2D adapter foundation |

## Important limitations (do not overclaim)

- The current avatar is **Live2D**, not a finished VRM/3D avatar. The animation director and adapter are a foundation; a final VRM model, renderer, animation assets and full model-driven motion system are not yet complete.
- The camera feature captures still images. It does **not** provide continuous video understanding, automatic fatigue/stress diagnosis, AR world tracking, depth occlusion, or a finished photo-compositing "Together Mode".
- Cloud vision depends on the free router having an eligible free model that accepts image input. If none is available or the provider is unavailable, the app must report that honestly and fall back without spending money.
- WorkManager is not an exact-to-the-minute alarm service. Android battery policies and notification permission affect delivery.
- CI builds and unit tests do not replace physical-device validation. Camera, microphone, TTS voice availability, WebGL rendering, Room upgrades, notification delivery and frame pacing must still be checked on a Galaxy A31 / Android 12.

## Build

The project uses Kotlin, Jetpack Compose, Android Gradle Plugin 8.9.0, Kotlin 2.1.10, Gradle 8.11.1 and JDK 21.

Run the CI-equivalent tasks from the repository root:

```bash
gradle testDebugUnitTest assembleDebug
```

The debug build uses the `.test` application ID suffix so it can be installed side-by-side with the existing production package. It is a separate app with separate local data; it does not inherit the installed app's chat history or memories.

- Production application ID: `com.aistudio.yoruhime.dpxmzt`
- Debug test application ID: `com.aistudio.yoruhime.dpxmzt.test`
- Minimum Android version: Android 8.0 (API 26)
- Target SDK: 36
- Java/Kotlin target: 21

## Configuration

1. Install the YoruHime Test debug APK.
2. Open Settings / API setup.
3. Add an OpenRouter API key supplied by the user.
4. Save it. The app encrypts the key locally.
5. Select Cloud when you want to attempt a free-router request. Offline mode remains available.

Creating or storing an API key does not require buying credits. Do not enable paid model fallbacks or automatic top-ups.

## Development status

The active integration work is tracked in [PR #11](https://github.com/towerking2010-hue/Yoruhime-/pull/11) on the `yoruhime/finish-integration` branch. Keep this PR in draft until the current CI run passes and the documented device checklist has been completed.

- [Device test checklist](docs/DEVICE_TEST_CHECKLIST.md)
- [Ayaka expressive avatar architecture](docs/AYAKA_EXPRESSIVE_AVATAR.md)
- [Pull requests](https://github.com/towerking2010-hue/Yoruhime-/pulls)
- [GitHub Actions](https://github.com/towerking2010-hue/Yoruhime-/actions)

**A successful CI build is not a release certification.** Do not describe any feature as device-verified unless it has actually been tested on hardware.
