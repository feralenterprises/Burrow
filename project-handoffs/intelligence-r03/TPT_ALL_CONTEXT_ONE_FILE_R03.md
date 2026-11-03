# TrashPanda Tracker | Complete Intelligence Master R03

**Compiled:** 2026-11-03 (America/Los_Angeles)  
**Program:** Feral Enterprises / Project Burrow  
**Latest input:** Cursor Pathfinder Wave 6, independently reviewed by Spoons L2 W06 R01  
**Global disposition:** **RELEASE HOLD**. Pathfinder overall **REWORK**.  
**Important:** This is a consolidated engineering brief and evidence index, **not** a claim that code has been merged, deployed, committed, run in RuneLite, or independently checked under JDK 17 Gradle. Do not treat text here as an implemented feature.

## CURRENT STATUS SUPERSEDING OLDER REFERENCE NOTES

1. Pathfinder source **Wave 6** is the latest *uploaded isolated* repair candidate, not a verified integration into the current complete TPT RuneLite code. The previous TPT master R02 still describes **Wave 7.5.1** as the latest documented full-plugin development candidate (complete W0751 source not supplied). These are different version lines: **Pathfinder W06** and **TPT W0751**. Do not assume one incorporates the other.
2. Spoons independently reran **136/136 supplied Wave 6 checks** on isolated Java source using **OpenJDK 21 `javac --release 17`**, not a real JDK 17 Gradle integration run. **10 newly authored negative expectations failed, 1 control passed**. The 10 failures are documented in `PATHFINDER_MASTER_AUDIT_AND_RISK_LEDGER.md` and the original independent L2 W06 report.
3. Prior Wave 5 had **101/101** established checks green; Spoons found **12/13 adversarial checks flagged problems**. Wave 6 repaired the previous 13 adverse scenarios, but new failures remain. Green historical regressions are **scoped repair evidence**, not full engine acceptance.
4. No full Pathfinder source integration, Windows RuneLite QA, durable per-account Account Memory, Bank Memory, fresh Outfitter provenance, or 7-day guided resync is verified by these Pathfinder ZIPs. **These requirements are mandatory and should not disappear from the roadmap.**
5. Wave 6 simulation: **12 profiles**, **11 end `NO_LEGAL_TRANSITION`**, 1 already-advanced profile reaches Song of the Elves. **Bowfa, full Recipe for Disaster and Barrows gloves paths remain unproven**. Do not claim overall progression intelligence is solved.
6. Highest severity Wave 6 open defect: an account-B Pathfinder snapshot with account-A leftover Outfitter/Best-in-Bank READY string can be treated as account-B READY. Self-copied account identity is **not genuine proof**. Also open: stale bank timestamp relative to session; inadequate Plank Make/String Jewellery rune/GP quantities; one action sold as 30-60 minute sustainability; unsourced resource/prerequisite handling for fly fishing, smithing, pottery, and gilded altar.
7. Wave 6 archive has **10 outdated `SHA256SUMS.txt` entries** and **6 stale source checksum entries**, per independent L2. New packaging integrity here does not retroactively fix Wave 6's internal checksum manifests.
8. **NEXT CURSOR WORK: Pathfinder Wave 7 only**. Use `NEXT_COMMAND_PATHFINDER_WAVE7.md` plus the unchanged L2 W06 audit. Preserve all existing passing tests, correct the flawed cross-account test oracle, independently reproduce red-to-green, cover every catalog readiness branch, and reconcile full current repo before any merge. No UI integration or other engines yet.

## EVIDENCE AUTHORITY / NO UNFOUNDED PROMOTION

- **Owner product requirements:** R02 master below, newer owner decisions override older ones. Bank Memory must be visible as a tab/view; whether fifth primary tab or nested tab remains for owner decision before UI implementation.
- **Independent executable source evidence:** Spoons audit bundles, each scoped to its included isolated Pathfinder Java source.
- **Cursor claims and attached results:** Cursor ZIPs; verify their baseline hashes, checksum manifests, and coverage against the actual repository.
- **Real native host data/behavior:** NOT VERIFIED by this pack. Earlier owner-tested Bank R05 manual Bank O success is narrow independent context, not proof of current candidate.
- **OSRS requirements:** source-verify on the Old School RuneScape Wiki (include URL, dated access and rule rationale); existing isolated tests may encode only subsets of quest/recipe data.

## HOW TO READ THE PACK

1. `README_START_HERE.md`: navigation and one-command instructions.
2. `PATHFINDER_MASTER_AUDIT_AND_RISK_LEDGER.md`: detailed verified-vs-open findings and W01-W06 results.
3. `PATHFINDER_FINDINGS_REGISTER.csv`: machine-readable defect tracking for Cursor.
4. `NEXT_COMMAND_PATHFINDER_WAVE7.md`: owner-ready next engineering prompt.
5. `CURSOR_ORIGINALS/`: five original Cursor ZIPs, unchanged.
6. `SPOONS_L2_ORIGINALS/`: five independent L2 packages, unchanged, with runnable probes.
7. `TPT_R02_UNCHANGED_REFERENCE/`: full unchanged prior TPT master context, source reference, product rules, research files, owner requirements and standalone UI reference. **Not newer deployed source.**
8. `PACKAGE_SHA256SUMS.txt` inventories every component file within this new pack.

---

# PREVIOUS COMPLETE TPT MASTER R02 (PRESERVED IN FULL BELOW)

> The following is carried forward verbatim from R02 for durable product, implementation and research context. The Pathfinder Wave 6 delta above takes precedence for Pathfinder status only. R02 source version language applies to its own compilation date.

# TRASHPANDA TRACKER (TPT) | CURSOR MASTER CONTEXT

**Feral Enterprises · Project Burrow · canonical product direction / engineering brief / decision ledger**  
**Compiled:** 2026-11-03 (America/Los_Angeles)  
**Audience:** Cursor AI agents, maintainers, QA/research crew, future implementation sessions  
**Owner correction:** **R02 (2026-11-03), persistent Account Memory + Bank Memory required and not verified in implementation**  
**Release state:** **HOLD** · development candidates exist, but there is no substantiated current production/Plugin Hub approval for TPT.

> **CURSOR: READ THIS ENTIRE FILE FIRST.** Use it as a decision-and-intent guide, not as proof of the current checkout's code, a complete OSRS encyclopedia, or authorization to merge, publish, or change the player's bank. Read the actual repository/ZIP, compare source hashes, identify the checked-out revision, run tests, and surface discrepancies before editing. The latest observed owner instruction always takes precedence over an older roadmap. `references/` carries original supporting handoffs. This document is a compilation, not a replacement for BUR/RSR canonical registers or independently verified RuneLite tests.

---

## 0 | One-minute executive brief

**Name:** TrashPanda Tracker (TPT). **Program:** Project Burrow. **Legacy concept:** Iron Ledger. **Publisher/team:** Feral Enterprises. **Tagline:** “We dig through your account so you don’t have to.”

**What TPT is:** A useful, source-grounded, account-aware Old School RuneScape RuneLite sidebar assistant, especially valuable to Ironmen. It transforms actually observed player state and trusted OSRS strategy knowledge into actionable decisions rather than duplicating the Wiki: *What can I do now? Why? What should I bring? Which gear do I really own and can equip? What happened this login? What is my next useful upgrade?*

**Shape:** historically **four main tabs** `Home / Pathfinder / Outfitter / Sessions` (newer explicit **Bank Memory tab** request is a pending navigation-placement decision; see §0A), **225px nominal RuneLite sidebar width**, vertical scrolling, Feral Modern graphite/charcoal with restrained warm bronze. **Boss Chronicle is an in-panel view**, Settings is in the header, and the native-bank **O** preparation button is **not a tab**. Original OSRS item/skill/boss sprites, compact armored-friendly raccoon art, legible real controls, 11 equipment slots and a 4-column × 7-row **28-slot** inventory. Do not swap in a giant fantasy-window illustration or a generic dashboard.

**Safety:** no automation of banking, gear moves, game clicks, combat or account actions. **No Wilderness Best in Bank**, even via cache/background preload. **UNKNOWN ≠ 0, research ≠ verified, loot ≠ KC, chosen goal ≠ completed goal, bank possession ≠ equipability, partial local tests ≠ native acceptance.** Only authenticated, source-labeled, sufficiently fresh evidence can drive owned-gear decisions.

**Current engineering lineage at compilation:** latest *described* cumulative implementation is **Wave 7.5.1** (test-contract repair on Wave 7.5 production code), documented in `references/TPT-W0751-READ-FIRST.md`; its full source archive was **not available in this compiled pack**. The latest fully materialized implementation archive here is **Wave 7.5** at `source/TPT-INTELLIGENCE-W075-MULTISTYLE-GEAR-CLEAN-DUAL-DEV-OWNER-HOLD.zip`. A **standalone UI shell R02/W02** is bundled as a separate `ui-reference/` archive and preview set; it is not integrated or approved production Java. Wave 7.5 Windows `:test` had **676 tests with 11 failures**; 7.5.1 adjusted 10 tests and two script banners and locally replayed the failures with Java21 `--release 17` shims. **The complete Windows JDK17/Gradle suite, actual RuneLite run and Bank O regression on Wave 7.5.1 are not established.** TPT remains release-held.

**Owner-tested baseline:** the earlier **Bank R05 / Build 38** was manually exercised in real RuneLite; owner reported Best in Bank successfully withdrawing **four items** while the temporary Bank Tags `O` tab stayed active. That is a narrow owner QA success, not blanket proof for account-switch, stale-bank, crash or all variants. The `#38` exact archive was previously identified as `38-FERAL-DUAL-DEV-BANK-R05-O-FREEZE-HOVERLITE-v0.2.8-WINDOWS-QA-LEAN.zip` SHA256 `711dd92392d63cde669d646e195610e2d0ca12a17683249874f9231e943ca0ec`. Do not downgrade the observed success, and do not claim current Wave 7.5 has inherited native QA by implication.

**Separate plugin:** HoverLite is a **separate plugin**, not a TPT feature. The Wave 7.5 dual-development launcher embeds **HoverLite v0.2.8** in an independent source set and runs both in **one** RuneLite developer client. A newer standalone HoverLite release may exist; do not silently substitute its source or version into this TPT archive. HoverLite should never change actual RuneLite menu-entry semantics.

---

## 0A | OWNER CRITICAL CORRECTION: ACCOUNT MEMORY + BANK MEMORY (2026-11-03)

**MANDATORY / RELEASE-BLOCKING / IMPLEMENTATION UNVERIFIED.** These requirements were discussed before this pack, but the earlier master and several older execution plans failed to record them as concrete end-to-end functionality. This is a **specification correction**, not a claim that the code was changed or native-tested. The audit must discover whether existing code already partially implements them before suggesting repairs.

1. **Persistent account progression baseline:** At first use for an authenticated OSRS account, TPT collects everything actually observable: skills and XP, quest statuses, Achievement Diaries, Combat Achievement completion, boss kill log and lifetime KC, and other explicitly supported progression. Where RuneLite cannot passively expose an in-game record, guide the player to **open the required native in-game interface manually**, capture what becomes available, and label coverage. No direct menu-click automation, no invented access to a hidden API, and no guessed completion. One incomplete domain must not prevent verified domains from saving.
2. **Save across logouts, world hops, RuneLite restarts, plugin updates, and crashes:** Maintain an authenticated, account-scoped, versioned local persistence store, with per-domain `observedAt`, `lastSuccessfullyVerifiedAt`, source/provenance, value, completeness, certainty and migrations/recovery. Update positively observed changes without deleting a last-known verified value when a later source is unavailable. User's current-login Sessions dashboard still resets at genuine logout; account progression and Bank Memory are **separate persistent stores**. Never ship personal bank/progress records, credentials or tokens in source/release ZIPs.
3. **Synchronization:** Update observed domains when the game/API actually produces a reliable event, provide an explicit **Sync / Refresh Account** control, and support a **guided first-run/re-sync checklist** for screens that must be opened by the player. If tracked information has gone approximately **seven days without a supported refresh or progress observation**, prompt to refresh without claiming the player was inactive. Distinguish `last observed progression change` from `last successful verification` for each domain; if the value is unchanged but the source was checked, update the verification timestamp. Dismiss/snooze reminders so they do not nag continuously. Never erase last-known good state after a failed sync.
4. **Persistent Bank Memory:** On an actual, account-authenticated original/full bank observation, save all safely observable bank item IDs, quantities, variant states/charges where evidenced, source, bank generation and timestamp. Provide a **dedicated, accessible Bank Memory tab/view** where players can inspect what TPT last saw and when. Outfitter and Best in Bank must be able to read it while the bank is closed, but display **last-known / stale / unverified** separately from live-confirmed ownership and must not turn the snapshot into unqualified `READY` or an unsafe automatic withdrawal plan. Bank placeholders, noted forms, charged variants, shared group storage and unknown/unloaded widgets require explicit handling; filtered temporary Bank O tabs must **not** overwrite the authoritative full-bank snapshot. Refresh on a genuine full bank scan; player controls bank withdrawals.
5. **Navigation conflict requiring owner clarification at UI integration:** Historical design locks exactly four *primary* sidebar tabs and no native-bank O tab in sidebar. On 2026-11-03 the owner **explicitly requested another visible Bank Memory tab**. This later request **must not be discarded**. For backend work, implement Bank Memory as an independently reachable user-facing feature contract; before changing primary navigation, confirm whether its placement is a new fifth main tab or a named nested tab/subview. Do not silently choose either and do not confuse Bank Memory with the existing temporary Bank Tags/O view.
6. **Acceptance:** Account A -> save -> restart -> read preserved values; account B cannot see A's data; domain-specific guided capture stores only actual observations; outage/partial scan does not clear old data; verified unchanging status updates last-verified time; seven-day prompt and manual sync behave correctly; Bank Memory displays exact latest known original-bank values and age; Outfitter handles stale/unknown correctly; actual native RuneLite behavior independently verified on the exact JDK17 build. **Research and prose do not satisfy acceptance.**

**Related read-only audit contracts:** `audits/WAVE-01-ACCOUNT-MEMORY-READ-ONLY.md` and `audits/WAVE-02-BANK-MEMORY-READ-ONLY.md`. **Only audit one engine per wave.** Wave 2 is not authorized merely because Wave 1 is complete; each repair/implementation wave requires owner approval.

---

## 1 | Authority, scope and how Cursor must work

### 1.1 Strict source-of-truth order

1. Most recent **direct owner decision** for intent, user-visible rules, and approvals.
2. Byte-identified **actual source checkout/archive**, changed-file diff, clean-extract recipe and test receipts for implementation claims.
3. Independent, nonauthor QA with narrowly described scope; owner native screenshots/logs for only what they show.
4. Canonical, current BUR ticket and RSR dossier registers, where available. Do **not** invent or allocate IDs from a stale `next_unused_id`.
5. Source-pinned OSRS Wiki primary pages `/Strategies`, item/equipment/quest/boss pages and exact screenshots/oldids for game mechanics.
6. Staged research, crew handoffs, compendia, historical plans, concepts and mockups. These inform decisions but do not activate new gameplay rules.

If two sources disagree, list **what, where, when, why, confidence**. New owner decisions win for design, but a user statement about a successful in-game action is proof of that scoped observation, not of every internal algorithm. Never silently rewrite history to make versions consistent.

### 1.2 Evidence labels and done criteria

- `OWNER_NATIVE_SCOPED`: specific witnessed RuneLite Windows/game action.
- `SOURCE_INSPECTED`: verified source line or archive relationship; not runtime behavior.
- `OFFLINE_TESTED`: synthetic/isolated tests, with actual compiler/runtime noted.
- `RESEARCHED_UNVERIFIED` / `RESEARCH_GEAR` / `CHECK`: knowledge not approved for production decisions.
- `UNKNOWN` / `UNAVAILABLE`: no supported observation; not falsity or zero.
- `PASS_SCOPED`, `REWORK`, `BLOCKED`: bounded QA/intake outcomes.
- `RELEASE_HOLD`: always until explicit owner authorization and all relevant gates pass.

One task may move to `CLOSED` only with exact code/file evidence, tests and a scope-correct approval record. A single UI screenshot never proves the underlying bank, KC, quest, or inventory data pipeline. Research files do not automatically create tickets or approved equipment entries.

### 1.3 Cursor guardrails

- First inventory the current repository/worktree and show the exact branch, commit, source ZIP hash, modified files and applicable build scripts. Do not assume the user's Cursor checkout equals this context pack.
- Favor focused, reversible patches. Preserve the latest verified source and maintain a rollback. Do not replace new code with historical CP9/R13/Build 20 material.
- One writer per overlapping high-risk source file: particularly `BurrowPlugin.java`, `BurrowPanel.java`, bank adapter and source catalog. Parallel work is allowed for independent research, tests, artifacts and non-overlapping paths.
- Before fixing a bug, state the actual reproduction and the **first relevant error/diagnostic**, not a pile of generic smoke tests.
- Run native **JDK 17 Gradle** tests and a real RuneLite launch when available. `javac --release 17` under JDK21 with stub JUnit has its own limited status.
- No plugin publishing, automated withdrawal, account modification, GitHub writes, self-assigned reviewer approval or mutable canonical ticket edits without explicit relevant authorization.
- Provide concise result-first owner updates with exactly changed behavior, acceptance evidence and pending blockers. Keep screenshots/step-by-step native Windows tests targeted, not bloated.

---

## 2 | Product philosophy and player experience

TPT should feel like an **intelligent field terminal** that remembers the player's actual context, not a static checklist, not a collection of Wiki embeds, not a full-screen app, not a fake animated statistic dashboard. Information architecture is organized around *NOW → NEXT → PREPARE → PROVE*.

- **NOW/Home:** the current account context, recent verified milestone and useful quick action.
- **NEXT/Pathfinder:** one achievable step in a real quest/skill/unlock/gear dependency chain, with a reason, time/attention fit and realistic travel/setup cost.
- **PREPARE/Outfitter:** choose boss, genuine fighting style/method, source mode and tier. Show a coherent trip with 11 worn slots, 28 inventory positions, required supplies and missing items explicitly distinguished as optional vs hard blockers. Explain *why* this version is recommended.
- **PROVE/Sessions:** what actually happened since the latest confirmed login, plus separately archived last 10 completed sessions and accurate provenance of XP/KC/drops/supplies.

Design for Ironmen first but remain useful to Mains. For an Ironman, the app should recognize practical obtainable substitutes and sidegrades (e.g., crystal bow, rune crossbow, Hunter's sunlight crossbow, blessed d'hide, Hueycoatl hide, Bloodbark, Swampbark) instead of always requiring expensive meta items. Do not hardcode a single player's bank or progression into generic product behavior. Explain what a player *can actually do* and what to unlock next.

**Product promise:** individualized, evidence-backed planning, not omniscience. Explicitly show `Source`, `Observed`, `Proposed`, `Needs verification`, `Unavailable` where relevant. Meaningful live-looking bars are fine only when backed by actual data.

---

## 3 | Locked visual system, interaction and navigation

### 3.1 Permanent structural constraints

| Aspect | Contract |
|---|---|
| Main tabs | Exactly `Home`, `Pathfinder`, `Outfitter`, `Sessions` |
| Nominal sidebar | **225 px** wide with vertical scrollbar; fit RuneLite side panel rather than desktop columns |
| Additional screens | Boss Chronicle subview/modal accessible from Home/Outfitter; Settings via compact header cog; no fifth tab |
| Native bank | Small green/crest `O` in bank header opens temporary TPT preparation tab; **not** a sidebar Bank tab |
| Equipment | **11 physical positions** matching the familiar worn interface; actual item sprite by RuneLite item ID |
| Backpack | **4 columns × 7 rows = 28 positions**; row-major and slot-stable; never rotate into 7 columns × 4 rows |
| Selectors | Boss list with five visible scroll rows; boss icon/name; category, search and grounded sort; separate method and style |
| Style buttons | Melee red / Ranged green / Magic blue; use sword, Magic shortbow, spellbook/staff sprites where available |
| Mode selector | `Wiki Recommended` and `Best in Bank` are **different from** `Entry / Recommended / Optimal` tiers |
| Typography | Compact readable hierarchy; safe tooltips and scrolling; user-controlled font size and color preferences |
| Assets | Actual OSRS icons for items/skills/bosses; mascot art is custom and small, not substitute for gameplay sprites |

**Palette reference family:** `#0D1112` graphite, `#1E1D1A` dark olive charcoal, `#282828` neutral, `#5C4B30` muted bronze, `#F5B141` accent amber, `#F0E6CF` warm text. This is the approved restrained direction, not a rule to fill every surface with gold. Contrast, text wrapping, hitboxes and motion matter more than decorative metallic frames.

**Mascot:** small friendly-feral armored raccoon in OSRS-adjacent pixel style. Compact header crest, contextual illustrations only where they earn their space. Do not resurrect the rejected oversized/chubby raccoon, massive header or one giant 4-column fantasy render. Feral Enterprises and product name should read clearly, but preserve viewport room for real information. Tagline may live in About/tooltip when it cannot fit.

**Accepted visual precedence:** owner-approved original graphite/bronze Burrow Pathfinder screenshot for color; `04_HOME_ARMORY_WORKBENCH.png` for Home density; `05_PATHFINDER_DEPENDENCIES.png` for dependencies; `02_OUTFITTER_FIELD_TERMINAL_LAYOUT_TOP.png` and `03_OUTFITTER_ARMORY_EQUIPMENT_SWITCH_PACK.png` for Outfitter; `06_SESSIONS_ARMORY_WORKBENCH.png` / `07_SESSIONS_DETAIL_AND_INTEGRITY.png` for Sessions; custom selected OSRS-style raccoon reference. This precedence comes from `references/01_COPY_PASTE_INTO_NEW_CHAT.md`. Do not assume artist mockups with 7×4 inventory or a fifth Boss Chronicle tab override explicit rules.

**UI shell state:** Standalone Java Swing shell iterations R01, R02/W01, R02/W02 were designed separately from production to demonstrate player-facing interactions and large phone-readable renders. Treat as visual/reference candidates, not integrated product behavior. The user specifically asked to preserve the underlying plugin code while designing the shell in waves. Controls in real native Java must be genuinely interactive; a screenshot or decorative HTML button is not sufficient. Any integration should be scoped and explicitly approved.

### 3.2 Boss selector and Chronicle isolation

- One compact boss picker component family; list displays **five entries** at a time with internal scrolling; optional search and category (Regular/Wilderness/Moons/Demi-bosses), sort `A→Z`, `Z→A`, `difficulty easiest→hardest` and reverse using grounded approximate difficulty instead of fabricated player readiness.
- Outfitter boss/method/tier/combat style/gear source/equipment selection stays untouched when Chronicle prev/next, boss dropdown or research navigation changes.
- Chronicle own boss selection independent of Outfitter selection. Back returns to the prior Outfitter state with scroll position where possible.
- Chronicle has overview, actual personal KC provenance, rare-drop math/reference, Combat Achievement info, preparation/switch quick tips, and a clear path back. **Not a main tab.**

### 3.3 Every control needs behavior

Selectors select; checkboxes alter goal state without marking unseen completion; click targets and hover state match their painted bounds; normal wheel scroll works; compact tooltips stay in screen; settings persist per account; equipment icons show understandable quantity/status/variant information. A filter or style button must change the corresponding strategy only when real data exists, otherwise display disabled/unavailable and why. Avoid repeated headers and repeated wording.

---

## 4 | Home: armory workbench / mission console

**Question:** “What is the single most useful thing to do next?”

Required content in 225 px:

1. Compact current account/mode and current encounter/weapon context, with separate verified-versus-inferred indicators.
2. **NEXT**: currently selected Pathfinder goal, immediately feasible prerequisite, time/attention estimate and why it is recommended.
3. **LAST**: last verified meaningful milestone (boss drop, quest completion, gear or unlocked activity), not necessarily the last raw event.
4. Compact activity ideas for **Skilling**, **Combat/Slayer** and **Quests/Diaries** grounded in current player state; optional real weekly XP/milestone highlight.
5. Quick launch for Outfitter, Sessions and **Boss Chronicle**.
6. Subtle live progress/bars, never fabricated percent or timer.

Do not make Home a clone of RuneLite skill tab or a massive stacked diary. Returning from Pathfinder should retain the selected goal. Returning from Chronicle cannot overwrite the selected Outfitter boss. Unknown goals remain visibly unknown, not complete.

---

## 5 | Pathfinder: route intelligence and player agency

### 5.1 Functional contract

- Goals, prerequisites and dependencies must be **real choices and real state**: quest status, levels, skills, unlocks, equipment feasibility, diary state where observed, item/task requirements, travel and setup time, and occasional cooldowns.
- Player chooses a goal, priority, skip, inspect and checkboxes; **inspect vs select** must be separate. A checked *intention* is never proof of an in-game completion.
- Show authentic activity icons; duration chips and attention (`Active`, `Low`, `AFK`); make it easier to plan 15/30/45/60/90/120 minutes. Goals can span Skilling, Combat/Slayer and Quests/Diaries.
- A possible old rule inventory has **393 structured rules**, including **91 AFK** and **35 money** ideas. It is a historical count, not proof of every rule in current native code or a full OSRS quest guide. Earlier inventory mentioned 215 quest-order entries and 213 mapped quests, not a validated entire guide.
- Set up `Plan My Session`: player selects time, attention and a goal; the planner accounts for actual setup/travel costs, available gear and prerequisites. Never claim an unreachable task is `READY` simply because an account level is high.
- In suggestions, avoid simplistic score inflation based on long AFK time/levels. Travel/setup unknown must not silently upgrade to READY. Explain filters, hard reasons, optional improvements and achievable alternative steps.
- Quest/diary or bank unknown should downgrade certainty, never fabricate a negative or success. Some requirements need manual source-backed confirmation.
- Support player preference sorting, clean checkboxes, undo/skip and persistence. Save goal state per account and version migrations safely.
- Skills, quests and diary completion should use actual RuneLite/game state when possible; click or checkbox alone does not authorize completion. Session observation can suggest replan or review, not auto-complete the goal.

### 5.2 Activity intelligence W04 and W06

Wave 4 staged **44** typed activity records including Farming guild crop gate differences, shared minigame teleport cooldown and clue-emote item requirements. They were all `CHECK`, with **zero approved production rules**. Wave 6 wired a verified-only activity prerequisite gate that can downgrade unsupported READY results; the W04 CHECK dataset is not allowed to activate gameplay recommendations without review.

### 5.3 Practical acceptance examples

- Player selects a quest goal; Home NEXT shows nearest genuinely reachable step and real prereq; reset does not mark quest done.
- Missing observation of diary/quest or unavailable bank yields VERIFY/UNKNOWN rather than READY.
- Setting 30 minutes/Low excludes activities whose travel/setup exceeds available time; missing travel estimate remains unknown.
- A logged actual milestone can update progress or highlight, but no phantom achievement from clicking a checkbox.

---

## 6 | Outfitter: encounter field terminal

### 6.1 Selection hierarchy and sources

**Five distinct conceptual steps**, without conflation:

1. **Boss** and category, via five-visible-row picker; own real OSRS boss sprite.
2. **Combat style**: Melee, Ranged, Magic buttons; active/available state. Hybrid applies only when the actual method uses multiple styles.
3. **Method**: named source strategy, shown when true alternatives exist, e.g. Giant Mole Dharok method, DK single-target/tribrid. A boss cannot inherit another boss's method or gear.
4. **Tier**: `Entry`, `Recommended`, `Optimal` **only if sourced**. If selected tier has no gear, offer an actually documented tier with clear explanation; never invent a full high-tier kit.
5. **Source mode**: `Wiki Recommended` versus `Best in Bank`; these are separate from tier and style and must persist correctly.

Show verdict with ordered, useful reasons; 11 worn item cells; 28 fixed inventory positions; exact known stack counts and notes; actual physical weapon/ammo compatibility and reasonable coherent whole-trip layout. Add source citation/date, bank evidence freshness, per-item ownership and upgrade explanations without flooding the 225px sidebar. Separate mandatory mechanic, optional upgrade, missing bank data and missing strategy research.

### 6.2 Encounter scope and exclusions

**Targeted included:** repeatable regular standalone bosses, God Wars, Barrows, Dagannoth Kings, three Perilous Moons, and supported Demi-bosses Tormented Demons, Demonic Gorillas, Lizardman Shamans. Boss discovery/CA research coverage may list more encounters than available certified strategies.

**Excluded from normal released Outfitter gear catalog under older locked owner policy:** raids (CoX, ToB, ToA, any later raid unless approved), Jad/Fight Caves, Zuk/Inferno, Colosseum, Gauntlet Hunllefs, one-off quest bosses, certain minigame encounters like Tempoross/Wintertodt/Zalcano and specific excluded Revenant boss entries. Do not interpret an R31 research row for these as release authorization.

**Wilderness:** all **11** previously flagged encounters and KBD Wilderness travel need visible risk warnings; Best in Bank is denied **everywhere**: UI toggle, planner, memoized plan, preload, temporary bank layout, automation. Read-only boss wiki or rare info may be visible with warnings. Default-deny when wilderness identity is unclear.

**Counts (distinct denominators):** older curated catalog **51** original gear profiles; R31 discovery **61** boss encounters; W07/W075 style coverage **60 × 3 = 180** selectable boss/style pairs; 75 source-catalog paths, 12 provisional gear previews, 4 strategy-only, 89 missing after W075. These are not 180 approved loadouts or necessarily 61 fully supported kits. W0751 tests assert **51 original profiles + 2 Brutus plans + 10 research drafts + 6 demon encounter drafts = 69** before a separately named Build 20 expansion. Count categories can overlap other planning inventories; never sum unrelated counts.

### 6.3 Gear intelligence: TPT's differentiator

Understand **OSRS gear as an item-ID-and-state graph**, not merely a shortlist of Wiki best-in-slot icons. Requirements:

- Raw exact `itemId` and item identity, equip slot(s), active/charged/degraded/broken/corrupted/ornamented/recolored state, noted/placeholder status, world/account context, and whether equip requirements are met.
- Functional families and **directional** transforms; no universal boolean `equivalent`. Context can include `STATS_ONLY`, `EQUIP_SLOT`, `COMBAT_EFFECT`, `SET_EFFECT`, `GOD_PROTECTION`, `TELEPORT`, `ITEM_USE`, `ECONOMIC` and tactic-specific predicates.
- Player needs recognizable variants: blessed god d'hide colors, Hueycoatl hide/bracers, Bloodbark, Swampbark, Hunter's sunlight crossbow and antler bolts, different Slayer helms and black mask states, imbued rings, charged glories, Barrows/Moons breakdown states, corrupt/charged bow families, ordinary vs dragon bolt compatibility.
- Treat **god-affiliated gear** as potentially mechanic-different despite same stats, especially God Wars protection.
- Treat **set effects** according to actual method and particular piece mechanics, not `broken => everything off` or `one missing piece => all content blocked`.
- Required combat/defense levels, quest/favor/unlock gates, ammo, spellbook, spell rune consumption and charges are first-class. A crafting requirement is NOT automatically equip requirement.
- Do not infer ownership from cosmetic/upgrade relationships. Bank placeholder is not stock; noted item cannot always be equipped without unnoting; charge counts matter.

**Specific research-gate examples (not permission to bulk activate rules):** Hunter's sunlight crossbow (ID **28869**) recorded with **66 Ranged + 50 Hunter** to equip, antler ammo rather than ordinary crossbow bolts; Masori body (f) (ID **27238**) recorded with **80 Ranged + 80 Defence** to equip; related fletching/crafting levels are fabrication/upgrading, not equip gates. Pending W01 variant rows omitted Hunter and Ranged constraints and need independent rework before implementation. Some broken Blood Moon effects may behave differently from zero-state Barrows; pin primary sources and independently test before encoding.

### 6.4 Whole-trip Best in Bank algorithm contract

Input is **authenticated current-account evidence**, not an unscoped ID set: login epoch, bank original/full read/generation, carried equipment and inventory, known item states/charges/quantities, actual source mode/method/style/tier, snapshot timestamp/freshness/lease. Unavailable or hidden original bank widget during Bank Tags O operation is not empty bank stock.

Decision pipeline must:

1. Filter boss safety and research provenance first: deny Wilderness and research-only previews from BIB/Bank O.
2. Select the actual boss-specific source profile for style/method/tier; allow lower owned tier or compatible alternate when certified.
3. Resolve feasible player-owned exact items with slot/level/quest/charge/ammo/spellbook and transformation rules.
4. Treat only mechanic protection, functional weapon/projectile/cast resources and method-required complete set as **hard blockers**. Other absent luxury items and optional blessings/offhands/gear are suggestions, not STOP.
5. Handle 1H/2H and shield exclusivity, real occupied slots, inventory switch items actually stocked, per-stack quantities, set-piece count, teleport charges, GWD god protection and task-only protections.
6. Rank coherent **whole trips** before individual expensive items: truly usable first, fewer empty hard-requirement slots, explain ties. Avoid claiming exact DPS/max-hit without reliable stats and model.
7. Emit `READY`, `VERIFY`, `BLOCKED`, `UNKNOWN` (legacy `NOT_READY` internal may exist) with the precise reason, what evidence was needed, and what item/upgrade fixes it; do not turn unknown into negative stock.
8. Include resolved **11-slot worn** and **28-cell inventory** positions, exact expected quantities/what to withdraw, source generation and bank-only quantities. Player withdraws manually.

**CP1/CP6 documented behavior intent:** missing nonmechanic offhand/Rada blessing should not block otherwise valid lower-tier owned weapon; set effect only enforced when a sourced method explicitly needs it; task protection blockers include dragonfire/wyvern shield, face mask for smoke/dust, earmuffs/nose peg/spiny helm/insulated boots/mirror shield/witchwood icon and complete Shayzien for Lizardmen where required. This is a **design/evidence policy**, not license to assert all edge cases currently pass.

**Known pitfalls fixed/repaired in later W07 source (needs native verification):**

- Kree'arra Tbow profile missing arrows; W07 adds arrow source but trip quantity remains VERIFY, not one-arrow READY.
- Leviathan rune crossbow incorrectly paired with **Ruby dragon bolts (e)**; W07 substitutes ordinary **Ruby bolts (e)** and W0751 updates the regression oracle.
- Source switch items absent from bank were previously counted as if physically present; W07 corrects pack/switch calculation.
- One-charge glory incorrectly satisfied six-charge teleport requirement; W07 avoids this substitution.
- Bank O could show physically owned but level/quest-unwearable withdrawals as ready; W07 gates that display.

### 6.5 New combat styles W07, W075, W0751

W07 style buttons changed from merely visual selection to method-specific gear when source-backed. Dagannoth Rex Magic / Prime Ranged / Supreme Melee are clear examples. If unsupported, show meaningful unavailable/strategy-only rather than borrowing an unrelated loadout.

W075 added these **provisional** plans (always `RESEARCH_GEAR`, `CHECK`, `Recommended` only, excluded from BIB/Bank O):

| Boss/style | Proposed plan | Prereq/limitation |
|---|---|---|
| Scurrius Ranged | Bone shortbow **28794** with rune arrows **892** | 50 Ranged and a Scurrius spine; **not** available for pre-spine attempt; inventory counts illustrative |
| Scurrius Magic | Bone staff **28796** | 50 Magic and stored chaos charges; rat-specific attack; loose chaos runes alone do not prove charged weapon |
| Giant Mole Earth Magic | Earth Bolt (29 Magic), staff of earth **1385**, **200 air + 100 chaos** for hypothetical 100 casts | Earth weakness sourced, specific trip not full validated strategy; light **lit lantern 4550**, spade where required; fire unlock may bypass light requirement |

Earlier W07 included provisional Brutus, Obor, Bryophyta and Giant Mole Bowfa ranged examples; W075 revises key semantics: first Obor giant key / Bryophyta mossy key unlocks lair, ordinary repeat kills do not require an extra key; optional chest claims may consume one. Inventory images and hypothetical 100-cast quantities are demonstrations, **not** certified packs.

**Status W0751:** no gameplay source changes vs W075; tests updated to reflect these existing three provisional research gear paths while preserving safety gates. Owner's W075 Windows Gradle reported **676 tests, 11 failed**; W0751 source-compatible local replays passed 11 focused failures + six additional test suites, but **full genuine Windows JDK17 Gradle remains pending**. Never call this full regression PASS or live RuneLite PASS.

### 6.6 Inventory switch guidance and actual position

For TD/Gorillas and hybrid bosses, visually indicate preferred player switch lanes, including Scorching Bow, Emberlight and defender, matching **the real 4×7 inventory slots**. This is a QoL overlay, not an inventory move or interaction. If a switch item is unavailable, do not render it as a physical packed item. Compact tips explain which attack/defense style/weapon changes and the intended method without implying TPT fights for the player.

### 6.7 Boss loot table, rare drops and Collection Log

- Distinct **boss-attributed loot panel** near Outfitter, not RuneLite Loot Tracker clone; item sprite, source-labeled drop, quantity and possibly value with price source/date.
- Drop-rate reference and actual KC must not confuse `p per kill` with cumulative `1 - (1-p)^n`. Separate historical verified KC, current observed KC, manual/hypothetical KC. Multiple outcomes/roll groups depend on their actual joint/conditional semantics; neither simple independent multiplication nor guaranteed drop is always applicable.
- 50/75/90/95% probability milestones are explanatory, never a promised drop by kill N. Collection-log unlock is not proof of current bank possession.
- Rare targets/ANY/ALL aggregations require mathematically valid roll group dependencies and current Wiki source. Source rates/guarantees/pre-rolls matter.
- An OSRS-native icon should represent the actual item, not an unrelated generic drop-rate symbol.

---

## 7 | Bank O: manual bank-workspace contract

**User-visible purpose:** press small `O` in the native bank to open a **temporary “trashpanda commander” Bank Tags tab** representing current planned gear and supplies in meaningful, correctly ordered slots. Equipment left/supplies right where applicable, with original 28 inventory positions preserved and never compacted. **No automated withdrawal.**

- Observe a full original bank safely upon actual bank-open; avoid treating temporary filtered Bank Tags contents as entire bank. Initial bank may be hidden/transient; `UNKNOWN/HOLD` is appropriate until complete authenticated original read.
- Bind bank snapshot to account ID, login epoch, generation and lease. On temporary tab activation preserve a valid frozen read while manual withdrawals occur. Invalidate only on supported real evidence change, not transient widget disappearance or tab switching. Reject stale or cross-account data.
- Green `O` button lifecycle: reliably visible when bank opens, remains discoverable through header rebuilds/tab switches, disappears/cleans up correctly on close, logout, account switch and plugin shutdown. Do not remove other players'/plugins' bank tags.
- Scans automatic on bank open; manual `Scan Bank` remains possible; bank highlighting **OFF** by default. Distinguish bank, inventory, worn and combined owned/required quantities.
- Preserve evidence through temporary tab operation but do not freeze stale stock indefinitely. Account switch, bank exit, genuine stock change and new capture must get correct lease transitions.
- Layout must use a **single resolved plan** across Outfitter preview and O, and suppress unwearable/unverified/research-only items as ready withdrawals.
- Known reported owner success: **four consecutive manual withdrawals** with O staying open in Bank R05/Build38. **Unproven:** full account A→B isolation, logout/relog, filtered-bank read vs original, quantity and note/charge edge cases, revalidation after O exit, true native acceptance of W07/W075 descendants.

---

## 8 | Sessions: authoritative current-login ledger

**Core distinction:** live `Sessions` = **current login only**; separate newest-first **last 10 completed logins** per authenticated account. On **confirmed** logout: atomically archive completed summary (if appropriate) then clear current live immediately. Same-account world hop, teleport, temporary LOADING, map reload are **not** a new login. Crash/interrupted event requires its own status, not a fabricated completed session.

### 8.1 Observation truth matrix

| Signal | Can show | Cannot assert |
|---|---|---|
| RuneLite authenticated XP deltas after trusted baseline | Actually observed current-login XP by skill | Unknown loading gaps as earned XP; quest success |
| Real Boss Kill Log / parsed trusted absolute game message | Historical/lifetime KC baseline; later exact source-valid delta | First absolute value as new session KC |
| RuneLite Loot Tracker `LootReceived` | *Loot-confirmed encounter estimate* with dedupe/provenance | Verified boss KC or one-loot-event-one-kill for all bosses |
| NPC/server item loot event | Source-specific attributable drops | Kill count without independent witness; minion drop as boss drop |
| Collection Log page | Actually observed unlock on scanned page | Bank ownership, complete CL coverage, current-session kill |
| Inventory/equipment state | Items held/worn at the read | Consumables used; transfers/decants/charges/death without event semantics |

**Native source clues previously demonstrated by owner:** a **ring of wealth** opens Boss Kill Log; an **enchanted gem** opens Slayer Kill Log (and Slayer ring can access relevant log). These are different source categories. Accessing a game dialog via manual action is not equivalent to automatically having a RuneLite adapter. For Commander Zilyana, owner screenshot showed **3 lifetime KC**, while Sessions KC can correctly remain **unverified** until second/source-valid delta. R03-era code had missed her boss-name alias in `RuneLiteBossKcAdapter` and boss-drop aliases in `IronLedgerLootIntake`; that is repair work, not assumed finished.

**XP weekly vs completed logins:** example owner Nov 2 weekly **62,087 XP** versus four completed cards total **50,736 XP**, difference **11,351 XP**. Five +0 XP completed cards were also identified across Nov 1–2. Different scope/time windows and unavailable source conditions must be reconciled, not forcibly made identical by rewriting totals or claiming zero gain. Weekly can include live sessions and prior completed sessions outside rolling ten.

**Supply consumption:** `SessionSupplyUsed` model existed in earlier source but no proven production emitter. Until real observed consumption evidence is available, show **“Supply tracking unavailable”** rather than `0 consumed`. Potion doses, food, ammunition, charges, banking, gear swap, drops, deaths and replenishment need distinct event semantics and anti-double-counting.

**Sessions UI:** total XP by skill, verified boss KC changes, loot-confirmed *estimates* separately, boss-attributed drops, confirmed supply consumption if available, current-login event tape, meaningful recent ten archived cards and weekly comparison; `Plan My Session` and replan options must distinguish selected activity from actually executed activity. Do not seed synthetic activity or imply an action merely because a plan was selected.

---

## 9 | Boss Chronicle, achievements, drop odds

**Chronicle is not the fifth tab.** A boss narrative and research/reference panel with its *independent* own browse/dropdown controls, sourced tips and provenance-labelled personal evidence. Requirements:

- Prev/next and five-row dropdown navigate Chronicle only. Outfitter boss, method, tier, style and source untouched.
- Boss-specific strategy details, practical gear and prep notes, optional conditions, compact Combat Achievements and drop targets.
- Older CP9 had **290 combat task records across 51 groups**, **77 nonempty quick tips and links**, remaining **213 requirement-only**. Wave 05 staged these **290 as CHECK**, not universal verified completions. Synthetic BQA-906 placeholder exports (652 records) quarantined/rejected.
- Wave 06 wired exact boss/name/tier crosswalk for approved conditions but unverified content must visibly read **RESEARCH CHECK**. No CA completion status unless actually observed. New aggregate Wiki counts in old wave reports are historical, not a substitute for source-reconciled current individual tasks.
- Source-link and recheck CA tips in five-boss batches. No guessed requirements, impossible mechanics or assuming a screenshot shows a completed task. E.g. Zilyana task list can be larger than the five featured quick tips.

---

## 10 | Research program: immutable R31, staged R32, GEAR-W01 and W02

### 10.1 R31 and R32 separation

- R31 historical immutable `BURROW-ALL-RESEARCH-SAVE-R31-2026-11-01.zip` SHA256 `625f8244dd4f86dcef7fed35b6b94fae9ae2678d5f70fbc662cb591e3a81cfdc`. Nested dispatch `BURROW-RESEARCH-DISPATCH-R31.zip` SHA256 `9796a0fc035c2aacb28e76932cb6f79fbc6df67db7bf75bfa849113751bb0a26`.
- **71 RSR tickets**: **61 boss encounter rows + 10 cross-cutting**. **31** `RESEARCHED_UNVERIFIED` and **40** `READY_RESEARCH` at saved snapshot; all 61 boss entries **owner_approved=NO**. This is preserved research, not 61 valid ready-to-bank kits.
- R32 isolated staged derivative, earlier audited dispatch SHA256 `36176521201338156334898582ee12077baa929fe42bf8c3846ef327440b1d0a`. No overwrite of R31, and no automatic promotion to source catalog.
- R32 scopes: KBD `RSR-0045` 41/48 scoped pass including project/policy outcomes; Moons `RSR-0056/57/58` 27/34 scoped; Maggot King `RSR-0029` 35/42; Kree'arra `RSR-0020` 32/42. **Scoped pass ≠ accepted loadout.** Thermonuclear Smoke Devil `RSR-0042` awaits Spoons intake; Kraken `RSR-0039` Tongs recheck; Brutus `RSR-0002` narrow corrections/review. Most original 28-position inventory images, quantities, variants, revision pins and independent source verification remain open.
- Do not mistake R31 71 research tickets, 61 encounter matrix, older 51 software profiles, W07 style metadata, 224 review queue or CA 290 records as interchangeable counts.

### 10.2 Source transcription rules (critical)

- OSRS Wiki `/Strategies` primary source; source URL plus UTC read date and ideally `oldid`/revision. Cite exact strategy **column**, ordering and source image. Preserve literal order and separator semantics `>` versus `/`.
- Equipment: exactly known 11 gear slots and item ID / charged state / optionality; inventories **28 cells**, with source image only when actually seen. Item icon identity, potion doses, rune-pouch contents and stack quantities only if visible or otherwise explicitly sourced. Use `x?`, `CHECK`, `UNKNOWN` for unseen.
- Do not fill empty source cells with probable food or raid potions. If source screenshot says uncertain, do not represent as certain.
- Avoid copyright/licensing assumptions: Wiki data provenance is not permission to redistribute all original images as owned art; item icons and mascot art need proper rights/review.
- Secondary corroboration: `ironman.guide`, OSRS Wiki Ironman guide, Mootrius, `osrsbestinslot.com`, specialty guides. These never replace primary conflicting Wiki strategies automatically. Research external calculators before trusting outputs.
- Separately track strategy source coverage vs implementation coverage vs QA vs owner approval. No guessed quantities, equip requirements, item IDs, drop probabilities or complete kit counts.

### 10.3 GEAR-W01 intake state

**All four producer ZIPs received and integrity checked**, but **zero new gameplay item/variant relations implementation-approved** in the latest Spoons R02. Cumulative indexed research: **121 claim rows; 117 item references; 80 distinct numeric IDs; 70 proposed relations; only 25 limited independent L1 item ID spotchecks; 68 synthetic expected Best in Bank scenarios unexecuted**.

- `GEAR-CORE-01` Noodles equipment catalog: 46 rows, 15 relations, 11 claims; verify remaining 16 researched and five CHECK IDs, RuneLite cache, equipability.
- `GEAR-CORE-02` Sporks state/variant directed graph: 45 references, 34 relations/claims; **22 provisional, 7 REWORK, 5 CHECK**. Correct Hunter gate for 28869, 80 Ranged for Masori (f), and source numeric evidence. Keep historical R01 immutable.
- `GEAR-CORE-03` Tongs mechanic audit: 52 claims; 39 scoped corroborated, 13 need deeper verification; 35 negative cases unrun. Tongs cannot self-certify own research; independent L2 needed.
- `GEAR-CORE-04` Noodles BIB typed policy: 24 claims, 17 IDs, 21 relations and 68 **design fixture** outcomes; requires account/session comparator, nullable bank unknown, exact withdrawal quantities/generation, typed charged states and actual executable adapter. `chronicleIndependent=true` in a data record is not proof of UI isolation.
- W02 gear resource integrated as quarantined **80 IDs / 9 typed relations / 0 approved gameplay rows**. W04 activity 44 records CHECK; W05 CA 290 legacy CHECK. W06 binds verified-only gates, does not magically approve any of these datasets.

### 10.4 Canonical ticket safeguards

The last separately retrieved BUR master pair was from October 30: `BURROW-MASTER.json` `BUR-SYNC-0020-R20`; `BURROW-TICKETS.json` had **34 unique BUR-0001..0034** and a status mismatch (`latest_sync=R19` vs `last_sync=R20`). Another owner conversation references ticket 35 in progress, so **do not create/reuse BUR-0035** without reading a fresher canonical register. Historical `BUR-0016` covers imbued berserker ring and glory(6) variants; `BUR-0030` whole-owned gear resolver. W01 research overlap does not close these automatically.

---

## 11 | Software/architecture map and current source design

### 11.1 Observed Java/Gradle layout from W075 archive

The bundled W075 ZIP is a **Windows-buildable Java/Gradle RuneLite source project**, not a precompiled signed release JAR. Root scripts:

```text
00-BUILD-AND-TEST-BOTH.cmd        # Genuine full-test entrypoint on owner's Windows/JDK17
00-START-BOTH-NO-REPEAT-QA.cmd   # Fast one-client dual-plugin dev launcher
01-BUILD-AND-LAUNCH-BOTH.cmd    # Older full test-and-run route
Project-Burrow/build.gradle
Project-Burrow/settings.gradle
Project-Burrow/gradlew.bat
Project-Burrow/src/main/java/gg/feral/burrow/
Project-Burrow/src/main/resources/
Project-Burrow/src/test/java/
Project-Burrow/src/hoverliteDev/java/com/feralenterprises/hoverlite/
```

On developer run, Gradle task `runBoth` loads **`BurrowPlugin.class`** and **`HoverLitePlugin.class`** via RuneLite's built-in plugin mechanism and starts `RuneLite.main(args)` once. Preserve separate plugins and one client; do not turn HoverLite into TPT source code or launch two clients.

**Architecture boundaries** (actual checkout must be inspected):

```text
RuneLite authenticated events / widgets / actual bank / skills / NPC / game logs
                 ↓
runelite adapter(s) + account/session/epoch fences + source-specific watchers
                 ↓
Current verified evidence snapshots (bank, carried, quest/diary, XP, KC, loot)
                 ↓
core domain rules + bib gear knowledge + intelligence research gates
                 ↓
Pathfinder suggestions / Outfitter whole trip / Chronicle reference math
                 ↓
BurrowPanel (Swing sidebar) + Bank O native extension + Sessions view
                 ↓
Safe per-account local history, preference migration and optional export
```

**Boundary rule:** adapters should never silently fabricate observations to satisfy planner expectations. Views should never pass provisional suggestions back as verified owned/equipped records. A copied reference image is not a usable Java UI asset until source/import/licensing is reviewed.

The actual class/module inventory in the W075 ZIP is automatically indexed at `SOURCE_TREE_INDEX.md`; it is more precise than old conceptual module names. Useful known classes include `core/BestBankPlanner.java`, `core/BankObservationCache.java`, `core/BankLayoutEligibility.java`, `core/BankWithdrawalFreezeGate.java`, `core/CompletedSessionHistory.java`, `core/BossKcDeltaTracker.java`, `core/ItemVariantPolicy.java`, `core/BossDropOddsMath.java`, `bib/AmmoRules.java`, `pathfinder/SuggestionEngine.java`, `ui/BurrowPanel.java`, and `runelite/BurrowPlugin.java` (check exact path). Do not assume line numbers from W06 audit remain correct after W07.

### 11.2 Account isolation and data contracts

Retain independent `AccountContext`, login epoch and scope; snapshots record status/freshness, bank generation, provenance and quantity. `AsyncRequestFence` must reject delayed older-account results. A changed bank or account invalidates candidate plans. Persist sessions, goals and settings per authenticated account; do not bundle account credentials, tokens, RuneLite login cookies or personal bank snapshots into shared ZIPs.

### 11.3 Error and provenance design

Preferred machine records should make evidence explicit (illustrative schema, **not** a claim current classes implement it):

```json
{
  "accountScope": "authenticated-current-player",
  "loginEpoch": "opaque-monotonic-session-id",
  "source": "native-original-bank-or-verified-game-message",
  "sourceGeneration": "opaque-generation",
  "observedAt": "ISO-8601",
  "availability": "OBSERVED | UNAVAILABLE | UNKNOWN",
  "confidence": "OWNER_NATIVE_SCOPED | SOURCE_INSPECTED | OFFLINE_TESTED | RESEARCH_GEAR | CHECK",
  "decision": "READY | VERIFY | BLOCKED | UNKNOWN",
  "reasons": ["human-readable evidence or gap"]
}
```

Do not copy this unverified schema into runtime code without checking existing types. The point is preserving independent truth dimensions and source provenance.

---

## 12 | Crew and development protocol

| Role | Responsibility | Boundary |
|---|---|---|
| **Spoons** | L1 intake, project coordinator, cross-ticket reconciliation and results-first owner updates | Does not mint stale IDs or promote unreviewed data |
| **Tongs** | Independent L2 QA, source-mechanics contradiction testing | Cannot certify own authored mechanics |
| **Sporks** | Targeted repairs, item-variant and UI fixes | Preserve owner UI and engine boundaries |
| **Noodles** | Feature development, catalog and Best in Bank mechanics | Scoped patches, explicit source/test receipts |
| **Corporate Wrench** | Senior/final engineering oversight and approval support | Final evidence gate, no automatic release |
| **Bandit** | Feral adversarial reviewer / weird-edge-cases | Findings must be reproduced, not guessed |

Keep permanent ticket namespaces (`BUR-*`, `RSR-*`, `GEAR-CORE-*`, `BQA-*`, `OUTF-W06-*`) distinct. Research-only parallel dispatch is welcome; no concurrent edits of same plugin Java source. Results first, small explicit waves, and **only one combined owner QA** when the owner directs this workflow. A request to move to the next wave does not itself publish a production build.

**Root packaging:** Windows-facing `.cmd` scripts in ZIP root, no stale temporary binaries, no user secrets. One-click launch should locate Java 17, Gradle wrapper, retain correct working dir, build and launch one client; keep test-only script distinct. If Windows Smart App Control blocks scripts, guide inspection/trusted execution, never instruct disabling platform security.

**Owner test handback style:** exact changed files/behavior, reproducible steps, expected result, failure signature and first relevant error/line. No vague “do a smoke test” instructions. Write to Discord/community only with approved artifact/status; unapproved work may be documented transparently as development.

---

## 13 | Version and evidence timeline (do not conflate)

| Date / lineage | Key evidence | Correct status |
|---|---|---|
| Build 19 (older) | Owner launched RuneLite on Windows, four tabs, wheel, real quest scan, XP gain, Tormented Demon loot, logout reset | **Owner scoped native success**; KC and supplies unproven |
| Build 20 WIP | Boss directory expansion, styles, Pathfinder and rare-drop work | Development snapshot only; later sources supersede |
| CP9 Final 2026-11-02 | Archive 798 entries, SHA256 `0b18b8cebdd278d39652eb89c5050af6d5cff94b3ce9b3f848b95ad77f16fcff`; **327/327** shim passes | Offline QA PASS; not native JDK17, nor latest |
| Bank R05 / Build 38 2026-11-02 | SHA256 `711dd92392d63cde669d646e195610e2d0ca12a17683249874f9231e943ca0ec`; owner four manual BIB withdrawals with O tab stable | Narrow owner native positive; other bank/account edge cases OPEN |
| W01–W03 intelligence | Confidence model, item/variant graph, source-gated BIB hook; all new research quarantined | Development candidate, not gameplay rules approved |
| W04 activities | 44 activity CHECK rows including Farming/teleport/clue; source quarantined | Development only, no live activation |
| W05 CA | 290 legacy CA CHECK, 77 tips, placeholder export rejected | Development only, no blanket tip validation |
| W06 2026-11-02 | Verified-only Pathfinder/Chronicle gateways; SHA256 `2d65bf89eaa1b4058a3c903c212b1867158ee4d37fc7a828af1b576e63f01c7e`; 423 entries | Offline candidate, six Outfitter issues audited |
| W07 | Gear-backed style methods, source-pair fixes, real-switch budgeting; 60×3 coverage | Offline source check, native QA pending |
| W075 | Adds Scurrius Ranged/Magic and Mole Earth-Magic provisional kits, key semantics | **Bundled latest materialized production-source lineage**; owner native QA not established |
| W075 owner Windows | JDK17 Gradle `:test`: 676 completed / 11 failures reported | Full suite not green |
| **W0751** (described 2026-11-03) | Ten tests + two root banner scripts corrected, production source **byte-identical to W075**; 11 focused local replays and six suites passed with Java21/`--release 17` tooling | Latest **described** test checkpoint; **no full W0751 ZIP in this pack**, genuine full JDK17/RuneLite gates remain OPEN |
| UI shell R02/W02 | Standalone visual/native Java mock iteration with phone-size screenshots | Separate concept/shell, **not** integrated/approved production |

**Historical GitHub:** `feralenterprises/Burrow` approved public repository. A prior Build38 source snapshot and project-branding docs were reported pushed to `main`; latest W07/W075/W0751 promotion is **not** thereby established. Confirm actual HEAD/PR status before edits or Plugin Hub submission.

---

## 14 | Known bugs, risks and missing functionality by priority

### P0: before claiming current build stable

0. **Mandatory owner feature audit:** inspect actual account data capture/persistence/first-run guided sync, seven-day refresh and the entire saved Bank Memory pipeline; do not mark this covered just because there is a Sessions archive or one ephemeral bank scan. See §0A and the dedicated engine audit contracts. Investigate implementation versus historical roadmap claims, per-domain freshness, account separation, and restart/crash behavior. A missing implementation is a release blocker.
1. Obtain exact current owner-installed JAR/source archive/checkout commit, hash and clean previous rollback. Reconcile W0751 pack against W075 tests without changing production source.
2. Run **authentic Windows Java 17 Gradle full suite** on W0751 and one RuneLite client with TPT + appropriate preserved HoverLite version; report all test failures with first assertion, no imaginary fixes.
3. Test Bank R05 lifecycle inherited by latest code: original-bank read vs temporary O tab, manual 4 withdrawals, bank closure/reopen, tab switch, stale cache invalidation, account A→B→A, logout, missing widgets, quality of actual quantity and variant decisions. Keep auto-withdraw forbidden.
4. Protect Wilderness BIB prohibition across UI, cache, background and Bank O. Preserve research Bank O gate and account-bound snapshot.
5. Review item equipability state/charges/ammo/GWD blessing and false source item profiles for every method touched. Explicitly verify 50 Hunter, 80 Ranged source-related W01 gaps.
6. Do not merge UI shell without owner-approved native integration scope and actual RuneLite Java UI/scroll testing.

### P1: near-term product truth / quality

7. Integrate gear W01 only after independent L2 on exact item IDs, state relations and source-revision pins. GEAR-CORE-04 executable adapter with 68 formerly design-only fixtures.
8. Sessions: trustworthy Commander Zilyana KC alias and boss-only drop mapping, source labels, +0 XP vs unavailable semantics, 10-login history/weekly reconciliation, consumed supplies producer with real events.
9. Pathfinder: verified prereqs, travel/setup estimates, persistent goals/checkboxes, activity research approval and inspect/select isolation.
10. Chronicle: approve CA task tips five bosses at a time from primary Wiki and protect against false completion; independent boss navigation.
11. Source completeness for 28-position inventories, potion dose/ID, pouch/runes, source column ranking and missing/unsupported style methods.
12. Build truthful boss-attributed loot table, marginal/cumulative rare-drop math, CL source coverage.

### P2: maintainability and release program

13. Portable reproducibility of old `scripts/build-combat-tasks.py` that historically hardcoded host paths; compare regenerated TSV byte-for-byte on clean source.
14. Regression tests for 225px UI, scroll/wheel, five-row selectors, tooltip bounds, OSRS sprites, settings/migrations, non-overlapping buttons, 11+28 geometry, one client/two independent plugins.
15. Source review for new game items, Sailing or other newer OSRS mechanics **only when source/date pinned**; no assumption that static research is complete.
16. Brother's one-click Windows updater: root launcher that discovers installation, verifies signed manifest, backs up, updates changed files, preserves settings, runs limited checks and **never** bypasses security. This is a future owner-requested convenience feature, not current release infrastructure.
17. GitHub Plugin Hub submission only after intended branch, build, account safety and release gates pass. Project document may show active development but cannot imply verified plugin publication.

---

## 15 | Cursor engineering workflow: first hour, every task

**A. Baseline**

1. Identify actual source: latest checked-out branch/commit and archive checksum. Read `README`, `build.gradle`, existing resource files, read-first and `SOURCE_TREE_INDEX.md`.
2. Show actual classes and tests involved and known unaffected invariants. Compare to `references/TPT-W0751-READ-FIRST.md` rather than applying old patch descriptions blindly.
3. Preserve rollback and produce a minimal acceptance matrix with real source/witness distinction.

**B. Implement in a narrow wave**

4. Keep production source changes only for requested features. If task is design-only, work under new shell/prototype paths and **do not** edit game logic.
5. For each new rule, attach primary source/date/item IDs, exact scope, negative cases, synthetic fixtures, owner QA requirement and `RESEARCH_GEAR` until approved.
6. Test only actual behavior: selection isolation, account fence, bank lease, gear compatibility, physically packed switch slots, XP/KC provenance, and preservation of existing tabs/28 slots.
7. Use genuine JDK17 and Gradle for final pass; label earlier shims correctly.

**C. Handback**

8. List actual files changed, what changed for the player and what was deliberately unchanged; no build/test count unless blocking, prefer user-facing effect.
9. Deliver full clean source checkpoint, exact SHA256, scripts in root, compile/test receipt, short Windows QA cases (step → expected → failure evidence) and blockers.
10. Status remains HOLD unless the owner explicitly authorizes release after evidence and independent review.

### Recommended first Cursor prompt

> Read `TPT_CURSOR_MASTER_CONTEXT.md`, `SOURCE_TREE_INDEX.md`, `.cursor/rules/tpt-project.mdc`, and every relevant document in `references/`. Inspect the actual checked-out repo before changing anything. Report current commit/source hash, discrepancy against Wave 7.5.1 notes, high-risk files, and a scoped implementation plan. Preserve exactly four 225px tabs, 11 equipment cells, the 4×7 backpack, Bank R05/manual O behavior, actual state/provenance, independence of Chronicle, and separate HoverLite dual launch. Do not claim a native pass or introduce unapproved research. Then work only on the concrete task I give you.

---

## 16 | Evidence/attachment manifest and unresolved absence

### Original supporting documents included in this pack

- `references/TPT-SPOONS-MASTER-RESEARCH-2026-11-02.md`: full research/release/status synthesis, R31/R32/GEAR-W01, item mechanics, BQA-902 sessions, hash table and crew queue.
- `references/01-MASTER-EXECUTION-PLAN.md`: historical full product feature map, real Build19 native evidence, Pathfinder/Sessions/Bank O design and release gates (some version statuses superseded).
- `references/01_COPY_PASTE_INTO_NEW_CHAT.md`: selected UI reference-image names, precedence, absolute visual rules and standalone shell acceptance.
- `references/TPT-W06-OWNER-READ-FIRST.md` and `TPT-W06-OUTFITTER-SANITY-AUDIT.md`: six logic defects and exact W06 source-limit evidence.
- `references/TPT-W07-OWNER-READ-FIRST.md`, `TPT-W075-READ-FIRST.md`, `TPT-W0751-READ-FIRST.md`: newest consecutive Outfitter waves and test caveats.
- `source/TPT-INTELLIGENCE-W075-MULTISTYLE-GEAR-CLEAN-DUAL-DEV-OWNER-HOLD.zip`: original W075 source archive. **Do not confuse with a W0751 corrected-test archive.** See `SOURCE_TREE_INDEX.md` for names and integrity manifest.
- `references/TPT-SPOONS-RESEARCH-COMPENDIUM-2026-11-02.zip`: the full 22-entry historical R31 + GEAR-W01 + Sessions research preserved-source pack. R32 derivative remains staging-only and unapproved.
- `ui-reference/TPT-UI-SHELL-R02-W02-STANDALONE-HOLD.zip`: inspected 79-entry isolated Java Swing UI shell with sample-only visuals, 12 previews and local source-backed icon assets; **not a RuneLite plugin**. Open `ui-reference/README-FIRST.md` before interpreting its screenshots.

### Other authoritative material **NOT** embedded here

- Exact most recent **W0751 source ZIP**: latest read-first exists, full archive was not visible/materialized in the current pack at compilation. Ask Cursor to inspect the user's real local W0751 checkpoint and compare tests.
- The research compendium ZIP **includes the immutable R31 archive and original producer/review packs**. The full separate canonical R32 archive, every primary Wiki source image, and currently live Wiki revision snapshots are not included. The R32 derivative in the compendium is explicitly staged/noncanonical.
- Canonical latest BUR/RSR JSON state after the October 30 snapshot; needs explicit synchronization.
- Original owner-selected October 31 design-reference image set is not embedded. The later standalone Shell R02/W02 ZIP **is** included under `ui-reference/` along with its actual 225px and phone-scale screenshots; it remains SAMPLE-only and is not native RuneLite.
- Any current player credentials, bank contents, session logs or private account data: intentionally excluded.

This is a **complete consolidated operating brief of retrieved TPT decisions and status**, with primary handoffs and an inspected W075 source archive, not an export of every historical asset ever discussed. Current repository and native test evidence supersede it when verified.

---

## 17 | Hard “never regress” acceptance checklist

- [ ] Exactly Home, Pathfinder, Outfitter, Sessions main tabs; no fifth tab, sidebar fits 225px with scrolling.
- [ ] Header settings cog, compact armored raccoon and real OSRS sprites rather than fake item imagery.
- [ ] 11 worn cells and 28 stable, 4-wide × 7-tall inventory positions, with actual equipped/owned quantities.
- [ ] Melee/Ranged/Magic **independent** style buttons, boss-specific real method; `Wiki Recommended/Best in Bank` independent source modes; Entry/Recommended/Optimal only where sourced.
- [ ] Chronicle prev/next/picker affects Chronicle only and preserves Outfitter selection/scroll.
- [ ] Wilderness Best in Bank cannot be activated anywhere; all 11 flagged encounters and KBD approach warn.
- [ ] Bank O shows an appropriate temporary manual preparation tab, survives legitimate withdrawals, never auto-withdraws or trashes foreign tags; unknown bank doesn't become empty.
- [ ] Gear candidate is physically owned, equippable, ammo/charges/spells/1H-2H/set/god protection valid; optional gaps do not block without mechanic reason.
- [ ] `RESEARCH_GEAR/CHECK` styles cannot trigger BIB preload/O; exact W075 Scurrius/Mole provisional methods are labeled provisional.
- [ ] Current Sessions resets only on confirmed logout and archives latest 10 completed separately by account; loading/teleports do not erase XP.
- [ ] Real lifetime KC first read is baseline only; loot-confirmed estimate is not verified KC; no fabricated XP, drops, consumed supplies or completed CA/quest.
- [ ] Pathfinder shows real checkbox/inspect/select states and source-backed next action/travel/setup; no completion by intention.
- [ ] TPT and HoverLite remain separate plugins in a **single** RuneLite dev client; HoverLite doesn't mutate game menu actions.
- [ ] Native Windows JDK17 Gradle and actual RuneLite regression evidence is clearly distinguished from offline shim/source inspections.
- [ ] Source ZIP hash, changed files, rollback, exact test receipts and owner approvals documented; **release HOLD** until explicit authorized gate.

**FINAL CURSOR DIRECTIVE:** Build the player's trust by showing useful, correct, *honest* intelligence. Preserve what works. Make uncertainty visible. An apparent feature that invents ownership, KC, potion doses or readiness is worse than a small truthful CHECK label.