# 🦝 TrashPanda Tracker | Wave 05 of 06: Combat Achievement Intelligence

**Developer checkpoint, release HOLD. Owner formal RuneLite QA deferred until the final six-wave handoff.**

## Only new Wave 05 logic

1. A **versioned, strict CA research pack** crosswalks the existing 290 Chronicle reference rows without changing their contents. Unique IDs are local deterministic crosswalk IDs, **not Jagex canonical task IDs**.
2. A CA has separate fields for *exact game-checked condition*, *legacy description*, *execution strategy/tip*, *verified prerequisite gates*, and *game-confirmed completion evidence*. Neither tips nor recommended equipment can define the task requirement.
3. An unreviewed task always yields `RESEARCH_ONLY`; unknown observed access yields `VERIFY`, confirmed missing required access yields `BLOCKED`, and observed prerequisites yield `ELIGIBLE_TO_ATTEMPT`, **never an automatic COMPLETE**.
4. Completion requires a source-approved task, identified account and explicitly game-confirmed task-specific source. Boss KC, gear, visits or generic drop messages cannot mark it done.
5. Placeholder-looking names/criteria are rejected if somebody tries to upgrade them to `VERIFIED_SOURCE`. Schema, ID, boss+name collision and six tier point rules are explicit.

## Actual researched data status

- 655-task / 2697-point official aggregate is a separate reference. We do **not** claim 655 locally researched records.
- 290 existing Chronicle reference rows preserved, 77 tips preserved, all CHECK, zero source-reviewed game conditions promoted. The 652 placeholder BQA-906 rows are not bundled as CA tasks.
- No CA tasks, tips or completion states in the live Chronicle are changed by Wave 05. Wave 06 is the user-facing integration/connection wave after independent provenance work.

## Protected

All Wave 04 Java source, content, UI, Bank R05, Best in Bank planner and HoverLite v0.2.8 remain byte-for-byte unchanged. Wave 05 only adds isolated intelligence model, decision rules, staged research, schema, tests staged for final QA, and packaging documents. The quick-launch script is relabeled to reflect Wave 05.

## Development safeguards, not formal QA

- Wave04 archive SHA and CRC checked.
- 290/290 JSON Schema Draft 2020-12 rows valid; 290 unique local IDs and 290 distinct boss+name pairs; zero approved entries.
- New isolated Java files compiled with local OpenJDK21 using `javac --release 17` and a small synthetic development probe checked gates/unknowns/no auto-completion.
- Not executed here: genuine JDK17 Gradle/RuneLite, Windows client, full JUnit suite, owner game QA, actual CA completion adapter.

Next Wave 06: connect only reviewed CA intelligence into existing Chronicle and route its completion evidence conservatively. Maintain owner QA hold until the combined deliverable.
