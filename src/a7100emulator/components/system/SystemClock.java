/*
 * SystemClock.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   05.04.2014 Kommentare vervollständigt
 *
 */
package a7100emulator.components.system;

import a7100emulator.components.modules.ClockModule;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Singleton-Klasse zur Realisierung des Systemtaktgebers
 *
 * @author Dirk
 */
public class SystemClock {

    /**
     * Liste der Module, welche auf Änderungen der Systemzeit reagieren
     */
    private LinkedList<ClockModule> clockModules = new LinkedList<ClockModule>();
    /**
     * Taktzähler
     */
    private long clock = 0;
    /**
     * Instanz
     */
    private static SystemClock instance;

    /**
     * Erstellt einen neuen Taktgeber
     */
    private SystemClock() {
    }

    /**
     * Gibt die Instanz des Taktgebers zurück
     *
     * @return Instanz
     */
    public static SystemClock getInstance() {
        if (instance == null) {
            instance = new SystemClock();
        }
        return instance;
    }

    /**
     * Aktualisiert den Taktgeber und benachrichtigt die registrierten Module
     *
     * @param amount Anzahl der Ticks
     */
    public void updateClock(int amount) {
        updateModules(amount);
        clock += amount;
    }

    /**
     * Gibt den aktuellen Takt zurück
     *
     * @return Takt
     */
    public long getClock() {
        return clock;
    }

    /**
     * Registriert ein Modul für Änderungen der Systemzeit
     *
     * @param module Modul
     */
    public void registerClock(ClockModule module) {
        clockModules.add(module);
    }

    /**
     * Meldet die geänderte Systemzeit an die registrierten Module weiter
     *
     * @param amount Anzahl der Ticks
     */
    private void updateModules(int amount) {
        for (ClockModule module : clockModules) {
            module.clockUpdate(amount);
        }
    }

    /**
     * Speichert den Zustand des Taktgebers in einer Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    public void saveState(DataOutputStream dos) throws IOException {
        dos.writeLong(clock);
    }

    /**
     * Lädt den Zustand des Systemtaktgebers aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Laden nicht erfolgreich war
     */
    public void loadState(DataInputStream dis) throws IOException {
        clock = dis.readLong();
    }

    /**
     * Setzt den Systemtaktgeber zurück
     */
    public void reset() {
        clock = 0;
    }
}
