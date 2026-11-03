package gg.feral.burrow.core;

import java.util.*;

/**
 * Build 20 Rare Drop Tracker data. Every rate below was copied exactly as displayed in the OSRS Wiki
 * "Drops" section of the linked page (read 2026-10-31, America/Los_Angeles). Item IDs were checked
 * against RuneLite 1.13.4 {@code net.runelite.api.ItemID}. Nothing here is estimated.
 *
 * <p>Loot mechanics: drops that share a roll group are mutually exclusive on one roll (a kill can
 * give at most one of them per roll); {@code rolls} is how many times that group is rolled per kill
 * ("2 ×" on the Wiki). Different groups, and every tertiary, are independent. Items whose Wiki rate
 * is governed by a mechanic this model cannot represent honestly (guaranteed at a kill count,
 * fewest-owned rules, chest/key loot) are listed in {@link Encounter#notModelled} and are never
 * offered for probability maths.
 */
public final class RareDropTable {
    private RareDropTable() {}
    public static final String READ_DATE = "2026-10-31/11-01";

    /** One verified drop. {@code numerator/denominator} is the per-roll chance as shown on the Wiki. */
    public record Drop(String key, int itemId, String name, double numerator, double denominator,
                       String group, int rolls, String condition) {
        public Drop {
            Objects.requireNonNull(key); Objects.requireNonNull(name); Objects.requireNonNull(group);
            if(!(numerator>0) || !(denominator>=numerator)) throw new IllegalArgumentException("rate "+name);
            if(rolls<1) throw new IllegalArgumentException("rolls "+name);
        }
        public double perRoll() { return numerator/denominator; }
        /** Wiki display text, e.g. "2 × 1/1,024". */
        public String rateText() {
            String n=numerator==Math.rint(numerator)?String.valueOf((long)numerator):String.valueOf(numerator);
            String d=denominator==Math.rint(denominator)?String.format(Locale.ROOT,"%,d",(long)denominator)
                :String.format(Locale.ROOT,"%,.2f",denominator).replaceAll("0+$","").replaceAll("\\.$","");
            return (rolls>1?rolls+" × ":"")+n+"/"+d;
        }
    }
    /** A kill-count unit: a boss, or one Dagannoth King. */
    /**
     * @param setTableChance for a duplicate-protected set (Moons of Peril): the per-kill chance of
     *        the set's unique roll (Wiki: 1/56); 0 when the encounter has no such rule.
     * @param chestUnit what one KC unit is for this encounter's rates (e.g. "Barrows chest").
     */
    public record Encounter(String boss, String encounter, String wikiUrl, List<Drop> drops,
                            List<String> notModelled, List<String> gaps, String rateNote,
                            double setTableChance, String chestUnit) {
        public Encounter { drops=List.copyOf(drops); notModelled=List.copyOf(notModelled); gaps=List.copyOf(gaps); }
        public Optional<Drop> drop(String key){return drops.stream().filter(d->d.key().equals(key)).findFirst();}
    }
    public enum Coverage { VERIFIED, PARTIAL, MECHANIC_NOT_MODELLED, NOT_YET_RESEARCHED }

    private static final Map<String,Encounter> BY_ENCOUNTER = new LinkedHashMap<>();
    private static final Map<String,List<Encounter>> BY_BOSS = new LinkedHashMap<>();
    private static final Map<String,String> BOSS_NOTES = new LinkedHashMap<>();
    private static final String W="https://oldschool.runescape.wiki/w/";

    private static final class B {
        final String boss, enc, url; final List<Drop> d=new ArrayList<>(); final List<String> nm=new ArrayList<>(), gaps=new ArrayList<>();
        String note=""; double setTable=0; String unit=null;
        B(String boss,String enc,String page){this.boss=boss;this.enc=enc;this.url=W+page;}
        /** Exclusive roll group member. */
        B u(String group,int rolls,int id,String name,double num,double den){return add(group,rolls,id,name,num,den,null);}
        B u(String group,int rolls,int id,String name,double num,double den,String cond){return add(group,rolls,id,name,num,den,cond);}
        /** Independent tertiary (own group, one roll). */
        B t(int id,String name,double num,double den){return add("t:"+id,1,id,name,num,den,null);}
        B t(int id,String name,double num,double den,String cond){return add("t:"+id,1,id,name,num,den,cond);}
        B add(String group,int rolls,int id,String name,double num,double den,String cond){
            d.add(new Drop(enc.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","_")+":"+id,id,name,num,den,group,rolls,cond));return this;}
        B notModelled(String s){nm.add(s);return this;}
        B gap(String s){gaps.add(s);return this;}
        B note(String s){note=s;return this;}
        B setTable(double p){setTable=p;return this;}
        B unit(String u){unit=u;return this;}
        void done(){
            Encounter e=new Encounter(boss,enc,url,d,nm,gaps,note,setTable,unit);
            if(BY_ENCOUNTER.putIfAbsent(enc,e)!=null) throw new IllegalStateException("duplicate "+enc);
            BY_BOSS.computeIfAbsent(boss,k->new ArrayList<>()).add(e);
        }
    }
    private static B b(String boss,String page){return new B(boss,boss,page);}

    static {
        b("Vorkath","Vorkath").note("Tertiary items have one roll per kill.")
            .t(22111,"Dragonbone necklace",1,1000).t(22106,"Jar of decay",1,3000).t(21992,"Vorki",1,3000)
            .t(11286,"Draconic visage",1,5000).t(22006,"Skeletal visage",1,5000)
            .notModelled("Vorkath's head 1/50: always received on the 50th kill").done();
        b("Zulrah","Zulrah").note("Two rolls per kill on the unique table (Wiki: about 1/512 per kill for a specific item).")
            .u("unique",2,12922,"Tanzanite fang",1,1024).u("unique",2,12932,"Magic fang",1,1024)
            .u("unique",2,12927,"Serpentine visage",1,1024).u("unique",2,6571,"Uncut onyx",1,1024)
            .t(12936,"Jar of swamp",1,3000).t(12921,"Pet snakeling",1,4000).done();
        b("Scurrius","Scurrius").note("MVP/solo table: rolled only by the player who dealt the most damage.")
            .t(28798,"Scurrius' spine",1,33,"Only one player may receive a spine per kill")
            .t(10977,"Curved bone",1,5012.5,"MVP/solo only").t(28801,"Scurry",1,3000,"MVP/solo only").done();
        b("Giant Mole","Giant_Mole")
            .t(12646,"Baby mole",1,3000).t(10977,"Curved bone",1,5012.5).done();
        b("Sarachnis","Sarachnis")
            .u("pre-roll",1,23528,"Sarachnis cudgel",1,384)
            .t(23525,"Jar of eyes",1,2000).t(23495,"Sraracha",1,3000)
            .notModelled("Tattered pages 1/5: you receive the page you own fewest of").done();
        b("Phantom Muspah","Phantom_Muspah").note("If a Venator shard is received no other drop is given.")
            .u("unique",1,27614,"Venator shard",1,100).u("unique",1,27627,"Ancient icon",2,100)
            .gap("Pet (Muphin) rate not present in the Drops text read on "+READ_DATE+": not offered").done();
        b("Grotesque Guardians","Grotesque_Guardians").note("Two rolls per kill including uniques; tertiaries roll once. Slayer task only.")
            .u("main",2,4153,"Granite maul",1,250).u("main",2,21736,"Granite gloves",1,500)
            .u("main",2,21739,"Granite ring",1,500).u("main",2,21742,"Granite hammer",1,750)
            .u("main",2,21730,"Black tourmaline core",1,1000)
            .t(21748,"Noon",1,3000).t(21745,"Jar of stone",1,5000).done();
        new B("Dagannoth Kings","Dagannoth Rex","Dagannoth_Rex")
            .u("main",1,6737,"Berserker ring",1,128).u("main",1,6735,"Warrior ring",1,128)
            .t(12645,"Pet dagannoth rex",1,5000).done();
        new B("Dagannoth Kings","Dagannoth Prime","Dagannoth_Prime")
            .u("main",1,6731,"Seers ring",1,128).u("main",1,6562,"Mud battlestaff",1,128)
            .t(12644,"Pet dagannoth prime",1,5000).done();
        new B("Dagannoth Kings","Dagannoth Supreme","Dagannoth_Supreme")
            .u("main",1,6733,"Archers ring",1,128).u("main",1,6724,"Seercull",1,128)
            .t(12643,"Pet dagannoth supreme",1,5000).done();
        b("Amoxliatl","Amoxliatl").note("The displayed drop rates represent one kill.")
            .t(29889,"Glacial temotli",1,100).t(30154,"Moxi",1,3000).done();
        b("Hueycoatl","The_Hueycoatl").note("Unique and pet chances are scaled by personal contribution; rates shown are the full-contribution base.")
            .u("unique",1,30070,"Dragon hunter wand",1,105).u("unique",1,30066,"Tome of earth (empty)",1,90)
            .u("unique",1,30085,"Hueycoatl hide",11,315)
            .t(30152,"Huberte",1,400,"Scaled by personal contribution").done();
        b("Brutus","Brutus")
            .u("pre-roll",1,33101,"Mooleta",5,150).u("pre-roll",1,33091,"Bottomless milk bucket (empty)",4,150)
            .u("pre-roll",1,33093,"Cow slippers",1,150).t(33124,"Beef",1,1000).done();
        b("Kalphite Queen","Kalphite_Queen")
            .t(7981,"Kq head",1,128).t(3140,"Dragon chainbody",1,128).t(7158,"Dragon 2h sword",1,256)
            .t(11920,"Dragon pickaxe",1,400).t(12885,"Jar of sand",1,2000).t(12647,"Kalphite princess",1,3000)
            .notModelled("Kq head (tattered): always on the 256th kill, once").done();
        b("Corporeal Beast","Corporeal_Beast").note("One main-table roll; the sigil table is reached at 1/585 from it.")
            .u("main",1,12823,"Spectral sigil",1,1365).u("main",1,12827,"Arcane sigil",1,1365)
            .u("main",1,12819,"Elysian sigil",1,4095).u("main",1,12829,"Spirit shield",8,512)
            .u("main",1,12833,"Holy elixir",3,512)
            .t(25521,"Jar of spirits",1,1000).t(12816,"Pet dark core",1,5000).done();
        gwd("General Graardor","General_Graardor",
            new Object[][]{{11832,"Bandos chestplate",381},{11834,"Bandos tassets",381},{11836,"Bandos boots",381},{11812,"Bandos hilt",508}},
            12650,"Pet general graardor");
        gwd("Kree'arra","Kree'arra",
            new Object[][]{{11826,"Armadyl helmet",381},{11828,"Armadyl chestplate",381},{11830,"Armadyl chainskirt",381},{11810,"Armadyl hilt",508}},
            12649,"Pet kree'arra");
        gwd("Commander Zilyana","Commander_Zilyana",
            new Object[][]{{11838,"Saradomin sword",127},{13256,"Saradomin's light",254},{11785,"Armadyl crossbow",508},{11814,"Saradomin hilt",508}},
            12651,"Pet zilyana");
        gwd("K'ril Tsutsaroth","K'ril_Tsutsaroth",
            new Object[][]{{11787,"Steam battlestaff",127},{11824,"Zamorakian spear",127},{11791,"Staff of the dead",508},{11816,"Zamorak hilt",508}},
            12652,"Pet k'ril tsutsaroth");
        b("Cerberus","Cerberus").note("1 in 130 chance of the unique table; each crystal/stone 1/520.")
            .u("unique",1,13231,"Primordial crystal",1,520).u("unique",1,13229,"Pegasian crystal",1,520)
            .u("unique",1,13227,"Eternal crystal",1,520).u("unique",1,13233,"Smouldering stone",1,520)
            .t(13245,"Jar of souls",1,2000).t(13247,"Hellpuppy",1,3000).done();
        // ---- second Wiki pass (read 2026-11-01 00:45 America/Los_Angeles) ----
        b("Alchemical Hydra","Alchemical_Hydra").note("The first drop can be a unique (~1/87.6 to hit the table); the second is always standard.")
            .u("unique",1,22988,"Hydra tail",1,513).u("unique",1,22983,"Hydra leather",1,514).u("unique",1,22966,"Hydra's claw",1,1001)
            .notModelled("Hydra's eye/fang/heart 1/181.1: always received in a fixed order, so a specific piece is not a constant rate")
            .gap("Pet (Ikkle hydra): Tertiary section read empty on "+READ_DATE+": not offered").done();
        b("Kraken","Kraken")
            .u("main",1,11905,"Trident of the seas (full)",1,512).u("main",1,12004,"Kraken tentacle",1,400)
            .t(12007,"Jar of dirt",1,1000).t(12655,"Pet kraken",1,3000).done();
        b("Thermonuclear smoke devil","Thermonuclear_smoke_devil")
            .u("pre-roll",1,12002,"Occult necklace",1,350).u("pre-roll",1,11998,"Smoke battlestaff",1,512)
            .u("pre-roll",1,3140,"Dragon chainbody",1,2000)
            .t(25524,"Jar of smoke",1,2000).t(12648,"Pet smoke devil",1,3000).done();
        b("Abyssal Sire","Abyssal_Sire")
            .u("pre-roll",1,13273,"Unsired",1,100)
            .notModelled("Bludgeon pieces, Abyssal dagger and the pet come from offering the Unsired, not from the kill").done();
        b("Araxxor","Araxxor").note("1/150 to roll the unique table from the main drop.")
            .u("main",1,29799,"Araxyte fang",1,600).u("main",1,29786,"Jar of venom",1,1500)
            .t(29836,"Nid",1,3000,"Harvest rate; destroying the corpse doubles it to 1/1,500")
            .notModelled("Noxious halberd pieces 1/200 any: no duplicate until a set is complete, so a specific piece is not constant").done();
        dt2("Duke Sucellus","Duke_Sucellus","1/90 unique table per kill",new Object[][]{{28276,"Chromium ingot",3,720},{28321,"Eye of the Duke",1,720},
            {26241,"Virtus mask",1,2160},{26243,"Virtus robe top",1,2160},{26245,"Virtus robe bottom",1,2160},{28334,"Awakener's orb",1,48.5}},
            0,null,0,"Magus vestige","Pet (Baron): Tertiary not readable on "+READ_DATE+": not offered");
        dt2("Vardorvis","Vardorvis","1/136 unique table per kill",new Object[][]{{28276,"Chromium ingot",3,1088},{28319,"Executioner's axe head",1,1088},
            {26241,"Virtus mask",1,3264},{26243,"Virtus robe top",1,3264},{26245,"Virtus robe bottom",1,3264},{28334,"Awakener's orb",1,80.6}},
            28248,"Butch",3000,"Ultor vestige",null);
        dt2("The Leviathan","The_Leviathan","1/96 unique table per kill",new Object[][]{{28276,"Chromium ingot",3,768},{28325,"Leviathan's lure",1,768},
            {26241,"Virtus mask",1,2304},{26243,"Virtus robe top",1,2304},{26245,"Virtus robe bottom",1,2304},{28334,"Awakener's orb",1,53.6}},
            28252,"Lil'viathan",2500,"Venator vestige",null);
        dt2("The Whisperer","The_Whisperer","1/64 unique table per kill",new Object[][]{{28276,"Chromium ingot",3,512},{28323,"Siren's staff",1,512},
            {26241,"Virtus mask",1,1536},{26243,"Virtus robe top",1,1536},{26245,"Virtus robe bottom",1,1536},{28334,"Awakener's orb",1,34.5}},
            28246,"Wisp",2000,"Bellator vestige",null);
        b("Royal Titans","Royal_Titans")
            .t(30622,"Bran",1,3000,"Loot rate; sacrificing loot doubles it to 1/1,500")
            .notModelled("Element staff crowns, prayer scrolls and Giantsoul amulet: per-Titan pre-roll that depends on contribution and loot choice").done();
        b("Yama","Yama").note("Solo rates; Yama rolls each duo player separately and uniques scale with contribution.")
            .u("unique",1,30759,"Soulflame horn",1,300,"Wiki: 1/120 unique roll × 2/5 horn (solo)")
            .t(30888,"Yami",1,2500)
            .notModelled("Oathplate armour: Wiki gives 1/120 × 3/5 for 'a piece' but not a per-piece rate").done();
        b("Phosani's Nightmare","Phosani's_Nightmare").note("Two unique tables are rolled every kill; both can succeed.")
            .u("staff",1,24422,"Nightmare staff",69,35000).u("staff",1,24419,"Inquisitor's great helm",1,700)
            .u("staff",1,24420,"Inquisitor's hauberk",1,700).u("staff",1,24421,"Inquisitor's plateskirt",1,700)
            .u("staff",1,24417,"Inquisitor's mace",31,35000)
            .u("orb",1,24517,"Eldritch orb",1,1600).u("orb",1,24511,"Harmonised orb",1,1600).u("orb",1,24514,"Volatile orb",1,1600)
            .t(25838,"Parasitic egg",1,200).t(24491,"Little nightmare",1,1400).t(24495,"Jar of dreams",1,4000)
            .notModelled("Slepey tablet: always on the 25th kill").done();
        b("Skotizo","Skotizo")
            .t(21275,"Dark claw",1,25).t(21273,"Skotos",1,65).t(19701,"Jar of darkness",1,200).done();
        b("Shellbane gryphon","Shellbane_gryphon")
            .u("pre-roll",1,31245,"Belle's folly (tarnished)",1,75)
            .t(32921,"Jar of feathers",1,2000).t(31285,"Gull",1,3000).done();
        b("Deranged archaeologist","Deranged_archaeologist").u("unique",1,30895,"Steel ring",3,131).done();
        b("Maggot King","Maggot_King").note("Open-stomach option; the take-eggs pet chance is 1/1,502.4 per egg-take.")
            .u("pre-roll",1,33634,"Elder venator fang",1,340).u("pre-roll",1,33631,"Crimson kisten",1,520)
            .t(33642,"Maggot marquess",1,3500,"Open-stomach option").done();
        b("Mad Angel","Mad_Angel")
            .u("pre-roll",1,34027,"Hallowfell",1,127).u("pre-roll",1,34030,"Jar of light",1,1000)
            .t(34042,"Aggy",1,2000).done();
        b("Hespori","Hespori").u("pre-roll",1,22994,"Bottomless compost bucket",1,35)
            .notModelled("Tangleroot: rate changes with Farming level").done();
        b("Tormented Demons","Tormented_Demon").note("Synapse is rolled first, then the burning claw; at most one per kill.")
            .u("unique",1,29580,"Tormented synapse",1,500).u("unique",1,29574,"Burning claw",499,250000).done();
        b("Demonic Gorillas","Demonic_gorilla").note("5/500 chance of the unique sub-table; at most one per kill.")
            .u("unique",1,19529,"Zenyte shard",1,300).u("unique",1,19592,"Ballista limbs",1,500).u("unique",1,19601,"Ballista spring",1,500)
            .u("unique",1,19586,"Light frame",1,750).u("unique",1,19589,"Heavy frame",1,1500).u("unique",1,19610,"Monkey tail",1,1500).done();
        b("Lizardman Shamans","Lizardman_shaman").u("pre-roll",1,13576,"Dragon warhammer",1,3000).done();
        // Wilderness: odds only. The Outfitter keeps Best in Bank off and shows the PvP/PK warning.
        wild3("Callisto","Callisto",196,new Object[][]{{27667,"Claws of Callisto",196},{27681,"Voidwaker hilt",360},{12603,"Tyrannical ring",512}},13178,"Callisto cub",1500,true);
        wild3("Artio","Artio",358,new Object[][]{{27667,"Claws of Callisto",618},{27681,"Voidwaker hilt",912},{12603,"Tyrannical ring",716}},13178,"Callisto cub",2800,false);
        wild3("Venenatis","Venenatis",196,new Object[][]{{27670,"Fangs of Venenatis",196},{27687,"Voidwaker gem",360},{12605,"Treasonous ring",512}},13177,"Venenatis spiderling",1500,true);
        wild3("Spindel","Spindel",358,new Object[][]{{27670,"Fangs of Venenatis",618},{27687,"Voidwaker gem",912},{12605,"Treasonous ring",716}},13177,"Venenatis spiderling",2800,false);
        wild3("Vet'ion","Vet'ion",196,new Object[][]{{27673,"Skull of Vet'ion",196},{27684,"Voidwaker blade",360},{12601,"Ring of the gods",512}},13179,"Vet'ion jr.",1500,true);
        wild3("Calvar'ion","Calvar'ion",358,new Object[][]{{27673,"Skull of Vet'ion",618},{27684,"Voidwaker blade",912},{12601,"Ring of the gods",716}},13179,"Vet'ion jr.",2800,false);
        b("King Black Dragon","King_Black_Dragon")
            .u("pre-roll",1,11920,"Dragon pickaxe",1,1000)
            .t(7980,"Kbd heads",1,128).t(12653,"Prince black dragon",1,3000).t(11286,"Draconic visage",1,5000).done();
        b("Chaos Elemental","Chaos_Elemental").u("pre-roll",1,11920,"Dragon pickaxe",1,256).t(11995,"Pet chaos elemental",1,300).done();
        b("Chaos Fanatic","Chaos_Fanatic")
            .u("pre-roll",1,11928,"Odium shard 1",1,256).u("pre-roll",1,11931,"Malediction shard 1",1,256)
            .t(11995,"Pet chaos elemental",1,1000).done();
        b("Crazy archaeologist","Crazy_archaeologist")
            .u("pre-roll",1,11929,"Odium shard 2",1,256).u("pre-roll",1,11932,"Malediction shard 2",1,256)
            .t(11990,"Fedora",1,128).done();
        b("Scorpia","Scorpia")
            .u("pre-roll",1,11930,"Odium shard 3",1,256).u("pre-roll",1,11933,"Malediction shard 3",1,256)
            .t(13181,"Scorpia's offspring",1,2015.75).done();
        // Key chests (Wiki "Chest (Obor's lair)" / "Chest (Bryophyta's lair)", read 2026-11-01): opened once
        // per kill with the key; the unique's rarity is shown per chest opening.
        new B("Obor","Obor","Chest_(Obor's_lair)").unit("giant-key chest (once per kill)")
            .note("Loot comes from the chest opened with a giant key after each kill.")
            .u("chest",1,20756,"Hill giant club",1,118).done();
        new B("Bryophyta","Bryophyta","Chest_(Bryophyta's_lair)").unit("mossy-key chest (once per kill)")
            .note("Loot comes from the chest opened with a mossy key after each kill.")
            .u("chest",1,22372,"Bryophyta's essence",1,118).done();
        // ---- Chest rewards (owner direction 2026-11-01: model these bosses by their chest) ----
        // Barrows Chest: Wiki "Chest (Barrows)", read 2026-11-01. With all six brothers killed the
        // Wiki gives ~1/350.14 per chest for a specific piece (any piece ~1/15.01). One KC unit = one
        // chest. Several pieces can come from ONE chest, so pieces are not mutually exclusive; each
        // is an independent per-chest roll here (labelled approximation when combining pieces).
        B bw=new B("Barrows","Barrows","Chest_(Barrows)").unit("Barrows chest")
            .note("Per chest with all six brothers killed (Wiki ~1/350.14 a specific piece, ~1/15.01 any piece). Fewer brothers = fewer rolls and lower odds. Combining pieces treats them as independent: an approximation.");
        Object[][] pieces={{4708,"Ahrim's hood"},{4712,"Ahrim's robetop"},{4714,"Ahrim's robeskirt"},{4710,"Ahrim's staff"},
            {4716,"Dharok's helm"},{4720,"Dharok's platebody"},{4722,"Dharok's platelegs"},{4718,"Dharok's greataxe"},
            {4724,"Guthan's helm"},{4728,"Guthan's platebody"},{4730,"Guthan's chainskirt"},{4726,"Guthan's warspear"},
            {4732,"Karil's coif"},{4736,"Karil's leathertop"},{4738,"Karil's leatherskirt"},{4734,"Karil's crossbow"},
            {4745,"Torag's helm"},{4749,"Torag's platebody"},{4751,"Torag's platelegs"},{4747,"Torag's hammers"},
            {4753,"Verac's helm"},{4757,"Verac's brassard"},{4759,"Verac's plateskirt"},{4755,"Verac's flail"}};
        for(Object[] pc:pieces) bw.t((Integer)pc[0],(String)pc[1],1,350.14,"All six brothers killed");
        bw.notModelled("Reward-potential (non-equipment) loot and runs with fewer than six brothers").done();
        // Lunar Chest (Moons of Peril): Wiki "Lunar Chest", read 2026-11-01. Each Moon defeated in
        // the run gives its own 1/56 roll for that Moon's set; a hit picks among pieces not yet owned
        // (duplicate protection). Table rate shown per piece: 1/224. One KC unit = one kill of THAT Moon.
        moon("Eclipse Moon",new Object[][]{{29000,"Eclipse atlatl"},{29010,"Eclipse moon helm"},{29004,"Eclipse moon chestplate"},{29007,"Eclipse moon tassets"}});
        moon("Blood Moon",new Object[][]{{28997,"Dual macuahuitl"},{29028,"Blood moon helm"},{29022,"Blood moon chestplate"},{29025,"Blood moon tassets"}});
        moon("Blue Moon",new Object[][]{{28988,"Blue moon spear"},{29019,"Blue moon helm"},{29013,"Blue moon chestplate"},{29016,"Blue moon tassets"}});
        BOSS_NOTES.put("The Nightmare","Group MVP/contribution mechanics: not modelled");
        BOSS_NOTES.put("Nex","Contribution-based uniques: not modelled");
        BOSS_NOTES.put("Doom of Mokhaiotl","Unique and pet rates change with delve level: not modelled");
        BOSS_NOTES.put("The Mimic","Rewards depend on casket tier and attempt number: not modelled");
    }
    private static void moon(String moon,Object[][] set){
        B x=new B(moon,moon,"Lunar_Chest").setTable(1/56.0).unit(moon+" kill (Lunar Chest roll)")
            .note("Loot comes from the Lunar Chest: each "+moon+" defeated that run = one 1/56 roll for its set; one piece per hit, no duplicates until the set is complete.");
        for(Object[] it:set) x.u("lunar-chest",1,(Integer)it[0],(String)it[1],1,224);
        x.notModelled("Standard Lunar Chest loot (rolled only when no unique is received)");
        x.done();
    }
    /** Desert Treasure II bosses: one unique table, secondary uniques replace normal loot when it fails. */
    private static void dt2(String boss,String page,String rate,Object[][] uniques,int petId,String pet,int petDen,String vestige,String gap){
        B x=b(boss,page).note(rate+"; secondary uniques replace normal loot when the unique roll fails, so at most one per kill. Normal (not awakened) rates.");
        for(Object[] u:uniques) x.u("unique",1,(Integer)u[0],(String)u[1],((Number)u[2]).doubleValue(),((Number)u[3]).doubleValue());
        if(petId>0) x.t(petId,pet,1,petDen);
        x.notModelled(vestige+": invisible-roll progression, not a constant per-kill rate");
        x.notModelled("Tablet and quartz: rates change with kill count");
        if(gap!=null) x.gap(gap);
        x.done();
    }
    /** Wilderness three-tier bosses: uniques are per kill, at most one per kill. */
    private static void wild3(String boss,String page,int d2,Object[][] uniques,int petId,String pet,int petDen,boolean big){
        B x=b(boss,page).note("Wilderness: PvP/PK risk. Uniques are per kill, at most one per kill"+(big?"; in groups loot is split by damage.":"."));
        x.u("unique",1,7158,"Dragon 2h sword",1,d2==196?256:358).u("unique",1,11920,"Dragon pickaxe",1,d2==196?256:358);
        for(Object[] u:uniques) x.u("unique",1,(Integer)u[0],(String)u[1],1,(Integer)u[2]);
        x.t(petId,pet,1,petDen,big?"Solo rate; the pet roll scales with contribution":null);
        x.done();
    }
    private static void gwd(String boss,String page,Object[][] uniques,int petId,String pet){
        B x=b(boss,page).note("One main-table roll; uniques are mutually exclusive. Godsword shards 1/762 each.");
        for(Object[] u:uniques) x.u("main",1,(Integer)u[0],(String)u[1],1,(Integer)u[2]);
        x.u("main",1,11818,"Godsword shard 1",1,762).u("main",1,11820,"Godsword shard 2",1,762).u("main",1,11822,"Godsword shard 3",1,762);
        x.t(petId,pet,1,5000).done();
    }

    // ---- R09 hotfix (owner native QA: Brutus log 3/4 unlocked, TrashPanda 2/4) ----
    // Brutus drops and the Collection Log list "Bottomless milk bucket (empty)" = 33091 (OSRS Wiki Brutus Drops +
    // Bottomless milk bucket infobox, read 2026-11-01). The table previously held 33089, the FULL form, which the log never
    // draws, so the unlocked bucket could never match. 33089 is kept as a reviewed equivalent (same item, filled state).
    // Cow slippers: OSRS Wiki infobox lists variant ids 33093, 33096, 33097, 33098 for the same item (read 2026-11-01).
    private static final Map<Integer,Set<Integer>> EQUIVALENT_IDS = Map.of(33091, Set.of(33089), 33093, Set.of(33096, 33097, 33098));
    /** Drop keys that existed in saved settings before an ID correction, mapped to the corrected key. */
    private static final Map<String,String> LEGACY_KEYS = Map.of("brutus:33089", "brutus:33091");
    /** The drop's own id plus reviewed same-item variants (for Collection Log / ownership matching only). */
    public static Set<Integer> equivalentIds(int itemId) {
        Set<Integer> out = new LinkedHashSet<>(); out.add(itemId);
        out.addAll(EQUIVALENT_IDS.getOrDefault(itemId, Set.of()));
        return out;
    }
    public static String canonicalKey(String key) { return key == null ? null : LEGACY_KEYS.getOrDefault(key, key); }
    public static List<Encounter> encounters(String boss){return BY_BOSS.getOrDefault(boss,List.of());}
    public static Optional<Encounter> encounter(String name){return Optional.ofNullable(BY_ENCOUNTER.get(name));}
    public static Collection<Encounter> all(){return Collections.unmodifiableCollection(BY_ENCOUNTER.values());}
    public static Optional<Drop> drop(String key){
        return BY_ENCOUNTER.values().stream().flatMap(e->e.drops().stream()).filter(d->d.key().equals(key)).findFirst();
    }
    public static Optional<Encounter> encounterOf(String dropKey){
        return BY_ENCOUNTER.values().stream().filter(e->e.drop(dropKey).isPresent()).findFirst();
    }
    public static Coverage coverage(String boss){
        var es=encounters(boss);
        if(es.isEmpty()) return BOSS_NOTES.containsKey(boss)?Coverage.MECHANIC_NOT_MODELLED:Coverage.NOT_YET_RESEARCHED;
        if(es.stream().allMatch(e->e.drops().isEmpty())) return Coverage.MECHANIC_NOT_MODELLED;
        if(es.stream().anyMatch(e->!e.gaps().isEmpty())) return Coverage.PARTIAL;
        return Coverage.VERIFIED;
    }
    public static String coverageNote(String boss){
        var es=encounters(boss);
        if(es.isEmpty()) return BOSS_NOTES.getOrDefault(boss,"Wiki drop rates not yet read for this build");
        List<String> parts=new ArrayList<>();
        for(var e:es){ parts.addAll(e.notModelled()); parts.addAll(e.gaps()); }
        return String.join("; ",parts);
    }
}
