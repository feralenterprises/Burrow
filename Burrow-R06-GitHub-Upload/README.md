# Project Burrow: Feral Edition 🦝

Experimental public-source OSRS RuneLite sidebar plugin. **Not approved for native game use, testing by third parties, or release.**

This repository contains browsable sources under `Project-Burrow/` and an **independent, exact-byte** sealed copy at `ci/BUR-0008-JAVA17-OFFLINE-FIX-SOURCE-R06.zip` (SHA-256 `e3950dea037e846ad23cc6121e2328c17ef0933ebe2681b65ccf2d88b91e82ae`). The manual GitHub Actions workflow checks the entire source tree against that immutable ZIP before attempting any build.

## Verify with Java 17

Run GitHub Actions → **BUR-0008 Java 17 RuneLite build verification** → **Run workflow**. Only a real JDK 17 and authentic RuneLite dependencies count. Offline checks explicitly run the 66-check baseline, 129-check P1 suite, and the P2 hitbox regression main without JUnit classpath contamination. The real Gradle step must separately compile RuneLite integration and run the JUnit suite. Failed jobs retain evidence. The workflow does not start RuneLite or publish a release.

**BUR-0008 R06 changes only `scripts/build-offline.sh`** compared with the R05 source; production UI, bank revision, gear geometry and host adapter Java files remain byte-for-byte unchanged. R05 hitbox changes remain unverified by actual Java 17 execution. Java 21 with `--release 17` is not acceptance.

Never commit secrets, personal account data, game credentials, access tokens or private bank data.

**Release status: HOLD.**
