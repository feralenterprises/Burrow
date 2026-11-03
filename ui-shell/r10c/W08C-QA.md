# Wave 8C QA | Standalone only

- Java compilation: PASS via JDK 21 `javac --release 17` (Java 17 compatible bytecode)
- Existing W01-W08B shell regression: PASS (all four tabs, boss selection independence, 11 equipment positions and 4x7 inventory, no rejected mascot)
- New artwork-review validation: PASS (`--self-test`) for PNG signature, min/max dimensions, alpha/transparency, SHA-256, rejected small fixture, missing model fallback and source isolation
- Manifest gate tightened: local RuneLite capture provenance also supported after exact checksum and explicit owner signoff; opaque images rejected
- Original model PNG files supplied: **ZERO**; fallback remains active for Scurrius, Sarachnis and Bryophyta
- Candidate staging code: saves only under `artwork-review/candidates/`, never approved model directory. Its live GUI click path remains for owner Windows QA; headless test validates underlying file checks only
- Standalone review window actual Swing screenshot: `previews/art-review-workstation.png` (560x770)
- Original shell renders: 36 actual 225x790 images and 36 separate 3x phone enlargements
- Original full-body model renders: NOT obtained; NOT approved; NOT included; no false source/rights claim
- Main Project Burrow / TPT plugin modified: NO
- Rejected bulky raccoon: NOT INCLUDED
- Native Windows/Jagex client QA: NOT RUN; integration HOLD
- Rights checks: source project documentation researched; no legal clearance of image redistributions was asserted

Shell Java source hash: `29c777baee72e2eee2d5a7b7610938c9bd6ffd1b09d0bb9e259202f653e1c348`
New art-review source hash: `0a527e174c5d61584e4acde28cda78a921b9c3e357905b1950b51ba3b0963659`
Standalone JAR hash: `d727e06c6b98370aee59b33486d5e1422b3863919ea59b32ec6e36c1fd4bc063`
