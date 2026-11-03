# 🦝 TrashPanda Tracker | PQA-W07 Independent Pathfinder QA
**Wave 7: Guided First-Run Capture & Historical Bank Memory UI · November 3, 2026**  
**Mode:** ISOLATED SOURCE CANDIDATE + DETERMINISTIC JAVA QA · **Release:** HOLD

## Owner-facing results first

**48 synthetic account profiles**; **48/48 witnessed Collection Log page round trips**; **48/48 manual-review records with zero invented KC or item observations**; **48/48 bank-memory as-of/age displays**; **314/314 new Java harness assertions passed** (291 main, 23 negative edges). All **24/24 frozen evidence files** across Waves 4, 5, 6 and 7 reproduced byte-for-byte from a fresh application of the cumulative patch. **Completed 100-, 500-, and 1,000-action progression journeys: 0**, not verified; engine ranking rules were unchanged by this wave.

### Five highest-impact findings

1. **First-run account data is still incomplete, P0.** The Wave 6 historical archive stores complete observed skills/quests and bank-only history in the isolated candidate. Wave 7 adds an authenticated, durable per-account journal, but only the *existing drawn Collection Log page reader* is connected as a native source. Boss kill log, Slayer kill log, diaries and combat achievements have **0/48 native section captures** in this harness because their host readers are not integrated. A manual screen visit is expressly *not* native data.
2. **Collection Log capture is correct but partial, P0 for complete baseline.** In **48/48** profiles a widget page was saved and reopened without account bleed, with exact positive obtained item IDs and only explicitly reported KC rows. It remains `PARTIALLY_WITNESSED` in **48/48**; none can declare all Collection Log pages witnessed. The existing RuneLite event is `onScriptPostFired` for `COLLECTION_DRAW_LIST`, and no other game UI is opened by the plugin.
3. **Bank Memory can be displayed without inventing ownership, but is not navigable in RuneLite yet, P0.** **48/48** modeled account views retain exact raw ID and quantity from the latest accepted Wave 6 bank scan and show `HISTORICAL`, not `LIVE`. The UI tests exercise a functioning Refresh callback (1/1). The candidate `BankMemoryPanel` is a standalone Swing component; its entry point and host callback are NOT wired into Burrow's four-tab sidebar. Item variants/charges lack independently reviewed labels in **48/48** fixtures.
4. **Provenance and corruption are fail-closed, candidate only.** **48/48** account fixtures reject wrong-account, old-epoch, logged-out, stale, future-timestamp, duplicate, checksum-tampered and cross-account-copied inputs (8 distinct fences per profile). The **23/23** edge tests also protect out-of-order pages, excessive payloads, count-zero semantics and an invalid bank timestamp. A malformed journal record is unavailable, not an empty success state. None of this substitutes for native Windows ConfigManager persistence or account-switch UI checks.
5. **The old Pathfinder quality defects remain, P0.** The engine's ranking and budgeting code was not edited. The Wave 6 **6,000 unchanged-state recommendation calls** still repeat; there are **zero verified long-run completed journeys**. Historical bank quantities cannot support live food, rune, GP or purchase planning. Goal convergence and 100/500/1000 completed activities still require a witnessed outcome feed.

## Exact source identity and differences

- **Pinned base:** `TPT-W0751-RUNELITE-135-ONE-CLICK-DUAL-DEV-FIX.zip`, SHA-256 `79e7d54a78a0bb76b6b3eca0e6804067b02b40a35e88339ee83a20408aa7ef3f`.
- The newer W08E package in Library is a standalone UI shell; it is not a newer engine source. GitHub B38 is older. This wave does not promote either as production authority.
- **Parent:** W06 cumulative isolated patch, not merged. **W07 incremental:** four files (one existing RuneLite host adapter + three new Java classes), **no edit** to `SuggestionEngine` or ranking/activity catalogs. W07 cumulative patch may be applied *alone* to a pristine W0751 extraction; do not stack W06/W07 cumulative patches.
- `core/PathfinderGuidedCapture.java`: per-account, current-login-fenced journal with positive-only page evidence, manual review, immutable/raw item IDs, guarded checksum persistence and an explicit `provesCompleteLog() == false` contract.
- `runelite/BurrowPlugin.java`, lines ~113 and ~224–254: connects only the existing authenticated Collection Log page event to the journal. There is no new script invocation, bank access or automated player action.
- `ui/BankMemoryViewModel.java`: historical scan timestamp, seven-day age, account-mismatch blanking, raw item IDs and unverified variant-state disclosure.
- `ui/BankMemoryPanel.java`: 225px-compatible standalone Swing view and a functional injected callback for a *requested* manual scan. No RuneLite sidebar hookup exists yet.

## Provenance, meaning and limitations

- **ACTUAL SOURCE:** the code paths and line references in `ISSUE-LEDGER.csv` are from the cumulative candidate applied to W0751.
- **OFFLINE EXECUTION:** Java 21 compiling with `javac --release 17`, headless Swing, and deterministic synthetic account fixtures. This is NOT a genuine Windows Java 17 Gradle or RuneLite acceptance run.
- **REAL GAME OBSERVATION:** none was performed in Wave 7. The host callback was source-inspected and added to the candidate, not exercised in the live RuneLite event bus.
- **MANUAL REVIEW:** a player can mark a screen reviewed, but it does not assert a completed diary, boss KC=0, collection-log ownership, full page coverage, or verified RuneLite data.
- **BANK MEMORY:** remains a historical bank-only snapshot. Its presence never proves items are still in the bank. No new bank scan occurs from the panel itself, and the O/Best in Bank withdrawal lease is untouched by the W07 host change.
- **SECURITY:** SHA-256 checksums flag accidental corruption, not privacy protection or encryption. Native ConfigManager limits and the on-disk confidentiality of local account history are unverified.

## Severity-ranked remaining engineering priorities

**P0-A:** Implement and validate bounded, authenticated readers for Ring of Wealth boss log, enchanted-gem Slayer log, diary tiers and combat-achievement completion. Any page/window coverage must be explicit; unknown values must not become zero. Coordinate the existing Collection Log listener instead of duplicating it.

**P0-B:** Wire a guided click-through first-run checklist to existing four-tab UI without a fifth navigation tab or auto-clicking game UI. Display `NOT OBSERVED / PARTIAL / REVIEWED BY PLAYER / WITNESSED` accurately. Never claim first-run COMPLETE when coverage is partial.

**P0-C:** Integrate the Bank Memory view and manual refresh into Outfitter as a subordinate view with account-switch/logout clearing, checked item sprites/variants and scan timestamps. The button may prompt a bank opening, but must not force a bank scan or interrupt the green O temporary Bank Tags withdrawal tab. Add seven-day stale warning and native JDK17 ConfigManager acceptance.

**P0-D:** Add safe same-login quest resurvey, permission/transport observations and completed-activity feedback, then repeat goal-directed journeys through 100/500/1000 **verified** completions. Track supply sustainability, long-term skill coverage and skipped recommendations.

## Files and reproduction

`REPRODUCE.md` gives clean apply/build commands. `W07-INCREMENTAL-AFTER-W06.patch` is the isolated four-file change; `W07-CUMULATIVE-CANDIDATE-AGAINST-W0751.patch` is the complete candidate for a fresh extraction. `ISSUE-LEDGER.csv` contains source locations, affected account numbers, severity, and reproduction details. `evidence/` contains seven W07 files, `regression_w04`/`regression_w05`/`regression_w06` preserve comparison evidence; a file-by-file SHA-256 manifest protects the bundle.

**Disposition: RELEASE HOLD.** Source-connected candidate work and offline QA advanced; full first-run capture, persistent Bank Memory UI, seven-day reminder, native RuneLite verification and end-to-end Pathfinder progression are still NOT implemented/accepted.