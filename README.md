# Convert264

Clean, ad-free Android video converter: pick a video (or share one from
File Manager), trim start/end, convert to H.264 MP4.

- Native Android, Kotlin + Jetpack Compose, Material 3
- Pick via Storage Access Framework or via
  Android Share intent (`ACTION_SEND video/*`)
- Transcoder: AndroidX Media3 Transformer — H.264 video + AAC audio, MP4
  (FFmpeg could not be bundled in the Google AI Studio environment the
  app was first generated in)
- Quality presets: 1080p / 720p / Original
- While converting: status bar with a real progress line (percent,
  current/total time, elapsed time, Cancel)
- When finished: Open and Share buttons, plus "Convert another"
- Output: `Movies/Convert264/<name>_h264.mp4` (never overwrites)
- "Try Sample Clip" generates a 3-second test video on-device, so the
  app can be tested end to end with no local files
- No ads, no account, fully offline

Originally generated in Google AI Studio
(https://aistudio.google.com/apps/6613a05e-0b02-449f-b2bb-c5e0dd872b67),
then handed off to GitHub + CI for a real build and emulator test.

## Build

```bash
gradle assembleRelease   # release APK in app/build/outputs/apk/release/
gradle testDebugUnitTest  # unit tests
```
(The AI Studio export contains no `gradlew` wrapper scripts; CI uses
Gradle 9.3.1 via `gradle/actions/setup-gradle`. Locally, open the
project in Android Studio or run Gradle 9.3.1 directly.)

CI builds the release APK on every `v*` tag and publishes it as a
GitHub Release (see `.github/workflows/android-release.yml`).
The release APK is signed with a throwaway keystore generated in CI,
so a future properly-signed build will need a reinstall, not an update.

## Emulator smoke test

`.github/workflows/emulator-smoke.yml` runs the in-process Compose
instrumented tests `app/src/androidTest/.../Convert264SmokeTest.kt`
and `ShareIntentSmokeTest.kt` (`gradle connectedDebugAndroidTest`)
on an API 35 emulator: the first taps "Try Sample Clip", trims the
end to 15s via the Exact Input dialog, and converts twice (Original
preset, then 720p), requiring the converting status bar to be shown,
the "Conversion Complete!" screen with Open/Share, a real `_h264.mp4`
output file on disk (verified with ffprobe), live elapsed time
during the export, and no 0-byte phantom rows in MediaStore; the
second launches the app with a real `ACTION_SEND` share intent and
requires the shared video to load. The runner script is
`tool/smoke-test.sh`.

Progress — what the emulator actually proved (v0.1.1 smoke runs
42656146569 and 42659639343, raw `Transformer.getProgress()` states
logged on every poll; 0=NOT_STARTED, 1=WAITING, 2=AVAILABLE,
3=UNAVAILABLE):
- **The % DOES ramp — on the 720p path.** Run 2 (720p preset,
  Presentation scale effect): percents seen 0, 3, 24, 31, 56, 85,
  99 over 89s, 192 of 195 polls AVAILABLE (second green run:
  0, 2, 4, 18, …, 97, 99 over 54s). The bar ramps for real
  whenever the encode is slow enough to observe.
- **On the fast Original path it jumps 0 → 99.** Run 1 (Original,
  no effects): the state stayed WAITING for the ~16s setup, became
  AVAILABLE once at 99%, and the export finished ~1s later. The
  earlier "percent stays 0 for the whole export" reading was wrong
  in detail: it was never UNAVAILABLE (state 3 never appeared), it
  was WAITING and then done. A time-based percent estimate would
  still be fabrication, so the app does NOT do that: while progress
  is WAITING it shows an indeterminate bar plus live elapsed time,
  and switches to a determinate percent the moment Media3 reports
  one. On a real phone with a hardware encoder the setup phase is
  shorter and the ramp should be visible on both paths — but that
  still needs a real-device check.
- Known sample quirk: the generated sample clip reports ~37s
  duration instead of 3s (its frames are timestamped in wall-clock
  as they encode on a slow emulator); the trim/output durations
  are still verified against the 15s trim.

Evidence from each smoke run is kept under
`tool/smoke-screenshots/<run-id>/` (in-test screenshots at every
step, screen-recorded video chunks, every output file ffprobed,
smoke.log) and uploaded as a CI artifact (30-day retention) — see
the `Emulator Smoke Test` workflow run for the release under test.

## Package

Application ID: `dev.dzianisv.convert264` (Kotlin source stays under
the `com.example` namespace the AI Studio export generated; only the
application ID was changed, to avoid a needless full-package refactor).
