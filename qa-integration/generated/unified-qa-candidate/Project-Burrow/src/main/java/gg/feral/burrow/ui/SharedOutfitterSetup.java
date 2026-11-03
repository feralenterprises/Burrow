package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.core.Outfitter;
import java.util.*;

/** R07: one immutable selection/projection contract for BOTH Outfitter categories.
 * The category filters available templates; neither Bank Tags nor item rendering reads
 * a separate, hidden boss selection when the player is browsing an Activity.
 * A reference is NOT evidence that the account owns an example item.
 */
public record SharedOutfitterSetup(Category category, String id, String title,
                                   Outfitter.Plan plan, List<Integer> wikiBankPositions,
                                   boolean bestBankPermitted, boolean bankTagPermitted) {
    public enum Category { BOSS, ACTIVITY }
    public SharedOutfitterSetup {
        Objects.requireNonNull(category); Objects.requireNonNull(id);Objects.requireNonNull(title);
        wikiBankPositions=wikiBankPositions==null?List.of():List.copyOf(wikiBankPositions);
        if(!wikiBankPositions.isEmpty() && wikiBankPositions.size()!=56)throw new IllegalArgumentException("bank positions must be 8x7");
        if(category==Category.ACTIVITY && bestBankPermitted) throw new IllegalArgumentException("Activity Best Bank is not yet connected");
    }
    public static SharedOutfitterSetup boss(BurrowModel.Selection selection) {
        if(selection==null)return null;
        String name=selection.boss();
        return new SharedOutfitterSetup(Category.BOSS,"boss:"+name,name,selection.plan(),List.of(),
             gg.feral.burrow.core.BossRegistry.bestBankAllowed(name),true);
    }
    private static final Map<String,SharedOutfitterSetup> ACTIVITIES=buildActivities();
    private static Map<String,SharedOutfitterSetup> buildActivities() {
        Map<String,SharedOutfitterSetup> out=new LinkedHashMap<>();long serial=100000;
        for(var kit:ActivityOutfitterCatalog.all()) {
            List<Outfitter.Requirement> gear=new ArrayList<>(Collections.nCopies(11,null));
            List<Outfitter.Requirement> bag=new ArrayList<>(Collections.nCopies(kit.inventory().size(),null));
            for(int i=0;i<11;i++) gear.set(i,requirement(kit.id(),true,i));
            for(int i=0;i<bag.size();i++)bag.set(i,requirement(kit.id(),false,i));
            var template=new Outfitter.Template(kit.title(),"Activity reference",gear,bag);
            // Deliberately unverified. Never attach a real account snapshot to catalog examples.
            var plan=Outfitter.resolve(++serial,new Outfitter.Scope("ACTIVITY-REFERENCE",0),template,"Recommended",null);
            var clue=ClueBankTagLayouts.find(kit.id());
            List<Integer> exact=List.of();
            if(clue!=null) {
                List<Integer> temp=new ArrayList<>(56);
                for(int value:clue.slots())temp.add(value>0?value:-1);
                exact=List.copyOf(temp);
            }
            // Wilderness is a risk-aware visual reference only: no bank optimization, O-button plan, or owned substitution.
            boolean bankTag=!ActivityOutfitterCatalog.isWilderness(kit.id()) &&
                (clue!=null || plan.cells().stream().anyMatch(c->c.requirement()!=null));
            out.put(kit.id(),new SharedOutfitterSetup(Category.ACTIVITY,"activity:"+kit.id(),kit.title(),
                plan,exact,false,bankTag));
        }
        return Map.copyOf(out);
    }
    private static Outfitter.Requirement requirement(String kit,boolean equipment,int slot) {
        var ref=ActivityItemRefs.find(kit,equipment,slot);
        if(ref==null)return null;
        // Only the documented seed quantity has a concrete multiple. All other icons
        // are ONE illustrative item per position, not a universal route count.
        int quantity=(kit.equals("birdhouse") && !equipment && slot==4)?40:1;
        return new Outfitter.Requirement(ref.itemId(),ref.itemName(),quantity);
    }
    public static SharedOutfitterSetup activity(String kitId) {
        SharedOutfitterSetup result=ACTIVITIES.get(kitId);
        if(result==null)throw new IllegalArgumentException("Unknown activity "+kitId);
        return result;
    }
    /** Exact 56-slot bank reference only for sourced 8x7 clue layouts. */
    public int[] sourcedBankLayout() {
        if(wikiBankPositions.isEmpty())return null;
        int[] ids=new int[56];for(int i=0;i<56;i++)ids[i]=wikiBankPositions.get(i);
        return ids;
    }
}
