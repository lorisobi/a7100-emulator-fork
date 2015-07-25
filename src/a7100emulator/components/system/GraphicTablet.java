/*
 * GraphicTablet.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2015 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   19.11.2014 - Erstellt
 */
package a7100emulator.components.system;

import a7100emulator.Tools.StateSavable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Klasse zur Realisierung des Grafiktabletts K6405
 * <p>
 * TODO: Diese Klasse ist noch nicht implementiert.
 *
 * @author Dirk Bräuer
 */
public class GraphicTablet implements StateSavable {

    /**
     * Speichert den Zustand des Grafiktabletts in einer Datei.
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Speichern fehlgeschlagen ist
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Lädt den Zustand des Grafiktablets aus einer Datei.
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Speichern fehlgeschlagen ist
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
