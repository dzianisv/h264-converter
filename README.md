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
./gradlew assembleRelease   # release APK in app/build/outputs/apk/release/
./gradlew testDebugUnitTest  # unit tests
```

CI builds the release APK on every `v*` tag and publishes it as a
GitHub Release (see `.github/workflows/android-release.yml`).
The release APK is signed with a throwaway keystore generated in CI,
so a future properly-signed build will need a reinstall, not an update.

## Emulator smoke test

`.github/workflows/emulator-smoke.yml` installs a debug APK on an
API 35 emulator, taps "Try Sample Clip", sets a trim, converts, and
requires the progress bar to advance, an output `_h264.mp4` file to
exist, and the Open/Share success screen to appear. The driver script
is `tool/smoke-test.sh`.

## Package

Application ID: `dev.dzianisv.convert264` (Kotlin source stays under
the `com.example` namespace the AI Studio export generated; only the
application ID was changed, to avoid a needless full-package refactor).
