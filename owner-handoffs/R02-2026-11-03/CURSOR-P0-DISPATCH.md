# CURSOR DISPATCH | TPT-INTEL-001 | P0 FIRST-RUN CAPTURE REPAIR

**Mode:** Implementation on a NEW isolated feature branch, with complete source delivery. Release HOLD. Do not merge `main`.
**Exact source:** Reconstruct owner-tested R02 from this handoff's `parts/` and `reassemble_r02.py`, verify SHA-256 `adf1d64fdb3d39b003deadff4e6ae312c27667a3b12f3a251037f14018437653`, and inspect actual Java implementation before coding. The inherited GitHub source is NOT necessarily R02.

## Player-visible defect to fix first
Account has 178/214 quests detected, but first-run capture still reports Achievement Diaries, Combat Achievements, Boss Kill Log and Slayer Kill Log as UNKNOWN even after the owner opened relevant native interfaces. The owner visually observed diary counts and slayer kill counts in RuneLite. Do not confuse `observed screen`, `review receipt` and `extracted persisted numeric progress`.

### Required P0 behavior
1. Identify precise RuneLite widget/config/event source for each observed native interface and instrument diagnostic evidence. Capture actual reliable data where the API exposes it. If it does NOT, ship guided click-through capture with explicit page coverage and honest partial/unknown status, not invented completions or an unconditional green check.
2. Store exact extracted values and provenance per OSRS account across logout, client restart and account switching. Handle existing legacy account profiles safely. Ensure persisted account snapshots are visible to the user in Capture/Home; never show `UNKNOWN` for a valid stored record, and never show `COMPLETE` for a view-only receipt.
3. Cover at least Achievement Diaries, Slayer Kill Log, Boss Kill Log, Combat Achievements and Collection Log observation completeness. Reuse already-working quest and bank snapshots. Avoid arbitrary hidden scan caps and report partial pagination explicitly.
4. Provide explicit user refresh controls and a seven-day stale-data reminder; account freshness must reflect changes to the underlying evidence, not just checkbox clicks.

### Do NOT change
- Bank R05 freeze, O tab, BankTags, ownership/variants, historic vs live withdrawability or bank controller.
- HoverLite, Chronicle UI, boss artwork, popup focus, Outfitter layout, typography.
- Pathfinder priorities/algorithms, gear research/method selection and Sessions; separate waves after capture passes.

### Verification
Add focused test fixtures exercising real selected widget/config decode logic, per-account persistence, refresh/stale transitions, save/restart and no false zero/complete. Explain unsupported native reads precisely. JUnit pass does NOT equal native verification.

**At most three owner actions:** (1) build/launch, (2) open diaries + Slayer Kill Log and inspect saved values, (3) restart/reopen Capture and verify persistence. Other collections may need independent QA without increasing owner test list.

### Handback FERAL-VERIFY-001
TICKET; STATUS (CODED/BUILT/FUNCTIONALLY VERIFIED/OWNER ACCEPTED/BLOCKED); WHAT CHANGED (exact paths); WHAT PLAYER CAN NOW DO; EVIDENCE (test outcomes, native proof or `NOT NATIVE VERIFIED`); OWNER TEST STEPS (max 3); STILL BROKEN; NEXT ACTION. Provide a verifiable GitHub commit/branch or complete downloadable source. Only owner can mark OWNER ACCEPTED/COMPLETE.

**First deliverable:** One implemented, persistently verified account-progress capture lane (prefer Slayer Kill Log + Achievement Diaries), with proof that real observed values are saved across restart. Stop for QA before expanding to Pathfinder.
