# YoruHime 2.0.1 — Galaxy A31 device test checklist

Use this after installing the debug APK on the Samsung Galaxy A31 (Android 12). The debug variant uses the separate package ID suffix `.test`, so it installs alongside the production app and has a separate database. It cannot directly verify migration of production app data.

## First launch and character
- [ ] App opens without crashing.
- [ ] The original Live2D model loads and remains interactive.
- [ ] Ayaka identifies herself as Ayaka Tōjou and keeps the caring, mature mommy-tsundere personality.
- [ ] Touch reactions update the model expression and bond progression.
- [ ] Send a greeting, a question, a compliment, an achievement, and a distress message; verify the avatar cue changes appropriately.
- [ ] Confirm distress overrides playful/teasing cues.
- [ ] Confirm compliments briefly trigger blush and gaze-away behaviour without changing Ayaka's identity.
- [ ] Confirm TTS start/end triggers subtle mouth movement and that it stops when speech ends.
- [ ] Leave the Companion tab idle for 45 seconds and confirm one restrained idle gaze/head movement.
- [ ] Manually select an expression in Settings and confirm it remains selected until the next conversation cue.

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
- [ ] While a focus session is active, navigate to Memories, Care, and Settings, then return; verify the same timer continues rather than restarting.
- [ ] While focusing, switch to another app and return; verify the timer pauses and Ayaka gives the return reaction once.
- [ ] Confirm completion still awards bond experience.
- [ ] Open bedtime settings, select a test time, and confirm the daily reminder is scheduled.
- [ ] Use the instant bedtime alert test button and confirm the notification appears.
- [ ] Confirm chat history and bond progress remain after app restart.

## Update and performance
- [ ] Install the `.test` APK alongside the production app; confirm both icons/packages exist and the production app remains untouched.
- [ ] Confirm the test package's chat, memory, and bond data survive its own app restart.
- [ ] Validate Room migrations 2-to-3 and 3-to-4 using a migration test database seeded with chat, bond, and custom bedtime data; confirm chat/bond/custom schedules are preserved and only the untouched old default changes.
- [ ] A same-package production update requires a release APK signed with the same signing key as the installed production app. The current CI debug artifact is not that release package.
- [ ] Use the camera dock and Live2D together for several minutes; watch for overheating, stutter, or memory-related restarts.
- [ ] Check that the app remains usable after returning from another app.

## Known scope limits
- Camera vision currently analyzes user-captured still images; it does not continuously interpret live video.
- The live camera background is a camera feed behind the existing Live2D stage, not world-tracked spatial AR.
- WorkManager bedtime reminders are best-effort and can be delayed by Android battery management.
- Android TTS and speech recognition use the device's installed services; a specific custom voice is not bundled.


## Repair milestone checks
- [ ] Verify the full Live2D model fits without head/leg clipping after rotation and window resizing.
- [ ] Tap each expression control and confirm the matching expression is highlighted and visible; normal clears the expression.
- [ ] Send an exhaustion/distress message and confirm Ayaka responds supportively rather than with a generic scolding.
- [ ] In Cloud mode, verify a text reply and a rear-camera snapshot; confirm provider/API failures are shown clearly.
- [ ] Confirm the new default is 11:00 PM and an existing custom curfew survives the database migration.
- [ ] Remove the saved API key while Cloud is selected and confirm Cloud mode switches off immediately.
- [ ] Confirm the Live2D WebView pauses when YoruHime is backgrounded and resumes when the app returns.
