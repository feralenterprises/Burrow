# Unified TrashPanda Tracker QA source: exact GitHub recovery

**Source:** `unified-qa-candidate-only.zip` recovered byte-for-byte from the Cursor code viewer HTML attachment on 2026-11-03.

- **Archive SHA-256:** `03abae8cd96356e4f255e66716e4576c59d515e19306adce151c2e68b14132d5`
- **Archive length:** 1,778,127 bytes
- **ZIP integrity:** PASS (no CRC errors)
- **Archive entries:** 526 total, comprising 465 files and 61 directories
- **Source Java files:** 380
- **Binary blobs:** 23 PNG images and one Gradle wrapper JAR
- **GitHub verification:** All **465/465** recovered file paths match the expected Git blob SHA-1 computed locally from their exact bytes, with no mismatches.
- **Committed location:** `qa-integration/generated/unified-qa-candidate/` plus the original QA launch/test scripts and reports directly under `qa-integration/`.
- **Original Cursor local Git tip:** `d4cb5ac0cd1c7542ccf11dbf3c722dbc8f31b63f`. **Not restored** as original Git ancestry; the user supplied the lean ZIP, not the Git bundle.
- **QA results:** Cursor reports Java 17 compile PASS and **681/681 offline tests PASS**. These tests were not rerun by ChatGPT or on the owner's Windows RuneLite client.
- **Release gate:** RELEASE HOLD. No Plugin Hub publication; no changes to `main`; no live account/bank interactions claimed.
- **External source gaps:** Later owner-tested R08 ZIP comparisons have not yet been reconciled into this candidate.

## Cursor follow-up

Pull `qa/tpt-integration-20261103` and use `qa-integration/BUILD-AND-TEST-QA.cmd` or `BUILD-AND-LAUNCH-QA.cmd`. Do not run `00-PREPARE-QA-SOURCES.cmd` over the tracked unified source; it's already present. This branch contains a **new source-import commit**, not the original Cursor cloud local commit chain, so the two histories may diverge. Reconcile intentionally, never force-push.

The `qa-integration/BUILD-AND-TEST-QA.cmd` embedded message mentioning 674/681 is a stale log message from the original transfer; the reported result after the seven fixes is 681/681. Treat results as Cursor-reported until independently rerun.
