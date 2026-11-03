package gg.feral.burrow.intelligence;

import gg.feral.burrow.core.MiniJson;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * W04: versioned, immutable activity intelligence. No account or bank reads, UI changes,
 * RuneLite APIs, network access or automatic game actions. Source review and observation
 * must be proved separately. Routes are patch lists, not invented time optimizations.
 */
public final class ActivityKnowledge {
    private ActivityKnowledge() {}

    public enum Kind { ACTIVITY, PATCH, TELEPORT, CLUE_PROFILE }
    public enum RequirementType { SKILL_REAL, SKILL_EFFECTIVE, COMBAT_LEVEL, QUEST, DIARY, UNLOCK, ITEM }
    public enum Role { REQUIRED, RECOMMENDED, OPTIONAL_UPGRADE }
    public enum PatchType { NONE, REGULAR_TREE, FRUIT_TREE }

    public record Requirement(String id, RequirementType type, String key, int minimum,
                              boolean boostable, Role role, String anyOfGroup) {
        public Requirement {
            id = nonblank(id, "requirement id");
            Objects.requireNonNull(type);
            key = nonblank(key, "requirement key");
            Objects.requireNonNull(role);
            anyOfGroup = Objects.requireNonNull(anyOfGroup);
            if (minimum < 0 || minimum > 100000) throw new IllegalArgumentException("Invalid minimum: " + id);
            if ((type == RequirementType.SKILL_REAL || type == RequirementType.SKILL_EFFECTIVE) &&
                (minimum < 1 || minimum > 99)) throw new IllegalArgumentException("Invalid skill level: " + id);
            if (type == RequirementType.COMBAT_LEVEL && (minimum < 1 || minimum > 126))
                throw new IllegalArgumentException("Invalid combat level: " + id);
            if (type == RequirementType.SKILL_REAL && boostable)
                throw new IllegalArgumentException("Nonboostable skill cannot have boostable=true: " + id);
            if (type != RequirementType.SKILL_REAL && type != RequirementType.SKILL_EFFECTIVE && boostable)
                throw new IllegalArgumentException("Boostable flag only applies to skills: " + id);
            if (type == RequirementType.ITEM && minimum < 1)
                throw new IllegalArgumentException("Item quantity must be positive: " + id);
        }
    }

    /** An equipment/inventory suggestion is never evidence of account ownership. */
    public record Supply(String name, int itemId, Role role, int minQuantity,
                         int maxQuantity, String slot, String purpose) {
        public Supply {
            name = nonblank(name, "supply name");
            if (itemId < 0) throw new IllegalArgumentException("Negative item ID");
            Objects.requireNonNull(role);
            if (minQuantity < 0 || maxQuantity < minQuantity)
                throw new IllegalArgumentException("Bad supply range: " + name);
            slot = Objects.requireNonNull(slot);
            purpose = Objects.requireNonNull(purpose);
        }
    }

    /**
     * Cooldown group is globally shared across related teleport options. A daily
     * cap of -1 means unlimited; 0 means no permitted daily uses. Neither bypasses
     * quest/diary gates or item ownership.
     */
    public record Travel(String destination, String cooldownGroup,
                         int cooldownSeconds, int dailyLimit, String dailyGroup,
                         String travelNotes) {
        public Travel {
            destination = nonblank(destination, "destination");
            cooldownGroup = Objects.requireNonNull(cooldownGroup);
            dailyGroup = Objects.requireNonNull(dailyGroup);
            travelNotes = Objects.requireNonNull(travelNotes);
            if (cooldownSeconds < 0 || dailyLimit < -1)
                throw new IllegalArgumentException("Invalid travel limits");
            if (cooldownSeconds > 0 && cooldownGroup.isBlank())
                throw new IllegalArgumentException("Cooldown requires shared group");
            if (dailyLimit >= 0 && dailyGroup.isBlank())
                throw new IllegalArgumentException("Daily cap requires charge group");
        }
    }

    public record Entry(String id, Kind kind, IntelligenceCatalog.Domain domain,
                        String name, String context, PatchType patchType,
                        boolean wilderness, IntelligenceCatalog.Review review,
                        IntelligenceCatalog.Source source,
                        List<Requirement> requirements, List<Supply> supplies,
                        Travel travel) {
        public Entry {
            id = nonblank(id, "entry id");
            if (!id.matches("[A-Z0-9-]{5,72}")) throw new IllegalArgumentException("Bad entry id: " + id);
            Objects.requireNonNull(kind);
            Objects.requireNonNull(domain);
            name = nonblank(name, "entry name");
            context = Objects.requireNonNull(context);
            Objects.requireNonNull(patchType);
            Objects.requireNonNull(review);
            requirements = List.copyOf(Objects.requireNonNull(requirements));
            supplies = List.copyOf(Objects.requireNonNull(supplies));
            if (kind == Kind.PATCH && patchType == PatchType.NONE)
                throw new IllegalArgumentException("Patch needs exact type: " + id);
            if (kind != Kind.PATCH && patchType != PatchType.NONE)
                throw new IllegalArgumentException("Nonpatch cannot have patch type: " + id);
            if (kind == Kind.TELEPORT && travel == null)
                throw new IllegalArgumentException("Teleport needs travel rules: " + id);
            if (kind != Kind.TELEPORT && travel != null)
                throw new IllegalArgumentException("Nonteleport cannot carry travel rules: " + id);
            if (review == IntelligenceCatalog.Review.VERIFIED_SOURCE && !GearKnowledge.immutableSource(source))
                throw new IllegalArgumentException("Approved activity needs immutable revision: " + id);
            Set<String> ids = new HashSet<>();
            java.util.Map<String,Role> altRoles = new java.util.HashMap<>();
            for (Requirement requirement : requirements) {
                if (!ids.add(requirement.id())) throw new IllegalArgumentException("Duplicate gate in " + id);
                if (!requirement.anyOfGroup().isBlank()) {
                    Role old = altRoles.putIfAbsent(requirement.anyOfGroup(), requirement.role());
                    if (old != null && old != requirement.role())
                        throw new IllegalArgumentException("Mixed role in alternatives " + id);
                }
            }
            if (kind == Kind.CLUE_PROFILE && domain != IntelligenceCatalog.Domain.CLUE)
                throw new IllegalArgumentException("Clue profile wrong domain");
        }
        public boolean approved() {
            return review == IntelligenceCatalog.Review.VERIFIED_SOURCE && GearKnowledge.immutableSource(source);
        }
    }

    public record Pack(int schemaVersion, String packId, List<Entry> entries) {
        public Pack {
            if (schemaVersion != 4) throw new IllegalArgumentException("Expected schema version 4");
            packId = nonblank(packId, "pack ID");
            entries = List.copyOf(Objects.requireNonNull(entries));
            Set<String> ids = new HashSet<>();
            for (Entry e : entries)
                if (!ids.add(e.id())) throw new IllegalArgumentException("Duplicate activity ID: " + e.id());
        }
        public Entry find(String id) {
            for (Entry e : entries) if (e.id().equals(id)) return e;
            return null;
        }
        public long approvedCount() {return entries.stream().filter(Entry::approved).count();}
        public List<Entry> ofKind(Kind kind) {return entries.stream().filter(e -> e.kind() == kind).toList();}
        public List<Entry> patches(PatchType type) {
            return entries.stream().filter(e -> e.kind() == Kind.PATCH && e.patchType() == type).toList();
        }
    }

    public static Pack bundled() throws IOException {
        String path = "/gg/feral/burrow/intelligence/activity-w04-research.json";
        try (InputStream stream = ActivityKnowledge.class.getResourceAsStream(path)) {
            if (stream == null) throw new IOException("Missing activity evidence: " + path);
            return parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    public static Pack parse(String json) {
        Object raw = MiniJson.parse(Objects.requireNonNull(json));
        Map<?,?> root = obj(raw, "root");
        if (integer(root, "schema_version") != 4) throw new IllegalArgumentException("Bad schema version");
        List<Entry> entries = new ArrayList<>();
        for (Object element : list(root, "entries")) {
            Map<?,?> m = obj(element, "entry");
            List<Requirement> requirements = new ArrayList<>();
            for (Object r : list(m, "requirements")) {
                Map<?,?> row = obj(r, "requirement");
                requirements.add(new Requirement(string(row, "id"),
                    e(RequirementType.class, string(row, "type")), string(row, "key"),
                    integer(row, "minimum"), bool(row, "boostable"), e(Role.class, string(row, "role")),
                    string(row, "any_of_group")));
            }
            List<Supply> supplies = new ArrayList<>();
            for (Object s : list(m, "supplies")) {
                Map<?,?> row = obj(s, "supply");
                supplies.add(new Supply(string(row, "name"), integer(row, "item_id"),
                    e(Role.class, string(row, "role")), integer(row, "min_quantity"),
                    integer(row, "max_quantity"), string(row, "slot"), string(row, "purpose")));
            }
            IntelligenceCatalog.Source source = null;
            if (m.get("source") != null) {
                Map<?,?> sr = obj(m.get("source"), "source");
                source = new IntelligenceCatalog.Source(string(sr, "url"), string(sr, "revision"),
                    string(sr, "retrieved_utc"), string(sr, "evidence_pointer"));
            }
            Travel travel = null;
            if (m.get("travel") != null) {
                Map<?,?> tr = obj(m.get("travel"), "travel");
                travel = new Travel(string(tr, "destination"), string(tr, "cooldown_group"),
                    integer(tr, "cooldown_seconds"), integer(tr, "daily_limit"),
                    string(tr, "daily_group"), string(tr, "notes"));
            }
            entries.add(new Entry(string(m, "id"), e(Kind.class, string(m, "kind")),
                e(IntelligenceCatalog.Domain.class, string(m, "domain")),
                string(m, "name"), string(m, "context"),
                e(PatchType.class, string(m, "patch_type")), bool(m, "wilderness"),
                e(IntelligenceCatalog.Review.class, string(m, "review")), source,
                requirements, supplies, travel));
        }
        return new Pack(4, string(root, "pack_id"), entries);
    }

    private static String nonblank(String s,String label) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException("Blank " + label);
        return s;
    }
    private static Map<?,?> obj(Object v,String label) {
        if (!(v instanceof Map<?,?> m)) throw new IllegalArgumentException("Expected object " + label);
        return m;
    }
    private static List<?> list(Map<?,?> m,String k) {
        if (!(m.get(k) instanceof List<?> list)) throw new IllegalArgumentException("Expected array " + k);
        return list;
    }
    private static String string(Map<?,?> m,String k) {
        if (!(m.get(k) instanceof String s)) throw new IllegalArgumentException("Expected string " + k);
        return s;
    }
    private static int integer(Map<?,?> m,String k) {
        if (!(m.get(k) instanceof Number n) || !Double.isFinite(n.doubleValue()) || n.doubleValue() != n.intValue())
            throw new IllegalArgumentException("Expected integer " + k);
        return n.intValue();
    }
    private static boolean bool(Map<?,?> m,String k) {
        if (!(m.get(k) instanceof Boolean b)) throw new IllegalArgumentException("Expected boolean " + k);
        return b;
    }
    private static <T extends Enum<T>> T e(Class<T> clazz, String value) {
        try { return Enum.valueOf(clazz, value); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Invalid " + clazz.getSimpleName() + ": " + value, ex); }
    }
}
