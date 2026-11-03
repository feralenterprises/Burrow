package gg.feral.burrow.experience;

import gg.feral.burrow.core.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.experience.BanditBossCandidate.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** W01-01 negative evidence: all tests depend on the actual CP9 registry, not invented boss methods. */
public class BanditSafetyJUnitTest {
    private static AccountEvidenceContext a() {
        return ExperienceFoundationJUnitTest.account(ExperienceFoundationJUnitTest.live(Map.of(851,1)));
    }
    private static String method(String boss) { return TieredOutfitterCatalog.methods(boss).get(0); }
    private static BestBankPlanner.Plan pretendReady(String boss, String method, Tier tier) {
        return new BestBankPlanner.Plan(boss,method,tier,tier,BestBankPlanner.Verdict.READY,
            ExperienceFoundationJUnitTest.GEAR,ExperienceFoundationJUnitTest.INV,
            List.of(),List.of(),List.of(),0,28,List.of(),"",null,List.of());
    }
    @Test void missingNullAndThrownPlanMustBeUnknownNotCrash() {
        for (BanditAssessor.PlanSource src : List.<BanditAssessor.PlanSource>of(
                (b,m,t)->null, (b,m,t)->{throw new IllegalStateException("cache evicted");})) {
            var c = new BanditAssessor(a(),src).assess("Scurrius");
            assertEquals(Readiness.UNKNOWN,c.readiness());
            assertEquals(Reason.NO_VERIFIED_LOADOUT,c.reasonCode());
        }
        assertEquals(Readiness.UNKNOWN,new BanditAssessor(a(),null).assess("Scurrius").readiness());
    }
    @Test void crossBossMethodAndTierCacheResultsAreRejected() {
        String m=method("Scurrius");
        var cases=List.of(pretendReady("Sarachnis",m,Tier.OPTIMAL),
            pretendReady("Scurrius","WRONG_METHOD",Tier.OPTIMAL),
            pretendReady("Scurrius",m,Tier.ENTRY));
        for(var plan:cases) {
            var c=new BanditAssessor(a(),(b,mm,t)->plan).assess("Scurrius");
            assertEquals(Readiness.UNKNOWN,c.readiness(),String.valueOf(plan));
        }
    }
    @Test void actualVorkathRangedMissingShieldIsBlocked() {
        String m=method("Vorkath");
        assertTrue(m.contains("Ranged"));
        // All 3 tiers are real CP9 catalog data. Deliberately omit ALL shield options.
        Map<Integer,Integer> items=new HashMap<>();
        for(Tier t:Tier.values()) items.putAll(ExperienceFoundationJUnitTest.kit("Vorkath",m,t));
        for(Tier t:Tier.values()){
            var sel=TieredOutfitterCatalog.find("Vorkath",m,t);
            items.remove(sel.profile().gear().get(6).id());
        }
        var account=ExperienceFoundationJUnitTest.account(ExperienceFoundationJUnitTest.live(items));
        var plan=BestBankPlanner.plan("Vorkath",m,Tier.OPTIMAL,account.bank(),account.gearContext(),ItemFacts.catalogDerived());
        assertTrue(plan.blockers().stream().anyMatch(t->t.contains("Shield")),plan.blockers().toString());
        var c=new BanditAssessor(account,(b,mm,t)->plan).assess("Vorkath");
        assertTrue(c.readiness()==Readiness.BLOCKED || c.readiness()==Readiness.UNKNOWN, "Never huntable: "+c);
    }
    @Test void actualCatalogVorkathMeleeDefenderIsNotClassedSafe() {
        String melee=TieredOutfitterCatalog.methods("Vorkath").stream().filter(m->m.startsWith("Melee")).findFirst().orElseThrow();
        var sel=TieredOutfitterCatalog.find("Vorkath",melee,Tier.RECOMMENDED);
        assertTrue(sel.profile().gear().get(6).name().contains("Dragon defender"),"Reproduce real catalog gap");
        var account=ExperienceFoundationJUnitTest.account(ExperienceFoundationJUnitTest.live(
            ExperienceFoundationJUnitTest.kit("Vorkath",melee,Tier.RECOMMENDED)));
        var real=BestBankPlanner.plan("Vorkath",melee,Tier.RECOMMENDED,account.bank(),account.gearContext(),ItemFacts.catalogDerived());
        // The melee template's defender is not dragonfire protection, even when the planner marks it owned.
        var c=new BanditAssessor(account,(b,m,t)->{
            if(!m.equals(melee)) return BestBankPlanner.unknown("Vorkath",m,t);
            return real;
        }).assess("Vorkath");
        assertTrue(c.readiness()==Readiness.UNKNOWN || c.readiness()==Readiness.BLOCKED,"No false-green Vorkath: "+c);
    }
    @Test void fabricatedReadyVorkathWithoutProtectionIsNeverDoable() {
        String m=method("Vorkath");
        var c=new BanditAssessor(a(),(b,mm,t)->pretendReady("Vorkath",m,Tier.OPTIMAL)).assess("Vorkath");
        assertEquals(Readiness.UNKNOWN,c.readiness());
        assertEquals(Reason.PROTECTION_MISSING,c.reasonCode());
    }
    @Test void unresolvedOrUnexplainedVerdictsCannotSuggestHunts() {
        String m=method("Scurrius");
        for(var verdict:List.of(BestBankPlanner.Verdict.UNKNOWN,BestBankPlanner.Verdict.VERIFY,BestBankPlanner.Verdict.NOT_READY)) {
            var p=new BestBankPlanner.Plan("Scurrius",m,Tier.OPTIMAL,Tier.OPTIMAL,verdict,
                ExperienceFoundationJUnitTest.GEAR,ExperienceFoundationJUnitTest.INV,
                List.of(),List.of(),List.of(),0,28,List.of(),"",null,List.of());
            var c=new BanditAssessor(a(),(b,mm,t)->p).assess("Scurrius");
            assertEquals(Readiness.UNKNOWN,c.readiness(),verdict.name());
        }
    }
    @Test void wildernessIsNeverPassedToPlanner() {
        var entry=BossRegistry.all().stream().filter(BossRegistry.Entry::wilderness).findFirst().orElseThrow();
        var c=new BanditAssessor(a(),(b,m,t)->{fail("planner must not run");return null;}).assess(entry.id());
        assertEquals(Reason.WILDERNESS_NOT_ASSESSED,c.reasonCode());
    }
    @Test void ownerOneKillFightsNeverComfortable() {
        for(var boss:List.of("General Graardor","K'ril Tsutsaroth")) {
            var c=new BanditAssessor(a(),(b,m,t)->pretendReady(boss,m,t)).assess(boss);
            assertNotEquals(Readiness.COMFORTABLE,c.readiness());
        }
    }
}
