# 🦝 Project Burrow | PQA-W06 Owner Report
**Wave 6 · Account Evidence, Persistent Memory & Goal Convergence · 2026-11-03**

**MODE:** Independent offline engineering candidate. **RELEASE: HOLD.** Source is the exact Wave 7.5.1 archive SHA-256 `79e7d54a78a0bb76b6b3eca0e6804067b02b40a35e88339ee83a20408aa7ef3f`. The Cursor R02 master pack includes older W075 source, not W0751; GitHub B38 is also older. W06 is a cumulative patch against W0751, not a production modification.

## Results first

**Cohorts and journeys:** 48 deterministic account profiles; 48/48 initial baseline round trips; 48/48 latest full skill/quest snapshot round trips including a synthetic later state; 48/48 historical bank scan round trips, 48/48 newer second scan updates, and 48/48 weekly re-observation cases. **13/13 negative-path cases** passed; **975/975 Java assertions** passed. **6,000 same-input recommendation calls** remain repetitive. **Zero 100-, 500- or 1,000-step completed journeys verified**, because no game outcomes were invented.

### Five important findings

1. **First-run foundation exists in isolated source, but the owner's full first-run feature does not.** New `PathfinderAccountArchive` with a host call records a one-time, account-keyed baseline of observed levels and quest statuses after a complete quest survey. 48/48 synthetic account fixtures round-trip; the initial record is never overwritten. Boss/slayer kill logs, diary tiers, collection log, quest-stage interactions, guided capture and manual refresh remain unimplemented in this archive. **P0.**
2. **Bank Memory has durable historical truth without false live ownership.** 48/48 accounts preserve a last-observed bank-only stack map; 48/48 later scans replace only the remembered bank data. Closed, stale, single-tab or wrong-epoch evidence never authorizes a fresh write. Saved bank records expose `provesCurrentStock() == false` and cannot feed current stock to the suggestion engine. The **Bank Memory view, variant labels, UI refresh, seven-day reminder and native storage validation remain missing**. **P0.**
3. **The engine responds to observed goal progress, but the host cannot yet witness it promptly.** In 48 source-connected counterfactuals, changing Cook's Assistant to synthetic FINISHED removed its recommendation in 32/48 relevant cases and changed the first recommendation in 28/48. These are simulated state inputs, **not completed quests**. The host's quest survey scans once per login and does not continually refresh progress mid-login. **P0.**
4. **Historical progress can be compared conservatively across full observations.** The isolated archive stores a latest observed full skill/quest frame, compares it with first login, detects skill-level increases, newly finished quests and regressions, and re-witnesses unchanged data at most weekly. 48/48 synthetic account comparisons passed. It does not infer XP, gear, loot, quest rewards, who caused progress, or which Pathfinder suggestion was followed. **P1.**
5. **Native and long-run acceptance remain open.** W04's 6 and W05's 6 CSV regression files reproduce byte-for-byte; a fresh W0751 extract accepts the W06 cumulative patch and reproduces all five W06 evidence CSVs exactly. This is **JDK21 `javac --release 17` only**. No native Windows JDK17 Gradle/RuneLite test, no direct live quest-stage witness, no bank O-tab acceptance, no durable full first-run baseline, and no 100/500/1000 completed-activity journeys. **P0.**

## What actually changed (isolated candidate only)

- Added `core/PathfinderAccountArchive.java`: versioned SHA-256-checked serialized initial, latest, and bank-history records with account-specific storage keys. Checksums detect accidental corruption; they are **not encryption or a substitute for local storage privacy**.
- Extended `runelite/BurrowPlugin.java` to call this archive only after the already-existing authenticated session checks. Full quest survey + full skill vector are required for first/latest. Fresh, accepted whole-bank observations are required for remembered bank stock; the Best in Bank O-tab withdrawal lease disallows false recaptures.
- Saved bank memory is never transformed into `SuggestionEngine.ResourceEvidence`; missing live evidence stays UNKNOWN.
- Did **not** implement a Bank Memory tab, a guided first-run capture workflow, a manual refresh button, a visible seven-day reminder, a spellbook reader, a quest-step permission reader, GP/food/potion consumption, or activity-completion listeners. These remain release blockers.

## Fix priority

**P0 A:** Finish first-run capture of all obtainable account data (boss and Slayer logs, collection log, diary states and partial/unknown flags), including a guided click-through route when RuneLite cannot read a menu.

**P0 B:** Add the Bank Memory UI/read endpoint, item-state identity, timestamp/freshness, manual Refresh and seven-day reminder. Guard account-switch, stale/partial evidence and the Bank O withdrawal lifecycle. Validate ConfigManager payload size/privacy and fallback failure behavior on native Windows JDK17.

**P0 C:** Add a safe quest-survey refresh and independent fairy-ring permission/spellbook evidence. Never infer permission from `IN_PROGRESS` or a Plank Make cast from bank runes alone.

**P0 D:** Implement source-backed per-action supply budgets, completion/attempt feedback and goal-sequence tracing. Only then attempt the 100/500/1000 **completed** recommendation journeys.

## Evidence boundaries

`ISSUE-LEDGER.csv` contains 13 source-located records, exact affected synthetic cohorts, severity and repro steps. `FEATURE-COVERAGE.csv` separates new foundation from unimplemented release requirements. The cumulative and incremental patches, Java harness, source snapshot, immutable traces and SHA manifest are in the QA bundle. All code is outside the user's active plugin and public repo.

**Disposition: RELEASE HOLD.** This is a candidate implementation and a reproducible offline QA advance, not a RuneLite acceptance or fully shipped Bank Memory/first-run feature.