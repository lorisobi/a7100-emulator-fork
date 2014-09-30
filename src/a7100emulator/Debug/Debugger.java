/*
 * Debugger.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   05.04.2014 Kommentare vervollständigt
 *   31.08.2014 Singleton entfernt
 *
 */
package a7100emulator.Debug;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton-Klasse für Debugger der CPU
 *
 * @author Dirk Bräuer
 */
public class Debugger {

    /**
     * Ausgabedatei
     */
    private PrintStream debugFile = null;
    /**
     * Gibt an ob der Debugger aktiviert ist
     */
    private boolean debug = false;
    /**
     * Verzögerung im Debug-Modus in ms
     */
    private int slowdown = 0;
    /**
     * Adresse für automatischen Start des Debuggers
     */
    private final int debugStart = 0;//(0x1000<<4)+0x4E0F;
    /**
     * Dateiname für Debug-Ausgaben
     */
    private final String filename;
    /**
     * Gibt an ob Code-Segmente verwendet werden
     */
    private final boolean useCS;

    /**
     * Erstellt einen neuen Debugger
     */
    public Debugger(String filename, boolean useCS) {
        this.filename = filename;
        this.useCS = useCS;
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
                debugFile = new PrintStream(new FileOutputStream("./debug/" + filename + ".log"));
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        this.debug = debug;
    }

    /**
     * Fügt einen Zeile zur Debug-Ausgabe hinzu basierend auf den aktuellen
     * Debugger Informationen
     */
    public void addLine(DebuggerInfo debugInfo) {
        // Ignoriere Interrupts
//        if (debugInfo.getCs() == 0x0104) {
//            return;
//        }
        String debugString;
        if (useCS) {
            debugString = String.format("%04X:%04X [%02X] ", debugInfo.getCs(), debugInfo.getIp(), debugInfo.getOpcode()) + debugInfo.getCode();
        } else {
            debugString = String.format("%04X [%02X] ", debugInfo.getIp(), debugInfo.getOpcode()) + debugInfo.getCode();
        }
        if (debugInfo.getOperands() != null) {
            debugString += " (" + debugInfo.getOperands() + ")";
        }
        debugFile.println(debugString);
        debugFile.flush();
    }

    /**
     * Fügt einen Kommentar zur Debug-Ausgabe hinzu
     *
     * @param comment
     */
    public void addComment(String comment) {
        debugFile.println(comment);
        debugFile.flush();
    }

    /**
     * Liefert die aktuelle Verzögerung des Debuggers in ms
     *
     * @return Verzögerung
     */
    public int getSlowdown() {
        return slowdown;
    }

    /**
     * Setzt die aktuelle Verzögerung des Debuggers
     *
     * @param slowdown Verzögerung in ma
     */
    public void setSlowdown(int slowdown) {
        this.slowdown = slowdown;
    }

    /**
     * Liefert die Adresse für automatischen Start des Debuggers
     *
     * @return Startadresse
     */
    public int getDebugStart() {
        return debugStart;
    }
}
