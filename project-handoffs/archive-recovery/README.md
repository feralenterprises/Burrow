# TrashPanda Tracker: original master archives for Cursor

**Branch:** `ui/tpt-shell-r10c-cursor-20261103`
**Verified on GitHub:** The original Intelligence R03 and Pathfinder R01 ZIPs are stored as **36 Base64 parts** under `parts/`. They are reconstructable byte-for-byte with SHA-256 checks, but **not** committed as standalone ZIP blobs.

## One-click full file-tree recovery (Windows)

1. Pull the Cursor branch and open the repository root.
2. Run `project-handoffs/archive-recovery/01-RESTORE-AND-EXPAND-WINDOWS.cmd`.
3. Use the full recovered file trees under `project-handoffs/archive-recovery/restored-full/intelligence-r03/` and `.../pathfinder-r01/`.

This first runs the original archive reassembly script, checks both original SHA-256 values, extracts into isolated folders, and verifies each extracted file against its exact ZIP entry (name, bytes, and decompressed SHA-256). Both outer ZIPs contain **92 files and 27 intact nested ZIPs**. It will **never overwrite** the partial Git-tracked `extracted/` folders, host code, or `main`; generated folders are ignored by Git.

The existing `project-handoffs/intelligence-r03/extracted/` and `project-handoffs/pathfinder-self-read-r01/extracted/` are **curated, incomplete text-only previews**, not source-of-truth full extractions. Do **not** report missing binaries in those previews as corrupted or lost archival ZIP bytes. Restore from the verified ZIPs instead. Nested ZIP archives are preserved as binary files and can be separately inspected as needed. Do not flatten competing historical source trees into one production source.

If you only need the original ZIP files, use `00-RESTORE-ORIGINAL-ARCHIVES.cmd`, which reconstructs them under `restored-original-zips/`.

## Expected archives

| Original ZIP | Parts | Bytes | SHA-256 | Files / nested ZIPs |
|---|---:|---:|---|---:|
| `TPT_COMPLETE_INTELLIGENCE_HANDOFF_R03_2026-11-03.zip` | 15 | 7,073,780 | `708c5a26d902426af9edc9c65339fb49711a53f37d607ae8b6f06641cd11eaad` | 53 / 13 |
| `TPT-PATHFINDER-AI-SELF-READ-MASTER-R01-2026-11-03.zip` | 21 | 10,111,186 | `94d135bd88db436c0f952df4683786deb09aff75737be15a6bd6ddbb44bcebac` | 39 / 14 |

The archives and source parts were checksum-tested when uploaded. The Windows expand script and verification steps require execution in Cursor's Windows environment; GitHub hosting alone is not a Windows run.

## R32 and release boundaries

The original `BURROW-RESEARCH-DISPATCH-R32.zip` is **not present** in this repository or the inspected Research Dispatch Library folder. R32-associated QA notes are separate staging evidence, not a substitute for the original ZIP. See `../research-r32-staging/R32-STATUS.md`.

These packages contain historical snapshots, QA candidates, and separate branches. No production RuneLite integration, account baseline capture, R32 promotion, or native regression test is implied. Never merge to `main` or rewrite immutable R31 without owner authorization.
