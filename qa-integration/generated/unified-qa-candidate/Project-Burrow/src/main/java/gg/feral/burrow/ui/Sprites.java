package gg.feral.burrow.ui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;

/** Faithful crops of owner-approved imagery; no generated icon substitutions. */
final class Sprites {
    private static final String ROOT="/gg/feral/burrow/ui/";
    private static final BufferedImage AVATAR=load("feral-avatar.png");
    private static final BufferedImage OWNER_BADGE=load("owner-r01-badge.png");
    // Prefer the owner-approved R10C armored raccoon; fall back to the historical r05 crop.
    private static final BufferedImage ARMORED_BADGE=firstPresent("approved-armored-raccoon.png","armored-raccoon-r05.png");
    private static final BufferedImage HERO=load("feral-approved-hero.png");
    private static final BufferedImage PACK_BRAND=load("brand-pack-chest.png");
    private static final BufferedImage WEAPON=load("signal-weapon.png");
    private static final BufferedImage COMBAT=load("signal-combat.png");
    private static final BufferedImage LANDSCAPE=load("pack-mountains.png");
    private static final BufferedImage[] NAV={load("nav-home.png"),load("nav-pathfinder.png"),
            load("nav-outfitter.png"),load("nav-sessions.png")};
    static BufferedImage load(String name) {
        try (InputStream stream=Sprites.class.getResourceAsStream(ROOT+name)) {
            return stream==null?null:ImageIO.read(stream);
        } catch(java.io.IOException ex) { return null; }
    }
    private static BufferedImage firstPresent(String... names) {
        for (String name : names) {
            BufferedImage image = load(name);
            if (image != null) return image;
        }
        return null;
    }
    private static void draw(Graphics2D g,BufferedImage sprite,int x,int y,int w,int h) {
        if(sprite==null)return;
        Object old=g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(sprite,x,y,w,h,null);
        if(old!=null)g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,old);
        else g.getRenderingHints().remove(RenderingHints.KEY_INTERPOLATION);
    }
    static void landscape(Graphics2D g,int x,int y,int w,int h) {draw(g,LANDSCAPE,x,y,w,h);}
    static void packBrand(Graphics2D g,int x,int y,int w,int h) {draw(g,PACK_BRAND,x,y,w,h);}
    static void signal(Graphics2D g,boolean weapon,int x,int y,int w,int h) {draw(g,weapon?WEAPON:COMBAT,x,y,w,h);}
    static void avatar(Graphics2D g,int x,int y,int w,int h) {draw(g,AVATAR,x,y,w,h);}
    static void ownerBadge(Graphics2D g,int x,int y,int w,int h) {draw(g,OWNER_BADGE,x,y,w,h);}
    static void armoredBadge(Graphics2D g,int x,int y,int w,int h) {draw(g,ARMORED_BADGE==null?OWNER_BADGE:ARMORED_BADGE,x,y,w,h);}
    static void hero(Graphics2D g,int x,int y,int w,int h) {draw(g,HERO,x,y,w,h);}
    static void nav(Graphics2D g,int tab,int x,int y,int w,int h) {draw(g,NAV[tab],x,y,w,h);}
    static boolean loaded() {
        if(AVATAR==null||HERO==null||PACK_BRAND==null||WEAPON==null||COMBAT==null||LANDSCAPE==null)return false;
        for(BufferedImage img:NAV)if(img==null)return false;
        return true;
    }
    private Sprites() {}
}
