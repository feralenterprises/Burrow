package gg.feral.burrow.ui;

import gg.feral.burrow.core.Outfitter;
import gg.feral.burrow.core.*;
import gg.feral.burrow.experience.*;
import gg.feral.burrow.core.AccountModePreference.Mode;
import gg.feral.burrow.pathfinder.SuggestionEngine;
import gg.feral.burrow.pathfinder.PathfinderActivityTags;
import gg.feral.burrow.core.Outfitter.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.function.IntFunction;
import javax.swing.*;
import java.util.*;

/** Build 16 owner-approved R01 four-panel presentation, Java/Swing; one event/data model. */
public final class BurrowPanel extends JPanel {
    public static final int WIDTH=225, HEIGHT=1390;
    private static final Color BG=new Color(13,17,18), PANEL=new Color(27,32,34);
    private static final Color LINE=new Color(74,79,75), GOLD=new Color(207,159,76);
    private static final Color MINT=new Color(131,195,150), TEXT=new Color(235,236,231);
    private static final Color MUTED=new Color(158,166,164), BAD=new Color(219,134,116);
    private enum UiScale { COMPACT(-1, "Compact"), DEFAULT(0, "Normal"), LARGE(1, "Large"), EXTRA_LARGE(3, "Extra Large");
        final int delta; final String label; UiScale(int delta,String label){this.delta=delta;this.label=label;} }
    private enum AccentTheme {
        BRONZE("Bronze", new Color(207,159,76)),
        BLUE("Blue", new Color(110,167,214)),
        GREEN("Green", new Color(133,191,129)),
        CRIMSON("Crimson", new Color(204,114,114));
        final String label; final Color color; AccentTheme(String label,Color color){this.label=label;this.color=color;}
    }
    private final BurrowModel model;
    private final IntFunction<BufferedImage> itemImage;
    private final boolean illustrativePreview;
    private int hoveredNav = -1;
    // The host opts in to managed first-run setup; offline previews remain usable.
    private boolean accountModeManaged;
    private long activeAccountModeHash = -1L;
    private Mode accountMode;
    private boolean settingsOpen;
    private UiScale uiScale = UiScale.DEFAULT;
    private AccentTheme accentTheme = AccentTheme.BRONZE;
    private FeralTypography.TextWeight textWeight = FeralTypography.TextWeight.REGULAR;
    private java.util.function.Consumer<String> textWeightAction = weight -> {};
    /** Saved independently from appearance for backward-compatible ConfigManager keys. */
    public void setTextWeightAction(java.util.function.Consumer<String> action) {
        textWeightAction=Objects.requireNonNull(action);
    }
    public void setTextWeight(String weight) {
        FeralTypography.TextWeight next=FeralTypography.parseWeight(weight);
        if (textWeight != next) { textWeight=next; revalidate(); repaint(); }
    }
    public String textWeightLabel(){return textWeight.label();}
    private java.util.function.BiConsumer<String,String> appearanceAction = (size,color) -> {};
    /** Only presentation choices, no account/gameplay state. Stored by the host ConfigManager. */
    public void setAppearanceAction(java.util.function.BiConsumer<String,String> action) {
        appearanceAction=Objects.requireNonNull(action);
    }
    public void setAppearance(String size,String color) {
        uiScale=parseSize(size);accentTheme=parseAccent(color);
        revalidate();repaint();
    }
    private static UiScale parseSize(String s) {
        try{return s==null?UiScale.DEFAULT:UiScale.valueOf(s);}catch(IllegalArgumentException e){return UiScale.DEFAULT;}
    }
    private static AccentTheme parseAccent(String s) {
        try{return s==null?AccentTheme.BRONZE:AccentTheme.valueOf(s);}catch(IllegalArgumentException e){return AccentTheme.BRONZE;}
    }
    private void saveAppearance(){
        appearanceAction.accept(uiScale.name(),accentTheme.name());
        textWeightAction.accept(textWeight.name());
    }
    private java.util.function.BiConsumer<Long,Mode> accountModeAction = (hash,mode) -> {};
    public void setAccountModeAction(java.util.function.BiConsumer<Long,Mode> action) {
        accountModeAction=Objects.requireNonNull(action);
    }
    public void setAccountModeState(long accountHash, Mode selected) {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Account mode UI must update on Swing EDT");
        if(accountHash == -1L && selected != null)
            throw new IllegalArgumentException("Account mode requires authenticated account");
        if(!accountModeManaged || activeAccountModeHash != accountHash)settingsOpen=false;
        accountModeManaged=true;
        activeAccountModeHash=accountHash;
        accountMode=selected;
        clearItemHover();revalidate();repaint();
    }
    public Mode currentAccountMode() {return accountMode;}
    public boolean isAccountSetupRequired() {return accountModeManaged && activeAccountModeHash==-1L;}
    public boolean isSettingsOpen() {return settingsOpen;}
    public String uiScaleLabel(){return uiScale.label;}
    public String accentThemeLabel(){return accentTheme.label;}
    private Color accent(){return accentTheme.color;}
    private int scaled(int size){return FeralTypography.size(size,uiScale.delta);} 
    private Font uiFont(int baseSize,boolean emphasized) {
        return FeralTypography.font(baseSize,uiScale.delta,emphasized,textWeight);
    }
    private Color themed(Color color){return GOLD.equals(color)?accent():color;}

    // W05: one compact owner-designed crew strip. No game/account reads happen on the EDT.
    // Host only supplies a current-login, account-verified immutable HomeFeed.
    private HomeFeed crewFeed;
    private java.util.function.BiConsumer<SpoonsTipService.State,SpoonsTipService.Closed> crewTipAction = (state,how) -> {};
    private static final int CREW_SHIFT = 152;
    private int crewRowHeight() {
        return uiScale==UiScale.EXTRA_LARGE?68:uiScale==UiScale.LARGE?50:35;
    }
    private int crewRowStep() { return crewRowHeight()+3; }
    private int extraCrewHeight() { return (crewRowStep()-38)*3; }
    public void setCrewTipAction(java.util.function.BiConsumer<SpoonsTipService.State,SpoonsTipService.Closed> action) {
        crewTipAction = Objects.requireNonNull(action);
    }
    public void setExperienceHomeFeed(HomeFeed value) {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Crew Home publication requires Swing EDT");
        // Clearing a verified feed is mandatory on logout/hop/account change.
        crewFeed = value;
        revalidate(); repaint();
    }
    int crewShiftForTest(){ return crewFeed == null ? 0 : CREW_SHIFT+extraCrewHeight(); }
    java.util.List<String> crewLinesForTest() {
        if(crewFeed == null) return java.util.List.of();
        String spoons=crewFeed.spoons().map(SpoonsTip::text).orElse("No current tip");
        String bandit=crewFeed.banditHunts().isEmpty()?"Readiness unknown - open bank":
            crewFeed.banditHunts().get(0).bossId()+" - "+crewFeed.banditHunts().get(0).readiness();
        String noodles=crewFeed.noodles()==null?"Choose a goal in Pathfinder":
            crewFeed.noodles().nextStep()!=null?crewFeed.noodles().nextStep():
            (crewFeed.noodles().why()==null?"Choose a goal in Pathfinder":crewFeed.noodles().why());
        return java.util.List.of(spoons,bandit,noodles);
    }
    private void paintCrewHome(Graphics2D g) {
        if(crewFeed == null) return;
        card(g,130,142+extraCrewHeight());
        zoneTitle(g,"FERAL CREW",148);
        java.util.List<String> lines=crewLinesForTest();
        String[] titles={"SPOONS TIP", "BANDIT'S HUNTS", "NOODLES' NEXT MOVE"};
        Color[] badges={GOLD,BAD,MINT};
        for(int i=0;i<3;i++) {
            int y=155+i*crewRowStep();
            g.setColor(new Color(34,40,41));g.fillRoundRect(14,y,196,crewRowHeight(),4,4);
            g.setColor(badges[i]);g.drawRoundRect(18,y+6,19,21,4,4);
            FeralUiArt.crew(g,i,18,y+6,20,21);
            label(g,titles[i],42,y+14,9,GOLD,true);
            String shown=lines.get(i);
            // Extra Large wraps actual data instead of silently dropping an objective.
            int available=i==0&&crewFeed.spoons().isPresent()?140:165;
            if (uiScale==UiScale.DEFAULT || uiScale==UiScale.COMPACT) {
                clipped(g,shown,42,y+28,MUTED,available);
            } else {
                Font font=uiFont(10,false);
                g.setFont(font);g.setColor(MUTED);
                int lineHeight=g.getFontMetrics().getHeight();
                int maxLines=uiScale==UiScale.EXTRA_LARGE?3:2;
                java.util.List<String> wrapped=FeralTypography.wrap(g.getFontMetrics(),shown,available,maxLines);
                for(int n=0;n<wrapped.size();n++)g.drawString(wrapped.get(n),42,y+30+n*lineHeight);
            }
            if(i==0&&crewFeed.spoons().isPresent()) {
                label(g,"✓",188,y+25,12,MINT,true);
                label(g,"×",200,y+25,12,MUTED,true);
            }
        }
    }
    private boolean crewClick(int x,int y) {
        if(crewFeed==null || x<14 || x>211 || y<155 || y>=155+3*crewRowStep())return false;
        int row=(y-155)/crewRowStep();
        if(row==0) {
            if(crewFeed.spoons().isPresent() && x>=182) {
                var closed=x<197?SpoonsTipService.Closed.MANUAL_DONE:SpoonsTipService.Closed.DISMISSED;
                crewTipAction.accept(crewFeed.spoonsState(),closed);
                // Fail closed immediately; host publishes authoritative state again after client-thread closure.
                crewFeed=new HomeFeed(crewFeed.generation(),crewFeed.weeklyXp(),crewFeed.biggestWin(),
                    java.util.Optional.empty(),crewFeed.spoonsStatus(),crewFeed.spoonsState(),crewFeed.banditHunts(),crewFeed.noodles());
                repaint();
            }
        } else if(row==1) {
            if(!crewFeed.banditHunts().isEmpty()) {
                String boss=crewFeed.banditHunts().get(0).bossId();
                if(BurrowModel.availableBosses().contains(boss)) {
                    activityMode=false;model.chooseBoss(boss);
                    BurrowModel.View old=model.view();model.show(BurrowModel.View.OUTFITTER);notifyViewChange(old);
                    revalidate();repaint();
                }
            }
        } else if(row==2) {
            BurrowModel.View old=model.view();model.show(BurrowModel.View.PATHFINDER);
            notifyViewChange(old);revalidate();repaint();
        }
        return true;
    }

    // BUR-0019: render a local item label on ordinary mouse movement. Swing's
    // native tooltip routing can be suppressed by the RuneLite host, so this
    // does not depend on a modifier key or ToolTipManager delays.
    private String hoveredItemText;
    private int hoveredItemX, hoveredItemY;
    private int hoveredViewportY;
    String hoveredItemTextForTest() { return model.view()==BurrowModel.View.OUTFITTER ? hoveredItemText : null; }
    // BUR-0040: UI-only explanation for a style with no verified method.
    private String styleFeedback;
    // R11: explicitly scanned same-login bank observations; no persistence, no
    // incomplete-bank absence claims and no change to canonical Wiki profiles.
    private Map<Integer,Integer> observedGearBank = Map.of();
    private volatile boolean bankMatchView;
    private BestBankPreloader.Prepared preparedBank;
    private String bankScanMessage = "Open bank for automatic gear scan";
    // BQA-901 Batch A: typed bank-read state from the plugin (null = nothing reported this login).
    private String bankCaptureHead, bankCaptureDetail;
    /** Shows why Best in Bank is (un)available. Never carries hashes or item lists. EDT only. */
    public void showBankCaptureState(String head,String detail) {
        bankCaptureHead=head; bankCaptureDetail=detail; repaint();
    }

    private Runnable bankScanAction = () -> {};
    public void setBankScanAction(Runnable action) { bankScanAction=Objects.requireNonNull(action); }
    private java.util.function.Consumer<Boolean> outfitterVisibilityAction = active -> {};
    private Runnable loadoutSelectedAction = () -> {};
    public void setOutfitterVisibilityAction(java.util.function.Consumer<Boolean> action) {
        outfitterVisibilityAction=Objects.requireNonNull(action);
    }
    public void setLoadoutSelectedAction(Runnable action) {
        loadoutSelectedAction=Objects.requireNonNull(action);
    }
    public void showAutomaticBankStatus(String message) {
        bankScanMessage=Objects.requireNonNull(message);repaint();
    }
    public boolean isBankMatchView() {return bankModeActive();}
    /** CP-8: thread-safe read for the host's client thread (bank tag layout): Best Bank toggle on, not in Chronicle. */
    public boolean bestBankToggleOnForHost() {return bankMatchView && !chronicleOpen && !activityMode && BossRegistry.bestBankAllowed(displayBossModel().boss()) && selectedMethodHasBankPlan();}
    /** Build 20 owner-locked rule: Best in Bank never applies to a Wilderness or unknown boss,
     *  even if the toggle was left on, a stale Prepared result arrives or a hidden click lands. */
    private boolean bankModeActive() {return !chronicleOpen && !activityMode && bankMatchView &&
        BossRegistry.bestBankAllowed(displayBossModel().boss()) && selectedMethodHasBankPlan();}
    /** True when the owner chose BEST BANK but the current boss forbids it (Wilderness). */
    private boolean bankModeBlockedByWilderness() {return bankMatchView && !BossRegistry.bestBankAllowed(displayBossModel().boss());}
    public void automaticBankResult(Map<Integer,Integer> counts,String message) {
        // Legacy compatible UI intake. Does not manufacture ranked candidates.
        if(!bankModeActive())return;
        preparedBank=null;
        showBankScanResult(counts,message);
    }
    public void automaticBankResult(BestBankPreloader.Prepared prepared,String message) {
        if(!bankModeActive() || prepared==null)return;
        if(!BossRegistry.bestBankAllowed(prepared.key().boss()))return; // never display a Wilderness plan
        if(!prepared.key().equals(new BestBankPreloader.Key(displayBossModel().boss(),displayBossModel().method(),displayBossModel().tier())))return;
        preparedBank=prepared;
        showBankScanResult(prepared.positiveOwned(),message);
    }
    private void notifySelection() {if(model.view()==BurrowModel.View.OUTFITTER)loadoutSelectedAction.run();}
    private void notifyViewChange(BurrowModel.View old) {
        if(old!=model.view())outfitterVisibilityAction.accept(model.view()==BurrowModel.View.OUTFITTER);
        notifySelection();
    }
    public void showBankScanResult(Map<Integer,Integer> counts,String message) {
        observedGearBank=Map.copyOf(counts);
        // The prepared plan is retained only when its generation and key are current.
        bankScanMessage=message == null ? "Observation unavailable" : message;
        repaint();
    }
    public void clearBankScan() {
        preparedBank=null;observedGearBank=Map.of(); bankScanMessage="Open bank for automatic fresh snapshot";
        repaint();
    }
    /** Build 19 whole-trip plan for the exact selected boss/method/tier, or null. */
    BestBankPlanner.Plan currentTripPlan() {
        if(!bankModeActive() || preparedBank==null || preparedBank.plan()==null)return null;
        var key=new BestBankPreloader.Key(displayBossModel().boss(),displayBossModel().method(),displayBossModel().tier());
        return key.equals(preparedBank.key())?preparedBank.plan():null;
    }
    private VerifiedOutfitterCatalog.Item inventoryItem(int index) {
        var profile=displayBossModel().profile();
        if(profile==null)return null;
        var plan=currentTripPlan();
        return plan!=null?plan.inventory().get(index).shown():profile.inventory().get(index);
    }
    private TierAwareBankResolver.Result currentOwnedPlan() {
        if(!bankModeActive() || preparedBank==null || observedGearBank.isEmpty())return null;
        var key=new BestBankPreloader.Key(displayBossModel().boss(),displayBossModel().method(),displayBossModel().tier());
        return key.equals(preparedBank.key())?preparedBank.tierResult():null;
    }
    private VerifiedOutfitterCatalog.Item ownedGear(int index) {
        var profile=displayBossModel().profile();
        if(profile==null)return null;
        var trip=currentTripPlan();
        if(trip!=null)return trip.gear().get(index).shown();
        var plan=currentOwnedPlan();
        if(plan!=null)return plan.slots().get(index).displayed();
        var original=profile.gear().get(index);
        if(!bankModeActive() || observedGearBank.isEmpty() || original==null)return original;
        BestBankMatches.Match match=BestBankMatches.gear(profile,observedGearBank).get(index);
        if(match.status()==BestBankMatches.Status.REVIEWED_VARIANT)
            return new VerifiedOutfitterCatalog.Item(match.observedId(),
                original.name()+" (reviewed owned variant)",original.amount());
        return original;
    }
    private void gearBankOutline(Graphics2D g,int index,int x,int y,int w,int h) {
        if(!bankModeActive() || observedGearBank.isEmpty())return;
        var profile=displayBossModel().profile(); if(profile==null)return;
        var trip=currentTripPlan();
        var plan=currentOwnedPlan();
        Color color;
        if(trip!=null) {
            color=switch(trip.gear().get(index).status()) {
                case OWNED,OWNED_VARIANT -> MINT;
                case OWNED_SUBSTITUTE,LOW_STOCK,REQUIREMENT_UNKNOWN -> GOLD;
                case LEVEL_UNMET,QUEST_UNMET,NOT_OBSERVED,PARTIAL_STACK -> BAD;
                case OPTIONAL_MISSING -> MUTED; // CP-1: optional upgrade not owned; slot is empty in the trip
                case EMPTY -> LINE;
            };
        } else if(plan!=null) {
            color=switch(plan.slots().get(index).kind()) {
                case EXACT,REVIEWED_VARIANT -> MINT;
                case LOWER_TIER_OWNED,WIKI_ALTERNATIVE_OWNED,LOW_STOCK_OWNED -> GOLD;
                case PARTIAL_STACK -> GOLD;
                case NOT_OBSERVED -> MUTED;
                case EMPTY -> LINE;
            };
        } else {
            var match=BestBankMatches.gear(profile,observedGearBank).get(index);
            color=switch(match.status()) {
                case EXACT,REVIEWED_VARIANT -> MINT;
                case PARTIAL_STACK -> GOLD;
                case NOT_OBSERVED -> MUTED;
                case UNSPECIFIED -> LINE;
            };
        }
        g.setColor(color);g.drawRoundRect(x,y,w,h,4,4);
    }

    private void inventoryBankOutline(Graphics2D g,int index,int x,int y,int w,int h) {
        var trip=currentTripPlan();
        if(trip==null)return;
        Color color=switch(trip.inventory().get(index).status()) {
            case STOCKED,SWITCH_OWNED -> MINT;
            case LOWER_DOSE,SHORT,QUANTITY_UNSPECIFIED,VARIANT_UNSPECIFIED -> GOLD;
            case NOT_OBSERVED,SWITCH_NOT_OBSERVED,SWITCH_REQUIREMENT_UNMET -> BAD;
            case EMPTY -> null;
        };
        if(color==null)return;
        g.setColor(color);g.drawRoundRect(x,y,w,h,4,4);
    }
    // ---- Build 19 Best Bank status card (top of the loadout, before equipment) ----
    private record StatusLine(String text,Color color,boolean bold) {}
    private static final int BANK_STATUS_TOP_PAD=26, BANK_STATUS_LINE=14;
    private java.util.List<StatusLine> bankStatusLines() {
        java.util.List<StatusLine> out=new ArrayList<>();
        var plan=currentTripPlan();
        if(plan==null) {
            if(observedGearBank.isEmpty()) {
                // BQA-901 BF-09: say WHY there is no plan and that the drawn gear is not ownership-checked.
                FontMetrics fm=getFontMetrics(PREPARATION_FONT);
                String head=bankCaptureHead==null?"BEST IN BANK UNAVAILABLE · OPEN BANK":bankCaptureHead;
                for(String line:wrapItemName(head,fm,186))out.add(new StatusLine(line,GOLD,true));
                String detail=bankCaptureDetail!=null && !bankCaptureDetail.isBlank()?bankCaptureDetail
                    :"Bank, inventory and worn gear are read automatically.";
                for(String line:wrapItemName(detail,fm,186)) { if(out.size()>=6)break; out.add(new StatusLine(line,MUTED,false)); }
                for(String line:wrapItemName("Wiki reference gear below is NOT ownership-checked",fm,186))
                    out.add(new StatusLine(line,BAD,true));
            } else {
                out.add(new StatusLine("PREPARING SELECTED TRIP PLAN",GOLD,true));
                for(String line:wrapItemName(bankScanMessage,getFontMetrics(PREPARATION_FONT),186))
                    if(out.size()<4)out.add(new StatusLine(line,MUTED,false));
            }
            return out;
        }
        Color head=switch(plan.verdict()){case READY->MINT;case READY_SHORT_SUPPLIES,VERIFY->GOLD;default->BAD;};
        FontMetrics fm=getFontMetrics(PREPARATION_FONT);
        for(String line:wrapItemName(plan.headline(),fm,186))out.add(new StatusLine(line,head,true));
        for(String line:wrapItemName(plan.trust(),fm,186))out.add(new StatusLine(line,MUTED,false));
        // R05: why owned gear was not used (unreviewed variant / withheld core swap / nothing reviewed owned).
        if(preparedBank!=null && !preparedBank.diagnosis().isEmpty()) {
            int shown=0;
            for(var f:preparedBank.diagnosis()) {
                if(shown++>=3)break;
                Color c=f.cause()==BestBankDiagnosis.Cause.OWNED_VARIANT_NOT_REVIEWED?GOLD:MUTED;
                for(String line:wrapItemName(f.text(),fm,186))out.add(new StatusLine(line,c,false));
            }
            if(preparedBank.diagnosis().size()>3)
                out.add(new StatusLine("+"+(preparedBank.diagnosis().size()-3)+" more slots not owned",MUTED,false));
        }
        for(String o:plan.options()) for(String l:wrapItemName("◆ Option: "+o,fm,186)) out.add(new StatusLine(l,MINT,false));
        if(plan.candidateSummaries().size()>1)
            for(String line:wrapItemName("Tiers: "+String.join(" · ",plan.candidateSummaries()),fm,186))
                out.add(new StatusLine(line,MUTED,false));
        int budget=5;
        for(String b:plan.blockers()) { if(budget--<=0)break; for(String l:wrapItemName("✕ "+b,fm,186))out.add(new StatusLine(l,BAD,false)); }
        for(String c:plan.checks()) { if(budget--<=0)break; for(String l:wrapItemName("? "+c,fm,186))out.add(new StatusLine(l,GOLD,false)); }
        for(String c:plan.shortages()) { if(budget--<=0)break; for(String l:wrapItemName("− "+c,fm,186))out.add(new StatusLine(l,GOLD,false)); }
        if(!plan.upgrades().isEmpty()) {
            // CP-1: optional slots the player does not own. Listed once, compactly; they never block the trip.
            java.util.List<String> slotsOnly=new ArrayList<>();
            for(String u:plan.upgrades()) { int c=u.indexOf(':'); slotsOnly.add(c>0?u.substring(0,c):u); }
            for(String l:wrapItemName("○ Optional slots empty (not blockers): "+String.join(", ",slotsOnly),fm,186))
                out.add(new StatusLine(l,MUTED,false));
        }
        int hidden=plan.blockers().size()+plan.checks().size()+plan.shortages().size()-5;
        if(hidden>0)out.add(new StatusLine("+"+hidden+" more (hover cells for details)",MUTED,false));
        String cap="28-slot plan: "+plan.usedSlots()+" used · "+plan.freeSlots()+" free";
        for(var sw:plan.switches())
            cap+=" · "+(sw.verified()?(sw.fits()?"switch fits":"switch needs "+sw.freeSlotsNeeded()):"switch unverified");
        for(String l:wrapItemName(cap,fm,186))out.add(new StatusLine(l,MUTED,false));
        return out;
    }
    private boolean bankDetailsExpanded;
    private int bankStatusHeight() {
        if(!bankModeActive() || model.view()!=BurrowModel.View.OUTFITTER || displayBossModel().profile()==null)return 0;
        return BANK_STATUS_TOP_PAD+(bankDetailsExpanded?bankStatusLines().size():2)*BANK_STATUS_LINE+12;
    }
    /** The display no longer pushes equipment below Rare Drops: they follow inventory. */
    private int preTrackerShift() {return 0;}
    private int contentShift() {return equipmentOffset();}
    private static final int RARE_GAP=8;
    private void bankStatusCard(Graphics2D g) {
        int h=bankStatusHeight();
        if(h==0)return;
        card(g,397,h-6);
        small(g,bankDetailsExpanded?"BEST BANK ▲":"BEST BANK ▼",20,413,GOLD); // CP-7: short, leaves room for the FILL chip
        // CP-7: what Best in Bank optimises when it fills empty slots with your own gear. Click to cycle.
        g.setColor(new Color(56,43,28));g.fillRoundRect(FOCUS_CHIP_X,400,FOCUS_CHIP_W,17,4,4);
        g.setColor(accent());g.drawRoundRect(FOCUS_CHIP_X,400,FOCUS_CHIP_W,17,4,4);
        small(g,"FILL "+bestBankFocus.label.toUpperCase(Locale.ROOT)+" ▸",FOCUS_CHIP_X+5,412,GOLD);
        int y=397+BANK_STATUS_TOP_PAD+8;
        java.util.List<StatusLine> lines=bankStatusLines();
        if(!bankDetailsExpanded) {
            String headline=lines.isEmpty()?"Bank result awaiting observation":lines.get(0).text();
            label(g,elideSessionText(g,headline,183),17,y,10,lines.isEmpty()?MUTED:lines.get(0).color(),true);
            y+=BANK_STATUS_LINE;
            int blockers=currentTripPlan()==null?0:currentTripPlan().blockers().size()+currentTripPlan().checks().size()+currentTripPlan().shortages().size();
            small(g,blockers>0?blockers+" issues · open details for context":
                "Gear ownership requires verified bank snapshot",17,y,MUTED);
        } else for(StatusLine line:lines) {
            label(g,line.text(),17,y,10,line.color(),line.bold());
            y+=BANK_STATUS_LINE;
        }
    }
    // ---- CP-7: Best in Bank fill focus chip ----
    static final int FOCUS_CHIP_X=128, FOCUS_CHIP_W=84;
    private OwnedGearStats.Focus bestBankFocus=OwnedGearStats.Focus.MATCH;
    private java.util.function.Consumer<OwnedGearStats.Focus> bestBankFocusAction=f->{};
    /** EDT only. Shows the host's current focus. */
    public void setBestBankFocus(OwnedGearStats.Focus focus){ if(focus!=null && focus!=bestBankFocus){bestBankFocus=focus;repaint();} }
    public void setBestBankFocusAction(java.util.function.Consumer<OwnedGearStats.Focus> action){bestBankFocusAction=Objects.requireNonNull(action);}
    OwnedGearStats.Focus bestBankFocusForTest(){return bestBankFocus;}
    String bankStatusTextForTest() {
        StringBuilder b=new StringBuilder();
        for(StatusLine l:bankStatusLines())b.append(l.text()).append('\n');
        return b.toString();
    }
    int contentShiftForTest() { return contentShift(); }
    String styleFeedbackForTest() { return styleFeedback; }
    /** W07: same selection engine drives mouse actions, button availability and hover text. */
    private StyleMethodPicker.Choice methodChoiceForStyle(String style) {
        var requested=CombatStyle.parse(style);
        return requested.map(cs->StyleMethodPicker.choose(displayBossModel().boss(),cs,
            displayBossModel().method(),displayBossModel().tier())).orElse(null);
    }
    private String availableMethodForStyle(String style) {
        var choice=methodChoiceForStyle(style);
        return choice==null?null:choice.method();
    }
    private boolean selectedMethodIsResearchGear() {
        return StyleMethodPicker.isProvisional(displayBossModel().boss(),displayBossModel().method(),displayBossModel().tier());
    }
    private boolean selectedMethodHasBankPlan() {
        return displayBossModel().profile()!=null && !selectedMethodIsResearchGear();
    }
    private static final int CONTENT_SHIFT = -8;
    private static final int PREPARATION_Y = 1014;
    private static final Font PREPARATION_FONT = new Font("Dialog", Font.PLAIN, 10);
    // BUR-0035: make PvP travel risk visible without relying on a tooltip.
    private static final int KBD_WARNING_SHIFT = 72;
    static boolean showsWildernessTravelWarning(String boss) {
        return BossRegistry.wilderness(boss); // Build 20: every Wilderness boss, KBD as approach risk
    }
    private boolean showsWildernessTravelWarning() {
        return showsWildernessTravelWarning(displayBossModel().boss());
    }
    private static final String KBD_WARNING_TOOLTIP =
        "Usual routes to King Black Dragon cross PvP Wilderness; " +
        "the King Black Dragon's lair itself is outside the Wilderness.";
    // This is intentionally NOT part of the recommendation plan or demo bank snapshot.
    private LiveWeapon observedWeapon = LiveWeapon.offline();
    private LiveCombat observedCombat = LiveCombat.offline();
    private QuestRecommendation observedQuest = QuestRecommendation.offline();
    private IronLedgerQuestSurvey.Snapshot questDiscovery =
        new IronLedgerQuestSurvey.Snapshot(Map.of(),false,0,0,0);
    private SuggestionEngine.Snapshot pathfinderObserved;
    private java.util.List<SuggestionEngine.Suggestion> pathfinderSuggestions=java.util.List.of();
    private SuggestionEngine.Focus pathfinderFocus=SuggestionEngine.Focus.ANY;
    private SuggestionEngine.Attention pathfinderAttention=SuggestionEngine.Attention.ACTIVE;
    private int pathfinderMinutes=60;
    private String pathfinderDetail="";
    private String pathfinderSelectedId;
    /** Observed source data is assembled on RuneLite client thread and posted on Swing EDT. */
    public void setPathfinderSnapshot(SuggestionEngine.Snapshot snapshot) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Pathfinder intake requires Swing EDT");
        if(snapshot!=null && !snapshot.verifiedIdentity())throw new IllegalArgumentException("Unverified identity");
        pathfinderObserved=snapshot;
        rebuildPathfinderSuggestions();
        repaint();
    }
    public java.util.List<SuggestionEngine.Suggestion> pathfinderSuggestions(){return pathfinderSuggestions;}
    public SuggestionEngine.Focus pathfinderFocus(){return pathfinderFocus;}
    public SuggestionEngine.Attention pathfinderAttention(){return pathfinderAttention;}
    public int pathfinderMinutes(){return pathfinderMinutes;}
    private int pathfinderOpenItems, pathfinderReadyItems;
    /** How many activities the same type and time would show under ACTIVE; only set when the list is empty. */
    private int pathfinderActiveFallback;
    private void rebuildPathfinderSuggestions() {
        pathfinderSuggestions=pathfinderObserved==null?java.util.List.of():SuggestionEngine.suggest(pathfinderObserved,
            new SuggestionEngine.Preferences(pathfinderFocus,pathfinderAttention,pathfinderMinutes));
        // W06: only approved activity prerequisites may refine an existing suggestion.
        // Current activity research is CHECK, so this is an identity operation today.
        pathfinderSuggestions=gg.feral.burrow.intelligence.PathfinderIntelligenceBridge.enrich(pathfinderObserved,pathfinderSuggestions);
        pathfinderActiveFallback=pathfinderObserved==null || !pathfinderSuggestions.isEmpty()
            || pathfinderAttention==SuggestionEngine.Attention.ACTIVE?0:SuggestionEngine.suggest(pathfinderObserved,
                new SuggestionEngine.Preferences(pathfinderFocus,SuggestionEngine.Attention.ACTIVE,pathfinderMinutes)).size();
        // Coverage counts computed once per input change, never per paint.
        var all=pathfinderObserved==null?java.util.List.<SuggestionEngine.Suggestion>of():SuggestionEngine.evaluateAll(pathfinderObserved);
        pathfinderOpenItems=all.size();
        pathfinderReadyItems=(int)all.stream().filter(z->z.readiness()==SuggestionEngine.State.MET).count();
        if(pathfinderSelectedId!=null && pathfinderSuggestions.stream().noneMatch(s->s.id().equals(pathfinderSelectedId)))
            pathfinderSelectedId=null;
    }
    /** Build 19: explicit owner goal actions, routed to the RuneLite client thread by the host. */
    public interface PathfinderGoalAction { void accept(long accountHash,String ruleId,SuggestionEngine.GoalMark mark); }
    private PathfinderGoalAction pathfinderGoalAction=(h,i,m)->{};
    public void setPathfinderGoalAction(PathfinderGoalAction action){pathfinderGoalAction=Objects.requireNonNull(action);}
    private static final int PF_LIST_ROWS=12, PF_ROW_H=34;
    private record PfLayout(int detailY,int detailH,java.util.List<String> detailLines,int buttonsY,int listY,
                            java.util.List<SuggestionEngine.Suggestion> rows,int coverageY,int coverageH,
                            java.util.List<String> coverageLines,int dropY,int dropH,int bottom,
                            int goalsY,int goalsH,java.util.List<RareDropSummary> goals) {}
    private int pathfinderActionY(){return pathfinderSuggestions.isEmpty()?607:806;}
    private int pathfinderExtrasStartY(){return pathfinderSuggestions.isEmpty()?651:850;}
    private PfLayout pathfinderLayout() {
        FontMetrics fm=getFontMetrics(PREPARATION_FONT);
        int y=pathfinderExtrasStartY();
        java.util.List<String> detail=new ArrayList<>();
        var sel=selectedSuggestion();
        int detailY=y, detailH=0, buttonsY=-1;
        if(sel.isPresent()) {
            var x=sel.get();
            detail.add("§"+x.title());
            detail.add("@"+(x.readiness()==SuggestionEngine.State.MET&&!x.readyNow()?"PREREQS MET: start time unknown for this window":
                x.readiness()==SuggestionEngine.State.MET?"READY: prerequisites observed":
                x.readiness()==SuggestionEngine.State.UNMET?"BLOCKED: prerequisite missing":"UNVERIFIED: some data not observed"));
            for(String l:wrapItemName("Why: "+x.why(),fm,183))detail.add(l);
            if(!x.missing().isEmpty())for(String l:wrapItemName("Missing: "+String.join(", ",x.missing()),fm,183))detail.add("!"+l);
            for(String l:wrapItemName(x.nextStep().isBlank()?"Next: see source":x.nextStep(),fm,183))detail.add(">"+l);
            for(String u:x.unverified())for(String l:wrapItemName("Check: "+u,fm,183))detail.add("?"+l);
            String src=x.source().split(" ; ")[0].replace("https://","");
            for(String l:wrapItemName("Source: "+src,fm,183))detail.add("~"+l);
            if(x.goal()==SuggestionEngine.GoalMark.SAVED)detail.add("@SAVED GOAL (your choice, this account)");
            detailH=26+detail.size()*14+38;
            buttonsY=detailY+detailH-34;
            y+=detailH+10;
        }
        java.util.List<SuggestionEngine.Suggestion> rows=new ArrayList<>();
        for(int i=3;i<pathfinderSuggestions.size() && rows.size()<PF_LIST_ROWS;i++)rows.add(pathfinderSuggestions.get(i));
        int listY=y;
        y+=30+Math.max(1,rows.size())*PF_ROW_H+8;
        java.util.List<String> cov=new ArrayList<>();
        cov.add(pathfinderOpenItems+" open guide items for this account");
        cov.add(pathfinderReadyItems+" ready now · "+pathfinderSuggestions.size()+" match filters");
        cov.add(gg.feral.burrow.pathfinder.GuideQuestOrder.entries().size()+" quests · "+
            gg.feral.burrow.pathfinder.GuideData.rules().stream().filter(r->r.category()==gg.feral.burrow.pathfinder.GuideRule.Category.AFK).count()+" AFK · "+
            gg.feral.burrow.pathfinder.GuideData.rules().stream().filter(r->r.category()==gg.feral.burrow.pathfinder.GuideRule.Category.MONEY).count()+" GP");
        cov.add("ironman.guide v4 · OSRS Wiki Ironman guide");
        cov.add("Full 576-step checklist: ironman.guide");
        int coverageY=y, coverageH=30+cov.size()*14+8;
        y+=coverageH+10;
        int dropY=y, dropH=30+5*14+8;
        y+=dropH+10;
        // Build 20: the owner's selected rare-drop goals (same verified account only).
        java.util.List<RareDropSummary> goals=pathfinderObserved!=null && rareDropState!=null?
            RareDropSummary.all(rareDropState):java.util.List.of();
        int goalsY=y, goalsH=goals.isEmpty()?0:30+goals.size()*PF_GOAL_H+22;
        if(goalsH>0) y+=goalsH+10;
        return new PfLayout(detailY,detailH,detail,buttonsY,listY,rows,coverageY,coverageH,cov,dropY,dropH,y,goalsY,goalsH,goals);
    }
    private void pathfinderExtras(Graphics2D g) {
        var L=pathfinderLayout();
        if(L.detailH()>0) {
            card(g,L.detailY(),L.detailH());
            small(g,"SELECTED STEP",20,L.detailY()+18,GOLD);
            int y=L.detailY()+36;
            for(String line:L.detailLines()) {
                char k=line.charAt(0);
                String t=line.substring(1);
                switch(k) {
                    case '§' -> clipped(g,t,17,y,TEXT,190);
                    case '@' -> small(g,t,17,y,t.startsWith("READY")||t.startsWith("SAVED")?MINT:t.startsWith("BLOCKED")?BAD:GOLD);
                    case '!' -> small(g,t,17,y,BAD);
                    case '>' -> small(g,t,17,y,MINT);
                    case '?' -> small(g,t,17,y,GOLD);
                    case '~' -> small(g,t,17,y,MUTED);
                    default -> small(g,line,17,y,TEXT);
                }
                y+=14;
            }
            boolean saved=selectedSuggestion().map(x->x.goal()==SuggestionEngine.GoalMark.SAVED).orElse(false);
            String[] labels={saved?"UNSAVE":"SAVE GOAL","SKIP","DONE"};
            for(int i=0;i<3;i++) {
                int bx=13+i*67;
                g.setColor(i==0?new Color(82,65,39):new Color(31,39,41));g.fillRoundRect(bx,L.buttonsY(),64,26,4,4);
                g.setColor(i==0?GOLD:LINE);g.drawRoundRect(bx,L.buttonsY(),64,26,4,4);
                label(g,labels[i],bx+(i==0?6:14),L.buttonsY()+17,9,i==0?GOLD:TEXT,true);
            }
        }
        zoneTitle(g,"MORE SUGGESTIONS",L.listY()+16);
        if(L.rows().isEmpty()) small(g,pathfinderObserved==null?"Log in for account-based suggestions":
            "No further matches; try ALL / ACTIVE / 60 MIN",19,L.listY()+44,MUTED);
        for(int i=0;i<L.rows().size();i++) {
            var x=L.rows().get(i);
            int ry=L.listY()+26+i*PF_ROW_H;
            g.setColor(x.id().equals(pathfinderSelectedId)?new Color(48,41,31):new Color(24,29,30));
            g.fillRoundRect(11,ry,203,PF_ROW_H-4,4,4);
            g.setColor(x.id().equals(pathfinderSelectedId)?GOLD:new Color(55,62,61));g.drawRoundRect(11,ry,203,PF_ROW_H-4,4,4);
            pfCheckbox(g,17,ry+6,x.goal()==SuggestionEngine.GoalMark.SAVED);
            pfIcon(g,PathfinderActivityTags.classify(x).primary(),40,ry+3);
            clipped(g,(i+4)+". "+x.title(),63,ry+13,TEXT,142);
            String tag=x.category()+" · "+(x.readyNow()?"READY":x.readiness()==SuggestionEngine.State.MET?"TRAVEL ?":
                x.readiness()==SuggestionEngine.State.UNMET?"BLOCKED":"UNVERIFIED")+(x.goal()==SuggestionEngine.GoalMark.SAVED?" · GOAL":"");
            clipped(g,PathfinderActivityTags.timeLabel(x)+" · "+tag,40,ry+26,
                x.readyNow()?MINT:x.readiness()==SuggestionEngine.State.UNMET?BAD:GOLD,165);
        }
        card(g,L.coverageY(),L.coverageH());
        small(g,"GUIDE COVERAGE / SOURCES",20,L.coverageY()+18,GOLD);
        for(int i=0;i<L.coverageLines().size();i++)small(g,L.coverageLines().get(i),17,L.coverageY()+36+i*14,i<2?TEXT:MUTED);
        card(g,L.dropY(),L.dropH());
        small(g,"DROP ODDS · INDEPENDENT ROLLS",20,L.dropY()+18,GOLD);
        var preset=gg.feral.burrow.pathfinder.DropChance.PRESETS.get(0);
        double p=preset.p();
        small(g,"1/"+preset.denominator()+" "+preset.label(),17,L.dropY()+36,TEXT);
        small(g,"25 kc "+gg.feral.burrow.pathfinder.DropChance.percent(gg.feral.burrow.pathfinder.DropChance.atLeastOne(p,25))+
            " · 75 kc "+gg.feral.burrow.pathfinder.DropChance.percent(gg.feral.burrow.pathfinder.DropChance.atLeastOne(p,75)),17,L.dropY()+50,MINT);
        small(g,"150 kc "+gg.feral.burrow.pathfinder.DropChance.percent(gg.feral.burrow.pathfinder.DropChance.atLeastOne(p,150))+
            " · 50% by "+gg.feral.burrow.pathfinder.DropChance.killsForConfidence(p,0.5)+" · 90% by "+
            gg.feral.burrow.pathfinder.DropChance.killsForConfidence(p,0.9),17,L.dropY()+64,MINT);
        small(g,"Chance, not a guarantee; no KC assumed",17,L.dropY()+78,MUTED);
        small(g,"Source: ironman.guide drop calculator",17,L.dropY()+92,MUTED);
        if(L.goalsH()>0) {
            card(g,L.goalsY(),L.goalsH());
            small(g,"RARE DROP GOALS · OUTFITTER",20,L.goalsY()+18,GOLD);
            for(int i=0;i<L.goals().size();i++) {
                var sum=L.goals().get(i); int gy=L.goalsY()+26+i*PF_GOAL_H;
                g.setColor(new Color(24,29,30));g.fillRoundRect(11,gy,203,PF_GOAL_H-4,4,4);
                g.setColor(new Color(55,62,61));g.drawRoundRect(11,gy,203,PF_GOAL_H-4,4,4);
                clipped(g,sum.encounter().encounter()+" · "+sum.mode()+" of "+sum.selected().size(),17,gy+13,TEXT,190);
                String kc=sum.kc().known()?"KC "+String.format(Locale.ROOT,"%,d",sum.kc().value().getAsLong())+
                    (sum.kc().source()==RareDropStore.Source.MANUAL?" (manual)":"")+" · now "+RareDropMath.percent(sum.chanceNow().getAsDouble()):"KC unknown";
                clipped(g,kc,17,gy+26,sum.kc().known()?MINT:MUTED,190);
                clipped(g,sum.milestoneText(2),17,gy+39,GOLD,190);
            }
            small(g,"Tap a goal to open it in Outfitter",17,L.goalsY()+L.goalsH()-9,MUTED);
        }
    }
    private static final int PF_GOAL_H=48;
    /** Click handling for the Build 19 Pathfinder extension (content coordinates). */
    private boolean pathfinderExtrasClick(int x,int sourceY) {
        var L=pathfinderLayout();
        if(L.buttonsY()>=0 && inside(x,sourceY,13,L.buttonsY(),199,26)) {
            var sel=selectedSuggestion();
            if(sel.isEmpty() || activeAccountModeHash==-1L)return true;
            int i=Math.min(2,(x-13)/67);
            boolean saved=sel.get().goal()==SuggestionEngine.GoalMark.SAVED;
            SuggestionEngine.GoalMark mark=i==0?(saved?null:SuggestionEngine.GoalMark.SAVED):
                i==1?SuggestionEngine.GoalMark.SKIPPED:SuggestionEngine.GoalMark.DONE;
            pathfinderGoalAction.accept(activeAccountModeHash,sel.get().id(),mark);
            return true;
        }
        for(int i=0;i<L.goals().size();i++) {
            int gy=L.goalsY()+26+i*PF_GOAL_H;
            if(inside(x,sourceY,11,gy,203,PF_GOAL_H-4)) {
                String boss=L.goals().get(i).encounter().boss();
                if(BurrowModel.availableBosses().contains(boss)) {
                    activityMode=false;model.chooseBoss(boss); rareDropExpanded=true; styleFeedback=null;
                    BurrowModel.View old=model.view(); model.show(BurrowModel.View.OUTFITTER); notifyViewChange(old); notifySelection();
                }
                return true;
            }
        }
        for(int i=0;i<L.rows().size();i++) {
            int ry=L.listY()+26+i*PF_ROW_H;
            if(inside(x,sourceY,11,ry,203,PF_ROW_H-4)) {
                if(x>=14&&x<=38&&sourceY<=ry+27){togglePathfinderCheckbox(L.rows().get(i));return true;}
                pathfinderSelectedId=L.rows().get(i).id();
                pathfinderDetail=L.rows().get(i).why();
                return true;
            }
        }
        return false;
    }
    String pathfinderDetailTextForTest() { return String.join("\n",pathfinderLayout().detailLines()); }
    int pathfinderGoalButtonsYForTest() { return pathfinderLayout().buttonsY(); }
    int pathfinderListYForTest() { return pathfinderLayout().listY(); }
    String pathfinderSelectedIdForTest() { return pathfinderSelectedId; }
    private Optional<SuggestionEngine.Suggestion> selectedSuggestion() {
        return pathfinderSuggestions.stream().filter(x->x.id().equals(pathfinderSelectedId)).findFirst();
    }
    private String selectedQuestGoal;
    /** Only a verified host quest scan may set this; no guessed quest completion. */
    public void setQuestSurvey(IronLedgerQuestSurvey.Snapshot data) {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Quest discovery requires Swing EDT");
        questDiscovery=Objects.requireNonNull(data);
        repaint();
    }
    public IronLedgerQuestSurvey.Snapshot questSurvey() { return questDiscovery; }

    public QuestRecommendation observedQuest() { return observedQuest; }
    public void setObservedQuest(QuestRecommendation quest) {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Quest display requires Swing EDT");
        observedQuest = Objects.requireNonNull(quest, "quest");
        repaint();
    }
    public LiveCombat observedCombat() { return observedCombat; }
    public void setObservedCombat(LiveCombat snapshot) {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Live combat display requires Swing EDT");
        observedCombat = Objects.requireNonNull(snapshot, "snapshot");
        repaint();
    }
    public void setObservedWeapon(LiveWeapon observed) {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Equipment display requires Swing EDT");
        observedWeapon = Objects.requireNonNull(observed);
        repaint();
    }
    public LiveWeapon observedWeapon() { return observedWeapon; }

    // BUR-0031: UI-only intake contract. Spoons must authenticate the source,
    // logical login epoch and event provenance BEFORE dispatching these records.
    // A Java record alone is not proof that a game event occurred.
    public enum SessionOrigin { VERIFIED_BRIDGE, SYNTHETIC_FIXTURE }
    public enum SessionState { OFFLINE, ACTIVE, SUSPENDED }
    public enum SessionFeed { XP, BOSS_KC, DROPS, SUPPLIES }
    public enum FeedStatus { NOT_CONNECTED, AVAILABLE, UNAVAILABLE }
    public record SessionScope(String accountToken, String loginEpoch) {
        public SessionScope {
            if (accountToken == null || accountToken.isBlank() || loginEpoch == null || loginEpoch.isBlank())
                throw new IllegalArgumentException("authenticated scope required");
        }
    }
    public sealed interface SessionEvent permits SessionStart, SessionStop, SessionPause,
        SessionResume, SessionFeedStatus, SessionXpGain, SessionBossKcGain, SessionDrop, SessionSupplyUsed,
        SessionLootEncounter {
        SessionScope scope();
        String eventId();
        SessionOrigin origin();
    }
    public record SessionStart(SessionScope scope, String eventId, SessionOrigin origin) implements SessionEvent {}
    /** Only a confirmed final authenticated logout, NOT a world-hop/loading transition. */
    public record SessionStop(SessionScope scope, String eventId, SessionOrigin origin) implements SessionEvent {}
    public record SessionPause(SessionScope scope, String eventId, SessionOrigin origin) implements SessionEvent {}
    public record SessionResume(SessionScope scope, String eventId, SessionOrigin origin) implements SessionEvent {}
    public record SessionFeedStatus(SessionScope scope, String eventId, SessionOrigin origin,
                                    SessionFeed feed, FeedStatus status) implements SessionEvent {}
    /** Positive already-baselined same-login delta; never raw absolute skill XP. */
    public record SessionXpGain(SessionScope scope, String eventId, SessionOrigin origin,
                                String skill, long delta, String sourceEvidence) implements SessionEvent {}
    /** Positive already-baselined BUR-0025 delta; never a raw KC or fictional kill. */
    public record SessionBossKcGain(SessionScope scope, String eventId, SessionOrigin origin,
                                    String boss, long delta, String sourceEvidence) implements SessionEvent {}
    /** Boss may be null: unproven attribution MUST stay Unassigned. */
    /** Build 20.1: one loot-confirmed encounter (Loot Tracker estimate). NEVER kill count. */
    public record SessionLootEncounter(SessionScope scope, String eventId, SessionOrigin origin,
                                       String encounter, String sourceEvidence) implements SessionEvent {}
    public record SessionDrop(SessionScope scope, String eventId, SessionOrigin origin,
                              int itemId, String itemName, long amount, String boss,
                              String sourceEvidence) implements SessionEvent {}
    /** Verified consumption only; item-container withdrawals are NOT use evidence. */
    public record SessionSupplyUsed(SessionScope scope, String eventId, SessionOrigin origin,
                                    int itemId, String itemName, long amount, String sourceEvidence) implements SessionEvent {}
    public record SessionItem(int itemId, String itemName, String attribution, long quantity) {}
    public record SessionReadout(SessionState state, boolean synthetic, long totalXp,
                                 Map<String,Long> xpBySkill, Map<String,Long> kcByBoss,
                                 java.util.List<SessionItem> drops, java.util.List<SessionItem> supplies,
                                 Map<SessionFeed,FeedStatus> feeds) {}

    private SessionScope activeSessionScope;
    private SessionOrigin activeSessionOrigin;
    private SessionState activeSessionState=SessionState.OFFLINE;
    private final Set<SessionScope> retiredSessionScopes=new HashSet<>();
    private final Set<String> acceptedSessionEventIds=new HashSet<>();
    private final EnumMap<SessionFeed,FeedStatus> sessionFeeds=new EnumMap<>(SessionFeed.class);
    private final Map<String,Long> sessionXp=new LinkedHashMap<>();
    private final Map<String,Long> sessionKc=new LinkedHashMap<>();
    private final Map<String,Long> sessionLootEncounters=new LinkedHashMap<>();
    private final Map<String,SessionItem> sessionDrops=new LinkedHashMap<>();
    private final Map<String,SessionItem> sessionSupplies=new LinkedHashMap<>();
    private long sessionTotalXp;
    private Runnable xpStartAction = () -> {};
    /** Build 20: distinct XP tracker states; never shows a transient pause as "turned off". */
    public enum XpTrackingState { SIGNED_OUT, OFF, SYNCING, ACTIVE, PAUSED, UNAVAILABLE }
    private XpTrackingState xpTrackingState; // null = legacy host that never publishes intent
    public void setXpTrackingState(XpTrackingState state) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("XP state requires Swing EDT");
        xpTrackingState=state; repaint();
    }
    public XpTrackingState xpTrackingState() { return xpTrackingState; }
    /** Host-provided user action. Preview/test panels remain inert. */
    public void setXpStartAction(Runnable action) { xpStartAction=Objects.requireNonNull(action); }

    /** Immutable defensive projection; never persists or reads another login's activity. */
    public SessionReadout sessionReadout() {
        return new SessionReadout(activeSessionState,activeSessionOrigin==SessionOrigin.SYNTHETIC_FIXTURE,
            sessionTotalXp,Map.copyOf(sessionXp),Map.copyOf(sessionKc),
            java.util.List.copyOf(sessionDrops.values()),java.util.List.copyOf(sessionSupplies.values()),
            Map.copyOf(sessionFeeds));
    }
    /** Build 20.1: loot-confirmed encounters this login (estimate; separate from KC). */
    public Map<String,Long> lootConfirmedEncounters() { return Map.copyOf(sessionLootEncounters); }
    private void wipeSessionData() {
        sessionTotalXp=0;
        sessionXp.clear();sessionKc.clear();sessionDrops.clear();sessionSupplies.clear();
        sessionLootEncounters.clear();
        acceptedSessionEventIds.clear();sessionFeeds.clear();
        for(SessionFeed feed:SessionFeed.values())sessionFeeds.put(feed,FeedStatus.NOT_CONNECTED);
    }
    /**
     * BQA-IMPL-R05 (fixes BurrowKcHostSafetyRegressionTest.panelReplacementReplaysOnlyCurrentScopeOnce): apply a COMPLETE
     * current-login snapshot. A replay is a full state, not a delta: the same-scope panel is rebuilt from the snapshot,
     * so replaying twice into one panel (or into a panel that already holds live rows) can never double-count.
     * A retired scope is refused; a different active scope is retired first, exactly like {@link SessionStart}. EDT only.
     */
    public boolean restoreSessionSnapshot(java.util.List<SessionEvent> snapshot) {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Session UI intake requires Swing EDT");
        if (snapshot == null || snapshot.isEmpty() || !(snapshot.get(0) instanceof SessionStart start)) return false;
        if (retiredSessionScopes.contains(start.scope())) return false;
        if (activeSessionScope != null && !activeSessionScope.equals(start.scope())) retiredSessionScopes.add(activeSessionScope);
        activeSessionScope = null; // same scope: start over from the snapshot (SessionStart wipes the rows)
        boolean ok = true;
        for (SessionEvent e : snapshot) ok &= ingestSessionEvent(e);
        return ok;
    }
    /** EDT-only, source-validated typed input. This patch does NOT register any RuneLite event handler. */
    public boolean ingestSessionEvent(SessionEvent event) {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Session UI intake requires Swing EDT");
        Objects.requireNonNull(event,"event");
        Objects.requireNonNull(event.scope(),"scope");
        Objects.requireNonNull(event.origin(),"origin");
        if(event.eventId()==null || event.eventId().isBlank())throw new IllegalArgumentException("event identity required");
        if(event instanceof SessionStart) {
            if(retiredSessionScopes.contains(event.scope()))return false;
            if(activeSessionScope!=null && activeSessionScope.equals(event.scope()))return false;
            if(activeSessionScope!=null)retiredSessionScopes.add(activeSessionScope);
            wipeSessionData();
            activeSessionScope=event.scope();activeSessionOrigin=event.origin();
            activeSessionState=SessionState.ACTIVE;
            acceptedSessionEventIds.add(event.eventId());
            revalidate();repaint();return true;
        }
        // Wrong-account callbacks and late old-epoch Swing events are ignored.
        if(activeSessionScope==null || !activeSessionScope.equals(event.scope()) ||
            activeSessionOrigin!=event.origin() || acceptedSessionEventIds.contains(event.eventId()))return false;
        if(event instanceof SessionStop) {
            retiredSessionScopes.add(activeSessionScope);
            wipeSessionData();activeSessionScope=null;activeSessionOrigin=null;
            activeSessionState=SessionState.OFFLINE;
        } else if(event instanceof SessionPause) {
            if(activeSessionState!=SessionState.ACTIVE)return false;
            activeSessionState=SessionState.SUSPENDED;
        } else if(event instanceof SessionResume) {
            if(activeSessionState!=SessionState.SUSPENDED)return false;
            activeSessionState=SessionState.ACTIVE;
        } else {
            if(activeSessionState!=SessionState.ACTIVE)return false;
            if(event instanceof SessionFeedStatus e) {
                Objects.requireNonNull(e.feed(),"feed");Objects.requireNonNull(e.status(),"status");
                sessionFeeds.put(e.feed(),e.status());
            } else if(event instanceof SessionXpGain e) {
                checkSessionRow(e.skill(),e.delta(),e.sourceEvidence());
                sessionTotalXp=Math.addExact(sessionTotalXp,e.delta());
                sessionXp.merge(e.skill(),e.delta(),Math::addExact);
                sessionFeeds.put(SessionFeed.XP,FeedStatus.AVAILABLE);
            } else if(event instanceof SessionBossKcGain e) {
                checkSessionRow(e.boss(),e.delta(),e.sourceEvidence());
                sessionKc.merge(e.boss(),e.delta(),Math::addExact);
                sessionFeeds.put(SessionFeed.BOSS_KC,FeedStatus.AVAILABLE);
            } else if(event instanceof SessionLootEncounter e) {
                checkSessionRow(e.encounter(),1,e.sourceEvidence());
                sessionLootEncounters.merge(e.encounter(),1L,Math::addExact);
            } else if(event instanceof SessionDrop e) {
                checkSessionItem(e.itemId(),e.itemName(),e.amount(),e.sourceEvidence());
                String boss=e.boss()==null || e.boss().isBlank()?"Unassigned":e.boss();
                addSessionItem(sessionDrops,e.itemId(),e.itemName(),boss,e.amount());
                sessionFeeds.put(SessionFeed.DROPS,FeedStatus.AVAILABLE);
            } else if(event instanceof SessionSupplyUsed e) {
                checkSessionItem(e.itemId(),e.itemName(),e.amount(),e.sourceEvidence());
                addSessionItem(sessionSupplies,e.itemId(),e.itemName(),"Confirmed use",e.amount());
                sessionFeeds.put(SessionFeed.SUPPLIES,FeedStatus.AVAILABLE);
            } else throw new IllegalArgumentException("unsupported event");
        }
        acceptedSessionEventIds.add(event.eventId());
        revalidate();repaint();return true;
    }
    private static void checkSessionRow(String name,long delta,String proof) {
        if(name==null||name.isBlank()||delta<=0||proof==null||proof.isBlank())
            throw new IllegalArgumentException("positive sourced delta required");
    }
    private static void checkSessionItem(int id,String name,long qty,String proof) {
        if(id<=0)throw new IllegalArgumentException("actual item ID required");
        checkSessionRow(name,qty,proof);
    }
    private static void addSessionItem(Map<String,SessionItem> map,int id,String name,String attribution,long amount) {
        String key=id+"/"+attribution;
        SessionItem prior=map.get(key);
        long count=prior==null?amount:Math.addExact(prior.quantity(),amount);
        map.put(key,new SessionItem(id,name,attribution,count));
    }

    public BurrowPanel(BurrowModel m) { this(m, false, id -> null); }
    public BurrowPanel(BurrowModel m, IntFunction<BufferedImage> itemImage) { this(m, false, itemImage); }
    /** Used only by offline owner-review renderer; NEVER constructed by RuneLite plugin. */
    public BurrowPanel(BurrowModel m, boolean illustrativePreview) { this(m, illustrativePreview, id -> null); }
    private BurrowPanel(BurrowModel m, boolean illustrativePreview, IntFunction<BufferedImage> itemImage) {
        this.illustrativePreview=illustrativePreview;
        this.itemImage=Objects.requireNonNull(itemImage);
        this.model=Objects.requireNonNull(m); wipeSessionData(); setMinimumSize(new Dimension(WIDTH,880));
        setOpaque(true); setBackground(BG);
        setToolTipText(" ");
        addMouseListener(new MouseAdapter(){
            public void mouseReleased(MouseEvent e) {
                if(!SwingUtilities.isLeftMouseButton(e))return;
                int y=e.getY()-CONTENT_SHIFT;
                if(activityMode && !isAccountSetupRequired() && !settingsOpen && isShowing() &&
                   model.view()==BurrowModel.View.OUTFITTER && inside(e.getX(),y,13,
                        spaciousOutfitter()?OUT_SELECT_Y:202,199,spaciousOutfitter()?OUT_SELECT_H:33)) {
                    int menuY=spaciousOutfitter()?OUT_SELECT_Y+OUT_SELECT_H:237;
                    activityMenu().show(BurrowPanel.this,13,CONTENT_SHIFT+menuY);
                } else if(chronicleOpen && chronicleBrowse && !isAccountSetupRequired() && !settingsOpen && isShowing() &&
                   model.view()==BurrowModel.View.OUTFITTER && inside(e.getX(),y,13,168,199,31)) {
                    bossMenu(true).show(BurrowPanel.this,13,CONTENT_SHIFT+199);
                } else if(chronicleOpen && !chronicleBrowse && !isAccountSetupRequired() && !settingsOpen && isShowing() &&
                   model.view()==BurrowModel.View.OUTFITTER && inside(e.getX(),y,96,130,91,30)) {
                    bossMenu(true).show(BurrowPanel.this,73,CONTENT_SHIFT+160);
                } else if(!chronicleOpen && !activityMode && !isAccountSetupRequired() && !settingsOpen && isShowing() && model.view()==BurrowModel.View.OUTFITTER && inside(e.getX(),y,13,outfitterSelectY(),spaciousOutfitter()?199:132,spaciousOutfitter()?OUT_SELECT_H:33)) {
                    bossMenu().show(BurrowPanel.this,13,CONTENT_SHIFT+outfitterSelectY()+(spaciousOutfitter()?OUT_SELECT_H:33));
                } else if (!chronicleOpen && !activityMode && !isAccountSetupRequired() && !settingsOpen && model.view()==BurrowModel.View.OUTFITTER &&
                           hasSpecialMethods() && inside(e.getX(),y,13,outfitterMethodY(),199,spaciousOutfitter()?38:29)) {
                    // BUR-0038: a real strategy menu, never hidden click-to-cycle.
                    if(isShowing()) methodMenu().show(BurrowPanel.this,13,CONTENT_SHIFT+outfitterMethodY()+(spaciousOutfitter()?38:29));
                } else if (!isAccountSetupRequired() && !settingsOpen && isShowing() &&
                           model.view()==BurrowModel.View.PATHFINDER && flowPathfinderEnabled() &&
                           pathfinderFlowActions.getOrDefault("focus",new Rectangle()).contains(e.getX(),y)) {
                    pathfinderFocusMenu().show(BurrowPanel.this,13,e.getY()+5);
                } else if (!isAccountSetupRequired() && !settingsOpen && isShowing() &&
                           model.view()==BurrowModel.View.PATHFINDER && flowPathfinderEnabled() &&
                           pathfinderFlowActions.getOrDefault("minutes",new Rectangle()).contains(e.getX(),y)) {
                    pathfinderMinutesMenu().show(BurrowPanel.this,13,e.getY()+5);
                } else if (!flowPathfinderEnabled() && !isAccountSetupRequired() && !settingsOpen && isShowing() && model.view()==BurrowModel.View.PATHFINDER &&
                           inside(e.getX(),y,13,431,110,22)) {
                    pathfinderFocusMenu().show(BurrowPanel.this,13,CONTENT_SHIFT+454);
                } else if (!flowPathfinderEnabled() && !isAccountSetupRequired() && !settingsOpen && isShowing() && model.view()==BurrowModel.View.PATHFINDER &&
                           inside(e.getX(),y,126,431,85,22)) {
                    pathfinderMinutesMenu().show(BurrowPanel.this,126,CONTENT_SHIFT+454);
                } else clickAt(e.getX(),e.getY());
            }
            public void mouseEntered(MouseEvent e) { updateItemHover(e); }
            public void mouseExited(MouseEvent e) {
                hoveredNav=-1; clearItemHover(); repaint();
            }
        });
        // Wheel registration belongs at the actual RuneLite scroll host, not
        // this custom panel alone. See SidebarWheelRouter.install(...).
        addMouseMotionListener(new MouseMotionAdapter(){
            public void mouseDragged(MouseEvent e) { clearItemHover(); repaint(); }
            public void mouseMoved(MouseEvent e) {
                int next=navAt(e.getX(),e.getY());
                if(next!=hoveredNav) {hoveredNav=next;repaint();}
                updateItemHover(e);
            }
        });
    }
    public void onViewportWheelScrolled() {clearItemHover();repaint();}

    /** Tier references: OSRS Wiki supplemental Guide:Bossing Ladder, checked Nov 1 2026.
     * Unlisted encounters are UNRANKED. These are broad learning tiers, not a player's readiness.
     * No guesses from combat levels or account state. */
    private static final Map<String,Integer> BOSS_DIFFICULTY = bossDifficulty();
    private static Map<String,Integer> bossDifficulty() {
        Map<String,Integer> tiers=new HashMap<>();
        addRank(tiers,1,"Brutus","Barrows","Obor","Bryophyta","Deranged archaeologist","Scurrius");
        addRank(tiers,2,"Shellbane gryphon","Amoxliatl","Hueycoatl","Giant Mole","Kraken","Sarachnis",
            "King Black Dragon","Thermonuclear smoke devil","The Mimic","Royal Titans","Mad Angel",
            "Chaos Fanatic","Crazy archaeologist","Blood Moon","Blue Moon","Eclipse Moon");
        addRank(tiers,3,"Scorpia","Chaos Elemental","Vet'ion","Calvar'ion","Venenatis","Spindel","Callisto","Artio",
            "Dagannoth Kings","Grotesque Guardians","Skotizo","Demonic gorillas","Tormented Demons",
            "Abyssal Sire","Cerberus","Araxxor","Alchemical Hydra","Corporeal Beast","Kalphite Queen",
            "Commander Zilyana","K'ril Tsutsaroth");
        addRank(tiers,4,"Zulrah","Vorkath","Phantom Muspah","Duke Sucellus","Maggot King","Kree'arra",
            "General Graardor","The Whisperer","The Leviathan","Vardorvis","Nex","Yama");
        addRank(tiers,5,"The Nightmare","Phosani's Nightmare","Doom of Mokhaiotl");
        return Collections.unmodifiableMap(tiers);
    }
    private static void addRank(Map<String,Integer> m,int t,String... names){for(String n:names)m.put(n,t);}
    private static String difficultyLabel(String boss){
        Integer level=BOSS_DIFFICULTY.get(boss);
        return level==null?"Unranked":switch(level){case 1->"Easy";case 2->"Medium";case 3->"Hard";case 4->"Elite";default->"Master";};
    }
    private static final String[] BOSS_SORTS={"Name A-Z","Name Z-A","Difficulty easy → hard","Difficulty hard → easy"};
    private int bossSortIndex; // shared presentation preference, separate selected boss state
    /** Compact native Swing picker; exactly five rows visible, scrolls and filters without mutating other surface. */
    JPopupMenu bossMenu(){return bossMenu(false);}
    JPopupMenu bossMenu(boolean forChronicle) {
        JPopupMenu popup=new JPopupMenu();
        popup.getAccessibleContext().setAccessibleName(forChronicle?"Chronicle boss picker":"Outfitter boss picker");
        JPanel panel=new JPanel(new BorderLayout(0,4));
        panel.setBackground(new Color(25,29,30));
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        JTextField search=new JTextField();
        search.setToolTipText("Search bosses by name");
        search.getAccessibleContext().setAccessibleName("Search bosses");
        search.setFont(uiFont(11,false));
        JPanel top=new JPanel(new BorderLayout(0,3));top.setOpaque(false);
        top.add(search,BorderLayout.NORTH);
        JComboBox<String> sort=new JComboBox<>(BOSS_SORTS);
        sort.setFont(uiFont(10,false));
        sort.setSelectedIndex(bossSortIndex);
        sort.setToolTipText("Difficulty is approximate: OSRS Wiki community Bossing Ladder; unranked if absent");
        JComboBox<String> group=new JComboBox<>(forChronicle
            ?new String[]{"All categories","Regular bosses","Wilderness","Moons of Peril","Demi-bosses","Raid encounters"}
            :new String[]{"All categories","Regular bosses","Wilderness","Moons of Peril","Demi-bosses"});
        group.setFont(uiFont(10,false));
        JPanel choices=new JPanel(new GridLayout(2,1,0,2));choices.setOpaque(false);
        choices.add(group);choices.add(sort);
        top.add(choices,BorderLayout.SOUTH);panel.add(top,BorderLayout.NORTH);
        DefaultListModel<String> listModel=new DefaultListModel<>();
        JList<String> list=new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int rowHeight=Math.max(28,uiFont(11,false).getSize()+17);
        list.setFixedCellHeight(rowHeight);
        list.setVisibleRowCount(5);
        list.setFont(uiFont(11,false));
        list.setBackground(new Color(27,32,34));
        list.setForeground(new Color(233,235,229));
        list.setSelectionBackground(new Color(85,67,38));
        list.setCellRenderer((j,v,index,selected,focus)->{
            boolean raid=forChronicle && gg.feral.burrow.core.RaidChronicle.isRaid(v);
            JLabel line=new JLabel(" "+v+"  ·  "+(raid?"Raid · Outfitter n/a":difficultyLabel(v)));
            line.setOpaque(true);
            line.setFont(uiFont(11,false));
            line.setBackground(selected?new Color(93,68,40):new Color(27,32,34));
            line.setForeground(selected?Color.WHITE:new Color(231,232,228));
            line.setToolTipText(raid?v+" · "+gg.feral.burrow.core.RaidChronicle.find(v).map(x->x.raid()).orElse("Raid")+" · Chronicle only; Outfitter not supported"
                :v+" · "+difficultyLabel(v)+" (approx. Wiki guide; not account readiness)");
            return line;
        });
        Runnable filter=()->{
            String query=search.getText().trim().toLowerCase(Locale.ROOT);
            String selectedBoss=forChronicle?chronicleSelection():model.boss();
            java.util.List<String> values=new ArrayList<>(BurrowModel.availableBosses());
            int raidGroup=5; // CP-5: Chronicle-only "Raid encounters" category; Outfitter never lists raids
            if(forChronicle)values.addAll(gg.feral.burrow.core.RaidChronicle.names());
            values.removeIf(b->!b.toLowerCase(Locale.ROOT).contains(query) ||
                (group.getSelectedIndex()==raidGroup && !gg.feral.burrow.core.RaidChronicle.isRaid(b)) ||
                (group.getSelectedIndex()>0 && group.getSelectedIndex()<raidGroup && BossRegistry.find(b).map(e->e.group().ordinal()+1!=group.getSelectedIndex()).orElse(true)));
            Comparator<String> name=String.CASE_INSENSITIVE_ORDER;
            switch(sort.getSelectedIndex()) {
                case 1 -> values.sort(name.reversed());
                case 2,3 -> {
                    boolean reverse=sort.getSelectedIndex()==3;
                    // Unknown is always last, including for descending sort.
                    values.sort(Comparator.comparingInt((String b)->BOSS_DIFFICULTY.get(b)==null?1:0)
                        .thenComparingInt(b->BOSS_DIFFICULTY.containsKey(b)?
                            (reverse?-BOSS_DIFFICULTY.get(b):BOSS_DIFFICULTY.get(b)):0)
                        .thenComparing(name));
                }
                default -> values.sort(name);
            }
            listModel.clear(); for(String b:values)listModel.addElement(b);
            int chosen=values.indexOf(selectedBoss);
            if(chosen>=0)list.setSelectedIndex(chosen);
        };
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){filter.run();}
            public void removeUpdate(javax.swing.event.DocumentEvent e){filter.run();}
            public void changedUpdate(javax.swing.event.DocumentEvent e){filter.run();}
        });
        sort.addActionListener(e->{bossSortIndex=sort.getSelectedIndex();filter.run();});
        group.addActionListener(e->filter.run());
        filter.run();
        list.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){
            if(e.getClickCount()>=1&&SwingUtilities.isLeftMouseButton(e)) {
                int index=list.locationToIndex(e.getPoint());
                if(index>=0&&list.getCellBounds(index,index).contains(e.getPoint())) {
                    selectBossFromPicker(listModel.getElementAt(index),forChronicle);popup.setVisible(false);
                }
            }
        }});
        list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"),"pick");
        list.getActionMap().put("pick",new AbstractAction(){public void actionPerformed(ActionEvent e){
            String value=list.getSelectedValue();if(value!=null){selectBossFromPicker(value,forChronicle);popup.setVisible(false);}
        }});
        JScrollPane scroll=new JScrollPane(list,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(204,5*rowHeight));
        scroll.getVerticalScrollBar().setUnitIncrement(rowHeight);
        // A filtered one-row result should not leave four phantom rows of empty popup.
        Runnable resizePicker=()->{
            int visible=Math.max(1,Math.min(5,listModel.size()));
            scroll.setPreferredSize(new Dimension(204,visible*rowHeight));
            scroll.revalidate();panel.revalidate();
            if(popup.isVisible())popup.setPopupSize(panel.getPreferredSize());
        };
        listModel.addListDataListener(new javax.swing.event.ListDataListener() {
            public void intervalAdded(javax.swing.event.ListDataEvent e){resizePicker.run();}
            public void intervalRemoved(javax.swing.event.ListDataEvent e){resizePicker.run();}
            public void contentsChanged(javax.swing.event.ListDataEvent e){resizePicker.run();}
        });
        resizePicker.run();
        panel.add(scroll,BorderLayout.CENTER);
        JLabel hint=new JLabel("5 rows · wheel · difficulty ≈ community guide");
        hint.setForeground(new Color(158,166,164));hint.setFont(uiFont(9,false));
        panel.add(hint,BorderLayout.SOUTH);
        popup.add(panel);
        return popup;
    }
    // ---- CP-5: Chronicle may show a raid encounter instead of a standalone boss (independent of Outfitter) ----
    private String chronicleRaid; // null = Chronicle shows chronicleModel's boss
    private String chronicleSelection(){return chronicleRaid!=null?chronicleRaid:chronicleModel.boss();}
    String chronicleRaidForTest(){return chronicleRaid;}
    void selectChronicleForTest(String name){selectBossFromPicker(name,true);}
    private void selectBossFromPicker(String boss,boolean forChronicle) {
        if(forChronicle && gg.feral.burrow.core.RaidChronicle.isRaid(boss)){chronicleRaid=boss;prepExpanded=false;chronicleBrowse=false;}
        else if(forChronicle){chronicleRaid=null;chronicleModel.chooseBoss(boss);prepExpanded=false;chronicleBrowse=false;}
        else {model.chooseBoss(boss);styleFeedback=null;bankDetailsExpanded=false;notifySelection();}
        clearItemHover();revalidate();repaint();
    }
    /** Convenient one-step browse with no effect on the Outfitter's saved selection. */
    private void cycleChronicleBoss(int delta) {
        java.util.List<String> bosses=new ArrayList<>(BurrowModel.availableBosses());
        bosses.addAll(gg.feral.burrow.core.RaidChronicle.names());
        bosses.sort(String.CASE_INSENSITIVE_ORDER);
        int index=bosses.indexOf(chronicleSelection());
        selectBossFromPicker(bosses.get(Math.floorMod(index+delta,bosses.size())),true);
    }
    /** BUR-0038: complete, source-backed method names in a native Swing dropdown.
     *  Package-private so focused UI tests can activate the same actual menu actions
     *  without displaying a window or manufacturing a new game method. */
    JPopupMenu methodMenu() {
        JPopupMenu menu=new JPopupMenu();
        menu.getAccessibleContext().setAccessibleName("Choose boss method");
        ButtonGroup group=new ButtonGroup();
        for (String method : displayBossModel().methods()) {
            JRadioButtonMenuItem choice=new JRadioButtonMenuItem(method, method.equals(displayBossModel().method()));
            choice.setToolTipText(method + (StyleMethodPicker.isProvisional(displayBossModel().boss(),method,Tier.RECOMMENDED)
                ? " · research example, Best in Bank disabled" : ""));
            group.add(choice);
            choice.addActionListener(event -> {
                model.chooseMethod(method);
                notifySelection();
                styleFeedback=null;
                clearItemHover();
                revalidate();
                repaint();
            });
            menu.add(choice);
        }
        return menu;
    }
    public BurrowModel model() { return model; }

    // The outer RuneLite JScrollPane owns the vertical scroll. Make the entire
    // preparation card as tall as its content instead of truncating the notes.
    @Override public Dimension getPreferredSize() {
        if(accountModeManaged && (isAccountSetupRequired() || settingsOpen)) return new Dimension(WIDTH,880);
        if(model!=null && model.view()==BurrowModel.View.PATHFINDER) {
            if(flowPathfinderEnabled()) {
                var img=new java.awt.image.BufferedImage(1,1,java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D g=img.createGraphics();
                try {return new Dimension(WIDTH,Math.max(880,CONTENT_SHIFT+flowPathfinder(g)+24));}
                finally {g.dispose();}
            }
            return new Dimension(WIDTH,Math.max(880,CONTENT_SHIFT+pathfinderLayout().bottom()+24));
        }
        if(model!=null && model.view()==BurrowModel.View.HOME) {
            if(flowHomeEnabled()) {
                java.awt.image.BufferedImage probe=new java.awt.image.BufferedImage(1,1,java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D g=probe.createGraphics();
                try {return new Dimension(WIDTH, Math.max(880,flowHome(g)+24));}
                finally {g.dispose();}
            }
            return new Dimension(WIDTH, Math.max(880,CONTENT_SHIFT+homeBottom()+crewShiftForTest()+24));
        }
        if(model!=null && model.view()==BurrowModel.View.SESSIONS) {
            if(flowSessionEnabled()) {
                var img=new java.awt.image.BufferedImage(1,1,java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D g=img.createGraphics();
                try {return new Dimension(WIDTH,Math.max(880,CONTENT_SHIFT+flowSessions(g)+24));}
                finally {g.dispose();}
            }
            return new Dimension(WIDTH, Math.max(880,CONTENT_SHIFT+sessionContentBottom()+24));
        }
        if (model == null || model.view() != BurrowModel.View.OUTFITTER)
            return new Dimension(WIDTH, 880);
        if(chronicleOpen&&chronicleBrowse){
            BufferedImage probe=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=probe.createGraphics();
            try{return new Dimension(WIDTH,Math.max(880,CONTENT_SHIFT+paintChronicleBrowser(g,false)+24));}
            finally{g.dispose();}
        }
        if(activityMode){
            BufferedImage probe=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=probe.createGraphics();
            try{return new Dimension(WIDTH,Math.max(880,CONTENT_SHIFT+paintActivityOutfitter(g,false)+24));}
            finally{g.dispose();}
        }
        return new Dimension(WIDTH,Math.max(880,CONTENT_SHIFT+outfitterBottom()+18));
    }

    /** B20.1: preparation notes open as a short preview; the full lossless text is one tap away. */
    // R13 UI child. No changes to the underlying account, event, bank or drop models.
    private volatile boolean chronicleOpen;
    private boolean chronicleBrowse; // independent Browse vs Detail, never changes Outfitter selection
    private int chronicleBrowseSort; // Featured, A-Z, Z-A, difficulty ascending, descending
    private int chronicleBrowseGroup; // all, regular, wilderness, moons, demi, raid
    private final java.util.Map<String,Rectangle> chronicleBrowseActions=new LinkedHashMap<>();
    public boolean chronicleBrowserForTest(){return chronicleOpen&&chronicleBrowse;}
    public void showChronicleBrowserForTest(){openChronicle(BurrowModel.View.OUTFITTER);chronicleBrowse=true;revalidate();repaint();}
    public void setChronicleBrowseSortForTest(int index){if(index<0||index>4)throw new IllegalArgumentException();chronicleBrowseSort=index;revalidate();repaint();}
    public java.util.List<String> chronicleBrowseBossesForTest(){return chronicleBrowseBosses();}
    Rectangle chronicleBrowseCardForTest(String boss){return chronicleBrowseActions.get(boss);}
    Rectangle homeFlowActionForTest(String action){return homeFlowActions.get(action);}
    public void chooseChronicleCardForTest(String boss){selectBossFromPicker(boss,true);chronicleBrowse=false;revalidate();repaint();}
    // ACT-R01: independent activity references. Never mutate saved boss or Best-in-Bank planner.
    private volatile boolean activityMode;
    /** Changes when category or Activity choice changes, even A→B→A between game ticks. */
    private volatile long outfitterSelectionEpoch;
    public long outfitterSelectionEpochForHost() { return outfitterSelectionEpoch; }
    private volatile String activityKitId="herb";
    /** Shared host contract. Category is browsing state, not a second bank/gear engine. */
    public SharedOutfitterSetup selectedOutfitterSetupForHost() {
        return activityMode ? SharedOutfitterSetup.activity(activityKitId)
                            : SharedOutfitterSetup.boss(model.bankSelection());
    }

    public boolean activityModeForTest(){return activityMode;}
    public String activityKitIdForTest(){return activityKitId;}
    public void selectActivityForTest(String id){
        if (ActivityOutfitterCatalog.all().stream().noneMatch(k->k.id().equals(id)))
            throw new IllegalArgumentException("Unknown activity kit: "+id);
        activityKitId=id;activityMode=true;chronicleOpen=false;outfitterSelectionEpoch++;revalidate();repaint();
    }
    private void switchActivityMode(boolean value){
        if(activityMode!=value)outfitterSelectionEpoch++;
        activityMode=value;clearItemHover();revalidate();repaint();
        var viewport=(JViewport)SwingUtilities.getAncestorOfClass(JViewport.class,this);
        if(viewport!=null)SwingUtilities.invokeLater(()->viewport.setViewPosition(new Point(0,0)));
    }
    /** Native selection uses a compact searchable six-row list; no 17-row popup overflow. */
    JPopupMenu activityMenu(){
        JPopupMenu popup=new JPopupMenu();
        popup.getAccessibleContext().setAccessibleName("Activity Outfitter kit picker");
        JPanel panel=new JPanel(new BorderLayout(0,4));
        panel.setBackground(new Color(25,29,30));
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        JTextField search=new JTextField();
        search.setToolTipText("Search activity loadouts, clue tiers or Slayer styles");
        search.getAccessibleContext().setAccessibleName("Search activity kits");
        search.setFont(uiFont(10,false));
        panel.add(search,BorderLayout.NORTH);
        DefaultListModel<ActivityOutfitterCatalog.Kit> modelList=new DefaultListModel<>();
        JList<ActivityOutfitterCatalog.Kit> choices=new JList<>(modelList);
        choices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        choices.setVisibleRowCount(6);
        choices.setFixedCellHeight(Math.max(29,uiFont(11,false).getSize()+17));
        choices.setBackground(new Color(27,32,34));
        choices.setForeground(TEXT);
        choices.setSelectionBackground(new Color(90,68,37));
        choices.setCellRenderer((list,value,index,selected,focused)->{
            JLabel l=new JLabel(" "+value.category()+" · "+value.title());
            l.setOpaque(true);l.setFont(uiFont(10,false));
            l.setBackground(selected?new Color(90,68,37):new Color(27,32,34));
            l.setForeground(TEXT);l.setToolTipText(value.description());
            return l;
        });
        Runnable filter=()->{
            String term=search.getText().trim().toLowerCase(Locale.ROOT);
            modelList.clear();
            for(var kit:ActivityOutfitterCatalog.all())
                if(kit.title().toLowerCase(Locale.ROOT).contains(term)||
                   kit.category().toLowerCase(Locale.ROOT).contains(term)||
                   kit.id().toLowerCase(Locale.ROOT).contains(term)) modelList.addElement(kit);
            for(int n=0;n<modelList.size();n++)
                if(modelList.get(n).id().equals(activityKitId)){choices.setSelectedIndex(n);break;}
        };
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){filter.run();}
            public void removeUpdate(javax.swing.event.DocumentEvent e){filter.run();}
            public void changedUpdate(javax.swing.event.DocumentEvent e){filter.run();}
        });
        filter.run();
        Runnable choose=()->{
            var kit=choices.getSelectedValue();if(kit==null)return;
            activityKitId=kit.id();outfitterSelectionEpoch++;clearItemHover();revalidate();repaint();popup.setVisible(false);
        };
        choices.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){if(e.getClickCount()>=1)choose.run();}});
        choices.addKeyListener(new KeyAdapter(){public void keyPressed(KeyEvent e){if(e.getKeyCode()==KeyEvent.VK_ENTER)choose.run();}});
        search.addActionListener(e->{if(modelList.size()>0){choices.setSelectedIndex(0);choose.run();}});
        JScrollPane scroll=new JScrollPane(choices);
        scroll.setPreferredSize(new Dimension(199,6*choices.getFixedCellHeight()+4));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scroll,BorderLayout.CENTER);
        popup.add(panel);
        popup.setPreferredSize(new Dimension(210,6*choices.getFixedCellHeight()+43));
        return popup;
    }
    /** Completely separate selection for Chronicle research. Does not own bank/session state. */
    private final BurrowModel chronicleModel = new BurrowModel();
    private boolean chronicleSelectionInitialized;
    private BurrowModel displayBossModel(){return chronicleOpen ? chronicleModel : model;}
    public String chronicleBossForUiTest(){return chronicleModel.boss();}
    private int outfitterReturnScroll;
    private BurrowModel.View chronicleReturnView=BurrowModel.View.OUTFITTER;
    private int expandedHistoryIndex=-1;
    private static final int BOSS_ROW_Y=133;
    private static final int CHRONICLE_X=149;
    // OUT-R06: shared two-category Outfitter shell. Both categories retain their distinct selection
    // but share the same menu location and scrollable equipment/inventory column.
    private static final int OUT_TAB_Y=130, OUT_SELECT_Y=193, OUT_SELECT_H=39;
    private boolean spaciousOutfitter(){return uiScale==UiScale.LARGE||uiScale==UiScale.EXTRA_LARGE;}
    private int outfitterSelectY(){return spaciousOutfitter()?OUT_SELECT_Y:BOSS_ROW_Y;}
    private int outfitterMethodY(){return spaciousOutfitter()?284:BOSS_ROW_Y+39;}
    private int outfitterLogY(){return 243;}
    private static final int OUT_SOURCE_HEIGHT=33;
    /** Shared tab rows have one click region per category, never hidden actions on adjacent controls. */
    private void outfitterTabs(Graphics2D g,boolean activities){
        for(int i=0;i<2;i++){
            int x=13+i*101;boolean active=(i==1)==activities;
            g.setColor(active?new Color(83,60,35):PANEL);
            g.fillRoundRect(x,OUT_TAB_Y,98,34,5,5);
            g.setColor(active?GOLD:LINE);g.drawRoundRect(x,OUT_TAB_Y,98,34,5,5);
            String title=i==0?"BOSSES":"ACTIVITIES";
            Font f=uiFont(10,active);g.setFont(f);FontMetrics fm=g.getFontMetrics();
            g.setColor(active?GOLD:TEXT);g.drawString(title,x+(98-fm.stringWidth(title))/2,OUT_TAB_Y+22);
        }
    }
    private void outfitterSharedSelect(Graphics2D g,boolean activities,String title){
        outfitterTabs(g,activities);
        label(g,activities?"SELECT ACTIVITY":"SELECT BOSS",17,184,9,GOLD,true);
        g.setColor(PANEL);g.fillRoundRect(13,OUT_SELECT_Y,199,OUT_SELECT_H,5,5);
        g.setColor(LINE);g.drawRoundRect(13,OUT_SELECT_Y,199,OUT_SELECT_H,5,5);
        g.setFont(uiFont(10,true));FontMetrics fm=g.getFontMetrics();
        // Show all available room; the native picker/hover remains the source for the full name.
        String t=title;while(t.length()>2&&fm.stringWidth(t)>165)t=t.substring(0,t.length()-1);
        if(!t.equals(title))t=t.stripTrailing()+"…";
        g.setColor(TEXT);g.drawString(t,19,OUT_SELECT_Y+25);
        label(g,"▾",192,OUT_SELECT_Y+25,12,GOLD,true);
    }
    private void outFullButton(Graphics2D g,int y,int h,String title,boolean active,boolean disabled){
        g.setColor(active?new Color(83,60,35):PANEL);g.fillRoundRect(13,y,199,h,5,5);
        g.setColor(active?GOLD:disabled?new Color(75,66,55):LINE);
        g.drawRoundRect(13,y,199,h,5,5);
        Font f=uiFont(10,active);g.setFont(f);FontMetrics fm=g.getFontMetrics();
        g.setColor(disabled?MUTED:active?GOLD:TEXT);
        java.util.List<String> lines=wrapItemName(title,fm,184);
        if(lines.size()==1)g.drawString(title,20,y+(h+fm.getAscent()-fm.getDescent())/2);
        else {
            int stride=Math.max(13,fm.getHeight()-1);
            int start=y+Math.max(fm.getAscent()+1,(h-(lines.size()-1)*stride+fm.getAscent()-fm.getDescent())/2);
            for(int i=0;i<Math.min(2,lines.size());i++)g.drawString(lines.get(i),20,start+i*stride);
        }
    }
    private static boolean outTabClick(int x,int y){return inside(x,y,13,OUT_TAB_Y,199,34);}
    public boolean spaciousOutfitterForTest(){return spaciousOutfitter();}

    boolean hasSpecialMethodsForR13Test(){return hasSpecialMethods();}
    boolean isChronicleOpenForR13Test(){return chronicleOpen;}
    public int equipmentYForR13Test(){return equipmentY();}
    public int tierYForR13Test(){return tierRowY();}
    public int sourceYForR13Test(){return gearSourceY();}
    public int chronicleNotesYForR13Test(){return chronicleNotesY();}
    int historyTopForR13Test(){return historyTop();}
    int historyRowHeightForR13Test(int i){return historyRowHeight(i);}
    private boolean hasSpecialMethods() {
        if(displayBossModel().methods().size()<2)return false;
        // Show only when a style has alternate strategies or a genuine hybrid/special method exists.
        var seen=new HashSet<String>();
        for(String method:displayBossModel().methods()) {
            String style=TieredOutfitterCatalog.style(displayBossModel().boss(),method);
            if(!Set.of("Melee","Ranged","Magic").contains(style)||!seen.add(style))return true;
        }
        return false;
    }
    private int tierRowY(){return spaciousOutfitter()?(hasSpecialMethods()?340:296):(hasSpecialMethods()?223:183);}
    private int gearSourceY(){return spaciousOutfitter()?tierRowY()+3*39+29:tierRowY()+38;}
    private int equipmentY(){return gearSourceY()+(spaciousOutfitter()?83:35)+(showsWildernessTravelWarning()?KBD_WARNING_SHIFT:0)
        +bankStatusHeight();}
    private int inventoryY(){return equipmentY()+281;}
    private int rareSectionY(){return inventoryY()+344;}
    private int rareDropTrackerY(){return chronicleOpen?205:rareSectionY()+93;}
    private int outfitterBottom(){
        if(chronicleOpen) return chronicleBottom();
        int bottom=rareSectionY()+83+(rareDropExpanded?rareDropTrackerHeight()+98:0);
        return displayBossModel().profile()==null?Math.max(bottom,equipmentY()+110):bottom;
    }
    private int combatTasksY(){return rareDropTrackerY()+rareDropTrackerHeight()+16;}
    private int chronicleNotesY(){int h=combatTasksHeight();return combatTasksY()+(h>0?h+10:0);}
    private int chronicleBottom() {
        if(chronicleRaid!=null) return 205+raidCardHeight()+20;
        return chronicleNotesY()+
        (displayBossModel().profile()!=null?preparationLayout().height()+132:
            ResearchCombatMethods.find(displayBossModel().boss(),displayBossModel().method())!=null?367:
            displayBossModel().bossEntry()!=null?107+guideNotVerifiedLines(displayBossModel().bossEntry()).size()*14:190);}
    private int equipmentOffset(){return equipmentY()-397;}
    private int visibleGearIndex(int x,int y){return gearIndex(x,y-(equipmentY()-376));}
    private int visibleInventoryIndex(int x,int y){return inventoryIndex(x,y-(equipmentY()-386));}
    private int combatButtonAt(int x,int y) {
        for(int i=0;i<3;i++)if(inside(x,y,87+i*42,equipmentY()+6,40,30))return i;
        return -1;
    }
    private int rareChipY(){return rareSectionY();}
    private boolean inRareChip(int x,int y){return inside(x,y,13,rareChipY(),199,42);}
    private void changeChronicle(boolean open) {
        if(chronicleOpen==open)return;
        var viewport=(JViewport)SwingUtilities.getAncestorOfClass(JViewport.class,this);
        if(open)outfitterReturnScroll=viewport==null?0:viewport.getViewPosition().y;
        chronicleOpen=open;clearItemHover();revalidate();repaint();
        if(viewport!=null) SwingUtilities.invokeLater(()->{
            viewport.setViewPosition(new Point(0,open?0:Math.max(0,outfitterReturnScroll)));
        });
    }
    void openChronicleForTest(){openChronicle(BurrowModel.View.OUTFITTER);}
    private void openChronicle(BurrowModel.View origin) {
        if(chronicleOpen)return;
        activityMode=false;
        chronicleReturnView=origin;
        if(!chronicleSelectionInitialized) {
            chronicleModel.chooseBoss(model.boss());
            chronicleSelectionInitialized=true;
        }
        BurrowModel.View old=model.view();
        if(old!=BurrowModel.View.OUTFITTER)model.show(BurrowModel.View.OUTFITTER);
        chronicleBrowse=origin==BurrowModel.View.HOME;
        changeChronicle(true);
        notifyViewChange(old);
    }
    private void closeChronicle() {
        if(!chronicleOpen)return;
        chronicleBrowse=false;
        changeChronicle(false);
        if(chronicleReturnView!=BurrowModel.View.OUTFITTER) {
            BurrowModel.View old=model.view();
            model.show(chronicleReturnView);
            notifyViewChange(old);
        }
        chronicleReturnView=BurrowModel.View.OUTFITTER;
        revalidate();repaint();
    }
        private boolean prepExpanded;
    static final int PREP_PREVIEW_LINES=6, PREP_TOGGLE_H=22;
    void setPreparationExpandedForTest(boolean e){prepExpanded=e;revalidate();}
    boolean preparationExpanded(){return prepExpanded;}
    private boolean prepCollapsible(){return preparationLayoutFull().lines().size()>PREP_PREVIEW_LINES+2;}
    private PreparationNotesLayout.Layout preparationLayout() {
        var full=preparationLayoutFull();
        if(displayBossModel().profile()==null || !prepCollapsible()) return full;
        if(prepExpanded) return new PreparationNotesLayout.Layout(full.lines(),full.height()+PREP_TOGGLE_H);
        var preview=full.lines().subList(0,PREP_PREVIEW_LINES);
        return new PreparationNotesLayout.Layout(preview,41+PREP_PREVIEW_LINES*PreparationNotesLayout.LINE_HEIGHT+PREP_TOGGLE_H+52);
    }
    private int prepToggleY(){return PREPARATION_Y+41+preparationLayout().lines().size()*PreparationNotesLayout.LINE_HEIGHT-10;}
    private PreparationNotesLayout.Layout preparationLayoutFull() {
        VerifiedOutfitterCatalog.Profile profile=displayBossModel().profile();
        if (profile==null) return PreparationNotesLayout.empty();
        var metadata=Final06ProfileMetadata.find(displayBossModel().boss(),displayBossModel().method(),displayBossModel().tier());
        return PreparationNotesLayout.layout(profile.notes(),
            metadata==null?Collections.emptyList():SwitchNoteFormatter.formatSwitches(metadata.switches(), profile),
            getFontMetrics(PREPARATION_FONT), 183);
    }

    private void clearItemHover() { hoveredItemText=null; }

    private String itemHoverAt(int x, int y) {
        if(activityMode || model.view()!=BurrowModel.View.OUTFITTER || displayBossModel().profile()==null) return null;
        if(chronicleOpen)return null;
        int contentY=y-CONTENT_SHIFT;
        int gear=visibleGearIndex(x,contentY), inventory=visibleInventoryIndex(x,contentY);
        VerifiedOutfitterCatalog.Item item=gear>=0?ownedGear(gear):
            inventory>=0?inventoryItem(inventory):null;
        return item==null?null:item.name()+(item.amount()>1?" × "+item.amount():
            item.amount()<0?" · quantity unspecified":"");
    }

    private void updateItemHover(MouseEvent event) {
        String next=itemHoverAt(event.getX(),event.getY());
        if(!Objects.equals(hoveredItemText,next) ||
                next!=null && (hoveredItemX!=event.getX() || hoveredItemY!=event.getY())) {
            hoveredItemText=next;
            hoveredItemX=event.getX(); hoveredItemY=event.getY();
            hoveredViewportY=getVisibleRect().y;
            repaint();
        }
    }

    /** Drawn inside the sidebar, so no Ctrl key, host popup or screen-wide tooltip. */
    private void paintItemHover(Graphics2D g) {
        if(hoveredItemText==null || model.view()!=BurrowModel.View.OUTFITTER) return;
        g.setFont(new Font("Dialog",Font.PLAIN,11));
        FontMetrics fm=g.getFontMetrics();
        Rectangle visible=getVisibleRect();
        // A scroll moved the item beneath a stationary mouse: discard stale text.
        if(visible.y!=hoveredViewportY) return;
        int availableWidth=visible.width>0?Math.min(WIDTH,visible.width):WIDTH;
        java.util.List<String> lines=wrapItemName(hoveredItemText,fm,Math.max(60,availableWidth-23));
        int width=0;
        for(String line:lines)width=Math.max(width,fm.stringWidth(line));
        width=Math.min(availableWidth-6,width+16);
        int height=lines.size()*15+10;
        int top=visible.height>0?visible.y:0;
        int bottom=visible.height>0?visible.y+visible.height:HEIGHT;
        int left=visible.width>0?visible.x:0;
        int x=Math.max(left+3,Math.min(hoveredItemX+10,left+Math.min(visible.width,WIDTH)-width-3));
        int y=hoveredItemY+15;
        if(y+height>bottom-3)y=hoveredItemY-height-12;
        y=Math.max(top+3,Math.min(y,Math.max(top+3,bottom-height-3)));
        g.setColor(new Color(11,15,16,245));g.fillRoundRect(x,y,width,height,5,5);
        g.setColor(GOLD);g.drawRoundRect(x,y,width-1,height-1,5,5);
        g.setColor(TEXT);
        for(int i=0;i<lines.size();i++)g.drawString(lines.get(i),x+8,y+16+i*15);
    }

    /** Splits long item names rather than truncating their identifying words. */
    private static java.util.List<String> wrapItemName(String text,FontMetrics fm,int maxWidth) {
        java.util.List<String> lines=new ArrayList<>();
        StringBuilder line=new StringBuilder();
        for(String word:text.split(" ")) {
            String candidate=line.length()==0?word:line+" "+word;
            if(line.length()>0 && fm.stringWidth(candidate)>maxWidth) {
                lines.add(line.toString()); line.setLength(0);
            }
            if(line.length()>0)line.append(' ');
            // Unusually long single-token names are broken without losing characters.
            for(int i=0;i<word.length();i++) {
                String attempt=line.toString()+word.charAt(i);
                if(fm.stringWidth(attempt)>maxWidth && line.length()>0) {
                    lines.add(line.toString()); line.setLength(0);
                }
                line.append(word.charAt(i));
            }
        }
        if(line.length()>0)lines.add(line.toString());
        return lines.isEmpty()?java.util.List.of(text):lines;
    }
    private static int navAt(int x,int y) {
        for(int i=0;i<4;i++) {
            int bx=9+i*52;
            if(inside(x,y,bx,79,51,33))return i;
        }
        return -1;
    }
    @Override public String getToolTipText(MouseEvent e) {
        if(model.view()==BurrowModel.View.OUTFITTER && activityMode){
            int x=e.getX(),y=e.getY()-CONTENT_SHIFT;
            ClueBankTagLayouts.Layout bankLayout=ClueBankTagLayouts.find(activityKitId);
            if(bankLayout!=null){
                if(activityBankGridTop>=0 && y>=activityBankGridTop+35 && y<activityBankGridTop+35+7*29 && x>=16 && x<208){
                    int col=(x-16)/24,row=(y-activityBankGridTop-35)/29;
                    int pos=row*8+col,id=bankLayout.slots()[pos];
                    return id>0?"Wiki bank layout · position "+(pos+1)+" · OSRS item ID "+id+" · quantity/ownership unverified":
                        "Empty Wiki bank-tag position "+(pos+1);
                }
                return "Wiki bank-tag template, not the player's carried inventory. Bank contents unverified.";
            }
            if(activityEquipmentTop>=0&&y>=activityEquipmentTop+37&&y<activityEquipmentTop+292){
                for(int i=0;i<11;i++){
                    int sx=14+ACT_EQUIPMENT_COLS[i]*66,sy=activityEquipmentTop+37+ACT_EQUIPMENT_ROWS[i]*51;
                    if(inside(x,y,sx,sy,61,45)){
                        var slot=ActivityOutfitterCatalog.find(activityKitId).equipment().get(i);
                        if(slot==null)return "Empty equipment slot";
                        ActivityItemRefs.Ref ref=ActivityItemRefs.find(activityKitId,true,i);
                        String provenance=ref==null?"text-only, item ID not source-verified":
                            "Wiki icon example: "+ref.itemName()+" (#"+ref.itemId()+") · "+ref.qualification();
                        var wikiSlayer=SlayerWikiGear.find(activityKitId);
                        if(wikiSlayer!=null && wikiSlayer.slot(i)!=null)
                            provenance += " · full Wiki rankings appear below the equipment diagram";
                        return (slot.conditional()?"Conditional: ":"Reference: ")+slot.label()+" · "+provenance+
                            (ActivityOutfitterCatalog.isWilderness(activityKitId)?" · BEST IN BANK LOCKED":" · bank not checked");
                    }
                }
            }
            if(activityInventoryTop>=0&&y>=activityInventoryTop+36&&y<activityInventoryTop+358){
                int col=(x-15)/49,row=(y-activityInventoryTop-36)/46;
                if(x>=15&&col>=0&&col<4&&row>=0&&row<7){
                    int n=row*4+col;var kit=ActivityOutfitterCatalog.find(activityKitId);
                    if(n<kit.inventory().size()){
                        var slot=kit.inventory().get(n);
                        ActivityItemRefs.Ref ref=ActivityItemRefs.find(activityKitId,false,n);
                        String icon=ref==null?"No confirmed item sprite":
                            "Wiki example: "+ref.itemName()+" (#"+ref.itemId()+") · "+ref.qualification();
                        return (slot.conditional()?"Conditional: ":"Reference: ")+slot.label()+
                            " · "+icon+" · quantity/ownership unverified";
                    }
                    return "Unassigned inventory space";
                }
            }
            return "Generic activity reference only; no live bank ownership or task requirements verified";
        }
        if(model.view()==BurrowModel.View.OUTFITTER) {
            int x=e.getX(), y=e.getY()-CONTENT_SHIFT;
            if(chronicleOpen) {
                if(chronicleBrowse)return "Browse sourced bosses; choose a card for details or open searchable picker";
                if(inside(x,y,13,130,83,30))return "Back to selected boss in Outfitter";
                if(chronicleRaid!=null)return null; // CP-5: raid card has no hidden notes
                if(displayBossModel().profile()!=null&&inside(x,y,13,chronicleNotesY(),199,preparationLayout().height())) {
                    var readiness=Final06ProfileMetadata.find(displayBossModel().boss(),displayBossModel().method(),displayBossModel().tier());
                    return PreparationTooltip.render(displayBossModel().profile().notes(),
                        readiness==null?Collections.emptyList():readiness.questRequirements(),
                        readiness==null?Collections.emptyList():readiness.chargedStateRequirements(),
                        readiness==null?Collections.emptyList():SwitchNoteFormatter.formatSwitches(readiness.switches(),displayBossModel().profile()));
                }
                return "Boss Chronicle · full sourced drop tracker, KC and preparation";
            }
            if(spaciousOutfitter()&&outTabClick(x,y))return x<114?"Bosses category":"Activities category";
            if(inside(x,y,13,outfitterSelectY(),spaciousOutfitter()?199:132,spaciousOutfitter()?OUT_SELECT_H:33))return "Select boss: "+displayBossModel().boss();
            if(inside(x,y,spaciousOutfitter()?13:CHRONICLE_X,spaciousOutfitter()?outfitterLogY():BOSS_ROW_Y,spaciousOutfitter()?199:63,spaciousOutfitter()?32:33))return "Open Boss Chronicle for "+displayBossModel().boss();
            if(hasSpecialMethods() && inside(x,y,13,outfitterMethodY(),199,spaciousOutfitter()?38:29))
                return "Choose method (special strategy): "+displayBossModel().method();
            if(inRareChip(x,y))return "Open sourced KC, drops and probability details for "+displayBossModel().boss();
            if(inside(x,y,13,tierRowY(),199,spaciousOutfitter()?3*39:31)) {
                Tier tier=Tier.values()[spaciousOutfitter()?Math.min(2,(y-tierRowY())/39):Math.min(2,(x-13)/66)];
                return TieredOutfitterCatalog.find(displayBossModel().boss(),displayBossModel().method(),tier)==null?
                    tier.label()+" setup unavailable":tier.label()+" · select setup";
            }
            int combatIndex=combatButtonAt(x,y);
            if(combatIndex>=0) {
                String selected=new String[]{"Melee","Ranged","Magic"}[combatIndex];
                String candidate=availableMethodForStyle(selected);
                var choice=methodChoiceForStyle(selected);
                return choice==null?"No researched "+selected+" method for "+displayBossModel().boss():
                    "Switch to "+selected+": "+choice.method()+" · "+choice.displayStatus();
            }
            if(inside(x,y,13,gearSourceY(),199,spaciousOutfitter()?73:21))
                return (spaciousOutfitter()?y<gearSourceY()+39:x<112)?"Show Wiki Recommended setup":
                    !BossRegistry.bestBankAllowed(displayBossModel().boss())?"Best in Bank blocked for Wilderness bosses":
                    "Use latest automatic bank snapshot; unknown is never unowned";
            if(showsWildernessTravelWarning() && inside(x,y,13,gearSourceY()+34,199,66))
                return wildernessTooltip(displayBossModel().boss());
            int contentY=y;
            VerifiedOutfitterCatalog.Profile profile=displayBossModel().profile();
            if(profile!=null) {
                int gear=visibleGearIndex(x,contentY), inv=visibleInventoryIndex(x,contentY);
                if(gear>=0) {
                    var item=ownedGear(gear);
                    if(item==null)return null;
                    var trip=currentTripPlan();
                    if(trip!=null) {
                        var cell=trip.gear().get(gear);
                        return cell.status()+" • "+tooltip(item)+(cell.note()==null||cell.note().isBlank()?"":" • "+cell.note());
                    }
                    var bankPlan=currentOwnedPlan();
                    if(bankPlan!=null) {
                        var choice=bankPlan.slots().get(gear);
                        return choice.kind()+" • "+tooltip(item)+" • "+choice.note();
                    }
                    return (bankModeActive() && !observedGearBank.isEmpty() ?
                        BestBankMatches.gear(profile,observedGearBank).get(gear).status()+" • " : "") + tooltip(item);
                }
                if(inv>=0) {
                    var trip=currentTripPlan();
                    if(trip!=null) {
                        var cell=trip.inventory().get(inv);
                        return cell.planned()==null?null:
                            cell.status()+" • "+tooltip(cell.shown())+(cell.note().isBlank()?"":" • "+cell.note());
                    }
                    var wikiItem=profile.inventory().get(inv);
                    if(wikiItem==null)return null;
                    String hint=switchHint(inv);
                    return (bankModeActive()?"WIKI SUPPLY (not bank-checked) • ":"")+tooltip(wikiItem)+(hint==null?"":" • "+hint);
                }
                if(chronicleOpen && chronicleRaid==null && inside(x,contentY,13,chronicleNotesY(),199,196)) {
                    var readiness=Final06ProfileMetadata.find(displayBossModel().boss(),displayBossModel().method(),displayBossModel().tier());
                    return PreparationTooltip.render(
                        profile.notes(),
                        readiness == null ? Collections.emptyList() : readiness.questRequirements(),
                        readiness == null ? Collections.emptyList() : readiness.chargedStateRequirements(),
                        readiness == null ? Collections.emptyList() : SwitchNoteFormatter.formatSwitches(readiness.switches(), profile));
                }
            }
        }
        return switch(navAt(e.getX(),e.getY())) {
            case 0 -> "Home - account overview";
            case 1 -> "Pathfinder - progression";
            case 2 -> "Outfitter - boss preparation";
            case 3 -> "Sessions - this login only";
            default -> null;
        };
    }
    private static String tooltip(VerifiedOutfitterCatalog.Item item) {
        return item==null?null:item.name()+(item.id()>0?" · ID "+item.id():" · exact variant unspecified")+
            (item.amount()>1?" × "+item.amount():item.amount()<0?" · quantity unspecified":"");
    }
    private static int gearIndex(int x,int y) {
        int[] columns={1,0,1,2,0,1,2,1,0,1,2};
        int[] rows={0,1,1,1,2,2,2,3,4,4,4};
        for(int i=0;i<11;i++) if(inside(x,y,29+columns[i]*62,419+rows[i]*46,43,42))return i;
        return -1;
    }
    private static int inventoryIndex(int x,int y) {
        for(int i=0;i<28;i++) if(inside(x,y,16+(i%4)*49,698+(i/4)*42,42,39))return i;
        return -1;
    }

    /** Matches the pixel bounds of the controls painted below; no invisible full-width buttons. */
    private static boolean inside(int x, int y, int bx, int by, int bw, int bh) {
        return x >= bx && x < bx + bw && y >= by && y < by + bh;
    }

    /** Testable mouse routing with strict rectangular hitboxes and no hidden actions. */
    public void clickAt(int x, int y) {
        // Build 20: long pages (preparation notes, Rare Drop Tracker, Pathfinder goals) extend past HEIGHT.
        if (!inside(x, y, 0, 0, WIDTH, Math.max(HEIGHT, getPreferredSize().height))) return;
        clearItemHover();
        if(accountModeManaged) {
            // Header settings toggle: only after choosing an account type.
            if (inside(x,y,196,45,23,24) && accountMode!=null && activeAccountModeHash!=-1L) {
                settingsOpen=!settingsOpen; clearItemHover();revalidate();repaint();return;
            }
            if(isAccountSetupRequired() || settingsOpen) {
                if(activeAccountModeHash!=-1L) {
                    Mode selected=inside(x,y,18,282,189,46)?Mode.IRONMAN:
                                  inside(x,y,18,340,189,46)?Mode.MAIN:null;
                    if(selected!=null)accountModeAction.accept(activeAccountModeHash,selected);
                }
                if(settingsOpen) {
                    if(inside(x,y,18,426,189,34))settingsOpen=false;
                    else {
                        UiScale previousSize=uiScale;AccentTheme previousAccent=accentTheme;
                        FeralTypography.TextWeight previousWeight=textWeight;
                        if(inside(x,y,19,523,88,26))uiScale=UiScale.COMPACT;
                        else if(inside(x,y,117,523,88,26))uiScale=UiScale.DEFAULT;
                        else if(inside(x,y,19,555,88,26))uiScale=UiScale.LARGE;
                        else if(inside(x,y,117,555,88,26))uiScale=UiScale.EXTRA_LARGE;
                        else if(inside(x,y,19,602,88,26))textWeight=FeralTypography.TextWeight.REGULAR;
                        else if(inside(x,y,117,602,88,26))textWeight=FeralTypography.TextWeight.BOLD;
                        else if(inside(x,y,19,705,88,26))accentTheme=AccentTheme.BRONZE;
                        else if(inside(x,y,117,705,88,26))accentTheme=AccentTheme.BLUE;
                        else if(inside(x,y,19,735,88,26))accentTheme=AccentTheme.GREEN;
                        else if(inside(x,y,117,735,88,26))accentTheme=AccentTheme.CRIMSON;
                        if(previousSize!=uiScale || previousAccent!=accentTheme || previousWeight!=textWeight)saveAppearance();
                    }
                }
                clearItemHover();revalidate();repaint();return;
            }
        }
        int navIndex=navAt(x,y);
        if(navIndex>=0) {
            BurrowModel.View before=model.view();
            model.show(BurrowModel.View.values()[navIndex]);
            chronicleOpen=false;chronicleReturnView=BurrowModel.View.OUTFITTER;notifyViewChange(before);
            revalidate();
            repaint();
            return;
        }
        int sourceY=y-CONTENT_SHIFT; // Only historic non-Home pages use this coordinate system.
        switch (model.view()) {
            case OUTFITTER -> {
                if(activityMode) {
                    if(spaciousOutfitter()&&outTabClick(x,sourceY)){
                        if(x<114)switchActivityMode(false);return;
                    }
                    if(!spaciousOutfitter()&&inside(x,sourceY,13,130,95,32)){switchActivityMode(false);return;}
                    if(inside(x,sourceY,13,spaciousOutfitter()?OUT_SELECT_Y:202,199,spaciousOutfitter()?OUT_SELECT_H:33)){
                        var kits=ActivityOutfitterCatalog.all();
                        int index=0;for(int i=0;i<kits.size();i++)if(kits.get(i).id().equals(activityKitId)){index=i;break;}
                        activityKitId=kits.get((index+1)%kits.size()).id();outfitterSelectionEpoch++;revalidate();repaint();
                    }
                    return;
                }
                if(chronicleOpen) {
                    if(chronicleBrowse){
                        if(inside(x,sourceY,13,130,71,30)){closeChronicle();return;}
                        if(inside(x,sourceY,13,168,199,31))return; // live search uses native popup
                        if(inside(x,sourceY,13,207,92,27)){
                            chronicleBrowseGroup=(chronicleBrowseGroup+1)%6;revalidate();repaint();return;
                        }
                        if(inside(x,sourceY,112,207,100,27)){
                            chronicleBrowseSort=(chronicleBrowseSort+1)%5;revalidate();repaint();return;
                        }
                        for(var e:chronicleBrowseActions.entrySet())if(e.getValue().contains(x,sourceY)){
                            selectBossFromPicker(e.getKey(),true);revalidate();repaint();return;
                        }
                        return;
                    }
                    if(inside(x,sourceY,134,165,78,32)) {chronicleBrowse=true;revalidate();repaint();return;}
                    if(inside(x,sourceY,13,130,56,30)) {closeChronicle();return;}
                    if(inside(x,sourceY,71,130,24,30)) {cycleChronicleBoss(-1);return;}
                    if(inside(x,sourceY,189,130,24,30)) {cycleChronicleBoss(1);return;}
                    if(inside(x,sourceY,96,130,91,30)) {return;} // native chooser on mouseReleased
                    if(chronicleRaid!=null) break; // CP-5: the raid card has no hidden tracker/notes hit areas
                    int trackerRel=sourceY-rareDropTrackerY();
                    if(trackerRel>=0&&trackerRel<rareDropTrackerHeight()&&inside(x,sourceY,13,rareDropTrackerY(),199,rareDropTrackerHeight()))
                        rareDropClick(x,trackerRel);
                    else if(combatTasksHeight()>0&&inside(x,sourceY,13,combatTasksY(),199,COMBAT_TASK_HEADER))
                        combatTasksExpanded=!combatTasksExpanded;
                    else if(displayBossModel().profile()!=null&&prepCollapsible()&&
                        inside(x,sourceY,13,chronicleNotesY()+prepToggleY()-PREPARATION_Y,199,20))
                        prepExpanded=!prepExpanded;
                    break;
                }
                // Shared category choice is organizational only. Never mutate the saved boss/tier.
                if(spaciousOutfitter()&&outTabClick(x,sourceY)){
                    if(x>=114)switchActivityMode(true);
                    return;
                }
                int trackerRel=sourceY-rareDropTrackerY();
                if(inRareChip(x,sourceY)) {
                    rareDropExpanded=!rareDropExpanded;rareDropFeedback=null;
                } else if(rareDropExpanded&&trackerRel>=0&&trackerRel<rareDropTrackerHeight()&&x>=13&&x<=212) {
                    rareDropClick(x,trackerRel);
                } else if(bankModeActive() && inside(x,sourceY,FOCUS_CHIP_X,equipmentY()-bankStatusHeight()-6+3,FOCUS_CHIP_W,17)) {
                    // CP-7: cycle Damage -> Defence -> Prayer; the host re-plans and confirms via setBestBankFocus.
                    bestBankFocus=bestBankFocus.next();bestBankFocusAction.accept(bestBankFocus);repaint();
                } else if(bankModeActive() && inside(x,sourceY,13,equipmentY()-bankStatusHeight()-6,199,bankStatusHeight())) {
                    bankDetailsExpanded=!bankDetailsExpanded;revalidate();repaint();
                } else if(!spaciousOutfitter()&&inside(x,sourceY,112,tierRowY()-17,100,16)) {
                    switchActivityMode(true);return;
                } else if(inside(x,sourceY,spaciousOutfitter()?13:CHRONICLE_X,
                        spaciousOutfitter()?outfitterLogY():BOSS_ROW_Y,spaciousOutfitter()?199:63,spaciousOutfitter()?32:33)) {
                    openChronicle(BurrowModel.View.OUTFITTER);return;
                } else if(inside(x,sourceY,13,outfitterSelectY(),spaciousOutfitter()?199:132,spaciousOutfitter()?OUT_SELECT_H:33)) {
                    // Live GUI uses the native grouped dropdown. Headless fallback advances bosses.
                    int index=BurrowModel.availableBosses().indexOf(displayBossModel().boss());
                    model.chooseBoss(BurrowModel.availableBosses().get((index+1)%BurrowModel.availableBosses().size()));
                    styleFeedback=null;notifySelection();
                } else if(hasSpecialMethods() && inside(x,sourceY,13,outfitterMethodY(),199,spaciousOutfitter()?38:29)) {
                    // Method choices are menu-only; never silently cycle in production.
                } else if(inside(x,sourceY,13,tierRowY(),199,spaciousOutfitter()?3*39:31)) {
                    int index=spaciousOutfitter()?Math.min(2,(sourceY-tierRowY())/39):Math.min(2,(x-13)/66);
                    if(TieredOutfitterCatalog.find(displayBossModel().boss(),displayBossModel().method(),Tier.values()[index])!=null
                        || ResearchCombatMethods.find(displayBossModel().boss(),displayBossModel().method())!=null)
                        model.chooseTier(Tier.values()[index]);
                    styleFeedback=null;notifySelection();
                } else if(inside(x,sourceY,13,gearSourceY(),199,spaciousOutfitter()?73:21)) {
                    if(spaciousOutfitter()?sourceY<gearSourceY()+39:x<112)bankMatchView=false;
                    else if(!BossRegistry.bestBankAllowed(displayBossModel().boss()))
                        styleFeedback="Best in Bank is off for Wilderness bosses";
                    else if(!selectedMethodHasBankPlan())
                        styleFeedback="No bank-approved gear for this method; use the research preview";
                    else {bankMatchView=true;notifySelection();}
                } else if(combatButtonAt(x,sourceY)>=0) {
                    int i=combatButtonAt(x,sourceY);
                    String style=new String[]{"Melee","Ranged","Magic"}[i];
                    String candidate=availableMethodForStyle(style);
                    if(candidate!=null) {
                        model.chooseMethod(candidate);notifySelection();
                        var choice=methodChoiceForStyle(style);
                        styleFeedback=selectedMethodIsResearchGear()?"RESEARCH GEAR · CHECK; Bank O disabled":
                            displayBossModel().profile()==null?"STRATEGY ONLY · no verified gear":null;
                    } else styleFeedback="No researched "+style+" method for this boss";
                }
            }
            case SESSIONS -> {
                if (flowSessionEnabled()) {flowSessionClick(x,sourceY);break;}
                // This is an explicit user action, never a synthetic XP event.
                // Only an explicitly OFF tracker (or a legacy host) offers START. Syncing, paused
                // and unavailable states are informational; nothing is armed by clicking them.
                if (inside(x,sourceY,13,SESSION_XP_TOP,199,sessionXpHeight()) &&
                    activeSessionState==SessionState.ACTIVE &&
                    sessionFeeds.get(SessionFeed.XP)!=FeedStatus.AVAILABLE &&
                    xpTrackingState==XpTrackingState.OFF)xpStartAction.run();
                else {
                    int index=historyIndexAt(sourceY);
                    if(index>=0)expandedHistoryIndex=expandedHistoryIndex==index?-1:index;
                }
                revalidate();
            }
            case PATHFINDER -> {
                if (flowPathfinderEnabled()) {flowPathfinderClick(x,sourceY);break;}
                if (sourceY>=pathfinderExtrasStartY() && pathfinderExtrasClick(x,sourceY)) { revalidate(); }
                else if (inside(x,sourceY,13,431,110,22)) {
                    // Headless/test path: advance the dropdown selection (the live panel opens pathfinderFocusMenu).
                    int i=java.util.Arrays.asList(FOCUS_ORDER).indexOf(pathfinderFocus);
                    pathfinderFocus=FOCUS_ORDER[(i+1)%FOCUS_ORDER.length];
                    rebuildPathfinderSuggestions();
                } else if (inside(x,sourceY,126,431,85,22)) {
                    // 60 -> 15 -> 30 -> 45 -> 60 ... then 90/120 via the live dropdown; cycle keeps Build 17 order.
                    int[] cyc={60,15,30,45,90,120};
                    int i=0; while(i<cyc.length && cyc[i]!=pathfinderMinutes)i++;
                    pathfinderMinutes=cyc[(i+1)%cyc.length];
                    rebuildPathfinderSuggestions();
                } else if (inside(x,sourceY,13,459,198,22)) {
                    int i=Math.min(2,(x-13)/66);
                    pathfinderAttention=new SuggestionEngine.Attention[]{SuggestionEngine.Attention.ACTIVE,
                        SuggestionEngine.Attention.LOW,SuggestionEngine.Attention.AFK}[i];
                    rebuildPathfinderSuggestions();
                } else if(inside(x,sourceY,13,505,199,285)) {
                    int i=(sourceY-505)/95;
                    if(i>=0 && i<Math.min(3,pathfinderSuggestions.size())) {
                        var s=pathfinderSuggestions.get(i);
                        if(x>=14&&x<=38 && sourceY>=505+i*95+4 && sourceY<=505+i*95+29) {
                            togglePathfinderCheckbox(s);
                            break;
                        }
                        pathfinderSelectedId=s.id();
                        pathfinderDetail=s.why()+" | "+s.requirement()+" | "+s.source();
                        revalidate();
                    }
                } else if(inside(x,sourceY,13,pathfinderActionY(),199,32)) {
                    var selected=pathfinderSuggestions.stream().filter(s->s.id().equals(pathfinderSelectedId)).findFirst();
                    if(selected.isPresent()) {
                        String boss=selected.get().outfitterBoss();
                        if(boss!=null && BurrowModel.availableBosses().contains(boss)) {
                            activityMode=false;model.chooseBoss(boss);
                            BurrowModel.View old=model.view();
                            model.show(BurrowModel.View.OUTFITTER);notifyViewChange(old);
                        }
                    }
                }
            }
            case HOME -> {
                if (flowHomeEnabled()) {flowHomeClick(x,y);break;}
                // Crew is drawn in natural Home coordinates; legacy hitboxes move only when a feed is visible.
                if(crewClick(x,y))return;
                int homeY=y-crewShiftForTest();
                BurrowModel.View old=model.view();
                if(inside(x,homeY,13,409,199,26))model.show(BurrowModel.View.OUTFITTER);
                else if(inside(x,homeY,13,446,199,26))model.show(BurrowModel.View.SESSIONS);
                else if(inside(x,homeY,13,483,199,26)) {openChronicle(BurrowModel.View.HOME);return;}
                else if(inside(x,homeY,9,526,207,106))model.show(BurrowModel.View.PATHFINDER);
                notifyViewChange(old);
            }
        }
        if(model.view()==BurrowModel.View.OUTFITTER) revalidate();
        repaint();
    }
    @Override public void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g=(Graphics2D)g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(BG);g.fillRect(0,0,WIDTH,Math.max(HEIGHT,getHeight()));
            header(g);
            if(accountModeManaged && (isAccountSetupRequired() || settingsOpen)) {
                accountModeScreen(g);
                return;
            }
            navigation(g);
            switch(model.view()) {
                case HOME -> home(g);
                case PATHFINDER -> { Graphics2D content=(Graphics2D)g.create();content.translate(0,CONTENT_SHIFT);pathfinder(content);content.dispose(); }
                case OUTFITTER -> { Graphics2D content=(Graphics2D)g.create();content.translate(0,CONTENT_SHIFT);outfitter(content);content.dispose(); }
                case SESSIONS -> { Graphics2D content=(Graphics2D)g.create();content.translate(0,CONTENT_SHIFT);sessions(content);content.dispose(); }
            }
            // The name label is a local mouse-hover overlay, not a host tooltip popup.
            paintItemHover(g);
            // Session observations are read-only; no demo kill-counter rendering.
        } finally { g.dispose(); }
    }
    // Owner's 2026-10-31 four-panel concept R01. One 225px shell for every screen.
    // All artwork is a tiny owner-reference badge; no oversized mascot artwork.
    private void header(Graphics2D g) {
        g.setPaint(new GradientPaint(0,0,new Color(28,32,33),0,75,new Color(16,20,21)));
        g.fillRect(0,0,WIDTH,77);
        g.setColor(new Color(61,66,62));g.drawLine(9,7,216,7);
        Sprites.armoredBadge(g,9,12,51,53);
        label(g,"FERAL ENTERPRISES",65,20,8,GOLD,true);
        String accountDescriptor=accountModeManaged && accountMode!=null ?
            (accountMode==Mode.IRONMAN?"IRONMAN":"MAIN")+" · ACCOUNT CONSOLE" :
            accountModeManaged?"LOG IN TO START":"ACCOUNT INTELLIGENCE";
        if(uiScale==UiScale.EXTRA_LARGE) {
            label(g,"TrashPanda",65,37,11,TEXT,true);
            label(g,"Tracker",65,53,11,TEXT,true);
            label(g,accountModeManaged?"ACCOUNT CONSOLE":"ACCOUNT INTELLIGENCE",65,67,8,MUTED,false);
        } else {
            label(g,"TrashPanda Tracker",65,39,12,TEXT,true);
            small(g,accountDescriptor,65,54,MUTED);
        }
        if(accountModeManaged && accountMode!=null && activeAccountModeHash!=-1L) {
            g.setColor(GOLD);g.drawOval(199,51,12,12);
            g.drawLine(205,48,205,51);g.drawLine(205,63,205,66);
            g.drawLine(196,57,199,57);g.drawLine(211,57,214,57);
            g.setColor(TEXT);g.fillOval(203,55,4,4);
        }
        g.setColor(new Color(94,75,42));g.drawLine(9,71,216,71);
    }
    private void accountModeScreen(Graphics2D g) {
        // Only the real host can arm first-run mode. This screen never fabricates account identity.
        g.setColor(BG);g.fillRect(0,77,WIDTH,803);
        label(g,settingsOpen?"ACCOUNT SETTINGS":"WELCOME TO TRASHPANDA",13,106,12,GOLD,true);
        g.setColor(new Color(118,89,47));g.drawLine(12,118,212,118);
        card(g,137,127);
        zoneTitle(g,settingsOpen?"ACCOUNT TYPE":"IRONMAN BY DEFAULT",158);
        small(g,"Default mode: Ironman",19,181,MUTED);
        small(g,"Change your mode anytime.",19,201,MUTED);
        if(activeAccountModeHash == -1L) {
            label(g,"LOG IN TO CONTINUE",19,236,11,GOLD,true);
            card(g,282,104);
            label(g,"Account identity unavailable",19,310,11,TEXT,true);
            small(g,"Accounts default to Ironman when logged in.",19,333,MUTED);
            return;
        }
        for(int i=0;i<2;i++) {
            Mode candidate=i==0?Mode.IRONMAN:Mode.MAIN;
            int y=i==0?282:340;
            boolean selected=candidate==accountMode;
            g.setColor(selected?new Color(88,68,38):new Color(34,43,44));
            g.fillRoundRect(18,y,189,46,5,5);
            g.setColor(selected?GOLD:new Color(93,102,99));g.drawRoundRect(18,y,188,45,5,5);
            label(g,candidate==Mode.IRONMAN?"IRONMAN":"MAIN ACCOUNT",27,y+20,12,selected?GOLD:TEXT,true);
            small(g,selected?"CURRENT SELECTION":"CHOOSE THIS MODE",27,y+36,selected?MINT:MUTED);
        }
        if(settingsOpen) {
            button(g,"BACK TO TRACKER",426,true);
            card(g,474,169);
            zoneTitle(g,"READABILITY",490);
            small(g,"Text size",19,512,MUTED);
            drawSettingChoice(g,19,523,88,26,"Compact",uiScale==UiScale.COMPACT);
            drawSettingChoice(g,117,523,88,26,"Normal",uiScale==UiScale.DEFAULT);
            drawSettingChoice(g,19,555,88,26,"Large",uiScale==UiScale.LARGE);
            drawSettingChoice(g,117,555,88,26,"Extra Large",uiScale==UiScale.EXTRA_LARGE);
            small(g,"Text weight",19,593,MUTED);
            drawSettingChoice(g,19,602,88,26,"Regular",textWeight==FeralTypography.TextWeight.REGULAR);
            drawSettingChoice(g,117,602,88,26,"Bold",textWeight==FeralTypography.TextWeight.BOLD);
            card(g,655,110);
            zoneTitle(g,"ACCENT COLOR",671);
            small(g,"Choose a highlight colour.",19,693,MUTED);
            drawAccentChip(g,19,705,88,26,AccentTheme.BRONZE,accentTheme==AccentTheme.BRONZE);
            drawAccentChip(g,117,705,88,26,AccentTheme.BLUE,accentTheme==AccentTheme.BLUE);
            drawAccentChip(g,19,735,88,26,AccentTheme.GREEN,accentTheme==AccentTheme.GREEN);
            drawAccentChip(g,117,735,88,26,AccentTheme.CRIMSON,accentTheme==AccentTheme.CRIMSON);
            card(g,777,81);
            zoneTitle(g,"WHAT THIS CHANGES",793);
            small(g,"Appearance settings only.",19,816,MUTED);
            small(g,"Game data is unchanged.",19,835,MUTED);
        } else {
            card(g,426,111);
            zoneTitle(g,"FIRST-TIME SETUP",445);
            small(g,"Your choice is saved for this account.",19,470,MUTED);
            small(g,"New accounts default to Ironman.",19,491,MUTED);
        }
    }
    private void navigation(Graphics2D g) {
        final String[] names={"Home","Pathfinder","Outfitter","Sessions"};
        g.setColor(new Color(17,21,22));g.fillRect(0,76,WIDTH,43);
        for(int i=0;i<4;i++) {
            int x=9+i*52, y=79, w=51, h=33;
            boolean selected=model.view().ordinal()==i;
            boolean hover=hoveredNav==i;
            g.setColor(selected?blend(accent(),PANEL,.38f):hover?new Color(39,48,49):new Color(29,35,36));
            g.fillRect(x,y,w,h);
            g.setColor(selected?accent():new Color(69,78,78));g.drawRect(x,y,w-1,h-1);
            String name=names[i];
            g.setFont(uiFont(i==1?8:9,selected));
            FontMetrics fm=g.getFontMetrics();
            g.setColor(selected?new Color(16,20,21):TEXT);
            if(fm.stringWidth(name)<=w-4) {
                g.drawString(name,x+(w-fm.stringWidth(name))/2,y+20);
            } else {
                String[] pair=switch(name) {
                    case "Pathfinder" -> new String[]{"Path","finder"};
                    case "Outfitter" -> new String[]{"Out","fitter"};
                    case "Sessions" -> new String[]{"Ses","sions"};
                    default -> new String[]{name,""};
                };
                g.drawString(pair[0],x+(w-fm.stringWidth(pair[0]))/2,y+14);
                if(!pair[1].isEmpty())g.drawString(pair[1],x+(w-fm.stringWidth(pair[1]))/2,y+27);
            }
        }
        g.setColor(new Color(109,82,43));g.drawLine(9,119,216,119);
    }
    private void title(Graphics2D g, String t, String s) {
        String code=switch(t){case "PATHFINDER"->"02 / ROUTE INTELLIGENCE";
            case "OUTFITTER"->"03 / FIELD TERMINAL";
            case "SESSIONS"->"04 / CURRENT-LOGIN LEDGER";
            default->"01 / ARMORY WORKBENCH";};
        label(g,code,12,135,8,GOLD,true);
        label(g,t.equals("OUTFITTER")?"Outfitter":t.equals("PATHFINDER")?"Pathfinder":
            t.equals("SESSIONS")?"Sessions":"Home",12,153,17,TEXT,true);
        g.setColor(new Color(118,89,47));g.drawLine(12,160,213,160);
    }
    private void card(Graphics2D g,int y,int h) {
        g.setColor(PANEL);g.fillRoundRect(9,y,207,h,4,4);
        g.setColor(new Color(72,80,79));g.drawRoundRect(9,y,206,h-1,4,4);
        g.setColor(new Color(116,86,41));g.fillRect(9,y,2,h);
    }
    private void button(Graphics2D g,String text,int y,boolean active) {
        g.setColor(active?blend(accent(),PANEL,.32f):new Color(31,39,41));g.fillRect(13,y,199,26);
        g.setColor(active?accent():LINE);g.drawRect(13,y,198,25);
        small(g,text,24,y+17,active?GOLD:TEXT);
    }
    private void dropdownBox(Graphics2D g,int x,int y,int w,int h,String text) {
        g.setColor(new Color(37,43,44));g.fillRoundRect(x,y,w,h,4,4);
        g.setColor(accent());g.drawRoundRect(x,y,w,h,4,4);
        clipped(g,text,x+6,y+15,GOLD,w-10);
    }
    static String focusLabel(SuggestionEngine.Focus f) {
        return switch(f) {case ANY->"ALL";case QUESTS->"QUESTS";case PVM->"PVM";case GP->"GP";case SUPPLIES->"SUPPLY";case AFK->"AFK";};
    }
    static final SuggestionEngine.Focus[] FOCUS_ORDER={SuggestionEngine.Focus.ANY,SuggestionEngine.Focus.QUESTS,
        SuggestionEngine.Focus.PVM,SuggestionEngine.Focus.GP,SuggestionEngine.Focus.SUPPLIES,SuggestionEngine.Focus.AFK};
    /** Native dropdown for the Pathfinder category filter. */
    JPopupMenu pathfinderFocusMenu() {
        JPopupMenu menu=new JPopupMenu(); menu.getAccessibleContext().setAccessibleName("Pathfinder activity type");
        ButtonGroup group=new ButtonGroup();
        for(var f:FOCUS_ORDER) {
            JRadioButtonMenuItem c=new JRadioButtonMenuItem(switch(f){case ANY->"All activity types";case QUESTS->"Quests";
                case PVM->"PvM / bosses";case GP->"Money making";case SUPPLIES->"Supplies";case AFK->"AFK methods";},f==pathfinderFocus);
            group.add(c); c.addActionListener(e->{pathfinderFocus=f;rebuildPathfinderSuggestions();revalidate();repaint();});
            menu.add(c);
        }
        return menu;
    }
    /** Native dropdown for the available-time filter. */
    JPopupMenu pathfinderMinutesMenu() {
        JPopupMenu menu=new JPopupMenu(); menu.getAccessibleContext().setAccessibleName("Pathfinder available minutes");
        ButtonGroup group=new ButtonGroup();
        for(int mins:SuggestionEngine.MINUTE_CHOICES) {
            JRadioButtonMenuItem c=new JRadioButtonMenuItem(mins+" minutes",mins==pathfinderMinutes);
            group.add(c); c.addActionListener(e->{pathfinderMinutes=mins;rebuildPathfinderSuggestions();revalidate();repaint();});
            menu.add(c);
        }
        return menu;
    }
    private void drawSettingChoice(Graphics2D g,int x,int y,int w,int h,String text,boolean selected) {
        g.setColor(selected?blend(accent(),PANEL,.42f):new Color(34,43,44));
        g.fillRoundRect(x,y,w,h,5,5);
        g.setColor(selected?accent():LINE);g.drawRoundRect(x,y,w-1,h-1,5,5);
        g.setFont(uiFont(10,selected));
        FontMetrics fm=g.getFontMetrics();
        g.setColor(selected?TEXT:MUTED);
        g.drawString(text,x+(w-fm.stringWidth(text))/2,y+(h+fm.getAscent()-fm.getDescent())/2-1);
    }
    private void drawAccentChip(Graphics2D g,int x,int y,int w,int h,AccentTheme theme,boolean selected) {
        g.setColor(blend(theme.color,PANEL,.45f));g.fillRoundRect(x,y,w,h,6,6);
        g.setColor(selected?theme.color:LINE);g.drawRoundRect(x,y,w-1,h-1,6,6);
        g.setColor(theme.color);g.fillRoundRect(x+8,y+8,14,h-16,4,4);
        label(g,theme.label,x+28,y+20,10,selected?TEXT:MUTED,selected);
    }
    private void zoneTitle(Graphics2D g,String text,int y) {
        label(g,text,15,y,9,GOLD,true);
    }
    private void detail(Graphics2D g,String text,int y,Color c) {
        clipped(g,text,19,y,c,181);
    }
    // ---- R07: current-login Home summary (same accepted events as Sessions) ----
    private java.util.List<LiveSessionLedger.Activity> recentActivity=java.util.List.of();
    private java.util.function.LongSupplier homeClock=System::currentTimeMillis;
    /** EDT only. Newest first; already scoped to the current login by the host. */
    public void setRecentActivity(java.util.List<LiveSessionLedger.Activity> list) {
        recentActivity=list==null?java.util.List.of():java.util.List.copyOf(list); revalidate(); repaint();
    }
    void setHomeClockForTest(java.util.function.LongSupplier c){homeClock=c;}
    java.util.List<LiveSessionLedger.Activity> recentActivityForTest(){return recentActivity;}
    static final int HOME_RECENT_Y=646, HOME_RECENT_ROWS=5, HOME_RECENT_ROW=32;
    int homeRecentBottom() {
        int rows=Math.min(HOME_RECENT_ROWS,recentActivity.size());
        return HOME_RECENT_Y+(rows==0?70:44+rows*HOME_RECENT_ROW);
    }
    private int homeWeekHeight(){return weeklyTotals==null?70:HOME_WEEK_H;}
    int homeWeekBottom() { return homeRecentBottom()+10+homeWeekHeight(); }
    int homeGoalsBottom() { return homeWeekBottom()+10+directionCardHeight(HomeDirection.dropGoals(rareDropState).size()); }
    int homeBottom() { return homeGoalsBottom()+10+directionCardHeight(homeUpgradeLines().size())+8; }
    // ---- R09: long-term drop goals + evidence-gated upgrades ----
    private BestBankPreloader.UpgradeEvidence upgradeEvidence=BestBankPreloader.UpgradeEvidence.NONE;
    private java.util.List<BestBankPreloader.Upgrade> upgrades=java.util.List.of();
    /** EDT only. Host passes the preloader's evidence state and current-generation upgrade list. */
    public void setUpgrades(BestBankPreloader.UpgradeEvidence evidence,java.util.List<BestBankPreloader.Upgrade> list) {
        setUpgrades(evidence,list,-1,-1);
    }
    private int upgradePrepared=-1, upgradeTotal=-1;
    /** CP-1: plus coverage (prepared plans out of all plans) so Home never implies a global optimisation. */
    public void setUpgrades(BestBankPreloader.UpgradeEvidence evidence,java.util.List<BestBankPreloader.Upgrade> list,int prepared,int total) {
        upgradeEvidence=evidence==null?BestBankPreloader.UpgradeEvidence.NONE:evidence;
        upgrades=list==null?java.util.List.of():java.util.List.copyOf(list);
        upgradePrepared=prepared; upgradeTotal=total; revalidate(); repaint();
    }
    java.util.List<HomeDirection.Line> homeUpgradeLines() { return HomeDirection.upgrades(upgradeEvidence,upgrades,upgradePrepared,upgradeTotal); }
    java.util.List<HomeDirection.Line> homeObjectiveLines() { return HomeDirection.objective(pathfinderObserved,pathfinderSuggestions); }
    java.util.List<HomeDirection.Line> homeGoalLines() { return HomeDirection.dropGoals(rareDropState); }
    private static int directionCardHeight(int lines) { return 34+Math.min(lines,10)*16+6; }
    private void paintDirectionLines(Graphics2D g,java.util.List<HomeDirection.Line> lines,int x,int y,int max,int step) {
        for(int i=0;i<Math.min(max,lines.size());i++) {
            var l=lines.get(i);
            Color c=switch(l.kind()){case HEAD->TEXT;case GOOD->MINT;case WARN->GOLD;case MUTED->MUTED;default->TEXT;};
            int tx=x;
            if(l.itemId()>0) {
                BufferedImage sprite=itemImage.apply(l.itemId());
                if(sprite!=null && sprite.getWidth()>0) {
                    double sc=Math.min(14.0/sprite.getWidth(),14.0/sprite.getHeight());
                    g.drawImage(sprite,x,y-11,(int)Math.round(sprite.getWidth()*sc),(int)Math.round(sprite.getHeight()*sc),null);
                }
                tx=x+18;
            }
            if(l.kind()==HomeDirection.Line.Kind.HEAD) clipped(g,l.text(),tx,y,c,206-tx);
            else small(g,elideSessionText(g,l.text(),206-tx),tx,y,c);
            y+=step;
        }
    }
    private void paintDirectionCard(Graphics2D g,int y,String title,java.util.List<HomeDirection.Line> lines) {
        int h=directionCardHeight(lines.size());
        card(g,y,h);
        zoneTitle(g,title,y+17);
        paintDirectionLines(g,lines,19,y+36,10,16);
    }
    // ---- R08: weekly progress card (Mon–Sun, local time; persisted per account) ----
    static final int HOME_WEEK_H=168;
    private WeeklyProgress.Totals weeklyTotals;
    private String weeklyNote="Sign in to load this account's weekly progress";
    public static final String COUNTER_LEVEL_UPS=VerifiedCounters.LEVEL_UPS, COUNTER_COLLECTION=VerifiedCounters.COLLECTION, COUNTER_CHESTS=VerifiedCounters.CHESTS;
    /** EDT only. null totals = signed out / storage unavailable. */
    public void setWeeklyProgress(WeeklyProgress.Totals totals,String note) {
        weeklyTotals=totals; weeklyNote=note==null?"":note; repaint();
    }
    /** Lines exactly as painted, for tests. */
    java.util.List<String> weeklyLinesForTest() {
        var t=weeklyTotals; var out=new ArrayList<String>();
        if(t==null){out.add(weeklyNote);return out;}
        out.add("Week of "+t.weekStart()+" (Mon–Sun)");
        out.add("Observed XP: +"+sessionAmount(t.xp()));
        out.add("Verified boss KC increases: +"+t.verifiedKc());
        out.add("Boss drops: "+sessionAmount(t.dropItems())+" items · ~"+t.lootEstimates()+" loot kills (est.)");
        java.util.function.Function<String,String> c=n->t.counter(n).isPresent()?String.valueOf(t.counter(n).getAsLong()):"not tracked";
        out.add("Level-ups: "+c.apply(COUNTER_LEVEL_UPS));
        out.add("Confirmed chests (Barrows): "+c.apply(COUNTER_CHESTS));
        out.add("Collection unlocks: "+c.apply(COUNTER_COLLECTION));
        out.add(t.logins()+" login"+(t.logins()==1?"":"s")+" · first KC readings are baselines");
        return out;
    }
    private void paintHomeWeek(Graphics2D g) {
        int y=homeRecentBottom()+10;
        card(g,y,homeWeekHeight());
        zoneTitle(g,"WEEKLY PROGRESS",y+17);
        var lines=weeklyLinesForTest();
        int ly=y+36;
        for(int i=0;i<lines.size();i++) {
            Color c=i==0?GOLD:lines.get(i).contains("not tracked")?MUTED:i==lines.size()-1?MUTED:TEXT;
            small(g,elideSessionText(g,lines.get(i),190),19,ly,c); ly+=16;
        }
    }
    static String ago(long now,long at) {
        long s=Math.max(0,now-at)/1000;
        if(s<60)return "now";
        if(s<3600)return (s/60)+"m ago";
        if(s<86400)return (s/3600)+"h ago";
        return (s/86400)+"d ago";
    }
    /** Text of the Home signal tiles exactly as painted (tests compare these with the Sessions readout). */
    java.util.List<String> homeSignalsForTest() {
        long kc=sessionKc.values().stream().mapToLong(Long::longValue).sum();
        long drops=sessionDrops.values().stream().mapToLong(SessionItem::quantity).sum();
        long est=sessionLootEncounters.values().stream().mapToLong(Long::longValue).sum();
        return java.util.List.of(
            activeSessionState==SessionState.ACTIVE?"+"+sessionAmount(sessionTotalXp):"—",
            sessionFeeds.get(SessionFeed.BOSS_KC)==FeedStatus.AVAILABLE?"+"+kc:"—",
            activeSessionState==SessionState.OFFLINE?"—":drops+" drop item"+(drops==1?"":"s")+" · ~"+est+" loot kill"+(est==1?"":"s"));
    }
    private void paintHomeRecent(Graphics2D g) {
        int y=HOME_RECENT_Y, rows=Math.min(HOME_RECENT_ROWS,recentActivity.size());
        int h=rows==0?70:44+rows*HOME_RECENT_ROW;
        card(g,y,h);
        zoneTitle(g,"RECENT ACTIVITY",y+17);
        if(rows==0) { small(g,activeSessionState==SessionState.OFFLINE?"Sign in to see this login's activity":"No verified XP, KC or drops yet",19,y+44,MUTED); return; }
        long now=homeClock.getAsLong();
        for(int i=0;i<rows;i++) {
            var a=recentActivity.get(i); int ry=y+30+i*HOME_RECENT_ROW;
            g.setColor(new Color(24,30,32));g.fillRoundRect(13,ry,199,HOME_RECENT_ROW-3,4,4);
            BufferedImage sprite=a.itemId()>0?itemImage.apply(a.itemId()):null;
            if(sprite!=null && sprite.getWidth()>0) {
                double sc=Math.min(1.0,Math.min(24.0/sprite.getWidth(),24.0/sprite.getHeight()));
                int iw=(int)Math.round(sprite.getWidth()*sc), ih=(int)Math.round(sprite.getHeight()*sc);
                g.drawImage(sprite,16+(24-iw)/2,ry+2+(24-ih)/2,iw,ih,null);
            }
            Color c=a.kind()==LiveSessionLedger.ActivityKind.LOOT_ESTIMATE?GOLD:
                a.kind()==LiveSessionLedger.ActivityKind.XP||a.kind()==LiveSessionLedger.ActivityKind.LEVEL_UP?MINT:TEXT;
            String when=ago(now,a.atMillis());
            int ww=g.getFontMetrics(new Font("Dialog",Font.PLAIN,9)).stringWidth(when);
            clipped(g,a.text(),44,ry+12,c,206-44-ww-6);
            label(g,when,208-ww,ry+12,9,MUTED,false);
            label(g,elideSessionText(g,a.detail(),160),44,ry+24,9,MUTED,false);
        }
    }
    private void home(Graphics2D g) {
        if (flowHomeEnabled()) {flowHome(g);return;}
        g.setColor(BG);g.fillRect(0,121,WIDTH,Math.max(760,homeBottom()+crewShiftForTest()));
        if(crewFeed==null) {realHome(g);return;}
        paintCrewHome(g);
        Graphics2D old=(Graphics2D)g.create();
        try {old.translate(0,crewShiftForTest());realHome(old);} finally {old.dispose();}
    }
    /** Compact R01 armory cards, every numerical value comes from observed in-session data. */
    // UI-R02: measured readable Home cards. The default/small legacy geometry remains
    // unchanged for historic hitbox tests. Large/Extra Large uses a separate, scrolling
    // layout which wraps real source text rather than silently eliding it.
    private boolean flowHomeEnabled(){return uiScale==UiScale.LARGE||uiScale==UiScale.EXTRA_LARGE;}
    private final java.util.Map<String,Rectangle> homeFlowActions=new HashMap<>();
    private record FlowLine(String text,Color color) {}
    private FlowLine flowLine(String text,Color color){return new FlowLine(text==null?"Not available":text,color);}
    private java.util.List<String> flowWrap(Graphics2D g,String source,int maxWidth,Font font){
        g.setFont(font);FontMetrics fm=g.getFontMetrics();
        String input=source==null?"Unavailable":source;
        java.util.List<String> output=new ArrayList<>();
        for(String paragraph:input.split("\\n",-1)) {
            if(paragraph.isBlank()){output.add("");continue;}
            StringBuilder line=new StringBuilder();
            for(String word:paragraph.split("\\s+")) {
                if(word.isEmpty())continue;
                String option=line.isEmpty()?word:line+" "+word;
                if(fm.stringWidth(option)<=maxWidth){line.setLength(0);line.append(option);continue;}
                if(!line.isEmpty()){output.add(line.toString());line.setLength(0);}
                if(fm.stringWidth(word)<=maxWidth){line.append(word);continue;}
                StringBuilder part=new StringBuilder();
                for(int i=0;i<word.length();i++){
                    char c=word.charAt(i);
                    if(!part.isEmpty()&&fm.stringWidth(part.toString()+c)>maxWidth){output.add(part.toString());part.setLength(0);}
                    part.append(c);
                }
                line.append(part);
            }
            if(!line.isEmpty())output.add(line.toString());
        }
        return output;
    }
    private int flowCard(Graphics2D g,int y,String heading,java.util.List<FlowLine> lines,String action){
        Font titleFont=uiFont(10,true), bodyFont=uiFont(10,false);
        java.util.List<String> titles=flowWrap(g,heading,187,titleFont);
        java.util.List<java.util.List<String>> wrapped=new ArrayList<>();
        int titleStep=Math.max(15,g.getFontMetrics(titleFont).getHeight());
        int bodyStep=Math.max(16,g.getFontMetrics(bodyFont).getHeight()+2);
        int bodyCount=0;
        for(FlowLine line:lines){var w=flowWrap(g,line.text(),181,bodyFont);wrapped.add(w);bodyCount+=Math.max(1,w.size());}
        int h=16+titles.size()*titleStep+8+bodyCount*bodyStep+10;
        card(g,y,h);
        int by=y+13+g.getFontMetrics(titleFont).getAscent();
        for(String t:titles){label(g,t,17,by,10,GOLD,true);by+=titleStep;}
        by+=7;
        for(int i=0;i<lines.size();i++){
            for(String t:wrapped.get(i)){
                label(g,t,19,by,10,lines.get(i).color(),false);
                by+=bodyStep;
            }
        }
        if(action!=null)homeFlowActions.put(action,new Rectangle(9,y,207,h));
        return y+h+10;
    }
    /** Owner-reference crew portraits; measured cards and hitboxes at true RuneLite width. */
    private int flowCrewCard(Graphics2D g,int y,int crew,String heading,java.util.List<FlowLine> lines,String action){
        Font head=uiFont(10,true), body=uiFont(10,false);
        int step=Math.max(16,g.getFontMetrics(body).getHeight()+3);
        int headingStep=Math.max(16,g.getFontMetrics(head).getHeight());
        java.util.List<String> titles=flowWrap(g,heading,128,head);
        java.util.List<java.util.List<String>> content=new ArrayList<>();
        int count=0;for(var l:lines){var split=flowWrap(g,l.text(),124,body);content.add(split);count+=Math.max(1,split.size());}
        int h=Math.max(91,17+titles.size()*headingStep+8+count*step+12);
        card(g,y,h);
        g.setColor(new Color(116,86,47));g.fillRoundRect(10,y+5,3,h-10,3,3);
        FeralUiArt.crew(g,crew,17,y+17,51,Math.min(70,h-24));
        int by=y+14+g.getFontMetrics(head).getAscent();
        for(String t:titles){label(g,t,76,by,10,GOLD,true);by+=headingStep;}
        by+=7;
        for(int i=0;i<lines.size();i++){
            int rowTop=by-g.getFontMetrics(body).getAscent();
            for(String t:content.get(i)){
                label(g,t,78,by,10,lines.get(i).color(),false);by+=step;
            }
            if(crew==1&&i<3&&crewFeed!=null&&i<crewFeed.banditHunts().size())
                homeFlowActions.put("bandit:"+i,new Rectangle(76,rowTop,130,Math.max(step,by-rowTop)));
        }
        if(action!=null)homeFlowActions.put(action,new Rectangle(13,y,199,h));
        return y+h+10;
    }
    private int flowAction(Graphics2D g,int y,String title,String id){
        Font f=uiFont(11,true);
        java.util.List<String> parts=flowWrap(g,title,175,f);
        int step=Math.max(16,g.getFontMetrics(f).getHeight());
        int h=Math.max(34,parts.size()*step+13);
        g.setColor(new Color(42,37,27));g.fillRoundRect(13,y,199,h,4,4);
        g.setColor(accent());g.drawRoundRect(13,y,198,h-1,4,4);
        int by=y+g.getFontMetrics(f).getAscent()+6;
        for(String part:parts){label(g,part,22,by,11,TEXT,true);by+=step;}
        homeFlowActions.put(id,new Rectangle(13,y,199,h));
        return y+h+7;
    }
    private int flowHome(Graphics2D g){
        homeFlowActions.clear();
        int y=130;
        java.util.List<FlowLine> adventure=new ArrayList<>();
        String status=activeSessionState==SessionState.ACTIVE?"ACTIVE":activeSessionState==SessionState.SUSPENDED?"PAUSED":"OFFLINE";
        adventure.add(flowLine("Session: "+status,activeSessionState==SessionState.ACTIVE?MINT:MUTED));
        adventure.add(flowLine("XP gained: "+homeSignalsForTest().get(0),TEXT));
        adventure.add(flowLine("Verified boss KC: "+homeSignalsForTest().get(1),TEXT));
        adventure.add(flowLine("Weapon: "+(observedWeapon.state()==LiveWeapon.State.OFFLINE?"Not observed":observedWeapon.displayName()),TEXT));
        y=flowCard(g,y,"YOUR ADVENTURE",adventure,null);
        if(crewFeed!=null){
            var lines=crewLinesForTest();
            y=flowCrewCard(g,y,0,"SPOONS TIP",java.util.List.of(flowLine(lines.get(0),TEXT)),null);
            if(crewFeed.spoons().isPresent()){
                y=flowAction(g,y,"✓ MARK TIP DONE","spoonsDone");
                y=flowAction(g,y,"× DISMISS TIP","spoonsDismiss");
            }
            java.util.List<FlowLine> hunts=new ArrayList<>();
            if(crewFeed.banditHunts().isEmpty())hunts.add(flowLine("Readiness unknown. Open your bank to check equipment.",MUTED));
            else for(var hunt:crewFeed.banditHunts().stream().limit(3).toList())
                hunts.add(flowLine("› "+hunt.bossId()+" · "+hunt.readiness().name().replace('_',' '),TEXT));
            y=flowCrewCard(g,y,1,"BANDIT'S BOSS HUNTS",hunts,null);
            y=flowCrewCard(g,y,2,"NOODLES' NEXT MOVE",java.util.List.of(flowLine(lines.get(2),TEXT)),"noodles");
        }
        java.util.List<FlowLine> objectives=new ArrayList<>();
        for(var line:homeObjectiveLines())objectives.add(flowLine(line.text(),line.kind()==HomeDirection.Line.Kind.WARN?GOLD:TEXT));
        if(objectives.isEmpty())objectives.add(flowLine("Open Pathfinder for account-based direction",MUTED));
        y=flowCard(g,y,"NEXT OBJECTIVE",objectives,"pathfinder");
        y=flowAction(g,y,"OPEN OUTFITTER  ›","outfitter");
        y=flowAction(g,y,"VIEW SESSION  ›","sessions");
        y=flowAction(g,y,"BOSS CHRONICLE  ›","chronicle");
        java.util.List<FlowLine> quests=new ArrayList<>();
        if(questDiscovery.total()>0){
            quests.add(flowLine(questDiscovery.completed()+" / "+questDiscovery.total()+" quests",MINT));
            quests.add(flowLine("Reported completed by RuneLite",MUTED));
        }else quests.add(flowLine("Quest status unavailable. Log in to scan quest progress.",MUTED));
        y=flowCard(g,y,"QUEST PROGRESS",quests,"pathfinder");
        java.util.List<FlowLine> recent=new ArrayList<>();
        if(recentActivity.isEmpty())recent.add(flowLine(activeSessionState==SessionState.OFFLINE?
            "Sign in to see this login's activity":"No verified XP, KC or drops yet",MUTED));
        else for(int i=0;i<Math.min(HOME_RECENT_ROWS,recentActivity.size());i++){
            var a=recentActivity.get(i);
            recent.add(flowLine(a.text()+" · "+a.detail()+" ("+ago(homeClock.getAsLong(),a.atMillis())+")",
                a.kind()==LiveSessionLedger.ActivityKind.LOOT_ESTIMATE?GOLD:TEXT));
        }
        y=flowCard(g,y,"RECENT ACTIVITY",recent,null);
        java.util.List<FlowLine> weekly=new ArrayList<>();
        for(String line:weeklyLinesForTest())weekly.add(flowLine(line,line.contains("not tracked")?MUTED:TEXT));
        y=flowCard(g,y,"WEEKLY PROGRESS",weekly,null);
        java.util.List<FlowLine> goals=new ArrayList<>();
        for(var l:homeGoalLines())goals.add(flowLine(l.text(),l.kind()==HomeDirection.Line.Kind.WARN?GOLD:TEXT));
        y=flowCard(g,y,"DROP GOALS",goals,null);
        java.util.List<FlowLine> upgrades=new ArrayList<>();
        for(var l:homeUpgradeLines())upgrades.add(flowLine(l.text(),l.kind()==HomeDirection.Line.Kind.WARN?GOLD:TEXT));
        y=flowCard(g,y,"BANK-BASED UPGRADES",upgrades,null);
        return y;
    }
    private void flowHomeClick(int x,int y){
        String action=null;
        // Actions are deliberately discovered from the latest measured layout,
        // never from stale hard-coded row offsets.
        for(var entry:homeFlowActions.entrySet())if(entry.getValue().contains(x,y)){action=entry.getKey();break;}
        if(action==null)return;
        if(action.startsWith("bandit:")){
            int n=Integer.parseInt(action.substring(7));
            if(crewFeed!=null&&n<crewFeed.banditHunts().size()){
                String boss=crewFeed.banditHunts().get(n).bossId();
                if(BurrowModel.availableBosses().contains(boss)){
                    activityMode=false;model.chooseBoss(boss);
                    BurrowModel.View previous=model.view();model.show(BurrowModel.View.OUTFITTER);
                    notifyViewChange(previous);notifySelection();revalidate();repaint();
                }
            }
            return;
        }
        BurrowModel.View old=model.view();
        switch(action){
            case "outfitter" -> model.show(BurrowModel.View.OUTFITTER);
            case "sessions" -> model.show(BurrowModel.View.SESSIONS);
            case "pathfinder","noodles" -> model.show(BurrowModel.View.PATHFINDER);
            case "chronicle" -> {openChronicle(BurrowModel.View.HOME);return;}
            case "spoonsDone" -> {crewClick(190,155);return;}
            case "spoonsDismiss" -> {crewClick(204,155);return;}
            default -> {return;}
        }
        notifyViewChange(old);revalidate();repaint();
    }

    private void realHome(Graphics2D g) {
        // R04: action-first dashboard, no second "Home" page heading and no duplicate session-boundary card.
        card(g,130,110);
        zoneTitle(g,"NEXT OBJECTIVE",145);
        paintDirectionLines(g,HomeDirection.objective(pathfinderObserved,pathfinderSuggestions),19,164,5,15);
        card(g,252,125);
        zoneTitle(g,"LIVE SIGNALS",269);
        String sessionStatus=activeSessionState==SessionState.ACTIVE?"ACTIVE":
            activeSessionState==SessionState.SUSPENDED?"PAUSED":"OFFLINE";
        Color sessionColor=activeSessionState==SessionState.ACTIVE?MINT:
            activeSessionState==SessionState.SUSPENDED?GOLD:MUTED;
        rightLabel(g,sessionStatus,203,269,9,sessionColor);
        g.setColor(new Color(42,50,51));g.fillRect(16,279,92,41);g.fillRect(115,279,92,41);
        small(g,"SESSION XP",23,292,MUTED);
        label(g,activeSessionState==SessionState.ACTIVE?"+"+sessionAmount(sessionTotalXp):"—",
            23,309,13,activeSessionState==SessionState.ACTIVE?MINT:GOLD,true);
        small(g,"BOSS KC +",122,292,MUTED);
        long kc=sessionKc.values().stream().mapToLong(Long::longValue).sum();
        label(g,sessionFeeds.get(SessionFeed.BOSS_KC)==FeedStatus.AVAILABLE?"+"+kc:"—",
            122,309,13,sessionFeeds.get(SessionFeed.BOSS_KC)==FeedStatus.AVAILABLE?MINT:GOLD,true);
        small(g,elideSessionText(g,homeSignalsForTest().get(2),186),19,331,MUTED);
        small(g,"CURRENT WEAPON",19,347,GOLD);
        clipped(g,observedWeapon.state()==LiveWeapon.State.OFFLINE?"Not observed":observedWeapon.displayName(),
            19,362,TEXT,181);
        zoneTitle(g,"QUICK ACTIONS",397);
        g.setColor(new Color(42,37,27));g.fillRect(13,409,199,26);
        g.setColor(accent());g.drawRect(13,409,198,25);
        label(g,"OPEN OUTFITTER   ›",19,426,10,TEXT,true);
        g.setColor(new Color(29,39,40));g.fillRect(13,446,199,26);
        g.setColor(LINE);g.drawRect(13,446,198,25);
        label(g,"VIEW SESSION   ›",19,463,10,TEXT,true);
        g.setColor(new Color(45,35,27));g.fillRect(13,483,199,26);
        g.setColor(accent());g.drawRect(13,483,198,25);
        label(g,"BOSS CHRONICLE   ›",19,500,10,TEXT,true);
        card(g,526,106);
        zoneTitle(g,"QUEST PROGRESS",543);
        if(questDiscovery.total()>0) {
            label(g,questDiscovery.completed()+" / "+questDiscovery.total()+" quests",19,566,13,MINT,true);
            small(g,"Reported completed by RuneLite",19,585,MUTED);
        } else {
            label(g,"Quest status unavailable",19,566,11,TEXT,true);
            small(g,"Log in to scan quest progress",19,585,MUTED);
        }
        small(g,"Open Pathfinder for observed blockers",19,614,GOLD);
        paintHomeRecent(g);
        paintHomeWeek(g);
        paintDirectionCard(g,homeWeekBottom()+10,"DROP GOALS",homeGoalLines());
        paintDirectionCard(g,homeGoalsBottom()+10,"BANK-BASED UPGRADES",homeUpgradeLines());
    }
    private void sampleHome(Graphics2D g) {
        g.setColor(new Color(136,51,39));g.fillRect(8,275,209,15);
        label(g,"LAYOUT EXAMPLE - NOT REAL ACCOUNT DATA",13,286,8,Color.WHITE,true);
        sectionBar(g,"CURRENT OBJECTIVE",309);
        focus(g,323,106,"Tombs of Amascut","Hunt the Osmumten's fang","Next: choose a verified loadout",true);
        sectionBar(g,"LATEST MILESTONE",468);
        row(g,482,"Quest completed","Song of the Elves",0);
        sectionBar(g,"LIVE FROM RUNELITE",551);
        signalRow(g,566,true);
        signalRow(g,620,false);
        g.setColor(new Color(150,49,39));g.drawLine(8,685,216,685);
        label(g,"SAMPLE DATA  •  NOT LIVE IN RUNE LITE",14,704,9,GOLD,true);
    }
    private void sectionBar(Graphics2D g,String label,int y) {
        g.setColor(new Color(25,31,30));g.fillRect(9,y-13,207,22);
        g.setColor(new Color(110,82,46));g.drawLine(9,y+9,216,y+9);
        g.setColor(new Color(185,132,59));g.fillRect(9,y-13,3,21);
        label(g,label,19,y+2,10,new Color(243,190,101),true);
    }
    private void focus(Graphics2D g,int y,int height,String title,String sub,String sub2,boolean hasProgress) {
        g.setPaint(new GradientPaint(11,y,new Color(48,41,31),214,y+height,new Color(21,29,29)));
        g.fillRect(11,y,203,height);
        g.setColor(new Color(144,105,52));g.drawRect(11,y,202,height-1);
        carved(g,title,19,y+37,title.length()>21?12:16,TEXT);
        label(g,sub,19,y+60,10,MUTED,false);
        label(g,sub2,19,y+75,10,MUTED,false);
        if(hasProgress) {
            g.setColor(new Color(48,54,47));g.fillRect(19,y+92,181,5);
            g.setColor(new Color(196,141,58));g.fillRect(19,y+92,51,5);
        } else {
            g.setColor(new Color(78,83,72));g.drawLine(19,y+91,199,y+91);
            label(g,"Awaiting verified account data",19,y+105,9,GOLD,false);
        }
    }
    private void row(Graphics2D g,int y,String label,String value,int icon) {
        Sprites.nav(g,icon,14,y,38,38);
        label(g,label,60,y+15,9,GOLD,true);
        clipped(g,value,60,y+31,TEXT,145);
        g.setColor(new Color(70,65,50));g.drawLine(12,y+41,213,y+41);
    }
    private void signalRow(Graphics2D g,int y,boolean weapon) {
        Sprites.signal(g,weapon,15,y,29,29);
        String title=weapon?"EQUIPPED WEAPON":"COMBAT LEVEL";
        String value;
        if(weapon) value=observedWeapon.state()==LiveWeapon.State.OFFLINE?
                "Sign in to inspect":observedWeapon.displayName();
        else {String lv=calculatedCombat();value=lv==null?
                (observedCombat.state()==LiveCombat.State.OFFLINE?"Sign in to inspect":observedCombat.status()):"Level "+lv;}
        label(g,title,54,y+10,9,GOLD,true);
        clipped(g,value,54,y+26,observedCombat.state()==LiveCombat.State.OBSERVED?MINT:TEXT,148);
        g.setColor(new Color(74,66,50));g.drawLine(13,y+42,212,y+42);
    }
    private void leather(Graphics2D g,int x,int y,int w,int h) {
        g.setPaint(new GradientPaint(x,y,new Color(48,43,32),x+w,y+h,new Color(23,28,28)));
        g.fillRect(x,y,w,h);
        g.setColor(new Color(177,121,51));g.drawRect(x,y,w-1,h-1);
        g.setColor(new Color(73,58,40));g.drawRect(x+3,y+3,w-7,h-7);
        g.setColor(new Color(224,159,70));
        for(int i=0;i<2;i++) {g.fillRect(x+6+i*(w-14),y+6,2,2);g.fillRect(x+6+i*(w-14),y+h-8,2,2);}
        g.setColor(new Color(73,59,38));g.drawLine(x+11,y+29,x+w-11,y+29);
    }
    private void section(Graphics2D g,String text,int baseline) {
        label(g,text,13,baseline,11,GOLD,true);
        g.setColor(new Color(125,89,43));
        FontMetrics fm=g.getFontMetrics();int x=Math.min(217,19+fm.stringWidth(text));
        g.drawLine(x,baseline-4,215,baseline-4);
    }
    private void chapter(Graphics2D g,String number,String text,int x,int y) {
        g.setColor(new Color(134,90,38));g.fillRect(x,y-13,24,18);
        label(g,number,x+5,y,10,new Color(252,221,154),true);
        label(g,text,x+30,y,11,GOLD,true);
    }
    private void progress(Graphics2D g,int x,int y,int w,int h,double ratio) {
        g.setColor(new Color(11,14,14));g.fillRect(x,y,w,h);
        g.setColor(new Color(116,88,47));g.drawRect(x,y,w-1,h-1);
        g.setPaint(new GradientPaint(x,y,new Color(92,174,112),x,y+h,new Color(43,101,80)));
        g.fillRect(x+2,y+2,(int)Math.round((w-4)*Math.max(0,Math.min(1,ratio))),h-4);
    }
    private void liveBox(Graphics2D g,int y) {
        leather(g,7,y,211,112);
        drawSignal(g,18,y+26,"WEAPON",observedWeapon.state()==LiveWeapon.State.OFFLINE?"Sign in to inspect":observedWeapon.displayName(),
                observedWeapon.state()==LiveWeapon.State.EQUIPPED);
        g.setColor(new Color(90,70,44));g.drawLine(17,y+53,208,y+53);
        String level=calculatedCombat();
        drawSignal(g,18,y+81,"COMBAT",level==null?(observedCombat.state()==LiveCombat.State.OFFLINE?"Sign in to inspect":observedCombat.status()):"Level " + level + "  •  observed",
                level!=null);
    }
    private String calculatedCombat() {
        if(observedCombat.state()!=LiveCombat.State.OBSERVED)return null;
        int atk=observedCombat.level(0),str=observedCombat.level(1),def=observedCombat.level(2);
        int hp=observedCombat.level(3),rng=observedCombat.level(4),mag=observedCombat.level(5),pray=observedCombat.level(6);
        double base=0.25*(def+hp+Math.floor(pray/2.0));
        double style=0.325*Math.max(atk+str,Math.max(Math.floor(rng*1.5),Math.floor(mag*1.5)));
        return Integer.toString((int)Math.floor(base+style));
    }
    private void drawSignal(Graphics2D g,int x,int y,String title,String value,boolean live) {
        g.setColor(new Color(23,28,28));g.fillRect(x,y-17,29,29);
        g.setColor(new Color(111,88,47));g.drawRect(x,y-17,28,28);
        Sprites.nav(g,title.equals("WEAPON")?2:3,x+2,y-16,25,25);
        label(g,title,x+38,y-8,9,GOLD,true);
        clipped(g,value,x+38,y+8,live?MINT:TEXT,151);
    }
    private void carved(Graphics2D g,String word,int x,int y,int fontSize,Color color) {
        g.setFont(new Font("Serif",Font.BOLD,fontSize));
        g.setColor(Color.BLACK);g.drawString(word,x+1,y+1);
        g.setColor(color);g.drawString(word,x,y);
    }
    private void frame(Graphics2D g,int x,int y,int w,int h,boolean hero) {
        g.setColor(hero?new Color(39,34,27):new Color(30,32,30));g.fillRect(x,y,w,h);
        g.setColor(new Color(114,80,39));g.drawRect(x,y,w-1,h-1);
        g.setColor(new Color(65,54,36));g.drawRect(x+2,y+2,w-5,h-5);
        g.setColor(hero?GOLD:new Color(160,110,46));g.drawLine(x+5,y+1,x+w-6,y+1);
    }
    private void tag(Graphics2D g,String s,int x,int y) {
        g.setColor(new Color(125,90,39));g.fillRect(x,y-13,3,13);
        label(g,s,x+9,y,11,GOLD,true);
    }
    // UI-R02: measured Pathfinder at Large/Extra Large, including its source
    // provenance, goals, actions, and filtering controls. Normal keeps legacy
    // mouse coordinates until all legacy tests can migrate to layout rectangles.
    private boolean flowPathfinderEnabled(){return uiScale!=UiScale.COMPACT;}
    private final java.util.Map<String,Rectangle> pathfinderFlowActions=new HashMap<>();
    private int flowPfCard(Graphics2D g,int y,String heading,java.util.List<FlowLine> body,String action){
        int top=y;int next=flowCard(g,y,heading,body,null);
        if(action!=null)pathfinderFlowActions.put(action,new Rectangle(9,top,207,next-top-10));
        return next;
    }
    private int flowPfButton(Graphics2D g,int y,String label,String action){
        int top=y, next=flowAction(g,y,label,action);
        homeFlowActions.remove(action);
        pathfinderFlowActions.put(action,new Rectangle(13,top,199,next-top-7));
        return next;
    }
    private int flowPathfinder(Graphics2D g){
        pathfinderFlowActions.clear();int y=128;
        y=flowPfCard(g,y,"PLAN YOUR NEXT STEP",java.util.List.of(flowLine("Quest · bank · skill evidence",MUTED)),null);
        boolean observed=pathfinderObserved!=null;
        boolean bank=observed && pathfinderObserved.bankStatus()==SuggestionEngine.BankStatus.VERIFIED_SAME_ACCOUNT;
        java.util.List<FlowLine> status=java.util.List.of(
            flowLine("Account: "+(observed?(pathfinderObserved.mode()==SuggestionEngine.Mode.MAIN?"MAIN":"IRONMAN"):"NOT LOGGED IN"),TEXT),
            flowLine("Outfitter: "+displayBossModel().boss()+" · "+displayBossModel().selectedStyle(),TEXT),
            flowLine(observed?(bank?"Bank: observed this login (positive only)":"Bank: open bank to use owned items"):
                "Bank: not observed",bank?MINT:MUTED),
            flowLine(questDiscovery.complete()?"Quest scan: observed":"Quest scan: incomplete / unknown",
                questDiscovery.complete()?MINT:GOLD));
        y=flowPfCard(g,y,"ACCOUNT STATUS",status,null);
        java.util.List<FlowLine> qs=new ArrayList<>();
        if(questDiscovery.total()>0){
            qs.add(flowLine(questDiscovery.completed()+" / "+questDiscovery.total()+" complete",MINT));
            qs.add(flowLine(questDiscovery.inProgress()+" in progress · "+questDiscovery.scanned()+" scanned",MUTED));
            qs.add(flowLine(questDiscovery.failures()+" scan errors",questDiscovery.failures()==0?MINT:BAD));
        }else qs.add(flowLine("Awaiting RuneLite quest scan · no states inferred",MUTED));
        y=flowPfCard(g,y,"QUEST DISCOVERY",qs,null);
        y=flowPfButton(g,y,"TYPE: "+focusLabel(pathfinderFocus)+"  ▾","focus");
        y=flowPfButton(g,y,"TIME: "+pathfinderMinutes+" MIN  ▾","minutes");
        y=flowPfButton(g,y,"ATTENTION: "+pathfinderAttention.name()+"  ▾","attention");
        if(!observed){
            y=flowPfCard(g,y,"AWAITING VERIFIED ACCOUNT",java.util.List.of(
                flowLine("Quest and Slayer data required for account-based suggestions.",MUTED)),null);
        }else if(pathfinderSuggestions.isEmpty()){
            java.util.List<FlowLine> msg=new ArrayList<>();
            if(pathfinderActiveFallback>0){
                msg.add(flowLine(focusLabel(pathfinderFocus)+" has none marked "+pathfinderAttention.name(),MUTED));
                msg.add(flowLine(pathfinderActiveFallback+" available under ACTIVE",MINT));
            }else msg.add(flowLine("Try TYPE ALL, more time, or ACTIVE attention.",MUTED));
            y=flowPfCard(g,y,"NO MATCHING ACTIVITIES",msg,null);
        }else{
            for(int i=0;i<Math.min(15,pathfinderSuggestions.size());i++){
                var suggestion=pathfinderSuggestions.get(i);
                String read=switch(suggestion.readiness()){
                    case MET -> suggestion.readyNow()?"PREREQUISITE OBSERVED":"PREREQ OK · TRAVEL TIME ?";
                    case UNMET -> "KNOWN PREREQ MISSING";
                    case UNKNOWN -> "READINESS UNVERIFIED";};
                java.util.List<FlowLine> details=java.util.List.of(
                    flowLine(read,suggestion.readyNow()?MINT:GOLD),
                    flowLine("Requirement: "+suggestion.requirement(),MUTED),
                    flowLine("Why: "+suggestion.why(),MUTED));
                y=flowPfSuggestion(g,y,suggestion,i);
            }
        }
        if(pathfinderSuggestions.size()>15)y=flowPfCard(g,y,"MORE MATCHES",java.util.List.of(
            flowLine((pathfinderSuggestions.size()-15)+" further matches. Refine TYPE, TIME, or ATTENTION to narrow the list.",MUTED)),null);
        var selected=selectedSuggestion();
        if(selected.isPresent()){
            var s=selected.get();
            java.util.List<FlowLine> data=new ArrayList<>();
            data.add(flowLine("Readiness: "+s.readiness()+" · "+(s.readyNow()?"Ready now":"Check start requirements"),GOLD));
            data.add(flowLine("Why: "+s.why(),TEXT));
            var tags=PathfinderActivityTags.classify(s);
            if(tags.dual()) data.add(flowLine("Combined: "+tags.note(),MUTED));
            data.add(flowLine("Time: "+PathfinderActivityTags.timeTooltip(s),MUTED));
            if(!s.missing().isEmpty())data.add(flowLine("Missing: "+String.join(", ",s.missing()),BAD));
            if(s.nextStep()!=null&&!s.nextStep().isBlank())data.add(flowLine("Next: "+s.nextStep(),MINT));
            for(String unknown:s.unverified())data.add(flowLine("Check: "+unknown,GOLD));
            data.add(flowLine("Source: "+s.source(),MUTED));
            y=flowPfCard(g,y,"SELECTED STEP: "+s.title(),data,null);
            if(s.outfitterBoss()!=null)y=flowPfButton(g,y,"OPEN SELECTED BOSS IN OUTFITTER","openboss");
            y=flowPfButton(g,y,s.goal()==SuggestionEngine.GoalMark.SAVED?"UNSAVE GOAL":"SAVE GOAL","save");
            y=flowPfButton(g,y,"SKIP THIS GOAL","skip");
            y=flowPfButton(g,y,"MARK GOAL DONE","done");
        }
        var L=pathfinderLayout();
        java.util.List<FlowLine> coverage=new ArrayList<>();
        for(String line:L.coverageLines())coverage.add(flowLine(line,MUTED));
        y=flowPfCard(g,y,"GUIDE COVERAGE / SOURCES",coverage,null);
        var preset=gg.feral.burrow.pathfinder.DropChance.PRESETS.get(0);
        double p=preset.p();
        java.util.List<FlowLine> odds=java.util.List.of(
            flowLine("1/"+preset.denominator()+" "+preset.label(),TEXT),
            flowLine("25 kc "+gg.feral.burrow.pathfinder.DropChance.percent(gg.feral.burrow.pathfinder.DropChance.atLeastOne(p,25))+
                " · 75 kc "+gg.feral.burrow.pathfinder.DropChance.percent(gg.feral.burrow.pathfinder.DropChance.atLeastOne(p,75)),MINT),
            flowLine("150 kc "+gg.feral.burrow.pathfinder.DropChance.percent(gg.feral.burrow.pathfinder.DropChance.atLeastOne(p,150))+
                " · 50% by "+gg.feral.burrow.pathfinder.DropChance.killsForConfidence(p,.5)+" · 90% by "+
                gg.feral.burrow.pathfinder.DropChance.killsForConfidence(p,.9),MINT),
            flowLine("Chance, not a guarantee; no KC assumed",MUTED),
            flowLine("Source: ironman.guide drop calculator",MUTED));
        y=flowPfCard(g,y,"DROP ODDS · INDEPENDENT ROLLS",odds,null);
        for(int i=0;i<L.goals().size();i++){
            var sum=L.goals().get(i);
            String kc=sum.kc().known()?"KC "+String.format(Locale.ROOT,"%,d",sum.kc().value().getAsLong())+
                (sum.kc().source()==RareDropStore.Source.MANUAL?" (manual)":"")+
                " · now "+RareDropMath.percent(sum.chanceNow().getAsDouble()):"KC unknown";
            y=flowPfCard(g,y,"RARE DROP GOAL · "+sum.encounter().encounter(),java.util.List.of(
                flowLine(sum.mode()+" of "+sum.selected().size(),TEXT),
                flowLine(kc,sum.kc().known()?MINT:MUTED),flowLine(sum.milestoneText(2),GOLD),
                flowLine("Tap to open in Outfitter",MUTED)),"goal:"+i);
        }
        return y;
    }
    /** PF-R04 selected goal is independent of currently focused/inspected detail. */
    private void togglePathfinderCheckbox(SuggestionEngine.Suggestion suggestion) {
        if(suggestion==null || activeAccountModeHash==-1L || pathfinderObserved==null ||
            !pathfinderObserved.verifiedIdentity())return; // never save for another/unknown account
        var next=suggestion.goal()==SuggestionEngine.GoalMark.SAVED?null:SuggestionEngine.GoalMark.SAVED;
        pathfinderGoalAction.accept(activeAccountModeHash,suggestion.id(),next);
    }
    private void pfCheckbox(Graphics2D g,int x,int y,boolean saved){
        g.setColor(saved?new Color(68,102,72):new Color(25,30,29));
        g.fillRoundRect(x,y,16,16,3,3);
        g.setColor(saved?MINT:accent());g.drawRoundRect(x,y,16,16,3,3);
        if(saved){g.setStroke(new BasicStroke(2f));g.drawLine(x+3,y+8,x+7,y+12);
            g.drawLine(x+7,y+12,x+13,y+4);g.setStroke(new BasicStroke(1f));}
    }
    /** Host-supplied authentic RuneLite skill icons; null leaves a safe local pictogram. */
    private java.util.function.Function<PathfinderActivityTags.Skill, BufferedImage> activityIconImages = ignored -> null;
    public void setActivityIconImages(java.util.function.Function<PathfinderActivityTags.Skill, BufferedImage> resolver) {
        activityIconImages=Objects.requireNonNull(resolver);repaint();
    }
    private void pfIcon(Graphics2D g,PathfinderActivityTags.Skill skill,int x,int y){
        BufferedImage authentic=activityIconImages.apply(skill);
        if(authentic!=null){
            Object interpolation=g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(authentic,x,y,19,19,null);
            if(interpolation!=null)g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,interpolation);
            return;
        }
        Color color= switch(skill){
            case QUEST,CLUE -> new Color(227,182,105);
            case FLETCHING,RANGED,HUNTER,FARMING,HERBLORE -> new Color(121,189,125);
            case MAGIC,RUNECRAFT,PRAYER -> new Color(131,167,229);
            case COMBAT,SLAYER,MELEE -> new Color(208,130,116);
            default -> new Color(191,176,141);};
        g.setColor(new Color(27,34,34));g.fillRoundRect(x,y,19,19,4,4);
        g.setColor(color);g.drawRoundRect(x,y,18,18,4,4);
        // Carefully bounded 16px original pictograms, no invented OSRS item IDs.
        switch(skill){
            case QUEST,CLUE -> {g.drawRect(x+6,y+4,8,11);g.drawLine(x+7,y+7,x+12,y+7);g.drawLine(x+7,y+10,x+12,y+10);}
            case COMBAT,SLAYER,MELEE -> {g.drawLine(x+4,y+4,x+14,y+15);g.drawLine(x+14,y+4,x+4,y+15);}
            case FLETCHING,RANGED -> {g.drawLine(x+3,y+14,x+15,y+4);g.drawLine(x+11,y+4,x+15,y+4);g.drawLine(x+15,y+4,x+15,y+8);}
            case FARMING,HERBLORE,HUNTER -> {g.drawLine(x+9,y+15,x+9,y+6);g.drawArc(x+4,y+5,8,6,0,170);g.drawArc(x+7,y+7,8,6,0,-170);}
            case MAGIC,RUNECRAFT,PRAYER -> {g.drawOval(x+6,y+6,7,7);g.drawLine(x+9,y+3,x+9,y+5);g.drawLine(x+9,y+14,x+9,y+16);}
            case MINING,SMITHING,CONSTRUCTION -> {g.drawLine(x+5,y+5,x+13,y+13);g.drawLine(x+4,y+8,x+12,y+4);}
            default -> {g.drawOval(x+5,y+5,9,9);g.drawLine(x+9,y+7,x+9,y+12);}
        }
    }
    /** Compact source-faithful chip: time is session window unless a named Wiki example exists. */
    private void pfMetadata(Graphics2D g,SuggestionEngine.Suggestion s,int x,int baseline,int maxWidth){
        var t=PathfinderActivityTags.classify(s);
        // label sits to the right of icon(s); no unsupported claims about skill XP.
        pfIcon(g,t.primary(),x,baseline-13);
        int at=x+23;
        if(t.dual()){pfIcon(g,t.secondary(),at,baseline-13);at+=23;}
        String info=t.primary().name().replace('_',' ')+(t.dual()?" + "+t.secondary().name().replace('_',' '):"");
        Font ft=uiFont(9,false);FontMetrics fm=g.getFontMetrics(ft);g.setFont(ft);g.setColor(MUTED);
        if(fm.stringWidth(info)>maxWidth-(at-x))info=t.dual()?switch(t.relation()){
            case SECONDARY_SUPPLIES -> "XP + MATERIALS";
            case SECONDARY_XP -> "TWO SKILL XP";
            case COMBAT_METHOD -> "SLAYER METHOD";
            default -> "TWO ACTIVITIES";
        }:t.primary().name();
        g.drawString(info,at,baseline);
    }
    private int flowPfSuggestion(Graphics2D g,int top,SuggestionEngine.Suggestion s,int index){
        Font title=uiFont(10,true), body=uiFont(10,false), caption=uiFont(9,false);
        var titles=flowWrap(g,(index+1)+". "+s.title(),157,title);
        int ts=Math.max(15,g.getFontMetrics(title).getHeight());
        int bs=Math.max(16,g.getFontMetrics(body).getHeight()+2);
        var req=flowWrap(g,"Requirement: "+s.requirement(),181,body);
        var why=flowWrap(g,"Why: "+s.why(),181,body);
        int metaH=Math.max(20,g.getFontMetrics(caption).getHeight()+4);
        int metaYPreview=top+12+g.getFontMetrics(title).getAscent()+titles.size()*ts+3;
        int statusY=metaYPreview+metaH+35;
        int h=statusY-top+bs*(req.size()+why.size())+18;
        boolean checked=s.goal()==SuggestionEngine.GoalMark.SAVED;
        g.setColor(checked?new Color(41,41,28):new Color(24,29,30));g.fillRoundRect(9,top,207,h,5,5);
        g.setColor(checked?GOLD:s.id().equals(pathfinderSelectedId)?new Color(149,130,89):LINE);
        g.drawRoundRect(9,top,206,h-1,5,5);
        pfCheckbox(g,17,top+12,checked);
        int lineY=top+12+g.getFontMetrics(title).getAscent();
        for(String line:titles){label(g,line,42,lineY,10,TEXT,true);lineY+=ts;}
        int metaY=lineY+3;
        pfMetadata(g,s,17,metaY+11,186);
        String time=PathfinderActivityTags.timeLabel(s);
        label(g,time,42,metaY+metaH+11,9,GOLD,false);
        int y=metaY+metaH+35;
        String status=s.readyNow()?"PREREQUISITE OBSERVED":s.readiness()==SuggestionEngine.State.UNMET?
            "KNOWN PREREQ MISSING":s.readiness()==SuggestionEngine.State.UNKNOWN?"READINESS UNVERIFIED":"PREREQ OK · TRAVEL ?";
        label(g,status,17,y,9,s.readyNow()?MINT:s.readiness()==SuggestionEngine.State.UNMET?BAD:GOLD,false);
        y+=bs;
        for(String str:req){label(g,str,17,y,10,MUTED,false);y+=bs;}
        for(String str:why){label(g,str,17,y,10,MUTED,false);y+=bs;}
        pathfinderFlowActions.put("inspect:"+index,new Rectangle(9,top,207,h));
        pathfinderFlowActions.put("check:"+index,new Rectangle(14,top+8,23,24));
        return top+h+10;
    }
    // Test helpers expose the *actual painted* checkbox and card hit regions.
    Rectangle pathfinderCheckboxRectForTest(int i){return pathfinderFlowActions.get("check:"+i);}
    Rectangle pathfinderInspectRectForTest(int i){return pathfinderFlowActions.get("inspect:"+i);}
    /** Returns the currently painted focus/attention/goal action hit rectangle, not old hardcoded coordinates. */
    Rectangle pathfinderActionRectForTest(String id){getPreferredSize();return pathfinderFlowActions.get(id);}
    void clickPathfinderActionForTest(String id){
        Rectangle r=pathfinderActionRectForTest(id);
        if(r==null)throw new IllegalArgumentException("No visible Pathfinder action: "+id);
        clickAt(id.startsWith("inspect:")?90:r.x+Math.min(45,r.width/2),r.y+Math.min(12,r.height/2)+CONTENT_SHIFT);
    }

    private void flowPathfinderClick(int x,int y){
        // Explicit checkbox hit has priority over the containing card, regardless of HashMap order.
        for(int i=0;i<Math.min(15,pathfinderSuggestions.size());i++){
            Rectangle checkbox=pathfinderFlowActions.get("check:"+i);
            if(checkbox!=null&&checkbox.contains(x,y)){
                togglePathfinderCheckbox(pathfinderSuggestions.get(i));repaint();return;
            }
        }
        String action=null;
        for(var rect:pathfinderFlowActions.entrySet())if(!rect.getKey().startsWith("check:")&&rect.getValue().contains(x,y)){
            action=rect.getKey();break;
        }
        if(action==null)return;
        if(action.startsWith("inspect:")||action.startsWith("suggestion:")){
            int i=Integer.parseInt(action.substring(action.startsWith("inspect:")?8:11));
            if(i>=0&&i<pathfinderSuggestions.size()){
                var s=pathfinderSuggestions.get(i);pathfinderSelectedId=s.id();
                pathfinderDetail=s.why()+" | "+s.requirement()+" | "+s.source();
            }
        }else if(action.startsWith("goal:")){
            int i=Integer.parseInt(action.substring(5));var goals=pathfinderLayout().goals();
            if(i>=0&&i<goals.size()){
                String boss=goals.get(i).encounter().boss();
                if(BurrowModel.availableBosses().contains(boss)){
                    activityMode=false;model.chooseBoss(boss);rareDropExpanded=true;styleFeedback=null;
                    BurrowModel.View old=model.view();model.show(BurrowModel.View.OUTFITTER);notifyViewChange(old);notifySelection();
                }
            }
        }else switch(action){
            case "focus" -> {int i=java.util.Arrays.asList(FOCUS_ORDER).indexOf(pathfinderFocus);
                pathfinderFocus=FOCUS_ORDER[(i+1)%FOCUS_ORDER.length];rebuildPathfinderSuggestions();}
            case "minutes" -> {int[] cyc={60,15,30,45,90,120};int i=0;
                while(i<cyc.length&&cyc[i]!=pathfinderMinutes)i++;
                pathfinderMinutes=cyc[(i+1)%cyc.length];rebuildPathfinderSuggestions();}
            case "attention" -> { // UX order is ACTIVE -> LOW -> AFK -> ACTIVE, independent of enum storage order.
                pathfinderAttention=switch(pathfinderAttention){
                    case ACTIVE -> SuggestionEngine.Attention.LOW;
                    case LOW -> SuggestionEngine.Attention.AFK;
                    case AFK -> SuggestionEngine.Attention.ACTIVE;
                };rebuildPathfinderSuggestions();}
            case "openboss" -> {var selected=selectedSuggestion();
                if(selected.isPresent()){
                    String boss=selected.get().outfitterBoss();
                    if(boss!=null&&BurrowModel.availableBosses().contains(boss)){
                        activityMode=false;model.chooseBoss(boss);BurrowModel.View old=model.view();
                        model.show(BurrowModel.View.OUTFITTER);notifyViewChange(old);
                    }
                }}
            case "save","skip","done" -> {var selected=selectedSuggestion();
                if(selected.isPresent()&&activeAccountModeHash!=-1L){
                    var mark=action.equals("save")?(selected.get().goal()==SuggestionEngine.GoalMark.SAVED?
                        null:SuggestionEngine.GoalMark.SAVED):action.equals("skip")?
                        SuggestionEngine.GoalMark.SKIPPED:SuggestionEngine.GoalMark.DONE;
                    pathfinderGoalAction.accept(activeAccountModeHash,selected.get().id(),mark);
                }}
            default -> {}
        }
        revalidate();repaint();
    }

    private void pathfinder(Graphics2D g) {
        if(flowPathfinderEnabled()){flowPathfinder(g);return;}
        small(g,"PLAN YOUR NEXT STEP",13,145,GOLD);
        small(g,"Quest · bank · skill evidence",13,159,MUTED);
        g.setColor(accent());g.drawLine(12,165,213,165);
        card(g,178,106);
        zoneTitle(g,"ACCOUNT STATUS · "+(pathfinderObserved==null?"NOT LOGGED IN":
            pathfinderObserved.mode()==SuggestionEngine.Mode.MAIN?"MAIN":"IRONMAN"),197);
        clipped(g,"Outfitter: "+displayBossModel().boss()+" • "+displayBossModel().selectedStyle(),19,219,TEXT,181);
        small(g,pathfinderObserved==null?"Bank: not observed":
            pathfinderObserved.bankStatus()==SuggestionEngine.BankStatus.VERIFIED_SAME_ACCOUNT?
            "Bank: observed this login (positive only)":"Bank: open bank to use owned items",19,238,
            pathfinderObserved!=null && pathfinderObserved.bankStatus()==SuggestionEngine.BankStatus.VERIFIED_SAME_ACCOUNT?MINT:MUTED);
        small(g,questDiscovery.complete()?"Quest scan: observed":"Quest scan: incomplete / unknown",19,258,
            questDiscovery.complete()?MINT:GOLD);
        card(g,297,112);
        zoneTitle(g,"QUEST DISCOVERY",314);
        if(questDiscovery.total()>0) {
            label(g,questDiscovery.completed()+" / "+questDiscovery.total()+" complete",19,340,15,MINT,true);
            double ratio=(double)questDiscovery.completed()/questDiscovery.total();
            progress(g,19,350,184,11,ratio);
            small(g,questDiscovery.inProgress()+" in progress  •  "+questDiscovery.scanned()+" scanned",19,379,MUTED);
            small(g,questDiscovery.failures()+" scan errors",19,397,questDiscovery.failures()==0?MINT:BAD);
        } else {
            label(g,"Awaiting RuneLite quest scan",19,341,11,TEXT,true);
            small(g,"No quest states inferred",19,371,MUTED);
        }
        zoneTitle(g,"SUGGESTIONS",425);
        // Build 20: category dropdown + minutes dropdown, then an ACTIVE / LOW / AFK segmented control.
        dropdownBox(g,13,431,110,22,"TYPE  "+focusLabel(pathfinderFocus)+" ▾");
        dropdownBox(g,126,431,85,22,pathfinderMinutes+" MIN ▾");
        SuggestionEngine.Attention[] seg={SuggestionEngine.Attention.ACTIVE,SuggestionEngine.Attention.LOW,SuggestionEngine.Attention.AFK};
        for(int i=0;i<seg.length;i++) {
            int sx=13+i*66; boolean on=pathfinderAttention==seg[i];
            g.setColor(on?new Color(64,54,32):new Color(30,35,36));g.fillRoundRect(sx,459,66,22,4,4);
            g.setColor(on?accent():LINE);g.drawRoundRect(sx,459,i==2?65:66,22,4,4);
            String t=seg[i].name();
            g.setFont(new Font("SansSerif",Font.BOLD,10));
            int w=g.getFontMetrics().stringWidth(t);
            g.setColor(on?accent():MUTED);g.drawString(t,sx+(66-w)/2,474);
        }
        if(pathfinderObserved==null) {
            card(g,505,93);zoneTitle(g,"AWAITING VERIFIED ACCOUNT",523);
            small(g,"Quest and Slayer data required",19,550,MUTED);
            small(g,"for account-based suggestions.",19,569,MUTED);
        } else if(pathfinderSuggestions.isEmpty()) {
            card(g,505,93);zoneTitle(g,"NO MATCHING ACTIVITIES",523);
            if(pathfinderActiveFallback>0) {
                small(g,focusLabel(pathfinderFocus)+" has none marked "+pathfinderAttention.name(),19,550,MUTED);
                small(g,pathfinderActiveFallback+" available under ACTIVE",19,569,MINT);
            } else {
                small(g,"Try TYPE ALL or more time",19,550,MUTED);
                small(g,"Nothing fits these filters.",19,569,MUTED);
            }
        } else {
            for(int i=0;i<Math.min(3,pathfinderSuggestions.size());i++) {
                var suggestion=pathfinderSuggestions.get(i);
                int y=505+i*95;
                card(g,y,91);
                String title=suggestion.title();
                pfCheckbox(g,17,y+7,suggestion.goal()==SuggestionEngine.GoalMark.SAVED);
                pfIcon(g,PathfinderActivityTags.classify(suggestion).primary(),39,y+5);
                clipped(g,(i+1)+". "+title,63,y+19,TEXT,143);
                String state=switch(suggestion.readiness()) {
                    case MET -> suggestion.readyNow()?"PREREQUISITE OBSERVED":"PREREQ OK · TRAVEL TIME ?";
                    case UNMET -> "KNOWN PREREQ MISSING";
                    case UNKNOWN -> "READINESS UNVERIFIED";
                };
                clipped(g,state,19,y+37,suggestion.readyNow()?MINT:GOLD,180);
                clipped(g,PathfinderActivityTags.classify(suggestion).primary().name()+" · "+
                    PathfinderActivityTags.timeLabel(suggestion),19,y+53,GOLD,182);
                clipped(g,"Req: "+suggestion.requirement(),19,y+69,MUTED,181);
                // Full why, source and multi-activity labels appear in the inspect detail.
                clipped(g,"Why: "+suggestion.why(),19,y+83,MUTED,181);
                if(suggestion.id().equals(pathfinderSelectedId)) {
                    g.setColor(GOLD);g.drawRoundRect(10,y,205,90,4,4);
                }
            }
        }
        if(selectedSuggestion().map(x->x.outfitterBoss()!=null).orElse(false)) {
            button(g,"OPEN SELECTED BOSS IN OUTFITTER",pathfinderActionY(),true);
        } else {
            g.setColor(new Color(30,37,37));g.fillRect(13,pathfinderActionY(),199,30);
            clipped(g,pathfinderSuggestions.isEmpty()?"Change the filters above":
                pathfinderSelectedId==null?"Tap a suggestion for its source":pathfinderDetail,
                18,pathfinderActionY()+19,MUTED,185);
        }
        pathfinderExtras(g);
    }
    // Activity loadouts use the same 225px shell but don't pretend to be a verified bank plan.
    // Painting and preferred-height calculation share the exact text-measuring implementation.
    private int activityWrapped(Graphics2D g,String text,int x,int top,int width,Color ink,boolean strong,boolean draw){
        Font font=uiFont(10,strong);FontMetrics fm=g.getFontMetrics(font);
        int step=Math.max(15,fm.getHeight()+3);
        java.util.List<String> lines=wrapItemName(text,fm,width);
        if(draw){g.setFont(font);g.setColor(themed(ink));
            for(int i=0;i<lines.size();i++)g.drawString(lines.get(i),x,top+fm.getAscent()+i*step);}
        return lines.size()*step;
    }
    private int activityListCard(Graphics2D g,int top,String heading,java.util.List<String> entries,boolean draw){
        int y=top+31;
        for(String row:entries)y+=activityWrapped(g,row,19,y,183,TEXT,false,false)+3;
        int h=y-top+9;
        if(draw){card(g,top,h);label(g,heading,19,top+21,11,GOLD,true);
            int pos=top+31;
            for(String row:entries){
                Color ink=row.startsWith("?")?MUTED:TEXT;
                pos+=activityWrapped(g,row,19,pos,183,ink,false,true)+3;
            }
        }
        return top+h+11;
    }
    private static final int[] ACT_EQUIPMENT_COLS={1,0,1,2,0,1,2,1,0,1,2};
    private static final int[] ACT_EQUIPMENT_ROWS={0,1,1,1,2,2,2,3,4,4,4};
    private int activityEquipmentTop=-1,activityInventoryTop=-1,activityBankGridTop=-1;
    /** Render the owner's Wiki Bank Tag Layout export as an 8-by-7 BANK template, not inventory. */
    private int paintClueBankLayout(Graphics2D g,ClueBankTagLayouts.Layout layout,boolean draw){
        activityEquipmentTop=-1;activityInventoryTop=-1;
        if(draw){
            if(spaciousOutfitter()){
                outfitterSharedSelect(g,true,layout.title());
                label(g,"GEAR SOURCE",17,253,9,GOLD,true);
                outFullButton(g,262,34,"WIKI REFERENCE",true,false);
                outFullButton(g,302,34,"BEST BANK  ·  NOT CONNECTED",false,true);
            }else{
                g.setColor(new Color(72,52,29));g.fillRoundRect(13,130,95,32,5,5);
                g.setColor(accent());g.drawRoundRect(13,130,95,32,5,5);
                label(g,"‹ BOSSES",20,151,10,GOLD,true);
                label(g,"ACTIVITY KITS",115,150,10,GOLD,true);
                label(g,"CLUES · WIKI BANK TEMPLATE",16,188,9,MINT,true);
                dropdownBox(g,13,202,199,33,"Wiki · "+layout.title()+" ▾");
            }
        }
        int y=spaciousOutfitter()?352:244;
        int titleH=activityWrapped(g,layout.section(),19,y+42,183,TEXT,true,false);
        int introH=90+titleH;
        if(draw){card(g,y,introH);
            label(g,"SOURCE-FAITHFUL BANK TAG",19,y+18,10,GOLD,true);
            activityWrapped(g,layout.section(),19,y+27,183,TEXT,true,true);
            label(g,"NOT 28-SLOT INVENTORY",19,y+introH-31,9,GOLD,true);
            label(g,"OWNERSHIP / COUNTS UNKNOWN",19,y+introH-14,9,MUTED,true);
        }
        y+=introH+12;
        activityBankGridTop=y;
        if(draw){card(g,y,267);label(g,"BANK LAYOUT · 8 x 7",19,y+20,11,GOLD,true);}
        int[] slots=layout.slots();
        for(int i=0;i<56;i++){
            int itemId=slots[i],sx=16+(i%8)*24,sy=y+35+(i/8)*29;
            if(!draw)continue;
            g.setColor(itemId>0?new Color(60,53,38):new Color(27,33,33));
            g.fillRoundRect(sx,sy,23,27,3,3);
            g.setColor(itemId>0?accent():LINE);
            g.drawRoundRect(sx,sy,23,27,3,3);
            if(itemId>0){
                BufferedImage sprite=itemImage.apply(itemId);
                if(sprite!=null)g.drawImage(sprite,sx+1,sy+2,21,21,null);
                else {g.setFont(uiFont(7,false));g.setColor(TEXT);g.drawString("#"+itemId%1000,sx+1,sy+16);}
            }
        }
        y+=279;
        java.util.List<String> guidance=new ArrayList<>();
        guidance.add(layout.count()+" source-positioned item references · duplicates retained.");
        guidance.add("Icons: RuneLite ItemManager when available; no generated item sprites.");
        guidance.add("Empty squares are Wiki bank template spaces; not empty pack slots.");
        guidance.add("These exports contain no quantities, bank ownership or step-specific gear requirements.");
        guidance.add("For Wilderness steps, review item risk before taking teleports or valuables.");
        guidance.add("Source: "+ClueBankTagLayouts.SOURCE);
        return activityListCard(g,y,"READ BEFORE PACKING",guidance,draw)+6;
    }
    /** Activity icons use the native ItemManager resolver; never synthesize an item ID. */
    private void activityItemIcon(Graphics2D g,String kitId,boolean equipment,int index,int x,int y,String fallback){
        // The diagram AND bank projection now consume the same immutable 11+28 plan.
        var plan=SharedOutfitterSetup.activity(kitId).plan();
        int position=equipment?Outfitter.GEAR_POSITIONS.get(index):Outfitter.inventoryPosition(index);
        var reference=plan.cells().get(position).requirement();
        BufferedImage sprite=reference==null?null:itemImage.apply(reference.itemId());
        if(sprite!=null)g.drawImage(sprite,x,y,26,26,null);
        else FeralUiArt.activityGlyph(g,fallback,x,y+3);
    }
    private int paintActivityOutfitter(Graphics2D g,boolean draw){
        ActivityOutfitterCatalog.Kit kit=ActivityOutfitterCatalog.find(activityKitId);
        ClueBankTagLayouts.Layout bankLayout=ClueBankTagLayouts.find(activityKitId);
        if(bankLayout!=null)return paintClueBankLayout(g,bankLayout,draw);
        activityBankGridTop=-1;
        if(draw){
            if(spaciousOutfitter()){
                outfitterSharedSelect(g,true,kit.title());
                label(g,"GEAR SOURCE",17,253,9,GOLD,true);
                outFullButton(g,262,34,"WIKI REFERENCE",true,false);
                outFullButton(g,302,34,ActivityOutfitterCatalog.isWilderness(kit.id())?
                    "BEST BANK  ·  LOCKED":"BEST BANK  ·  NOT CONNECTED",false,true);
            }else{
                g.setColor(new Color(72,52,29));g.fillRoundRect(13,130,95,32,5,5);
                g.setColor(accent());g.drawRoundRect(13,130,95,32,5,5);
                label(g,"‹ BOSSES",20,151,10,GOLD,true);
                label(g,"ACTIVITY KITS",115,150,10,GOLD,true);
                label(g,kit.category(),16,188,10,MINT,true);
                dropdownBox(g,13,202,199,33,kit.title()+" ▾");
            }
        }
        int y=spaciousOutfitter()?352:244;
        int descH=activityWrapped(g,kit.description(),19,y+25,183,TEXT,false,false)+87;
        if(draw){card(g,y,descH);label(g,"FERAL FIELD PREP",19,y+18,11,GOLD,true);
            activityWrapped(g,kit.description(),19,y+25,183,TEXT,false,true);
            label(g,ActivityOutfitterCatalog.isWilderness(kit.id())?
                "WILDERNESS · HIGH RISK":"REFERENCE KIT",19,y+descH-34,9,
                ActivityOutfitterCatalog.isWilderness(kit.id())?GOLD:MUTED,true);
            label(g,ActivityOutfitterCatalog.isWilderness(kit.id())?
                "BEST IN BANK LOCKED":"BANK NOT CHECKED",19,y+descH-16,9,GOLD,true);}
        y+=descH+12;
        if(draw){card(g,y,310);label(g,"EQUIPMENT · 11 SLOTS",19,y+20,11,GOLD,true);}
        activityEquipmentTop=y;
        String[] labels={"HEAD","CAPE","NECK","AMMO","WEAPON","BODY","SHIELD","LEGS","GLOVES","BOOTS","RING"};
        if(draw)for(int i=0;i<11;i++){
            var slot=kit.equipment().get(i);
            boolean planned=slot!=null;
            int sx=14+ACT_EQUIPMENT_COLS[i]*66,sy=y+37+ACT_EQUIPMENT_ROWS[i]*51;
            g.setColor(planned?new Color(72,57,32):new Color(29,35,35));
            g.fillRoundRect(sx,sy,61,45,4,4);
            g.setColor(planned?accent():LINE);g.drawRoundRect(sx,sy,61,45,4,4);
            label(g,labels[i],sx+4,sy+13,7,planned?TEXT:MUTED,false);
            if(planned){
                activityItemIcon(g,kit.id(),true,i,sx+4,sy+17,slot.label());
                boolean wikiSlayer=SlayerWikiGear.find(kit.id())!=null;
                var sourceSlot=wikiSlayer?SlayerWikiGear.find(kit.id()).slot(i):null;
                boolean hasWikiIcon=sourceSlot!=null && sourceSlot.exampleItemId()>0;
                label(g,hasWikiIcon?"WIKI":(wikiSlayer?"RISK":(slot.conditional()?"? CHECK":"KIT")),
                    sx+23,sy+33,7,hasWikiIcon?MINT:GOLD,true);
            }else label(g,"EMPTY",sx+5,sy+33,7,MUTED,true);
        }
        y+=322;
        java.util.List<String> equipment=new ArrayList<>();
        for(int i=0;i<kit.equipment().size();i++){
            var entry=kit.equipment().get(i);
            if(entry!=null)equipment.add((entry.conditional()?"? ":"")+labels[i]+" · "+entry.label());
        }
        y=activityListCard(g,y,"EQUIPMENT DETAILS",equipment,draw);
        // Display actual Wiki ranked columns; the sprite is only one illustrative choice.
        // This section preserves within-cell / and > alternatives and non-equipment special attacks.
        var slayerTable=SlayerWikiGear.find(kit.id());
        if(slayerTable!=null){
            java.util.List<String> ranked=new ArrayList<>();
            for(int i=0;i<11;i++){
                var option=slayerTable.slot(i);
                if(option==null){if(i==6)ranked.add("SHIELD · Empty (selected weapon is two-handed)");continue;}
                ranked.add(option.slot()+" · "+option.displayRanks());
            }
            if(!slayerTable.specialWeaponColumns().isEmpty())
                ranked.add("SPECIAL ATTACK SWITCHES · "+String.join(" | ",slayerTable.specialWeaponColumns()));
            ranked.add("Illustrated items are Wiki examples, NOT a bank scan or ownership claim.");
            if(ActivityOutfitterCatalog.isWilderness(kit.id()))
                ranked.add("HIGH RISK: never automatic Bank matching or Best in Bank; no safe-item guarantees.");
            y=activityListCard(g,y,"WIKI GEAR RANKINGS",ranked,draw);
        }
        activityInventoryTop=y;
        if(draw){card(g,y,368);label(g,"INVENTORY · 28 SLOTS",19,y+20,11,GOLD,true);}
        for(int i=0;i<28;i++){
            int sx=15+(i%4)*49,sy=y+36+(i/4)*46;
            boolean planned=i<kit.inventory().size();
            if(draw){g.setColor(planned?new Color(65,54,36):new Color(29,35,35));
                g.fillRoundRect(sx,sy,44,41,4,4);g.setColor(planned?accent():LINE);
                g.drawRoundRect(sx,sy,44,41,4,4);
                label(g,String.format(java.util.Locale.ROOT,"%02d",i+1),sx+4,sy+13,8,planned?GOLD:MUTED,true);
                if(planned){
                    var slot=kit.inventory().get(i);
                    activityItemIcon(g,kit.id(),false,i,sx+11,sy+13,slot.label());
                    if(slot.conditional())label(g,"?",sx+35,sy+12,9,GOLD,true);
                }
            }
        }
        y+=380;
        java.util.List<String> inventory=new ArrayList<>();
        for(int i=0;i<kit.inventory().size();i++){
            var entry=kit.inventory().get(i);
            inventory.add((entry.conditional()?"? ":"")+String.format(java.util.Locale.ROOT,"%02d",i+1)+" · "+entry.label());
        }
        y=activityListCard(g,y,"PACKING LIST",inventory,draw);
        java.util.List<String> notes=new ArrayList<>();
        notes.add("? = conditional suggestion, not a confirmed requirement.");
        notes.add("Real sprites identify source examples; never prove ownership or quantities.");
        if(ActivityOutfitterCatalog.isWilderness(kit.id()))
            notes.add("LOCKED: no Best in Bank, no owned-gear substitution or auto-upgrade in Wilderness.");
        notes.addAll(kit.notes());
        notes.add("Source: "+kit.sourceUrl());
        notes.add("No automatic equipping, inventory movement, bank ownership, or quest unlock assumptions.");
        y=activityListCard(g,y,"CHECK BEFORE YOU GO",notes,draw);
        return y+6;
    }
    private void outfitter(Graphics2D g) {
        if(activityMode){paintActivityOutfitter(g,true);return;}
        if(chronicleOpen){paintChronicle(g);return;}
        var profile=displayBossModel().profile();
        var entry=displayBossModel().bossEntry();
        int source=gearSourceY();
        if(spaciousOutfitter()) {
            outfitterSharedSelect(g,false,displayBossModel().boss());
            outFullButton(g,outfitterLogY(),32,"OPEN BOSS CHRONICLE  ↗",false,false);
            if(hasSpecialMethods()){
                label(g,"COMBAT METHOD",17,outfitterMethodY()-7,9,GOLD,true);
                outFullButton(g,outfitterMethodY(),38,"METHOD  ·  "+methodRowLabel()+"  ▾",false,false);
            }
            label(g,"EQUIPMENT TIER",17,tierRowY()-9,9,GOLD,true);
            for(int i=0;i<3;i++){
                Tier tier=Tier.values()[i];int y=tierRowY()+i*39;
                boolean available=TieredOutfitterCatalog.find(displayBossModel().boss(),displayBossModel().method(),tier)!=null;
                outFullButton(g,y,35,tier.label(),tier==displayBossModel().tier(),!available);
            }
            label(g,"GEAR SOURCE",17,gearSourceY()-8,9,GOLD,true);
            boolean wild=!BossRegistry.bestBankAllowed(displayBossModel().boss());
            boolean draft=selectedMethodIsResearchGear(), noGear=displayBossModel().profile()==null;
            outFullButton(g,gearSourceY(),34,draft?"RESEARCH GEAR · CHECK":noGear?"STRATEGY NOTES":"WIKI RECOMMENDED",!bankModeActive(),false);
            outFullButton(g,gearSourceY()+40,34,wild?"BEST BANK  ·  LOCKED":draft?"BEST BANK · CHECK":noGear?"BEST BANK · NO GEAR":"BEST IN BANK",bankModeActive(),wild||draft||noGear);
        } else {
        g.setColor(new Color(43,43,38));g.fillRoundRect(13,BOSS_ROW_Y,132,33,5,5);
        g.setColor(LINE);g.drawRoundRect(13,BOSS_ROW_Y,132,33,5,5);
        clipped(g,displayBossModel().boss(),20,BOSS_ROW_Y+21,TEXT,104);
        label(g,"▾",130,BOSS_ROW_Y+22,12,GOLD,true);
        g.setColor(new Color(57,43,28));g.fillRoundRect(CHRONICLE_X,BOSS_ROW_Y,63,33,5,5);
        g.setColor(GOLD);g.drawRoundRect(CHRONICLE_X,BOSS_ROW_Y,63,33,5,5);
        label(g,uiScale==UiScale.EXTRA_LARGE||uiScale==UiScale.LARGE?"LOG ↗":"CHRONICLE ↗",
            CHRONICLE_X+3,BOSS_ROW_Y+21,8,GOLD,true);
        if(hasSpecialMethods()) {
            int y=BOSS_ROW_Y+39;
            g.setColor(new Color(43,43,38));g.fillRoundRect(13,y,199,29,5,5);
            g.setColor(LINE);g.drawRoundRect(13,y,199,29,5,5);
            clipped(g,"SPECIAL · "+methodRowLabel(),20,y+18,TEXT,165);
            label(g,"▾",193,y+19,12,GOLD,true);
        }
        small(g,"TIER",17,tierRowY()-6,GOLD);
        // ACT-R01: visible switch without moving any legacy boss hitboxes.
        g.setColor(new Color(48,59,50));g.fillRoundRect(112,tierRowY()-17,100,16,4,4);
        g.setColor(accent());g.drawRoundRect(112,tierRowY()-17,100,16,4,4);
        label(g,"ACTIVITIES ↗",117,tierRowY()-5,9,GOLD,true);
        for(int i=0;i<3;i++) {
            Tier tier=Tier.values()[i];int x=13+i*66, y=tierRowY();
            boolean available=TieredOutfitterCatalog.find(displayBossModel().boss(),displayBossModel().method(),tier)!=null;
            boolean selected=displayBossModel().tier()==tier;
            g.setColor(selected?new Color(77,57,34):available?PANEL:new Color(28,31,31));
            g.fillRoundRect(x,y,64,31,5,5);
            g.setColor(selected?GOLD:available?LINE:new Color(62,65,64));g.drawRoundRect(x,y,64,31,5,5);
            String tierName=tier.label();
            // UI-R02: never shrink the requested Large/XL text to 7px just
            // to squeeze "Recommended" into a fixed 64px tier button.
            if(i==1 && (uiScale==UiScale.LARGE||uiScale==UiScale.EXTRA_LARGE)) {
                Font tierFace=uiFont(8,selected);
                g.setFont(tierFace);g.setColor(available?TEXT:MUTED);
                String first="Recom",last="mended";
                FontMetrics fm=g.getFontMetrics();
                g.drawString(first,x+(64-fm.stringWidth(first))/2,y+13);
                g.drawString(last,x+(64-fm.stringWidth(last))/2,y+26);
            } else {
                int tierFont=i==1?8:scaled(10);
                g.setFont(new Font("Dialog",selected?Font.BOLD:Font.PLAIN,tierFont));
                while(tierFont>7 && g.getFontMetrics().stringWidth(tierName)>59) {
                    tierFont--;g.setFont(new Font("Dialog",selected?Font.BOLD:Font.PLAIN,tierFont));
                }
                int tx=x+(64-g.getFontMetrics().stringWidth(tierName))/2;
                g.setColor(available?TEXT:MUTED);g.drawString(tierName,tx,y+20);
            }
        }
        g.setColor(new Color(22,26,27));g.fillRoundRect(13,source,199,21,4,4);
        boolean bankOn=bankModeActive(), wildBlock=!BossRegistry.bestBankAllowed(displayBossModel().boss());
        boolean researchBlocked=!selectedMethodHasBankPlan();
        g.setColor(bankOn?LINE:GOLD);g.drawRoundRect(13,source,96,21,4,4);
        g.setColor(bankOn?GOLD:wildBlock?new Color(62,65,64):LINE);g.drawRoundRect(112,source,100,21,4,4);
        label(g,"WIKI",42,source+15,10,bankOn?MUTED:GOLD,!bankOn);
        label(g,wildBlock?"BANK OFF":researchBlocked?"BANK CHECK":"BEST BANK",wildBlock?133:127,source+15,9,
            bankOn?GOLD:wildBlock||researchBlocked?BAD:MUTED,bankOn);
        }
        int afterSource=source+(spaciousOutfitter()?79:29);
        if(showsWildernessTravelWarning()) {
            Graphics2D warning=(Graphics2D)g.create();warning.translate(0,afterSource-397);kbdWildernessWarning(warning);warning.dispose();
            afterSource+=KBD_WARNING_SHIFT;
        }
        if(bankModeActive() && profile!=null) {
            Graphics2D status=(Graphics2D)g.create();status.translate(0,afterSource-397);bankStatusCard(status);status.dispose();
        }
        int top=equipmentY();
        if(styleFeedback!=null) small(g,elideSessionText(g,styleFeedback,185),17,top-3,BAD);
        card(g,top,277);
        small(g,"EQUIPMENT",20,top+22,GOLD);
        String[] styles={"Melee","Ranged","Magic"};
        var selected=displayBossModel().selectedStyles();
        for(int i=0;i<3;i++) {
            CombatStyle cs=CombatStyle.values()[i];int x=87+i*42,y=top+6;
            boolean enabled=availableMethodForStyle(styles[i])!=null;
            boolean primary=selected.primary()==cs;
            boolean secondary=!primary&&selected.uses(cs);
            Color accent=new Color(cs.rgb());
            g.setColor(primary?blend(accent,PANEL,.40f):secondary?blend(accent,PANEL,.20f):PANEL);
            g.fillRoundRect(x,y,40,30,4,4);
            g.setColor(primary?accent:secondary?accent:enabled?LINE:new Color(66,70,69));
            g.drawRoundRect(x,y,39,29,4,4);
            // Real RuneLite item sprites when the host provides them, with an offline vector fallback.
            int spriteId=new int[]{4587,861,6889}[i]; // Scimitar, Magic shortbow, Mage's book: real OSRS item icons
            BufferedImage icon=itemImage.apply(spriteId);
            if(icon!=null)g.drawImage(icon,x+5,y+1,29,27,null);
            else if(i==2)spellbookIcon(g,x+8,y+3,24,23,enabled||primary||secondary?accent:MUTED);
            else styleIcon(g,cs,x+7,y+3,enabled||primary||secondary?accent:MUTED);
        }
        String[] slots={"HEAD","CAPE","NECK","AMMO","WEAPON","BODY","SHIELD","LEGS","GLOVES","BOOTS","RING"};
        int[] columns={1,0,1,2,0,1,2,1,0,1,2};
        int[] rows={0,1,1,1,2,2,2,3,4,4,4};
        if(profile!=null)for(int i=0;i<11;i++) {
            int x=29+columns[i]*62,y=top+43+rows[i]*46;
            // CP-1: an optional slot item the player does not own is drawn faded (Wiki reference, not owned stock).
            var tripPlanForCell=bankModeActive()?currentTripPlan():null;
            boolean faded=tripPlanForCell!=null && tripPlanForCell.gear().get(i).status()==gg.feral.burrow.core.BestBankPlanner.GearStatus.OPTIONAL_MISSING;
            java.awt.Composite priorComposite=g.getComposite();
            if(faded)g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,0.35f));
            itemCell(g,ownedGear(i),x,y,43,42,slots[i]);
            g.setComposite(priorComposite);
            gearBankOutline(g,i,x,y,43,42);
        } else {
            small(g,"Strategy only · gear list unverified",20,top+65,BAD);
            small(g,"Open Chronicle for sourced guidance",20,top+85,MUTED);
        }
        int inv=inventoryY();
        card(g,inv,335);
        small(g,bankModeActive()?"INVENTORY · WIKI / BANK CHECK":"INVENTORY · 28 POSITIONS",20,inv+20,GOLD);
        for(int i=0;i<28;i++) {
            int x=16+(i%4)*49,y=inv+31+(i/4)*42;
            itemCell(g,profile==null?null:inventoryItem(i),x,y,42,39,null);
            if(profile!=null)inventoryBankOutline(g,i,x,y,42,39);
            if(profile!=null && switchHint(i)!=null) {
                // CP-5 (BQA-R12-018): visual placement hint only; RuneLite/OSRS cannot reserve or lock slots.
                g.setColor(new Color(31,37,38));g.fillRoundRect(x+28,y+1,13,12,3,3);
                g.setColor(accent());g.drawRoundRect(x+28,y+1,13,12,3,3);
                label(g,"⇄",x+30,y+11,9,GOLD,true);
            }
        }
        paintRareDropsSummary(g);
    }
    /** Source-backed compact summary. Full KC, selected goals and historical evidence stay in Chronicle. */
    /** Distinct spellbook glyph for Magic, intentionally not an invented item sprite. */
    private void spellbookIcon(Graphics2D g,int x,int y,int w,int h,Color c) {
        g.setColor(new Color(65,44,99));g.fillRoundRect(x,y,w,h,3,3);
        g.setColor(c);g.drawRoundRect(x,y,w-1,h-1,3,3);
        g.drawLine(x+5,y+3,x+5,y+h-4);
        g.drawLine(x+8,y+5,x+w-4,y+5);
        g.drawLine(x+8,y+h-5,x+w-4,y+h-5);
        g.drawLine(x+13,y+7,x+13,y+h-8);
        g.drawLine(x+10,y+11,x+17,y+11);
    }
    private void paintRareDropsSummary(Graphics2D g) {
        int y=rareSectionY();card(g,y,83);
        label(g,"RARE DROPS",20,y+20,11,GOLD,true);
        label(g,rareDropExpanded?"▴ CLOSE":"▾ DETAILS",141,y+20,9,GOLD,true);
        var encounters=RareDropTable.encounters(displayBossModel().boss());
        var drops=encounters.stream().flatMap(e->e.drops().stream()).filter(d->!d.name().equals("Curved bone")).limit(2).toList();
        if(drops.isEmpty())small(g,"Not modelled · open Chronicle",20,y+42,MUTED);
        else for(int i=0;i<drops.size();i++) {
            var d=drops.get(i);int ry=y+40+i*18;
            // No invented sprites/rates: the exact registered item ID and Wiki rate are used.
            BufferedImage sprite=d.itemId()>0?itemImage.apply(d.itemId()):null;
            if(sprite!=null)g.drawImage(sprite,20,ry-11,14,14,null);
            small(g,elideSessionText(g,d.name(),114),sprite==null?20:37,ry,TEXT);
            rightLabel(g,d.rateText(),203,ry,9,MUTED);
        }
        if(rareDropExpanded)rareDropTracker(g);
    }
    private java.util.Set<CombatStyle> chronicleSuggestedStyles(String boss){
        java.util.EnumSet<CombatStyle> styles=java.util.EnumSet.noneOf(CombatStyle.class);
        for(String method:TieredOutfitterCatalog.methods(boss))
            styles.addAll(MethodStyles.of(boss,method).required());
        if(styles.isEmpty())BossRegistry.find(boss).ifPresent(entry->styles.addAll(entry.suggestedStyles()));
        return styles;
    }
    private static final String[] CHRONICLE_FILTERS={"All","Regular","Wilderness","Moons","Demi","Raids"};
    private static final String[] CHRONICLE_SORTS={"Featured","A-Z","Z-A","Easy → Hard","Hard → Easy"};
    private java.util.List<String> chronicleBrowseBosses(){
        java.util.List<String> names=new ArrayList<>(BurrowModel.availableBosses());
        names.addAll(gg.feral.burrow.core.RaidChronicle.names());
        names.removeIf(n->{
            if(chronicleBrowseGroup==0)return false;
            if(chronicleBrowseGroup==5)return !gg.feral.burrow.core.RaidChronicle.isRaid(n);
            return BossRegistry.find(n).map(e->e.group().ordinal()!=chronicleBrowseGroup-1).orElse(true);
        });
        Comparator<String> byName=String.CASE_INSENSITIVE_ORDER;
        if(chronicleBrowseSort==0){
            java.util.List<String> featured=java.util.List.of("Scurrius","Sarachnis","Bryophyta");
            names.sort(Comparator.comparingInt((String n)->featured.contains(n)?featured.indexOf(n):100)
                .thenComparing(byName));
        } else if(chronicleBrowseSort==1)names.sort(byName);
        else if(chronicleBrowseSort==2)names.sort(byName.reversed());
        else{
            int direction=chronicleBrowseSort==3?1:-1;
            names.sort(Comparator.comparingInt((String n)->BOSS_DIFFICULTY.containsKey(n)?0:1)
                .thenComparingInt(n->direction*BOSS_DIFFICULTY.getOrDefault(n,0)).thenComparing(byName));
        }
        return names;
    }
    /** A scrollable, source-grounded card browser. Search uses the existing native searchable picker.
     *  Artwork is only available for the three explicit owner reference portraits. */
    private int paintChronicleBrowser(Graphics2D g,boolean draw){
        chronicleBrowseActions.clear();
        if(draw){
            g.setColor(new Color(55,42,28));g.fillRoundRect(13,130,70,30,5,5);
            g.setColor(accent());g.drawRoundRect(13,130,70,30,5,5);
            label(g,"‹ BACK",18,149,9,GOLD,true);
            label(g,"BOSS CHRONICLE",91,150,10,GOLD,true);
            dropdownBox(g,13,168,199,31,"⌕ Search bosses…   ▾");
            dropdownBox(g,13,207,92,27,CHRONICLE_FILTERS[chronicleBrowseGroup]+" ▾");
            dropdownBox(g,112,207,100,27,CHRONICLE_SORTS[chronicleBrowseSort]+" ▾");
        }
        java.util.List<String> names=chronicleBrowseBosses();
        int y=246;
        Font nameFont=uiFont(11,true),infoFont=uiFont(9,false);
        int titleStep=Math.max(16,g.getFontMetrics(nameFont).getHeight()+1);
        int metaStep=Math.max(14,g.getFontMetrics(infoFont).getHeight()+1);
        for(String boss:names){
            var entry=BossRegistry.find(boss).orElse(null);
            boolean raid=gg.feral.burrow.core.RaidChronicle.isRaid(boss);
            java.util.List<String> titles=flowWrap(g,boss,130,nameFont);
            int headH=Math.max(101,16+titles.size()*titleStep+3*metaStep+12);
            int h=headH+48;
            if(draw){
                card(g,y,h);
                FeralUiArt.boss(g,boss,17,y+13,53,55,accent());
                int baseline=y+13+g.getFontMetrics(nameFont).getAscent();
                for(String t:titles){label(g,t,77,baseline,11,TEXT,true);baseline+=titleStep;}
                String category=raid?"Raid · reference only":entry==null?"Research pending":entry.group().label();
                label(g,category,78,baseline+1,9,MUTED,false);baseline+=metaStep;
                label(g,(raid?"No gear plans":difficultyLabel(boss)+" · approximate"),78,baseline+1,9,GOLD,false);
                var allowed=chronicleSuggestedStyles(boss);
                if(allowed.isEmpty())label(g,"Styles: unverified",78,y+headH-16,9,MUTED,false);
                else for(int i=0;i<3;i++){
                    CombatStyle style=CombatStyle.values()[i];int sx=78+i*33,sy=y+headH-27;
                    g.setColor(allowed.contains(style)?new Color(style.rgb()).darker():new Color(40,44,44));
                    g.fillRoundRect(sx,sy,28,19,4,4);
                    g.setColor(allowed.contains(style)?new Color(style.rgb()):LINE);
                    g.drawRoundRect(sx,sy,28,19,4,4);
                    label(g,new String[]{"ME","RA","MA"}[i],sx+5,sy+13,8,allowed.contains(style)?TEXT:MUTED,true);
                }
                g.setColor(LINE);g.drawLine(18,y+headH-2,205,y+headH-2);
                var all=RareDropTable.encounters(boss);
                var drop=all.stream().flatMap(e->e.drops().stream()).findFirst().orElse(null);
                if(drop!=null){
                    BufferedImage icon=itemImage.apply(drop.itemId());
                    if(icon!=null)g.drawImage(icon,19,y+headH+8,18,18,null);
                    else FeralUiArt.activityGlyph(g,drop.name(),20,y+headH+9);
                    clipped(g,drop.name(),42,y+headH+20,TEXT,111);
                    rightLabel(g,drop.rateText(),206,y+headH+20,9,GOLD);
                }else {
                    label(g,raid?"Raid drops not modelled":"Rare drops not modelled",20,y+headH+20,9,MUTED,false);
                }
                label(g,"›",197,y+headH+41,13,GOLD,true);
            }
            chronicleBrowseActions.put(boss,new Rectangle(13,y,199,h));
            y+=h+9;
        }
        if(draw){
            card(g,y,54);
            label(g,"Styles = Wiki reference, not readiness",18,y+19,9,GOLD,true);
            label(g,"Unknown rates and access stay unknown",18,y+39,9,MUTED,false);
        }
        return y+65;
    }
    /** Chronicle is a subview: it never modifies the four-tab model or the selected boss. */
    private void paintChronicle(Graphics2D g) {
        if(chronicleBrowse){paintChronicleBrowser(g,true);return;}
        g.setColor(new Color(56,43,28));g.fillRoundRect(13,130,56,30,5,5);
        g.setColor(accent());g.drawRoundRect(13,130,56,30,5,5);
        label(g,"‹ BACK",16,149,9,GOLD,true);
        g.setColor(PANEL);g.fillRoundRect(71,130,24,30,4,4);g.fillRoundRect(189,130,24,30,4,4);
        g.fillRoundRect(97,130,90,30,4,4);
        g.setColor(LINE);g.drawRoundRect(71,130,24,30,4,4);g.drawRoundRect(189,130,24,30,4,4);
        g.setColor(accent());g.drawRoundRect(97,130,90,30,4,4);
        label(g,"‹",79,150,16,GOLD,true);
        label(g,"›",198,150,16,GOLD,true);
        clipped(g,chronicleSelection(),102,150,TEXT,69);
        label(g,"▾",175,149,10,GOLD,true);
        if(chronicleRaid!=null){ small(g,"CHRONICLE · RAID ENCOUNTER",17,182,GOLD); raidCard(g); return; }
        small(g,"KC / DROPS",17,182,GOLD);
        g.setColor(new Color(50,39,27));g.fillRoundRect(134,165,78,31,5,5);
        g.setColor(accent());g.drawRoundRect(134,165,77,30,5,5);
        label(g,"☷ BROWSE",139,185,9,GOLD,true);
        rareDropTracker(g);
        combatTasksCard(g);
        int y=chronicleNotesY();
        if(displayBossModel().profile()!=null) {
            Graphics2D notes=(Graphics2D)g.create();notes.translate(0,y-PREPARATION_Y);
            paintPreparationAndBank(notes);notes.dispose();
        } else {
            var research=ResearchCombatMethods.find(displayBossModel().boss(),displayBossModel().method());
            Graphics2D notes=(Graphics2D)g.create();notes.translate(0,y-397);
            if(research!=null)researchStrategyCard(notes,research);
            else if(displayBossModel().bossEntry()!=null) {
                notes.translate(0,-10);guideNotVerifiedCard(notes,displayBossModel().bossEntry());
            }
            notes.dispose();
        }
    }
    /**
     * CP-5 (BQA-R12-018): a weapon or offhand carried as an in-fight switch (e.g. Scorching bow, Emberlight, defender
     * on Tormented Demons / Demonic Gorillas). Returns guidance text, or null. Never moves or locks real slots.
     */
    String switchHint(int inventoryIndex) {
        var profile=displayBossModel().profile();
        if(profile==null || inventoryIndex<0 || inventoryIndex>=28)return null;
        var it=profile.inventory().get(inventoryIndex);
        if(it==null || it.id()<=0)return null;
        var fact=SwitchFacts.FACTS.find(it.id());
        if(fact.isEmpty())return null;
        int slot=fact.get().equipmentSlot();
        if(slot!=ItemFacts.WEAPON && slot!=ItemFacts.SHIELD)return null;
        String role=slot==ItemFacts.WEAPON?(fact.get().hands()==ItemFacts.Hands.TWO?"two-handed weapon switch":"weapon switch"):"offhand switch";
        return "SWITCH · "+role+": keep it in this position for a quick swap (visual guide only; slots are not locked)";
    }
    /** Catalog-derived slot facts are immutable; build once (lazily) instead of on every paint. */
    private static final class SwitchFacts { static final ItemFacts FACTS=ItemFacts.catalogDerived(); }
    /** CP-5: raid card lines (text, colour, gap-after); height is derived from the wrapped content. */
    private java.util.List<Object[]> raidCardLines() {
        java.util.List<Object[]> out=new ArrayList<>();
        var entry=gg.feral.burrow.core.RaidChronicle.find(chronicleRaid).orElse(null);
        if(entry==null)return out;
        FontMetrics fm=getFontMetrics(PREPARATION_FONT);
        java.util.function.BiConsumer<String,Color> add=(t,c)->{for(String l:wrapItemName(t,fm,183))out.add(new Object[]{l,c});};
        add.accept("Part of: "+entry.raid(),MUTED); out.add(null);
        add.accept(gg.feral.burrow.core.RaidChronicle.UNSUPPORTED,GOLD); out.add(null);
        add.accept("Rare drops, KC and preparation for raids are not modelled in this build.",MUTED); out.add(null);
        add.accept("Reference: "+entry.wikiUrl().replace("https://",""),TEXT); out.add(null);
        add.accept("Use ‹ › or the picker to browse. Back keeps your Outfitter boss unchanged.",MUTED);
        return out;
    }
    private int raidCardHeight() {
        int h=24+18;
        for(Object[] l:raidCardLines()) h+=l==null?8:14;
        return h+10;
    }
    private static final int COMBAT_TASK_HEADER=30;
    private boolean combatTasksExpanded;
    boolean combatTasksExpandedForTest(){return combatTasksExpanded;}
    void toggleCombatTasksForTest(){combatTasksExpanded=!combatTasksExpanded;}
    int combatTasksHeightForTest(){return combatTasksHeight();}
    java.util.List<String> combatTaskLinesForTest(){
        java.util.List<String> out=new ArrayList<>();
        for(Object[] l:combatTaskLines()) if(l!=null) out.add((String)l[0]);
        return out;
    }
    /** Chronicle task lines (text, colour, bold); null is a gap. Wrapped to the same width as the raid card. */
    private java.util.List<Object[]> combatTaskLines() {
        java.util.List<Object[]> out=new ArrayList<>();
        FontMetrics fm=getFontMetrics(PREPARATION_FONT);
        var tasks=gg.feral.burrow.core.CombatTaskGuide.forBoss(displayBossModel().boss());
        // W06: make the provenance boundary visible, without hiding the legacy 290 rows.
        if(tasks.stream().anyMatch(t->!gg.feral.burrow.intelligence.ChronicleIntelligenceBridge.display(t).reviewed())) {
            for(String l:wrapItemName("RESEARCH CHECK · criteria not game-verified",fm,183))
                out.add(new Object[]{l,MUTED,false});
            out.add(null);
        }
        for(var task:tasks) {
            var checked=gg.feral.burrow.intelligence.ChronicleIntelligenceBridge.display(task);
            for(String l:wrapItemName(task.name(),fm,183))out.add(new Object[]{l,GOLD,true});
            out.add(new Object[]{task.tier()+" · "+task.type(),MUTED,false});
            for(String l:wrapItemName(checked.condition(),fm,183))out.add(new Object[]{l,TEXT,false});
            if(!checked.tip().isBlank())for(String l:wrapItemName("Tip: "+checked.tip(),fm,183))out.add(new Object[]{l,MUTED,false});
            out.add(null);
        }
        return out;
    }
    private int combatTasksHeight() {
        if(chronicleRaid!=null || gg.feral.burrow.core.CombatTaskGuide.forBoss(displayBossModel().boss()).isEmpty())return 0;
        if(!combatTasksExpanded)return COMBAT_TASK_HEADER;
        int h=COMBAT_TASK_HEADER+4;
        for(Object[] l:combatTaskLines()) h+=l==null?8:14;
        return h+4;
    }
    private void combatTasksCard(Graphics2D g) {
        int height=combatTasksHeight();
        if(height==0)return;
        int top=combatTasksY();
        card(g,top,height);
        int count=gg.feral.burrow.core.CombatTaskGuide.forBoss(displayBossModel().boss()).size();
        small(g,"COMBAT TASKS · "+count,20,top+19,GOLD);
        label(g,combatTasksExpanded?"▴ Hide":"▾ Show",166,top+19,10,GOLD,true);
        if(!combatTasksExpanded)return;
        int y=top+COMBAT_TASK_HEADER+12;
        for(Object[] l:combatTaskLines()) {
            if(l==null){y+=8;continue;}
            label(g,(String)l[0],20,y,10,(Color)l[1],(boolean)l[2]); y+=14;
        }
    }
    /** CP-5: raid encounter reference card. No gear, no Best in Bank, no invented drops or tips. */
    private void raidCard(Graphics2D g) {
        var entry=gg.feral.burrow.core.RaidChronicle.find(chronicleRaid).orElse(null);
        if(entry==null)return;
        card(g,205,raidCardHeight());
        int y=229;
        label(g,entry.name(),20,y,12,TEXT,true); y+=18;
        for(Object[] l:raidCardLines()) {
            if(l==null){y+=8;continue;}
            small(g,(String)l[0],20,y,(Color)l[1]); y+=14;
        }
    }
    private void paintPreparationAndBank(Graphics2D g) {
        if(displayBossModel().profile()==null)return;
        PreparationNotesLayout.Layout preparation=preparationLayout();
        card(g,PREPARATION_Y,preparation.height());
        small(g,"PREPARATION / SWITCHES",20,PREPARATION_Y+20,GOLD);
        int noteY=PREPARATION_Y+41;
        for(PreparationNotesLayout.Line line:preparation.lines()) {
            if(line.heading())label(g,line.text(),17,noteY,10,GOLD,true);
            else small(g,line.text(),17,noteY,TEXT);
            noteY+=PreparationNotesLayout.LINE_HEIGHT;
        }
        if(prepCollapsible()) {
            int hidden=preparationLayoutFull().lines().size()-PREP_PREVIEW_LINES;
            int ty=prepToggleY();
            g.setColor(new Color(31,37,38));g.fillRoundRect(13,ty,199,20,4,4);
            g.setColor(LINE);g.drawRoundRect(13,ty,199,20,4,4);
            label(g,prepExpanded?"▴ Show fewer notes":"▾ Show all notes (+"+hidden+" lines)",20,ty+14,10,GOLD,true);
        }
        int by=PREPARATION_Y+preparation.height()+9;
        card(g,by,106);
        small(g,"BANK · RESEARCH VIEW",20,by+19,GOLD);
        small(g,observedGearBank.isEmpty()?"No current bank evidence for this login":
            "Bank observed · account-wide only",20,by+42,MUTED);
        clipped(g,"Outfitter gear checks never follow Chronicle",20,by+62,MUTED,183);
        clipped(g,displayBossModel().boss()+" · no boss-specific bank fit here",20,by+78,TEXT,183);
        small(g,"Open Outfitter for gear and bank plans",20,by+94,GOLD);
    }
    // ---------------- Build 20 Rare Drop Tracker (Outfitter) ----------------
    private RareDropStore.State rareDropState; // null when signed out / unverified account
    private long rareDropHash=-1L;
    private boolean rareDropExpanded;
    private String rareDropFeedback;
    /** Explicit owner actions; the plugin re-checks the account on the client thread. */
    public interface RareDropAction {
        void toggle(long hash,String dropKey);
        void mode(long hash,String encounter,RareDropMath.Mode mode);
        /** value==null clears the manual entry. */
        void manualKc(long hash,String encounter,Long value);
        void hiscores(long hash,String encounter);
    }
    private RareDropAction rareDropAction=new RareDropAction(){
        public void toggle(long h,String k){} public void mode(long h,String e,RareDropMath.Mode m){}
        public void manualKc(long h,String e,Long v){} public void hiscores(long h,String e){}};
    public void setRareDropAction(RareDropAction a){rareDropAction=Objects.requireNonNull(a);}
    private java.util.function.Function<String,String> manualKcPrompt=null;
    void setManualKcPromptForTest(java.util.function.Function<String,String> prompt){manualKcPrompt=prompt;}
    /** EDT only. state==null means signed out: rates stay visible, nothing is saved or attributed. */
    public void setRareDropState(long hash,RareDropStore.State state) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Rare drop intake requires Swing EDT");
        rareDropHash=state==null?-1L:hash; rareDropState=state; revalidate(); repaint();
    }
    public void setRareDropFeedback(String text){rareDropFeedback=text;repaint();}
    RareDropStore.State rareDropStateForTest(){return rareDropState;}
    void setRareDropExpandedForTest(boolean e){rareDropExpanded=e;revalidate();}
    boolean rareDropExpanded(){return rareDropExpanded;}

    /**
     * B20.1: the method row describes what is actually SHOWN for the selected tier (style + that
     * tier's weapon), not the method's top-tier title. "Melee, full Inquisitor with mace" on the
     * Entry tier showed Inquisitor wording over different Entry gear. The full Wiki method name
     * stays in the dropdown and the tooltip.
     */
    String methodRowLabel() {
        if(displayBossModel().method().isBlank()) return "Unavailable";
        var p=displayBossModel().profile();
        if(p==null) return displayBossModel().method();
        String style=p.style()==null||p.style().isBlank()?"":p.style().split("[ ,/(]")[0];
        var gear=p.gear();
        var weapon=gear.size()>4?gear.get(4):null;
        if(weapon==null || weapon.name()==null || weapon.name().isBlank()) return displayBossModel().method();
        return (selectedMethodIsResearchGear()?"DRAFT · ":"") + (style.isBlank()?"":style+" · ")+weapon.name();
    }
    /** True when the selected tier comes from an OSRS Wiki strategy DRAFT (not the original catalog). */
    boolean isWikiDraft() {
        var sel=TieredOutfitterCatalog.find(displayBossModel().boss(),displayBossModel().method(),displayBossModel().tier());
        return sel!=null && sel.authority()==TieredOutfitterCatalog.Authority.TPT_CURATED_WIKI_STRATEGY
            && displayBossModel().method()!=null && displayBossModel().method().startsWith("Wiki · ");
    }
    private void rightLabel(Graphics2D g,String text,int right,int y,int size,Color c) {
        g.setFont(new Font("Dialog",Font.BOLD,scaled(size)));
        g.setColor(themed(c));g.drawString(text,right-g.getFontMetrics().stringWidth(text),y);
    }
    String methodCaption() {
        String tier=displayBossModel().tier().label();
        int n=displayBossModel().methods().size();
        return displayBossModel().profile()==null?(n>1?n+" methods · tap to choose":"Only one researched method"):
            tier+" gear shown · "+(n>1?n+" methods ▾":"1 method");
    }
    /** B20.1: compact "Rare drops / KC" chip beside the Outfitter title; one tap opens the panel. */
    String rareDropChipText() {
        String boss=displayBossModel().boss();
        var encs=RareDropTable.encounters(boss);
        int drops=encs.stream().mapToInt(e->e.drops().size()).sum();
        if(drops==0) return RareDropTable.coverage(boss)==RareDropTable.Coverage.NOT_YET_RESEARCHED?"No rates yet":"Not modelled";
        if(rareDropState==null) return drops+" drops · sign in";
        String kc="KC ?";
        if(encs.size()==1) { var k=rareDropState.effective(encs.get(0).encounter()); if(k.known()) kc="KC "+String.format(Locale.ROOT,"%,d",k.value().getAsLong())+(k.source()==RareDropStore.Source.MANUAL?"*":""); }
        else kc=encs.size()+" KC units";
        long goals=RareDropSummary.all(rareDropState).stream().filter(x->x.encounter().boss().equals(boss)).count();
        return kc+(goals>0?" · "+goals+" goal"+(goals==1?"":"s"):"");
    }
    private void rareDropChip(Graphics2D g) {
        boolean open=rareDropExpanded;
        g.setColor(open?new Color(77,57,34):new Color(31,37,38));g.fillRoundRect(CHIP_X,CHIP_Y,CHIP_W,CHIP_H,5,5);
        g.setColor(GOLD);g.drawRoundRect(CHIP_X,CHIP_Y,CHIP_W-1,CHIP_H-1,5,5);
        label(g,"RARE DROPS",CHIP_X+7,CHIP_Y+14,9,GOLD,true);
        label(g,open?"▴":"▾",CHIP_X+CHIP_W-14,CHIP_Y+14,10,GOLD,true);
        clipped(g,rareDropChipText(),CHIP_X+7,CHIP_Y+27,TEXT,CHIP_W-12);
    }
    record RdRow(char kind,int y,int h,String text,Color color,String action,boolean on){}
    private static final int RARE_TOP=399;
    private static final int RD_NAME_W=124; // R06 card: 36px sprite box left, goal box right
    static final Font RD_NAME_FONT=new Font("Dialog",Font.BOLD,11);
    private static final int RD_TEXT_X=58, RD_ICON_X=21, RD_CARD_GAP=4;
    /** R06 drop card height: name (1-2 lines) + chance line + rate line, never shorter than the 36px sprite box. */
    static int rdCardHeight(int nameLines) { return Math.max(44,8+nameLines*13+30); }
    // Tracker Y is determined by its parent, either main Rare Drops or Chronicle.
    /** Bottom of the no-loadout discovery/strategy card (post-shift coordinates). */
    private int discoveryBottom(){
        if(ResearchCombatMethods.find(displayBossModel().boss(),displayBossModel().method())!=null) return 397+357;
        if(displayBossModel().methods().isEmpty() && displayBossModel().bossEntry()!=null) return 407+96+guideNotVerifiedLines(displayBossModel().bossEntry()).size()*14;
        return 407+161;
    }
    // ---- CP-4 (BQA-R12-010): recipe / multi-copy goals in Chronicle ----
    private gg.feral.burrow.core.BankEvidence recipeEvidence; // null = no bank read this login
    /** EDT only. The host's current Best-in-Bank evidence (including stale/closed state), or null. */
    public void setRecipeEvidence(gg.feral.burrow.core.BankEvidence evidence) {
        if(java.util.Objects.equals(recipeEvidence,evidence))return;
        recipeEvidence=evidence; revalidate(); repaint();
    }
    /**
     * Owned-vs-goal for every recipe that uses one of this encounter's verified drops, then the exact multi-copy
     * chance for the copies still missing (RecipeGoalMath: mutually exclusive groups respected, fail closed).
     */
    private void recipeGoalLines(RareDropTable.Encounter e,java.util.function.BiConsumer<String,Color> line) {
        for(var r:gg.feral.burrow.core.RecipeGoals.ALL) {
            if(!r.encounter().equals(e.encounter()))continue;
            var drop=e.drops().stream().filter(d->d.name().equalsIgnoreCase(r.component())).findFirst();
            if(drop.isEmpty())continue;
            var progress=gg.feral.burrow.core.RecipeGoals.progress(r,r.defaultCopies(),drop.get().itemId(),recipeEvidence,0);
            line.accept("GOAL · "+r.title(),GOLD);
            line.accept(progress.line(),progress.complete()?MINT:TEXT);
            if(progress.source()==gg.feral.burrow.core.RecipeGoals.OwnershipSource.CURRENT_EVIDENCE && progress.missing()>0) {
                var res=gg.feral.burrow.core.RecipeGoalMath.evaluate(e,java.util.Map.of(r.component(),progress.missing()),0,0.90);
                if(res.state()==gg.feral.burrow.core.RecipeGoalMath.State.COMPUTED) {
                    String k90=res.killsForTarget().isPresent()?String.format(Locale.ROOT,"%,d",res.killsForTarget().getAsLong()):"?";
                    String exp=res.expectedKills().isPresent()?String.format(Locale.ROOT,"%,.0f",res.expectedKills().getAsDouble()):"?";
                    line.accept("All "+progress.missing()+" more: 90% within "+k90+" more kills · ~"+exp+" expected",MINT);
                } else line.accept(res.reason(),MUTED);
            }
            for(String n:progress.notes()) line.accept(n,MUTED);
        }
    }
    java.util.List<RdRow> rareDropRows() {
        FontMetrics fm=getFontMetrics(PREPARATION_FONT);
        java.util.List<RdRow> rows=new ArrayList<>();
        int[] y={0};
        java.util.function.BiConsumer<String,Color> line=(t,c)->{for(String l:wrapItemName(t,fm,183)){rows.add(new RdRow('t',y[0],14,l,c,null,false));y[0]+=14;}};
        String boss=displayBossModel().boss();
        var encs=RareDropTable.encounters(boss);
        var cov=RareDropTable.coverage(boss);
        int dropCount=encs.stream().mapToInt(e->e.drops().size()).sum();
        rows.add(new RdRow('h',y[0],26,"RARE DROPS & KC · "+boss,GOLD,"expand",true)); y[0]+=26;
        if(BossRegistry.wilderness(boss)) line.accept("⚠ WILDERNESS · PvP/PK risk. Odds only; no gear or bank advice.",BAD);
        if(dropCount==0) {
            line.accept(cov==RareDropTable.Coverage.NOT_YET_RESEARCHED?"Guide not yet verified: Wiki drop rates for this boss are not bundled in this build.":
                "Not modelled: "+RareDropTable.coverageNote(boss),MUTED);
            y[0]+=6; rows.add(new RdRow('e',y[0],0,"",null,null,false)); return rows;
        }
        if(rareDropState==null) line.accept("Sign in: KC and goals are saved per account. Rates are shown below.",GOLD);
        if(rareDropFeedback!=null) line.accept(rareDropFeedback,GOLD);
        for(var e:encs) {
            if(e.drops().isEmpty()) { line.accept("Not modelled: "+String.join("; ",e.notModelled()),MUTED); continue; }
            y[0]+=4;
            if(encs.size()>1){rows.add(new RdRow('n',y[0],15,e.encounter().toUpperCase(Locale.ROOT),TEXT,null,false));y[0]+=15;}
            if(rareDropState!=null) {
                var kc=rareDropState.effective(e.encounter());
                // R06 (owner reference): encounter header card with big name + KC and its provenance.
                String kcText=kc.known()?"KC "+String.format(Locale.ROOT,"%,d",kc.value().getAsLong())+" · "+RareDropSummary.sourceLabel(kc.source())+
                    (kc.source()==RareDropStore.Source.MANUAL?"":" · observed"):"KC UNKNOWN · open this boss's Collection Log page";
                rows.add(new RdRow('k',y[0],46,kcText,kc.known()?(kc.source()==RareDropStore.Source.MANUAL?GOLD:MINT):MUTED,e.encounter(),kc.known()));
                y[0]+=50;
                long seen=sessionLootEncounters.getOrDefault(e.encounter(),0L);
                if(seen>0) line.accept("This login: "+seen+" loot-confirmed kill"+(seen==1?"":"s")+" (estimate, not KC)",GOLD);
                rows.add(new RdRow('b',y[0],22,e.encounter(),null,null,rareDropState.mode(e.encounter())==RareDropMath.Mode.ALL)); y[0]+=24;
            }
            for(var d:e.drops()) {
                boolean on=rareDropState!=null && rareDropState.selected().contains(d.key());
                boolean got=rareDropState!=null && rareDropState.ownership(d.itemId())==RareDropStore.Ownership.OBSERVED_OBTAINED;
                String name=(got?"✓ ":"")+d.name()+(d.condition()==null?"":" *");
                int lines=Math.min(2,wrapItemName(name,getFontMetrics(RD_NAME_FONT),RD_NAME_W).size());
                // R05: every row = sprite + name + verified Wiki rate, then "by KC n: x% · next kill y%" (or KC UNKNOWN).
                var odds=RareDropRowOdds.row(e,d,rareDropState==null?null:rareDropState.effective(e.encounter()));
                // R06 card: sprite | name / "Chance by your KC: x%" / rate · next kill. Click toggles the goal.
                int rh=rdCardHeight(lines);
                rows.add(new RdRow('d',y[0],rh,name+"|"+d.rateText()+"|"+odds.detail()+"|"+d.itemId()+"|"+RareDropRowOdds.label(odds)
                    +"|"+RareDropRowOdds.value(odds)+"|"+RareDropRowOdds.rateLine(odds),
                    on?GOLD:got?MINT:TEXT,"toggle:"+d.key(),on)); y[0]+=rh+RD_CARD_GAP;
            }
            for(var d:e.drops()) if(d.condition()!=null) line.accept("* "+d.name()+": "+d.condition(),MUTED);
            if(!e.rateNote().isBlank()) line.accept(e.rateNote(),MUTED);
            if(e.chestUnit()!=null) line.accept("1 KC = 1 "+e.chestUnit(),MUTED);
            if(rareDropState!=null && e.drops().stream().anyMatch(d->rareDropState.ownership(d.itemId())!=RareDropStore.Ownership.UNKNOWN)) {
                // R09 hotfix: the same x / y the in-game Collection Log shows, from observed unlocks only.
                long got=e.drops().stream().filter(d->rareDropState.ownership(d.itemId())==RareDropStore.Ownership.OBSERVED_OBTAINED).count();
                line.accept("Collection Log: "+got+" / "+e.drops().size()+" unlocked ✓",MINT);
                // R11: provenance and age of the log data (historical unlocks; separate from bank ownership, drops and KC).
                long nowMs=homeClock.getAsLong();
                var page=rareDropState.lastPageRead(e); var search=rareDropState.lastSearchRead();
                String src="Read: "+(page.isPresent()?"page "+ago(nowMs,page.getAsLong()):"page never")
                    +(search.isPresent()?" · search "+ago(nowMs,search.getAsLong()):"");
                line.accept(src,MUTED);
                var unmatched=rareDropState.unmatchedObtained(e);
                if(!unmatched.isEmpty()) line.accept("CHECK: your log shows "+unmatched.size()+" unlocked item"+(unmatched.size()==1?"":"s")
                    +" TrashPanda does not recognise (id "+unmatched.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(", "))+")",BAD);
            }
            else if(rareDropState!=null) line.accept("Log status UNKNOWN: open this boss's Collection Log page (or use the log's Search) in game",MUTED);
            if(rareDropState!=null) RareDropSummary.of(e,rareDropState).ifPresent(sum->{
                String head=sum.mode()+" of "+sum.selected().size()+" selected · ";
                line.accept(head+(sum.chanceNow().isPresent()?"by KC "+String.format(Locale.ROOT,"%,d",sum.kc().value().getAsLong())+": "+
                    RareDropMath.percent(sum.chanceNow().getAsDouble()):"chance now unknown (no KC)"),MINT);
                for(int mi=0;mi<4;mi++) line.accept(sum.milestoneText(mi),TEXT);
                RareDropSummary.duplicateProtectedPerKill(sum,rareDropState).ifPresent(p->{
                    long n=(long)Math.ceil(Math.log(0.1)/Math.log1p(-p));
                    line.accept("With your log (no duplicates): "+RareDropMath.percent(p)+" per kill · 90% within "+
                        String.format(Locale.ROOT,"%,d",n)+" more kills (lower bound)",MINT);
                });
                long seen=sessionLootEncounters.getOrDefault(e.encounter(),0L);
                if(seen>0) line.accept("Provisional, this login only: "+RareDropMath.percent(RareDropMath.chance(sum.mode(),sum.selected(),seen))+
                    " over "+seen+" loot-confirmed kill"+(seen==1?"":"s"),GOLD);
            });
            recipeGoalLines(e,line);
            // R06: fixed-KC guarantees get their own "Guaranteed / Unlocks" cards; other mechanics stay "Not modelled".
            var guarantees=new ArrayList<RareDropRowOdds.Guarantee>();
            for(String n:e.notModelled()) { var gq=RareDropRowOdds.guarantee(n); if(gq.isPresent())guarantees.add(gq.get()); else line.accept("Not modelled: "+n,MUTED); }
            if(!guarantees.isEmpty()) {
                y[0]+=4; rows.add(new RdRow('n',y[0],16,"GUARANTEED / UNLOCKS",GOLD,null,false)); y[0]+=18;
                var kcNow=rareDropState==null?null:rareDropState.effective(e.encounter());
                for(var gq:guarantees) {
                    String status=kcNow==null||!kcNow.known()?"KC unknown":
                        kcNow.value().getAsLong()>=gq.atKc()?"Reached":
                        String.format(Locale.ROOT,"%,d",gq.atKc()-kcNow.value().getAsLong())+" kills to go";
                    rows.add(new RdRow('g',y[0],38,gq.item()+"|Guaranteed at "+String.format(Locale.ROOT,"%,d",gq.atKc())+" KC|"+status,TEXT,null,false));
                    y[0]+=38+RD_CARD_GAP;
                }
            }
            for(String gp:e.gaps()) line.accept("Gap: "+gp,BAD);
        }
        y[0]+=4;
        line.accept("Chance, never a guarantee. Selected drops appear in Pathfinder.",MUTED);
        line.accept("~OSRS Wiki Drops, read "+RareDropTable.READ_DATE,MUTED);
        y[0]+=6; rows.add(new RdRow('e',y[0],0,"",null,null,false));
        return rows;
    }
    /** Panel (click) Y for a point rel pixels below the tracker top, in the current Outfitter layout. */
    int rareDropClickYForTest(int rel){return CONTENT_SHIFT+rareDropTrackerY()+rel;}
    /** Title-area chip that opens/closes the Rare drops / KC panel. */
    static final int CHIP_X=110, CHIP_Y=121, CHIP_W=103, CHIP_H=34;
    int rareDropChipClickYForTest(){return CONTENT_SHIFT+rareChipY()+12;}
    int pathfinderGoalsClickYForTest(int i){return CONTENT_SHIFT+pathfinderLayout().goalsY()+26+i*PF_GOAL_H+10;}
    int rareDropTrackerHeight(){var r=rareDropRows();return r.get(r.size()-1).y();}
    private void rareDropTracker(Graphics2D g) {
        int top=rareDropTrackerY();
        var rows=rareDropRows();
        card(g,top,rows.get(rows.size()-1).y());
        for(var r:rows) {
            int y=top+r.y();
            switch(r.kind()) {
                case 'h' -> { clipped(g,r.text(),20,y+18,GOLD,165); label(g,"✕",195,y+18,11,GOLD,true); }
                case 'n' -> label(g,r.text(),17,y+11,10,TEXT,true);
                case 't' -> small(g,r.text().startsWith("~")?r.text().substring(1):r.text(),17,y+10,r.color());
                case 'b' -> {
                    String[] l={"SET KC","HISCORES",r.on()?"MODE ALL":"MODE ANY"};
                    for(int i=0;i<3;i++){int bx=16+i*63;
                        g.setColor(new Color(31,39,41));g.fillRoundRect(bx,y,60,20,4,4);
                        g.setColor(i==2?GOLD:LINE);g.drawRoundRect(bx,y,60,20,4,4);
                        int w=g.getFontMetrics(new Font("SansSerif",Font.BOLD,9)).stringWidth(l[i]);
                        label(g,l[i],bx+(60-w)/2,y+14,9,i==2?GOLD:TEXT,true);}
                }
                case 'k' -> {
                    g.setColor(new Color(24,30,32));g.fillRoundRect(15,y,195,r.h(),6,6);
                    g.setColor(LINE);g.drawRoundRect(15,y,195,r.h(),6,6);
                    clipped(g,r.action(),23,y+20,GOLD,180);
                    String[] kp=r.text().split(" · ",2);
                    label(g,kp[0].replace("KC ","KC: "),23,y+37,11,r.on()?TEXT:MUTED,false);
                    if(kp.length>1) { int kw=g.getFontMetrics(new Font("Dialog",Font.PLAIN,11)).stringWidth(kp[0].replace("KC ","KC: "));
                        small(g,elideSessionText(g,kp[1],180-kw-8),23+kw+8,y+37,r.color()); }
                }
                case 'g' -> {
                    String[] gp=r.text().split("\\|",3);
                    g.setColor(new Color(24,30,32));g.fillRoundRect(15,y,195,r.h(),6,6);
                    g.setColor(LINE);g.drawRoundRect(15,y,195,r.h(),6,6);
                    clipped(g,gp[0],23,y+15,TEXT,180);
                    small(g,elideSessionText(g,gp[1]+" · "+gp[2],180),23,y+30,gp[2].startsWith("Reached")?MINT:MUTED);
                }
                case 'd' -> {
                    String[] parts=r.text().split("\\|",7);
                    g.setColor(new Color(24,30,32));g.fillRoundRect(15,y,195,r.h(),6,6);
                    g.setColor(r.on()?GOLD:LINE);g.drawRoundRect(15,y,195,r.h(),6,6);
                    // sprite box (authentic RuneLite ItemManager image only; empty box when unavailable)
                    g.setColor(new Color(31,39,41));g.fillRoundRect(RD_ICON_X,y+4,34,34,4,4);
                    g.setColor(LINE);g.drawRoundRect(RD_ICON_X,y+4,34,34,4,4);
                    int iconId=parts.length>3?Integer.parseInt(parts[3]):-1;
                    BufferedImage sprite=iconId>0?itemImage.apply(iconId):null;
                    if(sprite!=null && sprite.getWidth()>0) {
                        double sc=Math.min(1.0,Math.min(32.0/sprite.getWidth(),32.0/sprite.getHeight()));
                        int iw=(int)Math.round(sprite.getWidth()*sc), ih=(int)Math.round(sprite.getHeight()*sc);
                        g.drawImage(sprite,RD_ICON_X+1+(32-iw)/2,y+5+(32-ih)/2,iw,ih,null);
                    }
                    // goal box (top-right): ticked = selected GOAL, never "obtained"
                    g.setColor(r.on()?GOLD:LINE);g.drawRect(196,y+6,9,9);
                    if(r.on()){g.fillRect(198,y+8,6,6);}
                    Font nf=new Font("Dialog",Font.BOLD,11);
                    java.util.List<String> nl=wrapItemName(parts[0],g.getFontMetrics(RD_NAME_FONT),RD_NAME_W);
                    int lines=Math.min(2,nl.size());
                    for(int i=0;i<lines;i++) {
                        String t=nl.get(i);
                        if(i==1 && nl.size()>2) t=elideSessionText(g,t+" "+String.join(" ",nl.subList(2,nl.size())),RD_NAME_W);
                        label(g,t,RD_TEXT_X,y+16+i*13,11,r.color(),true);
                    }
                    g.setFont(nf);
                    int cy=y+16+lines*13+1;
                    String lab=parts.length>4?parts[4]:"", val=parts.length>5?parts[5]:"";
                    if(val.isEmpty()) small(g,elideSessionText(g,lab,210-RD_TEXT_X-4),RD_TEXT_X,cy,MUTED);
                    else {
                        String head=lab+": ";
                        small(g,head,RD_TEXT_X,cy,lab.contains("manual")?GOLD:MUTED);
                        int hw=g.getFontMetrics(new Font("Dialog",Font.PLAIN,10)).stringWidth(head);
                        label(g,val,RD_TEXT_X+hw,cy,10,GOLD,true);
                    }
                    if(parts.length>6) label(g,elideSessionText(g,parts[6],210-RD_TEXT_X-4),RD_TEXT_X,cy+13,9,MUTED,false);
                }
                default -> {}
            }
        }
    }
    /** rel = content Y relative to the tracker top. Returns true when consumed. */
    private boolean rareDropClick(int x,int rel) {
        for(var r:rareDropRows()) {
            if(rel<r.y() || rel>=r.y()+r.h()) continue;
            if(r.kind()=='h'){if(!chronicleOpen)rareDropExpanded=false;rareDropFeedback=null;return true;}
            if(rareDropState==null || rareDropHash==-1L) {
                if(r.kind()=='d'||r.kind()=='b'){rareDropFeedback="Sign in to save drop goals and KC for this account";return true;}
                return false;
            }
            if(r.kind()=='d' && r.action()!=null){rareDropAction.toggle(rareDropHash,r.action().substring(7));return true;}
            if(r.kind()=='b') {
                int i=Math.max(0,Math.min(2,(x-16)/63));
                String enc=r.text();
                if(i==0) {
                    String in=manualKcPrompt!=null?manualKcPrompt.apply(enc):isShowing()?
                        JOptionPane.showInputDialog(this,"Manual kill count for "+enc+" (blank clears).\nShown as MANUAL; an observed KC always takes priority.",
                            "Manual KC",JOptionPane.PLAIN_MESSAGE):null;
                    if(in==null) return true;
                    String t=in.trim().replace(",","");
                    if(t.isEmpty()){rareDropAction.manualKc(rareDropHash,enc,null);return true;}
                    try { long v=Long.parseLong(t); if(v<0||v>100_000_000L)throw new NumberFormatException();
                        rareDropAction.manualKc(rareDropHash,enc,v); rareDropFeedback=null; }
                    catch(NumberFormatException bad){rareDropFeedback="Manual KC must be a whole number (0 or more)";}
                } else if(i==1) { rareDropFeedback="Looking up official HiScores…"; rareDropAction.hiscores(rareDropHash,enc); }
                else rareDropAction.mode(rareDropHash,enc,rareDropState.mode(enc)==RareDropMath.Mode.ANY?RareDropMath.Mode.ALL:RareDropMath.Mode.ANY);
                return true;
            }
        }
        return false;
    }

    /** Build 20: discovery-only card. Real Wiki access facts, no gear, no readiness. */
    java.util.List<String> guideNotVerifiedLines(BossRegistry.Entry e) {
        FontMetrics fm=getFontMetrics(PREPARATION_FONT);
        java.util.List<String> out=new ArrayList<>();
        java.util.List<String> req=new ArrayList<>(e.quests());
        e.skills().forEach((k,v)->req.add(v+" "+k));
        for(String l:wrapItemName("Access: "+(req.isEmpty()?"no quest/skill gate on Wiki page":String.join(", ",req)),fm,183))out.add(l);
        for(String n:e.accessNotes())for(String l:wrapItemName("• "+n,fm,183))out.add(l);
        for(String l:wrapItemName("Boss attacks: "+e.bossAttacks(),fm,183))out.add(l);
        if(!e.suggestedStyles().isEmpty()) {
            java.util.List<String> st=new ArrayList<>(); for(var c:e.suggestedStyles())st.add(c.label());
            for(String l:wrapItemName("Wiki-suggested styles: "+String.join(", ",st),fm,183))out.add(l);
        }
        if(!e.mechanic().isBlank())for(String l:wrapItemName("Key mechanic: "+e.mechanic(),fm,183))out.add(l);
        for(String l:wrapItemName("Source: "+e.wikiUrl().replace("https://",""),fm,183))out.add("~"+l);
        return out;
    }
    private void guideNotVerifiedCard(Graphics2D g,BossRegistry.Entry e) {
        var lines=guideNotVerifiedLines(e);
        int h=96+lines.size()*14;
        card(g,407,h);
        small(g,e.group().label().toUpperCase(Locale.ROOT)+" · DISCOVERY ONLY",20,425,GOLD);
        label(g,"Guide not yet verified",20,447,13,BAD,true);
        int y=469;
        for(String l:lines){ boolean src=l.startsWith("~"); small(g,src?l.substring(1):l,20,y,src?MUTED:TEXT); y+=14; }
        small(g,"No gear, inventory or Best in Bank",20,y+8,MUTED);
        small(g,"readiness is shown until verified.",20,y+22,MUTED);
    }
    private void researchStrategyCard(Graphics2D g,ResearchCombatMethods.Guide research) {
        card(g,397,357);
        small(g,"RESEARCHED COMBAT APPROACH",20,419,GOLD);
        label(g,"STRATEGY ONLY",20,445,14,TEXT,true);
        small(g,"Complete gear not yet source-verified",20,468,BAD);
        small(g,"METHOD",20,495,GOLD);
        drawResearchLines(g,research.tactics(),20,517,10,3);
        small(g,"BEFORE YOUR TRIP",20,600,GOLD);
        drawResearchLines(g,research.safeguards(),20,621,10,5);
        small(g,"No fake equipment or bank ownership",20,722,MUTED);
        small(g,"See research source in project docs",20,740,MUTED);
    }
    private void drawResearchLines(Graphics2D g,String value,int x,int y,int font,int maxLines) {
        g.setFont(new Font("Dialog",Font.PLAIN,font));
        FontMetrics fm=g.getFontMetrics();
        for(String line:wrapItemName(value,fm,183).subList(0,Math.min(maxLines,wrapItemName(value,fm,183).size()))) {
            small(g,line,x,y,MUTED);y+=16;
        }
    }
    private static Color blend(Color a,Color b,float t){
        return new Color((int)(a.getRed()*t+b.getRed()*(1-t)),(int)(a.getGreen()*t+b.getGreen()*(1-t)),(int)(a.getBlue()*t+b.getBlue()*(1-t)));
    }
    private static Color brighter(Color c){return new Color(Math.min(255,c.getRed()+40),Math.min(255,c.getGreen()+40),Math.min(255,c.getBlue()+40));}
    /** Original vector glyphs (no external artwork): sword, bow, staff. 16x22 box. */
    static void styleIcon(Graphics2D g0,CombatStyle s,int x,int y,Color c) {
        Graphics2D g=(Graphics2D)g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(c);
            switch(s) {
                case MELEE -> { // sword: blade, guard, grip, pommel
                    g.setStroke(new BasicStroke(2.4f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                    g.drawLine(x+3,y+20,x+14,y+2);
                    g.setStroke(new BasicStroke(2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                    g.drawLine(x+2,y+12,x+10,y+17);
                    g.fillOval(x,y+19,4,4);
                }
                case RANGED -> { // bow: limb arc + string + arrow
                    g.setStroke(new BasicStroke(2.2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                    g.drawArc(x-4,y+1,14,21,-80,160);
                    g.setStroke(new BasicStroke(1f));
                    g.drawLine(x+3,y+2,x+3,y+21);
                    g.setStroke(new BasicStroke(1.6f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                    g.drawLine(x+1,y+11,x+15,y+11);
                    g.drawLine(x+15,y+11,x+12,y+8);g.drawLine(x+15,y+11,x+12,y+14);
                }
                case MAGIC -> { // staff with glowing orb
                    g.setStroke(new BasicStroke(2.2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                    g.drawLine(x+5,y+22,x+11,y+7);
                    g.fillOval(x+8,y+1,8,8);
                    g.setColor(new Color(255,255,255,140));g.fillOval(x+10,y+3,3,3);
                }
            }
        } finally { g.dispose(); }
    }
    private void kbdWildernessWarning(Graphics2D g) {
        final int x=13,y=397,w=199,h=66;
        boolean approach=BossRegistry.find(displayBossModel().boss()).map(e->e.risk()==BossRegistry.Risk.WILDERNESS_ACCESS).orElse(false);
        g.setColor(new Color(70,32,26));g.fillRoundRect(x,y,w,h,7,7);
        g.setColor(new Color(245,156,88));g.drawRoundRect(x,y,w-1,h-1,7,7);
        label(g,approach?"! WILDERNESS APPROACH · PK RISK":"!  WILDERNESS · PvP / PK RISK",x+9,y+17,10,new Color(255,195,124),true);
        label(g,"Players can attack you; death to a",x+9,y+33,10,TEXT,false);
        label(g,"player loses unprotected items.",x+9,y+46,10,TEXT,false);
        label(g,"Best in Bank OFF · Wiki/guide only",x+9,y+60,9,new Color(255,195,124),true);
    }
    /** Full Wilderness risk text (OSRS Wiki "Wilderness" danger rules); no invented escape guarantees. */
    static String wildernessTooltip(String boss) {
        boolean approach=BossRegistry.find(boss).map(e->e.risk()==BossRegistry.Risk.WILDERNESS_ACCESS).orElse(false);
        String common=" Other players can attack you in PvP Wilderness; dying to a player loses unprotected items, and a skulled"+
            " player keeps at most one item. Most teleports fail above level 20 and teleblock stops nearly all teleports."+
            " Best in Bank is disabled: Burrow will not suggest taking your owned gear here.";
        return approach ? KBD_WARNING_TOOLTIP+common :
            "Wilderness boss: the fight area is PvP Wilderness, so an instanced or quiet room is not a safety guarantee."+common;
    }
    private void itemCell(Graphics2D g, VerifiedOutfitterCatalog.Item item,
                          int x,int y,int w,int h,String slotName) {
        g.setColor(new Color(35,39,38));g.fillRoundRect(x,y,w,h,4,4);
        g.setColor(item!=null && item.id()<1?BAD:LINE);g.drawRoundRect(x,y,w,h,4,4);
        if(item!=null) {
            BufferedImage sprite=item.id()>0?itemImage.apply(item.id()):null;
            if(sprite!=null)g.drawImage(sprite,x+(w-32)/2,y+2,32,32,null);
            else if(item.id()<1)label(g,"?",x+w/2-4,y+24,15,BAD,true);
            // R05: stack counts top-left as in the game, clear of the slot name along the bottom.
            if(item.amount()>1)label(g,Integer.toString(item.amount()),x+3,y+11,9,GOLD,true);
            if(item.amount()<0)label(g,"×?",x+3,y+11,9,GOLD,true);
        }
        if(slotName!=null)label(g,slotName,x+2,y+h-2,7,MUTED,false);
    }
    private static String sessionAmount(long n) {return String.format(Locale.US,"%,d",n);}
    private static final int SESSION_XP_TOP=128;
    private int sessionXpHeight(){return 104+Math.max(1,sessionXp.size())*19;}
    private int bossingTop(){return SESSION_XP_TOP+sessionXpHeight()+12;}
    private int bossingSectionHeight(int entries){return 45+Math.max(1,entries)*18;}
    private int bossingHeight(){return 42+bossingSectionHeight(sessionKc.size())+
        bossingSectionHeight(sessionLootEncounters.size())+
        bossingSectionHeight(sessionDrops.size())+bossingSectionHeight(sessionSupplies.size())+8;}
    private int historyTop(){return bossingTop()+bossingHeight()+12;}
    private int historyRowHeight(int index){return expandedHistoryIndex==index?120:59;}
    private int completedHistoryHeight() {
        int h=66;for(int i=0;i<Math.min(10,completedSessions.size());i++)h+=historyRowHeight(i);
        return Math.max(Math.max(190,880-CONTENT_SHIFT-historyTop()-22),h);
    }
    private int sessionContentBottom(){return historyTop()+completedHistoryHeight()+10;}
    private int historyIndexAt(int y) {
        int top=historyTop()+56;
        for(int i=0;i<Math.min(10,completedSessions.size());i++) {
            int h=historyRowHeight(i);
            if(y>=top&&y<top+h)return i;
            top+=h;
        }
        return -1;
    }
    private void sessions(Graphics2D g) {
        if (flowSessionEnabled()) {flowSessions(g);return;}
        int xpY=SESSION_XP_TOP;
        card(g,xpY,sessionXpHeight());
        small(g,"XP GAINED",20,xpY+20,GOLD);
        Color stateColor=activeSessionState==SessionState.ACTIVE?MINT:
            activeSessionState==SessionState.SUSPENDED?GOLD:MUTED;
        String status=activeSessionState==SessionState.ACTIVE?"ACTIVE":
            activeSessionState==SessionState.SUSPENDED?"PAUSED":"OFFLINE";
        rightLabel(g,status,202,xpY+20,10,stateColor);
        boolean observing=sessionFeeds.get(SessionFeed.XP)==FeedStatus.AVAILABLE;
        var xs=xpTrackingState;
        if(activeSessionState==SessionState.SUSPENDED&&xs!=null)xs=XpTrackingState.PAUSED;
        String source=observing?"Observed this login":xs==XpTrackingState.UNAVAILABLE?"SOURCE UNAVAILABLE":
            xs==XpTrackingState.PAUSED?"Paused · totals retained":xs==XpTrackingState.SYNCING?"Waiting for fresh baseline":
            xs==XpTrackingState.OFF?"START XP TRACKING · click here":"Awaiting verified XP events";
        small(g,elideSessionText(g,source,185),20,xpY+38,observing?MINT:MUTED);
        label(g,observing?"+"+sessionAmount(sessionTotalXp):"XP UNAVAILABLE",20,xpY+66,
            observing?19:13,observing?MINT:MUTED,true);
        int rowY=xpY+88;
        if(sessionXp.isEmpty())small(g,"No verified skill changes",20,rowY,MUTED);
        for(var entry:sessionXp.entrySet()) {
            small(g,elideSessionText(g,entry.getKey(),99),20,rowY,TEXT);
            rightLabel(g,"+"+sessionAmount(entry.getValue()),202,rowY,10,MINT);
            rowY+=19;
        }
        int by=bossingTop();card(g,by,bossingHeight());
        label(g,"BOSSING SUMMARY",20,by+21,12,GOLD,true);
        small(g,"KC · loot estimates · drops · supplies",20,by+36,MUTED);
        int y=by+46;
        y=paintCompactBossing(g,y,"BOSS KC GAINS",SessionFeed.BOSS_KC,
            sessionKc.entrySet().stream().map(e->e.getKey()+"  +"+sessionAmount(e.getValue())).toList(),
            "No verified KC gain",false);
        y=paintCompactBossing(g,y,"LOOT-CONFIRMED KILLS",null,
            sessionLootEncounters.entrySet().stream().map(e->e.getKey()+"  ~"+sessionAmount(e.getValue())).toList(),
            "No loot-confirmed encounter observed",true);
        y=paintCompactBossing(g,y,"BOSS DROPS",SessionFeed.DROPS,
            sessionDrops.values().stream().map(e->e.itemName()+" ×"+sessionAmount(e.quantity())).toList(),
            "No verified drop events",false);
        paintCompactBossing(g,y,"SUPPLIES USED",SessionFeed.SUPPLIES,
            sessionSupplies.values().stream().map(e->e.itemName()+" ×"+sessionAmount(e.quantity())).toList(),
            "No verified supply use",false);
        paintCompletedHistory(g,historyTop());
    }
    private int paintCompactBossing(Graphics2D g,int y,String header,SessionFeed feed,
                                    java.util.List<String> details,String missing,boolean estimated) {
        int h=bossingSectionHeight(details.size());
        g.setColor(new Color(22,27,28));g.fillRoundRect(17,y,191,h-4,5,5);
        g.setColor(LINE);g.drawRoundRect(17,y,191,h-4,5,5);
        label(g,header,23,y+15,10,GOLD,true);
        FeedStatus source=feed==null?FeedStatus.NOT_CONNECTED:sessionFeeds.getOrDefault(feed,FeedStatus.NOT_CONNECTED);
        String caption=estimated?"ESTIMATE · from loot, never KC":
            source==FeedStatus.AVAILABLE?"Verified event source":
            source==FeedStatus.UNAVAILABLE?"Source unavailable":"Awaiting verified events";
        small(g,caption,23,y+29,estimated?GOLD:MUTED);
        if(details.isEmpty()) small(g,missing,23,y+47,MUTED);
        else {
            int iy=y+47;
            for(String line:details){small(g,elideSessionText(g,line,176),23,iy,TEXT);iy+=18;}
        }
        return y+h;
    }
    // Completed logins are read-only account-isolated host summaries, NEVER illustration data.
    private java.util.List<gg.feral.burrow.core.CompletedSessionHistory.Summary> completedSessions=java.util.List.of();
    private String completedHistoryNote="Sign in to load this account's history";
    public void setCompletedSessions(java.util.List<gg.feral.burrow.core.CompletedSessionHistory.Summary> list,String note) {
        completedSessions=list==null?java.util.List.of():java.util.List.copyOf(list);
        expandedHistoryIndex=-1;
        completedHistoryNote=note==null?"":note;
        revalidate();repaint();
    }
    java.util.List<gg.feral.burrow.core.CompletedSessionHistory.Summary> completedSessionsForTest(){return completedSessions;}
    static String historyWhen(long endMillis,long durationMillis) {
        var t=java.time.Instant.ofEpochMilli(endMillis).atZone(java.time.ZoneId.systemDefault());
        long mins=Math.max(0,durationMillis)/60000;
        String dur=mins>=60?(mins/60)+"h "+(mins%60)+"m":mins+"m";
        return String.format(Locale.US,"%1$tb %1$te %1$tH:%1$tM · %2$s",t,dur);
    }
    static String historyLine(gg.feral.burrow.core.CompletedSessionHistory.Summary s) {
        String xp=s.xpFeed()==gg.feral.burrow.core.CompletedSessionHistory.FeedState.OBSERVED?
            sessionAmount(s.totalXp())+" XP":"XP n/a";
        String kc=s.totalVerifiedKc()>0?s.totalVerifiedKc()+" KC":"no verified KC";
        long drops=s.drops().stream().mapToLong(gg.feral.burrow.core.CompletedSessionHistory.Drop::quantity).sum();
        return xp+" · "+kc+" · "+drops+" drop"+(drops==1?"":"s");
    }
    // UI-R02: measured Sessions layout for larger text; preserves provenance
    // exactly as supplied by the host and distinguishes estimated loot from KC.
    private boolean flowSessionEnabled(){return uiScale==UiScale.LARGE||uiScale==UiScale.EXTRA_LARGE;}
    private final java.util.List<Rectangle> sessionFlowHistory=new ArrayList<>();
    private Rectangle sessionFlowXp;
    private int flowSessions(Graphics2D g){
        sessionFlowHistory.clear();int y=128;
        boolean observing=sessionFeeds.get(SessionFeed.XP)==FeedStatus.AVAILABLE;
        var xs=xpTrackingState;
        if(activeSessionState==SessionState.SUSPENDED&&xs!=null)xs=XpTrackingState.PAUSED;
        String source=observing?"Observed this login":xs==XpTrackingState.UNAVAILABLE?"SOURCE UNAVAILABLE":
            xs==XpTrackingState.PAUSED?"Paused · totals retained":xs==XpTrackingState.SYNCING?"Waiting for fresh baseline":
            xs==XpTrackingState.OFF?"START XP TRACKING · click here":"Awaiting verified XP events";
        java.util.List<FlowLine> xp=new ArrayList<>();
        xp.add(flowLine("Session: "+activeSessionState.name(),MUTED));
        xp.add(flowLine(source,observing?MINT:MUTED));
        xp.add(flowLine(observing?"+"+sessionAmount(sessionTotalXp)+" XP":"XP UNAVAILABLE",observing?MINT:GOLD));
        if(sessionXp.isEmpty())xp.add(flowLine("No verified skill changes",MUTED));
        else for(var entry:sessionXp.entrySet())xp.add(flowLine(entry.getKey()+": +"+sessionAmount(entry.getValue()),TEXT));
        int start=y;y=flowCard(g,y,"XP GAINED",xp,null);
        sessionFlowXp=new Rectangle(9,start,207,y-start-10);
        y=flowCard(g,y,"BOSSING SUMMARY",java.util.List.of(flowLine(
            "Verified boss KC is separate from loot-confirmed encounter estimates.",MUTED)),null);
        y=flowBossSection(g,y,"BOSS KC GAINS",SessionFeed.BOSS_KC,
            sessionKc.entrySet().stream().map(e->e.getKey()+"  +"+sessionAmount(e.getValue())).toList(),
            "No verified KC gain",false);
        y=flowBossSection(g,y,"LOOT-CONFIRMED ENCOUNTERS",null,
            sessionLootEncounters.entrySet().stream().map(e->e.getKey()+"  ~"+sessionAmount(e.getValue())).toList(),
            "No loot-confirmed encounter observed",true);
        y=flowBossSection(g,y,"BOSS DROPS",SessionFeed.DROPS,
            sessionDrops.values().stream().map(e->e.itemName()+" ×"+sessionAmount(e.quantity())).toList(),
            "No verified drop events",false);
        y=flowBossSection(g,y,"SUPPLIES USED",SessionFeed.SUPPLIES,
            sessionSupplies.values().stream().map(e->e.itemName()+" ×"+sessionAmount(e.quantity())).toList(),
            "No verified supply use",false);
        java.util.List<FlowLine> intro=new ArrayList<>();
        intro.add(flowLine(completedHistoryNote,MUTED));
        intro.add(flowLine("Confirmed completed logins only",MUTED));
        y=flowCard(g,y,"LAST 10 COMPLETED LOGINS",intro,null);
        if(completedSessions.isEmpty())return flowCard(g,y,"HISTORY",java.util.List.of(flowLine("No saved logins yet",MUTED)),null);
        for(int i=0;i<Math.min(10,completedSessions.size());i++){
            var record=completedSessions.get(i);
            String xpText=record.xpFeed()==gg.feral.burrow.core.CompletedSessionHistory.FeedState.OBSERVED?
                "+"+sessionAmount(record.totalXp())+" XP":"XP unverified";
            String kcText=record.kcFeed()==gg.feral.burrow.core.CompletedSessionHistory.FeedState.OBSERVED?
                record.totalVerifiedKc()+" verified KC":"KC unverified";
            long drops=record.drops().stream().mapToLong(gg.feral.burrow.core.CompletedSessionHistory.Drop::quantity).sum();
            java.util.List<FlowLine> details=new ArrayList<>();
            details.add(flowLine(xpText+" · "+kcText+" · "+drops+" drops",TEXT));
            if(i==expandedHistoryIndex){
                String estimates=record.estimatedLootEncounters().isEmpty()?"No confirmed loot encounters":
                    "Estimated loot kills: "+record.estimatedLootEncounters().values().stream().mapToLong(Long::longValue).sum();
                details.add(flowLine(estimates+" · NOT KC",GOLD));
                details.add(flowLine("Skills: "+record.xpBySkill().keySet(),MUTED));
                details.add(flowLine("Drops: "+record.drops().stream().map(gg.feral.burrow.core.CompletedSessionHistory.Drop::itemName).toList(),MUTED));
                details.add(flowLine("Trust: "+(record.uncertainty().isEmpty()?"Observed sources":record.uncertainty()),MUTED));
            }else details.add(flowLine("Tap to inspect source details",MUTED));
            int by=y;
            y=flowCard(g,y,historyWhen(record.endMillis(),record.durationMillis())+
                (i==expandedHistoryIndex?" ▴":" ▾"),details,null);
            sessionFlowHistory.add(new Rectangle(9,by,207,y-by-10));
        }
        return y;
    }
    private int flowBossSection(Graphics2D g,int y,String header,SessionFeed feed,
                                java.util.List<String> lines,String missing,boolean estimated){
        java.util.List<FlowLine> text=new ArrayList<>();
        FeedStatus source=feed==null?FeedStatus.NOT_CONNECTED:sessionFeeds.getOrDefault(feed,FeedStatus.NOT_CONNECTED);
        String caption=estimated?"ESTIMATE · from loot, never KC":
            source==FeedStatus.AVAILABLE?"Verified event source":
            source==FeedStatus.UNAVAILABLE?"Source unavailable":"Awaiting verified events";
        text.add(flowLine(caption,estimated?GOLD:MUTED));
        if(lines.isEmpty())text.add(flowLine(missing,MUTED));
        else for(String line:lines)text.add(flowLine(line,TEXT));
        return flowCard(g,y,header,text,null);
    }
    private void flowSessionClick(int x,int y){
        if(sessionFlowXp!=null&&sessionFlowXp.contains(x,y)&&activeSessionState==SessionState.ACTIVE&&
           sessionFeeds.get(SessionFeed.XP)!=FeedStatus.AVAILABLE&&xpTrackingState==XpTrackingState.OFF){
            xpStartAction.run();revalidate();repaint();return;
        }
        for(int i=0;i<sessionFlowHistory.size();i++)if(sessionFlowHistory.get(i).contains(x,y)){
            expandedHistoryIndex=expandedHistoryIndex==i?-1:i;
            revalidate();repaint();return;
        }
    }

    private int paintCompletedHistory(Graphics2D g,int y) {
        int h=completedHistoryHeight();card(g,y,h);
        label(g,"LAST 10 COMPLETED LOGINS",19,y+21,11,GOLD,true);
        small(g,elideSessionText(g,completedHistoryNote,185),19,y+37,MUTED);
        if(completedSessions.isEmpty()) {
            small(g,"Confirmed logouts only",19,y+63,MUTED);
            small(g,"No saved logins yet",19,y+83,MUTED);
            return y+h;
        }
        int cy=y+56;
        for(int i=0;i<Math.min(10,completedSessions.size());i++) {
            var record=completedSessions.get(i);int rh=historyRowHeight(i);
            g.setColor(i==expandedHistoryIndex?new Color(60,46,29):new Color(23,28,29));
            g.fillRoundRect(17,cy,191,rh-3,5,5);
            g.setColor(i==expandedHistoryIndex?GOLD:LINE);g.drawRoundRect(17,cy,191,rh-3,5,5);
            small(g,elideSessionText(g,historyWhen(record.endMillis(),record.durationMillis()),163),23,cy+16,TEXT);
            rightLabel(g,i==expandedHistoryIndex?"▴":"▾",201,cy+16,11,GOLD);
            String xp=record.xpFeed()==gg.feral.burrow.core.CompletedSessionHistory.FeedState.OBSERVED?
                "+"+sessionAmount(record.totalXp())+" XP":"XP unverified";
            small(g,xp,23,cy+34,record.xpFeed()==gg.feral.burrow.core.CompletedSessionHistory.FeedState.OBSERVED?MINT:MUTED);
            String kc=record.kcFeed()==gg.feral.burrow.core.CompletedSessionHistory.FeedState.OBSERVED?
                record.totalVerifiedKc()+" verified KC":"KC unverified";
            long drops=record.drops().stream().mapToLong(gg.feral.burrow.core.CompletedSessionHistory.Drop::quantity).sum();
            small(g,kc+" · "+drops+" drops",23,cy+48,MUTED);
            if(i==expandedHistoryIndex) {
                int dy=cy+66;
                String estimated=record.estimatedLootEncounters().isEmpty()?"No confirmed loot encounters":
                    "Estimated loot kills: "+record.estimatedLootEncounters().values().stream().mapToLong(Long::longValue).sum();
                small(g,elideSessionText(g,estimated+" · NOT KC",178),23,dy,GOLD);
                small(g,elideSessionText(g,"Skills: "+record.xpBySkill().keySet(),178),23,dy+17,MUTED);
                small(g,elideSessionText(g,"Drops: "+record.drops().stream().map(gg.feral.burrow.core.CompletedSessionHistory.Drop::itemName).toList(),178),23,dy+34,MUTED);
                small(g,elideSessionText(g,"Trust: "+(record.uncertainty().isEmpty()?"Observed sources":record.uncertainty()),178),23,dy+51,MUTED);
            }
            cy+=rh;
        }
        return y+h;
    }
    private String elideSessionText(Graphics2D g,String text,int maxWidth) {
        g.setFont(uiFont(10,false));
        FontMetrics fm=g.getFontMetrics();
        if(fm.stringWidth(text)<=maxWidth)return text;
        String cut=text;
        while(cut.length()>1 && fm.stringWidth(cut+"…")>maxWidth)
            cut=cut.substring(0,cut.length()-1);
        return cut+"…";
    }
    private void label(Graphics2D g,String s,int x,int y,int size,Color color,boolean bold) {
        g.setColor(themed(color));g.setFont(uiFont(size,bold));g.drawString(s,x,y);
    }
    private void small(Graphics2D g,String s,int x,int y,Color c) {label(g,s,x,y,10,c,false);}
    private void clipped(Graphics2D g,String s,int x,int y,Color c,int pixels) {
        g.setFont(uiFont(11,true));
        FontMetrics metrics = g.getFontMetrics();
        String display = s;
        while (display.length()>1 && metrics.stringWidth(display)>pixels) {
            display = display.substring(0,display.length()-1);
        }
        if (!display.equals(s)) {
            while (display.length()>1 && metrics.stringWidth(display + "…")>pixels)
                display=display.substring(0,display.length()-1);
            display += "…";
        }
        g.setColor(themed(c));g.drawString(display,x,y);
    }
}
