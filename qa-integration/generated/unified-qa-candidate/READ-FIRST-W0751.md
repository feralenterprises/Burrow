# TrashPanda Tracker | Wave 7.5.1 | Windows Regression Contract Repair

**STATUS: OWNER WINDOWS RETEST REQUIRED. RELEASE HOLD.** This is a cumulative dual-plugin source build, not an independently verified RuneLite release. Based on the owner's Wave 7.5 Windows Gradle `:test` report: **676 completed, 11 failed**.

## What changed in Wave 7.5.1

**No gameplay source changes.** Ten test files were updated to reflect **existing** Wave 7/7.5 behavior without disabling the safety assertions. The two root scripts have version/banner-only text changes.

1. Count invariants now require 51 original gear profiles, 2 Brutus plans, 10 research drafts (7 older + 3 new), and 6 demon encounter drafts, **69 total before Build 20**. The original 51 remain a separate strict assertion.
2. Combat-style tests now require the new Scurrius/Mole gear previews, while continuing to assert that alternate styles do not borrow the wrong boss's items.
3. The Leviathan Rune crossbow test now checks that its fixed source loadout uses **ordinary Ruby bolts (e)**, and that higher-strength Ruby **dragon** bolts cannot be selected for that crossbow. It no longer expects the previously fixed broken source data.
4. The Kree'arra Twisted bow test explicitly removes the source's now-included arrow from the synthetic bank before asserting that a no-arrow plan blocks. The existing one-arrow quantity warning is still checked separately by Wave7OutfitterRegressionTest.
5. The protection blocker test now supplies an identity-matching blocked plan for **every** boss method; otherwise newly added alternate methods legitimately produce UNKNOWN. The test still requires BLOCKED when each method is actually missing protection.
6. Hitboxes, ordinary mouse hover, scrollable preparation notes, tooltip bounds and the provisional Bank O gate now cover the three new research gear paths rather than asserting those paths do not exist.

**No changes:** Bank R05 read/withdraw, Bank O lifecycle, ItemVariantPolicy, live Best in Bank, all equipment/item data, 4x7 inventory, four tabs, hoverliteDev v0.2.8, dual-plugin launch logic. Research setups remain provisional and cannot trigger Bank O.

## Developer checks performed in this environment

- Verified original Wave 7.5 ZIP integrity and computed an exact SHA-256 diff.
- Java 21 `javac --release 17` compiled dependency-free core/UI/experience/bib/intelligence/pathfinder source.
- Recompiled and executed the **11 specific previously failing test bodies** with local source-compatible JUnit assertion shims for the five JUnit methods and direct main calls for the other six: **11 local passes**. These are NOT an authentic JDK17 Gradle/JUnit execution.
- Replayed six additional tests: Wave075ResearchGearTest, Wave7BossStyleSelectionTest, Wave7OutfitterRegressionTest, BankLayoutEligibilityTest, OutfitterStyleInteractionTest (237 UI assertions), Wave7ResearchBankGateTest. All passed in the local offline Java21/`--release 17` environment.
- Verified every preexisting **production source file byte-for-byte unchanged**. SHA-256 list is in `SHA256-FILE-MANIFEST.csv`; changed files enumerated in `W0751-CHANGED-FILES.csv`.
- **NOT RUN HERE:** Windows JDK17 Gradle full 676-test suite, actual RuneLite launch, bank gameplay QA. We cannot call the complete test suite green until your Windows rerun.

## Windows retest (fresh folder, once)

1. Extract this ZIP into a fresh directory (do not overlay Wave 7.5).
2. Run `00-BUILD-AND-TEST-BOTH.cmd` for the full Gradle/JDK17 test gate.
3. Only after the build/test passes, run `00-START-BOTH-NO-REPEAT-QA.cmd` to launch **one RuneLite dev client with TPT and HoverLite**. The launch script does not rerun the entire test task.
4. If a test still fails, send its name plus the **first assertion message** from `Project-Burrow/build/reports/tests/test/index.html` (or the corresponding XML). Existing Wave 7.5 remains the rollback checkpoint.

No production release, GitHub update, bank action, or import of unapproved research has occurred.
