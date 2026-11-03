package gg.feral.burrow.ui;
import java.util.*;

/** Source-transcribed ranked equipment columns from OSRS Wiki Slayer_training, 2026-11-02.
 * Ordered columns are most to least effective, NOT a tier/ownership verdict. Slash
 * variants stay IN the source column, rather than turning into extra rankings.
 * A single depicted icon is an explicitly identified EXAMPLE for visualization.
 * Never interpret this table as account inventory or automatically risk gear.
 */
public final class SlayerWikiGear {
 public record SlotRank(String slot, int exampleItemId, String exampleName, List<String> rankedColumns){
  public SlotRank { rankedColumns=List.copyOf(rankedColumns); if(rankedColumns.isEmpty())throw new IllegalArgumentException("no rank columns"); }
  public String displayRanks(){var parts=new ArrayList<String>();for(int i=0;i<rankedColumns.size();i++)parts.add((i+1)+". "+rankedColumns.get(i));return String.join(" | ",parts);}
 }
 public record Table(String section, List<SlotRank> gear, List<String> specialWeaponColumns, String sourceUrl){
  public Table {gear=Collections.unmodifiableList(new ArrayList<>(gear));specialWeaponColumns=List.copyOf(specialWeaponColumns);if(gear.size()!=11)throw new IllegalArgumentException("11 slots required");}
  public SlotRank slot(int index){return gear.get(index);}
 }
 private static final Map<String,Table> TABLES=build();
 private static Map<String,Table> build(){Map<String,Table> m=new LinkedHashMap<>();
  m.put("slayer-melee",new Table("Melee",Arrays.asList(
   new SlotRank("HEAD",11865,"Slayer helmet (i)",List.of("Slayer helmet (i)","Black mask (i)")),
   new SlotRank("CAPE",6570,"Fire cape",List.of("Infernal cape","Fire cape","Mythical cape / Mixed hide cape","Ardougne cloak 4","Cape of Accomplishment")),
   new SlotRank("NECK",6585,"Amulet of fury",List.of("Amulet of rancour","Amulet of torture","Amulet of fury","Amulet of strength")),
   new SlotRank("AMMO",22947,"Rada's blessing 4",List.of("Rada's blessing 4","God blessing / Rada's blessing 3/2")),
   new SlotRank("WEAPON",4151,"Abyssal whip",List.of("Soulreaper axe + Scythe of Vitur","Ghrazi rapier / Noxious halberd","Abyssal whip","Abyssal bludgeon","Zamorakian hasta")),
   new SlotRank("BODY",11832,"Bandos chestplate",List.of("Torva platebody / Oathplate chest","Bandos chestplate","Blood Moon chestplate / Fighter torso","Obsidian platebody","Proselyte hauberk")),
   new SlotRank("SHIELD",12954,"Dragon defender",List.of("Avernic defender","Dragon defender","Dragonfire shield","Toktz-ket-xil","Book of War")),
   new SlotRank("LEGS",11834,"Bandos tassets",List.of("Torva platelegs / Oathplate legs","Bandos tassets","Blood Moon tassets","Obsidian platelegs","Proselyte cuisse / Proselyte tasset / Verac's plateskirt")),
   new SlotRank("GLOVES",7462,"Barrows gloves",List.of("Expeditious bracelet / Bracelet of slaughter","Ferocious gloves","Barrows gloves","Regen bracelet","Combat bracelet")),
   new SlotRank("BOOTS",11840,"Dragon boots",List.of("Avernic treads (max)","Primordial boots","Dragon boots / Aranea boots","Echo boots > Guardian boots","Rune boots / Mixed hide boots > Climbing boots")),
   new SlotRank("RING",11773,"Berserker ring (i)",List.of("Ultor ring","Berserker ring (i)","Brimstone ring"))
  ),List.of("Dragon claws / Burning claws + Dinh's bulwark","Dragon dagger + Crystal halberd","Saradomin godsword"),"https://oldschool.runescape.wiki/w/Slayer_training#Melee"));
  m.put("slayer-melee-hallowfell",new Table("Melee (Hallowfell)",Arrays.asList(
   new SlotRank("HEAD",11865,"Slayer helmet (i)",List.of("Slayer helmet (i)","Black mask (i)")),
   new SlotRank("CAPE",6570,"Fire cape",List.of("Infernal cape","Fire cape","Mythical cape / Mixed hide cape","Ardougne cloak 4","Cape of Accomplishment")),
   new SlotRank("NECK",6585,"Amulet of fury",List.of("Amulet of rancour","Amulet of torture","Amulet of fury","Amulet of strength")),
   new SlotRank("AMMO",22947,"Rada's blessing 4",List.of("Rada's blessing 4","God blessing / Rada's blessing 3/2")),
   new SlotRank("WEAPON",34027,"Hallowfell",List.of("Hallowfell")),
   new SlotRank("BODY",11832,"Bandos chestplate",List.of("Torva platebody / Oathplate chest","Bandos chestplate","Blood Moon chestplate / Fighter torso","Obsidian platebody","Proselyte hauberk")),
   null,
   new SlotRank("LEGS",11834,"Bandos tassets",List.of("Torva platelegs / Oathplate legs","Bandos tassets","Blood Moon tassets","Obsidian platelegs","Proselyte cuisse / Proselyte tasset / Verac's plateskirt")),
   new SlotRank("GLOVES",7462,"Barrows gloves",List.of("Expeditious bracelet / Bracelet of slaughter","Ferocious gloves","Barrows gloves","Regen bracelet","Combat bracelet")),
   new SlotRank("BOOTS",11840,"Dragon boots",List.of("Avernic treads (max)","Primordial boots","Dragon boots / Aranea boots","Echo boots > Guardian boots","Rune boots / Mixed hide boots > Climbing boots")),
   new SlotRank("RING",25975,"Lightbearer",List.of("Lightbearer","Ultor ring","Berserker ring (i)","Brimstone ring"))
  ),List.of("Crystal halberd","Dinh's bulwark","Saradomin godsword","Ancient mace"),"https://oldschool.runescape.wiki/w/Slayer_training#Melee_(Hallowfell)"));
  m.put("slayer-magic-barrage",new Table("Magic (Barrage)",Arrays.asList(
   new SlotRank("HEAD",11865,"Slayer helmet (i)",List.of("Slayer helmet (i)","Black mask (i)")),
   new SlotRank("CAPE",21791,"Imbued saradomin cape",List.of("Imbued god cape","God capes","Ardougne cloak 4","Cape of Accomplishment","Vestment cloak")),
   new SlotRank("NECK",12002,"Occult necklace",List.of("Occult necklace","Bonecrusher necklace")),
   new SlotRank("AMMO",22947,"Rada's blessing 4",List.of("Rada's blessing 4","God blessing / Rada's blessing 3/2")),
   new SlotRank("WEAPON",21006,"Kodai wand",List.of("Kodai wand / Nightmare staff","Ancient sceptre / Dragon hunter wand / Master wand","Blue Moon spear [two-handed]","Ancient staff")),
   new SlotRank("BODY",26243,"Virtus robe top",List.of("Virtus robe top","Ancestral robe top","Ahrim's robetop / Dagon'hai robe top","Proselyte hauberk [low-Magic-defence target] / Vestment robe top","Monk's robe top")),
   new SlotRank("SHIELD",27251,"Elidinis' ward (f)",List.of("Elidinis' ward (f)","Elidinis' ward / Arcane spirit shield","Ancient wyvern shield / Malediction ward / Mage's book","Book of Darkness","Unholy book")),
   new SlotRank("LEGS",26245,"Virtus robe bottom",List.of("Virtus robe bottom","Ancestral robe bottom","Ahrim's robeskirt / Dagon'hai robe bottom","Proselyte cuisse [low-Magic-defence target] / Vestment robe legs","Monk's robe")),
   new SlotRank("GLOVES",19544,"Tormented bracelet",List.of("Bracelet of slaughter / Expeditious bracelet","Confliction gauntlets","Tormented bracelet","Barrows gloves")),
   new SlotRank("BOOTS",22954,"Devout boots",List.of("Avernic treads (max)","Eternal boots / Devout boots","Holy sandals","Aranea boots / Ancient ceremonial boots","Mystic boots")),
   new SlotRank("RING",25486,"Magus ring",List.of("Magus ring","Ring of the gods (i)","Seers ring (i) / Brimstone ring","Ring of wealth"))
  ),List.of("Dinh's bulwark + Crystal halberd"),"https://oldschool.runescape.wiki/w/Slayer_training#Magic_(Barrage)"));
  m.put("slayer-ranged",new Table("Ranged",Arrays.asList(
   new SlotRank("HEAD",11865,"Slayer helmet (i)",List.of("Slayer helmet (i)","Black mask (i)")),
   new SlotRank("CAPE",22109,"Ava's assembler",List.of("Dizana's quiver","Ava's assembler","Ava's accumulator","Ranging cape","Ava's attractor")),
   new SlotRank("NECK",19547,"Necklace of anguish",List.of("Necklace of rupture","Necklace of anguish","Bonecrusher necklace / Dragonbone necklace","Amulet of fury","Amulet of glory")),
   new SlotRank("AMMO",21326,"Amethyst arrow",List.of("Amethyst arrow [bow only] / Moonlight antler bolts [sunlight crossbow only]","Rada's blessing 4","God blessing / Rada's blessing 3/2")),
   new SlotRank("WEAPON",27610,"Venator bow",List.of("Venator bow","Toxic blowpipe","Magic shortbow (i) / Hunters' sunlight crossbow")),
   new SlotRank("BODY",27238,"Masori body (f)",List.of("Masori body (f) / Masori body","Hueycoatl hide body","Blessed body","Black d'hide body")),
   null,
   new SlotRank("LEGS",27241,"Masori chaps (f)",List.of("Masori chaps (f) / Masori chaps","Hueycoatl hide chaps","Blessed chaps","Black d'hide chaps")),
   new SlotRank("GLOVES",21177,"Expeditious bracelet",List.of("Expeditious bracelet / Bracelet of slaughter","Zaryte vambraces","Barrows gloves","Blessed vambraces","Hueycoatl hide vambraces")),
   new SlotRank("BOOTS",13237,"Pegasian boots",List.of("Avernic treads (max)","Pegasian boots","Blessed boots","Aranea boots","Boots of Brimstone")),
   new SlotRank("RING",25487,"Venator ring",List.of("Venator ring","Ring of the gods (i)","Ring of suffering (i)","Archers ring (i)","Ring of wealth"))
  ),List.of("Toxic blowpipe"),"https://oldschool.runescape.wiki/w/Slayer_training#Ranged"));
  m.put("slayer-ranged-wilderness",new Table("Ranged (Wilderness Slayer)",Arrays.asList(
   new SlotRank("HEAD",-1,null,List.of("Slayer helmet (i)","Black mask (i)")),
   new SlotRank("CAPE",10499,"Ava's accumulator",List.of("Ava's assembler","Ava's accumulator","Ranging cape")),
   new SlotRank("NECK",1704,"Amulet of glory",List.of("Necklace of rupture","Necklace of anguish","Amulet of glory")),
   new SlotRank("AMMO",22947,"Rada's blessing 4",List.of("Rada's blessing 4","God blessing / Rada's blessing 3/2")),
   new SlotRank("WEAPON",12926,"Toxic blowpipe",List.of("Webweaver bow","Craw's bow","Toxic blowpipe")),
   new SlotRank("BODY",2503,"Black d'hide body",List.of("Black d'hide body")),
   null,
   new SlotRank("LEGS",2497,"Black d'hide chaps",List.of("Black d'hide chaps")),
   new SlotRank("GLOVES",21177,"Expeditious bracelet",List.of("Expeditious bracelet")),
   new SlotRank("BOOTS",6328,"Snakeskin boots",List.of("Mixed hide boots","Snakeskin boots")),
   new SlotRank("RING",2552,"Ring of dueling(8)",List.of("Ring of wealth","Explorer's ring 4","Ring of dueling"))
  ),List.of(),"https://oldschool.runescape.wiki/w/Slayer_training#Ranged_(Wilderness_Slayer)"));
  return Collections.unmodifiableMap(m);}
 public static Table find(String id){return TABLES.get(id);}
 public static Set<String> ids(){return TABLES.keySet();}
 private SlayerWikiGear(){}
}
