package gg.feral.burrow.runelite;

import gg.feral.burrow.core.BossRegistry;
import gg.feral.burrow.core.CollectionLogParser;
import java.util.*;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;

/** Build 20: maps tracker encounters to official HiScores boss entries. Unranked (-1) is UNKNOWN. */
final class HiscoreKc {
    private HiscoreKc() {}
    static Optional<HiscoreSkill> skillFor(String encounter) {
        for(HiscoreSkill s:HiscoreSkill.values()) {
            if(s.getType()!=HiscoreSkillType.BOSS) continue;
            String n=s.getName();
            if(n.equalsIgnoreCase(encounter) || CollectionLogParser.encounterName(n,n).equalsIgnoreCase(encounter)) return Optional.of(s);
        }
        return Optional.empty();
    }
    static HiscoreEndpoint endpointFor(int accountTypeVarbit) {
        return switch(accountTypeVarbit) {
            case 1 -> HiscoreEndpoint.IRONMAN;
            case 2 -> HiscoreEndpoint.ULTIMATE_IRONMAN;
            case 3 -> HiscoreEndpoint.HARDCORE_IRONMAN;
            default -> HiscoreEndpoint.NORMAL;
        };
    }
    static OptionalLong kc(HiscoreResult result, HiscoreSkill skill) {
        if(result==null) return OptionalLong.empty();
        try {
            var s=result.getSkill(skill);
            if(s==null || s.getLevel()<0) return OptionalLong.empty();
            return OptionalLong.of(s.getLevel());
        } catch(RuntimeException e) { return OptionalLong.empty(); }
    }
    static boolean registered(String encounter){return BossRegistry.find(encounter).isPresent();}
}
