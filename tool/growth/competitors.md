# Convert264 — competitors

Researched Nov 13, 2026 (WL-07, first pass — ratings, 1-star complaints,
update cadence from Play/AppBrain search results; screenshots not yet
checked from the live listings). Sources: Google Play pages + AppBrain,
all read Nov 13, 2026. No numbers invented.

## 1. Video Converter, Compressor — Inverse AI

- Package: `com.video_converter.video_compressor`
- Rating: **4.5★, ~182K ratings** (181,984 on the live page read Nov 13)
- Updated: Oct 27, 2026 (frequent — updated within the last month)
- Free with ads + PRO paywall; conversion to MP4/MKV/AVI/3GP/FLV/MTS/M4V/MOV
  is a PRO feature. Batch queue included.
- 1-star complaints: lifetime purchase advertised $9.99 but charged $16.53;
  conversion formats locked behind PRO; ads (the reason Convert264 exists —
  tried Nov 8, 2026, rejected for ads + awful UI/UX).
- The category leader by review count. Bar to beat: 4.5★ at 182K ratings.

## 2. Video Converter — VidSoftLab

- Package: `jaineel.videoconvertor`
- Rating: **4.5★, ~80K ratings** (80,187 per AppBrain, data Nov 7, 2026)
- Installs: 5M+ on Play; AppBrain: 8M total, ~20K in the last 30 days
- Updated: Nov 22, 2025 (a year stale — update cadence is slow)
- Size: 64 MB. Contains ads + in-app purchases. FFmpeg-based (LGPL).
- 1-star complaints: "Does its job (60% of the time)"; "ads are awful";
  free tier limited, quality slightly degraded; **no queue option**.
- 8M installs with a year-old build: the demand is durable, the product
  is coasting. Beating it = same formats, no ads, a real queue, current build.

## 3. Compress Video Size Compressor — Inverse AI

- Package: `com.video.compress.and.convert`
- Rating: **4.2★, 922 ratings**
- Updated: Oct 31, 2026 (frequent)
- 1-star complaints: slow compression, crashes, quality loss, unable to
  share on social media. (Reviewers praise it when it works — the
  complaints are reliability, not the concept.)

## 4. Video Converter, Video Editor — Vidsoftech

- Package: `jaineel.videoeditor`
- Rating: **4.51★, 8.6K ratings**
- Installs: 500K+ on Play; AppBrain: 750K total, ~1.7K in the last 30 days
- Updated: Aug 28, 2026. Size: 56.2 MB. Contains ads.
- Smaller, steady player. No standout 1-star theme surfaced in this pass.

## 5. FFmpeg Media Encoder — SilentLexx

- Package: `com.silentlexx.ffmpeggui`
- Rating: **3.8★, ~4.1K ratings** (4,432 on the latest crawl)
- Updated: Aug 28, 2026 (Play) — cadence roughly yearly
- Free, exposes raw FFmpeg flags. Power users only.
- 1-star complaints: output video doesn't appear / appears hours later;
  going to background re-encodes the same file simultaneously, leaving
  corrupt files + dead battery; crashes when selecting the output folder;
  hard to navigate, not beginner-friendly. A video over 4 min gets
  auto-reconverted ~3 min after finishing (bug since the FFmpeg 6.0 update).
- The cautionary tale for Convert264: this is what a raw FFmpeg wrapper
  looks like when lifecycle bugs ship. Our WL-12 bug (0-byte file reported
  Done) is the same class — fix it before launch, not after.

## Notable mention: Video Transcoder — Branden Archer (open source)

- Package: `protect.videoeditor`
- Rating: 3.4★, ~630 ratings. Last updated **Jan 2019** — abandoned.
- Open source, FFmpeg, no ads, no IAPs, no network permission. Reviewers
  call it "the Android App Golden Standard" and love it despite the age.
- This is Convert264's exact niche (private, free, no-ads FFmpeg converter)
  and it has had no maintained occupant since 2019. The gap is real.

## Repeated complaints across competitors (the demand)

1. **Ads everywhere** — the #1 complaint in every top app. Convert264's
   no-ads edge is the headline, not a footnote.
2. **Paywalled formats / surprise charges** — PRO locks on basic conversion,
   lifetime price ≠ charged price. Free means free.
3. **Reliability: outputs that never appear, corrupt files, crashes** —
   VidSoftLab "60% of the time", SilentLexx background re-encode,
   Inverse AI compressor crashes. A converter that silently produces a
   dead file is the worst failure in the category (see WL-12).
4. **No queue / no batch** — VidSoftLab has none; Inverse AI has one behind
   its compressor. Convert264's queue is a differentiator, not a commodity.
5. **Hard to use** — SilentLexx is powerful but "not beginner friendly".
   Presets + one-tap convert win over flag exposure.

## What this means for Convert264

- The bar: 4.5★ is table stakes; the leaders hold it with ads + paywalls +
  stale builds. A 4.5★+ no-ads, current, reliable converter has a real opening.
- The 1-star list is our feature list: no ads, no paywalled formats,
  queue + batch, outputs that always appear (WL-12), simple presets.
- Target to beat first: VidSoftLab (8M installs, stale since Nov 2025) on
  reliability + freshness; Inverse AI (182K reviews) on honesty (no ads,
  no surprise charges).

## Remaining for WL-07 (due Nov 20)

- Screenshots bar: open each live Play listing and note what the first
  three screenshots promise (text fetch doesn't carry screenshots).
- Deeper 1-star sampling per app (top 10 1-star reviews each) to confirm
  the complaint ranking above.
