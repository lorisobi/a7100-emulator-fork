/*
 * AdressSpace.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   05.04.2014 Kommentare vervollständigt
 *
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
