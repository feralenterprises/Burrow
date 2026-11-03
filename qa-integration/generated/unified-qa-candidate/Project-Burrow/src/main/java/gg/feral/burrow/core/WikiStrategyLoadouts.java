package gg.feral.burrow.core;

import gg.feral.burrow.core.TieredOutfitterCatalog.Authority;
import gg.feral.burrow.core.TieredOutfitterCatalog.Selection;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Item;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Profile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Build 20.1 R03: DRAFT Outfitter profiles for bosses that had no loadout, built from OSRS Wiki
 * "/Strategies" equipment tables transcribed slot by slot (resource
 * {@code build201-wiki-strategy-loadouts.txt}, read 2026-11-01). Owner decision 2026-11-01: Wiki
 * table columns map to tiers (OPTIMAL = first column, RECOMMENDED = middle column, ENTRY = last
 * column) and every profile is labelled DRAFT. Item IDs come from {@code build201-item-ids.tsv},
 * checked against RuneLite 1.13.4 {@code ItemID}. Nothing is invented: a cell the Wiki leaves
 * empty stays empty, and an inventory the Wiki does not count is not padded.
 */
public final class WikiStrategyLoadouts {
    private WikiStrategyLoadouts() {}
    public static final String READ_DATE = "2026-11-01";
    private static final String[] SLOTS = {"Head","Cape","Neck","Ammo","Weapon","Body","Shield","Legs","Hands","Feet","Ring"};
    /**
     * Two-handed weapons used in the transcription, each checked against the OSRS Wiki
     * "Two-handed slot table" (160 rows, read 2026-11-01). Anything not listed is one-handed there
     * (e.g. keris partisan, chainmaces, zombie axe, sanguinesti / nightmare staves, tridents,
     * Iban's staff, hunters' sunlight crossbow).
     */
    static final Set<String> TWO_HANDED = Set.of("twisted bow","bow of faerdhinen","scorching bow","toxic blowpipe",
        "scythe of vitur","tumeken's shadow","soulreaper axe","noxious halberd","crystal halberd","elder maul",
        "webweaver bow","craw's bow","magic shortbow (i)","crystal bow","dual macuahuitl","zamorakian spear",
        "venator bow","dragon 2h sword","dharok's greataxe","bandos godsword","saradomin godsword",
        "ancient godsword","armadyl godsword","colossal blade","granite maul","dinh's bulwark","eclipse atlatl",
        "ascension crossbows","abyssal bludgeon","dragon claws","glacial temotli","obligator",
        "saradomin's blessed sword","saradomin sword","zorya's tome");

    public record Setup(String boss, String label, String style, String url, Map<String,String> slots, String spec,
                        String inv, List<String> notes, String req) {}
    private static final Map<String,Integer> IDS = new HashMap<>();
    private static final Set<String> ANY = new HashSet<>();
    private static final List<Setup> SETUPS = new ArrayList<>();
    private static final Map<String,Selection> DATA = new LinkedHashMap<>();
    private static final Map<String,List<String>> METHODS = new LinkedHashMap<>();
    private static final List<String> UNRESOLVED = new ArrayList<>();

    private static final List<String> OVERFLOW=new ArrayList<>();
    private static final Set<String> NEEDS_CHECK=new LinkedHashSet<>();
    public static Set<String> needsCheck(){return Collections.unmodifiableSet(NEEDS_CHECK);}
    private static final Set<String> WIDE=new LinkedHashSet<>(); // per-build scratch: rows wider than the table
    /** R04: Wiki setups for the 15 protected original bosses are transcribed and tested but held out of the
     *  picker until the owner approves layering them next to the original methods. */
    private static final Map<String,Selection> HELD_DATA = new LinkedHashMap<>();
    private static final Set<String> HELD_BOSSES = new LinkedHashSet<>();
    static {
        try {
            for(String line:lines("build201-item-ids.tsv")) {
                if(line.isBlank()||line.startsWith("#")) continue;
                String[] f=line.split("\t",-1);
                IDS.put(norm(f[0]),Integer.parseInt(f[1]));
                if(f.length>3 && f[3].equals("ANY")) ANY.add(norm(f[0]));
            }
            parse(lines("build201-wiki-strategy-loadouts.txt"));
            for(Setup s:SETUPS) for(Tier t:Tier.values()) build(s,t);
        } catch(IOException e) { throw new ExceptionInInitializerError(e); }
    }
    private static List<String> lines(String name) throws IOException {
        try(InputStream in=WikiStrategyLoadouts.class.getResourceAsStream(name)) {
            if(in==null) throw new FileNotFoundException(name);
            return new String(in.readAllBytes(),StandardCharsets.UTF_8).lines().toList();
        }
    }
    static String norm(String s){return s.trim().toLowerCase(Locale.ROOT);}

    private static void parse(List<String> lines) {
        String boss=null,url=null,label=null,spec=null,inv=null,req=null; Map<String,String> slots=null; List<String> notes=null;
        for(String raw:lines) {
            String l=raw.strip();
            if(l.isEmpty()||l.startsWith("#")) continue;
            if(l.startsWith("BOSS ")||l.startsWith("SETUP ")) {
                if(label!=null) SETUPS.add(new Setup(boss,label,style(label),url,slots,spec,inv,notes,req));
                label=null;
                if(l.startsWith("BOSS ")) { boss=l.substring(5).trim(); url=null; continue; }
                label=l.substring(6).trim(); slots=new LinkedHashMap<>(); spec=null; inv=null; req=null; notes=new ArrayList<>();
                continue;
            }
            if(l.startsWith("URL ")) { url=l.substring(4).trim(); continue; }
            int c=l.indexOf(": ");
            if(c<0 || label==null) continue;
            String k=l.substring(0,c), v=l.substring(c+2).trim();
            switch(k) {
                case "Spec" -> spec=v;
                case "INV" -> inv=v;
                case "NOTE" -> notes.add(v);
                case "REQ" -> req=v;
                default -> slots.put(k,v);
            }
        }
        if(label!=null) SETUPS.add(new Setup(boss,label,style(label),url,slots,spec,inv,notes,req));
    }
    private static String style(String label){return label.split("[ (]")[0];}

    private record Pick(Item item, boolean any, boolean taskSkipped) {}
    /** Resolves one cell; null item = empty. taskOnly = only a task-gated item was listed. */
    private static Pick cell(String cellText, List<String> fixNotes) {
        String c=cellText.trim();
        if(c.equals("N/A")||c.startsWith("[generic]")) return new Pick(null,false,false);
        boolean task=false;
        for(String alt:c.split(" / ")) {
            String a=alt.trim();
            if(a.contains("[task]")) { task=true; continue; }
            boolean any=a.startsWith("[any]");
            a=a.replace("[any]","").trim();
            Integer id=IDS.get(norm(a));
            if(id==null) { UNRESOLVED.add(a); continue; }
            return new Pick(new Item(id,a,1),any||ANY.contains(norm(a)),false);
        }
        return new Pick(null,false,task);
    }
    private static List<String> cols(String row){return row==null?List.of():Arrays.asList(row.split(" > "));}
    /** R04: table width = the most common row width (ignoring single-cell rows), so one mis-split row cannot
     *  shift every other slot's tier column. Ties go to the wider width. */
    private static int columns(Setup s) {
        Map<Integer,Integer> freq=new HashMap<>();
        for(var e:s.slots().entrySet()) { if(e.getKey().startsWith("Switch ")) continue; int w=cols(e.getValue()).size(); if(w>1) freq.merge(w,1,Integer::sum); }
        if(freq.isEmpty()) return 1;
        int best=1,bc=-1; for(var e:freq.entrySet()) if(e.getValue()>bc || (e.getValue()==bc && e.getKey()>best)) { best=e.getKey(); bc=e.getValue(); }
        return best;
    }
    /** A row wider than its table (cells that should share a column were split) is read proportionally. */
    private static int rowColumn(int col,int n,int rowWidth) {
        if(rowWidth<=n||n<=1) return col;
        return Math.round(col*(rowWidth-1)/(float)(n-1));
    }
    static int column(Tier t,int n){return switch(t){case OPTIMAL->0;case RECOMMENDED->(n-1)/2;case ENTRY->n-1;};}

    private static Item pickSlot(String row,int col,int n,List<String> fix,String slot,Set<String> anyUsed,List<String> fellBack) {
        var cs=cols(row);
        if(cs.isEmpty()) return null;
        int c=cs.size()==1?0:rowColumn(col,n,cs.size());
        if(cs.size()>n && n>1) WIDE.add(slot);
        for(int i=c;i<cs.size();i++) {
            String cell=cs.get(i).trim();
            if(cell.startsWith("[noid]")) {
                fix.add(slot+": the Wiki lists "+cell.substring(6).trim()+" here, but its exact item ID is not confirmed yet, so the slot is left empty.");
                return null;
            }
            if(cell.startsWith("[generic]")) {
                fix.add(slot+": the Wiki names no specific item here (\""+cell.substring(9).trim()+"\"), so the slot is left empty.");
                return null;
            }
            if(cell.equals("N/A")) break; // no lesser option listed: fall back below
            Pick p=cell(cell,fix);
            if(p.taskSkipped()) continue; // task-only item: use the next column
            if(p.any() && p.item()!=null) anyUsed.add(p.item().name());
            if(p.item()!=null) return p.item();
            break;
        }
        // R04: the Wiki writes N/A when it lists no lesser option. A player at this tier keeps the
        // nearest better item the Wiki does list, rather than wearing nothing.
        for(int i=Math.min(c,cs.size())-1;i>=0;i--) {
            String cell=cs.get(i).trim();
            if(cell.startsWith("[generic]")||cell.startsWith("[noid]")) return null;
            if(cell.equals("N/A")) continue;
            Pick p=cell(cell,fix);
            if(p.item()==null) continue;
            if(p.any()) anyUsed.add(p.item().name());
            fellBack.add(slot);
            return p.item();
        }
        return null;
    }

    private static void build(Setup s,Tier t) {
        int n=columns(s), col=column(t,n);
        List<String> fix=new ArrayList<>();
        Set<String> anyUsed=new LinkedHashSet<>();
        List<String> fellBack=new ArrayList<>();
        WIDE.clear();
        Item[] gear=new Item[11];
        for(int i=0;i<11;i++) gear[i]=pickSlot(s.slots().get(SLOTS[i]),col,n,fix,SLOTS[i],anyUsed,fellBack);
        if(!fellBack.isEmpty()) fix.add("The Wiki lists no lower option for "+String.join(", ",fellBack)+" at this tier, so the nearest better item it lists is kept.");
        if(gear[4]==null) {
            var cs=cols(s.slots().get("Weapon"));
            for(int i=Math.min(col,cs.size()-1);i>=0 && gear[4]==null;i--) {
                Pick p=cell(cs.get(i),fix);
                if(p.item()!=null){gear[4]=p.item();fix.add("Weapon: the Wiki cell for this tier is empty; the next better column ("+p.item().name()+") is used.");}
            }
        }
        if(gear[4]==null) return; // never publish a loadout without a weapon
        String w=norm(gear[4].name());
        if(TWO_HANDED.contains(w) && gear[6]!=null) {
            fix.add("Shield: "+gear[6].name()+" left off because "+gear[4].name()+" is two-handed.");
            gear[6]=null;
        }
        gear[3]=coherentAmmo(s,s.slots().get("Ammo"),col,gear[3],gear[4],fix);
        Item[] inv=new Item[28];
        // R04: "Switch <Slot>:" rows are a second Wiki table (e.g. the Nightmare's totem mage gear) carried in the
        // inventory, picked at the same tier as the worn gear and skipped when identical to what is worn.
        List<String> switched=new ArrayList<>();
        int sn=1; for(var e:s.slots().entrySet()) if(e.getKey().startsWith("Switch ")) sn=Math.max(sn,cols(e.getValue()).size());
        int scol=column(t,sn); Item swWeapon=null;
        if(s.slots().get("Switch Weapon")!=null) {
            Item sw0=pickSlot(s.slots().get("Switch Weapon"),scol,sn,fix,"Switch Weapon",anyUsed,new ArrayList<>());
            if(sw0!=null && sw0.id()!=gear[4].id()) swWeapon=sw0;
        }
        for(String slot:SLOTS) {
            String row=s.slots().get("Switch "+slot);
            if(row==null) continue;
            Item it;
            if(slot.equals("Weapon")) it=swWeapon;
            else if(slot.equals("Shield")) {
                if(swWeapon!=null && TWO_HANDED.contains(norm(swWeapon.name()))) continue;
                it=pickSlot(row,scol,sn,fix,"Switch "+slot,anyUsed,new ArrayList<>());
            } else if(slot.equals("Ammo")) {
                if(swWeapon==null) continue;
                it=coherentAmmo(s,row,scol,pickSlot(row,scol,sn,new ArrayList<>(),"Switch Ammo",anyUsed,new ArrayList<>()),swWeapon,fix);
                if(it!=null && kind(it.name())==AmmoKind.NONE_NEEDED) continue; // blessings stay worn
            } else it=pickSlot(row,scol,sn,fix,"Switch "+slot,anyUsed,new ArrayList<>());
            if(it==null) continue;
            int gi=Arrays.asList(SLOTS).indexOf(slot);
            if(gear[gi]!=null && gear[gi].id()==it.id()) continue;
            add(inv,it,1,false,s); switched.add(it.name());
        }
        if(!switched.isEmpty()) fix.add("Switch gear carried in the inventory (same tier column of the Wiki switch table): "+String.join(", ",switched)+".");
        // Spec weapon for this tier. If the Wiki inventory already lists an item from the Spec row
        // (e.g. Venenatis lists Dinh's bulwark in a full 28), that inventory slot is swapped for this
        // tier's spec choice instead of adding a 29th item.
        Item sp=null; Set<String> specNames=new HashSet<>();
        if(s.spec()!=null) {
            var cs=cols(s.spec());
            for(String cell:cs) for(String a:cell.split(" / ")) specNames.add(norm(a.replace("[any]","").replace("[task]","").trim()));
            if(cs.size()>n && n>1) WIDE.add("Spec");
            int c=Math.min(cs.size()-1,rowColumn(col,n,cs.size()));
            for(int i=c;i>=0 && sp==null;i--) sp=cell(cs.get(i),fix).item();
        }
        boolean specPlaced=false;
        if(sp!=null && sp.id()==gear[4].id()) specPlaced=true; // equipped weapon already carries the spec
        if(sp!=null && switched.contains(sp.name())) specPlaced=true; // the switch weapon is the spec weapon
        String fill=null;
        if(s.inv()!=null) for(String part:s.inv().split(";")) {
            String p=part.trim();
            if(p.startsWith("FILL ")) { fill=p.substring(5).trim(); continue; }
            var m=java.util.regex.Pattern.compile("^(.*?)\\s+x(\\d+)(s?)$").matcher(p);
            if(!m.matches()) continue;
            String nm=m.group(1).trim();
            boolean keep=nm.endsWith("[keep]");
            if(keep) nm=nm.substring(0,nm.length()-6).trim();
            int q=Integer.parseInt(m.group(2));
            boolean stack=!m.group(3).isEmpty();
            if(!keep && sp!=null && q==1 && !stack && specNames.contains(norm(nm))) {
                if(specPlaced) { if(!norm(nm).equals(norm(sp.name()))) fix.add("Spec: the Wiki inventory's "+nm+" is left out because "+gear[4].name()+" is the spec weapon at this tier."); continue; }
                if(!norm(nm).equals(norm(sp.name()))) fix.add("Spec: the Wiki inventory's "+nm+" slot holds this tier's spec weapon ("+sp.name()+").");
                add(inv,sp,1,false,s); specPlaced=true; continue;
            }
            if(keep && sp!=null && norm(nm).equals(norm(sp.name()))) specPlaced=true;
            Integer id=IDS.get(norm(nm));
            if(id==null){UNRESOLVED.add(nm);continue;}
            if(ANY.contains(norm(nm))) anyUsed.add(nm);
            // R07: where the Wiki states no counts, a single listed item is shown as "×?" (amount -1), never as a real count of 1.
            boolean unstated=countsUnstated(s.notes());
            int amt=stack?q:(unstated && q==1?-1:1);
            add(inv,new Item(id,nm,amt),stack?1:q,stack,s);
        }
        if(sp!=null && !specPlaced) {
            if(Arrays.asList(inv).contains(null)) add(inv,sp,1,false,s);
            else fix.add("Spec: "+sp.name()+" is this tier's spec weapon but is not carried, because the Wiki inventory already fills all 28 slots.");
        }
        if(fill!=null) {
            Integer id=IDS.get(norm(fill));
            if(id==null) UNRESOLVED.add(fill);
            else for(int i=0;i<28;i++) if(inv[i]==null) inv[i]=new Item(id,fill,1);
        }
        if(!WIDE.isEmpty()) { fix.add("CHECK: the transcribed Wiki row(s) for "+String.join(", ",WIDE)+" have more columns than the table; they are read proportionally until re-checked against the Wiki."); NEEDS_CHECK.add(s.boss()+" / "+s.label()+": "+String.join(", ",WIDE)); }
        List<String> notes=new ArrayList<>();
        notes.add("BUILD 20.1 DRAFT · OSRS Wiki strategy table ("+READ_DATE+"): "+t.label()+" = the Wiki's "+
            (t==Tier.OPTIMAL?"first (best)":t==Tier.RECOMMENDED?"middle":"last")+" column. Not an official Wiki tier and not a bank or readiness check.");
        notes.addAll(s.notes());
        if(s.req()!=null) notes.add("Requirements: "+s.req()+".");
        notes.addAll(fix);
        if(!anyUsed.isEmpty()) notes.add("The Wiki names a family here (any god / any variant): "+String.join(", ",anyUsed)+" shown.");
        if(BossRegistry.wilderness(s.boss())) notes.add("WILDERNESS: PvP / PK risk. Best in Bank stays off for this boss.");
        String method="Wiki · "+s.label();
        Profile prof=new Profile(s.boss(),method,s.style(),s.url(),Arrays.asList(gear),Arrays.asList(inv),notes);
        String key=s.boss()+"\u0000"+method+"\u0000"+t;
        var sel=new Selection(prof,Authority.TPT_CURATED_WIKI_STRATEGY,s.url()+" (read "+READ_DATE+")");
        if(VerifiedOutfitterCatalog.BOSSES.contains(s.boss())) { HELD_DATA.put(key,sel); HELD_BOSSES.add(s.boss()); return; }
        DATA.put(key,sel);
        var ms=METHODS.computeIfAbsent(s.boss(),k->new ArrayList<>());
        if(!ms.contains(method)) ms.add(method);
    }
    /** Never throws during class init: an overflow is recorded (tests assert none) and the extra item dropped. */
    private static void add(Item[] inv,Item item,int count,boolean stack,Setup s) {
        for(int n=0;n<count;n++) {
            int j=Arrays.asList(inv).indexOf(null);
            if(j<0) { OVERFLOW.add(s.boss()+" / "+s.label()+": "+item.name()); return; }
            inv[j]=item;
        }
    }
    /** Setups whose transcribed inventory would exceed 28 slots (should stay empty). */
    public static List<String> overflow(){return List.copyOf(OVERFLOW);}


    // ---- Ammo coherence (R04): the Wiki ammo row is ranked per column, but a tier's weapon can need a
    // different ammo type than that column lists (or the column is N/A). Never publish a crossbow without bolts.
    enum AmmoKind { NONE_NEEDED, ARROW, BOLT, ASCENSION_BOLT, ANTLER_BOLT, OTHER }
    static AmmoKind needs(String weapon) {
        String w=norm(weapon);
        if(w.equals("ascension crossbows")) return AmmoKind.ASCENSION_BOLT;
        if(w.startsWith("hunters' sunlight crossbow")||w.startsWith("hunters' crossbow")) return AmmoKind.ANTLER_BOLT;
        if(w.equals("karil's crossbow")) return AmmoKind.OTHER;
        if(w.contains("crossbow")) return AmmoKind.BOLT;
        if(w.equals("bow of faerdhinen")||w.equals("bow of faerdhinen (c)")||w.equals("crystal bow")||w.equals("webweaver bow")||w.equals("craw's bow")) return AmmoKind.NONE_NEEDED;
        if(w.equals("twisted bow")||w.equals("scorching bow")||w.equals("venator bow")||w.contains("shortbow")||w.contains("longbow")||w.equals("dark bow")||w.equals("composite bow")) return AmmoKind.ARROW;
        if(w.equals("eclipse atlatl")||w.equals("toxic blowpipe")) return AmmoKind.OTHER;
        return AmmoKind.OTHER;
    }
    static AmmoKind kind(String ammo) {
        String a=norm(ammo);
        if(a.contains("ascension bolts")) return AmmoKind.ASCENSION_BOLT;
        if(a.contains("antler bolts")) return AmmoKind.ANTLER_BOLT;
        if(a.contains("bolts")) return AmmoKind.BOLT;
        if(a.contains("arrow")) return AmmoKind.ARROW;
        if(a.contains("blessing")) return AmmoKind.NONE_NEEDED;
        return AmmoKind.OTHER;
    }
    private static boolean fits(AmmoKind need,AmmoKind have) {
        if(need==AmmoKind.OTHER) return true; // not modelled: keep the Wiki cell
        if(need==AmmoKind.NONE_NEEDED) return have==AmmoKind.NONE_NEEDED;
        return need==have;
    }
    private static Item coherentAmmo(Setup setup,String row,int col,Item current,Item weapon,List<String> fix) {
        if(row==null||weapon==null) return current;
        AmmoKind need=needs(weapon.name());
        if(need==AmmoKind.OTHER) return current;
        // W07: matching the projectile family is not enough. Rune crossbows, for example,
        // cannot fire dragon bolts even though both are BOLT-type items.
        if(current!=null && fits(need,kind(current.name())) &&
            (need!=AmmoKind.BOLT || gg.feral.burrow.bib.AmmoRules.check(weapon.name(),current.name())
                ==gg.feral.burrow.bib.AmmoRules.Status.OK)) return current;
        var cs=cols(row);
        List<Integer> order=new ArrayList<>();
        if(col<cs.size()) order.add(col);
        for(int i=0;i<cs.size();i++) if(i!=col) order.add(i);
        for(int c:order) {
            String cell=cs.get(c).trim();
            if(cell.equals("N/A")||cell.startsWith("[generic]")) continue;
            for(String alt:cell.split(" / ")) {
                String a=alt.trim();
                if(a.contains("[task]")) continue;
                a=a.replace("[any]","").trim();
                Integer id=IDS.get(norm(a));
                if(id==null) continue;
                if(fits(need,kind(a)) &&
                    (need!=AmmoKind.BOLT || gg.feral.burrow.bib.AmmoRules.check(weapon.name(),a)
                        ==gg.feral.burrow.bib.AmmoRules.Status.OK)) {
                    fix.add("Ammo: "+(current==null?"the Wiki cell for this tier is empty":current.name()+" does not fit "+weapon.name())
                        +", so "+a+" is used from the same Wiki ammo row.");
                    return new Item(id,a,1);
                }
            }
        }
        if(need==AmmoKind.NONE_NEEDED) {
            if(current!=null) fix.add("Ammo: "+current.name()+" is left off because "+weapon.name()+" uses no arrows or bolts.");
            return null;
        }
        // W07: Kree'arra tank table has Twisted bow as its last-column alternative,
        // but that *crossbow-oriented* table has no arrow column. The SAME boss's
        // Tbow/Bowfa Wiki strategy table explicitly lists Seeking dragon arrow.
        // Do not manufacture arrows for any other boss or any other bow.
        if (need==AmmoKind.ARROW && sLabelIsKreeTankFallback(setup,weapon,row)) {
            Integer arrowId=IDS.get(norm("Seeking dragon arrow"));
            if(arrowId!=null) {
                fix.add("Ammo: Kree'arra tank table has no Twisted bow arrows; Seeking dragon arrow is sourced from the same boss's Tbow/Bowfa Wiki strategy. Trip stack quantity requires owner review.");
                return new Item(arrowId,"Seeking dragon arrow",1);
            }
        }
        fix.add("Ammo: the Wiki ammo row has nothing that fits "+weapon.name()+"; the ammo slot is left empty. Check before you go.");
        return null;
    }

    private static boolean sLabelIsKreeTankFallback(Setup setup,Item weapon,String row) {
        return setup!=null && setup.boss().equals("Kree'arra") && setup.label().equals("Ranged (tank)")
            && norm(weapon.name()).equals("twisted bow") && row!=null &&
            row.startsWith("Diamond dragon bolts (e) > Diamond bolts (e) > Ruby bolts (e)");
    }

    /** R05: the combat style of a setup's switch weapon (e.g. the Nightmare totem staff = MAGIC), if any. */
    public static Optional<CombatStyle> switchStyle(String boss,String method) {
        for(Setup su:SETUPS) {
            if(!su.boss().equals(boss) || !method.equals("Wiki · "+su.label())) continue;
            String row=su.slots().get("Switch Weapon");
            if(row==null) return Optional.empty();
            String w=norm(cols(row).get(0).split(" / ")[0].replaceAll("\\[\\w+\\]","").trim());
            if(w.matches(".*(bow|crossbow|blowpipe|chinchompa|atlatl|ballista|javelin|dart|knife|thrownaxe).*")) return Optional.of(CombatStyle.RANGED);
            if(w.matches(".*(staff|sceptre|wand|trident|shadow|tome|ayak).*")) return Optional.of(CombatStyle.MAGIC);
            return Optional.of(CombatStyle.MELEE);
        }
        return Optional.empty();
    }
    /** R07: note wordings used in the transcription for "the Wiki gives no inventory counts". */
    static boolean countsUnstated(List<String> notes) {
        for(String n:notes) {
            String x=n.toLowerCase(Locale.ROOT);
            if(x.contains("counts not stated")||x.contains("counts are not stated")||x.contains("gives no counts")
                ||x.contains("images without counts")||x.contains("not readable in full")) return true;
        }
        return false;
    }
    /** Held-back setups (protected original bosses), for audit and tests only; never shown in the picker. */
    public static Set<String> heldBosses(){return Collections.unmodifiableSet(HELD_BOSSES);}
    public static Selection findHeld(String boss,String method,Tier tier){return HELD_DATA.get(boss+"\u0000"+method+"\u0000"+tier);}
    public static int heldSize(){return HELD_DATA.size();}
    public static Selection find(String boss,String method,Tier tier){return DATA.get(boss+"\u0000"+method+"\u0000"+tier);}
    /** The Wiki weapon row for this setup, best first, as item IDs. Empty for non-Wiki methods. */
    public static List<Integer> weaponOrder(String boss,String method) {
        Set<Integer> order=new LinkedHashSet<>();
        for(Setup su:SETUPS) {
            if(!su.boss().equals(boss) || !method.equals("Wiki · "+su.label())) continue;
            for(String cell:cols(su.slots().get("Weapon"))) for(String alt:cell.split(" / ")) {
                Integer id=IDS.get(norm(alt.replaceAll("\\[\\w+\\]","")));
                if(id!=null) order.add(id);
            }
        }
        return List.copyOf(order);
    }
    /** True when this item sits in one of the setup's Switch rows, so the fight plan depends on it. */
    public static boolean requiredSwitch(String boss,String method,String item) {
        for(Setup su:SETUPS) {
            if(!su.boss().equals(boss) || !method.equals("Wiki · "+su.label())) continue;
            for(var e:su.slots().entrySet()) {
                if(!e.getKey().startsWith("Switch ")) continue;
                for(String cell:cols(e.getValue())) for(String alt:cell.split(" / "))
                    if(norm(alt.replaceAll("\\[\\w+\\]","")).equals(norm(item))) return true;
            }
        }
        return false;
    }
    /** The strategy data's display name for an item ID. */
    public static Optional<String> itemName(int id) {
        for(var e:IDS.entrySet()) if(e.getValue()==id) return Optional.of(e.getKey());
        return Optional.empty();
    }
    public static List<String> methods(String boss){return METHODS.getOrDefault(boss,List.of());}
    public static Set<String> bosses(){return METHODS.keySet();}
    public static int size(){return DATA.size();}
    public static List<Setup> setups(){return List.copyOf(SETUPS);}
    /** Names in the transcription that did not map to a verified item ID (should stay empty). */
    public static List<String> unresolved(){return List.copyOf(UNRESOLVED);}
}
