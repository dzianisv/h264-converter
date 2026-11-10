# Convert264

Clean, ad-free Android video converter: queue videos (or share them
from File Manager), trim start/end, convert to any of the supported
codecs and containers.

- Native Android, Kotlin + Jetpack Compose, Material 3
- Queue of files to convert, each with a thumbnail + length; Play
  converts the queue one by one, Pause parks the current file,
  Stop returns it to Queued, Clear empties the queue
- Pick via Storage Access Framework or via Android Share intent
  (`ACTION_SEND` / `ACTION_SEND_MULTIPLE` / `ACTION_VIEW` — every
  shared video joins the queue)
- Per-file detail screen (a real navigation destination): trim
  preview + trim controls + per-file codec/container/audio/bitrate
  overrides, on one screen without scrolling
- Transcoder: FFmpeg via the maintained ffmpeg-kit fork
  (`dev.ffmpegkit-maintained:ffmpeg-kit-full-gpl:8.1.7`, FFmpeg 8.1
  LTS — the original `com.arthenica` artifacts were retired and
  removed from Maven Central in 2025). The `-gpl` build is GPL-3.0
  licensed (libx264/libx265).
- While converting: status bar with a real progress line (percent
  from ffmpeg's own statistics output, current/total time, elapsed
  time, Cancel) plus a per-row progress bar in the queue
- When finished: Open and Share per file, plus Recent conversions
  history with Open/Share
- Output: `Movies/Convert264/<name>_<codec>.<ext>` (never overwrites)
- "Try Sample Clip" generates a 3-second test video on-device, so
  the app can be tested end to end with no local files
- No ads, no account, fully offline

Originally generated in Google AI Studio
(https://aistudio.google.com/apps/6613a05e-0b02-449f-b2bb-c5e0dd872b67),
then handed off to GitHub + CI for a real build and emulator test.
v0.2.0 replaced the original Media3 Transformer engine (MP4 only,
device-dependent encoders) with FFmpeg.

## Supported formats (v0.2.0)

One place in code defines the matrix —
`app/src/main/java/com/example/model/ConversionProfiles.kt` — and
the dropdowns filter each other, so an impossible combination can
never be selected (e.g. picking WebM narrows video codecs to VP9
and audio to Opus/Vorbis). Resolution is always the source
resolution; audio bitrate is fixed at 128 kbps (FLAC is lossless
and takes no bitrate); the customizable bitrate is the video one.

| Container | Video codecs | Audio codecs |
|---|---|---|
| MP4 | H.264 (libx264), H.265 (libx265), MPEG-4, Copy | AAC, MP3, FLAC, Copy, No audio |
| Matroska (MKV) | H.264, H.265, VP9 (libvpx-vp9), MPEG-4, Copy | AAC, MP3, Opus, Vorbis, FLAC, Copy, No audio |
| MOV | H.264, H.265, MPEG-4, Copy | AAC, MP3, FLAC, Copy, No audio |
| WebM | VP9, Copy | Opus, Vorbis, Copy, No audio |

Video bitrate presets: Low · 2 Mbps, Medium · 5 Mbps, High ·
10 Mbps, Custom (100–100,000 kbps). Encoder preset where the codec
has one: x264/x265 `ultrafast…veryslow` (default `medium`), VP9
`cpu-used 0–5` (default `2 · Balanced`).

Deliberately left out: AV1 (libaom-av1) — the v0.2.0 spike
proved it unusable on the emulator: a 3s clip at cpu-used 8
(fastest) did not finish in 150s (0 bytes out), while H.264
took 47s and VP9 79s for the same clip; video merge/join;
resolution control (dropped with the Media3 engine — source
resolution only).

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
The FFmpeg AAR makes the APK much bigger than the Media3 builds
(~18 MB → ~70 MB debug with x86_64 .so files; release arm64-v8a
only — see the release notes for the exact size).

## Emulator smoke test

`.github/workflows/emulator-smoke.yml` runs the in-process Compose
instrumented tests `app/src/androidTest/.../Convert264SmokeTest.kt`,
`ShareIntentSmokeTest.kt` and `FfmpegSpikeTest.kt`
(`gradle connectedDebugAndroidTest`) on an API 35 emulator. The
runner script is `tool/smoke-test.sh`.

`Convert264SmokeTest` (v0.2.0): launch (light + dark screenshots,
no chips/claims), "Try sample clip" → one queue entry with a real
thumbnail + duration, a second generated clip joins the queue (2
entries), settings dropdowns select MKV / VP9 / Opus (dropdown-open
screenshot), tapping an entry opens the detail screen and asserts
its Convert button is displayed without scrolling, Play → converting
card → Stop returns the file to Queued, Clear queue empties it,
re-add both files, Play → Pause (row shows "Paused") → Play resumes
→ both files Done with Open + Share, and two real `_vp9.mkv` files
on disk with no 0-byte phantom rows in MediaStore. Progress is
gated: ffmpeg must report at least one real percent via its
statistics callback.

`FfmpegSpikeTest` proves the engine itself: a real H.264/MP4 and a
real VP9/MKV conversion in the emulator, plus an AV1/WebM timing
probe (log only, never fails — it decided whether AV1 ships).

Progress — what the emulator proved: ffmpeg reports a real percent
(the statistics callback's output time / trimmed duration), so the
bar ramps whenever the encode is slow enough to observe; before the
first statistics callback the bar is indeterminate plus live elapsed
time. A time-based percent estimate would still be fabrication, so
the app does NOT do that.

Evidence from each smoke run is kept under
`tool/smoke-screenshots/<run-id>/` (in-test screenshots at every
step, screen-recorded video chunks, every output file ffprobed,
smoke.log) and uploaded as a CI artifact (30-day retention) — see
the `Emulator Smoke Test` workflow run for the release under test.

## Package

Application ID: `dev.dzianisv.convert264` (Kotlin source stays under
the `com.example` namespace the AI Studio export generated; only the
application ID was changed, to avoid a needless full-package refactor).
