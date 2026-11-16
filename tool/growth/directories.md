# Convert264 — directory submissions (WL-30)

Week 2 plan item (directories in the first week, social-marketing lane).
Researched 2026-11-15 by the hourly supervisor. Sources: dev.to "35 Best
Directories" (Sep 2026), getolune.com launch-venue rules (Oct 2026),
okara.ai "Product Hunt Alternatives" (Sep 2026), AlternativeTo add page
(live read 2026-11-15).

## Submission copy (use verbatim everywhere)

- **Name:** Convert264
- **URL:** https://dzianisv.github.io/h264-converter/
- **Tagline (48 chars):** Free on-device Android video converter — no ads
- **Short description (300 chars):** Convert264 converts videos on your
  Android phone — MP4, MKV, MOV, WebM with H.264, H.265, VP9. Trim with a
  real preview, queue multiple files. No ads, no account, no watermark,
  and no internet permission — your videos never leave your device.
- **Long description (500 chars):** Convert264 is a free video converter
  for Android that works entirely on your phone. Convert to MP4, MKV, MOV
  or WebM with H.264, H.265, VP9, MPEG-4 or Copy video and AAC, MP3, Opus,
  Vorbis, FLAC or Copy audio. Trim with a real preview and queue multiple
  files. No ads, no account, no watermark, no internet permission — your
  videos never leave your device. Free and open source.
- **Category:** Video Players & Editors / Video Converter
- **Tags:** video converter, mp4 converter, mkv converter, h264 converter,
  offline converter, no ads, open source
- **Logo/icon:** `tool/play-store/icon-512.png` (512×512, verified on disk)
- **AlternativeTo "alternative to" targets (from WL-07 competitors.md):**
  Video Converter by VidSoftLab, Video Converter/Compressor by Inverse AI
  (`com.inverseai.video_converter`), FFmpeg Media Encoder by SilentLexx,
  Video Transcoder by Branden Archer (open source)

## Status

| Directory | Cost | Account needed | Status 2026-11-15 |
| --- | --- | --- | --- |
| AlternativeTo | Free ($5 priority review — do not pay) | Yes — free account, sign-in required to add a product | IN PROGRESS Nov 16 — email-signup route (vibetechnologies@gmail.com, browser-generated password; no Google sign-in, no 2SV). Google OAuth was the wrong route: it stops at Den's 2SV by design. |
| Product Hunt | Free | Yes — personal account; new accounts wait ≥1 week; 1 launch / 6 months; Tue–Thu 12:01am PT | PLANNED — needs his account + launch timing; do not launch until the Play listing exists (waitlists without immediate access are not featured; the GitHub APK is immediate access, but a Play link converts better) |
| Show HN | Free | Yes — HN account with history; restricted for new users since Mar 2026 | IN PROGRESS Nov 16 — email-signup route (same as AlternativeTo); attempt and record the exact restriction error if new users are still blocked |
| Indie Hackers | Free | Yes — account | IN PROGRESS Nov 16 — email-signup route (same as AlternativeTo); submit after the route validates |
| BetaList | Paid only (price at checkout; no free option since 2026) + own domain required (github.io subdomain rejected) | — | RULED OUT — paid and our landing is a github.io subdomain |
| DevHunt | Free listing (nofollow); $19 launch / $49 boosted | Yes — GitHub login; dev-tools only | WEAK FIT — consumer Android app, not a dev tool; free queue ~6 months; revisit only if a GitHub-login route opens |
| SaaSHub | Free to submit | Yes — account | WEAK FIT — SaaS directory, not an Android-app directory |
| F-Droid | Free | GitLab account for the fdroiddata MR | POSSIBLE — verified Nov 15: ffmpeg-kit alone does NOT rule it out (GPL, from Maven Central — a trusted repo per the Inclusion Policy, which allows prebuilt FLOSS binaries from Maven Central; the prebuilt .so inside the AAR may still draw a scanner flag — only an fdroiddata MR attempt settles it). Real blocker: Play in-app review (`com.google.android.play:review-ktx`, app/build.gradle.kts:105) is a non-free library — needs a `foss` flavor without it. Flavor work DONE Nov 16 (WL-35: play/foss product flavors, review-ktx play-only, CI proves both variants); still needs a GitLab account + MR (owner) |
| Uneed / Microlaunch / Fazier / TinyLaunch | Free queues (weeks–months out) | Yes — account on each | NOT STARTED — email-signup route (same as AlternativeTo); submit after the route validates |

## Rule for this lane

- Never pay for a listing, fast-track, or boost without the owner's
  explicit approval (framework rule: never spend his money).
- Most directories now go via email signup with vibetechnologies@gmail.com and a browser-generated password (Nov 16 correction — Google OAuth stops at Den's 2SV by design, so it is the wrong route). Only Product Hunt still needs his personal account + launch timing, and F-Droid needs his GitLab account. Everything else in this file is ready to paste.
- Do not submit the same product twice to one directory.
