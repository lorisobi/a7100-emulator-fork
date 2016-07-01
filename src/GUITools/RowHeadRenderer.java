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
 *   28.07.2016 - Kommentare erweitert
 */
package GUITools;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

/**
 * Renderer für Spaltenheader. Dieser Renderer formatiert eine Spalte einer
 * JTable, damit sie im Aussehen einer Kopfzeile gleicht. Die Elemente innerhalb
 * der Spalte werden rechtsbündig dargestellt. Dies kann beispielsweise für die
 * Darstellung von Speicherbereiten effektiv verwendet werden.
 *
 * @author Dirk Bräuer
 */
public class RowHeadRenderer implements TableCellRenderer {

    /**
     * Gibt die darzustellende Komponente zurück.
     *
     * @param table Tabelle
     * @param value Zellwert
     * @param isSelected <code>true</code>, wenn die Zelle ausgewählt ist
     * @param hasFocus <code>true</code> wenn die Komponente den Fokus besitzt
     * @param row Zeile
     * @param column Spalte
     * @return Komponente
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel cellLabel = new JLabel(value.toString());
        cellLabel.setOpaque(true);
        cellLabel.setFont(table.getFont());
        cellLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        return cellLabel;
    }
}
