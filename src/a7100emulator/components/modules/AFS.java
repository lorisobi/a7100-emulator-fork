/*
 * AFS.java
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
 */
package a7100emulator.components.modules;

import a7100emulator.components.system.FloppyDrive;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Klasse zur Abbildung der AFS (Anschluß für Folienspeicher)
 *
 * @author Dirk Bräuer
 */
public final class AFS implements Module {

    /**
     * Array der angeschlossenen Laufwerke
     */
    private final FloppyDrive[] drives = new FloppyDrive[4];

    /**
     * Erzeugt eine neue AFS
     */
    public AFS() {
        init();
    }

    /**
     * Gibt die Referenz auf das entsprechende Diskettenlaufwerk zurück
     *
     * @param id ID des Laufwerks
     * @return Laufwerk
     */
    public FloppyDrive getFloppy(int id) {
        return drives[id];
    }

    /**
     * Initialisiert die AFS
     */
    @Override
    public void init() {
        drives[0] = new FloppyDrive(FloppyDrive.DriveType.K5601);
        drives[1] = new FloppyDrive(FloppyDrive.DriveType.K5601);
        drives[2] = new FloppyDrive(FloppyDrive.DriveType.K5600_20);
        drives[3] = new FloppyDrive(FloppyDrive.DriveType.K5600_20);
    }

    /**
     * Schreibt den Zustand der AFS in eine Datei
     *
     * @param dos Stream der Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        for (int i = 0; i < 4; i++) {
            drives[i].saveState(dos);
        }
    }

    /**
     * Liest den Zustand der AFS aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        for (int i = 0; i < 4; i++) {
            drives[i].loadState(dis);
        }
    }
}
