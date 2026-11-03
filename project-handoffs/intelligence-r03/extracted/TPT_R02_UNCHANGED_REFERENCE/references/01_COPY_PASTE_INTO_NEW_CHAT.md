# PROJECT BURROW | TRASHPANDA TRACKER | NEW UI DESIGN HANDOFF

**ROLE:** Independent senior RuneLite UI/UX designer + Feral Enterprises art integrator.
**MODE:** Regular Chat for design prototyping. VISUALS ONLY, release HOLD, no repository or production source modifications, no Windows/player testing, no ticket closure.
**SOURCE:** Read the complete attached `TRASHPANDA_NEXT_CHAT_DESIGN_HANDOFF_2026-10-31.zip`, especially `00_READ_ME_FIRST.md`, `02_OWNER_DECISIONS_AND_GATES.md`, and `03_REFERENCE_MAP.csv`, and actually inspect the included images. Do not rely on the file names alone.

## Mission

Make ONE genuinely new, coherent **TrashPanda Tracker** RuneLite sidebar interface by MERGING the specific information architectures and the pixel-raccoon style I selected. Stop presenting old designs with new colors. Do not regenerate a single giant illustration pretending it is real UI. Build actual layout/art integration with precisely placed elements and readable text, ideally an editable 225px HTML/CSS/JS local prototype, then capture every screen.

## Reference precedence

1. **COLOR**: the actual Burrow Pathfinder screenshot `references/01_OWNER_PRIORITY/01_PALETTE_APPROVED_REAL_BURROW_PATHFINDER.png` (near-black, charcoal, subtle amber/bronze, muted status green). Match its restrained tone.
2. **HOME**: `04_HOME_ARMORY_WORKBENCH.png`, supplemented by `references/03_INFORMATION_DENSITY/03_HOME_NEXT_LAST_WEEKLY_GOALS.png` and `04_HOME_OBSERVED_ACTIVITY_AND_UPGRADES.png`.
3. **PATHFINDER**: `05_PATHFINDER_DEPENDENCIES.png`, plus legacy route dependency reference. Show next action, chain, parallel activities, evidence status, and the three progression domains (Skilling, Combat including Slayer, Quests/Diaries) without fabricating dependency logic.
4. **OUTFITTER**: `02_OUTFITTER_FIELD_TERMINAL_LAYOUT_TOP.png`, `03_OUTFITTER_ARMORY_EQUIPMENT_SWITCH_PACK.png`, and `references/03_INFORMATION_DENSITY/01_DETAILED_EQUIPMENT_AND_REQUIREMENTS.png` + `02_BOSS_RARE_DROPS_AND_GUARANTEES.png`. Preserve the numbered FIVE-step hierarchy. Boss can be a dropdown; combat style is **three visibly separate BUTTONS**: Melee / Ranged / Magic. Wiki Recommended / Best in Bank is a DIFFERENT two-button mode control. Show the normal 11 gear slots and full 4x7 28-slot pack, exact item labels/quantities, user-preferred switch slot guidance only for bosses actually needing multi-style switches, equipment readiness, tracked boss loot and rare rates with appropriate unknown states.
5. **SESSIONS**: `06_SESSIONS_ARMORY_WORKBENCH.png` + `07_SESSIONS_DETAIL_AND_INTEGRITY.png`. CURRENT LOGIN ONLY: XP by skill, KC changes, drops, consumed supplies, event tape, reliable sourcing, unknown/unattributed states, reset on logout, no fictional first-KC kill.
6. **RACCOONS**: `references/02_CHARACTER_ART/01_OWNER_SELECTED_OSRS_FERAL_RACCOON_STYLE.jpg` and `02_FERAL_ENTERPRISES_RACCOON_POSE_CONCEPT_SHEET.png`. The pixel-art raccoons must actually resemble these. Choose one compact header crest and at most 1-2 SMALL contextual poses across a screen, not oversized mascots or new cartoon species. Artwork must not obscure real text or data.
7. **HEADER**: Previous headers were REJECTED while lower content was preferred. Design a FRESH, compact, practical Feral Enterprises + TrashPanda Tracker header and four small tabs that fit 225px; keep the exact tagline “We dig through your account so you don’t have to.” only if it fits without reducing usability (e.g., tooltip/about instead of two lines consuming the viewport).

## Absolute rules

- Sidebar is **225 CSS px wide**, vertically scrollable. Four tabs EXACTLY: **Home, Pathfinder, Outfitter, Sessions**. **NO BANK TAB, NO FIFTH TILE.** Bank O chest is separate live bank functionality, OUT of scope for the sidebar design.
- Do not use the REJECTED huge/chubby Feral Modern raccoon. The image is intentionally excluded. Do not use a large bottom-of-screen cartoon, a gold fantasy border explosion, or a full-window desktop dashboard.
- Use RuneLite-compatible density, native item silhouettes, real legible labels/tooltips. Boss style applicability must come from verified boss-strategy data; missing research cannot silently turn into unsupported style. Unsupported options disabled with explicit reason.
- Mock data is allowed ONLY if conspicuously marked SAMPLE. Never assert that example gear is Wiki-verified, that actual bank ownership is known, or that session XP/KC/loot is observed unless evidenced. Unknown ≠ zero.
- Do not swap selected UI hierarchy for generic cards. Genuine integration and novel header/visual treatment, not a reskin of `references/05_PRIOR_ATTEMPT_NOT_APPROVED/`.
- Stay inside new standalone conversation artifacts. Do NOT edit GitHub, actual Burrow source, official ticket records or release state.

## Deliverables in THIS conversation, not later

1. A coherent four-tab **editable standalone HTML/CSS/JS UI prototype** (preferably with working tab and Melee / Ranged / Magic and mode buttons).
2. Four INDIVIDUAL **full-height PNGs** for Home, Pathfinder, Outfitter and Sessions, each at exact 225px CSS viewport width (optionally 2x exports labeled with their scale).
3. Four **first-viewport previews** (225 x ~700-750 CSS px) and one comparison contact sheet. All content scrolls, never clips horizontally.
4. A single ZIP with prototype, all four images, local art assets, README and design decisions. Do not substitute five styles or a composite illustration for those files.
5. A brief QA receipt: file inventory, actual width, all tabs visible, 11 worn slots, full 28 inventory slots, distinct mode vs style controls, and which elements are only illustrative.

Work directly from the supplied evidence; don't waste a turn asking what style I want. **Bring one fresh, complete, cohesive design for review.** Do not call it approved until I say so.