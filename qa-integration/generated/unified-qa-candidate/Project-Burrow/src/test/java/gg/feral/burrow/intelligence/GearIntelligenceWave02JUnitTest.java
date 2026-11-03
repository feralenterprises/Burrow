package gg.feral.burrow.intelligence;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Future final-QA fixtures; artificial item IDs, not claims about the live OSRS cache. */
class GearIntelligenceWave02JUnitTest {
    private static final IntelligenceCatalog.Source PIN = new IntelligenceCatalog.Source(
        "https://oldschool.runescape.wiki/w/TEST_FIXTURE_ONLY", "oldid=1234567",
        "2026-11-03T03:20:00Z", "synthetic/test/no-gameplay-claim");
    private static GearKnowledge.Item item(int id, GearKnowledge.Slot slot, GearKnowledge.Hands hands,
                                           Map<String,Integer> skills, Set<String> flags, String god) {
        return new GearKnowledge.Item(id, "Synthetic fixture " + id, "synthetic", slot, hands, god,
            skills, flags, IntelligenceCatalog.Review.VERIFIED_SOURCE, PIN, List.of(), List.of(), false);
    }
    private static GearKnowledge.Relation edge(String id, int from, int to, GearKnowledge.RelationKind kind) {
        return new GearKnowledge.Relation(id,from,to,kind,"",IntelligenceCatalog.Review.VERIFIED_SOURCE,PIN,List.of());
    }
    @Test void allBundledGearIsQuarantined() throws Exception {
        var pack=GearKnowledge.bundled();
        assertEquals(80,pack.items().size());
        assertEquals(9,pack.relations().size());
        assertEquals(0,pack.approvedItemCount());
        assertEquals(0,pack.approvedRelationCount());
        assertEquals(GearDecisions.Verdict.RESEARCH_ONLY,
            GearDecisions.ammunition(pack,28869,28872));
    }
    @Test void falseRevisionCannotApproveItem() {
        var fake = new IntelligenceCatalog.Source("https://oldschool.runescape.wiki/w/Test",
            "UNKNOWN_NOT_CAPTURED", "2026-11-03T03:20:00Z", "synthetic");
        assertThrows(IllegalArgumentException.class,()->new GearKnowledge.Item(900001,"Fixture","test",
            GearKnowledge.Slot.WEAPON,GearKnowledge.Hands.ONE,"UNKNOWN", Map.of(),Set.of(),
            IntelligenceCatalog.Review.VERIFIED_SOURCE,fake,List.of(),List.of(),false));
    }
    @Test void chargedOrInactiveOwnedEquipmentIsNotAutomaticReady() {
        var ring=item(900001,GearKnowledge.Slot.RING,GearKnowledge.Hands.NOT_WEAPON,
            Map.of(),Set.of("CHARGED"),"UNKNOWN");
        assertEquals(GearDecisions.Verdict.NOT_OWNED,GearDecisions.usability(ring,
            new GearDecisions.Observation(0,Map.of(),Map.of())));
        assertEquals(GearDecisions.Verdict.UNKNOWN,GearDecisions.usability(ring,
            new GearDecisions.Observation(1,Map.of(),Map.of())));
        assertEquals(GearDecisions.Verdict.BLOCKED,GearDecisions.usability(ring,
            new GearDecisions.Observation(1,Map.of("CHARGED",GearDecisions.Signal.NO),Map.of())));
        assertEquals(GearDecisions.Verdict.USABLE,GearDecisions.usability(ring,
            new GearDecisions.Observation(1,Map.of("CHARGED",GearDecisions.Signal.YES),Map.of())));
    }
    @Test void exactSkillRequirementsMustAllBeObserved() {
        var bow=item(900002,GearKnowledge.Slot.WEAPON,GearKnowledge.Hands.ONE,
            Map.of("Ranged",66,"Hunter",50),Set.of(),"UNKNOWN");
        assertEquals(GearDecisions.Verdict.UNKNOWN,GearDecisions.usability(bow,
            new GearDecisions.Observation(1,Map.of(),Map.of("Ranged",66))));
        assertEquals(GearDecisions.Verdict.BLOCKED,GearDecisions.usability(bow,
            new GearDecisions.Observation(1,Map.of(),Map.of("Ranged",99,"Hunter",49))));
    }
    @Test void ammunitionRequiresApprovedDirectedRelation() {
        var weapon=item(900003,GearKnowledge.Slot.WEAPON,GearKnowledge.Hands.ONE,Map.of(),Set.of(),"UNKNOWN");
        var ammo=item(900004,GearKnowledge.Slot.AMMO,GearKnowledge.Hands.NOT_WEAPON,Map.of(),Set.of(),"UNKNOWN");
        var unknown=new GearKnowledge.Pack(2,"synthetic",List.of(weapon,ammo),List.of());
        assertEquals(GearDecisions.Verdict.UNKNOWN,GearDecisions.ammunition(unknown,900003,900004));
        var good=new GearKnowledge.Pack(2,"synthetic",List.of(weapon,ammo),
            List.of(edge("good",900003,900004,GearKnowledge.RelationKind.AMMO_COMPATIBLE)));
        assertEquals(GearDecisions.Verdict.USABLE,GearDecisions.ammunition(good,900003,900004));
        var bad=new GearKnowledge.Pack(2,"synthetic",List.of(weapon,ammo),
            List.of(edge("bad",900003,900004,GearKnowledge.RelationKind.AMMO_INCOMPATIBLE)));
        assertEquals(GearDecisions.Verdict.BLOCKED,GearDecisions.ammunition(bad,900003,900004));
    }
    @Test void godAlignmentPreventsFalseEquivalentGear() {
        var old=item(900010,GearKnowledge.Slot.BODY,GearKnowledge.Hands.NOT_WEAPON,Map.of(),Set.of(),"ARMADYL");
        var replacement=item(900011,GearKnowledge.Slot.BODY,GearKnowledge.Hands.NOT_WEAPON,Map.of(),Set.of(),"BANDOS");
        var pack=new GearKnowledge.Pack(2,"synthetic",List.of(old,replacement),
            List.of(edge("cosmetic",900010,900011,GearKnowledge.RelationKind.COSMETIC_PEER)));
        assertEquals(GearDecisions.Verdict.BLOCKED,
            GearDecisions.substitute(pack,900010,900011,"ARMADYL"));
    }
    @Test void fullSetIsNotPartialSet() {
        var rule=new GearDecisions.SetRule("full",Set.of(900101,900102,900103),
            GearDecisions.EffectMode.FULL_SET,3,IntelligenceCatalog.Review.VERIFIED_SOURCE,PIN);
        assertEquals(GearDecisions.Verdict.NOT_APPLICABLE,GearDecisions.setEffect(rule,
            Map.of(900101,GearDecisions.Signal.YES,900102,GearDecisions.Signal.YES,
                900103,GearDecisions.Signal.NO)).verdict());
        assertEquals(GearDecisions.Verdict.UNKNOWN,GearDecisions.setEffect(rule,
            Map.of(900101,GearDecisions.Signal.YES,900102,GearDecisions.Signal.YES)).verdict());
        assertEquals(GearDecisions.Verdict.USABLE,GearDecisions.setEffect(rule,
            Map.of(900101,GearDecisions.Signal.YES,900102,GearDecisions.Signal.YES,
                900103,GearDecisions.Signal.YES)).verdict());
    }
    @Test void weaponAndShieldRequireKnownHandedness() {
        var shield=item(900020,GearKnowledge.Slot.SHIELD,GearKnowledge.Hands.NOT_WEAPON,Map.of(),Set.of(),"UNKNOWN");
        var two=item(900021,GearKnowledge.Slot.WEAPON,GearKnowledge.Hands.TWO,Map.of(),Set.of(),"UNKNOWN");
        assertEquals(GearDecisions.Verdict.BLOCKED,GearDecisions.hands(two,shield));
    }
    @Test void duplicateItemIdsAreRejected() {
        var i=item(900030,GearKnowledge.Slot.RING,GearKnowledge.Hands.NOT_WEAPON,Map.of(),Set.of(),"UNKNOWN");
        assertThrows(IllegalArgumentException.class,()->new GearKnowledge.Pack(2,"synthetic",List.of(i,i),List.of()));
    }
}
