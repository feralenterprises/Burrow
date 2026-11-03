package gg.feral.burrow.ui;

import gg.feral.burrow.core.AccountIntelligenceReport;
import gg.feral.burrow.core.PathfinderGuidedCapture;
import gg.feral.burrow.core.PathfinderGuidedCapture.ChecklistStep;
import gg.feral.burrow.core.PathfinderGuidedCapture.Section;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.LongConsumer;
import javax.swing.*;

/**
 * First-run / guided account-intelligence view, subordinate to the four main TPT tabs.
 * Manual review is provenance-only; it never certifies an item, diary, or KC.
 * Screens are opened by the player in OSRS, not by this panel.
 */
public final class GuidedCapturePanel extends JPanel {
    private final JLabel title = new JLabel("ACCOUNT CAPTURE · SIGN IN");
    private final JLabel baseline = new JLabel("First-run baseline: unobserved");
    private final JLabel warning = new JLabel("No complete account capture is claimed");
    private final JLabel stale = new JLabel(" ");
    private final JComboBox<Section> selector = new JComboBox<>(Section.values());
    private final JTextArea instruction = new JTextArea(4,19);
    private final JButton reviewed = new JButton("I viewed this screen");
    private final JButton quests = new JButton("Refresh quest survey");
    private final JPanel sectionRows = new JPanel(new GridLayout(0,1,2,3));
    private BiConsumer<Long,Section> reviewAction=(hash,section)->{};
    private LongConsumer questSurveyAction=hash->{};
    private long currentAccount=-1L;

    public GuidedCapturePanel() {
        super(new BorderLayout(3,6));
        setName("GuidedCaptureView");
        JPanel top=new JPanel(new GridLayout(0,1,2,3));
        top.add(title);top.add(baseline);top.add(warning);top.add(stale);
        add(top,BorderLayout.NORTH);
        JPanel middle=new JPanel(new BorderLayout(3,6));
        middle.add(sectionRows,BorderLayout.NORTH);
        JPanel guidance=new JPanel(new BorderLayout(3,4));
        guidance.add(selector,BorderLayout.NORTH);
        instruction.setLineWrap(true);instruction.setWrapStyleWord(true);
        instruction.setEditable(false);instruction.setFocusable(false);
        instruction.setOpaque(false);
        guidance.add(instruction,BorderLayout.CENTER);
        middle.add(guidance,BorderLayout.CENTER);
        // No nested JScrollPane: SidebarHost owns the single sidebar scroll viewport.
        add(middle,BorderLayout.CENTER);
        JPanel controls=new JPanel(new GridLayout(0,1,2,4));
        reviewed.setName("ManualReviewOnly");
        quests.setName("RefreshQuestSurvey");
        controls.add(reviewed);controls.add(quests);
        add(controls,BorderLayout.SOUTH);
        selector.addActionListener(e->updateGuidance());
        reviewed.addActionListener(e->{Section section=(Section)selector.getSelectedItem();
            if(currentAccount!=-1L && section!=null)reviewAction.accept(currentAccount,section);});
        quests.addActionListener(e->{if(currentAccount!=-1L)questSurveyAction.accept(currentAccount);});
        setState(-1L,List.of(),false,false,null);
    }
    public void setReviewAction(BiConsumer<Long,Section> action) {reviewAction=Objects.requireNonNull(action);}
    public void setQuestSurveyAction(LongConsumer action) {questSurveyAction=Objects.requireNonNull(action);}

    /** Legacy publish path — no intelligence report. */
    public void setState(long hash,List<ChecklistStep> steps,boolean initial,boolean latest) {
        setState(hash, steps, initial, latest, null);
    }

    public void setState(long hash,List<ChecklistStep> steps,boolean initial,boolean latest,
                         AccountIntelligenceReport.Snapshot intel) {
        if(!SwingUtilities.isEventDispatchThread())throw new IllegalStateException("Capture UI needs EDT");
        currentAccount=hash;
        if(intel!=null) {
            title.setText(intel.headline());
            baseline.setText(intel.detail());
            warning.setText("Partial native pages never prove a complete log. Unknown ≠ zero.");
            StringBuilder staleText=new StringBuilder();
            if(intel.skillsQuestsStaleReminderDue())
                staleText.append("Skills/quests 7+ days old. ");
            if(intel.anyCaptureStaleReminderDue())
                staleText.append("Capture section(s) 7+ days old. ");
            if(intel.nextUnobserved().isPresent())
                staleText.append("Next: ").append(intel.nextUnobserved().get().name().replace('_',' ')).append('.');
            stale.setText(staleText.length()==0?" ":"<html><div style='width:190px'>"+staleText+"</div></html>");
        } else {
            title.setText(hash==-1L?"ACCOUNT CAPTURE · SIGN IN":"ACCOUNT CAPTURE · INCOMPLETE");
            baseline.setText("Initial skills/quests: "+(initial?"SAVED":"NOT VERIFIED"));
            warning.setText("Latest observed skills/quests: "+(latest?"SAVED":"NOT VERIFIED"));
            stale.setText(" ");
        }
        sectionRows.removeAll();
        for(Section s:Section.values()) {
            ChecklistStep step=steps==null?null:steps.stream().filter(x->x.section()==s).findFirst().orElse(null);
            String coverage=null;
            if(intel!=null) {
                coverage=intel.sections().stream().filter(r->r.section()==s).map(r->r.coverageLabel()).findFirst().orElse(null);
            }
            if(coverage==null) {
                String status=step==null?"UNOBSERVED":step.status().name();
                int pages=step==null?0:step.witnessedPages();
                int rows=step==null?0:step.witnessedEvidenceRows();
                boolean sectionStale=step!=null && step.staleReminderDue(System.currentTimeMillis());
                String age=step==null||step.lastSeenAtMillis()<=0?"never":
                    ("lastSeen="+step.lastSeenAtMillis()+(sectionStale?" · STALE":""));
                coverage=status+" · pages="+pages+" · rows="+rows+" · complete="
                    +(step!=null && step.provesComplete())+" · "+age;
            }
            JLabel row=new JLabel("<html><div style='width:190px'><b>"+s.name().replace('_',' ')
                +"</b><br>"+coverage+"</div></html>");
            sectionRows.add(row);
        }
        reviewed.setEnabled(hash!=-1L);
        quests.setEnabled(hash!=-1L);
        selector.setEnabled(hash!=-1L);
        if(intel!=null && intel.nextUnobserved().isPresent())
            selector.setSelectedItem(intel.nextUnobserved().get());
        updateGuidance();revalidate();repaint();
    }
    private void updateGuidance() {
        Section s=(Section)selector.getSelectedItem();
        instruction.setText(s==null?"Log in first":s.guidance()+". Reviewing this screen records only that you viewed it; unseen values remain unknown.");
    }
    public void selectForTest(Section section) {selector.setSelectedItem(section);}
    public void pressReviewForTest(){reviewed.doClick();}
    public void pressQuestRefreshForTest(){quests.doClick();}
    public long displayedAccountForTest(){return currentAccount;}
    public String firstRunTextForTest(){return baseline.getText();}
    public String titleTextForTest(){return title.getText();}
    public String staleTextForTest(){return stale.getText();}
}
