# YoruHime (夜姫)

An interactive native Android companion application built with Kotlin and Jetpack Compose featuring **Ayaka Tōjou** (東条 絢香), your strict, maternal "mommy-tsundere" senior student council vice-president.

## Features

1. **Live2D Rendering Stage (Top 60% of Screen)**
   - High-performance WebGL stage running in an optimized Android WebView with `WebViewAssetLoader`.
   - Renders the Live2D model with expressions (`smile`, `blush`, `happy`, `sad`, `angry`, `normal`) and motions.
   - Enforces a strict 30 FPS rendering cap to guarantee smooth frame pacing and low thermal/battery consumption on mid-range devices (like the Galaxy A31).
   - Interactive touch and gaze tracking reacting to touches on her head, face, or chest with flustered in-character dialogue.

2. **Chat & Personality Interface (Bottom 40% of Screen)**
   - Dark translucent glassmorphism interface styled in deep midnight indigos (`#0B0F19`), slate grays, and soft lavender accents.
   - **Ayaka Tōjou's Mommy-Tsundere AI**: Demanding and high-standard, complaining that taking care of you is an exhausting chore, but fiercely protective, maternal, and prone to flustered blushing panics when caught showing affection.
   - **Cloud Chat + Offline Fallback**: Uses OpenRouter with a user-supplied API key, recent conversation context, and an offline in-character dialogue bank when cloud chat is unavailable. Cloud usage and model availability depend on the selected OpenRouter route.

3. **Multimodal Vision & Camera Integration (CameraX)**
   - Floating Picture-in-Picture camera dock with front/back camera toggling.
   - **Camera Preview**: CameraX provides the live front/back camera background. Users can attach a photo to a chat for cloud vision analysis when the selected model supports images. Automatic facial fatigue/stress detection is not implemented yet.

4. **"Ayaka's Strict Study Session" (Pomodoro Timer)**
   - Focus intervals (15, 25, 45, 60 mins) and break intervals.
   - Lifecycle state detection: if you switch apps away while the timer is running, Ayaka immediately fires an in-character scolding when you return!
   - Awards Bond EXP upon completion.

5. **Midnight Bedtime Lock & Notifications (WorkManager)**
   - Background worker scheduled for midnight (12:00 AM local time).
   - Fires a high-priority heads-up notification with Ayaka demanding you stop looking at your screen and go to bed.
   - Includes an instant "Bedtime Alert" test button in the chat toolbar.

6. **Affection & Bond System (Room Database)**
   - Persistent progression stored locally in SQLite via Room.
   - Tiers from *Hopeless Slacker* to *Irreplaceable Soulmate* that unlock warmer dialogues and special reactions.


## Mobile companion upgrades

The Android app keeps its existing Live2D character and Compose interface while adding phone-friendly companion features:

- **Conversation continuity**: the latest 12 chat messages are sent to OpenRouter in chronological order.
- **Local personal memory**: type `Remember that ...` to save a note in Room. Type `Forget that ...` to remove an exact note. Use **Manage saved memories** in the cloud setup dialog to review or clear notes.
- **Memory privacy**: notes stay in the app's local database; when cloud chat is enabled, up to 12 notes are included in the OpenRouter request as context.
- **Protected API key**: the OpenRouter key is encrypted with AES-GCM using a key stored by Android Keystore.
- **Voice input and output**: use the microphone button for Android speech recognition. Assistant replies use the existing emotion-aware Android TTS and respect the mute control.

These are incremental mobile adaptations, not a port of MekaHime's Windows-only runtime. The existing YoruHime Live2D model and assets are retained.
