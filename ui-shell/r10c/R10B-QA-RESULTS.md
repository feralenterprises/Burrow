# R10B offline QA results

PASS (Linux JDK 21 with `--release 17` bytecode target, using Xvfb):
- Compile all three independent Java shell/artwork classes.
- Standalone Swing self-test including tabs, filters, search, mouse boss selection, keyboard bindings, Chronicle/Outfitter independent state, 11 equipment positions, 28 inventory slots, appearance persistence, bank evidence controls and rejected-mascot guard.
- BossArtReviewer `--self-test` for staged artwork checksums, dimensions, transparency and approval isolation.
- WikiBossArtFetcher `--self-test` for allowlist, response validation and fallback.
- 36 actual screenshots at 225×790; 36 enlarged threefold 675×2370 screenshots.
- All source artwork assets byte-equal to R10A; rejected chunky mascot absent.
- ZIP extraction and extracted-JAR test completed.

NOT VERIFIED: real RuneLite client or Windows native DPI; 3D boss models; account data or bank engine behavior. Release HOLD.
