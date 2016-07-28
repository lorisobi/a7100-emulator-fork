/*
 * Decoder.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * Copyright (c) 2011-2016 Dirk Bräuer
 *
 * Der A7100 Emulator ist Freie Software: Sie können ihn unter den Bedingungen
 * der GNU General Public License, wie von der Free Software Foundation,
 * Version 3 der Lizenz oder jeder späteren veröffentlichten Version, 
 * weiterverbreiten und/oder modifizieren.
 *
 * Der A7100 Emulator wird in der Hoffnung, dass er nützlich sein wird, aber
 * OHNE JEDE GEWÄHRLEISTUNG, bereitgestellt; sogar ohne die implizite
 * Gewährleistung der MARKTFÄHIGKEIT oder EIGNUNG FÜR EINEN BESTIMMTEN ZWECK.
 * Siehe die GNU General Public License für weitere Details.
 *
 * Sie sollten eine Kopie der GNU General Public License zusammen mit diesem
 * Programm erhalten haben. Wenn nicht, siehe <http://www.gnu.org/licenses/>.
 * 
 * Letzte Änderungen:
 *   05.04.2014 - Kommentare vervollständigt
 *   18.11.2014 - Fehlerausgabe geändert
 *   28.07.2016 - Singleton entfernt
 *              - Parameter für Namen und Segment-Verwendung hinzugefügt
 *              - Fehler beim Speichern behoben
 */
package a7100emulator.Debug;

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
 * Klasse zum Bereitstellen von Decoder Informationen.
 * <p>
 * TODO: Diese Klasse kann ggf. direkt in den Debugger integriert werden oder
 * benötigt eine separate Möglichkeit zum aktivieren/deaktivieren.
 *
 * @author Dirk Bräuer
 */
public class Decoder {

    /**
     * Decoder Informationen mit Adresse: Befehl, Operanden
     */
    private final TreeMap<Integer, String[]> decoder = new TreeMap();
    /**
     * Zuletzt hinzugefügte Adresse
     */
    private int lastAddress = 0;
    /**
     * Tabelle zur Anzeige der Decoder-Informationen
     */
    private final JTable table = new JTable(new DecoderTableModel());
    /**
     * Dateiname für Decoder-Ausgaben
     */
    private final String filename;
    /**
     * Gibt an ob Code-Segmente verwendet werden
     */
    private final boolean useCS;
    /**
     * Name der Decoder-Instanz
     */
    private final String ident;

    /**
     * Erstellt einen neuen Decoder.
     *
     * @param filename Dateiname für LOG-Datei
     * @param useCS    <code>true</code> wenn die Adressangabe aufgeteilt in
     *                 segment:offset erfolgen soll
     * @param ident    Bezeichner des Decoders (bspw. Modulname)
     */
    public Decoder(String filename, boolean useCS, String ident) {
        this.filename = filename;
        this.useCS = useCS;
        this.ident = ident;
    }

    /**
     * Zeigt die Decoder-Informationen in einem Fensteran
     */
    public void show() {
        JFrame frame = new JFrame("Disassembler - " + filename);
        frame.setMinimumSize(new Dimension(600, 500));
        frame.setPreferredSize(new Dimension(600, 500));
        frame.getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
        frame.setVisible(true);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(Decoder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Speichert die Decoder-Informationen in einer Datei.
     */
    public void save() {
        try {
            PrintStream decoderFile = new PrintStream(new FileOutputStream("./debug/" + filename + "_decoder.log"));
            for (String[] decoderLine : decoder.values()) {
                decoderFile.println(decoderLine[0] + " " + decoderLine[1] + (decoderLine[2].isEmpty() ? "" : (" (" + decoderLine[2] + ")")));
            }
            decoderFile.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Decoder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Fügt die aktuellen Debug-Informationen dem Decoder hinzu.
     *
     * @param debugInfo Debug-Informationen
     */
    public void addItem(DebuggerInfo debugInfo) {
        lastAddress = debugInfo.getCs() * 16 + debugInfo.getIp();
        synchronized (decoder) {
            String addressString = useCS ? String.format("%04X:%04X", debugInfo.getCs(), debugInfo.getIp()) : String.format("%04X", debugInfo.getIp());
            decoder.put(lastAddress, new String[]{addressString, debugInfo.getCode(), debugInfo.getOperands() == null ? "" : debugInfo.getOperands()});
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
         * @param row    Zeilennummer
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
         * @param row    Zeilennummer
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
         * @param value  Objekt
         * @param row    Zeilennummer
         * @param column Spaltennummer
         */
        @Override
        public void setValueAt(Object value, int row, int column) {
        }
    }
}
