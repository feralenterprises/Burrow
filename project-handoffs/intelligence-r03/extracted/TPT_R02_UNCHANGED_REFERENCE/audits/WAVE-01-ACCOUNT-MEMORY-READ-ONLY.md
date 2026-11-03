# CURSOR DISPATCH | TPT INTELLIGENCE AUDIT | WAVE 01: ACCOUNT MEMORY & SYNC

**MODE:** EXHAUSTIVE READ-ONLY AUDIT. **ONE ENGINE ONLY:** authentic account progression intake + durable per-account memory + synchronization. **DO NOT CODE, COMMIT, MERGE, SUBMIT OR OPEN NEW WAVES WITHOUT OWNER APPROVAL.** Time budget is not capped by prompt brevity; be systematic and reproducible. Study `TPT_CURSOR_MASTER_CONTEXT.md` §0A and `audits/REQUIREMENTS-GAP-REGISTER.md` first. The current checkout is implementation authority; archival packs are references only.

## Mission

Determine whether TPT **actually** gathers, saves, updates and preserves account progression knowledge correctly for RuneLite users, including first launch and assisted rescans. Do not simply check that classes have promising names. Prove the state transition from **native source observation → reconciler → persistent storage → restart/account switch → consuming engines**.

## Mandatory traces

1. Capture repo HEAD, dirty worktree, source/release lineage, JVM/runtime, data directories, schema and migrations. Avoid touching player data. Create a replayable test strategy in a disposable temp directory.
2. Inventory actual data sources and identify whether they are automatically observable, event-only, user-interface-open dependent, manually entered, external and authorized, or genuinely inaccessible. Analyze skills/XP, quests, Achievement Diaries, Combat Achievements, boss log/lifetime KC, Collection Log where relevant, progression unlocks, combat stats and player account mode.
3. For each, trace real classes/methods/events, event timing, initial/baseline reconciliation, observation coverage, confidence, negative evidence, unknown/zero, timestamps, storage serialization, consumers (Pathfinder/Spoons/Outfitter/Chronicle/Home), and stale data policy. Do not guess RuneLite APIs.
4. Verify guided first-run capture path, opening required in-game screens manually, completion/partial-capture indicator, interrupted resumes, denied/unavailable source, idempotent re-open/re-read, multi-login sequence.
5. Verify same-account persisted data across confirmed logout, world hop, LOADING, crash/forced exit, clean restart, plugin update/schema migration and OSRS display-name changes where supported; never leak another account or store credentials. Test pending async callbacks/event duplication and stale source generation.
6. Verify automatic observed updates, manual Sync, per-domain `lastChangeAt` vs `lastSuccessfullyVerifiedAt`, 7-day inactivity/staleness prompting, intentional unchanged verification, refusal/snooze, clock changes, and repeated failures. Player absence is **not** proof progress was zero.
7. Inspect privacy and local storage behavior. Verify read-only capture; do not introduce game-click automation, hidden menu scraping claims, password/token storage, or destructive data migration.
8. Check the observed baseline actually reaches recommendation engine decisions without upgrading `UNKNOWN` to `READY`, falsely completing CAs, inventing KC deltas, or altering current-login Sessions semantics.

## Required adversarial fixture matrix

Run or explicitly label unexecuted each relevant scenario: fresh account no screens opened; novice quest-only; midgame with partially observed boss log; near-max and maxed; Ironman/Main; quests marked user-selected not completed; CAs page unopened; diaries unreachable; KC 0 vs no reading; first absolute KC >0; +1 verified game KC event; loot without KC; new quest and XP during LOADING; same-account world hop; logout/relogin; A→B→A with overlapping names; missing account identity; stale update callback; event duplicate; partial manual scan; bank closed (must not block account memory); source unavailable; reset/reinstall preserving saved data; migration/corrupt file; interrupted write/crash; zero disk space; permissions denied; no gameplay progress but fresh verification; no verification in seven days; prompt dismissed; repeated prompt; manual rescan with unchanged data; OS clock skew.

## Evidence standard

Classify each requirement `SOURCE-PROVEN / PARTIAL-PROVEN / MISSING / UNVERIFIED / BLOCKED-BY-API`. For every claim cite `path:line`, exact test/fixture/seed, observed actual vs expected, and whether the test used native RuneLite, real Gradle/JDK17, isolated Java shim or only static analysis. If actual native game screens are inaccessible, clearly isolate human Windows acceptance tests. Do not write green blanket status from unit tests alone.

## Required handback (one focused report)

- Source map and per-domain capture/status matrix.
- Dataflow diagram with actual names, separate live-memory/archival data stores and account boundary.
- Reproducible test commands, individual PASS/FAIL/UNRUN with first failure evidence; synthetic fixtures preserved separately.
- Ranked `P0/P1/P2` gaps and specific suggested repairs, affected files and regression risks. **No repairs in this wave**.
- Exact owner-RuneLite manual steps to validate only the native-uncertain parts.
- Final verdict **VERIFIED / NEEDS REPAIR / NOT YET VERIFIED**, scoped to what was actually tested.
- Explicit recommendation whether the separate Wave 02 Bank Memory audit is safe to start; **do not start it automatically**.

## Stop rule

If account identity or persistence integrity is unsafe, do not mark the engine good even if visible screens work. If the source cannot expose diary/CA/KC directly, surface a guided manual observation path (subject to actual RuneLite availability), mark gaps and keep verified history. **Do not invent data or approve a release.**