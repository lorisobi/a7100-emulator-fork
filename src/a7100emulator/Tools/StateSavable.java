/*
 * StateSavable.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   18.11.2014 - Erstellt
 *
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
