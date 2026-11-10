#!/usr/bin/env bash
# Convert264 emulator smoke test (run inside android-emulator-runner).
# Runs the in-process Compose instrumented tests
# (app/src/androidTest/.../Convert264SmokeTest.kt +
#  ShareIntentSmokeTest.kt + FfmpegSpikeTest.kt) via
# connectedDebugAndroidTest — no uiautomator dumps, no adb UI
# driving, which proved unreliable on the slow GitHub Actions
# emulator (System UI ANRs, empty dumps for minutes at a time).
#
# Evidence: everything lands in tool/smoke-screenshots/<run-id>/
# (run-id is unique per run, so nothing is ever overwritten):
#   - in-test screenshots (launch / queue / settings / detail /
#     converting / done / share-intent) pulled from the app's external files dir
#   - screen-recorded video chunks of the whole run (best effort)
#   - every output file (_vp9.mkv in v0.2.0), ffprobed
#   - smoke.log + the SMOKE lines from the JUnit XML
# Afterwards the workflow uploads tool/smoke-screenshots/ as an
# artifact (retention 30 days).
set -u
RUN_ID="${GITHUB_RUN_ID:-local}-$(date +%Y%m%d-%H%M%S)"
OUT="tool/smoke-screenshots/$RUN_ID"
mkdir -p "$OUT"
LOG="$OUT/smoke.log"
: > "$LOG"

say() { echo "[smoke] $*" | tee -a "$LOG"; }
say "run id: $RUN_ID"

adb wait-for-device
say "device ready: $(adb shell getprop ro.build.model | tr -d '\r') / $(adb shell getprop ro.build.version.release | tr -d '\r')"
say "waiting for boot_completed"
adb shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'
# The runner reports boot completed while the device is still
# unresponsive often enough that Gradle then fails with
# "Skipping device 'emulator-5554': Unknown API Level" — do not
# start the build until the API level is actually queryable.
say "waiting for device properties to become queryable (API level)"
API=""
for i in $(seq 1 36); do
  API=$(timeout 20 adb shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r')
  if [ "$API" = "35" ]; then
    say "device API level: $API"
    break
  fi
  sleep 5
done
if [ "$API" != "35" ]; then
  say "device never became queryable (last API level='$API') — emulator infra failure, retry the run"
  exit 2
fi
# boot_completed + a queryable API level is NOT enough: on some
# runs the system server is still coming up and the package/activity
# services are not registered yet, so the test APK install then
# fails with "cmd: Can't find service: package" (killed the
# v0.2.0 spike run 42884094684 — the build had succeeded and 0
# tests ran). Do not start Gradle until both services answer.
say "waiting for package + activity services to be registered"
SERVICES_OK=""
for i in $(seq 1 36); do
  PKG=$(adb shell service check package 2>/dev/null | tr -d '\r')
  ACT=$(adb shell service check activity 2>/dev/null | tr -d '\r')
  if echo "$PKG" | grep -q "found" && echo "$ACT" | grep -q "found"; then
    SERVICES_OK=1
    say "package + activity services ready ($PKG / $ACT)"
    break
  fi
  sleep 5
done
if [ -z "$SERVICES_OK" ]; then
  say "package/activity services never became available — emulator infra failure, retry the run"
  exit 2
fi
say "boot completed; letting the system settle (first-boot dexopt, ANRs)"
sleep 90
# Digital Wellbeing ANRs on first boot; absent on AOSP images —
# the error below is harmless there.
adb shell pm disable-user --user 0 com.google.android.apps.wellbeing 2>&1 | tee -a "$LOG" || true
# The Settings app ANRs on slow first boots; force-stop it so no
# dialog blocks the app from drawing.
adb shell am force-stop com.android.settings 2>&1 | tee -a "$LOG" || true

# --- Screen recording (best effort; never fails the run) ---
# screenrecord caps a single recording at 180s, so restart it in
# chunks for the whole (long) test. Killed when the test finishes.
RECORD_PID=""
if adb shell ls /system/bin/screenrecord >/dev/null 2>&1; then
  (
    n=1
    while true; do
      # Log failures instead of discarding them: on some emulator
      # images screenrecord exits immediately (no encoder), and the
      # loop then breaks with no video and no explanation.
      adb shell screenrecord --time-limit 170 "/data/local/tmp/smoke-video-$n.mp4" >> "$OUT/screenrecord.log" 2>&1 || break
      n=$((n + 1))
    done
  ) &
  RECORD_PID=$!
  say "screen recording started (background pid $RECORD_PID)"
else
  say "screenrecord not available on this emulator — video evidence skipped"
fi
stop_recording() {
  if [ -n "$RECORD_PID" ]; then
    kill "$RECORD_PID" 2>/dev/null || true
    # A chunk may still be recording; give it a moment, then stop it.
    sleep 3
    adb shell pkill screenrecord 2>/dev/null || true
    sleep 2
  fi
}

say "running instrumented smoke tests"
gradle connectedDebugAndroidTest --no-daemon 2>&1 | tee -a "$LOG"
RC=${PIPESTATUS[0]}
say "connectedDebugAndroidTest exit code: $RC"

stop_recording

# --- Pull screen-recorded video chunks (written to /data/local/tmp:
# screenrecord cannot open files under /sdcard on API 35 —
# "Permission denied" — but /data/local/tmp is shell-writable) ---
adb shell ls /data/local/tmp/smoke-video-*.mp4 2>/dev/null | tr -d '\r' | while read -r f; do
  adb pull "$f" "$OUT/" 2>&1 | tee -a "$LOG" || true
done
ls -l "$OUT"/smoke-video-*.mp4 2>/dev/null | tee -a "$LOG" || say "no video chunks pulled"

# --- Pull in-test screenshots (public Pictures dir — see ScreenshotHelper.kt) ---
adb pull /sdcard/Pictures/Convert264-shots "$OUT/screenshots" 2>&1 | tee -a "$LOG" || say "no in-test screenshots pulled"
find "$OUT/screenshots" -name "*.png" 2>/dev/null | tee -a "$LOG" || true

# --- Output-file verification (the real outcome, not just the UI) ---
say "=== output file checks ==="
adb shell ls -l /sdcard/Movies/Convert264/ 2>&1 | tee -a "$LOG"
for OUT_FILE in $(adb shell ls /sdcard/Movies/Convert264/ 2>/dev/null | tr -d '\r' | grep -E '\.(mp4|mkv|mov|webm)$'); do
  say "output file: $OUT_FILE"
  OUT_SIZE=$(adb shell stat -c %s "/sdcard/Movies/Convert264/$OUT_FILE" 2>/dev/null | tr -d '\r')
  say "output size bytes: $OUT_SIZE"
  SAFE_NAME=$(echo "$OUT_FILE" | tr ' ' '_')
  adb pull "/sdcard/Movies/Convert264/$OUT_FILE" "$OUT/$SAFE_NAME" 2>&1 | tee -a "$LOG"
  if command -v ffprobe >/dev/null 2>&1; then
    ffprobe -v error -show_entries format=duration,size -show_entries stream=codec_name,width,height -of default=noprint_wrappers=1 "$OUT/$SAFE_NAME" 2>&1 | tee -a "$LOG"
    OUT_DUR=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$OUT/$SAFE_NAME" 2>/dev/null)
    say "output duration (ffprobe): ${OUT_DUR}s — expected ~3s (sample clip, untrimmed)"
  fi
done

# SMOKE lines the tests printed. println() output lands in the
# JUnit XML system-out on some runs and only in logcat on others —
# grep both so the diagnostics never go missing again.
find app/build/outputs/androidTest-results -name "*.xml" 2>/dev/null | while read -r f; do
  echo "=== $f ===" | tee -a "$LOG"
  grep -o "SMOKE[^<]*" "$f" 2>/dev/null | tee -a "$LOG" || true
done
find app/build/outputs/androidTest-results -name "logcat-*.txt" 2>/dev/null | while read -r f; do
  echo "=== $f (SMOKE lines) ===" | tee -a "$LOG"
  grep -h "SMOKE" "$f" 2>/dev/null | sed 's/^.*SMOKE/SMOKE/' | tee -a "$LOG" || true
done

# Keep a stable pointer to the latest run for humans.
ln -sfn "$RUN_ID" tool/smoke-screenshots/latest

exit $RC
