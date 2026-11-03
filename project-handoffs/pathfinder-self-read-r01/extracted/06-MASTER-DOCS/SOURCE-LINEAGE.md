# Source-of-truth and patch lineage

**Owner intent:** latest direct requirement. **Code truth:** exact byte-verified checkout or source ZIP and diffs. **Offline proof:** explicit tests only. **Runtime truth:** native RuneLite traces. **Research:** supportive, not merged automatically.

## Exact source identities

- **Full-host W0751**: `TPT-W0751-RUNELITE-135-ONE-CLICK-DUAL-DEV-FIX.zip`; **SHA256 `79e7d54a78a0bb76b6b3eca0e6804067b02b40a35e88339ee83a20408aa7ef3f`**; 441 ZIP entries, release hold. W0751 is a test-contract repair relative to W075, not proof of gameplay acceptance.
- **PQA-W09 cumulative patch**: `W09-CUMULATIVE-AGAINST-W0751.patch` **SHA256 `03df17575aeaec672a128df6a181a72dbb6cbd30b64f7a88bbbc2033b403b357`**. **WARNING: its claimed direct clean application FAILED in this independent audit. Do not use as a one-step patch.**
- **W08 cumulative patch**: `W08-CUMULATIVE-AGAINST-W0751.patch` SHA256 **`da3cf5258f3d4818eabd6cfc9ab963a8f8d1dac22a3cb116b80e8351664df66a`**. Applies clean to original W0751.
- **PQA-W09 incremental patch**: inside W09 original ZIP `W09-INCREMENTAL-AFTER-W08.patch` **SHA256 `3c3b103f59ff29bb153b0ab95b0267c9bb0bfc5c409386dc096dfed0bbc00096`**. Apply only after the correct W08 isolated candidate state.
- **PQA-W09 reconstructed candidate**: `02-PINNED-SOURCE/W09-ISOLATED-CANDIDATE-SOURCE-RECONSTRUCTED-NOT-PRODUCTION.zip`, produced by two independently successful git applications: **W08 cumulative** to pristine W0751, then **W09 incremental**; read the receipt for the exact new ZIP SHA. This is QA source, **not installed code**.
- **Cursor standalone Pathfinder Wave4**: `pathfinder_wave4_complete.zip` SHA256 **`ae019db6888b5870770a0c4458ef6b12089718977d4dcc92ea262d22d42570be`**. Reported baseline `17957600a0789c21a80aa20d78e14ee0068afb98` belongs to separate branch and is not pinned to full host.
- **Cursor master context R02**: `TPT_CURSOR_MASTER_PACK_R02_2026-11-03.zip` SHA256 **`b515e58e3f3457d10b5dd65740e06b13a90f9ff0062e80cddfd63488a3ca791e`**. Includes historical W075 reference source and owner requirements; not live build.
- **GitHub**: `feralenterprises/Burrow` public main previously pointed to `be91c6754bada3483bed82b5f3699286c2b8dbba` (archived Build #38), as observed at W01. **Re-check live branch/commit at next use.** Do not assume this is latest now, and do not write GitHub without owner approval.

## Cross-wave source posture

W01/W02 perform read-only source-connected simulation on original W0751. W03 is first isolated engine repair candidate. W04/W05 add account evidence, kit/budget and fairy-ring stage changes to cumulative candidate. W06 adds durable first-run/bank snapshots. W07 adds guided capture and a Bank Memory view. W08 mounts their subordinate view in the RuneLite host candidate. W09 adds source-connected read-only Kill Log widget capture and tests, without changing ranking. The **W09 patch labeled cumulative unexpectedly fails against pristine W0751**. To reproduce this exact isolated snapshot, apply W08 cumulative to pristine W0751, then W09 incremental. See `05-PATCHES-AND-REPRO/W09-PATCH-RECONSTRUCTION-DISCREPANCY.md`. Do not stack two cumulative patches or use the mislabeled one as a single-step install.

## Invalid conclusions to avoid

- No W09 release acceptance because a patched ZIP exists.
- No native execution proof from Java `--release 17` on JDK21 and stubbed RuneLite classes.
- No proof of an exact zero for an unseen log, bank slot, KC or diary.
- No proof the temporary bank O preparation tab equals the durable Bank Memory UI.
- No proof that a Cursor `SuggestionEngine` class is drop-in replacement for another branch's `SuggestionEngine`.
- No interpretation of missing progress-event evidence as user choosing to ignore advice.

## Safe process

Inspect active repo first, record its SHA and uncommitted changes, make a reversible scratch branch/copy, run exact regression fixtures, inspect diff, test the actual Windows RuneLite session, record user-visible failures and logs, then seek independent QA/owner acceptance. Build without auto publishing or touching game actions. The source handoff is not permission to mutate canonical ticket registers or original research saves.