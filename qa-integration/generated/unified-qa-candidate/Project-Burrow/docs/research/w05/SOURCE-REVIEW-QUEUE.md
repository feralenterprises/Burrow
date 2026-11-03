# Wave 05 | Combat Achievement research provenance and blockers

**Scope:** staging only. No currently reviewed CA criteria have been promoted.  
**Canonical global count:** OSRS Wiki `/Combat_Achievements/All_tasks` reports 655 tasks and `/Combat_Achievements` reports 2,697 points as of the November 2026 retrieval; this number is **not** the number of verified local records.  
**Existing Chronicle legacy reference:** 290 distinct `(boss, name)` rows from the original `combat-tasks.tsv`, including 77 author-written tips, maintained byte-for-byte. This is an independent subset with its own historical omissions, and not a representation of the full canonical 655-task set.  
**BQA-906 rejected export:** 655 objects, 652 synthetic placeholders, three named rows. The archive is excluded from the bundled active intelligence. Source SHA is in `BQA-906-QUARANTINE.json`.

## Source anchors

- Overview and count: https://oldschool.runescape.wiki/w/Combat_Achievements
- Named canonical task listing: https://oldschool.runescape.wiki/w/Combat_Achievements/All_tasks
- Task groups by boss: https://oldschool.runescape.wiki/w/Combat_Achievements/Tasks_by_boss
- Individual detail pages need separate immutable revision (`oldid`) and exact game-check condition verification before any status can change to `VERIFIED_SOURCE`.

## Targeted accuracy holds

- Vorkath / **The Walk**: do not require attacks to deal damage or impose an invented four-tick cadence. Requirements vs possible two-tick ranged technique must be separately sourced and expressed.
- Commander Zilyana / **Animal Whisperer**: preserve the private-instance prerequisite as a game condition, not an optional tip. Existing legacy description references this, but remains unverified against an immutable source revision.
- **Vet'eran**: the referenced achievement is a 20-kill-count milestone, not a mechanic execution task. Do not conflate kill-count evidence with completion of unrelated restriction challenges.
- **The Fractured Archive / Ocellus**: leave any pending or unsupported CA names in CHECK rather than creating task identities ahead of an official release.
- Do not make "eligible to attempt" equal "task completed"; a future game-confirmed individual task feed is required.

## Final Wave 06 connection gates

1. Crosswalk each accepted canonical task by `(encounter,boss + exact name)` or verified canonical ID, not guessed name similarity.
2. Keep legacy Chronicle task rows visible until the replacement canonical row has independently reviewed task criteria.
3. Keep kill-count tasks and raids out of the current existing Chronicle display until owner authorizes changes to its scoped content rules.
4. A future authenticated client observer must prove `task id -> confirmed completion source` before marking COMPLETE; unknown stays UNKNOWN.
5. Do not apply CA tips as loadout blockers or bank requirements.
6. Promotion needs source revision and an actual evidence review; mechanically formatted oldid strings alone are not proof.

**Status:** 290 CHECK / 0 VERIFIED_SOURCE / 0 admitted BQA-906 records. No overwrite of R31, R32 or previous CA TSV.
