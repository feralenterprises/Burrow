# P0 handback (open) | first-run + Bank Memory

**P0 disposition:** OPEN — partial acceptance addressed; not closed.  
**GitHub QA tip (visible):** `d77c55b9459b269841d2b29aba1a9a8f67c586f8`  
**Local work branch:** `cursor/p0-account-bank-memory-f628`  
**Exact local HEAD:** `b2d95acdc1bf3d64879c2b0c4d71bb02623d3ef8`  
**Parent:** `d77c55b` → `90d5bf7` (intel/stale) → `b2d95ac` (historical BiB planning)  
**Offline tests:** **689 / 689 PASS**  
**BiB freeze gate SHA:** `dd54167d46032823c45bc4287dfa702a9de0d3bdee84031e5c5813557c8a8cef` (unchanged)  
**main:** untouched (`be91c675…`)  
**GitHub push from this agent:** still blocked (no write credentials)

## Transfer (independent verification)

| Artifact | Purpose |
|---|---|
| `unified-qa-candidate-p0-source.zip` | Complete candidate tree + docs (no `build/`) |
| `p0-from-d77c55b-complete.patch` | `git am` onto `d77c55b` restores both P0 commits |
| `P0-COMMIT-SHA.txt` | `b2d95acdc1bf3d64879c2b0c4d71bb02623d3ef8` |

```bash
git fetch origin && git switch qa/tpt-integration-20261103
git am p0-from-d77c55b-complete.patch
cd qa-integration/generated/unified-qa-candidate/Project-Burrow && ./gradlew test
# expect 689/689
git push -u origin HEAD:cursor/p0-account-bank-memory-f628
# or merge into qa/tpt-integration-20261103 then push (no force, no main)
```

## WORKING

- Four tabs, 225px sidebar, approved raccoon, HoverLite independent
- Guided capture + per-account archive (skills/quests/bank)
- **Historical Bank Memory → closed-bank BiB read-only planning** (`BankEvidence.HISTORICAL_ARCHIVE`)
- Trust line shows timestamp/staleness + “NOT currently withdrawable”
- O tag / freeze gate still require live open bank
- Pathfinder current stock still rejects closed/historical (`PathfinderAccountTruth`)
- Per-section coverage labels: Collection Log partial native rows; diaries/CA player-review only; `provesComplete=false`

## FIXED (this continuation)

| Change | Behavior |
|---|---|
| `BankEvidence.fromHistoricalBankMemory` | Closed planning evidence from archive |
| `BurrowPlugin.hydrateHistoricalBankPlanningIfNeeded` | Loads archive into `BestBankPreloader` when bank closed / no live evidence |
| Capture checklist/report | Exact pages, evidence rows, coverage labels, complete=false |
| `BANK-OBSERVATION-CACHE-RESOLUTION.md` | BOC **not** wired; production path documented as replacement |
| +2 tests | Historical plans ≠ O authorize; diaries/CA/clog coverage |

## BLOCKED (P0 remains open)

1. **Native diary / Combat Achievement widget readers** — still guided manual only  
2. **R08 owner ZIPs still missing** — cannot reconcile (`QA-R08-RECON.md`)  
3. **GitHub branch not published from this agent** — needs owner push of transfer  
4. **Independent GitHub Actions CI** — blocked until branch is on GitHub  
5. **Live Windows RuneLite acceptance** — not run here  
6. Inv/equip still not in durable bank memory (bank map only)

## VERIFIED

```
./gradlew test → 689/689 PASS
BankWithdrawalFreezeGate.java SHA-256 = dd54167d…
BestBankLayoutGate rejects historical (bankStillOpen=false)
```

## WINDOWS QA (targeted)

1. Save bank memory on A → restart → Outfitter BiB plan with bank **closed** → status shows HISTORICAL BANK MEMORY / NOT withdrawable  
2. Attempt O tag with bank closed → must fail; open bank → O ×4 manual withdrawals still work  
3. Capture panel: Collection Log shows PARTIAL NATIVE + row counts; diaries/CA show PLAYER REVIEW ONLY; never COMPLETE  
4. A→B→A isolation unchanged  
5. After owner pushes branch: confirm Actions 689/689 on Windows JDK17  
