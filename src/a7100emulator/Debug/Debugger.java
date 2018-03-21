/*
 * Debugger.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * Copyright (c) 2011-2018 Dirk Bräuer
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
 *   31.08.2014 - Singleton entfernt
 *   17.11.2014 - Kommentare ergänzt
 *   06.01.2015 - Globalen Debugger ergänzt
 *              - Bezeichner hinzugefügt
 *   25.07.2015 - Funktionen für Automatischen Start und Slowdown deaktiviert
 *   07.08.2016 - Logger hinzugefügt und Ausgaben umgeleitet
 *   18.03.2018 - Verzeichnis zum Speichern wird aus Konfigurationsdatei geladen
 */
package a7100emulator.Debug;

import a7100emulator.Tools.ConfigurationManager;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasse für Debugger der CPUs und den globalen Debugger.
 *
 * @author Dirk Bräuer
 */
public class Debugger {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(Debugger.class.getName());

    /**
     * Globaler Debugger
     */
    private static final Debugger globalDebugger = new Debugger("a7100", false, "");

    /**
     * Ausgabedatei
     */
    private FileWriter debugFile = null;
    /**
     * Gibt an ob der Debugger aktiviert ist
     */
    private boolean debug = false;
    /**
     * Dateiname für Debug-Ausgaben
     */
    private final String filename;
    /**
     * Gibt an ob Code-Segmente verwendet werden
     */
    private final boolean useCS;
    /**
     * Bezeichnung des Debuggers zur eindeutingen Identifizierung in globaler
     * Datei.
     */
    private final String ident;

    /**
     * Erstellt einen neuen Debugger
     *
     * @param filename Dateiname für LOG-File
     * @param useCS Gibt an, ob Codesegmente verwendet werden
     * @param ident Bezeichner des Debuggers (bspw. Modulname)
     */
    public Debugger(String filename, boolean useCS, String ident) {
        this.filename = filename;
        this.useCS = useCS;
        this.ident = ident;
    }

    /**
     * Gibt an ob der Debugger aktuell aktiv ist
     *
     * @return true - Debugger aktiv , false - Debugger inaktiv
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Startet den Debugger oder hält ihn an
     *
     * @param debug true - wenn Debugger gestartet werden soll , false - sonst
     */
    public void setDebug(boolean debug) {
        if (debug) {
            String directory = ConfigurationManager.getInstance().readString("directories", "debug", "./debug/");
            try {
                debugFile = new FileWriter(directory + filename + ".log");
            } catch (IOException ex) {
                LOG.log(Level.WARNING, "Fehler beim Erzeugen der Debug-Ausgabe!", ex);
            }
        }
        this.debug = debug;
    }

    /**
     * Fügt einen Zeile zur Debug-Ausgabe hinzu basierend auf den aktuellen
     * Debugger Informationen
     *
     * @param debugInfo Debug-Informationen
     */
    public void addLine(DebuggerInfo debugInfo) {
        try {
            String debugString;
            if (useCS) {
                debugString = String.format("%04X:%04X [%02X] ", debugInfo.getCs(), debugInfo.getIp(), debugInfo.getOpcode()) + debugInfo.getCode();
            } else {
                debugString = String.format("%04X [%02X] ", debugInfo.getIp(), debugInfo.getOpcode()) + debugInfo.getCode();
            }
            if (debugInfo.getOperands() != null) {
                debugString += " (" + debugInfo.getOperands() + ")";
            }
            if (globalDebugger.isDebug()) {
                globalDebugger.debugFile.write(ident + ": " + debugString + "\n");
                globalDebugger.debugFile.flush();
            } else {
                debugFile.write(debugString + "\n");
                debugFile.flush();
            }
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Fehler beim Schreiben der Debug-Ausgabe!", ex);
        }
    }

    /**
     * Fügt einen Kommentar zur Debug-Ausgabe hinzu.
     *
     * @param comment Kommentar
     */
    public void addComment(String comment) {
        try {
            debugFile.write(comment + "\n");
            debugFile.flush();
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Fehler beim Schreiben der Debug-Ausgabe!", ex);
        }
    }

    /**
     * Gibt die globale DebuggerInstanz zurück.
     *
     * @return globale Debugger Instanz
     */
    public static Debugger getGlobalInstance() {
        return globalDebugger;
    }
}
