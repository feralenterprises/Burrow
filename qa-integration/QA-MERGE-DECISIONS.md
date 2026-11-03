# QA merge decisions | unified-qa-candidate

**Branch:** `qa/tpt-integration-20261103`  
**Disposition:** RELEASE HOLD · isolated QA candidate · not `main` · not Plugin Hub

## Host selection

| Decision | Choice | Exact identity | Rationale |
|---|---|---|---|
| Integration host | Pathfinder W09 reconstructed candidate | ZIP SHA `3e696901cc14d04a04c58473410f8548fcb47485f63baf596ba41d54e3c3ec13` | Newest verified full-host lineage (W0751 + W08 cumulative + W09 incremental) that already contains Bank Memory, Guided Capture, Account Archive, Kill Log readers, BiB/O freeze gate, Outfitter, Sessions |
| Host ancestor | W0751 | `79e7d54a78a0bb76b6b3eca0e6804067b02b40a35e88339ee83a20408aa7ef3f` | Pinned full RuneLite 1.13.5 dual-dev baseline |
| Bank O freeze reference | B38 gate byte-identical | `BankWithdrawalFreezeGate.java` SHA `dd54167d…` matches B38 | Preserve owner-witnessed temporary O / four-withdrawal semantics |
| UI visual contracts | R10C assets + host panel | `ui-shell/r10c` approved raccoon copied into plugin resources; `BurrowPanel.WIDTH=225`; four `BurrowModel.View` tabs | Shell is not swapped wholesale; host `SidebarHost` already mounts Bank Memory / Capture as subordinates |
| Pathfinder engine | Keep W09 host `SuggestionEngine` | Do **not** overwrite with Cursor Wave4 | Documented 112/288 top / 48/288 readyNow divergence; Wave4 retained under `generated/cursor-wave4` for comparison only |
| Research | R31 immutable; R32 staging | R31 inside gear-research compendium; R32 SHA `36176521201338156334898582ee12077baa929fe42bf8c3846ef327440b1d0a` restored | No automatic catalog promotion |
| Newer owner builds | **Unavailable on GitHub** | `TRASHPANDA-R08`, `FERAL-DUAL-DEV-R01`, `SLAY-R08` listed in inventory but not staged on this branch | Explicit blocker for “latest owner-tested” pin; continue with W09 as isolated candidate |

## Code changes in this candidate (beyond W09 baseline)

1. Added `approved-armored-raccoon.png` from R10C into `src/main/resources/gg/feral/burrow/ui/`.
2. `Sprites.java` prefers the R10C approved raccoon, falls back to historical `armored-raccoon-r05.png`.
3. Added `R10cUnifiedUiContractJUnitTest` (225px, four tabs, raccoon resource, 28 inventory slots, subordinate Bank/Capture controls).
4. Added Windows `BUILD-AND-TEST-QA.cmd` and `BUILD-AND-LAUNCH-QA.cmd`.
5. Lineage stamp `_UNIFIED_QA_LINEAGE.txt`.
6. **Compile fix (required):** pristine W09 `BurrowPlugin` does not compile on JDK17 — `SessionScope.loginEpoch()` is a `String` token but W09 Kill Log / Guided Capture call sites treated it as `long`. Added `sessionLoginFence` + `captureLoginEpoch(SessionScope)` and wired the six capture call sites. String login tokens remain for Sessions/experience/iron XP baselines.

## Offline JDK17 Gradle result (this environment)

- **Compile:** PASS
- **Tests:** **681 / 681 PASS** (after SidebarHost single-scroll, CP2 ResourceEvidence fixtures, Gauntlet food-gate scope, midgame MM2 prereq finish, Restless Ghost NO_HARD_SKILL_GATE assertion).
- **R08 packages:** still missing from GitHub/workspace; not reconciled.
- **GitHub push:** blocked in cloud agent; see `qa-integration/transfer/`.

## Explicit non-merges

- Cursor Wave4 Pathfinder tree not copied into host.
- Standalone Swing `TrashPandaShell` not used as RuneLite panel.
- R32 research rows not imported into Outfitter catalogs.
- Root repo `Project-Burrow/` (15-file stub) untouched.
- `main` untouched.

## Still requires live Windows RuneLite owner QA

Bank O four withdrawals on this tree; account A→B→A fences; first-run guided capture completeness; native Kill Log widgets; 7-day reminder timing; ConfigManager persistence after restart; Chronicle vs Outfitter independence in the real client; Sessions KC provenance.
