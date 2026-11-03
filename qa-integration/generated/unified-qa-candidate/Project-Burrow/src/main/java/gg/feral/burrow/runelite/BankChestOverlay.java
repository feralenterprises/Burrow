package gg.feral.burrow.runelite;

import gg.feral.burrow.ui.BankChestPixels;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Presentation-only overlay above bank widgets; does not own any click or menu events. */
final class BankChestOverlay extends Overlay {
    private final Client client;
    private final Supplier<OutfitterBank> bank;
    BankChestOverlay(Client client, Supplier<OutfitterBank> bank) {
        this.client=client;this.bank=bank;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }
    @Override public Dimension render(Graphics2D graphics) {
        if(!BankChestPixels.ready())return null;
        OutfitterBank active=bank.get();
        if(active==null)return null;
        Rectangle b=active.chestBounds();
        if(b==null || b.width<12 || b.height<12)return null;
        Point mouse=client.getMouseCanvasPosition();
        boolean hover=mouse!=null && b.contains(mouse.getX(),mouse.getY());
        BankChestPixels.State state=!active.chestHasPlan()?BankChestPixels.State.DISABLED:
            !hover?BankChestPixels.State.NORMAL:
            client.getMouseCurrentButton()==1?BankChestPixels.State.PRESSED:BankChestPixels.State.HOVER;
        BankChestPixels.draw(graphics,b,state);
        return null;
    }
}
