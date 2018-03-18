/*
 * HardDisk.java
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
 *   18.11.2014 - Interface StateSavable implementiert
 *   09.08.2016 - Logger hinzugefügt
 */
package a7100emulator.components.system;

import a7100emulator.Tools.StateSavable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Klasse zur Abbildung einer Festplatte
 * <p>
 * TODO: Diese Klasse ist noch nicht vollständig implementiert und wird nur für
 * die Emulation des A7150 benötigt.
 *
 * @author Dirk Bräuer
 */
public class HardDisk implements StateSavable {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(HardDisk.class.getName());

    /**
     * Speichert den Zustand der Festplatte in einer Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Speichern fehlschlägt
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Lädt den Zustand der Festplatte aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Laden fehlschlägt
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
