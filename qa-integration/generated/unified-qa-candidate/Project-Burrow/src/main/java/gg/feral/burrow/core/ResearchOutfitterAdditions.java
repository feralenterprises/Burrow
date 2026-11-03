package gg.feral.burrow.core;

import java.util.*;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.*;

/** Separate, conditional research overlay. The original 51 FINAL-06 profiles remain byte-for-byte unchanged.
 * Only the two explicitly researched Brutus Recommended strategies are registered here.
 * Unknown Entry and Optimal tiers intentionally return null. Not a wiki certification or bank scan. */
public final class ResearchOutfitterAdditions {
    private ResearchOutfitterAdditions() {}
    public static final String BRUTUS_URL = "https://oldschool.runescape.wiki/w/Brutus/Strategies";
    private static final String RANGED="Ranged (free-to-play)";
    private static final String MAGIC="Magic (free-to-play)";
    private static final Map<String, Selection> DATA;
    private static String key(String boss,String method,Tier tier) {return boss+"\u0000"+method+"\u0000"+tier.name();}
    private static Item item(int id,String name,int amount) {return new Item(id,name,amount);}
    private static Item[] inventory() {return new Item[28];}
    private static void put(Map<String,Selection> m,String method, Profile p) {
        if(!p.boss().equals("Brutus") || !p.method().equals(method)) throw new IllegalArgumentException("mismatched research profile");
        if(m.putIfAbsent(key("Brutus",method,Tier.RECOMMENDED),new Selection(p,Authority.TPT_CURATED_WIKI_STRATEGY,BRUTUS_URL))!=null)
            throw new IllegalStateException("duplicate research method");
    }
    static {
        Map<String,Selection> m=new LinkedHashMap<>();
        Item[] rangedGear={item(1169,"Coif",1),item(1007,"Red cape",1),item(1731,"Amulet of power",1),item(890,"Adamant arrow",100),item(853,"Maple shortbow",1),item(1135,"Green d'hide body",1),null,item(1099,"Green d'hide chaps",1),item(1065,"Green d'hide vambraces",1),item(1061,"Leather boots",1),null};
        Item[] rangedInv=inventory();
        rangedInv[0]=item(33104,"Cowbell amulet (charged)",1);
        for(int i=1;i<=12;i++)rangedInv[i]=item(373,"Swordfish",1);
        put(m,RANGED,new Profile("Brutus",RANGED,"Ranged",BRUTUS_URL,
            Arrays.asList(rangedGear),Arrays.asList(rangedInv),List.of(
            "RESEARCH / RECOMMENDED ONLY: F2P shortbow strategy; practice moving around Brutus, do not assume a safe AFK spot.",
            "40 Ranged for green d'hide and maple bow; green d'hide body also requires 40 Defence and Dragon Slayer I. Use a lower armour alternative when not eligible.",
            "The Ides of Milk and a charged cowbell amulet are required to access the encounter. Carry compatible adamant arrows; 100 arrows and 12 swordfish are suggested starting amounts, not wiki-prescribed quantities.")));
        Item[] magicGear={item(579,"Blue wizard hat",1),item(1007,"Red cape",1),item(1727,"Amulet of magic",1),null,item(1385,"Staff of earth",1),item(577,"Blue wizard robe",1),null,null,item(1059,"Leather gloves",1),item(1061,"Leather boots",1),null};
        Item[] magicInv=inventory();
        magicInv[0]=item(33104,"Cowbell amulet (charged)",1);
        magicInv[1]=item(556,"Air rune",100);
        magicInv[2]=item(558,"Mind rune",100);
        for(int i=3;i<=14;i++)magicInv[i]=item(373,"Swordfish",1);
        put(m,MAGIC,new Profile("Brutus",MAGIC,"Magic",BRUTUS_URL,
            Arrays.asList(magicGear),Arrays.asList(magicInv),List.of(
            "RESEARCH / RECOMMENDED ONLY: Earth Strike at 9 Magic; earth staff supplies earth runes, leaving 2 air and 1 mind rune per cast.",
            "100 air runes and 100 mind runes permit up to 50 Earth Strikes, assuming no other rune use. Higher-level spells need their own rune budget.",
            "The Ides of Milk and charged cowbell amulet are required. Rune and 12 swordfish amounts are starting suggestions; gear is proposed, not account-owned or native-tested.")));
        DATA=Collections.unmodifiableMap(m);
    }
    public static Selection find(String boss,String method,Tier tier) {return DATA.get(key(boss,method,tier));}
    public static List<String> methods(String boss) {return "Brutus".equals(boss)?List.of(RANGED,MAGIC):List.of();}
    public static int size(){return DATA.size();}
}
