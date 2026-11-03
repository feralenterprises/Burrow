# TrashPanda Tracker | R02 Wave 5 · Outfitter Shell

**Status: STANDALONE UI CHECKPOINT · RELEASE HOLD · OWNER REVIEW REQUIRED.** This is a separate Java Swing design shell, NOT the RuneLite plugin and NOT connected to any OSRS account or bank.

## Implemented in Wave 5

- Kept four Home / Pathfinder / Outfitter / Sessions tabs. Boss Chronicle remains a subview, and its selected boss remains separate from Outfitter's selected boss.
- Rebuilt the Outfitter control hierarchy around the selected boss, **Entry / Recommended / Optimal** tiers, and **Wiki / Best in Bank** modes. The previously clipped `Recommended` label now fits at 225px.
- Placed three clickable **Melee / Ranged / Magic** buttons immediately inside the **Equipment** card, above the 11-slot OSRS equipment silhouette. These use local verified item icons for visual illustration, never to assert ownership or a recommendation from live account data.
- Preserved exactly 11 meaningful equipment positions arranged within the standard 5×3 visual silhouette, including proper blank spaces.
- Built a numbered **7 rows × 4 columns = 28** inventory view. Tapping a slot cycles local *unassigned → SW (switch) → SUP (supply) → PR (prayer) → unassigned* markings; Reset clears all 28. This is a purely visual switch-lane editor and does not manipulate game inventory.
- Added an expandable **Bank Memory** design preview. It explicitly says there is no connected account, no most-recent bank scan, no saved items and no evidence freshness. Persistent per-account snapshots, first-run collection and 7-day stale-data reminders are NOT implemented by this shell.
- Added an expandable **Trip Evidence / VERIFY** card. It never treats unknown ownership as missing or claims trip readiness, blockers, substitute calculations or bank verification from absent data.
- Added a compact source-labelled **Notable Reward** card after the inventory. Its published per-drop odds are not converted into personal KC or cumulative odds.
- Retained approved lean armored raccoon header, with rejected chunky pixel mascot excluded.

## Exact behavior test

Run `java -jar build/TrashPanda-UI-SHELL-R02-W05.jar --self-test` from the extracted folder. The self-test checks all prior W04 navigation, Chronicle, Home, Pathfinder and Sessions behaviors, then verifies 11 equipment slots, the 7×4 grid, button wiring, Best in Bank state, Bank Memory, evidence expansion, inventory markers and reset, and absence of the rejected mascot.

## Screenshot evidence

`previews/outfitter-225px.png`, `previews/outfitter-bank-225px.png`, `previews/outfitter-guide-lower-225px.png`, `previews/outfitter-evidence-lower-225px.png`, `previews/outfitter-magic-225px.png`, and corresponding `*-3x-phone.png` are actual Java Swing prints, not image generation. The panel remains 225 logical pixels wide with vertical scrolling.

## Not yet implemented or validated

- No RuneLite client/Java17 machine verification, account connection, bank persistence, item-ID ownership scan, first-run account baseline, gear-recommendation engine, set-effect calculations, bank commander, or automated game actions.
- Inventory markers are temporary UI state, not stored account layouts.
- The 3 sample bosses are a limited shell fixture, not an exhaustive Outfitter catalog. Alternate boss methods are not fabricated.
- Owner has not approved the W05 UI. This is an isolated visual checkpoint.

## Source isolation

Only `src/gg/feral/tpt/shell/TrashPandaShell.java` inside this standalone shell was changed from W04, alongside shell-specific documentation, launch scripts, class files, and screenshots. No main TPT repository or existing plugin source was edited. The bundled artwork assets remain byte-identical to W04.
