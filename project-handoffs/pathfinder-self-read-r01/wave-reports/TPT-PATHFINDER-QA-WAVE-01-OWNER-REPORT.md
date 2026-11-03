# 🦝 FERAl ENTERPRISES | TRASHPANDA TRACKER
## PATHFINDER SIMULATION QA | PQA-W01 | 2026-11-03 (PST)

**Owner disposition: RELEASE HOLD | INTELLIGENCE QA NOT VERIFIED | READ-ONLY**

### 0. Results first

- **Accounts:** 48 deterministic synthetic profiles, including Ironmen, mains, fresh 3s, early/mid/late/maxed; four goals/preferences and eight additional stress conditions.
- **Rules exercised:** 393 exact rules from the inspected Pathfinder engine (215 QUEST; 91 AFK; 35 MONEY; 22 GEAR; 17 BOSS; 11 SKILL; 2 LEGACY).
- **Journeys:** 48 strict follow attempts (46 stopped at step 1 because outcome could not be safely modeled; two maxed quest-focus profiles had no suggestions), 12 IGNORE runs at 1,000 recommendation calls each (12,000 actual engine decisions), 12 SKIP runs until empty (1,636 actual engine decisions; 0–380 per run); 13,682 per-decision trace rows. Two conditional Wiki-supported early quest reward transitions separately tested, with hand-supplied legal inventory and explicit player-success assumptions.
- **100/500/1,000:** 12 unchanged-state IGNORE journeys checked those checkpoints. They are **not** 12 completed account-progressions. SKIP journeys terminate before 500; strict follow stops immediately rather than invent XP/drops/GP. **Complete validated 100/500/1,000 progression journeys: 0.**
- **Five leading findings:** (1) 19/48 top suggestions have outstanding explicit CHECK while 18/48 also say readyNow; (2) all 48 lack observable GP/food/rune quantities and resource burn-rate schema; (3) 11 Bowfa-goal accounts lack SOTE, and 10 of these lose quest suggestions under PVM filter; (4) 8/8 fresh accounts falsely gate Cook's Assistant behind Cooking 4; (5) 7/48 are sent first to Priest in Peril despite no modeled quest item/combat safety.
- **P0 fixes first:** source-data prerequisite integrity, real feasibility/budgets, goal prerequisite preservation and honest ready status. Next: repeated-decision policy and sustainable supply/quest acceleration.
- **Next wave:** PQA-W02, after adding provenance-backed state-transition adapters and goal dependency tests, run 100/500/1,000 *meaningful completions* with resource ledgers and independent oracle. Do not extrapolate this wave's static replays into verified account outcomes.

### 1. Exact source provenance / precedence

- Primary source: Library artifact `TPT-W0751-RUNELITE-135-ONE-CLICK-DUAL-DEV-FIX.zip`, **1,640,216 bytes**, **SHA-256 `79e7d54a78a0bb76b6b3eca0e6804067b02b40a35e88339ee83a20408aa7ef3f`**.
- Extracted under isolated scratch path `source/Project-Burrow` solely for analysis. **The exact included Java `gg.feral.burrow.pathfinder` implementation was compiled and called directly** via `SuggestionEngine.suggest`, not recreated in Python or a substitute scoring routine. No gameplay source was modified.
- Source archive README says W0751 only repairs test contracts/scripts and changes **no gameplay source** from W075. Source-level comparison to W075 confirmed 18 Pathfinder / related files byte-for-byte identical. W0751 does not claim genuine Windows Java17 Gradle full suite or RuneLite acceptance; prior local Java21 `javac --release 17` passes are not native verification.
- GitHub `feralenterprises/Burrow` public `main` HEAD was `be91c6754bada3483bed82b5f3699286c2b8dbba` and archived older Build #38 Bank R05. Its code is not this W0751 Pathfinder source. **Do not use GitHub main to reproduce this wave unless the exact W0751 archive has been added and verified.** The W0751 ZIP has no established Git commit identity; its SHA-256 is the reproducible revision identifier.
- Historical CP9/R31 handoffs are not production source authority. The W06/W0751 intelligence activity pack has 44 `CHECK` entries and zero `APPROVED`; the bridge is not a verified new requirement-enforcement layer. This wave exercises direct `SuggestionEngine`, not live `BurrowPanel`/RuneLite bindings.

### 2. Finding register (actual file: `issue_ledger.csv`, machine-readable mirror `issue_ledger.json`)

| ID | Severity | Count in 48 profiles or rule catalog | Issue / evidence | Source location |
|---|---|---|---|---|
| PQA-W01-001 | P0 | 48/48 snapshot schemas (structural exposure); 4/4 controlled resource-state pairs had unchanged first recommendation | Missing resource budgets, supply quantities, XP and consumption model | `src/main/java/gg/feral/burrow/pathfinder/SuggestionEngine.java:31-35; PathfinderTiming.java:57-67` |
| PQA-W01-002 | P0 | 19/48 top-ranked with explicit CHECK; 18/48 simultaneously readyNow=true; 9/48 had lower-ranked candidates with no explicit CHECK | Unverified requirements do not downgrade readyNow or reliably prevent top ranking | `src/main/java/gg/feral/burrow/pathfinder/SuggestionEngine.java:86-87,217-260,290-325` |
| PQA-W01-003 | P0 | 8/8 fresh level-3 cohorts; 8/8 Cook Assistant quest probes show UNMET | Cook's Assistant has false Cooking 4 requirement | `src/main/java/gg/feral/burrow/pathfinder/GuideQuestOrder.java:18; GuideData.java:126-145; SuggestionEngine.java:217-234` |
| PQA-W01-004 | P0 | 11/48 saved Bowfa-goal accounts have unfinished SOTE; 10/11 get zero QUEST suggestions under PVM focus | Combat focus drops necessary quest unlock chain for selected Bowfa goal | `src/main/java/gg/feral/burrow/pathfinder/SuggestionEngine.java:94-105,164-180,290-299` |
| PQA-W01-005 | P1 | 7/48 initial tops Priest in Peril; 8/8 fresh QUESTS focus probes rank it #1 while Waterfall #3-5 | Priest in Peril appears ready without supply or combat feasibility; Waterfall XP acceleration underweighted | `src/main/java/gg/feral/burrow/pathfinder/GuideData.java:126-145; SuggestionEngine.java:217-260,290-299` |
| PQA-W01-006 | P1 | 1/48 first-choice suggestions not readyNow; reproducible main_late_combat_short_window | A time-infeasible recommendation remains the top item | `src/main/java/gg/feral/burrow/pathfinder/SuggestionEngine.java:108-129,131-155; PathfinderTiming.java:18-55` |
| PQA-W01-007 | P1 | 12/12 IGNORE journeys returned identical first activity at steps 1,100,500,1000; 12,000/12,000 repeated decisions | Ignored/rejected suggestions repeat indefinitely without explicit skip or state mutation | `src/main/java/gg/feral/burrow/pathfinder/SuggestionEngine.java:90-105,201-205; PathfinderTiming.java:58-67` |
| PQA-W01-008 | P1 | 145/393 catalog rules lack automatic completion signal (91 AFK, 35 MONEY, 17 BOSS, 2 LEGACY); 12 skip-to-exhaustion journeys did not earn modeled progression | Activity completion and ongoing supply production cannot be measured across cycles | `src/main/java/gg/feral/burrow/pathfinder/GuideData.java:156-185,191-365; SuggestionEngine.java:201-215` |
| PQA-W01-009 | P1 | 8/8 external goal-change probes had unchanged #1; not proof of no ranking change elsewhere | Goal-change adaptation is sometimes invisible at the top recommendation | `src/main/java/gg/feral/burrow/pathfinder/SuggestionEngine.java:290-299; GuideData.java:126-145` |
| PQA-W01-010 | P1 | 8/8 fresh quest-focus probes; Waterfall rank 3–5 and Cook Assistant 79–88 | Quest XP rewards and their progression acceleration are not represented in scoring | `src/main/java/gg/feral/burrow/pathfinder/GuideData.java:126-145; SuggestionEngine.java:290-299` |

For every ID, the `issue_ledger.csv` and `.json` contain the precise scenario, source location, evidence classification, concrete consequences and suggested repair. Counts are synthetic deliberately sampled account counts, NOT estimates for OSRS players.

### 3. Priority evidence & representative decision traces

1. **Fresh level-3 Ironman, balanced, no food:** `quest_priest_in_peril` top, `readiness=MET`, `readyNow=true`, `unverified_count=0`. The Wiki requires **50 unnoted rune or pure essence**, a bucket, and defeating level-30 enemies; food recommended for low levels. TPT does not prove the account has these, nor distinguish quantity 0 from available. Fresh quests-focus profiles (8/8) rank Priest in Peril first, Waterfall third to fifth, and Cook's Assistant 79th to 88th. See `fresh_quest_ranking.csv`. This is a catalog/gating/priority finding, not proof the quest is impossible for every level 3.
2. **Fresh level-3 Ironman, combat, Bowfa saved:** `afk_sand_crabs` top, `MET` and `readyNow=true`, with one unverified check. Focus=PVM filters **all** QUEST entries, including Song of the Elves blockers. Among 11 Bowfa goal accounts with SOTE unfinished, 10 had zero QUEST suggestions. See `priority_alternatives.csv`.
3. **Iron midgame combat, Barrows:** `pvm_barrows` remains top in the paired no-food condition. Item quantities, prayer consumables, runes and travel time aren't encoded in the snapshot, so source-level readiness alone is insufficient for a bank-safe plan. No combat drops or kill counts were invented.
4. **Short-window main, late combat:** `pvm_bryophyta` first at 15 minutes although start is `UNKNOWN_FOR_WINDOW` and readyNow false. Do not assume there was a proven executable lower-ranked alternative in this cohort.
5. **Ignored top suggestion:** 12 independent profiles, each requerying the real engine with unchanged snapshots 1,000 times. Same first ID at decisions 1/100/500/1000 in all 12; 12,000/12,000 repeating choices. This is an invariant absent a rejection/defer signal, not a random test failure.
6. **Explicitly skipped suggestion:** 12 account profiles persisted `GoalMark.SKIPPED` and re-ran the engine. All 1,636 emitted suggestions were distinct within their journeys, exhausted in <=380 decisions; **the existing explicit SKIP behavior correctly prevents loops**. However, 1,636 skipped suggestions did not earn progress.
7. **Verified early quest transition probes (manual legal outcomes):** With required food/rope/elemental runes and successful survival explicitly assumed, Waterfall gives +13,750 Attack/+13,750 Strength, advancing levels 1→30; the quest then disappears from actual engine results. Cook's Assistant with egg/milk/flour grants +300 Cooking, 1→4; despite engine's prior false Cooking4 gate, externally completing it and marking FINISHED correctly removes it. Both conditional tests next recommend Priest in Peril, so reward acceleration alone does not automatically fix the priority choice. See `verified_reward_transition_probes.csv`.
8. **Goal changed externally:** eight composite state-change probes set SOTE goal and completed Waterfall; all eight retained same top result. Because two independent changes were combined, these are *low-confidence adaptation probes*, not isolated causal proof; rerun separately in W02. See `goal_change_probes.csv`.

### 4. Coverage and sustainability limits

- 145/393 rules have no automatic completion signal: 91 AFK, 35 MONEY, 17 BOSS, 2 LEGACY. Repeated activities can be appropriate, but this catalog cannot project returns, stock burn, skill XP or effective time per completion.
- The AFK catalog **does contain** karambwan at Fishing 65 with Tai Bwo Wannai Trio; herb runs, Herblore 81 and Fishing supply rules also exist. Therefore it would be incorrect to say sustainable food is omitted entirely. The structural defect is no dependency planner from depleted supplies to food production, quest unlock, Fishing/Cooking progression, bait, fishing vessel and banked supply inventory. A suggestion to shop for food is not automatically bad; unbounded shop dependence without income/stock rules is the risk.
- Recipe for Disaster final quest correctly annotates `175 QP` as an **unverified note**, not verified gate. Song of the Elves has level-70 requirements in eight skills in the catalog, but outcomes/XP, scarce resources, diaries and transport are not verified. **Barrows gloves, Song of the Elves and Bowfa convergence could not be established** in these static and early-ending runs.
- Goal focus can intentionally narrow suggestions; the defect is that chosen goal's critical prerequisites do not consistently survive narrowing. A 1000-step no-change replay is a stability/loop test, **not** evidence of account advancement.
- Real-world account progress is not guaranteed by synthetic level/quest prefix combinations. Some later-stage generated profiles are capability probes that might not be fully consistent with actual progression. Treat findings as code behavior under explicit synthetic inputs, not as a population-wide rate.
- No live game client, RuneLite navigation, multiplayer interactions, boss KC, market/GP/hr, random drop distribution, bank account sync, supply consumption, completed 100/500/1000 action sequences or genuine JDK17 Windows Gradle run was demonstrated.

### 5. Two separate evidence strata

**SOURCE PROOF:** directly inspected W0751 Java source logic, rule inventory, compiled Java invocation; see manifest and code paths in issue ledger.

**SIMULATED PROOF:** deterministic 48-profile inputs and static/skip/ignore engine decisions. The only permitted state transitions are (a) explicit SKIPPED marks, (b) two conditional manual Wiki-supported quest outcomes, with their inventory costs and XP enumerated. External assumptions and unsupported outcomes are plainly labelled.

**OSRS WIKI:** verified research facts as of Nov 3, 2026 at the source URLs below. They inform independent oracle expectations, not a hidden replacement recommender.

**UNKNOWN:** real journey time, GP/unit prices, drop luck, actual consumption, quest item acquisition from unknown stock, how many real players encounter these choices, complete account goal convergence, live RuneLite correctness.

### 6. Proposed fix sequence & acceptance

- **P0 A, data truth:** correct false quest gates (Cook), distinguish hard prerequisite vs guide milestone vs recommendation; add quest points, skills+XP, exact quest state, inventories/amounts, GP, bank provenance, resource costs, combat safety, equipment access. When unknown, show `VERIFY` not `READY`.
- **P0 B, goal graph:** saved goal-aware mandatory substeps survive PvM/AFK/Supplies filters; dynamically choose unlocks toward RFD/Barrows gloves, SOTE/Bowfa. Prove no inaccessible prerequisite is marked MET.
- **P0 C, budget:** resource balances and sustainable food/runes/potions/GP planning with contingencies (bad drops, depleted shops, missing food, skipping task). Include karambwan, Farming, Herblore, Fishing/Cooking capability chains.
- **P1 D, repeated decisions:** defer/reject cooldown and explicit skipped persistence, finite vs repeatable activity state, travel/setup time and attention. Test 100/500/1000 *actual supported completions*, not only 1000 calls.
- **P1 E, optimization:** reward-aware Waterfall/Cook's and other starter quests; anti-skill-neglect constraints, preference/policy cohorts and deterministic/random-luck sensitivity. Do not make unsupported XP/hour assumptions.

W02 exit criteria: exact owner-selected source ZIP hash; 100/500/1000 fully accounted actions for multiple legal Ironman/main profiles, XP/GP/stock reconciliation and explicit stop on unknowns; no falsely ready high-priority choice; no starvation of quest/skill supply goals; no repeated rejected suggestion before allowed retry; independent test oracle with OSRS source citations. This is an acceptance target, NOT a W01 result.

### 7. Source URLs for independent OSRS facts

- Cook's Assistant (300 Cooking XP, no Cooking prereq): https://oldschool.runescape.wiki/w/Quests/Experience_rewards
- Waterfall Quest (13,750 Attack+Strength XP, elemental runes, danger): https://oldschool.runescape.wiki/w/Waterfall_Quest
- Priest in Peril (50 unnoted essence, bucket, level-30 enemies): https://oldschool.runescape.wiki/w/Priest_in_Peril
- Recipe for Disaster finale (175 quest points): https://oldschool.runescape.wiki/w/Recipe_for_Disaster
- Karambwan food chain (65 Fishing, Tai Bwo Wannai Trio and supply logistics): https://oldschool.runescape.wiki/w/Ironman_Guide/Fishing

### 8. Reproduce precisely (JDK17 or newer JDK targeting release17)

This bundle does NOT include proprietary/project source. Supply verified W0751 ZIP with exact SHA above, extract into independent `source/Project-Burrow`, then run the following from this package's folder (Unix shell shown):

```bash
mkdir -p classes results
javac --release 17 -d classes source/Project-Burrow/src/main/java/gg/feral/burrow/pathfinder/*.java
javac --release 17 -cp classes -d classes PathfinderSimulationQA.java PathfinderTransitionProbe.java PathfinderPriorityProbe.java PathfinderQuestRankProbe.java
java -Xmx512m -cp classes PathfinderSimulationQA results
java -cp classes PathfinderTransitionProbe results/verified_reward_transition_probes.csv
java -cp classes PathfinderPriorityProbe results/priority_alternatives.csv
java -cp classes PathfinderQuestRankProbe results/fresh_quest_ranking.csv
```

Canonical CSV decisions/reports in this bundle are saved from the inspected source and preserve all 13,682 decision records. All rows are re-creatable with deterministic seed `20261103`. On any changed source ZIP, do not overwrite this handoff; create a new wave and compare the two artifact manifests.