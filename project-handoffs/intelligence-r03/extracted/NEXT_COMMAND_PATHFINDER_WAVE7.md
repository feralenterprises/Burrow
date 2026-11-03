# CURSOR | PATHFINDER WAVE 7
## Verified resource costs + genuine Outfitter origin + catalog false-READY closure

**RELEASE HOLD. Pathfinder only. No UI, GitHub merge, bank automation or unrelated engines.**

Read the entire attached `SPOONS-PATHFINDER-W06-INDEPENDENT-L2-REWORK.md`. Use the exact current repo commit and supplied Wave 6 source; reconcile to the actual current TPT baseline without overwriting newer host work. Retain Wave 1–6 repairs. No time limit: solve root classes, not fixtures.

### First: independently reproduce

Compile Wave 6 with Java 17 bytecode and run `evidence/Wave6SpoonsIndependent.java` **unmodified**, record all ten failures and the passing control. Inspect Cursor's Wave 6 `Wave6IdentityAdversarial.java`: it incorrectly expects B to accept A's leftover Vorkath READY string. That test expectation must be corrected to reject stale cross-account evidence, not counted as a safety success. Preserve the other legitimate green tests.

### Repair tracks

**P0 cross-engine origin:** Replace `Map<String,String> bossReadiness` as a source of certification with an Outfitter-issued typed result carrying immutable origin account, login/session, boss, method, plan ID, bank observation generation and observation timestamp. The trusted host supplies *separate* independently observed expectation fields. No `fromSnapshot` self-copy of host values into the claimed evidence. Reject A→B leftovers, old generations, old sessions, wrong method/plan, missing evidence and contradictions. Document how host actually populates the result; absent integration => VERIFY and unverified blocker.

**P0 craft/AFK feasibility:** Represent action recipes as verified minimum **per action** resource counts with item-ID forms and compatible substitutes, plus distinctly calculated duration budget. Fix Plank Make (2 astral, 1 nature, 15 earth or verified alternative, valid log and correct coin cost per wood), String Jewellery (2 astral, 10 earth, 5 water or verified alternatives) and the entire spell/crafting family. Handle staff *equipped vs in bank*, simultaneous rune alternatives, charge state, active spellbook evidence, log cost and current supply generation as available; do not invent unknown host evidence. For one action only display STARTABLE_ONCE, never a 30/60-minute sustainability certificate based on 1 item. If planned window cannot be substantiated, mark UNKNOWN_FOR_WINDOW.

**P0 catalog correctness:** Fix `afk_fly_fishing` with 20 Fishing, rod, feathers and accessibility; `afk_smithing_one_bar` with a bar, usable hammer and accessible anvil; `afk_gilded_altar` with bones and known altar access; `afk_firing_clay` with unfired pottery vs a separate clay-shaping activity. Audit every AFK, GP, SKILL, SUPPLIES, COMBAT and QUEST rule with a demonstrated readyNow path, especially all `NOT_CHECKABLE` rows and the 49 fresh-L1 READY results. Some training *goals* can be relevant at level 1, but must not be mislabeled as immediately performing a level-gated training method. Produce executable coverage, source URLs, and a blocked/verify/ready rationale per rule. Do not globally suppress all activities; model valid immediately-startable beginner actions faithfully.

**P1 progress simulator:** Reproduce the 12 deterministic profiles and trace why 11/12 dead-end. Distinguish missing observed account evidence, missing activity coverage, not-represented transitions, and legitimate incomplete goals. Real XP/resource costs only. Do not make up Bowfa, RFD or Barrows gloves success.

**Package integrity:** Regenerate `SHA256SUMS.txt` and `SHA256SUMS_SRC.txt` only after archive is finalized: Wave 6 shipped 10 and 6 stale entries respectively. Verify all recorded digests. Provide source patch against verified Git commit, raw repro logs, independent test outcomes, coverage map, unresolved risk ledger, actual JDK17 Gradle/RuneLite execution status. If full host source is still absent, label it NOT VERIFIED, never a pass.

### Mandatory handback

Produce `PATHFINDER_WAVE7_COMPLETE.zip` with exact commit/tree hash, source diff, current manifest, unmodified independent red→green evidence, 136 inherited checks, additional coverage and cross-account test cases, 12-profile simulation traces, findings, OPEN issues, and a short results-first owner report. TPT release remains HOLD until isolated engine PASS plus native integration. Final verdict: PASS_SCOPED, REWORK or BLOCKED with evidence.