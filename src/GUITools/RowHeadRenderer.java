/*
 * RowHeadRenderer.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   05.04.2014 Kommentare vervollständigt
 *
 */
package GUITools;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

/**
 * Renderer für Spaltenheader
 *
 * @author Dirk Bräuer
 */
public class RowHeadRenderer implements TableCellRenderer {

    /**
     * Gibt die Darzustellende Komponente zurück
     *
     * @param table Tabelle
     * @param value Wert
     * @param isSelected wahr, wenn Selektiert
     * @param hasFocus wahr, wenn Fokus
     * @param row Zeile
     * @param column Spalte
     * @return Komponente
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel lab = new JLabel(value.toString());
        lab.setOpaque(true);
        lab.setFont(table.getFont());
        lab.setHorizontalAlignment(SwingConstants.RIGHT);
        return lab;
    }
}
