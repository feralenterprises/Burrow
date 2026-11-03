package gg.feral.burrow.ui;
import java.util.*;
/** Source-indexed item examples for activity kit diagrams. Numbers are OSRS item IDs,
 * never bank ownership, quantities, rankings, or gear automatically chosen for an account.
 * An unmapped slot is intentionally text-only; do not fabricate a sprite or quantity.
 * References: OSRS Wiki Slayer_training, Farming_runs, Bird_house_trapping; 2026-11-02.
 */
public final class ActivityItemRefs {
    public record Ref(int itemId,String itemName,String qualification) {
        public Ref { if(itemId<=0 || itemName==null || itemName.isBlank())throw new IllegalArgumentException("invalid reference"); }
    }
    private static final Map<String,Map<String,Ref>> REFS=build();
    private static void add(Map<String,Map<String,Ref>> all,String kit,boolean gear,int slot,int id,String name,String reason){
        all.computeIfAbsent(kit,k->new HashMap<>()).put((gear?"E":"I")+slot,new Ref(id,name,reason));
    }
    private static Map<String,Map<String,Ref>> build(){
        Map<String,Map<String,Ref>> a=new HashMap<>();
        // Farming: matching exact named Wiki tools and documented exemplar options, never a fabricated seed selection.
        add(a,"herb",true,1,13122,"Ardougne cloak 2","Illustrative diary-tier option; upgrade only if unlocked");
        add(a,"herb",true,4,7409,"Magic secateurs","Fairytale I reward; effect also works when carried");
        add(a,"herb",false,1,952,"Spade","Source item");
        add(a,"herb",false,2,5341,"Rake","Optional when auto-weed enabled");
        add(a,"herb",false,3,5343,"Seed dibber","Optional with barehanded planting unlock");
        add(a,"herb",false,4,22994,"Bottomless compost bucket","One optional way to carry compost");
        add(a,"trees",false,1,952,"Spade","Source item");
        add(a,"trees",false,2,5341,"Rake","Optional if weeds remain");
        add(a,"fruit-trees",false,1,952,"Spade","Source item");
        add(a,"fruit-trees",false,3,5341,"Rake","Optional if weeds remain");
        // Oak birdhouses are explicit tier EXAMPLES only, not inferred from the player's Hunter/Crafting level.
        for(int n=0;n<4;n++)add(a,"birdhouse",false,n,21515,"Oak bird house",
            "Four prepared-house example at 14 Hunter; use the player's eligible birdhouse tier instead");
        add(a,"birdhouse",true,2,11194,"Digsite pendant (5)","Fossil Island option only after unlocked");
        add(a,"birdhouse",false,4,5305,"Barley seeds ×40","EXAMPLE of low-tier seeds for four traps; alternatives valid");
        add(a,"birdhouse",false,5,11194,"Digsite pendant (5)","Only if not equipped");
        // Melee (generic) uses representative item images; account's actual weapon is never inferred.
        add(a,"slayer-melee",true,0,11865,"Slayer helmet (i)","Only if owned and on assigned task");
        add(a,"slayer-melee",true,4,4151,"Abyssal whip","Wiki mid-tier EXAMPLE, not owned weapon");
        add(a,"slayer-magic",true,0,11865,"Slayer helmet (i)","Only if owned and on task");
        add(a,"slayer-magic",true,4,11907,"Trident of the seas","EXAMPLE for generic Magic, not Barrage autocast");
        add(a,"slayer-ranged",true,0,11865,"Slayer helmet (i)","Only if owned and on task");
        add(a,"slayer-ranged",true,1,22109,"Ava's assembler","Account-unlocked option, not guaranteed");
        add(a,"slayer-ranged",true,4,27610,"Venator bow","Wiki EXAMPLE, not owned or universally best");
        add(a,"slayer-melee-hallowfell",true,0,11865,"Slayer helmet (i)","On task only");
        add(a,"slayer-melee-hallowfell",true,1,6570,"Fire cape","Optional account unlock");
        add(a,"slayer-melee-hallowfell",true,2,6585,"Amulet of fury","Wiki alternate exemplar");
        add(a,"slayer-melee-hallowfell",true,4,34027,"Hallowfell","Source-specific two-handed weapon, 75 Attack");
        add(a,"slayer-melee-hallowfell",true,5,11832,"Bandos chestplate","Wiki alternate exemplar");
        add(a,"slayer-melee-hallowfell",true,7,11834,"Bandos tassets","Wiki alternate exemplar");
        add(a,"slayer-melee-hallowfell",true,9,11840,"Dragon boots","Wiki lower-tier option");
        add(a,"slayer-magic-barrage",true,0,11865,"Slayer helmet (i)","On task only");
        add(a,"slayer-magic-barrage",true,2,12002,"Occult necklace","Wiki source example");
        add(a,"slayer-magic-barrage",true,4,21006,"Kodai wand","Ancient-compatible example, not owned");
        add(a,"slayer-magic-barrage",true,5,26243,"Virtus robe top","Wiki source example");
        add(a,"slayer-magic-barrage",true,7,26245,"Virtus robe bottom","Wiki source example");
        add(a,"slayer-magic-barrage",true,9,22954,"Devout boots","Wiki source alternative");
        add(a,"slayer-magic-barrage",false,1,12791,"Rune pouch","Optional inventory saver");
        // Risk-aware Wiki references, not automatically chosen valuable gear.
        add(a,"slayer-ranged-wilderness",true,1,10499,"Ava's accumulator","Explicit replaceability/risk check");
        add(a,"slayer-ranged-wilderness",true,2,1704,"Amulet of glory","Risk-reviewed option");
        add(a,"slayer-ranged-wilderness",true,4,27655,"Webweaver bow","Wiki ranking ONLY, ether loss on death");
        add(a,"slayer-ranged-wilderness",true,5,2503,"Black d'hide body","Wiki reference");
        add(a,"slayer-ranged-wilderness",true,7,2497,"Black d'hide chaps","Wiki reference");
        add(a,"slayer-ranged-wilderness",true,9,6328,"Snakeskin boots","Wiki replaceable option");
        // R08: five exact /Slayer_training ranked tables share the same source-derived
        // examples for sidebar sprites AND O-button Wiki bank tag. No owned-item assertion.
        for(String kit:SlayerWikiGear.ids()){
            var table=SlayerWikiGear.find(kit);
            for(int i=0;i<11;i++){
                var slot=table.slot(i);
                if(slot!=null && slot.exampleItemId()>0){
                    add(a,kit,true,i,slot.exampleItemId(),slot.exampleName(),
                        "Wiki example only; owned gear unverified; full source ranks listed below");
                } else if(slot!=null) {
                    // Wilderness helmet is source-ranked but deliberately NOT risk-packed by default.
                    a.computeIfAbsent(kit,k->new HashMap<>()).remove("E"+i);
                }
            }
        }
        return Collections.unmodifiableMap(a);
    }
    public static Ref find(String kit,boolean equipment,int index){
        return REFS.getOrDefault(kit,Map.of()).get((equipment?"E":"I")+index);
    }
    public static Map<String,Ref> forKit(String kit){return Map.copyOf(REFS.getOrDefault(kit,Map.of()));}
    private ActivityItemRefs(){}
}
