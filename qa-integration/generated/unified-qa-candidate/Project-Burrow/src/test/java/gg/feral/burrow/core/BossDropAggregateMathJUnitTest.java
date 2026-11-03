package gg.feral.burrow.core;

import org.junit.jupiter.api.Test;

/** Integrates the BUR-0029 offline assertions into the project's existing JUnit 5 suite. */
public final class BossDropAggregateMathJUnitTest {
    @Test public void anyOfSelectedRaresUsesTableStructureNotNaiveAddition() {
        BossDropAggregateMathTest.main(new String[0]);
    }
}
