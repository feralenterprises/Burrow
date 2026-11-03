package gg.feral.burrow.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

class BestBankMatchesJUnitTest {
    private VerifiedOutfitterCatalog.Profile candidate() {
        return VerifiedOutfitterCatalog.profiles("Scurrius").get(0);
    }
    @Test void observedExactGearNeverChangesWikiProfile() {
        var original=candidate();
        var before=List.copyOf(original.gear());
        int id=original.gear().get(0).id();
        var results=BestBankMatches.gear(original,Map.of(id,1));
        assertEquals(BestBankMatches.Status.EXACT,results.get(0).status());
        assertEquals(BestBankMatches.Status.NOT_OBSERVED,results.get(1).status());
        assertEquals(before,original.gear());
    }
    @Test void verifiedImbuedRingVariantIsAcceptedButPlainRingIsNot() {
        var profile=candidate();
        assertEquals(11773,profile.gear().get(10).id());
        assertEquals(BestBankMatches.Status.REVIEWED_VARIANT,
            BestBankMatches.gear(profile,Map.of(25264,1)).get(10).status());
        assertEquals(25264,BestBankMatches.gear(profile,Map.of(25264,1)).get(10).observedId());
        assertEquals(BestBankMatches.Status.NOT_OBSERVED,
            BestBankMatches.gear(profile,Map.of(6737,1)).get(10).status());
    }
    @Test void absentBankEvidenceNeverBecomesMissingOrOwned() {
        var result=BestBankMatches.gear(candidate(),Map.of());
        assertEquals(11,result.size());
        for(var match:result) assertFalse(match.confirmed());
        assertEquals(BestBankMatches.Status.NOT_OBSERVED,result.get(0).status());
    }
    @Test void insufficientAmmoOnlyCountsAsPartial() {
        var profile=TieredOutfitterCatalog.find("King Black Dragon", "Crossbow, standard route", TieredOutfitterCatalog.Tier.ENTRY).profile();
        var ammo=profile.gear().get(3);
        assertTrue(ammo.amount()>1);
        assertEquals(BestBankMatches.Status.PARTIAL_STACK,
            BestBankMatches.gear(profile,Map.of(ammo.id(),ammo.amount()-1)).get(3).status());
    }
}
