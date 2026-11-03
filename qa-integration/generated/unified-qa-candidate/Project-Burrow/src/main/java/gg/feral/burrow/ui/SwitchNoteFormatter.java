package gg.feral.burrow.ui;

import gg.feral.burrow.core.VerifiedOutfitterCatalog;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BUR-0037: presentation-only conversion of FINAL-06 structured required_switches.
 * No gear, mechanics, quantity or ownership data is inferred here. Unknown fields
 * and unreadable source entries remain visible as explicit review warnings.
 */
final class SwitchNoteFormatter {
    private static final int MAX_SOURCE_CHARS = 16000;
    private static final Map<Integer, String> KNOWN_ITEM_NAMES = catalogItemNames();

    private SwitchNoteFormatter() { }

    static List<String> formatSwitches(List<String> source, VerifiedOutfitterCatalog.Profile profile) {
        List<String> formatted = new ArrayList<>();
        for (String raw : source) {
            if (raw == null || raw.isBlank()) {
                formatted.add("Switch detail missing. Verify the source before relying on this setup.");
                continue;
            }
            String trimmed = raw.strip();
            if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
                formatted.add(raw);
                continue;
            }
            if (trimmed.length() > MAX_SOURCE_CHARS) {
                formatted.add("Unverified switch data: source exceeds display parser limit. " +
                    "Review the source before attempting this switch. Source text: " + printable(raw));
                continue;
            }
            try {
                Object parsed = new JsonReader(trimmed).read();
                if (parsed instanceof Map<?, ?> value) {
                    formatted.add(formatObject(value, profile));
                } else if (parsed instanceof List<?> values) {
                    if (values.isEmpty()) formatted.add("Switch list is empty in the source data.");
                    for (Object entry : values) {
                        if (entry instanceof Map<?, ?> object) formatted.add(formatObject(object, profile));
                        else formatted.add("Unrecognized switch-list entry (verify source): " + readable(entry));
                    }
                } else {
                    formatted.add("Unrecognized structured switch entry (verify source): " + readable(parsed));
                }
            } catch (IllegalArgumentException error) {
                // Never silently replace malformed warnings with optimistic advice.
                formatted.add("Unverified switch data: could not parse source. " +
                    "Do not rely on these switch instructions until reviewed. Source text: " + printable(raw));
            }
        }
        return List.copyOf(formatted);
    }

    private static String formatObject(Map<?, ?> fields, VerifiedOutfitterCatalog.Profile profile) {
        List<String> lines = new ArrayList<>();
        String role = valueString(fields.get("role"));
        lines.add(role.isBlank() ? "Switch (role not supplied; verify source)" : "Switch: " + printable(role));
        if (fields.containsKey("role") && !(fields.get("role") instanceof String))
            lines.add("Unrecognized source role: " + readable(fields.get("role")) + ". Verify before relying on it.");
        if (fields.isEmpty()) lines.add("No switch details supplied; verify source before relying on it.");
        for (Map.Entry<?, ?> field : fields.entrySet()) {
            String key = String.valueOf(field.getKey());
            Object value = field.getValue();
            switch (key) {
                case "role" -> { } // Already emitted as heading.
                case "inventory_item_ids" -> lines.add("Switch items from inventory: " + itemNames(value, profile));
                case "unequipped_slots" -> lines.add("Equipment slots to vacate: " + slotNames(value));
                case "capacity_required" -> lines.add("Free inventory spaces required by plan: " + readable(value));
                case "condition" -> lines.add("When / caution: " + readable(value));
                case "instruction" -> lines.add("Steps: " + readable(value));
                case "requirements" -> lines.add("Requirements: " + readable(value));
                default -> lines.add("Additional source field (" + printable(key) + "): " + readable(value) +
                    ". Verify this field before relying on the switch.");
            }
        }
        return String.join("\n", lines);
    }

    private static String valueString(Object o) { return o instanceof String s ? s.strip() : ""; }

    private static String itemNames(Object ids, VerifiedOutfitterCatalog.Profile profile) {
        if (!(ids instanceof List<?> items)) return "unrecognized source value " + readable(ids);
        if (items.isEmpty()) return "none listed (verify source)";
        List<String> resolved = new ArrayList<>();
        for (Object id : items) {
            if (!(id instanceof BigDecimal number) || number.scale() > 0 ||
                number.signum() <= 0 || number.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
                resolved.add("unrecognized item identifier " + readable(id));
                continue;
            }
            int itemId=number.intValue();
            String name=fromProfile(profile, itemId);
            if(name==null) name=KNOWN_ITEM_NAMES.get(itemId);
            resolved.add(name==null ? "item ID " + itemId + " (name not verified)"
                : name + " (ID " + itemId + ")");
        }
        return String.join(", ", resolved);
    }

    private static String fromProfile(VerifiedOutfitterCatalog.Profile profile, int id) {
        if (profile == null) return null;
        for (VerifiedOutfitterCatalog.Item item : profile.gear())
            if (item!=null && item.id()==id && !item.name().isBlank()) return item.name();
        for (VerifiedOutfitterCatalog.Item item : profile.inventory())
            if (item!=null && item.id()==id && !item.name().isBlank()) return item.name();
        return null;
    }

    private static Map<Integer,String> catalogItemNames() {
        Map<Integer,String> names=new LinkedHashMap<>();
        // A conflicting name for the same ID cannot be trusted; keep ID-only.
        Map<Integer,Boolean> conflicts=new LinkedHashMap<>();
        for (String boss : VerifiedOutfitterCatalog.BOSSES) {
            for (VerifiedOutfitterCatalog.Profile profile : VerifiedOutfitterCatalog.profiles(boss)) {
                List<VerifiedOutfitterCatalog.Item> items=new ArrayList<>(profile.gear());
                items.addAll(profile.inventory());
                for (VerifiedOutfitterCatalog.Item item : items) {
                    if(item==null || item.id()<=0 || item.name().isBlank())continue;
                    String old=names.putIfAbsent(item.id(),item.name());
                    if(old!=null && !old.equals(item.name())) conflicts.put(item.id(),true);
                }
            }
        }
        for (int id : conflicts.keySet()) names.remove(id);
        return Collections.unmodifiableMap(names);
    }

    private static String slotNames(Object slots) {
        if (!(slots instanceof List<?> list)) return "unrecognized source value " + readable(slots);
        if (list.isEmpty()) return "none listed";
        List<String> labels=new ArrayList<>();
        for(Object slot:list) labels.add(readable(slot));
        return String.join(", ", labels);
    }

    private static String readable(Object value) {
        if (value == null) return "not specified";
        if (value instanceof String s) return printable(s);
        if (value instanceof BigDecimal number) return number.toPlainString();
        if (value instanceof Boolean b) return b.toString();
        if (value instanceof List<?> list) {
            List<String> parts=new ArrayList<>();
            for (Object entry:list) parts.add(readable(entry));
            return String.join(", ", parts);
        }
        if (value instanceof Map<?,?> map) {
            List<String> parts=new ArrayList<>();
            for (Map.Entry<?,?> entry:map.entrySet())
                parts.add(printable(String.valueOf(entry.getKey())) + ": " + readable(entry.getValue()));
            return String.join("; ",parts);
        }
        return printable(String.valueOf(value));
    }

    private static String printable(String input) {
        return input.replace('\r',' ').replace('\n',' ').replace('\t',' ').replaceAll("[\\p{Cntrl}&&[^ ]]", " ").strip();
    }

    /** Small strict JSON reader, confined to the known source text; no runtime host dependency. */
    private static final class JsonReader {
        private final String text;
        private int pos;
        private int depth;
        JsonReader(String text) { this.text=text; }
        Object read() {
            Object result=value(); ws();
            if(pos!=text.length())throw invalid();
            return result;
        }
        private Object value() {
            ws();if(pos>=text.length())throw invalid();
            char c=text.charAt(pos);
            if(c=='"')return string();
            if(c=='{' || c=='[') {
                if(++depth>16)throw invalid();
                try {return c=='{'?object():array();} finally {depth--;}
            }
            if(c=='t' && match("true"))return Boolean.TRUE;
            if(c=='f' && match("false"))return Boolean.FALSE;
            if(c=='n' && match("null"))return null;
            if(c=='-' || Character.isDigit(c))return number();
            throw invalid();
        }
        private Map<String,Object> object() {
            Map<String,Object> result=new LinkedHashMap<>(); expect('{');ws();
            if(take('}'))return result;
            while(true) {
                ws();if(!peek('"'))throw invalid();
                String key=string();ws();expect(':');Object val=value();
                if(result.containsKey(key))throw invalid(); // ambiguous duplicate keys
                result.put(key,val);ws();
                if(take('}'))return result;
                expect(',');
            }
        }
        private List<Object> array() {
            List<Object> result=new ArrayList<>();expect('[');ws();
            if(take(']'))return result;
            while(true){result.add(value());ws();if(take(']'))return result;expect(',');}
        }
        private String string() {
            expect('"');StringBuilder out=new StringBuilder();boolean ended=false;
            while(pos<text.length()) {
                char c=text.charAt(pos++);
                if(c=='"'){ended=true;break;}
                if(c<32)throw invalid();
                if(c!='\\'){out.append(c);continue;}
                if(pos>=text.length())throw invalid();
                char esc=text.charAt(pos++);
                switch(esc){
                    case '"','\\','/' -> out.append(esc);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if(pos+4>text.length())throw invalid();
                        try{out.append((char)Integer.parseInt(text.substring(pos,pos+4),16));}
                        catch(NumberFormatException exception){throw invalid();}
                        pos+=4;
                    }
                    default -> throw invalid();
                }
            }
            if(!ended)throw invalid();
            return out.toString();
        }
        private BigDecimal number() {
            int start=pos;if(take('-')&&pos>=text.length())throw invalid();
            if(take('0')){if(pos<text.length()&&Character.isDigit(text.charAt(pos)))throw invalid();}
            else {int before=pos;while(pos<text.length()&&text.charAt(pos)>='0'&&text.charAt(pos)<='9')pos++;if(pos==before)throw invalid();}
            if(take('.')) {int before=pos;while(pos<text.length()&&text.charAt(pos)>='0'&&text.charAt(pos)<='9')pos++;if(pos==before)throw invalid();}
            if(take('e')||take('E')) {if(!take('+'))take('-');int before=pos;while(pos<text.length()&&text.charAt(pos)>='0'&&text.charAt(pos)<='9')pos++;if(pos==before)throw invalid();}
            try{return new BigDecimal(text.substring(start,pos));}catch(NumberFormatException e){throw invalid();}
        }
        private boolean match(String word) {
            if(text.startsWith(word,pos)){pos+=word.length();return true;}
            return false;
        }
        private void expect(char ch) {if(!take(ch))throw invalid();}
        private boolean take(char ch){if(peek(ch)){pos++;return true;}return false;}
        private boolean peek(char ch){return pos<text.length()&&text.charAt(pos)==ch;}
        private void ws(){while(pos<text.length()&&(text.charAt(pos)==' '||text.charAt(pos)=='\t'||text.charAt(pos)=='\r'||text.charAt(pos)=='\n'))pos++;}
        private IllegalArgumentException invalid(){return new IllegalArgumentException("Malformed switch record at character " + pos);}
    }
}
