# R10C offline QA and approval gates

## PASS
- Java 17-target compilation (javac --release 17); standalone Swing self test passed.
- BossArtReviewer isolated authenticity/approval test passed; WikiBossArtFetcher allowlist/fallback test passed.
- R10C compare → genuine-sprites toggle regression passed; no automatic promotion of reference crops.
- 36 native 225×790 Swing screenshot pairs from the normal sprite mode and 36 from the reference mode; corresponding 3× phone previews for each: 144 PNGs total.
- Existing authentic item/boss sprites and approved-armored-raccoon.png are byte-for-byte unchanged from R10B; rejected feral-hero.png absent.
- Archive extraction and JAR self test checked separately.

## NOT VERIFIED
- Browser/Windows true native DPI; RuneLite gameplay tests; full 61-encounter catalog.
- Model authenticity and redistribution rights for full-size renders. Reference-mode crops are from an owner-provided illustrative screenshot and are NOT certified game-model art.
- No account-verified KC, CL, bank scans, rare drops, or Pathfinder intelligence is connected.

**Release HOLD: standalone design review only, no main TPT source changes.**
