package gg.feral.burrow.core;

import java.util.*;
import java.util.regex.*;

/**
 * Build 20: pure, read-only parser for one drawn RuneLite Collection Log entry page. The RuneLite
 * adapter copies widget text/item data on the client thread and hands it here; nothing is inferred.
 * A page that cannot be parsed yields no KC (unknown), never zero.
 */
public final class CollectionLogParser {
    private CollectionLogParser() {}
    /** One item slot of the drawn page. RuneLite draws obtained items opaque (opacity 0). */
    public record Slot(int itemId, int opacity, int quantity) {}
    public record Observation(String page, Map<String,Long> killCounts, Set<Integer> obtained, Set<Integer> listed) {
        public Observation { killCounts=Map.copyOf(killCounts); obtained=Set.copyOf(obtained); listed=Set.copyOf(listed); }
    }
    private static final Pattern KC = Pattern.compile(
        "^(.*?)\\s*(?:kill count|kills|completions|completion count|clears|chests opened|opened|chest count)\\s*:\\s*([0-9][0-9,]*)$", Pattern.CASE_INSENSITIVE);

    static String clean(String s) {
        if(s==null) return "";
        return s.replaceAll("<[^>]*>","").replace(' ',' ').trim();
    }
    /**
     * @param headerTexts the texts of the entry header's children, in order; the first is the page title
     * @param slots       every item slot on the page
     */
    public static Optional<Observation> parse(List<String> headerTexts, List<Slot> slots) {
        if(headerTexts==null || headerTexts.isEmpty()) return Optional.empty();
        String page=clean(headerTexts.get(0));
        if(page.isEmpty() || page.length()>60) return Optional.empty();
        Map<String,Long> kc=new LinkedHashMap<>();
        for(int i=1;i<headerTexts.size();i++) {
            Matcher m=KC.matcher(clean(headerTexts.get(i)));
            if(!m.matches()) continue;
            long v;
            try { v=Long.parseLong(m.group(2).replace(",","")); } catch(NumberFormatException e) { continue; }
            if(v<0 || v>100_000_000L) continue;
            String who=m.group(1).trim();
            kc.put(encounterName(who.isEmpty()?page:who,page),v);
        }
        Set<Integer> obtained=new LinkedHashSet<>(), listed=new LinkedHashSet<>();
        if(slots!=null) for(Slot s:slots) {
            if(s==null || s.itemId()<=0) continue;
            listed.add(s.itemId());
            if(s.opacity()==0) obtained.add(s.itemId());
        }
        return Optional.of(new Observation(page,kc,obtained,listed));
    }
    /** Maps a KC label to a tracker encounter where one exists; otherwise keeps the label as written. */
    public static String encounterName(String label,String page) {
        if(RareDropTable.encounter(label).isPresent()) return label;
        // CP-3: in-game headers may say "Tormented Demon kills" while the tracker encounter is "Tormented Demons".
        // Try singular/plural forms of the label and page before giving up (never invents a different boss).
        for(String form:pluralForms(label)) if(RareDropTable.encounter(form).isPresent()) return form;
        var entry=BossRegistry.find(label).or(()->BossRegistry.find(page));
        for(String form:pluralForms(label)) { if(entry.isPresent())break; entry=BossRegistry.find(form); }
        for(String form:pluralForms(page)) { if(entry.isPresent())break; entry=BossRegistry.find(form); }
        if(entry.isPresent()) {
            var es=RareDropTable.encounters(entry.get().id());
            if(es.size()==1) return es.get(0).encounter();
            for(var e:es) if(e.encounter().equalsIgnoreCase(label)) return e.encounter();
            if(es.isEmpty()) return entry.get().id();
        }
        return label;
    }
    static List<String> pluralForms(String name) {
        if(name==null || name.isBlank()) return List.of();
        String n=name.trim();
        String other=n.toLowerCase(Locale.ROOT).endsWith("s")?n.substring(0,n.length()-1):n+"s";
        // Case-insensitive registry/table lookups expect the canonical capitalisation of the first letter.
        return List.of(other, Character.toUpperCase(other.charAt(0))+other.substring(1));
    }
}
