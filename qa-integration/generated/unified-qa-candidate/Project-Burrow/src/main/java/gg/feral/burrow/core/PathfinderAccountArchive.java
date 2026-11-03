package gg.feral.burrow.core;

import gg.feral.burrow.pathfinder.SuggestionEngine;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * PQA-W06 ISOLATED CANDIDATE. Durable, account-scoped historical observations for
 * Pathfinder. Every entry is an as-observed record, NEVER an authorization to
 * treat a closed-bank item as currently owned or a quest as completed.
 *
 * Read/write storage is injected: the RuneLite host can supply ConfigManager.
 * Missing or malformed data remains unavailable. No game actions are possible.
 */
public final class PathfinderAccountArchive {
    private static final String BASELINE_PREFIX = "pathfinder_initial_";
    private static final String LATEST_PREFIX = "pathfinder_latest_";
    private static final String BANK_PREFIX = "pathfinder_bank_memory_";
    private static final String VERSION = "PQA6";
    // Bounds prevent accidental unbounded config writes; archives are never silently truncated.
    private static final int MAX_STORED_CHARS = 120_000;
    public static final long REMINDER_AFTER_MS = 7L * 24 * 60 * 60 * 1000;

    private final Function<String,String> reader;
    private final BiConsumer<String,String> writer;

    public PathfinderAccountArchive(Function<String,String> reader, BiConsumer<String,String> writer) {
        this.reader = Objects.requireNonNull(reader);
        this.writer = Objects.requireNonNull(writer);
    }
    private static String accountSuffix(long hash) {
        if (hash == -1L) throw new IllegalArgumentException("Unauthenticated account");
        return Long.toUnsignedString(hash, 16);
    }
    public static String baselineKey(long hash) { return BASELINE_PREFIX + accountSuffix(hash); }
    public static String latestKey(long hash) { return LATEST_PREFIX + accountSuffix(hash); }
    public static String bankKey(long hash) { return BANK_PREFIX + accountSuffix(hash); }

    public record Baseline(long observedAtMillis, Map<String,Integer> levels,
                           Map<String,SuggestionEngine.Quest> quests, boolean surveyComplete) {
        public Baseline {
            levels=Map.copyOf(levels); quests=Map.copyOf(quests);
        }
        /** Seven-day freshness reminder for skills/quests — never auto-refreshes the game UI. */
        public boolean reminderDue(long now) {
            return now < observedAtMillis || now - observedAtMillis >= REMINDER_AFTER_MS;
        }
    }
    /** Bank-only contents, deliberately excluding carried/gear items to avoid double count. */
    public record BankMemory(long observedAtMillis, Map<Integer,Integer> bankItems, boolean fullTabWitness) {
        public BankMemory { bankItems=Map.copyOf(bankItems); }
        public boolean reminderDue(long now) {
            return now < observedAtMillis || now - observedAtMillis >= REMINDER_AFTER_MS;
        }
        /** Historical entries do not prove current ownership or stock. */
        public boolean provesCurrentStock() { return false; }
    }

    /** Capture exactly once after a complete, same-login quest survey and full skill vector. */
    public boolean saveInitialOnce(long hash, long observedAt, SuggestionEngine.Snapshot snapshot,
                                   boolean allSkillsObserved, boolean questSurveyComplete) {
        if (hash == -1L || observedAt <= 0 || snapshot == null || !snapshot.verifiedIdentity() ||
            !allSkillsObserved || !questSurveyComplete || snapshot.skills().isEmpty() ||
            snapshot.quests().isEmpty() || snapshot.quests().containsValue(SuggestionEngine.Quest.UNKNOWN)) return false;
        String key=baselineKey(hash);
        // Existing valid records never overwrite; unreadable records require explicit recovery.
        String old=reader.apply(key);
        if (old!=null && !old.isBlank()) return false;
        String body=observedAt+"|"+encodeSkills(snapshot.skills())+"|"+encodeQuests(snapshot.quests());
        writer.accept(key, seal("B",body));
        return true;
    }
    public Optional<Baseline> readInitial(long hash) {
        return hash == -1L ? Optional.empty() : readProgress(baselineKey(hash));
    }
    /** Save the most recently VERIFIED full skill/quest survey, without repetitive tick writes.
     *  A newly observed state is not proof that Pathfinder caused the change. */
    public boolean rememberLatest(long hash, long observedAt, SuggestionEngine.Snapshot snapshot,
                                  boolean allSkillsObserved, boolean questSurveyComplete) {
        if (hash == -1L || observedAt <= 0 || snapshot == null || !snapshot.verifiedIdentity() ||
            !allSkillsObserved || !questSurveyComplete || snapshot.skills().isEmpty() ||
            snapshot.quests().isEmpty() || snapshot.quests().containsValue(SuggestionEngine.Quest.UNKNOWN))return false;
        String key=latestKey(hash),old=reader.apply(key);
        var prior=readProgress(key);
        if(old!=null && !old.isBlank() && prior.isEmpty())return false; // corruption -> explicit recovery
        if(prior.isPresent()) {
            Baseline p=prior.get();
            if(p.observedAtMillis()>=observedAt)return false;
            // Same content is re-witnessed at most once every seven days. This
            // refreshes the as-observed timestamp without writing every game tick.
            if(p.levels().equals(snapshot.skills()) && p.quests().equals(snapshot.quests()) &&
                observedAt-p.observedAtMillis()<REMINDER_AFTER_MS)return false;
        }
        String body=observedAt+"|"+encodeSkills(snapshot.skills())+"|"+encodeQuests(snapshot.quests());
        writer.accept(key,seal("B",body));
        return true;
    }
    public Optional<Baseline> readLatest(long hash) {
        return hash == -1L ? Optional.empty() : readProgress(latestKey(hash));
    }
    /** Exact observed deltas only. No XP, loot, travel, quest rewards or causal attribution. */
    public record ProgressComparison(Map<String,Integer> skillLevelIncreases,
                                     Set<String> newlyFinishedQuests,
                                     Set<String> conflicts) {
        public ProgressComparison {
            skillLevelIncreases=Map.copyOf(skillLevelIncreases);
            newlyFinishedQuests=Set.copyOf(newlyFinishedQuests);
            conflicts=Set.copyOf(conflicts);
        }
        public boolean consistent() { return conflicts.isEmpty(); }
    }
    public Optional<ProgressComparison> compareInitialToLatest(long hash) {
        var first=readInitial(hash);var last=readLatest(hash);
        if(first.isEmpty()||last.isEmpty()||last.get().observedAtMillis()<first.get().observedAtMillis())
            return Optional.empty();
        Map<String,Integer> levels=new TreeMap<>();Set<String> quests=new TreeSet<>(),conflicts=new TreeSet<>();
        for(var e:first.get().levels().entrySet()) {
            Integer newLevel=last.get().levels().get(e.getKey());
            if(newLevel==null){conflicts.add("SKILL_MISSING:"+e.getKey());continue;}
            if(newLevel<e.getValue())conflicts.add("SKILL_DECREASE:"+e.getKey());
            else if(newLevel>e.getValue())levels.put(e.getKey(),newLevel-e.getValue());
        }
        for(var e:first.get().quests().entrySet()) {
            SuggestionEngine.Quest next=last.get().quests().get(e.getKey());
            if(next==null){conflicts.add("QUEST_MISSING:"+e.getKey());continue;}
            if(e.getValue()==SuggestionEngine.Quest.FINISHED && next!=SuggestionEngine.Quest.FINISHED)
                conflicts.add("QUEST_REGRESSED:"+e.getKey());
            else if(e.getValue()!=SuggestionEngine.Quest.FINISHED && next==SuggestionEngine.Quest.FINISHED)
                quests.add(e.getKey());
        }
        return Optional.of(new ProgressComparison(levels,quests,conflicts));
    }
    private Optional<Baseline> readProgress(String key) {
        try {
            String body=unseal("B",reader.apply(key));
            if(body==null)return Optional.empty();
            String[] v=body.split("\\|",-1);
            if(v.length!=3) return Optional.empty();
            long time=Long.parseLong(v[0]);
            var skills=decodeSkills(v[1]);var quests=decodeQuests(v[2]);
            if(time<=0||skills.isEmpty()||quests.isEmpty())return Optional.empty();
            return Optional.of(new Baseline(time,skills,quests,true));
        }catch(RuntimeException ignored){return Optional.empty();}
    }

    /**
     * Persist only a complete and accepted stable bank observation from a current open epoch.
     * The caller must have proved live account/session identity with the W04 bridge, and
     * {@code acceptedCurrentOpenEpoch} must be true. No bank reopen or widget read occurs.
     */
    public boolean rememberBank(long hash, long now, BankEvidence evidence, boolean acceptedCurrentOpenEpoch) {
        if(hash==-1L || !acceptedCurrentOpenEpoch || evidence==null || now<=0 ||
           !evidence.bankStillOpen() || evidence.stale() ||
           evidence.population()!=BankEvidence.Population.STABLE_TAB_COUNTS_CONSISTENT ||
           evidence.observedAtMillis()<=0 || evidence.observedAtMillis()>now ||
           now-evidence.observedAtMillis()>120_000L) return false;
        for(int qty:evidence.bank().values()) if(qty<=0) return false;
        var previous=readBank(hash);
        if(previous.isPresent() && previous.get().observedAtMillis()>=evidence.observedAtMillis())return false;
        // Persist fullTabWitness honestly (1/0). Historical memory still never proves live stock.
        String body=evidence.observedAtMillis()+"|"+encodeBank(evidence.bank())+"|1";
        writer.accept(bankKey(hash),seal("M",body));
        return true;
    }
    public Optional<BankMemory> readBank(long hash) {
        if(hash==-1L)return Optional.empty();
        try {
            String body=unseal("M",reader.apply(bankKey(hash)));
            if(body==null)return Optional.empty();
            String[] v=body.split("\\|",-1);
            // v2: time|items|fullTabWitness ; legacy v1: time|items (witness unknown → false)
            if(v.length!=2 && v.length!=3)return Optional.empty();
            long time=Long.parseLong(v[0]);
            var items=decodeBank(v[1]);
            if(time<=0)return Optional.empty();
            boolean fullTab=v.length==3 && "1".equals(v[2]);
            return Optional.of(new BankMemory(time,items,fullTab));
        }catch(RuntimeException ignored){return Optional.empty();}
    }

    private static String encodeSkills(Map<String,Integer> levels) {
        var b=new StringJoiner(",");
        levels.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->{
            if(e.getKey()==null||e.getKey().isBlank()||e.getValue()==null||e.getValue()<1||e.getValue()>99)
                throw new IllegalArgumentException("Invalid skill observation");
            b.add(token(e.getKey())+"="+e.getValue());
        });
        return b.toString();
    }
    private static Map<String,Integer> decodeSkills(String input) {
        if(input.isBlank())return Map.of();
        Map<String,Integer> result=new TreeMap<>();
        for(String s:input.split(",",-1)) {
            String[] v=s.split("=",-1); if(v.length!=2)throw new IllegalArgumentException("Malformed skill");
            String name=fromToken(v[0]);int level=Integer.parseInt(v[1]);
            if(name.isBlank()||level<1||level>99||result.putIfAbsent(name,level)!=null)
                throw new IllegalArgumentException("Duplicate/invalid skill");
        }
        return result;
    }
    private static String encodeQuests(Map<String,SuggestionEngine.Quest> quests) {
        var b=new StringJoiner(",");
        quests.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->{
            if(e.getKey()==null||e.getKey().isBlank()||e.getValue()==null||e.getValue()==SuggestionEngine.Quest.UNKNOWN)
                throw new IllegalArgumentException("Incomplete quest survey");
            b.add(token(e.getKey())+"="+e.getValue().name());
        });
        return b.toString();
    }
    private static Map<String,SuggestionEngine.Quest> decodeQuests(String input) {
        if(input.isBlank())return Map.of();
        Map<String,SuggestionEngine.Quest> result=new TreeMap<>();
        for(String s:input.split(",",-1)) {
            String[] v=s.split("=",-1);if(v.length!=2)throw new IllegalArgumentException("Malformed quest");
            String name=fromToken(v[0]);var status=SuggestionEngine.Quest.valueOf(v[1]);
            if(name.isBlank()||status==SuggestionEngine.Quest.UNKNOWN||result.putIfAbsent(name,status)!=null)
                throw new IllegalArgumentException("Duplicate/invalid quest");
        }
        return result;
    }
    private static String encodeBank(Map<Integer,Integer> items) {
        var b=new StringJoiner(",");
        items.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->{
            if(e.getKey()==null||e.getKey()<=0||e.getValue()==null||e.getValue()<=0)
                throw new IllegalArgumentException("Invalid bank stack");
            b.add(e.getKey()+"="+e.getValue());
        });
        return b.toString();
    }
    private static Map<Integer,Integer> decodeBank(String input) {
        if(input.isBlank())return Map.of(); // a witnessed empty bank is a valid memory
        Map<Integer,Integer> result=new TreeMap<>();
        for(String s:input.split(",",-1)) {
            String[] v=s.split("=",-1);if(v.length!=2)throw new IllegalArgumentException("Malformed bank entry");
            int id=Integer.parseInt(v[0]),qty=Integer.parseInt(v[1]);
            if(id<=0||qty<=0||result.putIfAbsent(id,qty)!=null)
                throw new IllegalArgumentException("Duplicate/invalid bank entry");
        }
        return result;
    }
    private static String token(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }
    private static String fromToken(String s) {
        return new String(Base64.getUrlDecoder().decode(s),StandardCharsets.UTF_8);
    }
    private static String seal(String type,String body) {
        String envelope=VERSION+"|"+type+"|"+body;
        if(envelope.length()>MAX_STORED_CHARS)throw new IllegalArgumentException("Archive too large");
        return envelope+"|"+sha(envelope);
    }
    private static String unseal(String type,String raw) {
        if(raw==null||raw.isBlank()||raw.length()>MAX_STORED_CHARS+65)return null;
        int sep=raw.lastIndexOf('|');if(sep<0)return null;
        String envelope=raw.substring(0,sep),hash=raw.substring(sep+1);
        if(!sha(envelope).equals(hash))return null;
        String prefix=VERSION+"|"+type+"|";
        return envelope.startsWith(prefix)?envelope.substring(prefix.length()):null;
    }
    private static String sha(String s) {
        try {
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        }catch(NoSuchAlgorithmException e){throw new AssertionError(e);}
    }
}
