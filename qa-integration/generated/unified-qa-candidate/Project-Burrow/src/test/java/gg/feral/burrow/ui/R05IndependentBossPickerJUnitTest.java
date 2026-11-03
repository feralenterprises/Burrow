package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real UI routing assertions against independent browse state; no game or account fixtures. */
final class R05IndependentBossPickerJUnitTest {
    private static void edt(Runnable task) throws Exception { SwingUtilities.invokeAndWait(task); }
    private static JScrollPane pickerScroll(JPopupMenu popup) {
        JPanel panel=(JPanel)popup.getComponent(0);
        return (JScrollPane)((BorderLayout)panel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
    }
    private static JComboBox<?> sorter(JPopupMenu popup) {
        JPanel panel=(JPanel)popup.getComponent(0);
        JPanel top=(JPanel)((BorderLayout)panel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
        JPanel choices=(JPanel)((BorderLayout)top.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
        return (JComboBox<?>)choices.getComponent(1);
    }
    @Test void chronicleCanBrowseBrutusWhileOutfitterStaysScurrius() throws Exception {edt(()->{
        BurrowModel m=new BurrowModel();m.show(BurrowModel.View.OUTFITTER);BurrowPanel p=new BurrowPanel(m);p.setSize(225,1700);
        String boss=m.boss();String method=m.method();var tier=m.tier();
        p.clickAt(155,p.sourceYForR13Test()-8+10);
        assertTrue(p.isBankMatchView(),"Outfitter's Best Bank selection retained");
        p.clickAt(174,133-8+15);assertTrue(p.isChronicleOpenForR13Test());
        assertEquals(boss,p.chronicleBossForUiTest());
        JPopupMenu popup=p.bossMenu(true);
        JScrollPane sp=pickerScroll(popup);
        JList<?> list=(JList<?>)sp.getViewport().getView();
        assertEquals(5,list.getVisibleRowCount());assertEquals(28,list.getFixedCellHeight());
        assertEquals(4,sorter(popup).getItemCount());
        JPanel picker=(JPanel)popup.getComponent(0);
        JPanel top=(JPanel)((BorderLayout)picker.getLayout()).getLayoutComponent(BorderLayout.NORTH);
        JPanel controls=(JPanel)((BorderLayout)top.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
        assertEquals(6,((JComboBox<?>)controls.getComponent(0)).getItemCount()); // CP-5: Chronicle adds "Raid encounters"
        // Change independent Chronicle selection via a real menu row activation.
        JTextField search=(JTextField)((BorderLayout)top.getLayout()).getLayoutComponent(BorderLayout.NORTH);
        search.setText("Brutus");
        assertEquals(1,list.getModel().getSize());
        list.setSize(204,140);list.doLayout();
        Rectangle row=list.getCellBounds(0,0);
        MouseEvent event=new MouseEvent(list,MouseEvent.MOUSE_CLICKED,1,0,5,row.y+5,1,false,MouseEvent.BUTTON1);
        for(MouseListener listener:list.getMouseListeners())listener.mouseClicked(event);
        assertEquals("Brutus",p.chronicleBossForUiTest());
        assertEquals(boss,m.boss());assertEquals(method,m.method());assertEquals(tier,m.tier());
        assertEquals("Brutus",p.rareDropRows().get(0).text().split(" · ")[1].split("\\s")[0],"research header follows independent Chronicle selection");
        p.clickAt(45,130-8+14);assertFalse(p.isChronicleOpenForR13Test());
        assertEquals(boss,m.boss());assertEquals(method,m.method());assertEquals(tier,m.tier());
        p.clickAt(174,133-8+15);assertEquals("Brutus",p.chronicleBossForUiTest());
    });}
    @Test void pickerSortModesAndNoInventedDifficultyForUnknown() throws Exception {edt(()->{
        BurrowPanel p=new BurrowPanel(new BurrowModel());
        JPopupMenu popup=p.bossMenu();
        JComboBox<?> sort=sorter(popup);
        assertEquals("Name A-Z",sort.getItemAt(0));
        assertEquals("Name Z-A",sort.getItemAt(1));
        sort.setSelectedIndex(1);
        JList<?> list=(JList<?>)pickerScroll(popup).getViewport().getView();
        String first=(String)list.getModel().getElementAt(0);
        String last=(String)list.getModel().getElementAt(list.getModel().getSize()-1);
        assertTrue(first.compareToIgnoreCase(last)>0);
        sort.setSelectedIndex(2);
        assertEquals(5,list.getVisibleRowCount());
    });}
    @Test void homeChronicleDoesNotChangeOutfitterAndBackReturnsHome() throws Exception {edt(()->{
        BurrowModel m=new BurrowModel();BurrowPanel p=new BurrowPanel(m);p.setSize(225,1500);
        p.clickAt(50,490);assertTrue(p.isChronicleOpenForR13Test());
        p.clickAt(200,130-8+14);assertEquals("Scurrius",m.boss());
        p.clickAt(43,130-8+14);assertEquals(BurrowModel.View.HOME,m.view());
    });}
}
