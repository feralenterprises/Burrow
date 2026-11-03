# 🦝 TrashPanda Tracker | Wave 06 Complete | Clean Dual-Plugin Owner QA Build

This is the complete **Wave 01–06 development checkpoint**, not a production release. Wave 06 finished the intelligence integration and the **single combined owner QA** is now the next gate.

## Only Wave 06 logic changes

- Pathfinder now accepts a **verified-only activity prerequisite check** after its existing suggestions. It may downgrade an unsupported READY but never upgrades an UNKNOWN or assumes bank items, diaries, or cooldowns.
- Chronicle now has an **exact boss/name/tier crosswalk** to approved Combat Achievement conditions. Unverified tasks cannot replace existing research. The expanded CA card visibly says **RESEARCH CHECK**.
- Bank O, Best in Bank source, whole-loadout ranking, all four tabs, Sessions, and HoverLite v0.2.8 are unchanged from Wave 05. A single client still loads both plugins.

**Critical research limitation:** Gear W02, Activity W04 and CA W05 packs currently have **zero independently approved gameplay rules**, despite their 80 item IDs, 44 activity entries and 290 legacy CA references. The new intelligent gates are wired but cannot yet change live recommendations from this research. The old Chronicle references remain visible with a CHECK warning, not falsely marketed as verified. Do not treat the build as fully informed about all OSRS items or CAs.

## Windows launch

1. Extract this ZIP into a **new folder**. Keep your prior known-working Bank R05 build separately.
2. Confirm Windows **JDK 17**, or set `JAVA17_HOME` to your JDK17 directory.
3. Double-click **`00-START-BOTH-NO-REPEAT-QA.cmd`** in the ZIP root.
4. It invokes the Gradle `runBoth` task to compile and launch **one RuneLite client with TPT + HoverLite**. It intentionally skips a full repeat test suite.
5. If launch fails, copy the **first relevant terminal error**, not only the final `BUILD FAILED` line.

This ZIP contains **Windows-buildable source**, not a precompiled signed plugin. Full JDK17 Gradle, Windows RuneLite launch and gameplay QA were not run here.

## Your one QA pass

- Bank O tab and four successive Best in Bank withdrawals: no tab flicker or stale-bank reset.
- Gear choice and 4x7 inventory unchanged; source-gated intelligence must not invent ownership.
- Pathfinder still shows appropriate readiness; Chronicle expanded CA card marks its research as CHECK.
- HoverLite works independently alongside TPT, with right-click and actual actions untouched.

**Status: DEVELOPMENT HOLD.** The development integration is ready for your combined QA, not approved for Plugin Hub publication. See `Project-Burrow/docs/INTELLIGENCE-WAVE-06-READ-FIRST.md` for detailed boundaries.