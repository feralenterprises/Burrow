# TrashPanda Tracker | Cursor workspace

**Branch:** `ui/tpt-shell-r10c-cursor-20261103`  
**Disposition:** RELEASE HOLD. **Do not merge into main or publish to RuneLite without owner approval and native QA.**

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
- `project-handoffs/intelligence-r03/`: TPT combined context and source/readiness notes, plus the readable files extracted from the Intelligence Handoff R03 package.
- `project-handoffs/pathfinder-self-read-r01/`: Pathfinder Waves 1–9 owner reports, issue index, W08/W09 patch evidence, source lineage, and the readable files from the AI Self-Read Master package.
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

## Two original archives not yet published

The two complete binary ZIPs could not be uploaded through the connected GitHub action. Their **readable top-level contents are present** under `extracted/`, but nested binaries and original archive bytes are **not** in this branch.

See `project-handoffs/ARCHIVE-STATUS.md` for exact names and SHA-256 digests. If full archival fidelity is required, upload those two original ZIPs on this **same branch** using GitHub's file-upload interface or a normal git push from the machine where you downloaded them.

Never equate this GitHub tree with a successful native RuneLite integration, complete project-history migration, or source promotion.
