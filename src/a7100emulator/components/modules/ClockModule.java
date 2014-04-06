/*
 * ClockModule.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   02.04.2014 Kommentare vervollständigt
 *
 */
package a7100emulator.components.modules;

/**
 * Interface für Module welche auf Änderungen der Systemzeit reagieren
 *
 * @author Dirk Bräuer
 */
public interface ClockModule extends Module {

    /**
     * Registriert das Modul für Änderungen der Systemzeit
     */
    void registerClocks();

    /**
     * Verarbeitet die geänderte Systemzeit
     *
     * @param amount Anzahl der Ticks
     */
    void clockUpdate(int amount);

}
