package gg.feral.burrow.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Boss Chronicle Combat Achievement reference. Kill-count tasks are left out on the owner's instruction, and raids
 * stay out with the rest of raid support. Requirements are short restatements and tips are written for Burrow from
 * the linked OSRS Wiki page. A task with no checked tip shows its requirement only. Pure Java; this class does not
 * claim a task is complete.
 */
public final class CombatTaskGuide {
    private CombatTaskGuide() {}

    public record Task(String boss, String tier, String type, String name, String requirement, String tip, String source) {
        public boolean hasTip() { return !tip.isEmpty(); }
    }

    /** The in-game task list has one Moons of Peril entry; every Moon shows the shared tasks plus its own. */
    private static final Set<String> MOONS = Set.of("Blood Moon", "Blue Moon", "Eclipse Moon");
    private static final String SHARED_MOON = "Blood Moon";
    private static final List<String> TIERS = List.of("Easy", "Medium", "Hard", "Elite", "Master", "Grandmaster");
    private static final Map<String, List<Task>> BY_BOSS = load();

    private static Map<String, List<Task>> load() {
        Map<String, List<Task>> out = new LinkedHashMap<>();
        try (InputStream in = CombatTaskGuide.class.getResourceAsStream("combat-tasks.tsv")) {
            if (in == null) throw new IOException("combat-tasks.tsv");
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList()) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] f = line.split("\t", -1);
                if (f.length != 7) throw new IllegalStateException("Bad combat task row: " + line);
                if (!TIERS.contains(f[1])) throw new IllegalStateException("Bad tier: " + line);
                out.computeIfAbsent(f[0], k -> new ArrayList<>()).add(new Task(f[0], f[1], f[2], f[3], f[4], f[5], f[6]));
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        for (var tasks : out.values()) tasks.sort(Comparator.comparingInt(t -> TIERS.indexOf(t.tier())));
        return Map.copyOf(out);
    }

    /** Tasks for one Chronicle boss, easiest tier first. Empty for bosses with no non-kill-count task and for raids. */
    public static List<Task> forBoss(String boss) {
        if (boss == null) return List.of();
        List<Task> own = BY_BOSS.getOrDefault(boss, List.of());
        if (!MOONS.contains(boss) || boss.equals(SHARED_MOON)) return List.copyOf(own);
        List<Task> all = new ArrayList<>(BY_BOSS.getOrDefault(SHARED_MOON, List.of()));
        all.addAll(own);
        all.sort(Comparator.comparingInt(t -> TIERS.indexOf(t.tier())));
        return List.copyOf(all);
    }

    public static Set<String> bosses() { return new TreeSet<>(BY_BOSS.keySet()); }
    public static int taskCount() { return BY_BOSS.values().stream().mapToInt(List::size).sum(); }
}
