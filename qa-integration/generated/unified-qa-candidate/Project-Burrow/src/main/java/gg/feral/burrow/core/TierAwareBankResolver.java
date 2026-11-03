package gg.feral.burrow.core;

import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Item;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Profile;
import java.util.*;

/** Conservative first-stage, tier-aware owned-gear fallback.
 *
 * ONLY chooses owned alternatives previously listed at a lower tier in the
 * identical boss + method + equipment slot. Wiki profiles are immutable.
 * Weapon, ammunition, offhand and set-dependent substitutions are deliberately
 * withheld until their mechanics, requirements and equipment links are proven.
 * A chosen lower-tier item is an OWNED CANDIDATE, not a certified DPS winner.
 * Does not call RuneLite APIs or infer missing gear from incomplete observations.
 */
public final class TierAwareBankResolver {
    private TierAwareBankResolver() {}
    public enum Kind { EXACT, REVIEWED_VARIANT, LOWER_TIER_OWNED, WIKI_ALTERNATIVE_OWNED, LOW_STOCK_OWNED, NOT_OBSERVED, PARTIAL_STACK, EMPTY }
    public record Slot(Item displayed, Kind kind, Tier fromTier, String note) {
        public boolean confirmed() {return kind==Kind.EXACT || kind==Kind.REVIEWED_VARIANT || kind==Kind.LOWER_TIER_OWNED || kind==Kind.WIKI_ALTERNATIVE_OWNED || kind==Kind.LOW_STOCK_OWNED;}
        public boolean substitute() {return kind==Kind.LOWER_TIER_OWNED || kind==Kind.WIKI_ALTERNATIVE_OWNED || kind==Kind.LOW_STOCK_OWNED;}
    }
    public record Result(List<Slot> slots, int confirmed, int substitutions, boolean coreWeaponObserved,
                         boolean coreShieldObserved, boolean coreAmmoObserved, String summary) {
        public Result {slots=List.copyOf(slots);}
        public boolean notRecommended() {return !coreWeaponObserved || !coreShieldObserved || !coreAmmoObserved;}
    }
    // Allow same-slot armour/accessories only. Distinct weapon, ammunition and
    // offhand alternatives need hands/ammo/attack-style and capacity validation.
    private static final Set<Integer> SWAPPABLE = Set.of(0,1,2,5,7,8,9,10);
    private static final List<String> SET_OR_MECHANIC_KEYWORDS = List.of(
        "void", "crystal", "inquisitor", "blood moon", "blue moon", "eclipse", "dharok",
        "verac", "guthan", "ahrim", "karil", "torag", "masori", "obsidian", "ancestral",
        "slayer helmet", "slayer helm", "black mask", "graceful", "justiciar", "golem",
        "ranged void", "twinflame", "elite void", "moonlight", "sunfire fanatics");
    private static boolean sensitive(Item i) {
        if(i==null)return false;
        String text=i.name().toLowerCase(Locale.ROOT);
        return SET_OR_MECHANIC_KEYWORDS.stream().anyMatch(text::contains);
    }
    // If a loadout uses a named set, refuse any cross-tier armour swaps.
    // Mixing a Void or Crystal outfit while claiming the Wiki setup is dangerous.
    private static boolean hasCoordinatedSet(Profile profile) {
        for(Item item:profile.gear())if(sensitive(item))return true;
        return false;
    }
    /** The three F2P ranged methods explicitly cite the wiki's interchangeable
     * shortbow/armour items. No membership gear, crossbows, spells, or bosses
     * outside these guide-backed methods enter this pool. */
    private static boolean rangedF2p(String boss,String method) {
        return ("Brutus".equals(boss)&&"Ranged (free-to-play)".equals(method)) ||
            ("Obor".equals(boss)&&"Ranged · free-to-play".equals(method)) ||
            ("Bryophyta".equals(boss)&&"Ranged · free-to-play".equals(method));
    }
    private record Candidate(int id,String name,int level) {}
    private static final Map<Integer,List<Candidate>> F2P_RANGED_ALTERNATIVES=Map.of(
        0,List.of(new Candidate(1169,"Coif",20),new Candidate(1167,"Leather cowl",1)),
        2,List.of(new Candidate(1731,"Amulet of power",1),new Candidate(1478,"Amulet of accuracy",1)),
        5,List.of(new Candidate(1135,"Green d'hide body",40),new Candidate(1133,"Studded body",20),new Candidate(1131,"Hardleather body",10),new Candidate(1129,"Leather body",1)),
        7,List.of(new Candidate(1099,"Green d'hide chaps",40),new Candidate(1097,"Studded chaps",20),new Candidate(1095,"Leather chaps",1)),
        8,List.of(new Candidate(1065,"Green d'hide vambraces",40),new Candidate(1063,"Leather vambraces",1),new Candidate(1059,"Leather gloves",1))
    );
    private record Bow(int id,String name,int maxArrow) {}
    private record Arrow(int id,String name,int rank) {}
    private static final List<Bow> BOWS=List.of(
        new Bow(853,"Maple shortbow",4),new Bow(849,"Willow shortbow",3),
        new Bow(843,"Oak shortbow",2),new Bow(841,"Shortbow",1));
    private static final List<Arrow> ARROWS=List.of(
        new Arrow(890,"Adamant arrow",4),new Arrow(888,"Mithril arrow",3),
        new Arrow(886,"Steel arrow",2),new Arrow(884,"Iron arrow",1));
    private static Slot f2pOwnedArmour(int slot,Map<Integer,Integer> owned,Tier tier) {
        for(Candidate c:F2P_RANGED_ALTERNATIVES.getOrDefault(slot,List.of())) {
            if(owned.getOrDefault(c.id(),0)>0)
                return new Slot(new Item(c.id(),c.name(),1),Kind.WIKI_ALTERNATIVE_OWNED,tier,
                    "Owned F2P ranged guide alternative; verify level "+c.level()+" and quest prerequisites");
        }
        return null;
    }
    public static Result resolve(String boss, String method, Tier target,
                                 Map<Integer,Integer> observed) {
        return resolve(boss,method,target,observed,Map.of());
    }
    /** CP-1: names are RuneLite item names for observed IDs only, used for reviewed {@link NamedVariantRules}. */
    public static Result resolve(String boss, String method, Tier target,
                                 Map<Integer,Integer> observed, Map<Integer,String> names) {
        Objects.requireNonNull(boss);Objects.requireNonNull(method);
        Objects.requireNonNull(target);Objects.requireNonNull(observed);
        if(names==null)names=Map.of();
        var selection=TieredOutfitterCatalog.find(boss,method,target);
        if(selection==null)throw new IllegalArgumentException("No supported tier: "+boss+" "+method+" "+target);
        Profile profile=selection.profile();
        List<BestBankMatches.Match> original=BestBankMatches.gear(profile,observed,names);
        List<Slot> slots=new ArrayList<>(11);
        int substitutions=0,confirmed=0;
        for(int index=0;index<11;index++) {
            Item requirement=profile.gear().get(index);
            if(requirement==null || requirement.id()<=0 || requirement.amount()<1) {
                slots.add(new Slot(requirement,Kind.EMPTY,target,"No source-confirmed slot item"));continue;
            }
            BestBankMatches.Match exact=original.get(index);
            if(exact.confirmed()) {
                boolean variant=exact.status()==BestBankMatches.Status.REVIEWED_VARIANT;
                Item displayed=variant?new Item(exact.observedId(),requirement.name()+" (owned variant)",requirement.amount()):requirement;
                String variantNote="Reviewed equivalent owned";
                if(variant) {
                    final int seen=exact.observedId();
                    var named=NamedVariantRules.classify(requirement,observed,names).stream().filter(h->h.itemId()==seen).findFirst();
                    if(named.isPresent()) variantNote="Reviewed equivalent owned: "+named.get().ownedName()+" ("+named.get().note()+")";
                }
                slots.add(new Slot(displayed,variant?Kind.REVIEWED_VARIANT:Kind.EXACT,target,
                    variant?variantNote:"Exact wiki item owned"));confirmed++;continue;
            }
            if(exact.status()==BestBankMatches.Status.PARTIAL_STACK) {
                slots.add(new Slot(requirement,Kind.PARTIAL_STACK,target,"Observed quantity is insufficient"));continue;
            }
            // Only step DOWN from selected tier. This preserves the user's Entry /
            // Recommended / Optimal ceiling and avoids silently forcing expensive gear.
            // R05 (owner native QA: owned alternatives were never chosen): a coordinated set used to freeze EVERY
            // slot of the loadout. Only the set pieces themselves (sensitive requirement) stay frozen now; unrelated
            // slots (cape, neck, boots, ring, ...) may still use an owned reviewed lower-tier item. A sensitive
            // lower-tier item is still never brought in (checked below).
            boolean safeSlot=SWAPPABLE.contains(index) && !sensitive(requirement)
                && !ItemVariantPolicy.isGovernedVariant(requirement.id());
            Slot candidate=null;
            List<String> checked=new ArrayList<>();
            if(safeSlot) {
                for(int lower=target.ordinal()-1;lower>=0;lower--) {
                    Tier lowerTier=Tier.values()[lower];
                    var lowerSelection=TieredOutfitterCatalog.find(boss,method,lowerTier);
                    if(lowerSelection==null)continue;
                    if(!profile.style().equals(lowerSelection.profile().style()))continue;
                    Item lowerItem=lowerSelection.profile().gear().get(index);
                    if(lowerItem==null || lowerItem.id()<=0 || lowerItem.amount()<1)continue;
                    if(sensitive(lowerItem)){checked.add(lowerItem.name()+" ("+lowerTier.label()+", set piece: withheld)");continue;}
                    var match=BestBankMatches.gear(lowerSelection.profile(),observed,names).get(index);
                    if(!match.confirmed()){checked.add(lowerItem.name()+" ("+lowerTier.label()+", not owned)");continue;}
                    int actualId=match.observedId();
                    Item displayed=actualId==lowerItem.id()?lowerItem:new Item(actualId,lowerItem.name()+" (owned variant)",lowerItem.amount());
                    candidate=new Slot(displayed,Kind.LOWER_TIER_OWNED,lowerTier,
                        "Owned "+lowerTier.label()+" candidate. Check requirements and strategy before use.");
                    break;
                }
            }
            // The Brutus/Obor/Bryophyta ranged drafts have ONLY a Recommended
            // tier: the old lower-tier-only resolver could never substitute
            // anything. Use the Wiki's own ordered F2P alternatives instead.
            if(candidate==null && rangedF2p(boss,method))
                candidate=f2pOwnedArmour(index,observed,target);
            if(candidate!=null) {slots.add(candidate);substitutions++;confirmed++;}
            else {
                String why=!safeSlot?(sensitive(requirement)?"set piece: no cross-tier swap":
                        ItemVariantPolicy.isGovernedVariant(requirement.id())?"governed variant: only reviewed equivalents":
                        "weapon/ammo/offhand: swapped only as a complete lower-tier set")
                    :checked.isEmpty()?"no lower-tier alternative listed for this slot":"checked "+String.join(", ",checked);
                List<String> unusable=new ArrayList<>();
                for(var h:NamedVariantRules.classify(requirement,observed,names))
                    if(!h.usable()) unusable.add(h.ownedName()+" excluded: "+h.note());
                slots.add(new Slot(requirement,Kind.NOT_OBSERVED,target,
                    "Wiki target not positively observed; "+why+(unusable.isEmpty()?"":"; "+String.join("; ",unusable))));
            }
        }
        if(rangedF2p(boss,method)) {
            // Bow and arrow are an inseparable compatibility pair. Shortbows
            // accept arrows only up to their own tier. Do not show a random
            // banked bow alongside incompatible arrows, or imply low stock
            // meets the Wiki's 100-arrow trip budget.
            Bow selectedBow=null;Arrow selectedArrow=null;int arrowQty=0;
            int budget=Math.max(1,profile.gear().get(3).amount());
            // Compare COMPLETE compatible pairs, not a bow independently.
            // R14 would choose one adamant arrow + maple bow over 120
            // mithril arrows + willow bow, wrongly leaving a trip understocked.
            // First prefer a pair meeting the proposed trip budget; when every
            // pair is short, prefer more observed arrows. Within equal stock
            // adequacy prefer the stronger compatible bow and arrow tier.
            for(Bow bow:BOWS) {
                if(observed.getOrDefault(bow.id(),0)<1)continue;
                for(Arrow arrow:ARROWS) {
                    int amount=observed.getOrDefault(arrow.id(),0);
                    if(amount<=0 || arrow.rank()>bow.maxArrow())continue;
                    boolean sufficient=amount>=budget;
                    boolean bestSufficient=arrowQty>=budget;
                    boolean better=selectedBow==null ||
                        (sufficient && !bestSufficient) ||
                        (sufficient==bestSufficient &&
                            (sufficient ?
                                bow.maxArrow()>selectedBow.maxArrow() ||
                                bow.maxArrow()==selectedBow.maxArrow() && arrow.rank()>selectedArrow.rank() :
                                amount>arrowQty ||
                                amount==arrowQty && (bow.maxArrow()>selectedBow.maxArrow() ||
                                bow.maxArrow()==selectedBow.maxArrow() && arrow.rank()>selectedArrow.rank())));
                    if(better){selectedBow=bow;selectedArrow=arrow;arrowQty=amount;}
                }
            }
            if(selectedBow!=null) {
                Item targetBow=profile.gear().get(4);
                Item targetArrow=profile.gear().get(3);
                Kind bowKind=selectedBow.id()==targetBow.id()?Kind.EXACT:Kind.WIKI_ALTERNATIVE_OWNED;
                if(!slots.get(4).confirmed())confirmed++;
                if(bowKind==Kind.WIKI_ALTERNATIVE_OWNED)substitutions++;
                slots.set(4,new Slot(new Item(selectedBow.id(),selectedBow.name(),1),bowKind,target,
                    "Owned compatible F2P shortbow; check Ranged level"));
                // Show *observed* quantity, not the target inventory budget.
                int shown=Math.min(arrowQty,Math.max(1,targetArrow.amount()));
                boolean low=shown<targetArrow.amount();
                Kind arrowKind=low?Kind.LOW_STOCK_OWNED:
                    selectedArrow.id()==targetArrow.id()?Kind.EXACT:Kind.WIKI_ALTERNATIVE_OWNED;
                if(!slots.get(3).confirmed())confirmed++;
                if(selectedArrow.id()!=targetArrow.id())substitutions++;
                slots.set(3,new Slot(new Item(selectedArrow.id(),selectedArrow.name(),shown),arrowKind,target,
                    (low?"Low stock: "+shown+" observed vs "+targetArrow.amount()+" suggested. ":"")+
                    "Compatible with the selected "+selectedBow.name()));
            }
        }
        // R05: coupled weapon substitution. A weapon is never swapped alone (hands / ammunition / offhand). When the
        // target weapon is not owned, adopt the WHOLE weapon+ammo+shield trio of the highest lower tier of the same
        // boss, method and style in which all three are owned (or the tier has no item there). The trio is a
        // Wiki-reviewed combination, so ammo and 2H/shield stay compatible; the planner still checks 2H + shield.
        if(!slots.get(4).confirmed() && !rangedF2p(boss,method)) {
            for(int lower=target.ordinal()-1;lower>=0;lower--) {
                Tier lowerTier=Tier.values()[lower];
                var lowerSelection=TieredOutfitterCatalog.find(boss,method,lowerTier);
                if(lowerSelection==null || !profile.style().equals(lowerSelection.profile().style()))continue;
                Profile lp=lowerSelection.profile();
                Item lw=lp.gear().get(4);
                if(lw==null || lw.id()<=0 || sensitive(lw))continue;
                var lm=BestBankMatches.gear(lp,observed,names);
                boolean ok=true;
                for(int idx:new int[]{3,4,6}) {
                    Item it=lp.gear().get(idx);
                    if(it==null || it.id()<=0 || it.amount()<1) { if(idx==4) ok=false; continue; }
                    // CP-1: a missing NON-mechanic offhand (defender, book) or blessing no longer vetoes an owned
                    // lower-tier weapon; it is left empty and reported as an upgrade by the planner.
                    boolean optional=idx!=4 && !SlotCriticality.blocking(boss,idx,it) && !lm.get(idx).confirmed();
                    if(optional)continue;
                    if(!lm.get(idx).confirmed() || (idx!=4 && sensitive(it))) { ok=false; break; }
                }
                if(!ok)continue;
                for(int idx:new int[]{3,4,6}) {
                    Item it=lp.gear().get(idx);
                    if(it==null || it.id()<=0 || it.amount()<1) {
                        slots.set(idx,new Slot(null,Kind.EMPTY,lowerTier,"Not used with the "+lowerTier.label()+" weapon"));continue;
                    }
                    if(!lm.get(idx).confirmed()) {
                        slots.set(idx,new Slot(it,Kind.NOT_OBSERVED,lowerTier,
                            "Optional "+lowerTier.label()+" offhand/ammo slot item not owned; left empty"));continue;
                    }
                    int actual=lm.get(idx).observedId();
                    Item shown=actual==it.id()?it:new Item(actual,it.name()+" (owned variant)",it.amount());
                    slots.set(idx,new Slot(shown,Kind.LOWER_TIER_OWNED,lowerTier,
                        "Owned "+lowerTier.label()+" weapon set (weapon, ammo and offhand taken together). Check requirements."));
                }
                break;
            }
        }
        confirmed=0;substitutions=0;
        for(Slot sl:slots){ if(sl.confirmed())confirmed++; if(sl.substitute())substitutions++; }
        boolean weapon=slots.get(4).confirmed();
        // CP-1: only blocking (mechanic) offhands and projectile ammunition are core.
        boolean shield=profile.gear().get(6)==null || slots.get(6).confirmed() || slots.get(6).kind()==Kind.EMPTY
            || !SlotCriticality.blocking(boss,6,slots.get(6).displayed()==null?profile.gear().get(6):slots.get(6).displayed());
        boolean ammo=profile.gear().get(3)==null || slots.get(3).confirmed() || slots.get(3).kind()==Kind.EMPTY
            || !SlotCriticality.blocking(boss,3,slots.get(3).displayed()==null?profile.gear().get(3):slots.get(3).displayed());
        boolean lowStock=slots.get(3).kind()==Kind.LOW_STOCK_OWNED;
        String state=(!weapon || !shield || !ammo)?"NOT RECOMMENDED: core gear/ammunition unverified":
            lowStock?"LOW AMMUNITION: observed arrows below suggested trip budget":
            substitutions>0?"OWNED WIKI ALTERNATIVES: verify requirements":"Exact ownership matches only";
        return new Result(slots,confirmed,substitutions,weapon,shield,ammo,state);
    }
}
