# W08C | Source-backed model-render options (no unverified bytes bundled)

Research checked 2026-11-03.

## Route A: owner-local OSRS cache renders
Source: https://github.com/osrs-clansocket/clansocket-osrs-cache-extractor
- Public project documents lossless export of OSRS game-cache NPCs into PNG and three angles. It reads a RuneLite local cache (typically `~/.runelite/jagexcache/oldschool/LIVE/`).
- Requirements stated by the project: Node 20+, Java 24, local RuneLite cache; the extracted model data can require gigabytes. We did NOT run or bundle this third-party program, have not independently audited its binaries, and do not recommend disabling Windows protections.
- This can provide a route to genuine model renders, rather than upscaling 15px sprites. Verify correct NPC model IDs and exports before import.
- Its source BSD license does not automatically grant rights to redistribute Jagex model art.

## Route B: OSRS NPC image dataset
Source: https://github.com/OGladfelter/osrs-npc-img
- Describes 4,443 NPCs with chatheads and full-body images scraped from OSRS Wiki, with `data/npc.csv` metadata and `data/bodies/{id}.png` files.
- The dataset warns about wrongly matched images and includes junk-image review. Therefore an image name alone is NOT identity verification.
- Image bytes and Jagex reuse permissions have not been checked in this wave. No dataset art is bundled.

## Authority and rights
- https://oldschool.runescape.wiki/w/RuneScape:Images_and_media_policy
- https://legal.jagex.com/docs/policies/fan-content-policy
- Game-cache and wiki imagery can be Jagex copyrighted; keep clear rights records. Do not assert a third-party source-code license covers game art.

## Per-boss status
| Boss | Original model PNG | Source checksum | Rights evidence | Owner approved |
|---|---|---|---|---|
| Scurrius | Not obtained | Missing | Pending | No |
| Sarachnis | Not obtained | Missing | Pending | No |
| Bryophyta | Not obtained | Missing | Pending | No |

Once a verified local render is available, review it in the packaged GUI. Do not automatically elevate a staged candidate to approved art.
