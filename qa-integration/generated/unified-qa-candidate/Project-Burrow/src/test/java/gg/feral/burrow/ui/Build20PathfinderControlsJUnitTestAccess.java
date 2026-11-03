package gg.feral.burrow.ui;

import gg.feral.burrow.pathfinder.SuggestionEngine.*;
import java.util.*;

final class Build20PathfinderControlsJUnitTestAccess {
    static Snapshot account() {
        Map<String,Integer> skills=new HashMap<>();
        for(String s:List.of("Attack","Strength","Defence","Ranged","Magic","Hitpoints","Prayer","Slayer","Fishing",
            "Woodcutting","Mining","Agility","Thieving","Farming","Herblore","Cooking","Firemaking","Crafting",
            "Smithing","Fletching","Hunter","Construction","Runecraft")) skills.put(s,70);
        return new Snapshot(true,skills,Map.of(),BankStatus.UNAVAILABLE,Set.of());
    }
}
