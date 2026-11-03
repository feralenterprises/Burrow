package gg.feral.burrow.experience;

import gg.feral.burrow.experience.NavigationState.*;

/** Everything a UI may ask the navigation reducer to do. */
public sealed interface NavigationCommand {
    record OpenTab(TopTab tab) implements NavigationCommand {}
    record OpenChronicleList() implements NavigationCommand {}
    record UpdateBrowse(BrowseMemory browse) implements NavigationCommand {}
    /** {@code scrollKey} is remembered so Back can restore the list. */
    record OpenBossDetail(String boss, String scrollKey) implements NavigationCommand {}
    record BackToList() implements NavigationCommand {}
    record CloseChronicle() implements NavigationCommand {}
    /** The only command that copies the Chronicle boss into Outfitter. */
    record OpenBossInOutfitter() implements NavigationCommand {}
    record SelectInOutfitter(OutfitterSelection selection) implements NavigationCommand {}
    /** Login, logout or account change: nothing from the previous account survives. */
    record AccountChanged() implements NavigationCommand {}
}
