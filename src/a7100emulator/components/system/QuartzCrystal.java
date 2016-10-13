/*
 * QuartzCrystal.java
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
 *   15.06.2016 - Erste Version
 *   23.07.2016 - Kommentare ergänzt
 *   24.07.2016 - Parameter umbenannt
 *   09.08.2016 - Logger hinzugefügt
 */
package a7100emulator.components.system;

import a7100emulator.Tools.StateSavable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Klasse zur Realisierung eines Schwingquartzes.
 *
 * @author Dirk Bräuer
 */
public class QuartzCrystal implements StateSavable {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(QuartzCrystal.class.getName());

    /**
     * Frequenz des Quartzes
     */
    private final double frequency;
    /**
     * Verbleibende, nicht vollständige Takte
     */
    private double remain;

    /**
     * Erzeugt einen neuen Schwingquartz.
     *
     * @param frequency Frequenz des Quartzes in MHz
     */
    public QuartzCrystal(double frequency) {
        this.frequency = frequency;
    }

    /**
     * Berechnet die Anzahl der auszuführenden Takte für einen vorgegebenen
     * Zeitrahmen.
     *
     * @param micros Zeit in Mikrosekunden
     * @return Anzahl der Takte
     */
    public int getCycles(int micros) {
        // Addiere Anzahl der Takte für neue Zeit
        remain += frequency * micros;
        // Berechne vollständige Takte
        int cycles = (int) Math.floor(remain);
        // Ziehe vollständige Takte ab
        remain -= cycles;
        return cycles;
    }

    /**
     * Speichert den Zustand des Quartzes in einer Datei.
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war.
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        dos.writeDouble(remain);
    }

    /**
     * Lädt den Zustand des Quartzes aus einer Datei.
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Laden nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        remain = dis.readDouble();
    }
}
