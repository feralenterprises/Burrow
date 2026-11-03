# 🦝 TPT | PQA-W09 | Native Kill Log Readers & Source Reconciliation

**Date:** 2026-11-03 (America/Los_Angeles) · **Mode:** isolated source-connected engineering QA · **Release:** HOLD

## Results first

- **Accounts:** 48 deterministic synthetic account fixtures. **Completed journeys:** 0 verified. **New widget-contract observations:** 816 case rows and **2,016/2,016 assertions passed**. The original W08 sidebar tests remain **1,104/1,104 passing and two CSVs byte-identical**. The separate Cursor Wave4 engine recompiles offline with **19/19 Wave4 and 20/20 L2 tests passing**.
- **Source-lineage policy comparison:** 48 common input profiles × 6 focus policies = **288 matched scenarios**. The highest recommendation differed in **112/288 cases, affecting 48/48 profiles** (32 ANY, 32 QUESTS, 48 PVM). `readyNow` differed in **48/288 cases, affecting 48/48 profiles**, exclusively PVM. Supplies list lengths also differed in **48/288**. Both engines evaluated the same number of catalog rules per paired profile, so this is decision divergence, not merely catalog expansion.
- **File-level conflict:** full-host Pathfinder has **12 Java files**, Cursor has **16**; **9 overlap, 6 overlap files differ, and only 3 match byte-for-byte**. The important `Snapshot` and `Suggestion` record signatures differ, particularly around goal deferral, feasibility/Outfitter contract and resources. **No Cursor source was merged.**
- **New read-only reader:** an isolated Kill Log widget adapter now uses RuneLite's documented `InterfaceID.KillLog` title/name/count fields and `WidgetLoaded` event, defers reading until a later game tick, and stores only exact numeric rows to the existing account-fenced capture journal. **Boss source structure has public RuneLite precedent; Slayer layout/title is unverified in the target live client.** Screens with mismatched rows, hidden widgets, generic titles, broken counters, or duplicate names fail closed.
- **UNKNOWN is protected:** the 48 synthetic capped Slayer values (`Lots!`) were not interpreted as zero or exact 65,535. A witnessed page is always `PARTIALLY_WITNESSED`, never a complete log. Pending log events are rejected after account switches, A→B→A epoch changes, paused sessions or timeouts. No new banking/mouse/menu API calls or Bank O mutation path were introduced.

**Three highest-priority actions:** (1) reconcile `SuggestionEngine` and Outfitter feasibility contracts without copy-over; (2) run genuine JDK17 + RuneLite boss/Slayer widget QA, including A→B→A and Bank O; (3) add verified diary/Combat Achievement screen readers and complete first-run capture.

## Source precedence and limits

- Full-host source artifact: `TPT-W0751-RUNELITE-135-ONE-CLICK-DUAL-DEV-FIX.zip` SHA256 **79e7d54a78a0bb76b6b3eca0e6804067b02b40a35e88339ee83a20408aa7ef3f**. W08 is an isolated cumulative patch above it.
- Previous W08 artifact: `TPT-PATHFINDER-QA-WAVE-08-2026-11-03.zip` SHA256 **53388942c1e105364058f408fa489e1210512b085f6dda64f37b529879cd99b7**.
- Cursor engine artifact: `pathfinder_wave4_complete.zip` SHA256 **ae019db6888b5870770a0c4458ef6b12089718977d4dcc92ea262d22d42570be**, with reported standalone baseline commit `17957600a0789c21a80aa20d78e14ee0068afb98` (not a commit pinned to the full-host source).
- **W09-INCREMENTAL-AFTER-W08.patch** applies only over exactly W08-patched source; **W09-CUMULATIVE-AGAINST-W0751.patch** applies only over untouched W0751 source. Both were applied to independent pristine copies with byte-identical resulting `BurrowPlugin.java`.
- No production source, GitHub repository, RuneLite account, original research archive, or existing Bank O/Best in Bank integration was changed.

## Reader design and trust contract

1. `KillLogWidgetReader` uses `InterfaceID.KillLog.INTERFACE_TITLE`, `NAME`, and `KILL` and checks widget visibility and equal child array lengths. The original RuneLite ChatCommandsPlugin code provides public precedent for the boss log fields (not proof of the live owner's exact revision).
2. `KillLogPageParser` permits only an exact Boss Kill Log or candidate Slayer Kill Log title. It parses strictly displayed numbers and preserves a stable page identity based on visible names. A capped `Lots!` row is absent from exact-count evidence.
3. `KillLogCaptureFence` ensures the deferred read belongs to the very same authenticated account, login epoch and short widget load interval, excluding same-tick, expired, paused, logout, hop and A→B→A cases.
4. `PathfinderGuidedCapture.observeKillLogPage` stores positive widget rows under the correct section and login fence. It cannot mark exhaustive page coverage or general first-run COMPLETE.
5. `BurrowPlugin` subscribes to `WidgetLoaded`, calls a single deferred read from `onGameTick`, and drops any pending event on non-logged-in game states. It does not create a new scanner or touch Best in Bank withdrawal lease logic.

## What the tests do and do not prove

**ACTUAL CODE INSPECTED:** the full-host and Cursor Java files, exact file hashes, real `SuggestionEngine.suggest` in two separately compiled engines, and the newly added Java reader/capture/fence implementations.

**EXECUTED OFFLINE:** 48 account fixtures × 17 source/capture checks; 288 policy comparisons per engine; Cursor's 39 tests; W08 Swing 1,104 assertions and byte-equal evidence. `javac --release 17` ran on Java 21; the 1.13.5 full RuneLite dependency classpath was not compiled in this wave. Simplified test stubs model RuneLite UI types and must not be confused with a game client.

**PUBLIC SOURCE SUPPORT:** RuneLite's Chat Commands log reader: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/chatcommands/ChatCommandsPlugin.java ; RuneLite Widget API: https://static.runelite.net/runelite-api/apidocs/net/runelite/api/widgets/Widget.html ; OSRS enchanted gem and Slayer capped row behavior: https://oldschool.runescape.wiki/w/Enchanted_gem .

**ASSUMPTIONS / OPEN:** whether the 1.13.5 live Slayer interface uses the exact candidate title/layout; actual widget event order; true KC values on owner account; library item states; native JDK17 Gradle; player account identity at client startup; client restart persistence; Bank O coexistence. These are NOT accepted outcomes.

**GOAL CONVERGENCE:** no evidence from this wave that repeatedly following recommendations produces a well-rounded account. Zero verified completed journeys; no simulated XP/loot was invented.

## Targeted native QA to unblock Wave 10

Use the exact patched source hash in the ZIP manifest on a genuine Windows Java 17 RuneLite developer client. Record the actual `InterfaceID.KILL_LOG` group, title and visible name/count widget lengths when opening Ring of Wealth Boss Kill Log and enchanted-gem Slayer Kill Log. Test a known displayed zero, a `Lots!` row, multiple pages, a closed log, world hop, logout, A→B→A and client restart. Open and withdraw four owner-selected items through existing Best in Bank O, then verify O remains available and no historical bank evidence is treated as live. Open diaries/Combat Achievements and identify source-proven stage signals before implementing their readers. Only after passing this native acceptance should the separate Cursor engine changes be moved through staged source/behavior reconciliation.

## Files in this handoff

`ISSUE-LEDGER.csv`, `LINEAGE-FILE-MATRIX.csv`, `LINEAGE-POLICY-DIFF-288.csv`, `W09-INCREMENTAL-AFTER-W08.patch`, `W09-CUMULATIVE-AGAINST-W0751.patch`, the new Java classes, `harness/` with stubs, `evidence/` decision and widget traces, `SOURCE-RECEIPT.json`, and `REPRODUCE.md`.

**Final disposition:** `PASS_SCOPED_OFFLINE / LIVE_LOG_ACCEPTANCE_HOLD / MERGE_HOLD`.