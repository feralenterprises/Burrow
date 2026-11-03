package gg.feral.burrow.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

/**
 * PQA-W09: Strict, read-only parser for ONE currently visible OSRS Kill Log page.
 * A successful read is a partial page witness, never proof of the full account log.
 * No implied zeros, invented entity mapping, or automatic completion is permitted.
 *
 * RuneLite uses the KillLog interface for boss names/counts. Slayer title handling
 * is a guarded candidate until the actual target game revision is owner-observed.
 */
public final class KillLogPageParser {
    private KillLogPageParser() {}
    public enum Kind { BOSS, SLAYER }
    public record Observation(Kind kind, String pageId, Map<String, Long> exactCounts,
                              List<String> cappedUnknownRows, int visibleRowCount) {
        public Observation {
            Objects.requireNonNull(kind);
            if (pageId == null || pageId.isBlank()) throw new IllegalArgumentException("pageId");
            exactCounts = Map.copyOf(exactCounts);
            cappedUnknownRows = List.copyOf(cappedUnknownRows);
            if (visibleRowCount < 1 || visibleRowCount > 500) throw new IllegalArgumentException("rowCount");
        }
    }
    /** Null means no coherent, witnessed log page, NOT zero kills. */
    public static Optional<Observation> parse(String rawTitle, List<String> rawNames,
                                              List<String> rawCounts) {
        if (rawTitle == null || rawNames == null || rawCounts == null || rawNames.isEmpty() ||
            rawNames.size() > 500 || rawNames.size() != rawCounts.size()) return Optional.empty();
        String title = clean(rawTitle);
        Kind kind;
        if ("Boss Kill Log".equals(title)) kind = Kind.BOSS;
        else if ("Slayer Kill Log".equals(title)) kind = Kind.SLAYER;
        else return Optional.empty(); // do not infer from a generic or unrelated widget

        Map<String, Long> exact = new LinkedHashMap<>();
        List<String> capped = new ArrayList<>();
        Set<String> namesSeen = new HashSet<>();
        List<String> pageNames = new ArrayList<>();
        for (int i = 0; i < rawNames.size(); i++) {
            String name = clean(rawNames.get(i));
            String value = clean(rawCounts.get(i));
            if (name.endsWith(":")) name = name.substring(0, name.length()-1).trim();
            if (name.isBlank() || name.length() > 120 || value.isBlank() ||
                !namesSeen.add(name.toLowerCase(Locale.ROOT))) return Optional.empty();
            pageNames.add(name.toLowerCase(Locale.ROOT));
            // OSRS Slayer Log can cap visible counts as Lots!. This is unknown,
            // never an exact integer and never an inferred 65,535+ count.
            if (value.startsWith("Lots!")) {
                capped.add(name);
                continue;
            }
            if (!value.matches("(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)") || value.length() > 18)
                return Optional.empty();
            try {
                long parsed = Long.parseLong(value.replace(",", ""));
                if (parsed < 0) return Optional.empty();
                exact.put(name, parsed);
            } catch (NumberFormatException invalid) { return Optional.empty(); }
        }
        // Set-derived page identity must not change when kill counts increase.
        Collections.sort(pageNames);
        String pageId = "killlog:" + kind.name().toLowerCase(Locale.ROOT) + ":" + digest(String.join("\n",pageNames));
        return Optional.of(new Observation(kind,pageId,exact,capped,rawNames.size()));
    }
    private static String clean(String input) {
        if (input == null) return "";
        return input.replaceAll("<[^>]*>", "").replace('\u00a0', ' ').trim().replaceAll("\\s+", " ");
    }
    private static String digest(String input) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes,0,12);
        } catch (NoSuchAlgorithmException unavailable) { throw new AssertionError(unavailable); }
    }
}
