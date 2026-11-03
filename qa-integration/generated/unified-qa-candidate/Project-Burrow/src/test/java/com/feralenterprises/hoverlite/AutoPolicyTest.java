package com.feralenterprises.hoverlite;

import org.junit.Test;
import static org.junit.Assert.*;
import net.runelite.api.MenuAction;

/** Approved examples + negative semantic/collision guards, not full research-catalog certification. */
public class AutoPolicyTest
{
    private String show(HoverCategory kind, String action, String target)
    {
        return HoverText.format(HoverMode.SMART_SHORTEN, kind, action, target);
    }

    @Test public void ownerDisplayIntentNpcLevels()
    {
        String[][] examples = {
            {"Attack", "Imp (level-2)", "Attack Imp"},
            {"Attack", "Goblin (level-2)", "Attack Goblin"},
            {"Attack", "Lizardman brute (level-75)", "Attack Lizardman brute"},
            {"Feed", "Duke Sucellus (level-758)", "Feed Duke Sucellus"},
            {"Attack", "Duke Sucellus (level-758)", "Attack Duke Sucellus"},
            {"Attack", "Tormented demon (level-450)", "Attack Tormented demon"},
            {"Talk-to", "Banker", "Talk Banker"},
            {"Milk", "Dairy cow", "Milk Dairy cow"},
            {"Trade-with", "Banker", "Trade-with Banker"},
            {"Bank", "Banker", "Bank Banker"},
            {"Collect", "Banker", "Collect Banker"},
            {"Assignment", "Konar", "Assignment Konar"},
            {"Rewards", "Konar", "Rewards Konar"},
            {"Pay (north-east)", "Marisi", "Pay (north-east) Marisi"},
            {"Pay (south-west)", "Marisi", "Pay (south-west) Marisi"},
        };
        for (String[] sample : examples)
        {
            assertEquals(sample[0] + " / " + sample[1], sample[2], show(HoverCategory.NPC, sample[0], sample[1]));
        }
    }

    @Test public void knownWorldVerbsAndProtectedStates()
    {
        String[][] examples = {
            {"Chop down", "Oak tree", "Chop Oak"},
            {"Chop down", "Willow tree", "Chop Willow"},
            {"Chop down", "Magic tree", "Chop Magic"},
            {"Chop down", "Tree", "Chop Tree"},
            {"Chop down", "Ladder", "Chop down Ladder"},
            {"Chop down", "Fruit tree", "Chop down Fruit tree"},
            {"Chop down", "Celestial tree", "Chop down Celestial tree"},
            {"Chop-down", "Oak tree", "Chop-down Oak tree"},
            {"Pray-at", "Altar", "Pray Altar"},
            {"Pray-at", "Chaos altar", "Pray-at Chaos altar"},
            {"Drink", "Fiery Ornate Pool of Rejuvenation", "Drink Fiery Ornate Pool of Rejuvenation"},
            {"Enter", "Weiss Portal", "Enter Weiss Portal"},
            {"Climb-up", "Ladder", "Climb-up Ladder"},
            {"Climb-down", "Ladder", "Climb-down Ladder"},
            {"Climb-over", "Wall", "Climb-over Wall"},
            {"Open", "Gate", "Open Gate"},
            {"Close", "Gate", "Close Gate"},
            {"Search", "Chest", "Search Chest"},
            {"Check", "Chest", "Check Chest"},
            {"Check-health", "Fruit tree", "Check-health Fruit tree"},
            {"Enter", "Wilderness", "Enter Wilderness"},
            {"Exit", "Wilderness", "Exit Wilderness"},
            {"Last-destination", "Portal", "Last-destination Portal"},
            {"Deposit-runes", "Storage", "Deposit-runes Storage"},
            {"Deposit-items", "Storage", "Deposit-items Storage"},
            {"Use", "Ornate pool", "Use Ornate pool"},
        };
        for (String[] sample : examples)
        {
            assertEquals(sample[0] + " / " + sample[1], sample[2], show(HoverCategory.GAME_OBJECT, sample[0], sample[1]));
        }
    }

    @Test public void allOtherCategoriesAreIdentityPreserving()
    {
        HoverCategory[] categories = {HoverCategory.INVENTORY_ITEM, HoverCategory.GROUND_ITEM,
            HoverCategory.PLAYER, HoverCategory.INTERFACE, HoverCategory.OTHER};
        String[][] examples = {
            {"Eat", "Shark"}, {"Withdraw-1", "Shark"}, {"Withdraw-All", "Shark"},
            {"Take", "Coins (1000)"}, {"Drink", "Super restore(4)"},
            {"Chop down", "Oak tree"}, {"Attack", "Imp (level-2)"},
            {"Talk-to", "Banker"}, {"Pray-at", "Altar"}
        };
        for (HoverCategory kind : categories)
            for (String[] e : examples)
                assertEquals(kind + " " + e[0], e[0] + " " + e[1], show(kind, e[0], e[1]));
    }

    @Test public void colorAndMalformedMarkupGuard()
    {
        assertEquals("Attack <col=ffff00>Goblin</col>",
            show(HoverCategory.NPC, "Attack", "<col=ffff00>Goblin</col> (level-2)"));
        assertEquals("Attack <col=ffff00>Goblin</col>",
            show(HoverCategory.NPC, "Attack", "<col=ffff00>Goblin (level-2)</col>"));
        assertEquals("Attack Goblin (level-2) (level-5)",
            show(HoverCategory.NPC, "Attack", "Goblin (level-2) (level-5)"));
        assertEquals("Attack <img=1><col=ffff00>Goblin</col> (level-2)",
            show(HoverCategory.NPC, "Attack", "<img=1><col=ffff00>Goblin</col> (level-2)"));
        assertEquals("Attack <col=zzzzzz>Goblin</col> (level-2)",
            show(HoverCategory.NPC, "Attack", "<col=zzzzzz>Goblin</col> (level-2)"));
        assertEquals("Chop down <img=1>Oak tree", show(HoverCategory.GAME_OBJECT, "Chop down", "<img=1>Oak tree"));
        assertEquals("Attack (level-2)", show(HoverCategory.NPC, "Attack", "(level-2)"));
    }

    @Test public void protectSelectedItemAndSpellMenuEntries()
    {
        assertFalse(AutoShortenSafety.allows(MenuAction.ITEM_USE_ON_NPC));
        assertFalse(AutoShortenSafety.allows(MenuAction.WIDGET_TARGET_ON_NPC));
        assertFalse(AutoShortenSafety.allows(MenuAction.ITEM_USE_ON_GAME_OBJECT));
        assertFalse(AutoShortenSafety.allows(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT));
        assertFalse(AutoShortenSafety.allows(MenuAction.CC_OP));
        assertFalse(AutoShortenSafety.allows(MenuAction.RUNELITE_OVERLAY));
        assertTrue(AutoShortenSafety.allows(MenuAction.NPC_FIRST_OPTION));
        assertTrue(AutoShortenSafety.allows(MenuAction.GAME_OBJECT_FIRST_OPTION));
    }

    @Test public void npcLevelOnlyCollisionMustNotRestoreLevel()
    {
        assertTrue(TooltipCollisionGuard.isAmbiguous(
            "Attack Undead one (level-61)", "Attack Undead one",
            "Attack Undead one (level-73)", "Attack Undead one"));
        assertTrue(TooltipCollisionGuard.onlyNpcCombatLevelsDiffer(HoverCategory.NPC,
            "Attack", "<col=ffff00>Undead one<col=00ff00> (level-61)", HoverCategory.NPC,
            "Attack", "<col=ffff00>Undead one<col=00ff00> (level-73)"));
        assertFalse(TooltipCollisionGuard.onlyNpcCombatLevelsDiffer(HoverCategory.NPC,
            "Attack", "Undead one (level-73)", HoverCategory.NPC, "Examine", "Undead one (level-61)"));
        assertFalse(TooltipCollisionGuard.onlyNpcCombatLevelsDiffer(HoverCategory.NPC,
            "Attack", "Undead one (level-73)", HoverCategory.NPC, "Attack", "Guard (level-61)"));
        assertFalse(TooltipCollisionGuard.onlyNpcCombatLevelsDiffer(HoverCategory.NPC,
            "Attack", "Undead one (level-73)", HoverCategory.GAME_OBJECT, "Attack", "Undead one (level-61)"));
        assertFalse(TooltipCollisionGuard.onlyNpcCombatLevelsDiffer(HoverCategory.NPC,
            "Attack", "Undead one (level-73) (level-74)", HoverCategory.NPC, "Attack", "Undead one (level-61)"));
    }

    @Test public void collisionsAndIdempotence()
    {
        assertTrue(TooltipCollisionGuard.isAmbiguous(
            "Attack Guard (level-21)", "Attack Guard", "Attack Guard (level-22)", "Attack Guard"));
        assertTrue(TooltipCollisionGuard.isAmbiguous(
            "Chop down Oak tree", "Chop Oak", "Chop Oak tree", "Chop Oak"));
        assertFalse(TooltipCollisionGuard.isAmbiguous(
            "Attack <col=ffff00>Guard (level-21)</col>", "Attack Guard", "Attack <col=ff0000>Guard (level-21)</col>", "Attack Guard"));
        for (String name : new String[] {"Goblin (level-2)", "Guard (level-21)",
            "Duke Sucellus (level-758)", "Goblin (level-2) (level-5)", "King Black Dragon"})
        {
            final String once = AutoShorten.shorten(HoverCategory.NPC, name);
            assertEquals(once, AutoShorten.shorten(HoverCategory.NPC, once));
        }
    }


    @Test public void actualRuneLiteUnclosedColorTags()
    {
        assertEquals("Attack <col=ffff00>Imp", show(HoverCategory.NPC, "Attack",
            "<col=ffff00>Imp<col=00ff00> (level-2)"));
        assertEquals("Attack <col=ffff00>Imp", show(HoverCategory.NPC, "Attack",
            "<col=ffff00>Imp (level-2)"));
        assertEquals("Attack <col=ffff00>Imp</col>", show(HoverCategory.NPC, "Attack",
            "<col=ffff00>Imp</col><col=00ff00> (level-2)</col>"));
        assertEquals("Chop <col=00ffff>Oak", show(HoverCategory.GAME_OBJECT, "Chop down",
            "<col=00ffff>Oak tree"));
        assertEquals("Pick <col=00ffff>Whiteberry bush", show(HoverCategory.GAME_OBJECT, "Pick-from",
            "<col=00ffff>Whiteberry bush"));
        assertEquals("Pick <col=00ffff>Cactus", show(HoverCategory.GAME_OBJECT, "Pick-spine",
            "<col=00ffff>Cactus"));
        assertEquals("Pray <col=00ffff>Altar", show(HoverCategory.GAME_OBJECT, "Pray-at",
            "<col=00ffff>Altar"));
        assertEquals("Chop down <img=1>Oak tree", show(HoverCategory.GAME_OBJECT, "Chop down", "<img=1>Oak tree"));
    }

    @Test public void endToEndMenuCategoryDefaultAndColoredText()
    {
        net.runelite.api.MenuEntry obj = (net.runelite.api.MenuEntry) java.lang.reflect.Proxy.newProxyInstance(
            net.runelite.api.MenuEntry.class.getClassLoader(),
            new Class<?>[]{net.runelite.api.MenuEntry.class},
            (proxy, method, args) -> {
                if (method.getName().equals("getType")) return MenuAction.GAME_OBJECT_FIRST_OPTION;
                if (method.getName().equals("getParam1")) return 0;
                throw new UnsupportedOperationException(method.getName());
            });
        HoverCategory kind = HoverCategory.from(obj);
        HoverMode mode = HoverModePolicy.resolve(kind, HoverMode.FULL, true);
        assertEquals(HoverCategory.GAME_OBJECT, kind);
        assertEquals("Chop <col=00ffff>Oak", HoverText.format(mode, kind,
            "Chop down", "<col=00ffff>Oak tree"));

        net.runelite.api.MenuEntry npc = (net.runelite.api.MenuEntry) java.lang.reflect.Proxy.newProxyInstance(
            net.runelite.api.MenuEntry.class.getClassLoader(),
            new Class<?>[]{net.runelite.api.MenuEntry.class},
            (proxy, method, args) -> {
                if (method.getName().equals("getType")) return MenuAction.NPC_FIRST_OPTION;
                if (method.getName().equals("getParam1")) return 0;
                throw new UnsupportedOperationException(method.getName());
            });
        kind = HoverCategory.from(npc);
        mode = HoverModePolicy.resolve(kind, HoverMode.ACTION_ONLY, true);
        assertEquals("Attack <col=ffff00>Imp", HoverText.format(mode, kind,
            "Attack", "<col=ffff00>Imp<col=00ff00> (level-2)"));
    }

    @Test public void newMasterAutoDefaultOverridesExistingPreferences()
    {
        assertEquals(HoverMode.SMART_SHORTEN,
            HoverModePolicy.resolve(HoverCategory.NPC, HoverMode.ACTION_ONLY, true));
        assertEquals(HoverMode.SMART_SHORTEN,
            HoverModePolicy.resolve(HoverCategory.GAME_OBJECT, HoverMode.FULL, true));
        assertEquals(HoverMode.ACTION_ONLY,
            HoverModePolicy.resolve(HoverCategory.NPC, HoverMode.ACTION_ONLY, false));
        assertEquals(HoverMode.FULL,
            HoverModePolicy.resolve(HoverCategory.INVENTORY_ITEM, HoverMode.FULL, true));
    }

    @Test public void screenshotExamplesAndProtectedVerbs()
    {
        assertEquals("Pick Whiteberry bush", show(HoverCategory.GAME_OBJECT, "Pick-from", "Whiteberry bush"));
        assertEquals("Pick Cactus", show(HoverCategory.GAME_OBJECT, "Pick-spine", "Cactus"));
        assertEquals("Pick-spine Potato cactus", show(HoverCategory.GAME_OBJECT, "Pick-spine", "Potato cactus"));
        assertEquals("Exchange Leprechaun", show(HoverCategory.NPC, "Exchange", "Tool Leprechaun"));
        assertEquals("Open Vault", show(HoverCategory.GAME_OBJECT, "Open", "Vault"));
        assertEquals("Drop Tool", show(HoverCategory.INVENTORY_ITEM, "Drop", "Tool"));
        assertEquals("Remove plant", show(HoverCategory.GAME_OBJECT, "Remove", "plant"));
        assertEquals("Climb-down Ladder", show(HoverCategory.GAME_OBJECT, "Climb-down", "Ladder"));
    }

    @Test public void existingModesStillBehave()
    {
        assertEquals("Talk-to Banker", HoverText.format(HoverMode.FULL, HoverCategory.NPC, "Talk-to", "Banker"));
        assertEquals("Talk-to", HoverText.format(HoverMode.ACTION_ONLY, HoverCategory.NPC, "Talk-to", "Banker"));
        assertEquals("<col=65eeb0>●</col>", HoverText.format(HoverMode.INDICATOR, HoverCategory.NPC, "Talk-to", "Banker"));
        assertNull(HoverText.format(HoverMode.HIDDEN, HoverCategory.NPC, "Talk-to", "Banker"));
    }

    @Test public void exactOwnerTargetsAndColorPreservation()
    {
        assertEquals("Chop Oak", show(HoverCategory.GAME_OBJECT, "Chop down", "Oak tree"));
        assertEquals("Chop <col=00ffff>Oak", show(HoverCategory.GAME_OBJECT, "Chop down", "<col=00ffff>Oak tree"));
        assertEquals("Chop <col=00ffff>Oak</col>", show(HoverCategory.GAME_OBJECT, "Chop down", "<col=00ffff>Oak tree</col>"));
        assertEquals("Pick Limpwurt", show(HoverCategory.GAME_OBJECT, "Pick", "Limpwurt plant"));
        assertEquals("Pick <col=00ffff>Limpwurt", show(HoverCategory.GAME_OBJECT, "Pick", "<col=00ffff>Limpwurt plant"));
        assertEquals("Open Vault", show(HoverCategory.GAME_OBJECT, "Open", "Seed vault"));
        assertEquals("Open <col=00ffff>Vault", show(HoverCategory.GAME_OBJECT, "Open", "<col=00ffff>Seed vault"));
        assertEquals("Exchange Leprechaun", show(HoverCategory.NPC, "Exchange", "Tool Leprechaun"));
        assertEquals("Exchange <col=ffff00>Leprechaun", show(HoverCategory.NPC, "Exchange", "<col=ffff00>Tool Leprechaun"));
        // Safety: never remove words from unapproved actions or similarly named targets.
        assertEquals("Examine Oak tree", show(HoverCategory.GAME_OBJECT, "Examine", "Oak tree"));
        assertEquals("Chop down Spirit tree", show(HoverCategory.GAME_OBJECT, "Chop down", "Spirit tree"));
        assertEquals("Open Bank vault", show(HoverCategory.GAME_OBJECT, "Open", "Bank vault"));
        assertEquals("Exchange <img=1>Tool Leprechaun", show(HoverCategory.NPC, "Exchange", "<img=1>Tool Leprechaun"));
        assertEquals("Pick Limpwurt plant", show(HoverCategory.INVENTORY_ITEM, "Pick", "Limpwurt plant"));
    }

    @Test public void unbalancedColorLevelIsNotGuessed()
    {
        assertEquals("Attack Ram<col=00ff00> (level-2)",
            show(HoverCategory.NPC, "Attack", "Ram<col=00ff00> (level-2)"));
        assertEquals("Attack <col=ffff00>Undead one",
            show(HoverCategory.NPC, "Attack", "<col=ffff00>Undead one<col=00ff00> (level-73)"));
        assertEquals("Cross Stepping stone",
            show(HoverCategory.GAME_OBJECT, "Cross", "Stepping stone"));
        assertEquals("Build Inconspicuous crate (hard)",
            show(HoverCategory.GAME_OBJECT, "Build", "Inconspicuous crate (hard)"));
    }
}
