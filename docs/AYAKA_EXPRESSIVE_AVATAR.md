# Ayaka Expressive Avatar

## Current implementation

YoruHime currently renders the bundled Tsubaki Live2D model through a Pixi/WebGL page inside Android WebView. The semantic animation director is now connected to that renderer; it is no longer only an isolated contract.

### Conversation-to-avatar pipeline

1. `AyakaConversationCueClassifier` classifies the latest user message into a bounded cue: greeting, question, compliment, achievement, distress, playful challenge, or none.
2. Distress has priority over playful cues. Compliments and playful challenges briefly select the Riko-inspired tsundere persona; Ayaka's core caring identity remains the default.
3. `AyakaAnimationDirector` creates deterministic renderer-neutral commands for face, gaze, head, body, and gesture layers.
4. `AyakaLive2DPerformanceAdapter` maps supported semantic commands to actual Tsubaki model controls.
5. `Live2DController` queues performance and speech commands until the WebView reports that the model is ready.

### What the current Live2D rig actually supports

- Six registered expressions: normal, smile, blush, happy, sad, angry.
- Existing idle motion clip.
- Procedural breathing, blinking, gentle organic sway, pointer gaze, and touch bounce.
- Semantic expression mapping (e.g. embarrassed blush, warm smile, concern, pride).
- Semantic gaze targets and smoothly blended head/body offsets.
- A subtle mouth-open animation driven by Android TTS start/end state. This is utterance-level movement, not phoneme-accurate lip-sync.
- One restrained idle gaze/head adjustment after 45 seconds of quiet on the Companion tab.
- WebView animation pauses when the Companion tab is hidden or the app loses foreground focus, then resumes when visible.

### Deliberate limitations

The current Tsubaki model only ships with an `Idle` motion. The director can produce semantic cues such as `small_wave`, `gentle_celebration`, and `playful_point`, but the current Live2D adapter does not pretend those arm/hand gestures exist. There is no real wave, walk, sit, or full-body gesture animation in this model.

The director's `AyakaSpeechDelivery` output is not yet connected to the TTS pitch/rate system; TTS currently uses the response emotion tag. The current performance mapping is intentionally small and deterministic. It does not yet implement a full priority/cooldown/interruption governor.

## Architecture

- `AvatarPerformance.kt`: renderer-neutral enums, context, layer commands, and performance contract.
- `AyakaAnimationDirector.kt`: deterministic decision engine for face/gaze/head/body/gesture intent.
- `AyakaConversationCueClassifier.kt`: conservative message-to-cue classification and persona selection.
- `AyakaLive2DPerformanceAdapter.kt`: supported semantic cues to concrete Tsubaki controls.
- `Live2DStage.kt`: WebView bridge, command queuing, lifecycle pause/resume, and speech state.
- `app/src/main/assets/live2d/index.html`: Pixi/Live2D procedural movement, gaze/head/body targets, TTS mouth movement, and low-cost rendering loop.
- Unit tests cover director decisions, cue priority/persona selection, and Live2D mapping.

## VRM direction

A future VRM renderer may provide richer humanoid gestures and animation clips:

- VRM 1.0 model loading with Three.js and `@pixiv/three-vrm`.
- VRMA animation support where model and clip compatibility is verified.
- Model-specific expression/blendshape and humanoid-bone mapping.
- A renderer adapter that preserves the same `AvatarPerformance` contract.
- Performance governor for layer priority, interruption, cooldown, lifecycle, and frame budget.

No final Ayaka VRM model has been selected or bundled. The target is a mature, elegant, anime-styled woman with silver hair, violet eyes, natural proportions, and a dark purple/black/silver outfit. A model must be free to download **and** legally redistributable inside the APK, with modification and attribution conditions understood. Free personal-use permission is not enough.

## Next work

1. Find or create a model whose visual style fits Ayaka and whose VRM license explicitly permits the intended redistribution.
2. Inspect facial expressions, blink/lip-sync morphs, humanoid rig, spring bones, triangle count, texture sizes, and VRM/VRMA version.
3. Prototype the VRM renderer on a separate branch while keeping Live2D as fallback.
4. Test loading, idle, expression blending, speech mouth movement, gaze, and several real clips on the Galaxy A31.
5. Only promote VRM to default after APK, lifecycle, thermal, memory, and visual tests pass.
6. Implement Together Mode only after the renderer is stable: camera preview, draggable/resizable transparent avatar, and a real composite photo export. The existing camera background is not spatial AR.
