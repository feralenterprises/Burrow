package gg.feral.burrow.intelligence;

import static org.junit.jupiter.api.Assertions.*;
import gg.feral.burrow.core.AccountContext;
import gg.feral.burrow.core.BankEvidence;
import gg.feral.burrow.core.BestBankPlanner;
import gg.feral.burrow.core.TieredOutfitterCatalog;
import gg.feral.burrow.core.VerifiedOutfitterCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Synthetic, non-gameplay item IDs. Full JUnit suite reserved for owner's final Wave 6 QA. */
class BestBankIntelligenceWave03JUnitTest {
    private static final IntelligenceCatalog.Source FIXTURE = new IntelligenceCatalog.Source(
        "https://oldschool.runescape.wiki/w/SYNTHETIC_FIXTURE_NOT_A_REAL_ITEM", "oldid=1234567",
        "2026-11-03T03:25:00Z", "synthetic-test-only/no-gameplay-assertions");

    private GearKnowledge.Item item(int id, GearKnowledge.Slot slot, GearKnowledge.Hands hands,
                                    Set<String> requiredStates, Map<String,Integer> levels,
                                    IntelligenceCatalog.Review review, boolean conflict) {
        return new GearKnowledge.Item(id, "Fixture-" + id, "TEST_ONLY", slot, hands, "UNKNOWN",
            levels, requiredStates, review, review == IntelligenceCatalog.Review.VERIFIED_SOURCE ? FIXTURE : null,
            List.of(), List.of(), conflict);
    }
    private GearKnowledge.Relation relation(int from,int to,GearKnowledge.RelationKind kind,String context) {
        return new GearKnowledge.Relation("synthetic-" + kind, from, to, kind, context,
            IntelligenceCatalog.Review.VERIFIED_SOURCE, FIXTURE, List.of());
    }
    private GearKnowledge.Pack pack(List<GearKnowledge.Item> items, List<GearKnowledge.Relation> relations) {
        return new GearKnowledge.Pack(2, "synthetic-wave03", items, relations);
    }
    private BestBankPlanner.Plan plan(boolean wilderness, int weapon, int ammo, int shield, int boots) {
        List<BestBankPlanner.GearCell> gear = new ArrayList<>();
        for (int i=0; i<11; i++) {
            int id = switch (i) {case 3 -> ammo; case 4 -> weapon; case 6 -> shield; case 9 -> boots; default -> 0;};
            var shown = id > 0 ? new VerifiedOutfitterCatalog.Item(id,"Fixture-" + id,1) : null;
            gear.add(new BestBankPlanner.GearCell(shown, shown,
                shown == null ? BestBankPlanner.GearStatus.EMPTY : BestBankPlanner.GearStatus.OWNED,
                "fixture"));
        }
        List<BestBankPlanner.SupplyCell> inv = new ArrayList<>();
        for (int i=0; i<28; i++) inv.add(new BestBankPlanner.SupplyCell(null,null,BestBankPlanner.SupplyStatus.EMPTY,""));
        return new BestBankPlanner.Plan("Synthetic", "Synthetic", TieredOutfitterCatalog.Tier.ENTRY,
            TieredOutfitterCatalog.Tier.ENTRY, wilderness ? BestBankPlanner.Verdict.NOT_AVAILABLE_WILDERNESS :
            BestBankPlanner.Verdict.READY, gear, inv, List.of(), List.of(), List.of(),0,28,List.of(),"fixture",
            null,List.of(),List.of(),List.of());
    }
    private BankEvidence evidence(int... ids) {
        java.util.Map<Integer,Integer> stock=new java.util.HashMap<>();
        for (int id:ids) stock.put(id,1);
        return new BankEvidence(stock,Map.of(),Map.of(),BankEvidence.Population.STABLE_READ,1,true,false);
    }
    @Test void allW02ResearchRemainsCompletelyNonAuthoritative() throws Exception {
        var p=plan(false,900001,900002,900003,900004);
        assertSame(p, BestBankIntelligenceBridge.enrich(p,evidence(900001,900002,900003,900004),
            AccountContext.UNKNOWN,GearKnowledge.bundled()));
    }
    @Test void unpublishedItemNeverChangesBestInBankReadiness() {
        var p=plan(false,900001,0,0,0);
        var staged=item(900001,GearKnowledge.Slot.WEAPON,GearKnowledge.Hands.TWO,Set.of("CHARGED"),Map.of("Attack",99),IntelligenceCatalog.Review.CHECK,false);
        assertSame(p, BestBankIntelligenceBridge.enrich(p,evidence(900001),AccountContext.UNKNOWN,pack(List.of(staged),List.of())));
    }
    @Test void verifiedSkillFailureBlocksButDoesNotAlterInventorySlots() {
        var p=plan(false,900001,0,0,0);
        var sword=item(900001,GearKnowledge.Slot.WEAPON,GearKnowledge.Hands.ONE,Set.of(),Map.of("Attack",60),IntelligenceCatalog.Review.VERIFIED_SOURCE,false);
        var updated=BestBankIntelligenceBridge.enrich(p,evidence(900001),new AccountContext(Map.of("Attack",50),Map.of()),pack(List.of(sword),List.of()));
        assertEquals(BestBankPlanner.Verdict.NOT_READY,updated.verdict());
        assertEquals(p.gear(),updated.gear()); assertEquals(p.inventory(),updated.inventory());
    }
    @Test void unobservableChargeDemotesReadyToVerifyNotNotReady() {
        var p=plan(false,900001,0,0,0);
        var sword=item(900001,GearKnowledge.Slot.WEAPON,GearKnowledge.Hands.ONE,Set.of("CHARGED"),Map.of(),IntelligenceCatalog.Review.VERIFIED_SOURCE,false);
        var updated=BestBankIntelligenceBridge.enrich(p,evidence(900001),AccountContext.UNKNOWN,pack(List.of(sword),List.of()));
        assertEquals(BestBankPlanner.Verdict.VERIFY,updated.verdict());
    }
    @Test void conflictingApprovedAmmoRelationsBecomeVerifyNotArbitraryBlock() {
        var p=plan(false,900001,900002,0,0);
        var w=item(900001,GearKnowledge.Slot.WEAPON,GearKnowledge.Hands.ONE,Set.of(),Map.of(),IntelligenceCatalog.Review.VERIFIED_SOURCE,false);
        var a=item(900002,GearKnowledge.Slot.AMMO,GearKnowledge.Hands.NOT_WEAPON,Set.of(),Map.of(),IntelligenceCatalog.Review.VERIFIED_SOURCE,false);
        var bad=relation(900001,900002,GearKnowledge.RelationKind.AMMO_INCOMPATIBLE,"");
        var good=relation(900001,900002,GearKnowledge.RelationKind.AMMO_COMPATIBLE,"");
        assertEquals(BestBankPlanner.Verdict.VERIFY,
            BestBankIntelligenceBridge.enrich(p,evidence(900001,900002),AccountContext.UNKNOWN,pack(List.of(w,a),List.of(bad,good))).verdict());
    }
    @Test void unconditionalNegativeAmmoRelationBlocksAndContextualIsVerify() {
        var p=plan(false,900001,900002,0,0);
        var w=item(900001,GearKnowledge.Slot.WEAPON,GearKnowledge.Hands.ONE,Set.of(),Map.of(),IntelligenceCatalog.Review.VERIFIED_SOURCE,false);
        var a=item(900002,GearKnowledge.Slot.AMMO,GearKnowledge.Hands.NOT_WEAPON,Set.of(),Map.of(),IntelligenceCatalog.Review.VERIFIED_SOURCE,false);
        var bad=relation(900001,900002,GearKnowledge.RelationKind.AMMO_INCOMPATIBLE,"");
        assertEquals(BestBankPlanner.Verdict.NOT_READY,
            BestBankIntelligenceBridge.enrich(p,evidence(900001,900002),AccountContext.UNKNOWN,pack(List.of(w,a),List.of(bad))).verdict());
        var conditional=relation(900001,900002,GearKnowledge.RelationKind.AMMO_INCOMPATIBLE,"Special phase only");
        assertEquals(BestBankPlanner.Verdict.VERIFY,
            BestBankIntelligenceBridge.enrich(p,evidence(900001,900002),AccountContext.UNKNOWN,pack(List.of(w,a),List.of(conditional))).verdict());
    }
    @Test void verifiedTwoHandedShieldCollisionBlocks() {
        var p=plan(false,900001,0,900003,0);
        var w=item(900001,GearKnowledge.Slot.WEAPON,GearKnowledge.Hands.TWO,Set.of(),Map.of(),IntelligenceCatalog.Review.VERIFIED_SOURCE,false);
        var s=item(900003,GearKnowledge.Slot.SHIELD,GearKnowledge.Hands.NOT_WEAPON,Set.of(),Map.of(),IntelligenceCatalog.Review.VERIFIED_SOURCE,false);
        assertEquals(BestBankPlanner.Verdict.NOT_READY,
            BestBankIntelligenceBridge.enrich(p,evidence(900001,900003),AccountContext.UNKNOWN,pack(List.of(w,s),List.of())).verdict());
    }
    @Test void unresolvedIdentityAndWildernessNeverGetPromoted() {
        var p=plan(false,900001,0,0,0);
        var conflicted=item(900001,GearKnowledge.Slot.WEAPON,GearKnowledge.Hands.TWO,Set.of("CHARGED"),Map.of("Attack",99),IntelligenceCatalog.Review.VERIFIED_SOURCE,true);
        var k=pack(List.of(conflicted),List.of());
        assertSame(p,BestBankIntelligenceBridge.enrich(p,evidence(900001),AccountContext.UNKNOWN,k));
        var wild=plan(true,900001,0,0,0);
        assertSame(wild,BestBankIntelligenceBridge.enrich(wild,evidence(900001),AccountContext.UNKNOWN,k));
    }
}
