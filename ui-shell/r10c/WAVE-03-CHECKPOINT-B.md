# TrashPanda Tracker UI Shell | Wave 3 Checkpoint B

Pure Java Swing, 225px standalone shell. Does not integrate or mutate the live TPT project.

- Preserves the approved bronze-armored raccoon portrait; rejects and excludes feral-hero.png.
- Refines the boss gallery with non-truncating two-row native filters and consistent loot cards.
- Keeps separate Chronicle and Outfitter boss picks; adds previous/next boss navigation and a chooser.
- Renders game-sourced OSRS pixel icons with nearest-neighbor scaling instead of blurry smoothing.
- Updates boss detail rare reward provenance. Bryophyta's essence is a **chest** reward, not a direct boss drop (changed 2025-06-25); 1/118 is chest-open chance.
- Adds published-rate explanation toggles and 5 Scurrius, 5 Sarachnis, 7 Bryophyta Wiki Combat Achievement tasks with tiers and short summaries; personal completion explicitly unknown.
- Shows consistent personal-state labels (Verified KC: Unavailable, rare drops: Unverified), and never estimates personal odds without verified data.
- Captures 10 screenshot views at 225px and 3x scale, including expanded and scrolled detail screens.

**Sources verified 2026-11-03:**
- https://oldschool.runescape.wiki/w/Scurrius
- https://oldschool.runescape.wiki/w/Sarachnis
- https://oldschool.runescape.wiki/w/Bryophyta
- https://oldschool.runescape.wiki/w/Combat_Achievements/Tasks_by_boss
- https://oldschool.runescape.wiki/w/Bryophyta%27s_essence

**Release hold:** Windows/RuneLite native QA not run, three real boss fixtures only, user approval pending. Existing Best in Bank/engine never modified.
