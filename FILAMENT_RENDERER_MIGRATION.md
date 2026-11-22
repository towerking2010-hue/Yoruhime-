# Native Ayaka renderer migration

This branch replaces the Android WebView / Three.js avatar surface with a native Google Filament `SurfaceView` and glTF loader.

## Build compatibility

- Filament version: `1.75.0` (includes the required `ChoreographerHelper` and `ModelViewer.destroy()` APIs).
- Android compile SDK: 35; minimum SDK: 26.
- Target device: Samsung Galaxy A31, Android 12, `arm64-v8a`.
- The avatar path no longer depends on WebView or browser WebGL.

## Model conversion

The build verifies the original `Ayaka_Tojou 2.vrm` SHA-256 before converting it to `assets/ayaka.glb`. Supported standard glTF extensions such as `KHR_materials_unlit` and `KHR_texture_transform` are preserved. The model's 20 materials use unlit shading, so Filament does not need an environment-lighting bake to display their base colors.

All VRMC extension payloads are retained in `assets/ayaka-vrm-metadata.json` with their source JSON paths before being removed from the GLB. This preserves creator and license metadata, humanoid mapping, expression definitions, MToon parameters and spring-bone data for future native mapping. A Node regression test checks that KHR extensions survive and VRMC data is retained in the sidecar.

The current GLB keeps core geometry, skins, morph targets and textures. MToon rendering, humanoid semantic mapping, spring-bone simulation and named facial-expression mapping are not yet implemented in the Filament renderer.

## Renderer diagnostics and lifecycle

The native view validates finite, non-empty model bounds and checks for renderable mesh entities. It reports success only after asynchronous asset loading completes and Filament submits a frame containing renderables. `ModelViewer` owns its `SurfaceView` detach cleanup; the parent view does not destroy the same engine a second time.

The renderer uses the timestamp-based frame callback for Android 12 compatibility. Resolution and FXAA settings are tuned as a starting point for mid-range hardware, not a measured Galaxy A31 benchmark.

## Validation status

CI runs the converter regression test, verifies the original model fingerprint, performs Android unit-test tasks and Android Lint, assembles the debug APK, and verifies APK alignment/signature. CI success is not proof of a successful physical-device render. Galaxy A31 installation, first visible frame, framing, thermal behavior and runtime feature testing are still required.


## Startup and emulator validation hardening

- Filament JNI initialization now runs inside the renderer's guarded initialization path rather than a Kotlin class initializer, so native loading failures are reported in the avatar surface instead of escaping during class loading.
- The Filament viewer is created after the Android SurfaceView attaches. The frame loop starts only after viewer creation succeeds, and the renderer can be recreated after a detach/reattach.
- Speech recognition and text-to-speech providers are created only after an explicit voice action; optional device services cannot block initial app composition.
- CI launches the APK on an Android 12 emulator and requires both a live app process and Filament's first rendered-mesh callback. This is an automated smoke test, not a substitute for Galaxy A31 GPU/thermal testing.
