/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.Debug;

import a7100emulator.Tools.BitmapGenerator;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.TreeMap;
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
public class Decoder {

    private final TreeMap<Integer, String[]> decoder = new TreeMap();
    private static Decoder instance;
    private final DebuggerInfo debugInfo = DebuggerInfo.getInstance();
    private int lastAddress = 0;
    private final JTable table = new JTable(new DecoderTableModel());

    private Decoder() {
    }

    /**
     *
     * @return
     */
    public static Decoder getInstance() {
        if (instance == null) {
            instance = new Decoder();
        }
        return instance;
    }

    /**
     *
     */
    public void show() {

        JFrame frame = new JFrame("Disassembler");
        frame.setMinimumSize(new Dimension(600, 500));
        frame.setPreferredSize(new Dimension(600, 500));
        frame.getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
        frame.setVisible(true);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(BitmapGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     *
     */
    public void save() {
        try {
            PrintStream decoderFile = new PrintStream(new FileOutputStream("./debug/Decoder.log"));
            Object[] dec = decoder.values().toArray();
            for (Object dec1 : dec) {
                decoderFile.println(dec1.toString());
            }
            decoderFile.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Decoder.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     *
     */
    public void addItem() {
//        String debugString = String.format("%04X:%04X ", debugInfo.getCs(), debugInfo.getIp()) + debugInfo.getCode();
        lastAddress = debugInfo.getCs() * 16 + debugInfo.getIp();
        synchronized (decoder) {
            decoder.put(lastAddress, new String[]{String.format("%04X:%04X", debugInfo.getCs(), debugInfo.getIp()), debugInfo.getCode(), debugInfo.getOperands() == null ? "" : debugInfo.getOperands()});
        }
        ((AbstractTableModel) table.getModel()).fireTableDataChanged();
    }

    public void clear() {
        decoder.clear();
    }

    class DecoderTableModel extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return decoder.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Adresse";
                case 1:
                    return "Befehl";
                case 2:
                    return "Operanden";
                default:
                    throw new IllegalArgumentException("Column " + column + " existiert nicht!");
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
            synchronized (decoder) {
                if (((Integer) decoder.keySet().toArray()[row]) == lastAddress) {
                    table.setRowSelectionInterval(row, row);
                }
                return ((String[]) decoder.values().toArray()[row])[column];
            }
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
        }
    }
}
