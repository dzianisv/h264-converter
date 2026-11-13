# Convert264 — Decision Log

Every closed decision: decision, date, decider, reason in one line. A
decision in this log is closed — a new session does not re-open it without
new evidence.

- 2026-11-08 — Build Convert264 (Kotlin + Jetpack Compose) instead of using
  an ad-filled converter. Decider: owner. Reason: the Play Store app he
  tried was full of ads with awful UI.
- 2026-11-10 — Standard Material 3 components only; real screenshots only,
  never mockups. Decider: owner. Reason: the first UI was AI-generated
  slop with no design system.
- 2026-11-10 — Share-intent fix: no blanket `*/*` filter. Decider: Muse.
  Reason: a null-type sender still cannot see the app, accepted as a
  platform limitation.
- 2026-11-11 — Engine is FFmpeg (ffmpeg-kit-maintained 8.1.7, full GPL);
  AV1 left out. Decider: owner (direction) / Muse. Reason: AV1 took over
  150s for a 3s clip in testing.
- 2026-11-11 — "USB-OTG ready" claim removed from the UI. Decider: Muse.
  Reason: never tested with a real drive; no unproven claims in the UI.
- 2026-11-12 — Play Store goal; free, no ads, no account; store title
  "Convert264 – Video Converter"; brand mark stays (film strip to convert
  arrow, cyan to purple on navy). Decider: owner. Reason: the edge over
  the top converters is on-device, no upload, no watermark.
- 2026-11-12 — Analytics = Android Vitals + in-app review + rate link;
  Firebase deferred until his Firebase project. Decider: Muse. Reason:
  no Firebase project exists; Vitals covers crashes, review covers
  feedback.
- 2026-11-12 — Release workflow builds the AAB alongside the APK; the CI
  AAB is a build artifact, not the Play upload. Decider: Muse. Reason:
  CI signs with a throwaway keystore that changes every run.
- 2026-11-13 — Goal raised 100K → 1M downloads. Decider: owner.
- 2026-11-13 — app-product-owner adopted; daily sweep + weekly reflection
  created; hourly supervisor deferred until Stage 1. Decider: owner
  (directed the daily run) + Muse (recorded reason: pre-launch, no live
  users, hourly checks would be noise).
