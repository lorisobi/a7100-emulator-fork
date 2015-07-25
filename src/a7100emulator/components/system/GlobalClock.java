/*
 * GlobalClock.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2015 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   16.11.2014 - Erste Version
 *   18.11.2014 - Kommentare vervollständigt
 *              - Speichern und Laden implementiert
 *              - Interface StateSavable implementiert
 *   19.11.2014 - Thread Funktionalität entfernt
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
 * zyklischen Ablauf der einzelnen Systemkomponenten
 *
 * @author Dirk Bräuer
 */
public class GlobalClock implements StateSavable {

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
        clock += amount;
        for (ClockModule module : modules) {
            module.clockUpdate(amount);
        }
//        if ((clock / 4915000) > (clock - amount) / 4915000) {
//            System.out.println("Globale Zeit: " + clock / 4915000 + "s");
//        }
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
    }

    /**
     * Setzt die Systemzeit zurück in den Grundzustand.
     */
    public void reset() {
        modules.clear();
        clock = 0;
    }
}
