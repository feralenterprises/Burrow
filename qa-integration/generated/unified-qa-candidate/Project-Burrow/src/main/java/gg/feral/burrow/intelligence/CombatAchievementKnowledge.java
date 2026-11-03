package gg.feral.burrow.intelligence;

import gg.feral.burrow.core.MiniJson;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * W05: an independent, immutable CA intelligence catalog. The existing 290-row
 * Chronicle reference is preserved as legacy research, NOT canonically approved data.
 * The rejected BQA-906 652 placeholders must never become verified task records.
 * This class never accesses RuneLite or player accounts, and cannot award a CA.
 */
public final class CombatAchievementKnowledge {
    private CombatAchievementKnowledge() {}

    public enum Tier { EASY(1), MEDIUM(2), HARD(3), ELITE(4), MASTER(5), GRANDMASTER(6);
        private final int points;
        Tier(int points) {this.points=points;}
        public int points() {return points;}
    }
    public enum GateType { SKILL, QUEST, DIARY, ITEM, INSTANCE, TEAM_SIZE, UNLOCK, SPELL, LOCATION }
    public enum Review { CHECK, VERIFIED_SOURCE, REJECTED }

    public record Gate(String id, GateType type, String key, int minimum, boolean boostable) {
        public Gate {
            id = required(id,"gate id");
            Objects.requireNonNull(type);
            key = required(key,"gate key");
            if (minimum < 0) throw new IllegalArgumentException("Negative gate minimum: " + id);
            if (type == GateType.SKILL && (minimum < 1 || minimum > 99))
                throw new IllegalArgumentException("Invalid skill gate: " + id);
            if (type == GateType.ITEM && minimum < 1)
                throw new IllegalArgumentException("Item gate needs quantity: " + id);
            if (boostable && type != GateType.SKILL)
                throw new IllegalArgumentException("Boostable flag is skill-only: " + id);
        }
    }

    public record Task(String id, String boss, String name, Tier tier, String type,
                       String canonicalCondition, String legacyDescription, String strategyTip,
                       Review review, IntelligenceCatalog.Source source, List<Gate> gates) {
        public Task {
            id=required(id,"task id");
            if (!id.matches("[A-Z0-9-]{8,80}")) throw new IllegalArgumentException("Invalid task ID: " + id);
            boss=required(boss,"boss");
            name=required(name,"task name");
            Objects.requireNonNull(tier);
            type=required(type,"type");
            canonicalCondition=Objects.requireNonNull(canonicalCondition);
            legacyDescription=Objects.requireNonNull(legacyDescription);
            strategyTip=Objects.requireNonNull(strategyTip);
            Objects.requireNonNull(review);
            gates=List.copyOf(Objects.requireNonNull(gates));
            Set<String> seen=new HashSet<>();
            for (Gate gate:gates) if (!seen.add(gate.id())) throw new IllegalArgumentException("Duplicate gate " + gate.id());
            if (review==Review.VERIFIED_SOURCE) {
                if (!GearKnowledge.immutableSource(source))
                    throw new IllegalArgumentException("Approved CA requires immutable source: " + id);
                if (canonicalCondition.isBlank() || looksLikePlaceholder(canonicalCondition) || looksLikePlaceholder(name))
                    throw new IllegalArgumentException("Approved CA requires actual game-checked condition: " + id);
            }
        }
        public int points() {return tier.points();}
        public boolean approved() {return review==Review.VERIFIED_SOURCE && GearKnowledge.immutableSource(source);}
        public boolean legacyOnly() {return canonicalCondition.isBlank() && !legacyDescription.isBlank();}
        public String crosswalkKey() {return key(boss,name);}
    }

    public record Pack(int schemaVersion, String packId, int referenceTotal,
                       String referenceTotalStatus, List<Task> tasks) {
        public Pack {
            if (schemaVersion!=5) throw new IllegalArgumentException("Expected CA intelligence schema 5");
            packId=required(packId,"pack ID");
            referenceTotalStatus=required(referenceTotalStatus,"reference total status");
            if (referenceTotal<0) throw new IllegalArgumentException("Invalid total");
            tasks=List.copyOf(Objects.requireNonNull(tasks));
            Set<String> ids=new HashSet<>(), keys=new HashSet<>();
            for (Task task:tasks) {
                if (!ids.add(task.id())) throw new IllegalArgumentException("Duplicate task ID: " + task.id());
                if (!keys.add(task.crosswalkKey())) throw new IllegalArgumentException("Duplicate boss/name: " + task.crosswalkKey());
            }
            // referenceTotal is a separately sourced global count, NEVER a claim that this pack is complete.
        }
        public long approvedCount() {return tasks.stream().filter(Task::approved).count();}
        public Task find(String id) {for(Task t:tasks) if(t.id().equals(id)) return t; return null;}
        public Task matchLegacy(String boss,String name) {
            String target=key(boss,name);
            for (Task t:tasks) if(t.crosswalkKey().equals(target)) return t;
            return null;
        }
        public List<Task> forBoss(String boss) {
            if(boss==null) return List.of();
            return tasks.stream().filter(t->t.boss().equalsIgnoreCase(boss)).toList();
        }
    }

    public static Pack bundled() throws IOException {
        String path="/gg/feral/burrow/intelligence/ca-w05-research.json";
        try(InputStream in=CombatAchievementKnowledge.class.getResourceAsStream(path)) {
            if(in==null) throw new IOException("Missing Wave 05 CA catalog: " + path);
            return parse(new String(in.readAllBytes(),StandardCharsets.UTF_8));
        }
    }

    public static Pack parse(String json) {
        Map<?,?> root=object(MiniJson.parse(Objects.requireNonNull(json)),"root");
        if(integer(root,"schema_version")!=5) throw new IllegalArgumentException("Wrong schema version");
        List<Task> tasks=new ArrayList<>();
        for (Object raw:array(root,"tasks")) {
            Map<?,?> row=object(raw,"task");
            List<Gate> gates=new ArrayList<>();
            for (Object candidate:array(row,"gates")) {
                Map<?,?> r=object(candidate,"gate");
                gates.add(new Gate(string(r,"id"),choice(GateType.class,string(r,"type")),
                    string(r,"key"),integer(r,"minimum"),bool(r,"boostable")));
            }
            IntelligenceCatalog.Source source=null;
            if(row.get("source")!=null) {
                Map<?,?> s=object(row.get("source"),"source");
                source=new IntelligenceCatalog.Source(string(s,"url"),string(s,"revision"),
                    string(s,"retrieved_utc"),string(s,"evidence_pointer"));
            }
            tasks.add(new Task(string(row,"id"),string(row,"boss"),string(row,"name"),
                choice(Tier.class,string(row,"tier")),string(row,"type"),
                string(row,"canonical_condition"),string(row,"legacy_description"),
                string(row,"strategy_tip"),choice(Review.class,string(row,"review")),source,gates));
        }
        return new Pack(5,string(root,"pack_id"),integer(root,"reference_total"),
            string(root,"reference_total_status"),tasks);
    }

    public static String key(String boss,String name) {
        return (boss==null?"":boss.strip().toLowerCase(Locale.ROOT)) + "\u001f" +
            (name==null?"":name.strip().toLowerCase(Locale.ROOT));
    }
    private static boolean looksLikePlaceholder(String s) {
        String lower=s.toLowerCase(Locale.ROOT).strip();
        return lower.matches(".*(?:combat task #[0-9]+|game-checked condition for|follow mechanical execution guide|todo|tbd|placeholder|lorem ipsum).*" );
    }
    private static String required(String text,String label) {
        if(text==null || text.isBlank()) throw new IllegalArgumentException("Missing "+label);
        return text;
    }
    private static Map<?,?> object(Object value,String label) {
        if(!(value instanceof Map<?,?> m)) throw new IllegalArgumentException("Expected object: "+label);
        return m;
    }
    private static List<?> array(Map<?,?> m,String name) {
        if(!(m.get(name) instanceof List<?> list)) throw new IllegalArgumentException("Expected array: "+name);
        return list;
    }
    private static String string(Map<?,?> m,String name) {
        if(!(m.get(name) instanceof String s)) throw new IllegalArgumentException("Expected string: "+name);
        return s;
    }
    private static int integer(Map<?,?> m,String name) {
        Object o=m.get(name);
        if(!(o instanceof Number n) || n.doubleValue()!=n.intValue())
            throw new IllegalArgumentException("Expected integer: "+name);
        return n.intValue();
    }
    private static boolean bool(Map<?,?> m,String name) {
        if(!(m.get(name) instanceof Boolean b)) throw new IllegalArgumentException("Expected boolean: "+name);
        return b;
    }
    private static <T extends Enum<T>> T choice(Class<T> type,String name) {
        try {return Enum.valueOf(type,name);} catch(IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown "+type.getSimpleName()+": "+name,e);
        }
    }
}
