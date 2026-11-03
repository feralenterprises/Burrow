# FERAL ENTERPRISES | TPT Pathfinder Simulation QA | PQA-W02
**Date:** 2026-11-03 (America/Los_Angeles)  
**Mode:** read-only independent offline simulation; production / GitHub untouched; RELEASE HOLD

## 0. Results-first owner handback

- **Profiles:** 48 inherited capability probes + 42 fresh account/goal/stock configurations = **90 distinct core simulated configurations** (the 48 include synthetic later-game capability probes, not all guaranteed to be a legal OSRS history). The 42 fresh configurations comprise both Ironmen and regular accounts, seven selected beginner-quest goals, and full/missing-one/empty stock.
- **Policy tests:** 240 source-engine ranking evaluations (48 x 5 policies), 384 stock-state sensitivity rows, 24 karambwan item-access probes, 30 fresh target-goal-chain probes, and 2 verified-reward goal-change probes.
- **Strict recommendation journeys:** **210** (42 configurations x 5 policy settings), **226 actual engine decisions**, **16 conditional Wiki-backed quest completions** in 16 journeys, **0 full end-to-end verified journeys**, and **0 journeys reaching 100/500/1,000 genuinely completed tasks**. Every strict journey terminated when the top decision was unsupported or infeasible. Do not call early stops successful account progression.
- **Longitudinal replays:** 12 independent account-policy unchanged-state journeys x 1,000 actual engine calls = **12,000 decisions**, with checkpoint rows for 1/100/500/1,000. All 12 repeated the same top recommendation because none had a verified state change. These are *decision replays*, not skill-training completions.
- **No production change:** Java source exact W0751 ZIP; harness and logs are external; offline compile used JDK21 `javac --release 17`, NOT genuine JDK17 RuneLite/Gradle on Windows.

### Top five findings

1. **P0 false beginner gates:** Five source-verified novice quest requirement errors, including Cook’s Assistant, Sheep Shearer, The Restless Ghost, X Marks the Spot and Witch’s Potion. **12/210 strict journeys** ran into a false UNMET with a complete first-step kit and an independently valid quest opportunity. This extends W01’s Cook finding.
2. **P0 supply blindness:** **384/384** external GP/food/rune state variations left the top recommendation unchanged. TPT’s current Snapshot contains no exact GP/item quantities or burn rate.
3. **P0 karambwan readiness:** **8/12** completed-Tai-Bwo-Wannai-Trio/65-Fishing probes asserted readyNow with inadequate bait or missing vessel; only 4/12 had independently sufficient bait + vessel. A successful quest unlock cannot be treated as an available food source.
4. **P0 Farming excluded from Supplies:** **48/48** SUPPLIES-focused account profiles had *zero Farming-labelled suggestions anywhere in the filtered list*. The current herb-run and Farming-contract rules are MONEY/GP entries. Karambwans exist but quantities and sustainable food rate are not modeled.
5. **P0 goal-route filtering:** **39/39** account profiles where SOTE was visible under goal-aware ANY lost it when PvM or Supplies focus was chosen. In **12/12** focused fresh goal-chain tests (SOTE, Bowfa, Barrows gloves across both modes, PvM + Supplies), SOTE was hidden. Goal-dependency routing cannot be trusted to converge under these filters.

**Immediate fix priority:** hard quest requirements and readiness truth → budget/quantity-aware state → protect goal prerequisites across filters → reclassify Farming/supply producers → provenance-backed completion transitions. Next W03 should re-run all W02 frozen cohorts after these engineering changes and extend reliable completed-activity sequences rather than generating synthetic 1,000-task successes.

## 1. Source identity / limitations

- **Inspected source:** `TPT-W0751-RUNELITE-135-ONE-CLICK-DUAL-DEV-FIX.zip`.
- **Source ZIP byte SHA256:** `79e7d54a78a0bb76b6b3eca0e6804067b02b40a35e88339ee83a20408aa7ef3f`.
- **W01 exact ZIP SHA256:** `6717e6f96062ec28b3164f5f43c448c36ee15c409a34a843f45d7bb7e7fcadbb` (read-only historical comparison; W01 remains unmodified).
- **GitHub:** No assumption that public `feralenterprises/Burrow` main (older Build #38 at W01) is this W0751 source. No GitHub writes. W0751 has no proven Git commit identifier; source ZIP SHA is the audit revision.
- **Engine contract:** all recommendations are returned by unchanged Java `SuggestionEngine.suggest(Snapshot, Preferences)`; external oracle checks stock/real Wiki quest outcomes but does **not score or create recommendations**. `PathfinderActivityTags.classify` is TPT’s actual UI tagger for coverage counts.
- **UI bridge scope:** actual BurrowPanel calls `SuggestionEngine.suggest` then `PathfinderIntelligenceBridge.enrich`; its bundle reports zero approved activity rules, making enrichment identity in this revision. This is source inspection, not an executed graphical UI test.
- **Data**: 215 quest-order rules, 91 AFK activities, 35 money activities, 393 total source rules; the rule set includes unverified and non-repeatable outcomes. No assumed market prices, combat XP/hour, fishing catch RNG, burn RNG, quest survival, supplies gathered, or unobserved item drops.

## 2. Comparative policies and exact outcomes

All five options are **native Pathfinder settings**, not a substitute optimization engine: `GOAL_ANY` (saved goal + ANY), `BALANCED_ANY` (no saved goal + ANY), `GOAL_QUESTS`, `GOAL_PVM`, `GOAL_SUPPLIES` (saved goal + native focus). The last is only a *supplies-focus proxy*, not a functioning resource-conservative policy.

| Policy | 48 profile top-ranked explicit CHECK | 48 profile top not readyNow | Have quests somewhere in matches | First recommendation dominant |
|---|---:|---:|---:|---|
| `GOAL_ANY` | 11/48 | 0/48 | 39/48 | `quest_priest_in_peril` (16/48) |
| `BALANCED_ANY` | 15/48 | 0/48 | 39/48 | `quest_priest_in_peril` (19/48) |
| `GOAL_QUESTS` | 2/48 | 9/48 | 39/48 | `quest_priest_in_peril` (16/48) |
| `GOAL_PVM` | 48/48 | 1/48 | 0/48 | `pvm_barrows` (20/48) |
| `GOAL_SUPPLIES` | 16/48 | 0/48 | 0/48 | `skill_woodcutting_guild` (22/48) |

**Important:** Full filter categories intentionally narrow suggestions. The bug is that a *saved goal’s mandatory prerequisites* can be hidden too. The first position is not proof of feasibility even if `readyNow=true`; it checks only the current narrow rule fields and time-window marker.

### Real conditional quest progression, not imaginary leveling

- Full, precisely allocated quest kits support only **16 player-success-conditional completions**: Doric’s Quest (1,300 Mining XP and +180 GP), Imp Catcher (875 Magic XP), Druidic Ritual (250 Herblore XP), and Waterfall Quest (13,750 Attack and Strength XP). Each occurred twice by mode and twice by policy (GOAL_ANY / GOAL_QUESTS), after the actual engine ranked that quest first. Items consumed / reward bank additions are written into the trace.
- One-step successes do not establish a capability of training any skill repeatedly, and they do not imply safe quest combat or travel time. The oracle checks finite stock and carries explicit **player successfully completed quest** conditional assumptions. We stop at the next unsupported top recommendation.
- Cook’s Assistant, Sheep Shearer and The Restless Ghost quests receive **0 completions in 30 attempts each** because the engine emits a false UNMET when directly selected, or another category occupies first place. This is not proof those quests cannot be completed in OSRS.
- Waterfall→Cook goal shift in both Ironman and main is a verified reward-state sensitivity check: Attack 1→30, Strength 1→30 and Waterfall FINISHED. With all Cook ingredients already on hand, selecting Cook leaves top `UNMET` due the incorrect Cooking 4 gate. Two cases reproduce. Nothing is auto-completed by TPT.

### Coverage and neglect

- Across **48/48** GOAL_ANY profiles, top-ten tag lists include Fishing, but **0/48** include Cooking, Farming or Herblore. This does not mean these activities are absent from the whole catalog; rank placement and category filters matter.
- Across **48/48** GOAL_SUPPLIES profiles, *all suggestions* include zero FARMING tags. In SUPPLIES mode, 22/48 accounts (8/8 fresh level 3) are led by the broad “Reach 60 Woodcutting for the Woodcutting Guild” milestone instead of an exact executable short-term training activity.
- From 12 unchanged-state 1,000-step replays, **12/12** repeat the first recommendation at checkpoints 100, 500 and 1,000. Existing explicit SKIP behavior was demonstrated separately in W01; there is no automatically inferred rejection/defer from ignoring advice.
- Karambwan case: the Wiki lists level 65 Fishing, Tai Bwo Wannai Trio, raw karambwanji bait and a vessel. TPT’s `afk_karambwan` rule has a skill and quest gate but no observed quantity/gear check; `MET/readyNow` was asserted even without one or both tools.

## 3. Defect register

Issues list exact affected denominators and forensic pointers for owner/L2 replication. Counts are **scenario-cohort counts, not real-user incidence estimates**.

| ID | Severity | Affected frequency | Key fix |
|---|---|---|---|
| `PQA-W02-001` | P0 | 5/5 independently checked novice quest gates | Separate guide milestone from Wiki hard quest requirement |
| `PQA-W02-002` | P0 | 384/384 stock-state rows unchanged, spanning 48 profiles x 2 policies x 4 external GP/food/rune conditions | Add quantity-stamped per-account bank/inventory plus GP/consumable ledger and feasibility guard |
| `PQA-W02-003` | P0 | 8/12 Tai Bwo Wannai Trio-complete karambwan probes marked readyNow despite absent vessel or bait (24 probes total) | Gate repeatable supplies on exact bait, vessel, quantity, and route |
| `PQA-W02-004` | P0 | 48/48 GOAL_SUPPLIES profiles have zero FARMING-labelled suggestions | Multi-objective activity classification |
| `PQA-W02-005` | P0 | 39/39 account profiles with SOTE suggestion under GOAL_ANY lose it under both GOAL_PVM and GOAL_SUPPLIES | Always keep goal-critical blockers in the goal route as a separate lane, preserving filter for optional suggestions |
| `PQA-W02-006` | P1 | 16/210 source-backed conditional quest completions, each stops by next decision | Implement provenance-backed finite quest outcomes and repeatable skilling/GP/drop models separately |
| `PQA-W02-007` | P1 | 12/12 identical-policy unchanged-state replays retain same top at steps 1,100,500,1000 (12,000 engine calls) | Persist explicit snooze/reject with expiration and reason |
| `PQA-W02-008` | P1 | 22/48 GOAL_SUPPLIES profiles (including 8/8 fresh level 3) first get reach-60-Woodcutting guild milestone | Decompose abstract milestones into reachable training steps |
| `PQA-W02-009` | P1 | 0/48 GOAL_ANY top-ten lists include Cooking, Farming or Herblore tags | Add milestone gap debt / eventual convergence constraints, with transparent priority |
| `PQA-W02-010` | P0 | 2/2 conditional Waterfall→Cook-goal changes retain Cook UNMET despite all ingredients available | Correct novice gates |
| `PQA-W02-011` | P1 | 9/48 GOAL_QUESTS first suggestions not readyNow | Separate Ready / Verify / Work Toward lanes, prioritize executable tasks, keep provenance of uncertain access and safety |

For each issue’s **source file/line, reproduction, outcome, and evidence table**, see `issue_ledger_w02.csv` and `.json` in the ZIP. Every source location is W0751 ZIP-relative.

## 4. Explicit evidence boundaries

**ENGINE:** unchanged W0751 Java recommendation/ranking, actual first results and filtered lists. **ORACLE:** separately researched OSRS quest prerequisites, exact known XP and material rewards, with kit availability as an injected test condition. **SIMULATED:** state changes only on conditional quest success. **ASSUMPTION:** successful travel, survival, or dialogue for the quest; time and combat readiness not verified. **UNKNOWN:** drops, GP/hr, food consumed, paid-shop stock, continuous XP rates, total gameplay hours, end-to-end SOTE/Bowfa/Barrows-gloves convergence, real RuneLite integration.

**The 100/500/1,000 tables are not completed-activity journeys.** They are 12,000 unchanged-state calls to demonstrate repetitive advice. The 210 strict journeys terminate at unsupported outcomes after at most one approved conditional quest reward. No 100/500/1,000 completed-activity claims can be made.

## 5. Freeze and reproduce

`PathfinderWave02.java`, `PathfinderWave02GoalProbe.java`, the W01 base `PathfinderSimulationQA.java`, `REPRODUCE.md`, 11 issue records, CSV decision evidence, and a SHA256 manifest are included in the archive. Reproduction uses the exact separate W0751 ZIP with matching hash, not GitHub main. Two independent clean runs of W02 generated identical CSV bytes.

## 6. Source links for independent game facts

- OSRS quest XP/requirements: https://oldschool.runescape.wiki/w/Quests/Experience_rewards
- Cook’s Assistant: https://oldschool.runescape.wiki/w/Cooking
- Sheep Shearer: https://oldschool.runescape.wiki/w/Sheep_Shearer
- The Restless Ghost: https://oldschool.runescape.wiki/w/Prayer
- X Marks the Spot: https://oldschool.runescape.wiki/w/X_Marks_the_Spot
- Witch’s Potion: https://oldschool.runescape.wiki/w/Quest_experience_rewards
- Imp Catcher: https://oldschool.runescape.wiki/w/Imp_Catcher
- Doric’s Quest: https://oldschool.runescape.wiki/w/Doric%27s_Quest
- Druidic Ritual: https://oldschool.runescape.wiki/w/Druidic_Ritual
- Waterfall Quest: https://oldschool.runescape.wiki/w/Waterfall_Quest
- Karambwan catch: https://oldschool.runescape.wiki/w/Money_making_guide/Catching_raw_karambwan
- Ironman food and fishing: https://oldschool.runescape.wiki/w/Ironman_Guide/Fishing

**Next wave: PQA-W03** should run *after* named P0 fixes, validate all W02 fixtures on the resulting byte-pinned source, and then use the verified transition adapters to extend real action sequences. Current conclusion remains: **Pathfinder quality and account outcomes NOT VERIFIED; release hold**.