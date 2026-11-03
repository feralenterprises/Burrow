# CURSOR | TRASHPANDA TRACKER UNIFIED QA BUILD | INTELLIGENCE + UI

**Repository:** `feralenterprises/Burrow`
**Working branch:** `qa/tpt-integration-20261103`
**Scope:** Implement a NEW runnable RuneLite QA candidate by combining account/gear/Pathfinder intelligence with the approved R10C 225px sidebar. DO NOT publish or modify `main`.

## First do the actual source intake

1. `git fetch origin && git switch qa/tpt-integration-20261103 && git pull --ff-only`.
2. Run `project-handoffs/archive-recovery/01-RESTORE-AND-EXPAND-WINDOWS.cmd` to reconstruct exact historical source.
3. Run `qa-integration/00-PREPARE-QA-SOURCES.cmd`. Read `qa-integration/MERGE-SOURCE-INVENTORY.md` and `MERGE-GATES.md`.
4. Restore/verify original R32 using `project-handoffs/research-r32-staging/archive/RESTORE-R32.ps1`, but keep R32 research staging only.
5. Inspect all available versions, in particular newer `TRASHPANDA-R08` owner Windows QA and `FERAL-DUAL-DEV-R01` if their exact original ZIPs are staged. Verify SHA-256 and actual code tree before selecting a host. If missing, call out precisely and continue only in an isolated merge candidate. Never default to ancient root `Project-Burrow/` just because it has build.gradle.

## Merge goal (implement, do not merely summarize)

Produce an integrated, compiling **Java 17 RuneLite** QA build, retaining live Best-in-Bank O commander and four-withdrawal lifecycle. Port approved UI from `ui-shell/r10c` into the actual RuneLite plugin sidebar at **225 px** (not a separate standalone demo). Maintain exactly four top tabs Home / Pathfinder / Outfitter / Sessions; Chronicle navigation and selection must not mutate Outfitter, authentic OSRS icons, approved lean bronze-armored red-scarf raccoon, 4×7 inventory and truthful stateful controls. No rejected chunky mascot.

Reconcile Pathfinder W0751/W09 candidate against Cursor Wave4 per method/contract with deterministic fixtures. Never overwrite one engine with another wholesale. Outfitter must recognize gear variants, charge/ornament states, mechanic-specific protection, tiers and owned-state evidence. Preserve safe Bank O behavior and add saved per-account historical Bank Memory with quantities, variants, scan timestamp/freshness and no fabricated live bank contents.

Implement/verify account-specific first-run progression baseline, guided click-through capture for unavailable menus, manual refresh, approximately 7-day stale reminder, truthful coverage/unknowns; Collection Log, quest/diary/CA, boss and Slayer log evidence with exact KC provenance. Preserve per-account isolation through logout/world hop/account switch. Sessions and Pathfinder must use evidence, not assumptions.

## Required output, tests, and boundaries

- A **new isolated integrated source folder** (`qa-integration/generated/unified-qa-candidate/`), source manifest, exact upstream commit/file hashes, per-subsystem diffs, and executable Windows `BUILD-AND-LAUNCH-QA.cmd` and `BUILD-AND-TEST-QA.cmd`. No user accounts committed.
- Compile with JDK17 and run focused tests for UI/Pathfinder/BIB Bank O/KC/account memory. Keep logs targeted to failing interactions.
- Present exact test steps and expected results for native Windows RuneLite QA. Explicitly distinguish compile PASS, synthetic test PASS, and real owner live test PASS.
- Source decisions must be tracked in `qa-integration/QA-MERGE-DECISIONS.md`. Mark unavailable original source as a blocker; never invent test evidence.
- No auto banking, gameplay manipulation, Plugin Hub publication, or merge to `main`. Keep R31 immutable and R32 staging until independent QA permits promotion.
- After implementation, commit only reconciled QA candidate, its tests and docs **to this QA branch**, then provide results-first report with commit SHA, diffs, unresolved blockers, and one-step Windows launch.

**Start source inspection and implement now.** Do not stop after writing an audit or ask for additional permission to edit this QA branch.
