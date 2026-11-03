# Pathfinder | Spoons Master Audit & Risk Ledger (Waves 1–6)

**Date:** 2026-11-03  
**Audit mode:** Read-only, independent source-level review of Cursor's isolated Pathfinder ZIPs.  
**Current status:** **REWORK** for full Pathfinder engine; **PASS_SCOPED** for previously tested repairs; **RELEASE HOLD** for integrated TPT and RuneLite.

## Executive finding

Pathfinder learned how to produce numerous suggestions before it learned how to certify their **true prerequisites, resource quantities, evidence freshness, account identity and intended activity duration**. Repeatedly patching a demonstrated counterexample improves the specific scenario, but all routes to `readyNow=true` and all significant progression journeys require independent scrutiny. Recommendation capability and genuine, account-safe startability are different contracts.

## Wave-by-wave facts and limitations

| Wave | Supplied fixes / independent evidence | New independent findings at that wave | Disposition |
|---|---|---|---|
| W01–W02 | Independently compiled supplied isolated Java; Cook's Assistant guide over-gating repaired. Simulation CSV 4,578 actions; 1,268 COMPLETE, **3,286 NO_TRANSITION**, 24 STOP_NO_LEGAL; **11** actual profiles although summary said 12. 522 'meaningful completions' aggregate repeated runs, not unique. | Song of the Elves level-1 READY, Black Knights' Fortress with 0 QP READY, Biohazard without Plague City, glassblowing and battlestaff crafting without materials, Vorkath using stale/unverified food. | REWORK |
| W03 | **20/20** targeted regressions reran; previous named examples no longer falsely READY. OSRS XP level threshold table included. | Outfitter BLOCKED still allowed Vorkath READY; stale/missing/contradictory bank evidence; bank methods without defined input groups; simulator free level gains / partial XP lost; no flagship goal reached. | PASS_SCOPED + REWORK |
| W04 | **19/19** Wave 4 and **20/20** L2 reran; the two specifically reported W03 defects addressed. | **8/8** newly authored safety gates failed: incomplete rune AND groups, enchanting/stringing missing targets, glassblowing tool, stale bank quantities, unbound Best-in-Bank READY; hard-coded output directory. 35/36 simulation journeys stopped without legal transition. | PASS_SCOPED + REWORK |
| W05 | **101/101** established checks passed. Previously demonstrated eight W04 cases repaired. | Independent **12/13** adversarial scenarios flagged unmet expectations: bolt-tip chisel, full spell inputs, account/method/plan provenance, missing quantity keys and broader crafting/cooking coverage; 11/12 simulation profiles dead-ended. Historical W04 parent ZIP hash in Cursor W05 handoff did not match provided W04 ZIP. | PASS_SCOPED + REWORK |
| W06 | Independently reran **136/136** supplied regressions, including the prior 13 adversarial W05 scenarios; positive coverage checks passed for restricted recipe set. Source compared to uploaded W05 (6 modified and 4 new Java, 23 identical). W06 parent W05 ZIP SHA matches. | **10/11** new independent scenarios failed (1 control passed), including cross-account false READY, pre-session bank snapshot, spell quantities, unsupported duration claims, fly fishing level/tool, smithing hammer/bars, raw vs unfired pottery, gilded altar supplies/access. **11/12** simulated profiles dead-end. **10 package + 6 source stale checksum entries**. | PASS_SCOPED + REWORK |

**Counting warning:** Regression counts across waves are not additive measures of unique functionality or gameplay correctness. In particular, 136/136 inherited checks plus new independent failed cases does **not** imply engine PASS. New independent red probes are working as designed to reveal gaps.

## Highest-priority open Wave 6 defects, independently reproduced

| ID | Severity | Failing behavior | Correct contract |
|---|---|---|---|
| W6-L2-01 | P0 | Account B with leftover account A `bossReadiness["Vorkath"]` string is READY. The wrapper assigns the host B identity to the old string, effectively self-authenticating it. | An Outfitter-issued immutable account/session/method/plan/bank-generation evidence record, validated independently against trusted host expectations; mismatch or missing provenance must be VERIFY, never READY. A→B→A test. |
| W6-L2-02 | P0/P1 | Bank snapshot labeled VERIFIED_CURRENT but dated before current login still certifies READY. | Trusted account, session, timestamp and observation-generation chronology; invalid or contradictory freshness returns VERIFY. |
| W6-L2-03 | P0 | Plank Make marked READY with one astral, one nature, one earth, one coin and a log. | Verified per-cast rune counts (2 astral, 1 nature, 15 earth or sound substitutions), log-specific coins, active Lunar/quest access and valid observed supplies. |
| W6-L2-04 | P0 | String Jewellery marked READY with only one each of astral, earth and water runes. | Verified per cast 2 astral + 10 earth + 5 water or valid substitutes, Lunar access, unstrung jewellery and source-backed state. |
| W6-L2-05 | P1 | One Wind Strike cast supplies are treated as sustainable for a 60-minute splashing session. | Separate `STARTABLE_ONCE` from `SUSTAINABLE_FOR_WINDOW`, use verified rates/quantity budgets or UNKNOWN_FOR_WINDOW. |
| W6-L2-06 | P1 | One raw food item produces 30-minute sustainable cooking recommendation. | No duration promise without verified supplies and action rate; one action may be startable. |
| W6-L2-07 | P1 | Raw clay alone certifies pottery firing readiness. | Use correct unfired item for firing; model shaping distinct from firing and confirm access. |
| W6-L2-08 | P0 | Fishing level 1, no fly fishing rod/feathers -> `afk_fly_fishing` READY. | Source-backed minimum Fishing 20 for trout, required gear/bait and accessible area; 'train fishing eventually' must not be framed as immediately fly fish. |
| W6-L2-09 | P0/P1 | Level 1, no hammer or bars -> `afk_smithing_one_bar` READY. | Actual usable bar, hammer and anvil access, plus appropriate skill level for recipe. |
| W6-L2-10 | P1 | No bones or verified altar access -> `afk_gilded_altar` READY. | Evidence of consumable bones and relevant altar access, or VERIFY/route instruction rather than current READY. |

**Independent control:** Missing-chisel bolt-tip case is now NOT_READY, which is correct. The W6 independent harness had **10 failed expectations and 1 passing control**. Reproduction and raw outputs are retained unmodified in `SPOONS_L2_ORIGINALS/PATHFINDER_SPOONS_L2_W06_R01.zip`.

## Cross-cutting architectural reasons for false READY

1. **Positive evidence is not bound to its origin:** Untrusted `Map<String,String> bossReadiness` text can be relabeled with current host identity. Future typed result MUST have separately produced claimed identity and trusted expected identity. Matching strings alone are not authentication.
2. **Snapshot freshness is not invariant:** `VERIFIED_CURRENT` enums and positive item quantities may disagree with bank availability or timestamp. Validate all fields together; a saved bank snapshot is useful planning evidence, not always current possession proof.
3. **Recipes are incomplete:** `Set.of` ambiguously expresses AND versus OR; item presence ignores amount, charge, staff substitutes, tool, spellbook, access, location and item form.
4. **Immediate feasibility is confused with duration:** One action can be possible while 15–120 min AFK sustainability is unverified; label those outcomes differently and budget resources from source-backed rates.
5. **Catalog not completely gated:** W06 restricted recipe coverage correctly had zero false READY *among configured RECIPES*, but a separate fresh level-1 / empty-bank probe still showed **49 READY** overall (not all are necessarily wrong). Every resource-dependent `NOT_CHECKABLE` rule needs classified evidence or must be VERIFY.
6. **Simulator incompleteness:** 11/12 profiles dead-end; distinguish no represented transition, no observed state, unsupported quest/gear/skill action and truly impossible progress. A late pre-qualified SoTE finish does not demonstrate full fresh-account goal paths.
7. **Source and packaging lineage:** W05 earlier claimed W04 parent ZIP hash differs from uploaded W04; W06 correctly matches actual W05 archive but has stale internal checksum manifests. Git commit IDs cited by Cursor are not independently confirmed against repository.

## State model that should emerge (specification, not claimed implementation)

- `AccountObservation`: authenticated account identity, domain, revision, source, observedAt/verifiedAt, completeness and certainty; persistent and per-account.
- `BankSnapshot`: full native bank generation, timestamp, ID/form/quantity/variant/charge provenance, source completeness and account; keep last known without overclaiming current stock; O temporary filtered bank lease is not an authoritative full scan.
- `OutfitterReadiness`: **Outfitter-produced** immutable account, login, boss, combat style/method/plan, bank generation, data timestamp, status and blockers. Host independently evaluates expected identity and current generation.
- `PathfinderAction`: sourced hard requirements, alternatives, item forms, quantities per action, verified tool/location, resource budget per planned time, uncertainty and reason for NOT_READY/VERIFY/BLOCKED.
- `SimulationTransition`: actual XP ledger, item/GP consumption, authentic unlock prerequisites, time, legal action transitions and unsupported end causes.

Do not implement new Account/Bank Memory inside Pathfinder just to make unit tests pass. Connect to the later approved host-state implementation and treat absent native data as a readiness blocker for certified advice.

## Product requirements that must not fall off the roadmap

- First-run account capture for skills and XP, quests, Achievement Diaries, Combat Achievements and boss/slayer kill-log where legitimately observable; guided player-opened native interfaces where passive observation unavailable.
- Versioned per-account persistent memory across logouts, hops, restarts and plugin upgrades. Changes in evidence must not erase last-known verified facts if source currently unavailable.
- Manual account Sync and roughly seven-day per-domain verification reminders that distinguish unchanged verified data from unobserved data, with snooze/dismiss.
- A dedicated accessible Bank Memory **tab/view**, complete-full-bank snapshot only, timestamp/quantities/variants/charges where observable, last known rather than unqualified live ownership; placement as fifth main or nested tab still owner decision.
- No automated banking, no Wilderness Best in Bank, no fabricated KC, no user account actions, no production merge/Plugin Hub submission without exact current repo and native QA.
- UI stays last, preserve four historical tabs until owner resolves new Bank Memory navigation, 225px width, 4×7 inventory, independent Chronicle selector, armored lean raccoon and no rejected chunky mascot.

## What has actually been verified

- Original archive SHA and CRC for Cursor W01-02 through W06: recorded in unchanged independent audits; original file bytes preserved in pack.
- W06 original source behavior on synthetic account data: 136 supplied checks green, 10 new negative expectations red, 1 control green, JDK21 `--release 17` isolated compilation.
- Prior targeted repair behavior for named quest, bank and recipe cases. **No full native plugin verification** in these Pathfinder waves.

## Next steps, in order

1. **Wave 7** (read `NEXT_COMMAND_PATHFINDER_WAVE7.md`): independent provenance from Outfitter, quantities per cast and time window, entire catalog rule gating, correct 49 fresh-L1 readiness audit, fix source checksum manifests, track 12 profile dead ends honestly. Reproduce ten red negative cases before repairs; turn green *without lowering safety expectations*.
2. Reconcile precise `feralenterprises/Burrow` checkout and latest full W0751 (or newer) host source. These Wave 1–6 isolated patches must not silently revert working Bank R05/Bank O, Outfitter, Sessions, HoverLite, UI shell or owner's data.
3. Complete one-engine-at-a-time independent QA until Pathfinder can demonstrate credible, source-backed prerequisite checks and meaningful progression. Preserve negative tests as permanent regression oracles.
4. Separately implement and independently verify Account Memory / Bank Memory in full RuneLite host when authorized; establish JDK17 Gradle and native account-switch/bank-close/sync tests. Confirm Bank Memory tab placement before UI integration.
5. Only then contemplate cross-engine integration, polished final sidebar, security/release review and Plugin Hub submission.

**Handoff verdict:** `PATHFINDER=REWORK`; `SCOPE-TESTS=PASS_SCOPED`; `LIVE-INTEGRATION=NOT_VERIFIED`; `TPT-RELEASE=HOLD`.