#!/usr/bin/env bash
# Convert264 emulator smoke test (run inside android-emulator-runner).
# Runs the in-process Compose instrumented test
# (app/src/androidTest/.../Convert264SmokeTest.kt) via
# connectedDebugAndroidTest — no uiautomator dumps, no adb UI driving,
# which proved unreliable on the slow GitHub Actions emulator
# (System UI ANRs, empty dumps for minutes at a time).
# Afterwards pulls the output _h264.mp4 (if any) and ffprobes it.
set -u
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
# Digital Wellbeing ANRs on first boot; absent on AOSP images —
# the error below is harmless there.
adb shell pm disable-user --user 0 com.google.android.apps.wellbeing 2>&1 | tee -a "$LOG" || true
# The Settings app ANRs on slow first boots; force-stop it so no
# dialog blocks the app from drawing.
adb shell am force-stop com.android.settings 2>&1 | tee -a "$LOG" || true

say "running instrumented smoke test"
gradle connectedDebugAndroidTest --no-daemon 2>&1 | tee -a "$LOG"
RC=${PIPESTATUS[0]}
say "connectedDebugAndroidTest exit code: $RC"

# --- Output-file verification (the real outcome, not just the UI) ---
say "=== output file checks ==="
adb shell ls -l /sdcard/Movies/Convert264/ 2>&1 | tee -a "$LOG"
OUT_FILE=$(adb shell ls /sdcard/Movies/Convert264/ 2>/dev/null | tr -d '\r' | grep '_h264\.mp4' | head -n 1)
if [ -n "$OUT_FILE" ]; then
  say "output file: $OUT_FILE"
  OUT_SIZE=$(adb shell stat -c %s "/sdcard/Movies/Convert264/$OUT_FILE" 2>/dev/null | tr -d '\r')
  say "output size bytes: $OUT_SIZE"
  adb pull "/sdcard/Movies/Convert264/$OUT_FILE" "$OUT/output.mp4" 2>&1 | tee -a "$LOG"
  if command -v ffprobe >/dev/null 2>&1; then
    ffprobe -v error -show_entries format=duration,size -show_entries stream=codec_name,width,height -of default=noprint_wrappers=1 "$OUT/output.mp4" 2>&1 | tee -a "$LOG"
    OUT_DUR=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$OUT/output.mp4" 2>/dev/null)
    say "output duration (ffprobe): ${OUT_DUR}s — expected ~5s (sample trimmed to 5s)"
  fi
else
  say "no _h264.mp4 under /sdcard/Movies/Convert264"
fi

# SMOKE lines the test printed (land in the JUnit XML system-out)
find app/build/outputs/androidTest-results -name "*.xml" 2>/dev/null | while read -r f; do
  echo "=== $f ===" | tee -a "$LOG"
  grep -o "SMOKE[^<]*" "$f" 2>/dev/null | tee -a "$LOG" || true
done

exit $RC
