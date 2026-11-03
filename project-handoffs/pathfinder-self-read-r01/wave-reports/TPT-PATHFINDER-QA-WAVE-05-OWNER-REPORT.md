# 🦝 PQA-W05 | Resource-Aware Decision Intelligence
**November 3, 2026 · Feral Enterprises · Independent offline QA · RELEASE HOLD**

## Results first

**Baseline:** Wave 7.5.1 ZIP SHA-256 `79e7d54a78a0bb76b6b3eca0e6804067b02b40a35e88339ee83a20408aa7ef3f`. **Parent independent candidate:** W04 QA ZIP SHA-256 `734ff4d6dca4b48e7104be1c05fa7419edc86160d1c4a31a7bc47161e0432db5`. W05 is an isolated cumulative patch against the original W0751 source, not an edit to the owner's active plugin, GitHub or account data.

**Accounts/journeys:** 48 frozen accounts; 186 paired W04-versus-W05 source decisions (162 Plank Make and 24 quest-stage); 1,512 verified-code budget cases, 100 alternative essence threshold cases, 48 witnessed-stage probes, 6,000 unchanged-account recommendation calls across six 1,000-decision replay cohorts. **0 verified completed 100-step journeys**. Replayed W04 tests: 24/24 account fences, 9/9 bank-stack reconciliations, 25/25 supply kits, 1,152 resource-policy comparisons (one W04 assertion deliberately updated for the corrected Fairytale II gate).

### Five most important findings

1. **Plank Make input truth materially improves, but Lunar spellbook is still unobserved.** Across 162 paired scenarios, W04 labelled all 162 `MET`. W05 now reports **90 UNKNOWN and 72 UNMET**; the first recommendation changes in **54/162**. W05 has a separate exact source-approved budget for seven log types (normal, oak, teak, mahogany, camphor, ironwood, rosewood). In 1,512 cross-product cases: **160 ENOUGH_FOR_ACTION, 258 VERIFIED_SHORT, 1,094 UNKNOWN**, all 1,512 pass the offline harness, and an independent cast-count calculation found zero discrepancies. This is not proof of a cast, selected spellbook, equipped staff, travel or sustainability.
2. **Partial alternative-item counts no longer cause false shortages.** In **4/100** essence evidence cases, W04 could label the 50-essence Priest in Peril input short even though the other essence variant was unobserved. W05 returns `UNKNOWN`, not `VERIFIED_SHORT`. Positive known totals >=50 remain sufficient for the finite ingredient check; this does not claim the quest can be completed.
3. **Fairy-ring unlock is recognized as a witnessed stage, not simply quest IN_PROGRESS.** W04 hid the milestone in **6/24 paired quest-stage cases** immediately on `IN_PROGRESS`; W05 instead keeps it visible as `UNKNOWN` until the Godfather's permission is separately witnessed, or the quest is finished. Another **6/24 cases** with `Quest.UNKNOWN` are no longer incorrectly called ready. The W04 hard completion-level gate prevented Fairytale II starting in **4/6** paired NOT_STARTED low/high level cases; W05 removes that gate, while explaining full completion still needs 40 Thieving/49 Farming/57 Herblore. Synthetic permission proofs are *not* live RuneLite evidence.
4. **Other bank methods still need action-specific budgets.** `afk_string_jewellery` and `afk_enchant_jewellery` had partial ingredient tests that could mark them ready on astral/cosmic runes alone. W05 quarantines both as `UNKNOWN` pending exact target, full rune recipe and selected spellbook evidence. General GP planning, travel time, potion dose and loot probability remain unsupported.
5. **Long-term sustainability and convergence remain unproved.** Across the inherited 96 paired stock comparisons, one food vs 200 food changed the first recommendation **0/96**; 0 vs 500,000 GP and removed rune stock changed it **0/96** for the general cohorts. Six unchanged-state profiles repeat the same recommendation at checkpoints 100, 500 and 1,000. No actual quest, combat, XP, loot or budget state transitions were fabricated to inflate completion counts.

## Fix priority

- **P0:** Native RuneLite JDK17 Gradle and gameplay acceptance; read-only quest-stage permission witness and observed spellbook, bank identity/freshness boundaries, Bank O-tab lifecycle. No game interaction automation.
- **P0:** Per-activity GP, rune, food and potion consumption models with exact resource units and UNKNOWN for unsupported methods. Do not use one global rune total or generic 'food count' as a trip budget.
- **P0:** Durable per-account baseline and Bank Memory with timestamp and trust state. W04/W05 open-bank adapter is *not* the owner's first-run account snapshot implementation.
- **P1:** Goal convergence with completion evidence and explicit rejection/skip feedback. Return to 100/500/1000 **completed** actions only when reliable state transitions are available.

## Evidence, caveats and source lines

`ISSUE-LEDGER.csv` has **14** issues with scenario, exact affected counts, severity and source locations. `evidence/` contains all fresh decision outputs, the W04 replay, and paired before/after source traces. `candidate/` contains 14 relevant Java source files for independent review. The cumulative 8-file patch is relative to **W0751**, not W04. `SOURCE-LINKS.md` records verified Wiki pages and separate simulation assumptions. `REPRODUCE.md` documents a clean patch, compile and byte-equality check.

**Disposition: RELEASE HOLD.** Offline source compile with JDK21 `--release 17` and deterministic test harness only. No independent Windows JDK17 Gradle or actual RuneLite verification; no production code, GitHub, account progression, bank, or canonical tickets modified.