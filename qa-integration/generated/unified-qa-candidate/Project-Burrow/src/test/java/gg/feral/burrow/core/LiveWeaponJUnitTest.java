package gg.feral.burrow.core;

import org.junit.jupiter.api.Test;

/** Run by real JDK 17 Gradle CI after isolated candidate source is authorized for CI. */
public final class LiveWeaponJUnitTest {
    @Test void weaponObservationEpochAndUiRegressions() { LiveWeaponTest.main(new String[0]); }
}
