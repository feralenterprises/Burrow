package gg.feral.burrow.core;

import java.util.*;
import java.util.function.Predicate;

/**
 * BQA UI-01: human-readable name for the temporary O-button Bank Tags view.
 * Replaces the raw {@code burrow-outfitter-<UUID>} the owner saw in the bank title.
 * Lower-case letters, digits and single spaces only (Bank Tags compares tag names case-insensitively),
 * at most {@value #MAX} characters, never colliding with a tag/layout the player already saved.
 */
public final class BankTabTitle {
    public static final int MAX = 30;
    private BankTabTitle() {}

    public static String base(String boss) {
        String b = boss == null ? "" : boss.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim().replaceAll(" +", " ");
        String s = b.isEmpty() ? "trashpanda gear" : "trashpanda " + b;
        return s.length() <= MAX ? s : s.substring(0, MAX).trim();
    }

    /** First free name: "trashpanda vorkath", then "... 2" ... "... 9"; empty if all are taken. */
    public static Optional<String> available(String boss, Predicate<String> inUse) {
        String base = base(boss);
        if (!inUse.test(base)) return Optional.of(base);
        for (int i = 2; i <= 9; i++) {
            String root = base.length() + 2 > MAX ? base.substring(0, MAX - 2).trim() : base;
            String c = root + " " + i;
            if (!inUse.test(c)) return Optional.of(c);
        }
        return Optional.empty();
    }
    /** O layouts own a temporary custom filter, so use an unpredictable per-open name.
     * Do not override another plugin's registered custom tag with the readable base name.
     * Names remain legible and within the native Bank Tags length limit. */
    public static Optional<String> temporary(String boss, Predicate<String> inUse) {
        String root=base(boss);
        for (int attempt=0;attempt<9;attempt++) {
            String nonce=UUID.randomUUID().toString().substring(0,8);
            int maxRoot=MAX-1-nonce.length();
            String prefix=root.length()>maxRoot?root.substring(0,maxRoot).trim():root;
            String name=prefix+" "+nonce;
            if (!inUse.test(name)) return Optional.of(name);
        }
        return Optional.empty();
    }

}
