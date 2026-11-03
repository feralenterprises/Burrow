# TONGS | PROJECT BURROW | RSR-0045 KING BLACK DRAGON | R32 INDEPENDENT L2 QA

**TO: SPOONS | Date checked: 2026-11-01 UTC | READ-ONLY | RELEASE HOLD**

## Verdict first

**REWORK (B041) + 7 BLOCKED; otherwise 40 provisionally PASS (38 indexed-source facts, 2 existing owner/project policies). No import approval.**

- 48/48 original ledger IDs reviewed; `B001–B048` sequential and unique.
- `PASS_INDEXED` 38. `PASS_POLICY` 2 (B045 owner safety, B046 project provenance). `REWORK` 1 (B041). `BLOCKED` 7 (B013, B014, B017, B038, B043, B044, B048).
- No claim is `PASS_REVISION_PINNED`; direct current canonical Wiki oldids and inventory images unavailable, while public indexed content and derivative strategy mirror are readable.
- Package integrity: R32 all **83/83** SHA256 manifest entries match, ZIP contains **84** entries including manifest. R31 542,127-byte cumulative transcript is an exact byte prefix of R32 566,106 bytes, followed by one separator newline and 23,978-byte KBD append. R31 archive SHA-256 matches R32 manifest. No preceding research file content changed except administrative status/register/matrix/log/manifest.
- 71 ticket rows, 61 encounter rows; RSR-0045 alone changed in registers. Researcher wrote handback but Tongs verdict is independent and is **not yet in the Library**.

## Concrete REWORK: B041 (KBD heads downstream item)

The source append currently says: `RECIPE Kbd heads (7980) can be used in cosmetic Black slayer helmet / dark slayer helmet variant and Dragon hunter crossbow (b) after hard CA`.

**Correction (only this phrase):** `Kbd heads (7980) can be used to create a black slayer helmet or black slayer helmet (i) (King Black Bonnet unlock, 1,000 Slayer reward points), and Dragon hunter crossbow (b) after hard Combat Achievements rewards.` Remove `dark slayer helmet variant`. Wiki lists the Dark Claw (Skotizo) as the basis for the *purple* Slayer helmet, not a KBD-head product. Source S4, S7.

No contradiction was found in B041 consumption/ownership warning; preserve deduplication of Kbd heads versus resulting cosmetic item. Treat this as an isolated line correction for Researcher, not permission to rewrite R32.

## Original equipment and method verification

| Method | Ranked source columns | Ranked source rows | QA scope |
|---|---:|---:|---|
| Four-item Melee | 5 | 11 + Spec | PASS_INDEXED: task helm, 2H halberd shield exclusion, super-antifire defender and 4 valuables conditional |
| BIS Melee smuggle | 5 | 11 + Spec | PASS_INDEXED: Serpentine > faceguard, Defender > dragonfire shields *within cell*, grave/PK risk mandatory |
| Regular Ranged 4-item | 4 | 10, no Shield or Spec | PASS_INDEXED: Tbow/Bowfa two-handed; crystal-only alternatives; no fabricated ranked offhand |
| Crossbow Ranged | 5 | 11 | PASS_INDEXED: shield, Moonlight > Sunlight bolts only for hunters sunlight crossbow |
| BIS Ranged smuggle | 5 | 11 | PASS_INDEXED: Tbow and crossbow alternatives must be combined with matching ammunition, never equip 2H + shield together |
| AFK Magic | 0 certified ranked columns | Illustrated method only | Dragon hunter wand + Blood Barrage narrative, original gear icon grid unavailable, no ranked Magic table |

These are **ranked source alternative columns**, not 5 simultaneously legal loadout tiers or complete concurrent equipment outfits. Both smuggle entries are high-risk source *descriptions*, not endorsed automated Ironman trips. Source S1.

## Inventory and Rune pouch evidence blockers

R32 preserves every illustrated physical inventory position 01–28 as `CHECK/x?`. This is correct because none of the six original 7×4 diagrams survived the accessible indexed text as identifiable source-specific icon+quantity+position data. No empty icon, 1-dose bottle or rune stack may be filled in by inference. General 3-type regular / 4-type divine / 16,000 each rune-pouch facts do NOT certify a KBD setup. Blood Barrage rune costs can only be used to generate a *labeled calculated proposal*, never a screenshot transcription. Source S1/S6.

## KBD risk mechanics and loot verification

- Wilderness at ~42 is dangerous; lair itself non-PvP. Teleblocked player cannot operate the lever to escape. KBD task exception for Krystilia applies. Five source routes and optional requirements checked. Private instance costs 50,000 coins from bank/inventory; boss/player drop timers are different. Source S1, S2, S3.
- KBD 276 combat, 240 HP, 4 tick; melee max 25, dragonfire max 65, special max 50. Close range melee 1/3, each special 1/9; at long range each special 1/6. These are attack-type probabilities, NOT independent joint drop-roll odds. Water spell weakness 50% since 2024-05-29; KBD dragonbane. Source S1/S2.
- Always drops dragon bones x1 and black dragonhide x2. Separate 1/1000 dragon pickaxe **pre-roll**, heads 1/128, visage 1/5000, pet 1/3000, elite clue 1/450 or elite CA 1/427, Konar-only brimstone key 1/65. Standard drops and MMII conditional fire rune/javelin table support indexed. Source S2/S4.
- Rare drop table 8/128 and gem table 2/128 are source-backed. **However, exact sequencing and joint probabilities with pre-roll and tertiary rewards remain unknown** (B038 BLOCKED). Do not invent an aggregated “any rare in one kill” probability. Source S2.
- Item IDs: Kbd heads 7980, dragon pickaxe 11920, draconic visage 11286, Prince Black Dragon 12653. Source article indexed; current RuneLite 1.13.4 runtime check not performed. Source S4.
- KBD-derived Kbd heads may be changed into black Slayer helmet or DHC(b), and Dragon pickaxe may be converted to crystal/infernal or ornamented versions. Record single ingredient source when descendants are crafted; do not create duplicate rare-drop completions. Source S4.

## Remaining blockers and exact smallest evidence

| Claim | BLOCKED evidence | Precise recheck trigger |
|---|---|---|
| B013 | Per-method original 28-slot grids | Source pictures/markup with each actual item ID, quantity, slot index and blank slots |
| B014 | Original pictured switches/doses/teleports | Tooltip/image for each slot, distinguishing prose recommendations from pictured supply count |
| B017 | KBD rune-pouch icon and runes | Actual pouch contents, spellbook, quantities and variant, not a generic pouch capacity |
| B038 | Overall rare roll relationship | Boss-specific authoritative joint/pre-roll mechanism before compound probabilities |
| B043 | Wiki oldid and last-edit UTC | Direct original boss + strategy page stable revisions |
| B044 | Historical R08 IntelliJ and R04 master ZIP | Exact archives to compare; recovered R08 plain text lacks KBD |
| B048 | Source media/item IDs and image licences | Original source file pages/rights plus icon names and item IDs |

## Exact B001–B048 independent claim dispositions

| ID | TONGS verdict | Finding | Sources |
|---|---|---|---|
| B001 | PASS_INDEXED | 5 ranked equipment tables; AFK Magic is illustration/prose, not sixth rank table. | S1 |
| B002 | PASS_INDEXED | Risk-limited 4-item Melee has 5 columns and 11 gear rows plus Spec. Source snippet confirms structural shape and footnotes. | S1 |
| B003 | PASS_INDEXED | BIS smuggle Melee has 5 columns and 11 gear rows plus Spec, high-risk presentation only. | S1 |
| B004 | PASS_INDEXED | Ranged risk-limited has 4 columns x 10 rows. Original has no shield nor Spec row; do not invent ranked blank slots. | S1 |
| B005 | PASS_INDEXED | Crossbow Ranged has 5 columns x 11 rows with shield. | S1 |
| B006 | PASS_INDEXED | BIS smuggle Ranged has 5 columns x 11 rows; includes conditional 2H bow and shield alternatives, not a single legal kit. | S1 |
| B007 | PASS_INDEXED | AFK Magic: Dragon hunter wand plus Blood Barrage narrative corroborated; pictured equipment/icons NOT authenticated (B013/B048). | S1 |
| B008 | PASS_INDEXED | 5 ranked columns and original context-specific Melee risk/antifire/trouver/Slayer helm notes verified; do not turn source columns into whole worn sets. | S1 |
| B009 | PASS_INDEXED | Serpentine > faceguard and defender > shield are inside original BIS Melee cells. | S1 |
| B010 | PASS_INDEXED | Moonlight antler bolts > Sunlight antler bolts in single Crossbow ammunition cell; only hunters sunlight crossbow. | S1 |
| B011 | PASS_INDEXED | Slayer helm only when eligible on-task; source warns expensive gear at risk. Keep as conditional recommendation. | S1 |
| B012 | PASS_INDEXED | Tbow, Bowfa and Noxious halberd two-handed; no simultaneous shield. Crossbow shield mode separate. | S1 |
| B013 | BLOCKED | Six pictured 7x4 inventories cannot be authenticated via stripped indexed source. All original positions x?; no invented empties. | S1 |
| B014 | BLOCKED | Icon switch, teleport, doses and physical positions inaccessible; prose can support items but not grid. | S1 |
| B015 | PASS_INDEXED | Antidote++, antifires, restore, food, emergency teleports in prose; no fixed stack counts. | S1 |
| B016 | PASS_INDEXED | Source 1-hour/48-minute potion narrative is not source image proof of 28 physical grid counts. | S1 |
| B017 | BLOCKED | Original pictured pouch runes and stacks not recovered. Cannot claim specific KBD pouch preset. | S1 |
| B018 | PASS_INDEXED | Generic 3/4 rune-type normal/divine pouch and 16k max supported; does NOT certify pictured KBD contents. | S6 |
| B019 | PASS_INDEXED | No universal strict KBD entry quest/skill gate; recommendations and shield/armour unlocks conditional. | S1 |
| B020 | PASS_INDEXED | KBD counts for most black dragon tasks but NOT Krystilia task, confirmed boss+Slayer task page. | S2;S5 |
| B021 | PASS_INDEXED | Level ~42 Wilderness approach is PvP risk; lair is outside Wilderness. Teleblocked players cannot use lever to escape. | S1;S2 |
| B022 | PASS_INDEXED | Five travel routes enumerated: burning amulet, POH obelisk, Ghorrock, games necklace+obelisk, waka canoe; route-specific quests/diary/woodcutting only. | S1 |
| B023 | PASS_INDEXED | Risk caution and no auto endorse risky gravestone/alt smuggling; protected item counts must not become an absolute player guarantee. | S1 |
| B024 | PASS_INDEXED | Private instance 50k bank/inventory; separate death and ground timers (boss 30min, player 3min). | S2 |
| B025 | PASS_INDEXED | 276 combat, 240 HP, max melee 25/fire 65, NPC IDs 239/2642; boss IDs not item IDs. | S2 |
| B026 | PASS_INDEXED | Normal fire + toxic/shock/ice; poison 8, shock -2 stats, freeze 10 ticks, 50 special max. | S1;S2 |
| B027 | PASS_INDEXED | Adjacent melee 1/3; 1/9 each special adjacent, 1/6 each outside; effects/procs separate. | S1 |
| B028 | PASS_INDEXED | Shield+antifire nulls regular dragonfire; specials can still reach max10. Source protection matrix icons not fully independently certified. | S2;S1 |
| B029 | PASS_INDEXED | 5-tick weapons with 4-tick KBD, walk-under 1:1 and ~20% fewer attacks only if timed correctly. | S1 |
| B030 | PASS_INDEXED | 50% water weakness since 2024-05-29 supported by boss change log. | S2 |
| B031 | PASS_INDEXED | Always 1 dragon bones and 2 black dragonhide. Do not import regular black dragon table. | S2 |
| B032 | PASS_INDEXED | Dragon pickaxe pre-roll 1/1,000; separate general rare table/tertiary. Distinct acquisition provenance important. | S2 |
| B033 | PASS_INDEXED | Kbd heads tertiary 1/128. | S2;S4 |
| B034 | PASS_INDEXED | Prince Black Dragon pet 1/3000. | S2;S4 |
| B035 | PASS_INDEXED | Draconic visage tertiary 1/5000. | S2 |
| B036 | PASS_INDEXED | Elite clue 1/450 or 1/427 with elite CA; Brimstone key 1/65 Konar-only. | S2 |
| B037 | PASS_INDEXED | Main 128-denominator weapons/runes/resources/other list and MMII fire rune/javelin condition corroborated; not tertiary joint odds. | S2 |
| B038 | BLOCKED | Rare subtable 8/128 and gem 2/128 supported, but sequencing/exclusivity/independence with pre-roll and tertiary is not established. BLOCK joint-drop modeling. | S2 |
| B039 | PASS_INDEXED | Four Collection Log items, cross-source visage and pickaxe; collected ≠ KBD loot provenance. | S2 |
| B040 | PASS_INDEXED | Heads 7980; dpick 11920; visage 11286; pet 12653 supported by primary indexed Wiki item pages. Current RuneLite 1.13.4 constant check not done. | S2;S4 |
| B041 | REWORK | CORRECTION: transcript says KBD heads create Black slayer helmet / dark slayer helmet variant. Wiki supports BLACK SLAYER HELMET (normal/imbued), not dark; dark claw from Skotizo unlocks purple Slayer helmet. Retain King Black Bonnet 1,000 points and DHC(b) hard CA. | S4;S7 |
| B042 | PASS_INDEXED | Dpick crystal/infernal/cosmetic, visage DFS with variant-specific source/pathfinder requirements. Do not duplicate one consumed ingredient as new drop. | S4 |
| B043 | BLOCKED | Canonical live oldid, edit UTC and media/tooltip rights not available. Indexed crawl is not an immutable revision. | S1;S2 |
| B044 | BLOCKED | R08 plaintext indeed has no KBD section; original R08 IntelliJ and historical R04 full ZIP not recovered. Source-code/binary crosscheck BLOCKED. | P1 |
| B045 | PASS_POLICY | PASS_POLICY: wilderness risk restriction from owner; never enable Best in Bank for KBD route. Wiki confirms risk, not code implementation. | P2;S1 |
| B046 | PASS_POLICY | PASS_POLICY: bank/CLOG/loot-only observations do not establish KC. No RuneLite implementation asserted. | P2 |
| B047 | PASS_INDEXED | Wiki text CC BY-NC-SA 3.0 except exceptions; artwork Jagex/per-file copyright separate. | S8 |
| B048 | BLOCKED | Original source equipment/28-cell icon IDs and permissions not recovered; no production import or completed loadout certification. | S1 |

## Source index and evidence limits

- **S1:** https://oldschool.runescape.wiki/w/King_Black_Dragon/Strategies ; https://osrsindex.com/wiki/king-black-dragon-strategies?site=osrs_wiki
- **S2:** https://oldschool.runescape.wiki/w/King_Black_Dragon
- **S3:** https://oldschool.runescape.wiki/w/Slayer_task/Black_dragons
- **S4:** https://oldschool.runescape.wiki/w/Kbd_heads ; https://oldschool.runescape.wiki/w/Prince_Black_Dragon ; https://oldschool.runescape.wiki/w/Dragon_pickaxe ; https://oldschool.runescape.wiki/w/Draconic_visage
- **S5:** https://oldschool.runescape.wiki/w/Slayer_task
- **S6:** https://oldschool.runescape.wiki/w/Rune_pouch ; https://oldschool.runescape.wiki/w/Divine_rune_pouch ; https://oldschool.runescape.wiki/w/Blood_Barrage
- **S7:** https://oldschool.runescape.wiki/w/Slayer_helmet ; https://oldschool.runescape.wiki/w/Suq-a-nother_one ; https://oldschool.runescape.wiki/w/Skotizo
- **S8:** https://oldschool.runescape.wiki/w/RuneScape:Policies ; https://oldschool.runescape.wiki/w/RuneScape:Images_and_media_policy ; https://creativecommons.org/licenses/by-nc-sa/3.0/
- **P1:** Recovered R08 plaintext: BURROW_R08_WIKI_SOURCE_TRANSCRIPT_2026-11-01.txt; R31/R32 immutable archives
- **P2:** Project Burrow owner Wilderness Best in Bank prohibition, BQA-902 KC provenance rules

Public sources accessed via web search-indexed primary Wiki snippets and one historically mirrored source on **2026-11-01**, not immutable canonical article revision. OSRS Index is derived from the Wiki and **does not count as independent confirmation**. Retrieval crawler statements such as `last week` are not Wiki edit timestamps. Original Wiki image/file rights must be checked per file; Wiki article text generally CC BY-NC-SA 3.0 except exceptions; Jagex copyrighted game images are separately licensed.

## SPOONS targeted intake / next dispatch

1. Preserve R31 and R32 immutable. Mark RSR-0045 **L2 REWORK/BLOCKED**, never `QA PASS` or imported. The 40 indexed/policy supported facts remain provisionally accepted only within the narrow evidence scope.
2. Assign Researcher **B041 single-line correction** only, creating isolated candidate+diff. Independent Tongs recheck validates corrected wording. Do not modify cumulative Library or register under QA-only lane.
3. Carry B013/B014/B017/B038/B043/B044/B048 as open CHECK/evidence blockers. Do not require immediate plugin/Windows test and do not disable Wilderness Best-in-Bank restrictions.
4. If original Wiki images are recovered, perform exact 28-slot count per all six methods and rune pouch verification as a separate task. Only then seek source-revision-certified method approval.

**RELEASE HOLD / READ-ONLY:** No GitHub/code/Windows/canonical production ticket/Library mutation. Research QA has not established anything about the running plugin.