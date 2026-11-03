package gg.feral.burrow.intelligence;

import gg.feral.burrow.core.MiniJson;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * W02: read-only exact-item-ID gear intelligence catalog.
 * Source snapshots are proposals, not approved gameplay rules. No bank observation,
 * RuneLite client, account data or existing Best in Bank code is accessed.
 */
public final class GearKnowledge {
    private GearKnowledge() {}

    public enum Slot { HEAD, CAPE, NECK, AMMO, WEAPON, BODY, SHIELD, LEGS, HANDS, FEET, RING, NONE, UNKNOWN }
    public enum Hands { ONE, TWO, UNKNOWN, NOT_WEAPON }
    public enum RelationKind { AMMO_COMPATIBLE, AMMO_INCOMPATIBLE, CONTEXTUAL_ALTERNATIVE,
        MECHANIC_DIFFERENCE, COSMETIC_PEER, DIRECTED_UPGRADE }

    /** A URL with revision="UNKNOWN" is never a verified source; require immutable source identity. */
    static boolean immutableSource(IntelligenceCatalog.Source source) {
        return source != null && source.pinned() &&
            (source.revision().matches("oldid=[0-9]+") ||
             source.revision().matches("git:[0-9a-fA-F]{40}"));
    }

    public record Item(int itemId, String name, String family, Slot slot, Hands hands,
                       String godAlignment, Map<String,Integer> minimumSkills,
                       Set<String> requiredStateFlags,
                       IntelligenceCatalog.Review review, IntelligenceCatalog.Source source,
                       List<String> researchLanes, List<String> researchUrls,
                       boolean identityConflict) {
        public Item {
            if (itemId <= 0) throw new IllegalArgumentException("Invalid item ID");
            name = notBlank(name, "name");
            family = notBlank(family, "family");
            Objects.requireNonNull(slot);
            Objects.requireNonNull(hands);
            godAlignment = Objects.requireNonNull(godAlignment).toUpperCase(java.util.Locale.ROOT);
            Objects.requireNonNull(review);
            minimumSkills = Map.copyOf(Objects.requireNonNull(minimumSkills));
            for (var row : minimumSkills.entrySet()) {
                if (row.getKey() == null || row.getKey().isBlank() || row.getValue() == null || row.getValue() < 1 || row.getValue() > 99)
                    throw new IllegalArgumentException("Invalid skill requirement for " + itemId);
            }
            requiredStateFlags = Set.copyOf(Objects.requireNonNull(requiredStateFlags));
            researchLanes = List.copyOf(Objects.requireNonNull(researchLanes));
            researchUrls = List.copyOf(Objects.requireNonNull(researchUrls));
            if (review == IntelligenceCatalog.Review.VERIFIED_SOURCE &&
                (source == null || !immutableSource(source)))
                throw new IllegalArgumentException("Verified gear needs pinned evidence: " + itemId);
            if (slot != Slot.WEAPON && hands != Hands.NOT_WEAPON && hands != Hands.UNKNOWN)
                throw new IllegalArgumentException("Non-weapon cannot claim weapon handedness: " + itemId);
        }
        public boolean approved() { return review == IntelligenceCatalog.Review.VERIFIED_SOURCE && source != null && immutableSource(source); }
    }

    /** Relations are DIRECTIONAL. A shared name, stat total or family does not create an edge. */
    public record Relation(String id, int fromId, int toId, RelationKind kind,
                           String requiredContext, IntelligenceCatalog.Review review,
                           IntelligenceCatalog.Source source, List<String> researchLanes) {
        public Relation {
            id = notBlank(id, "relation ID");
            if (fromId <= 0 || toId <= 0 || fromId == toId) throw new IllegalArgumentException("Bad relation endpoints: " + id);
            Objects.requireNonNull(kind);
            requiredContext = Objects.requireNonNull(requiredContext);
            Objects.requireNonNull(review);
            researchLanes = List.copyOf(Objects.requireNonNull(researchLanes));
            if (review == IntelligenceCatalog.Review.VERIFIED_SOURCE && (source == null || !immutableSource(source)))
                throw new IllegalArgumentException("Verified relation needs pinned evidence: " + id);
        }
        public boolean approved() { return review == IntelligenceCatalog.Review.VERIFIED_SOURCE && source != null && immutableSource(source); }
    }

    public record Pack(int schemaVersion, String packId, List<Item> items, List<Relation> relations) {
        public Pack {
            if (schemaVersion != 2) throw new IllegalArgumentException("Expected Wave 2 schema version 2");
            packId = notBlank(packId, "pack ID");
            items = List.copyOf(Objects.requireNonNull(items));
            relations = List.copyOf(Objects.requireNonNull(relations));
            Set<Integer> ids = new HashSet<>();
            for (Item item : items)
                if (!ids.add(item.itemId())) throw new IllegalArgumentException("Duplicate exact item ID " + item.itemId());
            Set<String> edgeIds = new HashSet<>();
            for (Relation relation : relations) {
                if (!edgeIds.add(relation.id())) throw new IllegalArgumentException("Duplicate relation " + relation.id());
                if (!ids.contains(relation.fromId()) || !ids.contains(relation.toId()))
                    throw new IllegalArgumentException("Unknown relation endpoint: " + relation.id());
            }
        }
        public Item find(int itemId) {
            for (Item item: items) if (item.itemId() == itemId) return item;
            return null;
        }
        public Relation relation(int fromId, int toId, RelationKind kind) {
            for (Relation rel: relations)
                if (rel.fromId()==fromId && rel.toId()==toId && rel.kind()==kind) return rel;
            return null;
        }
        public long approvedItemCount() { return items.stream().filter(Item::approved).count(); }
        public long approvedRelationCount() { return relations.stream().filter(Relation::approved).count(); }
    }

    public static Pack bundled() throws IOException {
        String path = "/gg/feral/burrow/intelligence/gear-w02-research.json";
        try (InputStream stream = GearKnowledge.class.getResourceAsStream(path)) {
            if (stream == null) throw new IOException("Missing gear evidence: " + path);
            return parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @SuppressWarnings("unchecked")
    public static Pack parse(String json) {
        Object parsed = MiniJson.parse(Objects.requireNonNull(json));
        if (!(parsed instanceof Map<?,?> m)) throw new IllegalArgumentException("Gear pack must be object");
        if (!(m.get("schema_version") instanceof Number n) || n.intValue()!=2)
            throw new IllegalArgumentException("Expected schema_version 2");
        String id = string(m,"pack_id");
        List<Item> items = new ArrayList<>();
        for (Object v : array(m,"items")) {
            Map<?,?> item = object(v,"item");
            Map<String,Integer> skills=new HashMap<>();
            for (var e: object(item.get("minimum_skills"),"minimum_skills").entrySet()) {
                if (!(e.getKey() instanceof String key) || !(e.getValue() instanceof Number value) || value.doubleValue()!=value.intValue())
                    throw new IllegalArgumentException("Malformed skill gate");
                skills.put(key,value.intValue());
            }
            items.add(new Item(integer(item,"item_id"),string(item,"name"),string(item,"family"),
                enumValue(Slot.class,string(item,"slot")), enumValue(Hands.class,string(item,"hands")),
                string(item,"god_alignment"),skills,new HashSet<>(stringList(item,"required_state_flags")),
                enumValue(IntelligenceCatalog.Review.class,string(item,"review")),source(item.get("source")),
                stringList(item,"research_lanes"),stringList(item,"research_urls"),bool(item,"identity_conflict")));
        }
        List<Relation> relations=new ArrayList<>();
        for (Object v: array(m,"relations")) {
            Map<?,?> r=object(v,"relation");
            relations.add(new Relation(string(r,"id"),integer(r,"from_id"),integer(r,"to_id"),
                enumValue(RelationKind.class,string(r,"kind")),string(r,"required_context"),
                enumValue(IntelligenceCatalog.Review.class,string(r,"review")),
                source(r.get("source")),stringList(r,"research_lanes")));
        }
        return new Pack(2,id,items,relations);
    }
    private static IntelligenceCatalog.Source source(Object o) {
        if (o == null) return null;
        Map<?,?> m=object(o,"source");
        return new IntelligenceCatalog.Source(string(m,"url"),string(m,"revision"),
            string(m,"retrieved_utc"),string(m,"evidence_pointer"));
    }
    private static <T extends Enum<T>> T enumValue(Class<T> clazz, String name) {
        try {return Enum.valueOf(clazz,name);} catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid "+clazz.getSimpleName()+" value "+name,e);
        }
    }
    private static List<?> array(Map<?,?> m,String k) {
        if (!(m.get(k) instanceof List<?> l)) throw new IllegalArgumentException("Expected array "+k);
        return l;
    }
    private static Map<?,?> object(Object o,String k) {
        if (!(o instanceof Map<?,?> m)) throw new IllegalArgumentException("Expected object "+k);
        return m;
    }
    private static int integer(Map<?,?> m,String k) {
        if (!(m.get(k) instanceof Number n) || n.doubleValue()!=n.intValue()) throw new IllegalArgumentException("Expected integer "+k);
        return n.intValue();
    }
    private static String string(Map<?,?> m,String k) {
        if (!(m.get(k) instanceof String s)) throw new IllegalArgumentException("Expected string "+k);
        return s;
    }
    private static boolean bool(Map<?,?> m,String k) {
        if (!(m.get(k) instanceof Boolean b)) throw new IllegalArgumentException("Expected boolean "+k);
        return b;
    }
    private static List<String> stringList(Map<?,?> m,String k) {
        List<String> out=new ArrayList<>();
        for (Object o:array(m,k)) {
            if (!(o instanceof String s)) throw new IllegalArgumentException("Non-string in "+k);
            out.add(s);
        }
        return Collections.unmodifiableList(out);
    }
    private static String notBlank(String s,String label) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException("Blank "+label);
        return s;
    }
}
