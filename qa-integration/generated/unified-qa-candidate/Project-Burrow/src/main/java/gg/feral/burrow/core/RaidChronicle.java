package gg.feral.burrow.core;

import java.util.*;

/**
 * CP-5 (BQA-R12-013, owner decision 2026-11-01 21:11 UTC): raid encounters are VISIBLE in Boss Chronicle for
 * discovery/reference, while the Outfitter stays limited to supported standalone bosses. Names are exactly the raid
 * entries on {@link BossRegistry#EXCLUDED}, which remains the Outfitter's hard exclusion list.
 *
 * <p>No loadouts, no Best in Bank, no Combat Achievement tips and no invented drop rates for raids. The Chronicle shows
 * the encounter, its raid, an explicit "Outfitter not supported" line and a Wiki link.
 */
public final class RaidChronicle {
    private RaidChronicle() {}
    public static final String UNSUPPORTED =
        "Outfitter not supported for this encounter. No raid loadouts planned in the near term.";
    public record Entry(String name, String raid, String wikiUrl) {}

    private static final String W = "https://oldschool.runescape.wiki/w/";
    private static final List<Entry> ALL = List.of(
        e("Chambers of Xeric", "Chambers of Xeric"), e("Tekton", "Chambers of Xeric"), e("Vanguard", "Chambers of Xeric"),
        e("Vespula", "Chambers of Xeric"), e("Vasa Nistirio", "Chambers of Xeric"), e("Muttadile", "Chambers of Xeric"),
        e("Great Olm", "Chambers of Xeric"),
        e("Theatre of Blood", "Theatre of Blood"), e("The Maiden of Sugadinti", "Theatre of Blood"),
        e("Pestilent Bloat", "Theatre of Blood"), e("Nylocas Vasilias", "Theatre of Blood"), e("Sotetseg", "Theatre of Blood"),
        e("Xarpus", "Theatre of Blood"), e("Verzik Vitur", "Theatre of Blood"),
        e("Tombs of Amascut", "Tombs of Amascut"), e("Akkha", "Tombs of Amascut"), e("Ba-Ba", "Tombs of Amascut"),
        e("Kephri", "Tombs of Amascut"), e("Zebak", "Tombs of Amascut"), e("Tumeken's Warden", "Tombs of Amascut"),
        e("Elidinis' Warden", "Tombs of Amascut"));
    private static Entry e(String name, String raid) { return new Entry(name, raid, W + name.replace(' ', '_')); }

    public static List<Entry> all() { return ALL; }
    public static List<String> names() { return ALL.stream().map(Entry::name).toList(); }
    public static Optional<Entry> find(String name) {
        if (name == null) return Optional.empty();
        return ALL.stream().filter(x -> x.name().equalsIgnoreCase(name.trim())).findFirst();
    }
    public static boolean isRaid(String name) { return find(name).isPresent(); }
}
