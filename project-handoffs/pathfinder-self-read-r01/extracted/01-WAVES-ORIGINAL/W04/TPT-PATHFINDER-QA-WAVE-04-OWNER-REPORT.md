# 🦝 PQA-W04 | Pathfinder Account Truth & Supply Sustainability
**November 3, 2026 · Feral Enterprises · Independent offline QA · RELEASE HOLD**

## Owner handback | Results first

**Source:** W0751 integrated archive SHA-256 `79e7d54a78a0bb76b6b3eca0e6804067b02b40a35e88339ee83a20408aa7ef3f` (441 ZIP members). **Parent test candidate:** PQA-W03 SHA-256 `b29c211e5f8e7cdcfced3c767951d136683053686f608fe7402858d3b1e3082d`. W04 is an **isolated cumulative patch against W0751**, not a modification of the installed source or GitHub. The standalone newer UI shell is not proof of a newer integrated Pathfinder engine.

**Accounts/journeys:** 48 frozen account profiles; 1,152 new source-driven resource-policy comparisons; 24 identity/freshness/container tests; 9 stack reconciliation cases; 25 kit checks; 10 quest-stage probes; 48 bank-close coverage cases. Replayed 210 W02 strict-follow journeys and W03 invariants. **0 verified long journeys**; only 56 conditional first quest completions across 210 (maximum 1 per journey).

### Five important findings

1. **W04 account truth bridge is isolated and guarded, not live-accepted.** It takes actual W0751 `BankEvidence`, not a replacement RuneLite scanner, and builds W03 `ResourceEvidence` only for current authenticated same-account, same-session, same-opening stock. **24/24 fence/coverage cases pass**; 9/9 disjoint bank+inventory+equipment count checks pass. A partial stable read preserves positives but cannot turn absent item IDs into verified zero. The host patch emits immediate invalidation on bank close/change/O-tab use and at 120 seconds; these host paths are **NOT RuneLite compiled/tested**.
2. **GP and rune planning remain supply-blind.** With everything else constant, changing 0→500,000 GP changes the top recommendation **0/96** times. Likewise, 0→6,000 of one rune item changes **0/96**. GP from verified ID 995 can now be observed; generic rune totals deliberately remain UNKNOWN because spell-specific requirements matter.
3. **Food quantity still does not establish sustainability.** One cooked food versus 200 changes the first recommendation **0/96** times. Zero versus 200 food changes 12/96; **84/96** unchanged. No food/kill or dose/trip model is certified. W04 recognizes only cooked karambwan ID 3144, monkfish 7946 and shark 385, and does not treat that sum as interchangeable HP or survival time.
4. **Finite kits now have real evidence boundaries.** **25/25 cases pass** (12 KIT_PRESENT, 10 VERIFIED_SHORT, 3 UNKNOWN): Cook’s Assistant (egg 1944, milk 1927, flour 1933), Priest in Peril (50 unnoted rune/pure essence 1436/7936 and bucket 1925), and first karambwan attempt (raw bait 3150 and vessel 3157/3159). A kit never implies a successful quest or enough karambwan bait for a 30-minute session.
5. **Fairy-ring acceleration has a new verified quest-stage bottleneck.** In **6/10** paired mode/skill profiles, Pathfinder blocks the Fairytale II recommendation on 40 Thieving, 49 Farming or 57 Herblore although the Wiki explicitly says these are not needed to start or unlock fairy rings. Do not 'fix' by automatically claiming ring access when quest status is IN_PROGRESS. Implement a witnessed staged unlock.

### Priorities

- **P0 owner + Tongs:** native compile and client-event test the new bank-stock bridge; preserve temporary O-tab freeze and account identity; test immediate sidebar invalidation under close, hop, withdrawal and revalidation.
- **P0 Pathfinder:** introduce per-action GP and rune requirements, approved bank+carried resource state, exact food/consumable trip budgets and resupply targets. Don't guess hours, kill counts, or catches.
- **P0 prerequisites:** distinguish quest START, intermediate stage and COMPLETE. Fairytale II partial unlock and the remaining 133 historically unaudited annotations are priorities.
- **P0 account memory:** durable per-account Bank Memory with item identity, quantity, variant, timestamp, source and stale status is STILL NOT IMPLEMENTED by this W04 patch; do not treat the open-bank-only adapter as meeting first-run account baseline requirements.
- **P1 simulations:** when real transition evidence exists, rerun exact 48 cohort baselines toward 100/500/1000 **completed**, not merely repeated, activities.

### Provenance

**ACTUAL W0751 engine:** `GuideData.rules()`, `SuggestionEngine.suggest/evaluateAll`, `BankEvidence` and original `BurrowPlugin`. **W03 inherited candidate:** five corrected novice quest gates, route preservation, Farming coverage, quantity reasoning. **NEW W04 candidate:** `PathfinderAccountTruth`, `PathfinderSupplyKit`, `SuggestionEngine` kit explanations and integer overflow protection, and `BurrowPlugin` guarded source connection/lifecycle publishing. **Independent Wiki:** item IDs and explicit requirements listed below. **Assumptions/policy:** 120-second freshness threshold is a conservative QA design choice, NOT an OSRS mechanic; fixture-owned bank state is injected, NOT harvested from live RuneLite. **Unsupported:** live persistence, quest success/XP/loot, consumption rates, game actions, 100+ verified tasks, RuneLite host runtime.

### Source-backed OSRS checks (retrieved Nov 3 2026)

- https://oldschool.runescape.wiki/w/Cook%27s_Assistant ; https://oldschool.runescape.wiki/w/Egg ; https://oldschool.runescape.wiki/w/Bucket_of_milk ; https://oldschool.runescape.wiki/w/Pot_of_flour
- https://oldschool.runescape.wiki/w/Priest_in_Peril ; https://oldschool.runescape.wiki/w/Rune_essence ; https://oldschool.runescape.wiki/w/Pure_essence ; https://oldschool.runescape.wiki/w/Bucket
- https://oldschool.runescape.wiki/w/Karambwan_vessel ; https://oldschool.runescape.wiki/w/Raw_karambwanji ; https://oldschool.runescape.wiki/w/Money_making_guide/Catching_raw_karambwan
- https://oldschool.runescape.wiki/w/Cooked_karambwan ; https://oldschool.runescape.wiki/w/Monkfish ; https://oldschool.runescape.wiki/w/Shark
- https://oldschool.runescape.wiki/w/Fairytale_II_-_Cure_a_Queen ; https://oldschool.runescape.wiki/w/Fairy_ring

### QA evidence and source locations

See `ISSUE-LEDGER.csv` for all 14 issues: exact affected counts and denominators (or N/A when native evidence does not exist), source locations, reproduced scenario, severity and suggested fix. `evidence/` contains full source-generated CSVs, including before/after lifecycle and W02/W03 regression results. `candidate/` is for code review only.

**Release disposition:** HOLD. No production code, RuneLite account, bank data, GitHub, R31/R32 research or canonical ticket register changed. No native acceptance claimed.