package gg.feral.burrow.pathfinder;
import gg.feral.burrow.core.PathfinderGoalStore;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Owner-approved single-checked goal and audited primary/secondary tags. */
public final class PathfinderR04TaxonomyJUnitTest {
    private SuggestionEngine.Suggestion suggestion(String id,String title,GuideRule.Category category) {
        return new SuggestionEngine.Suggestion(id,title,"Source reviewed",SuggestionEngine.State.MET,
          "Requirements", "https://oldschool.runescape.wiki", SuggestionEngine.Focus.ANY,
          SuggestionEngine.Attention.ACTIVE,20,category,"",List.of(),"",null,1,null,
          false,List.of(),0);
    }
    @Test void distinctDirectXpAndConstructionMaterialRelationships(){
        var plank=PathfinderActivityTags.classify(suggestion("plank_make","Casting Plank Make",GuideRule.Category.AFK));
        assertEquals(PathfinderActivityTags.Skill.MAGIC,plank.primary());
        assertEquals(PathfinderActivityTags.Skill.CONSTRUCTION,plank.secondary());
        assertEquals(PathfinderActivityTags.Relation.SECONDARY_SUPPLIES,plank.relation());
        assertTrue(plank.note().contains("not direct Construction/Sailing XP"));
        var glass=PathfinderActivityTags.classify(suggestion("superglass_make","Superglass Make",GuideRule.Category.AFK));
        assertEquals(PathfinderActivityTags.Relation.SECONDARY_XP,glass.relation());
        assertEquals(PathfinderActivityTags.Skill.CRAFTING,glass.secondary());
        var quest=PathfinderActivityTags.classify(suggestion("quest_fairytale_ii","Continue Fairytale II",GuideRule.Category.QUEST));
        assertEquals(PathfinderActivityTags.Skill.QUEST,quest.primary());
        assertFalse(quest.dual());
        var fletching=PathfinderActivityTags.classify(suggestion("bolt_tips","Cutting bolt tips",GuideRule.Category.AFK));
        assertEquals(PathfinderActivityTags.Skill.FLETCHING,fletching.primary());
        assertFalse(fletching.dual());
        assertEquals("WINDOW ≥20m",PathfinderActivityTags.timeLabel(suggestion("bolt_tips","Cutting bolt tips",GuideRule.Category.AFK)));
        assertTrue(PathfinderActivityTags.timeTooltip(suggestion("bolt_tips","Cutting bolt tips",GuideRule.Category.AFK)).contains("not a verified"));
        assertEquals("~90s / inv",PathfinderActivityTags.timeLabel(suggestion("afk_plank_make","Autocast Plank Make",GuideRule.Category.AFK)));
        assertEquals("~50m reset",PathfinderActivityTags.timeLabel(suggestion("birdhouses","Birdhouse runs",GuideRule.Category.AFK)));
        assertTrue(PathfinderActivityTags.timeTooltip(suggestion("birdhouses","Birdhouse runs",GuideRule.Category.AFK)).contains("not active run"));
    }
    @Test void oneCheckedGoalPreservesOtherMarksAndAccountIsolation(){
        Map<String,String> cfg=new HashMap<>();final int[] writes={0};
        var store=new PathfinderGoalStore(cfg::get,(k,v)->{writes[0]++;cfg.put(k,v);});
        store.set(17L,"quest_one",SuggestionEngine.GoalMark.SAVED);
        store.set(17L,"quest_two",SuggestionEngine.GoalMark.SAVED); // old data: multiple saved
        store.set(17L,"quest_skip",SuggestionEngine.GoalMark.SKIPPED);
        store.set(18L,"other",SuggestionEngine.GoalMark.SAVED);
        int before=writes[0];
        var selected=store.setSingleActive(17L,"quest_three",true);
        assertEquals(before+1,writes[0],"atomic single write");
        assertEquals(1,selected.values().stream().filter(v->v==SuggestionEngine.GoalMark.SAVED).count());
        assertEquals(SuggestionEngine.GoalMark.SAVED,selected.get("quest_three"));
        assertEquals(SuggestionEngine.GoalMark.SKIPPED,selected.get("quest_skip"));
        assertEquals(SuggestionEngine.GoalMark.SAVED,store.read(18L).get("other"));
        store.setSingleActive(17L,"quest_three",false);
        assertEquals(0,store.read(17L).values().stream().filter(v->v==SuggestionEngine.GoalMark.SAVED).count());
        assertEquals(SuggestionEngine.GoalMark.SKIPPED,store.read(17L).get("quest_skip"));
    }
}
