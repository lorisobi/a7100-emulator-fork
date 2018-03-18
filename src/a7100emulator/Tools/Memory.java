/*
 * Memory.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * Copyright (c) 2011-2018 Dirk Bräuer
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
 *   01.01.2015 - Konstruktor mit Byte Array hinzugefügt
 *   26.03.2016 - Methode clear() ergänzt
 *   09.08.2016 - Logger hinzugefügt
 *              - Exception beim Laden von Datei ausgeben
 */
package a7100emulator.Tools;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Klasse zur Abbildung eines Speichers
 *
 * @author Dirk Bräuer
 */
public class Memory {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(Memory.class.getName());

    /**
     * Speichertyp
     */
    public enum MemoryType {

        /**
         * Ram
         */
        RAM,
        /**
         * Rom
         */
        ROM;
    }

    /**
     * Modus zum Laden von Roms aus Dateien
     */
    public enum FileLoadMode {

        /**
         * Nur das niedere Byte laden
         */
        LOW_BYTE_ONLY,
        /**
         * Nur das höhere Byte laden
         */
        HIGH_BYTE_ONLY,
        /**
         * Niederes und Höheres Byte laden
         */
        LOW_AND_HIGH_BYTE;
    }
    /**
     * Speicherinhalt
     */
    private final byte[] memory;

    /**
     * Erstellt einen neuen Speicher.
     *
     * @param size Größe in Byte
     */
    public Memory(int size) {
        memory = new byte[size];
    }

    /**
     * Erstellt einen neuen Speicher basierend auf einem Byte Array.
     *
     * @param data Speicherinhalt
     */
    public Memory(byte[] data) {
        memory = new byte[data.length];
        System.arraycopy(data, 0, memory, 0, data.length);
    }

    /**
     * Lädt einen Speicherinhalt aus einer Datei
     *
     * @param baseAddress Basisadresse
     * @param file Datei mit Speicherinhalt
     * @param loadMode Lademodus
     * @throws java.io.IOException Wenn das Laden nicht erfolgreich war
     */
    public void loadFile(int baseAddress, File file, FileLoadMode loadMode) throws IOException {
        InputStream in = null;
        try {
            byte[] buffer = new byte[(int) file.length()];
            in = new FileInputStream(file);
            in.read(buffer);
            in.close();

            int address = baseAddress + (loadMode.equals(FileLoadMode.HIGH_BYTE_ONLY) ? 1 : 0);
            for (byte b : buffer) {
                memory[address++] = b;
                if (!loadMode.equals(FileLoadMode.LOW_AND_HIGH_BYTE)) {
                    address++;
                }
            }
        } finally {
            in.close();
        }
    }

    /**
     * Schreibt ein Byte in den Speicher
     *
     * @param address Adresse
     * @param value Daten
     */
    public void writeByte(int address, int value) {
        memory[address] = (byte) value;
    }

    /**
     * Liest ein Byte aus dem Speicher
     *
     * @param address Adresse
     * @return gelesene Daten
     */
    public int readByte(int address) {
        return memory[address] & 0xFF;
    }

    /**
     * Schreibt ein Wort in den Speicher.
     *
     * @param address Adresse
     * @param value Daten
     */
    public void writeWord(int address, int value) {
        byte hb = (byte) (value >> 8);
        byte lb = (byte) value;
        memory[address] = lb;
        memory[address + 1] = hb;
    }

    /**
     * Liest ein Wort aus dem Speicher.
     *
     * @param address Adresse
     * @return gelesene Daten
     */
    public int readWord(int address) {
        int result;
        int lb = memory[address];
        int hb = memory[address + 1];
        result = ((hb << 8) | (lb & 0xFF));
        return result & 0xFFFF;
    }

    /**
     * Gibt die Größe des Speichers in bytes zurück
     *
     * @return Speichergröße
     */
    public int getSize() {
        return memory.length;
    }

    /**
     * Löscht den Speicher durch setzen von allen Bytes zurück auf 0x00.
     */
    public void clear() {
        for (int i = 0; i < memory.length; i++) {
            memory[i] = 0x00;
        }
    }

    /**
     * Schreibt den Speicherinhalt in eine Datei
     *
     * @param dos Stream zur DAtei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    public void saveMemory(DataOutputStream dos) throws IOException {
        dos.write(memory);
    }

    /**
     * Liest einen Speicherinhalt aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Laden nicht erfolgreich war
     */
    public void loadMemory(DataInputStream dis) throws IOException {
        dis.read(memory);
    }
}
