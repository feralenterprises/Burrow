package gg.feral.burrow.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.*;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.*;

/** Cross-language 51-profile fixture for BUR-0039s F06-derived Muspah correction; original FINAL-06 remains in source archives.
 * This is a research-data fidelity test, NOT live unlock/stock or native-client verification. */
public final class Final06CatalogTest {
    private static int checks;
    private static void check(boolean condition, String what) {
        checks++; if(!condition)throw new AssertionError("FINAL-06: "+what);
    }
    private static String sha256(byte[] bytes) throws Exception {
        var digest=MessageDigest.getInstance("SHA-256").digest(bytes);
        var out=new StringBuilder();for(byte b:digest)out.append(String.format("%02x",b&255));return out.toString();
    }
    private static int[] slots(String values) {
        String[] pair=values.split(",",-1);int[] out=new int[pair.length*2];
        for(int i=0;i<pair.length;i++) {
            String[] parts=pair[i].split(":",-1);
            out[2*i]=Integer.parseInt(parts[0]);out[2*i+1]=Integer.parseInt(parts[1]);
        }
        return out;
    }
    private static void verify(List<Item> items,String spec,int expectedCount,String id) {
        var expected=slots(spec);
        check(items.size()==expectedCount && expected.length==2*expectedCount,id+" geometry");
        for(int i=0;i<expectedCount;i++) {
            var item=items.get(i);int actualId=item==null?0:item.id();int amount=item==null?0:item.amount();
            check(actualId==expected[2*i] && amount==expected[2*i+1],id+" position "+i);
            check(item==null || (item.id()>0 && item.amount()>0),id+" concrete item requirement "+i);
        }
    }
    public static void main(String[] args) throws Exception {
        String hash="165f0781ab4e0e738235833e51dee04c21f30825cc8b3ab421c051e48e6dbe98";
        check(TieredOutfitterCatalog.DATASET_SHA256.equals(hash),"catalog source hash");
        check(Final06ProfileMetadata.DATASET_SHA256.equals(hash),"metadata source hash");
        try(var stream=Final06CatalogTest.class.getResourceAsStream(Final06ProfileMetadata.RESOURCE)) {
            check(stream!=null,"full source payload embedded");
            check(sha256(stream.readAllBytes()).equals(hash),"full source bytes are byte-identical");
        }
        int count=0;Set<String> bosses=new HashSet<>(),methods=new HashSet<>();
        var fixture=Final06CatalogTest.class.getResourceAsStream("/gg/feral/burrow/outfitter/final06-expected.tsv");
        check(fixture!=null,"expected manifest installed");
        try(var reader=new BufferedReader(new InputStreamReader(fixture,StandardCharsets.UTF_8))) {
            for(String line; (line=reader.readLine())!=null;) {
                String[] f=line.split("\\t",-1);check(f.length==6,"expected manifest shape");
                String boss=f[0],method=f[1];Tier tier=Tier.valueOf(f[2]);
                var selected=TieredOutfitterCatalog.find(boss,method,tier);
                check(selected!=null,boss+" / "+method+" / "+tier+" exists");
                var p=selected.profile();
                check(p.boss().equals(boss) && p.method().equals(method) && p.style().equals(f[3]),"selection metadata");
                verify(p.gear(),f[4],11,"gear "+boss+" "+tier);
                verify(p.inventory(),f[5],28,"inventory "+boss+" "+tier);
                check(selected.provenance().startsWith("https://oldschool.runescape.wiki/"),"wiki provenance");
                var meta=Final06ProfileMetadata.find(boss,method,tier);
                check(meta!=null && meta.verdict().equals("APPROVED WITH CONDITIONS") && !meta.finalConditions().isEmpty(),"conditional gate exists");
                check(meta.wikiSource().equals(p.source()),"source alignment");
                check(!meta.tripObjective().isBlank() && !meta.spellbook().isBlank(),"trip and spellbook");
                check(!meta.questRequirements().isEmpty(),"quest/skill prerequisites");
                // Profile/model must preserve exact deliberate empties in the 56-cell RuneLite bank projection.
                Outfitter.Plan plan=Outfitter.resolve(1,new Outfitter.Scope("F06-TEST",1),p.template(),"Recommended",null);
                check(plan.gear().size()==11 && plan.inventory().size()==28,"physical template projection");
                for(int i=0;i<11;i++) {
                    var source=p.gear().get(i);var cell=plan.gear().get(i);
                    check((source==null)==(cell.requirement()==null),"gear projection position "+i);
                }
                for(int i=0;i<28;i++) {
                    var source=p.inventory().get(i);var cell=plan.cells().get(Outfitter.inventoryPosition(i));
                    check((source==null)==(cell.requirement()==null),"inventory projection position "+i);
                }
                count++;bosses.add(boss);methods.add(boss+" / "+method);
            }
        }
        check(count==51 && bosses.size()==15 && methods.size()==17,"exact canonical coverage");
        check(TieredOutfitterCatalog.size()==51 && Final06ProfileMetadata.size()==51,"no duplicate/extra selection");
        check(TieredOutfitterCatalog.find("Scurrius","not-a-method",Tier.ENTRY)==null,"no borrowed fallback");
        check(Final06ProfileMetadata.find("Scurrius","not-a-method",Tier.ENTRY)==null,"no borrowed metadata fallback");
        var scythe=TieredOutfitterCatalog.find("Scurrius","Standard melee",Tier.OPTIMAL).profile();
        check(scythe.gear().get(4).id()==22325 && scythe.gear().get(6)==null,"two-handed scythe requires empty offhand");
        var vorkath=Final06ProfileMetadata.find("Vorkath","Ranged crossbow",Tier.ENTRY);
        check(!vorkath.runePouchContents().isEmpty(),"Vorkath rune pouch preserved");
        var vorkathRanged = TieredOutfitterCatalog.find("Vorkath", "Ranged crossbow", Tier.RECOMMENDED).profile();
        check(vorkathRanged.gear().get(9).id()==19921,"Vorkath Recommended Ranged uses blessed d'hide boots instead of Dragon boots");
        check(Final06ProfileMetadata.find("Vorkath", "Ranged crossbow", Tier.RECOMMENDED)
            .itemAcquisitionConditions().stream().anyMatch(x->x.contains("19921") && x.contains("Hard Treasure Trails")),
            "Vorkath recommended boots have accurate acquisition disclosure");
        check(TieredOutfitterCatalog.find("Vorkath", "Ranged crossbow", Tier.ENTRY).profile().gear().get(9).id()==3105,
            "Vorkath Entry boots unchanged");
        check(TieredOutfitterCatalog.find("Vorkath", "Ranged crossbow", Tier.OPTIMAL).profile().gear().get(9).id()==31097,
            "Vorkath Optimal boots unchanged");
        check(TieredOutfitterCatalog.find("Phantom Muspah", "Ranged-only, Twisted bow with Zaryte crossbow shield-phase switch", Tier.RECOMMENDED)
            .profile().gear().get(9).id()==19933,"Muspah reviewed blessed boots unchanged");
        try {vorkath.finalConditions().add("hack");throw new AssertionError("mutable metadata");}
        catch(UnsupportedOperationException expected){checks++;}
        System.out.println("FINAL06 EXACT DATA CATALOG PASS: "+checks+" assertions, 15 bosses / 17 methods / 51 profiles, "+hash);
    }
}
