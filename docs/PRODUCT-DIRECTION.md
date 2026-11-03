# Product direction · TrashPanda Tracker 🦝

**Our promise:** *We dig through your account so you don't have to.*

TPT should help players spend less time interpreting interfaces and more time actually playing Old School RuneScape. Useful recommendations must be grounded in **what the account has**, **what the activity requires**, and **what remains uncertain**.

## Four places, one coherent experience

| Area | Player question |
| --- | --- |
| **Home** | “What's worth doing next?” |
| **Pathfinder** | “What can I realistically attempt?” |
| **Outfitter** | “What should I equip and bring from my bank?” |
| **Sessions** | “What progress have I actually made?” |

The compact sidebar is designed around roughly **225px width**, vertical scrolling, legible information, authentic OSRS item/icon recognition, and a friendly armored raccoon identity. Equipment and the 4-column, 7-row inventory should mirror the way players think about real in-game slots.

## Intelligence before decoration

- **Bank-aware, never bank-controlling.** Recommendations can help identify items; the user performs the banking actions.
- **Mechanics before best-in-slot.** Protect against genuinely required equipment gaps without labeling a viable lower-tier setup impossible.
- **Variants matter.** Charged, degraded, corrupted, ornamented, imbued, ammunition, set effects, and activity-specific exceptions must be understood deliberately, not guessed from a name.
- **Explain uncertainty.** A missing bank snapshot, unknown requirement, or unverified source should say **verify**, not invent an answer.
- **Independent controls.** Changing one view, such as a boss Chronicle entry, should not silently rewrite another view's chosen Outfitter setup.
- **Progress from evidence.** Kill counts and activity history must come from legitimate observed events, not inferred drops.

## Public-release boundary

This file describes the intended player experience. It is **not** a list of completed source features. Our current GitHub Java prototype predates newer owner-tested development builds. The public release will follow a reconciled implementation, reproducible Java/RuneLite build, owner QA, and Plugin Hub review.

Read the [development log](DEVELOPMENT-LOG.md) for a dated status report.
