#!/usr/bin/env bash
set -euo pipefail

APK="reborn/build/outputs/apk/debug/reborn-debug.apk"
PACKAGE="com.yoruhime.reborn"
ACTIVITY="$PACKAGE/.MainActivity"

adb install -r "$APK"
adb shell am force-stop "$PACKAGE"
adb shell am start -n "$ACTIVITY"
sleep 45

APP_PID="$(adb shell pidof -s "$PACKAGE" | tr -d '\r')"
if [[ -z "$APP_PID" ]]; then
  echo "::error::YoruHime process exited after launch."
  adb logcat -d -t 2000 | grep -E "FATAL EXCEPTION|Fatal signal|AndroidRuntime|AyakaFilament" || true
  exit 1
fi

echo "YoruHime stayed alive after launch (PID $APP_PID)."
LOGS="$(adb logcat -d -t 3000)"
echo "$LOGS" | grep -E "FATAL EXCEPTION|Fatal signal|AndroidRuntime|AyakaFilament|Ayaka mesh rendered" || true

if echo "$LOGS" | grep -Eq "FATAL EXCEPTION|Fatal signal"; then
  echo "::error::Android reported a fatal app crash."
  exit 1
fi

if echo "$LOGS" | grep -q "Ayaka produced visible framebuffer pixels"; then
  echo "Ayaka produced visible pixels in Filament's framebuffer."
else
  echo "::error::The emulator did not verify visible Ayaka pixels. A loaded mesh or successful render submission is not sufficient."
  exit 1
fi
