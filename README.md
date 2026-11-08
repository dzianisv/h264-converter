# Convert264

Clean, ad-free Android video converter: pick a video (or share one from
File Manager), trim start/end, convert to H.264 MP4.

- Native Android, Kotlin + Jetpack Compose, Material 3
- Pick via Storage Access Framework (USB-OTG flash drives work) or via
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
instrumented test `app/src/androidTest/.../Convert264SmokeTest.kt`
(`gradle connectedDebugAndroidTest`) on an API 35 emulator: it taps
"Try Sample Clip", trims the end to 15s via the Exact Input dialog,
converts, requires the converting status bar to be shown, the
"Conversion Complete!" screen with Open/Share, and a real `_h264.mp4`
output file on disk (verified with ffprobe). The runner script is
`tool/smoke-test.sh`.

Known emulator limitation: on the GitHub Actions emulator the
Media3 progress percent stays 0 for the whole export
(`Transformer.getProgress()` stays unavailable/0 on the software
encoder — proven by 75 polls over a 44s conversion), so the test
logs the observed values but does not gate on the % ramping; % ramping
needs a real device. (An earlier `adb`/`uiautomator` driver,
`tool/smoke-driver.py`, is kept as a manual fallback — the emulator
ANRs so often that hierarchy dumps return empty for minutes at a
time, which is why CI uses the in-process test.)

## Package

Application ID: `dev.dzianisv.convert264` (Kotlin source stays under
the `com.example` namespace the AI Studio export generated; only the
application ID was changed, to avoid a needless full-package refactor).
