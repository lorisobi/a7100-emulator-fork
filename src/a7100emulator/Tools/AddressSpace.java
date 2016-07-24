/*
 * AdressSpace.java
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
package a7100emulator.Tools;

/**
 * Klasse zum Speichern eines Adressbereiches
 *
 * @author Dirk Bräuer
 */
public class AddressSpace {

    /**
     * Erste Adresse des Bereiches
     */
    private final int lowerAddress;
    /**
     * Letzte Adresse des Bereiches
     */
    private final int higherAddress;

    /**
     * Erstellt einen neuen Adressbereich
     *
     * @param low erste Adresse
     * @param high letzte Adresse
     */
    public AddressSpace(int low, int high) {
        this.lowerAddress = low;
        this.higherAddress = high;
    }

    /**
     * Gibt die Startadresse des Adressbereichs zurück
     *
     * @return Startadresse
     */
    public int getLowerAddress() {
        return lowerAddress;
    }

    /**
     * Gibt die Endadresse des Adressbereichs zurück
     *
     * @return Endadresse
     */
    public int getHigherAddress() {
        return higherAddress;
    }
}
