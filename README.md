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

3. **Camera & Snapshot Vision (CameraX)**
   - User-triggered camera dock with front/back lens switching shared with the live camera background, plus a still-image capture action.
   - Captured front- or rear-camera snapshots are sent with the user's prompt to a configured vision-capable OpenRouter model in Cloud mode. Cloud failures are surfaced in the chat instead of being silently presented as normal offline replies.
   - The full-screen camera background can sit behind the Live2D stage as a lightweight visual overlay. This is not spatial AR: there is no world tracking, plane detection, or world-anchored object placement.
   - Offline mode explicitly tells the user that the image was not analyzed. Continuous live-video understanding, automatic fatigue/stress detection, and always-on camera monitoring are not implemented.

4. **"Ayaka's Strict Study Session" (Pomodoro Timer)**
   - Focus intervals (15, 25, 45, 60 mins) and break intervals.
   - Lifecycle state detection: if you switch apps away while the timer is running, Ayaka immediately fires an in-character scolding when you return!
   - Awards Bond EXP upon completion.

5. **Daily Curfew Reminder (WorkManager)**
   - Schedules a daily notification at the saved local time; new installs default to 11:00 PM; the database migration changes only the untouched old 11:00 AM default and preserves custom schedules, and the settings dialog includes midnight and evening presets.
   - WorkManager delivery is best-effort and may be delayed by Android battery restrictions; this is a reminder, not a device lock.
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

- **Evolving emotional state**: a lightweight local mood model tracks warmth, energy, stress, affection, and confidence. Chat replies, touch reactions, study events, and bedtime events influence it; the values gently decay toward a calm baseline. Its richer internal state maps only to the six expressions already supported by the existing Live2D model. No replacement model assets are used. Manual expression selection is kept in sync with the displayed Live2D expression; the stage uses a contain-style fit so the full model remains inside the available viewport.

These are incremental mobile adaptations, not a port of MekaHime's Windows-only runtime. The existing YoruHime Live2D model and assets are retained.
