package gg.feral.burrow.runelite;

import gg.feral.burrow.core.BurrowModel;
import java.awt.Rectangle;
import java.util.Arrays;
import net.runelite.api.ScriptEvent;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.WidgetInfo;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** SPORKS P1: simulated bank-widget lifecycles, NOT evidence of a native RuneLite pass. */
class BankOHeaderLifecycleJUnitTest {
    private static OutfitterBankJUnitTest.HeaderHarness harness() {
        return new OutfitterBankJUnitTest.HeaderHarness();
    }

    @Test void firstOpenHeaderWorksBeforeBankItemAndContentWidgetsExist() {
        var h = harness();
        h.header.symbols.remove(WidgetInfo.BANK_ITEM_CONTAINER);
        h.header.symbols.remove(WidgetInfo.BANK_CONTENT_CONTAINER);
        var e = h.adapter.refresh();
        assertNotNull(e, "valid ordinary bank chrome, not the item grid, owns the O control");
        assertEquals("O", h.header.get(e.child).text);
        assertEquals(1, h.header.attachedEntries());
        int[] clicked = {0};
        h.adapter.bind(e, () -> clicked[0]++);
        h.press++;
        h.header.op(e.child, 1);
        assertEquals(1, clicked[0]);
        assertSame(e, h.adapter.refresh());
    }

    @Test void delayedHeaderChildIsRetriedAndNativeActionIsAvailableOnFirstValidTick() {
        var h = harness();
        var nativeSettings = h.header.settings;
        h.header.symbols.remove(WidgetInfo.BANK_SETTINGS_BUTTON);
        assertNull(h.adapter.refresh());
        assertEquals(0, h.header.attachedEntries());
        h.header.symbols.put(WidgetInfo.BANK_SETTINGS_BUTTON, nativeSettings);
        var e = h.adapter.refresh();
        assertNotNull(e);
        assertEquals(1, h.header.attachedEntries());
        h.adapter.bind(e, () -> {});
        assertNotNull(h.adapter.pressBounds());
        assertEquals(1, h.header.nativeHover(h.adapter.pressBounds().x + 2, h.adapter.pressBounds().y + 2).size());
    }

    @Test void bankTagsItemSwapDoesNotRecreateOrUnbindStableHeader() {
        var h = harness();
        var e = h.adapter.refresh();
        int[] pressed = {0};
        h.adapter.bind(e, () -> pressed[0]++);
        h.header.items.hidden = true;
        h.header.content.hidden = true;
        h.header.symbols.remove(WidgetInfo.BANK_ITEM_CONTAINER);
        h.header.symbols.remove(WidgetInfo.BANK_CONTENT_CONTAINER);
        h.header.root.statics = Arrays.stream(h.header.root.statics)
            .filter(w -> w != h.header.content.widget).toArray(net.runelite.api.widgets.Widget[]::new);
        assertSame(e, h.adapter.refresh(), "replaced bank grid cannot invalidate a valid header");
        assertSame(e, h.adapter.entry());
        h.press++;
        h.header.op(e.child, 1);
        assertEquals(1, pressed[0]);
        assertEquals(1, h.header.attachedEntries());
    }

    @Test void rootReallocatedBeforeRefreshCannotDispatchStaleBoundAction() {
        var h = harness();
        var e = h.adapter.refresh();
        int[] pressed = {0};
        h.adapter.bind(e, () -> pressed[0]++);
        h.header.rebuild();
        h.press++;
        h.header.op(e.child, 1); // Old listener can still arrive before next client tick.
        assertEquals(0, pressed[0]);
        var replacement = h.adapter.refresh();
        assertNotNull(replacement);
        assertNotSame(e, replacement);
        assertTrue(h.header.get(e.child).hidden);
        assertEquals(1, h.header.attachedEntries());
    }

    @Test void movedNativeHeaderRevokesPriorClickAndRepositionsWithoutDuplication() {
        var h = harness();
        var e = h.adapter.refresh();
        int[] pressed = {0};
        h.adapter.bind(e, () -> pressed[0]++);
        Rectangle oldBox = h.adapter.pressBounds();
        h.header.settings.box.translate(84, 0);
        h.press++;
        h.header.op(e.child, 1);
        assertEquals(0, pressed[0], "entry must not act after native header geometry moves");
        var moved = h.adapter.refresh();
        assertNotNull(moved);
        assertNotSame(e, moved);
        assertNotEquals(oldBox, h.adapter.pressBounds());
        assertEquals(1, h.header.attachedEntries());
    }

    @Test void foreignHeaderControlOccupiesItsOwnRectangleAndSurvivesCleanup() {
        var h = harness();
        var original = h.adapter.refresh();
        var originalBox = h.adapter.pressBounds();
        h.adapter.clear();
        var foreign = h.header.extra(originalBox, "Foreign action");
        var next = h.adapter.refresh();
        assertNotNull(next, "a different collision-free native slot exists");
        assertFalse(h.adapter.pressBounds().intersects(foreign.bounds()));
        h.adapter.clear();
        assertSame(foreign.widget, h.header.root.children[foreign.index]);
        assertEquals(0, h.header.attachedEntries());
        assertNotSame(original, next);
    }

    @Test void bankClosePinAndUnloadedHeaderRemoveOwnedControlsAndAllowCleanReopen() {
        var h = harness();
        var first = h.adapter.refresh();
        int[] pressed = {0};
        h.adapter.bind(first, () -> pressed[0]++);
        var callback = (JavaScriptCallback) h.header.get(first.child).op[0];
        h.open = false;
        assertNull(h.adapter.refresh());
        assertEquals(0, h.header.attachedEntries());
        h.open = true;
        assertNotNull(h.adapter.refresh());
        h.header.pin = true;
        assertNull(h.adapter.refresh());
        assertEquals(0, h.header.attachedEntries());
        h.header.pin = false;
        assertNotNull(h.adapter.refresh());
        h.logged = false;
        assertNull(h.adapter.refresh());
        assertEquals(0, h.header.attachedEntries());
        h.logged = true;
        assertNotNull(h.adapter.refresh());
        h.adapter.clear();
        assertEquals(0, h.header.attachedEntries());
        h.press++;
        callback.run(BankHeaderFixture.proxy(ScriptEvent.class, (method, args) -> switch(method) {
            case "getSource" -> first.child;
            case "getOp" -> 1;
            default -> throw new AssertionError(method);
        }));
        assertEquals(0, pressed[0]);
    }

    @Test void bestBankPendingDoesNotHideOOrOpenUnverifiedGear() {
        var h = harness();
        var provider = new OutfitterBankJUnitTest.Provider();
        var model = new BurrowModel();
        var controller = new OutfitterBank(h.client, model::bankSelection, () -> provider);
        controller.setBestBankRequested(() -> true);
        controller.setBestBankOverride(() -> null);
        controller.refresh();
        var button = h.header.all.stream().filter(n -> n.text.equals("O") && !n.hidden)
            .findFirst().orElseThrow();
        h.press++;
        h.header.op(button.widget, 1);
        assertEquals(0, provider.opens, "pending verified Best in Bank must not open a Wiki or stale layout");
        assertEquals(1, h.header.attachedEntries(), "O visibility does not certify item ownership");
    }

    @Test void settingsPanelOrHiddenNativeControlsCannotHostO() {
        var h = harness();
        var first = h.adapter.refresh();
        assertNotNull(first);
        h.header.settings.hidden = true;
        assertNull(h.adapter.refresh());
        assertEquals(0, h.header.attachedEntries());
        h.header.settings.hidden = false;
        assertNotNull(h.adapter.refresh());
    }
}
