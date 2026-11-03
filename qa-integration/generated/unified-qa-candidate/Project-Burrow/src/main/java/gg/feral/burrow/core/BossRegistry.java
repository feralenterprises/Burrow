package gg.feral.burrow.core;

import java.util.*;

/**
 * Build 20 Outfitter boss scope: every boss in the owner's machine-readable
 * BOSS-SCOPE-MANIFEST.json (R01), grouped into Regular / Wilderness / Moons of Peril / Demi-bosses,
 * plus the hard exclusion list. Access facts were read from each boss's OSRS Wiki page on
 * 2026-11-01 (see {@link Entry#wikiUrl}); where Burrow has no verified loadout the entry is
 * selectable for discovery only and shows "Guide not yet verified".
 *
 * <p>Wilderness rule (owner-locked): Best in Bank is DENIED for any WILDERNESS or WILDERNESS_ACCESS
 * entry and for any name not in this registry (default deny).
 */
public final class BossRegistry {
    private BossRegistry() {}
    public enum Group {
        REGULAR("Regular bosses"), WILDERNESS("Wilderness"), MOONS("Moons of Peril"), DEMI("Demi-bosses");
        private final String label; Group(String l){label=l;} public String label(){return label;}
    }
    public enum Risk { NORMAL, WILDERNESS, WILDERNESS_ACCESS }
    public enum Evidence {
        /** Original FINAL-06 Wiki/curated tier profiles exist (Build 19 catalog). */
        CATALOG_PROFILES,
        /** Draft profiles authored from a Wiki strategy table; not an official Wiki tier layout. */
        DRAFT_PROFILES,
        /** Selectable for discovery; no verified gear/inventory: "Guide not yet verified". */
        GUIDE_NOT_YET_VERIFIED
    }
    public record Entry(String id, String manifestName, List<String> aliases, Group group, Risk risk,
                        boolean existing, String wikiUrl, String strategyUrl, List<String> quests,
                        Map<String,Integer> skills, List<String> accessNotes, String bossAttacks,
                        List<CombatStyle> suggestedStyles, String mechanic) {
        public Entry {
            aliases=List.copyOf(aliases); quests=List.copyOf(quests); skills=Map.copyOf(skills);
            accessNotes=List.copyOf(accessNotes); suggestedStyles=List.copyOf(suggestedStyles);
        }
        public boolean wilderness() { return risk != Risk.NORMAL; }
        public boolean bestBankAllowed() { return risk == Risk.NORMAL; }
    }
    /** Hard exclusions (owner screenshots + HARD-EXCLUSION-MATRIX.csv). Never selectable in Outfitter. */
    public static final List<String> EXCLUDED = List.of(
        "Crystalline Hunllef","Corrupted Hunllef","TzTok-Jad","TzKal-Zuk","Sol Heredit",
        "Tempoross","Wintertodt","Zalcano",
        "Chambers of Xeric","Tekton","Vanguard","Vespula","Vasa Nistirio","Muttadile","Great Olm",
        "Theatre of Blood","The Maiden of Sugadinti","Pestilent Bloat","Nylocas Vasilias","Sotetseg","Xarpus","Verzik Vitur",
        "Tombs of Amascut","Akkha","Ba-Ba","Kephri","Zebak","Tumeken's Warden","Elidinis' Warden",
        "The Fractured Archive","Archive Guardian","Ezekiel","Bork","The Smorks","Moia","Dreadnoth the Jellied","Xarth",
        "Dagon'hai Elder","Dagon'hai Monk","Ocellus","The Blade",
        "Revenant maledictus","Galvek","Elvarg","Glough","Lowerniel Drakan (quest-only)","Wrathmaw",
        // Owner decision 2026-11-01: no gear needed, so not an Outfitter boss (still a Pathfinder AFK activity).
        "Gemstone Crab");

    private static final String W="https://oldschool.runescape.wiki/w/";
    private static final CombatStyle ME=CombatStyle.MELEE, RA=CombatStyle.RANGED, MA=CombatStyle.MAGIC;
    private static final List<Entry> ALL = build();
    private static final Map<String,Entry> BY_KEY = index();

    public static List<Entry> all() { return ALL; }
    public static List<String> selectableIds() { return ALL.stream().map(Entry::id).toList(); }
    public static List<Entry> group(Group g) { return ALL.stream().filter(e->e.group()==g).toList(); }
    /** Lookup by id, manifest name or alias (case-insensitive). */
    public static Optional<Entry> find(String nameOrAlias) {
        if(nameOrAlias==null)return Optional.empty();
        return Optional.ofNullable(BY_KEY.get(nameOrAlias.toLowerCase(Locale.ROOT)));
    }
    /** Default DENY: unknown names and every Wilderness-risk entry are never bank-optimized. */
    public static boolean bestBankAllowed(String boss) { return find(boss).map(Entry::bestBankAllowed).orElse(false); }
    public static boolean wilderness(String boss) { return find(boss).map(Entry::wilderness).orElse(false); }
    public static boolean excluded(String name) {
        if(name==null)return false;
        return EXCLUDED.stream().anyMatch(x->x.equalsIgnoreCase(name.trim()));
    }
    public static Evidence evidence(String boss) {
        if(!TieredOutfitterCatalog.methods(boss).isEmpty()) {
            if(Build20EncounterLoadouts.bosses().contains(boss)) return Evidence.DRAFT_PROFILES;
            return Evidence.CATALOG_PROFILES;
        }
        return Evidence.GUIDE_NOT_YET_VERIFIED;
    }

    private static Map<String,Entry> index() {
        Map<String,Entry> m=new HashMap<>();
        for(Entry e:ALL) {
            for(String k:keys(e)) if(m.put(k.toLowerCase(Locale.ROOT),e)!=null && !m.get(k.toLowerCase(Locale.ROOT)).equals(e))
                throw new IllegalStateException("alias collision "+k);
        }
        return Collections.unmodifiableMap(m);
    }
    private static List<String> keys(Entry e) {
        List<String> k=new ArrayList<>(e.aliases()); k.add(e.id()); k.add(e.manifestName()); return k;
    }

    private static final class B {
        final String id; String manifest; final Group group; Risk risk=Risk.NORMAL; boolean existing;
        final List<String> aliases=new ArrayList<>(), quests=new ArrayList<>(), notes=new ArrayList<>();
        final Map<String,Integer> skills=new LinkedHashMap<>(); String attacks="Not ingested"; String mechanic="";
        final List<CombatStyle> styles=new ArrayList<>(); String strategy;
        B(String id,Group g){this.id=id;this.manifest=id;this.group=g;}
        B manifest(String m){manifest=m;return this;}
        B existing(){existing=true;return this;}
        B risk(Risk r){risk=r;return this;}
        B alias(String... a){aliases.addAll(List.of(a));return this;}
        B quest(String q){quests.add(q);return this;}
        B skill(String s,int l){skills.put(s,l);return this;}
        B note(String n){notes.add(n);return this;}
        B attacks(String a){attacks=a;return this;}
        B styles(CombatStyle... s){styles.addAll(List.of(s));return this;}
        B mech(String m){mechanic=m;return this;}
        B strategy(String s){strategy=s;return this;}
        Entry build(){
            String page=W+manifest.replace(' ','_');
            return new Entry(id,manifest,aliases,group,risk,existing,page,strategy==null?page+"/Strategies":strategy,
                quests,skills,notes,attacks,styles,mechanic);
        }
    }
    private static List<Entry> build() {
        List<B> b=new ArrayList<>();
        Group R=Group.REGULAR;
        // ---- Existing Build 19 encounters (catalog profiles preserved) ----
        b.add(new B("Scurrius",R).existing().note("Members; private instance assumed by supply budget"));
        b.add(new B("Brutus",R).existing().quest("The Ides of Milk").note("Cowbell amulet obtained and charged"));
        b.add(new B("Giant Mole",R).existing().note("Spade and light source"));
        b.add(new B("Sarachnis",R).existing());
        b.add(new B("Amoxliatl",R).existing().quest("The Heart of Darkness"));
        b.add(new B("Hueycoatl",R).existing().manifest("The Hueycoatl").alias("The Hueycoatl"));
        b.add(new B("Grotesque Guardians",R).existing().quest("Priest in Peril").skill("Slayer",75).note("Gargoyle task and brittle key rooftop unlock"));
        b.add(new B("Barrows",R).existing().alias("Barrows Brothers","Barrows Chests","Barrows chest","Ahrim the Blighted","Dharok the Wretched","Guthan the Infested",
            "Karil the Tainted","Torag the Corrupted","Verac the Defiled").quest("Priest in Peril").note("Grouped: six brothers, one Outfitter entry"));
        b.add(new B("Dagannoth Kings",R).existing().alias("Dagannoth Rex","Dagannoth Prime","Dagannoth Supreme")
            .note("Grouped: Rex / Prime / Supreme are methods of one entry; Waterbirth route tools"));
        b.add(new B("Zulrah",R).existing().note("Regicide route (partial quest progress may suffice)"));
        b.add(new B("Obor",R).existing().note("Giant key"));
        b.add(new B("Bryophyta",R).existing().note("Mossy key"));
        b.add(new B("Vorkath",R).existing().quest("Dragon Slayer II"));
        b.add(new B("Phantom Muspah",R).existing().quest("Secrets of the North"));
        // ---- New regular bosses (Wiki access facts; guide not yet verified) ----
        b.add(new B("Deranged archaeologist",R).note("Fossil Island access; axe for the swamp vine").attacks("Ranged, crush, book special").styles(MA,RA)
            .mech("'Learn to read!' books land where you stood: keep moving"));
        b.add(new B("Kalphite Queen",R).note("Two ropes (dungeon + lower level)").attacks("Stab, ranged, magic").styles(ME)
            .mech("Her ranged and magic attacks always hit; protection prayers only reduce damage"));
        b.add(new B("Corporeal Beast",R).note("Lair is not Wilderness, but the walk-in entrance is: use games necklace teleport")
            .attacks("Magic, crush, stomp").styles(ME).mech("Non-Corpbane weapons deal 50% damage with stab"));
        b.add(new B("General Graardor",R).alias("Bandos").skill("Strength",70).note("Hammer; 40 Bandos follower KC or ecumenical key")
            .attacks("Crush, ranged slam").styles(ME).mech("40% earth weakness since June 2025"));
        b.add(new B("Kree'arra",R).alias("Armadyl").skill("Ranged",70).note("Crossbow + mithril grapple; 40 Armadyl follower KC")
            .attacks("Ranged, melee").styles(RA,MA).mech("Melee only hits with halberds or salamanders"));
        b.add(new B("Commander Zilyana",R).alias("Saradomin").skill("Agility",70).note("40 Saradomin follower KC (unboostable Agility)")
            .attacks("Crush, magic").styles(RA).mech("Health resets if all players leave"));
        b.add(new B("K'ril Tsutsaroth",R).alias("Zamorak").skill("Hitpoints",70).note("40 Zamorakian KC or ecumenical key")
            .attacks("Slash, magic").styles(ME).mech("Special hits through Protect from Melee and drains prayer"));
        b.add(new B("Nex",R).quest("The Frozen Door").skill("Hitpoints",70).skill("Ranged",70).skill("Strength",70).skill("Agility",70)
            .note("Group content: no universal solo trip readiness").attacks("Magic, ranged, crush").styles(RA)
            .mech("Empowering mage must die at each 20% phase"));
        b.add(new B("Royal Titans",R).alias("Branda the Fire Queen","Eldric the Ice King").note("Linked duo encounter (solo possible)")
            .attacks("Melee, elemental magic").styles(ME,RA,MA).mech("If one titan dies first the survivor's melee pierces prayer"));
        b.add(new B("The Nightmare",R).alias("Nightmare").note("Group encounter (5-80 players); Morytania access")
            .attacks("Slash, ranged, magic").styles(ME,MA).mech("Melee always hits; wrong prayer raises her max hit 20%"));
        b.add(new B("Phosani's Nightmare",R).note("Solo only; Pool of Nightmares; 60,000 gp to retrieve items after death")
            .attacks("Slash, ranged, magic").styles(ME,MA).mech("Correct prayers fully block standard attacks"));
        b.add(new B("Yama",R).quest("A Kingdom Divided").attacks("Slash, ranged, magic").styles(ME,MA)
            .mech("A Judge of Yama must be defeated at 66.6% and 33.3%"));
        b.add(new B("Doom of Mokhaiotl",R).quest("The Final Dawn").attacks("Melee, magic, ranged").styles(RA,MA)
            .mech("Each delve raises difficulty; dying resets to delve 1 and forfeits the run's drops"));
        b.add(new B("Maggot King",R).quest("The Blood Moon Rises").note("Solo only").attacks("Ranged, magic, melee").styles(ME,MA,RA)
            .mech("Melee cannot damage it until about 65% health"));
        b.add(new B("Mad Angel",R).quest("Fallen From Grace").note("Repeatable post-quest version only, not the quest fight")
            .attacks("Melee, magic").styles(ME,MA).mech("Golembane melee or earth spells (15% weakness)"));
        b.add(new B("The Mimic",R).alias("Mimic").note("Needs a mimic casket from an elite or master clue; deathbank instance")
            .attacks("Crush, magic").styles(ME,MA).mech("Candy throws and stomp; repeatable per casket"));
        b.add(new B("Hespori",R).skill("Farming",65).note("Hespori seed, spade, rake, seed dibber; not the raid variant")
            .attacks("Ranged, magic, vine entangle").styles(ME,MA,RA).mech("Kill each flower bud in one hit; 100% fire weakness"));
        b.add(new B("Skotizo",R).note("Dark totem consumed per attempt").attacks("Stab, magic").styles(ME)
            .mech("40% water weakness; ground loot lost on exit"));
        b.add(new B("The Leviathan",R).alias("Leviathan").quest("Desert Treasure II - The Fallen Empire").attacks("Melee, ranged, magic").styles(RA)
            .mech("Awakened variant uses an Awakener's orb"));
        b.add(new B("The Whisperer",R).alias("Whisperer").quest("Desert Treasure II - The Fallen Empire").note("Blackstone fragment")
            .attacks("Ranged, magic, melee").styles(MA).mech("Sanity drains in the Shadow Realm"));
        b.add(new B("Vardorvis",R).quest("Desert Treasure II - The Fallen Empire").attacks("Slash and specials").styles(ME)
            .mech("Head Gaze disables overheads unless blocked with Protect from Missiles"));
        b.add(new B("Duke Sucellus",R).quest("Desert Treasure II - The Fallen Empire").attacks("Magic, crush, gaze").styles(ME)
            .mech("Hide behind a pillar at each gaze"));
        b.add(new B("Abyssal Sire",R).skill("Slayer",85).note("Abyssal demon or Sire task; Abyss visited").attacks("Melee, fumes, spawns").styles(ME)
            .mech("Shadow spells or 75 damage disorient the tentacles"));
        b.add(new B("Kraken",R).skill("Slayer",87).note("Kraken / cave kraken task").attacks("Typeless magic").styles(MA)
            .mech("Protect from Magic gives no reduction"));
        b.add(new B("Cerberus",R).skill("Slayer",91).note("Hellhound or Cerberus task").attacks("Magic, ranged, stab").styles(ME)
            .mech("Restores all health if nobody is in her lair"));
        b.add(new B("Araxxor",R).skill("Slayer",92).note("Spider/araxyte or boss task").attacks("Crush, magic, ranged").styles(ME,RA)
            .mech("Enrages at 25%: +35 Defence, faster attacks"));
        b.add(new B("Thermonuclear smoke devil",R).skill("Slayer",93).note("Smoke devil task only").attacks("Magical ranged, max hit 8")
            .mech("A dwarf multicannon in the boss room is destroyed"));
        b.add(new B("Alchemical Hydra",R).skill("Slayer",95).note("Hydra task; boots of stone/brimstone/granite unless elite Kourend & Kebos diary")
            .attacks("Ranged, magic").styles(RA).mech("Lure over the correct vent to remove 75% damage reduction"));
        b.add(new B("Shellbane gryphon",R).quest("Troubled Tortugans").skill("Slayer",51).note("Gryphons task; tortugan shield")
            .attacks("Melee, ranged, knockback, spit").styles(ME).mech("Keep equipped weight 40 kg+ against knockback"));
        // ---- Wilderness (no Best in Bank, ever) ----
        Group WG=Group.WILDERNESS;
        b.add(new B("King Black Dragon",WG).existing().risk(Risk.WILDERNESS_ACCESS)
            .note("Lair is outside the Wilderness, but the usual approach crosses PvP Wilderness"));
        for(String w:List.of("Chaos Fanatic","Crazy archaeologist","Scorpia","Chaos Elemental","Vet'ion","Calvar'ion",
                "Venenatis","Spindel","Callisto","Artio"))
            b.add(new B(w,WG).risk(Risk.WILDERNESS).note("Wilderness boss: PvP area; level, route and loadout not verified in Build 20"));
        // ---- Moons of Peril (owner exception: three individually targetable fights) ----
        Group MO=Group.MOONS; String moons=W+"Moons_of_Peril/Strategies";
        b.add(new B("Eclipse Moon",MO).quest("Perilous Moons").strategy(moons).attacks("Three-hit series; clones; shield").styles(ME)
            .mech("Stab weakness. Protection prayers do nothing; parry clones by facing them"));
        b.add(new B("Blood Moon",MO).quest("Perilous Moons").strategy(moons).attacks("Three-hit series; blood rain; jaguars").styles(ME)
            .mech("Slash weakness. Heals when it damages you; Defence reduces its healing"));
        b.add(new B("Blue Moon",MO).quest("Perilous Moons").strategy(moons).attacks("Three-hit series; weapon freeze; frost storm").styles(ME)
            .mech("Crush weakness. Break the glowing ice block; relight braziers"));
        // ---- Demi-bosses (permanent expandable product category) ----
        Group D=Group.DEMI;
        b.add(new B("Tormented Demons",D).existing().quest("While Guthix Sleeps"));
        b.add(new B("Demonic Gorillas",D).existing().quest("Monkey Madness II"));
        b.add(new B("Lizardman Shamans",D).manifest("Lizardman Shamans").alias("Lizardman shaman")
            .strategy(W+"Lizardman_shaman/Strategies")
            .note("Canyon: no task needed; Lizardman Caves: lizardman Slayer task only (a 'lizard' task is different)")
            .note("Slayer helmet gets Shayzien protection only after hard Kourend & Kebos diary + Captain Cleive")
            .attacks("Crush, ranged, acid spit, jump, spawns").styles(RA,ME)
            .mech("Full Shayzien (5) negates acid spit damage; poison can still apply"));
        List<Entry> out=new ArrayList<>();
        for(B x:b) out.add(x.build());
        // Fix the manifest page for the shaman entry (Wiki page is singular).
        out.replaceAll(e->e.id().equals("Lizardman Shamans")?new Entry(e.id(),e.manifestName(),e.aliases(),e.group(),e.risk(),
            e.existing(),W+"Lizardman_shaman",e.strategyUrl(),e.quests(),e.skills(),e.accessNotes(),e.bossAttacks(),
            e.suggestedStyles(),e.mechanic()):e);
        for(Entry e:out) if(excluded(e.id()) || excluded(e.manifestName())) throw new IllegalStateException("excluded boss registered: "+e.id());
        return List.copyOf(out);
    }
}
