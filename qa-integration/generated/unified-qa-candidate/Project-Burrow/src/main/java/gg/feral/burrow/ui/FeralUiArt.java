package gg.feral.burrow.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Map;
import javax.imageio.ImageIO;

/** Visual assets cropped from the owner's UI references; never a source of boss or account facts.
 * Other bosses intentionally receive a neutral category crest, not fabricated creature artwork. */
final class FeralUiArt {
    private static final String PATH="/gg/feral/burrow/ui/";
    private static BufferedImage load(String name){
        try(InputStream in=FeralUiArt.class.getResourceAsStream(PATH+name)){
            return in==null?null:ImageIO.read(in);
        }catch(Exception e){return null;}
    }
    private static final BufferedImage[] CREW={load("crew-spoons.png"),load("crew-bandit.png"),load("crew-noodles.png")};
    private static final Map<String,BufferedImage> BOSSES=bosses();
    private static Map<String,BufferedImage> bosses(){
        var art=new java.util.HashMap<String,BufferedImage>();
        for(String boss:new String[]{"Scurrius","Sarachnis","Bryophyta"}){
            BufferedImage image=load("chronicle-"+boss.toLowerCase(java.util.Locale.ROOT)+".png");
            if(image!=null)art.put(boss,image);
        }
        return Map.copyOf(art);
    }
    static void crew(Graphics2D g,int index,int x,int y,int w,int h){
        BufferedImage img=index>=0&&index<CREW.length?CREW[index]:null;
        draw(g,img,x,y,w,h);
    }
    static void boss(Graphics2D g,String name,int x,int y,int w,int h,Color accent){
        BufferedImage img=BOSSES.get(name);
        if(img!=null){draw(g,img,x,y,w,h);return;}
        g.setColor(new Color(35,43,43));g.fillRoundRect(x,y,w,h,8,8);
        g.setColor(accent);g.drawRoundRect(x,y,w-1,h-1,8,8);
        // Abstract boss crest; deliberately not an impersonation of a missing OSRS model.
        g.setStroke(new BasicStroke(2f));
        int cx=x+w/2,cy=y+h/2;
        g.drawOval(cx-13,cy-13,26,26);
        g.drawLine(cx-18,cy,cx-8,cy);g.drawLine(cx+8,cy,cx+18,cy);
        g.drawLine(cx,cy-18,cx,cy-8);g.drawLine(cx,cy+8,cx,cy+18);
        g.setStroke(new BasicStroke(1f));
    }
    static void activityGlyph(Graphics2D g,String label,int x,int y){
        String l=label.toLowerCase(java.util.Locale.ROOT);
        boolean farm=l.contains("seed")||l.contains("sapling")||l.contains("compost")||l.contains("herb")||l.contains("rake");
        boolean clue=l.contains("clue")||l.contains("emote")||l.contains("stash")||l.contains("scroll");
        boolean travel=l.contains("tele")||l.contains("ecto")||l.contains("ring")||l.contains("necklace")||l.contains("cloak");
        Color c=farm?new Color(129,193,137):clue?new Color(217,184,112):travel?new Color(136,172,214):new Color(200,179,146);
        g.setColor(c);g.setStroke(new BasicStroke(2f));
        if(farm){g.drawLine(x+7,y+14,x+7,y+4);g.drawArc(x+1,y+1,11,8,0,170);g.drawArc(x+5,y+4,11,8,0,-170);}
        else if(clue){g.drawRect(x+3,y+2,10,13);g.drawLine(x+5,y+6,x+11,y+6);g.drawLine(x+5,y+9,x+11,y+9);}
        else if(travel){g.drawOval(x+2,y+2,12,12);g.drawLine(x+8,y+4,x+8,y+12);g.drawLine(x+4,y+8,x+12,y+8);}
        else{g.drawRoundRect(x+3,y+3,10,11,3,3);g.drawLine(x+5,y+6,x+11,y+6);}
        g.setStroke(new BasicStroke(1f));
    }
    private static void draw(Graphics2D g,BufferedImage img,int x,int y,int w,int h){
        if(img==null)return;
        Object old=g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double ratio=Math.min((double)w/img.getWidth(),(double)h/img.getHeight());
        int iw=(int)Math.round(ratio*img.getWidth()),ih=(int)Math.round(ratio*img.getHeight());
        g.drawImage(img,x+(w-iw)/2,y+(h-ih)/2,iw,ih,null);
        if(old!=null)g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,old);
    }
    private FeralUiArt(){}
}
