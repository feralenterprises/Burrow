# 🦝 TrashPanda Tracker

**We dig through your account so you don't have to.**

**TrashPanda Tracker (TPT)** is an in-development **Old School RuneScape RuneLite sidebar plugin** by Feral Enterprises. We're building a clearer way for players to understand their account, prepare for activities, and make useful decisions with the gear they already own.

**Status:** Active development · owner testing · **not yet a Plugin Hub release**

> **Repository note:** The Java project in `Project-Burrow/` is an earlier public engineering snapshot. Recent owner-tested TPT development builds and research are **not all represented by the code on `main`**. This repository should not be treated as the latest runnable client or as a ready-to-install plugin.

## What we're building

A compact **four-tab** experience designed to be useful during play:

| Tab | Direction |
| --- | --- |
| **Home** | A quick view of account progress and useful next steps |
| **Pathfinder** | Activity suggestions based on readiness and requirements |
| **Outfitter** | Equipment, inventory, and **Best in Bank** planning |
| **Sessions** | Boss and activity progress with trustworthy kill-count evidence |

**Outfitter** is a major focus: understand item variants and charged states, recognize alternatives, distinguish mandatory mechanics from optional upgrades, and present a practical **4 × 7 inventory**. Bank-aware features are being designed around player-controlled actions. **No automatic banking or gameplay automation.**

## Development pulse · November 2, 2026

- **Best in Bank:** owner Windows testing of a separate development build demonstrated four successful **manual** item withdrawals through the temporary bank-planning tab. The bank UI and evidence lifecycle remain active development areas.
- **Gear intelligence:** expanding recognition for functional variants, charged/imbued states, sets, ammunition, and activity-specific requirements.
- **Interface:** continuing the 225px, scrollable, four-tab layout and a distinct raccoon-themed identity with recognizable OSRS item icons.
- **Progress tracking:** researching reliable in-game kill-log sources rather than inferring boss kills from loot.

These are **development updates**, not claims that the older source snapshot in this repository implements each feature. Read the [development log](docs/DEVELOPMENT-LOG.md) for the current public summary and [product direction](docs/PRODUCT-DIRECTION.md) for the design principles.

## Tested development snapshot

The complete [Build #38 Bank R05 development ZIP](development-snapshots/B38/README.md) is archived with its original source and checksum. It is distinct from the older public prototype and includes an independent experimental HoverLite v0.2.8 copy for dual-plugin QA. This is **not yet a Plugin Hub release**.

## Source and build information

The browsable `Project-Burrow/` folder and `ci/` archives document an **earlier Java 17 prototype and regression work**. The existing manual GitHub Actions workflow references sealed **BUR-0008 R07** source and checks a genuine JDK 17 / Gradle environment; it is **not** a release or a live RuneLite gameplay test.

- [Prototype technical README](Project-Burrow/README.md)
- [Historical engineering roadmap](Project-Burrow/BURROW-ROADMAP.md)
- [Java 17 verification workflow](.github/workflows/burrow-java17.yml)

Do not install the offline preview JAR as a RuneLite plugin. Public distribution remains on hold pending a reconciled release source, reproducible build, native verification, and Plugin Hub review.

## Follow the build

This repository is a public window into work in progress. Follow commits and the development log as we refine the gameplay experience. Future source updates will be labeled with their tested baseline so experimental research, prototypes, and actual releases cannot be confused.

*Built by Feral Enterprises. TrashPanda Tracker is an independent community project and is not affiliated with Jagex or RuneLite.*
