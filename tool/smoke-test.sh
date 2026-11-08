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

say "installing $APK"
adb install -r -g "$APK" 2>&1 | tee -a "$LOG"
say "launching app"
adb shell am start -n "$PKG/.MainActivity" 2>&1 | tee -a "$LOG"
sleep 3

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
