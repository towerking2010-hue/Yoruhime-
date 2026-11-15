# YoruHime

YoruHime is a native Android companion app built around **Ayaka Tōjou**: a mature, caring, gently strict tsundere companion with a playful side. The app keeps the existing Live2D character as its current renderer and uses a free-only cloud route with a lightweight offline fallback.

> **Project status:** This branch is an integration and validation branch. A successful CI build proves compilation, unit tests, and APK assembly only; camera, voice, Live2D rendering, database migration, and performance still require real-device checks.

## Product principles

- Ayaka remains the same character: warm, protective, emotionally perceptive, confident, and occasionally flustered or teasing.
- Playful Riko-inspired tsundere behaviour is a situational mode, not a replacement personality. Distress cues take priority over teasing.
- Cloud AI must remain **free-only**. The app uses OpenRouter's `openrouter/free` router and does not configure paid model fallbacks, purchases, or automatic top-ups.
- Offline responses are intentionally limited scripted responses. YoruHime must not pretend that offline mode provides full LLM reasoning or image understanding.
- Preserve local chat, memory, bond, and schedule data during upgrades.
- Keep the app usable on mid-range Android hardware, including the Samsung Galaxy A31.

## Current application

| Item | Value |
|---|---|
| Platform | Native Android |
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| Application ID (installed production package) | `com.aistudio.yoruhime.dpxmzt` |
| Debug test package | `com.aistudio.yoruhime.dpxmzt.test` |
| Minimum Android | API 26 |
| Compile / target SDK | 36 |
| Java / Kotlin JVM target | 21 |
| App version in this branch | 2.0.0 (versionCode 2) |
| Local database | Room |
| Avatar renderer | Bundled Live2D in WebView / Pixi |
| Camera | CameraX |
| Cloud provider | OpenRouter free-model router |

## Features

### Companion and avatar
- Live2D stage with the bundled Tsubaki model, touch zones, idle motion, speech bubble, and six registered expressions: normal, smile, blush, happy, sad, and angry.
- The renderer-neutral `AyakaAnimationDirector` selects semantic face, gaze, head, body, and gesture cues.
- The current Live2D adapter maps supported cues to actual model expressions, gaze focus, and subtle head/body offsets.
- Android TTS speaking state drives a subtle mouth-open animation; this is utterance-level movement, not phoneme-accurate lip-sync.
- After 45 seconds of quiet on the Companion tab, Ayaka makes one restrained idle gaze/head adjustment rather than running extra high-cost animations continuously.
- Conversation cues can switch between Ayaka's caring mode and a brief Riko-inspired tsundere mode. Distress cues override playful cues.
- The current model only includes an idle motion clip. Arm/hand gestures such as waving are not implemented by this rig. A VRM/VRMA renderer and a new Ayaka model are future work, not current features.

### Chat and AI
- Chat history is stored locally in Room.
- Cloud chat sends recent conversation context (up to 12 messages) to OpenRouter.
- The only configured model route is `openrouter/free`. Free providers may be rate-limited, unavailable, or unsuitable for a particular request.
- Image requests require an eligible free model that supports image input. If no such model is available, the app reports the failure and does not switch to a paid model.
- Offline dialogue covers a limited set of common intents. Complex questions receive an honest offline limitation message.
- Quick replies and all conversation paths still require real-device regression testing.

### Memories and privacy
- The user can explicitly save a note by saying `Remember that ...` or remove one by saying `Forget that ...`.
- Saved notes can be reviewed, edited, deleted individually, or cleared.
- Memory notes remain in the local Room database. When Cloud chat is enabled, up to 12 notes may be included in the OpenRouter request for context.
- The OpenRouter API key is encrypted with AES-GCM using a key held in Android Keystore. The key is not embedded in the app.
- Clearing chat history does not clear saved memories or bond progress.

### Voice
- Replies use Android's installed Text-to-Speech engine.
- Speech rate and pitch vary by the reply's emotion tag.
- The first TTS initialization is lazy; the first utterance is queued while the engine starts.
- Chat voice input uses the Android speech-recognition service. Availability and language support depend on the device's installed services.
- This is system TTS, not a custom cloned Ayaka voice.

### Camera and photos
- CameraX provides a user-triggered preview dock with front/rear camera switching and still-image capture.
- Android Photo Picker can attach a selected image without broad storage permission.
- Images are downsampled before upload to limit memory use.
- Camera preview and snapshot capture do not require an API key. Cloud image analysis requires Cloud mode, a saved key, internet, and a free image-capable model.
- Offline mode never claims to have inspected an image.
- The full-screen camera background is a camera feed behind the Live2D overlay. It is **not spatial AR**: no world tracking, plane detection, depth occlusion, or world-anchored placement is implemented.
- Continuous live-video understanding and always-on camera monitoring are not implemented.

### Focus and care
- Focus timer presets: 15, 25, 45, and 60 minutes, followed by a five-minute break.
- The active focus session is kept as one shared composable while navigating between tabs. Leaving YoruHime during a focus block pauses it and should trigger Ayaka's return reaction.
- Focus progress contributes study minutes and bond experience.
- WorkManager schedules a daily bedtime/curfew notification. The default is 11:00 PM; custom time and phrase are stored locally.
- WorkManager delivery is best-effort and can be delayed by Android battery restrictions. This is a reminder, not a device lock.
- Timer process-death restoration is not yet implemented.

### Workspace
- **Companion:** Ayaka, conversation, camera, and focus overlay.
- **Memories:** review, edit, and delete explicit memory notes.
- **Care:** bond progress, study time, focus session entry, and bedtime reminder.
- **Settings:** cloud/offline selection, API-key setup, voice mute, and manual expression selection.

## Build and validation

The GitHub Actions workflow uses JDK 21, Gradle 8.11.1, and runs:

```bash
gradle testDebugUnitTest assembleDebug
```

The debug APK output path is:

```
app/build/outputs/apk/debug/app-debug.apk
```

The debug variant adds the `.test` application ID suffix and the `YoruHime Test` label. It is intended to install alongside the existing production app without replacing its package or app data. The test package has separate storage and will not contain production chat or memory data.

A successful CI run does **not** prove that the app launches on a Galaxy A31 or that camera, Live2D/WebGL, TTS, speech recognition, notifications, or migrations work on the device. Complete the checklist in [docs/DEVICE_TEST_CHECKLIST.md](docs/DEVICE_TEST_CHECKLIST.md) before treating a build as release-ready.

## Current limitations and planned work

- Select or create a legally redistributable Ayaka VRM model. A free download is not automatically licensed for bundling in an APK.
- Prototype a VRM 1.0 renderer with Three.js and three-vrm, then profile it on the Galaxy A31 before replacing or supplementing Live2D.
- Add actual VRMA clips and model-specific expression/morph mappings only after a suitable model and license are confirmed.
- Build Together Mode as a camera preview with a movable/resizable avatar overlay and a real composite photo export. This is not implemented yet.
- Persist focus timer state across process death if that behaviour is desired.
- Complete physical-device validation, UI accessibility checks, Room migration tests, and camera lifecycle tests.
- Audit all bundled Live2D runtime/model asset licenses and attribution requirements before public redistribution.

## Repository workflow

- Work on `yoruhime/finish-integration` while completing integration and validation.
- Keep pull request #11 in draft until the source review and CI are complete.
- Do not merge into `main` merely because an APK assembles.
- Preserve the production package ID and existing user data.
- Never add paid model IDs, paid fallbacks, purchase flows, or auto-top-up behaviour.

## Key resources

- [OpenRouter API documentation](https://openrouter.ai/docs)
- [OpenRouter free router](https://openrouter.ai/openrouter/free)
- [Android CameraX](https://developer.android.com/training/camerax)
- [Android WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Android Keystore](https://developer.android.com/privacy-and-security/keystore)
- [Three.js](https://threejs.org/)
- [three-vrm](https://github.com/pixiv/three-vrm)
- [VRM specification](https://vrm.dev/en/)
- [VRoid Studio](https://vroid.com/en/studio)
