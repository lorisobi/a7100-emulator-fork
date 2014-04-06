/*
 * Module.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   02.04.2014 Kommentare vervollständigt
 *
 */
package a7100emulator.components.modules;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Allgemeines Interface für alle Module
 *
 * @author Dirk Bräuer
 */
public interface Module {

    /**
     * Initialisiert das Modul
     */
    public void init();

    /**
     * Schreibt den Zustand des Moduls in einen Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    public void saveState(DataOutputStream dos) throws IOException;

    /**
     * Liest den Zustand des Moduls aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    public void loadState(DataInputStream dis) throws IOException;

}
