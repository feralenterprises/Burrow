package gg.feral.burrow.runelite;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuEntry;
import net.runelite.api.MenuAction;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;

/** C03 native header entry. All widget access is client-thread confined.
 * Geometry is rediscovered before render and before accepting an operation. No item reads.
 */
final class BankWidgetAdapter {
    static final String LABEL = "Outfitter bank prep";
    static final int MAX_NODES = 256, MAX_SLOTS = 1024, MAX_DEPTH = 8;
    private final Client client;
    private void trace(String stage, String reason) { }
    private volatile Entry entry;
    private volatile String menuState="NO_ENTRY";
    String menuRouteState() { return menuState; }
    private volatile Rectangle pressBox; // Immutable published copy; no widget access on AWT.
    Rectangle pressBounds() { Rectangle box=pressBox;return box==null?null:new Rectangle(box); }
    private final List<TitleReservation> titleReservations=new ArrayList<>();
    private record Pose(int x,int y,int width,int height,int xMode,int yMode,int widthMode,int heightMode) {
        static Pose read(Widget w) { return new Pose(w.getOriginalX(),w.getOriginalY(),w.getOriginalWidth(),w.getOriginalHeight(),
            w.getXPositionMode(),w.getYPositionMode(),w.getWidthMode(),w.getHeightMode()); }
        void apply(Widget w) { w.setPos(x,y,xMode,yMode);w.setSize(width,height,widthMode,heightMode);w.revalidate(); }
    }
    private record TitleReservation(Widget widget,Pose original,Pose written) {}
    private void restoreTitles() {
        for(TitleReservation reservation:List.copyOf(titleReservations)) {
            // Restore only our exact layout write, never a newer native/other-plugin layout.
            try { if(Pose.read(reservation.widget).equals(reservation.written))reservation.original.apply(reservation.widget); }
            catch(RuntimeException unloaded) { /* Old bank object may already be unloaded; no foreign UI is touched. */ }
        }
        titleReservations.clear();
    }
    private String status = "Bank preparation entry: ordinary bank required";
    private record Layout(Widget root, Widget settings, Widget tutorial,
                          Rectangle rootBox, Rectangle box, int font) {}
    static final class Entry {
        final Widget parent, child, border;
        final Layout layout;
        long lastPress, bindingPress;
        java.util.function.Consumer<MenuEntry> menuClick;
        Entry(Widget parent, Widget child, Widget border, Layout layout, long lastPress) {
            this.parent = parent; this.child = child; this.border = border;
            this.layout = layout; this.lastPress = lastPress;
        }
    }
    BankWidgetAdapter(Client client) { this.client = client; }
    Entry entry() { return entry; } // Identity alone may cross to EDT; never dereference widgets there.
    String status() { return status; }

    Entry refresh() {
        try {
            Layout layout = discover();
            if (layout == null) { String reason = status; clear(); status = reason; return null; }
            Entry old = entry;
            if (old != null && attached(old) && old.layout.equals(layout)
                    && layout.box.equals(logicalBounds(old.child, layout.root, layout.rootBox, MAX_DEPTH))
                    && layout.box.equals(logicalBounds(old.border, layout.root, layout.rootBox, MAX_DEPTH))
                    && !old.child.isHidden() && !old.border.isHidden()) {
                pressBox=new Rectangle(layout.box);
                status = "Native Outfitter bank prep entry in Settings header cluster";
                return old;
            }
            Widget child = null, border = null;
            long lastPress = 0;
            if (old != null && old.parent == layout.root && attached(old)) {
                child = old.child; border = old.border; lastPress = old.lastPress;
                entry = null; // Reject callbacks captured before this layout change.
            } else clearOwnedEntry(); // discover() already installed the current safe title layout.
            if (child == null) {
                // Two siblings form one bounded native control. No dynamic-layer child nesting.
                border = createFree(layout.root, WidgetType.RECTANGLE);
                entry = new Entry(layout.root, border, border, layout, 0); // Own partial creation on failure.
                child = createFree(layout.root, WidgetType.TEXT);
            }
            Entry next = new Entry(layout.root, child, border, layout, lastPress);
            entry = next; // Track exact ownership even if subsequent host initialization fails.
            position(border, layout);
            border.setFilled(false).setTextColor(0x8f8265).setOpacity(0);
            border.setName("");
            position(child, layout);
            child.setFontId(layout.font);
            child.setText("O").setTextColor(0x57c98f).setTextShadowed(true)
                .setXTextAlignment(WidgetTextAlignment.CENTER).setYTextAlignment(WidgetTextAlignment.CENTER)
                .setName(""); // Action alone is the one standard RuneLite hover label.
            child.setAction(0, LABEL);
            child.setClickMask(0); // Client-only listeners; never enable transmission to the game server.
            child.setHasListener(true);
            child.setNoClickThrough(true);
            border.setHidden(false);
            child.setHidden(false);
            pressBox=new Rectangle(layout.box);
            status = "Native Outfitter bank prep entry in Settings header cluster";
            return next;
        } catch (RuntimeException unavailable) {
            clear();
            status = "Bank preparation entry unavailable: widget layout not ready";
            return null;
        }
    }
    private Widget createFree(Widget parent, int type) {
        Widget[] children = parent.getChildren();
        int free = children == null ? 0 : children.length;
        if (children != null) for (int i = 0; i < children.length; i++)
            if (children[i] == null) { free = i; break; }
        return parent.createChild(free, type);
    }
    private void position(Widget widget, Layout layout) {
        widget.setHidden(true);
        widget.setPos(layout.box.x - layout.rootBox.x, layout.box.y - layout.rootBox.y,
            WidgetPositionMode.ABSOLUTE_LEFT, WidgetPositionMode.ABSOLUTE_TOP);
        widget.setSize(layout.box.width, layout.box.height, WidgetSizeMode.ABSOLUTE, WidgetSizeMode.ABSOLUTE);
        widget.revalidate();
    }
    void bind(Entry expected, Runnable action) {
        if (entry != expected) return;
        expected.bindingPress=client.getMouseLastPressedMillis();
        expected.menuClick=new OwnedMenuClick(expected,action);
        menuState="AWAITING_OWN_ACTION";
        expected.child.setOnOpListener((JavaScriptCallback) event -> {
            if (event != null && event.getOp() == 1) dispatch(expected, event, false, action);
        });
        expected.child.setOnClickListener((JavaScriptCallback) event -> dispatch(expected, event, true, action));
    }
    private void dispatch(Entry expected, net.runelite.api.ScriptEvent event, boolean pointer, Runnable action) {
        trace("O_CLICK_EVENT",pointer?"WIDGET_CLICK":"WIDGET_OP");
        if(entry!=expected) { trace("O_CLICK_REJECT","STALE_ENTRY");return; }
        if(event==null || event.getSource()!=expected.child) { trace("O_CLICK_REJECT","EVENT_SOURCE");return; }
        // Pinned injected ph input code subtracts the widget's absolute origin, not its parent.
        // The 1.13.1 API's parent-relative comment does not describe this native onClick path.
        if(pointer && (event.getMouseX()<0 || event.getMouseY()<0
            || event.getMouseX()>=expected.child.getWidth() || event.getMouseY()>=expected.child.getHeight())) {
            trace("O_CLICK_REJECT","POINTER_OUTSIDE");return;
        }
        activate(expected,action);
    }
    private void activate(Entry expected,Runnable action) {
        if(entry!=expected) { trace("O_CLICK_REJECT","STALE_ENTRY");return; }
        // A widget/menu callback can arrive after a tab or header rebuild but before
        // refresh() has run again. Never activate a detached or moved header entry.
        if(!current(expected)) { trace("O_CLICK_REJECT","HEADER_NOT_CURRENT");return; }
        long press = client.getMouseLastPressedMillis();
        // RuneLite can emit onClick and onOp for the same physical press, in either order.
        if (press > 0 && press == expected.lastPress) { trace("O_CLICK_REJECT","DUPLICATE_PRESS");return; }
        expected.lastPress = press;
        action.run();
    }
    /** Converts only our exact native widget action; never adds a global menu entry. */
    void routeMenu(MenuEntry menu) {
        routeMenuUnchanged(menu);
    }
    private void routeMenuUnchanged(MenuEntry menu) {
        Entry expected=entry;
        if(expected==null || menu==null || !LABEL.equals(menu.getOption()))return;
        if(menu.onClick() instanceof BankWidgetAdapter.OwnedMenuClick tagged && tagged.ownedBy(this)
            && tagged==expected.menuClick && tagged.expected==expected && tagged.identity(menu)
            && menu.getType()==MenuAction.RUNELITE) { menuState="RUNELITE_ACTION_ROUTED";return; }
        if(expected.menuClick==null) { menuState="ACTION_NOT_BOUND";return; }
        if(menu.getIdentifier()!=1) { menuState="OTHER_OPERATION";return; }
        if(menu.getParam0()!=expected.child.getIndex() || menu.getParam1()!=expected.parent.getId()) {
            menuState="ACTION_ADDRESS_MISMATCH";return;
        }
        if(menu.getWidget()!=expected.child) { menuState="WIDGET_SOURCE_MISMATCH";return; }
        if(menu.getType()!=MenuAction.CC_OP && menu.getType()!=MenuAction.CC_OP_LOW_PRIORITY) {
            menuState="NOT_NATIVE_WIDGET_ACTION";return;
        }
        if(menu.onClick()!=null && !(menu.onClick() instanceof BankWidgetAdapter.OwnedMenuClick owned
                && owned.ownedBy(this))) {
            menuState="FOREIGN_CALLBACK";return;
        }
        menu.setType(MenuAction.RUNELITE).setForceLeftClick(true).onClick(expected.menuClick);
        menuState="RUNELITE_ACTION_ROUTED";
    }
    private final class OwnedMenuClick implements java.util.function.Consumer<MenuEntry> {
        final Entry expected;final Runnable action;MenuEntry selected;
        OwnedMenuClick(Entry expected,Runnable action) { this.expected=expected;this.action=action; }
        @Override public void accept(MenuEntry menu) {
            trace("O_CLICK_EVENT","RUNELITE_MENU");
            if(menu!=selected || selected==null) { trace("O_CLICK_REJECT","MENU_NOT_SELECTED");return; }
            selected=null;
            if(!identity(menu)) {
                trace("O_CLICK_REJECT","MENU_IDENTITY");return;
            }
            activate(expected,action);
        }
        boolean identity(MenuEntry menu) {
            return menu!=null && menu.onClick()==this && menu.getIdentifier()==1 && LABEL.equals(menu.getOption())
                && menu.getParam0()==expected.child.getIndex() && menu.getParam1()==expected.parent.getId();
        }
        boolean ownedBy(BankWidgetAdapter owner) { return BankWidgetAdapter.this==owner; }
    }
    /** Consume only our selected action before native CC_OP/server handling.
     * MenuEntryAdded is useful for presentation, but delivery there is not authorization
     * for a later click. An untagged native selection must prove the live widget identity
     * as well as its address. Never adopt a foreign consumer or an unresolvable RUNELITE
     * entry. Both routes use the same bound action/controller and physical-press dedupe. */
    boolean selectMenu(net.runelite.api.events.MenuOptionClicked event) {
        if(event==null || event.getMenuEntry()==null)return false;
        MenuEntry menu=event.getMenuEntry();
        if(!(menu.onClick() instanceof BankWidgetAdapter.OwnedMenuClick click) || !click.ownedBy(this)) {
            Entry expected=entry;
            if(expected==null || !LABEL.equals(menu.getOption()) || menu.getIdentifier()!=1
                || menu.getParam0()!=expected.child.getIndex() || menu.getParam1()!=expected.parent.getId()
                || menu.getType()!=MenuAction.CC_OP && menu.getType()!=MenuAction.CC_OP_LOW_PRIORITY
                || menu.getWidget()!=expected.child)return false;
            trace("O_CLICK_EVENT","SELECTED_NATIVE_ACTION");
            if(menu.onClick()!=null) { trace("O_CLICK_REJECT","FOREIGN_CALLBACK");return false; }
            if(event.isConsumed()) { trace("O_CLICK_REJECT","MENU_ALREADY_CONSUMED");return true; }
            if(!(expected.menuClick instanceof BankWidgetAdapter.OwnedMenuClick bound)) {
                trace("O_CLICK_REJECT","ACTION_NOT_BOUND");return false;
            }
            long press=client.getMouseLastPressedMillis();
            if(press<=0 || press==expected.bindingPress) {
                event.consume(); // Owned native op remains client-only even when its intent is stale.
                trace("O_CLICK_REJECT","NATIVE_PRESS_NOT_NEW");return true;
            }
            // No consumer is attached to this menu. Dispatch here exactly once, rather
            // than depending on the missed menu-build tag or adding another listener.
            event.consume();
            activate(expected,bound.action);
            return true;
        }
        if(!click.identity(menu)) { click.selected=null;trace("O_CLICK_REJECT","MENU_IDENTITY");return false; }
        if(event.isConsumed()) { click.selected=null;trace("O_CLICK_REJECT","MENU_ALREADY_CONSUMED");return true; }
        event.consume();click.selected=menu;
        // Pinned iv dispatch invokes MenuEntry.onClick AFTER EventBus post, even if consumed.
        // Arm that one real callback; never invoke it a second time from this subscriber.
        return true;
    }
    boolean current(Entry expected) {
        if (expected == null || entry != expected) return false;
        try {
            Layout layout = discover();
            return layout != null && expected.layout.equals(layout) && attached(expected)
                && !expected.child.isHidden() && !expected.border.isHidden() && layout.box.equals(logicalBounds(expected.child, layout.root, layout.rootBox, MAX_DEPTH))
                && layout.box.equals(logicalBounds(expected.border, layout.root, layout.rootBox, MAX_DEPTH));
        } catch (RuntimeException unavailable) { return false; }
    }
    private static boolean attached(Entry value) {
        Widget[] now = value.parent.getChildren();
        return slot(now, value.child) && slot(now, value.border);
    }
    private static boolean slot(Widget[] now, Widget child) {
        int index = child.getIndex();
        return now != null && index >= 0 && index < now.length && now[index] == child;
    }
    void clear() { restoreTitles(); clearOwnedEntry(); }
    private void clearOwnedEntry() {
        Entry old = entry;
        entry = null; // Listener/queued callback revocation precedes widget mutation.
        pressBox=null;menuState="NO_ENTRY";
        status = "Bank preparation entry: ordinary bank/header not ready";
        if (old == null) return;
        for (Widget child : new Widget[]{old.child, old.border}) {
            child.setHidden(true);
            child.clearActions();
            child.setOnOpListener((Object[]) null);
            child.setOnClickListener((Object[]) null);
            child.setHasListener(false);
            child.setClickMask(0);
            child.setNoClickThrough(false);
        }
        // Clear only identity-checked slots in the CURRENT array; never restore saved children.
        Widget[] now = old.parent.getChildren();
        if (now != null) {
            Widget[] updated = now.clone();
            boolean removed = false;
            for (Widget child : new Widget[]{old.child, old.border})
                if (slot(now, child)) { updated[child.getIndex()] = null; removed = true; }
            if (removed) {
                int length = updated.length;
                while (length > 0 && updated[length - 1] == null) length--;
                old.parent.setChildren(Arrays.copyOf(updated, length));
            }
        }
    }
    /** Logical layout follows CS2 revalidation immediately; getBounds may still describe a prior draw. */
    private static Rectangle logicalBounds(Widget w, Widget root, Rectangle rootBox, int remaining) {
        if (w == root) return new Rectangle(rootBox);
        if (w == null || remaining <= 0) return null;
        Widget parent = w.getParent();
        Rectangle parentBox = logicalBounds(parent, root, rootBox, remaining - 1);
        return parentBox == null ? null : new Rectangle(parentBox.x + w.getRelativeX() - parent.getScrollX(),
            parentBox.y + w.getRelativeY() - parent.getScrollY(), w.getWidth(), w.getHeight());
    }
    private boolean ready(Widget w, WidgetInfo symbol, Rectangle canvas, Widget root, Rectangle rootBox) {
        if (w == null || w.getId() != symbol.getId() || w.isHidden()) return false;
        Rectangle bounds = logicalBounds(w, root, rootBox, MAX_DEPTH);
        return bounds != null && bounds.width > 0 && bounds.height > 0 && canvas.contains(bounds);
    }
    private Layout discover() {
        restoreTitles();
        status = "Bank preparation entry: ordinary bank/header not ready";
        if (client.getGameState() != GameState.LOGGED_IN) return null;
        Widget pin = client.getWidget(WidgetInfo.BANK_PIN_CONTAINER);
        if (pin != null && !pin.isHidden()) return null;
        Widget storage = client.getWidget(WidgetInfo.GROUP_STORAGE_UI);
        Widget deposit = client.getWidget(WidgetInfo.DEPOSIT_BOX_INVENTORY_ITEMS_CONTAINER);
        if (storage != null && !storage.isHidden() || deposit != null && !deposit.isHidden()) {
            status = "Bank preparation entry unavailable in group storage/deposit box";
            return null;
        }
        Rectangle canvas = new Rectangle(0, 0, client.getCanvasWidth(), client.getCanvasHeight());
        Widget root = client.getWidget(WidgetInfo.BANK_CONTAINER);
        // Header chrome is independent of the bank item/content children. Those
        // children may be populated only after the first bank render, or temporarily
        // replaced by Bank Tags. Neither transition should remove the O button.
        // Item ownership/verification is enforced separately by OutfitterBank.
        Widget title = client.getWidget(WidgetInfo.BANK_TITLE_BAR);
        Widget settings = client.getWidget(WidgetInfo.BANK_SETTINGS_BUTTON);
        Widget tutorial = client.getWidget(WidgetInfo.BANK_TUTORIAL_BUTTON);
        if (root == null || root.isHidden() || root.getId() != WidgetInfo.BANK_CONTAINER.getId()
            || root.getType() != WidgetType.LAYER || root.getBounds() == null) return null;
        Rectangle r = new Rectangle(root.getBounds());
        if (r.width <= 0 || r.height <= 0 || !canvas.contains(r) || root.getScrollX() != 0 || root.getScrollY() != 0
            || !ready(title, WidgetInfo.BANK_TITLE_BAR, canvas, root, r)
            || !ready(settings, WidgetInfo.BANK_SETTINGS_BUTTON, canvas, root, r)
            || settings.getParent() != root || title.getFontId() < 0) return null;
        // Tutorial is optional native chrome. Hidden/absent Tutorial must not block every reopen.
        boolean visibleTutorial = tutorial != null && !tutorial.isHidden();
        if (visibleTutorial && (!ready(tutorial, WidgetInfo.BANK_TUTORIAL_BUTTON, canvas, root, r)
                || tutorial.getParent() != root)) return null;
        Rectangle s = logicalBounds(settings, root, r, MAX_DEPTH);
        Rectangle t = visibleTutorial ? logicalBounds(tutorial, root, r, MAX_DEPTH) : null;
        Rectangle header = logicalBounds(title, root, r, MAX_DEPTH).intersection(r);
        if (!header.contains(s) || s.width != s.height || t != null && (!header.contains(t)
            || t.width != s.width || t.height != s.height || s.y != t.y)) return null;
        int gap = t == null ? Math.max(2, s.width / 7)
            : Math.max(2, Math.min(s.width / 3, Math.abs(t.x - s.x) - s.width));
        List<Rectangle> occupied = new ArrayList<>();
        occupied.add(s); if (t != null) occupied.add(t);
        Walk walk = new Walk(header, occupied, root, r, title);
        // Full relevant header traversal, unlike the intentionally truncated diagnostic.
        if (!walk.visit(root, 0)) { status = "Bank prep unavailable: header collision traversal limit"; return null; }
        for (int distance = 1; distance <= 3; distance++) {
            for (int direction : new int[]{-1, 1}) {
                Rectangle box = new Rectangle(s.x + direction * distance * (s.width + gap), s.y, s.width, s.height);
                if (!header.contains(box) || !canvas.contains(box)) continue;
                boolean collision = occupied.stream().anyMatch(box::intersects);
                if (!collision) return new Layout(root, settings, visibleTutorial ? tutorial : null, new Rectangle(r), box, title.getFontId());
            }
        }
        // Full-bank value text can occupy every otherwise safe cluster slot. Reserve the
        // minimum header space by centering complete native title text in the remaining span.
        // No text, font, Settings, Tutorial or callbacks are modified. If it cannot fit, omit safely.
        for(int distance=1;distance<=3;distance++)for(int direction:new int[]{-1,1}) {
            Rectangle box=new Rectangle(s.x+direction*distance*(s.width+gap),s.y,s.width,s.height);
            if(!header.contains(box) || !canvas.contains(box)
                || walk.nonTitle.stream().anyMatch(box::intersects) || walk.titles.isEmpty())continue;
            int left=box.x+box.width+gap,right=header.x+header.width;
            for(Rectangle obstacle:walk.nonTitle)
                if(obstacle.x>=left && obstacle.intersects(header))right=Math.min(right,obstacle.x-gap);
            int width=right-left;
            if(width<=0 || walk.titles.stream().anyMatch(n->n.textWidth>width))continue;
            for(TitleNode node:walk.titles) {
                Widget parent=node.widget.getParent();Rectangle parentBox=logicalBounds(parent,root,r,MAX_DEPTH);
                Pose old=Pose.read(node.widget);
                Pose written=new Pose(left-parentBox.x+parent.getScrollX(),node.box.y-parentBox.y+parent.getScrollY(),
                    width,node.box.height,WidgetPositionMode.ABSOLUTE_LEFT,WidgetPositionMode.ABSOLUTE_TOP,
                    WidgetSizeMode.ABSOLUTE,WidgetSizeMode.ABSOLUTE);
                titleReservations.add(new TitleReservation(node.widget,old,written));written.apply(node.widget);
            }
            return new Layout(root,settings,visibleTutorial?tutorial:null,new Rectangle(r),box,title.getFontId());
        }
        status = "Bank prep unavailable: no collision-free slot beside Settings/Tutorial (complete title cannot fit)";
        return null;
    }
    private record TitleNode(Widget widget,Rectangle box,int textWidth) {}
    private final class Walk {
        final Rectangle header;
        final Widget canonicalTitle;
        final List<TitleNode> titles=new ArrayList<>();
        final List<Rectangle> nonTitle=new ArrayList<>();
        final List<Rectangle> occupied;
        final Widget root;
        final Rectangle rootBox;
        final IdentityHashMap<Widget,Boolean> seen = new IdentityHashMap<>();
        int slots;
        Walk(Rectangle header, List<Rectangle> occupied, Widget root, Rectangle rootBox,Widget canonicalTitle) {
            this.header = header; this.occupied = occupied; this.root = root; this.rootBox = rootBox;
            this.canonicalTitle=canonicalTitle;nonTitle.addAll(occupied);
        }
        void obstacle(Rectangle bounds) { occupied.add(bounds);nonTitle.add(bounds); }
        boolean visit(Widget w, int depth) {
            if (w == null || seen.put(w, true) != null || w.isHidden()) return true;
            if (seen.size() > MAX_NODES || depth > MAX_DEPTH) return false;
            if (entry != null && (w == entry.child || w == entry.border)) return true;
            Rectangle box = logicalBounds(w, root, rootBox, MAX_DEPTH);
            if (box == null) return false;
            if (!box.intersects(header)) return true;
            if (w.getId() >>> 16 != WidgetInfo.BANK_CONTAINER.getGroupId()) return false;
            if (w.getType() == WidgetType.TEXT) {
                String text = w.getText();
                if (text != null && !text.isEmpty()) {
                    var font = w.getFont();
                    if (font == null || text.contains("<br>") || text.contains("\n")) obstacle(box);
                    else {
                        int fullWidth=Math.max(0,font.getTextWidth(text))+4;
                        int width = Math.min(box.width, fullWidth);
                        int align = w.getXTextAlignment();
                        if (align != WidgetTextAlignment.LEFT && align != WidgetTextAlignment.CENTER
                                && align != WidgetTextAlignment.RIGHT) return false;
                        int x = box.x + (align == WidgetTextAlignment.CENTER ? (box.width - width) / 2
                            : align == WidgetTextAlignment.RIGHT ? box.width - width : 0);
                        Rectangle ink=new Rectangle(x,box.y,width,box.height);
                        boolean nativeTitle=w==canonicalTitle || w.getId()==net.runelite.api.gameval.InterfaceID.Bankmain.FRAME
                            && w.getParent()!=null && w.getParent().getId()==net.runelite.api.gameval.InterfaceID.Bankmain.FRAME
                            // The supplied pinned live probe identifies FRAME dynamic child1 as its native title.
                            && w.getIndex()==1 && w.getParent().getChildren()!=null
                            && w.getParent().getChildren().length>1 && w.getParent().getChildren()[1]==w && box.equals(header);
                        if(nativeTitle && align==WidgetTextAlignment.CENTER && !w.hasListener() && w.getClickMask()==0 && !hasActions(w)) {
                            titles.add(new TitleNode(w,new Rectangle(box),fullWidth));occupied.add(ink);
                        } else obstacle(ink);
                    }
                }
            }
            if (depth != 0 && (w.hasListener() || w.getClickMask() != 0 || hasActions(w))) obstacle(box);
            if (w.getType() != WidgetType.LAYER) return true;
            return children(w.getChildren(), depth) && children(w.getStaticChildren(), depth)
                && children(w.getNestedChildren(), depth);
        }
        boolean children(Widget[] values, int depth) {
            if (values == null) return true;
            if (values.length > MAX_SLOTS - slots) return false;
            slots += values.length;
            for (Widget child : values) if (!visit(child, depth + 1)) return false;
            return true;
        }
        boolean hasActions(Widget w) {
            String[] actions = w.getActions();
            if (actions != null) for (String action : actions) if (action != null && !action.isEmpty()) return true;
            return false;
        }
    }
}
