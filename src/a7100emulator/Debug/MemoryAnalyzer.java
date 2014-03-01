/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.Debug;

import GUITools.RowHeadRenderer;
import a7100emulator.components.system.SystemMemory;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Dirk
 */
public class MemoryAnalyzer {

    /**
     * 
     */
    public void show() {
        JTable table = new JTable(new MemoryTableModel());
        table.setShowGrid(false);
        table.getColumn("").setCellRenderer(new RowHeadRenderer());
        table.getColumn("").setPreferredWidth(150);
        table.getColumn("ASCII").setMinWidth(150);
        table.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JFrame frame = new JFrame("Memory");
        frame.setMinimumSize(new Dimension(800, 600));
        frame.setPreferredSize(new Dimension(800, 600));
        frame.getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
        frame.setVisible(true);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(MemoryAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        }


    }

    class MemoryTableModel extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return 65536;
        }

        @Override
        public int getColumnCount() {
            return 18;
        }

        @Override
        public String getColumnName(int column) {
            if (column > 0 && column < 17) {
                return "" + Integer.toHexString(column - 1).toUpperCase();
            } else if (column==17) {
                return "ASCII";
            } else {
                return "";
            }
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (column == 0) {
                return String.format("%05X", row * 16);
            } else if (column==17) {
                String ascii="";
                for (int i=0;i<16;i++) {
                    Integer val=SystemMemory.getInstance().readByte(row * 16 + i);
                    if (val<0x20||val==127) ascii+='.'; 
                    else ascii+=(char)(val&0xFF);
                }
                return ascii;
            }else {
                return String.format("%02X", SystemMemory.getInstance().readByte(row * 16 + column - 1));
            }
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
        }
    }
}
