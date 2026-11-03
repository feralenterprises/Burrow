# 🦝 TrashPanda Tracker | Wave 07 | Outfitter Repair + Boss Combat Styles

**DEVELOPMENT HOLD • Windows owner QA required.** This is cumulative source through Wave 7, with TrashPanda Tracker and **HoverLite v0.2.8** in one RuneLite developer client. The actual Windows RuneLite launch has **not** been performed in this environment.

## Wave 07 logic changes, by example

1. **Combat styles actually choose boss-specific gear methods.** Melee, Ranged and Magic buttons select a corresponding method with a source gear profile where available; for example, Dagannoth Kings switches between Rex/Magic, Prime/Ranged and Supreme/Melee. The selected tier is retained if supported, or moves to a tier that actually contains gear. A hybrid can be an alternate only if its recorded method genuinely includes that style.
2. **Existing research is available without pretending to be certified.** Brutus, Obor and Bryophyta have three style examples, but several of those gear layouts are *provisional research drafts*. Giant Mole has Melee and a provisional Bowfa Ranged layout; its Earth Magic research has **strategy only**, not an invented 11-slot recommendation. Scurrius Ranged and Magic remain strategy-only pending full loadout evidence. The UI states the difference.
3. **No false Best in Bank activation.** Provisional gear drafts and strategy-only methods can be selected and viewed, but are excluded from background bank preparation and Bank O. Existing source catalog methods retain the normal Best in Bank workflow. No research-only item relationship is promoted as approved. Wilderness bank prohibition remains in force.
4. **Repair invalid source pairings.** Kree'arra Twisted bow receives a boss-specific arrow source with the *trip quantity still VERIFY* (no one-arrow READY). The Leviathan Rune crossbow is no longer paired with incompatible dragon bolts.
5. **More truthful switch/slot logic.** Missing physical switches are not simulated as fitted into the 4×7 inventory. Inventory slot usage reflects observed packed items, not absent planned items.
6. **Charge and wear safety.** A one-charge glory no longer satisfies a six-charge teleport plan. Bank O no longer displays unwearable, unverified or absent item entries as ready withdrawals.

## Gear methods and research boundary

`W07-BOSS-COMBAT-STYLE-COVERAGE.csv` inventories method availability for **60 selectable bosses** and three styles. The catalog has 75 boss-style choices with a source gear profile; 9 have provisional example gear; 7 are strategy-only; and 89 boss-style pairs have no method. **23 bosses offer two or more selectable style paths**, but not all have a complete bank-approved gear profile in every style. These counts are **coverage metadata, not proof of Wiki validation or native RuneLite behavior**.

This wave does NOT complete the full OSRS item/variant research intake. W02 staged 80 item IDs with zero independently approved newly active item rules, and the CA BQA-906 placeholder export stays quarantined. Do not claim full multi-style gear coverage for every boss.

## Windows launch

1. Extract the ZIP into a **new, clean folder**; keep the prior known-working Bank R05 ZIP for rollback.
2. Use Windows **JDK 17** or set `JAVA17_HOME` to its installation directory.
3. Run `00-START-BOTH-NO-REPEAT-QA.cmd` from the ZIP root.
4. That invokes Gradle `runBoth` to build the changed Java source and launch **one RuneLite client with TPT + HoverLite v0.2.8**. It does not repeat the full test suite.
5. If the launch fails, capture the first relevant error line, not only `BUILD FAILED`.

## Development check scope

The source uses Java 17-compatible language features and passed a **Java 21 `javac --release 17`** isolated compilation, headless Swing style-click tests, bank-gate tests, targeted regression probes, and a synthetic full-profile scan. This is **not** equivalent to a genuine JDK 17 Gradle build or gameplay QA.

No RuneLite client was opened here. The plugin JAR isn't signed or submitted to Plugin Hub. **Release remains on hold.**