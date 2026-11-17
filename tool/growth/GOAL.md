# Convert264 — goal file (app-product-owner)

Owner: Dzianis Vashchuk. Product owner: Muse, running the `app-product-owner`
skill (`dzianisv/skills-internal`, local copy in `~/workspace/custom-skills/`).
Repo: `dzianisv/h264-converter`. Package: `dev.dzianisv.convert264`.
Hub: Notion "ffmpeg app" page. Chat: the "ffmpeg app" side chat.
Goal record: `Convert264: 1M downloads on Google Play` (slug
`convert264-1m-downloads-on-google-play`).

## Goal (owner's words)

Publish on Google Play, not buggy. Make it the top video converting app.
1M downloads. (Nov 12: 100K. Nov 13: raised to 1M.)

## Acceptance (skill default)

- Good Google Play reviews — target 4.5+, every 1–3 star review answered in
  24–48 hours.
- Zero open GitHub issues.
- All Sentry repeats fixed fundamentally. (Sentry not installed yet — gap.)
- Zero open bug reports.

## Maturity stage

**Stage 0 — pre-launch.** Not on Play yet. Evidence to move to Stage 1: Play
listing live, Sentry receiving events, issues enabled (done: issues enabled,
0 open, verified Nov 13).

## Baseline (Nov 14, 2026 — measured; unknown where unmeasurable)

- Downloads: unknown — not on Play. Unblock: Play Console account.
- Rating / reviews: unknown — same unblock.
- Open GitHub issues: 0 (verified Nov 15, 2026 — `gh issue list` empty;
  0 open PRs; last 5 CI runs green, latest 43715239728;
  issue #1 fixed and closed Nov 14, shipped as v0.2.2).
- Sentry: not installed. Unblock: Sentry DSN (needs a Sentry project).
- Store state: not created. Unblock: Play Console account ($25).
- Landing page: live Nov 14 — https://dzianisv.github.io/h264-converter/ (GitHub Pages, docs/ on main; HTTP 200 re-verified Nov 15).
- Useful articles: 10 live (WL-08 + WL-19–WL-25 + WL-27 + WL-29), all 7 quick wins
  covered + top core fight "convert video to mp4" covered + secondary
  "trim video android" / "video trimmer" covered
  (verified Nov 15 — latest article trim-video.html HTTP 200).
- Latest build: v0.2.5 (tag; Release published Nov 15, 2026 10:56 UTC,
  APK + AAB, versionCode 16; smoke test GREEN on run 43706165212 in
  38m10s — WL-28 dead template deps removed + Data Safety draft).
  Previous: v0.2.4 (smoke GREEN on re-run 43608544016 — WL-17 full
  error UI, first attempt failed on a ComposeNotIdle emulator flake,
  re-run passed). Before that: v0.2.2 (smoke GREEN on run 43546002038
  — WL-12 fix for the 0-byte MKV reported Done, issue #1 closed).

## Scoreboard (updated weekly, Mondays)

| Metric | Now | Next milestone |
| --- | --- | --- |
| Downloads | unknown | 100 |
| Play rating | unknown | 4.5+ |
| Open GitHub issues | 0 | 0 |
| Sentry repeats | n/a (not installed) | 0 |
| Store listing conversion | unknown | measure at 100 installs |
| Activation (first real conversion) | unknown | measure at 100 installs |
| Day-7 retention | unknown | measure at 1,000 installs |
| Support queue age | none (no channel yet) | < 1 day |

## First 30 days (from Nov 13)

- **Week 1 — foundation (by Nov 19).** Issues enabled (done). Sentry in the
  app (blocked: DSN). Play project created (blocked: his account). Landing
  page live (done Nov 14, pulled forward). Store listing drafted (done, `tool/play-store/listing.md`).
  Icon and screenshots shipped (done, `tool/play-store/`).
- **Week 2 — publish.** Internal track, real-phone test (his Pixel), closed
  track, production staged rollout. Baseline measured. First useful article
  published. Directory submissions done.
- **Week 3 — first users.** Two or three channels live, three posts per
  channel per week, five user conversations. Support queue open and answered
  daily. First reflection written.
- **Week 4 — first verdict.** Scoreboard reviewed: what moved, what did not.
  Kill the weakest channel, double the strongest. Listing refreshed once
  from what users actually said.
- If week 1 is not done by day 7, the plan is late — name the one blocker,
  not five.

## Gaps (recorded, not invented)

- Lane skills missing from skills-internal: `app-maintainer` (ASO, SEO, AI
  discoverability, free marketing) and the marketingskills modules (aso,
  ai-seo, seo-audit, directory-submissions). Until they exist, the owner
  framework runs those lanes directly from the `app-product-owner` skill.
- `dev-workflow` and `dev-workflow-supervisor` exist in skills-internal
  (repo root) and are installed locally.
- No Play Console access, no Sentry DSN, no landing-page host decision.

## Jobs

- Daily loop (dispatcher) — ~09:48 WITA; runs the control cycle, audits
  the job set, reflects, folds one lesson into a skill.
- Hourly supervisor / nudge — every 1h; build board control.
- Daily growth sweep — ~09:48 WITA; triage issues + reviews, verify site
  + listing, next growth work.
- Weekly reflection — Mondays ~09:48 WITA; writes the reflection and the
  next three actions into this file.
- Daily session reflection — 21:48 WITA; reflects on the Convert264 /
  ffmpeg session only (bounded scope, Nov 14).
- The four-role set (loop + supervisor + sweep + reflection) is complete
  since the Nov 14 framework adoption, which superseded the Nov 13
  decision to skip the hourly supervisor pre-launch.

## Growth folder

- Store listing + creatives: `tool/play-store/` (listing.md, icon, feature
  graphic, 4 screenshots).
- This file, Work Log, Decision Log: `tool/growth/`.
- Searches to win: `tool/growth/searches.md`. Competitors:
  `tool/growth/competitors.md`. Reviews: `tool/growth/reviews.md`.
  Experiments: `tool/growth/experiments.md`. History:
  `tool/growth/history.md`.

## Framework adoption (Nov 14, 2026)

- product-framework adopted: dispatcher + 12 lanes installed from
  dzianisv/product-framework; product file products/convert264.md
  (pushed to the framework repo). Local copies in
  ~/workspace/custom-skills/ and ~/workspace/products/convert264.md.
- Job set now complete (4 roles): daily loop ~09:48 WITA, hourly
  supervisor every 1h, daily growth sweep ~09:48 WITA, weekly
  reflection Mondays ~09:48 WITA.
- Open owner decisions: app name (WL-13), PostHog project (WL-15). Payment model (WL-14) decided Nov 17 by the product lead: free core + one-time Pro $9.99.

## Weekly reflections

### Week 1 reflection — Nov 16, 2026 (first reflection; covers Nov 13–15)

Stage 0 — pre-launch, not on Play. One blocker: Play Console is
unreachable (saved password rejected twice Nov 15); no listing, no
Sentry DSN, no keystore until the founder clears it.

**1. Scoreboard — this week vs last week.** Last week = before the
goal existed (goal created Nov 13), so every number below is new.

| Metric | This week | Last week |
| --- | --- | --- |
| Downloads | unknown (not on Play) | — (no product) |
| Play rating / reviews | unknown (not on Play) | — |
| Open GitHub issues | 0 (verified Nov 16, 01:48 UTC: `gh issue list` empty) | — |
| Open PRs | 0 (verified Nov 16, 01:48 UTC) | — |
| Sentry repeats | n/a (not installed) | — |
| Support queue age | none (no channel yet) | — |
| Releases shipped | 3 (v0.2.2, v0.2.4, v0.2.5) | 0 |
| Useful articles live | 10 | 0 |
| CI (last 5 runs) | green (latest 43788767836, Nov 16 01:52 UTC) | — |

**2. What worked.**

- Article lane: 10 how-to articles live (WL-08, WL-19–WL-25, WL-27,
  WL-29), all 7 quick wins + top core fight "convert video to mp4" +
  trim covered; every FFmpeg command verified locally against real files.
- Release loop: 3 releases in 3 days, each smoke-green (v0.2.2 run
  43546002038, v0.2.4 re-run 43608544016, v0.2.5 run 43706165212 in
  38m10s).
- Issue #1 (0-byte MKV shown Done) fixed and closed Nov 14 (WL-12,
  v0.2.2).
- Landing page live Nov 14, HTTP 200 re-verified Nov 16; Download
  button fixed to v0.2.5 the same day it was caught stale (WL-32).
- Competitor leader corrected: Inverse AI, 18M installs, ~209K
  ratings (WL-07 re-check Nov 15).

**3. What did not.**

- Play Console sign-in failed twice Nov 15 (saved password rejected)
  — 0 of 3 founder-gated week-1 items cleared (Console, keystore,
  Sentry DSN); week 1 ends Nov 19.
- Pixel 9a test still pending: no conversion result and no copied
  error text back from the founder (WL-05 / WL-18).
- Directory submissions: 0 completed — every remaining directory
  needs an owner account (WL-30).
- v0.2.3 release deleted: test-compile error, not an app bug; PR gate
  fixed to compile instrumented tests too (WL-17).
- Article lane exhausted: all quick wins + top core fight covered;
  no further articles planned until Play is live.

**4. Support.** No channel, no reviews, no complaints yet (Stage 0).
Top complaint: none. Closest user feedback is the Pixel 9a
conversion failure (WL-18) — it already changed the product: WL-17
shipped the full-error UI (full log tail, Copy error) so the next
failure arrives readable.

**5. Next three actions.**

1. Founder — Play Console reachable: correct password, account
   recovery, or sign in himself (WL-02). Due Nov 19.
2. Founder — Pixel 9a test of v0.2.5: run the same conversion, paste
   the copied error if it fails (WL-05 / WL-18). Due Nov 19.
3. Founder — name + payment decisions (WL-13 VidSwitch recommended,
   WL-14): the listing cannot be finalised without them. Due Nov 19.
