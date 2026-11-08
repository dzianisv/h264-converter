#!/usr/bin/env bash
# Convert264 emulator smoke test (run inside android-emulator-runner).
# Installs the debug APK, drives the UI via tool/smoke-driver.py, then
# verifies a real _h264.mp4 output file exists on disk.
set -u
PKG="dev.dzianisv.convert264"
APK="app/build/outputs/apk/debug/app-debug.apk"
OUT="tool/smoke-screenshots"
mkdir -p "$OUT"
LOG="$OUT/smoke.log"
: > "$LOG"

say() { echo "[smoke] $*" | tee -a "$LOG"; }

adb wait-for-device
say "device ready: $(adb shell getprop ro.build.model | tr -d '\r') / $(adb shell getprop ro.build.version.release | tr -d '\r')"
say "waiting for boot_completed"
adb shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'
say "boot completed; letting the system settle (first-boot dexopt, ANRs)"
sleep 90
# Digital Wellbeing ANRs on first boot and blocks the whole screen;
# it is unrelated to the app under test. (Absent on AOSP images —
# the error below is harmless there.)
adb shell pm disable-user --user 0 com.google.android.apps.wellbeing 2>&1 | tee -a "$LOG" || true
# The Settings app ANRs on slow first boots and its dialog blocks the
# app from drawing (and stalls uiautomator dumps). Force-stop it;
# the settings provider is a separate process and unaffected.
adb shell am force-stop com.android.settings 2>&1 | tee -a "$LOG" || true
adb shell settings put global window_animation_scale 0 2>/dev/null || true
adb shell settings put global transition_animation_scale 0 2>/dev/null || true
adb shell settings put global animator_duration_scale 0 2>/dev/null || true

say "installing $APK"
adb install -r -g "$APK" 2>&1 | tee -a "$LOG"
say "launching app"
# NOTE: the Kotlin source stays under the com.example namespace the
# AI Studio export generated — only the applicationId was renamed.
# So the activity is com.example.MainActivity, not $PKG.MainActivity.
adb shell am start -n "$PKG/com.example.MainActivity" 2>&1 | tee -a "$LOG"
sleep 3
# Cold start on a slow emulator can take a while to spawn the process
# (first run: still starting after 3s, with a system ANR dialog on
# top). Poll for the process, but do NOT fail here — the driver's
# 90s wait for the app title (which also dismisses ANR dialogs) is
# the real gate; failing here would kill a healthy slow launch.
APP_PID=""
for _i in $(seq 1 30); do
  APP_PID=$(adb shell pidof "$PKG" 2>/dev/null | tr -d '\r')
  [ -n "$APP_PID" ] && break
  sleep 2
done
if [ -n "$APP_PID" ]; then
  say "app process running (pid $APP_PID)"
else
  say "WARN: app process not visible via pidof after 60s — continuing; driver will decide"
fi
# The Settings ANR can reappear once the app starts drawing; kill it
# again right before the driver starts so no dialog blocks the UI.
adb shell am force-stop com.android.settings 2>/dev/null || true
sleep 2

python3 tool/smoke-driver.py 2>&1 | tee -a "$LOG"
DRIVER_RC=${PIPESTATUS[0]}
say "driver exit code: $DRIVER_RC"
if [ "$DRIVER_RC" -ne 0 ]; then
  say "FAIL: UI flow failed (see screenshots + log)"
  adb logcat -d -t 300 2>/dev/null | tail -n 300 > "$OUT/logcat-tail.txt" || true
  exit 1
fi

# --- Output-file verification (the real outcome, not just the UI) ---
say "=== output file checks ==="
adb shell ls -l /sdcard/Movies/Convert264/ 2>&1 | tee -a "$LOG"
OUT_FILE=$(adb shell ls /sdcard/Movies/Convert264/ 2>/dev/null | tr -d '\r' | grep '_h264\.mp4' | head -n 1)
if [ -z "$OUT_FILE" ]; then
  say "no _h264.mp4 under /sdcard/Movies/Convert264 — checking MediaStore"
  adb shell content query --uri content://media/external/video/media --projection _display_name:_data:size --where "relative_path='Movies/Convert264/'" 2>&1 | tee -a "$LOG"
  say "FAIL: no output file in Movies/Convert264"
  exit 1
fi
say "output file: $OUT_FILE"
OUT_SIZE=$(adb shell stat -c %s "/sdcard/Movies/Convert264/$OUT_FILE" 2>/dev/null | tr -d '\r')
say "output size bytes: $OUT_SIZE"
if [ -z "$OUT_SIZE" ] || [ "$OUT_SIZE" -le 0 ]; then
  say "FAIL: output file is empty/missing"
  exit 1
fi
adb pull "/sdcard/Movies/Convert264/$OUT_FILE" "$OUT/output.mp4" 2>&1 | tee -a "$LOG"
if command -v ffprobe >/dev/null 2>&1; then
  ffprobe -v error -show_entries format=duration,size -show_entries stream=codec_name,width,height -of default=noprint_wrappers=1 "$OUT/output.mp4" 2>&1 | tee -a "$LOG"
  OUT_DUR=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$OUT/output.mp4" 2>/dev/null)
  say "output duration (ffprobe): ${OUT_DUR}s — expected ~2s (3s sample trimmed to 2s)"
else
  say "ffprobe not available; duration taken from success screen"
fi

# Input sample size for the report (debug build is debuggable -> run-as works)
IN_SIZE=$(adb shell run-as "$PKG" stat -c %s cache/sample_clip.mp4 2>/dev/null | tr -d '\r')
say "input sample_clip.mp4 size bytes: ${IN_SIZE:-unknown}"
say "SUMMARY input_bytes=${IN_SIZE:-unknown} output_file=$OUT_FILE output_bytes=$OUT_SIZE"
say "=== SMOKE TEST PASSED ==="
exit 0
