# TrashPanda Tracker · Development log

Public development notes for **TrashPanda Tracker (TPT)**, also known internally as **Project Burrow**.

**Current status:** Active development. Not published on the RuneLite Plugin Hub. Entries describe the work and evidence available at the time; they do not certify the code currently checked into this repository.

## November 2, 2026 · Build #38 source archived

The [exact owner-tested Build #38 ZIP](../development-snapshots/B38/README.md) is now preserved on GitHub. Owner-confirmed four manual Best in Bank withdrawals with the temporary O tab remaining available. This does not certify unrelated features. The independent HoverLite v0.2.8 development copy is included for the historical dual-client test. **Plugin Hub release remains on hold.**

## November 2, 2026 · Building account-aware intelligence

### Owner-tested development progress

- **Best in Bank:** In a separate Windows RuneLite development build, the owner reported successfully withdrawing **four suggested items manually** while the temporary Bank Tags workflow remained usable. This validated that user interaction in that test, **not** every bank state, account, or equipment rule.
- **Bank safety:** Continued work on detecting when the bank view is valid, preventing stale observations from being used for another account or session, and keeping the player in control of withdrawals.
- **Outfitter decisions:** Improving the distinction between required mechanics and optional equipment so an owned lower-tier option can be useful without falsely demanding every high-tier slot.

### Research and design in progress

- **Item intelligence:** Broader catalog coverage, including blessed d'hide variations, Huey equipment, bloodbark/swampbark, hunter's sunlight crossbow, ornament/imbued states, charge status, ammunition, and set effects. Research coverage is **not** equivalent to implemented recognition.
- **Activity preparation:** Working toward realistic gear and supplies for bossing, Slayer, clues, and skilling, with only verified requirements enforced.
- **Sessions:** Research into in-game Boss Kill Log and Slayer Kill Log sources. Kill-count ingestion must have trustworthy event provenance; loot drops must not be treated as proof of a kill.
- **UI contract:** Four tabs (**Home / Pathfinder / Outfitter / Sessions**), compact scrollable sidebar, recognizable OSRS icons, and a true **4 × 7** inventory plan. Chronicle navigation must not unexpectedly change Outfitter selections.

### What happens next

1. Reconcile the latest owner-tested development baseline before moving actual modern plugin source into the public repository.
2. Keep documentation, source snapshots, and release candidates clearly separated.
3. Finish bank, gear, and progress-tracking intelligence updates; test the integrated candidate in RuneLite before Plugin Hub submission.

**Source transparency:** `Project-Burrow/` on the current public `main` branch represents an older prototype. This dated log records newer project progress without implying that those changes have already been uploaded as Java implementation.

---

*Feral Enterprises · Public development update, not a release announcement.*
