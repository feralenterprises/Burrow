package gg.feral.burrow.ui;

import org.junit.jupiter.api.Test;

/** Run the real-event BUR-0040 UI regression during Java 17 Gradle test. */
final class OutfitterStyleInteractionJUnitTest {
    @Test void switchingAndUnsupportedExplanations() throws Exception {
        OutfitterStyleInteractionTest.main(new String[0]);
    }
}
