package gg.feral.burrow.pathfinder;

import gg.feral.burrow.pathfinder.GuideRule.Category;
import gg.feral.burrow.pathfinder.GuideRule.ModeScope;
import gg.feral.burrow.pathfinder.SuggestionEngine.Focus;
import java.util.*;

/**
 * Build 19 Pathfinder rule registry. Every rule carries a source link. Rules are compact original
 * summaries authored from: ironman.guide (v4, 2026: quest order, AFK and money-making tables), the
 * OSRS Wiki Ironman guide (oldid 15382330), Wiki quest requirement sections (SotE, DS2, WGS, MM2,
 * DT2), the Wiki AFK guide (oldid 15375936) and Burrow's own reviewed FINAL-06 Outfitter metadata.
 * Item IDs were verified against RuneLite 1.13.4 {@code net.runelite.api.ItemID}.
 */
public final class GuideData {
    private GuideData() {}
    private static final Map<String,String> LEGACY_QUEST_IDS = Map.of(
        "Cook's Assistant","quest_cooks_assistant", "Song of the Elves","quest_sote");
    // Verified OSRS Wiki no-level novice quests; ironman.guide waypoints are NOT hard quest gates.
    private static final Set<String> NO_HARD_SKILL_GATE = Set.of(
        "Cook's Assistant", "Sheep Shearer", "The Restless Ghost", "X Marks the Spot", "Witch's Potion");
    private static List<GuideRule> RULES;
    private static final Map<String,List<String>> QUEST_PREREQS = questPrerequisites();
    public static synchronized List<GuideRule> rules() { if(RULES==null)RULES=build(); return RULES; }
    public static Map<String,List<String>> questPrerequisites() {
        if(QUEST_PREREQS!=null)return QUEST_PREREQS;
        Map<String,List<String>> m=new LinkedHashMap<>();
        // OSRS Wiki quest pages, Requirements sections (flattened lists as published), checked 2026-11-01.
        m.put("Song of the Elves",List.of("Mourning's End Part II","Mourning's End Part I","Big Chompy Bird Hunting","Sheep Herder",
            "Roving Elves","Waterfall Quest","Regicide","Underground Pass","Biohazard","Plague City","Making History",
            "Priest in Peril","The Restless Ghost","Druidic Ritual"));
        m.put("Dragon Slayer II",List.of("Legends' Quest","Family Crest","Heroes' Quest","Shield of Arrav","Lost City",
            "Merlin's Crystal","Dragon Slayer I","Underground Pass","Biohazard","Plague City","Waterfall Quest","Dream Mentor",
            "Lunar Diplomacy","The Fremennik Trials","Rune Mysteries","Shilo Village","Jungle Potion","Eadgar's Ruse",
            "Druidic Ritual","Troll Stronghold","Death Plateau","A Tail of Two Cats","Icthlarin's Little Helper",
            "Gertrude's Cat","Animal Magnetism","Ernest the Chicken","Priest in Peril","The Restless Ghost","Ghosts Ahoy",
            "Bone Voyage","The Dig Site","Client of Kourend","X Marks the Spot"));
        m.put("While Guthix Sleeps",List.of("Defender of Varrock","Shield of Arrav","Temple of Ikov","Below Ice Mountain",
            "Family Crest","What Lies Below","Romeo & Juliet","Demon Slayer","Garden of Tranquillity","Creature of Fenkenstrain",
            "Priest in Peril","The Restless Ghost","The Path of Glouphrie","The Eyes of Glouphrie","The Grand Tree",
            "Waterfall Quest","Tree Gnome Village","Fight Arena","Dream Mentor","Eadgar's Ruse","Druidic Ritual",
            "Troll Stronghold","Death Plateau","Lunar Diplomacy","The Fremennik Trials","Lost City","Rune Mysteries",
            "Shilo Village","Jungle Potion","The Hand in the Sand","Wanted!","Recruitment Drive","Black Knights' Fortress",
            "The Lost Tribe","Goblin Diplomacy","Temple of the Eye","Enter the Abyss","Tears of Guthix","Nature Spirit",
            "A Tail of Two Cats","Icthlarin's Little Helper","Gertrude's Cat"));
        m.put("Monkey Madness II",List.of("Enlightened Journey","The Eyes of Glouphrie","Recipe for Disaster - King Awowogei",
            "Monkey Madness I","The Grand Tree","Tree Gnome Village","Troll Stronghold","Death Plateau","Watchtower"));
        m.put("Desert Treasure II - The Fallen Empire",List.of("Desert Treasure I","The Dig Site","Temple of Ikov","The Tourist Trap",
            "Troll Stronghold","Death Plateau","Priest in Peril","Waterfall Quest","Secrets of the North",
            "Making Friends with My Arm","My Arm's Big Adventure","Eadgar's Ruse","Druidic Ritual","The Feud","Jungle Potion",
            "Swan Song","One Small Favour","Rune Mysteries","Shilo Village","Garden of Tranquillity","Creature of Fenkenstrain",
            "The Restless Ghost","Cold War","Romeo & Juliet","The General's Shadow","Fight Arena","Devious Minds","Wanted!",
            "Recruitment Drive","Black Knights' Fortress","The Lost Tribe","Goblin Diplomacy","Doric's Quest","Enter the Abyss",
            "Hazeel Cult","Enakhra's Lament","Temple of the Eye","The Garden of Death","Below Ice Mountain","His Faithful Servants"));
        // ironman.guide/quests annotations.
        m.put("The Grand Tree",List.of("Tree Gnome Village"));
        m.put("Enter the Abyss",List.of("Rune Mysteries"));
        // Recipe for Disaster structure (Wiki): every subquest follows Another Cook's Quest; the finale needs all.
        List<String> rfd=List.of("Recipe for Disaster - Mountain Dwarf","Recipe for Disaster - Wartface & Bentnoze",
            "Recipe for Disaster - Pirate Pete","Recipe for Disaster - Lumbridge Guide","Recipe for Disaster - Evil Dave",
            "Recipe for Disaster - Skrach Uglogwee","Recipe for Disaster - Sir Amik Varze","Recipe for Disaster - King Awowogei");
        for(String sub:rfd) m.put(sub,List.of("Recipe for Disaster - Another Cook's Quest"));
        List<String> finale=new ArrayList<>(); finale.add("Recipe for Disaster - Another Cook's Quest"); finale.addAll(rfd);
        m.put("Recipe for Disaster - Culinaromancer",finale);
        // Named sequels.
        m.put("Fairytale II - Cure a Queen",List.of("Fairytale I - Growing Pains"));
        m.put("Mourning's End Part II",List.of("Mourning's End Part I"));
        m.put("Elemental Workshop II",List.of("Elemental Workshop I"));
        m.put("Rag and Bone Man II",List.of("Rag and Bone Man I"));
        m.put("Mage Arena II",List.of("Mage Arena I"));
        return Collections.unmodifiableMap(m);
    }

    static String slug(String s) {
        return s.toLowerCase(Locale.ROOT).replace("'","").replace("&","and").replaceAll("[^a-z0-9]+","_").replaceAll("^_|_$","");
    }

    private static List<GuideRule> build() {
        List<GuideRule> out=new ArrayList<>();
        legacy(out);
        quests(out);
        bosses(out);
        afk(out);
        money(out);
        gear(out);
        milestones(out);
        Set<String> ids=new HashSet<>();
        for(GuideRule r:out) if(!ids.add(r.id())) throw new IllegalStateException("duplicate rule "+r.id());
        return List.copyOf(out);
    }

    // ----- The eight Build 17/18 activities: identical IDs and semantics, now with sourced requirements -----
    private static void legacy(List<GuideRule> out) {
        out.add(GuideRule.of("pvm_gauntlet","Try the Gauntlet",Category.LEGACY).section("2.0 Goals after Barrows gloves")
            .focus(Focus.PVM).minutes(20).quest("Song of the Elves").boss(null)
            .why("A Prifddinas encounter and the route toward crystal equipment (Corrupted Gauntlet).")
            .next("Practise the normal Gauntlet before the Corrupted version.")
            .source(GuideSources.WIKI+"The_Gauntlet").build());
        out.add(GuideRule.of("pvm_vorkath","Fight Vorkath",Category.BOSS).section("2.0 Goals after Barrows gloves")
            .focus(Focus.PVM).minutes(20).quest("Dragon Slayer II").boss("Vorkath")
            .why("Boss activity with supplies and alchable drops; check your combat setup first.")
            .next("Open Vorkath in Outfitter and check Best in Bank.")
            .source(GuideSources.WIKI+"Vorkath/Strategies").build());
        out.add(GuideRule.of("gp_gargoyles","Consider gargoyles for coins",Category.MONEY).section("Money-making")
            .focus(Focus.GP).low().minutes(20).skill("Slayer",75).gp(400,650)
            .unverified("Slayer Tower access, rock hammer and gargoyle task")
            .why("Slayer activity with alchable drops; ironman.guide lists 400k-650k/hr (estimate).")
            .next("Bring a rock hammer and take a gargoyle task.")
            .source(GuideSources.WIKI+"Gargoyle").build());
        out.add(GuideRule.of("gp_silk","Steal silk in Ardougne",Category.MONEY).section("Money-making")
            .focus(Focus.GP).minutes(10).skill("Thieving",20).gp(50,80).mode(ModeScope.BOTH)
            .why("Direct coins by selling stolen silk to an NPC; ironman.guide lists 50k-80k/hr (estimate).")
            .next("Steal from the Ardougne silk stall and sell to the silk trader.")
            .source(GuideSources.IRONMAN_GUIDE_MONEY).build());
        out.add(GuideRule.of("supplies_fishing","Gather food through Fishing",Category.LEGACY).section("Supplies")
            .focus(Focus.SUPPLIES).low().minutes(10).afk(117)
            .why("Gather food and gain Fishing experience; choose a method you can access.")
            .next("Fly fish trout/salmon (Wiki AFK guide: ~2 min between inputs).")
            .source(GuideSources.WIKI+"Ironman_guide").build());
        out.add(GuideRule.of("pvm_demons","Consider Tormented Demons",Category.BOSS).section("Bossing")
            .focus(Focus.PVM).minutes(30).quest("While Guthix Sleeps").needsOwned(29589).boss("Tormented Demons")
            .why("Emberlight was observed in your bank; a second style, supplies and switch space are also needed.")
            .next("Open Tormented Demons in Outfitter; Best in Bank checks the bow/defender switch space.")
            .source(GuideSources.WIKI+"Tormented_Demon/Strategies").build());
        // quest_cooks_assistant and quest_sote come from the quest-order generator with these legacy IDs.
    }


    private static void quests(List<GuideRule> out) {
        var prereq=questPrerequisites();
        for(var e:GuideQuestOrder.entries()) {
            String id=LEGACY_QUEST_IDS.getOrDefault(e.name(),"quest_"+slug(e.name()));
            var b=GuideRule.of(id,e.name(),Category.QUEST).focus(Focus.QUESTS).order(e.order())
                .section(questSection(e.order())).completes(e.name()).minutes(e.order()<=30?10:e.order()<=120?20:30)
                .quests(prereq.getOrDefault(e.name(),List.of()))
                .why("Step "+e.order()+" in the ironman.guide quest order"+(e.miniquest()?" (miniquest)":"")+".")
                .source(GuideSources.IRONMAN_GUIDE_QUESTS+" ; "+GuideSources.WIKI+e.name().replace(' ','_'));
            if (NO_HARD_SKILL_GATE.contains(e.name())) {
                if (!e.skills().isEmpty()) b.why("Step "+e.order()+" in the ironman.guide route. "
                    +"Its annotated "+e.skills()+" is a route waypoint, NOT a quest requirement (OSRS Wiki verified).");
            } else if(e.name().equals("Fairytale II - Cure a Queen")) {
                // Unlocking fairy rings requires a quest stage, not 40 Thieving/49 Farming/57 Herblore.
                // Full completion requires those levels; this START action must not be gated by them.
                b.unverified("Full Fairytale II completion (NOT start/ring permission): 40 Thieving, 49 Farming, 57 Herblore");
            } else e.skills().forEach(b::skill);
            if(!e.note().isBlank()) b.unverified(e.note());
            if(e.name().equals("Song of the Elves")) b.source(GuideSources.WIKI_SOTE);
            if(e.name().equals("Dragon Slayer II")) b.source(GuideSources.WIKI_DS2);
            if(e.name().equals("While Guthix Sleeps")) b.source(GuideSources.WIKI_WGS);
            if(e.name().equals("Monkey Madness II")) b.source(GuideSources.WIKI_MM2);
            if(e.name().startsWith("Desert Treasure II")) b.source(GuideSources.WIKI_DT2);
            if(e.name().equals("Recipe for Disaster - Culinaromancer")) b.why("Final Recipe for Disaster step: unlocks Barrows gloves (ironman.guide section 1.5).");
            if(e.name().equals("Song of the Elves")) b.why("Unlocks Prifddinas, the Gauntlet and crystal equipment (ironman.guide 2.0).");
            if(e.name().equals("Dragon Slayer II")) b.why("Unlocks Vorkath and rune dragons (ironman.guide 2.0).");
            out.add(b.build());
        }
    }
    /** Approximate mapping of quest order to ironman.guide main sections (route position, not exact). */
    static String questSection(int order) {
        if(order<=105) return "1.1 Early quests, Wintertodt and Ardy cloak";
        if(order<=148) return "1.3 Fairy rings, 43 prayer, kingdom, 99 thieving";
        if(order<=155) return "1.5 Diaries and finishing RFD";
        return "2.0 Goals after Barrows gloves";
    }

    // ----- The 17 Outfitter encounters (access gates from Burrow's reviewed FINAL-06 metadata) -----
    private static void bosses(List<GuideRule> out) {
        record B(String boss,String quest,String slayer,String note,int minutes){}
        List<B> list=List.of(
            new B("Scurrius",null,null,"Members; private solo encounter assumed by supply budget",20),
            new B("Brutus","The Ides of Milk",null,"Cowbell amulet must be obtained and charged",20),
            new B("Giant Mole",null,null,"Spade and light source; Falador hard diary only for shield tracking",30),
            new B("Phantom Muspah","Secrets of the North",null,"Sapphire bolts + crossbow needed for the shield phase",30),
            new B("Sarachnis",null,null,"Slash tool for the web if needed",20),
            new B("Hueycoatl",null,null,"Supply budget assumes a competent group",30),
            new B("Amoxliatl","The Heart of Darkness",null,"Post-quest farming encounter",20),
            new B("King Black Dragon",null,null,"Usual routes cross PvP Wilderness; anti-dragon shield required",20),
            new B("Grotesque Guardians","Priest in Peril","75","Gargoyle task and brittle key rooftop unlock required",30),
            new B("Zulrah",null,null,"Regicide route and boat access (partial quest progress may suffice)",30),
            new B("Barrows","Priest in Peril",null,"Morytania access",20),
            new B("Dagannoth Kings",null,null,"Waterbirth route tools or helper/shortcut",30),
            new B("Obor",null,null,"Giant key; F2P entry tier",10),
            new B("Bryophyta",null,null,"Mossy key; F2P entry tier",10),
            new B("Demonic Gorillas","Monkey Madness II",null,"Two attack styles needed; switch prayers on style change",30));
        for(B b:list) {
            var r=GuideRule.of("pvm_"+slug(b.boss),b.boss,Category.BOSS).section("Bossing").focus(Focus.PVM)
                .minutes(b.minutes).boss(b.boss)
                .why("Supported Outfitter encounter; Best in Bank checks your owned trip setup.")
                .next("Open "+b.boss+" in Outfitter and switch to BEST BANK after opening your bank.")
                .source(GuideSources.OUTFITTER_METADATA);
            if(b.quest!=null) r.quest(b.quest);
            if(b.slayer!=null) r.skill("Slayer",Integer.parseInt(b.slayer));
            if(b.boss.equals("Zulrah")) r.unverified("Regicide route (partial progress may suffice)");
            r.unverified(b.note);
            out.add(r.build());
        }
    }

    // ----- ironman.guide/afk (91 methods; levels, quests and look-away times as listed) -----
    /** id | title | skills | quest (exact RuneLite name) or ~unverified | afk m:ss | produces | members(M/F) */
    private static final String AFK = """
infected_root|Infected roots (log basket)|Woodcutting 80||30:00|Demon tears|M
stranglewood_fishing|Stranglewood fishing|Fishing 63|~Secrets of the Ancients (partial)|30:00|Bass and other fish|M
crewmate_salvaging_sort|Crewmate salvaging (sorting)|Sailing 40||30:00|Salvage|M
crewmate_salvaging_bank|Crewmate salvaging (banking)|Sailing 40||30:00|Opulent salvage|M
blast_furnace_pump|Blast Furnace pump|Strength 30||30:00||M
karambwanji|Karambwanji|Fishing 5|~Tai Bwo Wannai Trio (partial)|20:00|Raw karambwanji|M
anglerfish_sea_max|Anglerfish at sea (max AFK)|Fishing 82||18:20|Raw anglerfish|M
sharks_sea_max|Sharks at sea (max AFK)|Fishing 76||14:22|Raw sharks|M
guardian_fragments|Guardian fragments (GOTR)|Mining 1,Runecraft 27||10:00|Guardian fragments|M
poh_agility|POH agility|Agility 1,Construction 88|~High-level POH setup|10:00||M
anglerfish_sea_fast|Anglerfish at sea (faster)|Fishing 82||9:00|Raw anglerfish|M
monkfish_sea_max|Monkfish at sea (max AFK)|Fishing 62|Swan Song|7:36|Raw monkfish|M
dark_crabs|Dark crabs|Fishing 85|~Elite Wilderness diary|7:32|Raw dark crabs|M
vampyrium_rockslide|Vampyrium rockslide|Agility 78||7:30||M
sharks_sea_fast|Sharks at sea (faster)|Fishing 76||7:19|Raw sharks|M
churning|Churning (dairy churn)|Cooking 21||7:17|Cheese / butter|M
shooting_stars|Shooting stars|Mining 10||7:00|Stardust|F
civitas_fishing|Civitas illa Fortis fishing|Fishing 1||7:00|House keys|M
pedals|Pedals|Agility 30|~Uses stamina potions|5:19||M
duke_salt|Duke salt mining|Mining 72||5:00|Salts|M
cave_eels|Cave eels|Fishing 38||4:58|Raw cave eels|M
infernal_shale|Infernal shale|Mining 78||4:45|Infernal shale|M
rosewood|Rosewood trees|Woodcutting 92||4:35|Rosewood logs|M
redwood|Redwood trees|Woodcutting 90||4:24|Redwood logs|M
karambwan|Karambwan|Fishing 65|Tai Bwo Wannai Trio|4:13|Raw karambwan|M
ironwood|Ironwood trees|Woodcutting 80||4:00|Ironwood logs|M
merchant_shipwrecks|Merchant shipwrecks|Sailing 87||4:00|Salvage|M
magic_trees|Magic trees|Woodcutting 75||3:54|Magic logs|M
monkfish_sea_fast|Monkfish at sea (faster)|Fishing 62|Swan Song|3:48|Raw monkfish|M
fremennik_shipwrecks|Fremennik shipwrecks|Sailing 80||3:38|Salvage|M
mercenary_shipwrecks|Mercenary shipwrecks|Sailing 73||3:15|Salvage|M
pirate_shipwrecks|Pirate shipwrecks|Sailing 64||2:38|Salvage|M
anglerfish_land|Anglerfish on land|Fishing 82||2:37|Raw anglerfish|M
foresters_campfire|Forester's campfire|Firemaking 1||2:31||M
large_shipwrecks|Large shipwrecks|Sailing 53||2:31|Salvage|M
sunstone|Sunstone monolith|Mining 50,Sailing 62||2:30|Sunstone|M
barracuda_shipwrecks|Barracuda shipwrecks|Sailing 35||2:21|Salvage|M
fishermans_shipwrecks|Fisherman's shipwrecks|Sailing 26||2:12|Salvage|M
camphor|Camphor trees|Woodcutting 66||2:00|Camphor logs|M
mortton_temple|Building the Mort'ton temple|Crafting 20|Shades of Mort'ton|2:00|Sanctity|M
firing_clay|Firing clay|Crafting 1||1:58|Pottery|M
amethyst|Amethyst crystals|Mining 92|~Expert mining gloves|1:57|Amethyst|M
fly_fishing|Fly fishing|Fishing 1||1:57|Trout and salmon|F
barbarian_fishing|Barbarian fishing|Fishing 48||1:57|Leaping fish|M
yew_trees|Yew trees|Woodcutting 60||1:54|Yew logs|F
enchant_jewellery|Auto-cast enchant jewellery|Magic 7||1:53|Enchanted jewellery|M
bloodwood|Engorged bloodwood tree|Woodcutting 77||1:50|Bloodwood sap|M
wealthy_citizen|Auto-pickpocket wealthy citizen|Thieving 50||1:32|Coins and house keys|M
plank_make|Casting Plank Make|Magic 86|Dream Mentor|1:30|Mahogany planks|M
bolt_tips|Cutting bolt tips|Fletching 11||1:21|Gem bolt tips|M
cooking|Cooking fish or meat|Cooking 1||1:07|Cooked food|F
smithing_one_bar|Smithing one-bar items|Smithing 1||1:07|Nails / dart tips|M
cannonballs|Smithing cannonballs|Smithing 35|Dwarf Cannon|1:02|Cannonballs|M
repair_kits_low_max|Repair kits (low, max AFK)|Construction 1||1:02|Camphor repair kits|M
gilded_altar|Auto-offer at a gilded altar|Prayer 1||1:02||M
mahogany_trees|Mahogany trees|Woodcutting 50||1:00|Mahogany logs|M
maple_trees|Maple trees|Woodcutting 45||1:00|Maple logs|F
daeyalt|Daeyalt essence|Mining 60|Sins of the Father|1:00|Daeyalt essence|M
sacred_eels|Sacred eels|Fishing 87|~Regicide (partial); Rada's blessing 4|1:00|Zulrah's scales|M
infernal_eels|Infernal eels|Fishing 80|~Rada's blessing 4; fire cape|1:00|Onyx bolt tips, lava shards|M
small_shipwrecks|Small shipwrecks|Sailing 15||1:00|Salvage|M
crystal_extractor|Crystal extractor|Sailing 73||1:00|Crystal shards|M
burgling|Burgling houses|Thieving 50|~Consumes house keys|0:55|Valuables|M
zeah_blood|Zeah blood runes|Runecraft 77||0:55|Blood runes|M
zeah_soul|Zeah soul runes|Runecraft 90||0:55|Soul runes|M
glassblowing|Blowing glass|Crafting 1||0:50|Orbs|M
spinning|Spinning|Crafting 1||0:50|Bowstrings / yarn|F
foresters_rations|Forester's rations|Cooking 35||0:49|Forester's rations|M
battlestaves|Making battlestaves|Fletching 40||0:49|Battlestaves|M
string_jewellery|Casting String Jewellery|Magic 80|Dream Mentor|0:49|Strung amulets|M
redwood_shields|Redwood shields|Fletching 92||0:47|Redwood shields|M
herb_tar|Making herb tar|Herblore 11|~Swamp tar|0:47|Herb tar|M
stymphike|Stymphike|Hunter 82||0:39|Stymphike feathers|M
summer_garden|One-click summer garden|Thieving 65|~Beer glasses|0:36|Sq'irk juice|M
cleaning_herbs|Auto-cleaning herbs|Herblore 3||0:34|Clean herbs|M
stackable_secondaries|Stackable secondary potions|Herblore 77||0:31|Potions|M
repair_kits_low_fast|Repair kits (low, faster)|Construction 1|~Amy's saw / Imcando hammer|0:31|Camphor repair kits|M
zmi|ZMI (Ourania altar)|Runecraft 1||0:25|Assorted runes|M
maniacal_monkeys_hunter|Maniacal monkeys (Hunter)|Hunter 60|~Monkey Madness II (partial)|0:25|Damaged monkey tails|M
barronite|Barronite rocks|Mining 14|~Charm of Mining|0:20|Barronite shards|M
repair_kits_high|Repair kits (high)|Construction 80|~Amy's saw / Imcando hammer|0:17|Rosewood repair kits|M
splashing|Splashing|Magic 1||20:00||F
nmz_melee|NMZ melee (absorption)|Attack 60,Strength 60|~Various Nightmare Zone requirements|20:00|NMZ points|M
brutus_afk|Brutus (AFK melee)|Attack 80,Strength 80|The Ides of Milk|20:00||M
nmz_ranged|NMZ ranged (absorption)|Ranged 70|~Various Nightmare Zone requirements|20:00|NMZ points|M
vyrewatch|Vyrewatch Sentinels|Attack 70,Strength 70|Sins of the Father|17:00|Blood shards|M
monkeys_barrage|Maniacal monkeys (Ice Barrage)|Magic 94|Monkey Madness II|10:00||M
gemstone_crabs|Gemstone crabs|Attack 1|Children of the Sun|10:00|Gem drops|M
sand_crabs|Sand crabs|Attack 1||10:00||M
ammonite_crabs|Ammonite crabs|Attack 1|Bone Voyage|10:00|Unidentified fossils|M
monkeys_chinning|Maniacal monkeys (chinning)|Ranged 80|Monkey Madness II|10:00||M
""";
    private static void afk(List<GuideRule> out) {
        for(String raw:AFK.split("\n")) {
            if(raw.isBlank())continue;
            String[] f=raw.split("\\|",-1);
            String[] t=f[4].split(":");
            int seconds=Integer.parseInt(t[0])*60+Integer.parseInt(t[1]);
            var b=GuideRule.of("afk_"+f[0],f[1],Category.AFK).section("AFK methods")
                .focus(f[5].isBlank()?Focus.ANY:Focus.SUPPLIES).afk(seconds)
                .minutes(seconds>=600?30:15)
                .why("ironman.guide lists ~"+f[4]+" between inputs"+(f[5].isBlank()?"":"; produces "+f[5].toLowerCase(Locale.ROOT))+".")
                .next("Look-away time varies with gear and world; check the source before committing.")
                .source(GuideSources.IRONMAN_GUIDE_AFK);
            if(seconds>=45) b.low();
            if(!f[2].isBlank()) for(String pair:f[2].split(",")) {
                String p=pair.trim(); int sp=p.lastIndexOf(' ');
                b.skill(p.substring(0,sp),Integer.parseInt(p.substring(sp+1)));
            }
            if(!f[3].isBlank()) { if(f[3].startsWith("~")) b.unverified(f[3].substring(1)); else b.quest(f[3]); }
            if("M".equals(f[6])) b.unverified("Members world");
            out.add(b.build());
        }
    }

    // ----- ironman.guide/money-making (Ironman coin economics; GP/hr ranges are the page's estimates) -----
    /** id | title | skills | quest | unverified | L=low attention | gpLowK | gpHighK | minutes */
    private static final String MONEY = """
alchemical_hydra|Alchemical Hydra|Slayer 95||Slayer task|A|800|1200|60
rune_dragons|Rune dragons||Dragon Slayer II|Insulated boots / anti-dragon protection|A|600|1000|60
chambers_of_xeric|Chambers of Xeric (raids)||||A|500|1000|60
tombs_of_amascut|Tombs of Amascut (raids)||Beneath Cursed Sands||A|500|900|60
vorkath_gp|Vorkath for coins||Dragon Slayer II||A|500|800|60
theatre_of_blood|Theatre of Blood (raids)||||A|400|800|60
brutal_black_dragons|Brutal black dragons||||L|500|750|60
greater_nechryael|Greater nechryaels|Slayer 80||Slayer task|A|350|600|60
demonic_gorillas_gp|Demonic gorillas for coins||Monkey Madness II||A|400|600|60
dhide_crafting|D'hide body crafting and alching||||A|300|500|30
kraken|Kraken|Slayer 87||Slayer task|L|300|500|60
dust_devils|Dust devils|Slayer 65||Slayer task|A|250|400|60
shipwreck_salvaging|Shipwreck salvaging|Sailing 15||Peaks at 73 Sailing|L|22|358|30
battlestaff_crafting|Battlestaff crafting and alching|Crafting 54|||A|200|350|30
kurasks|Kurasks|Slayer 70||Slayer task; leaf-bladed weapon|L|200|350|60
skeletal_wyverns|Skeletal wyverns|Slayer 72||Slayer task; elemental/mind shield|L|200|350|60
blackjacking|Blackjacking Menaphite thugs|Thieving 45||Sophanem access|A|150|250|30
aberrant_spectres|Aberrant spectres|Slayer 60||Nose peg; Slayer task|L|150|250|60
agility_pyramid|Agility Pyramid|Agility 30|||A|150|200|30
master_farmers|Thieving master farmers|Thieving 38|||A|100|200|30
ham_storerooms|HAM storerooms (jewellery)||||A|100|150|30
stealing_valuables|Stealing valuables (Varlamore)|Thieving 50|||L|90|150|30
artefacts|Stealing artefacts (Port Piscarilius)|Thieving 49|||A|20|40|30
rogues_den|Rogues' Den sales (Martin Thwait)||||L|0|0|10
sailing_bounties|Sailing bounty tasks|Sailing 1||Best at 80+ Sailing|A|0|0|30
blast_furnace_runes|Blast Furnace rune smithing|Smithing 85|||A|0|0|30
deaths_coffer|Death's coffer (duplicate uniques)||||L|0|0|10
zaff_battlestaves|Zaff battlestaves (daily)|Crafting 54,Magic 56||Varrock diary|L|35|276|10
birdhouses|Birdhouse runs|Hunter 5||Crafting for birdhouses|L|20|60|10
herb_runs|Herb runs|Farming 32||Herb seeds|L|0|0|10
miscellania|Kingdom of Miscellania|||Royal Trouble for 75k/day withdrawals|L|0|0|10
farming_contracts|Farming contracts|Farming 45||Farming Guild access|L|30|80|10
cats_death_runes|Selling cats in West Ardougne||Gertrude's Cat|100 death runes per cat every 3h|L|0|0|10
""";
    private static void money(List<GuideRule> out) {
        for(String raw:MONEY.split("\n")) {
            if(raw.isBlank())continue;
            String[] f=raw.split("\\|",-1);
            int lo=Integer.parseInt(f[6]), hi=Integer.parseInt(f[7]);
            var b=GuideRule.of("gp_"+f[0],f[1],Category.MONEY).section("Money-making").focus(Focus.GP)
                .minutes(Integer.parseInt(f[8])).gp(lo,hi).source(GuideSources.IRONMAN_GUIDE_MONEY)
                .why(hi>0?"ironman.guide lists "+lo+"k-"+hi+"k coins/hr from alching, drops or NPC shops (estimate).":
                    "ironman.guide lists this without a comparable coins/hr figure.")
                .next("Coins come from High Alchemy, coin drops or NPC sales, not the Grand Exchange.");
            if("L".equals(f[5])) b.low();
            if(!f[2].isBlank()) for(String pair:f[2].split(",")) {
                String p=pair.trim(); int sp=p.lastIndexOf(' ');
                b.skill(p.substring(0,sp),Integer.parseInt(p.substring(sp+1)));
            }
            if(!f[3].isBlank()) b.quest(f[3]);
            if(!f[4].isBlank()) b.unverified(f[4]);
            // These have supplies utility as well as coin utility. Focus filtering in SuggestionEngine
            // keeps them available in both GP and SUPPLIES rather than moving them out of GP.
            if(f[0].equals("miscellania")) b.completes(null).quest("Throne of Miscellania");
            if(f[0].equals("deaths_coffer") || f[0].equals("miscellania") || f[0].equals("herb_runs")) b.mode(ModeScope.IRONMAN);
            out.add(b.build());
        }
    }

    // ----- Gear goals from the Wiki Ironman guide overview (oldid 15382330); skipped when observed in bank -----
    private static void gear(List<GuideRule> out) {
        record G(String id,String title,int item,String stage,String skills,String quest,String note){}
        List<G> list=List.of(
            new G("barrows_gloves","Barrows gloves",7462,"early",null,"Recipe for Disaster - Culinaromancer",null),
            new G("fire_cape","Fire cape",6570,"early",null,null,"Defeat TzTok-Jad in the Fight Caves"),
            new G("fighter_torso","Fighter torso",10551,"early",null,null,"Barbarian Assault honour"),
            new G("dragon_defender","Dragon defender",12954,"early","Defence 60",null,"Warriors' Guild: Attack+Strength 130 or a 99"),
            new G("berserker_ring_i","Berserker ring (i)",11773,"early",null,null,"Dagannoth Rex drop, then imbue"),
            new G("rune_crossbow","Rune crossbow",9185,"early","Ranged 61",null,null),
            new G("black_dhide","Black d'hide body",2503,"early","Ranged 70,Defence 40",null,null),
            new G("ibans_staff","Iban's staff",1409,"early","Magic 50","Underground Pass",null),
            new G("imbued_god_cape","Imbued god cape",21791,"early",null,"Mage Arena II","Any of the three imbued god capes"),
            new G("zombie_axe","Zombie axe",28810,"early","Attack 65",null,null),
            new G("slayer_helmet_i","Slayer helmet (i)",11865,"early",null,null,"400 Slayer points, 55 Crafting, imbue"),
            new G("abyssal_whip","Abyssal whip",4151,"mid","Attack 70,Slayer 85",null,null),
            new G("trident","Trident of the seas",11907,"mid","Magic 75,Slayer 87",null,null),
            new G("dragon_warhammer","Dragon warhammer",13576,"mid","Strength 60",null,null),
            new G("dragon_boots","Dragon boots",11840,"mid","Defence 60,Slayer 83",null,null),
            new G("amulet_of_fury","Amulet of fury",6585,"mid","Crafting 90",null,"Wiki guide: Crafting 90 to make it"),
            new G("bowfa","Bow of Faerdhinen",25865,"mid","Ranged 80,Agility 70","Song of the Elves","Corrupted Gauntlet drop"),
            new G("toxic_blowpipe","Toxic blowpipe",12926,"late","Ranged 75",null,"Zulrah drop"),
            new G("occult_necklace","Occult necklace",12002,"late","Magic 70,Slayer 93",null,null),
            new G("primordial_boots","Primordial boots",13239,"late","Strength 75,Defence 75",null,null),
            new G("infernal_cape","Infernal cape",21295,"late",null,null,"Complete the Inferno"),
            new G("dragon_hunter_lance","Dragon hunter lance",22978,"late","Attack 78,Slayer 95",null,null));
        for(G g:list) {
            var b=GuideRule.of("gear_"+g.id,"Obtain "+g.title,Category.GEAR).section("Gear progression ("+g.stage+")")
                .focus(Focus.PVM).minutes(60).skipIfOwned(g.item).optional()
                .why("Listed as "+g.stage+"-game Ironman gear in the OSRS Wiki Ironman guide.")
                .next("Burrow hides this once it is positively observed in your bank, inventory or worn gear.")
                .source(GuideSources.WIKI_IRONMAN_GUIDE+" ; "+GuideSources.WIKI+g.title.replace(' ','_'));
            if(g.skills!=null) for(String pair:g.skills.split(",")) {
                String p=pair.trim(); int sp=p.lastIndexOf(' ');
                b.skill(p.substring(0,sp),Integer.parseInt(p.substring(sp+1)));
            }
            if(g.quest!=null) b.quest(g.quest);
            if(g.note!=null) b.unverified(g.note);
            b.unverified("Wear/acquisition levels authored from Wiki item and monster pages; verify before grinding");
            out.add(b.build());
        }
    }

    // ----- Route milestones (ironman.guide section names + Wiki Ironman guide targets) -----
    private static void milestones(List<GuideRule> out) {
        out.add(GuideRule.of("skill_magic_55_alch","Reach 55 Magic for High Level Alchemy",Category.SKILL)
            .section("1.1 Early quests, Wintertodt and Ardy cloak").focus(Focus.GP).minutes(30).doneAt("Magic",55)
            .why("High Alchemy is the Ironman coin staple (Wiki Ironman guide).").next("Train Magic to 55.")
            .source(GuideSources.WIKI_IRONMAN_GUIDE).build());
        out.add(GuideRule.of("skill_wintertodt","Wintertodt for early supplies",Category.SKILL)
            .section("1.1 Early quests, Wintertodt and Ardy cloak").focus(Focus.SUPPLIES).minutes(30).skill("Firemaking",50)
            .doneAt("Firemaking",85).optional()
            .why("Part of ironman.guide section 1.1; supply crates and Firemaking XP.").next("Bring warm clothing and food.")
            .source(GuideSources.IRONMAN_GUIDE).build());
        out.add(GuideRule.of("skill_woodcutting_guild","Reach 60 Woodcutting for the Woodcutting Guild",Category.SKILL)
            .section("1.1 Early quests, Wintertodt and Ardy cloak").focus(Focus.SUPPLIES).low().minutes(30).doneAt("Woodcutting",60)
            .why("Wiki Ironman guide: Woodcutting 60 for guild access (rune axe route).").next("Chop willows/maples toward 60.")
            .source(GuideSources.WIKI_IRONMAN_GUIDE).build());
        out.add(GuideRule.of("skill_barbarian_fishing_74","Barbarian Fishing to 74",Category.SKILL)
            .section("1.2 Thieving, fishing and mining").focus(Focus.SUPPLIES).low().minutes(30).skill("Fishing",48).doneAt("Fishing",74)
            .why("Wiki Ironman guide stop point; also trains Agility and Strength.").next("Use the Otto's Grotto spot.")
            .source(GuideSources.WIKI_IRONMAN_GUIDE).build());
        out.add(GuideRule.of("skill_prayer_43","Reach 43 Prayer",Category.SKILL)
            .section("1.3 Fairy rings, 43 prayer, kingdom, 99 thieving").focus(Focus.ANY).minutes(30).doneAt("Prayer",43)
            .why("Protection prayers; named in ironman.guide section 1.3.").next("Train Prayer to 43.")
            .source(GuideSources.IRONMAN_GUIDE).build());
        out.add(GuideRule.of("unlock_fairy_rings","Unlock fairy rings",Category.SKILL)
            .section("1.3 Fairy rings, 43 prayer, kingdom, 99 thieving").focus(Focus.QUESTS).minutes(30)
            .quest("Lost City").quest("Fairytale I - Growing Pains").completes("Fairytale II - Cure a Queen")
            .unverified("Fairy Godfather permission is an intermediate quest stage; IN_PROGRESS does not prove it")
            .why("Fairy rings become usable after the Fairy Godfather grants permission during Fairytale II, not merely on quest start.")
            .next("Begin Fairytale II, speak to Fairy Godfather and Co-ordinator; verify permission before calling rings unlocked.")
            .source(GuideSources.WIKI+"Fairy_ring").build());
        out.add(GuideRule.of("unlock_kingdom","Set up the Kingdom of Miscellania",Category.SKILL)
            .section("1.3 Fairy rings, 43 prayer, kingdom, 99 thieving").focus(Focus.SUPPLIES).low().minutes(10)
            .completes("Royal Trouble").quest("Throne of Miscellania").mode(ModeScope.IRONMAN)
            .why("Passive herbs/hardwood; Royal Trouble raises the daily coffer withdrawal to 75k (Wiki).")
            .next("Keep the coffer funded daily.").source(GuideSources.WIKI_IRONMAN_GUIDE).build());
        out.add(GuideRule.of("skill_thieving_99","Train Thieving toward 99",Category.SKILL)
            .section("1.3 Fairy rings, 43 prayer, kingdom, 99 thieving").focus(Focus.GP).minutes(60).doneAt("Thieving",99).optional()
            .why("ironman.guide section 1.3 milestone; coins and seeds along the way.").next("Blackjack at 45, then valuables at 50.")
            .source(GuideSources.IRONMAN_GUIDE).build());
        out.add(GuideRule.of("skill_agility_graceful","Agility to 60 for Graceful",Category.SKILL)
            .section("1.4 Various skilling, agility for graceful").focus(Focus.ANY).minutes(60).doneAt("Agility",60).skipIfOwned(11850)
            .why("Graceful needs ~260 marks of grace (~60+ Agility per Wiki Ironman guide).").next("Run rooftop courses for marks.")
            .source(GuideSources.WIKI_IRONMAN_GUIDE).build());
        out.add(GuideRule.of("unlock_ardougne_cloak","Ardougne cloak 1 (easy diary)",Category.SKILL)
            .section("1.1 Early quests, Wintertodt and Ardy cloak").focus(Focus.ANY).minutes(30).skipIfOwned(13121)
            .unverified("Easy Ardougne diary completion (not observable)")
            .why("Early teleport named in ironman.guide section 1.1.").next("Complete the easy Ardougne diary.")
            .source(GuideSources.IRONMAN_GUIDE).build());
        out.add(GuideRule.of("skill_herblore_81_brews","Herblore 81 for Saradomin brews",Category.SKILL)
            .section("Gear progression (mid)").focus(Focus.SUPPLIES).minutes(60).doneAt("Herblore",81).optional()
            .why("Mid-game supply milestone in the Wiki Ironman guide.").next("Herb runs and Miscellania herbs feed this.")
            .source(GuideSources.WIKI_IRONMAN_GUIDE).build());
    }
}
