/*
 * StateSavable.java
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
 *   18.11.2014 - Erstellt
 */
package a7100emulator.Tools;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Interface für alle Komponenten deren Zustand beim Speichern und Laden der
 * A7100-Daten berücksichtigt werden soll.
 *
 * @author Dirk Bräuer
 */
public interface StateSavable {

    /**
     * Speichert den Zustand der Komponente in einer Datei.
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Speichern nicht erfolgreich war
     */
    public void saveState(final DataOutputStream dos) throws IOException;

    /**
     * Liest den Zustand der Komponente aus einer Datei.
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    public void loadState(final DataInputStream dis) throws IOException;

}
