#!/usr/bin/env bash
set -euo pipefail

APK="reborn/build/outputs/apk/debug/reborn-debug.apk"
PACKAGE="com.yoruhime.reborn"
ACTIVITY="$PACKAGE/.MainActivity"
OUT="artifacts/android-smoke"

mkdir -p "$OUT"
adb install -r "$APK"
adb shell am force-stop "$PACKAGE"
adb logcat -c
START_RESULT="$(adb shell am start -W -n "$ACTIVITY" 2>&1)"
printf '%s\n' "$START_RESULT" | tee "$OUT/activity-start.txt"
if ! printf '%s\n' "$START_RESULT" | grep -q "Status: ok"; then
  echo "::error::Android did not report a successful activity launch."
  exit 1
fi

# Do not mistake a surviving background process for a visible activity.
FOREGROUND=""
for attempt in $(seq 1 30); do
  FOCUS="$(adb shell dumpsys window 2>/dev/null | grep -E 'mCurrentFocus|mFocusedApp' | head -n 2 || true)"
  RESUMED="$(adb shell dumpsys activity activities 2>/dev/null | grep -E 'mResumedActivity|topResumedActivity' | head -n 2 || true)"
  printf '%s\n%s\n' "$FOCUS" "$RESUMED" > "$OUT/foreground-state.txt"
  if printf '%s\n%s\n' "$FOCUS" "$RESUMED" | grep -q "$PACKAGE"; then
    FOREGROUND="yes"
    break
  fi
  sleep 1
done
if [[ "$FOREGROUND" != "yes" ]]; then
  echo "::error::YoruHime process may exist, but Android does not report its activity as foreground/resumed."
  cat "$OUT/foreground-state.txt"
  adb logcat -d -v threadtime > "$OUT/logcat.txt" || true
  adb exec-out screencap -p > "$OUT/screen.png" || true
  exit 1
fi

# Give Compose, the SurfaceView and Filament time to initialize after foreground confirmation.
sleep 35
FOCUS="$(adb shell dumpsys window 2>/dev/null | grep -E 'mCurrentFocus|mFocusedApp' | head -n 2 || true)"
RESUMED="$(adb shell dumpsys activity activities 2>/dev/null | grep -E 'mResumedActivity|topResumedActivity' | head -n 2 || true)"
printf '%s\n%s\n' "$FOCUS" "$RESUMED" > "$OUT/foreground-state.txt"
if ! grep -q "$PACKAGE" "$OUT/foreground-state.txt"; then
  echo "::error::YoruHime lost foreground status during the rendering test."
  adb logcat -d -v threadtime > "$OUT/logcat.txt" || true
  adb exec-out screencap -p > "$OUT/screen.png" || true
  exit 1
fi

adb exec-out screencap -p > "$OUT/yoruhime-screen.png"
adb shell uiautomator dump /sdcard/yoruhime-window.xml >/dev/null 2>&1 || true
adb pull /sdcard/yoruhime-window.xml "$OUT/window.xml" >/dev/null 2>&1 || true
adb logcat -d -v threadtime > "$OUT/logcat.txt" || true
APP_PID="$(adb shell pidof -s "$PACKAGE" | tr -d '\r')"
if [[ -z "$APP_PID" ]]; then
  echo "::error::YoruHime process exited during the foreground render test."
  exit 1
fi

if ! grep -q "AyakaFilament" "$OUT/logcat.txt"; then
  echo "::error::YoruHime is foreground, but the native Ayaka renderer emitted no diagnostics. Check Compose placement and SurfaceView attachment."
  exit 1
fi
if grep -Eq "FATAL EXCEPTION|Fatal signal" "$OUT/logcat.txt"; then
  echo "::error::Android reported a fatal app or native crash."
  grep -E "FATAL EXCEPTION|Fatal signal|AndroidRuntime|AyakaFilament" "$OUT/logcat.txt" | tail -n 100 || true
  exit 1
fi
if grep -q "Ayaka produced visible framebuffer pixels" "$OUT/logcat.txt"; then
  echo "PASS: YoruHime is foreground and Ayaka produced visible Filament framebuffer pixels."
else
  echo "::error::YoruHime is foreground, but Ayaka did not pass the framebuffer visibility probe."
  grep -E "AyakaFilament|Ayaka GLB|visiblePixels|Native renderer" "$OUT/logcat.txt" | tail -n 100 || true
  exit 1
fi
