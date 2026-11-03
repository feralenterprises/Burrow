# Wave 9 Patch Reconstruction Discrepancy (verified 2026-11-03)

The original W09 handoff claims that `W09-CUMULATIVE-AGAINST-W0751.patch` applies to clean untouched W0751 source. In the newly independent package reconstruction that `git apply --check` **FAILED**: files `core/PathfinderGuidedCapture.java` and `ui/BankMemoryPanel.java` are absent, and the `BurrowPlugin.java` hunk does not match.

The following two-step sequence **PASSED**, independently of prior scratch trees:

1. Unzip exactly `TPT-W0751-RUNELITE-135-ONE-CLICK-DUAL-DEV-FIX.zip` (SHA256 pinned in SOURCE-LINEAGE).
2. Apply `W08-CUMULATIVE-AGAINST-W0751.patch` from its original W08 archive. Git apply check and actual apply PASS.
3. Apply `W09-INCREMENTAL-AFTER-W08.patch` from its original W09 archive. Git apply check and actual apply PASS.
4. Package the resulting isolated full-host source, with original files and new source classes, as `02-PINNED-SOURCE/W09-ISOLATED-CANDIDATE-SOURCE-RECONSTRUCTED-NOT-PRODUCTION.zip`.

**Do not use the misleading W09 'cumulative' patch as a one-step patch.** The two-step result is the verified reconstruction method in this handoff. The original W09 archive is preserved unchanged for audit and possible independent correction, not rewritten.

This verification checks patch application only; it does **not** establish native Java17 Gradle or RuneLite acceptance.