# CURSOR DISPATCH | TPT INTELLIGENCE AUDIT | WAVE 02: PERSISTENT BANK MEMORY

**MODE:** FUTURE OWNER-AUTHORIZED READ-ONLY AUDIT. **ONE ENGINE ONLY:** capture/persist/display/reuse last-seen bank state. **DO NOT START JUST BECAUSE WAVE 01 EXISTS.** No code or publishing work until owner directs a repair wave.

## Mission

Prove or disprove that a complete, original authenticated bank read can be saved locally per account and later displayed and consumed by Outfitter/Best in Bank with correct *last-known* semantics while the bank is closed. This engine is distinct from the temporary native `O` / Bank Tags preparation lease and from account progression memory.

## Exhaustive test areas

- Identity and completeness: original/full bank widget vs placeholders, empty slots, noted items, stacking, quantities, charged/degraded/imbued/ornament variants, worn and inventory kept separate, inventory placeholders and state uncertainty, account scope, ordered generations and timestamps.
- Capture lifecycle: first bank opened, full scan confirmed, early-close partial read, Bank Tags filtered O tab, 4 manual withdrawals while lease remains active, withdraw quantity changes, original-bank reopen, account switch, logout, world hop, stale async task, missing widget and hidden view; do not let O tab overwrite full-bank memory.
- Storage: atomic save, schema migration, encrypted? (report what actually exists, do not assume), per-account data directories, corruption recovery, persisted exact IDs and counts, account A→B→A, restart with bank closed and last verified age displayed.
- Consumers: gear ownership lookup, weapon/ammo pairing, equipment eligibility, notes vs usable stock, set effects, optional vs hard blocker, quantity math, stale/unknown vs zero, Wilderness no-BIB, provisional research no-preload, exact withdrawal count and owner manual only. No stale snapshot silently promoted to current `READY`.
- UI contract: dedicated reachable Bank Memory tab/view, item searchable/displayable if implemented, last sync timestamp and manual refresh prompt. Existing four-primary-tab design versus newer explicit owner Bank Memory tab is an unresolved placement decision, not permission to omit feature.
- Failure scenarios: first-run no bank read, cache empty/partial, player sold/used gear since scan, mismatched game world/account, unavailable bank, placeholder-only matching, wrong charge count, failed disk write, stale cache time, repeated bank opens, bank ownership changes while O lease active, nested view navigation and idle/offline use.

## Deliverable

Actual class/method path-line evidence, exact binary/commit identity, full component/source graph, deterministic fixture output, JDK17/native test boundary, P0/P1/P2 defects and a narrowly scoped repair proposal. Verdict: `VERIFIED`, `NEEDS REPAIR`, or `NOT YET VERIFIED`. **This wave is not a visual integration wave and is not approval to change current primary tab structure.**