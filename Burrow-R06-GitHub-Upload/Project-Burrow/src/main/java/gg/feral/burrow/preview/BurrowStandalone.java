package gg.feral.burrow.preview;
import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.ui.BurrowPanel;
import javax.swing.*;
/** Offline interactive preview. DOES NOT log in to RuneLite or read your bank. */
public final class BurrowStandalone {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Project Burrow • Feral Experimental Preview");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.setContentPane(new BurrowPanel(new BurrowModel()));
            f.pack(); f.setResizable(false); f.setLocationRelativeTo(null); f.setVisible(true);
        });
    }
}
