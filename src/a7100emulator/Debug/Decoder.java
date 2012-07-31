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

    private TreeMap<Integer, String> decoder = new TreeMap();
    private static Decoder instance;

    private Decoder() {
    }

    public static Decoder getInstance() {
        if (instance == null) {
            instance = new Decoder();
        }
        return instance;
    }

    public void addItem(int address, String code) {
        decoder.put(address, code);
    }

    public void show() {
        JTable table = new JTable(new DecoderTableModel());
        JFrame frame = new JFrame("Disassembler");
        frame.setMinimumSize(new Dimension(300, 200));
        frame.setPreferredSize(new Dimension(300, 200));
        frame.getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
        frame.setVisible(true);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(BitmapGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void save() {
        try {
            PrintStream decoderFile = new PrintStream(new FileOutputStream("Decoder.log"));
            Object[] dec=decoder.values().toArray();
            for (int i=0;i<dec.length;i++)
                decoderFile.println(dec[i].toString());
            decoderFile.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Decoder.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    class DecoderTableModel extends AbstractTableModel {

        public int getRowCount() {
            return decoder.size();
        }

        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Adresse";
                case 1:
                    return "Befehl";
                default:
                    throw new IllegalArgumentException("Column " + column + " existiert nicht!");
            }
        }

        public Class<?> getColumnClass(int column) {
            return String.class;
        }

        public boolean isCellEditable(int row, int column) {
            return false;
        }

        public Object getValueAt(int row, int column) {
            if (column == 0) {
                return ((String) decoder.values().toArray()[row]).substring(0, 9);
            } else {
                return ((String) decoder.values().toArray()[row]).substring(10);
            }
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
        }
    }
}
