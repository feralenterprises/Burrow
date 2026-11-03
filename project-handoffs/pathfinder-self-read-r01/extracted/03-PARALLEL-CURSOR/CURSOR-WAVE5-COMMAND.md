# CURSOR | PATHFINDER WAVE 5
## Source-level resource recipe and provenance integrity repair

**Engine:** Pathfinder ONLY. **Release:** HOLD. **Evidence:** read `SPOONS-PATHFINDER-W04-INDEPENDENT-L2-REWORK.md` plus independent Java probes and original Wave 4 source. **Goal:** eliminate the eight reproduced false-READY outcomes and audit every equivalent path. **Timebox:** none. **UI:** defer.

**1. Baseline and regression discipline**
Identify exact current source tree/commit and the supplied Wave 4 base; prove lineage before editing. Do not overwrite the working project with the isolated archive. First reproduce all independent failing gates as RED, plus the 39 passing existing regressions as GREEN. No changing assertions to hide failures.

**2. Replace partial material checks with complete, typed requirements**
Review each of the 12 enumerated bank-standing methods and every other resource-dependent activity. Require all indispensable inputs (AND), support only sourced functional alternatives (OR), and track required quantities/charge states, spellbooks, equipment/tools, recipe completion, account mode, unlocks, current inventory/equipment/bank sources and travel/setup feasibility. In particular audit splashing (both runes or valid staff), enchanting and String Jewellery (required jewelry), glassblowing (pipe), crafting, herb inputs, ammo, and food. `READY` requires a coherent startable setup, not one item from a recipe.

**3. Unify truth and provenance**
Prevent `BankStatus` and `BankFreshness` contradictions; stale/absent/persisted quantity keys must never become `OBSERVED` current stock. Add account ID, snapshot generation, capture timestamp, and read/lease type as needed for actual production integration. Any unrecognized or cross-account evidence yields VERIFY/UNKNOWN. Preserve saved historical bank data for informative display, but never upgrade stale evidence to current ownership.

**4. Pathfinder-Outfitter typed contract**
Replace or securely adapt plain `Map<String,String>` boss readiness flags with typed boss/method/plan/account/bank-generation evidence and proof of readiness. A bare `READY: ...` string cannot certify current setup when bank observations are unavailable or contradictory. Blocked gear remains blocked; unobserved gear remains VERIFY. Preserve wilderness Best in Bank prohibition and Bank O manual-only behavior.

**5. Test as a hostile reviewer**
Run 8 provided negative gates, 39 existing wave regressions, exhaustive property/matrix tests per recipe (each required ingredient missing in turn, alternative variants, zero/insufficient/stale supplies, inventory-only and bank-only, missing/unknown IDs, cross-account contamination, login/logout, bank reopen/O lease). Add user-goal progression tests with actual XP thresholds and verifiable transitions. Show test inputs, expected verdict, real verdict, and actionable correction.

**6. Clean reproducibility**
Remove `/workspace` and `/opt/cursor` hard-coded paths from simulation and coverage tools. A new clean checkout with genuine JDK 17 Gradle should work; if unavailable, state NOT VERIFIED rather than substituting Java 21 release-target compilation. Do not modify any UI or unrelated plugin engine to make tests green.

**7. Handoff**
Deliver `PATHFINDER_WAVE5_COMPLETE.zip` with exact baseline hash/commit; changed-file manifest/diff; full executable source (not a test-only shim); every RED→GREEN evidence trace; all rule coverage; remaining false-READY risk ledger; complete deterministic simulation summaries; authentic test logs; the current host integration and Account/Bank Memory blockers clearly labeled; instructions for independent Spoons QA. Declare `PASS_SCOPED`, `REWORK`, or `BLOCKED`, never an unsupported blanket PASS. Do not publish to GitHub or the Plugin Hub without owner authorization.

Proceed with Wave 5. Stop for independent review when done.