# YoruHime Reborn

**A clean-room rebuild of YoruHime.**

This branch is a new Android application module named `:reborn`. The previous `:app` implementation is not included in the build and is not reused by the new experience. Existing repository history is retained only as a safety reference.

## Product direction

YoruHime is an intimate, polished Android companion centered on Ayaka Tōjou: mature, elegant, protective, warm, emotionally intelligent, and playfully tsundere when the moment is right. The app is designed for Android 12+ and tuned for a Samsung Galaxy A31-class device.

## Clean architecture boundary

- New Gradle application module: `:reborn`
- New package: `com.yoruhime.reborn`
- New application ID: `com.yoruhime.reborn`
- New Compose UI; no legacy screen composables or legacy Live2D WebView are used.
- The old `:app` module is excluded from `settings.gradle.kts`.

## First foundation milestone

- New portrait-first Companion screen and distinct navigation destinations.
- Dedicated Memories, Care, and Settings screens with visible content.
- Camera action launches Android's camera capture flow only after the user taps it.
- Layout uses system-safe drawing insets and a single app navigation scaffold.
- No cloud endpoint is called and no paid service is configured in this foundation milestone.

## Planned engineering sequence

1. Stabilize the clean application shell and verify it on CI.
2. Add secure free-only OpenRouter connectivity and a useful offline conversation engine.
3. Add opt-in local memory persistence with explicit remember/forget controls.
4. Add camera capture, image review, and optional free-route vision.
5. Add voice input/output with correct lifecycle ownership.
6. Add a proper expressive full-height Ayaka avatar, sourced or authored with clear redistribution rights.
7. Add focus, care routines, bond progression, and Together Mode after core reliability.

## Non-negotiable constraints

- No paid AI fallback, subscriptions, credits, hidden charges, or automatic top-ups.
- API credentials must be encrypted and never committed to Git.
- Camera must never run in the background.
- Memory must be transparent, local-first, and user-controlled.
- Avoid always-on heavy rendering; respect Galaxy A31 thermal, RAM, and battery limits.
- Never describe a feature as complete until it is implemented and tested.

## Build

The dedicated workflow runs:

```bash
gradle :reborn:testDebugUnitTest :reborn:assembleDebug
```

The generated debug APK is uploaded as the `yoruhime-reborn-debug-apk` workflow artifact. CI success is not a substitute for testing on a physical Android 12 device.
