package gg.feral.burrow.pathfinder;

import gg.feral.burrow.pathfinder.SuggestionEngine.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Build 20 Pathfinder controls: AFK category, ACTIVE/LOW/AFK attention, minutes choices. Synthetic fixtures. */
final class Build20PathfinderControlsJUnitTest {
    static Snapshot account() {
        Map<String,Integer> skills=new HashMap<>();
        for(String s:List.of("Attack","Strength","Defence","Ranged","Magic","Hitpoints","Prayer","Slayer","Fishing",
            "Woodcutting","Mining","Agility","Thieving","Farming","Herblore","Cooking","Firemaking","Crafting",
            "Smithing","Fletching","Hunter","Construction","Runecraft")) skills.put(s,70);
        return new Snapshot(true,skills,Map.of(),BankStatus.UNAVAILABLE,Set.of());
    }
    @Test void afkCategoryOnlyShowsAfkMethods() {
        var r=SuggestionEngine.suggest(account(),new Preferences(Focus.AFK,Attention.ACTIVE,120));
        assertFalse(r.isEmpty(),"AFK methods available for a 70s account");
        assertTrue(r.stream().allMatch(s->s.category()==GuideRule.Category.AFK));
    }
    @Test void afkAttentionRequiresSourcedLookAwayInterval() {
        var r=SuggestionEngine.suggest(account(),new Preferences(Focus.ANY,Attention.AFK,120));
        assertFalse(r.isEmpty());
        assertTrue(r.stream().allMatch(s->s.afkSeconds()>=SuggestionEngine.AFK_MIN_SECONDS),"never inferred AFK");
        assertTrue(r.stream().noneMatch(s->s.category()==GuideRule.Category.QUEST),"quests are never AFK");
        var low=SuggestionEngine.suggest(account(),new Preferences(Focus.ANY,Attention.LOW,120));
        assertTrue(low.size()>=r.size(),"AFK is a stricter subset of low attention");
    }
    @Test void activeIsUnfilteredAndMinutesBound() {
        var all=SuggestionEngine.suggest(account(),new Preferences(Focus.ANY,Attention.ACTIVE,120));
        var low=SuggestionEngine.suggest(account(),new Preferences(Focus.ANY,Attention.LOW,120));
        assertTrue(all.size()>=low.size());
        assertEquals(List.of(15,30,45,60,90,120),SuggestionEngine.MINUTE_CHOICES);
        for(int m:SuggestionEngine.MINUTE_CHOICES)
            assertTrue(SuggestionEngine.suggest(account(),new Preferences(Focus.ANY,Attention.ACTIVE,m))
                .stream().allMatch(s->s.minimumMinutes()<=m),"minutes "+m);
    }
    @Test void enumOrdinalsOfBuild19ValuesPreserved() {
        assertEquals(0,Attention.LOW.ordinal()); assertEquals(1,Attention.ACTIVE.ordinal());
        assertEquals(4,Focus.SUPPLIES.ordinal()); assertEquals(5,Focus.AFK.ordinal());
    }
}
