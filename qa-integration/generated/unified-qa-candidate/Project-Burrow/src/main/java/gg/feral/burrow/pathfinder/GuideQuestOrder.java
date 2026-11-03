package gg.feral.burrow.pathfinder;

import java.util.*;

/**
 * Build 19: ironman.guide/quests order (215 entries, checked 2026-11-01), names normalised to RuneLite
 * 1.13.4 {@code Quest#getName()}. Skill levels are as listed by that page next to each entry (the
 * page's own "needed at this point" annotations, not a full recursive Wiki requirement table).
 * QP, kudos and other non-skill notes are kept as UNVERIFIED notes. Entries absent from the RuneLite
 * Quest enum (e.g. Natural History Quiz, Knight Waves) are reported in the coverage gap register.
 */
public final class GuideQuestOrder {
    private GuideQuestOrder() {}
    public record Entry(int order, String name, Map<String,Integer> skills, String note, boolean miniquest) {}
    /** name | skills | note.  "*" before a name marks a miniquest. */
    private static final String DATA = """
Learning the Ropes|Hitpoints 10|
Cook's Assistant|Cooking 4|
Sheep Shearer|Crafting 3|
Misthalin Mystery||
The Restless Ghost|Prayer 9|
X Marks the Spot|Agility 4|
Witch's Potion|Magic 4|
Imp Catcher||
Romeo & Juliet||
Gertrude's Cat||
Rune Mysteries||
Client of Kourend|Agility 10|
Children of the Sun|Crafting 1,Magic 10|
Pandemonium|Sailing 5|
Doric's Quest||
Goblin Diplomacy||
Ernest the Chicken||
Prince Ali Rescue||
Shield of Arrav||
Pirate's Treasure||
Black Knights' Fortress||12 QP
The Knight's Sword|Mining 10|
Druidic Ritual||
Witch's House||
*Daddy's Home||
*Natural History Quiz||not in RuneLite Quest enum
Waterfall Quest||
Tree Gnome Village||
The Grand Tree|Agility 25|requires Tree Gnome Village
Fight Arena||
Vampyre Slayer||
Death Plateau||
Holy Grail|Attack 20|
Merlin's Crystal||
Plague City||
Biohazard||
Hazeel Cult||
Monk's Friend||
Sheep Herder||
Priest in Peril||
Recruitment Drive||12 QP
The Tourist Trap|Fletching 10,Smithing 20|
Clock Tower||
Murder Mystery||
Dwarf Cannon||
Fishing Contest|Fishing 10|
Jungle Potion||
The Corsair Curse||
Below Ice Mountain||16 QP
The Ides of Milk||
Sleeping Giants|Smithing 15|
Making History||
*Enter the Abyss||requires Rune Mysteries
Demon Slayer||
A Ruff Situation|Crafting 15|
Fairytale I - Growing Pains||
Lost City|Woodcutting 36,Crafting 31|
Fairytale II - Cure a Queen|Thieving 40,Farming 49,Herblore 57|not needed to unlock fairy rings
Nature Spirit|Crafting 18|
Observatory Quest||
The Lost Tribe|Agility 13,Thieving 13,Mining 17|
Death to the Dorgeshuun|Agility 23,Thieving 23|
Another Slice of H.A.M.|Attack 15,Prayer 25|
Ghosts Ahoy|Agility 25,Cooking 20|
Dragon Slayer I||32 QP
Scorpion Catcher|Prayer 31|
Tribal Totem|Thieving 21|
The Dig Site|Agility 10,Herblore 10,Thieving 25|
Shadow of the Storm|Crafting 30|
Horror from the Deep|Agility 35|
Animal Magnetism|Slayer 18,Crafting 19,Ranged 30,Woodcutting 35|
Icthlarin's Little Helper||
The Golem|Crafting 20,Thieving 25|
Elemental Workshop I|Mining 20,Smithing 20,Crafting 20|
Elemental Workshop II|Magic 20,Smithing 30|
A Soul's Bane||
Shilo Village|Crafting 20,Agility 32|
Big Chompy Bird Hunting|Fletching 5,Cooking 30,Ranged 30|
Zogre Flesh Eaters|Smithing 4,Herblore 8,Ranged 30|
Mountain Daughter|Agility 20|
Sea Slug|Firemaking 30|
The Slug Menace|Crafting 30,Runecraft 30,Slayer 30,Thieving 30|
Wanted!||32 QP
Tower of Life|Construction 10|
Creature of Fenkenstrain|Crafting 20,Thieving 25|
Troll Stronghold|Agility 15|
Troll Romance|Agility 28|
Eadgar's Ruse|Herblore 31|
The Feud|Thieving 30|
The Fremennik Trials|Fletching 25,Woodcutting 40,Crafting 40|
The Fremennik Isles|Construction 20|
In Search of the Myreque|Agility 25|
In Aid of the Myreque|Crafting 25,Mining 15,Magic 7|
Shades of Mort'ton|Crafting 15,Firemaking 20,Herblore 5|
What Lies Below|Runecraft 35|
The Giant Dwarf|Crafting 12,Firemaking 16,Magic 33,Thieving 14|
Forgettable Tale...|Cooking 22,Farming 17|
Between a Rock...|Defence 30,Mining 40,Smithing 50|
Contact!||
The Eyes of Glouphrie|Construction 5,Magic 46|
Temple of Ikov|Ranged 40,Thieving 42|
Watchtower|Herblore 14,Magic 14,Thieving 15,Agility 25,Mining 40|
Rag and Bone Man I||
Eagles' Peak|Hunter 27|
Enlightened Journey|Firemaking 20,Farming 30,Crafting 36|20 QP
Tears of Guthix|Firemaking 49,Crafting 20,Mining 20|43 QP
Bone Voyage||100 Kudos
The Queen of Thieves|Thieving 20|
The Depths of Despair|Agility 18|
The Ascent of Arceuus|Hunter 12|
Tale of the Righteous|Strength 16,Mining 10|
The Forsaken Tower||
A Porcine of Interest||
Ratcatchers||
Temple of the Eye|Runecraft 10|
The Ribbiting Tale of a Lily Pad Labour Dispute||
Getting Ahead|Crafting 30,Construction 26|
Cold War|Hunter 10,Agility 30,Crafting 30,Construction 34,Thieving 15|
The Hand in the Sand|Thieving 17,Crafting 49|
Enakhra's Lament|Magic 39,Prayer 43,Firemaking 45,Crafting 50|
One Small Favour|Agility 36,Crafting 25,Herblore 18,Smithing 30|
Cabin Fever|Agility 42,Crafting 45,Smithing 50,Ranged 40|
Rum Deal|Farming 40,Crafting 42,Slayer 42,Prayer 47,Fishing 50|
Tai Bwo Wannai Trio|Agility 15,Cooking 30,Fishing 5|
Family Crest|Crafting 40,Mining 40,Smithing 40,Magic 59|
Recipe for Disaster - Another Cook's Quest|Cooking 10|
Recipe for Disaster - Mountain Dwarf||
Recipe for Disaster - Wartface & Bentnoze||
Recipe for Disaster - Pirate Pete|Cooking 31|
Recipe for Disaster - Lumbridge Guide|Cooking 40|
Recipe for Disaster - Evil Dave||
Recipe for Disaster - Skrach Uglogwee|Cooking 41,Firemaking 20|
Prying Times|Sailing 12,Smithing 30|
Current Affairs|Fishing 10,Sailing 22|
Crab Quest|Sailing 40,Fishing 30|
Desert Treasure I|Magic 50,Firemaking 50,Slayer 10,Thieving 53|
Monkey Madness I||
Underground Pass|Ranged 25|
Regicide|Agility 56|
Roving Elves||
Mourning's End Part I|Ranged 60,Thieving 50|
Mourning's End Part II||
Lunar Diplomacy|Herblore 5,Defence 40,Firemaking 49,Woodcutting 55,Mining 60,Crafting 61,Magic 65|
Dream Mentor||85 combat level
King's Ransom|Magic 45,Defence 65|
*Knight Waves Training Ground||not in RuneLite Quest enum
Darkness of Hallowvale|Construction 5,Mining 20,Thieving 22,Agility 26,Crafting 32,Magic 33,Strength 40|
A Taste of Hope|Crafting 48,Agility 45,Attack 40,Herblore 40,Slayer 38|
Throne of Miscellania||
Royal Trouble|Agility 40,Slayer 40|
Heroes' Quest|Herblore 25,Mining 50,Cooking 53,Fishing 53|55 QP
Legends' Quest|Agility 50,Crafting 50,Herblore 45,Magic 56,Mining 52,Prayer 42,Smithing 50,Strength 50,Thieving 50,Woodcutting 50|107 QP
Recipe for Disaster - Sir Amik Varze||107 QP
Recipe for Disaster - King Awowogei|Agility 48,Cooking 70|
Recipe for Disaster - Culinaromancer||175 QP
Garden of Tranquillity|Farming 25|
A Tail of Two Cats||
Devious Minds|Fletching 50,Runecraft 50,Smithing 65|
My Arm's Big Adventure|Farming 29|
Swan Song|Crafting 40,Firemaking 42,Smithing 45,Cooking 62,Fishing 62,Magic 66|100 QP
The Great Brain Robbery|Crafting 16,Construction 30,Prayer 50|
Olaf's Quest|Firemaking 40,Woodcutting 50|
Grim Tales|Farming 45,Herblore 52,Thieving 58,Agility 59,Woodcutting 71|
The Path of Glouphrie|Agility 45,Ranged 47,Slayer 56,Thieving 56,Strength 60|
Land of the Goblins|Agility 38,Fishing 40,Thieving 45,Herblore 48|
Spirits of the Elid|Magic 33,Ranged 37,Mining 37,Thieving 37|
The Garden of Death|Farming 20|
Twilight's Promise||
At First Light|Hunter 46,Herblore 30,Construction 27|
Death on the Isle|Agility 32,Thieving 34|
The Heart of Darkness|Agility 46,Slayer 48,Thieving 48,Mining 55|
Meat and Greet||
Ethically Acquired Antiquities|Thieving 25|
Shadows of Custodia|Hunter 36,Construction 41,Fishing 45,Slayer 54|
Scrambled!|Smithing 35,Cooking 36,Construction 38|
Troubled Tortugans|Crafting 34,Woodcutting 40,Hunter 45,Sailing 45,Construction 48,Slayer 51|
The Red Reef|Smithing 48,Sailing 52|
Fallen From Grace|Sailing 62,Crafting 60,Mining 53,Runecraft 47|
*Alfred Grimhand's Barcrawl||
Skippy and the Mogres|Cooking 20|
*Barbarian Training||
*Mage Arena I|Magic 60|
Song of the Elves|Agility 70,Construction 70,Farming 70,Herblore 70,Hunter 70,Mining 70,Smithing 70,Woodcutting 70|
Dragon Slayer II|Construction 50,Hitpoints 50,Agility 60,Thieving 60,Crafting 62,Mining 68,Smithing 70,Magic 75|200 QP
Monkey Madness II|Agility 55,Thieving 55,Firemaking 60,Hunter 60,Slayer 69,Crafting 70|
Sins of the Father|Magic 49,Attack 50,Slayer 50,Agility 52,Crafting 56,Fletching 60,Woodcutting 62|
A Night at the Theatre||90+ combat recommended
A Kingdom Divided|Magic 35,Crafting 38,Mining 42,Herblore 50,Thieving 52,Woodcutting 52,Agility 54|
Beneath Cursed Sands|Crafting 55,Firemaking 55,Agility 62|
The Fremennik Exiles|Runecraft 55,Fishing 60,Mining 60,Smithing 60,Slayer 60,Crafting 65|
Making Friends with My Arm|Firemaking 66,Mining 72,Agility 68,Construction 35|
Secrets of the North|Hunter 56,Thieving 64,Agility 69|
Desert Treasure II - The Fallen Empire|Construction 60,Runecraft 60,Herblore 62,Thieving 70,Firemaking 75,Magic 75|
Perilous Moons|Construction 10,Fishing 20,Hunter 20,Runecraft 20,Slayer 48|
Defender of Varrock|Hunter 52,Smithing 55|
The Curse of Arrav|Slayer 37,Strength 58,Agility 61,Ranged 62,Thieving 62,Mining 64|
While Guthix Sleeps|Thieving 72,Magic 67,Agility 66,Farming 65,Herblore 65,Hunter 62,Firemaking 49|180 QP; Warriors' Guild entry
*The Fractured Archive||110+ combat recommended
The Final Dawn|Fletching 52,Runecraft 52,Thieving 66|
The Blood Moon Rises|Magic 57,Crafting 64,Herblore 64,Hunter 65,Mining 66,Fletching 70,Cooking 72,Smithing 72,Slayer 74,Woodcutting 74|
*Mage Arena II|Magic 75|
*His Faithful Servants||high combat recommended
*Into the Tombs||95+ combat recommended
Lair of Tarn Razorlor|Slayer 40|
Haunted Mine|Agility 15,Crafting 35|
Rag and Bone Man II|Slayer 40,Defence 20|
*Curse of the Empty Lord|Thieving 53|31 Prayer if ghost is in Edgeville Monastery
*The Enchanted Key||
*The General's Shadow||
*Bear Your Soul||
*Family Pest||
*Hopespear's Will|Prayer 50|
*Vale Totems|Fletching 20|
*In Search of Knowledge||
*The Frozen Door|Agility 70,Hitpoints 70,Ranged 70,Strength 70|
""";
    private static final List<Entry> ENTRIES = parse();
    public static List<Entry> entries() { return ENTRIES; }
    private static List<Entry> parse() {
        List<Entry> out=new ArrayList<>();
        int order=0;
        for(String raw:DATA.split("\n")) {
            if(raw.isBlank())continue;
            String[] f=raw.split("\\|",-1);
            boolean mini=f[0].startsWith("*");
            String name=mini?f[0].substring(1):f[0];
            Map<String,Integer> skills=new LinkedHashMap<>();
            if(!f[1].isBlank()) for(String pair:f[1].split(",")) {
                String p=pair.trim(); int sp=p.lastIndexOf(' ');
                skills.put(p.substring(0,sp),Integer.parseInt(p.substring(sp+1)));
            }
            out.add(new Entry(++order,name,Collections.unmodifiableMap(skills),f.length>2?f[2]:"",mini));
        }
        return List.copyOf(out);
    }
}
