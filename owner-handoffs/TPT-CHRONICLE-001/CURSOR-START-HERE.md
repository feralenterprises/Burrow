# 🦝 FERAL CREW | CURSOR DISPATCH | TPT-CHRONICLE-001

**Purpose:** Implement the owner's approved Boss Chronicle in the EXISTING TrashPanda Tracker plugin, using the actual working PC build as the baseline. This is a handoff, not finished implementation.

**Authority / hold:** Owner approval covers product direction and the Chronicle icon, NOT code acceptance, a release, or a merge to main. Spoons must reconcile TPT-CHRONICLE-001 with the permanent master register. FERAL-VERIFY-001 applies. Keep release HOLD and do not publish to Plugin Hub.

## Source identity FIRST
1. Identify the exact source corresponding to the build that is **currently running and owner-confirmed working on the PC**. Record ZIP SHA-256, branch, full commit SHA (if available), and build date. Do not quietly substitute GitHub `main` (previously observed on an older Build #38 baseline), `handoff/r02-...`, or an unreconciled Cursor integration.
2. The previous TPT-INTEGRATION-001 handoff reports a capture/sidebar candidate at `cursor/tpt-integration-001-1337`, commit `dd804132a36cb166aac7cce890438ea92ff537a6`, but that is NOT proven to be the running build and must not overwrite it.
3. If the exact working baseline cannot be recovered, keep this task **BLOCKED** for source edits and hand back the mismatch. Do not reset player data, branch history, or bank protections.
4. Use a new implementation branch. No unauthorized merge to `main`; do not publish or replace the working PC build.

## Owner-approved feature set
**A. Boss browser is a compact table.**
- Replace oversized repeated boss-browsing cards with a compact searchable Boss Index; show about five visible rows with a vertical scroll at the 225px RuneLite sidebar width.
- Search by boss name; offer A→Z / Z→A; optional approximate easiest→hardest only when supported by explicit research, never fabricated player-readiness scores.
- Keep existing boss names/artwork. Boss card/detail is opened by selecting a row; detail contains boss-specific artwork, KC, drops/rates, mechanics, preparation and Combat Achievements. Do not show repetitive drop-rate paragraphs in the index.
- Existing four top-level tabs are **Home, Pathfinder, Outfitter, Sessions**. Chronicle remains the existing internal view/action, NOT a fifth tab.

**B. Chronicle detail → Open in Outfitter.**
- Add a visible, compact **Open in Outfitter** action under the boss header/near the title. Clicking from Bryophyta selects Bryophyta in Outfitter, navigates to Outfitter, and refreshes that boss's existing recommendations.
- Chronicle and Outfitter remain independent outside this EXPLICIT handoff. Browsing Chronicle must not silently change Outfitter boss/method/source/style; clicking this button is the sole requested synchronization. Returning to Chronicle restores its selected boss/detail/browse position.
- Never bypass a boss's existing method, readiness, account restrictions, bank trust, or wilderness gates.

**C. Boss-specific Combat Achievement task list.**
- Show actual tasks for selected boss with compact status indicators and meaningful hints/difficulty where source-approved. Do not conflate a research tip with a game's awarded criterion.
- Distinguish **VERIFIED_COMPLETED**, **VERIFIED_INCOMPLETE** (only if proven), and **UNKNOWN / NOT SYNCED**. Checkmarks are reserved for task-specific completion evidence. Unknown must not appear as an unchecked confirmed failure.
- CA aggregate points, total tasks and six tier counts are NOT proof of WHICH individual tasks were completed. In the earlier owner's running capture, 441 points and 178 tasks were visible; six-tier native storage was not yet verified, and individual task completion was not decoded.
- Investigate the pinned RuneLite 1.13.5 legitimate API/widgets/varbits or an approved source for individual CA task states. Cite exact source + meaning; if not verifiable, DO NOT invent completion or claim the sync is complete. Show aggregate progress separately, with provenance/time, and individual rows as unknown until trustworthy evidence exists. Avoid pretending a manual 'Viewed' receipt equals a completed task.
- Use existing account-hash, login-epoch and persistent evidence paths. Recheck restart and account switching; don't create a parallel account-intelligence engine.

**D. Dedicated Chronicle icon.**
- Use the custom original bestiary-book/paw Chronicle artwork from `owner-handoffs/TPT-CHRONICLE-001/assets/` as a visual source. Asset(s) are design inputs, not necessarily ready-to-ship icons.
- Render crisp transparent 16/20/24px sizes at 225px sidebar scale, with accessibility/hover text and neutral/gold contrast. Do not use rejected chunky mascot art. Reuse icon on the Chronicle entry/book shortcut consistently.
- Prefer the most legible asset after actual tiny-size and native RuneLite inspection; don't paste a whole screen mockup as an icon.

## Hard invariants: DO NOT REGRESS
- Exact four tabs and 225px sidebar, vertical scroll, clean UI, owner-approved armored raccoon and boss art.
- Wiki Recommended / Best in Bank, Melee/Ranged/Magic controls, 4×7 inventory, Chronicle↔Outfitter selection isolation except explicit handoff.
- R02 bank protection: Bank O/withdrawal freeze, live vs as-of-close vs historical memory, `NOT CURRENTLY WITHDRAWABLE`, per-account storage. No automatic bank withdrawals.
- Existing Pathfinder, Sessions/KC, quest/diary/CA aggregate capture, player preferences and HoverLite.
- Do not import the 498-name gear research longlist as verified item IDs or ranked gear. Do not add a second gear engine, extra tabs, fake stats or readiness bars.

## Focused acceptance and proof
- Cite exact changed paths, full commit/branch URL or complete source ZIP, source SHA, and JDK17 build/test results. Headless screenshots are not native proof.
- Run targeted tests for: five-row searchable scrolling boss list; A-Z sorting; selecting a row; explicit Chronicle→Outfitter transition; independence when merely browsing; return/back to same Chronicle boss; task status invariants; account switch/restart; 225px clipping; bank-guard immutability.
- Native RuneLite owner tests, no more than three:
  1. Browse and search the boss table; open Bryophyta detail; use **Open in Outfitter**; verify matching boss and return navigation.
  2. Open a boss with a CA known completed and another unknown; verify only per-task **evidence-backed** checkmarks, and inspect aggregate sync/timestamp after restart.
  3. Verify 225px layout/icon alongside current Wiki Recommended / Best in Bank and historical bank trust. Do not retest unrelated game systems without a specific regression reason.

## Required FERAL-VERIFY-001 handback
**TICKET** / **STATUS** (CODED, BUILT, FUNCTIONALLY VERIFIED, OWNER ACCEPTED, BLOCKED only) / **WHAT CHANGED** / **WHAT THE PLAYER CAN NOW DO** / **EVIDENCE** / **OWNER TEST STEPS** (≤3) / **STILL BROKEN** / **NEXT ACTION**.
Only OWNER ACCEPTED may be called COMPLETE. Cursor implements; Tongs independently checks; Spoons coordinates; owner alone accepts. Keep RELEASE HOLD.

## Related research notes
- Earlier CA captured two aggregates in owner screenshot: 441 combat points and 178 tasks. Latest TPT-INTEGRATION-001 handoff states a newer sidebar-fit/tier-widget candidate exists but native owner proof of tier rows and repaired width was not yet established.
- `TPT-INTEL-RESEARCH-002-CONSOLIDATED-INDEPENDENT-AUDIT.md` was previously supplied to the owner as a separate research artifact. Do not assert its findings were integrated without reading the actual file and current source.
- Best in Bank gear fix `TPT-GEAR-INTEL-001` is separate and on HOLD; don't transplant an older source ZIP over the working integration to include it.
