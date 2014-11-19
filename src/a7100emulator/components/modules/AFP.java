/*
 * AFP.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   01.04.2014 - Kommentare vervollständigt
 *   18.11.2014 - Interface Modul hinzugefügt
 *
 */
package a7100emulator.components.modules;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Klasse zur Abbildung der AFP (Adapter für Festplatten)
 * <p>
 * TODO: Diese Klasse ist noch nicht vollständig implementiert und wird nur für
 * die Emulation des A7150 benötigt
 *
 * @author Dirk Bräuer
 */
public class AFP implements Module {

    @Override
    public void init() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void loadState(DataInputStream dis) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
