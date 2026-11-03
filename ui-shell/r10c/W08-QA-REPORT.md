# Wave 8 checkpoint A standalone QA

- Source baseline: W06 shell (`src/.../TrashPandaShell.java`) SHA256 `a836a2d4390facae225a0b5ed752fa6247cd4e864dc82066e81cc44ec5a3efd5`
- Result shell source SHA256 `14fb86896c41d0cfa022a759c14aa80ed002f81bc8ddbf5c5da87c43f6bf8358`
- Java target: `javac --release 17` on JDK 21 host: PASS
- Offline click/navigation/state regression: PASS (`W08-SELF-TEST.txt`), includes W01–W06 checks
- Boss book drawing: PASS (icon is an actual custom Java2D icon)
- Boss portrait source: genuine mini OSRS NPC sprite fallback, NOT approved 3D model art
- Number of screenshots: 72 = 36 real 225×790 and 36 scaled 675×2370
- Rejected mascot (`feral-hero.png`): not bundled
- Production RuneLite/Bank logic changed: none (standalone directory only)
- Windows/RuneLite native QA: NOT RUN
- Owner visual signoff: NOT RECEIVED
- Status: **CHECKPOINT A / HOLD**
