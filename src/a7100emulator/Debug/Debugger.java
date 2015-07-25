/*
 * Debugger.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2015 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   05.04.2014 - Kommentare vervollständigt
 *   31.08.2014 - Singleton entfernt
 *   17.11.2014 - Kommentare ergänzt
 *   06.01.2015 - Globalen Debugger ergänzt
 *              - Bezeichner hinzugefügt
 *   25.07.2015 - Funktionen für Automatischen Start und Slowdown deaktiviert
 */
package a7100emulator.Debug;

import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton-Klasse für Debugger der CPU
 *
 * @author Dirk Bräuer
 */
public class Debugger {

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
     * Verzögerung im Debug-Modus in ms
     */
    // TODO: Slowdown gegenwärtig deaktiviert
    //private int slowdown = 0;
    /**
     * Adresse für automatischen Start des Debuggers
     */
    // TODO: Automatischer Start gegenwärtig deaktiviert
    //private final int debugStart = 0;//(0x1000<<4)+0x4E0F;
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
            try {
                debugFile = new FileWriter("./debug/" + filename + ".log");
            } catch (IOException ex) {
                Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Fügt einen Kommentar zur Debug-Ausgabe hinzu
     *
     * @param comment Kommentar
     */
    public void addComment(String comment) {
        try {
            debugFile.write(comment + "\n");
            debugFile.flush();
        } catch (IOException ex) {
            Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Liefert die aktuelle Verzögerung des Debuggers in ms
     *
     * @return Verzögerung
     */
    // TODO: Slowdown gegenwärtig deaktiviert
//    public int getSlowdown() {
//        return slowdown;
//    }
    /**
     * Setzt die aktuelle Verzögerung des Debuggers
     *
     * @param slowdown Verzögerung in ma
     */
    // TODO: Slowdown gegenwärtig deaktiviert
//    public void setSlowdown(int slowdown) {
//        this.slowdown = slowdown;
//    }
    /**
     * Liefert die Adresse für automatischen Start des Debuggers
     *
     * @return Startadresse
     */
    // TODO: Automatischer Start gegenwärtig deaktiviert
//    public int getDebugStart() {
//        return debugStart;
//    }
    /**
     * Gibt die globale DebuggerInstanz zurück
     *
     * @return globale Debugger Instanz
     */
    public static Debugger getGlobalInstance() {
        return globalDebugger;
    }
}
