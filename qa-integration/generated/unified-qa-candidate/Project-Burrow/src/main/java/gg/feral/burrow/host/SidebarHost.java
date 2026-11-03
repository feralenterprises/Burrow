package gg.feral.burrow.host;

import gg.feral.burrow.core.AccountIntelligenceReport;
import gg.feral.burrow.core.PathfinderGuidedCapture;
import gg.feral.burrow.ui.BankMemoryPanel;
import gg.feral.burrow.ui.BankMemoryViewModel;
import gg.feral.burrow.ui.BurrowPanel;
import gg.feral.burrow.ui.GuidedCapturePanel;
import gg.feral.burrow.ui.SidebarWheelRouter;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.LongConsumer;
import javax.swing.*;
import net.runelite.client.ui.PluginPanel;

/**
 * One main RuneLite sidebar, four unchanged TPT tabs. Bank Memory and the
 * first-run checklist are subordinate panels, not a fifth root navigation tab.
 * Exactly one JScrollPane owns wheel scrolling; subordinate views reuse it.
 */
public final class SidebarHost extends PluginPanel {
    private final JScrollPane scroll;
    private final BurrowPanel content;
    private final BankMemoryPanel bank=new BankMemoryPanel();
    private final GuidedCapturePanel capture=new GuidedCapturePanel();
    private final JButton bankButton=new JButton("Bank");
    private final JButton captureButton=new JButton("Capture");
    private final JButton backButton=new JButton("Back");
    private final JLabel reminder=new JLabel(" ");
    private long displayedAccount=-1L;
    private Runnable requestPublication=()->{};
    private LongConsumer requestBankRefresh=account->{};
    private String active="main";
    private int savedMainScroll;

    private SidebarHost(BurrowPanel content) {
        super(false);
        this.content=Objects.requireNonNull(content);
        setLayout(new BorderLayout());
        scroll=new JScrollPane(content,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.getVerticalScrollBar().setBlockIncrement(240);
        SidebarWheelRouter.install(scroll,content);
        JPanel top=new JPanel(new BorderLayout());
        JPanel actions=new JPanel(new GridLayout(1,3,2,1));
        bankButton.setName("OutfitterBankMemory");
        captureButton.setName("FirstRunCapture");
        backButton.setName("BackToFourTabs");
        actions.add(bankButton);actions.add(captureButton);actions.add(backButton);
        top.add(actions,BorderLayout.NORTH);top.add(reminder,BorderLayout.SOUTH);
        add(top,BorderLayout.NORTH);add(scroll,BorderLayout.CENTER);
        bankButton.addActionListener(e->open("bank"));
        captureButton.addActionListener(e->open("capture"));
        backButton.addActionListener(e->open("main"));
        bank.setRequestManualScan(()->{ if(displayedAccount!=-1L)requestBankRefresh.accept(displayedAccount); });
        updateButtons();
    }
    public static SidebarHost mount(BurrowPanel content) {return new SidebarHost(content);}
    public void setRequestPublication(Runnable action){requestPublication=Objects.requireNonNull(action);}
    public void setRequestBankRefresh(LongConsumer action){requestBankRefresh=Objects.requireNonNull(action);}
    public void setManualReviewAction(BiConsumer<Long,PathfinderGuidedCapture.Section> action){capture.setReviewAction(action);}
    public void setQuestSurveyAction(LongConsumer action){capture.setQuestSurveyAction(action);}

    private void open(String selected){
        if(!"main".equals(selected) && displayedAccount==-1L)return;
        if("main".equals(active) && !"main".equals(selected))
            savedMainScroll=scroll.getVerticalScrollBar().getValue();
        active=selected;
        if("main".equals(selected)) {
            scroll.setViewportView(content);
            SwingUtilities.invokeLater(()->scroll.getVerticalScrollBar().setValue(savedMainScroll));
        } else if("bank".equals(selected)) {
            scroll.setViewportView(bank);
            scroll.getVerticalScrollBar().setValue(0);
        } else {
            scroll.setViewportView(capture);
            scroll.getVerticalScrollBar().setValue(0);
        }
        updateButtons();
        if(!"main".equals(selected))requestPublication.run();
    }
    public void showMain(){open("main");}
    private void updateButtons(){
        bankButton.setEnabled(displayedAccount!=-1L && !"bank".equals(active));
        captureButton.setEnabled(displayedAccount!=-1L && !"capture".equals(active));
        backButton.setEnabled(!"main".equals(active));
    }
    /** Called on Swing EDT with immutable account-fenced copies from the client thread. */
    public void publish(long hash,BankMemoryViewModel.Display history,
                        List<PathfinderGuidedCapture.ChecklistStep> steps,
                        boolean initial,boolean latest) {
        publish(hash, history, steps, initial, latest, null);
    }

    public void publish(long hash,BankMemoryViewModel.Display history,
                        List<PathfinderGuidedCapture.ChecklistStep> steps,
                        boolean initial,boolean latest,
                        AccountIntelligenceReport.Snapshot intel) {
        if(!SwingUtilities.isEventDispatchThread())throw new IllegalStateException("Sidebar publication requires EDT");
        if(hash!=displayedAccount){
            active="main";
            scroll.setViewportView(content);
        }
        displayedAccount=hash;
        bank.setSnapshot(Objects.requireNonNull(history));
        capture.setState(hash,steps,initial,latest,intel);
        reminder.setText(buildReminder(hash, history, intel));
        updateButtons();
    }

    private static String buildReminder(long hash, BankMemoryViewModel.Display history,
                                        AccountIntelligenceReport.Snapshot intel) {
        if(hash==-1L) return "Sign in for account memory";
        StringBuilder sb=new StringBuilder("<html><center>");
        boolean any=false;
        if(history.freshness()==BankMemoryViewModel.Freshness.SEVEN_DAYS_OR_OLDER) {
            sb.append("Bank scan 7+ days old. Open bank to refresh.");
            any=true;
        } else if(history.freshness()==BankMemoryViewModel.Freshness.NOT_SCANNED) {
            sb.append("Bank not yet witnessed. Open bank to capture.");
            any=true;
        }
        if(intel!=null) {
            if(intel.skillsQuestsStaleReminderDue()) {
                if(any) sb.append(' ');
                sb.append("Skills/quests 7+ days old — refresh quest survey.");
                any=true;
            }
            if(intel.anyCaptureStaleReminderDue()) {
                if(any) sb.append(' ');
                sb.append("Capture section(s) 7+ days old — reopen those screens.");
                any=true;
            }
            if(intel.nextUnobserved().isPresent()
                && intel.overall()==AccountIntelligenceReport.Overall.FIRST_RUN_INCOMPLETE) {
                if(any) sb.append(' ');
                sb.append("First-run open: ").append(intel.nextUnobserved().get().name().replace('_',' ')).append('.');
                any=true;
            }
        }
        if(!any) return " ";
        sb.append("</center></html>");
        return sb.toString();
    }
    public void showBankRefreshInstruction(long account) {
        if(!SwingUtilities.isEventDispatchThread() || account!=displayedAccount)return;
        bank.setRefreshNotice("Open the OSRS bank normally. TPT saves the next stable full scan. Nothing is auto-clicked.");
    }
    public JScrollPane scrollOwner(){return scroll;}
    public BurrowPanel content(){return content;}
    public BankMemoryPanel bankPanelForTest(){return bank;}
    public GuidedCapturePanel capturePanelForTest(){return capture;}
    public String activeViewForTest(){return active;}
    public long displayedAccountForTest(){return displayedAccount;}
}
