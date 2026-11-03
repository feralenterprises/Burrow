package gg.feral.burrow.ui;

import java.util.*;

/** ACT-R01: Wiki-grounded, generic utility kit references, never account claims or bank scans.
 * No guessed item IDs, quantities, unlocks, STASH content or task-specific Slayer protections.
 * Each named entry occupies one proposed physical inventory slot; the other slots stay empty.
 */
public final class ActivityOutfitterCatalog {
    public record Slot(String label, boolean conditional) {
        public Slot { Objects.requireNonNull(label); if (label.isBlank()) throw new IllegalArgumentException("empty slot"); }
    }
    public record Kit(String id, String category, String title, String description,
                      List<Slot> equipment, List<Slot> inventory, List<String> notes,
                      String sourceUrl) {
        public Kit {
            id=Objects.requireNonNull(id); category=Objects.requireNonNull(category);
            title=Objects.requireNonNull(title); description=Objects.requireNonNull(description);
            equipment=Collections.unmodifiableList(new ArrayList<>(equipment)); inventory=List.copyOf(inventory);
            notes=List.copyOf(notes); sourceUrl=Objects.requireNonNull(sourceUrl);
            if(equipment.size()!=11 || inventory.size()>28 || inventory.isEmpty())
                throw new IllegalArgumentException("invalid 11-slot equipment/28-slot inventory");
            if(!sourceUrl.startsWith("https://oldschool.runescape.wiki/w/"))
                throw new IllegalArgumentException("source required");
        }
    }
    private static Slot x(String s){return new Slot(s,false);}
    private static Slot c(String s){return new Slot(s,true);}
    private static List<Slot> gear(Slot head,Slot cape,Slot neck,Slot ammo,Slot weapon,Slot body,
                                   Slot shield,Slot legs,Slot gloves,Slot boots,Slot ring){
        return Arrays.asList(head,cape,neck,ammo,weapon,body,shield,legs,gloves,boots,ring);
    }
    private static final List<Kit> KITS=buildKits();
    private static List<Kit> buildKits(){
    List<Kit> initial=List.of(
        new Kit("herb","FARMING","Herb Run","Harvest and replant at your unlocked herb patches.",
            gear(null,c("Ardougne cloak 2+"),null,null,c("Magic secateurs"),
                 c("Weight-reducing / Farmer's"),null,c("Weight-reducing / Farmer's"),null,
                 c("Weight-reducing"),c("Explorer's ring 2+")),
            List.of(x("Herb seeds"),x("Spade"),c("Rake"),c("Seed dibber"),
                    c("Bottomless compost bucket / compost"),c("Ectophial"),
                    c("Xeric's talisman"),c("Teleport options"),c("Herb sack")),
            List.of("Seeds depend on the patches actually unlocked; amounts are not inferred.",
                    "Rake optional with auto-weed; seed dibber optional after barehanded planting unlock.",
                    "Compost may be stored with a Tool Leprechaun; leave harvesting slots free."),
            "https://oldschool.runescape.wiki/w/Farming_runs"),
        // ACT-R02 A1: retain the original `trees` key so returning players do not lose
        // their activity selection. Tree and fruit tree patch requirements are distinct.
        new Kit("trees","FARMING","Tree Run","Check, clear and replant ordinary trees at accessible patches.",
            gear(null,c("Travel / Farmer cape"),null,null,null,c("Light / Farmer gear"),null,
                 c("Light / Farmer gear"),null,c("Travel boots"),c("Travel ring")),
            List.of(x("Tree saplings for chosen patches"),x("Spade"),
                    c("Rake if no auto-weed"),c("Gardener payments OR compost"),
                    c("Coins to clear trees"),c("Unlocked tree patch teleports")),
            List.of("Patch count, tree type and payments depend on the selected accessible route; never assume the Wiki full-route seven saplings.",
                    "Spade is used for replanting; rake is only necessary where weeds remain.",
                    "Gardener payment and compost are conditional alternatives; growth wait is not active run time."),
            "https://oldschool.runescape.wiki/w/Farming_runs"),
        new Kit("fruit-trees","FARMING","Fruit Tree Run","Check fruit trees; optional calquat stops only with access.",
            gear(null,c("Travel / Farmer cape"),null,null,null,c("Light / Farmer gear"),null,
                 c("Light / Farmer gear"),null,c("Travel boots"),c("Travel ring")),
            List.of(x("Fruit tree saplings for chosen patches"),x("Spade"),
                    c("Calquat saplings if route allows"),c("Rake if no auto-weed"),
                    c("Gardener payments OR compost"),c("Tree clearing coins"),
                    c("Unlocked fruit tree teleports"),c("Calquat teleports if selected")),
            List.of("Fruit tree and calquat patches have different level/quest/access requirements; do not assume full-route counts.",
                    "Fruit trees take roughly 15 hours to grow, but that is not the active run duration.",
                    "Only pack calquat saplings if your route includes accessible calquat patches (72 Farming required)."),
            "https://oldschool.runescape.wiki/w/Farming_runs"),
        new Kit("birdhouse","HUNTER","Birdhouse Run","Four Fossil Island traps; collect and reset when full.",
            gear(null,c("Light / travel cape"),c("Digsite pendant"),null,null,
                 c("Light gear"),null,c("Light gear"),null,c("Light boots"),null),
            List.of(x("Replacement birdhouse #1"),x("Replacement birdhouse #2"),
                    x("Replacement birdhouse #3"),x("Replacement birdhouse #4"),
                    x("40 low seeds OR 20 eligible high seeds"),
                    c("Digsite pendant if not equipped"),c("Crafting tools for build-on-run variant")),
            List.of("Only count four prepared houses if the player already built them; each birdhouse uses one physical inventory slot.",
                    "For four traps use 40 suitable low-tier seeds or 20 eligible high-tier seeds, with mixtures possible per trap.",
                    "Bone Voyage is required for access. The ~50-minute refill time is not how long the run takes.",
                    "If making houses on the route, use an alternate packing plan with logs, clockworks, hammer and chisel; keep loot space free."),
            "https://oldschool.runescape.wiki/w/Bird_house_trapping"),
        new Kit("clue","CLUES","Clue Scroll","Light travel kit. The current step decides exact gear and combat needs.",
            gear(null,c("Travel cape"),c("Teleport jewellery"),null,null,
                 c("Light armour"),null,c("Light gear"),null,c("Travel boots"),c("Travel ring")),
            List.of(x("Clue scroll"),c("Spade"),x("Teleport options"),
                    c("Stamina / energy potion"),c("Coins"),c("Dramen / lunar staff"),
                    c("Combat weapon"),c("Food"),c("Prayer potion"),c("Antipoison"),
                    c("Step-specific emote items"),c("Clue box (Wilderness)")),
            List.of("No emote outfit is assumed. STASH units, quest locks and steps vary.",
                    "Wilderness steps require a separate risk assessment before taking valuable gear.",
                    "Some clue steps need combat; the generic travel kit is not a combat guarantee."),
            "https://oldschool.runescape.wiki/w/Treasure_Trails/Guide"),
        new Kit("slayer-melee","SLAYER","Slayer · Melee","Ordinary melee task starter. Swap protection and weapon by monster.",
            gear(c("Slayer helm / black mask on task"),c("Melee / prayer cape"),c("Strength / prayer amulet"),null,
                 x("Wiki melee weapon example · bank unchecked"),c("Melee body"),c("Defender / protection shield"),
                 c("Melee legs"),c("Melee gloves"),c("Melee boots"),c("Melee ring")),
            List.of(c("Food"),c("Prayer potion"),c("Combat boosting potion"),
                    x("Teleport out"),c("Task-required protection item"),
                    c("Expeditious / slaughter bracelet"),c("Dwarf multicannon")),
            List.of("No universal Slayer loadout replaces task-specific protection items.",
                    "Helmet/black-mask effect only applies on an eligible Slayer task.",
                    "Cannon and prayer supplies depend on the monster and location."),
            "https://oldschool.runescape.wiki/w/Slayer_training"),
        new Kit("slayer-magic","SLAYER","Slayer · Magic","Generic Magic preparation. Barrage only on suitable tasks.",
            gear(c("Slayer helm (i) on task"),c("Magic / prayer cape"),c("Magic necklace"),null,
                 x("Magic weapon"),c("Magic / prayer body"),c("Magic offhand"),
                 c("Magic / prayer legs"),c("Magic gloves"),c("Magic boots"),c("Magic ring")),
            List.of(c("Runes for chosen spell"),c("Rune pouch"),c("Prayer potions"),
                    c("Food"),c("Aggro / stacking tool"),x("Teleport out"),
                    c("Task-required item")),
            List.of("Burst/barrage needs the correct spellbook, runes, and a suitable encounter.",
                    "Imbued Slayer helm/black mask bonuses are conditional on task.",
                    "This generic reference is not a claim that every Slayer monster can be barraged."),
            "https://oldschool.runescape.wiki/w/Slayer_training"),
        new Kit("slayer-ranged","SLAYER","Slayer · Ranged","General ranged task starter; adjust ammunition and protection.",
            gear(c("Slayer helm (i) on task"),x("Ava's device appropriate to account"),
                 c("Ranged necklace"),x("Weapon-matched ammunition"),x("Ranged weapon"),
                 c("Ranged body"),null,
                 c("Ranged legs"),c("Ranged gloves"),c("Ranged boots"),c("Ranged ring")),
            List.of(x("Weapon-matched ammo / charges"),c("Food"),
                    c("Ranging potion"),c("Prayer potion"),x("Teleport out"),
                    c("Dwarf multicannon"),c("Task-required item")),
            List.of("Ava's device remains in the cape slot for ordinary Ranged setups.",
                    "Ammo, shield compatibility, safespots, and cannon use depend on the selected task.",
                    "Do not assume black mask or unimbued Slayer helm boosts Ranged damage."),
            "https://oldschool.runescape.wiki/w/Slayer_training"),
        // SLAY-R04: deliberately separate these source variants from generic Slayer.
        new Kit("slayer-melee-hallowfell","SLAYER","Slayer · Hallowfell","Two-handed Melee cleaving for source-supported multi-target tasks.",
            gear(c("Slayer helmet (i), only on task"),c("Fire cape / prayer cape"),
                 c("Amulet of fury / strength"),null,x("Hallowfell (75 Attack)"),
                 c("Torva / Bandos / prayer body"),null,c("Torva / Bandos / prayer legs"),
                 c("Slayer bracelet / melee gloves"),c("Dragon boots / prayer boots"),c("Melee ring")),
            List.of(c("Food"),c("Prayer potions"),c("Combat boosting potion"),
                    x("Teleport out"),c("Aggression tool: darts / knives / goading"),
                    c("Dwarf multicannon if permitted"),c("Task-required protection")),
            List.of("Hallowfell is two-handed: shield slot is intentionally EMPTY; do not generate defender pairings.",
                    "Use only for suitable grouped Slayer targets; cleaving is positional and not universal.",
                    "Source equipment table is best-to-worst, not verified ownership; all illustrated gear is conditional.",
                    "Weapon requires 75 Attack and source-approved encounter mechanics."),
            "https://oldschool.runescape.wiki/w/Slayer_training#Melee_(Hallowfell)"),
        new Kit("slayer-magic-barrage","SLAYER","Slayer · Barrage","Ancient Magicks burst/barrage for suitable stacked multicombat monsters.",
            gear(c("Slayer helmet (i), only on task"),c("Imbued god cape / prayer cape"),
                 c("Occult necklace / prayer necklace"),null,c("Ancient-compatible weapon / Kodai"),
                 c("Virtus / Proselyte robe top"),c("Offhand only with 1h weapon"),
                 c("Virtus / prayer robe bottom"),c("Magic / prayer gloves"),
                 c("Devout boots / magic boots"),c("Magic / prayer ring")),
            List.of(x("Correct runes for chosen Ancient spell"),c("Rune pouch"),
                    c("Prayer potions"),c("Food"),c("Aggro lure / darts / goading"),
                    x("Teleport out"),c("Task protection / antidote")),
            List.of("Ancient Magicks requires Desert Treasure I; spell choice and rune types depend on level and unlocks.",
                    "Only suitable multi-target tasks qualify; do not propose Barrage for all Magic tasks.",
                    "Prayer gear can beat offensive Magic gear versus targets with low Magic defence.",
                    "Ancient-compatible weapon only; no autocast guarantee for a generic Magic weapon."),
            "https://oldschool.runescape.wiki/w/Slayer_training#Magic_(Barrage)"),
        new Kit("slayer-ranged-wilderness","WILDERNESS","Slayer · Wilderness Ranged","HIGH RISK · risk-aware Wiki reference only; never Best in Bank.",
            gear(c("Slayer helmet (i), risk your own"),c("Ava's accumulator / risk review"),
                 c("Amulet of glory / disposable necklace"),c("Blessing if desired"),
                 c("Webweaver / Craw's / blowpipe (risk review)"),
                 x("Black d'hide body"),null,x("Black d'hide chaps"),
                 c("Expeditious bracelet"),c("Snakeskin / mixed hide boots"),
                 c("Ring of dueling / wealth")),
            List.of(x("Wilderness escape teleport (check depth)"),
                    c("Limited weapon ammo / ether"),c("Food"),c("Super restores / brews"),
                    c("Ranging potion"),c("Stamina potion"),c("Anti-venom as needed"),
                    c("Task-required protection"),c("Cannon only if accepting loss risk")),
            List.of("WILDERNESS: BEST IN BANK PERMANENTLY DISABLED. No bank scan, owned-item upgrades or auto-substitution.",
                    "The Wiki's four-items guidance is not a universal safe-on-death guarantee; skull, location and items change risk.",
                    "Ether in Webweaver/Craw's bows is lost on death. Carry only what you accept losing.",
                    "Check teleport restrictions, PKer risk, and task requirements; no guaranteed-safe label."),
            "https://oldschool.runescape.wiki/w/Slayer_training#Ranged_(Wilderness_Slayer)")
    );
    List<Kit> all=new ArrayList<>(initial);
    for(var reference:ClueBankTagLayouts.all()) all.add(new Kit(reference.id(),"CLUES",
        "Wiki · "+reference.title(),"Sourced bank-tag template; each item keeps its original 8x7 bank position.",
        gear(null,null,null,null,null,null,null,null,null,null,null),
        List.of(x("Clue scroll / step-specific items")),
        List.of("The 8x7 grid is the Wiki bank-tag layout, not a 28-slot carried inventory.",
                "No quantities are encoded in bank-tag exports; identical IDs at multiple positions are intentional.",
                "Choose travel, clue step and Wilderness gear separately; bank ownership is unknown."),
        ClueBankTagLayouts.SOURCE));
    return List.copyOf(all);
    }
    /** Explicit security boundary for any Wilderness activity, including future variants. */
    public static boolean isWilderness(String id){
        if(id==null)return false;
        Kit kit=KITS.stream().filter(k->k.id().equals(id)).findFirst().orElse(null);
        return id.toLowerCase(Locale.ROOT).contains("wilderness") ||
            (kit!=null && kit.category().equals("WILDERNESS"));
    }
    /** A permission boundary, NOT a claim that non-Wilderness activity BIB exists yet. */
    public static boolean bestBankPermitted(String id){return !isWilderness(id);}
    public static List<Kit> all(){return KITS;}
    public static Kit find(String id){return KITS.stream().filter(k->k.id().equals(id)).findFirst().orElse(KITS.get(0));}
    private ActivityOutfitterCatalog(){}
}
