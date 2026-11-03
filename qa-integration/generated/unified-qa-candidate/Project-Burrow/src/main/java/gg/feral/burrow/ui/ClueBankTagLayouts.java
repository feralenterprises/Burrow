package gg.feral.burrow.ui;

import java.util.*;

/** Wiki Treasure Trails/Strategies bank-tag layout references supplied by the owner.
 * Positions 0..55 are BANK-TAG 8x7 positions, NOT 4x7 player inventory positions.
 * Values are OSRS item IDs, NOT item quantities and NOT evidence of bank ownership.
 * The host's RuneLite ItemManager supplies sprites when running in game.
 * Source: https://oldschool.runescape.wiki/w/Treasure_Trails/Strategies
 * Data recorded 2026-11-02. No account data or automated item actions.
 */
public final class ClueBankTagLayouts {
    public static final String SOURCE="https://oldschool.runescape.wiki/w/Treasure_Trails/Strategies";
    public record Layout(String id,String title,String section,String original,String raw,int tagIconId,int[] slots) {
        public Layout { slots=slots.clone(); }
        @Override public int[] slots(){return slots.clone();}
        public int count(){int c=0;for(int i:slots)if(i>0)c++;return c;}
    }
    private static Layout parse(String id,String title,String section,String raw) {
        String[] s=raw.split(",",-1);
        if(s.length<7||!s[0].equals("banktags")||!s[1].equals("1")||!s[4].equals("layout")||((s.length-5)%2)!=0)
            throw new IllegalArgumentException("invalid bank tag export");
        int[] slots=new int[56];
        for(int i=5;i<s.length;i+=2){
            int pos=Integer.parseInt(s[i]),item=Integer.parseInt(s[i+1]);
            if(pos<0||pos>=56||item<=0||slots[pos]!=0)throw new IllegalArgumentException("invalid/duplicate bank position: "+pos);
            slots[pos]=item;
        }
        return new Layout(id,title,section,s[2],raw,Integer.parseInt(s[3]),slots);
    }
    private static final List<Layout> ALL=List.of(
        parse("clue-beginner-f2p","Beginner · F2P","Beginner clues (free-to-play)","banktags,1,Beginner clues,23182,layout,54,1351,5,9013,6,563,7,556,8,1007,12,3010,14,554,16,1333,20,3010,32,1065,33,1061,17,1127,21,3010,25,1079,34,1635,9,1725,13,3010,15,557,18,13660,22,555,52,12297,55,1654,53,952,1,1949"),
        parse("clue-beginner-members","Beginner · Members","Beginner clues (members)","banktags,1,Beginner clues,23182,layout,54,952,5,9013,6,563,7,4699,8,22109,12,19615,16,12926,20,13660,32,11126,33,11860,17,2503,25,2497,34,2562,9,1704,13,12780,15,4695,55,12627,53,12297,1,11850"),
        parse("clue-easy","Easy · General","Easy clues","banktags,1,Easy clues,12179,layout,38,8007,46,4696,4,12179,5,952,6,21389,7,13069,8,13280,12,13393,14,11968,16,11787,20,13128,28,2552,32,11126,33,13132,17,11854,21,26818,25,11856,29,21276,34,2572,9,19707,13,3853,15,21146,18,25818,22,8011,30,8009,36,12625,47,566,1,13140,39,12791,23,8010,31,8008"),
        parse("clue-medium","Medium · General","Medium Clue Scrolls","banktags,1,Medium Clue Scrolls,2829,layout,38,2829,54,24363,4,952,5,27281,6,19476,7,9813,8,13280,10,22947,12,21268,14,13124,16,21276,20,21146,28,11105,32,11972,40,4696,33,13132,41,4694,17,11854,21,3853,25,13115,29,21389,34,26818,42,561,9,13393,43,563,13,19707,15,13221,18,25818,22,29893,30,12780,36,11980,55,20545,37,29577,53,11248,1,11850,23,2552,31,4446"),
        parse("clue-medium-hunting","Medium · Hunt implings","Medium clues (hunting your own implings)","banktags,1,Medium clues,2829,layout,38,11260,46,11260,4,11259,5,952,6,3853,7,11113,8,11852,10,22947,12,21760,14,21151,16,6562,20,13393,28,12791,32,11858,40,563,33,11860,41,556,17,11854,21,772,25,11856,29,11258,34,21268,42,561,9,22400,13,21387,15,2562,18,20714,22,2357,30,20545,36,11260,44,11260,47,11260,37,11260,45,11260,1,11850,39,11260,23,8778"),
        parse("clue-hard-implings","Hard · Implings","Hard clues (from implings)","banktags,1,Hard clues,3550,layout,46,4251,54,566,4,3550,5,952,6,3853,7,21166,8,13280,12,12625,14,21151,16,12926,20,3024,28,6685,32,11126,33,13132,17,2503,21,9813,25,13115,29,13111,34,21268,9,19707,13,12789,15,11113,22,2572,30,13124,36,171,44,9075,52,563,55,12791,47,21387,45,561,53,565,1,13140,23,2552,31,13103"),
        parse("clue-hard-other","Hard · Other sources","Hard clues (other sources)","banktags,1,Hard clues,3550,layout,4,3550,5,12695,6,13393,7,563,8,13280,12,952,14,21387,16,12926,20,13652,28,6685,32,11126,33,13132,17,2503,21,12625,25,13115,29,3024,34,21268,9,19707,13,2444,15,4699,22,12789,1,13140,23,4695"),
        parse("clue-elite","Elite · General","Elite clues","banktags,1,Elite clues,21524,layout,38,954,54,12695,4,21524,5,4699,6,565,7,566,8,13280,10,22947,12,952,14,561,16,12926,20,13393,28,13111,32,10368,33,13132,17,12508,21,21387,25,13115,29,12789,34,21268,9,19707,13,4695,15,12791,22,9813,30,6707,36,23904,44,13652,52,12625,55,2444,37,3690,53,3024,1,13140,39,21541,23,19476,31,13136"),
        parse("clue-master","Master · General","Master clues","banktags,1,Master clues,19835,layout,38,13441,4,19835,5,13221,6,9813,7,19476,8,13280,10,22947,12,952,14,13103,16,12926,20,21387,28,13652,32,10368,33,13132,17,2503,21,13393,25,13115,29,12625,34,21268,9,19707,13,19939,15,13136,22,12789,30,12695,37,13441,1,13140,39,13441,23,13111,31,2444")
    );
    public static List<Layout> all(){return ALL;}
    public static Layout find(String id){return ALL.stream().filter(l->l.id().equals(id)).findFirst().orElse(null);}
    private ClueBankTagLayouts(){}
}
