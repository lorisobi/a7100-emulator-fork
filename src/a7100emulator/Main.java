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
 *   27.07.2016 - Kommentare ergänzt
 *              - Neue showMainView() Funktion aufgerufen
 *   07.08.2016 - Logger hinzugefügt
 */
package a7100emulator;

import a7100emulator.components.A7100;
import java.io.IOException;
import java.util.Calendar;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Diese Klasse dient ausschließlich dem Start des A7100, dem Initialisieren des
 * Loggers sowie dem Erzeugen und Anzeigen der Benutzeroberfläche.
 *
 * @author Dirk Bräuer
 */
public class Main {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    /**
     * Das ist der Programmeinstiegspunkt des A7100 Emulators. Diese Methode
     * initialisiert den Logger, erstellt das A7100 Objekt und startet die
     * grafische Benutzeroberfläche. Eventuelle Kommandozeilenparameter werden
     * von der Software ignoriert.
     *
     * @param args Kommandozeilenparameter (werden ignoriert)
     */
    public static void main(String[] args) {
        try {
            Logger.getLogger("a7100emulator").setLevel(Level.ALL);
            String logName = String.format("./log/%1$tF-A7100-Emulator.log", Calendar.getInstance());
            FileHandler fileHandler = new FileHandler(logName, true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);
            Logger.getLogger("").addHandler(fileHandler);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Kann FileHandler für Logger nicht erzeugen!", ex);
        }

        A7100 a7100 = new A7100();
        MainView mainView = new MainView(a7100);
        mainView.showMainView();
    }
}
