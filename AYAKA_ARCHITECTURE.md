# Ayaka / YoruHime — Interaction and Technical Blueprint

This document records the selected direction for the Android companion. It is a product and engineering plan, not a claim that every planned subsystem is already implemented.

## 1. The chosen experience: character-first, not chat-first

The Home destination is one immersive character stage. Ayaka occupies the full usable screen. The interface is deliberately quiet and keeps only a few floating controls visible.

- Top edge: YoruHime identity and small shortcuts to Memories and Settings.
- Main canvas: Ayaka's full-height VRM avatar, with a restrained ambient backdrop.
- Lower identity area: Ayaka's name, a short presence line and a small relationship indicator.
- Floating action dock: camera, voice input, open conversation and Care.
- Conversation: a modal bottom sheet opened on demand. It overlays the character rather than permanently dividing the screen.
- Keyboard: appears only after the user taps the message composer. The sheet resizes with Android's IME and keeps the latest message and composer accessible.
- Dismissal: swipe down or Back to return to the uninterrupted character stage.

Secondary destinations (Memories, Care, Settings) remain native screens. They are not permanent navigation chrome on the character stage.

## 2. Conversation states

The companion should expose a small, understandable state machine:

| State | What the user sees | Expected behavior |
|---|---|---|
| Present | Ayaka idles naturally | Light breathing, occasional blink, subtle gaze |
| Listening | Listening indicator near Ayaka | Microphone is active only after explicit tap |
| Thinking | Small unobtrusive indicator | AI/network work runs off the UI thread |
| Speaking | Ayaka's mouth/face responds | Voice output and facial expression remain synchronized where supported |
| Offline | Clear offline status | Local fallback responds honestly; never pretends to have cloud intelligence |
| Recovering | Visible diagnostic message | Avatar/WebView failures expose a retry path instead of an empty rectangle |

A conversation sheet must not steal microphone permission, start the camera, or upload an image by itself.

## 3. Ayaka's character model

Ayaka's identity is independent of Riko and Sakura.

- Core: mature, elegant, caring, protective, emotionally perceptive, warmly affectionate.
- Surface voice: composed and natural; playful tsundere teasing is occasional, not a constant gimmick.
- Serious situations: empathy and clarity override teasing.
- Boundaries: never cruel, humiliating, possessive, coercive or manipulative.
- Honesty: she must not claim to see, hear, remember or perform an action unless the app supplied that input or completed that action.
- Memory: only explicitly approved memories are injected into later conversation.

### Emotional direction

OpenSakura demonstrates why emotion should be structured state rather than a single adjective. YoruHime should eventually use a compact bounded emotion vector (for example: calm, joy, warmth, concern, sadness, frustration, surprise, shyness and playfulness) with slow decay between turns.

The model response should provide a small structured affect/action hint alongside the natural-language reply. The app validates the hint against known expressions and motions. Unknown labels are ignored. A second paid or cloud LLM call solely to decide a gesture is intentionally avoided.

### Motion direction

The future motion library should be small and purposeful:

- Idle: breathe, blink, subtle weight shift.
- Greeting: small wave, smile, slight bow.
- Thinking: look aside, thoughtful pose.
- Listening: attentive gaze and restrained nod.
- Happy: smile, small celebratory gesture.
- Shy: blush/averted gaze if the model supports it.
- Concern: softened expression, slight lean-in.
- Strict/playful: arms folded or a small pout, used sparingly.

Motion transitions must blend smoothly, cancel when a new action arrives, and always return to a neutral idle. Do not trigger a full-body animation for every short message.

## 4. Reference-project adaptations

### Project Riko — https://github.com/rayenfeng/riko_project

The public repository is MIT-licensed and separates ASR, LLM, TTS and avatar presentation. Its VRM viewer frames the camera from the actual avatar's head height instead of assuming every model has the same proportions. It also treats lip-sync as a separate presentation layer.

YoruHime adopts the clean turn pipeline, modular boundaries and model-relative framing. Riko's Python desktop runtime, external voice services and character identity are not copied.

### OpenSakura — https://github.com/zeekk0/OpenSakura

The harness is MIT-licensed. Its brain owns emotion state, memory, relationship context and persona, while a separate action controller chooses from a known motion library. It decays emotional state over time and stores user-specific memory notes.

YoruHime adopts the separation of concerns and the idea of emotion-aware presentation, simplified for Android. Sakura's character, likeness, VRM, voice model and voice assets are not reused. Its Windows/NVIDIA speech stack is not suitable for this phone target.

## 5. Android architecture

### Presentation layer
- Jetpack Compose owns navigation, the immersive HUD, conversation sheet, memories, care and settings.
- The home screen does not reserve a permanent chat column.
- The message composer is focus-driven; keyboard visibility is controlled by Android.
- WebView is isolated inside the avatar component, not used as the whole application UI.

### Avatar layer
- Three.js and @pixiv/three-vrm render a locally bundled VRM.
- AndroidX WebViewAssetLoader serves the compiled Vite bundle from app assets.
- The WebView must remain hardware accelerated.
- The WebGL context is requested explicitly and failures are reported to the native Compose layer.
- The canvas is capped at 30 FPS and its device pixel ratio is bounded for mid-range hardware.
- A local VRM is loaded from app assets; no remote avatar CDN is required at runtime.
- When the activity is backgrounded, animation should pause; when the user returns, it resumes without starting the camera.

### Companion engine
- One request per user turn, with bounded conversation history.
- OpenRouter openrouter/free only when a user-provided key exists.
- No automatic paid model fallback, top-ups or hidden credit use.
- Offline fallback is a small rule-based response path, not an on-device LLM.
- Network requests run on IO dispatchers and have bounded connect/read timeouts.
- Errors are translated into clear, non-technical in-character feedback without hiding the actual connection state.

### Memory and privacy
- Chat history is local.
- Long-term memory is opt-in and user-editable.
- API key is encrypted with Android Keystore AES-GCM.
- Camera activation is explicit.
- A captured image remains local until the user confirms the send action.
- No telemetry or background recording is introduced.

## 6. Galaxy A31 performance rules

- 30 FPS cap; do not chase 60 FPS.
- Bound WebGL pixel ratio (currently 1.25).
- Avoid expensive post-processing, real-time shadows and multiple full-screen render passes.
- Keep the avatar mesh/materials and animation clips modest.
- Pause rendering when the app is not visible.
- Do not run local 7B/8B language models, desktop speech stacks or GPU model downloads on this phone.
- Use Android's installed speech recognition and TTS providers as the initial zero-cost voice path.
- Do not keep camera frames or microphone streams running outside a user-initiated session.
- Validate memory pressure, thermal behavior, keyboard resizing and WebView process recovery on the actual device.

## 7. Release gates

A CI-green APK is only a build artifact. Before calling a build reliable, test on a Galaxy A31 / Android 12:

1. Install alongside the legacy app without overwriting it.
2. Confirm the avatar page loads and native status changes to ready.
3. Confirm the VRM itself is visible at the intended scale and facing direction.
4. Rotate/tap the avatar and verify expressions.
5. Open the chat sheet, focus the composer, show/hide the keyboard, send, scroll and dismiss.
6. Test voice permission grant/deny and transcription.
7. Test camera permission/capture and verify no image is uploaded before confirmation.
8. Test no-key offline mode, invalid key, no network, and free-route unavailability.
9. Background and resume the app; verify rendering resumes and no camera/microphone remains active.
10. Inspect battery, temperature and memory during a longer session.

## 8. Current known gaps

The current bundled VRM is pixiv's AvatarSample_B, a temporary licensed sample, not the final Ayaka model. The current avatar runtime has basic idle sway, blink and tap-triggered happy expression; it does not yet have the full motion library, emotion engine, dialogue-to-action mapping or a custom Ayaka voice. The new immersive layout and native startup diagnostics are committed, but physical Galaxy A31 verification remains outstanding.
