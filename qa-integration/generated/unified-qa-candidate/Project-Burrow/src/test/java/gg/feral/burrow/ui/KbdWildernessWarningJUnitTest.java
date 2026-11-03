package gg.feral.burrow.ui;

import org.junit.jupiter.api.Test;

/** Run the KBD Wilderness warning regression as part of Java 17 Gradle tests. */
final class KbdWildernessWarningJUnitTest {
    @Test void routeRiskIsVisibleAndScopedToKbd() throws Exception {
        KbdWildernessWarningTest.main(new String[0]);
    }
}
