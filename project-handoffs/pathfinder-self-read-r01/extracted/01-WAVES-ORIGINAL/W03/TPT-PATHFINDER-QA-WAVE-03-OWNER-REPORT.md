# 🦝 FERAL ENTERPRISES | TPT Pathfinder Independent Intelligence QA | PQA-W03
**Date:** 2026-11-03 (America/Los_Angeles)  
**Authority:** read-only production / isolated Java patch candidate; **RELEASE HOLD**.  
**No production source, GitHub, installed RuneLite, live account or old research ZIP was modified.**

## Results-first owner handback

- **Same real engine, two revisions:** untouched W0751 ZIP SHA256 `79e7d54a78a0bb76b6b3eca0e6804067b02b40a35e88339ee83a20408aa7ef3f` versus isolated `PQA-W03-CANDIDATE-R01` patch in this bundle. W0751 is the newest accessible integrated source found; later UI-shell ZIPs are standalone, not a proven replacement for the integrated Pathfinder engine. GitHub main's last inspected snapshot was older B38; no matching W0751 Git SHA established.
- **Simulation coverage:** 48 frozen profile baselines; 240 policy comparisons; **210 strict follow journeys**; 240 goal-route rows; 144 Farming rows; 96 karambwan feasibility cases; 6 bank-standing cases; 10 gear ownership cases; 36 saved-goal attention/window cases; **672** resource-budget rows (48 profiles × 2 policies × 7 stock states). Frozen 12 × 1,000 unchanged-state replays also repeated on candidate.
- **Completed journeys:** **0 verified full long journeys**. Conditional, player-success-dependent first-step quest completions rise from **16/210 original to 56/210 candidate**. The remaining attempts fail at unsupported activities or a finite-kit oracle boundary; **0** reach 100/500/1,000 completed activities.

### Top five owner findings

1. **Novice quest gate error isolated:** **5/5** Wiki-confirmed false level requirements corrected in candidate; **10/10** mode checks MET. W0751 falsely required Cooking 4, Crafting 3, Prayer 9, Agility 4 and Magic 4 for five no-level novice quests. Candidate separates those ironman.guide waypoints from hard quest gates.
2. **Goal chain survives filters:** **20/20** unfinished SOTE/Bowfa-goal profiles retain SOTE route in PvM and **20/20** in Supplies. W0751 did so for **0/20** in both. **36/36** saved-goal short-window / LOW / AFK visibility checks preserve the target without calling incompatible activities startable.
3. **Farming is no longer invisible in Supplies:** **48/48** candidate profiles show a Farming-related recommendation in Supplies and **48/48** in GP, keeping herb runs and farming contracts dual-purpose. Previously **0/48** in Supplies. This is coverage, not proof of herbs/hr or Farming XP.
4. **Stock honesty improved, but only in synthetic injected data:** **96/96** karambwan scenarios correctly distinguish start feasibility (vessel ID 3157/3159, raw bait ID 3150, same account/freshness), **6/6** bank-standing tests guard quantity truth and **10/10** owned-gear tests avoid stale-quantity completion. **No 30-minute bait/food production rate is certified.**
5. **Economy and long-term sustainability still blocked:** **96/96** GP-zero comparisons and **96/96** runes-zero comparisons kept identical top choices. **96/96** one-food-versus-200-food comparisons also kept identical tops. Zero food changed the top choice in only **12/96** and downgraded Barrows readiness in **58/96**. Crucially, W0751's RuneLite host has **2/2 constructor paths** still using the old 8-field snapshot, so candidate quantities are not live-wired.

**Recommended priority:** P0 wire trusted per-account quantity evidence and safe host bridge; P0 independently verify remaining quest gates; P0 encounter-specific consumables and GP/rune budgets; P1 distinct goal-route lane and task-status semantics. **Next wave: PQA-W04, account truth + source-backed supply transition adapters + targeted Windows owner QA.**

## Design and evidence boundaries

**ACTUAL ENGINE:** both revisions use `SuggestionEngine.suggest(snapshot, preferences)` and `evaluateAll(snapshot)` in Java; no substituted ranking logic. **PATCHED CANDIDATE:** only `GuideData.java`, `PathfinderActivityTags.java`, `PathfinderTiming.java`, `SuggestionEngine.java` are changed in a separate directory. **ORACLE:** OSRS Wiki verified novice quest hard requirements, raw karambwanji/vessel identities, finite quest XP rewards/ingredients. **ASSUMED:** successful completion of a quest after independently validated kit, combat and route; actual travel/survival/gameplay is not observed. **UNSUPPORTED:** arbitrary repeatable XP/loot, GP/hr under current economy, drops, item consumption over time, cooking burn rate and 100+ genuine task-completion progress.

**Important difference:** `readyNow` historically means *can start* from observable preconditions, not *has all inputs to finish a quest*. In **112/210** candidate strict journeys, the external kit oracle could not verify immediate quest completion, despite a startable quest. Do not advertise these as quest-completable itineraries. An explicit quest `Gather supplies` stage and verified live snapshot are needed.

### Remaining release blockers

- **133 other skill-annotated quest rules unverified.** In W0751, 138/215 quest rules have an ironman.guide level annotation treated as a hard gate; five are verified false and patched. We have not certified the remaining 133. Even if the candidate passes its current five corrections, the general rule import problem remains.
- **Food quantity is not a trip budget.** One food and 200 food behave identically in all 96 matched comparisons; a zero-food mark merely demotes some bosses to `UNKNOWN`. Don't infer travel, sustain or safe kill count.
- **Raw bank IDs are not a durable account baseline.** Candidate `ResourceEvidence` enforces synthetic `sameAccount/fresh/completeCoverage` flags; W0751 `BurrowPlugin.java:735-750` does not wire trusted counts, GP, carried state, account keys and timestamps into it.
- **No verified long-term convergence.** One-step reward transitions work for Waterfall→Cook goal changes (2/2 modes); 12/12 unchanged-state 1,000-step replays still repeat top advice at every checkpoint. Nothing establishes progress toward Barrows gloves, SOTE or Bowfa through hundreds of completed tasks.
- **No RuneLite acceptance.** Offline compiler is Java 21 with `--release 17`, not a Windows Java 17 RuneLite client run. W03 engine source patch is not a plug-and-play release ZIP.

## Fix audit trail and frequency

All **12** findings are in `ISSUE-LEDGER.csv` and `.json`: 6 candidate improvements (some with limitations) and 6 open blockers. Each record contains an exact count/denominator, Java source location, reproduction scenario, severity, disposition and proposed fix. Results are deliberate synthetic cohort counts, not an estimate of real player incidence. `evidence/baseline-w02` exactly matches the frozen prior W02 CSVs; two independent clean candidate runs produced matching output hashes.

## OSRS facts used

- Wiki quest requirements: https://oldschool.runescape.wiki/w/Quests/Requirements and https://oldschool.runescape.wiki/w/Cook%27s_Assistant
- Karambwan: https://oldschool.runescape.wiki/w/Raw_karambwan ; https://oldschool.runescape.wiki/w/Raw_karambwanji ; https://oldschool.runescape.wiki/w/Karambwan_vessel
- XP reward and quest-order controls: https://oldschool.runescape.wiki/w/Quest_experience_rewards ; https://oldschool.runescape.wiki/w/Waterfall_Quest

**Disposition: RELEASE HOLD**. Candidate is useful for engineering intake but must not be merged or represented as live Pathfinder verification without independent L2, a safe account-data adapter and targeted owner Windows QA.