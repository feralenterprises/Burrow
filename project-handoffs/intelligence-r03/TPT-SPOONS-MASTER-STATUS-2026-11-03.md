# Feral Enterprises | TrashPanda Tracker
## Spoons: consolidated status from recent work chats

**Snapshot:** November 3, 2026, ~07:58 America/Los_Angeles  
**Mode:** STATUS + EVIDENCE COMPILATION ONLY  
**Release status:** **HOLD**  
**Merge status:** **NONE.** No Java source, GitHub branch, canonical register, bank data, user account or prior ZIP was mutated to create this pack.

### Results first

TPT has three distinct active implementation lines, plus research:

1. **Main dual-plugin TPT source:** `TPT-W0751-RUNELITE-135-ONE-CLICK-DUAL-DEV-FIX.zip` (verified SHA-256 `79e7d54a78a0bb76b6b3eca0e6804067b02b40a35e88339ee83a20408aa7ef3f`). Cumulative Wave 7.5.1 plus RuneLite dependency 1.13.5 and an embedded one-click launcher. This is the newest **retrieved packaged main implementation**; actual Windows launch with RuneLite 1.13.5 and full Gradle/JDK17 retest still have no passing owner receipt.
2. **Pathfinder simulation and account-evidence candidate:** PQA-W07, **isolated cumulative patch against the exact W0751 base**, not merged. It improves evidence capture, bank-memory representation and account isolation but does not fix long-run ranking/goal completion or connect all in-game readers.
3. **UI design/prototype:** R02-W08E standalone Swing shell with interactive HTML phone prototype. **Not a newer main engine**, not in live RuneLite. The R09 independent UI audit says do not merge yet: 0/3 intended boss-model portraits approved, 32 findings, 6/10 appearance and 5/10 owner reference match.
4. **Research:** R31 immutable historical boss research; R32 scoped staging; GEAR-W01 research with no production-approved rows; activity packs staged; BQA-906 CA 655-row export rejected for 652 placeholders. These are evidence libraries, not a greenlight to import.

**DO NOT treat W08E's apparent version number as later main TPT source. DO NOT apply Pathfinder's cumulative W07 patch on top of an already-patched W06 source.**

## Workstream rollup

| Lane | Latest point | What actually exists | Blockers / unverified |
|---|---|---|---|
| Main TPT + HoverLite | W0751 + RuneLite 1.13.5 launcher fix | Complete cumulative dual-plugin dev source; one-click START-HERE; archive and manifest checks | Full 676-test Windows Gradle rerun and successful Windows RuneLite 1.13.5 login not reported; `error_game_js5connect_outofdate` after earlier launch |
| Outfitter multi-style | W07, W075, W0751 | Genuine style-specific method selection; provisional Scurrius Ranged/Magic and Giant Mole Earth Magic examples; Obor/Bryophyta key corrections | Provisional gear cannot activate Best in Bank or Bank O; complete source verification/owner gameplay QA pending |
| Best in Bank / Bank O | Earlier owner-reported Bank R05 four manual withdrawals, later preservation in intelligence waves | Existing bank tag and withdrawal workflow retained through W0751; gear safety logic source candidate | Need same-source Windows confirmation, unknown bank states/charged variants, whole-loadout logic and native regression on new launcher |
| Intelligence W01-W06 | Cumulative through W06 | Typed requirements and research-safety bridge; activity and CA catalogs represented separately | Staged research deliberately not active; CA and activity recommendation value still mostly not live |
| Pathfinder progression simulation | PQA-W07 isolated cumulative patch | 48 profiles, 314 offline assertions; 48/48 partial Collection Log page tests; historical Bank Memory views, provenance checks | 0 verified long-run 100/500/1000-action journeys; repeated suggestions, GP/rune/food budgets, native integration |
| Account-wide first-run capture | PQA-W07 candidate | Logged-in journal and Collection Log page reader; manual-review status, per-account provenance | Boss/Slayer logs, diaries, CA completion still not natively captured end-to-end; no full coverage witness |
| Bank Memory | PQA-W06/W07 candidate | Historical per-account bank snapshots with quantities, timestamps and freshness; local standalone view model | Not connected to live Outfitter. **Latest explicit owner instruction favors no new visible TPT Bank Memory tab**; existing separate plugin may show it, but internal snapshot remains mandatory |
| UI redesign | R02-W08E plus R09 audit | 225px four-tab standalone Swing shell, Chronicle search/detail, style UI skeleton, controls, phone HTML prototype | Not merged; three authentic boss models 0/3 approved; live account and bank adapters missing; owner visual signoff pending |
| UI independent audit | R09 | 66/66 browser control/overflow checks at 320/390/430px; source-side Swing tests; 32 tracked findings | Browser HTML != RuneLite; appearance 6/10; reference fidelity 5/10; Windows DPI/native interaction untested |
| Boss research R31 | 2026-11-01 immutable save | 71 research tickets, 61 encounters; 31 researched-unverified, 40 ready-research | All 61 encounter rows lacked owner gameplay approval; immutable save must not be rewritten |
| Boss research R32 | Later staged corrections | KBD, Perilous Moons, Maggot King and Kree'arra scoped corrections; other partial tickets | Thermonuclear intake, Kraken recheck, Brutus/Tongs work, image/item and quantity uncertainties |
| Gear W01 | Spoons R02 intake | 4/4 producer handbacks; 121 claim rows, 117 item refs, 80 exact IDs, 70 proposed relations, 68 BIB design scenarios | 0 implementation-approved gameplay gear rows; design fixtures != executed tests |
| Activity, travel, farming, clue | W04 and newer research conversations | 44 staged activity examples/records; skilling, diary & minigame travel, tree routes and clue gear rules | Incorrect source examples were found and audited; source-backed access/teleport/gear details need approval before live use |
| Combat Achievements | W05, BQA-906 | 290 existing Chronicle references and 77 tip/link entries retained; CA-logic structure separated game condition from strategy | 655-row BQA rework contains 652 placeholders; do not import; no complete validated canonical CA catalog |
| Sessions/KC/rare drops | BQA-902 R33/R34 forensics | KC source separation, zero-vs-unavailable rules, provenance and supply-feed research | Last-10 real session history, native KC/CL capture, supply quantities, per-account isolation acceptance |
| HoverLite | v0.2.8 bundled in dual-dev; v0.1.1 public lineage separately reported | Same-client launch mechanism maintained | New bundled version lacks independent live gameplay approval in this snapshot; TPT's Plugin Hub approval separate |
| GitHub / RuneLite Plugin Hub | Older Build #38 reportedly published to Burrow | Historical public dev snapshot, NOT this latest W0751 and not W08E UI or PQA-W07 | TPT standalone upstream Plugin Hub submission/approval unverified, do not call published as official RuneLite plugin |
| Discord / organization | Separate Feral server operations | Community and crew workstreams exist | Server migration/verification independent of TPT plugin source, not included as accepted build |

## Latest explicit owner product/UI rules

- Product is **TrashPanda Tracker**, tagline **“We dig through your account so you don't have to.”**
- Exactly four main tabs: **Home / Pathfinder / Outfitter / Sessions**. Boss Chronicle is internal, not a fifth tab. The 225px RuneLite sidebar scrolls vertically.
- Native equipment layout is **11 worn slots**; inventory **4 columns × 7 rows** (28).
- Outfitter must distinguish **Melee, Ranged, Magic**, change the actual method/gear (not just colored button), and keep method/style/gear-source selections independent from Chronicle's boss navigation.
- **Wiki Recommended vs Best in Bank** remain distinct. Missing required equipment or mechanic protection may block; missing optional upgrades do not. Unknown bank evidence is not the same as an observed empty bank.
- Equipment variants, charges, imbues, cosmetics, set effects, ammunition and style compatibility require exact ID/state/context and verification.
- Manual green **O** bank preparation stays; no auto-banking, no destructive tags or repeated evidence refresh while an active withdrawal tab is being used. Wilderness BIB stays unavailable.
- **Latest Bank Memory UI decision:** Do not add another visible tab to TPT. The owner noted that a separate existing Bank Memory plugin is sufficient for display. TPT still must SAVE persistent account-specific observed bank inventory for Outfitter use. Note that earlier project documents still request a Bank Memory view; the more recent direct user instruction takes precedence for UI placement, not for underlying data persistence.
- First-run account-specific capture, guided coverage, manual refresh and 7-day stale-data reminder remain mandatory; a viewed menu with no readings is not a complete capture. Never infer unobserved KC, quests, diary completion or bank quantities.
- Visual identity: Feral Modern graphite/bronze, lean friendly armored raccoon with red scarf/bronze armour per approved reference; explicitly rejected chunky round mascot is forbidden. Authentic OSRS item/skill/boss iconography.
- The standalone UI prototype **is not approved to merge** on the strength of screenshots or synthetic control clicks.

## Wave-by-wave main engine, concise

1. **W01 Foundation:** versioned knowledge and fact confidence, required versus optional, bank unknown state; separate from live planner.
2. **W02 Gear:** exact IDs, states, requirements, equipment compatibility and set-effects model; research remains quarantined.
3. **W03 Best in Bank:** guarded intelligence bridge into existing whole-loadout planner; preserve bank withdrawal code.
4. **W04 Activities:** staged Farming, skilling, minigame, diary teleport and clue knowledge; source gating.
5. **W05 CA:** game-checked achievement conditions and provenance separated from tips, preserve 290 legacy references and avoid 652 fake records.
6. **W06 Integration:** limited Pathfinder and Chronicle hooks, no new tab or full CA import.
7. **W07 Outfitter repairs + styles:** source ammunition and switch corrections, state-specific Bank O eligibility, boss-specific Melee/Ranged/Magic selection; incomplete research explicitly provisional.
8. **W07.5 Gear examples:** Scurrius Ranged/Magic, Giant Mole Earth Magic, Obor/Bryophyta first-access vs reward key logic.
9. **W07.5.1 Test contract repair:** owner Windows W075 `:test` ran 676 tests with 11 failures. Ten test files rewritten to reflect corrected behavior; local source-level replay of the 11 passed. **No proof the genuine Windows suite now passes.** Launcher patch then bumps RuneLite 1.13.4 to 1.13.5, adds one-click start. This is newest packaged main source.

## Observed / attempted QA and why they are not interchangeable

| Evidence | Proven | Not proven |
|---|---|---|
| Owner Bank R05 manual testing | Four items successfully withdrawn in earlier Windows run, green O temp bank tags stayed active | Current W0751 same-source gameplay, all bank/account edges |
| W075 Windows `:test` | 665 passed, 11 failed (676 attempted/completed) | A clean later Windows Gradle run |
| W0751 local replay | All 11 old failures replayed with local Java21 `--release 17` shims and extra focused checks | Actual JDK17/JUnit/Gradle on Windows |
| W0751 launcher static audit | ZIP, hash/paths, source preservation; RuneLite default 1.13.5 | Whether dependency resolves and client launches/logs in |
| PQA-W07 offline | 314 assertions, 48 fixture profiles, 24/24 frozen evidence reproductions | End-to-end in-game capture and completed long-term journeys |
| UI W08E/R09 | 66/66 phone browser checks; Swing self-tests, 225px screenshots | Native RuneLite connection; owner approval; 3 model portraits |

## Merge order / integration fence (NOT executed)

The latest working hypothesis for integration is **main W0751 as the starting source**, subject to its owner Windows JDK17/live RuneLite gate; the **Pathfinder W07 cumulative patch separately** and **UI W08E as a design reference only**, not a bulk source overwrite. Research archives remain immutable or CHECK. Do not combine their contents by archive overlay. Before any implementation merge, re-identify the exact installed main source/JAR and confirm independent evidence for the features being promoted.

### Suggested next engineering gates (no work started by this compilation)

1. Confirm Windows RuneLite 1.13.5 dual-launch and full JDK17 Gradle tests, plus Bank O and actual Melee/Ranged/Magic changeovers. Preserve evidence from current installation.
2. Close the first-run capture and persistent internal Bank Memory gaps with per-account/freshness/unknown-state integrity, taking care not to add a fifth tab.
3. Reconcile Pathfinder W07 patch with canonical source only when live evidence and long-run simulation coverage are ready.
4. Reach owner-approved visual parity and authentic boss artwork for W08E; plan read-only UI adapters into the existing panel rather than transplanting a standalone Swing app.
5. Source-approve gear, activities, CA and R32 rows in batches; do not treat the 655-claim BQA-906 as a real ledger.
6. Publish no TPT Plugin Hub release based on current hold-state evidence.

## Evidence included in this compilation

See `EVIDENCE-SOURCE-MANIFEST.csv` for each exact filename and SHA-256. Sources are included **unchanged**, as individual archives/reports nested under `evidence/`. Original ZIP boundaries are preserved deliberately. Reports may reflect historical status as written; this master status resolves newer superseding decisions explicitly. Neither source packages nor historical R31/R32 records have been edited or merged.