# 🦝 TRASHPANDA TRACKER | PATHFINDER AI SELF-READ MASTER

**Edition:** R01, compiled 2026-11-03 (America/Los_Angeles)  
**Coverage:** Pathfinder independent QA Waves PQA-W01 through PQA-W09, exact baseline, isolated reconstructed W09 candidate, competing Cursor engine, selected broader TPT owner constraints.  
**Disposition:** **RELEASE HOLD**. No native RuneLite acceptance for these QA patches and no verified account progressing through 100/500/1,000 completed recommendations.

## How a new ChatGPT/Cursor session should read this

1. Read **`00-READ-FIRST.md`** (this file) completely.
2. Read **`06-MASTER-DOCS/MASTER-STATE-AND-DECISION-LEDGER.md`** for consolidated facts, provenance, contradictions and blocker priorities.
3. Read **`06-MASTER-DOCS/RELEASE-GATES-AND-NEXT-WAVE.md`** for the actionable next wave.
4. Read **`06-MASTER-DOCS/SOURCE-LINEAGE.md`** before changing any source.
5. Read **`06-MASTER-DOCS/OWNER-CONTRACT.md`** before changing UX, Bank O or account-capture logic.
6. For any individual issue, consult **`04-ISSUE-INDEX/ALL-WAVE-ISSUES.csv`** and then the ORIGINAL issue ledger inside its corresponding Wave ZIP. Never treat the condensed index as superior to its source.
7. If implementing a repair, first verify the actual current repo/worktree, not simply this archive. Only then choose the applicable patch; treat W09 patched source as isolated QA, never installed product truth.

## Critical distinction

- **Source baseline:** `02-PINNED-SOURCE/TPT-W0751-RUNELITE-135-ONE-CLICK-DUAL-DEV-FIX.zip`; SHA-256 `79e7d54a78a0bb76b6b3eca0e6804067b02b40a35e88339ee83a20408aa7ef3f`.
- **Wave 9 isolated source:** `02-PINNED-SOURCE/W09-ISOLATED-CANDIDATE-SOURCE-RECONSTRUCTED-NOT-PRODUCTION.zip`, reconstructed using the W08 cumulative patch **then** W09 incremental patch; the mislabeled W09 cumulative patch fails against pristine W0751. **NOT merged, NOT native tested.** Use receipt beside it.
- **Separate Cursor Pathfinder:** `03-PARALLEL-CURSOR/pathfinder_wave4_complete.zip`; SHA-256 `ae019db6888b5870770a0c4458ef6b12089718977d4dcc92ea262d22d42570be`. **DO NOT DROP THIS TREE OVER THE FULL HOST**: 112/288 top choices and 48/288 readiness choices differed in matched probes.
- **Broader owner context:** `03-PARALLEL-CURSOR/TPT_CURSOR_MASTER_PACK_R02_2026-11-03.zip`; this package contains historical W075 source, NOT W0751, and its Account Memory/Bank Memory correction is mandatory.
- **No progression certification:** 1,000 same-state recommendation calls are not 1,000 completed activities. Wave 9 reported **zero** verified 100/500/1,000 completed-activity journeys.

## What this archive does NOT prove

No native JDK17 Windows Gradle/RuneLite acceptance, Bank O coexistence for the new candidate, full first-run boss/Slayer/diary/CA capture, full collection-log completeness, native stable bank read after crash, or proof of sustainable Ironman progression. Synthetic UI and widget stubs are not game observations. Research descriptions are not release approval. Current GitHub/main is not established as equal to either candidate.

## Artifact layout

`01-WAVES-ORIGINAL/` contains the untouched original 9 ZIPs plus their 9 owner reports.  
`02-PINNED-SOURCE/` contains unchanged W0751 and the isolated W09 reconstructed source snapshot plus a provenance receipt.  
`03-PARALLEL-CURSOR/` contains standalone Cursor Wave4, independent L2, Cursor Wave5 prompt, and larger TPT master-context R02 pack.  
`04-ISSUE-INDEX/` contains every source-provided CSV issue row with its wave provenance.  
`05-PATCHES-AND-REPRO/` contains both successful reconstruction patches and an explicit forensic note showing W09’s mislabeled cumulative patch fails cleanly.  
`06-MASTER-DOCS/` contains synthesized agent handoff, owner constraints, lineage, release gates, wave index and continuation prompt.  
`SHA256SUMS.txt` and `MANIFEST.json` verify package contents.

**Important packaging correction:** W09 original reported a direct cumulative patch pass, but a fresh independent `git apply --check` fails. The verified reconstruction is W08 cumulative → W09 incremental; see forensic note.

**Safe default for the next session:** audit first, do not implement by copy-over, keep release HOLD, never fabricate unknown resource/account state, avoid altering Bank O, and report concrete evidence of improvements rather than test-count theater.