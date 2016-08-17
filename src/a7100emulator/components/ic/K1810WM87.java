/*
 * KR1810WM87.java
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
 *   15.07.2014 - Kommentare aktualisiert
 *   09.08.2014 - Zugriffe auf SystemMemory und SystemPorts durch MMS16Bus ersetzt
 *   18.11.2014 - Interface IC implementiert
 *   23.07.2016 - Runnable entfernt, executeCycles() hinzugefügt
 *   24.07.2016 - Neue Methoden aus Interface CPU hinzugefügt
 *   28.07.2016 - Methode getDecoder() hinzugefügt
 *   09.08.2016 - Logger hinzugefügt
 */
package a7100emulator.components.ic;

import a7100emulator.Debug.Decoder;
import a7100emulator.components.system.MMS16Bus;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Klasse zur Realisierung des numerischen Coprozessors K1810WM87
 * <p>
 * TODO: Diese Klasse ist noch nicht vollständig implementiert und wird nur für
 * die Emulation des A7150 benötigt
 *
 * @author Dirk Bräuer
 */
public class K1810WM87 implements CPU {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(K1810WM87.class.getName());

    /**
     * Klasse zur Abbildung eines Stack-Registers
     */
    private class StackRegister {

        /**
         * Vorzeichen
         */
        int sign;
        /**
         * Exponent
         */
        int exponent;
        /**
         * Mantisse
         */
        long mantissa;
    }

    /**
     * MMS16 Systembus
     */
    private final MMS16Bus mms16 = MMS16Bus.getInstance();
    /**
     * Stack-Register
     */
    private final StackRegister[] stackRegisters = new StackRegister[8];
    /**
     * Status-Register
     */
    private int statusRegister;
    /**
     * Steuerwort
     */
    private int controlWord;
    /**
     * Tag-Register
     */
    private int tagRegister;

    /**
     * Erzeugt einen neuen Koprozessor
     */
    public K1810WM87() {
    }

    /**
     * Führt den nächsten Befehl aus
     */
    private void executeNextInstruction() {
    }

    /**
     * Führt die angegebene Anzahl von Zyklen aus.
     *
     * @param ticks Anzahl der Takte
     */
    @Override
    public void executeCycles(int ticks) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void loadState(DataInputStream dis) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setDebug(boolean debug) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Decoder getDecoder() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
