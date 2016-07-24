/*
 * RowHeadRenderer.java
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
