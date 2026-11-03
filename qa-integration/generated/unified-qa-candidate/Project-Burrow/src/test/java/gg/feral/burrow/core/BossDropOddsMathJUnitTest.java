package gg.feral.burrow.core;

import org.junit.jupiter.api.Test;

/** Integrates the offline assertions into the project's existing JUnit 5 suite. */
public final class BossDropOddsMathJUnitTest {
    @Test public void fixedIndependentDropOddsAreNumericallyStable() {
        BossDropOddsMathTest.main(new String[0]);
    }
}
