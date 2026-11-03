package gg.feral.burrow.ui;

import org.junit.jupiter.api.Test;

/** BUR-0019: Java 17 Gradle/JUnit wrapper around the dependency-free Swing test. */
final class ItemHoverNoModifierJUnitTest {
    @Test void itemsDescribeThemselvesOnNormalMouseHover() throws Exception {
        ItemHoverNoModifierTest.main(new String[0]);
    }
}
