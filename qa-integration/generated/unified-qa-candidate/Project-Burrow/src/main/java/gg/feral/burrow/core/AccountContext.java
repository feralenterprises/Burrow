package gg.feral.burrow.core;

import java.util.*;

/** Build 19: observed same-login real skill levels and quest states for requirement checks.
 *  Missing entries are UNKNOWN, never level 1 or NOT_STARTED. */
public record AccountContext(Map<String,Integer> realLevels, Map<String,String> questStates) {
    public enum Check { MET, UNMET, UNKNOWN }
    public static final AccountContext UNKNOWN = new AccountContext(Map.of(), Map.of());
    public AccountContext {
        realLevels=Map.copyOf(realLevels); questStates=Map.copyOf(questStates);
        for(var v:realLevels.values()) if(v==null || v<1 || v>99) throw new IllegalArgumentException("level");
    }
    public Check level(String skill,int minimum) {
        Integer v=realLevels.get(skill);
        return v==null?Check.UNKNOWN:v>=minimum?Check.MET:Check.UNMET;
    }
    public Integer levelOf(String skill) { return realLevels.get(skill); }
    /** Only names RuneLite reported (quest survey keys) are considered quests. */
    public boolean knowsQuest(String name) { return questStates.containsKey(name); }
    public Check quest(String name) {
        String s=questStates.get(name);
        if(s==null)return Check.UNKNOWN;
        return "FINISHED".equals(s)?Check.MET:Check.UNMET;
    }
}
