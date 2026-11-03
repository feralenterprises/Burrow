# QA merge gates: executable acceptance contract

**Target:** a new combined TrashPanda Tracker RuneLite QA candidate on GitHub, not historical archive restoration alone.

| Gate | Acceptance evidence | Current status |
|---|---|---|
| Pin host source | Exact original ZIP and SHA, Git commit, owner QA lineage, license/assets | OPEN: latest owner-tested post-B38 source unclear |
| Full source staging | All nested source ZIPs recoverable and isolated, no silent replacement | AVAILABLE via GitHub archive restore + QA prepare script |
| Pathfinder | 48+ profile deterministic regressions, no impossible prereqs, no fake advancement; goal convergence check | ISOLATED W09/independent Cursor divergent; merge pending |
| Bank Best in Bank | B38 four manual withdrawals still work with O, no evidence revalidation closure | Previous narrow owner QA only; rerun after merge |
| First-run baseline | Account-specific persisted skills/XP, quests, diaries, CAs, KC/log, Collection Log, captured only when observable | RELEASE BLOCKED |
| Guided capture / sync | Manual click-through workflow, timestamped evidence, explicit refresh, 7-day staleness notice | RELEASE BLOCKED |
| Bank Memory | Last real scan persisted per account with variant, state, count, timestamp; readable bank closed; freshness visible | RELEASE BLOCKED, separate from O tab |
| Four-tab UI | Home / Pathfinder / Outfitter / Sessions; 225px; authentic icons; approved lean armored raccoon; 4 × 7 inventory | R10C standalone shell only |
| Chronicle independence | Prev/next and boss picker never change Outfitter selected boss/method/tier/loadout | Requires native regression |
| Sessions | KC and logs from ring-of-wealth/enchanted gem game widgets; no invented KC | Requires native regression |
| Research R31/R32 | Immutable R31; R32 exact original SHA/manifest before promotion | R31 immutable; R32 original recovered and manifest 83/83 verified; research still staging with 7 QA evidence blockers |
| Gradle Java17 | CI/Windows exact Java17 build and focused tests; no broken dependencies | NOT YET RUN ON QA INTEGRATION |
| Owner native QA | Real RuneLite single-client dual-plugin launch, account hop/logout, BIB O, snapshot persistence, UX inspection | NOT YET RUN |

**Never auto-bank, automate gameplay, publish to Plugin Hub, force-push, or merge to `main` during QA setup.**

## Report format for Cursor

Provide source hash and code path for every implementation claim, then a change list with owner-visible behavior, exact test steps, expected results and targeted diagnostics. Do not substitute document existence for implementation acceptance.
