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
   - **Infinite Chat Guarantee**: Connected to the Gemini API for online cloud inference with an automatic seamless offline fallback to a rich local in-character response engine and Room database. You will never be cut off or blocked by quota limits.

3. **Multimodal Vision & Camera Integration (CameraX)**
   - Floating Picture-in-Picture camera dock with front/back camera toggling.
   - **Front Camera**: Real-time facial expression and stress/fatigue analysis that triggers Ayaka's maternal care or scolding.
   - **Back Camera**: Visual workspace and late-night meal inspection allowing Ayaka to inspect your desk or snacks and roast/advise you in character.

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
