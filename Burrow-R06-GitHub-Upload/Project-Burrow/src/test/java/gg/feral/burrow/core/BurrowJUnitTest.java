package gg.feral.burrow.core;
import org.junit.jupiter.api.Test;
/** The same pure regression suite also runs in a connected Gradle/JUnit build. */
public class BurrowJUnitTest {
    @Test void coreAndInteraction() throws Exception { BurrowCoreTest.main(new String[0]); }
}
