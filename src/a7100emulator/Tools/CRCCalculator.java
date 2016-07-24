/*
 * CRCCalculator.java
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
 *   16.08.2014 - Erste Version
 */
package a7100emulator.Tools;

/**
 * Klasse zum Berechnen von CRC-Prüfsummen.
 *
 * @author Dirk Bräuer
 */
public class CRCCalculator {

    /**
     * Privater Konstruktor, da nur statische funktionen verwendet werden.
     */
    private CRCCalculator() {
    }

    /**
     * Berechnet die CRC16 Prüfsumme für ein Datenarray.
     *
     * @param data Array mit Daten
     * @param generator Generatorpolynom
     * @param start Startwert
     * @return CRC-Prüfsumme
     */
    public static int calculateCRC16(byte[] data, int generator, int start) {
        int crc = start;

        for (byte b : data) {
            // Gehe bitweise durch Byte
            for (int i = 0; i < 8; i++) {
                // Prüfe ob höchstes aus Daten ungleich höchstem Bit in CRC
                if (BitTest.getBit(b, 7 - i) ^ BitTest.getBit(crc, 15)) {
                    // Schiebe CRC und berechne XOR Generatorpolynom
                    crc <<= 1;
                    crc ^= generator;
                } else {
                    // Schiebe CRC
                    crc <<= 1;
                }
            }

        }
        return crc & 0xFFFF;
    }
    
    public static void main(String args[]) {
        byte[] data=new byte[]{(byte)0xA1,(byte)0xA1,(byte)0xA1,(byte)0xFE,0x00,0x01,0x01,0x01,(byte)0xCD,0x3C};
        int generator=0x1021;
        int start=0xFFFF;
        int crc=CRCCalculator.calculateCRC16(data, generator, start);
        System.out.println(String.format("%04X",crc));
    }
}
