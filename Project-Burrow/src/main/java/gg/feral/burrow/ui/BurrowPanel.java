package gg.feral.burrow.ui;

import gg.feral.burrow.core.*;
import gg.feral.burrow.core.Outfitter.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/** 225px native-scale, single-pass Swing painter. No nested BoxLayout or reentrant UI mutation. */
public final class BurrowPanel extends JPanel {
    public static final int WIDTH=225, HEIGHT=730;
    private static final Color BG=new Color(12,17,20), PANEL=new Color(25,32,35);
    private static final Color LINE=new Color(61,68,69), GOLD=new Color(232,179,99);
    private static final Color MINT=new Color(91,214,176), TEXT=new Color(230,229,218);
    private static final Color MUTED=new Color(151,165,165), BAD=new Color(219,134,116);
    private final BurrowModel model;
    public BurrowPanel(BurrowModel m) {
        this.model=Objects.requireNonNull(m); setPreferredSize(new Dimension(WIDTH,HEIGHT)); setMinimumSize(getPreferredSize());
        setOpaque(true); setBackground(BG);
        addMouseListener(new MouseAdapter(){ public void mouseReleased(MouseEvent e) {
            clickAt(e.getX(), e.getY());
        }});
    }
    public BurrowModel model() { return model; }
    /** Matches the pixel bounds of the controls painted below; no invisible full-width buttons. */
    private static boolean inside(int x, int y, int bx, int by, int bw, int bh) {
        return x >= bx && x < bx + bw && y >= by && y < by + bh;
    }

    /** Testable mouse routing with strict rectangular hitboxes and no hidden actions. */
    public void clickAt(int x, int y) {
        if (!inside(x, y, 0, 0, WIDTH, HEIGHT)) return;
        for (int i = 0; i < 4; i++) {
            if (inside(x, y, i * 56 + 2, 72, 52, 40)) {
                model.show(BurrowModel.View.values()[i]);
                repaint();
                return;
            }
        }
        switch (model.view()) {
            case OUTFITTER -> {
                if (inside(x, y, 13, 199, 199, 26)) model.setMode("Recommended");
                else if (inside(x, y, 13, 230, 199, 26)) model.setMode("Best in Bank");
                else if (inside(x, y, 13, 576, 199, 26)) model.loadDemoBank();
                else if (inside(x, y, 13, 611, 199, 26)) model.clearBank();
            }
            case SESSIONS -> {
                if (inside(x, y, 13, 236, 199, 26)) model.recordDemoKill();
                else if (inside(x, y, 13, 280, 199, 26)) model.resetDemo();
            }
            case PATHFINDER -> {
                if (inside(x, y, 17, 211, 190, 25)) model.chooseBoss("Vorkath");
                else if (inside(x, y, 17, 240, 190, 25)) model.chooseBoss("Scurrius");
                else if (inside(x, y, 17, 269, 190, 25)) model.chooseBoss("Barrows");
            }
            case HOME -> { /* No click actions on the Home preview. */ }
        }
        repaint();
    }
    @Override public void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g=(Graphics2D)g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(BG);g.fillRect(0,0,WIDTH,HEIGHT);
            header(g); navigation(g);
            switch(model.view()) {
                case HOME -> home(g);
                case PATHFINDER -> pathfinder(g);
                case OUTFITTER -> outfitter(g);
                case SESSIONS -> sessions(g);
            }
            g.setColor(LINE);g.drawLine(10,695,215,695);
            small(g,"FERAL LABS  /  BURROW 0.1",13,711,MUTED);
            small(g,"EXPERIMENTAL  •  NOT LIVE",13,724,GOLD);
        } finally { g.dispose(); }
    }
    private void header(Graphics2D g) {
        g.setColor(PANEL);g.fillRect(0,0,WIDTH,64);
        Raccoon.draw(g,8,11,2);
        label(g,"BURROW",67,30,17,GOLD,true);
        small(g,"by the Feral crew",68,45,MUTED);
        g.setColor(LINE);g.drawLine(0,63,WIDTH,63);
    }
    private void navigation(Graphics2D g) {
        String[] tabs={"HOME","PATH","GEAR","LOG"};
        for(int i=0;i<4;i++) {
            int x=i*56;boolean selected=model.view().ordinal()==i;
            g.setColor(selected ? new Color(67,54,38) : PANEL);g.fillRoundRect(x+2,72,52,40,7,7);
            g.setColor(selected ? GOLD : MUTED);g.drawRoundRect(x+2,72,52,40,7,7);
            small(g,tabs[i],x+7,96,selected ? GOLD : TEXT);
        }
    }
    private void title(Graphics2D g, String t, String s) {label(g,t,11,147,16,TEXT,true);small(g,s,11,165,MUTED);}
    private void card(Graphics2D g,int y,int h) {
        g.setColor(PANEL);g.fillRoundRect(9,y,207,h,9,9);
        g.setColor(LINE);g.drawRoundRect(9,y,207,h,9,9);
    }
    private void button(Graphics2D g,String text,int y,boolean active) {
        g.setColor(active?new Color(77,57,34):new Color(35,43,46));g.fillRoundRect(13,y,199,26,6,6);
        g.setColor(active?GOLD:LINE);g.drawRoundRect(13,y,199,26,6,6);
        small(g,text,24,y+17,active?GOLD:TEXT);
    }
    private void home(Graphics2D g) {
        title(g,"THE DEN","Your journey. Your next move.");
        card(g,181,108);small(g,"PROGRESSION BOARD",20,199,GOLD);
        label(g,"Ready for the next boss?",19,222,12,TEXT,true);
        small(g,"Pathfinder builds a route.",20,245,MUTED);
        small(g,"Outfitter prepares the kit.",20,263,MUTED);
        card(g,303,124);small(g,"FIELD NOTES",20,321,GOLD);
        small(g,"• One immutable loadout plan",20,343,TEXT);
        small(g,"• Bank scan stays manual",20,363,TEXT);
        small(g,"• Unknown gear stays unknown",20,383,TEXT);
        small(g,"• No automatic withdrawal",20,403,TEXT);
        card(g,441,130);small(g,"FERAL CREW MANIFESTO",20,460,GOLD);
        small(g,"Build it. Break it. Fix it.",20,482,TEXT);
        small(g,"Ship only what survived QA.",20,503,TEXT);
        small(g,"Current state: preview only",20,548,MUTED);
    }
    private void pathfinder(Graphics2D g) {
        title(g,"PATHFINDER","The boss board, rebuilt.");
        card(g,177,130);small(g,"PICK A TARGET",20,195,GOLD);
        String[] names={"Vorkath","Scurrius","Barrows"};
        for(int i=0;i<3;i++) {
            int yy=211+i*29; boolean chosen=names[i].equals(model.boss());
            g.setColor(chosen?new Color(90,68,40):new Color(38,46,47));
            g.fillRoundRect(17,yy,190,25,5,5);
            small(g,names[i],27,yy+17,chosen?GOLD:TEXT);
            small(g,chosen?"SELECTED":"CHOOSE",146,yy+17,chosen?GOLD:MUTED);
        }
        card(g,316,166);small(g,"RARE-DROP MATH LAB",20,334,GOLD);
        label(g,model.boss(),20,360,15,TEXT,true);
        small(g,"Illustration: 1 in 50 per kill",20,389,MUTED);
        String p=String.format(Locale.ROOT,"%.1f%%",100*BurrowModel.cumulativeChance(1.0/50,20));
        label(g,p,20,432,30,MINT,true);
        small(g,"At least one by 20 kills*",20,454,MUTED);
        small(g,"* Independent rolls. Demo odds.",20,472,MUTED);
        card(g,497,91);small(g,"NEXT IDEA",20,517,GOLD);
        small(g,"Track prerequisites and route",20,544,TEXT);
        small(g,"upgrades before the boss.",20,563,TEXT);
    }
    private void outfitter(Graphics2D g) {
        Plan plan=model.plan();
        title(g,"OUTFITTER","Scurrius • sample melee loadout");
        card(g,178,84);small(g,"RECOMMENDATION MODE",20,193,GOLD);
        button(g,"Recommended  [reference]",199,model.mode().equals("Recommended"));
        button(g,"Best in Bank  [demo]",230,model.mode().equals("Best in Bank"));
        card(g,273,290);
        small(g,"NATIVE GRID  /  8 x 7",20,292,GOLD);
        small(g,"11 gear   •   28 inventory   •   17 spare",20,311,MUTED);
        int ox=24,oy=326,dx=22,dy=24;
        for(Cell c : plan.cells()) {
            int col=c.position()%8, row=c.position()/8;
            Color fill=switch(c.stock()) {
                case SPACE -> c.kind()==Kind.INVENTORY ? new Color(35,45,47) : new Color(24,28,31);
                case UNVERIFIED -> new Color(91,80,59);
                case OWNED -> new Color(45,96,78);
                case MISSING -> new Color(107,59,56);
            };
            g.setColor(fill);g.fillRoundRect(ox+col*dx,oy+row*dy,19,20,3,3);
            g.setColor(LINE);g.drawRoundRect(ox+col*dx,oy+row*dy,19,20,3,3);
            if(c.kind()!=Kind.SPACE) small(g,c.kind()==Kind.GEAR?"G":"I",ox+col*dx+6,oy+row*dy+14,TEXT);
        }
        small(g,plan.bankVerified()?"DEMO STOCK • NOT A LIVE SCAN":"UNVERIFIED STOCK • NO SCAN",20,519,
                plan.bankVerified()?MINT:BAD);
        small(g,"Missing: " + plan.missing().size() + "   Unknown gear: " +
            plan.gear().stream().filter(c -> c.requirement()==null).count(),20,540,TEXT);
        button(g,"LOAD DEMO BANK (manual)",576,false);
        button(g,"CLEAR BANK SNAPSHOT",611,false);
        small(g,"Same plan ID: #" + plan.planId(),15,655,MUTED);
        small(g,"No highlights / bank actions",15,675,MUTED);
    }
    private void sessions(Graphics2D g) {
        title(g,"SESSIONS","Every boss has a story.");
        card(g,178,148);
        small(g,"SANDBOX KILL COUNTER",20,200,GOLD);
        label(g,Integer.toString(model.demoKills()),20,230,30,MINT,true);
        button(g,"+ LOG DEMO KILL",236,false);
        button(g,"RESET DEMO",280,false);
        card(g,341,196);small(g,"FIELD JOURNAL",20,361,GOLD);
        int yy=386;
        if(model.journal().isEmpty()) small(g,"No recorded demo events.",20,387,MUTED);
        for(String entry:model.journal()) { small(g,entry,17,yy,TEXT); yy+=24; }
        card(g,553,74);
        small(g,"LIVE INTEGRATION GATE",20,573,GOLD);
        small(g,"No genuine RuneLite events",20,598,MUTED);
        small(g,"have been collected yet.",20,613,MUTED);
    }
    private void label(Graphics2D g,String s,int x,int y,int size,Color color,boolean bold) {
        g.setColor(color);g.setFont(new Font("Dialog",bold?Font.BOLD:Font.PLAIN,size));g.drawString(s,x,y);
    }
    private void small(Graphics2D g,String s,int x,int y,Color c) {label(g,s,x,y,10,c,false);}
}
