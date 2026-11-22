Native Ayaka renderer migration

This branch replaces the Android WebView / Three.js avatar surface with a native Google Filament SurfaceView and glTF loader.

The build adapter removes VRM-only extension payloads while preserving core glTF geometry, skins, morph targets, textures and standard PBR material data. MToon-only styling may fall back to core glTF PBR values. This first milestone does not yet map VRM humanoid semantics, spring bones or named facial expressions. CI success is not proof of a successful physical-device render; Galaxy A31 QA is still required.
