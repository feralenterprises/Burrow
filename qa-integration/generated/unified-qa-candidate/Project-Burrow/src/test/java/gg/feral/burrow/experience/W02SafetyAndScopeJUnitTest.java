package gg.feral.burrow.experience;

import gg.feral.burrow.core.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.experience.BanditAssessor.PlanTicket;
import gg.feral.burrow.experience.BanditAssessor.ScopedPlan;
import gg.feral.burrow.experience.BanditBossCandidate.Readiness;
import gg.feral.burrow.pathfinder.SuggestionEngine.Quest;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** W02 negative regressions. Real catalog + separate synthetic account binding tests. */
public class W02SafetyAndScopeJUnitTest {
    static String method(String boss) {return TieredOutfitterCatalog.methods(boss).get(0);}
    static BestBankPlanner.Plan ready(String boss,String method,Tier tier) {
        return new BestBankPlanner.Plan(boss,method,tier,tier,BestBankPlanner.Verdict.READY,
            ExperienceFoundationJUnitTest.GEAR,ExperienceFoundationJUnitTest.INV,
            List.of(),List.of(),List.of(),0,28,List.of(),"fixture",null,List.of());
    }
    static Tier highest(String boss,String method) {
        for(int i=Tier.values().length-1;i>=0;i--)
            if(TieredOutfitterCatalog.find(boss,method,Tier.values()[i])!=null) return Tier.values()[i];
        throw new IllegalArgumentException("No method");
    }
    static AccountEvidenceContext account(String key,String login,long gen, BankEvidence bank) {
        var a=ExperienceFoundationJUnitTest.account(key,login,bank,99,Quest.FINISHED,
            Map.of(),AccountEvidenceContext.Spellbook.UNKNOWN,Map.of());
        return new AccountEvidenceContext(a.accountKey(),a.loginId(),gen,a.bank(),a.snapshot(),a.gearContext(),a.diaries(),a.spellbook());
    }
    static AccountEvidenceContext account() {return account("acct-a","login-a",7,
        ExperienceFoundationJUnitTest.live(Map.of(851,1)));}
    @Test void realVorkathMeleeDefenderCannotBeReadyWithoutShieldWitness() {
        String boss="Vorkath", method;
        method=TieredOutfitterCatalog.methods(boss).stream().filter(m->m.startsWith("Melee"))
            .findFirst().orElseThrow();
        Map<Integer,Integer> items=new HashMap<>();
        for(Tier t:Tier.values()) if(TieredOutfitterCatalog.find(boss,method,t)!=null)
            items.putAll(ExperienceFoundationJUnitTest.kit(boss,method,t));
        // The real method wears a defender, not an anti-dragon shield. A spare in the bank
        // does not prove it is equipped in the resolved plan.
        items.remove(1540);items.remove(11283);items.remove(22003);
        var a=ExperienceFoundationJUnitTest.account(ExperienceFoundationJUnitTest.live(items));
        var plan=BestBankPlanner.plan(boss,method,highest(boss,method),a.bank(),a.gearContext(),ItemFacts.catalogDerived());
        assertFalse(plan.playable(),plan.headline());
        assertTrue(plan.checks().stream().anyMatch(s->s.contains("Vorkath dragonfire protection")),plan.checks().toString());
        var c=BanditAssessor.withPlanner(a).assess(boss);
        assertNotEquals(Readiness.COMFORTABLE,c.readiness());
    }
    @Test void realVorkathMeleeStillNeedsReviewEvenIfSuperAntifireIsInInventory() {
        String method=TieredOutfitterCatalog.methods("Vorkath").stream().filter(m->m.startsWith("Melee")).findFirst().orElseThrow();
        Map<Integer,Integer> items=new HashMap<>();
        for(Tier t:Tier.values()) if(TieredOutfitterCatalog.find("Vorkath",method,t)!=null)
            items.putAll(ExperienceFoundationJUnitTest.kit("Vorkath",method,t));
        items.put(22209,12); // Super antifire inventory alone doesn't establish active prayer/protection
        var a=ExperienceFoundationJUnitTest.account(ExperienceFoundationJUnitTest.live(items));
        var p=BestBankPlanner.plan("Vorkath",method,highest("Vorkath",method),a.bank(),a.gearContext(),ItemFacts.catalogDerived());
        assertFalse(p.playable(),p.headline());
    }
    @Test void wildBossNeverReEnablesBestBank() {
        assertFalse(BossRegistry.bestBankAllowed("King Black Dragon"));
    }
    @Test void scopedPlansAcceptCorrectAccountLoginGenerationAndBankEpoch() {
        var a=account();
        var ass=BanditAssessor.withScopedPlans(a,(b,m,t,tk)->new ScopedPlan(tk,ready(b,m,t)));
        assertNotEquals(Readiness.UNKNOWN,ass.assess("Scurrius").readiness());
    }
    @Test void scopedPlansRejectOldAccountLoginGenerationAndBankEpoch() {
        var a=account();
        for(int i=0;i<5;i++) {
            final int variant=i;
            var ass=BanditAssessor.withScopedPlans(a,(b,m,t,tk)->{
                PlanTicket old=switch(variant) {
                    case 0 -> new PlanTicket("acct-b",tk.loginId(),tk.generation(),tk.bankObservedAtMillis(),tk.carriedAtMillis(),tk.bankStillOpen());
                    case 1 -> new PlanTicket(tk.accountKey(),"old-login",tk.generation(),tk.bankObservedAtMillis(),tk.carriedAtMillis(),tk.bankStillOpen());
                    case 2 -> new PlanTicket(tk.accountKey(),tk.loginId(),tk.generation()-1,tk.bankObservedAtMillis(),tk.carriedAtMillis(),tk.bankStillOpen());
                    case 3 -> new PlanTicket(tk.accountKey(),tk.loginId(),tk.generation(),tk.bankObservedAtMillis()+1,tk.carriedAtMillis(),tk.bankStillOpen());
                    default -> new PlanTicket(tk.accountKey(),tk.loginId(),tk.generation(),tk.bankObservedAtMillis(),tk.carriedAtMillis(),!tk.bankStillOpen());
                };
                return new ScopedPlan(old,ready(b,m,t));
            });
            assertEquals(Readiness.UNKNOWN,ass.assess("Scurrius").readiness(),"variant "+variant);
        }
    }
    @Test void scopedPlansFailClosedOnNullOrThrownCache() {
        var a=account();
        assertEquals(Readiness.UNKNOWN,BanditAssessor.withScopedPlans(a,(b,m,t,tk)->null).assess("Scurrius").readiness());
        assertEquals(Readiness.UNKNOWN,BanditAssessor.withScopedPlans(a,(b,m,t,tk)->{throw new IllegalStateException("eviction");}).assess("Scurrius").readiness());
    }
    @Test void noTicketFromStaleOrUnidentifiedAccount() {
        var a=account();
        assertTrue(BanditAssessor.ticket(a).isPresent());
        assertTrue(BanditAssessor.ticket(account("acct-a","login-a",7,a.bank().markStale())).isEmpty());
        assertTrue(BanditAssessor.ticket(AccountEvidenceContext.signedOut()).isEmpty());
    }
}
