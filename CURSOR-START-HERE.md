# TrashPanda Tracker | Cursor workspace

**Branch:** `ui/tpt-shell-r10c-cursor-20261103`  
**Disposition:** RELEASE HOLD. **Do not merge into main or publish to RuneLite without owner approval and native QA.**

## Complete original master archives on GitHub

The missing intelligence and Pathfinder master ZIPs have been preserved on this branch as **36 verified Base64 parts**. To restore **all 92 original outer ZIP entries** on Windows, run `project-handoffs/archive-recovery/01-RESTORE-AND-EXPAND-WINDOWS.cmd` (the old `00-...` script restores ZIPs only). See [archive recovery instructions](project-handoffs/archive-recovery/README.md) and [exact archive status](project-handoffs/ARCHIVE-STATUS.md). Archival recovery is **not** an engine merge or live RuneLite QA pass.

## Open directly in Cursor

```powershell
git clone https://github.com/feralenterprises/Burrow.git
cd Burrow
git switch ui/tpt-shell-r10c-cursor-20261103
```

If you already cloned Burrow:

```powershell
git fetch origin
git switch ui/tpt-shell-r10c-cursor-20261103
git pull --ff-only origin ui/tpt-shell-r10c-cursor-20261103
```

Open the **entire Burrow repository** in Cursor so the historical master handoffs and the UI shell are visible together.

## Where to look

- `ui-shell/r10c/`: editable standalone Java Swing 225px UI shell, Windows run/build scripts, approved/rejected-artwork separation, and source assets. **Not** a wired RuneLite plugin.
- `project-handoffs/intelligence-r03/`: TPT combined context and curated readable previews. **Not the complete source tree:** recover the original via `archive-recovery/restored-full/intelligence-r03/` after running the script.
- `project-handoffs/pathfinder-self-read-r01/`: Pathfinder Waves 1–9 owner reports, issue index, W08/W09 patch evidence, source lineage, and curated readable previews. Recover original binaries via `archive-recovery/restored-full/pathfinder-r01/`.
- Existing repository code from `main`: inherited historical Build #38 snapshot. **Do not assume it is the current canonical host** without a source-hash comparison against later builds.

## Read in order

1. `project-handoffs/intelligence-r03/TPT_ALL_CONTEXT_ONE_FILE_R03.md`
2. `project-handoffs/pathfinder-self-read-r01/TPT-PATHFINDER-SELF-READ-FIRST-2026-11-03.md`
3. `project-handoffs/pathfinder-self-read-r01/TPT-PATHFINDER-MASTER-HANDOFF-2026-11-03.md`
4. `project-handoffs/pathfinder-self-read-r01/extracted/06-MASTER-DOCS/SOURCE-LINEAGE.md`
5. `project-handoffs/pathfinder-self-read-r01/extracted/04-ISSUE-INDEX/ALL-WAVE-ISSUES.csv`
6. `ui-shell/r10c/README-CURSOR.md`

## Engineering safety

- No production changes from the standalone shell without an independent code/behavior review.
- Keep exactly four main tabs. Chronicle is a subview, independent of Outfitter selection.
- Original approved armored raccoon only. The chunky pixel raccoon was rejected.
- Full NPC model portraits for Scurrius, Sarachnis, Bryophyta are **not** verified/approved. Reference-crop comparison imagery is never a production substitute.
- Preserve real Best in Bank, green O bank commander, account-specific Bank Memory, and evidence provenance.
- **First-run account baseline capture, durable per-account memory, guided click-through sync, explicit refresh, seven-day stale notice, and per-account Bank Memory remain release-blocking criteria, not implemented just because they are described in handoffs.**
- Do not fabricate personal KC, drops, readiness, or bank item ownership.
- Pathfinder W09 source is an **isolated reconstructed QA candidate**, not proven production or accepted merged host.

## R32 research staging status

The full `BURROW-RESEARCH-DISPATCH-R32.zip` is **not recovered or uploaded**. See `project-handoffs/research-r32-staging/R32-STATUS.md` for available original R32 QA reports, pending blockers, and the reported checksum. Never call R32 complete from these documents.

## Archive fidelity

Both original master ZIPs are byte-for-byte reconstructable from the GitHub archive-recovery parts. Their original ZIP blobs are not directly committed; use the restoration script above. Do not equate archival presence with a completed historical migration, source promotion, or native RuneLite integration.
