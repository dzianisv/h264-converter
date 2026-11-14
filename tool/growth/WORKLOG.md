# Convert264 — Work Log (the only task tracker)

Status: `[ ]` open, `[x]` done (with date + reason). Never delete a row.
Close with a dated reason.

## Open

- [x] WL-19 Second useful article: "How to convert MOV to MP4 without
  uploading your video" (Nov 14, 2026): same WL-08 pattern — FFmpeg copy
  (`-c copy`) + re-encode fallback + VLC method, what to avoid (grounded
  in the WL-07 1-star sampling), Convert264 mentioned once at the end.
  Targets the "convert mov to mp4" quick win (rel 5, diff 3). Both
  FFmpeg commands verified locally against a real file (copy + re-encode
  produce valid MP4). Published: docs/convert-mov-to-mp4.html, linked
  from the index footer, in sitemap.xml. Owner: Muse.
- [ ] WL-01 Sentry in the app. Needs a Sentry DSN — owner creates a Sentry
  project and hands over the DSN. Owner: founder. Due: Nov 19 (week 1).
- [ ] WL-02 Play Console developer account ($25, one-time). Owner: founder.
  Due: Nov 19 (week 1).
- [ ] WL-03 Stable upload keystore for the first AAB upload. CI signs with a
  throwaway key that changes every run — the CI AAB is a build artifact, not
  the upload. Options: keystore in GitHub Secrets, or a local build.
  Owner: founder. Due: Nov 19 (week 1).
- [x] WL-04 Landing page live (Nov 14, 2026 — pulled forward from Nov 19).
  https://dzianisv.github.io/h264-converter/ — pure HTML+CSS, no JS;
  title 45 chars, meta 144 chars, OG tags, robots.txt, sitemap.xml,
  llms.txt. GitHub Pages enabled from docs/ on main. Owner: Muse.
- [ ] WL-05 Real-Pixel test of v0.2.2 (queue, MKV output, dark mode, share
  sheet, OTG with a real drive). Only he has the phone. Owner: founder.
  Due: Nov 19.
- [x] WL-07 Competitor study: top 5 video converters on Play — rating, 1-star
  complaints, screenshots, update cadence. Owner: Muse. DONE Nov 14, 2026:
  first pass Nov 13 (ratings + complaints + cadence) + second pass Nov 14
  (screenshots bar + top-10 newest 1-star per app, live Play listings,
  in competitors.md). Two listing-identity flags recorded there
  (com.video_converter.video_compressor retitled; com.video.compress.and.convert
  now Code Craft, Inc.) — re-check on the next pass.
- [x] WL-08 First useful article: a how-to that works without the app
  (mention Convert264 once, at the end). DONE Nov 14, 2026 (pulled
  forward from Nov 26): "How to convert MKV to MP4 without uploading
  your video" — FFmpeg copy (`-c copy`) + re-encode fallback + VLC
  method, what to avoid (online uploaders, ad-heavy apps — grounded
  in the WL-07 1-star sampling), Convert264 mentioned once at the end.
  Published on the landing site: docs/convert-mkv-to-mp4.html, linked
  from the index footer, in sitemap.xml. Owner: Muse.
- [ ] WL-09 Video join — build it or drop it. Open question to the founder
  since Nov 11 ("join them" was read as joining the dropdowns). Owner:
  founder. No date — his answer.
- [ ] WL-10 Firebase Analytics / Crashlytics. Needs his Firebase project.
  Optional — Vitals + in-app review cover crashes and feedback until then.
  Owner: founder. No date.
- [x] WL-11 Score the searches to win (searches.md) on relevance,
  difficulty, search volume. DONE Nov 14, 2026 (pulled forward from
  Nov 20): 16 keywords scored 1–5 on relevance/difficulty grounded in
  the WL-07 competitor file; volume is a qualitative estimate only
  (no keyword tool / Play Console data yet — re-score from Console
  once live). 7 quick wins (relevance 5, difficulty ≤3: convert mkv
  to mp4, h264/vp9/webm/mkv/mov converter, no-ads, offline); 5 core
  fights (video converter, convert video, mp4, video converter free);
  "compress video" marked Avoid (not the product). Owner: Muse.
- [ ] WL-18 Pixel 9a conversion failure — root cause unknown. The
  v0.2.2 error line was too short to read. Needs Den's copied error
  details from v0.2.3 (tap the failed row → Show details → Copy
  error) + what file/settings he used. Owner: founder (error text)
  / Muse (fix once known).
- [ ] WL-13 App name — pick a name people understand. 'Convert264'
  reopened Nov 14 (owner): '264' means nothing to a normal person.
  Checked: VidConvert TAKEN on Play (Vinson Developers, $0.89, live
  since Sep 2026) + older VidConvert by Acro Media Studio; VidConverter
  taken on Play too; 'Video Converter' is the incumbent's name
  (VidSoftLab, 4.6 stars, 12K reviews, 100K+ downloads). Clear on Play:
  VidSwitch (recommended — says 'switch video formats', brandable,
  title 'VidSwitch – Video Converter' is 27 chars), ReVid (short but
  unclear). All four .com domains registered. Rename is display name +
  store title + repo/landing copy only — applicationId never changes.
  Owner: founder (his pick).
- [ ] WL-14 Payment model — stay free vs one-time pro unlock vs
  subscription. Framework rule (MVP with payment first, Nov 14): payment
  ships in the MVP, scale waits for the first paying customer. The app's
  edge as launched is free / no ads / no account — a real fork. Never
  invent a price or a tier. Owner: founder.
- [ ] WL-15 PostHog: install the Android SDK + run the self-driving
  wizard on the landing page; project key in the Secure Vault only.
  Owner: founder (PostHog project) / Muse (SDK + wizard).
- [x] WL-16 product-framework adopted Nov 14: dispatcher + 12 lanes
  installed from dzianisv/product-framework; product file
  products/convert264.md created + pushed; 4-role job set complete
  (daily loop + hourly supervisor added). Supersedes the Nov 13
  hourly-supervisor deferral. Owner: Muse.

## Done

- [x] WL-17 Full error UI (Nov 14, 2026): ConversionError(summary,
  details) end to end — full ffmpeg log tail (30 lines, never
  truncated) + return code + file/target; plain-language summary
  (ErrorMessages); ErrorDetailsCard shared by queue row + detail
  screen (selectable scrollable details, Copy error, Try again).
  PR #3 merged (54f077e). v0.2.3 FAILED its smoke check — test
  compile error (assertDoesNotExist unresolved), NOT an app bug;
  per release discipline the v0.2.3 release + tag were deleted and
  the fix shipped as v0.2.4 (PR #4: assertCountEquals + PR gate now
  compiles instrumented tests). v0.2.4 Release + PR green; smoke run
  43608544016 first attempt FAILED (queue test hit ComposeNotIdle in
  setNightMode before any conversion ran; error-details + share-intent
  tests passed) — re-run started 2026-11-14 15:47 UTC, verdict
  SETTLED Nov 14 ~16:36 UTC: smoke re-run SUCCESS (job "smoke"
  success, 48m58s), v0.2.4 verified green. Done Nov 14, 2026.
  Owner: Muse.

- [x] WL-12 Fix smoke failure: 0-byte MKV reported Done (issue #1).
  Per-session transcoder lifecycle + empty-file guard (PR #2, merge
  9e0a326, Stage 4 PASS comment 6870082714). Shipped as v0.2.2 (tag on
  89029d4; Release published Nov 14, 2026; smoke run 43546002038 green).
  Issue #1 closed. Done Nov 14, 2026.

- [x] WL-06 Verify v0.2.1 smoke re-run (run 43375580799). FAILED Nov 13:
  real bug, not the boot flake — queue test failed, first file
  sample_clip_2_vp9.mkv 0 bytes yet shown Done. Superseded by WL-12 /
  issue #1. Closed Nov 13, 2026.

- [x] WL-00 Store listing drafted, icon + feature graphic + 4 screenshots
  shipped, v0.2.1 with in-app review prompt and rate link, PRIVACY.md,
  AAB build in the release workflow. Done Nov 12, 2026.
- [x] WL-00b GitHub issues enabled, 0 open issues. Verified Nov 13, 2026.
- [x] WL-00c app-product-owner adopted; goal file, hub, Work Log, Decision
  Log, growth folder, daily sweep + weekly reflection jobs created.
  Done Nov 13, 2026.
