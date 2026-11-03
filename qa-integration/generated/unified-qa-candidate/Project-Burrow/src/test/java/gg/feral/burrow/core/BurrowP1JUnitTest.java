package gg.feral.burrow.core;

import org.junit.jupiter.api.Test;

/**
 * BUR-0007: make the executable 129-check P1 regression part of real Gradle/JUnit test.
 * Source only until verified under actual JDK 17 and genuine JUnit dependencies.
 */
public final class BurrowP1JUnitTest {
    @Test
    void verifiedP1Regression() throws Exception {
        BurrowP1RegressionTest.main(new String[0]);
    }
}
