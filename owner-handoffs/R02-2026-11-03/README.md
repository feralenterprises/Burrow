# TrashPanda Tracker: R02 owner-tested source handoff (2026-11-03)

**AUTHORITATIVE SOURCE IS IN THIS FOLDER, NOT IN REPOSITORY MAIN OR LEGACY QA PATHS.**

This handoff branch is a staging reference only, not an implementation branch or release.
The existing `Project-Burrow/` and `qa-integration/` trees inherited from the branch base are older and must NOT be treated as R02.

## Exact recovered R02 source

- Filename: `TPT-W04-CURSOR-RECOVERY-MERGE-R02-WINDOWS-SOURCE-OWNER-HOLD.zip`
- Byte size: **3,560,073**
- SHA-256: `adf1d64fdb3d39b003deadff4e6ae312c27667a3b12f3a251037f14018437653`
- Contains **461 files**, including complete Project-Burrow Gradle sources and original Windows launch scripts.
- Parts in `parts/part-01.bin` through `part-20.bin` are contiguous segments of the binary ZIP, NOT independent archives.
- Run `python owner-handoffs/R02-2026-11-03/reassemble_r02.py` from the repo root (Python 3). The script verifies the exact ZIP SHA-256 and extracts to a dedicated folder, without altering older source trees.
- Do not use the part files as Java source. Work from the reconstructed ZIP and its extracted `Project-Burrow/`.

## Grounded owner evidence (Windows native, November 3)

- R01 test run: 695 tests, one obsolete test expectation failed; no RuneLite launch.
- R02 owner Windows run: Gradle `BUILD SUCCESSFUL` after compilation of TPT and HoverLite; client launched and TPT Home, Chronicle and Outfitter were visible.
- Quest discovery eventually displayed **178/214 completed quests**.
- Bank Memory UI displayed **880 distinct item IDs** in a saved snapshot. This alone does not establish correct quantities or item variants.
- Best in Bank correctly indicated `HISTORICAL ... NOT CURRENTLY WITHDRAWABLE` when bank was closed.
- Owner said **bank part works**; the owner has NOT explicitly confirmed the number of successful manual withdrawals in the R02 run, so do not claim all four were retested.
- First-run Achievement Diaries, Combat Achievements, Boss Kill Log and Slayer Kill Log capture remained unknown/incomplete despite visiting relevant OSRS screens.
- R02 native status: **BUILT; partial native functionality observed; not OWNER ACCEPTED**.
- A later R03 archive contains a boss-picker focus attempt, not yet native accepted. **Do NOT use R03 for this engine wave.**

## Protection / scope

**Build #38** remains immutable known-working fallback (SHA-256 `711dd92392d63cde669d646e195610e2d0ca12a17683249874f9231e943ca0ec`). W04 was tested with bank withdrawals. Do not modify Bank R05 withdrawal freeze, BankTags/live-bank gate or HoverLite for first-run intelligence work.

Only four top-level tabs: Home, Pathfinder, Outfitter, Sessions. Do not redesign UI here.
No main merge, release or publication without owner approval.

Read `CURSOR-P0-DISPATCH.md` for the narrow first task and mandatory evidence handback.
