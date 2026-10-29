package gg.feral.burrow.ui;
import java.awt.*;
/** New tiny pixel mascot composed in code: not imported from previous plugin. */
final class Raccoon {
    private static final String[] PIXELS = {
        "....##......##....",
        "...####....####...",
        "..################..",
        ".##################.",
        ".###............###.",
        "###..####..####..###",
        "###..#..#..#..#..###",
        "###..####..####..###",
        ".##..............##.",
        "..###....##....###..",
        "...####......####...",
        "....############....",
        ".......######......."
    };
    static void draw(Graphics2D g, int x, int y, int scale) {
        for (int j=0;j<PIXELS.length;j++) for(int i=0;i<PIXELS[j].length();i++) {
            if (PIXELS[j].charAt(i)=='#') {
                g.setColor(j < 4 ? new Color(227,177,99) : new Color(225,226,216));
                g.fillRect(x+i*scale,y+j*scale,scale,scale);
            }
        }
    }
}
