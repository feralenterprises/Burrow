package gg.feral.burrow.intelligence;

import gg.feral.burrow.core.MiniJson;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * W01: read-only, versioned activity intelligence. This is intentionally not wired to the
 * live Best in Bank planner or bank observer. A reviewed fact is NOT a claim of ownership.
 * No account data is serialized here. No network calls are made.
 */
public final class IntelligenceCatalog {
    private IntelligenceCatalog() {}

    public enum Review { STAGING, CHECK, VERIFIED_SOURCE, REJECTED }
    public enum Role { REQUIRED, RECOMMENDED, OPTIONAL_UPGRADE }
    public enum Domain { GEAR, BOSS, CLUE, FARMING, SKILLING, TRANSPORT, COMBAT_ACHIEVEMENT }

    /** Source provenance is an audit assertion, NOT independent proof of Wiki accuracy. */
    public record Source(String url, String revision, String retrievedUtc, String evidencePointer) {
        public Source {
            url = Objects.requireNonNull(url);
            revision = Objects.requireNonNull(revision);
            retrievedUtc = Objects.requireNonNull(retrievedUtc);
            evidencePointer = Objects.requireNonNull(evidencePointer);
        }
        public boolean pinned() {
            if (!url.startsWith("https://") || revision.isBlank() || evidencePointer.isBlank()) return false;
            try { Instant.parse(retrievedUtc); return true; }
            catch (RuntimeException e) { return false; }
        }
    }

    public record Fact(String id, Domain domain, String activity, String subject,
                       String context, Role role, Review review, Source source) {
        public Fact {
            if (id == null || !id.matches("[A-Za-z0-9][A-Za-z0-9._:-]{2,95}"))
                throw new IllegalArgumentException("Invalid fact ID");
            Objects.requireNonNull(domain);
            activity = requireText(activity, "activity");
            subject = requireText(subject, "subject");
            context = requireText(context, "context");
            Objects.requireNonNull(role);
            Objects.requireNonNull(review);
            if (review == Review.VERIFIED_SOURCE && (source == null || !source.pinned()))
                throw new IllegalArgumentException("VERIFIED_SOURCE requires pinned provenance: " + id);
        }
        /** Even verified reference data does not imply an item is owned, equipped or usable. */
        public boolean eligibleForEvaluation() { return review == Review.VERIFIED_SOURCE; }
    }

    public record Snapshot(int schemaVersion, String packId, List<Fact> facts) {
        public Snapshot {
            if (schemaVersion != 1) throw new IllegalArgumentException("Unsupported intelligence schema");
            packId = requireText(packId, "pack ID");
            facts = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(facts)));
            Set<String> ids = new HashSet<>();
            for (Fact fact : facts) {
                if (!ids.add(fact.id())) throw new IllegalArgumentException("Duplicate intelligence ID: " + fact.id());
            }
        }
    }

    public static Snapshot bundled() throws IOException {
        String path = "/gg/feral/burrow/intelligence/wave1-catalog.json";
        try (InputStream stream = IntelligenceCatalog.class.getResourceAsStream(path)) {
            if (stream == null) throw new IOException("Missing bundled intelligence catalog: " + path);
            return parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @SuppressWarnings("unchecked")
    public static Snapshot parse(String json) {
        Object root = MiniJson.parse(Objects.requireNonNull(json));
        if (!(root instanceof Map<?,?> raw)) throw new IllegalArgumentException("Catalog root must be object");
        Map<String,Object> m = (Map<String,Object>)raw;
        if (!(m.get("schema_version") instanceof Number v) || v.intValue()!=1)
            throw new IllegalArgumentException("schema_version must be 1");
        String packId = value(m,"pack_id");
        if (!(m.get("facts") instanceof List<?> facts)) throw new IllegalArgumentException("facts must be array");
        List<Fact> out = new ArrayList<>();
        for (Object candidate : facts) {
            if (!(candidate instanceof Map<?,?> item)) throw new IllegalArgumentException("fact must be object");
            Map<String,Object> row = (Map<String,Object>)item;
            Source source = null;
            Object provenance = row.get("source");
            if (provenance != null) {
                if (!(provenance instanceof Map<?,?> sourceMap)) throw new IllegalArgumentException("source must be object");
                Map<String,Object> s = (Map<String,Object>)sourceMap;
                source = new Source(value(s,"url"),value(s,"revision"),value(s,"retrieved_utc"),value(s,"evidence_pointer"));
            }
            out.add(new Fact(value(row,"id"),enumValue(Domain.class,value(row,"domain")),
                value(row,"activity"),value(row,"subject"),value(row,"context"),
                enumValue(Role.class,value(row,"role")),
                enumValue(Review.class,value(row,"review")),source));
        }
        return new Snapshot(1,packId,out);
    }
    private static <E extends Enum<E>> E enumValue(Class<E> type, String raw) {
        try { return Enum.valueOf(type,raw); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid " + type.getSimpleName() + ": " + raw,e); }
    }
    private static String value(Map<String,Object> map,String key) {
        if (!(map.get(key) instanceof String s)) throw new IllegalArgumentException("Expected string " + key);
        return s;
    }
    private static String requireText(String s,String label) {
        if (s==null || s.isBlank()) throw new IllegalArgumentException("Blank " + label);
        return s;
    }
}
