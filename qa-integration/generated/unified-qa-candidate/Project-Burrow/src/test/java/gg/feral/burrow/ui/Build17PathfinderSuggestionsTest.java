package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.pathfinder.SuggestionEngine;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Set;
import javax.swing.SwingUtilities;

/** Actual 225px Swing intake and hitbox checks; snapshots are synthetic TEST FIXTURES. */
public final class Build17PathfinderSuggestionsTest {
    static int assertions;
    static void check(boolean b, String reason){ assertions++; if(!b)throw new AssertionError(reason); }
    static boolean has(BurrowPanel p,String id){return p.pathfinderSuggestions().stream().anyMatch(s->s.id().equals(id));}
    public static void main(String[] a)throws Exception {
        SwingUtilities.invokeAndWait(()-> {
            var m=new BurrowModel();var p=new BurrowPanel(m);p.setSize(225,880);
            p.clickAt(90,94);
            check(m.view()==BurrowModel.View.PATHFINDER,"Pathfinder navigation");
            check(p.pathfinderSuggestions().isEmpty(),"no suggestions without account evidence");
            var snap=new SuggestionEngine.Snapshot(true,
                Map.of("Slayer",80),Map.of("Song of the Elves",SuggestionEngine.Quest.FINISHED,
                    "Dragon Slayer II",SuggestionEngine.Quest.FINISHED,
                    "Cook's Assistant",SuggestionEngine.Quest.FINISHED,
                    "While Guthix Sleeps",SuggestionEngine.Quest.FINISHED),
                SuggestionEngine.BankStatus.VERIFIED_SAME_ACCOUNT,Set.of(29589));
            p.setPathfinderSnapshot(snap);
            check(has(p,"pvm_gauntlet"),"SotE opens a proposed Gauntlet suggestion");
            check(has(p,"pvm_demons"),"positive observed Emberlight creates conditional TD suggestion");
            check(!has(p,"quest_cooks_assistant"),"finished quests not suggested");
            var before=new BufferedImage(225,880,BufferedImage.TYPE_INT_ARGB);
            p.paint(before.getGraphics());
            check(before.getRGB(30,540)!=0,"new suggestions draw at sidebar width");
            p.clickPathfinderActionForTest("focus"); // R04 measured TYPE control, ALL to QUESTS
            check(p.pathfinderFocus()==SuggestionEngine.Focus.QUESTS,"focus control wired");
            check(!has(p,"pvm_gauntlet"),"focus filters activities");
            p.clickPathfinderActionForTest("focus"); // QUESTS -> PVM
            check(p.pathfinderFocus()==SuggestionEngine.Focus.PVM,"PVM focus selected");
            check(has(p,"pvm_demons"),"PVM includes boss suggestions");
            p.clickPathfinderActionForTest("attention"); // ACTIVE -> LOW
            check(p.pathfinderAttention()==SuggestionEngine.Attention.LOW,"attention toggle");
            check(!has(p,"pvm_demons"),"low attention hides active boss suggestion");
            p.clickPathfinderActionForTest("attention"); p.clickPathfinderActionForTest("attention"); // LOW -> AFK -> ACTIVE
            p.clickPathfinderActionForTest("minutes"); // 60 -> 15
            check(p.pathfinderMinutes()==15,"time toggle");
            check(!has(p,"pvm_demons"),"short window excludes 30m TD candidate");
            p.clickPathfinderActionForTest("minutes"); // 15 -> 30
            check(has(p,"pvm_demons"),"30m window includes TD candidate");
            // Selected suggestions are allowed to change only explicit selected boss.
            int idx=-1;
            for(int i=0;i<p.pathfinderSuggestions().size() && i<3;i++)
                if(p.pathfinderSuggestions().get(i).id().equals("pvm_demons"))idx=i;
            check(idx>=0,"TD appears within visible suggestions");
            p.clickPathfinderActionForTest("inspect:"+idx); // body inspects, not checkbox
            p.clickPathfinderActionForTest("openboss");
            check(m.view()==BurrowModel.View.OUTFITTER,"selected boss opens Outfitter");
            check(m.boss().equals("Tormented Demons"),"exact selected boss preserved");
            p.clickAt(91,94);
            p.setPathfinderSnapshot(null);
            check(p.pathfinderSuggestions().isEmpty(),"logout clears prior suggestions");
            System.out.println("Build17PathfinderSuggestionsTest PASS "+assertions+" checks");
        });
    }
}
