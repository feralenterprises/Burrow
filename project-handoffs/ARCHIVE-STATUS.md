# Master package GitHub recovery status

**Branch:** `ui/tpt-shell-r10c-cursor-20261103`
**Status:** Both original master packages are now **fully reconstructable** from 36 GitHub-tracked Base64 part files in `project-handoffs/archive-recovery/parts/`. Their original ZIP bytes are not directly committed as two standalone GitHub ZIP blobs. Use `project-handoffs/archive-recovery/00-RESTORE-ORIGINAL-ARCHIVES.cmd` on Windows, or run the companion PowerShell script, to rebuild and verify them.

| Original archive | Original size | Expected SHA-256 | GitHub part files |
|---|---:|---|---:|
| `TPT_COMPLETE_INTELLIGENCE_HANDOFF_R03_2026-11-03.zip` | 7,073,780 | `708c5a26d902426af9edc9c65339fb49711a53f37d607ae8b6f06641cd11eaad` | 15 / 15 |
| `TPT-PATHFINDER-AI-SELF-READ-MASTER-R01-2026-11-03.zip` | 10,111,186 | `94d135bd88db436c0f952df4683786deb09aff75737be15a6bd6ddbb44bcebac` | 21 / 21 |

**Verification performed:** Git blob SHA-1 IDs for all 36 parts match locally verified Library source part files. Locally recombining the source parts reproduces the SHA-256 values above. The original ZIPs pass archive integrity tests. No native RuneLite regression test or integrated engine merge is implied.

**Not guaranteed:** Every Project Burrow historical chat, independent research pack, newer owner Windows build, or current working source is included. Comprehensive project-wide archival coverage requires a separate file-to-source inventory.

**Policy:** Historical R31 is immutable. Pathfinder isolated candidates and UI shell must not overwrite working code by default. `main` remains unchanged pending owner-approved separately tested merges.
