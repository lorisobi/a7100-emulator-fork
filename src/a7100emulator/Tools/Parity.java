/*
 * Parity.java
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
 *   31.07.2016 - Erstellt aus interner OPS-Klasse
 */
package a7100emulator.Tools;

/**
 * Enum für verwendete Paritäten.
 *
 * @author Dirk Bräuer
 */
public enum Parity {

    /**
     * gerade Parität
     */
    EVEN,
    /**
     * ungerade Parität
     */
    ODD;

    /**
     * Prüft die Parität eines Bytes
     *
     * @param data Daten
     * @param parity Gewünschte Parität
     * @return Parität (0-gerade / 1-ungerade)
     */
    public static int calculateParityBit(int data, Parity parity) {
        int par = (parity.equals(Parity.EVEN)) ? 0x00 : 0x01;
        for (int i = 0; i < 8; i++) {
            par ^= (0x01 & (data >> i));
        }
        return par;
    }
}
