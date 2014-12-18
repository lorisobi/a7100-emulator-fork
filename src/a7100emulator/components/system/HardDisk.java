/*
 * HardDisk.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   05.04.2014 - Kommentare vervollständigt
 *   18.11.2014 - Interface StateSavable implementiert
 *
 */
package a7100emulator.components.system;

import a7100emulator.Tools.StateSavable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

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
