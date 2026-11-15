# Convert264 — Work Log (the only task tracker)

Status: `[ ]` open, `[x]` done (with date + reason). Never delete a row.
Close with a dated reason.

## Open

- [ ] WL-31 Unverified USB-drive claim in landing page + listing (Nov 15,
  2026): the Nov 11 decision removed "USB-OTG ready" from the UI (never
  tested with a real drive; no unproven claims), but the claim survived
  in docs/index.html ("From Files, Gallery, or a USB drive") and
  tool/play-store/listing.md ("Works with videos from a USB drive").
  Remove both until WL-05 proves OTG on the real Pixel. Same pass:
  listing "road to 100K" is stale (goal is 1M since Nov 13) and
  "ships v0.2.2" is stale (current v0.2.5). Owner: Muse.
- [ ] WL-30 Directory submissions (Nov 15, 2026): per social-marketing
  lane + Week 2 plan — submit Convert264 to free directories
  (AlternativeTo, DevHunt, BetaList, Product Hunt, Show HN, Indie
  Hackers, category directories) for permanent backlinks. Submission
  copy + account requirements tracked in `tool/growth/directories.md`.
  PROGRESS Nov 15 (fec69cd, CI 43725883921 green): research + copy done
  — BetaList RULED OUT (paid only + own domain required, landing is
  github.io); AlternativeTo needs a free account (sign-in wall verified
  live); Product Hunt / Show HN / Indie Hackers need his accounts;
  DevHunt weak fit (dev-tools only, ~6mo free queue); F-Droid VERIFIED
  Nov 15 (see directories.md): ffmpeg-kit alone does NOT rule it out
  (GPL from Maven Central, a trusted repo per the Inclusion Policy) —
  the real blocker is Play in-app review (`review-ktx`, a non-free
  library), which needs a `foss` flavor; flavor work deferred until
  Play is live, still needs his GitLab account + fdroiddata MR. No
  submission completed — all remaining need an owner account. Owner:
  Muse (submissions); founder (account creation).
- [ ] WL-01 Sentry in the app. Needs a Sentry DSN — owner creates a Sentry
  project and hands over the DSN. Owner: founder. Due: Nov 19 (week 1).
- [ ] WL-02 Play Console developer account ($25, one-time). Owner: founder.
  Due: Nov 19 (week 1).

- [ ] WL-03 Stable upload keystore for the first AAB upload. CI signs with a
  throwaway key that changes every run — the CI AAB is a build artifact, not
  the upload. Options: keystore in GitHub Secrets, or a local build.
  Owner: founder. Due: Nov 19 (week 1).

- [ ] WL-05 Real-Pixel test of v0.2.4 (queue, MKV output, dark mode, share
  sheet, OTG with a real drive). v0.2.4 APK sent Nov 14 — retry the same
  conversion, paste the copied error back if it fails. Only he has the
  phone. Owner: founder. Due: Nov 19.

- [ ] WL-09 Video join — build it or drop it. Open question to the founder
  since Nov 11 ("join them" was read as joining the dropdowns). Owner:
  founder. No date — his answer.

- [ ] WL-10 Firebase Analytics / Crashlytics. Needs his Firebase project.
  Optional — Vitals + in-app review cover crashes and feedback until then.
  Owner: founder. No date.

- [ ] WL-18 Pixel 9a conversion failure — root cause unknown. The
  v0.2.2 error line was too short to read. v0.2.4 ships the full-error
  UI — needs Den's copied error details from v0.2.4 (tap the failed row
  → Show details → Copy error) + what file/settings he used. Owner:
  founder (error text) / Muse (fix once known).

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

## Done

- [x] WL-29 Tenth useful article: "How to trim a video on Android without
  uploading it" (Nov 15, 2026): same WL-08 pattern — find cut points
  (ffprobe duration), FFmpeg exact trim (re-encode, `-ss`/`-to` after
  `-i`) + fast trim (copy, `-ss` before `-i` with `-t` duration) +
  VLC record method, what to avoid (grounded in the WL-07 1-star
  sampling), Convert264 mentioned once at the end. Targets the
  secondary keywords "trim video android" + "video trimmer" (rel 4,
  diff 4) — trim with a real preview is a real app feature with no
  article yet. All three FFmpeg commands verified locally against a
  real 10s H.264+AAC file: output-opts re-encode trim → exactly
  3.000s; input-`-ss` + `-t 3` copy → 3.1s (keyframe rounding);
  input-`-ss` + `-to 5` copy → 5.1s (gotcha: `-to` counts from the
  seek point — documented in the article). Published:
  docs/trim-video.html, linked from the index footer, in sitemap.xml
  and llms.txt. Also fixed in the same commit: llms.txt still linked
  the v0.2.4 APK — now v0.2.5. Owner: Muse.

- [x] WL-28 Dead template deps contradict the no-tracking claim (Nov 15,
  2026): dead Firebase AI + App Check, OkHttp, Retrofit, Moshi deps
  removed (PR #6, merge e9103f5; google-services plugin + block also
  dropped after CI fix faeb133); Data Safety draft added to
  `tool/play-store/listing.md` + "compress video" dropped from the
  listing keywords (PR #7, merge 0573fa7). Shipped as v0.2.5
  (versionCode 16, tag, Release published 2026-11-15 10:56 UTC, APK +
  AAB); smoke run 43706165212 SUCCESS in 38m10s (verified Nov 15 by
  the hourly supervisor, which waited out the in-flight smoke rather
  than re-running it). Owner: Muse.

- [x] WL-27 Ninth useful article: "How to convert video to MP4 without
  uploading it" (Nov 15, 2026): same WL-08 pattern — check codec first
  (ffprobe), FFmpeg copy (`-c copy`, H.264+AAC) + re-encode fallback
  (H.264+AAC, `-c:v libx264 -crf 20 -c:a aac`, works from any input:
  MKV/MOV/WebM/AVI) + VLC method (Video – H.264 + MP3 (MP4) profile),
  what to avoid (grounded in the WL-07 1-star sampling), Convert264
  mentioned once at the end. Targets the top core-fight job
  "convert video to mp4" (rel 5, diff 4, High) + "mp4 converter"
  (rel 5, diff 4) — first core-fight article now that all 7 quick wins
  are covered. All three FFmpeg commands verified locally against real
  files (VP9 MKV: re-encode → valid MP4 h264; H.264 MP4: copy → valid
  MP4 h264; VP9 WebM: re-encode → valid MP4 h264; ffprobe confirms).
  Published: docs/convert-to-mp4.html, linked from the index footer,
  in sitemap.xml and llms.txt. Owner: Muse.

- [x] WL-26 llms.txt lists the 8 how-to guides (Nov 15, 2026):
  sitemap + index footer linked all 8 articles but llms.txt (the
  AI-citation file) mentioned none. Added a "How-to guides" section
  with all 8 titles + URLs. Commit 349cb54; Android PR run
  43691048357 success; Pages deploy 43691048135 success; live
  llms.txt HTTP 200 with all 8 links. Owner: Muse.

- [x] WL-25 Eighth useful article: "How to convert video to MOV without
  uploading it" (Nov 15, 2026): same WL-08 pattern — check codec first
  (ffprobe), FFmpeg copy (`-c copy` into MOV) + re-encode fallback
  (H.264+AAC, `-c:v libx264 -crf 20 -c:a aac`), VLC method (Video –
  H.264 + MP3 (MOV) profile, verified from two sources), what to avoid
  (grounded in the WL-07 1-star sampling), Convert264 mentioned once
  at the end. Targets the last uncovered quick win: "mov converter"
  (rel 5, diff 3). All three FFmpeg commands verified locally against
  real files (H.264 MP4: copy → valid MOV h264; re-encode → valid MOV
  h264; VP9 MKV: copy refused "vp9 only supported in MP4", re-encode →
  valid MOV h264; ffprobe confirms). Published:
  docs/convert-to-mov.html, linked from the index footer, in
  sitemap.xml. Commit 4a40c7c; Android PR run 43677862009 success;
  Pages deploy 43677862058 success; article live HTTP 200. Owner: Muse.

- [x] WL-24 Seventh useful article: "How to convert videos offline on
  Android without ads" (Nov 15, 2026): same WL-08 pattern — the
  no-network-permission test (Settings → Apps → Permissions) + ffprobe
  check, FFmpeg copy (`-c copy`) + re-encode fallback (H.264+AAC,
  `-c:v libx264 -crf 20 -c:a aac`), on-phone method = Convert264
  (on-device, no INTERNET permission, airplane-mode proof), what to
  avoid (grounded in the WL-07 1-star sampling), Convert264 mentioned
  once at the end. Targets the last two uncovered quick wins: "offline
  video converter" (rel 5, diff 3) and "video converter no ads"
  (rel 5, diff 3). All three commands verified locally against a real
  H.264 sample (ffprobe → h264; copy → valid MP4 h264; re-encode →
  valid MP4 h264). Published: docs/convert-offline-no-ads.html, linked
  from the index footer, in sitemap.xml. Commit 03368cb; Android PR run
  43673496039 success; Pages deploy 43673495197 success; article live
  HTTP 200 (landing HTTP 200). Owner: Muse.

- [x] WL-23 Sixth useful article: "How to convert video to MKV without
  uploading it" (Nov 15, 2026): same WL-08 pattern — check codec first
  (ffprobe), FFmpeg copy (`-c copy` into MKV) + re-encode fallback
  (H.264+AAC, `-c:v libx264 -crf 20 -c:a aac`) + VP9+Opus variant
  (`-c:v libvpx-vp9 -crf 30 -b:v 0 -c:a libopus`) + VLC method
  (H.264 + MP3 (MKV) profile), what to avoid (grounded in the WL-07
  1-star sampling), Convert264 mentioned once at the end. Targets the
  "mkv converter" quick win (rel 5, diff 3). All three FFmpeg commands
  verified locally against a real MP4 file (H.264+AAC sample: copy →
  valid MKV with h264/aac; re-encode → valid MKV with h264/aac;
  VP9+Opus → valid MKV with vp9/opus; ffprobe confirms). Published:
  docs/convert-to-mkv.html, linked from the index footer, in
  sitemap.xml. Commit 83be3bc; Android PR run 43669042534 success;
  Pages deploy 43669041866 success; article live HTTP 200. Owner: Muse.

- [x] WL-22 Fifth useful article: "How to convert MP4 to WebM without
  uploading your video" (Nov 15, 2026): same WL-08 pattern — no copy
  shortcut (H.264 is not valid in WebM), VP9 re-encode (`-c:v libvpx-vp9
  -crf 30 -b:v 0 -c:a libopus`) + faster/smaller variant (`-crf 35
  -deadline good -cpu-used 2`) + VLC method (VP8 profile), what to avoid
  (grounded in the WL-07 1-star sampling), Convert264 mentioned once at
  the end. Targets the "webm converter" quick win in the reverse
  direction + "vp9 converter" (rel 5, diff 1). Both FFmpeg commands
  verified locally against a real MP4 file (H.264 video-only sample:
  VP9 WebM out, ffprobe confirms vp9). Published:
  docs/convert-mp4-to-webm.html, linked from the index footer, in
  sitemap.xml. Owner: Muse.

- [x] WL-21 Fourth useful article: "How to convert video to H.264 without
  uploading it" (Nov 15, 2026): same WL-08 pattern — check codec first
  (ffprobe), encode to H.264 (`-c:v libx264 -crf 20 -c:a aac`) + VLC
  method, what to avoid (grounded in the WL-07 1-star sampling),
  Convert264 mentioned once at the end. Targets the "h264 converter"
  quick win (rel 5, diff 1 — literally the app name, no competitor
  brands H.264). Both commands verified locally against a real WebM
  file (VP9+Opus → H.264+AAC MP4, ffprobe confirms h264/aac).
  Published: docs/convert-to-h264.html, linked from the index footer,
  in sitemap.xml. Commit 81d3dfd; Android PR run 43665017239 success;
  Pages deploy 43665016606 success; article live HTTP 200. Owner: Muse.

- [x] WL-20 Third useful article: "How to convert WebM to MP4 without
  uploading your video" (Nov 15, 2026): same WL-08 pattern — FFmpeg copy
  (`-c copy`) + re-encode fallback + VLC method, what to avoid (grounded
  in the WL-07 1-star sampling), Convert264 mentioned once at the end.
  Targets the "webm converter" quick win (rel 5, diff 2). Both FFmpeg
  commands verified locally against a real WebM file (VP9+Opus:
  copy → valid MP4 with VP9+Opus; re-encode → H.264+AAC). Published:
  docs/convert-webm-to-mp4.html, linked from the index footer, in
  sitemap.xml. Commit 1de2551; Android PR run 43664353001 success;
  Pages deploy 43664352669 success; article live HTTP 200. Owner: Muse.

- [x] WL-19 Second useful article: "How to convert MOV to MP4 without
  uploading your video" (Nov 14, 2026): same WL-08 pattern — FFmpeg copy
  (`-c copy`) + re-encode fallback + VLC method, what to avoid (grounded
  in the WL-07 1-star sampling), Convert264 mentioned once at the end.
  Targets the "convert mov to mp4" quick win (rel 5, diff 3). Both
  FFmpeg commands verified locally against a real file (copy + re-encode
  produce valid MP4). Published: docs/convert-mov-to-mp4.html, linked
  from the index footer, in sitemap.xml. Owner: Muse.

- [x] WL-04 Landing page live (Nov 14, 2026 — pulled forward from Nov 19).
  https://dzianisv.github.io/h264-converter/ — pure HTML+CSS, no JS;
  title 45 chars, meta 144 chars, OG tags, robots.txt, sitemap.xml,
  llms.txt. GitHub Pages enabled from docs/ on main. Owner: Muse.

- [x] WL-07 Competitor study: top 5 video converters on Play — rating, 1-star
  complaints, screenshots, update cadence. Owner: Muse. DONE Nov 14, 2026:
  first pass Nov 13 (ratings + complaints + cadence) + second pass Nov 14
  (screenshots bar + top-10 newest 1-star per app, live Play listings,
  in competitors.md). Two listing-identity flags recorded there
  (com.video_converter.video_compressor retitled; com.video.compress.and.convert
  now Code Craft, Inc.) — re-check on the next pass. RE-CHECKED Nov 15, 2026:
  both resolved in competitors.md — the first pass had conflated two distinct
  Inverse AI listings (converter = com.inverseai.video_converter, 18M installs,
  ~209K ratings, the real leader); Code Craft identity confirmed unchanged.

- [x] WL-08 First useful article: a how-to that works without the app
  (mention Convert264 once, at the end). DONE Nov 14, 2026 (pulled
  forward from Nov 26): "How to convert MKV to MP4 without uploading
  your video" — FFmpeg copy (`-c copy`) + re-encode fallback + VLC
  method, what to avoid (online uploaders, ad-heavy apps — grounded
  in the WL-07 1-star sampling), Convert264 mentioned once at the end.
  Published on the landing site: docs/convert-mkv-to-mp4.html, linked
  from the index footer, in sitemap.xml. Owner: Muse.

- [x] WL-11 Score the searches to win (searches.md) on relevance,
  difficulty, search volume. DONE Nov 14, 2026 (pulled forward from
  Nov 20): 16 keywords scored 1–5 on relevance/difficulty grounded in
  the WL-07 competitor file; volume is a qualitative estimate only
  (no keyword tool / Play Console data yet — re-score from Console
  once live). 7 quick wins (relevance 5, difficulty ≤3: convert mkv
  to mp4, h264/vp9/webm/mkv/mov converter, no-ads, offline); 5 core
  fights (video converter, convert video, mp4, video converter free);
  "compress video" marked Avoid (not the product). Owner: Muse.

- [x] WL-16 product-framework adopted Nov 14: dispatcher + 12 lanes
  installed from dzianisv/product-framework; product file
  products/convert264.md created + pushed; 4-role job set complete
  (daily loop + hourly supervisor added). Supersedes the Nov 13
  hourly-supervisor deferral. Owner: Muse.

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

## Session reflections (daily, 21:48 WITA — verdict per run)

- [x] Nov 15, 2026 — NOT STOPPED (healthy, no-op day). Verdict from live
  readback 13:50 UTC: 0 open GitHub issues, 0 open PRs, last 5 CI runs
  green (latest 43715411580 pages / 43715410575 Android PR), v0.2.5
  latest release (2026-11-15T10:56:54Z), landing + trim article HTTP
  200, working tree clean at 08badf4. All 5 jobs enabled (daily loop,
  hourly supervisor, daily growth sweep, weekly reflection, this
  reflection). Daily loop last ran Nov 14 17:48 PST succeeded (WL-22);
  growth sweep same time succeeded (WL-21); hourly supervisor ran
  every hour through 05:40 PST (WL-28 closed 03:40, WL-29 shipped
  04:40, 05:40 no-op verified). The 02:40 supervisor run was marked
  failed only because its smoke test was still in flight — the 03:40
  run verified it green and closed WL-28; self-resolved, not a stop.
  Weekly reflection has no runs yet — expected, first Monday run is
  Nov 16 (job created Nov 14). All 10 open rows are founder-gated
  (WL-01/02/03/05/09/10/13/14/15/18), none past due; the article lane
  is exhausted (10 articles, all quick wins + top core fight + trim
  covered). No skill fix needed — the Nov 14 re-check rule is being
  followed. Nothing new for Den; no chat report (healthy no-op day).
  Note for the Nov 16 daily loop: next Muse-owned work is directory
  submissions / listing refresh (supervisor flagged 04:40), since no
  further articles are planned.
