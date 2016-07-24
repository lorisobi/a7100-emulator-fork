/*
 * Main.java
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
 *   05.04.2014 - Kommentare vervollständigt
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
