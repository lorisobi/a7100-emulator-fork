/*
 * AFP.java
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
 *   01.04.2014 - Kommentare vervollständigt
 *   18.11.2014 - Interface Modul hinzugefügt
 *   05.12.2015 - Methoden für lesen Eproms hinzugefügt
 *   09.08.2016 - Logger hinzugefügt
 *   14.10.2016 - Eproms Lesen entfernt
 *              - Ausgaben für Logger umgeleitet
 */
package a7100emulator.components.modules;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasse zur Abbildung der AFP (Adapter für Festplatten)
 * <p>
 * TODO: Diese Klasse ist noch nicht vollständig implementiert und wird nur für
 * die Emulation des A7150 benötigt.
 *
 * @author Dirk Bräuer
 */
public class AFP implements Module {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(AFP.class.getName());

    /**
     * Initialisiert die AFP
     */
    @Override
    public void init() {
        LOG.log(Level.SEVERE, "Methode init() in Klasse AFP noch nicht implementiert!");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Speichert den Zusand der AFP in einer Datei.
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        LOG.log(Level.SEVERE, "Methode saveState() in Klasse AFP noch nicht implementiert!");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Lädt den Zustand der AFP aus einer Datei.
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Laden nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        LOG.log(Level.SEVERE, "Methode loadState() in Klasse AFP noch nicht implementiert!");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
