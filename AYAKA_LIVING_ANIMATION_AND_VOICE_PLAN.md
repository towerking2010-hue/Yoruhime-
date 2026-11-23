# Ayaka Living Animation and Offline Voice Architecture

## Product target

Ayaka remains the original creator-supplied VRM-derived character, rendered by the native Filament Android renderer. The target is an autonomous-feeling companion, not a menu of disconnected emotes. All inference and playback must remain local where possible, avoid paid services, and degrade gracefully on the Galaxy A31.

## Recommended architecture

Compose conversation/UI
  -> Ayaka conversation engine (free route; local fallback)
  -> compact semantic reaction plan (mood, gesture intent, gaze intent, speech style)
  -> deterministic on-device motion controller
  -> Filament animation runtime
  -> audio playback + timestamp/viseme synchronization

The LLM may suggest intent, but it must never generate arbitrary bone rotations that are applied directly. The local controller validates action names, cooldowns, priorities, duration, and allowed combinations. If a model response has no valid action metadata, a local planner supplies a conservative reaction.

### Motion layers

1. **Base idle:** breathing, tiny posture shifts, subtle head/neck motion and non-periodic blink timing.
2. **Gaze and attention:** look toward the user's interaction area, brief glance-away when shy, return to attentive gaze; clamp eye/head rotations.
3. **Facial layer:** VRM preset/custom morphs (blink, happy, angry, sad, relaxed, surprised, aa/ih/ou/ee/oh) blended independently from body clips.
4. **Gesture layer:** wave, nod, head tilt, pout, arms-crossed, hand-to-chest, shrug, point, laugh, shy look-away, thinking pose, greeting, farewell and dance.
5. **Secondary motion:** VRM spring-bone hair/accessory motion, damped and frame-rate independent.
6. **Speech layer:** actual audio playback clock drives viseme curves; speech style influences pitch/rate/pauses, not arbitrary mouth animation.

### Motion asset strategy

Prefer authored animation clips over inventing all motion procedurally:
- Import VRM Animation (.vrma) clips where possible. VRMA standardizes humanoid bone animation, expression animation and gaze targets, and is designed to reuse clips across VRM avatars.
- Also support glTF animation clips and retargeted BVH/FBX/Mixamo clips through an offline conversion pipeline. Keep retargeting in build/asset preparation, not on the phone.
- Build an animation catalogue with metadata: action, mood, tags, duration, loopability, body-part mask, transition in/out, intensity and cooldown.
- Keep original VRM creator/model metadata and license constraints. Verify every downloaded motion pack's license independently.

### Motion planner

The planner should use a priority queue and state machine:
- Safety/system states: loading, interrupted, user speaking, app backgrounded.
- Conversation states: listening, thinking, speaking, reacting, idle.
- Actions: one full-body gesture at a time, with compatible facial/gaze overlays.
- Blend transitions using duration-aware interpolation. Do not stack multiple full-body clips on the same bones without a real blend implementation.
- Avoid repetition with a short per-session history and cooldowns.
- Explicit user animation requests outrank spontaneous reactions.
- Never dance or wave at random in a serious/sad conversation.
- Use a deterministic local fallback so the avatar still behaves offline.

### Research references and integration assessment

- DLP3D / Digital Life Project 2: strong reference architecture for orchestrating LLM, TTS, emotion/reaction, Audio2Face and Speech2Motion. Its published implementation is a browser-oriented, service-based system with motion databases and backend components. Reuse architectural ideas and interfaces; do not embed the whole Python/browser stack into the Android APK.
- DLP3D Speech2Motion: MIT-licensed service with motion retrieval, session memory and blending. It expects a motion database and service infrastructure; useful for offline asset preparation/research, not a direct lightweight A31 runtime.
- DLP3D Audio2Face: audio-driven facial animation service. Use as a reference for audio-synchronized facial motion; assess model/runtime and memory before considering any on-device port.
- VRM Animation (.vrma): preferred interchange format for humanoid clips, expressions and gaze. Preserve VRM humanoid mapping and rest-pose corrections when converting.
- UniMate text-to-motion: promising generative research/workbench for creating motion clips from natural-language descriptions. Current browser/Python inference workflow is not a sensible real-time dependency on the A31. Generate candidate clips on a development machine/CI, inspect and retarget them, then package approved clips.
- Motion capture: record/obtain licensed BVH/FBX/VRMA clips, retarget to Ayaka, and author variants. This is likely to yield better quality and reliability than asking a small on-device model to invent full-body motion in real time.
- Procedural animation: retain for breathing, blink timing, gaze micro-motion, subtle sway and physics; do not use it as the only source for expressive gestures.

## Offline voice direction

### Primary candidate: Kokoro-82M via MediaPipe Tasks Audio / LiteRT-LM

- On-device neural TTS, local PCM output, full-utterance and streaming APIs, and per-utterance voice selection.
- Model bundle is approximately 295 MB; this must be downloaded only after the user opts in, not bundled into the APK.
- Candidate voice auditions: bf_emma (British female, first candidate for refined ojou-sama), bf_isabella (British female alternate), af_bella (American female alternate). The final choice must be made by listening on the target phone, not by voice ID alone.
- The upstream benchmark is on Galaxy S26, not Galaxy A31. The documented GPU model can use about 0.35–0.5 GB peak memory; the CPU-friendly model can use about 0.7–0.95 GB. Do not assume either is suitable for A31 before measuring cold start, synthesis latency, peak memory, thermals and audio quality.
- If the GPU delegate is unavailable or unstable, test the CPU-compatible model. If the device runs out of memory or synthesis is too slow, keep Android system TTS as a fallback and allow the user to delete the downloaded model.
- Use PCM samples and the playback clock to synchronize visemes. Do not infer lip sync from spelling or a fixed timer.

### Personality voice styles

Use one core voice identity, with subtle delivery profiles:
- Elegant/default: measured pace, clear articulation, composed pauses.
- Playful/tsundere: slightly quicker timing and brighter intonation.
- Caring: softer pace and longer pauses.
- Serious: neutral, slower delivery.

Switch style parameters per utterance where the engine supports them. Avoid abrupt pitch changes or caricature. Do not clone a real person's voice without permission.

### OpenVoice

OpenVoice is a voice-conversion/style layer, not the first Android TTS engine to ship. Its common reference implementation is Python/PyTorch-oriented and is not an appropriate always-on dependency for the A31. Revisit only if there is a genuinely local, mobile-compatible conversion runtime and a voice reference with explicit rights.

## Device acceptance tests (Galaxy A31)

1. Cold start with avatar visible and no ANR/crash.
2. Idle animation stable for 10 minutes with acceptable battery/temperature.
3. Each gesture transitions in and out without snapping, T-pose or mesh distortion.
4. Facial expression and body gesture can run simultaneously without one resetting the other.
5. Gaze/blink remains subtle and does not jitter.
6. Interrupt speech: audio stops and mouth returns to neutral immediately.
7. Kokoro cold-start time, warm synthesis time, peak memory, audio underruns, device temperature and battery drain measured on A31.
8. Low-memory fallback to Android TTS works without losing chat or avatar.
9. App background/foreground resets transient gestures safely.

## Current implementation status

- Native Filament renderer and original Ayaka GLB: working on user's Galaxy A31 by user report.
- Build-generated idle/facial/gesture clips: present, but gesture quality and blend smoothness still require physical-device validation.
- Animation Studio: manual expression and gesture test controls exist.
- Newly added local conversational gesture planner: basic intent heuristics only; it is not an LLM motion model and needs A31 validation.
- TTS: current app still uses Android system TextToSpeech; pitch/rate and auto-speaking are available. Kokoro is researched but not yet integrated.
- True audio-clock-driven viseme playback, spring-bone physics, reusable VRMA import, robust crossfading and rich autonomous behavior remain future implementation work.

## Licensing and privacy

Keep the original Ayaka model and attribution. Only package motion assets whose licenses permit the intended redistribution and use. Download the optional TTS model only on explicit user action. Voice synthesis stays local; do not upload voice recordings or clone another person's voice.
