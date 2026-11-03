package gg.feral.burrow.core;
import java.util.*;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.*;
/** Separate provisional two-style combat presets for two additional encounters.
 * Carefully labelled research DRAFTS, not certified wiki tier layouts or account ownership.
 * All source data and original 51 profiles remain unchanged; no Best in Bank projection.
 */
public final class DemonEncounterLoadouts {
  private DemonEncounterLoadouts() {}
  public static final String TD="Tormented Demons", DG="Demonic Gorillas";
  public static final String TD_GUIDE="https://oldschool.runescape.wiki/w/Tormented_Demon/Strategies";
  public static final String DG_GUIDE="https://oldschool.runescape.wiki/w/Demonic_gorilla/Strategies";
  public static final String MELEE="Melee + Ranged (Emberlight / Scorching bow)";
  public static final String RANGED="Ranged + Melee (Scorching bow / Emberlight)";
  public static final String MAGIC="Magic + Melee (Purging staff / Emberlight)";
  public static final String TRIBRID="Melee + Ranged + Magic (three-way switch)";
  public static final String GORILLA="Melee + Ranged (Emberlight / Scorching bow)";
  public static final String GORILLA_RANGE="Ranged + Melee (Scorching bow / Emberlight)";
  private static final Map<String,Selection> DATA;
  private static final Map<String,List<String>> METHODS;
  private static String key(String b,String m,Tier t){return b+"\u0000"+m+"\u0000"+t;}
  private static Item i(int id,String name,int quantity){return new Item(id,name,quantity);}
  private static Item[] inv(){return new Item[28];}
  private static Item[] gear(){return new Item[11];}
  private static Item[] melee(){
    Item[] g=gear();
    g[0]=i(10828,"Helm of Neitiznot",1);g[1]=i(6570,"Fire cape",1);
    g[2]=i(6585,"Amulet of fury",1);g[3]=i(21326,"Amethyst arrow",500);
    g[4]=i(29589,"Emberlight",1);g[5]=i(10551,"Fighter torso",1);
    g[6]=i(12954,"Dragon defender",1);g[7]=i(21304,"Obsidian platelegs",1);
    g[8]=i(7462,"Barrows gloves",1);g[9]=i(11840,"Dragon boots",1);
    g[10]=i(11773,"Berserker ring (i)",1);
    return g;
  }
  private static Item[] ranged(){
    Item[] g=melee();g[1]=i(10499,"Ava's accumulator",1);g[4]=i(29591,"Scorching bow",1);
    g[5]=i(2503,"Black d'hide body",1);g[6]=null;
    return g;
  }
  private static Item[] magic(){
    Item[] g=melee();g[1]=i(2412,"Saradomin cape",1);g[3]=null;
    g[4]=i(29594,"Purging staff",1);g[5]=i(4091,"Mystic robe top",1);
    g[6]=null;g[7]=i(4093,"Mystic robe bottom",1);
    return g;
  }
  private static void add(Item[] a,Item item){for(int j=0;j<a.length;j++)if(a[j]==null){a[j]=item;return;}throw new IllegalStateException("inventory full");}
  private static void potions(Item[] a,boolean magic,boolean gorilla){
    add(a,i(12695,"Super combat potion(4)",1));
    if(!magic)add(a,i(2444,"Ranging potion(4)",1));
    if(magic)add(a,i(556,"Air rune",250));
    if(magic)add(a,i(554,"Fire rune",350));
    if(magic)add(a,i(565,"Blood rune",50));
    for(int j=0;j<(gorilla?5:6);j++)add(a,i(3024,"Super restore(4)",1));
    add(a,i(gorilla?19564:29684,gorilla?"Royal seed pod":"Guthixian temple teleport",1));
    add(a,i(8013,"Teleport to house (tablet)",1));
    for(int j=0;j<(gorilla?10:7);j++)add(a,i(385,"Shark",1));
  }
  private static void put(Map<String,Selection> data,Map<String,List<String>> methods,String boss,
      String method,String style,Item[] gear,Item[] inv,String... notes){
    if(gear.length!=11||inv.length!=28)throw new IllegalStateException("invalid slot geometry");
    var combined=new ArrayList<String>();
    combined.add("RESEARCH DRAFT, not certified gear or bank ownership. This is a proposed player preparation layout, not complete current Wiki tier tables.");
    combined.addAll(Arrays.asList(notes));
    var profile=new Profile(boss,method,style,TD.equals(boss)?TD_GUIDE:DG_GUIDE,
                            Arrays.asList(gear),Arrays.asList(inv),combined);
    if(data.putIfAbsent(key(boss,method,Tier.RECOMMENDED),new Selection(profile,Authority.TPT_CURATED_WIKI_STRATEGY,profile.source()))!=null)
       throw new IllegalStateException("duplicate demon encounter method");
    methods.computeIfAbsent(boss,k->new ArrayList<>()).add(method);
  }
  static {
    Map<String,Selection> data=new LinkedHashMap<>();
    Map<String,List<String>> methods=new LinkedHashMap<>();
    String switchGuide="PREFERRED SLOTS (INFORMATION ONLY): inventory 1 Scorching bow; 2 Emberlight; 3 defender when unequipped. " +
      "RuneLite cannot reserve slots, lock inventory positions or move equipment. A two-handed bow cannot be worn with a defender; leave at least two free switch spaces.";
    Item[] m=inv();add(m,i(29591,"Scorching bow",1));potions(m,false,false);
    put(data,methods,TD,MELEE,"Melee",melee(),m,
      "While Guthix Sleeps is required. Melee primary: Emberlight with dragon defender; Scorching bow plus amethyst arrows as ranged switch.",
      switchGuide,
      "Keep player-position fire bombs in mind; after prayer switches attack with a different combat style. Starting supplies are estimates, not wiki-set counts.");
    Item[] r=inv();add(r,i(29589,"Emberlight",1));add(r,i(12954,"Dragon defender",1));potions(r,false,false);
    put(data,methods,TD,RANGED,"Ranged",ranged(),r,
      "While Guthix Sleeps is required. Scorching bow is two-handed; equip Emberlight + defender for melee prayer swaps.",switchGuide,
      "Amethyst arrows are an optional owned consumable, not guaranteed. Use usable alternatives if arrows or bow are unavailable.");
    Item[] mg=inv();add(mg,i(29589,"Emberlight",1));add(mg,i(12954,"Dragon defender",1));potions(mg,true,false);
    put(data,methods,TD,MAGIC,"Magic",magic(),mg,
      "While Guthix Sleeps required. Purging staff (77 Magic) with Fire Wave as a proposed ordinary-spell baseline; melee switch is Emberlight + defender.",
      "Fire Wave proposed budget: 5 air + 7 fire + 1 blood per cast, 50 casts. Check spellbook, combat requirements and actual owned runes.",
      "The spell is an example, not a proven optimum; when switching to Emberlight and defender, the staff must fit back in inventory. Preserve open switch slots.");
    Item[] tri=inv();add(tri,i(29591,"Scorching bow",1));add(tri,i(29594,"Purging staff",1));potions(tri,true,false);
    put(data,methods,TD,TRIBRID,"Hybrid",melee(),tri,
      "Three-style experimental setup: Emberlight + defender equipped, Scorching bow and Purging staff in preferred inventory slots.",
      "Use different styles when demon changes protection prayer. One-handed magic staff and two-handed bow change offhand requirements.",
      "Rune counts are Fire Wave examples; avoid pretending unverified demonbane spellbook or rune pouch compatibility. " + switchGuide);
    Item[] gi=inv();add(gi,i(29591,"Scorching bow",1));potions(gi,false,true);
    put(data,methods,DG,GORILLA,"Melee",melee(),gi,
      "Monkey Madness II required. Melee primary Emberlight + defender; Scorching bow as ranged switch. Avoid boulder impacts.",
      "Gorilla protection prayers require two attack styles. Switch own protection after three successfully blocked/missed attacks.",
      switchGuide);
    Item[] gir=inv();add(gir,i(29589,"Emberlight",1));add(gir,i(12954,"Dragon defender",1));potions(gir,false,true);
    put(data,methods,DG,GORILLA_RANGE,"Ranged",ranged(),gir,
      "Monkey Madness II required. Scorching bow + amethyst arrows equipped; Emberlight + defender as melee switch.",
      "When wearing the two-handed bow, the defender MUST remain in inventory. Change prayer when the gorilla swaps its own combat style.",switchGuide);
    DATA=Collections.unmodifiableMap(data);
    Map<String,List<String>> done=new LinkedHashMap<>();methods.forEach((b,ns)->done.put(b,List.copyOf(ns)));
    METHODS=Collections.unmodifiableMap(done);
  }
  public static List<String> bosses(){return List.of(TD,DG);}
  public static List<String> methods(String boss){return METHODS.getOrDefault(boss,List.of());}
  public static Selection find(String boss,String method,Tier tier){return DATA.get(key(boss,method,tier));}
  public static int size(){return DATA.size();}
}
