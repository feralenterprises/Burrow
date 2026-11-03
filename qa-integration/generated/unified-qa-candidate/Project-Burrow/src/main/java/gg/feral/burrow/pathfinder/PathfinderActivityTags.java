package gg.feral.burrow.pathfinder;

import java.util.Locale;
/**
 * R04. Display-only tags, never used to recommend, gate or award XP.
 * SECONDARY_XP and SECONDARY_SUPPLIES are deliberately distinct:
 * Plank Make provides Magic XP and Construction materials, NOT Construction XP.
 * Unverifiable classifications remain GENERIC rather than guessing a skill.
 */
public final class PathfinderActivityTags {
    private PathfinderActivityTags() {}
    public enum Skill { QUEST, COMBAT, SLAYER, MAGIC, RANGED, MELEE, FLETCHING, CRAFTING,
        CONSTRUCTION, FARMING, HUNTER, MINING, SMITHING, HERBLORE, PRAYER, AGILITY,
        FISHING, COOKING, WOODCUTTING, RUNECRAFT, THIEVING, SAILING, CLUE, MONEY, GEAR, GENERIC }
    public enum Relation { NONE, SECONDARY_XP, SECONDARY_SUPPLIES, COMBAT_METHOD }
    public record Tags(Skill primary,Skill secondary,Relation relation,String note) {
        public Tags { if(primary==null||relation==null)throw new IllegalArgumentException("tags");
            if((secondary==null)!=(relation==Relation.NONE))throw new IllegalArgumentException("secondary relation");
            note=note==null?"":note;
        }
        public boolean dual(){return secondary!=null;}
    }
    private static Tags single(Skill skill){return new Tags(skill,null,Relation.NONE,"");}
    private static Tags dual(Skill a,Skill b,Relation relation,String why){return new Tags(a,b,relation,why);}
    public static Tags classify(SuggestionEngine.Suggestion s) {
        String id=s.id().toLowerCase(Locale.ROOT), title=s.title().toLowerCase(Locale.ROOT);
        if(s.category()==GuideRule.Category.QUEST) return single(Skill.QUEST);
        // Positive, source-reviewed dual use cases. Neither regex match nor all melee/slayer
        // can assume a combat style without source evidence for that exact activity.
        if(id.equals("plank_make")||title.contains("plank make"))
            return dual(Skill.MAGIC,Skill.CONSTRUCTION,Relation.SECONDARY_SUPPLIES,"Magic XP; supplies for Construction and Sailing boat parts, not direct Construction/Sailing XP");
        if(id.equals("superglass_make")||title.contains("superglass make"))
            return dual(Skill.MAGIC,Skill.CRAFTING,Relation.SECONDARY_XP,"Magic and Crafting XP from spell");
        if(id.equals("string_jewellery")||title.contains("string jewellery"))
            return dual(Skill.MAGIC,Skill.CRAFTING,Relation.SECONDARY_XP,"Magic and small Crafting XP per amulet");
        boolean slayer=title.contains("slayer")||id.startsWith("slayer_");
        if(slayer && (title.contains("barrage")||title.contains("burst")))
            return dual(Skill.SLAYER,Skill.MAGIC,Relation.COMBAT_METHOD,"Magic used on Slayer task; task/location must support it");
        if(slayer && (title.contains("ranged")||title.contains("cannon")))
            return dual(Skill.SLAYER,Skill.RANGED,Relation.COMBAT_METHOD,"Ranged/cannon Slayer method; check site limitations");
        if(slayer && title.contains("melee"))
            return dual(Skill.SLAYER,Skill.MELEE,Relation.COMBAT_METHOD,"Melee Slayer method");
        if(slayer)return single(Skill.SLAYER);
        if(title.contains("bird house")||title.contains("birdhouse"))return single(Skill.HUNTER);
        if(title.contains("farming run")||title.contains("herb run")||title.contains("tree run")||title.contains("farming contract"))return single(Skill.FARMING);
        if(title.contains("clue scroll")||title.contains("treasure trail"))return single(Skill.CLUE);
        // Specific-skill text in supported skilling rules. Do not infer secondary XP from ingredients.
        for(var entry : new Object[][] {
            {"fletch",Skill.FLETCHING},{"bolt tip",Skill.FLETCHING},
            {"craft",Skill.CRAFTING},{"glassblow",Skill.CRAFTING},{"battlestav",Skill.CRAFTING},
            {"herblore",Skill.HERBLORE},{"potion",Skill.HERBLORE},
            {"agility",Skill.AGILITY},{"mining",Skill.MINING},{"smithing",Skill.SMITHING},
            {"construction",Skill.CONSTRUCTION},{"hunter",Skill.HUNTER},
            {"fishing",Skill.FISHING},{"cooking",Skill.COOKING},
            {"woodcutting",Skill.WOODCUTTING},{"runecraft",Skill.RUNECRAFT},
            {"thieving",Skill.THIEVING},{"prayer",Skill.PRAYER},
            {"sailing",Skill.SAILING}, {"magic",Skill.MAGIC}
        }) {
            String token=(String)entry[0];
            if(title.contains(token)||id.contains(token.replace(' ','_')))return single((Skill)entry[1]);
        }
        if(s.category()==GuideRule.Category.BOSS)return single(Skill.COMBAT);
        if(s.category()==GuideRule.Category.MONEY)return single(Skill.MONEY);
        if(s.category()==GuideRule.Category.GEAR)return single(Skill.GEAR);
        return single(Skill.GENERIC);
    }
    /** Avoid fake-precision: minimumMinutes is a suggested availability window, not completion ETA. */
    public static String timeLabel(SuggestionEngine.Suggestion s) {
        if(s.id().equals("plank_make")||s.id().equals("afk_plank_make"))return "~90s / inv";
        if(s.id().equals("bird_house_trapping")||s.id().equals("birdhouses"))return "~50m reset";
        return "WINDOW \u2265"+s.minimumMinutes()+"m";
    }
    public static String timeTooltip(SuggestionEngine.Suggestion s) {
        if(s.id().equals("plank_make")||s.id().equals("afk_plank_make"))return "Wiki example: ~90 seconds per auto-cast inventory; varies with banking";
        if(s.id().equals("bird_house_trapping")||s.id().equals("birdhouses"))return "Bird houses refill after ~50 minutes; this is not active run duration";
        return "Planning minimum from rule, not a verified completion-time estimate";
    }
}
