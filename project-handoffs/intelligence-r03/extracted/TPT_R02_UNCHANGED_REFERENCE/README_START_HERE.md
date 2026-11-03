# 🦝 TPT Cursor Context Pack | Read This First

**Date:** 2026-11-03  
**Scope:** TrashPanda Tracker / Project Burrow information, research, engineering decisions, and inspected Wave 7.5 source archive  
**Status:** DEVELOPMENT / RELEASE HOLD  
**R02 correction:** Persistent Account Memory, first-run/guided sync and Bank Memory now documented as mandatory, unverified requirements

## The exact file to give Cursor

**`TPT_CURSOR_MASTER_CONTEXT.md`** is the one consolidated master brief. Put it in the root of your TPT Cursor workspace or ask Cursor to read it from this extracted pack. It records owner decisions, engineering constraints, four-tab UI, pathfinder, gear/Bank O, sessions/Chronicle, research and actual build/QA lineage.

For persistent workspace guidance, copy `AGENTS.md` to the repo root and copy `.cursor/rules/tpt-project.mdc` into the repo's `.cursor/rules/`. Both are compact pointers to the longer master and do not supersede actual current source.

**Read the R02 correction before any code audit:** `audits/REQUIREMENTS-GAP-REGISTER.md` explicitly records missing owner requirements that the previous pack omitted. Run `audits/WAVE-01-ACCOUNT-MEMORY-READ-ONLY.md` first; do not combine engines. The standalone `audits/WAVE-02-BANK-MEMORY-READ-ONLY.md` is for a later owner-authorized wave. These are audit instructions, not implementation or production QA evidence.

`SOURCE_TREE_INDEX.md` is an automatically generated file/class map of the included **Wave 7.5** development source ZIP. `references/` contains original project handoffs for auditability. `source/` contains the unchanged original W075 ZIP if Cursor needs a reproducible older source reference. `ui-reference/` contains the isolated Java Swing shell R02/W02 and actual image previews. `references/` also includes a 22-entry research archive with immutable R31 and unapproved R32/GEAR-W01 staging.

**Important:** Wave 7.5.1 is the **latest documented test-contract update** but its full source ZIP was not available in this compiled pack; the source bundled here is **Wave 7.5**, not the latest verified native candidate. Do not overwrite your actual current project with the bundled W075 source. No full Windows JDK17/RuneLite W0751 acceptance has been established.

## First Cursor prompt (copy/paste)

> Read `TPT_CURSOR_MASTER_CONTEXT.md`, `SOURCE_TREE_INDEX.md`, `AGENTS.md`, and any relevant `references/` files. Compare them against the CURRENT repo and show exactly what revision/branch/build I have, what is confirmed and what is only planned. Do not edit source yet. Preserve the four historic core 225px tabs pending the owner's Bank Memory tab placement decision, OSRS sprites, 11 worn and 4×7 inventory, independent Chronicle, reliable Bank R05 manual O workflow, separate HoverLite, no Wilderness Best in Bank and verified-only session/gear evidence. Identify the next safe implementation wave with targeted test cases. Release HOLD.

## What's here

- **One master context**: 550+ lines of compiled source-backed product decisions and current status.
- **Source navigation**: file/class tree and SHA receipt for materialized W075 source.
- **Persistent Cursor rules**: compact guidance for agents.
- **Original reference documents and preserved research archives**: R31, GEAR-W01, Sessions, implementation/QA and UI handoffs, without status promotion.
- **Standalone UI shell R02/W02**: actual Java Swing design source, screenshot previews and icon provenance, never to be deployed as RuneLite source.
- **Wave 7.5 code archive**: historical reference only, not a claimed newer live build.

Nothing in this pack edits GitHub, the canonical research registers, the user's local installed plugin, or OSRS account state.