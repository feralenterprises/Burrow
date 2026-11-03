package gg.feral.burrow.runelite;

import gg.feral.burrow.core.ChatKcParser;
import java.util.*;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreSkill;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Build 20 KC sources: game message (absolute) and official HiScores mapping. */
final class Build20KcSourcesJUnitTest {
    @Test void chatKcIsAbsoluteAndMappedToEncounters() {
        assertEquals(new ChatKcParser.Kc("Vorkath",1234),ChatKcParser.parse("Your Vorkath kill count is: <col=ff0000>1,234</col>.").orElseThrow());
        assertEquals("Dagannoth Rex",ChatKcParser.parse("Your Dagannoth Rex kill count is: 5.").orElseThrow().encounter());
        assertEquals("Hueycoatl",ChatKcParser.parse("Your The Hueycoatl kill count is: 3.").orElseThrow().encounter());
        assertTrue(ChatKcParser.parse("Fight duration: 1:23").isEmpty());
        assertTrue(ChatKcParser.parse("Player says: Your Vorkath kill count is: 99999.").isEmpty(),"only the game's own sentence");
    }
    @Test void hiscoresMapOnlyRealBossEntries() {
        assertEquals(HiscoreSkill.VORKATH,HiscoreKc.skillFor("Vorkath").orElseThrow());
        assertEquals(HiscoreSkill.DAGANNOTH_REX,HiscoreKc.skillFor("Dagannoth Rex").orElseThrow());
        assertEquals(HiscoreSkill.THE_HUEYCOATL,HiscoreKc.skillFor("Hueycoatl").orElseThrow());
        assertTrue(HiscoreKc.skillFor("Lizardman Shamans").isEmpty(),"no HiScores entry is invented");
        assertEquals(HiscoreEndpoint.IRONMAN,HiscoreKc.endpointFor(1));
        assertEquals(HiscoreEndpoint.NORMAL,HiscoreKc.endpointFor(0));
        assertTrue(HiscoreKc.kc(null,HiscoreSkill.VORKATH).isEmpty(),"unavailable stays unknown");
    }
}
