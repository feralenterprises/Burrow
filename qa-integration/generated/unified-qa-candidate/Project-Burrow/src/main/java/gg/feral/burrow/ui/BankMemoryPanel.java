package gg.feral.burrow.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Objects;

/**
 * PQA-W07 standalone Swing view candidate. Embeddable under Outfitter without
 * altering TPT's four main tabs. NOT yet wired to the RuneLite host/sidebar.
 */
public final class BankMemoryPanel extends JPanel {
    private final JLabel title = new JLabel("BANK MEMORY · NO SCAN");
    private final JLabel notice = new JLabel("No account bank evidence");
    private final JLabel freshness = new JLabel("Unknown");
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Exact item", "Qty", "State"},0) {
        @Override public boolean isCellEditable(int row,int column){return false;}
    };
    private final JButton refresh = new JButton("Scan bank manually");
    private Runnable requestManualScan=()->{};
    public BankMemoryPanel() {
        super(new BorderLayout(5,5));
        setName("BankMemoryView");
        var header=new JPanel(new BorderLayout());
        header.add(title,BorderLayout.NORTH);
        header.add(freshness,BorderLayout.CENTER);
        header.add(notice,BorderLayout.SOUTH);
        add(header,BorderLayout.NORTH);
        JTable grid=new JTable(model);grid.setName("BankMemoryRows");
        // No nested JScrollPane: SidebarHost owns the single sidebar scroll viewport.
        add(grid,BorderLayout.CENTER);
        var actions=new JPanel(new FlowLayout(FlowLayout.LEFT,2,2));
        refresh.setName("BankMemoryManualScan");
        refresh.addActionListener(e->requestManualScan.run());
        actions.add(refresh);add(actions,BorderLayout.SOUTH);
        setSize(225,360);
    }
    public void setRequestManualScan(Runnable command) {requestManualScan=Objects.requireNonNull(command);}
    public void setSnapshot(BankMemoryViewModel.Display data) {
        if(!SwingUtilities.isEventDispatchThread())throw new IllegalStateException("Bank Memory updates require EDT");
        title.setText("BANK MEMORY · HISTORICAL");
        freshness.setText(data.asOf()+" · "+data.freshness());
        notice.setText("<html>"+escape(data.notice())+"</html>");
        model.setRowCount(0);
        for(var r:data.rows())model.addRow(new Object[]{r.name()+" (#"+r.exactItemId()+")",r.observedQuantity(),r.variantState()});
        refresh.setText(data.suggestManualScan()?"Open bank to refresh":"Refresh from next bank scan");
    }
    private static String escape(String s) {return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}
    public void setRefreshNotice(String message) { if(!SwingUtilities.isEventDispatchThread())throw new IllegalStateException("EDT required"); notice.setText("<html>"+escape(message)+"</html>"); }
    public int displayedRows(){return model.getRowCount();}
    public void pressRefreshForTest(){refresh.doClick();}
}
