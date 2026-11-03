# TrashPanda Tracker UI Shell · R10C (standalone)

**Release hold.** This branch is a separate UI shell, not the RuneLite plugin. No main TPT source, bank or player data were changed.

## Use with Cursor
1. Clone the repo, then checkout `ui/tpt-shell-r10c-cursor-20261103`.
2. Open `ui-shell/r10c` in Cursor.
3. On Windows with Java 17+, run `01-REBUILD-UI-SHELL-WINDOWS.cmd`, then `00-OPEN-UI-SHELL-WINDOWS.cmd`.
4. For genuine 225px Swing previews, run `05-RENDER-SWING-PREVIEWS-WINDOWS.cmd`. For the screenshot-comparison version, run `07-RENDER-DESIGN-REFERENCE-WINDOWS.cmd`.

These are demo fixtures for three bosses only. Nothing is connected to live RuneLite account data, KC, Collection Log, bank, or Pathfinder engine. Design-reference boss portraits are owner-supplied image crops, not authenticated OSRS models. `assets/verified-models` has zero approved model PNGs.

Non-negotiable: four tabs; independent Chronicle and Outfitter selection; real 225px sidebar; equipment has 11 positions and inventory 4x7; rejected chunky raccoon must stay excluded. Do not merge into the main plugin without explicit owner authorization.

Generated screenshots, jars, classes and old ZIPs are excluded from Git; regenerate with the provided scripts.
