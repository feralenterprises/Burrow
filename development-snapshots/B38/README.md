# TrashPanda Tracker | Build #38 development snapshot

**We dig through your account so you don't have to.**

This directory preserves the unchanged owner-tested November 2, 2026 dual-development QA ZIP. It is **not a RuneLite Plugin Hub release**.

## Download
[Original Build #38 source archive](38-FERAL-DUAL-DEV-BANK-R05-O-FREEZE-HOVERLITE-v0.2.8-WINDOWS-QA-LEAN.zip)

## Contents
- **TPT:** Project-Burrow Java 17 Gradle source and tests with Bank R05 Best in Bank temporary O-tab behavior.
- **HoverLite v0.2.8:** separate older development copy included only for two-plugin RuneLite testing. Its public release is managed in a different repository.
- Windows launch scripts, owner QA instructions and source manifest.

## Evidence and provenance
- Archive integrity: 395 members, CRC PASS.
- SHA-256: `711dd92392d63cde669d646e195610e2d0ca12a17683249874f9231e943ca0ec`
- Git blob SHA-1: `a36d5f70775aa8143a61eb7b791f741b1e78bc1f`
- Owner-confirmed Windows Best in Bank QA: **four manual withdrawals** while the temporary O tab stayed functional. This is focused testing, not certification of every feature.

## Developer instructions
Extract the full ZIP into a fresh folder on Windows with JDK 17 and run `01-BUILD-AND-LAUNCH-BOTH.cmd`. See `READ-ME-FIRST.md` and `BANK-R05-WINDOWS-QA.md` inside the archive. Do not install the preview JAR as a RuneLite release.

## Release boundary
Root `Project-Burrow/` is an earlier prototype kept intact for its sealed CI workflow. B38 is a separate historical dual-development source snapshot; later intelligence waves are not incorporated. **Plugin Hub release remains on hold. No automatic banking.**
