/*
 * CPU.java
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
 *   23.07.2016 - Erstellt
 *   24.07.2016 - Methoden reset() und setDebug() hinzugefügt
 */
package a7100emulator.components.ic;

import a7100emulator.Debug.Decoder;

/**
 * Interface für alle CPU Schlatkreise, welche basierend auf einem externen Takt
 * Befehle aus einem Speicherbereich decodieren und abarbeiten.
 *
 * @author Dirk Bräuer
 */
public interface CPU extends IC {

    /**
     * Führt Zyklen gemäß der angegebenen Anzahl von Takten aus.
     *
     * @param ticks Anzahl der Takte
     */
    void executeCycles(int ticks);

    /**
     * Setzt die CPU in ihren Anfangszustand.
     */
    void reset();

    /**
     * Aktiviert oder Deaktiviert den Debugger
     *
     * @param debug <code>true</code> zum Aktivieren, <code>false</code> zum
     * Deaktivieren
     */
    void setDebug(boolean debug);

    /**
     * Gibt die Instanz des Decoders zurück.
     *
     * @return Decoderinstanz oder <code>null</code> wenn kein Decoder
     *         initialisiert ist.
     */
    Decoder getDecoder();
}
