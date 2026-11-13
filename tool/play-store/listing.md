# Convert264 — Google Play listing (ready to paste into Play Console)

Status: prepared Nov 12, 2026. Not yet submitted — needs a Play Console developer
account ($25, one-time) and a signed release build (AAB).

## Store identity

- **App name:** Convert264
- **Store title (max 30 chars):** `Convert264 – Video Converter` (27)
- **Package:** `dev.dzianisv.convert264`
- **Category:** Video Players & Editors
- **Content rating:** Everyone (no user content, no ads, no purchases)
- **Price:** Free, no ads, no in-app purchases
- **Languages at launch:** English (UI is English; add Russian later if downloads justify it)

## Brand

- Mark: film strip curling into a circular convert arrow, cyan (#22D3EE) to
  purple (#8B5CF6) on navy (#0F172A). Already the launcher icon — keep it.
- Voice: plain. "Convert any video. On your device. No ads."
- The promise that makes it different from the top converters: everything
  happens on the phone. No upload, no account, no watermark, no ads.

## Short description (80 chars max)

`Convert any video on your device. MP4, MKV, MOV, WebM. No ads, no account.`

## Full description

Convert any video, right on your phone. No account, no ads, no watermark —
your videos never leave your device.

Pick a video or share one to Convert264, choose the format, and press play.

WHAT YOU CAN DO
- Convert a whole queue of videos in one go
- Choose the container: MP4, MKV, MOV, WebM
- Choose the video codec: H.264, H.265, VP9, MPEG-4 — or copy the stream as-is
- Choose the audio codec: AAC, MP3, Opus, Vorbis, FLAC — or keep it, or drop it
- Set quality and encoder preset for each codec
- Trim start and end with a real preview of the frame you're cutting

BUILT FOR REAL USE
- Share a video from Files or Gallery straight into the converter
- Works with videos from a USB drive
- Real progress while it converts; open and share when it's done
- Dark mode included

PRIVACY
Convert264 has no account, no ads, and no tracking. Conversion runs entirely
on your phone. Nothing is uploaded anywhere.

## Keywords (for the listing text, not a Play field)

video converter, convert video, mp4 converter, mkv converter, mov converter,
webm converter, video trimmer, compress video, h264 converter, vp9 converter,
convert video to mp4, video converter no ads, offline video converter

## Assets (in tool/play-store/)

- `icon-512.png` — 512×512 store icon (the existing mark)
- `feature-graphic-1024x500.png` — 1024×500 feature graphic
- `store-01-launch.png` — 1080×1920, "Convert any video"
- `store-02-queue.png` — 1080×1920, "A whole queue, one tap"
- `store-03-trim.png` — 1080×1920, "Trim with a real preview"
- `store-04-converting.png` — 1080×1920, "Watch it convert"
- All four screenshots show the real app (emulator captures from the v0.2.0
  smoke run). Replace with Pixel captures once the real phone is tested.

## Privacy policy

`PRIVACY.md` in the repo. Play Console link:
https://github.com/dzianisv/h264-converter/blob/main/PRIVACY.md

## Analytics and feedback (the loop that keeps it not-buggy)

1. **Android Vitals (automatic, no code):** crash rate and ANR rate from Play
   Console. Targets: crash-free sessions above 99.5%, ANR below 0.47%.
   Any crash that shows up here gets a fix release, not a note.
2. **Play in-app review (v0.2.1, in the app):** after the 3rd successful
   conversion, ask once for a rating through Play's own prompt. Never nag.
3. **Rate the app (v0.2.1, in the app):** a footer link to the Play listing.
4. **Store stats (Play Console):** listing conversion rate, installs by
   country, rating trend. Review the numbers weekly; A/B test the listing
   (screenshots first) once installs pass 1,000.
5. **Firebase Analytics + Crashlytics:** NOT yet added — it needs a Firebase
   project in his Google account. Steps: create project in Firebase Console,
   add Android app `dev.dzianisv.convert264`, download google-services.json,
   hand it to Muse, who wires the SDK and ships v0.2.2. Until then, Vitals
   covers crashes and the review prompt covers feedback.

## Launch checklist (owner)

- [ ] Play Console developer account created ($25) — founder, needs his payment
- [ ] Privacy policy URL set (PRIVACY.md link above)
- [ ] Signed AAB built from a tagged release (v0.2.1 or later) — Play needs AAB,
      not the sideload APK
- [ ] Icon, feature graphic, 4 screenshots uploaded
- [ ] Listing text pasted from this file
- [ ] Content rating questionnaire filled (Everyone)
- [ ] Closed testing track first (20 testers not needed for personal account
      anymore — check current Play policy at submission time), then production
- [ ] Watch Vitals daily for the first week
- [ ] Real Pixel test of the shipped build before production rollout

## The road to 100K downloads

Honest version: 100K is a goal, not a forecast. The plan is boring and it is
the whole plan.

1. Ship a converter that does not crash and does not lie (no fake claims in
   the listing — every sentence above is true of the current build).
2. Win on the one thing the top converters fail at: no ads, no watermark,
   no upload. That is the headline in every screenshot.
3. Ratings: the review prompt after the 3rd conversion, plus a reply to
   every review under 4 stars within 48 hours.
4. ASO: title carries "Video Converter", description repeats the formats
   people search for (mp4, mkv, webm, h264, vp9).
5. Channels after Play: F-Droid (open source, no trackers — a real audience
   for this app), GitHub README, one launch post on Reddit r/AndroidApps and X.
6. What would kill the goal: a crash spike in week one, or a listing claim
   the app can't back up. Both are checked before every release.
