# TPT-INTEGRATION-001: exact input archives for Cursor (2026-11-03)

**CURRENT BLOCKER RESOLVED IN GITHUB:** All three packages from the owner's ChatGPT upload are stored on this isolated GitHub handoff branch as Git-managed binary parts. All part Git blob hashes were checked against the exact uploaded bytes. Run the provided reconstruction script to recreate the original ZIP bytes and verify SHA-256.

**DO NOT assume the older repo root Project-Burrow folder is R02.**
The baseline R02 handoff is inherited in this branch at `owner-handoffs/R02-2026-11-03/`. Reconstruct it using its own script. R02 was owner-tested in Windows RuneLite and reported working banking, but its newer fixes must not be overwritten by the older UI ZIP.

## Recover all 3 ZIPs (Cursor cloud computer, Python 3)

From the checked-out repository root:

```bash
python3 owner-handoffs/TPT-INTEGRATION-001/reassemble_inputs.py
python3 owner-handoffs/R02-2026-11-03/reassemble_r02.py
```

The first script recreates:
- `recovered/TPT-INTEL-001-source.zip` (1,726,128 bytes, SHA-256 `c358be1f9715d8990cec1b468b03daa7e233da9de717b200e8d0208ec70db436`)
- `recovered/TPT-OUTFITTER-UI-SIMPLIFY-AUDIT.zip` (6,123,067 bytes, SHA-256 `91efc7f0dbb1416dfb330021fb0e564ef25f01b17078f48d53964614bf4f397b`)
- `recovered/TPT_Boss_Icons_ALL.zip` (5,670,736 bytes, SHA-256 `4da2d7c68145fe5f12b408c4db9abd339c3f294c7046c99a584950e04a2ba41d`)

The owner's artwork upload was named `TPT_Boss_Icons_ALL(1).zip`; the reconstructed name drops the (1), **without modifying the archive bytes**. `manifest.json` records exact part Git object hashes and archive checksums.

The second script recreates the baseline R02 source under `owner-handoffs/R02-2026-11-03/R02-source-extracted/`. Do not modify the handoff archive or inherited old source. Create a NEW isolated implementation branch from this handoff and selectively apply source updates to the verified R02 tree.

## Cursor responsibilities

- Continue **TPT-INTEGRATION-001**, status currently BLOCKED until archives were recovered; now resume after verifying all four SHA-256 hashes.
- Incorporate Intelligence capture fixes; selectively integrate UI changes; preserve working R02 Bank R05 safeguards, BankTags/O, read-only historical bank evidence, HoverLite independence and root-level Windows build/launch scripts.
- Keep Melee/Ranged/Magic combat style, with just two player-facing gear-source choices Wiki Recommended and Best in Bank. No Entry/Recommended/Optimal tier controls.
- Integrate supplied boss art correctly across Chronicle browser, Chronicle boss details and Outfitter banner. Audit mappings, not just file count.
- Chronicle detail Back to browser, not Outfitter. Boss search multi-character input. Exactly four main tabs. Preserve equipment and 4x7 inventory geometry.
- Do not overwrite existing shared assets based only on filenames; compare identities and source hashes.
- Deliver full source, exact modified-file list, build evidence, native verification status, <=3 owner tests and remaining defects under FERAL-VERIFY-001. No merge to main, no release or publication.
- If still blocked, give precise failure evidence and stop rather than silently substituting older code.

No functionality has been changed by this handoff. It is **source transfer only**.
