package gg.feral.burrow.runelite;
import java.awt.Rectangle;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import javax.swing.SwingUtilities;
import net.runelite.api.FontTypeFace;
import net.runelite.api.ScriptEvent;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.*;
import net.runelite.client.ui.NavigationButton;
import static org.junit.jupiter.api.Assertions.*;


class BankHeaderFixture {
    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> type, java.util.function.BiFunction<String,Object[],Object> calls) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)-> {
            if(m.getDeclaringClass()==Object.class) return switch(m.getName()) {
                case "equals" -> p==a[0]; case "hashCode" -> System.identityHashCode(p); default -> "Test proxy";
            };
            return calls.apply(m.getName(),a);
        });
    }
    static final class Header {
        final BooleanSupplier onClient, open;
        final Map<WidgetInfo, Node> symbols = new EnumMap<>(WidgetInfo.class);
        final List<Node> all = new ArrayList<>();
        Node root, title, settings, tutorial, items, content, frame, count, scrollbar;
        int created, widgetCalls, collisionReads;
        boolean pin, staleDrawBounds, nestedDynamicChildren;
        int settingsClicks, tutorialClicks;
        Header(BooleanSupplier onClient, BooleanSupplier open) {
            this.onClient = onClient; this.open = open; rebuild();
        }
        void rebuild() {
            symbols.clear();
            root = named(WidgetInfo.BANK_CONTAINER, WidgetType.LAYER, new Rectangle(283,2,488,592), null);
            title = named(WidgetInfo.BANK_TITLE_BAR, WidgetType.TEXT, new Rectangle(289,8,476,24), root);
            title.text = "Bank of Gielinor"; title.align = WidgetTextAlignment.CENTER;
            settings = named(WidgetInfo.BANK_SETTINGS_BUTTON, WidgetType.LAYER, new Rectangle(317,9,21,21), root);
            tutorial = named(WidgetInfo.BANK_TUTORIAL_BUTTON, WidgetType.LAYER, new Rectangle(341,9,21,21), root);
            settings.listener = tutorial.listener = true;
            settings.op = new Object[]{(JavaScriptCallback) e -> settingsClicks++};
            tutorial.op = new Object[]{(JavaScriptCallback) e -> tutorialClicks++};
            settings.actions = new String[]{"Settings"}; tutorial.actions = new String[]{"Tutorial"};
            content = named(WidgetInfo.BANK_CONTENT_CONTAINER, WidgetType.LAYER, new Rectangle(288,37,478,552), root);
            items = named(WidgetInfo.BANK_ITEM_CONTAINER, WidgetType.LAYER, new Rectangle(289,79,460,471), content);
            scrollbar = named(WidgetInfo.BANK_SCROLLBAR, WidgetType.LAYER, new Rectangle(749,79,16,471), content);
            count = named(WidgetInfo.BANK_ITEM_COUNT_TOP, WidgetType.TEXT, new Rectangle(286,8,32,24), root);
            count.text = "100";
            frame = node(root.id + 1, WidgetType.LAYER, new Rectangle(283,2,488,592), root);
            Node titleCopy = node(frame.id, WidgetType.TEXT, new Rectangle(title.box), frame);
            titleCopy.text = title.text; titleCopy.align = WidgetTextAlignment.CENTER; titleCopy.index=1;
            Node decoration = node(frame.id, WidgetType.GRAPHIC, new Rectangle(283,2,488,592), frame);
            frame.children = new Widget[]{decoration.widget,titleCopy.widget};
            root.statics = new Widget[]{frame.widget, title.widget, count.widget, settings.widget, tutorial.widget, content.widget};
            content.statics = new Widget[]{items.widget,scrollbar.widget};
            named(WidgetInfo.BANK_PIN_CONTAINER, WidgetType.LAYER, new Rectangle(100,100,200,200), null);
        }
        Node named(WidgetInfo info, int type, Rectangle box, Node parent) {
            Node n = node(info.getId(), type, box, parent); symbols.put(info,n); return n;
        }
        Node node(int id, int type, Rectangle box, Node parent) {
            Node n = new Node(id,type,box,parent); all.add(n); return n;
        }
        Widget lookup(WidgetInfo info) {
            assertTrue(onClient.getAsBoolean());
            if (info == WidgetInfo.BANK_PIN_CONTAINER && !pin) return null;
            Node n = symbols.get(info); return n == null ? null : n.widget;
        }
        Node extra(Rectangle box, String action) {
            Node n = node(root.id,WidgetType.TEXT,box,root);
            n.text = "Q"; n.listener = true; n.actions = new String[]{action}; n.index = root.children.length;
            Widget[] now = java.util.Arrays.copyOf(root.children,root.children.length + 1);
            now[n.index] = n.widget; root.children = now; return n;
        }
        Node get(Widget w) { return all.stream().filter(n -> n.widget == w).findFirst().orElseThrow(); }
        void op(Widget w, int operation) {
            Node n = get(w); assertNotNull(n.op);
            ScriptEvent event = BankHeaderFixture.proxy(ScriptEvent.class, (method,args) -> switch (method) {
                case "getSource" -> w;
                case "getOp" -> operation;
                default -> throw new AssertionError("Unexpected script event access: " + method);
            });
            ((JavaScriptCallback)n.op[0]).run(event);
        }
        List<String> nativeHover(int x, int y) {
            List<String> result = new ArrayList<>();
            for (int i=root.children.length-1;i>=0;i--) {
                Widget w=root.children[i]; if(w==null)continue;
                Node n=get(w);
                if(!n.hidden && open.getAsBoolean() && n.bounds().contains(x,y) && n.op!=null
                        && n.listener && n.actions!=null && n.actions[0]!=null) {
                    result.add((n.actions[0]+(n.name.isEmpty()?"":" "+n.name)).trim());
                    break;
                }
            }
            return result;
        }
        void pointer(int x, int y, boolean opFirst) {
            for(int i=root.children.length-1;i>=0;i--) {
                Widget w=root.children[i];if(w==null)continue;
                Node n=get(w);
                if(n.hidden || !open.getAsBoolean() || !n.bounds().contains(x,y) || !n.listener)continue;
                if(opFirst && n.op!=null)op(w,1);
                if(n.click!=null) {
                    // Verified injected ph input producer subtracts this widget's screen origin.
                    // Legacy test names mentioning parent-relative coordinates are historical.
                    int parentX=x-n.bounds().x,parentY=y-n.bounds().y;
                    ((JavaScriptCallback)n.click[0]).run(BankHeaderFixture.proxy(ScriptEvent.class,(method,args)->switch(method){
                        case "getSource" -> w;
                        case "getMouseX" -> parentX;
                        case "getMouseY" -> parentY;
                        default -> throw new AssertionError("Unexpected pointer event " + method);
                    }));
                }
                if(!opFirst && n.op!=null)op(w,1);
                return;
            }
        }
        int attachedEntries() {
            int result = 0;
            for (Node n : all) if (n.text.equals("O") && n.parent != null)
                for (Widget child : n.parent.children) if (child == n.widget) result++;
            return result;
        }
        final class Node {
            final int id;
            int type, index = -1, font = 496, align, x, y, width, height, dragTime;
            Rectangle box;
            final Node parent;
            final Widget widget;
            Widget[] children = {}, statics = {}, nested = {};
            Object[] op, click, wheel;
            String text = "", name = "";
            String[] actions;
            boolean hidden, listener, dynamic, noClick, owned, filled;
            int spriteId=-1;
            int contentType, clickMask, xMode, yMode, widthMode, heightMode, scrollY, scrollHeight, itemId=-1, itemQuantity;
            Rectangle renderedBox = new Rectangle();
            Node(int id, int type, Rectangle box, Node parent) {
                this.id=id; this.type=type; this.box=box; this.parent=parent;
                this.x=parent==null?box.x:box.x-parent.bounds().x;this.y=parent==null?box.y:box.y-parent.bounds().y;
                this.width=box.width;this.height=box.height;
                widget = (Widget)Proxy.newProxyInstance(Widget.class.getClassLoader(),new Class<?>[]{Widget.class},(proxy,m,args) -> {
                    if (m.getDeclaringClass() == Object.class) return switch(m.getName()) {
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "Synthetic header widget";
                        default -> throw new AssertionError(m.getName());
                    };
                    assertTrue(onClient.getAsBoolean(),"all widget access must be client-thread confined");
                    assertFalse(SwingUtilities.isEventDispatchThread());
                    widgetCalls++;
                    Object result = switch(m.getName()) {
                        case "getId" -> id;
                        case "getType" -> this.type;
                        case "getParent" -> parent == null ? null : (dynamic && !nestedDynamicChildren ? canonical().widget : parent.widget);
                        case "getParentId" -> parent == null ? -1 : parent.id;
                        case "getIndex" -> index;
                        case "getBounds" -> dynamic && staleDrawBounds ? new Rectangle(renderedBox) : bounds();
                        case "getRelativeX" -> dynamic ? x : parent==null ? this.box.x : this.box.x-parent.bounds().x;
                        case "getRelativeY" -> dynamic ? y : parent==null ? this.box.y : this.box.y-parent.bounds().y;
                        case "getScrollX" -> 0;
                        case "getScrollY" -> scrollY;
                        case "getScrollHeight" -> scrollHeight;
                        case "setScrollY" -> { scrollY=(int)args[0]; yield null; }
                        case "setScrollHeight" -> { scrollHeight=(int)args[0]; yield null; }
                        case "getOriginalX" -> dynamic || xMode!=0 ? x : this.box.x-(parent==null?0:parent.bounds().x);
                        case "getOriginalY" -> dynamic || yMode!=0 ? y : this.box.y-(parent==null?0:parent.bounds().y);
                        case "getOriginalWidth" -> dynamic || widthMode!=0 ? width : this.box.width;
                        case "getOriginalHeight" -> dynamic || heightMode!=0 ? height : this.box.height;
                        case "getXPositionMode" -> xMode;
                        case "getYPositionMode" -> yMode;
                        case "getWidthMode" -> widthMode;
                        case "getHeightMode" -> heightMode;
                        case "getWidth" -> bounds().width;
                        case "getHeight" -> bounds().height;
                        case "isHidden" -> hidden || !open.getAsBoolean();
                        case "isSelfHidden" -> hidden;
                        case "getDragDeadTime" -> dragTime;
                        case "setDragDeadTime" -> {dragTime=(int)args[0];yield null;}
                        case "getItemId" -> itemId;
                        case "getSpriteId" -> spriteId;
                        case "getItemQuantity" -> itemQuantity;
                        case "setItemQuantityMode" -> null;
                        case "setOriginalX" -> {x=(int)args[0];yield null;}
                        case "setOriginalY" -> {y=(int)args[0];yield null;}
                        case "setOriginalWidth" -> {width=(int)args[0];yield null;}
                        case "setOriginalHeight" -> {height=(int)args[0];yield null;}
                        case "hasListener" -> listener;
                        case "getClickMask" -> clickMask;
                        case "getContentType" -> contentType;
                        case "getNoClickThrough" -> noClick;
                        case "getName" -> name;
                        case "setClickMask" -> { clickMask=(int)args[0]; yield null; }
                        case "getActions" -> actions;
                        case "getText" -> text;
                        case "getFontId" -> font;
                        case "getXTextAlignment" -> align;
                        case "getFont" -> font < 0 ? null : BankHeaderFixture.proxy(FontTypeFace.class,(m2,a2) -> {
                            assertEquals("getTextWidth",m2); return ((String)a2[0]).replaceAll("<[^>]*>","").length()*6;
                        });
                        case "getChildren" -> { collisionReads++; yield children; }
                        case "getDynamicChildren" -> java.util.Arrays.stream(nestedDynamicChildren ? children : canonical().children).filter(w -> w!=null && get(w).parent==this).toArray(Widget[]::new);
                        case "getStaticChildren" -> { collisionReads++; yield statics; }
                        case "getNestedChildren" -> { collisionReads++; yield nested; }
                        case "createChild" -> {
                            int slot = (int)args[0];
                            Node owner=nestedDynamicChildren ? this : canonical();
                            assertEquals(WidgetType.LAYER,this.type);
                            assertTrue(slot >= 0); assertTrue(slot >= owner.children.length || owner.children[slot] == null,"never replace a sibling");
                            Node n = node(id,(int)args[1],new Rectangle(),this); n.dynamic = true; n.owned = true; n.index = slot;
                            owner.children = java.util.Arrays.copyOf(owner.children,Math.max(owner.children.length,slot+1));
                            owner.children[slot] = n.widget; created++; yield n.widget;
                        }
                        case "setChildren" -> {
                            assertTrue(nestedDynamicChildren || !dynamic,"only static components own the flat array in pinned client");
                            Widget[] next=(Widget[])args[0];
                            for(int i=0;i<children.length;i++) if(children[i]!=null && !get(children[i]).owned)
                                assertTrue(i<next.length && next[i]==children[i],"other plugin slot/index retained");
                            children=next; yield null;
                        }
                        case "setHidden" -> { hidden=(boolean)args[0]; yield null; }
                        case "setPos" -> { x=(int)args[0]; y=(int)args[1];xMode=(int)args[2];yMode=(int)args[3];
                            if(!dynamic){this.box.x=parent.bounds().x+x;this.box.y=parent.bounds().y+y;}yield null; }
                        case "setSize" -> { width=(int)args[0]; height=(int)args[1];widthMode=(int)args[2];heightMode=(int)args[3];
                            if(!dynamic){this.box.width=widthMode==1?parent.bounds().width-width:width;
                                this.box.height=heightMode==1?parent.bounds().height-height:height;}yield null; }
                        case "setFontId" -> { font=(int)args[0]; yield null; }
                        case "setText" -> { text=(String)args[0]; yield null; }
                        case "setName" -> { name=(String)args[0]; yield null; }
                        case "setXTextAlignment" -> { align=(int)args[0]; yield null; }
                        case "setTextColor", "setTextShadowed", "setYTextAlignment", "setLineHeight", "revalidate", "revalidateScroll", "setBorderType" -> null;
                        case "setItemId" -> { itemId=(int)args[0];yield null; }
                        case "setItemQuantity" -> { itemQuantity=(int)args[0];yield null; }
                        case "setAction" -> { assertEquals(0,args[0]); actions=new String[]{(String)args[1]}; yield null; }
                        case "clearActions" -> { actions=null; yield null; }
                        case "setOnOpListener" -> { op=(Object[])args[0]; yield null; }
                        case "setOnClickListener" -> { click=(Object[])args[0]; yield null; }
                        case "setOnScrollWheelListener" -> { wheel=(Object[])args[0]; yield null; }
                        case "setFilled" -> { filled=(boolean)args[0]; yield null; }
                        case "setOpacity" -> null;
                        case "setHasListener" -> { listener=(boolean)args[0]; yield null; }
                        case "setNoClickThrough" -> { noClick=(boolean)args[0]; yield null; }
                        case "setNoScrollThrough" -> null;
                        default -> throw new AssertionError("Unexpected native widget/item access: " + m.getName());
                    };
                    return m.getReturnType()==Widget.class && m.getName().startsWith("set") ? proxy : result;
                });
            }
            Node canonical() { return dynamic ? parent.canonical() : this; }
            Rectangle bounds() { return dynamic ? new Rectangle(parent.bounds().x+x,parent.bounds().y+y-parent.scrollY,width,height) : new Rectangle(box); }
        }
    }
}
