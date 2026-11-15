# Ayaka Expressive Avatar

## Goal

Make expressive behaviour part of Ayaka's identity. The AI chooses *what she
wants to express*; a deterministic animation director turns that intent into
bounded animation-layer commands; the renderer performs the commands.

This is inspired by the public architectural ideas in Project Riko, but does
not copy its paid/private implementation or depend on its code.

## Current baseline

The current YoruHime character stage is a Pixi/Live2D WebView. It already has
procedural breathing, blinking, gaze movement, tap reactions, named expressions
and a single named-motion call. It does not yet provide a general layered
animation system or autonomous AI-directed performances.

## Architecture

1. **Conversation understanding** produces structured cues and emotion.
2. **Persona state** supplies Ayaka's current delivery mode:
   - Mommy-Tsundere (default)
   - Riko-style Tsundere (playful)
   - Caring / serious
   - Quiet companion
3. **AyakaAnimationDirector** produces renderer-neutral commands for:
   - base idle
   - face
   - gaze
   - head
   - upper body
   - gesture
4. **Renderer adapter** maps semantic clip keys to actual model expressions,
   bones, morph targets and animation clips.
5. **Performance governor** (next stage) arbitrates priorities, cooldowns,
   interruption, lifecycle pause/resume and device frame budget.

## Rules

- Emotional safety and conversational context outrank playful teasing.
- Only one command per animation layer may be active at once.
- A gesture must not cancel breathing, blinking, gaze or facial expression.
- Long idle behaviour is subtle and sparse; avoid constant movement.
- Renderer commands are allow-listed semantic keys, never arbitrary code.
- The director is deterministic and unit-testable.
- The existing Live2D model remains the fallback until a replacement model and
  renderer pass APK and Galaxy A31 device testing.

## Staged implementation

1. Add the renderer-neutral performance contract and deterministic director.
2. Build and test a VRM renderer prototype in an isolated feature path.
3. Inspect candidate models for facial expressions, humanoid rig, spring bones,
   animation compatibility and redistribution rights.
4. Add a renderer adapter and animation blending / interruption manager.
5. Connect conversation cues, voice timing and autonomous idle scheduling.
6. Benchmark thermal load, memory and frame pacing on the Galaxy A31.
7. Only then make VRM the default; preserve Live2D fallback.

## VRM interoperability

Prefer standard VRM expression and humanoid animation interfaces. VRM Animation
(VRMA) supports portable humanoid motion, expression animation and gaze data.
The renderer adapter should use those standards where supported.
