# 🦝 Spoons | Wave 6 Outfitter Logic Sanity Audit

**Scope:** exact owner test candidate `TPT-INTELLIGENCE-W06-FINAL-CLEAN-DUAL-DEV-OWNER-QA-HOLD.zip`  
**SHA256:** `2d65bf89eaa1b4058a3c903c212b1867158ee4d37fc7a828af1b576e63f01c7e`  
**Package entries:** 423; CRC integrity PASS; examined source matches package bytes  
**Disposition:** **SOURCE/DECISION-CORRECTNESS HOLD.** No source edits, game actions, or GitHub changes made.

## Executed scope

- Fresh compilation of **332** pure Java class files (all `core`, `bib` and `intelligence` sources) with OpenJDK 21 `javac --release 17`. This checks Java 17 language/API compatibility but is **not** a JDK 17 Gradle or RuneLite launch.
- Synthetic planning over **190** available non-Wilderness boss+method+tier keys, each with **gear-only** and **all-template-items-owned** positive bank evidence: **380** plans.
- **0 exceptions, 0 11-gear/28-inventory geometry failures.**
- **2/190** fully-owned templates still produce `NOT_READY` due to source gear/ammo incompatibility. Other plans may still be `VERIFY`; this is *not* evidence of all other templates being gameplay-ready.
- Gear-only bank examples: **105/190** emitted a successful (`fits=true`) switch-space calculation despite at least one required switch not observed. These plans generally remain `NOT_READY` due to separate missing-switch blockers, but their fit diagnostic is hypothetical/misleading.
- W02 resource: **80** item IDs, **9** typed relations, **zero** approved gameplay rows. New source-gated bridge does not materially improve live Outfitter recommendations yet.

## Specific findings

| ID | Severity | Finding | Required action |
|---|---|---|---|
| OUTF-W06-001 | P1 | Kree'arra, `Wiki ? Ranged (tank)` Entry: Twisted bow worn with **no arrows** in ammo or inventory. | Re-source this profile with ammunition and quantity. |
| OUTF-W06-002 | P1 | The Leviathan, `Wiki ? Ranged` Entry: Rune crossbow paired with **Ruby dragon bolts (e)**, which it cannot fire. | Correct projectile/weapon pairing from canonical source. |
| OUTF-W06-003 | P1 | Switch simulation and slot budgeting include source inventory items absent from the owned plan. | Simulate only physically present, resolved switch items and count actual free slots. |
| OUTF-W06-004 | P1 | A six-charge glory request accepts a one-charge glory as a reviewed equivalent. | Differentiate combat-equivalent gear from charge-sufficient travel gear. |
| OUTF-W06-005 | P2 | The Bank O positional export may include owned but unusable gear (unmet level, quest or unknown requirement). | Separate bank-present from equip-ready in the withdrawal presentation. |
| OUTF-W06-006 | GATE | All new gear research remains `CHECK`; no verified rows are activated. | Independently verify individual facts before enabling them. |

## Exact code evidence

- `BestBankPlanner.java`: inventory `used++` for every non-null source cell (`~323-400`), then `simulateSwitches(gear, profile.inventory(), free, facts)` (`~408`), which iterates **source** items rather than resolved stocked switches (`~898-960`).
- `ItemVariantPolicy.java`: `GLORY_CHARGED` chooses the first owned charged variant, regardless of planned minimum charges.
- `BurrowPlugin.java:551-570`: nonempty ids exported for gear statuses such as `LEVEL_UNMET` and `QUEST_UNMET`.
- `BestBankIntelligenceBridge.java:55-58`: exits early when no approved gear items or relations.
- `AmmoRules.java`: Rune crossbow max bolt tier 61 (runite); dragon bolts tier 64. Twisted bow requires separate arrows.

## Reproduction receipts

```text
GLORY6_WITH_GLORY1 EQUIVALENT_VARIANT selected_id 1706
LEVEL_UNMET.physicallyOwned=true
QUEST_UNMET.physicallyOwned=true
GEAR_PACK items=80 approved=0 relations=9 approved_relations=0
SCENARIO Hueycoatl Body melee plus tail switches tier RECOMMENDED verdict VERIFY shown RECOMMENDED inv planned 14 used 14 shown_available 0 missing_switches 0 free 14 switch_checks [] blockers 0
SCENARIO Grotesque Guardians Mid-game melee with Eclipse atlatl ranged switch tier RECOMMENDED verdict NOT_READY shown RECOMMENDED inv planned 17 used 17 shown_available 0 missing_switches 1 free 11 switch_checks [SwitchCheck[label=Switch to Eclipse atlatl, verified=false, fits=true, freeSlotsNeeded=0]] blockers 1
  missingSwitch Eclipse atlatl
SCENARIO Scurrius Standard melee tier OPTIMAL verdict NOT_READY shown OPTIMAL inv planned 13 used 13 shown_available 0 missing_switches 2 free 15 switch_checks [SwitchCheck[label=Switch to Bone mace, verified=true, fits=true, freeSlotsNeeded=0], SwitchCheck[label=Switch to Dragon claws, verified=true, fits=true, freeSlotsNeeded=0]] blockers 2
  missingSwitch Bone mace
  missingSwitch Dragon claws
SCENARIO Dagannoth Kings Rex safespot with Guthan healing switch tier RECOMMENDED verdict NOT_READY shown RECOMMENDED inv planned 17 used 17 shown_available 0 missing_switches 3 free 11 switch_checks [] blockers 3
  missingSwitch Guthan's warspear
  missingSwitch Pet rock
  missingSwitch Rune thrownaxe
```

```text
PROFILE_KEYS=190 ATTEMPTS=380 ERRORS=0 GEOMETRY=0 MISSING_SWITCH_SIMULATION=105 MISSING_SWITCH_FIT_TRUE=105 MISSING_SWITCH_FIT_FALSE=0 OWN_ALL_PROFILES=190 OWN_ALL_NOT_READY=2
MISSING_SWITCH_CALC Scurrius / Standard melee / OPTIMAL free=15 missing=2 calculated=[SwitchCheck[label=Switch to Bone mace, verified=true, fits=true, freeSlotsNeeded=0], SwitchCheck[label=Switch to Dragon claws, verified=true, fits=true, freeSlotsNeeded=0]]
MISSING_SWITCH_CALC Sarachnis / Low-level melee, crush with Zamorakian hasta / OPTIMAL free=10 missing=2 calculated=[SwitchCheck[label=Switch to Dragon claws, verified=false, fits=true, freeSlotsNeeded=0]]
MISSING_SWITCH_CALC Amoxliatl / Melee, full Inquisitor with mace / OPTIMAL free=14 missing=2 calculated=[SwitchCheck[label=Switch to Dragon claws, verified=false, fits=true, freeSlotsNeeded=0]]
MISSING_SWITCH_CALC Hueycoatl / Body melee plus tail switches / ENTRY free=12 missing=1 calculated=[SwitchCheck[label=Switch to Glacial temotli, verified=false, fits=true, freeSlotsNeeded=0]]
MISSING_SWITCH_CALC Hueycoatl / Body melee plus tail switches / OPTIMAL free=10 missing=6 calculated=[SwitchCheck[label=Switch to Dual macuahuitl, verified=false, fits=true, freeSlotsNeeded=0], SwitchCheck[label=Switch to Obligator, verified=false, fits=true, freeSlotsNeeded=0], SwitchCheck[label=Switch to Dragon claws, verified=false, fits=true, freeSlotsNeeded=0]]
OWN_ALL_NOT_READY Kree'arra / Wiki ? Ranged (tank) / ENTRY blocker=[CORE Ammo: Twisted bow needs ammo and none usable is owned]
OWN_ALL_NOT_READY The Leviathan / Wiki ? Ranged / ENTRY blocker=[CORE Ammo: Ruby dragon bolts (e) cannot be used with Rune crossbow]
```

## Recommendation

Do **not** describe Wave 6 Outfitter as fully researched, complete, or ready for release. Fix source-loadout contradictions and switch budgeting, then perform the owner's full Windows RuneLite test against one repaired cumulative build. Preserve the working Bank R05 layout lifecycle and HoverLite sources. No bulk promotion of the staged 80-item catalog.