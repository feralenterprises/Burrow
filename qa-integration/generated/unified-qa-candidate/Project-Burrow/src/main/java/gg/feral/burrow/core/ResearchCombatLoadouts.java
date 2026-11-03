package gg.feral.burrow.core;
import java.util.*;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.*;
/** Ten bounded research DRAFT recommendations with explicit gear examples.
 * Not a complete 15-boss audit, account recommendation, or bank ownership claim.
 * Input research and spell Wiki links are retained in each profile. */
public final class ResearchCombatLoadouts {
  private ResearchCombatLoadouts() {}
  private static final Map<String,Selection> DATA;
  private static String key(String b,String m){return b+"\u0000"+m;}
  private static Item item(int id,String n,int qty){return new Item(id,n,qty);}
  private static Item[] inv(){return new Item[28];}
  private static void put(Map<String,Selection> map,String boss,String method,String style,String url,
                          Item[] gear,Item[] inventory,String... notes){
    if(gear.length!=11||inventory.length!=28)throw new IllegalStateException("slot geometry");
    if(ResearchCombatMethods.find(boss,method)==null)throw new IllegalStateException("unresearched style");
    var list=new ArrayList<String>();
    list.add("RESEARCH DRAFT, NOT CERTIFIED: equipment and quantities are an explicitly proposed example, NOT a complete verified Wiki layout or a claim of ownership.");
    list.addAll(Arrays.asList(notes));
    var profile=new Profile(boss,method,style,url,Arrays.asList(gear),Arrays.asList(inventory),list);
    if(map.putIfAbsent(key(boss,method),new Selection(profile,Authority.TPT_CURATED_WIKI_STRATEGY,url))!=null)
      throw new IllegalStateException("duplicate gear draft");
  }
  static {
    Map<String,Selection> m=new LinkedHashMap<>();
    String obor="https://oldschool.runescape.wiki/w/Obor";
    String bryo="https://oldschool.runescape.wiki/w/Bryophyta";
    String mole="https://oldschool.runescape.wiki/w/Giant_Mole/Strategies";
    var brutusRanged=ResearchOutfitterAdditions.find("Brutus","Ranged (free-to-play)",Tier.RECOMMENDED).profile();
    var brutusMagic=ResearchOutfitterAdditions.find("Brutus","Magic (free-to-play)",Tier.RECOMMENDED).profile();
    // F2P shortbow + compatible arrows, documented for Bryophyta and as a ranged option for Obor.
    Item[] oborR=brutusRanged.gear().toArray(new Item[0]);Item[] oborRI=inv();
    // A giant key is not a recurring combat requirement after permanent first-access unlock.
    oborRI[1]=item(373,"Swordfish",1);
    for(int i=2;i<13;i++)oborRI[i]=item(373,"Swordfish",1);
    put(m,"Obor","Ranged · free-to-play","Ranged",obor,oborR,oborRI,
        "Maple shortbow + adamant arrows are a proposed budget F2P example, not an Obor wiki loadout table.",
        "FIRST ACCESS: one giant key permanently unlocks Obor. Later fights do not consume a key; each reward chest does. Account unlock is UNKNOWN here; 12 swordfish are a TPT estimate.");
    Item[] bryoR=brutusRanged.gear().toArray(new Item[0]);Item[] bryoRI=inv();
    // First-access and chest keys are conditional, not universal boss-fight inventory.
    bryoRI[1]=item(1359,"Rune axe",1);
    for(int i=2;i<14;i++)bryoRI[i]=item(373,"Swordfish",1);
    put(m,"Bryophyta","Ranged · free-to-play","Ranged",bryo,bryoR,bryoRI,
        "Coif, green dragonhide and maple shortbow are documented F2P ranged options; 12 swordfish is a TPT suggestion.",
        "FIRST ACCESS: one mossy key permanently unlocks the lair. Later fights need no key; the reward chest consumes one. Rune axe is optional; bronze works.",
        "Green d'hide body requires 40 Ranged, 40 Defence and Dragon Slayer I; use a lower-body alternative if unavailable.");
    // F2P Fire Bolt: staff supplies fire, 3 air + 1 chaos per cast. 300 air/100 chaos for 100 casts.
    Item[] fire=brutusMagic.gear().toArray(new Item[0]);fire[4]=item(1387,"Staff of fire",1);
    Item[] fireI=inv();fireI[1]=item(1359,"Rune axe",1);
    fireI[2]=item(556,"Air rune",300);fireI[3]=item(562,"Chaos rune",100);
    for(int i=4;i<16;i++)fireI[i]=item(373,"Swordfish",1);
    put(m,"Bryophyta","Fire magic","Magic",bryo,fire,fireI,
        "Fire Bolt requires 35 Magic. Fire staff supplies fire runes; 300 air and 100 chaos runes permit 100 casts.",
        "Rune budget from OSRS Wiki Fire Bolt: https://oldschool.runescape.wiki/w/Fire_Bolt . 12 swordfish suggested, not Wiki quantity.",
        "FIRST ACCESS: one mossy key unlocks the lair permanently. Later fights do not consume keys; the optional reward chest does. Axe required for growthlings. Members poison awareness applies.");
    // Obor: verified Snare cost from OSRS Wiki. Earth staff provides earth runes; water and nature remain.
    Item[] oborM=brutusMagic.gear().toArray(new Item[0]);Item[] oborMI=inv();
    oborMI[1]=item(556,"Air rune",100);
    oborMI[2]=item(558,"Mind rune",100);oborMI[3]=item(555,"Water rune",200);
    oborMI[4]=item(561,"Nature rune",150);
    for(int i=5;i<17;i++)oborMI[i]=item(373,"Swordfish",1);
    put(m,"Obor","Magic · earth / snare","Magic",obor,oborM,oborMI,
        "Earth Strike with earth staff uses air/mind runes; 100 air allow up to 50 Earth Strikes. Snare (level 50) uses 4 earth, 4 water, 3 nature per cast.",
        "With earth staff, 200 water and 150 nature runes support 50 snares; https://oldschool.runescape.wiki/w/Snare",
        "FIRST ACCESS: one giant key unlocks the lair permanently. Later fights need no key; each optional reward chest consumes one. Obor knockback still applies; 12 swordfish suggested.");
    // Ranged Bowfa/crystal armour explicitly listed for Mole, but DK Prime supplies are NOT transplanted.
    var prime=TieredOutfitterCatalog.find("Dagannoth Kings","Prime, ranged",Tier.RECOMMENDED).profile();
    Item[] moleR=prime.gear().toArray(new Item[0]);Item[] moleRI=inv();
    moleRI[0]=item(952,"Spade",1);moleRI[1]=item(4550,"Bullseye lantern (lit)",1);
    moleRI[2]=item(2434,"Prayer potion(4)",1);moleRI[3]=item(2434,"Prayer potion(4)",1);
    moleRI[4]=item(12625,"Stamina potion(4)",1);moleRI[5]=item(8009,"Falador teleport (tablet)",1);
    for(int i=6;i<16;i++)moleRI[i]=item(385,"Shark",1);
    put(m,"Giant Mole","Ranged · Bowfa / crossbow","Ranged",mole,moleR,moleRI,
        "This draft depicts only the Bowfa + crystal armour option, not the alternative crossbow version. Bowfa is two-handed and requires no ammo.",
        "80 Ranged, 70 Agility, Bowfa charges/corruption and crystal gear ownership must be checked. No bank verification.",
        "Spade/light (unless permanent fire), prayer, energy and Falador travel. 10 sharks and potion quantities are TPT starting suggestions.");
    // Two independently wiki-supported melee approaches, intentionally separated from the existing ranged profiles.
    var supreme=TieredOutfitterCatalog.find("Dagannoth Kings","Supreme, melee",Tier.RECOMMENDED).profile();
    Item[] vorkathM=supreme.gear().toArray(new Item[0]);
    vorkathM[2]=item(12018,"Salve amulet(ei)",1);
    vorkathM[4]=item(26219,"Osmumten's fang",1);
    Item[] vorkathI=inv();
    vorkathI[0]=item(22215,"Extended super antifire(2)",1);
    vorkathI[1]=item(12913,"Anti-venom+(4)",1);
    vorkathI[2]=item(12695,"Super combat potion(4)",1);
    vorkathI[3]=item(2434,"Prayer potion(4)",1);vorkathI[4]=item(2434,"Prayer potion(4)",1);
    vorkathI[5]=item(4696,"Dust rune",100);vorkathI[6]=item(562,"Chaos rune",100);
    vorkathI[7]=item(563,"Law rune",20);vorkathI[8]=item(8013,"Teleport to house (tablet)",1);
    for(int i=9;i<21;i++)vorkathI[i]=item(385,"Shark",1);
    put(m,"Vorkath","Melee · lance / fang","Melee","https://oldschool.runescape.wiki/w/Vorkath/Strategies",vorkathM,vorkathI,
        "Fang is the selected alternative, not a claimed Dragon hunter lance; Salve(ei) works for melee too. This is a research draft from source-listed alternatives.",
        "Use super antifire + Protect from Magic with defender. Dragon Slayer II required. Dodge fireballs/acid and use Crumble Undead for spawn.",
        "Bring a real emergency teleport. 12 sharks, charges, runes and potions are a proposed starting budget; validate spell casting and consumable requirements before relying on this.");
    Item[] kbdM=supreme.gear().toArray(new Item[0]);kbdM[4]=item(26219,"Osmumten's fang",1);
    kbdM[6]=item(1540,"Anti-dragon shield",1);
    Item[] kbdI=inv();
    kbdI[0]=item(21166,"Burning amulet(5)",1);kbdI[1]=item(2452,"Antifire potion(4)",1);
    kbdI[2]=item(2446,"Antipoison(4)",1);kbdI[3]=item(2434,"Prayer potion(4)",1);
    kbdI[4]=item(12695,"Super combat potion(4)",1);kbdI[5]=item(8013,"Teleport to house (tablet)",1);
    for(int i=6;i<18;i++)kbdI[i]=item(385,"Shark",1);
    put(m,"King Black Dragon","Melee · low-risk alternative","Melee","https://oldschool.runescape.wiki/w/King_Black_Dragon/Strategies",kbdM,kbdI,
        "Defensive proposed melee example: Fang (one handed) with anti-dragon shield. Not certified as optimal or low risk from PvP.",
        "WILDERNESS ROUTE: nearby players can attack you and you can lose your gear. Review protected items and do not assume bank ownership.",
        "Dragonfire protection and anti-poison are essential. Carry travel, food and prayer supplies; quantities are provisional.");
    // W07.5: bounded gear illustrations for three previously strategy-only styles.
    // These examples are deliberately RESEARCH_GEAR, excluded from Best in Bank and Bank O.
    String scurrius="https://oldschool.runescape.wiki/w/Scurrius/Strategies";
    // Bone shortbow (28794) requires 50 Ranged and a previously earned Scurrius spine.
    // Rune arrows (892) are compatible; 150 is a suggested stock, not a verified trip requirement.
    Item[] scurR=brutusRanged.gear().toArray(new Item[0]);
    scurR[3]=item(892,"Rune arrow",150);
    scurR[4]=item(28794,"Bone shortbow",1);
    scurR[6]=null; // Two-handed: never carry a shield as worn gear.
    Item[] scurRI=inv();
    scurRI[0]=item(2434,"Prayer potion(4)",1);
    scurRI[1]=item(2434,"Prayer potion(4)",1);
    scurRI[2]=item(2444,"Ranging potion(4)",1);
    scurRI[3]=item(8013,"Teleport to house (tablet)",1);
    for(int i=4;i<16;i++)scurRI[i]=item(385,"Shark",1);
    put(m,"Scurrius","Ranged · rat-bone option","Ranged",scurrius,scurR,scurRI,
        "Bone shortbow ID 28794 requires 50 Ranged and Scurrius' spine; not available to first-time spine hunters. Source: https://oldschool.runescape.wiki/w/Bone_shortbow",
        "Rune arrows ID 892 are compatible; 150 arrows and consumables are proposed planning amounts, not a proven trip budget. Two-handed weapon; no offhand.",
        "Green d'hide body also requires Dragon Slayer I, 40 Ranged and 40 Defence. Replace the body if unavailable; do not treat that optional upgrade as an encounter access gate.");
    // The powered Bone staff must ALREADY BE CHARGED with chaos runes, not treated as an uncharged ordinary staff.
    Item[] scurM=brutusMagic.gear().toArray(new Item[0]);
    scurM[4]=item(28796,"Bone staff",1);
    Item[] scurMI=inv();
    scurMI[0]=item(2434,"Prayer potion(4)",1);
    scurMI[1]=item(2434,"Prayer potion(4)",1);
    scurMI[2]=item(562,"Chaos rune",100); // Optional recharging stock, not direct spell cost while charged.
    scurMI[3]=item(8013,"Teleport to house (tablet)",1);
    for(int i=4;i<16;i++)scurMI[i]=item(385,"Shark",1);
    put(m,"Scurrius","Magic · rat-bone option","Magic",scurrius,scurM,scurMI,
        "Bone staff ID 28796 requires 50 Magic, a Scurrius spine and PRECHARGED chaos runes. Each powered attack consumes one stored charge. Source: https://oldschool.runescape.wiki/w/Bone_staff",
        "The staff can ONLY attack rats and does not cast ordinary Earth/Fire spells. 100 loose chaos runes are an optional recharge stock, not 100 ready casts when the staff is empty.",
        "First-spine acquisition requires another weapon. Food and prayer amounts are research suggestions; current charge state must be observed, never assumed.");
    // Earth Bolt requires 29 Magic, 2 air + 3 earth + 1 chaos per cast.
    // Staff of earth supplies earth runes. Giant Mole has 50% earth-elemental weakness.
    Item[] moleM=brutusMagic.gear().toArray(new Item[0]);
    moleM[4]=item(1385,"Staff of earth",1);
    Item[] moleMI=inv();
    moleMI[0]=item(952,"Spade",1);
    moleMI[1]=item(4550,"Bullseye lantern (lit)",1);
    moleMI[2]=item(556,"Air rune",200);
    moleMI[3]=item(562,"Chaos rune",100);
    moleMI[4]=item(2434,"Prayer potion(4)",1);
    moleMI[5]=item(2434,"Prayer potion(4)",1);
    moleMI[6]=item(12625,"Stamina potion(4)",1);
    moleMI[7]=item(8009,"Falador teleport (tablet)",1);
    for(int i=8;i<18;i++)moleMI[i]=item(385,"Shark",1);
    put(m,"Giant Mole","Earth magic","Magic",mole,moleM,moleMI,
        "Giant Mole has a 50% earth elemental weakness. This draft demonstrates Earth Bolt at 29 Magic with an earth staff; lower Earth Strike (9 Magic) or higher-tier spells are alternatives. Source: https://oldschool.runescape.wiki/w/Giant_Mole/Strategies",
        "Earth Bolt costs 2 air, 3 earth and 1 chaos rune per cast. Staff of earth supplies earth; 200 air + 100 chaos provide 100 casts. Source: https://oldschool.runescape.wiki/w/Earth_Bolt",
        "Spade required; light only if Fire of Eternal Light is not built. Supplies and food are suggestions, not a source-exact complete inventory. Avoid treating an unknown spellbook or rune count as READY.");
    DATA=Collections.unmodifiableMap(m);
  }
  public static Selection find(String boss,String method,Tier tier){return tier==Tier.RECOMMENDED?DATA.get(key(boss,method)):null;}
  public static int size(){return DATA.size();}
}
