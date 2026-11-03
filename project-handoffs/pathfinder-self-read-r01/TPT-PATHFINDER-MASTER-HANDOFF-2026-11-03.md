# TPT Pathfinder | Consolidated Self-Read State & Decision Ledger

**As-of:** November 3, 2026, after PQA-W09 handback.  
**Project:** TrashPanda Tracker / Project Burrow, Feral Enterprises.  
**Review authority:** owner decisions for intent, exact source hashes for code claims, source-lined independent tests for offline behavior, real owner RuneLite observation only where specifically shown.  
**Overall disposition:** `PASS_SCOPED_OFFLINE / LIVE_LOG_ACCEPTANCE_HOLD / ENGINE_MERGE_HOLD / RELEASE_HOLD`.

## Executive result (important before reading any wave)

Pathfinder was not found to be a certified account progression planner. The independent waves explored real `SuggestionEngine.suggest` behavior, implemented a succession of isolated candidate repairs and adapters, and generated reproducible QA traces. Those tests do **not** establish that repeatedly obeying the plugin produces a capable Ironman, any 100/500/1,000 real completed-activity journey, or a reliable live RuneLite capture system. No full native JDK17 RuneLite session was executed in these waves. No PQA patch was merged into the user's production source or GitHub by these waves.

At W09, two distinct Pathfinder development lines remain: (A) the full RuneLite-host W0751 source plus the isolated W08→W09 reconstructed candidate, and (B) a Cursor standalone Pathfinder Wave4 engine. In 288 matched account/policy comparisons, 112 top recommendation choices differ, while 48 `readyNow` verdicts differ. Six of nine overlapping Pathfinder Java source files differ; their Snapshot/Suggestion contracts diverge. **No wholesale merge** is safe.

## Waves and reproducible outcomes

| Wave | Focus | Confirmed scoped result | Explicit non-result |
|---|---|---|---|
| W01 | Initial exhaustive simulation | 48 synthetic profiles; 393 actual catalog rules; 13,682 decision trace rows; 12 × 1,000 unchanged-state IGNORE calls and 12 skip-until-empty runs | 0 certified 100-step completed journeys; false Cook's Assistant requirement; resource quantity absent |
| W02 | Policy, goal, supply comparison | 210 strict-follow attempts; **16** conditional first quest completions; 384/384 external supply-state comparisons unchanged; Farming absent from Supplies for 48/48 profiles | No long-term outcome; recorded external stock not in engine input |
| W03 | Isolated P0 repairs | Five false novice quest gates addressed; saved-goal quest chain visible under PvM/Supplies; Farming restored in supplies-focused recommendation pool; conditional first quest completions improved W02 16 → W03 56 | Still 0 verified 100-step completions; not production |
| W04 | Account-truth and supplies | 1,152 resource comparisons; 24 account evidence fences; 9 stack reconciliations; 25 kit checks; stable bank observation candidate | GP/rune budgets and food burn rates largely unmodeled; fairy-ring stage concern |
| W05 | Exact action budgets & fairy-ring stage | 1,512 Plank Make budget tests, 100 alternative essence cases, 48 fairy-ring checks; seven Wiki-supported log types; incomplete jewelry spell recipes quarantined | Food 1 vs 200 unchanged in 96/96 comparisons, no sustained activity planning |
| W06 | Persistent baseline and historical bank snapshot | 48/48 synthetic first-run skill/quest baselines and bank records persisted in isolated candidate; repeated scans did not overwrite first baseline; 975 candidate assertions | Does not establish full first-run coverage, native persistence or bank UI |
| W07 | Guided capture, Bank Memory standalone UI | 314 targeted assertions; Collection Log page observed/persisted; manual review never fabricated KC; test Bank Memory Swing view | Other game readers and sidebar wiring missing at W07 |
| W08 | Sidebar Bank Memory and capture wiring | 1,104 headless assertions across 48 profiles; 24 inherited CSVs byte-identical; subordinate Bank/Guided Capture views, manual quest refresh callback | Native UI acceptance missing; status banner only on publication; navigation placement requires owner acceptance |
| W09 | Kill Log reader, Cursor source comparison | 2,016 widget-contract assertions across 48 synthetic profiles; 816 case rows; Cursor standalone 39 regressions pass; 112/288 top results and 48/288 ready verdicts differ | 0 native log reads, 0 Bank O native coexistence tests, 0 100/500/1000 completed journeys |

**Metrics caution:** these denominators are fixtures, individual assertions or decisions, not separate real players. Do not add overlapping wave case counts as independent confirmations. In particular W02's 16 and W03/W04's 56 are conditional first-step quest completions under a stated player-success assumption; they are not independent native observations and do not make an account progression complete.

## High-confidence defects and remaining open gates

### P0. Per-account baseline & first-run coverage

Owner requires a durable account-specific first-use history of skills/XP, quests, Achievement Diaries, Combat Achievements, boss KC/log, collection unlocks and every other actually obtainable in-game progression surface; a guided click-through workflow for inaccessible menus; truthful coverage; manual refresh and approximately seven-day stale-data reminders. W06 provides a synthetic persistence base. W07 adds an account-specific guided capture journal and Collection Log page data; W08 wires a candidate guided sidebar; W09 adds a partial boss/Slayer log reader. **Complete, native, end-to-end feature is not accepted.** Unknown must never be coerced into zero, and manual player review does not become game evidence.

### P0. Historical Bank Memory and Best in Bank

Owner requires durable per-account Bank Memory from a genuinely completed bank opening/scan with raw item identities, exact observed variants/states and quantities, timestamp and freshness. It must remain viewable when bank is closed; the last scan is historical, **not proof of live stock**, and stale or cross-account evidence cannot drive false-ready choices. W06 stores a historical observation; W07 previews it; W08 connects subordinate sidebar view but needs real RuneLite acceptance. The original owner-tested Bank R05 `O` Bank Tags preparation tab successfully handled four manual withdrawals in a specific earlier session. That narrow success MUST be preserved. Never start a competing bank scanner or automate banking, and do not conflate Bank Memory with the temporary O preparation tab. The newest owner Bank Memory **tab** request must be reconciled with the historical four-root-tabs contract before final visual acceptance. W08 subordinate view was a candidate, not final owner approval.

### P0. Engine divergence / merge safety

W09 compared a full-host W0751+PQA candidate against Cursor's independent 25-Java-file Pathfinder Wave4 subset. Full host had 12 Pathfinder Java files, Cursor 16, nine overlap and six differ. At the same 48×6 common inputs, top advice differed 112/288; readiness 48/288. Competing Snapshot and Suggestion signatures, goal deferral, feasibility/Outfitter contracts and resources mean direct tree replacement is forbidden. Reconcile one behavior/rule at a time with contract tests and owner-visible before/after cases, then run full-host native regression.

### P0. Native RuneLite proof

Waves used source inspection and Java21 `javac --release 17`, frequently with stubs. **This is not** a genuine JDK17 Gradle and RuneLite run. Verify correct RuneLite revision/client on Windows, live event timing, exact boss/Slayer widgets, login fences across world hop/logout/A→B→A, persistent restart, manual quest refresh, genuine bank opening and four safe `O` withdrawals, sidebar 225px layout and state transitions. No native acceptance was established by W09.

### P0. Real outcome quality and sustainability

All analyzed waves stop rather than invent XP, loot, skill levels, bank stock, quest changes or drop RNG. Static 1,000-decision replays repeat exactly when state is unchanged; this is not a true completed journey. GP, food/buff duration, runes, charge states and resource consumption need source-backed action budgets, re-supply dependencies, balanced skill coverage and a completion-feedback loop. Goal convergence to Barrows gloves, SOTE, Bowfa and others is unproved. Protect the selected goal's prerequisite chain even under PvM/Supplies preference. Do not infer real world time savings from synthetic ranking.

### P1. High-value OSRS activity integrity

W01 identified false novice quest requirements and a Priest in Peril/Waterfall Quest ordering issue. W03 corrected candidate novice gates, visible SOTE goals and Supplies Farming. W04/W05 refined Priest in Peril/Cook's Assistant supply contracts, physical counts, Plank Make action budgets and staged fairy-ring permission. These remain **candidate changes**, not proven current production. For fairy rings, Fairytale II quest started/ongoing is not itself permission; confirm the intermediate unlock, and distinguish skill requirements for full completion from earlier access. Rune pouch, spellbook, karambwan vessel/bait and partial alternative items require verified identity/quantity evidence. Incomplete String Jewellery and enchant recipes must not become false-ready.

### P1. Repetition and preference behavior

An explicit Skip removed rejected activity IDs in synthetic replays. Merely ignoring advice without user signal produced same-state top picks at 100/500/1,000 recommendation calls in all 12 W01 replay cohorts. Design honest ignore/defer/cooldown and progress-aware ranking without automatically marking activities done or burning resources. Document eligibility and why/not now.

## Information/authority strata: never collapse them

- `OWNER_WITNESSED_NATIVE_SCOPED`: earlier Bank R05 manually withdrew four items via Best in Bank O. Not blanket approval of later code.
- `SOURCE_INSPECTED`: exact Java/file/ZIP lineage and code paths. A method's existence is not runtime proof.
- `OFFLINE_TESTED`: actual Java engine/harness with synthetic state; 21→17 language targeting and RuneLite stubs are not native acceptance.
- `RESEARCHED_UNVERIFIED`: Wiki-backed mechanic evidence or R31/R32/GEAR research may inform tests; not automatically imported/approved loadouts.
- `UNKNOWN`: missing or stale data. Not equal to zero, not permission to optimize based on guessed possession.
- `CANDIDATE_ONLY`: PQA patch or UI; no production merge or approval.

## What's actually contained, and what is not

Every prior PQA wave archive and owner report is embedded unchanged. The original W0751 is embedded unchanged and verified by SHA. W09 source candidate is reconstructed into a separate source ZIP through verified W08 cumulative then W09 incremental patch application to the exact W0751 bytes (see receipt). Cursor engine is embedded unchanged and verified by SHA; Cursor master R02 is embedded for broader product decisions and historical research pointers. **This pack is not a complete live repository clone, canonical BUR/RSR register, all W08E UI snapshots, or a fresh RuneLite runtime trace**. It intentionally avoids pretending otherwise.

## First tasks when resumed

1. Verify actual current source/repo against these hashes, including whether Cursor or another process has advanced since this handoff. Do not silently use the older GitHub Build #38 as Wave9 source.
2. Reconcile Cursor Wave4's exact SuggestionEngine/Snapshot/Outfitter contract against W09 full host; write a behavior-by-behavior merge plan and run matched 288-case oracle. Do not copy-over.
3. Validate candidate boss/Slayer widgets and capture event fences in real RuneLite; log actual widget IDs/title/row counts; test capped `Lots!` and A→B→A; make no guessed KC assertions.
4. Verify full first-run data sources: quests/XP, diary tiers, CA, boss/Slayer logs, collection pages; persist per-account and truthful completeness across restarts/hops. Add unobtrusive guided steps for source-API gaps.
5. Accept Bank Memory navigation/freshness/rescan with owner, preserve 225px and Bank O; verify four manual withdrawals without temporary tab loss.
6. Expand sustainable resource budgets and evidence-supported completion transitions, then rerun 100/500/1,000 ACTIVITY checkpoints, stopping when unverifiable.
7. Continue independent Tongs L2 and owner native QA. Do not publish TPT until explicit approval and full gates pass.