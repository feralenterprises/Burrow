# TPT Wave 7.5.1: One-click RuneLite 1.13.5 launch repair

**Status: owner Windows retest required.** This is a cumulative TPT + HoverLite dual-plugin source ZIP based on Wave 7.5.1 test-repair. The game crash `error_game_js5connect_outofdate` is consistent with an outdated RuneLite/game client; a fresh RuneLite dependency is a targeted fix, not a guarantee.

## Do this

1. Extract this **entire ZIP** into a new folder. Do not run the `.cmd` inside ZIP preview and do not paste the file's contents into Command Prompt.
2. Double-click **`START-HERE-TPT-AND-HOVERLITE.cmd`** in the extracted top-level folder.
3. The script locates `Project-Burrow` automatically, checks **JDK 17**, refreshes the RuneLite dependency at **1.13.5**, and launches exactly one RuneLite development client with **both TrashPanda Tracker and HoverLite**.
4. It does **not** run the complete test suite during launch. To run the full test suite separately use `00-BUILD-AND-TEST-BOTH.cmd`.

## Repair scope

- Updated `Project-Burrow/build.gradle` default RuneLite dependency from **1.13.4 to 1.13.5**.
- Added `START-HERE-TPT-AND-HOVERLITE.cmd` and made the standard launcher self-locating and refresh dependencies before `runBoth`.
- Synchronized the two optional legacy/test launchers with RuneLite 1.13.5.
- **No TPT gameplay Java source, bank operations, UI, HoverLite Java source, gear recommendations or data resources changed.**
- This package does not run any external updater or delete RuneLite caches, preferences or account data.

## If the same crash still happens

Try the normal installed RuneLite app. If that also gives the same error, the game client/cache or connection needs attention independently of the plugin. RuneLite's own troubleshooting suggests that outdated-client issues may stem from a recent game update or cache write failure. Do not remove cache folders before making a backup. Log location is `%USERPROFILE%\.runelite\logs\client.log`.

## Verification in this build environment

- Source archive integrity, full-file SHA manifest, exact source-diff and launcher static checks: **PASS**.
- Actual Windows JDK 17 Gradle compilation, RuneLite 1.13.5 dependency resolution, actual RuneLite launch/login, and owner gameplay QA: **NOT EXECUTED here**.
- The previously repaired 11 regression-test cases are carried forward unchanged from Wave 7.5.1; the full 676-test Windows run is still not independently verified.

This is a developer/owner test build only; **not a release candidate until Windows checks pass**.
