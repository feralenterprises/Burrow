package gg.feral.burrow.core;

import org.junit.jupiter.api.Test;

/** Native project test-suite entry point; shares the standalone replay fixtures. */
public final class BossKcDeltaTrackerJUnitTest {
    @Test public void replayAllBossKcCases() throws Exception {
        BossKcDeltaTrackerReplayTest.main(new String[0]);
    }
}
