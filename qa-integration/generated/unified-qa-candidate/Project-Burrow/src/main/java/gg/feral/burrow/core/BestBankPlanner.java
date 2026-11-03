package gg.feral.burrow.core;

import gg.feral.burrow.bib.AmmoRules;
import gg.feral.burrow.bib.BossTaskMatrix;
import gg.feral.burrow.bib.HeadAdvisor;
import gg.feral.burrow.bib.RangedCapeAdvisor;
import gg.feral.burrow.bib.SlayerTask;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Item;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Profile;
import java.util.*;

/**
 * Build 19 Best in Bank trip solver. Pure Java: no RuneLite calls, safe on a worker thread.
 *
 * <p>Compares WHOLE candidate setups, never first-owned-by-slot:
 * <ol>
 *   <li>Candidates are the selected boss + method at the selected tier and each lower tier of the
 *       same combat style. Each candidate keeps its own source-consistent weapon / ammunition /
 *       offhand bundle. Only armour/accessory slots may take reviewed owned lower-tier items
 *       (existing {@link TierAwareBankResolver} rules), plus the validated F2P bow+arrow pairs.</li>
 *   <li>Physical stock is allocated ONCE across worn gear and all 28 inventory positions.</li>
 *   <li>Wear levels and quest gates come from the bundled FINAL-06 requirement registry and are
 *       checked against observed real levels and quest states; unknown stays unknown.</li>
 *   <li>Two-handed weapons cannot coexist with a shield; inventory switches are simulated slot by
 *       slot to prove the 28-slot transition fits.</li>
 *   <li>The verdict is READY only when every planned gear slot and switch is owned, usable and
 *       coherent and supplies are stocked. A lower tier is shown only when it scores better as a
 *       complete plan. Wiki profiles are never modified.</li>
 * </ol>
 */
public final class BestBankPlanner {
    private BestBankPlanner() {}
    public enum Verdict { READY, READY_SHORT_SUPPLIES, VERIFY, NOT_READY, UNKNOWN,
        /** Build 20: Wilderness / unknown-risk boss. Best in Bank is never computed. */
        NOT_AVAILABLE_WILDERNESS }
    public enum GearStatus { OWNED, OWNED_VARIANT, OWNED_SUBSTITUTE, LOW_STOCK, LEVEL_UNMET, QUEST_UNMET,
        REQUIREMENT_UNKNOWN, NOT_OBSERVED, PARTIAL_STACK, EMPTY,
        /** CP-1: an optional (non-blocking) slot item not owned: the trip leaves this slot empty. */
        OPTIONAL_MISSING;
        /** The shown item is physically in the observed stock (worn in the planned set). */
        public boolean physicallyOwned() { return this!=NOT_OBSERVED && this!=OPTIONAL_MISSING && this!=EMPTY && this!=PARTIAL_STACK; }
    }
    public enum SupplyStatus { STOCKED, LOWER_DOSE, SHORT, NOT_OBSERVED, QUANTITY_UNSPECIFIED,
        VARIANT_UNSPECIFIED, EMPTY, SWITCH_OWNED, SWITCH_NOT_OBSERVED, SWITCH_REQUIREMENT_UNMET }
    public record GearCell(Item planned, Item shown, GearStatus status, String note) {}
    public record SupplyCell(Item planned, Item shown, SupplyStatus status, String note) {}
    public record SwitchCheck(String label, boolean verified, boolean fits, int freeSlotsNeeded) {}
    public record Plan(String boss, String method, Tier selectedTier, Tier shownTier, Verdict verdict,
                       List<GearCell> gear, List<SupplyCell> inventory, List<String> blockers,
                       List<String> checks, List<String> shortages, int usedSlots, int freeSlots,
                       List<SwitchCheck> switches, String trust, TierAwareBankResolver.Result tierResult,
                       List<String> candidateSummaries, List<String> upgrades, List<String> options) {
        public Plan {
            options=options==null?List.of():List.copyOf(options);
            gear=List.copyOf(gear); inventory=List.copyOf(inventory); blockers=List.copyOf(blockers);
            checks=List.copyOf(checks); shortages=List.copyOf(shortages); switches=List.copyOf(switches);
            candidateSummaries=List.copyOf(candidateSummaries);
            upgrades=upgrades==null?List.of():List.copyOf(upgrades);
            if(gear.size()!=11 || inventory.size()!=28) throw new IllegalArgumentException("geometry");
        }
        /** CP-1 shape: no owned-gear options. */
        public Plan(String boss, String method, Tier selectedTier, Tier shownTier, Verdict verdict,
                    List<GearCell> gear, List<SupplyCell> inventory, List<String> blockers,
                    List<String> checks, List<String> shortages, int usedSlots, int freeSlots,
                    List<SwitchCheck> switches, String trust, TierAwareBankResolver.Result tierResult,
                    List<String> candidateSummaries, List<String> upgrades) {
            this(boss,method,selectedTier,shownTier,verdict,gear,inventory,blockers,checks,shortages,usedSlots,freeSlots,
                switches,trust,tierResult,candidateSummaries,upgrades,List.of());
        }
        /** Pre-CP-1 shape: no upgrade list. */
        public Plan(String boss, String method, Tier selectedTier, Tier shownTier, Verdict verdict,
                    List<GearCell> gear, List<SupplyCell> inventory, List<String> blockers,
                    List<String> checks, List<String> shortages, int usedSlots, int freeSlots,
                    List<SwitchCheck> switches, String trust, TierAwareBankResolver.Result tierResult,
                    List<String> candidateSummaries) {
            this(boss,method,selectedTier,shownTier,verdict,gear,inventory,blockers,checks,shortages,usedSlots,freeSlots,
                switches,trust,tierResult,candidateSummaries,List.of());
        }
        public boolean fallback() { return shownTier!=selectedTier; }
        public boolean playable() { return verdict==Verdict.READY || verdict==Verdict.READY_SHORT_SUPPLIES; }
        public String headline() {
            String tier=shownTier.label()+(fallback()?" fallback (selected "+selectedTier.label()+")":"");
            String up=upgrades.isEmpty()?"":" · "+upgrades.size()+" optional slot"+(upgrades.size()==1?"":"s")+" empty";
            return switch(verdict) {
                case READY -> "TRIP READY · "+tier+up;
                case READY_SHORT_SUPPLIES -> "GEAR READY · SUPPLIES SHORT · "+tier+up;
                case VERIFY -> "OWNED · VERIFY "+checks.size()+" · "+tier+up;
                case NOT_READY -> "NOT RECOMMENDED · "+blockers.size()+" blocker"+(blockers.size()==1?"":"s");
                case UNKNOWN -> "UNKNOWN · open bank for a fresh snapshot";
                case NOT_AVAILABLE_WILDERNESS -> "BEST IN BANK OFF · WILDERNESS · Wiki/guide only";
            };
        }
    }
    private static final String[] SLOT_NAMES = {"Head","Cape","Neck","Ammo","Weapon","Body","Shield","Legs","Gloves","Boots","Ring"};

    /** No trusted snapshot: return the unchanged Wiki layout labelled UNKNOWN. */
    public static Plan unknown(String boss, String method, Tier tier) {
        var sel=TieredOutfitterCatalog.find(boss,method,tier);
        if(sel==null)throw new IllegalArgumentException("No supported tier");
        Profile p=sel.profile();
        List<GearCell> g=new ArrayList<>();
        for(Item i:p.gear()) g.add(new GearCell(i,i,i==null?GearStatus.EMPTY:GearStatus.NOT_OBSERVED,"No trusted bank snapshot"));
        List<SupplyCell> inv=new ArrayList<>();
        for(Item i:p.inventory()) inv.add(new SupplyCell(i,i,i==null?SupplyStatus.EMPTY:SupplyStatus.NOT_OBSERVED,"No trusted bank snapshot"));
        int used=(int)p.inventory().stream().filter(Objects::nonNull).count();
        return new Plan(boss,method,tier,tier,Verdict.UNKNOWN,g,inv,List.of(),List.of(),List.of(),used,28-used,
            List.of(),"No bank snapshot this login",null,List.of());
    }

    /** Unchanged Wiki layout, no ownership or readiness of any kind. */
    public static Plan wildernessBlocked(String boss, String method, Tier tier) {
        var u=unknown(boss,method,tier);
        List<GearCell> g=new ArrayList<>();
        for(var c:u.gear()) g.add(new GearCell(c.planned(),c.planned(),c.planned()==null?GearStatus.EMPTY:GearStatus.NOT_OBSERVED,"Wilderness: guide only"));
        List<SupplyCell> inv=new ArrayList<>();
        for(var c:u.inventory()) inv.add(new SupplyCell(c.planned(),c.planned(),c.planned()==null?SupplyStatus.EMPTY:SupplyStatus.NOT_OBSERVED,"Wilderness: guide only"));
        return new Plan(boss,method,tier,tier,Verdict.NOT_AVAILABLE_WILDERNESS,g,inv,List.of(),List.of(),List.of(),
            u.usedSlots(),u.freeSlots(),List.of(),"Best in Bank disabled for Wilderness bosses",null,List.of());
    }
    public static Plan plan(String boss, String method, Tier selected, BankEvidence evidence,
                            AccountContext context, ItemFacts facts) {
        return plan(boss,method,selected,evidence,context,facts,Map.of());
    }
    public static Plan plan(String boss, String method, Tier selected, BankEvidence evidence,
                            AccountContext context, ItemFacts facts, Map<Integer,String> names) {
        return plan(boss,method,selected,evidence,context,facts,names,Map.of(),OwnedGearStats.Focus.MATCH);
    }
    /**
     * CP-1: plus RuneLite names of the observed IDs (reviewed {@link NamedVariantRules}).
     * CP-7: plus RuneLite equipment stats of observed IDs and the owner's fill focus: slots the Wiki plan leaves empty
     * are filled with the player's own best item for that focus ({@link OwnedGearStats}).
     */
    public static Plan plan(String boss, String method, Tier selected, BankEvidence evidence,
                            AccountContext context, ItemFacts facts, Map<Integer,String> names,
                            Map<Integer,OwnedGearStats.Stats> stats, OwnedGearStats.Focus focus) {
        return plan(boss,method,selected,evidence,context,facts,names,stats,focus,SlayerTask.UNKNOWN);
    }
    /** Step 2: plus the live Slayer task. UNKNOWN keeps today's behaviour and never grants an on-task bonus. */
    public static Plan plan(String boss, String method, Tier selected, BankEvidence evidence,
                            AccountContext context, ItemFacts facts, Map<Integer,String> names,
                            Map<Integer,OwnedGearStats.Stats> stats, OwnedGearStats.Focus focus, SlayerTask task) {
        if(task==null)task=SlayerTask.UNKNOWN;
        if(names==null)names=Map.of();
        if(stats==null)stats=Map.of();
        if(focus==null)focus=OwnedGearStats.Focus.MATCH;
        Objects.requireNonNull(boss); Objects.requireNonNull(method); Objects.requireNonNull(selected);
        // Build 20 owner-locked rule, enforced in the solver itself (not only by hiding a button):
        // never derive an owned-item plan for a Wilderness or unregistered boss.
        if(!BossRegistry.bestBankAllowed(boss)) return wildernessBlocked(boss,method,selected);
        // BQA-901 BF-04: only a missing observation is UNKNOWN. A legacy SINGLE_READ fixture with an empty
        // bank carries no attestation and stays UNKNOWN; a stable live read of an empty bank is planned.
        if(evidence==null || !evidence.planningInput())
            return unknown(boss,method,selected);
        if(context==null) context=AccountContext.UNKNOWN;
        if(facts==null) facts=ItemFacts.catalogDerived();
        var chosen=TieredOutfitterCatalog.find(boss,method,selected);
        if(chosen==null)throw new IllegalArgumentException("No supported tier");
        String style=chosen.profile().style();
        List<Plan> candidates=new ArrayList<>();
        for(int t=selected.ordinal();t>=0;t--) {
            Tier tier=Tier.values()[t];
            var sel=TieredOutfitterCatalog.find(boss,method,tier);
            if(sel==null || !style.equals(sel.profile().style()))continue;
            // W03: apply independently approved exact-ID intelligence to each whole-loadout
            // candidate BEFORE ranking. Current W02 research is CHECK-only and changes nothing.
            // Bank R05 snapshots, O tag ownership and withdrawal lifecycle remain untouched.
            Plan candidate=evaluate(boss,method,selected,tier,sel.profile(),evidence,context,facts,names,stats,focus,task);
            candidates.add(gg.feral.burrow.intelligence.BestBankIntelligenceBridge.enrich(candidate,evidence,context));
        }
        Plan best=candidates.get(0);
        for(Plan p:candidates) if(better(p,best)) best=p; // ties keep higher tier
        List<String> summaries=new ArrayList<>();
        for(Plan p:candidates) summaries.add(p.shownTier().label()+": "+p.verdict()+
            (p.blockers().isEmpty()?"":" ("+p.blockers().size()+" blocker"+(p.blockers().size()==1?"":"s")+": "
                +String.join("; ",p.blockers().subList(0,Math.min(2,p.blockers().size())))+(p.blockers().size()>2?"; ...":"")+")")
            +(p.upgrades().isEmpty()?"":" ["+p.upgrades().size()+" optional empty]")
            +(p==best?" <- shown":""));
        return new Plan(best.boss(),best.method(),selected,best.shownTier(),best.verdict(),best.gear(),best.inventory(),
            best.blockers(),best.checks(),best.shortages(),best.usedSlots(),best.freeSlots(),best.switches(),
            best.trust(),best.tierResult(),summaries,best.upgrades(),best.options());
    }
    /**
     * CP-1: a candidate wins when it is usable where the other is not; then when it leaves FEWER optional slots
     * empty (owned higher-tier armour beats a bare lower-tier set); then by verdict. Ties keep the higher tier.
     */
    /** CP-1: this many empty optional slots turns a playable trip into VERIFY rather than READY. */
    static final int WEAK_TRIP_EMPTY_SLOTS = 3;
    static boolean better(Plan a, Plan b) {
        boolean ua=usable(a.verdict()), ub=usable(b.verdict());
        if(ua!=ub) return ua;
        if(ua && a.upgrades().size()!=b.upgrades().size()) return a.upgrades().size()<b.upgrades().size();
        return rank(a.verdict())>rank(b.verdict());
    }
    private static boolean usable(Verdict v) { return v==Verdict.READY || v==Verdict.READY_SHORT_SUPPLIES || v==Verdict.VERIFY; }
    static int rank(Verdict v) {
        return switch(v){case READY->4;case READY_SHORT_SUPPLIES->3;case VERIFY->2;case NOT_READY->1;case UNKNOWN,NOT_AVAILABLE_WILDERNESS->0;};
    }

    private static Plan evaluate(String boss,String method,Tier selected,Tier tier,Profile profile,
                                 BankEvidence evidence,AccountContext ctx,ItemFacts facts,Map<Integer,String> names,
                                 Map<Integer,OwnedGearStats.Stats> stats,OwnedGearStats.Focus focus,SlayerTask task) {
        Map<Integer,Integer> combined=evidence.combined();
        Map<Integer,Integer> remaining=new HashMap<>(combined);
        TierAwareBankResolver.Result slots=TierAwareBankResolver.resolve(boss,method,tier,combined,names);
        List<String> blockers=new ArrayList<>(), checks=new ArrayList<>(), shortages=new ArrayList<>(), upgrades=new ArrayList<>();
        List<GearCell> gear=new ArrayList<>(11);
        Set<Integer> chargeIds=chargedItemIds(boss,method,tier);
        for(int i=0;i<11;i++) {
            var s=slots.slots().get(i);
            Item planned=profile.gear().get(i);
            // CP-1: an adopted lower-tier weapon set can fill a slot this tier left empty (e.g. defender after a 2H).
            if(planned==null && s.kind()!=TierAwareBankResolver.Kind.EMPTY && s.displayed()!=null) planned=s.displayed();
            if(planned==null || s.kind()==TierAwareBankResolver.Kind.EMPTY) {
                // R05: a slot emptied by a coupled lower-tier weapon set shows nothing, not the unowned Wiki item.
                Item shownEmpty=planned!=null && s.displayed()==null?null:planned;
                gear.add(new GearCell(planned,shownEmpty,GearStatus.EMPTY,planned!=null && shownEmpty==null?s.note():"No item planned"));continue;
            }
            boolean core=isCore(i);
            if(!s.confirmed()) {
                Item missing=s.displayed()==null?planned:s.displayed();
                GearStatus st=s.kind()==TierAwareBankResolver.Kind.PARTIAL_STACK?GearStatus.PARTIAL_STACK:GearStatus.NOT_OBSERVED;
                // CP-1 (TRIP-X02): only weapons, projectile ammo and mechanic protection block a trip. An unowned
                // ring/cape/armour piece/non-mechanic offhand leaves the slot empty and is listed as an upgrade.
                if(!SlotCriticality.blocking(boss,i,missing)) {
                    upgrades.add(SLOT_NAMES[i]+": "+missing.name()+(st==GearStatus.PARTIAL_STACK?" (quantity short)":"")+" not owned; slot left empty");
                    gear.add(new GearCell(planned,missing,GearStatus.OPTIONAL_MISSING,"Optional upgrade, not owned: "+s.note()));continue;
                }
                String text=SLOT_NAMES[i]+": "+missing.name()+(st==GearStatus.PARTIAL_STACK?" quantity short":" not observed");
                blockers.add((core?"CORE ":"")+text);
                gear.add(new GearCell(planned,missing,st,s.note()));continue;
            }
            Item shown=s.displayed();
            int take=Math.max(1,shown.amount());
            allocate(remaining,shown.id(),take);
            GearStatus status=switch(s.kind()){
                case EXACT -> GearStatus.OWNED;
                case REVIEWED_VARIANT -> GearStatus.OWNED_VARIANT;
                case LOW_STOCK_OWNED -> GearStatus.LOW_STOCK;
                default -> GearStatus.OWNED_SUBSTITUTE;
            };
            if(status==GearStatus.LOW_STOCK) shortages.add(SLOT_NAMES[i]+": "+s.note());
            String note=s.note();
            var req=requirement(shown.id(),planned.id(),ctx);
            if(req.status!=null) {
                status=req.status;
                note=req.text;
                if(req.status==GearStatus.REQUIREMENT_UNKNOWN) checks.add(SLOT_NAMES[i]+": "+req.text);
                else blockers.add(SLOT_NAMES[i]+": "+req.text);
            }
            String lower=shown.name().toLowerCase(Locale.ROOT);
            if(lower.contains("slayer helm") || lower.contains("black mask"))
                checks.add(SLOT_NAMES[i]+": "+shown.name()+" bonus applies only on a matching Slayer task");
            if(chargeIds.contains(planned.id()) || chargeIds.contains(shown.id()))
                checks.add(SLOT_NAMES[i]+": "+shown.name()+" charges cannot be read from the bank; check before leaving");
            gear.add(new GearCell(planned,shown,status,note));
        }
        // CP-7: no owned Wiki/tier weapon at all -> strongest owned MELEE weapon from RuneLite stats.
        if(!gear.get(4).status().physicallyOwned() && gear.get(4).planned()!=null) {
            var pick=ownedWeaponFallback(boss,method,tier,profile,gear,remaining,stats,names,ctx,focus,facts);
            if(pick!=null) {
                Item wikiWeapon=gear.get(4).planned();
                blockers.removeIf(b->b.startsWith("CORE Weapon:")||b.startsWith("Weapon:"));
                allocate(remaining,pick.itemId(),1);
                gear.set(4,new GearCell(wikiWeapon,new Item(pick.itemId(),pick.name(),1),GearStatus.OWNED_SUBSTITUTE,
                    pick.why()+" · Wiki plan: "+wikiWeapon.name()+" · check its attack style suits "+boss+" and wear requirements"));
                checks.add("Weapon: "+pick.name()+" picked from your bank (Wiki plan: "+wikiWeapon.name()+"); check attack style and requirements");
                if(OwnedGearStats.style(profile.style())==OwnedGearStats.Style.MAGIC)
                    checks.add("Weapon: Burrow does not plan spells yet; bring the runes or charges "+pick.name()+" needs");
                var shieldCell=gear.get(6);
                if(pick.twoHanded() && shieldCell.shown()!=null && shieldCell.status().physicallyOwned()) {
                    remaining.merge(shieldCell.shown().id(),1,Integer::sum);
                    gear.set(6,new GearCell(shieldCell.planned(),null,GearStatus.EMPTY,"Removed: "+pick.name()+" is two-handed"));
                }
            }
        }
        reviewMagicWeapon(boss,method,tier,profile,gear,blockers,checks,remaining,names,ctx);
        // CP-8: crossbow plans: bring the best owned bolts the crossbow can fire (e.g. diamond (e) over broad).
        reviewCrossbowBolts(gear,blockers,shortages,remaining,stats,names);
        // Step 2: every Ranged weapon's ammo slot is checked against typed weapon rules.
        reviewAmmo(profile,gear,blockers,checks,shortages,remaining,stats,names,ctx);
        // W07 independent sanity: Kree'arra tank's source has a Twisted bow fallback
        // without a trip-sized arrow quantity. The same-boss Tbow row establishes
        // an arrow TYPE, not how many arrows are enough for a visit. A synthetic
        // bank holding one arrow must never make this entry plan TRIP READY.
        if (profile.notes().stream().anyMatch(n -> n.startsWith(
                "Ammo: Kree'arra tank table has no Twisted bow arrows;")))
            checks.add("Ammo: Kree'arra Twisted bow source gives no trip arrow quantity; verify stack before leaving");
        // CP-7: fill slots the plan leaves empty with the player's own best item for the chosen focus.
        int filled=fillFromOwnedStats(boss,profile,gear,upgrades,remaining,stats,names,focus,ctx,facts);
        if(filled>0) checks.add(filled+" slot"+(filled==1?"":"s")+(focus==OwnedGearStats.Focus.MATCH
            ?" use your closest-statted or clearly better owned item":" use your best owned "+focus.label.toLowerCase(Locale.ROOT)+" item")
            +" (RuneLite stats); check wear requirements");
        List<String> options=new ArrayList<>();
        // Step 2: the owner's Ranged cape order and live Slayer eligibility, after owned-stat fills.
        reviewRangedCape(boss,profile,gear,upgrades,options,checks,remaining,names,ctx);
        reviewSlayerHead(boss,method,profile,gear,checks,options,upgrades,remaining,names,ctx,focus,task);
        reviewProtection(boss,gear,blockers,checks);
        OwnedGearStats.voidOption(profile.style(),combined,names).ifPresent(v->
            options.add("You own full "+v.label()+": its set effect may beat this plan; compare before swapping"));
        // CP-1: set effects need every planned piece of the set; a partial set loses the effect.
        boolean[] ownedSet=new boolean[11];
        for(int i=0;i<11;i++) {
            var c=gear.get(i);
            // Step 2: occupancy is not membership; a substitute only counts when it is really part of the set.
            ownedSet[i]=c.status().physicallyOwned() && c.status()!=GearStatus.LEVEL_UNMET && c.status()!=GearStatus.QUEST_UNMET
                && SlotCriticality.sameSet(c.planned(),c.shown());
        }
        for(var sc:SlotCriticality.setEffects(method,profile.gear(),ownedSet)) {
            if(sc.complete())continue;
            if(sc.methodDepends()) blockers.add(sc.text());
            else if(!sc.partial()) continue;
            else checks.add(sc.text());
        }
        // Two-handed weapon with an occupied shield slot is never a valid worn set.
        Item weapon=gear.get(4).shown(), shield=gear.get(6).shown();
        if(weapon!=null && shield!=null && gear.get(4).status().physicallyOwned() && gear.get(6).status().physicallyOwned()) {
            var wf=facts.find(weapon.id());
            if(wf.isPresent() && wf.get().hands()==ItemFacts.Hands.TWO)
                blockers.add("Weapon "+weapon.name()+" is two-handed and cannot be worn with "+shield.name());
        }
        // Inventory: allocate remaining physical stock once; never reorder or lock real slots.
        // CP-1: a switch item that is now WORN as the substituted weapon set is not needed again in the pack.
        Set<Integer> wornSubstitutes=new HashSet<>();
        for(int i:new int[]{3,4,6}) { // weapon set only
            var sl=slots.slots().get(i);
            if(sl.confirmed() && sl.substitute() && sl.displayed()!=null && gear.get(i).status().physicallyOwned())
                wornSubstitutes.add(sl.displayed().id());
        }
        List<SupplyCell> inv=new ArrayList<>(28);
        int used=0;
        for(int i=0;i<28;i++) {
            Item planned=profile.inventory().get(i);
            if(planned==null){inv.add(new SupplyCell(null,null,SupplyStatus.EMPTY,""));continue;}
            if(wornSubstitutes.remove(planned.id())) {
                inv.add(new SupplyCell(planned,null,SupplyStatus.EMPTY,"Worn instead: "+planned.name()+" is the owned substitute weapon"));
                continue;
            }
            used++;
            boolean switchItem=planned.id()>0 && (ItemRequirementRegistry.isEquipment(planned.id()) || facts.find(planned.id()).isPresent());
            if(planned.id()<=0) {
                checks.add("Inventory: "+planned.name()+" exact item variant unspecified by source");
                inv.add(new SupplyCell(planned,planned,SupplyStatus.VARIANT_UNSPECIFIED,"Exact variant unspecified"));continue;
            }
            if(planned.amount()<0) {
                int have=remaining.getOrDefault(planned.id(),0);
                if(have>0){allocate(remaining,planned.id(),have);
                    inv.add(new SupplyCell(planned,new Item(planned.id(),planned.name(),have),SupplyStatus.QUANTITY_UNSPECIFIED,
                        have+" observed; source gives no trip quantity"));
                    checks.add("Inventory: "+planned.name()+" quantity not specified by source ("+have+" observed)");
                } else {
                    shortages.add(planned.name()+" not observed");
                    inv.add(new SupplyCell(planned,planned,SupplyStatus.NOT_OBSERVED,"Not observed in snapshot"));
                }
                continue;
            }
            int need=Math.max(1,planned.amount());
            int exact=remaining.getOrDefault(planned.id(),0);
            if(exact>=need) {
                allocate(remaining,planned.id(),need);
                SupplyStatus st=switchItem?SupplyStatus.SWITCH_OWNED:SupplyStatus.STOCKED;
                String note=switchItem?"Owned switch":"Stocked";
                if(switchItem) {
                    var req=requirement(planned.id(),planned.id(),ctx);
                    if(req.status==GearStatus.LEVEL_UNMET || req.status==GearStatus.QUEST_UNMET) {
                        st=SupplyStatus.SWITCH_REQUIREMENT_UNMET;note=req.text;blockers.add("Switch "+req.text);
                    } else if(req.status==GearStatus.REQUIREMENT_UNKNOWN) checks.add("Switch "+req.text);
                    if(chargeIds.contains(planned.id()))
                        checks.add("Switch "+planned.name()+" charges cannot be read from the bank");
                }
                inv.add(new SupplyCell(planned,planned,st,note));continue;
            }
            if(!switchItem) {
                Integer dose=null;
                for(int candidate:DoseFamilies.candidates(planned.id())) {
                    if(candidate==planned.id())continue;
                    if(remaining.getOrDefault(candidate,0)>=need){dose=candidate;break;}
                }
                if(dose!=null) {
                    allocate(remaining,dose,need);
                    shortages.add(planned.name()+": using "+DoseFamilies.doses(dose)+"-dose");
                    inv.add(new SupplyCell(planned,new Item(dose,planned.name().replaceAll("\\(\\d\\)","("+DoseFamilies.doses(dose)+")"),need),
                        SupplyStatus.LOWER_DOSE,"Lower dose of the same potion"));
                    continue;
                }
            }
            if(exact>0) {
                allocate(remaining,planned.id(),exact);
                shortages.add(planned.name()+": "+exact+" of "+need);
                inv.add(new SupplyCell(planned,new Item(planned.id(),planned.name(),exact),SupplyStatus.SHORT,exact+" of "+need+" observed"));
                continue;
            }
            if(switchItem && optionalExtra(boss,method,planned)) {
                upgrades.add("Inventory: "+planned.name()+" not owned; optional extra");
                inv.add(new SupplyCell(planned,planned,SupplyStatus.SWITCH_NOT_OBSERVED,"Optional extra, not owned"));
            } else if(switchItem) {
                blockers.add("Switch "+planned.name()+" not observed");
                inv.add(new SupplyCell(planned,planned,SupplyStatus.SWITCH_NOT_OBSERVED,"Switch not observed"));
            } else {
                shortages.add(planned.name()+" not observed");
                inv.add(new SupplyCell(planned,planned,SupplyStatus.NOT_OBSERVED,"Not observed in snapshot"));
            }
        }
        // W07: source plan cells may be placeholders for absent bank items. Do not
        // count them as physically packed or use them to 'prove' a switch fits.
        used=(int)inv.stream().filter(c -> switch(c.status()) {
            case STOCKED, LOWER_DOSE, SHORT, QUANTITY_UNSPECIFIED,
                 SWITCH_OWNED, SWITCH_REQUIREMENT_UNMET -> c.shown()!=null && c.shown().id()>0;
            default -> false;
        }).count();
        int free=28-used;
        List<Item> presentSwitches=new ArrayList<>();
        for(SupplyCell c:inv) if(c.status()==SupplyStatus.SWITCH_OWNED && c.shown()!=null)
            presentSwitches.add(c.shown());
        List<SwitchCheck> switches=simulateSwitches(gear,presentSwitches,free,facts);
        for(var sw:switches) {
            if(!sw.verified()) checks.add(sw.label()+": hand count unknown, switch space unverified");
            else if(!sw.fits()) blockers.add(sw.label()+" needs "+sw.freeSlotsNeeded()+" free inventory slot(s); plan leaves "+free);
        }
        if(evidence.stale()) checks.add("Inventory or worn gear changed after the bank closed; reopen bank to refresh");
        // CP-1: playable, but not silently "ready" when much of the Wiki protection/damage gear is missing.
        if(upgrades.size()>=WEAK_TRIP_EMPTY_SLOTS)
            checks.add(upgrades.size()+" optional slots empty: noticeably weaker than the Wiki plan; check your defence and damage");
        Verdict verdict=!blockers.isEmpty()?Verdict.NOT_READY:!checks.isEmpty()?Verdict.VERIFY:
            !shortages.isEmpty()?Verdict.READY_SHORT_SUPPLIES:Verdict.READY;
        return new Plan(boss,method,selected,tier,verdict,gear,inv,dedupe(blockers),dedupe(checks),dedupe(shortages),
            used,free,switches,evidence.trustLine(),slots,List.of(),dedupe(upgrades),options);
    }
    /** CP-7: strongest owned melee weapon (damage focus) passing known wear requirements; melee plans only. */
    /**
     * Magic plans only knew the three weapons of their own tier chain, so a usable trident in the bank was ignored.
     * Walks the method's weapon order from the planned weapon downward and takes the first owned trident that beats
     * what the plan has now. The planned weapon is the ceiling, so a tier never gains a stronger weapon.
     */
    private static void reviewMagicWeapon(String boss,String method,Tier tier,Profile profile,List<GearCell> gear,List<String> blockers,
                                          List<String> checks,Map<Integer,Integer> remaining,Map<Integer,String> names,AccountContext ctx) {
        if(OwnedGearStats.style(profile.style())!=OwnedGearStats.Style.MAGIC)return;
        var cell=gear.get(4);
        Item planned=cell.planned();
        if(planned==null)return;
        List<Integer> order=magicWeaponOrder(boss,method,tier,profile.style());
        int ceiling=order.indexOf(planned.id());
        if(ceiling<0)return;
        boolean usable=cell.shown()!=null && cell.status().physicallyOwned()
            && cell.status()!=GearStatus.LEVEL_UNMET && cell.status()!=GearStatus.QUEST_UNMET;
        int current=usable?order.indexOf(gg.feral.burrow.bib.MagicWeaponRules.canonical(cell.shown().id())):-1;
        int limit=current>=0?current:order.size();
        for(int rank=ceiling;rank<limit;rank++) {
            int wikiId=order.get(rank);
            if(!gg.feral.burrow.bib.MagicWeaponRules.isTrident(wikiId))continue;
            int owned=gg.feral.burrow.bib.MagicWeaponRules.ownedVariant(wikiId,remaining);
            if(owned<=0)continue;
            int magic=gg.feral.burrow.bib.MagicWeaponRules.magicLevel(owned);
            if(ctx.level("Magic",magic)==AccountContext.Check.UNMET)continue;
            if(usable)remaining.merge(cell.shown().id(),Math.max(1,cell.shown().amount()),Integer::sum);
            allocate(remaining,owned,1);
            blockers.removeIf(b->b.startsWith("CORE Weapon:")||b.startsWith("Weapon:"));
            String name=names.getOrDefault(owned,wikiId==gg.feral.burrow.bib.MagicWeaponRules.SEAS?"Trident of the seas":"Trident of the swamp");
            gear.set(4,new GearCell(planned,new Item(owned,name,1),GearStatus.OWNED_SUBSTITUTE,
                "Owned Wiki weapon for this boss · Wiki plan: "+planned.name()));
            checks.add("Weapon: "+name+" charges cannot be read from the bank; check before leaving");
            if(ctx.level("Magic",magic)==AccountContext.Check.UNKNOWN)
                checks.add("Weapon: "+name+" needs "+magic+" Magic (level not observed)");
            return;
        }
    }
    /**
     * Owner QA (Zilyana): a Ranged or Magic plan must not stop at the kit's one weapon. Walks the method's weapon list
     * from the planned weapon down, preferring a weapon whose ammo is owned, then falls back to the best owned weapon of
     * that style by RuneLite stats. Weapons listed above the planned one stay out, so a tier keeps its ceiling.
     */
    private static OwnedGearStats.Pick ownedRangedOrMagicWeapon(String boss,String method,Tier tier,Profile profile,List<GearCell> gear,
                                                                Map<Integer,Integer> remaining,Map<Integer,OwnedGearStats.Stats> stats,
                                                                Map<Integer,String> names,AccountContext ctx,ItemFacts facts) {
        boolean ranged=OwnedGearStats.style(profile.style())==OwnedGearStats.Style.RANGED;
        Item planned=gear.get(4).planned();
        List<Integer> order=magicWeaponOrder(boss,method,tier,profile.style());
        int ceiling=planned==null?-1:order.indexOf(planned.id());
        Set<Integer> skip=new HashSet<>();
        for(Item it:profile.inventory()) if(it!=null && it.id()>0) skip.add(it.id());
        if(ceiling>0) skip.addAll(order.subList(0,ceiling));
        OwnedGearStats.Pick noAmmoYet=null;
        for(int rank=Math.max(0,ceiling);rank<order.size();rank++) {
            int wikiId=order.get(rank);
            int owned=gg.feral.burrow.bib.MagicWeaponRules.isTrident(wikiId)?gg.feral.burrow.bib.MagicWeaponRules.ownedVariant(wikiId,remaining)
                :remaining.getOrDefault(wikiId,0)>0?wikiId:-1;
            if(owned<=0 || skip.contains(owned) || OwnedGearStats.unusableName(names.get(owned)) || isUnmet(requirement(owned,owned,ctx)))continue;
            if(gg.feral.burrow.bib.MagicWeaponRules.isTrident(owned) && ctx.level("Magic",gg.feral.burrow.bib.MagicWeaponRules.magicLevel(owned))==AccountContext.Check.UNMET)continue;
            boolean twoHanded=facts.find(owned).map(f->f.hands()==ItemFacts.Hands.TWO).orElse(false)
                || Optional.ofNullable(stats.get(owned)).map(OwnedGearStats.Stats::twoHanded).orElse(false);
            if(twoHanded && shieldRequired(boss,gear))continue;
            String name=names.getOrDefault(owned,itemName(profile,wikiId));
            var pick=new OwnedGearStats.Pick(owned,name,0,"Owned Wiki weapon for this boss",twoHanded);
            if(!ranged || AmmoRules.slotIsFree(name) || OwnedGearStats.bestCompatibleAmmo(name,1,remaining,stats,names).isPresent())return pick;
            if(noAmmoYet==null)noAmmoYet=pick;
        }
        if(noAmmoYet!=null)return noAmmoYet;
        Set<Integer> exclude=new HashSet<>(skip);
        for(int attempt=0;attempt<6;attempt++) {
            var c=OwnedGearStats.bestRangedOrMagic(profile.style(),remaining,stats,names,exclude);
            if(c.isEmpty())return null;
            if(isUnmet(requirement(c.get().itemId(),c.get().itemId(),ctx)) || (c.get().twoHanded() && shieldRequired(boss,gear))){exclude.add(c.get().itemId());continue;}
            return c.get();
        }
        return null;
    }
    private static String itemName(Profile profile,int id) {
        for(int t=0;t<Tier.values().length;t++) {
            var sel=TieredOutfitterCatalog.find(profile.boss(),profile.method(),Tier.values()[t]);
            if(sel==null)continue;
            Item w=sel.profile().gear().get(4);
            if(w!=null && w.id()==id)return w.name();
        }
        return WikiStrategyLoadouts.itemName(id).orElse("Item "+id);
    }
    /** The Wiki weapon row, or for curated methods the tier chain with the seas trident directly under the swamp trident. */
    private static List<Integer> magicWeaponOrder(String boss,String method,Tier tier,String style) {
        List<Integer> wiki=WikiStrategyLoadouts.weaponOrder(boss,method);
        if(!wiki.isEmpty())return wiki;
        List<Integer> chain=new ArrayList<>();
        for(int t=tier.ordinal();t>=0;t--) {
            var sel=TieredOutfitterCatalog.find(boss,method,Tier.values()[t]);
            if(sel==null || !style.equals(sel.profile().style()))continue;
            Item weapon=sel.profile().gear().get(4);
            if(weapon==null || chain.contains(weapon.id()))continue;
            chain.add(weapon.id());
            if(weapon.id()==gg.feral.burrow.bib.MagicWeaponRules.SWAMP)chain.add(gg.feral.burrow.bib.MagicWeaponRules.SEAS);
        }
        return chain;
    }
    private static OwnedGearStats.Pick ownedWeaponFallback(String boss,String method,Tier tier,Profile profile,List<GearCell> gear,Map<Integer,Integer> remaining,
                                                           Map<Integer,OwnedGearStats.Stats> stats,Map<Integer,String> names,AccountContext ctx,
                                                           OwnedGearStats.Focus focus,ItemFacts facts) {
        if(OwnedGearStats.style(profile.style())!=OwnedGearStats.Style.MELEE)
            return ownedRangedOrMagicWeapon(boss,method,tier,profile,gear,remaining,stats,names,ctx,facts);
        if(stats.isEmpty())return null;
        Set<Integer> exclude=new HashSet<>();
        for(Item it:profile.inventory()) if(it!=null && it.id()>0) exclude.add(it.id()); // keep the plan's switches
        for(int attempt=0;attempt<6;attempt++) {
            Item wiki=gear.get(4).planned();
            var target=wiki==null?null:stats.get(wiki.id());
            var c=focus==OwnedGearStats.Focus.MATCH && target!=null
                ?OwnedGearStats.closest(ItemFacts.WEAPON,profile.style(),target,wiki.name(),remaining,stats,names,exclude)
                :OwnedGearStats.best(ItemFacts.WEAPON,profile.style(),OwnedGearStats.Focus.DAMAGE,remaining,stats,names,exclude);
            if(c.isEmpty())return null;
            var req=requirement(c.get().itemId(),c.get().itemId(),ctx);
            if(req.status==GearStatus.LEVEL_UNMET || req.status==GearStatus.QUEST_UNMET){exclude.add(c.get().itemId());continue;}
            // Step 2: a two-handed pick must never remove a shield this encounter requires.
            if(c.get().twoHanded() && shieldRequired(boss,gear)){exclude.add(c.get().itemId());continue;}
            return c.get();
        }
        return null;
    }
    /** CP-7: returns how many slots were filled. Never the weapon, projectile ammo or mechanic-protection slots. */
    private static int fillFromOwnedStats(String boss,Profile profile,List<GearCell> gear,List<String> upgrades,
                                          Map<Integer,Integer> remaining,Map<Integer,OwnedGearStats.Stats> stats,
                                          Map<Integer,String> names,OwnedGearStats.Focus focus,AccountContext ctx,ItemFacts facts) {
        if(stats.isEmpty())return 0;
        Set<Integer> reserved=new HashSet<>(); // the plan's own pack items (switches, supplies) are never worn instead
        for(Item it:profile.inventory()) if(it!=null && it.id()>0) reserved.add(it.id());
        for(var c:gear) if(c.shown()!=null && c.status().physicallyOwned()) reserved.add(c.shown().id());
        var weaponCell=gear.get(4);
        boolean twoHanded=weaponCell.shown()!=null && weaponCell.status().physicallyOwned()
            && (facts.find(weaponCell.shown().id()).map(f->f.hands()==ItemFacts.Hands.TWO).orElse(false)
                || Optional.ofNullable(stats.get(weaponCell.shown().id())).map(OwnedGearStats.Stats::twoHanded).orElse(false));
        int filled=0;
        for(int i=0;i<11;i++) {
            if(i==4)continue;
            var c=gear.get(i);
            boolean open=c.status()==GearStatus.OPTIONAL_MISSING || (c.status()==GearStatus.EMPTY && c.planned()==null);
            if(!open) {
                // Owned Wiki/tier item: replace it only by a clearly better owned item of the same kind.
                boolean ownedUsable=c.shown()!=null && c.status().physicallyOwned()
                    && c.status()!=GearStatus.LEVEL_UNMET && c.status()!=GearStatus.QUEST_UNMET;
                if(!ownedUsable || SlotCriticality.protectedItem(boss,i,c.shown()) || setPiece(c.shown()))continue;
                if(i==3 && OwnedGearStats.style(profile.style())==OwnedGearStats.Style.RANGED)continue;
                var currentStats=stats.get(c.shown().id());
                if(currentStats==null)continue;
                Set<Integer> ex=new HashSet<>(reserved);
                OwnedGearStats.Pick up=null;
                for(int attempt=0;attempt<6;attempt++) {
                    var cand=OwnedGearStats.upgradeOver(ItemFacts.CATALOG_TO_SLOT[i],profile.style(),focus,currentStats,
                        c.shown().name(),remaining,stats,names,ex);
                    if(cand.isEmpty())break;
                    var req=requirement(cand.get().itemId(),cand.get().itemId(),ctx);
                    if(req.status==GearStatus.LEVEL_UNMET || req.status==GearStatus.QUEST_UNMET){ex.add(cand.get().itemId());continue;}
                    up=cand.get();break;
                }
                if(up==null)continue;
                if(i==6 && up.twoHanded())continue;
                remaining.merge(c.shown().id(),1,Integer::sum); // the Wiki item goes back to the bank
                allocate(remaining,up.itemId(),1);
                reserved.add(up.itemId());
                gear.set(i,new GearCell(c.planned(),new Item(up.itemId(),up.name(),1),GearStatus.OWNED_SUBSTITUTE,
                    up.why()+" · wear requirements not checked"));
                filled++;
                continue;
            }
            if(c.planned()!=null && SlotCriticality.blocking(boss,i,c.planned()))continue;
            if(i==6 && (twoHanded || !weaponCell.status().physicallyOwned()))continue;
            if(i==3 && c.planned()==null && profile.gear().get(4)!=null && profile.gear().get(4).name()!=null
                && profile.gear().get(4).name().toLowerCase(Locale.ROOT).matches(".*(bow|crossbow|ballista|knife|dart|thrownaxe|chinchompa).*"))continue;
            Set<Integer> exclude=new HashSet<>(reserved);
            OwnedGearStats.Pick pick=null;
            // Owner rule: observe the Wiki item, find the closest-statted owned item, choose it. A slot the Wiki plan
            // leaves empty stays empty in MATCH mode; the Damage/Defence/Prayer focuses may fill it.
            Item wiki=c.planned();
            var target=wiki==null?null:stats.get(wiki.id());
            if(focus==OwnedGearStats.Focus.MATCH && wiki==null)continue;
            OwnedGearStats.Focus scoring=focus==OwnedGearStats.Focus.MATCH?OwnedGearStats.Focus.DAMAGE:focus;
            for(int attempt=0;attempt<6;attempt++) {
                var candidate=focus==OwnedGearStats.Focus.MATCH && target!=null
                    ?OwnedGearStats.closest(ItemFacts.CATALOG_TO_SLOT[i],profile.style(),target,wiki.name(),remaining,stats,names,exclude)
                    :OwnedGearStats.best(ItemFacts.CATALOG_TO_SLOT[i],profile.style(),scoring,remaining,stats,names,exclude);
                if(candidate.isEmpty())break;
                var req=requirement(candidate.get().itemId(),candidate.get().itemId(),ctx);
                if(req.status==GearStatus.LEVEL_UNMET || req.status==GearStatus.QUEST_UNMET){exclude.add(candidate.get().itemId());continue;}
                pick=candidate.get();break;
            }
            if(pick==null)continue;
            allocate(remaining,pick.itemId(),1);
            reserved.add(pick.itemId());
            String prefix=SLOT_NAMES[i]+": ";
            upgrades.removeIf(u->u.startsWith(prefix));
            Item planned=c.planned();
            gear.set(i,new GearCell(planned,new Item(pick.itemId(),pick.name(),1),GearStatus.OWNED_SUBSTITUTE,
                pick.why()+(planned==null?"":" · Wiki plan: "+planned.name())+" · wear requirements not checked"));
            filled++;
        }
        return filled;
    }
    private static void reviewCrossbowBolts(List<GearCell> gear,List<String> blockers,List<String> shortages,
                                            Map<Integer,Integer> remaining,Map<Integer,OwnedGearStats.Stats> stats,
                                            Map<Integer,String> names) {
        var weapon=gear.get(4);
        if(weapon.shown()==null || !weapon.status().physicallyOwned() || OwnedGearStats.crossbowTier(weapon.shown().name())==0)return;
        var ammo=gear.get(3);
        Item planned=ammo.planned();
        int need=planned==null?1:Math.max(1,planned.amount());
        boolean currentOwned=ammo.shown()!=null && (ammo.status().physicallyOwned() || ammo.status()==GearStatus.PARTIAL_STACK);
        int currentTake=0;
        if(currentOwned && ammo.status().physicallyOwned()) {
            currentTake=Math.max(1,ammo.shown().amount());
            remaining.merge(ammo.shown().id(),currentTake,Integer::sum); // consider the current bolts on equal terms
        }
        var pick=OwnedGearStats.bestBolts(weapon.shown().name(),need,remaining,stats,names,Set.of());
        if(pick.isEmpty()) { if(currentTake>0) allocate(remaining,ammo.shown().id(),currentTake); return; }
        var p=pick.get();
        if(currentOwned && p.itemId()==ammo.shown().id()) { allocate(remaining,ammo.shown().id(),currentTake); return; }
        var curStats=currentOwned?stats.get(ammo.shown().id()):null;
        var newStats=stats.get(p.itemId());
        boolean better=!currentOwned || ammo.status()==GearStatus.PARTIAL_STACK
            || curStats==null || (newStats!=null && newStats.rstr()>curStats.rstr() && (int)p.score()>=need);
        if(!better) { if(currentTake>0) allocate(remaining,ammo.shown().id(),currentTake); return; }
        int qty=(int)p.score();
        allocate(remaining,p.itemId(),qty);
        blockers.removeIf(b->b.startsWith("CORE Ammo")||b.startsWith("Ammo:"));
        if(qty<need) shortages.add("Ammo: "+p.name()+" "+qty+" of "+need);
        gear.set(3,new GearCell(planned,new Item(p.itemId(),p.name(),qty),qty<need?GearStatus.LOW_STOCK:GearStatus.OWNED_SUBSTITUTE,
            p.why()+(planned==null?"":" · Wiki plan: "+planned.name())));
    }
    static boolean shieldRequired(String boss,List<GearCell> gear) {
        var shield=gear.get(SlotCriticality.SHIELD);
        return SlotCriticality.demandsDragonfireWitness(boss)
            || (shield.planned()!=null && SlotCriticality.blocking(boss,SlotCriticality.SHIELD,shield.planned()));
    }
    /** Step 2: final gate on the finished setup, so no earlier swap can quietly drop required protection. */
    private static void reviewProtection(String boss,List<GearCell> gear,List<String> blockers,List<String> checks) {
        for(int i=0;i<11;i++) {
            if(i==SlotCriticality.AMMO || i==SlotCriticality.WEAPON)continue;
            var c=gear.get(i);
            if(c.planned()==null || !SlotCriticality.blocking(boss,i,c.planned()))continue;
            boolean kept=c.shown()!=null && c.status().physicallyOwned() && SlotCriticality.blocking(boss,i,c.shown());
            String slot=SLOT_NAMES[i]+":";
            if(!kept && blockers.stream().noneMatch(b->b.startsWith(slot)||b.startsWith("CORE "+slot)))
                blockers.add(slot+" required protection "+c.planned().name()+" is not in the final setup");
        }
        // W02-01: an encounter-level gate, independent of the chosen Wiki method's offhand.
        // A one-handed defender template, a missing shield, and any 2H fallback cannot silently
        // become READY just because that specific template did not list an anti-dragon shield.
        // Potions and temporary protection states are not observed by this source-only solver.
        if(SlotCriticality.demandsDragonfireWitness(boss)) {
            var offhand=gear.get(SlotCriticality.SHIELD);
            boolean owned=offhand!=null && offhand.shown()!=null && offhand.status()!=null
                && (offhand.status()==GearStatus.OWNED || offhand.status()==GearStatus.OWNED_VARIANT
                    || offhand.status()==GearStatus.OWNED_SUBSTITUTE);
            if(!owned || !SlotCriticality.witnessedDragonfireShield(offhand.shown())) {
                String text="Shield: Vorkath dragonfire protection is not verified on the worn setup; "
                    +"potion-only or two-handed alternatives require separate review";
                // Shieldless melee can work using a verified super-antifire + Protect from Magic
                // combination. This planner has bank items but NO active-prayer or potion-timer
                // witness, so return VERIFY, not READY or an assertion the fight is impossible.
                if(!checks.contains(text)) checks.add(text);
            } else {
                String name=offhand.shown().name().toLowerCase(Locale.ROOT);
                if(name.contains("dragonfire shield") || name.contains("dragonfire ward")) {
                    checks.add("Shield: verify dragonfire gear charges/active variant before a trip");
                }
            }
        }
        var cape=gear.get(1);
        if(cape.planned()!=null && cape.planned().name()!=null && cape.planned().name().toLowerCase(Locale.ROOT).contains("tortugan shield")
            && !(cape.shown()!=null && cape.status().physicallyOwned() && cape.shown().name()!=null
                && cape.shown().name().toLowerCase(Locale.ROOT).contains("tortugan shield")))
            checks.add("Cape: Tortugan shield is recommended here; without it melee damage gets through Protect from Melee");
        if("Alchemical Hydra".equals(boss)) {
            var feet=gear.get(9);
            String n=feet.shown()==null||feet.shown().name()==null||!feet.status().physicallyOwned()?"":feet.shown().name().toLowerCase(Locale.ROOT);
            if(!(n.contains("boots of stone")||n.contains("boots of brimstone")||n.contains("granite boots")))
                checks.add("Boots: Karuulm heat needs boots of stone, brimstone or granite unless the elite Kourend & Kebos diary is done");
        }
    }
    /** Step 2: Ranged plans only. A wrong or missing projectile can never leave a plan READY. */
    private static void reviewAmmo(Profile profile,List<GearCell> gear,List<String> blockers,List<String> checks,List<String> shortages,
                                   Map<Integer,Integer> remaining,Map<Integer,OwnedGearStats.Stats> stats,Map<Integer,String> names,
                                   AccountContext ctx) {
        if(OwnedGearStats.style(profile.style())!=OwnedGearStats.Style.RANGED)return;
        var weapon=gear.get(4);
        if(weapon.shown()==null || !weapon.status().physicallyOwned())return;
        String w=weapon.shown().name();
        var ammo=gear.get(3);
        boolean owned=ammo.shown()!=null && ammo.status().physicallyOwned();
        var status=AmmoRules.check(w,owned?ammo.shown().name():null);
        var internal=AmmoRules.internalResource(w);
        if(internal==AmmoRules.InternalResource.DARTS_AND_SCALES)
            checks.add("Weapon: "+w+" needs darts and scales loaded; contents cannot be read from the bank");
        else if(internal==AmmoRules.InternalResource.WEAPON_CHARGES && checks.stream().noneMatch(c->c.startsWith("Weapon: "+w)))
            checks.add("Weapon: "+w+" charges cannot be read from the bank; check before leaving");
        switch(status) {
            case OK -> {}
            case NOT_REQUIRED -> blockers.removeIf(b->b.startsWith("CORE Ammo")||b.startsWith("Ammo:"));
            case UNKNOWN_WEAPON, UNKNOWN_AMMO -> checks.add("Ammo: "+(owned?ammo.shown().name():"empty slot")+" with "+w+" not confirmed by the weapon rules");
            default -> {
                int need=ammo.planned()==null?1:Math.max(1,ammo.planned().amount());
                if(owned) remaining.merge(ammo.shown().id(),Math.max(1,ammo.shown().amount()),Integer::sum);
                Set<Integer> unusable=new HashSet<>();
                var pick=OwnedGearStats.bestCompatibleAmmo(w,need,remaining,stats,names,unusable);
                while(pick.isPresent() && isUnmet(requirement(pick.get().itemId(),pick.get().itemId(),ctx))) {
                    unusable.add(pick.get().itemId());
                    pick=OwnedGearStats.bestCompatibleAmmo(w,need,remaining,stats,names,unusable);
                }
                if(pick.isPresent()) {
                    var p=pick.get();
                    int qty=(int)p.score();
                    noteUnknownRequirement("Ammo",p.itemId(),ctx,checks);
                    allocate(remaining,p.itemId(),qty);
                    blockers.removeIf(b->b.startsWith("CORE Ammo")||b.startsWith("Ammo:"));
                    if(qty<need) shortages.add("Ammo: "+p.name()+" "+qty+" of "+need);
                    gear.set(3,new GearCell(ammo.planned(),new Item(p.itemId(),p.name(),qty),qty<need?GearStatus.LOW_STOCK:GearStatus.OWNED_SUBSTITUTE,
                        p.why()+(ammo.planned()==null?"":" · Wiki plan: "+ammo.planned().name())));
                    return;
                }
                if(owned) allocate(remaining,ammo.shown().id(),Math.max(1,ammo.shown().amount()));
                String text=status==AmmoRules.Status.MISSING?"CORE Ammo: "+w+" needs ammo and none usable is owned"
                    :"CORE Ammo: "+ammo.shown().name()+" cannot be used with "+w;
                if(blockers.stream().noneMatch(b->b.startsWith("CORE Ammo"))) blockers.add(text);
                if(owned) gear.set(3,new GearCell(ammo.planned(),ammo.shown(),GearStatus.NOT_OBSERVED,text.substring(5)));
            }
        }
    }
    /** Step 2: owner's order for any Ranged weapon. Ammo saving is a note, never proof the plan is valid. */
    private static void reviewRangedCape(String boss,Profile profile,List<GearCell> gear,List<String> upgrades,List<String> options,
                                         List<String> checks,Map<Integer,Integer> remaining,Map<Integer,String> names,AccountContext ctx) {
        if(OwnedGearStats.style(profile.style())!=OwnedGearStats.Style.RANGED)return;
        var cape=gear.get(1);
        if(cape.planned()!=null && SlotCriticality.blocking(boss,1,cape.planned()))return;
        var weapon=gear.get(4);
        Item shownWeapon=weapon.shown()!=null?weapon.shown():weapon.planned();
        if(shownWeapon==null)return;
        Integer level=ctx.levelOf("Ranged");
        boolean capeOwned=cape.shown()!=null && cape.status().physicallyOwned();
        int current=capeOwned?cape.shown().id():-1;
        var torso=RangedCapeAdvisor.torso(gear.get(5).shown()==null?null:gear.get(5).shown().name());
        Set<Integer> pool=new HashSet<>(remaining.keySet());
        for(Item it:profile.inventory()) if(it!=null) pool.remove(it.id());
        if(capeOwned) pool.add(current);
        for(int attempt=0;attempt<6;attempt++) {
            var pick=RangedCapeAdvisor.pick(pool,level==null?current>0?99:0:level,shownWeapon.name(),torso,RangedCapeAdvisor.QuiverUpgrade.UNKNOWN);
            if(pick.itemId<=0)return;
            String saving=RangedCapeAdvisor.savingNote(pick.saving);
            if(level==null || RangedCapeAdvisor.rank(current)>=RangedCapeAdvisor.rank(pick.itemId)) {
                if(capeOwned && RangedCapeAdvisor.rank(current)>0)
                    gear.set(1,new GearCell(cape.planned(),cape.shown(),cape.status(),cape.note()+" · "+saving));
                return;
            }
            if(isUnmet(requirement(pick.itemId,pick.itemId,ctx))){pool.remove(pick.itemId);continue;}
            noteUnknownRequirement("Cape",pick.itemId,ctx,checks);
            if(capeOwned) remaining.merge(current,1,Integer::sum);
            allocate(remaining,pick.itemId,1);
            upgrades.removeIf(u->u.startsWith("Cape: "));
            String name=names.getOrDefault(pick.itemId,"Ranged cape");
            gear.set(1,new GearCell(cape.planned(),new Item(pick.itemId,name,1),GearStatus.OWNED_SUBSTITUTE,
                "Owner's Ranged cape order · "+saving+(cape.planned()==null?"":" · Wiki plan: "+cape.planned().name())));
            if(pick.saving==RangedCapeAdvisor.Saving.OFF_METAL_TORSO) options.add(name+": "+saving);
            return;
        }
    }
    /** Step 2: acts only on a known task state. UNKNOWN changes nothing, so an on-task bonus is never assumed. */
    private static void reviewSlayerHead(String boss,String method,Profile profile,List<GearCell> gear,List<String> checks,List<String> options,
                                         List<String> upgrades,Map<Integer,Integer> remaining,Map<Integer,String> names,AccountContext ctx,
                                         OwnedGearStats.Focus focus,SlayerTask task) {
        if(task.state==SlayerTask.State.UNKNOWN)return;
        var head=gear.get(0);
        boolean headOwned=head.shown()!=null && head.status().physicallyOwned();
        boolean wearingHelm=headOwned && HeadAdvisor.isSlayerHead(head.shown().id());
        var style=OwnedGearStats.style(profile.style());
        var in=new HeadAdvisor.Input();
        in.style=style==OwnedGearStats.Style.RANGED?HeadAdvisor.Style.RANGED:style==OwnedGearStats.Style.MAGIC?HeadAdvisor.Style.MAGIC:HeadAdvisor.Style.MELEE;
        in.focus=focus==OwnedGearStats.Focus.PRAYER?HeadAdvisor.Focus.PRAYER:HeadAdvisor.Focus.DAMAGE;
        Set<Integer> pool=new HashSet<>(remaining.keySet());
        for(Item it:profile.inventory()) if(it!=null) pool.remove(it.id());
        if(headOwned) pool.add(head.shown().id());
        in.owned=Set.copyOf(pool);
        in.boss=boss;
        in.task=task;
        in.undead=BossTaskMatrix.undead(boss);
        in.plannedNeck=gear.get(2).shown()!=null && gear.get(2).status().physicallyOwned()?gear.get(2).shown().id():-1;
        in.awakened=method.toLowerCase(Locale.ROOT).contains("awakened");
        in.setNeedsHeadSlot=headOwned && !wearingHelm && setPiece(head.shown());
        in.atlatl=gear.get(4).shown()!=null && "eclipse atlatl".equalsIgnoreCase(gear.get(4).shown().name());
        var pick=HeadAdvisor.pick(in);
        boolean mechanic=head.planned()!=null && SlotCriticality.blocking(boss,0,head.planned());
        if(pick.reason==HeadAdvisor.Reason.HELM_ON_TASK) {
            checks.removeIf(c->c.startsWith("Head: ") && c.endsWith("matching Slayer task"));
            if(wearingHelm || mechanic || isUnmet(requirement(pick.itemId,pick.itemId,ctx)))return;
            noteUnknownRequirement("Head",pick.itemId,ctx,checks);
            swapHead(head,pick.itemId,names.getOrDefault(pick.itemId,"Slayer helmet"),"On task for this boss: helmet bonus applies",gear,upgrades,remaining,headOwned);
            return;
        }
        if(!wearingHelm)return;
        if(pick.reason==HeadAdvisor.Reason.MELEE_FALLBACK && !mechanic) {
            if(isUnmet(requirement(pick.itemId,pick.itemId,ctx)))return;
            checks.removeIf(c->c.startsWith("Head: ") && c.endsWith("matching Slayer task"));
            noteUnknownRequirement("Head",pick.itemId,ctx,checks);
            swapHead(head,pick.itemId,names.getOrDefault(pick.itemId,"Melee helm"),"Not on task for this boss, so the helmet gives no bonus",gear,upgrades,remaining,headOwned);
            return;
        }
        switch(pick.reason) {
            case SALVE_TAKES_PRIORITY -> options.add("Salve is in this plan: its undead bonus replaces the Slayer helmet's, so a different helmet may be stronger");
            case EFFECT_DISABLED_AWAKENED -> options.add("Awakened fight: the Slayer helmet's on-task boost does not work here");
            default -> options.add(task.state==SlayerTask.State.NONE?"No active Slayer task: the Slayer helmet gives no bonus":"Your current task does not confirm a bonus at "+boss);
        }
    }
    private static boolean isUnmet(Req req) { return req.status==GearStatus.LEVEL_UNMET || req.status==GearStatus.QUEST_UNMET; }
    /** Step 2: a swapped-in item keeps a VERIFY check when its requirements are unknown or unlisted. */
    private static void noteUnknownRequirement(String slot,int itemId,AccountContext ctx,List<String> checks) {
        var req=requirement(itemId,itemId,ctx);
        if(req.status==GearStatus.REQUIREMENT_UNKNOWN) checks.add(slot+": "+req.text);
        else if(req.status==null && ItemRequirementRegistry.find(itemId).isEmpty()) checks.add(slot+": swapped item's wear requirements are not in the registry; check them");
    }
    private static void swapHead(GearCell head,int id,String name,String why,List<GearCell> gear,List<String> upgrades,
                                 Map<Integer,Integer> remaining,boolean headOwned) {
        if(headOwned) remaining.merge(head.shown().id(),1,Integer::sum);
        allocate(remaining,id,1);
        upgrades.removeIf(u->u.startsWith("Head: "));
        gear.set(0,new GearCell(head.planned(),new Item(id,name,1),GearStatus.OWNED_SUBSTITUTE,
            why+(head.planned()==null?"":" · Wiki plan: "+head.planned().name())));
    }
    /**
     * Owner rule: a missing dream item is an upgrade. In a Wiki setup only the Switch rows and mechanic protection are
     * required. Spec weapons and other carried gear are extras. Curated methods keep every switch, because their names
     * describe the switch the method depends on.
     */
    private static boolean optionalExtra(String boss,String method,Item item) {
        if(!method.startsWith("Wiki · ") || SlotCriticality.blocking(boss,1,item))return false;
        return !WikiStrategyLoadouts.requiredSwitch(boss,method,item.name());
    }
    private static boolean setPiece(Item it) {
        if(it==null || it.name()==null)return false;
        String n=it.name().toLowerCase(Locale.ROOT);
        for(var se:SlotCriticality.SetEffect.values()) if(n.contains(se.keyword))return true;
        return false;
    }
    private static boolean isCore(int catalogIndex) { return catalogIndex==3 || catalogIndex==4 || catalogIndex==6; }
    private static List<String> dedupe(List<String> in) { return new ArrayList<>(new LinkedHashSet<>(in)); }
    private static void allocate(Map<Integer,Integer> remaining,int id,int amount) {
        remaining.computeIfPresent(id,(k,v)->v-amount>0?v-amount:null);
    }

    private record Req(GearStatus status,String text) {}
    /** Wear-level and quest-gate check for an OWNED item. */
    private static Req requirement(int shownId,int plannedId,AccountContext ctx) {
        var entry=ItemRequirementRegistry.find(shownId).or(()->ItemRequirementRegistry.find(plannedId));
        if(entry.isEmpty())return new Req(null,null);
        var e=entry.get();
        List<String> unmet=new ArrayList<>(), unknown=new ArrayList<>();
        boolean questUnmet=false;
        for(var lv:e.wearLevels().entrySet()) {
            var c=ctx.level(lv.getKey(),lv.getValue());
            if(c==AccountContext.Check.UNMET) unmet.add(lv.getValue()+" "+lv.getKey()+" (observed "+ctx.levelOf(lv.getKey())+")");
            else if(c==AccountContext.Check.UNKNOWN) unknown.add(lv.getValue()+" "+lv.getKey());
        }
        for(String u:e.unlocks()) {
            if(!ctx.knowsQuest(u))continue; // diaries, kill counts and acquisition notes: possession proves acquisition
            if(ctx.quest(u)==AccountContext.Check.UNMET){unmet.add(u+" not completed");questUnmet=true;}
        }
        if(!unmet.isEmpty()) {
            boolean levels=unmet.stream().anyMatch(x->x.contains("observed"));
            return new Req(levels?GearStatus.LEVEL_UNMET:GearStatus.QUEST_UNMET,e.name()+" needs "+String.join(", ",unmet));
        }
        if(!unknown.isEmpty()) return new Req(GearStatus.REQUIREMENT_UNKNOWN,e.name()+" needs "+String.join(", ",unknown)+" (level not observed)");
        return new Req(null,null);
    }

    /** Charged-state requirements recorded in FINAL-06 metadata for this exact profile. */
    private static Set<Integer> chargedItemIds(String boss,String method,Tier tier) {
        var meta=Final06ProfileMetadata.find(boss,method,tier);
        if(meta==null)return Set.of();
        Set<Integer> out=new HashSet<>();
        for(String json:meta.chargedStateRequirements()) {
            try {
                Object o=MiniJson.parse(json);
                if(o instanceof Map<?,?> m && m.get("item_id") instanceof Number n) out.add(n.intValue());
            } catch(RuntimeException malformed) { /* informational only */ }
        }
        return out;
    }

    /**
     * Simulates equipping each inventory switch group from the worn set, one item at a time,
     * counting physical inventory cells. Equipping frees the item's own cell; every displaced worn
     * item (same slot, or shield displaced by a two-handed weapon, or 2H weapon displaced by a
     * shield) needs a free cell. Visual guidance only: RuneLite cannot reserve or move slots.
     */
    static List<SwitchCheck> simulateSwitches(List<GearCell> gear,List<Item> inventory,int free,ItemFacts facts) {
        Map<Integer,Integer> worn=new HashMap<>();
        for(int i=0;i<11;i++) {
            var c=gear.get(i);
            // CP-1: only gear that is actually owned is worn; an unowned optional slot is empty during switches.
            if(c.shown()!=null && (c.status()==GearStatus.OWNED || c.status()==GearStatus.OWNED_VARIANT
                || c.status()==GearStatus.OWNED_SUBSTITUTE || c.status()==GearStatus.LOW_STOCK))
                worn.put(ItemFacts.CATALOG_TO_SLOT[i],c.shown().id());
        }
        List<Item> weapons=new ArrayList<>(), others=new ArrayList<>();
        for(Item it:inventory) {
            if(it==null || it.id()<=0)continue;
            var f=facts.find(it.id());
            if(f.isEmpty())continue;
            if(f.get().equipmentSlot()==ItemFacts.WEAPON) weapons.add(it); else if(f.get().equipmentSlot()!=ItemFacts.AMMO) others.add(it);
        }
        List<SwitchCheck> out=new ArrayList<>();
        if(weapons.isEmpty() && !others.isEmpty()) out.add(simulate("Armour switch",others,worn,free,facts));
        for(Item w:weapons) {
            List<Item> group=new ArrayList<>(); group.add(w);
            var hands=facts.find(w.id()).map(ItemFacts.Fact::hands).orElse(ItemFacts.Hands.UNKNOWN);
            for(Item o:others) {
                int slot=facts.find(o.id()).get().equipmentSlot();
                if(slot==ItemFacts.SHIELD && hands==ItemFacts.Hands.TWO)continue;
                group.add(o);
            }
            out.add(simulate("Switch to "+w.name(),group,worn,free,facts));
        }
        return out;
    }
    private static SwitchCheck simulate(String label,List<Item> group,Map<Integer,Integer> wornStart,int free,ItemFacts facts) {
        Map<Integer,Integer> worn=new HashMap<>(wornStart);
        int cells=free, lowest=free;
        boolean verified=true;
        for(Item it:group) {
            var f=facts.find(it.id()).orElseThrow();
            cells+=1; // item leaves its inventory cell
            Integer displaced=worn.put(f.equipmentSlot(),it.id());
            if(displaced!=null) cells-=1;
            if(f.equipmentSlot()==ItemFacts.WEAPON && worn.containsKey(ItemFacts.SHIELD)) {
                if(f.hands()==ItemFacts.Hands.TWO){worn.remove(ItemFacts.SHIELD);cells-=1;}
                else if(f.hands()==ItemFacts.Hands.UNKNOWN) verified=false;
            }
            if(f.equipmentSlot()==ItemFacts.SHIELD && worn.containsKey(ItemFacts.WEAPON)) {
                var wf=facts.find(worn.get(ItemFacts.WEAPON));
                var h=wf.map(ItemFacts.Fact::hands).orElse(ItemFacts.Hands.UNKNOWN);
                if(h==ItemFacts.Hands.TWO){worn.remove(ItemFacts.WEAPON);cells-=1;}
                else if(h==ItemFacts.Hands.UNKNOWN) verified=false;
            }
            lowest=Math.min(lowest,cells);
        }
        int needed=lowest<0?free-lowest:0;
        return new SwitchCheck(label,verified,lowest>=0,needed);
    }
}
