# Convert264 — history

- **Nov 8, 2026** — v0.1.0: first CI-built release (18.2 MB), emulator
  conversion proven. Built in Google AI Studio as "Convert264".
- **Nov 9** — v0.1.1: testing completed; progress % fixed; 0-byte MediaStore
  bug fixed.
- **Nov 10** — v0.1.2, v0.1.3: Cancel button to plain Material text button.
  v0.1.4: full UI rebuild (stock Material 3), share-intent fix. v0.1.8:
  "USB-OTG ready" chip removed; trim shows the real video frame.
  v0.1.5–0.1.7 built, failed their own screenshot checks, deleted, never
  announced.
- **Nov 11** — v0.1.9: native launch screen, no chips, light + dark verified.
  Real-device proof 02:06: a real Pixel video (22.3 MB, 1:03) converted on
  his phone. v0.2.0: universal FFmpeg converter — queue, containers
  MP4/MKV/MOV/WebM, video H.264/H.265/VP9/MPEG-4/Copy, audio
  AAC/MP3/Opus/Vorbis/FLAC/Copy/None, quality + preset, per-file detail
  screen. APK 54.8 MB.
- **Nov 12** — Play Store goal set (100K downloads). Weekly report written
  to the Notion hub page. v0.2.1: in-app review prompt (once, after 3rd
  conversion), rate link, PRIVACY.md, store assets (icon, feature graphic,
  4 screenshots), full listing, AAB build in the release workflow.
- **Nov 13** — Goal raised to 1M downloads. app-product-owner adopted:
  goal file, Work Log, Decision Log, growth folder, daily sweep +
  weekly reflection jobs. v0.2.1 smoke test failed on an emulator boot
  flake (adb unreachable); re-run started.
- **Nov 13 (evening sweep)** — v0.2.1 smoke re-run (run 43375580799)
  FAILED with a real bug, not the boot flake: first queue file converted
  to a 0-byte MKV yet shown Done. Root cause verified from logcat + code:
  a late completion callback from a cancelled FFmpeg session deletes the
  new run's staging files, and success never checks the published file.
  Filed issue #1; fix WL-12 dispatched (ships as v0.2.2). Competitor
  study first pass done (competitors.md): Inverse AI 4.5★/182K, VidSoftLab
  4.5★/8M installs (stale since Nov 2025), SilentLexx 3.8★; repeated
  1-star themes are ads, paywalled formats, and dead/corrupt outputs.
