# BankObservationCache resolution

**Disposition:** Documented replacement — do **not** dual-wire into production.

## What BankObservationCache is

`gg.feral.burrow.core.BankObservationCache` is an isolated, in-memory attestation core
(BUR-0023/0042). By its own contract it has:

- no RuneLite event subscriptions
- no persistence
- no Best in Bank integration
- no UI

It was designed as a future client-thread adapter target, not as the live production path.

## What production already uses instead

| Concern | Production path |
|---|---|
| Live bank read | `AutomaticBankSnapshotReader` → `BankEvidence` → `BestBankPreloader.observe` |
| Closed-bank as-of-close planning | `BestBankPreloader.markBankClosed` / `refreshCarried` |
| Durable restart-safe bank snapshot | `PathfinderAccountArchive.rememberBank` / `readBank` |
| Historical planning after restart | `BankEvidence.fromHistoricalBankMemory` → `BestBankPreloader.observe` (this P0) |
| O tag / withdrawals | `BestBankLayoutGate` + `BankWithdrawalFreezeGate` (require **live** open bank) |
| Pathfinder current stock | `PathfinderAccountTruth.capture` (rejects closed / historical) |

## Why not wire BankObservationCache now

1. Dual caches create conflicting sources of truth for the same account/session.
2. Production already has stronger live gates (`BankScanTracker`, open-epoch, freeze gate).
3. Durable needs are met by `PathfinderAccountArchive` with sealed ConfigManager records.
4. Wiring BOC without a verified full-load attestation adapter would invent readiness signals its own docs forbid.

## Revisit condition

Only introduce a BOC adapter if owner R08/native work proves the current `BankEvidence` capture path cannot attest full-bank load, **and** a single production path is designed to supersede (not parallel) `BestBankPreloader` evidence.
