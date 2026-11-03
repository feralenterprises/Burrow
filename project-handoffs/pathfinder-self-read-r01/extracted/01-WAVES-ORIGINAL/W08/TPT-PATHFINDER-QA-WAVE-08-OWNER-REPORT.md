# 🦝 TPT | PQA-W08 | First-Run Capture & Bank Memory Native Host Integration
**November 3, 2026 · Independent QA candidate · RELEASE HOLD**

## Results first
**48 synthetic account fixtures**, **1,104/1,104 new assertions passed**, **24/24 older Wave 4–7 evidence CSVs byte-identical**, **2/2 Wave 8 CSVs byte-identical** after fresh compile and replay. No native RuneLite JDK17 build/run was performed. No verified 100/500/1000 completed-activity journey exists. **Four source files** changed from isolated W07; the engine's ranking/catalog remains unchanged. This is a source-connected candidate, not production acceptance.

### Most important findings
1. **Four tabs preserved, bank/capture accessible (48/48 offline fixtures).** `SidebarHost` adds narrow subordinate Bank and Capture buttons with a Back action. It keeps the existing main scroll viewport; no fifth TPT root tab. Account changes return to main and erase displayed historical rows. The new `GuidedCapturePanel` labels its first-run state **INCOMPLETE**.
2. **Persistent Bank Memory now actually reaches the host (48/48).** `BurrowPlugin.publishAccountMemory()` reads the W06 `PathfinderAccountArchive`, and the Swing host publishes only the authenticated account's historical values, exact item IDs, timestamps, and seven-day staleness. Bank Memory is *not* a live ownership proof. Item charge/variant identity is still not independently reviewed.
3. **Manual capture has truthful semantics (48/48).** User clicks record `PLAYER_REVIEW` only, with **zero** native KC/item/diary assertions. The existing W07 native Collection Log observer is unchanged. Dedicated native boss, Slayer, diary, and CA readers remain absent, so first-run COMPLETE cannot be claimed.
4. **Explicit quest refresh, bank-safe manual refresh (48/48 callback fixtures).** The account-guarded quest button starts a bounded survey; the bank button explains that the player must open the OSRS bank and the existing scanner will save the next accepted full-bank observation. No auto game UI actions or new Bank O scan pathway were introduced. A bank already open may still need a separately designed safe rescan trigger.
5. **Two independently evolving source lineages need reconciliation (2 lineages).** The pinned full host is W0751 (SHA below); the later `pathfinder_wave4_complete.zip` is a separate Cursor Pathfinder engine candidate with baseline commit `1795760...`, whose handoff says W0751 was missing in that lane. It was not merged or overwrote this host. Native acceptability and goal convergence remain blocked.

## Evidence and source identity
- **Pinned full-host original:** `TPT-W0751-RUNELITE-135-ONE-CLICK-DUAL-DEV-FIX.zip` SHA-256 `79e7d54a78a0bb76b6b3eca0e6804067b02b40a35e88339ee83a20408aa7ef3f`.
- **W08 incremental patch:** `W08-INCREMENTAL-AFTER-W07.patch` (for W07 source *only*).
- **W08 cumulative patch:** `W08-CUMULATIVE-AGAINST-W0751.patch` (for untouched original source *only*). Independently checked and applied cleanly to the exact ZIP bytes. The four W08 source files match after application.
- **Divergent Cursor engine archive:** `pathfinder_wave4_complete.zip` SHA-256 `ae019db6888b5870770a0c4458ef6b12089718977d4dcc92ea262d22d42570be` contains a separate 25-Java-file Pathfinder engine/test subset, not a complete RuneLite host. Its own source lineage must be evaluated independently.
- **Code category:** W08 uses existing `RuneLite ClientToolbar`, `PluginPanel(false)`, `ClientThread.invoke`, `ConfigManager`, existing bank scanning, and W07 guided capture. No new game widget API was guessed.

## Distinction between observed and unobserved
- `ACTUAL SOURCE`: the new host and callback routes exist in the candidate files.
- `OFFLINE EXECUTION`: actual Java W08 UI + guided journal tested against small RuneLite host stubs. `javac --release 17` ran on **Java 21**. These stubs are **not** evidence of a native RuneLite host working.
- `ACTUAL GAMEPLAY`: **none** performed. No bank opening/withdrawal, Ring of Wealth log, enchanted-gem Slayer log, diary, client restart persistence or real Quest#getState feedback was independently accepted.
- `SYNTHETIC REPLAY`: 48 accounts with three bank-staleness conditions; results include both new CSVs and 24 preserved regression files. The 6,000 unchanged-state calls from earlier waves remain unchanged and do not represent 6,000 completed game activities.
- `UNKNOWN`: no count is inferred as zero when a screen was not witnessed; manual review never equals KC, completed diary or whole Collection Log.

## Open issues / exact counts
See `ISSUE-LEDGER.csv` for 12 issue records with severity, affected fixtures, specific source locations and reproduction. The release-critical findings are: missing four native capture readers (**48/48**), first-run still incomplete (**48/48**), no native JDK17 RuneLite acceptance (**0 native runs**), bank variant/charge interpretation unverified (**48/48 fixtures**), and unverified 100/500/1000 completed-activity goal convergence (**0 verified journeys**). The seven-day banner is event/publish-driven, not a time-triggered notification during an indefinitely idle session.

## Next wave (PQA-W09)
First reconcile the standalone Cursor Pathfinder Wave 4 source and this full-host W08 branch. Then independently validate source-owned Ring of Wealth and enchanted-gem log widgets, diaries, CA stages and their account/session owner boundaries. Implement only verified readers, preserve UNKNOWN, and test A→B→A, logout, world hops, client restart, and Bank O withdrawal in real RuneLite using JDK17. Once the capture has true coverage, test manual rescan and seven-day reminder wakeups, then rerun goal journeys with actual completion evidence. **No merge/publish until both independent L2 and owner-native QA.**

**Disposition:** `PASS_SCOPED_OFFLINE / NATIVE_HOLD`. The isolated package doesn't modify production code, GitHub, historical research archives, or player accounts.