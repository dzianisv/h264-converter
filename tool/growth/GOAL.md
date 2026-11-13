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

## Baseline (Nov 13, 2026 — measured; unknown where unmeasurable)

- Downloads: unknown — not on Play. Unblock: Play Console account.
- Rating / reviews: unknown — same unblock.
- Open GitHub issues: 0 (verified via API Nov 13).
- Sentry: not installed. Unblock: Sentry DSN (needs a Sentry project).
- Store state: not created. Unblock: Play Console account ($25).
- Landing page: none. Unblock: hosting decision (proposal: GitHub Pages
  from this repo, free).
- Latest build: v0.2.1 (tag; Release + PR green; smoke test failed on an
  emulator boot flake — adb daemon unreachable, snapshot failed to load —
  re-run started Nov 13; AAB 35.9 MB + APK 54.8 MB on the release).

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
  page live (open). Store listing drafted (done, `tool/play-store/listing.md`).
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

- Daily growth sweep — 09:00 WITA, project chat, material changes only.
- Weekly reflection — Mondays 09:00 WITA; writes the reflection and the next
  three actions into this file.
- Hourly supervisor / nudge — NOT created. Pre-launch, no live users or
  reviews; hourly checks would be noise. Revisit at Stage 1. (Recorded
  reason, per the skill's "adjust only with a recorded reason" rule.)

## Growth folder

- Store listing + creatives: `tool/play-store/` (listing.md, icon, feature
  graphic, 4 screenshots).
- This file, Work Log, Decision Log: `tool/growth/`.
- Searches to win: `tool/growth/searches.md`. Competitors:
  `tool/growth/competitors.md`. Reviews: `tool/growth/reviews.md`.
  Experiments: `tool/growth/experiments.md`. History:
  `tool/growth/history.md`.
