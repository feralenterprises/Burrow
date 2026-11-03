# TPT | Owner Requirements Gap Register | 2026-11-03 R02

**This register exists because multiple previously discussed requirements were not reliably carried into the earlier master plan.** It is a noncanonical audit register, **NOT** a new `BUR-*` ticket and not code evidence. Nothing is marked implemented until exact source + test + native receipts support it.

| Local gap label (not ticket ID) | Explicit owner requirement | Current verified outcome | Hard gate |
|---|---|---|---|
| `GAP-ACCOUNT-001` | First-use baseline of boss log/KC, Combat Achievements, quests, diaries, skill levels/XP | **UNKNOWN/NOT AUDITED**; historical quest scans and Sessions history do not demonstrate a complete durable baseline | Enumerate sources by domain, verify actual available API/UI surface, save supported observations and flag unsupported |
| `GAP-ACCOUNT-002` | Persist per authenticated account across logout, restart, migration, account switch and crash | **UNKNOWN/NOT AUDITED**; do not infer from a separate 10-session history implementation | Restart, A→B→A, migration and corruption tests against real store |
| `GAP-ACCOUNT-003` | First-run guided scan when direct API data unavailable | **UNKNOWN/NOT AUDITED** | Real manual screen navigation + capture path, truthful coverage and no automated game clicks |
| `GAP-ACCOUNT-004` | Manual sync and approximately seven-day resync reminder if no recent verified update/progression | **UNKNOWN/NOT AUDITED** | Independent last-change and last-verified timestamps, prompt snooze, source-unavailable behavior |
| `GAP-BANK-001` | Durable full-bank snapshot on authentic bank open, saved per account with age and stock evidence | **UNKNOWN/NOT AUDITED**; earlier bank preloader or temporary Bank O lease is not proof | Original-bank completeness, identity and crash-safe persistence |
| `GAP-BANK-002` | Dedicated Bank Memory tab/view displaying saved bank details | **UNKNOWN/NOT AUDITED** | Actually usable native UI and final nav placement after owner clarification |
| `GAP-BANK-003` | Outfitter / BIB reads saved bank knowledge while bank closed with stale/last-known boundaries | **UNKNOWN/NOT AUDITED**; never assume cached stock is live current ownership | Stale-state policy, blockers, snapshots, cross-account isolation, manual Bank O restrictions |

## Conflicting owner decisions, not permission to drop requirements

- Through October 2026 the owner explicitly required **four primary tabs**: Home, Pathfinder, Outfitter, Sessions. `O` in the actual bank was not a fifth sidebar tab.
- On November 3 the owner explicitly requested **another tab for Bank Memory**. This is a later owner decision and must be honored as a feature request. The UI-navigation implementation must clarify whether this new tab is a fifth primary navigation destination or a dedicated nested tab/panel; earlier four-tab language must not cause Cursor to delete or hide the feature.
- A **saved Bank Memory inventory** is **not the same thing** as a temporary `trashpanda commander` bank preparation/tag tab. Preserve the working native manual Bank O lease; do not conflate or replace it.

## Nonnegotiable audit outcome labels

`SOURCE-PROVEN`, `PARTIAL-PROVEN`, `MISSING`, `UNVERIFIED`, `BLOCKED-BY-API`. Cite exact class/method/line and tests for each. A roadmap paragraph is **not implementation proof**. Do not claim whole-product completion from a passing small synthetic test.

## Release blocker

Account baseline persistence, guided/manual refresh, and Bank Memory are **owner-required acceptance conditions**. Their presence, reliability, UX and exact RuneLite API availability must be proven before declaring TPT release-ready. The UI aesthetic integration itself still comes last.