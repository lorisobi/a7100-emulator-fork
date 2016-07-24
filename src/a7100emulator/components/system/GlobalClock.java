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
 *   17.07.2016 - Ausführzeit in Konstante ausgelagert
 *   23.07.2016 - Kommentare überarbeitet
 *              - Methoden zum Pausieren ergänzt
 *   24.07.2016 - Synchronisation mit Systemzeit ermöglicht
 */
package a7100emulator.components.system;

import a7100emulator.Tools.StateSavable;
import a7100emulator.components.modules.ClockModule;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasse zur Abbildung der globalen Systemzeit. Dies ist die
 * Softwarerealisierung aller Taktgeber im System und sorgt für den parallelen,
 * zyklischen Ablauf der einzelnen Systemkomponenten.
 * <p>
 * TODO: - Zyklendauer sinnvoll wählen, Parallele Implementierung
 *
 * @author Dirk Bräuer
 */
public class GlobalClock implements Runnable, StateSavable {

    /**
     * Zyklendauer in Mikrosekunden
     */
    private static final int CYCLE_TIME = 10;
    /**
     * Sysnchronisierungsintervall in Millisekunden
     */
    private static final int SYNC_TIME = 40;
    /**
     * Singleton Instanz
     */
    private static GlobalClock instance;
    /**
     * Systemzeit in Mikrosekunden
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
     * Gibt an, ob der Zeitgeber pausiert ist.
     */
    private boolean suspended = false;
    /**
     * Gibt an, ob der Emulator synchronisiert zur realen Zeit läuft
     */
    private boolean synchronizeClock = true;

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
     * Aktualisiert die Systemzeit um die angegebene Zeit und meldet die
     * geänderte Zeit an alle registrierten Module weiter.
     *
     * @param micros Zeitdauer in Mikrosekunden
     */
    public void updateClock(int micros) {
        // Zeit in µs aktualisieren
        clock += micros;

        for (ClockModule module : modules) {
            module.clockUpdate(micros);
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
        dos.writeBoolean(suspended);
        dos.writeBoolean(synchronizeClock);
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
        suspended = dis.readBoolean();
        synchronizeClock = dis.readBoolean();
    }

    /**
     * Setzt die Systemzeit zurück in den Grundzustand.
     */
    public void reset() {
        modules.clear();
        clock = 0;
        stopped = false;
        suspended = false;
        synchronizeClock = true;
    }

    /**
     * Startet den globalen Zeitgeber.
     */
    @Override
    public void run() {
        // Beginne mit erster Ausführung sofort
        long nextExecutionTime = System.currentTimeMillis();

        while (!stopped) {
            try {
                if (suspended) {
                    // Wenn Emulator pausiert ist
                    synchronized (this) {
                        wait();
                        // Setze nächste Ausführungszeit auf jetzt
                        nextExecutionTime = System.currentTimeMillis();
                    }
                }

                if (synchronizeClock) {
                    long currentTime = System.currentTimeMillis();

                    // Prüfe ob ggf. noch Puffer bis zur nächsten Ausführung
                    if (currentTime < nextExecutionTime) {
                        synchronized (this) {
                            wait(nextExecutionTime - currentTime);
                        }
                    }

                    nextExecutionTime += SYNC_TIME;
                } else {
                    // Bereite für nächste Synchronisierung vor
                    nextExecutionTime = System.currentTimeMillis();
                }

                // Führe Zyklen bis zum nächsten Synchronisierungsintervall aus
                for (int cycle = 0; cycle < (SYNC_TIME * 1000) / CYCLE_TIME; cycle++) {
                    updateClock(CYCLE_TIME);
                }

//                if (clock%1000000==0) {
//                    System.out.println("Systemzeit: "+(clock/1000000)+"s");
//                }
            } catch (InterruptedException ex) {
                Logger.getLogger(GlobalClock.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Beendet den Zeitgeber
     */
    public void stop() {
        stopped = true;
    }

    /**
     * Pausiert den Zeitgeber oder lässt ihn weiterlaufen.
     *
     * @param pause <code>true</code> - wenn der Zeitgeber angehalten werden
     * soll,<code>false</code> sonst
     */
    public void setPause(boolean pause) {
        suspended = pause;
    }

    /**
     * Legt fest, ob die Emulatorzeit mit der realen Zeit synchronisiert werden
     * soll.
     *
     * @param synchronizeClock <code>true</code> falls synchronisiert wird,
     * <code>false</code> sonst
     */
    public void setSynchronizeClock(boolean synchronizeClock) {
        this.synchronizeClock = synchronizeClock;
    }
}
