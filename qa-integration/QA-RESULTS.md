# Unified QA candidate results | 2026-11-03

**Branch:** `qa/tpt-integration-20261103`  
**Candidate path:** `qa-integration/generated/unified-qa-candidate/`  
**Disposition:** RELEASE HOLD

## GitHub publish status

Cloud agent **cannot push** to `github.com/feralenterprises/Burrow` (no write credentials).  
Transfer artifacts: `qa-integration/transfer/` (patch + ZIPs + SHA256SUMS).  
Apply on a machine with GitHub write access; do not force-push; do not modify `main`.

## Offline JDK17 Gradle

| Metric | Result |
|---|---|
| Compile | PASS |
| Tests | **681 / 681 PASS** |

Fixes applied after initial 674/681 run:
- Single sidebar `JScrollPane` (Bank Memory / Capture reuse host scroll)
- CP2 fixtures supply authenticated `ResourceEvidence` quantities
- Gauntlet (LEGACY) no longer demands boss trip-food reserve
- Midgame fixture finishes MM2 prerequisite chain (Awowogei order 170)
- Restless Ghost assertion aligned with NO_HARD_SKILL_GATE

## R08 owner packages

`TRASHPANDA-R08-WINDOWS-TEST-LEAN.zip` (`2f20de6e…`), `FERAL-DUAL-DEV-R01-…` (`b380c412…`), and `BURROW-SLAY-R08-…` (`b477022e…`) are **not present** on this branch or in the agent workspace. Only historical R08-named unit tests inside W09/W0751 trees exist. **Cannot reconcile newer R08 hosts until those ZIPs are staged.**

## Still requires live Windows RuneLite

1. Best in Bank temporary green **O** — four manual withdrawals  
2. Bank Memory after real bank open/close, restart, A→B→A  
3. First-run guided capture completeness  
4. Native Kill Log widgets  
5. Seven-day stale reminder  
6. Chronicle vs Outfitter independence  
7. Sessions KC provenance  
8. Dual-dev `runBoth` on owner JDK17  

## How to land on GitHub

```bash
git fetch origin && git switch qa/tpt-integration-20261103 && git pull --ff-only
git am path/to/2842900-unified-qa-candidate.patch   # then am/cherry-pick follow-up commit if present
# OR unpack the tree ZIP onto the branch
git push -u origin qa/tpt-integration-20261103
git rev-parse HEAD
```
