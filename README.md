# YoruHime Reborn

YoruHime Reborn is an Android companion app centered on Ayaka Tōjō (東条 絢香). The active Android application is the `:reborn` module with application ID `com.yoruhime.reborn`.

## Product direction

**Ayaka is the experience.** The Home screen is designed around a full-screen character stage. Conversation opens on demand rather than occupying a permanent split-screen panel.

Ayaka's character is mature, elegant, caring, protective and emotionally perceptive, with occasional playful tsundere teasing. Empathy takes priority when the user is distressed. Riko and MekaHime are inspirations for selected interaction qualities, not replacements for Ayaka's identity.

Target device: Samsung Galaxy A31 / Android 12. Performance, memory use, heat and battery consumption must be validated on the actual device.

## Current architecture

- **Android UI:** Kotlin, Jetpack Compose and Material 3.
- **Avatar rendering:** Native Android `SurfaceView` backed by Google Filament 1.75.0 and gltfio. The active renderer does not use WebView or Three.js.
- **Character source:** The creator-provided Ayaka VRM 1.0 model is stored at `models/ayaka/Ayaka_Tojou 2.vrm`.
- **Model preparation:** `scripts/prepare-ayaka-glb.mjs` converts the VRM GLB container into a Filament-compatible `ayaka.glb` and writes preserved VRM extension data to `ayaka-vrm-metadata.json` in the generated Android assets directory.
- **Conversation:** Kotlin conversation engine with Ayaka persona instructions, local chat history, optional OpenRouter `openrouter/free` requests, and a basic rule-based offline fallback.
- **Local data:** SQLite chat, explicitly saved memories and mood check-ins.
- **Secret storage:** User-supplied OpenRouter key encrypted with Android Keystore AES-GCM.
- **Voice:** Android SpeechRecognizer and TextToSpeech integrations.
- **Care tools:** Focus timer, mood check-in, bond indicator and bedtime reminders.
- **Build:** Gradle and GitHub Actions.

## Ayaka model and permissions

The model is the actual Ayaka VRM supplied by its creator, epic_noobster. Its SHA-256 is:

`66b0944b3be41da8efe78909340bb64b31afa7262776b07679ce0b2fd1d2a601`

See [models/ayaka/MODEL-CREDITS.md](models/ayaka/MODEL-CREDITS.md). Preserve the embedded VRM metadata and creator attribution. The creator's stated terms are author-only and personal/non-profit use, with credit required; do not treat this as permission for commercial use.

The conversion preserves core geometry, skinning, morph targets, textures, supported KHR glTF extensions, and VRMC metadata in a sidecar. Native MToon fidelity, semantic humanoid mapping, spring-bone simulation, named expression mapping and animation drivers remain future work.

## Privacy and cost constraints

- No paid AI fallback, subscriptions, automatic credit purchases or hidden top-ups.
- Cloud AI is attempted only when the user supplies an API key; the app does not include credentials.
- Without a key, the offline fallback is rule-based, not a full local language model.
- Camera use must be user-initiated. No background camera operation.
- A captured image must not be uploaded without explicit user confirmation.
- Long-term memories must remain user-controlled; do not silently save personal details.

## Build and validation

The Reborn workflow runs the VRM converter regression test, verifies the original model fingerprint, prepares the GLB and metadata sidecar, runs Android unit-test tasks and Lint, assembles the debug APK, checks alignment/signature, and uploads the APK artifact.

The build workflow is not a device test. A green CI run does **not** prove that the app launches, that Ayaka is visible, or that the renderer is stable on a Galaxy A31.

### Build locally

```bash
gradle :reborn:testDebugUnitTest :reborn:assembleDebug
```

The debug APK is produced at `reborn/build/outputs/apk/debug/reborn-debug.apk`.

## Current known gaps

- The app has a launch crash reported on the target phone; root cause is still under investigation.
- Filament model visibility and framing have not yet been confirmed on the Galaxy A31.
- VRM expressions, idle animation, spring-bone physics and lip synchronization are incomplete.
- Offline conversation is basic and is not equivalent to a local LLM.
- Cloud AI requires the user's own key and depends on availability of the free route.
- Voice, camera vision, AR compositing and care tools still require runtime validation.
- Galaxy A31 performance and thermal behavior have not been measured.

## Release gates

Do not call a build release-ready until it has been installed and tested on the target Android 12 device. Verify app startup, Ayaka's first visible frame, model scale/orientation, chat, no-key offline mode, voice permissions, camera confirmation, notifications, background/foreground recovery, memory use and thermal behavior.
