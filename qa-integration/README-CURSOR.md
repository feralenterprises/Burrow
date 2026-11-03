# TrashPanda Tracker | Unified QA Integration Branch

**Branch:** `qa/tpt-integration-20261103` | **Owner objective:** put historical source, research, UI, current QA input candidates and documentation in one GitHub workspace so Cursor can build a new merged RuneLite QA candidate.

**Current disposition:** QA SOURCE STAGING. **Not a tested integrated QA build, production source promotion, or Plugin Hub release.** The branch was created from the existing UI/Cursor archive branch, preserving its 36 reconstructable master-package parts and separate source/reference trees.

## How Cursor prepares actual source trees

On Windows (JDK 17 expected for testing), in repository root:

```powershell
git fetch origin
git switch qa/tpt-integration-20261103
git pull --ff-only origin qa/tpt-integration-20261103
.\qa-integration\00-PREPARE-QA-SOURCES.cmd
```

The script verifies the two historical master archives, extracts all 92 outer entries + 27 nested ZIPs as archived, then creates **separate, SHA-verified source directories** in `qa-integration/generated/`. It does **not** merge them or modify `main`.

**Navigate to:**

- `qa-integration/generated/owner-b38/`: the owner-tested earlier Best-in-Bank R05 / green O snapshot, with HoverLite dev copy
- `qa-integration/generated/host-w0751/`: full RuneLite-host baseline
- `qa-integration/generated/pathfinder-w09/`: isolated cumulative Pathfinder W08→W09 QA candidate, not production
- `qa-integration/generated/host-w075/`: W075 historical precursor for provenance
- `qa-integration/generated/cursor-wave4/`: independent, divergent Pathfinder engine
- `qa-integration/generated/gear-research/`: R31 + Gear W01 immutable research and staging
- `ui-shell/r10c/`: approved 225 px UI shell, tracked independently; **not** integrated plugin code
- `project-handoffs/research-r32-staging/`: R32 read-only QA notes; the original R32 archive is **missing**

Read `MERGE-SOURCE-INVENTORY.md` and `MERGE-GATES.md` before writing any plugin source.

## Intended merged QA candidate

The goal is a **single JDK17 RuneLite plugin test candidate**, integrating the chosen, source-verified host, Pathfinder fixes, BIB O/Bank Memory and first-run capture, Outfitter/gear intelligence, Sessions/KC, and the approved R10C shell. It must retain the four main tabs, 225 px width, 4 × 7 inventory and original armored raccoon. HoverLite remains an independent development plugin.

**Do not call a source archive an executable merged build.** The W09 candidate differs from the independent Cursor Pathfinder line; do not wholesale replace classes. Resolve contracts and test one subsystem at a time. No automatic login, banking or game actions.

## Safety and release gates

- Do not touch `main`, alter immutable R31, rewrite the missing R32, claim unobserved KC/ownership, or publish to Plugin Hub.
- Run `qa-integration/01-RUN-OFFLINE-QA-WINDOWS.cmd` only after reviewing the candidate source. It targets the **isolated W09 candidate** for offline smoke/regression tests and does **not** prove live RuneLite gameplay.
- Only an independently reviewed, traceable, merged source tree plus real Windows RuneLite owner QA can become an approved QA build.
- Missing latest owner-tested SLAY-R08 / other post-B38 host source and missing original R32 ZIP are **open intake blockers** until their exact artifacts are recovered and SHA-verified.

## Quick audit result

The GitHub branch now houses recovered complete master archive bytes, readable handoffs, R31 in the nested research compendium, B38 historical QA ZIP, W075/W0751 full hosts, W09 isolated candidate, Gear W01 research, R10C UI shell, Pathfinder QA reports and R32-specific QA notes. It **does not yet prove** every historical Library file or later working build is present.

The source matrix, missing-artifact register and QA gates are authoritative status summaries here. When later source becomes available, add new pinned inputs on this QA branch, never silently replace an existing pin.
