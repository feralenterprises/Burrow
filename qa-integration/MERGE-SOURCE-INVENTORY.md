# QA integration: source-of-truth matrix | 2026-11-03

## Byte-verified source/archive inputs

| Role | Stored on QA GitHub branch | Verified SHA-256 | Source / size / status |
|---|---|---|---|
| Complete intelligence handoff R03 | `project-handoffs/archive-recovery/parts/`, 15 pieces; decoded locally | `708c5a26d902426af9edc9c65339fb49711a53f37d607ae8b6f06641cd11eaad` | 7,073,780 B original, 53 entries, 13 nested ZIPs; historical |
| Complete Pathfinder master R01 | `project-handoffs/archive-recovery/parts/`, 21 pieces; decoded locally | `94d135bd88db436c0f952df4683786deb09aff75737be15a6bd6ddbb44bcebac` | 10,111,186 B original, 39 entries, 14 nested ZIPs |
| Earlier owner-tested B38/BANK R05 | `development-snapshots/B38/...WINDOWS-QA-LEAN.zip` | `711dd92392d63cde669d646e195610e2d0ca12a17683249874f9231e943ca0ec` | Owner witnessed 4 manual BIB/O withdrawals, **not** certification of all features |
| Historical full host W075 | `restored-full/intelligence-r03/TPT_R02_UNCHANGED_REFERENCE/source/TPT-INTELLIGENCE-W075-MULTISTYLE-GEAR-CLEAN-DUAL-DEV-OWNER-HOLD.zip` | `eea049b7ad56d12de7f3354333579783325c6785f2b4d2932496cd237bfb696c` | 435 ZIP entries, 368 Java; historical |
| Full host W0751 | `restored-full/pathfinder-r01/02-PINNED-SOURCE/TPT-W0751-RUNELITE-135-ONE-CLICK-DUAL-DEV-FIX.zip` | `79e7d54a78a0bb76b6b3eca0e6804067b02b40a35e88339ee83a20408aa7ef3f` | 441 entries, 368 Java; pinned test-contract repair, release hold |
| W09 isolated Pathfinder candidate | `restored-full/pathfinder-r01/02-PINNED-SOURCE/W09-ISOLATED-CANDIDATE-SOURCE-RECONSTRUCTED-NOT-PRODUCTION.zip` | `3e696901cc14d04a04c58473410f8548fcb47485f63baf596ba41d54e3c3ec13` | 452 entries, 379 Java; W08 cumulative + W09 incremental; not production |
| Cursor Pathfinder Wave4 independent | `restored-full/pathfinder-r01/03-PARALLEL-CURSOR/pathfinder_wave4_complete.zip` | `ae019db6888b5870770a0c4458ef6b12089718977d4dcc92ea262d22d42570be` | 88 entries, 25 Java, divergent contracts |
| Gear/Research Compendium Nov 2 | `restored-full/intelligence-r03/TPT_R02_UNCHANGED_REFERENCE/references/TPT-SPOONS-RESEARCH-COMPENDIUM-2026-11-02.zip` | `d99f06386783b8784108f56fbeb1d8d90c6bafd01c6b4d50fd40e35cd99f38b5` | 22 entries, Gear W01 and immutable R31 subarchive |
| Approved UI shell R10C | `ui-shell/r10c/` | Git blob SHA provenance, no standalone full-host SHA | Separate 225px Java Swing demo; not RuneLite panel |

All paths beginning with `restored-full/` are relative to `project-handoffs/archive-recovery/`, produced by the verified GitHub restoration script. They're not independent tracked binary ZIPs on GitHub but the original binary bytes are reconstructable from the tracked archive parts.

## Known-source divergence and integration risks

- **W09 and Cursor Wave4 are NOT one engine.** Prior measured 112/288 top-suggestion disagreements, 48/288 readiness differences. Signatures/activities differ. Select behavior with unit tests and real owner decisions.
- **Bank R05** has witnessed behavior on B38. Preserve O tab and 4 successful withdrawal semantics. Bank Memory is a persistent historical snapshot, not the temporary O tab; never infer live stock after bank closure.
- **First run / account truth**: full baseline capture, seven-day reminders, guided click-through and per-account persistence remain native acceptance blockers despite isolated candidate test passes.
- **R10C** shell is a standalone artifact. Preserve four tabs (Home, Pathfinder, Outfitter, Sessions), Chronicle selection independence, authentic approved armored raccoon. Do not use rejected chunky raccoon.
- **Sessions** boss/slayer logs and KC must use actual game evidence with provenance, not loot or manual assumptions.
- **R31 research** immutable. Original R32 archive is now SHA-verified and GitHub-reconstructable; R32 remains staging-only, not a canonical promotion.
- **Latest owner-tested source** is NOT established by git branch names or timestamps. B38 is scoped owner QA; W0751 and W09 are newer source snapshots/isolated candidates. Need separate source/hash and Windows QA for reported SLAY-R08 and subsequent changes before picking the true integrated host.

## Exact next merge order

1. Pin latest owner-tested RuneLite source (SLAY-R08 / dual R08 intake found in Library, GitHub staging pending); retain B38 for regressions. Original R32 is now recovered under `qa-integration/research-r32/`.
2. Define one canonical account-progression snapshot model and first-run provenance/freshness contracts.
3. Reconcile Pathfinder W0751/W09 with Cursor engine behavior, do not replace wholesale.
4. Integrate persistent Bank Memory as a subordinate view, maintain BIB O lifecycle and equipment variants.
5. Integrate approved R10C visuals in actual RuneLite 225px panel, keeping four root tabs.
6. Sessions provenance and KC capture, owner-visible evidence gates.
7. JDK17 build/test and independent Tongs QA, owner Windows RuneLite playback, final approval.

| Original research dispatch R32 | `qa-integration/research-r32/parts/` four parts; restore script | `36176521201338156334898582ee12077baa929fe42bf8c3846ef327440b1d0a` | 351,885 B, 84 ZIP entries, 83/83 manifest items verified; **research staging, unapproved** |

## New verified November 3 inputs awaiting isolated GitHub upload

- `BURROW-RESEARCH-DISPATCH-R32.zip` recovered in QA branch Base64, SHA-256 `36176521201338156334898582ee12077baa929fe42bf8c3846ef327440b1d0a`.
- `TRASHPANDA-R08-WINDOWS-TEST-LEAN.zip` local verified original, SHA-256 `2f20de6ee111d3d1a6a10ece68b226afb15f6b8f30ba6497acd357b9d38d0225`. GitHub source upload still in progress unless path verified.
- `FERAL-DUAL-DEV-R01-TRASHPANDA-R08-HOVERLITE-v0.3.0-LEAN.zip` local verified original, SHA-256 `b380c4128aa3bf753c8e5ed66906fd2024532c6dfd48e7fcd80515914d96f675`. GitHub source upload still in progress unless path verified.
- `TPT-SPOONS-ALL-CHATS-UI-CONSOLIDATION-2026-11-03.zip` local verified owner/conversation consolidation, SHA-256 `fd79c0a5c985f9ef28b020d3986db9760e187886491d9c15c4831c5401bb3d91`; larger archive is not yet independently pinned on GitHub.
- `BURROW-SLAY-R08-WIKI-RANKED-SLAYER-GEAR-WINDOWS-CANDIDATE.zip` local verified full development candidate, SHA-256 `b477022ee195e02e942199e76bb9ca692b9eee397a206d000316c6ce146a7a0f`; staged copy is not yet independently pinned on GitHub.

**Integration decision:** Prefer source hashes and narrow owner QA over build numbers. Never declare the merged runnable QA source complete from the presence of documentation or archives alone.
