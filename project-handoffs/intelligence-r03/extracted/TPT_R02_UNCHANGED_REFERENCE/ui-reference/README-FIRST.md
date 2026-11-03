# TrashPanda Tracker | Standalone UI Shell R02 · Wave 2

**Status: UI SHELL ONLY | STANDALONE WINDOWS PREVIEW | RELEASE HOLD**

This is an isolated 225px Java Swing mock plugin, not a RuneLite plugin and not the live TPT build. No existing TPT source, Bank Tags, Best in Bank logic, account data, or HoverLite implementation was changed.

## New in W02: authentic OSRS icon assets

- Replaced all three **boss reference-image crops** with pixel-accurate source-backed OSRS boss/category icon artwork: **Scurrius, Sarachnis, Bryophyta**. Boss tile backgrounds are local layout decoration; the character pixels are unchanged apart from nearest-neighbor enlargement.
- Replaced all three **notable rare-drop reference crops** with verified 36×32 inventory sprites: Scurrius' spine (ID 28798), Sarachnis cudgel (23528), Bryophyta's essence (22372).
- Added real OSRS item sprites to the Melee, Ranged and Magic badges and Outfitter selectors: Dragon scimitar (4587), Magic shortbow (861), Mage's book (6889). Active and inactive states are still UI sample recommendations, not inferred account eligibility.
- Preserved independent Chronicle vs Outfitter boss selection, four tabs, 225px logical width, 11 equipment slots, 4×7 inventory, sample-only values, and all W01 layout fixes.
- Bundled assets inside the offline JAR. There is **no web fetch** during shell use.

Every source PNG was checked byte-for-byte against its repository Git blob SHA; read `W02-ASSET-PROVENANCE.md`. The enlarged 65px boss tiles intentionally use low-resolution game/UI icons and **are not claimed to be native 3D game-model renders**. Source-backed full boss model artwork is an opportunity for Chronicle Wave 3 if a properly licensed and verifiable source is available.

## Windows launch

1. Extract the entire ZIP into a fresh folder.
2. Double-click `00-OPEN-UI-SHELL-WINDOWS.cmd` with Java 17 or newer. A standalone UI window opens, *not RuneLite*.
3. Optionally use `01-REBUILD-UI-SHELL-WINDOWS.cmd` to rebuild from this isolated source with JDK 17+.
4. For review, open `previews/browse-3x-phone.png`, `previews/detail-3x-phone.png`, and `previews/outfitter-3x-phone.png` on a phone. Original-size 225px Swing captures are included alongside them.

## Verification performed

- Java source compiled successfully with `javac --release 17`, Java 17-target bytecode.
- Standalone JAR launched through virtual X11 display for six native Swing screens (each 225px wide plus 3× enlargement).
- `--self-test` passed asset-loading checks, all four tabs, Chronicle boss search/card/back, independent boss state, gear-source selection, and icon-backed combat-style buttons.
- ZIP integrity, source asset hashes, and no references to the removed placeholder sprite files checked.

## Limits and safety

- **No real RuneLite client, game item image provider, native Windows click testing, bank linkage or live account data.** The shell is solely a design approval vehicle.
- Home XP and readiness, boss odds, Pathfinder and Sessions are sample/placeholder UI only.
- Boss portraits are genuine sourced small **OSRS icons**, not high-resolution 3D boss models. They can look intentionally pixelated at 3× phone preview scale.
- This package must not be deployed to RuneLite Plugin Hub as a plugin or used to replace working TPT source.

## Remaining waves

W03 Boss Chronicle presentation and interactions · W04 Home/Pathfinder/Sessions · W05 Outfitter detail · W06 Settings, final polish and owner visual QA. Production integration requires separate authorization.