/*
 * IOModule.java
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
 *   09.08.2014 - Umbenannt von Port Module in IOModule
 *   18.11.2014 - Funktionen umbenannt
 */
package a7100emulator.components.modules;

/**
 * Interface für Module welche E/A Ports verwenden
 *
 * @author Dirk Bräuer
 */
public interface IOModule extends Module {

    /**
     * Registriert das Modul im E/A Bereich
     */
    void registerPorts();

    /**
     * Liest ein Byte von einem Port
     *
     * @param port Port
     * @return gelesenes Byte
     */
    int readPortByte(int port);

    /**
     * Liest ein Wort von einem Port
     *
     * @param port Port
     * @return gelesenes Wort
     */
    int readPortWord(int port);

    /**
     * Gibt ein Byte auf einem Port aus
     *
     * @param port Port
     * @param data Daten
     */
    void writePortByte(int port, int data);

    /**
     * Gibt ein Wort auf einem Port aus
     *
     * @param port Port
     * @param data Daten
     */
    void writePortWord(int port, int data);

}
