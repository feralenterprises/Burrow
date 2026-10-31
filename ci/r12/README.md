# BUR-0049 R12 sealed-source CI staging

**RELEASE HOLD. Do not merge or release.** This directory belongs only to `ci/bur-0049-r12-java17-hold`.

Upload this exact file to this directory on this branch only:

`SPOONS-BUR0049-R12-XP-DISABLED-RECONCILED-CHILD-R01.zip`

Expected SHA-256: `1bf12c78f058894708155c9e72a3f345dbabc65ae9250eec10bdbd33ebba53a6`

The isolated `.github/workflows/bur-0049-r12-java17.yml` workflow triggers **only** when this ZIP path changes on this branch. It verifies the ZIP SHA-256, validates hashes of 274 project files against the bundled R12 manifest, checks that XP remains disabled, then attempts genuine JDK 17 / Gradle 8.14.6 / RuneLite 1.13.4 `clean test jar`. Logs, JUnit XML and JAR hashes are stored as CI evidence, not releases.

**State at creation:** sealed ZIP **not uploaded**. No build has run for R12. `main` stays unchanged. Even a green CI run is not Tongs/Wrench approval or native Windows gameplay proof.
