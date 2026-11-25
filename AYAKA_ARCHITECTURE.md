# YoruHime Reborn — Ayaka architecture

This document describes the active Reborn implementation and the intended product direction. Planned behavior is not presented as completed functionality.

## 1. Product experience

Ayaka is the centre of YoruHime. The Home destination is an immersive character stage; conversation opens on demand as an overlay. Secondary destinations provide Memories, Care and Settings without permanently splitting the home screen.

Ayaka's personality is mature, elegant, caring, protective and emotionally perceptive. She can be playfully tsundere, but teasing is occasional. Empathy and clarity take priority in serious situations. Riko and MekaHime inform selected design qualities only; Ayaka keeps her own identity.

## 2. Android application structure

- `:reborn` is the sole included Android application module.
- Kotlin and Jetpack Compose own navigation, screens, chat, settings and care tools.
- `AyakaAvatar.kt` hosts the native Filament avatar surface.
- `RebornEngine.kt` contains local storage, API-key handling and conversation logic.
- `BedtimeReminder.kt` manages scheduled bedtime notifications.

The old WebView/Three.js renderer and the former Live2D/Tsubaki prototype are not part of the active Reborn build.

## 3. Native avatar pipeline

Source model: `models/ayaka/Ayaka_Tojou 2.vrm`.

Build-time flow:

1. Verify the source model's SHA-256 against the expected creator-supplied file.
2. Run `scripts/prepare-ayaka-glb.mjs`.
3. Write `reborn/src/main/assets/ayaka.glb`.
4. Write `reborn/src/main/assets/ayaka-vrm-metadata.json`.
5. Load the GLB with Filament gltfio from a native Android `SurfaceView`.

The converter retains core glTF geometry, skinning, morph targets, textures and supported KHR extensions. VRMC extension payloads are preserved in the metadata sidecar before VRM-specific extensions are removed from the GLB.

The sidecar preserves creator/license data, humanoid mapping, expressions, MToon parameters and spring-bone data. Preservation is not the same as runtime support.

### Known renderer gaps

- Full VRM MToon shading fidelity is not implemented.
- VRM humanoid semantic mapping is incomplete.
- Spring-bone simulation is not implemented.
- Named VRM expressions are not yet mapped to native morph-target controls.
- Blinking, emotion-driven expressions, idle animation and speech lip-sync need implementation and validation.
- The renderer has not yet been verified on the Galaxy A31.

Filament 1.75.0 is pinned because it matches the APIs used by this implementation and the project's Android compile SDK configuration. The current quality/resolution settings are starting values, not measured device benchmarks.

## 4. Conversation and personality

The conversation engine combines Ayaka's persona instructions, bounded chat history and explicitly approved memories.

- OpenRouter `openrouter/free` is optional and requires a user-supplied API key.
- The key is stored using Android Keystore AES-GCM.
- No paid fallback, subscription, credit purchase or automatic top-up is permitted.
- The no-key/offline path is a small rule-based response system, not an on-device language model.
- Network operations must remain off the UI thread.
- The app must be honest when offline or when a cloud request fails.

A future emotional state system may represent calm, joy, warmth, concern, sadness, frustration, surprise, shyness and playfulness as bounded values with decay over time. It should remain separate from the character's core identity. Structured expression/action hints must be validated against a small known set; unknown labels are ignored.

## 5. Voice, camera and privacy

- Speech recognition starts only after an explicit user action and required permission.
- Text-to-speech uses the Android provider.
- Camera capture starts only after explicit user action.
- Captured images remain local until the user confirms sending them.
- No background camera or microphone session is allowed.
- Chat history is local. Long-term memories are explicitly created, editable and deletable by the user.
- Never claim Ayaka saw, heard, remembered or did something unless the app supplied the input or completed the action.

Live AR compositing and camera-based character placement are future work, not implemented features.

## 6. Companion tools

The current source includes foundations for a focus timer, mood check-in, bond indicator and bedtime reminders. Each needs runtime validation, including persistence and Android permission behavior.

## 7. Galaxy A31 engineering constraints

- Target: Samsung Galaxy A31, Android 12, arm64-v8a.
- Keep rendering and animation work modest; avoid unnecessary post-processing and background activity.
- Pause rendering when the app is not visible.
- Do not attempt to run a large local language model or desktop-class speech stack on this device.
- Validate startup, first visible frame, memory pressure, thermal behavior, battery use, keyboard resizing, permission denial and background/foreground recovery on the physical phone.

## 8. Release gates

A successful CI build only proves the configured build checks passed. Before calling a build reliable, test on the Galaxy A31:

1. Install and launch; verify no startup crash.
2. Verify Ayaka is visibly rendered at the correct scale and orientation.
3. Exercise chat, scrolling, keyboard, navigation and app resume.
4. Test offline/no-key mode, invalid keys, no network and free-route unavailability.
5. Test voice permission grant/denial and speech output.
6. Test camera capture and explicit send confirmation.
7. Test memory controls, mood persistence and bedtime notifications.
8. Monitor memory, temperature, frame pacing and battery during a longer session.

## 9. Model attribution

Ayaka's VRM was created by epic_noobster. Preserve the embedded VRM metadata and `models/ayaka/MODEL-CREDITS.md`. The stated permissions are author-only, personal/non-profit use with credit required. Do not infer commercial rights from permission to modify or redistribute the file.
