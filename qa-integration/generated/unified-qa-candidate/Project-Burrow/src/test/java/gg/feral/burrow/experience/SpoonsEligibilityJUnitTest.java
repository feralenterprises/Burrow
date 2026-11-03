package gg.feral.burrow.experience;

import gg.feral.burrow.core.BankEvidence;
import gg.feral.burrow.experience.AccountEvidenceContext.Spellbook;
import gg.feral.burrow.pathfinder.SuggestionEngine.Quest;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** W01-02 negative evidence for quantity/entitlement gates. */
public class SpoonsEligibilityJUnitTest {
    private static AccountEvidenceContext account(Map<Integer,Integer> items,Spellbook book){
        return ExperienceFoundationJUnitTest.account("a","login-1",ExperienceFoundationJUnitTest.live(items),
            99,Quest.FINISHED,Map.of("Varrock Easy",true),book,Map.of());
    }
    private static boolean ok(String id,AccountEvidenceContext a){return SpoonsTipCatalog.find(id).orElseThrow().eligible(a);}
    private static Map<Integer,Integer> plank(int astral,int nature,int earth,int coins){
        return Map.of(1511,1,9075,astral,561,nature,557,earth,995,coins);
    }
    @Test void plankMakeRequiresExactRunesAndCoinAmount(){
        assertTrue(ok("plank_make",account(plank(2,1,15,70),Spellbook.LUNAR)));
        for(var shortfall:List.of(plank(1,1,15,70),plank(2,1,14,70),plank(2,1,15,69))){
            assertFalse(ok("plank_make",account(shortfall,Spellbook.LUNAR)));
        }
        assertFalse(ok("plank_make",account(plank(2,1,15,70),Spellbook.STANDARD)));
    }
    @Test void logTypeChangesCoinCost(){
        assertFalse(ok("plank_make",account(Map.of(6332,1,9075,2,561,1,557,15,995,1049),Spellbook.LUNAR)));
        assertTrue(ok("plank_make",account(Map.of(6332,1,9075,2,561,1,557,15,995,1050),Spellbook.LUNAR)));
    }
    @Test void carriedAndBankStacksAreSummedOnlyOnce(){
        var e=new BankEvidence(Map.of(9075,1,561,1,557,10,1511,1,995,70),
            Map.of(9075,1,557,5),Map.of(),BankEvidence.Population.STABLE_READ,10L,true,false);
        var c=ExperienceFoundationJUnitTest.account("a","login-1",e,99,Quest.FINISHED,Map.of(),Spellbook.LUNAR,Map.of());
        assertTrue(ok("plank_make",c));
    }
    @Test void staleEmptyAndUnknownEvidenceFailClosed(){
        var e=ExperienceFoundationJUnitTest.live(plank(2,1,15,70));
        var good=account(plank(2,1,15,70),Spellbook.LUNAR);
        assertFalse(ok("plank_make",ExperienceFoundationJUnitTest.account("a","login-1",e.markStale(),99,Quest.FINISHED,Map.of(),Spellbook.LUNAR,Map.of())));
        assertFalse(ok("plank_make",ExperienceFoundationJUnitTest.account("a","login-1",null,99,Quest.FINISHED,Map.of(),Spellbook.LUNAR,Map.of())));
        assertFalse(ok("plank_make",account(Map.of(),Spellbook.LUNAR)));
        assertTrue(ok("plank_make",good));
    }
    @Test void zaffDoesNotPretendDiaryIsClaimedOrDailyStockIsAvailable(){
        assertFalse(ok("zaff_battlestaves",account(Map.of(995,105000),Spellbook.STANDARD)));
        assertFalse(ok("zaff_battlestaves",account(Map.of(995,1),Spellbook.STANDARD)));
        assertFalse(ok("zaff_battlestaves",account(Map.of(),Spellbook.STANDARD)));
    }
    @Test void highAlchNeedsFiveFireOneNatureAndKnownSpareBow(){
        assertTrue(ok("high_alch",account(Map.of(851,1,561,1,554,5),Spellbook.STANDARD)));
        assertFalse(ok("high_alch",account(Map.of(851,1,561,1,554,4),Spellbook.STANDARD)));
        assertFalse(ok("high_alch",account(Map.of(561,1,554,5),Spellbook.STANDARD)));
        assertFalse(ok("high_alch",account(Map.of(851,1,561,1,554,5),Spellbook.LUNAR)));
    }
    @Test void genericFarmQuestionDoesNotAssertFarmObservation(){
        assertTrue(ok("check_farms",account(Map.of(),Spellbook.UNKNOWN)));
        assertFalse(ok("cut_gems",account(Map.of(),Spellbook.UNKNOWN)));
        assertEquals(EvidenceStatus.GENERIC_REMINDER,SpoonsTipService.select(account(Map.of(),Spellbook.UNKNOWN),null).status());
    }
    @Test void onePerLoginDismissedTipDoesNotReappear(){
        var a=account(Map.of(),Spellbook.UNKNOWN);
        var first=SpoonsTipService.select(a,null);
        var done=SpoonsTipService.close(first.state(),SpoonsTipService.Closed.MANUAL_DONE);
        assertTrue(SpoonsTipService.select(a,done).tip().isEmpty());
    }
}
