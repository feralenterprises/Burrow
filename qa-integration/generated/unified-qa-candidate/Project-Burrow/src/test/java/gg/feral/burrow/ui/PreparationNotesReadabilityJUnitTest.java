package gg.feral.burrow.ui;

import org.junit.jupiter.api.Test;

/** Execute the same actual Swing regression in genuine Java17 Gradle/JUnit. */
final class PreparationNotesReadabilityJUnitTest {
    @Test void fullPreparationNotesRemainScrollableAndReadable() throws Exception {
        PreparationNotesReadabilityTest.main(new String[0]);
    }
}
