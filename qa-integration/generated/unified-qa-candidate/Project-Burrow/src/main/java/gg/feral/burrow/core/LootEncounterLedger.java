package gg.feral.burrow.core;

import java.util.*;

/**
 * Build 20.1: READ-ONLY "loot-confirmed encounters this login" counter fed by RuneLite's built-in
 * Loot Tracker {@code LootReceived} event.
 *
 * <p>This is an ESTIMATE, never kill count. It is a separate number from:
 * <ol>
 *   <li>authoritative total KC (game "kill count is" message, observed Collection Log, official
 *       HiScores), and</li>
 *   <li>verified this-login KC delta (accepted absolute KC changes).</li>
 * </ol>
 * It is never added to either of them and never written to the persistent Rare Drop store.
 *
 * <p>Rules:
 * <ul>
 *   <li>Only {@code NPC}-type records whose exact NPC name is on a vetted list where one loot
 *       event corresponds to one kill of one boss. Chests, events, players, pickpockets,
 *       multi-entity rewards and unlisted names are LOOT_ONLY (counted nowhere).</li>
 *   <li>One event = +1 encounter regardless of how many item stacks it carries.</li>
 *   <li>An event that reports {@code amount != 1} is ambiguous and not counted.</li>
 *   <li>An event whose item stacks are all empty/invalid is not counted.</li>
 *   <li>The same event object, or an equal event on the same game tick (a duplicate subscriber
 *       or re-post), is counted once.</li>
 *   <li>State belongs to one login scope. A different scope (logout/login, A→B→A) starts at
 *       zero; LOADING / world hops keep the same scope and therefore the same counts.</li>
 * </ul>
 */
public final class LootEncounterLedger {
    public enum RecordType { NPC, PLAYER, EVENT, PICKPOCKET, UNKNOWN }
    public enum Outcome { COUNTED, DUPLICATE, NO_LOGIN, NOT_NPC, NOT_VETTED, AMBIGUOUS_AMOUNT, NO_ITEMS }
    /** One copied Loot Tracker event. {@code identity} is the original event object (never retained after scope change). */
    public record Observation(Object identity, String name, RecordType type, int amount, int tick,
                              List<int[]> itemStacks) {
        public Observation { itemStacks = itemStacks == null ? List.of() : List.copyOf(itemStacks); }
    }

    /**
     * Exact (lower-case) NPC names where one Loot Tracker NPC record is one kill of one boss,
     * mapped to the tracker/registry encounter name. Excluded on purpose: Grotesque Guardians
     * (Dusk/Dawn pair), Barrows, Moons (Lunar Chest), Royal Titans (two titans), The Nightmare,
     * Phosani's Nightmare, Nex, Yama, Doom of Mokhaiotl, The Mimic, Hueycoatl (contribution
     * scaled group fight), Araxxor (destroy option gives no loot), Maggot King (two loot options),
     * Gemstone Crab, Hespori, Mad Angel (name not vetted).
     */
    private static final Map<String,String> VETTED = Map.ofEntries(
        Map.entry("tormented demon", "Tormented Demons"),
        Map.entry("demonic gorilla", "Demonic Gorillas"),
        Map.entry("lizardman shaman", "Lizardman Shamans"),
        Map.entry("scurrius", "Scurrius"),
        Map.entry("brutus", "Brutus"),
        Map.entry("giant mole", "Giant Mole"),
        Map.entry("vorkath", "Vorkath"),
        Map.entry("zulrah", "Zulrah"),
        Map.entry("phantom muspah", "Phantom Muspah"),
        Map.entry("sarachnis", "Sarachnis"),
        Map.entry("amoxliatl", "Amoxliatl"),
        Map.entry("obor", "Obor"),
        Map.entry("bryophyta", "Bryophyta"),
        Map.entry("dagannoth rex", "Dagannoth Rex"),
        Map.entry("dagannoth prime", "Dagannoth Prime"),
        Map.entry("dagannoth supreme", "Dagannoth Supreme"),
        Map.entry("kalphite queen", "Kalphite Queen"),
        Map.entry("corporeal beast", "Corporeal Beast"),
        Map.entry("general graardor", "General Graardor"),
        Map.entry("kree'arra", "Kree'arra"),
        Map.entry("commander zilyana", "Commander Zilyana"),
        Map.entry("k'ril tsutsaroth", "K'ril Tsutsaroth"),
        Map.entry("cerberus", "Cerberus"),
        Map.entry("alchemical hydra", "Alchemical Hydra"),
        Map.entry("kraken", "Kraken"),
        Map.entry("thermonuclear smoke devil", "Thermonuclear smoke devil"),
        Map.entry("abyssal sire", "Abyssal Sire"),
        Map.entry("skotizo", "Skotizo"),
        Map.entry("shellbane gryphon", "Shellbane gryphon"),
        Map.entry("deranged archaeologist", "Deranged archaeologist"),
        Map.entry("duke sucellus", "Duke Sucellus"),
        Map.entry("vardorvis", "Vardorvis"),
        Map.entry("the leviathan", "The Leviathan"),
        Map.entry("the whisperer", "The Whisperer"),
        Map.entry("king black dragon", "King Black Dragon"),
        Map.entry("callisto", "Callisto"),
        Map.entry("artio", "Artio"),
        Map.entry("venenatis", "Venenatis"),
        Map.entry("spindel", "Spindel"),
        Map.entry("vet'ion", "Vet'ion"),
        Map.entry("calvar'ion", "Calvar'ion"),
        Map.entry("scorpia", "Scorpia"),
        Map.entry("chaos elemental", "Chaos Elemental"),
        Map.entry("chaos fanatic", "Chaos Fanatic"),
        Map.entry("crazy archaeologist", "Crazy archaeologist"));

    public static Optional<String> vettedEncounter(String npcName) {
        if (npcName == null) return Optional.empty();
        String n = npcName.replaceAll("<[^>]*>", "").trim().toLowerCase(Locale.ROOT);
        return Optional.ofNullable(VETTED.get(n));
    }
    public static Set<String> vettedEncounters() { return Set.copyOf(VETTED.values()); }

    private Object scope;
    private final Map<String,Integer> counts = new LinkedHashMap<>();
    private final Set<Object> seenIdentity = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<List<Object>> seenSignature = new HashSet<>();

    /** Binds to a login scope; a different scope (or null) discards everything from the old one. */
    public void bind(Object loginScope) {
        if (Objects.equals(scope, loginScope)) return;
        scope = loginScope; counts.clear(); seenIdentity.clear(); seenSignature.clear();
    }
    public Object scope() { return scope; }

    public Outcome accept(Object loginScope, Observation o) {
        if (scope == null || loginScope == null || !scope.equals(loginScope)) return Outcome.NO_LOGIN;
        if (o == null || o.type() != RecordType.NPC) return Outcome.NOT_NPC;
        var enc = vettedEncounter(o.name());
        if (enc.isEmpty()) return Outcome.NOT_VETTED;
        if (o.amount() != 1) return Outcome.AMBIGUOUS_AMOUNT;
        List<List<Integer>> stacks = new ArrayList<>();
        for (int[] s : o.itemStacks()) if (s != null && s.length >= 2 && s[0] > 0 && s[1] > 0) stacks.add(List.of(s[0], s[1]));
        if (stacks.isEmpty()) return Outcome.NO_ITEMS;
        if (o.identity() != null && !seenIdentity.add(o.identity())) return Outcome.DUPLICATE;
        stacks.sort(Comparator.<List<Integer>>comparingInt(l -> l.get(0)).thenComparingInt(l -> l.get(1)));
        List<Object> signature = List.of(enc.get(), o.tick(), stacks);
        if (!seenSignature.add(signature)) return Outcome.DUPLICATE;
        counts.merge(enc.get(), 1, Integer::sum);
        return Outcome.COUNTED;
    }
    public Map<String,Integer> counts() { return Map.copyOf(counts); }
    public int count(String encounter) { return counts.getOrDefault(encounter, 0); }
}
