# Original Research Dispatch R32 | SHA-verified GitHub recovery

**Status:** ORIGINAL RECOVERED and GitHub stored, **research staging only**. Do not promote to immutable R31 or production.

The Library root contained the original `BURROW-RESEARCH-DISPATCH-R32.zip`. A separately saved `BURROW-RESEARCH-CLAUDE-CONSOLIDATED-2026-11-01-READONLY.zip` also contained a byte-identical copy. Original ZIP SHA-256: `36176521201338156334898582ee12077baa929fe42bf8c3846ef327440b1d0a`; size 351,885 bytes. All 84 ZIP entries pass ZIP CRC checks and all 83 manifest entries match their documented SHA-256.

The original bytes are stored here in **4 Base64 text parts**, SHA-recovered by `RESTORE-R32.ps1`.

In a Windows checkout of `qa/tpt-integration-20261103`:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\qa-integration\research-r32\RESTORE-R32.ps1
```

This regenerates `qa-integration/generated/BURROW-RESEARCH-DISPATCH-R32.zip` and validates every entry. The generated ZIP is intentionally Git-ignored; the original complete content remains in the tracked parts.

R32 contains the original, **pre-B041-correction** claims. The later isolated correction and Tongs recheck are separate QA evidence under `project-handoffs/research-r32-staging/`. Seven evidence blockers remain; no production import.
