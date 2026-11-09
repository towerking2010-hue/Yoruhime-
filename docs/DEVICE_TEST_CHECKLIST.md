# YoruHime 2.0 — Galaxy A31 device test checklist

Use this after installing the debug APK on the Samsung Galaxy A31 (Android 12). Keep the existing app installed when testing an update so Room migration and saved data can be checked.

## First launch and character
- [ ] App opens without crashing.
- [ ] The original Live2D model loads and remains interactive.
- [ ] Ayaka identifies herself as Ayaka Tōjou and keeps the caring, mature mommy-tsundere personality.
- [ ] Touch reactions update the model expression and bond progression.

## Cloud chat and memory
- [ ] Enable Cloud mode and enter an OpenRouter API key.
- [ ] Send two or more messages and confirm Ayaka uses recent conversation context.
- [ ] Send `Remember that ...`; confirm the note appears under Manage saved memories.
- [ ] Close and reopen the app; confirm the note remains.
- [ ] Send `Forget that ...`; confirm the matching note is removed.
- [ ] Switch to offline mode and confirm ordinary chat uses the offline dialogue bank.
- [ ] In offline mode, attach an image and confirm Ayaka clearly says she did not inspect it.

## Voice
- [ ] Tap the microphone and grant speech-recognition permission if requested.
- [ ] Confirm recognized speech appears in the message field before sending.
- [ ] Confirm assistant replies are spoken.
- [ ] Mute voice and confirm ordinary replies stay silent; unmute and test again.

## Front/rear camera and vision
- [ ] Open the camera dock from the chat composer and grant camera permission.
- [ ] Confirm the rear camera preview appears by default.
- [ ] Switch to the front camera and back; confirm the preview changes.
- [ ] Close the dock, enable the live camera background, and confirm it uses the same selected lens.
- [ ] Capture a still image and ask Ayaka a specific question about visible content.
- [ ] Confirm the image is inspected only in Cloud mode with an API key and a vision-capable route.
- [ ] Confirm the dock closes after capture and the camera can be opened again.
- [ ] Confirm camera access stops when the camera view is closed or the app leaves the foreground.

## Existing app features
- [ ] Start, pause, and complete a Pomodoro study session.
- [ ] Confirm completion still awards bond experience.
- [ ] Open bedtime settings, select a test time, and confirm the daily reminder is scheduled.
- [ ] Use the instant bedtime alert test button and confirm the notification appears.
- [ ] Confirm chat history and bond progress remain after app restart.

## Update and performance
- [ ] Install the new APK over the previous app without uninstalling first.
- [ ] Confirm existing chat and bond data survive the Room 2-to-3 migration.
- [ ] Use the camera dock and Live2D together for several minutes; watch for overheating, stutter, or memory-related restarts.
- [ ] Check that the app remains usable after returning from another app.

## Known scope limits
- Camera vision currently analyzes user-captured still images; it does not continuously interpret live video.
- The live camera background is a camera feed behind the existing Live2D stage, not world-tracked spatial AR.
- WorkManager bedtime reminders are best-effort and can be delayed by Android battery management.
- Android TTS and speech recognition use the device's installed services; a specific custom voice is not bundled.
