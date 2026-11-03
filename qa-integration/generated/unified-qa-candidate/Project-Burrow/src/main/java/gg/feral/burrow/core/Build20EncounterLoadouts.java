package gg.feral.burrow.core;

import gg.feral.burrow.core.TieredOutfitterCatalog.Authority;
import gg.feral.burrow.core.TieredOutfitterCatalog.Selection;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Item;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Profile;
import java.util.*;

/**
 * Build 20 DRAFT profiles authored from the OSRS Wiki "Lizardman shaman/Strategies" equipment and
 * inventory tables (read 2026-11-01). Every item ID was checked against RuneLite 1.13.4
 * {@code net.runelite.api.ItemID}. These are Wiki-listed items assembled into one Entry tier per
 * style; they are NOT an official Wiki tier layout and are labelled DRAFT in the UI.
 * Original FINAL-06 and Build 19 profiles are untouched.
 */
public final class Build20EncounterLoadouts {
    private Build20EncounterLoadouts() {}
    public static final String LIZARD = "Lizardman Shamans";
    public static final String LIZARD_RANGED = "Ranged · Shayzien (5) + crossbow";
    public static final String LIZARD_MELEE = "Melee · Shayzien (5) + hasta";
    public static final String LIZARD_URL = "https://oldschool.runescape.wiki/w/Lizardman_shaman/Strategies";
    private static final Map<String,Selection> DATA = new LinkedHashMap<>();
    private static final Map<String,List<String>> METHODS = new LinkedHashMap<>();
    private static String key(String b,String m,Tier t){return b+"\u0000"+m+"\u0000"+t;}
    private static Item i(int id,String n,int q){return new Item(id,n,q);}
    private static Item[] shayzien(){
        Item[] g=new Item[11];
        g[0]=i(13379,"Shayzien helm (5)",1);
        g[5]=i(13381,"Shayzien body (5)",1);
        g[7]=i(13380,"Shayzien greaves (5)",1);
        g[8]=i(13377,"Shayzien gloves (5)",1);
        g[9]=i(13378,"Shayzien boots (5)",1);
        return g;
    }
    private static void fill(Item[] inv,Item item,int count){
        for(int n=0;n<count;n++){int j=Arrays.asList(inv).indexOf(null); if(j<0)throw new IllegalStateException("inventory full"); inv[j]=item;}
    }
    static {
        // Ranged: lowest (most accessible) Wiki column for each slot.
        Item[] rg=shayzien();
        rg[1]=i(10499,"Ava's accumulator",1);
        rg[2]=i(6585,"Amulet of fury",1);
        rg[3]=i(11875,"Broad bolts",500);
        rg[4]=i(9185,"Rune crossbow",1);
        rg[6]=i(22284,"Black d'hide shield",1);
        rg[10]=i(13128,"Explorer's ring 4",1);
        Item[] ri=new Item[28];
        fill(ri,i(23733,"Divine ranging potion(4)",1),2);
        fill(ri,i(5952,"Antidote++(4)",1),1);
        fill(ri,i(2434,"Prayer potion(4)",1),7);
        fill(ri,i(13393,"Xeric's talisman",1),1);
        fill(ri,i(8013,"Teleport to house (tablet)",1),1);
        fill(ri,i(6714,"Holy wrench",1),1);
        fill(ri,i(13226,"Herb sack",1),1);
        fill(ri,i(7946,"Monkfish",1),14);
        put(LIZARD_RANGED,"Ranged",rg,ri,
            "DRAFT from the Wiki ranged table (no-cannon variant). Full Shayzien (5) negates acid-spit damage; poison can still apply: Antidote++ carried.",
            "Slayer helmet (i) replaces the Shayzien helm ONLY after the hard Kourend & Kebos diary and speaking to Captain Cleive.",
            "Canyon (3 shamans, multi) needs no task; Lizardman Caves need a lizardman Slayer task. Dodge the jump and step away from spawns.",
            "Dragon warhammer 1/3,000 and Xeric's talisman are the notable drops; no drop is implied by selecting this boss.");
        // Melee: Wiki melee table items available to a mid-level account.
        Item[] mg=shayzien();
        mg[1]=i(6570,"Fire cape",1);
        mg[2]=i(1731,"Amulet of power",1);
        mg[3]=i(22947,"Rada's blessing 4",1);
        mg[4]=i(11889,"Zamorakian hasta",1);
        mg[6]=i(12954,"Dragon defender",1);
        mg[10]=i(11773,"Berserker ring (i)",1);
        Item[] mi=new Item[28];
        fill(mi,i(8013,"Teleport to house (tablet)",1),1);
        fill(mi,i(6714,"Holy wrench",1),1);
        fill(mi,i(13226,"Herb sack",1),1);
        fill(mi,i(5952,"Antidote++(4)",1),2);
        fill(mi,i(7946,"Monkfish",1),2);
        fill(mi,i(2434,"Prayer potion(4)",1),21);
        put(LIZARD_MELEE,"Melee",mg,mi,
            "DRAFT from the Wiki melee table. Pray Protect from Melee; the Wiki inventory line for melee boosts is ambiguous, so no combat potion is assumed.",
            "Full Shayzien (5) negates acid-spit damage; poison can still apply. Slayer helmet only with the hard Kourend & Kebos diary + Captain Cleive.",
            "Wiki suggests 80+ Attack/Strength for melee. Canyon needs no task; Lizardman Caves need a lizardman Slayer task.");
    }
    private static void put(String method,String style,Item[] gear,Item[] inv,String... notes){
        var notesList=new ArrayList<String>();
        notesList.add("BUILD 20 DRAFT · Wiki-listed items, not an official Wiki tier layout or ownership claim.");
        notesList.addAll(List.of(notes));
        Profile p=new Profile(LIZARD,method,style,LIZARD_URL,Arrays.asList(gear),Arrays.asList(inv),notesList);
        if(DATA.putIfAbsent(key(LIZARD,method,Tier.ENTRY),new Selection(p,Authority.TPT_CURATED_WIKI_STRATEGY,LIZARD_URL))!=null)
            throw new IllegalStateException("duplicate");
        METHODS.computeIfAbsent(LIZARD,k->new ArrayList<>()).add(method);
    }
    // B20.1 R03: Wiki strategy-table drafts for the remaining bosses ride on this same hook, so the
    // protected TieredOutfitterCatalog stays byte-identical to Build 20.
    public static Selection find(String boss,String method,Tier tier){
        var s=DATA.get(key(boss,method,tier));
        return s!=null?s:WikiStrategyLoadouts.find(boss,method,tier);
    }
    public static List<String> methods(String boss){
        var own=METHODS.getOrDefault(boss,List.of());
        var wiki=WikiStrategyLoadouts.methods(boss);
        if(wiki.isEmpty()) return own;
        var all=new ArrayList<>(own); all.addAll(wiki); return List.copyOf(all);
    }
    public static Set<String> bosses(){
        var all=new LinkedHashSet<>(METHODS.keySet()); all.addAll(WikiStrategyLoadouts.bosses()); return all;
    }
    public static int size(){return DATA.size()+WikiStrategyLoadouts.size();}
}
