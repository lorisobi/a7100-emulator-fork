/*
 * GlobalClock.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   16.11.2014 - Erste Version
 *   18.11.2014 - Kommentare vervollständigt
 *
 */
package a7100emulator.components.system;

import a7100emulator.components.modules.ClockModule;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasse zur Abbildung der globalen Systemzeit. Dies ist die
 * Softwarerealisierung aller Taktgeber im System und sorgt für den parallelen,
 * zyklischen Ablauf der einzelnen Systemkomponenten
 *
 * @author Dirk Bräuer
 */
public class GlobalClock implements Runnable {

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
    private final LinkedList<ClockModule> modules = new LinkedList<ClockModule>();
    /**
     * Gibt an, ob der Zeitgeben angehalten wurde
     */
    private boolean suspended;

    /**
     * Privater Konstruktor
     */
    private GlobalClock() {
    }

    /**
     * Gibt die Singleton-Instanz zurück.
     * @return  Singleton-Instanz
     */
    public static GlobalClock getInstance() {
        if (instance == null) {
            instance = new GlobalClock();
        }
        return instance;
    }

    /**
     * Registriert ein Modul für die Änderungen an der Systemzeit
     * @param module 
     */
    public void registerModule(ClockModule module) {
        synchronized (modules) {
            modules.add(module);
        }
    }
   
    /**
     * Startet den Thread der Systemzeit
     */
    @Override
    public void run() {
        boolean stopped = false;
        while (!stopped) {
            clock++;
            for (ClockModule module : modules) {
                module.clockUpdate(1);
            }
            if ((clock / 1000) > (clock - 1) / 1000) {
                System.out.println("Globale Zeit: " + clock / 1000 + "s");
            }
            if (suspended) {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(GlobalClock.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    /**
     * Pausiert den Zeitgeber
     */
    public void pause() {
        suspended = true;
    }

    /**
     * Lässt den Zeitgeber weiterlaufen
     */
    public void resume() {
        suspended = false;
    }

}
