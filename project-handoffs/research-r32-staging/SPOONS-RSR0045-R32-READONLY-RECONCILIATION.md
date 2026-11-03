# SPOONS | PROJECT BURROW | RSR-0045 KBD | R32 Tongs L2 Reconciliation

**Date:** 2026-11-01 UTC  
**Mode:** READ-ONLY | RESEARCH ONLY | RELEASE HOLD  
**Disposition:** LIMITED RESEARCH INTAKE; ONE TARGETED REWORK + SEVEN EVIDENCE BLOCKERS. **NOT QA COMPLETE; NO PRODUCTION IMPORT.**

## Evidence and integrity

- R32 ZIP: `BURROW-RESEARCH-DISPATCH-R32.zip` SHA-256 `36176521201338156334898582ee12077baa929fe42bf8c3846ef327440b1d0a`; source manifest 83/83; R31 542,127-byte transcript exact prefix of 566,106-byte R32 cumulative transcript, joined by 1 separator byte and 23,978-byte KBD append.
- Tongs ZIP: `TONGS-RSR0045-R32-INDEPENDENT-L2-HANDBACK.zip` SHA-256 `c310eb4bdedf1fa938da9fbf135abea610ebba501540603e54e32e005c64b631`.
- Independent disposition CSV: **38 PASS_INDEXED, 2 PASS_POLICY, 1 REWORK, 7 BLOCKED = 48**. No live-revision pinned PASS.
- R32 research register retains **71 tickets** and **61 encounter rows**. No canonical approvals changed or authorized by this handback.
- R32, its cumulative transcript, research CSVs, prior 31 research sections and all passed claims are **immutable inputs** to this proposal.

## Exact disposition

**Provisionally accept at documented evidence scope:** B001–B012, B015–B016, B018–B037, B039–B040, B042, B045–B047. B045/B046 are **PASS_POLICY**, not independently verified gameplay implementation; the remaining 38 are **PASS_INDEXED**, not confirmed current page revisions.

**REWORK:** B041 only. Source KBD append original line, transcribed exactly:

`RECIPE Kbd heads (7980) can be used in cosmetic Black slayer helmet / dark slayer helmet variant and Dragon hunter crossbow (b) after hard CA; may be stuffed for 50,000 coins, offered for Prayer XP, or cut at 90 Magic into 3 ensouled dragon heads. Crafted descendant and consumed head should not double count as another boss drop.`

**Smallest textual correction:** Replace only `cosmetic Black slayer helmet / dark slayer helmet variant` with `a black slayer helmet or black slayer helmet (i) after unlocking King Black Bonnet (1,000 Slayer reward points)`. Preserve all other sentences, Kbd heads ID 7980, Dragon hunter crossbow (b) after hard Combat Achievements, and ingredient/descendant deduplication notes. Reference: https://oldschool.runescape.wiki/w/Kbd_heads and https://oldschool.runescape.wiki/w/Slayer_helmet . Researcher should independently reconfirm source phrasing and avoid claiming a hard CA unlock on *all* helmet variants.

**Remain BLOCKED:**

- B013: All six source 28-slot inventory grids, original icons, indices, counts, true blanks.
- B014: Source-specific switches, teleport placement, exact potion doses.
- B017: Source KBD rune-pouch contents, quantities and selected spellbook.
- B038: Boss-specific joint/sequential/mutually-exclusive drop-roll relationship; published marginal rates may be shown, combined odds may not be fabricated.
- B043: Live canonical boss/strategy Wiki `oldid`, last-edit UTC.
- B044: Original R08 IntelliJ and historical R04 master archives; R08 plaintext alone is not the missing build/archive.
- B048: Authentic per-image/item metadata, file rights and original source references.

## NEXT: targeted Researcher dispatch (do not run broad boss research)

RESEARCHER | PROJECT BURROW | RSR-0045-R32-B041-CORRECTION-R01 | REGULAR CHAT | RESEARCH ONLY | RELEASE HOLD. Retrieve immutable BURROW-RESEARCH-DISPATCH-R32.zip, R32 claim ledger and TONGS-RSR0045-R32-INDEPENDENT-L2-HANDBACK.zip. Correct **only B041**, replacing the unsupported `dark slayer helmet variant` wording with exact source-grounded black Slayer helmet / black Slayer helmet (i) (King Black Bonnet unlock) language. Preserve Kbd heads ID 7980, Dragon hunter crossbow (b) hard Combat Achievement condition, ownership/recipe deduplication, all remaining 47 claim contents, all source table and inventory CHECK/x? states, and every previous cumulative section. Produce an isolated one-line candidate, unified diff, sources with retrieval date and licence, checksum and exact Tongs recheck request. Do NOT modify R32 ZIP, cumulative Library, canonical tickets, source code, GitHub, Windows or release status. NEXT QA TONGS.

## NEXT: independent Tongs recheck after Researcher correction

TONGS | PROJECT BURROW | RSR-0045-R32-B041-CORRECTION-R01 | INDEPENDENT L2 QA | READ-ONLY | RELEASE HOLD. Verify single changed B041 append line and source-backed black Slayer helmet / black Slayer helmet (i) (King Black Bonnet) correction. Validate exact diff applies to immutable R32 and reproduces candidate with no unrelated edits. Reconfirm the 40 provisionally supported claims unchanged and all 7 BLOCKED claims remain unverified unless new independent evidence genuinely resolves them; preserve 5/5/4/5/5 ranked table columns, unranked AFK Magic, Wilderness Best in Bank denial, 28-slot CHECK/x?, conditional loot probabilities, original 71/61 registry scope, and R31 prefix. Return B001–B048 PASS_INDEXED/PASS_POLICY/REWORK/BLOCKED ledger and Spoons handback. No cumulative Library, GitHub, code, Windows, canonical tickets or release actions.

## Owner decisions

**None needed to authorize this narrow research correction.** Original inventory/media and missing historical source evidence remain independently open, not a reason to alter approved research or dispatch software QA.

**Handoff:** RESEARCHER B041 -> TONGS exact-diff L2 -> SPOONS research intake. All sources retain RELEASE HOLD.