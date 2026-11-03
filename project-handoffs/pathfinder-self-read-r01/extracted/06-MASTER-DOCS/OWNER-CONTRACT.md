# Owner contract: what every future AI must preserve

**Project:** TrashPanda Tracker (TPT), Feral Enterprises / Project Burrow. **Tagline:** “We dig through your account so you don’t have to.” **Audience:** OSRS Ironmen and mains. **Release:** HOLD. **Purpose:** account-aware progression intelligence, not just Wiki links.

## Required player-observable functionality

- First authenticated use: capture, save and maintain every **actually obtainable** account-progress category, including skills/XP, quests, boss lifetime KC/log, Slayer log, Collection Log, Achievement Diaries and Combat Achievements. Where RuneLite cannot passively read a menu, guide **manual opening** and capture only what is actually shown. No game-click automation, hidden-API inventions or guessed zero.
- Every account's observations must survive logout, world hops, client restarts, plugin updates and safe migrations; A→B→A must not expose one account's data to another. Separate first-use baseline, latest actual observation, actual account progress and unverified manual review.
- Manual refresh/sync action and approximately seven-day age/staleness reminder, with accurate timestamps and evidence scope. Display absent, stale, or partial as unknown/incomplete, not current inventory.
- **Bank Memory**: durable per-account actual last-bank-scan view, item IDs and variant/charge states, quantities, date, freshness; allow users to inspect saved bank outside bank screen. Best in Bank may use historical snapshot cautiously but never claim stale stock is currently present. Preserve native `O` temporary Bank Tags preparation workflow with manual withdrawals and do not introduce a competing scan or auto-bank script.
- The owner later specifically requested a **Bank Memory tab/view**. The historically locked four primary tabs and the W08 subordinate Bank Memory proposal are a genuine navigation reconciliation item. Do not silently erase the later requirement under a four-tab assumption; have owner accept final placement in the native UI.
- Pathfinder should produce achievable balanced trajectories: quests and rewards, prerequisites, transport, combat, key utility skills (Farming/Fishing/Cooking/Herblore), equipment, bank states, GP, sustainable food and other consumables; follow goals such as Barrows gloves → Song of the Elves → Bowfa realistically. Not one giant static list or repetitive high-scoring AFK task. Explain readiness, blocked/missing items and why-now, and handle goal changes/skips/rejections.

## Visual/UI invariants unless superseded explicitly

Graphite/bronze Feral Modern style; ~225px RuneLite sidebar with vertical scroll; historically Home/Pathfinder/Outfitter/Sessions main tabs; Chronicle is independent subview with its own boss selector; 5-row scrollable/searchable boss picker; authentic OSRS sprites; 11 equipment slots; 4×7 inventory positions; compact equipped, armored raccoon with red scarf if an approved asset is available. The chunky, round pixel-art raccoon was specifically rejected. Never substitute a full-width fantasy dashboard for actual 225px interactive panels. Provide functional controls, not illustrative buttons.

## Other core safety contracts

- Best in Bank: observed, source-scoped and item-state-aware; no Wilderness Best in Bank; preserve bank `O` manual withdrawal lease; no game automation.
- Outfitter: actual named styles/methods, Wiki Recommended versus Best in Bank, real equipment, exact item/variant/states, hard mechanic blockers vs optional missing, full-set effects only when applicable, no fabricated supplies or slot counts.
- Sessions: independently verified XP/KC/drop provenance; a loot receipt is not boss KC; rolling last ten actual sessions; unknown KC not zero.
- Research: R31 historical save remains immutable, R32 staged and not automatically promoted, GEAR-W01 researched rows not approved by mere inclusion.
- HoverLite is a separate plugin; do not mix its code with TPT runtime behavior or change game menus.
- Do not publish to Plugin Hub, claim L2/owner approval, change account data or GitHub, or mutate canonical tickets based solely on this pack.

## Source versus acceptance

Documentation records intent, not proof. A Java/Swing simulation passes only the checks actually asserted. Native Windows JDK17 + RuneLite gameplay, account/session persistence and Bank O coexistence must be proven with targeted actual client steps and evidence. Recent PQA waves contain scoped isolated work, not an accepted product update.

For more detailed historical product context and the later requirements correction, read inside `03-PARALLEL-CURSOR/TPT_CURSOR_MASTER_PACK_R02_2026-11-03.zip`, particularly `TPT_CURSOR_MASTER_CONTEXT.md` and `audits/REQUIREMENTS-GAP-REGISTER.md`.