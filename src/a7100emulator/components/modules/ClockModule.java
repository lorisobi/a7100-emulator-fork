/*
 * ClockModule.java
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
 *   02.04.2014 - Kommentare vervollständigt
 *   24.07.2016 - Parameter umbenannt
 */
package a7100emulator.components.modules;

/**
 * Interface für Module welche auf Änderungen der Systemzeit reagieren
 *
 * @author Dirk Bräuer
 */
public interface ClockModule extends Module {

    /**
     * Registriert das Modul für Änderungen der Systemzeit
     */
    void registerClocks();

    /**
     * Verarbeitet die geänderte Systemzeit
     *
     * @param micros Zeitdauer in Mikrosekunden
     */
    void clockUpdate(int micros);
    
}
