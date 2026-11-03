# R02 Wave 6 | Offline UI QA Report

**Overall: OFFLINE SHELL PASS / RELEASE HOLD.** This audit covers a standalone Swing design shell, not the live TPT plugin. It is not an independent Tongs/owner acceptance result.

## Checks performed

- Compilation: Java `javac --release 17` successful (run using Java 21 host emitting Java 17 class files).
- Offline interactions: `--self-test` checks all W01–W05 regression behaviors plus new Appearance controls, saved font/accent preferences, reset-to-defaults, four-tab invariant, and Chronicle/Outfitter boss independence.
- 225px bounds: all rendered screenshots inspected for physical output size 225x790; 3× versions are 675x2370.
- Visual review: default and Large + Azure screenshots for Home, Pathfinder, Outfitter, Sessions, Boss Chronicle browse/detail; Settings bronze/azure/moss previews. The previous Unicode-gear and truncated-font-button defects were corrected before release packaging.
- Artifact integrity: ZIP CRC, JAR presence, assets SHA cross-check, rejected `feral-hero.png` absent, standalone-only Java source, no client code or RuneLite imports.

## Explicitly NOT verified / known remaining issues

- Genuine OSRS boss model renders are not included. Small OSRS sprites remain instead; no art-fidelity sign-off.
- Some content is below the fold and requires scrolling as expected in a 225px sidebar.
- This is sample data. No personal account information, bank freshness, first-run capture, real recommendations, or session statistics.
- No native Windows screenshot, real RuneLite client, or in-game click/hover/keyboard test has been performed.
- Font scaling was reviewed at the target screen size offline, not exhaustively across every OS font configuration or display scaling factor.
- No owner acceptance and no integration into main TPT. Best in Bank and bank commander remain untouched.

## Acceptance gate

Owner should verify color/typography, the four-tab layout, header art, and touch/click/scroll at the 225px preview. Do not merge with the main plugin or mark the art as approved until the authentic boss-model portrait issue and native RuneLite behavior are addressed.
