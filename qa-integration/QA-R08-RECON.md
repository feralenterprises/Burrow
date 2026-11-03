# R08 source reconciliation | 2026-11-03

**Branch tip audited:** `d77c55b9459b269841d2b29aba1a9a8f67c586f8` (parent of this workstream)  
**Child branch:** `cursor/p0-account-bank-memory-f628`  
**Unified candidate:** `qa-integration/generated/unified-qa-candidate/Project-Burrow/`

## Status: BLOCKED — packages not staged

Expected owner-tested R08 archives were **not present** in this workspace or on the QA branch:

| Package | Expected role | Status |
|---|---|---|
| `TRASHPANDA-R08-WINDOWS-TEST-LEAN.zip` | Owner Windows lean QA host | MISSING |
| `FERAL-DUAL-DEV-R01-…` / dual-dev R08 | Dual-dev HoverLite+TPT launcher lineage | MISSING (B38 dual-dev ZIP present under development-snapshots, not R08) |
| `BURROW-SLAY-R08-…` | Slayer wiki gear R08 intake | MISSING |

## What exists today

- Historical **R08-named unit tests** inside the unified candidate (`R08DemonUiTest`, `SlayerWikiGearR08JUnitTest`, etc.) — these are test fixtures, not owner R08 host ZIPs.
- Comments in `ActivityItemRefs` referencing R08 Slayer tables already baked into W09 lineage.

## Rule before any R08 merge

1. Stage the exact ZIPs with SHA-256.
2. Diff against unified candidate file-by-file (especially `BankWithdrawalFreezeGate`, Bank Memory, Guided Capture, SidebarHost, Sprites).
3. Preserve BiB O freeze gate byte-identity (`dd54167d…`) unless owner re-witnesses.
4. Keep HoverLite independently loadable.
5. Do **not** overwrite working features solely because another tree is newer.

## Hashes to capture when ZIPs arrive

```
sha256sum TRASHPANDA-R08-WINDOWS-TEST-LEAN.zip FERAL-DUAL-DEV-*.zip BURROW-SLAY-R08-*.zip
```
