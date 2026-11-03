package com.feralenterprises.hoverlite;

import org.junit.Test;
import static org.junit.Assert.*;

public class HoverTextTest
{
    @Test public void dukeFull() {
        assertEquals("Feed <col=ffff00>Duke Sucellus</col> (level-758)",
            HoverText.format(HoverMode.FULL, "Feed", "<col=ffff00>Duke Sucellus</col> (level-758)"));
    }
    @Test public void dukeActionOnly() {
        assertEquals("Feed", HoverText.format(HoverMode.ACTION_ONLY, "Feed", "Duke Sucellus (level-758)"));
    }
    @Test public void inventoryFull() {
        assertEquals("Eat <col=ff9040>Shark</col>",
            HoverText.format(HoverMode.FULL, "Eat", "<col=ff9040>Shark</col>"));
    }
    @Test public void indicatorOnly() {
        assertEquals("<col=65eeb0>●</col>", HoverText.format(HoverMode.INDICATOR, "Feed", "Duke"));
    }
    @Test public void hideAndGuard() {
        assertNull(HoverText.format(HoverMode.HIDDEN, "Feed", "Duke"));
        assertNull(HoverText.format(HoverMode.FULL, "", "Duke"));
        assertNull(HoverText.format(HoverMode.FULL, null, "Duke"));
    }
    @Test public void missingTarget() {
        assertEquals("Use", HoverText.format(HoverMode.FULL, "Use", null));
    }
}
