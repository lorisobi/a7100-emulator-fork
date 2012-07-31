/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package GUITools;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author Dirk
 */
public class RowHeadRenderer implements TableCellRenderer {

    /**
     * Gibt die Darzustellende Komponente zur++ck
     * @param table Tabelle
     * @param value Wert
     * @param isSelected wahr, wenn Selektiert
     * @param hasFocus wahr, wenn Fokus
     * @param row Zeile
     * @param col Spalte
     * @return Komponente
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
        JLabel lab = new JLabel(value.toString());
        lab.setOpaque(true);
        lab.setFont(table.getFont());
        lab.setHorizontalAlignment(SwingConstants.RIGHT);
        return lab;
    }
}
