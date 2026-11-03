package gg.feral.burrow.core;

import org.junit.jupiter.api.Test;

/** Integrates the BUR-0016 offline assertions into the project's existing JUnit 5 suite. */
public final class ItemVariantPolicyJUnitTest {
    @Test public void ownedVariantsResolveWithoutUnsafeSubstitution() {
        ItemVariantPolicyTest.main(new String[0]);
    }
}
