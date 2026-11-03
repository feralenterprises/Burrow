package gg.feral.burrow.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Build 19: read-only view of the bundled, Wiki-revision-stamped FINAL-06 item requirement registry
 * ({@code BUR-OUTFITTER-MASTER-FINAL-06-ITEM-REQUIREMENTS.json}, 220 reviewed items). Provides wear
 * levels and named unlocks for the Best in Bank solver. The resource is NOT modified.
 *
 * <p>Unlock strings that exactly name a RuneLite quest are treated as wear/use gates. Every other
 * unlock text (diaries, kill counts, acquisition routes such as "Dragon Slayer I started to obtain
 * shield") is retained as information only: possessing the item already proves acquisition.
 */
public final class ItemRequirementRegistry {
    public static final String RESOURCE="/gg/feral/burrow/outfitter/BUR-OUTFITTER-MASTER-FINAL-06-ITEM-REQUIREMENTS.json";
    public record Entry(int itemId, String name, Map<String,Integer> wearLevels, List<String> unlocks,
                        boolean equipment, String wikiUrl, long revision) {
        public Entry { wearLevels=Map.copyOf(wearLevels); unlocks=List.copyOf(unlocks); }
    }
    private static final Map<Integer,Entry> DATA=load();
    private ItemRequirementRegistry() {}

    public static Optional<Entry> find(int itemId) { return Optional.ofNullable(DATA.get(itemId)); }
    public static int size() { return DATA.size(); }
    public static boolean isEquipment(int itemId) { Entry e=DATA.get(itemId); return e!=null && e.equipment(); }

    @SuppressWarnings("unchecked")
    private static Map<Integer,Entry> load() {
        try(InputStream in=ItemRequirementRegistry.class.getResourceAsStream(RESOURCE)) {
            if(in==null)throw new IllegalStateException("missing "+RESOURCE);
            String text=new String(in.readAllBytes(),StandardCharsets.UTF_8);
            List<Object> rows=(List<Object>)MiniJson.parse(text);
            Map<Integer,Entry> out=new LinkedHashMap<>();
            for(Object o:rows) {
                Map<String,Object> r=(Map<String,Object>)o;
                int id=((Number)r.get("item_id")).intValue();
                Map<String,Integer> levels=new LinkedHashMap<>();
                Object wl=r.get("wear_levels");
                if(wl instanceof Map<?,?> m) for(var e:m.entrySet())
                    levels.put(capitalise(String.valueOf(e.getKey())),((Number)e.getValue()).intValue());
                List<String> unlocks=new ArrayList<>();
                if(r.get("unlocks") instanceof List<?> l) for(Object u:l) unlocks.add(String.valueOf(u));
                boolean equipment=Boolean.TRUE.equals(r.get("equipment_definition"));
                Object rev=r.get("revision");
                out.put(id,new Entry(id,String.valueOf(r.get("name")),levels,unlocks,equipment,
                    String.valueOf(r.get("wiki_url")),rev instanceof Number n?n.longValue():-1L));
            }
            return Collections.unmodifiableMap(out);
        } catch(IOException e) { throw new UncheckedIOException(e); }
    }
    /** "defence" -> "Defence": matches RuneLite Skill#getName(). */
    static String capitalise(String skill) {
        if(skill.isEmpty())return skill;
        return Character.toUpperCase(skill.charAt(0))+skill.substring(1).toLowerCase(Locale.ROOT);
    }
}
