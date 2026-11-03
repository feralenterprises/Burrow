package gg.feral.tpt.shell;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * A completely standalone Java/Swing VISUAL SHELL for TPT.
 * NO RuneLite imports, plugin wiring, bank, player, event, or network access.
 * Sample values are not account information; OSRS item sprites and pixel-art boss icons are bundled with provenance.
 * Optional model art must satisfy a local owner-approval and SHA-256 manifest gate; no unauthorized art is bundled.
 * Baseline is 225 logical pixels, matching the approved TPT sidebar design constraint.
 */
public final class TrashPandaShell {
    public static final int SIDEBAR_WIDTH = 225;
    private static final int HEIGHT = 790;
    private static final int INNER = 193;
    private static final int CARD_TEXT = 168;
    private static final Color BG = new Color(18, 19, 19);
    private static final Color CARD = new Color(34, 36, 35);
    private static final Color LIGHT = new Color(235, 227, 211);
    private static final Color MUTED = new Color(175, 170, 160);
    // Default approved bronze; only the standalone shell's appearance setting changes this token.
    private static Color GOLD = new Color(213, 162, 89);
    private static int FONT_OFFSET = 0;
    private static int uiPt(int base) {return Math.max(7,base+FONT_OFFSET);}
    private static final String[] ACCENTS = {"Bronze","Azure","Moss"};
    private static final String[] FONT_SIZES = {"Compact","Standard","Large"};
    private static Color accentColor(String name) {
        return switch(name){case "Azure"->new Color(116,166,232);case "Moss"->new Color(149,188,119);
            default->new Color(213,162,89);};
    }
    private static int offsetFor(String name) {return name.equals("Compact")?-1:name.equals("Large")?1:0;}
    private static final Color GREEN = new Color(146, 217, 111);
    private static final Color EDGE = new Color(94, 92, 86);

    public static final class Boss {
        public final String id, name, subtitle, drop, odds, combatNote, tip, family;
        public final boolean melee, ranged, magic;
        public final int difficulty;
        Boss(String id, String name, String subtitle, String drop, String odds,
             String combatNote, String tip, String family,
             boolean melee, boolean ranged, boolean magic, int difficulty) {
            this.id=id; this.name=name; this.subtitle=subtitle; this.drop=drop;
            this.odds=odds; this.combatNote=combatNote; this.tip=tip;
            this.family=family; this.melee=melee; this.ranged=ranged;
            this.magic=magic; this.difficulty=difficulty;
        }
    }
    private static final List<Boss> BOSSES = List.of(
        new Boss("scurrius", "Scurrius", "Rat King · Training boss", "Scurrius' spine", "1/33 · 3.03%", "", "Bone weapons improve damage against rats. Practice prayer changes.", "Training", true,true,true,1),
        new Boss("sarachnis", "Sarachnis", "Spider boss", "Sarachnis cudgel", "1/384 · 0.26%", "Crush", "Bring a crush weapon and change prayers as she moves.", "Regular",true,false,false,2),
        new Boss("bryophyta", "Bryophyta", "Moss giant boss", "Bryophyta's essence", "1/118 · 0.85%", "Fire", "Fire spells exploit her weakness. Equip an axe or magic secateurs to handle growthlings.", "Regular",true,false,true,1)
    );

    private final JFrame window = new JFrame("TrashPanda Tracker · UI Visual Review R10C");
    private final JPanel frame = new JPanel(new BorderLayout());
    private final JPanel nav = new JPanel(new GridLayout(1,4,3,0));
    private final JPanel content = new JPanel(new BorderLayout());
    private final JScrollPane scroll;
    private final java.util.Map<String, FButton> navButtons = new java.util.LinkedHashMap<>();
    private String page = "home", chronicleReturn = "home", outfitBoss = "scurrius", chronicleBoss = "scurrius";
    private String search = "", category = "All", ordering = "Top", difficulty = "Any";
    private String style = "Melee", source = "Wiki Recommended", tier = "Recommended";
    // Standalone W05 presentation state only. Never persisted to OSRS or used as bank truth.
    private boolean bankMemoryVisible=false, gearEvidenceVisible=false, switchGuideVisible=false;
    private final byte[] inventoryMarks=new byte[28]; // 0=unassigned, 1=switch, 2=supply, 3=prayer
    private int selectedEquipSlot=-1, selectedInventorySlot=-1;

    // User-provided design screenshots, not approved in-game NPC image assets.
    // Can be toggled for owner layout comparison; never read by the production art manifest.
    private boolean referencePreviewMode=false;
    private boolean tipDismissed=false, caExpanded=false, chronicleInitialized=false;
    private boolean dropInfoExpanded=false;
    private final boolean[] pathDone = {false,false,false};
    private int chronicleListY=0;
    // UI-only fixture controls. Never represent them as live account history or readiness.
    private String pathGoal="Desert Treasure II";
    private final boolean[] bowfaPlanDone={false,false,false};
    private boolean pathWhyExpanded=false, sessionsHistoryExpanded=false, sessionsSourcesExpanded=false;
    private boolean homeWinExpanded=false;
    private String tipAction="";
    // Appearance is local to this standalone shell. Never touches RuneLite, account or bank files.
    private final Path settingsPath = Path.of(System.getProperty("tpt.shell.settings.file",
        Path.of(System.getProperty("user.home"),".trashpanda-ui-shell","appearance.properties").toString()));
    private String fontChoice="Standard", accentChoice="Bronze", settingsReturn="home", settingsError="";
    private int settingsReturnY=0;
    private boolean settingsSaved=false;
    private static final class Appearance {
        final String font, accent;
        Appearance(String f,String a){font=f;accent=a;}
    }
    private static Appearance readAppearance(Path file) {
        Properties props=new Properties();
        if(Files.isRegularFile(file))try(InputStream in=Files.newInputStream(file)){props.load(in);}
        catch(IOException e){return new Appearance("Standard","Bronze");}
        String f=props.getProperty("font","Standard"), a=props.getProperty("accent","Bronze");
        return new Appearance(java.util.Arrays.asList(FONT_SIZES).contains(f)?f:"Standard",
                              java.util.Arrays.asList(ACCENTS).contains(a)?a:"Bronze");
    }
    private static void writeAppearance(Path file,String f,String a) throws IOException {
        Path parent=file.toAbsolutePath().getParent();if(parent!=null)Files.createDirectories(parent);
        Properties props=new Properties();props.setProperty("font",f);props.setProperty("accent",a);
        Path tmp=Files.createTempFile(parent,"appearance-",".tmp");
        try {
            try(OutputStream stream=Files.newOutputStream(tmp)){props.store(stream,"TPT standalone shell appearance only");}
            try {Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(tmp);}
    }
    private void loadAppearance() {
        Appearance appearance=readAppearance(settingsPath);
        fontChoice=appearance.font;accentChoice=appearance.accent;
        applyAppearanceTokens();
    }
    private void applyAppearanceTokens(){GOLD=accentColor(accentChoice);FONT_OFFSET=offsetFor(fontChoice);}
    private void updateAppearance(String newFont,String newAccent){
        if(!java.util.Arrays.asList(FONT_SIZES).contains(newFont)||!java.util.Arrays.asList(ACCENTS).contains(newAccent))
            throw new IllegalArgumentException("Unexpected appearance option");
        fontChoice=newFont;accentChoice=newAccent;applyAppearanceTokens();
        try {writeAppearance(settingsPath,fontChoice,accentChoice);settingsError="";settingsSaved=true;}
        catch(IOException e){settingsSaved=false;settingsError="Could not save settings. Check folder permissions.";}
        // Refresh header typography, navigation, and the currently selected page.
        navButtons.forEach((k,b)->b.setFont(new Font("SansSerif",Font.PLAIN,uiPt(k.equals("pathfinder")?8:9))));
        render();
    }
    private void openSettings(){
        if(page.equals("settings"))return;
        settingsReturn=page;settingsReturnY=scroll.getVerticalScrollBar().getValue();
        page="settings";render();
    }
    private void closeSettings(){
        page=settingsReturn;render();
        SwingUtilities.invokeLater(()->scroll.getVerticalScrollBar().setValue(settingsReturnY));
    }


    public TrashPandaShell() {
        loadAppearance();
        frame.setBackground(BG);
        frame.setOpaque(true);
        frame.setPreferredSize(new Dimension(SIDEBAR_WIDTH, HEIGHT));
        frame.setBorder(BorderFactory.createLineBorder(new Color(82,79,69)));
        JPanel upper = column(SIDEBAR_WIDTH);
        upper.add(header());
        upper.add(navBar());
        frame.add(upper, BorderLayout.NORTH);
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(9, 8, 15, 6));
        JScrollPane s = new JScrollPane(content, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        s.setBorder(null);
        s.setOpaque(false); s.getViewport().setOpaque(false);
        s.getVerticalScrollBar().setUnitIncrement(18);
        s.getVerticalScrollBar().setPreferredSize(new Dimension(7, 0));
        s.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {thumbColor=new Color(90,84,74);trackColor=BG;}
            @Override protected JButton createDecreaseButton(int orientation){return zeroButton();}
            @Override protected JButton createIncreaseButton(int orientation){return zeroButton();}
            private JButton zeroButton(){JButton b=new JButton();b.setPreferredSize(new Dimension(0,0));return b;}
        });
        scroll=s;
        frame.add(s, BorderLayout.CENTER);
        window.setContentPane(frame);
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.pack();
        window.setLocationRelativeTo(null);
        render();
    }
    public JFrame window(){return window;}
    public void show(){window.setVisible(true);}

    private static JPanel column(int width) {
        JPanel p = new JPanel() {
            @Override public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(width, d.height);
            }
        };
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
        return p;
    }
    private static JPanel row() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT,3,0));
        p.setOpaque(false); p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }
    private static JPanel xrow() {
        JPanel p = new JPanel(new BorderLayout(4,0));
        p.setOpaque(false); p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }
    private static void size(JComponent c, int w, int h) {
        c.setPreferredSize(new Dimension(w,h));
        c.setMinimumSize(new Dimension(w,h));
        c.setMaximumSize(new Dimension(w,h));
    }
    private static JLabel lbl(String text, int pt, Color color, boolean bold) {
        JLabel l = new JLabel(text); l.setOpaque(false);
        l.setForeground(color); l.setFont(new Font("SansSerif",bold?Font.BOLD:Font.PLAIN,uiPt(pt)));
        return l;
    }
    private static JLabel serif(String text, int pt) {
        JLabel l=lbl(text,pt,LIGHT,true);
        l.setFont(new Font("Serif",Font.BOLD,uiPt(pt))); return l;
    }
    /** Word wrapping with a measured Swing layout, not unsupported JLabel HTML CSS. */
    private static JComponent wrapped(String text, int width, int pt, Color c) {
        int actualWidth=Math.min(width,CARD_TEXT);
        JTextArea area=new JTextArea(text);
        area.setFont(new Font("SansSerif",Font.PLAIN,uiPt(pt)));
        area.setForeground(c);area.setOpaque(false);area.setEditable(false);
        area.setLineWrap(true);area.setWrapStyleWord(true);
        area.setFocusable(false);area.setHighlighter(null);
        area.setMargin(new Insets(0,0,0,0));
        area.setSize(new Dimension(actualWidth,Short.MAX_VALUE));
        int actualHeight=Math.max(area.getFontMetrics(area.getFont()).getHeight(),area.getPreferredSize().height);
        size(area,actualWidth,actualHeight);
        return area;
    }
    private static Component gap(int px) {return Box.createRigidArea(new Dimension(1,px));}
    private static Component line(int width) {
        JPanel p=new JPanel();p.setBackground(new Color(91,87,76));size(p,width,1);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);return p;
    }
    /**
     * Only local artwork with an explicit checked manifest, content hash, and owner sign-off
     * is eligible for the high-res model slot. This guards against accidental asset swaps.
     * The manifest is NOT proof that legal permission or authenticity has been independently
     * verified. We make that distinction visible in the documentation.
     */
    private static final java.util.Set<String> MODEL_BOSSES=java.util.Set.of("scurrius","sarachnis","bryophyta");
    private static final String MODEL_MANIFEST="approved-models.properties";
    private static final byte[] PNG_MAGIC=new byte[]{(byte)137,80,78,71,13,10,26,10};
    private static final class ModelAsset {
        final BufferedImage image; final String status;
        ModelAsset(BufferedImage i,String s){image=i;status=s;}
    }
    private static ModelAsset resolveModelArt(String boss,Path folder) {
        if(!MODEL_BOSSES.contains(boss))return new ModelAsset(null,"unknown boss");
        Path image=folder.resolve(boss+"-model.png");
        Path manifest=folder.resolve(MODEL_MANIFEST);
        if(!Files.isRegularFile(image))return new ModelAsset(null,"no approved model file");
        if(!Files.isRegularFile(manifest))return new ModelAsset(null,"approval manifest missing");
        try {
            Properties metadata=new Properties();
            try(InputStream in=Files.newInputStream(manifest)){metadata.load(in);}
            if(!"true".equalsIgnoreCase(metadata.getProperty(boss+".owner_approved", "false").trim()))
                return new ModelAsset(null,"owner approval not recorded");
            String expected=metadata.getProperty(boss+".sha256","").trim().toLowerCase(Locale.ROOT);
            String source=metadata.getProperty(boss+".source_url","").trim();
            String rights=metadata.getProperty(boss+".rights_note","").trim();
            if(!expected.matches("[a-f0-9]{64}")||!(source.startsWith("https://")||source.startsWith("local-capture:"))||rights.length()<12)
                return new ModelAsset(null,"provenance fields incomplete");
            long bytes=Files.size(image);
            if(bytes<100||bytes>8*1024*1024)return new ModelAsset(null,"PNG size out of bounds");
            byte[] fileBytes=Files.readAllBytes(image);
            if(fileBytes.length<PNG_MAGIC.length)return new ModelAsset(null,"invalid PNG");
            for(int i=0;i<PNG_MAGIC.length;i++)if(fileBytes[i]!=PNG_MAGIC[i])
                return new ModelAsset(null,"file is not PNG");
            var md=java.security.MessageDigest.getInstance("SHA-256");
            String digest=java.util.HexFormat.of().formatHex(md.digest(fileBytes));
            if(!digest.equals(expected))return new ModelAsset(null,"checksum mismatch");
            BufferedImage img=ImageIO.read(image.toFile());
            if(img==null||img.getWidth()<128||img.getHeight()<128||
               img.getWidth()>4096||img.getHeight()>4096)
                return new ModelAsset(null,"image dimensions invalid");
            if(!img.getColorModel().hasAlpha())return new ModelAsset(null,"render has no transparency");
            int samples=0,clear=0;
            for(int y=0;y<img.getHeight();y+=Math.max(1,img.getHeight()/100))for(int x=0;x<img.getWidth();x+=Math.max(1,img.getWidth()/100)){
                samples++;if(((img.getRGB(x,y)>>>24)&255)<230)clear++;
            }
            if(clear*10<samples)return new ModelAsset(null,"render needs transparent background");
            return new ModelAsset(img,"owner-reviewed source recorded");
        }catch(Exception ignored){return new ModelAsset(null,"failed to parse artwork");}
    }
    private static ModelAsset approvedModel(String boss){
        return resolveModelArt(boss,Path.of("assets","verified-models"));
    }
    private static BufferedImage readArt(String asset) throws IOException {
        if(asset.endsWith("-portrait.png")) {
            String boss=asset.substring(0,asset.indexOf("-portrait.png"));
            BufferedImage result=approvedModel(boss).image;
            if(result!=null)return result;
        }
        try(var stream=TrashPandaShell.class.getResourceAsStream("/assets/"+asset)) {
            return stream==null?null:ImageIO.read(stream);
        }
    }
    private static boolean hasModelArt(String boss){return approvedModel(boss).image!=null;}
    private static ImageIcon scaled(String asset,int w,int h) {
        try {
            BufferedImage img=readArt(asset);
            if(img==null)return null;
            BufferedImage fitted=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=fitted.createGraphics();
            boolean lowResolution=img.getWidth()<64||img.getHeight()<64;
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                lowResolution?RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR:RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            double scale=Math.min((double)w/img.getWidth(),(double)h/img.getHeight());
            int sw=Math.max(1,(int)Math.round(img.getWidth()*scale));
            int sh=Math.max(1,(int)Math.round(img.getHeight()*scale));
            g.drawImage(img,(w-sw)/2,(h-sh)/2,sw,sh,null);g.dispose();
            return new ImageIcon(fitted);
        } catch(Exception ignored){return null;}
    }
    private static JLabel artwork(String asset,int w,int h) {
        JLabel l=new JLabel();
        ImageIcon icon=scaled(asset,w,h);
        if(icon!=null)l.setIcon(icon);
        else {l.setText("ART");l.setForeground(GOLD);l.setHorizontalAlignment(SwingConstants.CENTER);}
        size(l,w,h);return l;
    }
    private static JLabel bookGlyph() {
        Icon book=new Icon(){
            public int getIconWidth(){return 19;}
            public int getIconHeight(){return 20;}
            public void paintIcon(Component c,Graphics graphics,int x,int y) {
                Graphics2D g=(Graphics2D)graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setStroke(new BasicStroke(1.45f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g.setColor(GOLD);
                g.drawLine(x+9,y+4,x+9,y+16);
                g.drawLine(x+2,y+4,x+2,y+15);g.drawLine(x+16,y+4,x+16,y+15);
                g.drawLine(x+2,y+4,x+5,y+3);g.drawLine(x+5,y+3,x+9,y+5);
                g.drawLine(x+9,y+5,x+13,y+3);g.drawLine(x+13,y+3,x+16,y+4);
                g.drawLine(x+2,y+15,x+6,y+14);g.drawLine(x+6,y+14,x+9,y+16);
                g.drawLine(x+9,y+16,x+13,y+14);g.drawLine(x+13,y+14,x+16,y+15);
                g.dispose();
            }
        };
        JLabel l=new JLabel(book);l.setToolTipText("Open Boss Chronicle details");size(l,20,20);return l;
    }
    private JPanel portraitFrame(String boss,int w,int h) {
        boolean verified=hasModelArt(boss);
        JPanel p=new JPanel(new BorderLayout(0,1));p.setOpaque(true);
        p.setBackground(boss.equals("scurrius")?new Color(36,36,29):boss.equals("sarachnis")?new Color(39,29,29):new Color(28,37,28));
        p.setBorder(BorderFactory.createLineBorder(new Color(141,111,67)));
        int artworkHeight=h-2;
        // Comparison view is a crop of the owner's example design, NOT verified OSRS model art.
        boolean reference=referencePreviewMode&&!verified;
        int imageSide=(verified||reference)?Math.min(w-2,artworkHeight):Math.min(49,artworkHeight);
        JLabel a=artwork(reference?boss+"-design-reference.png":assetFor(boss),imageSide,artworkHeight);
        a.setHorizontalAlignment(SwingConstants.CENTER);
        a.setToolTipText(verified?"Owner-reviewed full boss model":reference?
            "Design screenshot crop only; not approved game artwork":
            "Authentic small OSRS sprite; full model art pending approval");
        p.add(a,BorderLayout.CENTER);
        size(p,w,h);return p;
    }
    private static String assetFor(String boss){return boss+"-portrait.png";}
    private static String dropIcon(String boss){return boss.equals("scurrius")?"28798-osrs.png":boss.equals("sarachnis")?"23528-osrs.png":"22372-osrs.png";}
    private static String styleIcon(String s){return s.equalsIgnoreCase("Melee")?"4587-osrs.png":s.equalsIgnoreCase("Ranged")?"861-osrs.png":"6889-osrs.png";}
    private JPanel header() {
        JPanel h=new JPanel(new BorderLayout(6,0));h.setBackground(new Color(25,25,25));
        h.setBorder(new EmptyBorder(6,7,6,7));size(h,SIDEBAR_WIDTH,56);
        JLabel raccoon=artwork("approved-armored-raccoon.png",42,42);h.add(raccoon,BorderLayout.WEST);
        JPanel branding=column(137);
        JLabel title=serif("TrashPanda Tracker",11);
        title.setToolTipText("TrashPanda Tracker · Feral Enterprises");
        append(branding,title,3);
        append(branding,lbl("FERAL ENTERPRISES · BURROW",7,MUTED,false),0);
        h.add(branding,BorderLayout.CENTER);
        FButton gear=action("",23,26,false,this::openSettings);
        gear.setIcon(settingsIcon());gear.setName("appearance-gear");
        gear.getAccessibleContext().setAccessibleName("Appearance settings");
        gear.setToolTipText("Appearance settings · shell only");
        JPanel gearWrap=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,7));gearWrap.setOpaque(false);
        gearWrap.add(gear);h.add(gearWrap,BorderLayout.EAST);
        return h;
    }
    // Draw the header gear directly. Fonts vary by OS/JDK and may render Unicode gear as an ellipsis.
    private static Icon settingsIcon(){
        return new Icon(){
            public int getIconWidth(){return 17;}
            public int getIconHeight(){return 17;}
            public void paintIcon(Component c,Graphics gr,int x,int y){
                Graphics2D g=(Graphics2D)gr.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(GOLD);g.setStroke(new BasicStroke(2.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                int cx=x+8,cy=y+8;
                for(int i=0;i<8;i++){
                    double angle=i*Math.PI/4.0;
                    int x1=cx+(int)Math.round(Math.cos(angle)*5.0);
                    int y1=cy+(int)Math.round(Math.sin(angle)*5.0);
                    int x2=cx+(int)Math.round(Math.cos(angle)*7.2);
                    int y2=cy+(int)Math.round(Math.sin(angle)*7.2);
                    g.drawLine(x1,y1,x2,y2);
                }
                g.setStroke(new BasicStroke(1.6f));g.drawOval(cx-5,cy-5,10,10);
                g.fillOval(cx-2,cy-2,4,4);g.dispose();
            }
        };
    }
    private JPanel navBar() {
        JPanel whole=new JPanel(new BorderLayout());whole.setOpaque(false);
        whole.setBorder(new EmptyBorder(0,6,4,6));
        nav.setOpaque(false);
        for(String tab:List.of("Home","Pathfinder","Outfitter","Sessions")) {
            String id=tab.toLowerCase(Locale.ROOT);
            FButton b=new FButton(tab,false);
            b.setFont(new Font("SansSerif",Font.PLAIN, uiPt(tab.equals("Pathfinder")?8:9)));
            b.setMargin(new Insets(0,0,0,0));
            b.addActionListener(e->go(id));navButtons.put(id,b);nav.add(b);
        }
        size(nav,213,33);whole.add(nav,BorderLayout.CENTER);
        JLabel rule=lbl("",1,GOLD,false);rule.setBorder(BorderFactory.createMatteBorder(0,0,1,0,GOLD));
        whole.add(rule,BorderLayout.SOUTH);return whole;
    }
    private static final class FButton extends JButton {
        private boolean active;
        FButton(String text,boolean active) {
            super(text);this.active=active;
            setOpaque(false);setContentAreaFilled(false);setBorderPainted(false);
            setFocusPainted(false);setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setForeground(active?new Color(251,216,159):LIGHT);
            setFont(new Font("SansSerif",Font.BOLD,uiPt(11)));
            setMargin(new Insets(2,4,2,4));
        }
        void active(boolean v){active=v;setForeground(v?new Color(255,222,166):LIGHT);repaint();}
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g=(Graphics2D)graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth()-1,h=getHeight()-1;
            Color top=active?GOLD.darker().darker():new Color(54,54,53);
            Color bottom=active?GOLD.darker().darker().darker():new Color(32,33,33);
            g.setPaint(new GradientPaint(0,0,top,0,h,bottom));
            g.fillRoundRect(1,1,w-2,h-2,6,6);
            if(getModel().isRollover()) {g.setColor(new Color(220,174,98,30));g.fillRoundRect(1,1,w-2,h-2,6,6);}
            g.setColor(active?GOLD:EDGE);g.setStroke(new BasicStroke(active?1.4f:1f));
            g.drawRoundRect(1,1,w-2,h-2,6,6);
            g.dispose();super.paintComponent(graphics);
        }
    }
    private static final class Card extends JPanel {
        private final boolean golden;
        private boolean striped;
        Card(boolean golden) {
            this.golden=golden;setOpaque(false);
            setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(9,9,10,9));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g=(Graphics2D)g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0,0,new Color(45,46,43),getWidth(),getHeight(),CARD));
            g.fill(new RoundRectangle2D.Float(0.5f,0.5f,getWidth()-1.5f,getHeight()-1.5f,8,8));
            g.setColor(golden?GOLD:EDGE);
            g.setStroke(new BasicStroke(golden?1.5f:1f));
            g.drawRoundRect(1,1,getWidth()-3,getHeight()-3,8,8);
            if(striped) {g.setColor(GOLD);g.fillRoundRect(1,3,4,getHeight()-6,3,3);}
            g.dispose();super.paintComponent(g0);
        }
    }
    private Card card(boolean gold){Card c=new Card(gold);c.setMaximumSize(new Dimension(INNER,Short.MAX_VALUE));return c;}
    private static void clickable(Component c,Runnable callback) {
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        c.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){callback.run();}});
        if(c instanceof Container && !(c instanceof JButton))
            for(Component child:((Container)c).getComponents())clickable(child,callback);
    }
    private JPanel verticalPage() {
        JPanel p=column(INNER);
        p.setBorder(new EmptyBorder(0,0,8,0));
        return p;
    }
    private static void append(JPanel page, JComponent c, int after) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(c);if(after>0)page.add(gap(after));
    }
    private static JLabel section(String name) {
        JLabel l=lbl(name,12,GOLD,true);
        l.setBorder(new EmptyBorder(2,2,3,0));return l;
    }
    private static FButton action(String title,int w,int h,boolean gold,Runnable task) {
        FButton b=new FButton(title,gold);size(b,w,h);b.addActionListener(e->task.run());return b;
    }
    private void render() {
        for(var e:navButtons.entrySet())e.getValue().active(e.getKey().equals(activeTab()));
        JPanel view=switch(page){
            case "home"->home();case "browse"->browse();case "detail"->detail();case "settings"->settings();
            case "pathfinder"->pathfinder();case "outfitter"->outfitter();
            default->sessions();};
        content.removeAll();content.add(view,BorderLayout.NORTH);
        content.revalidate();content.repaint();
        SwingUtilities.invokeLater(()->scroll.getVerticalScrollBar().setValue(0));
    }
    private String activeTab() {
        String tab=page.equals("settings")?settingsReturn:page;
        return (tab.equals("browse")||tab.equals("detail"))?"outfitter":tab;
    }
    public void go(String tab){page=tab;render();}
    private void chronicle() {if(!chronicleInitialized){chronicleBoss=outfitBoss;chronicleInitialized=true;}chronicleReturn=page;page="browse";render();}
    private void openBoss(String id) {
        chronicleBoss=id;
        if(page.equals("browse"))chronicleListY=scroll.getVerticalScrollBar().getValue();
        if(!page.equals("browse")&&!page.equals("detail"))chronicleReturn=page;
        page="detail";caExpanded=false;dropInfoExpanded=false;render();
    }
    private void backToBosses() {page="browse";render();SwingUtilities.invokeLater(()->scroll.getVerticalScrollBar().setValue(chronicleListY));}
    /** Keep the reader anchored when expanding reference details. */
    private void rerenderKeepingScroll() {
        final int oldY=scroll.getVerticalScrollBar().getValue();
        render();
        SwingUtilities.invokeLater(()->scroll.getVerticalScrollBar().setValue(oldY));
    }

    private Boss boss(String id) {return BOSSES.stream().filter(b->b.id.equals(id)).findFirst().orElse(BOSSES.get(0));}
    private void rotateChronicle(int delta) {
        List<Boss> sorted = new ArrayList<>(BOSSES);
        sorted.sort(Comparator.comparing(b -> b.name));
        int pos=0;
        for(int i=0;i<sorted.size();i++)if(sorted.get(i).id.equals(chronicleBoss))pos=i;
        chronicleBoss=sorted.get((pos+delta+sorted.size())%sorted.size()).id;
        caExpanded=false;dropInfoExpanded=false;
        page="detail";
        render();
    }
    /** Native scrollable 5-row popup, restricted to actual fixture bosses; no fabricated catalog. */
    private void chooseBoss(Component anchor, boolean chronicleSelection) {
        JPopupMenu popup=new JPopupMenu();
        popup.setBackground(CARD);
        String[] names=BOSSES.stream().map(b->b.name).sorted().toArray(String[]::new);
        JList<String> options=new JList<>(names);
        options.setVisibleRowCount(5);
        options.setFixedCellHeight(26);
        options.setFont(new Font("SansSerif",Font.PLAIN,uiPt(12)));
        options.setForeground(LIGHT);options.setBackground(CARD);
        options.setSelectionForeground(LIGHT);options.setSelectionBackground(new Color(113,80,44));
        options.setSelectedValue(boss(chronicleSelection?chronicleBoss:outfitBoss).name,true);
        Runnable selectBoss=()->{
            if(options.getSelectedValue()==null)return;
            for(Boss b:BOSSES)if(b.name.equals(options.getSelectedValue())){
                if(chronicleSelection){chronicleBoss=b.id;page="detail";caExpanded=false;dropInfoExpanded=false;}
                else outfitBoss=b.id;
                popup.setVisible(false);render();break;
            }
        };
        options.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){
            if(e.getClickCount()>=1 && SwingUtilities.isLeftMouseButton(e))selectBoss.run();
        }});
        options.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0),"chooseBoss");
        options.getActionMap().put("chooseBoss",new AbstractAction(){
            @Override public void actionPerformed(ActionEvent e){selectBoss.run();}
        });
        JScrollPane listScroll=new JScrollPane(options,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        listScroll.setPreferredSize(new Dimension(174,Math.min(names.length,5)*26+4));
        popup.add(listScroll);
        popup.show(anchor,0,anchor.getHeight());
    }

    private static void labelText(JPanel p,String message,int width,int pt,Color color) {append(p,wrapped(message,width,pt,color),3);}
    private JPanel home() {
        JPanel p=verticalPage();
        append(p,section("YOUR ADVENTURE"),5);
        Card xp=card(true);
        JPanel xpTop=xrow();
        xpTop.add(lbl("This week's XP",10,MUTED,false),BorderLayout.WEST);
        xpTop.add(lbl("EXAMPLE",8,GOLD,true),BorderLayout.EAST);
        append(xp,xpTop,1);
        append(xp,lbl("+842K XP",23,GREEN,true),6);
        append(xp,(JComponent)line(170),5);
        append(xp,lbl("★  Biggest Win",13,new Color(245,213,158),true),2);
        append(xp,wrapped("Combat Achievement unlocked",169,10,LIGHT),3);
        append(xp,action(homeWinExpanded?"Hide achievement  ⌃":"Review achievement  ›",169,22,false,()->{homeWinExpanded=!homeWinExpanded;rerenderKeepingScroll();}),0);
        if(homeWinExpanded) {
            labelText(xp,"Example only. No verified achievement feed or past sessions are connected.",169,9,MUTED);
            append(xp,action("Open Sessions  ›",169,24,true,()->go("sessions")),2);
        }
        append(p,xp,8);
        if(tipDismissed){
            Card collapsed=card(false);collapsed.striped=true;
            append(collapsed,lbl(tipAction.equals("Done")?"Spoons Tip · done":"Spoons Tip · dismissed",11,LIGHT,true),3);
            append(collapsed,action("Show tip again  ›",169,23,false,()->{tipDismissed=false;tipAction="";rerenderKeepingScroll();}),0);
            append(p,collapsed,8);
        }else{
            Card tip=card(false);tip.striped=true;
            JPanel head=xrow();head.add(artwork("spoons.png",50,58),BorderLayout.WEST);
            JPanel rt=column(113);append(rt,serif("Spoons Tip",14),2);
            labelText(rt,"Cut 2 inventories of uncut gems before your next adventure.",110,9,LIGHT);
            append(rt,lbl("◷  ~2 min · demo",8,GREEN,false),0);head.add(rt,BorderLayout.CENTER);
            append(tip,head,5);
            JPanel buttons=new JPanel(new GridLayout(1,2,6,0));buttons.setOpaque(false);
            buttons.add(action("Done",81,23,false,()->{tipDismissed=true;tipAction="Done";rerenderKeepingScroll();}));
            buttons.add(action("Dismiss",81,23,false,()->{tipDismissed=true;tipAction="Dismiss";rerenderKeepingScroll();}));
            size(buttons,168,23);append(tip,buttons,0);append(p,tip,8);
        }
        Card hunts=card(false);hunts.striped=true;
        JPanel title=xrow();title.add(artwork("bandit.png",49,43),BorderLayout.WEST);
        JPanel txt=column(116);append(txt,serif("Bandit's Boss Hunts",12),2);
        append(txt,lbl("Sample trip ratings",8,MUTED,false),0);title.add(txt,BorderLayout.CENTER);append(hunts,title,5);
        for(String id:List.of("scurrius","sarachnis","bryophyta")) {
            Boss b=boss(id);JPanel hr=xrow();
            JPanel left=row();JLabel dot=lbl("●",12,id.equals("scurrius")?GREEN:id.equals("sarachnis")?new Color(243,200,74):new Color(229,92,83),true);
            left.add(dot);left.add(lbl(b.name,10,LIGHT,false));
            hr.add(left,BorderLayout.WEST);
            hr.add(lbl(id.equals("scurrius")?"Comfortable ›":id.equals("sarachnis")?"Stretch ›":"Practice ›",9,GOLD,false),BorderLayout.EAST);
            append(hunts,hr,4);if(!id.equals("bryophyta"))append(hunts,(JComponent)line(169),4);
            clickable(hr,()->openBoss(id));
        }
        append(p,hunts,8);
        Card noodles=card(false);noodles.striped=true;JPanel n=xrow();
        n.add(artwork("noodles.png",52,57),BorderLayout.WEST);
        JPanel text=column(110);append(text,serif("Noodles' Next Move",12),3);
        labelText(text,"Desert Treasure II? Review your next Pathfinder requirement.",109,9,LIGHT);
        n.add(text,BorderLayout.CENTER);append(noodles,n,5);
        append(noodles,action("View path  ›",CARD_TEXT,25,true,()->go("pathfinder")),0);
        append(p,noodles,9);
        append(p,action("Boss Chronicle  ›",INNER,27,true,this::chronicle),5);
        append(p,wrapped("UI-ONLY DEMO · NO ACCOUNT DATA",188,8,MUTED),0);
        return p;
    }
    private String[] cats={"All","Training","Regular"};
    /** Bronze/graphite native Swing selector. No platform-blue borders or arrows. */
    private JComboBox<String> combo(String[] values,String selected,int width) {
        JComboBox<String> cb=new JComboBox<>(values);
        cb.setUI(new BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                JButton arrow=new JButton() {
                    @Override protected void paintComponent(Graphics g0) {
                        Graphics2D g=(Graphics2D)g0.create();
                        g.setColor(new Color(49,45,39));g.fillRect(0,0,getWidth(),getHeight());
                        g.setColor(GOLD);g.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                        int mx=getWidth()/2,my=getHeight()/2;
                        g.drawLine(mx-4,my-2,mx,my+2);g.drawLine(mx,my+2,mx+4,my-2);
                        g.dispose();
                    }
                };
                arrow.setFocusPainted(false);arrow.setBorderPainted(false);
                arrow.setPreferredSize(new Dimension(18,24));
                return arrow;
            }
        });
        cb.setRenderer((list,value,index,isSelected,cellHasFocus)->{
            JLabel label=lbl(value==null?"":value.toString(),10,LIGHT,false);
            label.setOpaque(true);label.setBorder(new EmptyBorder(2,6,2,2));
            label.setBackground(isSelected?new Color(103,75,46):new Color(35,35,34));
            return label;
        });
        cb.setSelectedItem(selected);cb.setFocusable(false);
        cb.setForeground(LIGHT);cb.setBackground(new Color(35,35,34));
        cb.setBorder(new LineBorder(new Color(112,102,83),1,true));
        size(cb,width,28);return cb;
    }
    private JPanel browse() {
        JPanel p=verticalPage();JPanel heading=xrow();
        heading.add(action("‹ Back",62,28,true,()->go(chronicleReturn)),BorderLayout.WEST);
        heading.add(lbl("BOSS CHRONICLE",12,GOLD,true),BorderLayout.CENTER);
        append(p,heading,11);
        JTextField field=new JTextField(search) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D icon=(Graphics2D)g.create();
                icon.setColor(GOLD);icon.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                icon.setStroke(new BasicStroke(1.5f));icon.drawOval(9,11,9,9);icon.drawLine(17,19,21,23);icon.dispose();
                if(getText().isEmpty()&&!hasFocus()) {
                    g.setColor(MUTED);g.setFont(new Font("SansSerif",Font.PLAIN,uiPt(11)));
                    g.drawString("Search bosses...",29,21);
                }
            }
        };
        field.setForeground(LIGHT);field.setCaretColor(GOLD);field.setBackground(new Color(19,20,20));
        field.setFont(new Font("SansSerif",Font.PLAIN,uiPt(12)));
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(EDGE),new EmptyBorder(0,29,0,5)));
        size(field,INNER,34);field.setToolTipText("Search bosses by name");append(p,field,8);
        // One native 225px filter row: category, name order, difficulty.
        // Tooltips carry full labels without adding another row of UI chrome.
        JPanel filters=new JPanel(new GridLayout(1,3,4,0));filters.setOpaque(false);
        JComboBox<String> types=combo(cats,category,69);
        JComboBox<String> alpha=combo(new String[]{"Top","A–Z","Z–A"},ordering,57);
        JComboBox<String> diff=combo(new String[]{"Any","Easy first","Hard first"},difficulty,59);
        types.setToolTipText("Boss category");alpha.setToolTipText("Sort: Top = featured example bosses, or A–Z / Z–A by name");
        diff.setToolTipText("Difficulty: Any / Easy first / Hard first");
        types.setFont(new Font("SansSerif",Font.PLAIN,uiPt(9)));
        alpha.setFont(new Font("SansSerif",Font.PLAIN,uiPt(9)));
        diff.setFont(new Font("SansSerif",Font.PLAIN,uiPt(9)));
        filters.add(types);filters.add(alpha);filters.add(diff);
        size(filters,INNER,28);append(p,filters,5);
        JPanel compare=xrow();
        JLabel modeInfo=lbl(referencePreviewMode?"DESIGN REF · NOT GAME ART":"GENUINE OSRS SPRITES",7,MUTED,false);
        compare.add(modeInfo,BorderLayout.WEST);
        FButton compareButton=action(referencePreviewMode?"Sprites":"Compare",64,19,false,()->{
            referencePreviewMode=!referencePreviewMode;rerenderKeepingScroll();
        });
        compareButton.setToolTipText("Toggle screenshots from approved visual brief; these are not vetted OSRS images");
        compare.add(compareButton,BorderLayout.EAST);
        append(p,compare,7);
        JPanel results=column(INNER);append(p,results,8);
        Runnable refresh=()->{results.removeAll();
            List<Boss> visible=new ArrayList<>();
            for(Boss b:BOSSES)if((category.equals("All")||category.equals(b.family))&&
                (b.name.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))||b.subtitle.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))))visible.add(b);
            if(difficulty.equals("Easy first"))visible.sort(Comparator.comparingInt(b->b.difficulty));
            else if(difficulty.equals("Hard first"))visible.sort((a,b)->Integer.compare(b.difficulty,a.difficulty));
            else if(ordering.equals("A–Z"))visible.sort(Comparator.comparing(b->b.name));
            else if(ordering.equals("Z–A"))visible.sort(Comparator.comparing((Boss b)->b.name).reversed());
            if(visible.isEmpty()) {
                append(results,wrapped("No matching bosses in this 3-boss demo.",170,11,MUTED),7);
                append(results,action("Clear filters  ›",163,26,false,()->{
                    search="";category="All";ordering="Top";difficulty="Any";render();
                }),5);
            }
            for(Boss b:visible)append(results,bossCard(b),11);
            results.revalidate();results.repaint();};
        field.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){sync();}public void removeUpdate(DocumentEvent e){sync();}
            public void changedUpdate(DocumentEvent e){sync();}
            private void sync(){search=field.getText();refresh.run();}
        });
        types.addActionListener(e->{category=String.valueOf(types.getSelectedItem());refresh.run();});
        alpha.addActionListener(e->{ordering=String.valueOf(alpha.getSelectedItem());refresh.run();});
        diff.addActionListener(e->{difficulty=String.valueOf(diff.getSelectedItem());refresh.run();});
        refresh.run();
        append(p,wrapped("Combat icons show styles, not account readiness. Original boss renders are still awaiting approval.",INNER-9,8,MUTED),8);
        return p;
    }
    private JPanel settings(){
        JPanel p=verticalPage();
        JPanel head=xrow();head.add(section("APPEARANCE"),BorderLayout.WEST);
        head.add(action("‹ Back",69,27,false,this::closeSettings),BorderLayout.EAST);
        append(p,head,6);
        append(p,wrapped("SHELL SETTINGS · Stored on this computer only",185,9,MUTED),10);
        Card font=card(true);
        append(font,lbl("FONT SIZE",11,GOLD,true),6);
        labelText(font,"Change text scale without widening the approved 225px sidebar.",169,9,MUTED);
        JPanel opts=new JPanel(new GridLayout(1,3,4,0));opts.setOpaque(false);
        for(String name:FONT_SIZES){
            String display=name.equals("Compact")?"Small":name.equals("Standard")?"Normal":"Large";
            FButton item=action(display,53,30,name.equals(fontChoice),()->updateAppearance(name,accentChoice));
            item.setFont(new Font("SansSerif",Font.PLAIN,uiPt(8)));
            item.setToolTipText(name+" font size");opts.add(item);
        }
        size(opts,169,30);append(font,opts,8);
        labelText(font,"Selected: "+fontChoice,160,10,LIGHT);
        append(p,font,10);
        Card accents=card(false);
        append(accents,lbl("ACCENT COLOR",11,GOLD,true),7);
        JPanel colors=new JPanel(new GridLayout(3,1,0,4));colors.setOpaque(false);
        for(String name:ACCENTS){
            FButton item=action((name.equals(accentChoice)?"●  ":"○  ")+name,169,29,name.equals(accentChoice),()->updateAppearance(fontChoice,name));
            item.setHorizontalAlignment(SwingConstants.LEFT);
            item.setForeground(name.equals(accentChoice)?accentColor(name):LIGHT);
            item.setToolTipText("Use "+name+" accent. Does not affect game data.");colors.add(item);
        }
        size(colors,169,96);append(accents,colors,3);
        append(p,accents,10);
        Card preview=card(true);
        append(preview,lbl("LIVE PREVIEW",11,GOLD,true),6);
        append(preview,serif("Scurrius",17),5);
        labelText(preview,"Your next adventure · Sample text",167,10,LIGHT);
        append(preview,(JComponent)line(166),5);
        append(preview,lbl("VERIFY · No account connected",9,GOLD,true),4);
        append(p,preview,10);
        Card save=card(false);
        append(save,lbl("LOCAL PREFERENCES",11,GOLD,true),4);
        labelText(save,"Changes are saved to a standalone appearance file. They do not touch TPT, RuneLite settings, account data, or your bank.",169,9,MUTED);
        if(settingsSaved)append(save,lbl("✓  Appearance saved",10,GREEN,true),4);
        if(!settingsError.isEmpty())labelText(save,settingsError,169,9,GOLD);
        append(save,action("Restore defaults",169,29,false,()->updateAppearance("Standard","Bronze")),3);
        append(p,save,12);
        append(p,wrapped("No fifth tab: Appearance is a subview opened from the header gear.",181,9,MUTED),0);
        return p;
    }

    private static String rewardType(Boss b) {
        return b.id.equals("bryophyta")?"Chest reward":"Notable boss drop";
    }
    private JPanel bossCard(Boss b) {
        Card c=card(false);
        JPanel top=xrow();
        top.add(portraitFrame(b.id,59,63),BorderLayout.WEST);
        JPanel info=column(108);
        JPanel nameRow=xrow();
        JLabel name=serif(b.name,b.name.equals("Bryophyta")?13:14);
        nameRow.add(name,BorderLayout.WEST);
        nameRow.add(bookGlyph(),BorderLayout.EAST);
        size(nameRow,105,21);append(info,nameRow,1);
        append(info,wrapped(b.subtitle,103,9,MUTED),3);
        append(info,styleMarks(b),0);top.add(info,BorderLayout.CENTER);
        append(c,top,7);append(c,(JComponent)line(168),6);
        JPanel reward=xrow();
        JPanel left=row();left.add(artwork(dropIcon(b.id),17,17));
        left.add(lbl(b.drop,8,LIGHT,false));reward.add(left,BorderLayout.WEST);
        JLabel oddsLabel=lbl(b.odds,8,GOLD,true);oddsLabel.setToolTipText("Published reward rate · no personal KC inferred");reward.add(oddsLabel,BorderLayout.EAST);
        append(c,reward,0);
        if(b.id.equals("bryophyta"))c.setToolTipText("Chest reward after Bryophyta kill, not a direct boss drop. Open for details.");
        else c.setToolTipText("Open "+b.name+" Chronicle detail · demo only");
        clickable(c,()->openBoss(b.id));
        c.setFocusable(true);
        c.getAccessibleContext().setAccessibleName("Open "+b.name+" in Boss Chronicle");
        c.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0),"open-boss");
        c.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,0),"open-boss");
        c.getActionMap().put("open-boss",new AbstractAction(){public void actionPerformed(ActionEvent e){openBoss(b.id);}});
        return c;
    }
    private JPanel styleMarks(Boss b) {
        JPanel r=row();r.add(styleBadge("Melee",b.melee,new Color(143,62,55)));
        r.add(styleBadge("Ranged",b.ranged,new Color(45,109,66)));
        r.add(styleBadge("Magic",b.magic,new Color(65,80,155)));
        if(!b.combatNote.isEmpty())r.add(lbl(b.combatNote,9,GOLD,false));return r;
    }
    /** Authentic OSRS item graphics. Inactive means not recommended, not unavailable to the account. */
    private JLabel styleBadge(String combat,boolean enabled,Color tint) {
        JLabel l=new JLabel();
        l.setToolTipText(combat+(enabled?" recommended":" not suggested for this example"));
        ImageIcon icon=scaled(styleIcon(combat),19,18);
        if(icon!=null)l.setIcon(icon);
        l.setHorizontalAlignment(SwingConstants.CENTER);l.setVerticalAlignment(SwingConstants.CENTER);
        l.setOpaque(true);l.setBackground(enabled?tint:new Color(43,43,43));
        l.setBorder(BorderFactory.createLineBorder(enabled?tint.brighter():EDGE));
        if(!enabled)l.setEnabled(false);
        size(l,23,23);return l;
    }
    /** Wiki-sourced task names and tiers. Account completion is deliberately UNKNOWN. */
    private static String[][] caTasks(String bossId) {
        return switch(bossId) {
            case "scurrius" -> new String[][] {
                {"Easy","Scurrius Novice","Kill Scurrius once."},
                {"Easy","Sit Rat","Finish Scurrius with a ratbane weapon in a private instance."},
                {"Medium","Perfect Scurrius","Avoid Tail Swipe and Falling Bricks; pray correctly against ranged and magic."},
                {"Medium","Scurrius Champion","Kill Scurrius 10 times."},
                {"Medium","Efficient Pest Control","Kill 6 Giant Rats in the lair within 3 seconds."}
            };
            case "sarachnis" -> new String[][] {
                {"Easy","Sarachnis Novice","Kill Sarachnis 10 times."},
                {"Medium","Newspaper Enthusiast","Kill Sarachnis with a crush weapon."},
                {"Medium","Sarachnis Champion","Kill Sarachnis 25 times."},
                {"Hard","Inspect Repellent","Kill Sarachnis without her dealing damage to anyone."},
                {"Hard","Ready to Pounce","Kill Sarachnis without two consecutive ranged attacks."}
            };
            case "bryophyta" -> new String[][] {
                {"Easy","Bryophyta Novice","Open Bryophyta's chest once."},
                {"Easy","Preparation Is Key","Kill Bryophyta without poison damage."},
                {"Easy","Fighting as Intended II","Kill Bryophyta on a free-to-play world."},
                {"Easy","A Slow Death","Kill Bryophyta with poison or venom as the final damage."},
                {"Easy","Protection from Moss","Kill with Protect from Magic active."},
                {"Medium","Bryophyta Champion","Open Bryophyta's chest five times."},
                {"Medium","Quick Cutter","Kill three growthlings within three seconds of the first."}
            };
            default -> new String[0][0];
        };
    }
    private static String rewardDescription(Boss b) {
        return b.id.equals("bryophyta")?
            "The 1/118 rate is for opening Bryophyta's chest with a mossy key, not for the boss kill itself.":
            b.id.equals("scurrius")?
            "Scurrius' spine: published 1/33 MVP or solo loot roll. Only one player can receive a spine per kill.":
            "Sarachnis cudgel: published 1/384 Sarachnis pre-roll.";
    }
    private JPanel detail() {
        JPanel p=verticalPage();Boss b=boss(chronicleBoss);
        JPanel heading=xrow();heading.add(action("‹ BOSSES",73,28,true,this::backToBosses),BorderLayout.WEST);
        heading.add(lbl(b.name.toUpperCase(Locale.ROOT),13,GOLD,true),BorderLayout.CENTER);append(p,heading,6);
        JPanel navboss=new JPanel(new BorderLayout(4,0));navboss.setOpaque(false);
        navboss.add(action("‹",31,26,false,()->rotateChronicle(-1)),BorderLayout.WEST);
        FButton bossPicker=action("Choose boss ▾",111,26,false,()->{});
        bossPicker.addActionListener(e->chooseBoss(bossPicker,true));
        navboss.add(bossPicker,BorderLayout.CENTER);
        navboss.add(action("›",31,26,false,()->rotateChronicle(1)),BorderLayout.EAST);
        append(p,navboss,9);
        // Account data is unavailable; the dashboard itself carries explicit evidence states.
        Card boss=card(true);JPanel hero=xrow();
        JPanel pic=portraitFrame(b.id,65,73);hero.add(pic,BorderLayout.WEST);
        JPanel text=column(108);append(text,serif(b.name,18),1);
        append(text,wrapped(b.id.equals("scurrius")?"The Rat King":b.subtitle,103,10,MUTED),5);append(text,styleMarks(b),0);
        hero.add(text,BorderLayout.CENTER);append(boss,hero,5);
        if(referencePreviewMode&&!hasModelArt(b.id))
            append(boss,lbl("REFERENCE ONLY · UNVERIFIED ART",7,MUTED,false),5);
        append(boss,(JComponent)line(CARD_TEXT),6);
        JPanel rare=xrow();rare.add(lbl(b.id.equals("bryophyta")?"CHEST REWARD":"NOTABLE RARE DROP",10,GOLD,true),BorderLayout.WEST);
        JLabel published=lbl("Published",8,new Color(238,203,146),false);
        published.setToolTipText("Published OSRS rate · demonstration, not your personal loot history");
        rare.add(published,BorderLayout.EAST);
        append(boss,rare,4);
        JPanel drop=xrow();
        JPanel rewardName=row();rewardName.add(artwork(dropIcon(b.id),17,17));
        rewardName.add(lbl(b.drop,8,LIGHT,false));drop.add(rewardName,BorderLayout.WEST);
        JLabel dropRate=lbl(b.odds,8,GOLD,true);
        dropRate.setToolTipText("Published per eligible roll; not personal aggregated probability");
        drop.add(dropRate,BorderLayout.EAST);
        append(boss,drop,5);
        append(boss,action(dropInfoExpanded?"Hide acquisition notes  ‹":"Source & acquisition  ›",167,19,false,()->{dropInfoExpanded=!dropInfoExpanded;rerenderKeepingScroll();}),0);
        if(dropInfoExpanded)labelText(boss,rewardDescription(b),165,9,LIGHT);
        append(p,boss,8);
        append(p,section("YOUR BOSS DASHBOARD"),3);
        JPanel grid=new JPanel(new GridLayout(1,2,5,0));grid.setOpaque(false);
        for(String[] v:new String[][]{{"Verified kills","Unavailable"},{"Your rare drops","Unverified"}}) {
            Card stat=card(false);append(stat,lbl(v[0],8,MUTED,false),3);
            append(stat,lbl(v[1],11,LIGHT,true),0);grid.add(stat);
        }
        size(grid,INNER,52);append(p,grid,7);
        Card odds=card(false);append(odds,lbl("◎  Aggregated odds",11,new Color(243,211,155),true),4);
        labelText(odds,"Verified personal KC required.",169,10,LIGHT);
        labelText(odds,"No probability calculated · demo",169,9,GOLD);append(p,odds,7);
        Card ca=card(false);
        append(ca,lbl("★  Combat Achievements",12,new Color(243,211,155),true),4);
        append(ca,lbl(caTasks(b.id).length+" Wiki tasks · progress unknown",9,MUTED,false),3);
        append(ca,action(caExpanded?"Hide task details  ‹":"View tasks and tiers  ›",167,23,false,()->{caExpanded=!caExpanded;rerenderKeepingScroll();}),3);
        if(caExpanded) {
            for(String[] task:caTasks(b.id)){
                append(ca,lbl("○  "+task[0]+" · "+task[1],9,LIGHT,true),2);
                labelText(ca,task[2],163,9,MUTED);
                append(ca,(JComponent)line(166),5);
            }
            labelText(ca,"Source: OSRS Wiki Combat Achievements. Personal completion is not connected.",163,8,GOLD);
        }
        append(ca,(JComponent)line(168),5);append(ca,lbl("Quick tip",11,GOLD,true),2);
        labelText(ca,b.tip,165,10,LIGHT);append(p,ca,7);
        Card prepare=card(false);append(prepare,lbl("⚒  Prepare for the fight",12,new Color(243,211,155),true),4);
        labelText(prepare,"Preview gear and supplies · demo only",169,9,LIGHT);
        append(prepare,action("Open "+b.name+" in Outfitter  ›",174,29,true,()->{outfitBoss=b.id;go("outfitter");}),0);
        append(p,prepare,7);
        append(p,wrapped("‹ Back restores Chronicle search and scroll position. The Outfitter boss stays independent.",INNER-5,9,MUTED),0);
        return p;
    }
    /** Clear in-memory visual slot markers; does not modify an OSRS inventory. */
    private void resetInventoryMarks(){java.util.Arrays.fill(inventoryMarks,(byte)0);selectedInventorySlot=-1;rerenderKeepingScroll();}
    private void cycleInventoryMark(int index){
        if(index<0||index>=28)throw new IllegalArgumentException("Inventory index");
        inventoryMarks[index]=(byte)((inventoryMarks[index]+1)%4);
        selectedInventorySlot=index;
        rerenderKeepingScroll();
    }
    private String markName(int mark){return switch(mark){case 1->"Switch";case 2->"Supply";case 3->"Prayer";default->"Unassigned";};}
    private Color markColor(int mark){return switch(mark){case 1->GOLD;case 2->GREEN;case 3->new Color(116,165,235);default->EDGE;};}
    /** Accessible, genuine Swing button for an OSRS slot. No fake ownership state is encoded. */
    private static JButton slotButton(String label,int width,int height,Color border,String tooltip,Runnable onClick){
        JButton b=new JButton(label){
            @Override protected void paintComponent(Graphics gr){
                Graphics2D g=(Graphics2D)gr.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(43,45,43));g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,5,5);
                g.setColor(border);g.setStroke(new BasicStroke(1.2f));g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,5,5);
                g.dispose();super.paintComponent(gr);
            }
        };
        b.setFont(new Font("SansSerif",Font.PLAIN,label.length()>7?7:8));
        b.setForeground(LIGHT);b.setOpaque(false);b.setContentAreaFilled(false);
        b.setBorderPainted(false);b.setFocusPainted(false);b.setMargin(new Insets(0,0,0,0));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setHorizontalTextPosition(SwingConstants.CENTER);
        b.setVerticalTextPosition(SwingConstants.BOTTOM);
        b.setIconTextGap(-1);b.setToolTipText(tooltip);
        size(b,width,height);b.addActionListener(e->onClick.run());return b;
    }
    private boolean styleRecommended(String s){
        Boss b=boss(outfitBoss);
        return (s.equals("Melee")&&b.melee)||(s.equals("Ranged")&&b.ranged)||(s.equals("Magic")&&b.magic);
    }
    private JPanel outfitter() {
        JPanel p=verticalPage();JPanel heading=xrow();
        heading.add(section("OUTFITTER"),BorderLayout.WEST);
        heading.add(action("Chronicle ›",98,27,true,this::chronicle),BorderLayout.EAST);
        append(p,heading,9);

        Card plan=card(true);
        append(plan,lbl("BOSS · LOADOUT PLANNER",9,GOLD,true),4);
        append(plan,serif(boss(outfitBoss).name,19),5);
        FButton bossPicker=action("Change boss  ▾",169,26,false,()->{});
        bossPicker.addActionListener(e->chooseBoss(bossPicker,false));append(plan,bossPicker,8);
        // The W05 fixture bosses have no source-confirmed alternate special methods; don't invent one.
        append(plan,lbl("Plan tier",10,GOLD,true),4);
        JPanel tiers=new JPanel(new BorderLayout(3,0));tiers.setOpaque(false);
        for(String t:List.of("Entry","Recommended","Optimal")){
            FButton b=action(t,t.equals("Recommended")?89:37,28,t.equals(tier),()->{tier=t;rerenderKeepingScroll();});
            b.setFont(new Font("SansSerif",Font.PLAIN,t.equals("Recommended")?9:8));
            b.setMargin(new Insets(0,1,0,1));
            b.setToolTipText("Visual tier: "+t+"; no actual gear optimizer connected");
            tiers.add(b,t.equals("Entry")?BorderLayout.WEST:t.equals("Optimal")?BorderLayout.EAST:BorderLayout.CENTER);
        }
        size(tiers,169,28);append(plan,tiers,8);
        append(plan,lbl("Gear source",10,GOLD,true),4);
        JPanel sources=new JPanel(new GridLayout(1,2,4,0));sources.setOpaque(false);
        sources.add(action("Wiki",81,29,source.equals("Wiki Recommended"),()->{source="Wiki Recommended";rerenderKeepingScroll();}));
        FButton best=action("Best in Bank",84,29,source.equals("Best in Bank"),()->{source="Best in Bank";rerenderKeepingScroll();});
        best.setFont(new Font("SansSerif",Font.PLAIN,9));sources.add(best);size(sources,169,29);append(plan,sources,7);
        labelText(plan,source.equals("Best in Bank")?"VERIFY · Saved bank not connected. Ownership unknown.":"Wiki mode preview · No live plan or item ownership data.",168,9,MUTED);
        append(plan,action(bankMemoryVisible?"Hide Bank Memory  ⌃":"Bank Memory  ›",169,26,false,()->{bankMemoryVisible=!bankMemoryVisible;rerenderKeepingScroll();}),6);
        if(bankMemoryVisible){
            append(plan,(JComponent)line(169),6);
            labelText(plan,"Account: not connected  ·  Last bank scan: none",168,9,LIGHT);
            labelText(plan,"Saved items: unavailable  ·  Freshness: unknown",168,9,MUTED);
            labelText(plan,"Preview only. Persistent per-account snapshots and the 7-day sync reminder must be engineered in TPT, not assumed here.",168,9,MUTED);
        }
        append(p,plan,9);

        Card equip=card(false);
        JPanel equipHeading=xrow();equipHeading.add(lbl("EQUIPMENT",11,GOLD,true),BorderLayout.WEST);
        equipHeading.add(lbl("11 slots",9,MUTED,false),BorderLayout.EAST);append(equip,equipHeading,7);
        // Three combat buttons live alongside Equipment, never disconnected in a separate card.
        JPanel styles=new JPanel(new GridLayout(1,3,4,0));styles.setOpaque(false);
        for(String s:List.of("Melee","Ranged","Magic")){
            FButton button=action(s,53,43,style.equals(s),()->{style=s;selectedEquipSlot=-1;rerenderKeepingScroll();});
            button.setIcon(scaled(styleIcon(s),20,19));
            button.setFont(new Font("SansSerif",Font.BOLD,9));
            button.setVerticalTextPosition(SwingConstants.BOTTOM);
            button.setHorizontalTextPosition(SwingConstants.CENTER);button.setIconTextGap(-1);
            button.setToolTipText(s+(styleRecommended(s)?" · recommended style":" · not a recommended style for this boss, not a player restriction"));
            styles.add(button);
        }
        size(styles,169,43);append(equip,styles,4);
        labelText(equip,styleRecommended(style)?"Recommended approach · not account readiness":"Alternative style · not recommended by this boss fixture",168,9,MUTED);
        append(equip,(JComponent)line(169),6);
        JPanel slots=new JPanel(new GridLayout(5,3,3,3));slots.setOpaque(false);
        String[] slotNames={"","Helm","","Cape","Neck","Ammo","Weapon","Body","Shield","","Legs","","Gloves","Boots","Ring"};
        int realCount=0;
        for(int idx=0;idx<slotNames.length;idx++){
            String slot=slotNames[idx];
            if(slot.isEmpty()){
                JLabel spacer=new JLabel();spacer.setOpaque(false);size(spacer,53,30);slots.add(spacer);continue;
            }
            realCount++;
            final int selectedIdx=idx;
            JButton cell=slotButton(slot,53,30,selectedEquipSlot==idx?GOLD:EDGE,
                slot+" · ownership UNKNOWN · no RuneLite data",()->{selectedEquipSlot=selectedIdx;rerenderKeepingScroll();});
            // Only the items genuinely supplied with W02 may illustrate these slots.
            // These are examples, NOT recognized equipped or owned items.
            String graphic=(slot.equals("Weapon")&&style.equals("Melee"))?"4587-osrs.png":
                (slot.equals("Weapon")&&style.equals("Ranged"))?"861-osrs.png":
                (slot.equals("Shield")&&style.equals("Magic"))?"6889-osrs.png":null;
            if(graphic!=null)cell.setIcon(scaled(graphic,16,16));
            slots.add(cell);
        }
        if(realCount!=11)throw new IllegalStateException("Equipment must have 11 slots");
        size(slots,169,162);slots.setName("osrs-equipment-11");append(equip,slots,6);
        if(selectedEquipSlot>=0){
            labelText(equip,"Selected: "+slotNames[selectedEquipSlot]+" · UNKNOWN ownership",168,9,LIGHT);
        }else labelText(equip,"Icons illustrate style only. Empty means UNKNOWN, not missing.",168,9,MUTED);
        append(equip,(JComponent)line(169),7);
        JPanel inventoryHeading=xrow();inventoryHeading.add(lbl("INVENTORY · 4 × 7",10,GOLD,true),BorderLayout.WEST);
        FButton guide=action(switchGuideVisible?"Hide guide":"Slot guide",69,24,switchGuideVisible,()->{switchGuideVisible=!switchGuideVisible;rerenderKeepingScroll();});
        guide.setFont(new Font("SansSerif",Font.PLAIN,8));inventoryHeading.add(guide,BorderLayout.EAST);
        append(equip,inventoryHeading,6);
        JPanel inv=new JPanel(new GridLayout(7,4,3,3));inv.setOpaque(false);inv.setName("osrs-inventory-4x7");
        for(int index=0;index<28;index++){
            final int slotIndex=index;
            int mark=inventoryMarks[index];
            String label=mark==0?String.format(Locale.ROOT,"%02d",index+1):
                mark==1?"SW":mark==2?"SUP":"PR";
            JButton cell=slotButton(label,39,28,selectedInventorySlot==index?GOLD:markColor(mark),
                 "Position "+(index+1)+" · "+markName(mark)+" · local visual markup only",()->cycleInventoryMark(slotIndex));
            cell.setName("inventory-slot-"+(index+1));inv.add(cell);
        }
        size(inv,169,214);append(equip,inv,7);
        if(switchGuideVisible){
            labelText(equip,"Tap any position: unassigned → SW → SUP → PR → unassigned.",168,9,LIGHT);
            labelText(equip,"SW = switch • SUP = supply • PR = prayer. Standard OSRS 4-column inventory order.",168,9,MUTED);
            append(equip,action("Reset slot markers",169,24,false,this::resetInventoryMarks),4);
        }else labelText(equip,"All 28 positions editable. Tap Slot guide for local switch-lane markers.",168,9,MUTED);
        if(selectedInventorySlot>=0)labelText(equip,"Position "+(selectedInventorySlot+1)+": "+markName(inventoryMarks[selectedInventorySlot])+" (visual demo)",168,9,LIGHT);
        append(equip,wrapped("No supplies are withdrawn, placed or counted. The shell has no bank or inventory access.",168,9,MUTED),0);
        append(p,equip,9);

        Card evidence=card(false);
        JPanel evHeading=xrow();evHeading.add(lbl("TRIP EVIDENCE",11,GOLD,true),BorderLayout.WEST);
        evHeading.add(miniPill("VERIFY",GOLD,59),BorderLayout.EAST);append(evidence,evHeading,8);
        labelText(evidence,"Account: not connected · equipment UNKNOWN",168,9,LIGHT);
        labelText(evidence,"Bank: no scan · blockers not evaluated",168,9,MUTED);
        append(evidence,action(gearEvidenceVisible?"Hide evidence details  ⌃":"Evidence details  ›",169,26,false,()->{gearEvidenceVisible=!gearEvidenceVisible;rerenderKeepingScroll();}),5);
        if(gearEvidenceVisible){
            append(evidence,(JComponent)line(169),6);
            labelText(evidence,"UNKNOWN is not MISSING. No ownership verdict is made without a verified inventory/bank snapshot.",168,9,MUTED);
            labelText(evidence,"Required weapon, ammo and mechanic protection may block a trip when verified; optional upgrades remain advisory.",168,9,MUTED);
            labelText(evidence,"This UI has no candidate ranking or substitute calculation. No READY/blocked claim is made.",168,9,MUTED);
        }
        append(p,evidence,9);

        Card reward=card(false);append(reward,lbl("NOTABLE REWARD · published",10,GOLD,true),5);
        JPanel dropRow=row();dropRow.add(artwork(dropIcon(outfitBoss),19,19));
        dropRow.add(lbl(boss(outfitBoss).drop,9,LIGHT,false));append(reward,dropRow,5);
        JPanel odds=xrow();odds.add(lbl(rewardType(boss(outfitBoss)),9,MUTED,false),BorderLayout.WEST);
        odds.add(lbl(boss(outfitBoss).odds,9,GOLD,true),BorderLayout.EAST);append(reward,odds,5);
        labelText(reward,"Your KC / drops: Unavailable · not inferred from loot.",168,9,MUTED);
        append(p,reward,8);
        append(p,wrapped("SHELL ONLY · No RuneLite connection, saved bank snapshots, account progression or gameplay changes.",INNER-6,9,MUTED),0);
        return p;
    }

    /** Tiny native painted progress bar, no default Windows white outline. */
    private static JComponent progressTrack(int current,int total,int width) {
        JComponent bar=new JComponent(){
            @Override protected void paintComponent(Graphics gr) {
                Graphics2D g=(Graphics2D)gr.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(70,66,58));g.fillRoundRect(0,0,getWidth(),getHeight(),7,7);
                int w=(int)Math.round(Math.max(0,Math.min(1.0,(double)current/Math.max(1,total)))*getWidth());
                if(w>0){g.setColor(GREEN);g.fillRoundRect(0,0,w,getHeight(),7,7);}
                g.dispose();
            }
        };size(bar,width,8);return bar;
    }
    private static JComponent miniPill(String text,Color fg,int w){
        JLabel l=lbl(text,9,fg,true);
        l.setOpaque(true);l.setBackground(new Color(47,47,42));
        l.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(88,79,63),1,true),new EmptyBorder(3,5,3,5)));
        l.setHorizontalAlignment(SwingConstants.CENTER);size(l,w,23);return l;
    }
    private void planToggle(int index) {
        boolean[] flags=pathGoal.equals("Bowfa")?bowfaPlanDone:pathDone;
        flags[index]=!flags[index];
        rerenderKeepingScroll();
    }
    private boolean[] activePlan(){return pathGoal.equals("Bowfa")?bowfaPlanDone:pathDone;}
    private static final String[][] PLAN_LABELS={
        {"Review quest requirements","Check recommended gear","Plan food and supplies"},
        {"Review SOTE requirements","Check gear in Outfitter","Plan CG practice"}
    };
    private void setGoal(String selected) {
        if(selected==null || selected.equals(pathGoal))return;
        pathGoal=selected;pathWhyExpanded=false;render();
    }
    private JPanel planTaskRow(int i,String title,boolean selected){
        JPanel p=new JPanel(new BorderLayout(6,0));p.setOpaque(false);
        JLabel box=lbl(selected?"✓":" ",13,selected?GREEN:GOLD,true);
        box.setHorizontalAlignment(SwingConstants.CENTER);box.setOpaque(true);
        box.setBackground(selected?new Color(40,67,45):new Color(50,49,46));
        box.setBorder(new LineBorder(selected?GREEN:EDGE));size(box,22,22);
        p.add(box,BorderLayout.WEST);
        p.add(wrapped(title,134,10,selected?MUTED:LIGHT),BorderLayout.CENTER);
        p.setBorder(new EmptyBorder(5,3,5,0));
        p.setName("path-plan-step-"+i);
        clickable(p,()->planToggle(i));return p;
    }
    private static JPanel statusLine(String left,String right,Color emphasis){
        JPanel row=xrow();row.add(lbl(left,10,LIGHT,false),BorderLayout.WEST);
        row.add(lbl(right,9,emphasis,true),BorderLayout.EAST);return row;
    }

    private JPanel pathfinder() {
        JPanel p=verticalPage();append(p,section("PATHFINDER"),4);
        append(p,wrapped("PLANNING DEMO  ·  No player progression is connected",183,9,MUTED),7);
        Card goal=card(true);
        JPanel heading=xrow();heading.add(artwork("noodles.png",38,40),BorderLayout.WEST);
        JPanel headingText=column(123);append(headingText,serif("Noodles'",14),0);append(headingText,serif("Next Move",14),0);
        heading.add(headingText,BorderLayout.CENTER);append(goal,heading,7);
        append(goal,lbl("Choose an example goal",10,MUTED,false),4);
        JComboBox<String> chooser=combo(new String[]{"Desert Treasure II","Bowfa"},pathGoal,174);
        chooser.addActionListener(e->setGoal((String)chooser.getSelectedItem()));append(goal,chooser,10);
        JPanel badges=row();badges.add(miniPill("VERIFY",GOLD,71));badges.add(miniPill("NO SYNC",MUTED,87));append(goal,badges,7);
        labelText(goal,"Quest, inventory and travel readiness cannot be verified without real account data.",172,10,LIGHT);
        append(goal,action(pathWhyExpanded?"Hide why  ⌃":"Why VERIFY?  ›",168,26,false,()->{pathWhyExpanded=!pathWhyExpanded;rerenderKeepingScroll();}),3);
        if(pathWhyExpanded) {
            labelText(goal,"Unknown: quest unlocks, inventory supplies, available travel and setup time. Checklist ticks below do not change account readiness.",169,9,MUTED);
        }
        append(p,goal,9);
        Card checklist=card(false);
        append(checklist,lbl("YOUR PLANNING CHECKLIST",11,GOLD,true),3);
        boolean[] flags=activePlan();int cnt=0;for(boolean x:flags)if(x)cnt++;
        append(checklist,lbl("Local demo tasks   "+cnt+" / 3",10,MUTED,false),7);
        append(checklist,progressTrack(cnt,3,167),9);
        String[] tasks=PLAN_LABELS[pathGoal.equals("Bowfa")?1:0];
        for(int i=0;i<tasks.length;i++){
            append(checklist,planTaskRow(i,tasks[i],flags[i]),2);
            if(i<tasks.length-1)append(checklist,(JComponent)line(171),2);
        }
        append(checklist,action("Open Outfitter  ›",169,30,true,()->go("outfitter")),5);
        append(checklist,wrapped("Toggling steps changes only this mockup. It never asserts a quest is completed.",169,8,MUTED),2);
        append(p,checklist,10);
        Card next=card(false);next.striped=true;
        append(next,lbl("WHAT COMES NEXT?",11,GOLD,true),5);
        labelText(next,"When TPT is connected, this space will explain the highest-priority feasible action and show why it was chosen.",170,10,LIGHT);
        append(next,(JComponent)line(169),5);
        append(next,statusLine("Account feasibility","Unknown",GOLD),5);
        append(next,statusLine("Travel/setup cost","Unknown",MUTED),5);
        append(next,statusLine("Resource coverage","Unknown",MUTED),0);
        append(p,next,10);
        append(p,wrapped("No simulated XP, GP, quests, or bank ownership are written by this shell.",186,9,MUTED),0);
        return p;
    }
    private int doneCount(){int v=0;for(boolean b:pathDone)if(b)v++;return v;}
    private JPanel sessions() {
        JPanel p=verticalPage();append(p,section("SESSIONS"),4);
        append(p,wrapped("DEMO SESSIONS  ·  Not connected",183,9,MUTED),8);
        Card current=card(true);
        JPanel h=xrow();h.add(serif("Current Login",12),BorderLayout.WEST);
        h.add(miniPill("NO FEED",MUTED,65),BorderLayout.EAST);append(current,h,8);
        append(current,statusLine("Session XP","Unavailable",GOLD),8);
        append(current,(JComponent)line(170),6);
        append(current,statusLine("Largest skill gain","Unknown",MUTED),7);
        append(current,(JComponent)line(170),6);
        append(current,statusLine("Login duration","Unavailable",MUTED),7);
        append(current,wrapped("No XP events, session timer, or account feed connected.",170,9,MUTED),0);
        append(p,current,9);
        Card summary=card(false);
        append(summary,serif("Bossing Summary",15),7);
        append(summary,statusLine("Verified KC","Unavailable",GOLD),8);
        append(summary,(JComponent)line(171),6);
        append(summary,statusLine("Encounter estimates","Unavailable",MUTED),8);
        append(summary,(JComponent)line(171),6);
        append(summary,statusLine("Rare drops","Unverified",MUTED),8);
        append(summary,(JComponent)line(171),6);
        append(summary,statusLine("Supplies consumed","Unknown",MUTED),6);
        append(summary,wrapped("Loot-confirmed encounters must never count as verified boss KC.",166,9,GOLD),0);
        append(p,summary,9);
        Card history=card(false);history.striped=true;
        append(history,serif("Last 10 Logins",15),3);
        labelText(history,"Completed sessions · newest first",171,10,MUTED);
        append(history,action(sessionsHistoryExpanded?"Hide history  ⌃":"Open history  ›",169,27,false,()->{sessionsHistoryExpanded=!sessionsHistoryExpanded;rerenderKeepingScroll();}),5);
        if(sessionsHistoryExpanded){
            append(history,(JComponent)line(170),7);
            labelText(history,"No completed login records are connected.",170,10,LIGHT);
            append(history,wrapped("No fictional entries are added to fill the list.",169,9,MUTED),0);
        }
        append(p,history,9);
        Card provenance=card(false);
        append(provenance,lbl("DATA SOURCES",11,GOLD,true),5);
        append(provenance,action(sessionsSourcesExpanded?"Hide source details  ⌃":"Show source details  ›",169,27,false,()->{sessionsSourcesExpanded=!sessionsSourcesExpanded;rerenderKeepingScroll();}),4);
        if(sessionsSourcesExpanded){
            append(provenance,statusLine("XP events","Disconnected",MUTED),6);
            append(provenance,statusLine("Boss kill log","Disconnected",MUTED),6);
            append(provenance,statusLine("Loot feed","Disconnected",MUTED),6);
            append(provenance,statusLine("History storage","Shell only",MUTED),4);
            append(provenance,wrapped("These are mock UI source states, not diagnostics from the live plugin.",167,9,MUTED),2);
        }
        append(p,provenance,10);
        append(p,wrapped("Release hold · real account retention and event attribution belong to the main TPT engine, not this visual shell.",185,9,MUTED),0);
        return p;
    }
    
    public void saveScreenshot(Path destination) throws IOException {saveScreenshot(destination,1);}
    public void saveScreenshot(Path destination,int renderScale) throws IOException {
        frame.validate();
        BufferedImage img=new BufferedImage(frame.getWidth()*renderScale,frame.getHeight()*renderScale,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.scale(renderScale,renderScale);frame.printAll(g);g.dispose();
        File file=destination.toFile();File folder=file.getParentFile();if(folder!=null)folder.mkdirs();
        ImageIO.write(img,"png",file);
    }
    public void runScreenshots(Path folder) throws IOException {
        try {
            for(String name:List.of("home","browse","detail","detail-sarachnis","detail-bryophyta","detail-expanded","detail-lower","outfitter","outfitter-magic","outfitter-bank","outfitter-bank-lower","outfitter-guide","outfitter-guide-lower","outfitter-evidence-lower","outfitter-lower","pathfinder","pathfinder-bowfa","pathfinder-checked","sessions","sessions-expanded","home-win","home-tip-done","home-lower","pathfinder-lower","sessions-lower","settings-default","settings-large-azure","settings-large-moss","settings-return-detail","home-large-azure","browse-large-azure","detail-large-azure","outfitter-large-azure","pathfinder-large-azure","sessions-large-azure","home-compact-bronze")) {
                if(name.equals("home-compact-bronze")){fontChoice="Compact";accentChoice="Bronze";applyAppearanceTokens();page="home";}
                else if(name.endsWith("-large-azure")&&!name.startsWith("settings-")){
                    fontChoice="Large";accentChoice="Azure";applyAppearanceTokens();page=name.substring(0,name.length()-12);
                    if(page.equals("detail"))chronicleBoss="scurrius";
                    if(page.equals("outfitter"))outfitBoss="scurrius";
                }
                else if(name.equals("settings-default")){fontChoice="Standard";accentChoice="Bronze";applyAppearanceTokens();settingsSaved=false;settingsReturn="home";page="settings";}
                else if(name.equals("settings-large-azure")){fontChoice="Large";accentChoice="Azure";applyAppearanceTokens();settingsReturn="home";page="settings";}
                else if(name.equals("settings-large-moss")){fontChoice="Large";accentChoice="Moss";applyAppearanceTokens();settingsReturn="home";page="settings";}
                else if(name.equals("settings-return-detail")){fontChoice="Standard";accentChoice="Bronze";applyAppearanceTokens();settingsReturn="detail";chronicleBoss="scurrius";page="settings";}
                else if(name.equals("browse")){fontChoice="Standard";accentChoice="Bronze";applyAppearanceTokens();chronicleReturn="home";page="browse";}
                else if(name.startsWith("detail")) {
                    chronicleBoss=name.equals("detail-sarachnis")?"sarachnis":name.equals("detail-bryophyta")?"bryophyta":"scurrius";
                    caExpanded=name.equals("detail-expanded");
                    dropInfoExpanded=name.equals("detail-expanded");
                    page="detail";
                }
                else if(name.startsWith("outfitter")){
                    page="outfitter";outfitBoss="scurrius";style=name.equals("outfitter-magic")?"Magic":"Melee";
                    source=name.contains("bank")?"Best in Bank":"Wiki Recommended";tier="Recommended";
                    bankMemoryVisible=name.contains("bank");gearEvidenceVisible=name.contains("evidence");
                    switchGuideVisible=name.contains("guide");selectedEquipSlot=-1;selectedInventorySlot=-1;
                    java.util.Arrays.fill(inventoryMarks,(byte)0);
                    if(switchGuideVisible){inventoryMarks[0]=1;inventoryMarks[4]=1;inventoryMarks[8]=2;inventoryMarks[12]=3;selectedInventorySlot=4;}
                }
                else if(name.equals("pathfinder-bowfa")){pathGoal="Bowfa";page="pathfinder";}
                else if(name.equals("pathfinder-checked")){pathGoal="Desert Treasure II";pathDone[0]=true;pathDone[1]=true;page="pathfinder";}
                else if(name.equals("sessions-expanded")){sessionsHistoryExpanded=true;sessionsSourcesExpanded=true;page="sessions";}
                else if(name.equals("home-win")){homeWinExpanded=true;page="home";}
                else if(name.equals("home-tip-done")){tipDismissed=true;tipAction="Done";homeWinExpanded=false;page="home";}
                else if(name.equals("home-lower")){tipDismissed=false;tipAction="";homeWinExpanded=false;page="home";}
                else if(name.equals("pathfinder-lower"))page="pathfinder";
                else if(name.equals("sessions-lower"))page="sessions";
                else page=name;
                render();
                window.validate();window.repaint();
                // No screenshots of a scroll position from the previous page.
                scroll.getVerticalScrollBar().setValue(name.endsWith("-lower")?scroll.getVerticalScrollBar().getMaximum():0);
                saveScreenshot(folder.resolve(name+"-225px.png"));
                saveScreenshot(folder.resolve(name+"-3x-phone.png"),3);
            }
        } finally {loadAppearance();page="home";render();}
    }
    private static <T extends Component> T locate(Component c, Class<T> type, String buttonText) {
        if(type.isInstance(c)) {
            T found=type.cast(c);
            if(buttonText==null || (found instanceof JButton && ((JButton)found).getText().contains(buttonText)) ||
                (found instanceof JLabel && ((JLabel)found).getText()!=null && ((JLabel)found).getText().contains(buttonText)))return found;
        }
        if(c instanceof Container)for(Component child:((Container)c).getComponents()){
            T found=locate(child,type,buttonText);if(found!=null)return found;
        }
        return null;
    }
    private static FButton findButtonNamed(Component c,String name){
        if(c instanceof FButton && name.equals(c.getName()))return (FButton)c;
        if(c instanceof Container)for(Component child:((Container)c).getComponents()){
            FButton found=findButtonNamed(child,name);if(found!=null)return found;
        }
        return null;
    }
    private static JPanel findNamed(Component c,String name){
        if(c instanceof JPanel && name.equals(c.getName()))return (JPanel)c;
        if(c instanceof Container)for(Component child:((Container)c).getComponents()){
            JPanel found=findNamed(child,name);if(found!=null)return found;
        }
        return null;
    }
    private void selfTest() {
        if(SIDEBAR_WIDTH!=225||BOSSES.size()!=3)throw new AssertionError("Width / fixture count");
        for(String art:List.of("scurrius-portrait.png","sarachnis-portrait.png","bryophyta-portrait.png",
                "28798-osrs.png","23528-osrs.png","22372-osrs.png",
                "4587-osrs.png","861-osrs.png","6889-osrs.png")) {
            if(scaled(art,19,19)==null)throw new AssertionError("Authentic image asset missing: "+art);
        }
        if(scaled("approved-armored-raccoon.png",42,42)==null)throw new AssertionError("Approved armored mascot is missing");
        if(TrashPandaShell.class.getResource("/assets/feral-hero.png")!=null)throw new AssertionError("Rejected fat mascot must not be bundled");
        if(bookGlyph().getIcon()==null)throw new AssertionError("Hand-drawn Chronicle book icon missing");
        // R10: gallery and Home must remain functional at 225px and keep all three filters.
        if(!page.equals("home"))page="home";
        render();
        if(locate(content,FButton.class,"Review achievement")==null)throw new AssertionError("Home achievement action lost");
        page="browse";render();
        if(locate(content,JComboBox.class,null)==null)throw new AssertionError("Compact Chronicle filters missing");
        FButton compare=locate(content,FButton.class,"Compare");
        if(compare==null)throw new AssertionError("R10C reference comparison missing");
        compare.doClick();
        if(!referencePreviewMode||scaled("scurrius-design-reference.png",30,35)==null)
            throw new AssertionError("R10C design reference view not usable");
        FButton backToSprite=locate(content,FButton.class,"Sprites");
        if(backToSprite==null)throw new AssertionError("R10C design comparison cannot be reversed");
        backToSprite.doClick();
        if(referencePreviewMode)throw new AssertionError("R10C owner preview must not leak into genuine sprite mode");
        page="home";render();
        if(MODEL_BOSSES.size()!=3)throw new AssertionError("Model-art contract changed");
        // QA exercises real checksum/approval logic using an ephemeral synthetic test square.
        // Synthetic art is NEVER shown as a boss or included with the release ZIP.
        try {
            Path scratch=Files.createTempDirectory("tpt-art-gate-");
            try {
                BufferedImage fixture=new BufferedImage(160,160,BufferedImage.TYPE_INT_ARGB);
                Graphics2D q=fixture.createGraphics();q.setColor(Color.BLUE);q.fillOval(30,20,100,100);q.dispose();
                Path art=scratch.resolve("scurrius-model.png");ImageIO.write(fixture,"PNG",art.toFile());
                if(resolveModelArt("scurrius",scratch).image!=null)
                    throw new AssertionError("Model file without source approval was accepted");
                String digest=java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(art)));
                Properties meta=new Properties();
                meta.setProperty("scurrius.owner_approved","false");
                meta.setProperty("scurrius.sha256",digest);
                meta.setProperty("scurrius.source_url","https://example.org/authorized-candidate.png");
                meta.setProperty("scurrius.rights_note","Synthetic QA fixture only");
                try(OutputStream out=Files.newOutputStream(scratch.resolve(MODEL_MANIFEST))){meta.store(out,"QA");}
                if(resolveModelArt("scurrius",scratch).image!=null)
                    throw new AssertionError("Unapproved model was accepted");
                meta.setProperty("scurrius.owner_approved","true");
                meta.setProperty("scurrius.sha256","0".repeat(64));
                try(OutputStream out=Files.newOutputStream(scratch.resolve(MODEL_MANIFEST))){meta.store(out,"QA");}
                if(resolveModelArt("scurrius",scratch).image!=null)
                    throw new AssertionError("Tampered model was accepted");
                meta.setProperty("scurrius.sha256",digest);
                try(OutputStream out=Files.newOutputStream(scratch.resolve(MODEL_MANIFEST))){meta.store(out,"QA");}
                if(resolveModelArt("scurrius",scratch).image==null)
                    throw new AssertionError("Explicit approved checksum image did not load");
                meta.setProperty("scurrius.source_url","local-capture: RuneLite model export with owner record");
                try(OutputStream out=Files.newOutputStream(scratch.resolve(MODEL_MANIFEST))){meta.store(out,"QA");}
                if(resolveModelArt("scurrius",scratch).image==null)
                    throw new AssertionError("Local source provenance failed after owner signoff");
            }finally {
                Files.deleteIfExists(scratch.resolve("scurrius-model.png"));
                Files.deleteIfExists(scratch.resolve(MODEL_MANIFEST));
                Files.deleteIfExists(scratch);
            }
        }catch(Exception ex){throw new AssertionError("Strict model-art QA failed",ex);}
        if(!ordering.equals("Top"))throw new AssertionError("R10B default Top order must match owner design featured bosses");
        if(!BOSSES.get(0).id.equals("scurrius"))throw new AssertionError("R10B featured boss must begin with Scurrius");
        if(!boss("scurrius").name.equals("Scurrius"))throw new AssertionError("Boss fixture");
        navButtons.get("home").doClick();
        FButton homeChronicle=locate(content,FButton.class,"Boss Chronicle");
        if(homeChronicle==null)throw new AssertionError("Missing Home Chronicle button");
        homeChronicle.doClick();
        if(!page.equals("browse"))throw new AssertionError("Home Chronicle navigation");
        JTextField searchField=locate(content,JTextField.class,null);
        if(searchField==null)throw new AssertionError("Boss search missing");
        searchField.setText("Sarachnis");
        if(!search.equals("Sarachnis"))throw new AssertionError("Search input not wired");
        Card result=locate(content,Card.class,null);
        if(result==null)throw new AssertionError("Search results missing");
        if(!result.isFocusable()||result.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0))==null||
           result.getActionMap().get("open-boss")==null)
            throw new AssertionError("R10B Chronicle cards must be keyboard-operable");
        MouseEvent event=new MouseEvent(result,MouseEvent.MOUSE_CLICKED,System.currentTimeMillis(),0,10,10,1,false);
        for(MouseListener listener:result.getMouseListeners())listener.mouseClicked(event);
        if(!page.equals("detail")||!chronicleBoss.equals("sarachnis"))throw new AssertionError("Boss card not tappable");
        FButton back=locate(content,FButton.class,"BOSSES");
        if(back==null)throw new AssertionError("Missing back button");
        back.doClick();if(!page.equals("browse")||!search.equals("Sarachnis"))throw new AssertionError("Back lost search state");
        chronicleReturn="outfitter";outfitBoss="scurrius";openBoss("bryophyta");
        FButton prev=locate(content,FButton.class,"‹");
        if(prev==null)throw new AssertionError("Chronicle previous boss navigation missing");
        if(caTasks("scurrius").length!=5||caTasks("sarachnis").length!=5||caTasks("bryophyta").length!=7)
            throw new AssertionError("Wiki CA task fixtures invalid");
        if(!rewardType(boss("bryophyta")).equals("Chest reward"))
            throw new AssertionError("Bryophyta chest provenance must be explicit");
        FButton caToggle=locate(content,FButton.class,"View tasks");
        if(caToggle==null)throw new AssertionError("CA task toggle missing");
        caToggle.doClick();
        if(!caExpanded||locate(content,JLabel.class,"Bryophyta Novice")==null)
            throw new AssertionError("CA expanded task list not shown");
        if(locate(content,FButton.class,"Choose boss")==null)throw new AssertionError("Chronicle picker missing");
        if(!chronicleBoss.equals("bryophyta")||!outfitBoss.equals("scurrius"))throw new AssertionError("Boss selections crossed");
        rotateChronicle(1);
        if(!chronicleBoss.equals("sarachnis")||!outfitBoss.equals("scurrius"))
            throw new AssertionError("Next boss must move A-Z without overwriting Outfitter");
        rotateChronicle(-1);
        if(!chronicleBoss.equals("bryophyta")||!outfitBoss.equals("scurrius"))
            throw new AssertionError("Previous boss must restore Chronicle boss only");
        FButton outfitLink=locate(content,FButton.class,"Open Bryophyta in Outfitter");
        if(outfitLink==null)throw new AssertionError("Missing Outfitter link");
        outfitLink.doClick();
        if(!page.equals("outfitter")||!outfitBoss.equals("bryophyta"))throw new AssertionError("Outfitter link not working");
        FButton rangedBtn=locate(content,FButton.class,"Ranged");
        if(rangedBtn==null||rangedBtn.getIcon()==null)throw new AssertionError("Ranged OSRS sprite not rendered");
        rangedBtn.doClick();if(!style.equals("Ranged"))throw new AssertionError("Ranged style button not wired");
        FButton magicBtn=locate(content,FButton.class,"Magic");
        if(magicBtn==null||magicBtn.getIcon()==null)throw new AssertionError("Magic OSRS sprite not rendered");
        magicBtn.doClick();if(!style.equals("Magic"))throw new AssertionError("Magic style button not wired");
        FButton bankBtn=locate(content,FButton.class,"Best in Bank");
        if(bankBtn==null)throw new AssertionError("Gear source button missing");
        bankBtn.doClick();if(!source.equals("Best in Bank"))throw new AssertionError("Gear source button not wired");
        navButtons.get("sessions").doClick();if(!page.equals("sessions"))throw new AssertionError("Sessions tab not clickable");
        navButtons.get("pathfinder").doClick();if(!page.equals("pathfinder"))throw new AssertionError("Pathfinder tab not clickable");
        navButtons.get("home").doClick();

        // W04 controls must be actually wired, not painted placeholders.
        navButtons.get("home").doClick();
        FButton win=locate(content,FButton.class,"Review achievement");
        if(win==null)throw new AssertionError("Home biggest-win disclosure missing");
        win.doClick();if(!homeWinExpanded)throw new AssertionError("Home biggest win disclosure inactive");
        FButton done=locate(content,FButton.class,"Done");
        if(done==null)throw new AssertionError("Spoons tip missing");
        done.doClick();if(!tipDismissed||!tipAction.equals("Done"))throw new AssertionError("Spoons Done did not persist");
        FButton reset=locate(content,FButton.class,"Show tip again");
        if(reset==null)throw new AssertionError("Spoons reset missing");
        reset.doClick();if(tipDismissed)throw new AssertionError("Spoons tip reset failed");
        navButtons.get("pathfinder").doClick();
        FButton why=locate(content,FButton.class,"Why VERIFY");
        if(why==null)throw new AssertionError("Pathfinder verify explanation missing");
        why.doClick();if(!pathWhyExpanded)throw new AssertionError("Pathfinder verify explanation inactive");
        JComboBox<?> goalSelect=locate(content,JComboBox.class,null);
        if(goalSelect==null)throw new AssertionError("Pathfinder goal selector not mounted");
        goalSelect.setSelectedItem("Bowfa");
        if(!pathGoal.equals("Bowfa"))throw new AssertionError("Pathfinder goal selector not wired");
        setGoal("Desert Treasure II");
        JPanel taskRow=findNamed(content,"path-plan-step-0");
        if(taskRow==null)throw new AssertionError("Interactive planning task row missing");
        MouseEvent taskClick=new MouseEvent(taskRow,MouseEvent.MOUSE_CLICKED,System.currentTimeMillis(),0,8,8,1,false);
        for(MouseListener listener:taskRow.getMouseListeners())listener.mouseClicked(taskClick);
        if(!pathDone[0])throw new AssertionError("Pathfinder task click not wired");
        setGoal("Bowfa");if(!pathGoal.equals("Bowfa")||bowfaPlanDone[0])throw new AssertionError("Goal-independent checklist broken");
        planToggle(1);if(!bowfaPlanDone[1]||!pathDone[0])throw new AssertionError("Goal-specific checklist did not persist");
        navButtons.get("sessions").doClick();
        FButton history=locate(content,FButton.class,"Open history");
        if(history==null)throw new AssertionError("Sessions history expander missing");
        history.doClick();if(!sessionsHistoryExpanded)throw new AssertionError("Sessions history not expanded");
        FButton srcDetails=locate(content,FButton.class,"Show source details");
        if(srcDetails==null)throw new AssertionError("Sessions provenance toggle missing");
        srcDetails.doClick();if(!sessionsSourcesExpanded)throw new AssertionError("Sessions provenance not expanded");
        if(scaled("approved-armored-raccoon.png",42,42)==null)throw new AssertionError("Approved raccoon lost");
        if(TrashPandaShell.class.getResource("/assets/feral-hero.png")!=null)throw new AssertionError("Rejected image resurfaced");

        // W05: actual control interactions, OSRS shape invariants, and strict no-bank-data semantics.
        navButtons.get("outfitter").doClick();
        JPanel equipSlots=findNamed(content,"osrs-equipment-11");
        if(equipSlots==null||equipSlots.getComponentCount()!=15)throw new AssertionError("W05 11-slot OSRS silhouette missing");
        JPanel invSlots=findNamed(content,"osrs-inventory-4x7");
        if(invSlots==null||invSlots.getComponentCount()!=28||!(invSlots.getLayout() instanceof GridLayout))
            throw new AssertionError("W05 OSRS 4x7 inventory grid missing");
        GridLayout invGrid=(GridLayout)invSlots.getLayout();
        if(invGrid.getRows()!=7||invGrid.getColumns()!=4)throw new AssertionError("W05 inventory positions transposed");
        FButton optimal=locate(content,FButton.class,"Optimal");
        if(optimal==null)throw new AssertionError("W05 tier buttons missing");
        optimal.doClick();if(!tier.equals("Optimal"))throw new AssertionError("W05 tier state not wired");
        FButton bankView=locate(content,FButton.class,"Bank Memory");
        if(bankView==null)throw new AssertionError("W05 Bank Memory preview missing");
        bankView.doClick();if(!bankMemoryVisible)throw new AssertionError("W05 Bank Memory expand broken");
        FButton evidence=locate(content,FButton.class,"Evidence details");
        if(evidence==null)throw new AssertionError("W05 evidence button missing");
        evidence.doClick();if(!gearEvidenceVisible)throw new AssertionError("W05 evidence expansion broken");
        FButton guide=locate(content,FButton.class,"Slot guide");
        if(guide==null)throw new AssertionError("W05 slot guide toggle missing");
        guide.doClick();if(!switchGuideVisible)throw new AssertionError("W05 switch guide inactive");
        JButton first=locate(content,JButton.class,"01");
        if(first==null)throw new AssertionError("W05 first inventory cell missing");
        first.doClick();if(inventoryMarks[0]!=1)throw new AssertionError("W05 slot marking broken");
        cycleInventoryMark(0);if(inventoryMarks[0]!=2)throw new AssertionError("W05 supply mark broken");
        cycleInventoryMark(0);if(inventoryMarks[0]!=3)throw new AssertionError("W05 prayer mark broken");
        cycleInventoryMark(0);if(inventoryMarks[0]!=0)throw new AssertionError("W05 reset cycle broken");
        inventoryMarks[7]=1;resetInventoryMarks();if(inventoryMarks[7]!=0)throw new AssertionError("W05 reset marks broken");
        if(TrashPandaShell.class.getResource("/assets/feral-hero.png")!=null)throw new AssertionError("Rejected mascot resurfaced");
        // W06: appearance is a subview, not a fifth tab, and is saved outside OSRS folders.
        if(navButtons.size()!=4)throw new AssertionError("Appearance created a fifth top tab");
        outfitBoss="scurrius";openBoss("sarachnis");int bossY=scroll.getVerticalScrollBar().getValue();
        FButton gear=findButtonNamed(frame,"appearance-gear");
        if(gear==null||gear.getIcon()==null)throw new AssertionError("Header appearance control missing");
        gear.doClick();if(!page.equals("settings")||!settingsReturn.equals("detail"))throw new AssertionError("Appearance entry failed");
        if(locate(content,FButton.class,"Large")==null)throw new AssertionError("Large font option absent");
        FButton large=locate(content,FButton.class,"Large");large.doClick();
        if(!fontChoice.equals("Large")||FONT_OFFSET!=1)throw new AssertionError("Large font did not apply");
        FButton azure=locate(content,FButton.class,"Azure");azure.doClick();
        if(!accentChoice.equals("Azure")||!GOLD.equals(accentColor("Azure")))throw new AssertionError("Azure theme did not apply");
        Appearance saved=readAppearance(settingsPath);
        if(!saved.font.equals("Large")||!saved.accent.equals("Azure"))throw new AssertionError("Appearance was not persisted");
        FButton backSettings=locate(content,FButton.class,"‹ Back");backSettings.doClick();
        if(!page.equals("detail")||!chronicleBoss.equals("sarachnis")||!outfitBoss.equals("scurrius"))
            throw new AssertionError("Settings did not preserve independent boss selections");
        openSettings();FButton resetTheme=locate(content,FButton.class,"Restore defaults");
        if(resetTheme==null)throw new AssertionError("Appearance reset missing");
        resetTheme.doClick();
        if(!fontChoice.equals("Standard")||!accentChoice.equals("Bronze"))throw new AssertionError("Reset appearance failed");
        Appearance resetSaved=readAppearance(settingsPath);
        if(!resetSaved.font.equals("Standard")||!resetSaved.accent.equals("Bronze"))throw new AssertionError("Reset not persisted");
        if(TrashPandaShell.class.getResource("/assets/feral-hero.png")!=null)throw new AssertionError("Rejected mascot resurfaced");
        System.out.println("PASS W08C: W01-W06 regression, strict boss art manifest/checksum, approved model rendering, safe fallback, independent bosses, rejected mascot absent");
    }
    public static void main(String[] args) throws Exception {
        String mode=args.length>0?args[0]:"";
        if(mode.equals("--audit-art")){
            for(String boss:new String[]{"scurrius","sarachnis","bryophyta"}){
                ModelAsset m=approvedModel(boss);
                System.out.println(boss+": "+(m.image==null?"FALLBACK ("+m.status+")":"APPROVED SLOT ("+m.status+")"));
            }
            return;
        }
        System.setProperty("sun.java2d.uiScale","1");
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        // QA must not overwrite appearance choices made in an interactive shell.
        if(mode.equals("--self-test") && System.getProperty("tpt.shell.settings.file")==null){
            System.setProperty("tpt.shell.settings.file",Path.of(System.getProperty("java.io.tmpdir"),
                "tpt-shell-qa-"+java.util.UUID.randomUUID()+".properties").toString());
        }
        final Exception[] problem={null};
        SwingUtilities.invokeAndWait(()->{
            try {
                TrashPandaShell app=new TrashPandaShell();
                if(mode.equals("--self-test")){app.selfTest();app.window.dispose();}
                else if(mode.equals("--screenshots")||mode.equals("--screenshots-reference")){
                    if(mode.equals("--screenshots-reference"))app.referencePreviewMode=true;
                    app.show();app.runScreenshots(Path.of(args.length>1?args[1]:"previews"));app.window.dispose();
                    System.out.println("Saved actual Java Swing screenshots at 225px plus 3x phone previews."+
                        (mode.equals("--screenshots-reference")?" These show DESIGN-REFERENCE screenshot crops, not approved NPC assets.":""));
                } else app.show();
            }catch(Exception e){problem[0]=e;}
        });
        if(problem[0]!=null)throw problem[0];
    }
}
