package gg.feral.burrow.core;
import java.util.*;
/** Combat approaches explicitly identified in 2026-10-31 15-boss research.
 * Strategy-only entries NEVER fabricate Wiki gear, item IDs, bank ownership or tiers.
 * Every style can be selected and viewed, even when a safe equipment table is missing. */
public final class ResearchCombatMethods {
  private ResearchCombatMethods() {}
  public record Guide(String boss, String method, String style, String url,
                      String tactics, String safeguards) {}
  private static final Map<String,List<Guide>> BY_BOSS;
  private static final Map<String,Guide> BY_KEY;
  private static String key(String boss,String method){return boss+"\u0000"+method;}
  static {
    Map<String,List<Guide>> b=new LinkedHashMap<>(); Map<String,Guide> k=new LinkedHashMap<>();
    add(b,k,new Guide("Scurrius","Ranged · rat-bone option","Ranged","https://oldschool.runescape.wiki/w/Scurrius/Strategies","A ranged method is documented; bone shortbow can handle summoned rats after a spine is obtained.","The 50-Ranged bone shortbow requires a previously earned spine. W07.5 gear is a provisional example only; no assumed bank ownership or item unlock."));
    add(b,k,new Guide("Scurrius","Magic · rat-bone option","Magic","https://oldschool.runescape.wiki/w/Scurrius/Strategies","A magic method is documented; bone staff is an unlock-dependent rat-bone weapon.","The 50-Magic bone staff requires a previously earned spine and stored chaos charges. W07.5 supplies are provisional and cannot imply the staff is charged."));
    add(b,k,new Guide("Giant Mole","Ranged · Bowfa / crossbow","Ranged","https://oldschool.runescape.wiki/w/Giant_Mole/Strategies","Ranged choices named in research include Bowfa with crystal armour, crystal bow, twisted bow and crossbows.","Bring a spade and light unless permanent cave fire is unlocked; Falador shield and staminas are optional."));
    add(b,k,new Guide("Giant Mole","Earth magic","Magic","https://oldschool.runescape.wiki/w/Giant_Mole/Strategies","Earth spells exploit the mole elemental weakness; spell and rune choice must match level and charges.","W07.5 shows a provisional Earth Bolt example, not a certified gear table. Bring spade/light unless fire built and check spellbook plus runes."));
    add(b,k,new Guide("Giant Mole","Melee · Dharok risk","Melee","https://oldschool.runescape.wiki/w/Giant_Mole/Strategies","Dharok method requires the full four-piece set and intentional health management.","High-risk low-health approach. The source does not certify a safe HP threshold or complete inventory."));
    add(b,k,new Guide("Sarachnis","Magic · Tumeken shadow","Magic","https://oldschool.runescape.wiki/w/Sarachnis/Strategies","Research identifies a separate high-level Tumeken shadow magic strategy alongside crush melee.","Shadow charges, pathing, prayer/food and web handling need a complete loadout before gear is recommended."));
    add(b,k,new Guide("King Black Dragon","Melee · low-risk alternative","Melee","https://oldschool.runescape.wiki/w/King_Black_Dragon/Strategies","Research recognizes melee as an alternative to the existing crossbow plan.","Wilderness travel exposes items to PvP; an antifire arrangement and appropriate anti-dragon protection are essential."));
    add(b,k,new Guide("Vorkath","Melee · lance / fang","Melee","https://oldschool.runescape.wiki/w/Vorkath/Strategies","Dragon hunter lance and Osmumten fang are documented melee approaches.","Dragon Slayer II, venom/dragonfire protection and a solution to the zombified spawn still apply."));
    add(b,k,new Guide("Phantom Muspah","Hybrid · ranged + magic","Hybrid","https://oldschool.runescape.wiki/w/Phantom_Muspah/Strategies","Magic is used on melee form, ranged on other forms; phase switching and equipment space are essential.","Account for sapphire enchanted bolts/crossbow at shield phase and a consistent spellbook/rune budget."));
    add(b,k,new Guide("Phantom Muspah","Melee · alternative","Melee","https://oldschool.runescape.wiki/w/Phantom_Muspah/Strategies","The research names a melee alternative to ranged-only and range/mage hybrid.","No complete melee-tier inventory or phase-by-phase recommendation was verified."));
    add(b,k,new Guide("Dagannoth Kings","Tribrid · Rex Prime Supreme","Hybrid","https://oldschool.runescape.wiki/w/Dagannoth_Kings/Strategies","Magic for Rex, ranged for Prime, melee for Supreme, with switches and individual king positioning.","Waterbirth route, poison protection, prayer restoration, switch capacity and isolation require a verified tribrid plan."));
    add(b,k,new Guide("Zulrah","Hybrid · magic + ranged","Hybrid","https://oldschool.runescape.wiki/w/Zulrah/Strategies","Magic/ranged hybrid is the general wiki strategy; use phase-specific combat styles.","Venom protection, recoils, compatible ammo/runes, switching room and phase rotation are mandatory."));
    add(b,k,new Guide("Zulrah","Ranged-only","Ranged","https://oldschool.runescape.wiki/w/Zulrah/Strategies","Research identifies ranged-only as an available approach with rotation-related constraints.","Blue/green/red forms vary in defence; venom protection, recoils and an actual range inventory need verification."));
    add(b,k,new Guide("Obor","Ranged · free-to-play","Ranged","https://oldschool.runescape.wiki/w/Obor","F2P ranged is documented. A coif, maple shortbow and suitable arrows are candidate equipment from related research.","One giant key unlocks first access permanently. Later fights are keyless; each optional reward chest consumes a key. Knockback and boulder positioning still matter."));
    add(b,k,new Guide("Obor","Magic · earth / snare","Magic","https://oldschool.runescape.wiki/w/Obor","The research identifies a magic/snare approach; exact spell runes and kiting position matter.","A giant key is required only for first access and optional reward chest claims, not every fight. The proposed spell/rune budget is not a source-complete table."));
    add(b,k,new Guide("Bryophyta","Ranged · free-to-play","Ranged","https://oldschool.runescape.wiki/w/Bryophyta","F2P ranged gear can use green dragonhide, coif and maple shortbow.","Mossy key is needed for permanent first access or optional reward chest, not every fight. Use a woodcutting axe for growthlings."));
    add(b,k,new Guide("Bryophyta","Fire magic","Magic","https://oldschool.runescape.wiki/w/Bryophyta","Fire spells exploit the boss weakness; account for mage gear, rune budget and movement.","Use a woodcutting axe for growthlings and poison awareness on members worlds. Mossy key is first-access or optional chest-only."));
    Map<String,List<Guide>> frozen=new LinkedHashMap<>(); b.forEach((name,gs)->frozen.put(name,List.copyOf(gs)));
    BY_BOSS=Collections.unmodifiableMap(frozen);BY_KEY=Collections.unmodifiableMap(k);
  }
  private static void add(Map<String,List<Guide>> b,Map<String,Guide> k,Guide g) {
    if(k.putIfAbsent(key(g.boss(),g.method()),g)!=null)throw new IllegalStateException("duplicate method");
    b.computeIfAbsent(g.boss(),x->new ArrayList<>()).add(g);
  }
  public static Guide find(String boss,String method){return BY_KEY.get(key(boss,method));}
  public static List<String> methods(String boss){return BY_BOSS.getOrDefault(boss,List.of()).stream().map(Guide::method).toList();}
  public static int size(){return BY_KEY.size();}
  public static Set<String> bosses(){return BY_BOSS.keySet();}
}