/*
 * Main.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   05.04.2014 Kommentare vervollständigt
 *
 */
package a7100emulator;

import a7100emulator.components.A7100;

/**
 * Hauptklasse des A7100 Emulators
 *
 * @author Dirk Bräuer
 */
public class Main {

    /**
     * Programmeinstiegspunkt. Erstellt das A7100 Objekt und startet die
     * grafische Benutzeroberfläche.
     *
     * @param args Kommandozeilenparameter
     */
    public static void main(String[] args) {
        A7100 a7100 = new A7100();
        MainView mainView = new MainView(a7100);
    }

}
