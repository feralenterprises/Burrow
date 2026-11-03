# TrashPanda Tracker | Wave 7.5 | Gear-Backed Combat-Style Research

**DEVELOPMENT HOLD.** This is the complete cumulative source from Wave 7, with Wave 7.5 research-only changes. One RuneLite client launches **TrashPanda Tracker + HoverLite v0.2.8** through `00-START-BOTH-NO-REPEAT-QA.cmd`. No Windows RuneLite session was run in this environment.

## What's changed

1. **Scurrius Ranged:** selectable provisional bone shortbow (item 28794) + rune arrow (892) plan. 50 Ranged and a Scurrius spine required; cannot be used for first-spine attempts. Equipment/inventory quantities are suggested, NOT source-complete or bank-verified.
2. **Scurrius Magic:** selectable provisional bone staff (item 28796), 50 Magic and *stored chaos charges* required; its powered attack is rat-only. Loose chaos runes are optional recharging stock, not proof of charge.
3. **Giant Mole Earth Magic:** selectable provisional Earth Bolt (29 Magic) demonstration with staff of earth (1385), 200 air runes and 100 chaos runes for 100 theoretical casts. Mole's earth weakness is verified. Light item changed to **lit lantern 4550**; spade required; fire unlock can remove light need.
4. **Obor/Bryophyta:** First giant/mossy key permanently unlocks respective lair. Normal repeat fights don't require a new key; optional reward chest claims consume one. Research-only Ranged/Magic inventories no longer imply key-per-kill. Existing catalog core untouched.
5. **Safety:** All new styles remain `RESEARCH_GEAR` with `CHECK` and cannot be used by Best in Bank preload/Bank O. Only the Recommended tier exists for the three new gear proposals. No fabricated Entry/Optimal setup.

## Source links used

- Scurrius bone weapons: https://oldschool.runescape.wiki/w/Bone_shortbow ; https://oldschool.runescape.wiki/w/Bone_staff
- Scurrius strategies: https://oldschool.runescape.wiki/w/Scurrius/Strategies
- Mole earth weakness and method: https://oldschool.runescape.wiki/w/Giant_Mole/Strategies ; https://oldschool.runescape.wiki/w/Earth_Bolt
- Obor/Bryophyta keys: https://oldschool.runescape.wiki/w/Obor ; https://oldschool.runescape.wiki/w/Bryophyta
- Lit lantern: https://oldschool.runescape.wiki/w/Bullseye_lantern

## Current research style coverage (60 bosses × 3 styles)

- Source catalog gear: **75** style paths
- Provisional gear examples: **12** style paths
- Strategy-only (no gear): **4** style paths
- Remaining absent: **89** boss/style pairs

These are coverage metadata, not independent Wiki gear-table certification.

## Development verification performed

- `javac --release 17` on Java 21: 4 targeted source-driven tests compiled
- `Wave075ResearchGearTest`: PASS
- `Wave7BossStyleSelectionTest`: PASS
- `Wave7OutfitterRegressionTest`: PASS
- `OutfitterStyleInteractionTest`: PASS, 237 Swing assertions
- All test inputs are synthetic. Actual JDK 17 Gradle, RuneLite UI, bank O withdrawals and HoverLite runtime need owner testing.
- Source comparison and ZIP integrity: see `W075-CHECKPOINT-RECEIPT.json` and SHA-256 manifest.

## Windows launch

Extract into a **fresh folder** and run `00-START-BOTH-NO-REPEAT-QA.cmd`. It runs Java 17 preflight followed by `gradlew.bat --no-daemon --console=plain runBoth` for **one RuneLite client with both plugins**. Keep the previous W07 ZIP for rollback.

**No GitHub publish, source promotion, auto-banking, or release approval.**
