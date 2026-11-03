# 🦝 SPOONS | BQA-906 independent intake R01

**As of:** November 2, 2026 Pacific | **Verdict:** REWORK / RELEASE HOLD | **Source ZIP SHA-256:** `db600a04531915f447f203606bdd97a4749e065a52d33f54860a66d075783742`

## Results first

The archive is structurally readable, CRC-clean and contains **5 files** (total **15,921 uncompressed bytes**). It is **not** a complete CA corpus. The included YAML has **3 structured tasks**, while the summary claims **655**. The tier arithmetic for 655 tasks / 2,697 points agrees with the OSRS Wiki, but no 655-row ledger was delivered.

**No task row is approved for TPT import from BQA-906**. This is a judgment about the submitted evidence, not a claim that every task name or tactic is false. Existing historical TPT CA research remains separate and unchanged.

## Machine evidence

- Source: `BQA-906_PROJECT_BURROW_COMPLETE_AUDIT_EXPORT.zip`; SHA-256 `db600a04531915f447f203606bdd97a4749e065a52d33f54860a66d075783742`; ZIP CRC `PASS`; duplicate ZIP names `0`.
- Included task rows: **3**. Names/IDs: `CA_VORKATH_001` The Walk; `CA_TFA_001` Tendril Blocker; `CA_TFA_002` Perfect Ocellus.
- Canonical task IDs present: **0/3**. Row-level source URLs: **0/3**. Stable source revisions/oldids: **0/3**. Validation-status fields: **0/3**.
- Draft-07 JSON schema validation: **FAIL (2 errors)**.
  - `$.metadata`: 'total_tasks' is a required property
  - `$.metadata`: 'total_points' is a required property
- `runelite_version` in schema vs `target_environment` in data is a nonrequired naming inconsistency, separate from the two actual validation failures.
- 11/11 completion claim not corroborated by 11 separate batch evidence records; actual prose contains five thematic audit sections.
- A printed PR commit is only a claim. The export does not contain a verifiable repository receipt, actual commit diff or CI proof.

## External source crosscheck (public documents checked November 2, 2026)

**Confirmed:** The OSRS Wiki currently lists 655 tasks and 2,697 points, split Easy 41, Medium 64, Hard 89, Elite 166, Master 173 and Grandmaster 122. Source: https://oldschool.runescape.wiki/w/Combat_Achievements and https://oldschool.runescape.wiki/w/Combat_Achievements/All_tasks.

**Contradiction P0 — The Walk:** BQA-906 says deal damage 12 times with fixed 4-tick movement and lists DHCB/Bowfa/blowpipe as gear requirements. The task requires 12 *hits*, not 12 successful damage splats; wiki states even 0-damage attacks count. It describes 2-tick ranged Woox-walking and alternate multi-hit melee solutions. Preserve objective separately from optional execution methods. Source: https://oldschool.runescape.wiki/w/The_Walk.

**Contradiction P1 — Animal Whisperer:** Actual task explicitly requires killing Zilyana in a **private instance** without damage from boss/bodyguards; BQA-906's tactic omits the private-instance condition. Source: https://oldschool.runescape.wiki/w/Animal_Whisperer.

**Contradiction P1 — Vet'eran / Callisto Adept / Venenatis Adept:** Current boss CA list describes these as specific **kill-count** achievements, not the tactical sequences supplied as if task definitions. Tactics may be useful, but the award condition must be accurate. Source: https://oldschool.runescape.wiki/w/Combat_Achievements/Tasks_by_boss.

**Raid IV:** The Fractured Archive was released on October 20, 2026, but Jagex's October 6 briefing said bosses would *eventually* receive Combat Achievements. As of this review, the submitted Ocellus CA task names, IDs and release state are not supported by an official task listing. Keep `PROPOSED / CHECK` and do not count them as released CAs. Sources: https://oldschool.runescape.wiki/w/Update%3AThe_Fractured_Archive_-_Everything_You_Need_To_Know?mobileaction=toggle_view_desktop ; https://oldschool.runescape.wiki/w/Combat_Achievements/All_tasks ; https://www.mrshacostore.com/blog/osrs-raids-4-fractured-archive (secondary dated Nov 2, states CAs not out yet).

## Canonical project boundary

- The older CP9-final independent source audit documented **290 CA task rows, 77 nonempty tips** and **213 untipped**. These are *not* overwritten, combined, or regarded as proven source-revision accurate by this intake.
- TPT Outfitter's owner scope excludes raids; a separate CA research catalogue may mention raids but must not unlock their Best in Bank plans or imply gameplay support.
- Never infer account CA completion from a guide, KC estimate, source list or claimed simulated test.
- Existing R31 and R32 history are immutable. This report does not modify the canonical BUR/RSR registers, Plugin Hub, project sources or user data.

## Exact return contract for BQA-906 producer

1. Supply the full **655-row** source ledger, one unique canonical task ID, exact name/boss/tier/type/award objective and point value per row. Tier totals must be recomputed from real rows.
2. For every task, include `source_url`, Wiki `oldid`/revision (or `RECHECK_UNPINNED`), UTC access time, source text pointer, provenance and independent evidence status. Unavailable tasks remain `UNKNOWN`, not invented.
3. Separate exact success condition (`objective`) from optional `strategy`, `tick_model`, `method_prerequisites`, `risk`, and `tested_environment`.
4. Fix schema/data field names. Require a programmatically executed Draft-07 validation transcript and explicit negative tests.
5. Put new Raids IV mechanics in **research-only** pending an official CA release, published task identifiers and independent audit; do not inflate the canonical count.
6. Provide claimed 11-batch register plus each immutable artifact, checksums and per-claim Tongs independent L2 receipt; do not self-approve.
7. Provide GitHub/repo commit evidence if integration is claimed; source/build hash, Java 17 Gradle/run and owner-native receipt as *separate* gates.

**NEXT_FOR_SPOONS:** first receive the actual canonical structured task corpus and revisions, validate its row count/schema/task identity, then prioritize per-boss mechanical counterexample audits. No release or automatic import.

## Issue ledger

See `BQA-906-ISSUES-R01.csv` for {len(issues)} individually scoped findings and correction contracts.

## Included source manifest

| File | Uncompressed bytes | SHA-256 |
|---|---:|---|
| `BQA-906-BOSS-CHRONICLE-CA-MASTER-COMPLETE.md` | 3894 | `ba7b2483dc66d9a050ee5dee9f60bb240a7f9055ccaaf4bdef326a2f4261cc92` |
| `LAYER_2_MECHANICAL_EXECUTION_AUDITS.md` | 4443 | `ec263ef487047f9f65fa92b82752d187f889829486149dac8e407a66c40d13d7` |
| `PR_SUMMARY_AND_COMMIT_LOG.md` | 1656 | `e1d62b8c147416a2cb03919bc9a4c122ab8230eba687f59fe380f217f0977dc2` |
| `runelite_ca_ledger_schema.json` | 2684 | `e5b3312ac53f342d55b266ba04cda39f8d2bf7c473087653aeb326e7401f9438` |
| `runelite_ca_sample_ledger.yaml` | 3244 | `c3428a6387a844d7b71f6ccbe39a1716ee3a26f19935d24e06296093ac05b0b1` |