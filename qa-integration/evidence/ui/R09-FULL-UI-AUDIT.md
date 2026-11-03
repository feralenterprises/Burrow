# TrashPanda Tracker | R09 Independent UI Audit

**Date:** 2026-11-03. **Subject:** Standalone Swing shell R02-W08E + phone audit HTML. **Not:** canonical TPT source, authenticated RuneLite integration, or independent verification of current main plugin.

## Results-first verdict

- **Looks great?** Not yet. Cohesive graphite/bronze appearance but not nearly identical to the three owner-approved images. Subjective visual quality **6/10**, reference fidelity **5/10**.
- **Feels functional?** In the demo, **7/10**. The controls are wired and responsive in the available automated tests, but state largely changes labels rather than using real account intelligence. Live RuneLite functional readiness: **not established**, not a passing grade.
- **Release/integration status:** **HOLD**. Do not merge this standalone app into the main RuneLite plugin.
- **Most important visual blocker:** Three boss model renders remain **0/3 verified and approved**. Small original category icons are 15×18, 18×18, and 19×19 px, composited into 65×70 portraits. Those cannot visually satisfy the target.
- **Most important engineering blocker:** No real account or bank/view-model adapter. Only three hard-coded boss fixtures; Home XP/readiness and sessions are labeled example/unknown.

## Independent checks run

1. Extracted latest Wave 8E ZIP into clean directory; compiled the three Java sources via `javac --release 17` on Java 21 host.
2. Ran Swing `--self-test`, artwork reviewer `--self-test`, Wiki fetcher `--self-test`, and artifact `--audit-art` status. All pass when graphical Java tests run under Xvfb.
3. The Wiki self-test fails without a display here (AWT initialization); it passes with Xvfb, so this is **environment/test runner dependency**, not evidence of a user-facing import failure.
4. Chromium phone audit model: **66 of 66** automated control/overflow checks passed across 320, 390, 430px, **0 JS errors**. Actual iPhone Safari was **not** tested. Browser model is separate from the Java Swing code.
5. Confirmed code and screenshots use **225×790** native Swing frames; 3× phone screenshots are pixel enlargements, not a 675px-wide plugin layout.
6. Confirmed from source: separate `chronicleBoss` and `outfitBoss`; 4 tab buttons; 11 equipment slots; 7 rows × 4 columns inventory; local appearance file; no RuneLite integration.
7. Checked original three submitted visual references against current Home, Chronicle Browse, and Scurrius Detail.

## Requirement-by-requirement comparison

| Area | Originally requested | What we have now | Verdict |
|---|---|---|---|
| Global shell | 225px, four tabs, armored friendly raccoon, graphite/bronze, no fifth Bank tab | Present; header art low-resolution | Partial |
| Home | Weekly XP, Biggest Win, Spoons Tip, Bandit boss hunts, Noodles next step; friendly polished cards | Cards and sample interactions work; text-heavy, pixelated mascots, demo hunts differ from screenshot | Partial |
| Chronicle browse | Authentic boss model portraits, search, A-Z/Z-A, difficulty, filters, book buttons, drop row | Search/sort/cards work; 3 fixtures, two filter rows, tiny category sprites | Partial / artwork blocked |
| Chronicle detail | Boss portrait + notable drop + verified KC/rare drops + aggregated odds + CA + quick tip + preparation action | All sections exist; KC and odds unavailable by design; presentation more verbose than target | Partial |
| Independent navigation | Chronicle boss vs Outfitter boss/method/tier/source/style independent; Back returns to search/scroll | Boss selections, search, Back in source/self-test; full source-method independence not testable without engine | Partial pass |
| Outfitter | Combat style controls next to Equipment; Entry/Recommended/Optimal; Wiki/Best Bank; 11 positions; real 4×7 inventory | UI skeleton yes; icons illustrate style, no actual gear, 28 slot markers only | Partial |
| Bank Memory and Best in Bank | Durable per-account bank snapshot, quantities/states, timestamp/freshness, bank-closed access; preserve bank O | Only expandable text saying unavailable; no bank writes or reads | Blocked in shell |
| Pathfinder | Feasible highest-priority account recommendation, visible why, prereqs/resources/travel gating | Two local example goals and checklist; engine not connected | Blocked as actual feature |
| Sessions | Real XP, largest skill gain, verified KC versus loot estimates, last 10 completed logins | Expandable panels, no real events/records | Blocked as actual feature |
| Settings | Font sizes and accent options persist, no fifth tab | Works locally; not tested for RuneLite profile migration | Partial pass |
| OSRS authenticity | Game sprites/item IDs, correct boss-model and drop artwork | Item sprites authentic for six examples; NPC portraits not full 3D models | Partial |
| First-run baseline | Mandatory account-specific capture and guided read-through, manual refresh, 7-day stale reminder | Not provided in shell; status in main plugin cannot be verified by this audit | Release blocker |
| Native interaction QA | Real RuneLite click/wheel/scroll/focus/DPI and no regression | Only standalone Swing and HTML automated QA | Not tested |

## Priorities

**P0 / Must fix before integration:** (1) Obtain approved boss model art; (2) owner signoff of 3 reference matches; (3) current canonical TPT source hash and adapter; (4) first-run and per-account data capture acceptance; (5) bank memory and Best-in-Bank/green O native regression; (6) strict KC, CA, CL and Sessions provenance; (7) real Windows RuneLite QA.

**P1 / Product-feel repairs:** reduce card bulk, trim demo disclosure copy from live layouts, improve 225px type hierarchy, source better-resolution mascot assets, restore one-row or comparably compact filter experience, test keyboard and wheel, bind source-backed recommendations/gear and logins.

**P2 / Cleanups:** fix stale W08C text in W08E source/window/footer, validate all font/color themes at Windows DPI, retain approved raccoon and zero rejected assets, ensure all prompts are actionable.

## Recommended staged next steps

1. **R10 Design match**: No more importer-only waves. Obtain the real boss image files or approved local RuneLite renders; make Home, Browse, Detail visually match originals at actual 225px and submit paired before/after screenshots for owner signoff.
2. **R11 Interaction QA**: Test live mouse pointer hitboxes, keyboard focus, dropdown wheel with 60+ bosses, screen scaling, scroll restoration, all theme/font combinations; resolve failures without main engine edits.
3. **R12 Controlled integration design**: Record exact current TPT source hash; build read-only view-model adapters and rollback plan, not a wholesale Swing `JFrame` transplant. Specifically check persistent first-run capture, bank snapshots, KC/CL/session evidence and stale reminders.
4. **R13 Native owner gate**: Java 17 Gradle + real Windows RuneLite, bank O and 4-item withdrawal regression, Best in Bank with bank closed, multi-account isolation, source-authentic boss and gear catalog, native screenshot approval.

## Audit boundary

No main plugin source was modified. A passing isolated shell self-test is **not** proof of working live game behavior. Likewise, phone browser checks validate the HTML review prototype only. Owner approval is pending. See `R09-ISSUES.csv`, `R09-MANIFEST.json`, and `qa/` evidence logs.