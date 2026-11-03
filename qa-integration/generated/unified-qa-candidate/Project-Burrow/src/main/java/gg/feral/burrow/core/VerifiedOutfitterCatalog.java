package gg.feral.burrow.core;

import java.util.*;
import gg.feral.burrow.core.Outfitter.*;
/** Generated from BUR-OUTFITTER-S01-R04-LOADOUTS.json. Source URLs and unknown variants are retained. */
public final class VerifiedOutfitterCatalog {
    private VerifiedOutfitterCatalog() {}
    public record Item(int id, String name, int amount) {
        public Requirement requirement() { return id > 0 && amount > 0 ? new Requirement(id,name,amount) : null; }
    }
    public record Profile(String boss, String method, String style, String source,
                          List<Item> gear, List<Item> inventory, List<String> notes) {
        public Profile {
            gear=Collections.unmodifiableList(new ArrayList<>(gear));
            inventory=Collections.unmodifiableList(new ArrayList<>(inventory));
            notes=List.copyOf(notes);
            if(gear.size()!=11 || inventory.size()!=28) throw new IllegalArgumentException("geometry");
        }
        public Template template() {
            List<Requirement> g=new ArrayList<>(), i=new ArrayList<>();
            for(Item item:gear) g.add(item==null?null:item.requirement());
            for(Item item:inventory) i.add(item==null?null:item.requirement());
            return new Template(boss,style,g,i);
        }
    }
    public static final List<String> BOSSES=List.of("Scurrius","Brutus","Giant Mole","Vorkath","Phantom Muspah","Sarachnis","Hueycoatl","Amoxliatl","King Black Dragon","Grotesque Guardians","Zulrah","Barrows","Dagannoth Kings","Obor","Bryophyta");
    private static final Map<String,List<Profile>> DATA;
    static {
        Map<String,List<Profile>> m=new LinkedHashMap<>();
        add(m,new Profile("Scurrius","Standard melee","Melee","https://oldschool.runescape.wiki/w/Scurrius/Strategies?action=raw&oldid=15385528",
            Arrays.asList(
                new Item(11665,"Void melee helm",1), new Item(6570,"Fire cape",1), new Item(6585,"Amulet of fury",1), new Item(22943,"Rada's blessing 2",1),
                new Item(28792,"Bone mace",1), new Item(8839,"Void knight top",1), new Item(12954,"Dragon defender",1), new Item(8840,"Void knight robe",1),
                new Item(8842,"Void knight gloves",1), new Item(11840,"Dragon boots",1), new Item(11773,"Berserker ring (i)",1)),
            Arrays.asList(
                new Item(864,"Bronze knife",1), new Item(2440,"Super strength(4)",1), new Item(2436,"Super attack(4)",1), new Item(2442,"Super defence(4)",1),
                new Item(2434,"Prayer potion(4)",1), new Item(2434,"Prayer potion(4)",1), new Item(2434,"Prayer potion(4)",1), new Item(2434,"Prayer potion(4)",1),
                new Item(5698,"Dragon dagger(p++)",1), new Item(3144,"Cooked karambwan",1), new Item(3144,"Cooked karambwan",1), new Item(3144,"Cooked karambwan",1),
                new Item(3144,"Cooked karambwan",1), new Item(3144,"Cooked karambwan",1), new Item(3144,"Cooked karambwan",1), new Item(3144,"Cooked karambwan",1),
                new Item(3144,"Cooked karambwan",1), new Item(3144,"Cooked karambwan",1), new Item(3144,"Cooked karambwan",1), new Item(3144,"Cooked karambwan",1),
                new Item(3144,"Cooked karambwan",1), new Item(3144,"Cooked karambwan",1), new Item(3144,"Cooked karambwan",1), new Item(3144,"Cooked karambwan",1),
                new Item(554,"Fire rune",-1), new Item(556,"Air rune",-1), new Item(561,"Nature rune",-1), new Item(563,"Law rune",-1)),
            List.of("Bone mace requires a Scurrius spine; the Wiki template does not specify rune stack amounts.")));
        add(m,new Profile("Brutus","Standard free-to-play","Melee","https://oldschool.runescape.wiki/w/Brutus/Strategies?action=raw&oldid=15354867",
            Arrays.asList(
                new Item(1163,"Rune full helm",1), new Item(1007,"Red cape",1), new Item(1725,"Amulet of strength",1), null,
                new Item(1333,"Rune scimitar",1), new Item(1113,"Rune chainbody",1), new Item(33101,"Mooleta",1), new Item(1093,"Rune plateskirt",1),
                new Item(23261,"Gilded d'hide vambraces",1), new Item(25171,"Decorative boots (gold)",1), null),
            Arrays.asList(
                new Item(113,"Strength potion(4)",1), new Item(113,"Strength potion(4)",1), new Item(333,"Trout",1), new Item(333,"Trout",1),
                new Item(333,"Trout",1), new Item(333,"Trout",1), new Item(333,"Trout",1), new Item(333,"Trout",1),
                new Item(333,"Trout",1), new Item(333,"Trout",1), new Item(333,"Trout",1), new Item(333,"Trout",1),
                new Item(333,"Trout",1), new Item(333,"Trout",1), new Item(333,"Trout",1), new Item(333,"Trout",1),
                new Item(333,"Trout",1), new Item(333,"Trout",1), new Item(333,"Trout",1), new Item(333,"Trout",1),
                null, null, null, null,
                null, null, new Item(590,"Tinderbox",1), new Item(-1,"Cowbell amulet",1)),
            List.of("Cowbell amulet has charged and uncharged variants; 33103 is the base item, 33104 charged. The template does not specify charge state. Mooleta is a post-quest Brutus drop; Rune kiteshield is the source-listed alternative.")));
        add(m,new Profile("Giant Mole","Melee and prayer budget","Melee","https://oldschool.runescape.wiki/w/Giant_Mole/Strategies?action=raw&oldid=15361945",
            Arrays.asList(
                new Item(10828,"Helm of Neitiznot",1), new Item(6570,"Fire cape",1), new Item(6585,"Amulet of fury",1), new Item(22947,"Rada's blessing 4",1),
                new Item(13265,"Abyssal dagger",1), new Item(10551,"Fighter torso",1), new Item(12954,"Dragon defender",1), new Item(21304,"Obsidian platelegs",1),
                new Item(7462,"Barrows gloves",1), new Item(11840,"Dragon boots",1), new Item(11773,"Berserker ring (i)",1)),
            Arrays.asList(
                new Item(23685,"Divine super combat potion(4)",1), new Item(23685,"Divine super combat potion(4)",1), new Item(2434,"Prayer potion(4)",1), new Item(2434,"Prayer potion(4)",1),
                new Item(2434,"Prayer potion(4)",1), new Item(2434,"Prayer potion(4)",1), new Item(2434,"Prayer potion(4)",1), new Item(2434,"Prayer potion(4)",1),
                new Item(2434,"Prayer potion(4)",1), new Item(2434,"Prayer potion(4)",1), new Item(2434,"Prayer potion(4)",1), new Item(2434,"Prayer potion(4)",1),
                new Item(12625,"Stamina potion(4)",1), new Item(12625,"Stamina potion(4)",1), new Item(12625,"Stamina potion(4)",1), new Item(12625,"Stamina potion(4)",1),
                null, null, null, null,
                null, null, null, new Item(952,"Spade",1),
                new Item(13120,"Falador shield 4",1), new Item(8013,"Teleport to house (tablet)",1), new Item(8009,"Falador teleport (tablet)",1), new Item(4548,"Bullseye lantern",1)),
            List.of("Head selection uses the Wiki listed Helm of Neitiznot alternative because Slayer helmet applies only on task. Light source can be waived after the Fire of Eternal Light unlock.")));
        add(m,new Profile("Sarachnis","Low-level melee, crush with Zamorakian hasta","Melee","https://oldschool.runescape.wiki/w/Sarachnis/Strategies?action=raw&oldid=15384353",
            Arrays.asList(
                new Item(10828,"Helm of Neitiznot",1), new Item(6570,"Fire cape",1), new Item(6585,"Amulet of fury",1), new Item(22947,"Rada's blessing 4",1),
                new Item(11889,"Zamorakian hasta",1), new Item(29022,"Blood Moon chestplate",1), new Item(12954,"Dragon defender",1), new Item(29025,"Blood Moon tassets",1),
                new Item(7462,"Barrows gloves",1), new Item(29806,"Aranea boots",1), new Item(11773,"Berserker ring (i)",1)),
            Arrays.asList(
                new Item(29577,"Burning claws",1), new Item(-1,"Super combat potion",1), new Item(-1,"Prayer potion",1), new Item(-1,"Prayer potion",1),
                new Item(385,"Shark",1), new Item(385,"Shark",1), new Item(-1,"Prayer potion",1), new Item(-1,"Prayer potion",1),
                new Item(385,"Shark",1), new Item(385,"Shark",1), new Item(385,"Shark",1), new Item(385,"Shark",1),
                new Item(385,"Shark",1), new Item(385,"Shark",1), new Item(385,"Shark",1), new Item(385,"Shark",1),
                new Item(385,"Shark",1), new Item(385,"Shark",1), new Item(385,"Shark",1), new Item(385,"Shark",1),
                new Item(385,"Shark",1), new Item(385,"Shark",1), new Item(385,"Shark",1), new Item(25818,"Book of the Dead",1),
                new Item(12791,"Rune pouch",1), new Item(13108,"Wilderness sword 1",1), new Item(13393,"Xeric's talisman",1), new Item(8013,"Teleport to house (tablet)",1)),
            List.of("The Wiki names potions without doses. Their exact dose variant is deliberately unresolved.")));
        add(m,new Profile("Amoxliatl","Melee, full Inquisitor with mace","Melee","https://oldschool.runescape.wiki/w/Amoxliatl/Strategies?action=raw&oldid=15384597",
            Arrays.asList(
                new Item(24419,"Inquisitor's great helm",1), new Item(21295,"Infernal cape",1), new Item(29801,"Amulet of rancour",1), new Item(22947,"Rada's blessing 4",1),
                new Item(24417,"Inquisitor's mace",1), new Item(24420,"Inquisitor's hauberk",1), new Item(22322,"Avernic defender",1), new Item(24421,"Inquisitor's plateskirt",1),
                new Item(22981,"Ferocious gloves",1), new Item(31097,"Avernic treads (max)",1), new Item(28307,"Ultor ring",1)),
            Arrays.asList(
                new Item(29577,"Burning claws",1), new Item(23685,"Divine super combat potion(4)",1), new Item(2434,"Prayer potion(4)",1), new Item(2434,"Prayer potion(4)",1),
                new Item(2434,"Prayer potion(4)",1), new Item(2434,"Prayer potion(4)",1), new Item(2434,"Prayer potion(4)",1), new Item(2434,"Prayer potion(4)",1),
                new Item(2434,"Prayer potion(4)",1), new Item(2434,"Prayer potion(4)",1), new Item(2434,"Prayer potion(4)",1), new Item(3144,"Cooked karambwan",1),
                new Item(3144,"Cooked karambwan",1), new Item(3144,"Cooked karambwan",1), new Item(3144,"Cooked karambwan",1), new Item(3144,"Cooked karambwan",1),
                new Item(3144,"Cooked karambwan",1), null, null, null,
                null, null, new Item(566,"Soul rune",-1), new Item(25340,"Ectoplasmator",1),
                new Item(29893,"Pendant of Ates",1), new Item(25818,"Book of the Dead",1), new Item(27281,"Divine rune pouch",1), new Item(8013,"Teleport to house (tablet)",1)),
            List.of("Avernic defender is paired with the explicitly listed one-handed Inquisitor mace, not the two-handed Obligator. Soul rune stack count is unspecified.")));
        add(m,new Profile("King Black Dragon","Crossbow, standard route","Ranged","https://oldschool.runescape.wiki/w/King_Black_Dragon/Strategies?action=raw&oldid=15372306",
            Arrays.asList(
                new Item(12496,"Ancient coif",1), new Item(10499,"Ava's accumulator",1), new Item(19547,"Necklace of anguish",1), new Item(21946,"Diamond dragon bolts (e)",-1),
                new Item(21012,"Dragon hunter crossbow",1), new Item(27238,"Masori body (f)",1), new Item(-1,"Dragonfire ward",1), new Item(27241,"Masori chaps (f)",1),
                new Item(7462,"Barrows gloves",1), new Item(19921,"Ancient d'hide boots",1), new Item(28327,"Ring of shadows",1)),
            Arrays.asList(
                new Item(-1,"Antidote++",1), new Item(-1,"Extended antifire",1), new Item(2444,"Ranging potion(4)",1), new Item(8013,"Teleport to house (tablet)",1),
                new Item(3024,"Super restore(4)",1), new Item(3024,"Super restore(4)",1), new Item(3024,"Super restore(4)",1), new Item(3024,"Super restore(4)",1),
                new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1),
                new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1),
                new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1),
                new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1),
                new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1), new Item(21166,"Burning amulet(5)",1)),
            List.of("Wilderness entry risks and item protection matter. The Wiki ranged inventory uses unspecified doses for Antidote++ and Extended antifire.")));
        add(m,new Profile("Vorkath","Ranged crossbow","Ranged","https://oldschool.runescape.wiki/w/Vorkath/Strategies?action=raw&oldid=15383963",
            Arrays.asList(
                new Item(27235,"Masori mask (f)",1), new Item(28951,"Dizana's quiver",1), new Item(12018,"Salve amulet(ei)",1), new Item(21944,"Ruby dragon bolts (e)",-1),
                new Item(21012,"Dragon hunter crossbow",1), new Item(27238,"Masori body (f)",1), new Item(-1,"Dragonfire ward",1), new Item(27241,"Masori chaps (f)",1),
                new Item(26235,"Zaryte vambraces",1), new Item(31097,"Avernic treads (max)",1), new Item(28310,"Venator ring",1)),
            Arrays.asList(
                new Item(21946,"Diamond dragon bolts (e)",1), new Item(23736,"Divine ranging potion(3)",1), new Item(22215,"Extended super antifire(2)",1), new Item(-1,"Extended anti-venom+",1),
                new Item(26374,"Zaryte crossbow",1), new Item(2434,"Prayer potion(4)",1), new Item(2434,"Prayer potion(4)",1), new Item(2434,"Prayer potion(4)",1),
                new Item(3144,"Cooked karambwan",1), new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1),
                new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1), new Item(3144,"Cooked karambwan",1), new Item(3144,"Cooked karambwan",1),
                new Item(3144,"Cooked karambwan",1), new Item(3144,"Cooked karambwan",1), new Item(3144,"Cooked karambwan",1), new Item(3144,"Cooked karambwan",1),
                new Item(3144,"Cooked karambwan",1), new Item(3144,"Cooked karambwan",1), new Item(3144,"Cooked karambwan",1), new Item(3144,"Cooked karambwan",1),
                new Item(27281,"Divine rune pouch",1), new Item(-1,"Guthix rest",1), new Item(-1,"Guthix rest",1), new Item(-1,"Guthix rest",1)),
            List.of("low HP Diamond dragon bolts (e) Vorkath/Strategies Ranged crossbow inventory #1","zombified spawn Crumble Undead ['Dust rune', 'Law rune', 'Chaos rune'] variable Vorkath/Strategies Inventory setups prose","Extended anti-venom+ and Guthix rest are named without dose variants. Rendered cheap food is Dark crab and cheap prayer is Prayer potion(4) on retrieval; price-driven choice can change. Dragonfire ward charge variant is unspecified.")));
        add(m,new Profile("Phantom Muspah","Ranged-only, Twisted bow with Zaryte crossbow shield-phase switch","Ranged","https://oldschool.runescape.wiki/w/Phantom_Muspah/Strategies?action=raw&oldid=15381056",
            Arrays.asList(
                new Item(27235,"Masori mask (f)",1), new Item(28951,"Dizana's quiver",1), new Item(33639,"Necklace of rupture",1), new Item(33595,"Seeking dragon arrow",-1),
                new Item(20997,"Twisted bow",1), new Item(27238,"Masori body (f)",1), null, new Item(27241,"Masori chaps (f)",1),
                new Item(26235,"Zaryte vambraces",1), new Item(31097,"Avernic treads (max)",1), new Item(25975,"Lightbearer",1)),
            Arrays.asList(
                new Item(26374,"Zaryte crossbow",1), new Item(21940,"Sapphire dragon bolts (e)",1), new Item(25818,"Book of the Dead",1), new Item(27281,"Divine rune pouch",1),
                new Item(21944,"Ruby dragon bolts (e)",1), new Item(-1,"Divine ranging potion",1), new Item(-1,"Stamina potion",1), new Item(-1,"Stamina potion",1),
                new Item(-1,"Surge potion",1), new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1),
                new Item(-1,"Prayer potion",1), new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1),
                new Item(-1,"Prayer potion",1), new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1),
                new Item(-1,"Prayer potion",1), new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1), new Item(11936,"Dark crab",1),
                new Item(-1,"Prayer potion",1), new Item(11936,"Dark crab",1), new Item(28327,"Ring of shadows",1), new Item(8013,"Teleport to house (tablet)",1)),
            List.of("prayer shield Zaryte crossbow Sapphire dragon bolts (e) Ranged-only Inventory #2 and shielded-phase prose","['Blood rune', 'Fire rune', 'Cosmic rune', 'Soul rune'] variable Ranged-only Rune pouch","Unspecified doses on the rendered potion families are retained as variant candidates; MinPrice food currently renders Dark crab and can change. The Wiki lists a buckler for a crossbow alternative, so it cannot be equipped with the selected two-handed Twisted bow.")));
        add(m,new Profile("Hueycoatl","Body melee plus tail switches","Melee","https://oldschool.runescape.wiki/w/The_Hueycoatl/Strategies?action=raw&oldid=15383983",
            Arrays.asList(
                new Item(24419,"Inquisitor's great helm",1), new Item(21295,"Infernal cape",1), new Item(29801,"Amulet of rancour",1), new Item(22947,"Rada's blessing 4",1),
                new Item(24417,"Inquisitor's mace",1), new Item(24420,"Inquisitor's hauberk",1), new Item(22322,"Avernic defender",1), new Item(24421,"Inquisitor's plateskirt",1),
                new Item(22981,"Ferocious gloves",1), new Item(31097,"Avernic treads (max)",1), new Item(28307,"Ultor ring",1)),
            Arrays.asList(
                new Item(23685,"Divine super combat potion(4)",1), new Item(30125,"Prayer regeneration potion(4)",1), new Item(2434,"Prayer potion(4)",1), new Item(2434,"Prayer potion(4)",1),
                new Item(28997,"Dual macuahuitl",1), new Item(29028,"Blood Moon helm",1), new Item(2434,"Prayer potion(4)",1), new Item(385,"Shark",1),
                new Item(29022,"Blood Moon chestplate",1), new Item(29025,"Blood Moon tassets",1), new Item(385,"Shark",1), new Item(385,"Shark",1),
                new Item(13652,"Dragon claws",1), new Item(27660,"Ursine chainmace",1), new Item(385,"Shark",1), new Item(385,"Shark",1),
                new Item(385,"Shark",1), new Item(385,"Shark",1), new Item(385,"Shark",1), new Item(385,"Shark",1),
                new Item(385,"Shark",1), new Item(385,"Shark",1), new Item(385,"Shark",1), new Item(6714,"Holy wrench",1),
                new Item(13441,"Anglerfish",1), new Item(27641,"Saturated heart",1), new Item(25818,"Book of the Dead",1), new Item(27281,"Divine rune pouch",1)),
            List.of("tail Dual macuahuitl ['Blood Moon helm', 'Blood Moon chestplate', 'Blood Moon tassets'] The Hueycoatl/Strategies Tail recommended equipment and Inventory #1","The selected Inquisitor mace is one-handed with Avernic defender for body and head. For the two-handed tail switch, consume a potion or food first to create room for the defender. Source image starts with 28 occupied slots; a switch without freeing space would fail.")));
        add(m,new Profile("Grotesque Guardians","Mid-game melee with Eclipse atlatl ranged switch","Melee","https://oldschool.runescape.wiki/w/Grotesque_Guardians/Strategies?action=raw&oldid=15383964",
            Arrays.asList(
                new Item(11865,"Slayer helmet (i)",1), new Item(6570,"Fire cape",1), new Item(6585,"Amulet of fury",1), new Item(28991,"Atlatl dart",1),
                new Item(21742,"Granite hammer",1), new Item(10551,"Fighter torso",1), new Item(12954,"Dragon defender",1), new Item(4759,"Verac's plateskirt",1),
                new Item(7462,"Barrows gloves",1), new Item(11840,"Dragon boots",1), new Item(11773,"Berserker ring (i)",1)),
            Arrays.asList(
                new Item(29000,"Eclipse atlatl",1), new Item(22109,"Ava's assembler",1), new Item(-1,"Divine super combat potion",1), new Item(-1,"Divine ranging potion",1),
                new Item(29007,"Eclipse Moon tassets",1), new Item(29004,"Eclipse Moon chestplate",1), new Item(-1,"Saradomin brew",1), new Item(-1,"Saradomin brew",1),
                new Item(13441,"Anglerfish",1), new Item(13441,"Anglerfish",1), new Item(3024,"Super restore(4)",1), new Item(3024,"Super restore(4)",1),
                new Item(13441,"Anglerfish",1), new Item(13441,"Anglerfish",1), new Item(3024,"Super restore(4)",1), new Item(3024,"Super restore(4)",1),
                new Item(13441,"Anglerfish",1), new Item(13441,"Anglerfish",1), new Item(3024,"Super restore(4)",1), new Item(3024,"Super restore(4)",1),
                new Item(13441,"Anglerfish",1), new Item(13441,"Anglerfish",1), new Item(3024,"Super restore(4)",1), new Item(3024,"Super restore(4)",1),
                new Item(1215,"Dragon dagger",1), new Item(3144,"Cooked karambwan",1), new Item(-1,"Slayer ring",1), new Item(27281,"Divine rune pouch",1)),
            List.of("Dawn ranged Eclipse atlatl ['Eclipse Moon chestplate', 'Eclipse Moon tassets', \"Ava's assembler\"] Atlatl dart Mid-game tab, Inventory #2","['Nature rune', 'Fire rune', 'Law rune', 'Dust rune'] variable Mid-game Rune pouch","Granite hammer worn removes the separate rock-hammer requirement. Eclipse atlatl is a two-handed ranged switch: first consume a potion or food to make an empty inventory slot for the defender.")));
        add(m,new Profile("Barrows","Standard lower-level hybrid, Magic primary","Magic","https://oldschool.runescape.wiki/w/Barrows/Strategies?action=raw&oldid=15384294",
            Arrays.asList(
                new Item(10828,"Helm of Neitiznot",1), new Item(10448,"Guthix cloak",1), new Item(34407,"Amulet of air",1), new Item(892,"Rune arrow",1),
                new Item(1397,"Air battlestaff",1), new Item(4091,"Mystic robe top",1), new Item(1201,"Rune kiteshield",1), new Item(4093,"Mystic robe bottom",1),
                new Item(11118,"Combat bracelet(4)",1), new Item(3105,"Climbing boots",1), new Item(2552,"Ring of dueling(8)",1)),
            Arrays.asList(
                new Item(4151,"Abyssal whip",1), new Item(2501,"Red d'hide body",1), new Item(12788,"Magic shortbow (i)",1), new Item(2434,"Prayer potion(4)",1),
                new Item(12954,"Dragon defender",1), new Item(2495,"Red d'hide chaps",1), null, new Item(2434,"Prayer potion(4)",1),
                new Item(1215,"Dragon dagger",1), new Item(2436,"Super attack(4)",1), new Item(2440,"Super strength(4)",1), new Item(2434,"Prayer potion(4)",1),
                new Item(385,"Shark",1), new Item(2430,"Restore potion(4)",1), new Item(12625,"Stamina potion(4)",1), new Item(2434,"Prayer potion(4)",1),
                new Item(385,"Shark",1), new Item(385,"Shark",1), new Item(385,"Shark",1), new Item(385,"Shark",1),
                new Item(385,"Shark",1), new Item(385,"Shark",1), null, new Item(25340,"Ectoplasmator",1),
                new Item(560,"Death rune",1), new Item(554,"Fire rune",1), new Item(952,"Spade",1), new Item(19629,"Barrows teleport (tablet)",1)),
            List.of("Equipped rune arrows serve the magic shortbow (i) inventory switch. Two inventory cells are intentionally empty in the Wiki illustration.")));
        add(m,new Profile("Dagannoth Kings","Rex safespot with Guthan healing switch","Magic","https://oldschool.runescape.wiki/w/Dagannoth_Kings/Strategies?action=raw&oldid=15383969",
            Arrays.asList(
                new Item(4724,"Guthan's helm",1), new Item(21791,"Imbued Saradomin cape",1), new Item(12002,"Occult necklace",1), new Item(20226,"Peaceful blessing",1),
                new Item(12899,"Trident of the Swamp",1), new Item(4728,"Guthan's platebody",1), new Item(23991,"Crystal shield",1), new Item(4730,"Guthan's chainskirt",1),
                new Item(7462,"Barrows gloves",1), new Item(11836,"Bandos boots",1), new Item(13127,"Explorer's ring 3",1)),
            Arrays.asList(
                new Item(4726,"Guthan's warspear",1), new Item(-1,"Prayer potion",1), new Item(12625,"Stamina potion(4)",1), new Item(5952,"Antidote++(4)",1),
                new Item(2434,"Prayer potion(4)",1), new Item(2434,"Prayer potion(4)",1), new Item(2434,"Prayer potion(4)",1), new Item(5952,"Antidote++(4)",1),
                new Item(385,"Shark",1), new Item(2434,"Prayer potion(4)",1), new Item(5952,"Antidote++(4)",1), new Item(5952,"Antidote++(4)",1),
                new Item(385,"Shark",1), new Item(385,"Shark",1), new Item(385,"Shark",1), new Item(5952,"Antidote++(4)",1),
                new Item(385,"Shark",1), new Item(385,"Shark",1), new Item(385,"Shark",1), new Item(5952,"Antidote++(4)",1),
                new Item(385,"Shark",1), new Item(385,"Shark",1), new Item(385,"Shark",1), new Item(8013,"Teleport to house (tablet)",1),
                new Item(13226,"Herb sack",1), new Item(3695,"Pet rock",1), new Item(805,"Rune thrownaxe",1), new Item(12791,"Rune pouch",1)),
            List.of("Guthan healing Guthan's warspear [\"Guthan's helm\", \"Guthan's platebody\", \"Guthan's chainskirt\"] Rex Safespot inventory","Rex safespot tab explicitly pairs Guthan armour with the warspear in inventory. Guthan healing switch is two-handed; free a slot before stowing trident and shield. Crystal shield charge state is not specified. Prime and Supreme remain separate unvalidated methods.")));
        add(m,new Profile("Zulrah","Magic-only Zulrah loadout","Magic","https://oldschool.runescape.wiki/w/Zulrah/Strategies?action=raw&oldid=15384094",
            Arrays.asList(
                new Item(4708,"Ahrim's hood",1), new Item(21791,"Imbued Saradomin cape",1), new Item(12002,"Occult necklace",1), new Item(22947,"Rada's blessing 4",1),
                new Item(30634,"Twinflame staff",1), new Item(4712,"Ahrim's robetop",1), new Item(20714,"Tome of Fire",1), new Item(4714,"Ahrim's robeskirt",1),
                new Item(19544,"Tormented bracelet",1), new Item(6920,"Infinity boots",1), new Item(20657,"Ring of suffering (ri)",1)),
            Arrays.asList(
                new Item(385,"Shark",1), new Item(3144,"Cooked karambwan",1), new Item(385,"Shark",1), new Item(3144,"Cooked karambwan",1),
                new Item(385,"Shark",1), new Item(3144,"Cooked karambwan",1), new Item(385,"Shark",1), new Item(3144,"Cooked karambwan",1),
                new Item(385,"Shark",1), new Item(3144,"Cooked karambwan",1), new Item(385,"Shark",1), new Item(3144,"Cooked karambwan",1),
                new Item(385,"Shark",1), new Item(3144,"Cooked karambwan",1), new Item(385,"Shark",1), new Item(29824,"Extended anti-venom+(4)",1),
                new Item(385,"Shark",1), new Item(3144,"Cooked karambwan",1), new Item(385,"Shark",1), new Item(2434,"Prayer potion(4)",1),
                new Item(385,"Shark",1), new Item(3144,"Cooked karambwan",1), new Item(385,"Shark",1), new Item(2434,"Prayer potion(4)",1),
                new Item(12938,"Zul-andra teleport",1), new Item(27641,"Saturated heart",1), new Item(-1,"Ring of dueling",1), new Item(12791,"Rune pouch",1)),
            List.of("['Air rune', 'Blood rune'] Rune pouch rendered beside Zulrah (magic) Loadout","Wiki magic-only method performs worse on Tanzanite phase; rune pouch shows Air and Blood runes. Potion and food slots are rendered explicitly.")));
        Map<String,List<Profile>> immutable=new LinkedHashMap<>();
        m.forEach((k,v)->immutable.put(k,List.copyOf(v)));
        DATA=Collections.unmodifiableMap(immutable);
    }
    private static void add(Map<String,List<Profile>> map,Profile profile) {
        map.computeIfAbsent(profile.boss(),k->new ArrayList<>()).add(profile);
    }
    public static List<Profile> profiles(String boss) { return DATA.getOrDefault(boss,List.of()); }
    public static Profile find(String boss,String method) {
        return profiles(boss).stream().filter(p->p.method().equals(method)).findFirst().orElse(null);
    }
}
