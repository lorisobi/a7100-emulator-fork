/*
 * GlobalClock.java
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
 *   16.11.2014 - Erste Version
 *   18.11.2014 - Kommentare vervollständigt
 *              - Speichern und Laden implementiert
 *              - Interface StateSavable implementiert
 *   19.11.2014 - Thread Funktionalität entfernt
 *   05.06.2016 - Interface Runnable implementiert
 *              - Doppelte Typdefinitionen entfernt
 */
package a7100emulator.components.system;

import a7100emulator.Tools.StateSavable;
import a7100emulator.components.modules.ClockModule;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Klasse zur Abbildung der globalen Systemzeit. Dies ist die
 * Softwarerealisierung aller Taktgeber im System und sorgt für den parallelen,
 * zyklischen Ablauf der einzelnen Systemkomponenten.
 *
 * @author Dirk Bräuer
 */
public class GlobalClock implements Runnable, StateSavable {

    /**
     * Zyklendauer in ns TODO: Was ist hier angebracht?
     */
    private static final int CYCLE_TIME = 1000;
    /**
     * Singleton Instanz
     */
    private static GlobalClock instance;
    /**
     * Systemzeit in ms
     */
    private long clock = 0;
    /**
     * Liste mit allen Modulen, welche die globale Systemzeit verwenden
     */
    private final LinkedList<ClockModule> modules = new LinkedList<>();
    /**
     * Gibt an, ob der Zeitgeber beendet werden soll.
     */
    private boolean stopped = false;

    /**
     * Privater Konstruktor
     */
    private GlobalClock() {
    }

    /**
     * Gibt die Singleton-Instanz zurück.
     *
     * @return Singleton-Instanz
     */
    public static GlobalClock getInstance() {
        if (instance == null) {
            instance = new GlobalClock();
        }
        return instance;
    }

    /**
     * Registriert ein Modul für die Änderungen an der Systemzeit.
     *
     * @param module Zu registrierendes Modul
     */
    public void registerModule(ClockModule module) {
        modules.add(module);
    }

    /**
     * Aktualisiert die Systemzeit auf Basis der Haupt-CPU Frequenz.
     *
     * @param amount Anzahl der Ticks
     */
    public void updateClock(int amount) {
        // Zeit in ms aktualisieren
        clock += amount;
        // Skalieren auf Haupt CPU
        
        for (ClockModule module : modules) {
            module.clockUpdate(amount);
        }
    }
    

    /**
     * Speichert den Zustand der Systemzeit in einer Datei.
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        dos.writeLong(clock);
        dos.writeBoolean(stopped);
    }

    /**
     * Lädt den Zustand der Systemzeit aus einer Datei.
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Laden nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        clock = dis.readLong();
        stopped = dis.readBoolean();
    }

    /**
     * Setzt die Systemzeit zurück in den Grundzustand.
     */
    public void reset() {
        modules.clear();
        clock = 0;
        stopped = false;
    }

    /**
     * Startet den globalen Zeitgeber.
     */
    @Override
    public void run() {
        while (!stopped) {
            updateClock(10);
        }
    }

    /**
     * Beendet den Zeitgeber
     */
    public void stop() {
        stopped = true;
    }
}
