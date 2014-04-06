/*
 * MemoryAnalyzer.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   05.04.2014 Kommentare vervollständigt
 *
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
 * Klasse zum Anzeigen des Speicherinhalts
 *
 * @author Dirk Bräuer
 */
public class MemoryAnalyzer {

    /**
     * Zeigt den Speicherinhalt in einem separaten Fenster an
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

    /**
     * Tabellenmodell zum anzeigen von Speicherinformationen
     *
     * @author Dirk Bräuer
     */
    private class MemoryTableModel extends AbstractTableModel {

        /**
         * Gibt die Anzahl der Zeilen zurück
         *
         * @return Zeilenanzahl
         */
        @Override
        public int getRowCount() {
            return 65536;
        }

        /**
         * Gibt die Anzahl der Spalten zurück
         *
         * @return Spaltenanzahl
         */
        @Override
        public int getColumnCount() {
            return 18;
        }

        /**
         * Gibt den Spaltennamen zurück
         *
         * @param column Spaltennummer
         * @return Spaltenname
         */
        @Override
        public String getColumnName(int column) {
            if (column > 0 && column < 17) {
                return "" + Integer.toHexString(column - 1).toUpperCase();
            } else if (column == 17) {
                return "ASCII";
            } else {
                return "";
            }
        }

        /**
         * Gibt die Klasse der Spaltendaten zurück
         *
         * @param column Spaltennummer
         * @return Klasse
         */
        @Override
        public Class<?> getColumnClass(int column) {
            return String.class;
        }

        /**
         * Gibt an ob die gewählte Zelle editierbar ist
         *
         * @param row Zeilennummer
         * @param column Spaltennummer
         * @return true - wenn editierbar , false - sonst
         */
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        /**
         * Gibt das Objekt an der gegebenen Poision zurück
         *
         * @param row Zeilennummer
         * @param column Spaltennummer
         * @return Objekt der Zelle
         */
        @Override
        public Object getValueAt(int row, int column) {
            if (column == 0) {
                return String.format("%05X", row * 16);
            } else if (column == 17) {
                String ascii = "";
                for (int i = 0; i < 16; i++) {
                    Integer val = SystemMemory.getInstance().readByte(row * 16 + i);
                    if (val < 0x20 || val == 127) {
                        ascii += '.';
                    } else {
                        ascii += (char) (val & 0xFF);
                    }
                }
                return ascii;
            } else {
                return String.format("%02X", SystemMemory.getInstance().readByte(row * 16 + column - 1));
            }
        }

        /**
         * Setzt das Objekt der Zelle
         *
         * @param value Objekt
         * @param row Zeilennummer
         * @param column Spaltennummer
         */
        @Override
        public void setValueAt(Object value, int row, int column) {
        }
    }
}
