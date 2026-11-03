package gg.feral.burrow.core;

import gg.feral.burrow.ui.BurrowPanel;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

/** Offline assertions; they do NOT pretend to be a real RuneLite quest-event test. */
public final class QuestRecommendationTest {
    private static int checks;
    private static void ok(boolean condition,String message) {
        checks++; if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] unused) throws Exception {
        for (QuestRecommendation.State state : QuestRecommendation.State.values()) {
            QuestRecommendation q = QuestRecommendation.of(state);
            ok(q.state() == state,"all statuses preserved " + state);
            ok(q.eligible() == (state == QuestRecommendation.State.NOT_STARTED ||
                state == QuestRecommendation.State.IN_PROGRESS),"only observed unfinished recommended: " + state);
        }
        ok(QuestRecommendation.of(QuestRecommendation.State.NOT_STARTED).headline().startsWith("Start"),"start recommendation");
        ok(QuestRecommendation.of(QuestRecommendation.State.IN_PROGRESS).headline().startsWith("Finish"),"finish recommendation");
        ok(!QuestRecommendation.of(QuestRecommendation.State.FINISHED).headline().contains("Start"),"finished not repeated");
        ok(!QuestRecommendation.unknown().eligible(),"unknown not eligible");
        ok(!QuestRecommendation.waiting().eligible(),"loading not eligible");
        ok(!QuestRecommendation.offline().eligible(),"offline not eligible");
        // The same epoch fence already used by weapon/combat must reject queued old-account updates.
        LiveWeaponGate fence = new LiveWeaponGate();
        fence.start(); long first = fence.enterLoggedIn();
        ok(fence.canObserve(first),"first account can observe");
        long logout = fence.leaveLoggedIn();
        ok(!fence.isCurrent(first),"first account stale after logout");
        ok(fence.isCurrent(logout),"offline state still paintable");
        long second = fence.enterLoggedIn();
        ok(!fence.canObserve(first),"first account cannot republish after second login");
        ok(fence.canObserve(second),"second account fresh");
        fence.stop(); ok(!fence.isCurrent(second),"stop invalidates queued paint");
        AtomicReference<Throwable> fail=new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                BurrowPanel panel=new BurrowPanel(new BurrowModel());
                panel.setSize(225,880);
                panel.clickAt(85,94); // exact C07 Pathfinder icon
                ok(panel.model().view()==BurrowModel.View.PATHFINDER,"Pathfinder destination still works");
                panel.setObservedQuest(QuestRecommendation.of(QuestRecommendation.State.NOT_STARTED));
                ok(panel.observedQuest().eligible(),"UI receives unstarted real-state projection");
                panel.setObservedQuest(QuestRecommendation.of(QuestRecommendation.State.FINISHED));
                ok(!panel.observedQuest().eligible(),"completed quest immediately loses recommendation");
                panel.setObservedQuest(QuestRecommendation.unknown());
                ok(!panel.observedQuest().eligible(),"unavailable state does not leak previous recommendation");
                panel.setObservedQuest(QuestRecommendation.offline());
                ok(!panel.observedQuest().eligible(),"logout purges recommendation");
                BufferedImage rendered=new BufferedImage(225,880,BufferedImage.TYPE_INT_ARGB);
                var g=rendered.createGraphics();panel.paint(g);g.dispose();
                ok(rendered.getRGB(22,310)!=0,"quest card paints on Pathfinder");
            } catch(Throwable t){ fail.set(t); }
        });
        if (fail.get()!=null) throw new AssertionError(fail.get());
        System.out.println("BUR-0014 QUEST OFFLINE PASS: " + checks + " assertions (Java/Swing synthetic inputs, not RuneLite)");
    }
}
