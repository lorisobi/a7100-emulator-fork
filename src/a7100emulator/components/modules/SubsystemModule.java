/*
 * SubsystemModule.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * Copyright (c) 2011-2015 Dirk Bräuer
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
 *   30.11.2015 - Interface aus KGS extrahiert
 *   05.12.2015 - requestInterrupt hinzugefügt
 */
package a7100emulator.components.modules;

/**
 * Interface für Module welche ein eigenes Subsystem mit Prozessor,
 * Arbeitsspeicher und lokalen Ports realisieren.
 *
 * @author Dirk Bräuer
 */
public interface SubsystemModule {

    /**
     * Liest ein Byte von einem Lokalen Port
     *
     * @param port Port
     * @return gelesenes Byte
     */
    int readLocalPort(int port);

    /**
     * Gibt ein Byte auf einem lokalen Port aus
     *
     * @param port Port
     * @param data Daten
     */
    void writeLocalPort(int port, int data);

    /**
     * Liest ein Byte aus dem Arbeitsspeicher des Moduls.
     *
     * @param address Adresse
     * @return gelesenes Wort
     */
    int readLocalByte(int address);

    /**
     * Liest ein Wort aus dem Arbeitsspeicher des Moduls.
     *
     * @param address Adresse
     * @return gelesenes Wort
     */
    int readLocalWord(int address);

    /**
     * Schreibt ein Byte in den Arbeitsspeicher des Moduls.
     *
     * @param address Adresse
     * @param data Daten
     */
    void writeLocalByte(int address, int data);

    /**
     * Schreibt ein Wort in den Arbeitsspeicher des Moduls.
     *
     * @param address Adresse
     * @param data Daten
     */
    void writeLocalWord(int address, int data);

    /**
     * Aktualisiert die Systemzeit des Moduls Basis der Taktzyklen des
     * Prozessors.
     *
     * @param cycles Anzahl der Takte
     */
    void localClockUpdate(int cycles);

    /**
     * Leitet die Anfrage der Interruptbehandlung an die CPU weiter
     *
     * @param i Interruptnummer
     */
    void requestInterrupt(int i);
}
