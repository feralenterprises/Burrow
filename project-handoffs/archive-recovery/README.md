# TrashPanda Tracker: original archive recovery for Cursor

**Branch:** `ui/tpt-shell-r10c-cursor-20261103`
**Storage:** Complete source ZIP bytes are recoverable from 36 GitHub-tracked Base64-encoded parts in `parts/`. Original ZIP files are **not individually committed** as standalone ZIP blobs.

## Restore after cloning in Cursor

On Windows double-click `00-RESTORE-ORIGINAL-ARCHIVES.cmd` in this directory, or run:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\project-handoffs\archive-recovery\RESTORE-ORIGINAL-ARCHIVES.ps1
```

Both original ZIPs are reconstructed into `project-handoffs/archive-recovery/restored-original-zips/`. The restore script verifies the exact byte size and SHA-256 checksums, and throws on any mismatch. It does not push, merge, or change `main`.

| Original package | Parts | Bytes | SHA-256 |
|---|---:|---:|---|
| `TPT_COMPLETE_INTELLIGENCE_HANDOFF_R03_2026-11-03.zip` | 15 | 7,073,780 | `708c5a26d902426af9edc9c65339fb49711a53f37d607ae8b6f06641cd11eaad` |
| `TPT-PATHFINDER-AI-SELF-READ-MASTER-R01-2026-11-03.zip` | 21 | 10,111,186 | `94d135bd88db436c0f952df4683786deb09aff75737be15a6bd6ddbb44bcebac` |

## Integrity proof and release boundary

All 36 GitHub blob IDs match the IDs for the corresponding original Library transfer parts. The locally decoded parts reconstruct original ZIPs matching the SHA-256 values above, and all 92 entries across the two archives pass ZIP integrity checking.

This confirms **archival completeness of these two packages**, not completeness of every Project Burrow revision, research ticket or latest working host. These packages contain historical snapshots and isolated candidate code. Do not wholesale merge with `main`, treat the standalone Swing UI shell as the integrated RuneLite panel, or claim first-run baseline/Bank Memory native acceptance merely because files exist.
