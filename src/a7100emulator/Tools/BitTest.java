/*
 * SCPFileModel.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2015 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   30.09.2014 - Erstellt
 */
package a7100emulator.Tools;

/**
 * Statische Klasse zum Testen von Bits
 *
 * @author Dirk Bräuer
 */
public class BitTest {

    /**
     * Privater Konstruktur
     */
    private BitTest() {
    }

    /**
     * Prüft ob ein Bit des Operanden gesetzt ist
     *
     * @param op Operand
     * @param bit Nummer des Bits
     * @return true - wenn das Bit gesetzt ist, false - sonst
     */
    public static boolean getBit(int op, int bit) {
        return (((op >> bit) & 0x01) == 0x01);
    }
}
