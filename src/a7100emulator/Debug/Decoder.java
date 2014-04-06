/*
 * Decoder.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   05.04.2014 Kommentare vervollständigt
 *
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
 * Singleton-Klasse zum Bereitstellen von Decoder Informationen
 *
 * @author Dirk Bräuer
 */
public class Decoder {

    /**
     * Decoder Informationen mit Adresse: Befehl, Operanden
     */
    private final TreeMap<Integer, String[]> decoder = new TreeMap();
    /**
     * Instanz des Decoders
     */
    private static Decoder instance;
    /**
     * Referenz auf Debugger Informationen
     */
    private final DebuggerInfo debugInfo = DebuggerInfo.getInstance();
    /**
     * Zuletzt hinzugefügte Adresse
     */
    private int lastAddress = 0;
    /**
     * Tabelle zur Anzeige der Decoder-Informationen
     */
    private final JTable table = new JTable(new DecoderTableModel());

    /**
     * Erstellt einen neuen Decoder
     */
    private Decoder() {
    }

    /**
     * Gibt die Instanz des Decoders zurück
     *
     * @return Instanz
     */
    public static Decoder getInstance() {
        if (instance == null) {
            instance = new Decoder();
        }
        return instance;
    }

    /**
     * Zeigt die Decoder-Informationen in einem Fensteran
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
     * Speichert die Decoder-Informationen in der Datei ./debug/Decoder.log
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
     * Fügt die aktuellen Debug-Informationen dem Decoder hinzu
     */
    public void addItem() {
//        String debugString = String.format("%04X:%04X ", debugInfo.getCs(), debugInfo.getIp()) + debugInfo.getCode();
        lastAddress = debugInfo.getCs() * 16 + debugInfo.getIp();
        synchronized (decoder) {
            decoder.put(lastAddress, new String[]{String.format("%04X:%04X", debugInfo.getCs(), debugInfo.getIp()), debugInfo.getCode(), debugInfo.getOperands() == null ? "" : debugInfo.getOperands()});
        }
        ((AbstractTableModel) table.getModel()).fireTableDataChanged();
    }

    /**
     * Löscht den Decoderinhalt
     */
    public void clear() {
        decoder.clear();
    }

    /**
     * Tabellenmodell zur Anzeige von Decoder-Informationen
     *
     * @author Dirk Bräuer
     */
    private class DecoderTableModel extends AbstractTableModel {

        /**
         * Gibt die Anzahl der Tabellenzeilen zurück
         *
         * @return Zeilenanzahl
         */
        @Override
        public int getRowCount() {
            return decoder.size();
        }

        /**
         * Gibt die Anzahl der Spalten zurück
         *
         * @return Spaltenanzahl
         */
        @Override
        public int getColumnCount() {
            return 3;
        }

        /**
         * Gibt die Spaltennamen zurück
         *
         * @param column Spaltennummer
         * @return Spaltenname
         */
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
            synchronized (decoder) {
                if (((Integer) decoder.keySet().toArray()[row]) == lastAddress) {
                    table.setRowSelectionInterval(row, row);
                }
                return ((String[]) decoder.values().toArray()[row])[column];
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
