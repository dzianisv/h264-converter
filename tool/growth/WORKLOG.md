# Convert264 — Work Log (the only task tracker)

Status: `[ ]` open, `[x]` done (with date + reason). Never delete a row.
Close with a dated reason.

## Open

- [ ] WL-01 Sentry in the app. Needs a Sentry DSN — owner creates a Sentry
  project and hands over the DSN. Owner: founder. Due: Nov 19 (week 1).
- [ ] WL-02 Play Console developer account ($25, one-time). Owner: founder.
  Due: Nov 19 (week 1).
- [ ] WL-03 Stable upload keystore for the first AAB upload. CI signs with a
  throwaway key that changes every run — the CI AAB is a build artifact, not
  the upload. Options: keystore in GitHub Secrets, or a local build.
  Owner: founder. Due: Nov 19 (week 1).
- [ ] WL-04 Landing page live. Proposal: GitHub Pages from this repo (free).
  Owner: Muse. Due: Nov 19 (week 1).
- [ ] WL-05 Real-Pixel test of v0.2.1 (queue, MKV output, dark mode, share
  sheet, OTG with a real drive). Only he has the phone. Owner: founder.
  Due: Nov 19.
- [ ] WL-07 Competitor study: top 5 video converters on Play — rating, 1-star
  complaints, screenshots, update cadence. Owner: Muse. Due: Nov 20.
- [ ] WL-08 First useful article: a how-to that works without the app
  (mention Convert264 once, at the end). Owner: Muse. Due: Nov 26 (week 2).
- [ ] WL-09 Video join — build it or drop it. Open question to the founder
  since Nov 11 ("join them" was read as joining the dropdowns). Owner:
  founder. No date — his answer.
- [ ] WL-10 Firebase Analytics / Crashlytics. Needs his Firebase project.
  Optional — Vitals + in-app review cover crashes and feedback until then.
  Owner: founder. No date.
- [ ] WL-11 Score the searches to win (searches.md) on relevance,
  difficulty, search volume. Owner: Muse. Due: Nov 20.

- [ ] WL-12 Fix smoke failure: 0-byte MKV reported Done (issue #1).
  Late completion callback from a cancelled FFmpeg session deletes the new
  run's staging files; success never checks the published file is
  non-empty. Root cause verified from run 43375580799 logcat + code.
  Owner: Muse (lead subagent, session fea12aae-4cfc-4955-a11e-fbe592d377b5).
  Stage 0 CLAIMED Nov 13, 2026 (issue comment 6869808148, read back; no
  competing claims/PRs/branches). Due: Nov 14. Ships as v0.2.2.

## Done

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
