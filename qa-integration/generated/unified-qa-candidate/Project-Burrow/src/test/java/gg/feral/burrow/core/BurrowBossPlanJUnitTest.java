package gg.feral.burrow.core;

import org.junit.jupiter.api.Test;

/** Runs the same boss-plan regression in genuine Gradle JUnit Java 17 CI. */
public final class BurrowBossPlanJUnitTest {
    @Test void selectedBossControlsOutfitterTruth() {
        BurrowBossPlanTest.main(new String[0]);
    }
}
