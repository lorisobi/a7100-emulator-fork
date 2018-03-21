/*
 * MemoryModule.java
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
 *   02.04.2014 - Kommentare vervollständigt
 */
package a7100emulator.components.modules;

/**
 * Interface für Module welche Arbeitsspeicher zur Verfügung stellen
 *
 * @author Dirk Bräuer
 */
public interface MemoryModule extends Module {

    /**
     * Registriert das Modul im Systemspeicher
     */
    void registerMemory();

    /**
     * Liest ein Byte von der angegebenen Adresse
     *
     * @param address Adresse
     * @return gelesenes Byte
     */
    int readByte(int address);

    /**
     * Liest ein Wort von der angegebenen Adresse
     *
     * @param address Adresse
     * @return gelesenes Wort
     */
    int readWord(int address);

    /**
     * Schreibt ein Byte an die angegebene Adresse
     *
     * @param address Adresse
     * @param data Daten
     */
    void writeByte(int address, int data);

    /**
     * Schreibt ein Wort an die angegebene Adresse
     *
     * @param address Adresse
     * @param data Daten
     */
    void writeWord(int address, int data);
}
