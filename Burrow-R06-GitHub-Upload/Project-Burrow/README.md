# Project Burrow • Feral Edition 🦝

Experimental **independent rebuild**, NOT a modification, release, or replacement of Iron Ledger.

## What exists in R01
- A 225px interactive Swing preview, with four working destinations: Home, Pathfinder, Outfitter, Sessions.
- A **single immutable source-of-truth plan** driving all 56 Outfitter cells (11 gear / 28 inventory / 17 reserved), stock allocation, and missing-supply checks.
- **Best in Bank is a mode-switch demonstration only**; no real gear optimization or wiki advice is wired in R01.
- Manual **demo** bank input; account/epoch and revision isolation; no automatically inferred bank contents, account history, overlays, or game actions.
- Demo boss selection and cumulative probability math, sandbox session events, generated previews, and offline regression tests.
- A *minimal, unverified* RuneLite integration adapter source, which still requires real Gradle/RuneLite compilation and authentic client QA. This preview JAR is **NOT a RuneLite plugin JAR**.

## Execute locally (EXACT JDK 17 required; macOS/Linux/WSL)
```bash
export JAVA_HOME=/path/to/jdk-17
bash scripts/build-offline.sh
"$JAVA_HOME/bin/java" -jar build/BURROW-OFFLINE-PREVIEW.jar
```
`build/previews/*.png` are rendered from the real Swing panel, not a marketing illustration.

For Windows, select an actual JDK 17 install as JAVA_HOME and run the included scripts using WSL/Git Bash. `javac --release 17` on JDK 21 alone is not accepted. Do not install the offline preview JAR as a RuneLite plugin. The RuneLite adapter can be compiled later with a Java 17 Gradle build and resolvable RuneLite client artifacts using `gradle test jar` or an approved wrapper.

## Feral project contract
- Visual/product North Star: owner's accepted Iron Ledger V2 walkthrough and its interactive V2 concept. They are references, not required source code.
- Crew: **Spoons** (intake/register/QA L1); **Noodles** (features/integration); **Sporks** (surgical bugs); **Tongs** (independent QA L2); **Wrench** (corporate oversight and raccoon advocacy).
- Distinct project prefix: `BUR-`, separate folder, never inherit `ILT-` status or source.
- Source, tests, hashes and reproducible commands first; paperwork cannot be marked implementation.
- Any user-observable action currently performed is explicitly **DEMO** unless authenticated by RuneLite owner testing.
- Release: **HOLD**. Not approved for live account use, private beta, auto-withdrawal, or Plugin Hub.

## Next engineer mission
Connect read-only RuneLite account and equipment observations behind a strictly scoped adapter. Keep bank scan **manual**, bank highlighting **OFF**, never infer stock from a stale bank, and preserve the same `Plan` object across bank/gear/checklist. Build genuine JUnit/RuneLite matrix and run owner live-event/click tests before claiming functionality.

## BUR-0006 strict Java 17 policy and host bridge

**JDK 17 compiler and Java 17 runtime are mandatory**. `scripts/require-java17.sh` blocks any other major version before compilation. Gradle is also required to run on JDK 17, use Java 17 toolchains for tests and JavaExec, and target Java 17 class files.

```bash
export JAVA_HOME=/absolute/path/to/jdk-17
bash scripts/build-runelite-java17.sh
```

No authentic RuneLite compilation is claimed for BUR-0006 yet: this executor only has OpenJDK 21, no Gradle or resolved RuneLite client artifacts, and no working DNS to fetch them. The new read-only lifecycle coordinator and JUnit tests are **source only, not Java 17 compiled or executed** as of this checkpoint. Release stays HOLD.
