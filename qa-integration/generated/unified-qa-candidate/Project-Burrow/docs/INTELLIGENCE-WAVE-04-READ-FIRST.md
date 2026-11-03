# TrashPanda Tracker | Intelligence Wave 04 | Activity Intelligence

**Status:** CUMULATIVE DEVELOPMENT CHECKPOINT, RELEASE HOLD. **Owner protocol:** no formal QA until Wave 06; final native RuneLite QA is owner's responsibility.

## Wave 04 scope, logic changes ONLY

1. Added a **versioned activity pack**, separate from the W01 foundation and W02 gear catalog. It contains 44 carefully bounded *research-staging* entries: 14 distinct Farming tree/fruit-tree patches, 19 diary or minigame teleport methods, 7 skilling/minigame activities, and 4 clue-step profiles. All remain `CHECK` and **none drive live player recommendations**.
2. Added **requirement semantics**: actual vs boostable skill levels, derived combat level, quest/diary unlocks, exact item requirements, required vs recommended/optional supplies, and OR alternatives (e.g., Volcanic Mine entry via per-game fee or permanent unlock).
3. Added **transport constraints**: one shared 20-minute minigame teleport cooldown across spell/Grouping use, optional daily-use caps and observed remaining charges, carried item prerequisites, with UNKNOWN never treated as unlocked or charged.
4. Added **route truth semantics**: seven regular trees and seven fruit trees, with two separate Gnome Stronghold and Farming Guild records, plus Nemus Retreat and Kastori. Teleport suggestions are contextual, not performance-ranked. No invented fastest route or observed account state.
5. Added **clue-step separation**: general travel, emote equipment, combat and Wilderness clues. Step-specific required items need exact IDs before a reviewed recipe can read READY. Wilderness clue profiles never authorize automated bank/Best in Bank behavior.

## Protected

- **All 423 Wave 03 ZIP members are byte-for-byte unchanged.** There is no modification to existing Java source, UI, gear planning, Bank O, Bank R05 withdrawal, Bank Tags, and HoverLite code.
- Wave 03's existing source-gated gear bridge remains exactly as supplied.
- No CA task changes: BQA-906 652 placeholders remain rejected and are not imported.
- Owner's four tabs, 11 worn slots, 28 inventory cells unchanged.

## Research provenance and limitations

- Staged URLs and notes are review pointers, **not immutable Wiki revision pins**. Source revision is `UNKNOWN`; the typed engine only authorizes `VERIFIED_SOURCE` with a true `oldid=<digits>` or 40-digit Git SHA. There are **zero approved activity entries**.
- The activity pack is conservative: unfamiliar item IDs are zero (unresolved) rather than fabricated; incomplete bank/account evidence stays UNKNOWN. Daily remaining teleports and cooldown status must come from observed sources, never inferred from owned gear.
- Supported examples include Aug 2026 Sepulchre Agility level changes, 30/3000 numulite Volcanic Mine payments, and live patch expansion to 7+7. All examples are CHECK until independently pinned and reviewed.
- The shared minigame cooldown is a common group and not a per-hub independent timer.
- For final QA, review the pending provenance pins and add tests for goal/route presentation and account data adapters. Integration to user-facing activity selection is a Wave 06 concern. No new tab.

## Development safeguards, not formal QA

- CRC and SHA manifest checks of parent and cumulative ZIP.
- Java 17 *language compatibility* compilation against local OpenJDK 21 `javac --release 17` for this isolated engine and required core parser, not a genuine JDK 17 Gradle/native run.
- Synthetic activity logic probes; these are **development checks only**. JUnit fixtures included but not run.
- The entire Windows Java17 Gradle build, actual RuneLite bank/quest observation, runtime UI and owner test remain pending.

## Next Wave

**Wave 05: Combat Achievement Intelligence.** Research-only 655-task correction staging, no placeholder promotion or unverified game-check requirements. Continue from this exact cumulative W04 ZIP.
