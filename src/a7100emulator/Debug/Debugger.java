/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.Debug;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Dirk
 */
public class Debugger {

    private PrintStream debugFile = null;
    private boolean debug = false;
    private int debugstart = 0x812 * 16 + 0x58C;
    //private int debugstart = -1;
    private static Debugger instance;
    private final DebuggerInfo debugInfo = DebuggerInfo.getInstance();

    private Debugger() {
    }

    public static Debugger getInstance() {
        if (instance == null) {
            instance = new Debugger();
        }
        return instance;
    }

    /**
     * @return the debug
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * @param debug the debug to set
     */
    public void setDebug(boolean debug) {
        if (debug) {
            try {
                debugFile = new PrintStream(new FileOutputStream("K1810WM86.log"));
                //SystemMemory.getInstance().dump("start_debug.hex");
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        this.debug = debug;
    }

    /**
     * @return the debugstart
     */
    public int getDebugstart() {
        return debugstart;
    }

    public void addLine() {
        String debugString = String.format("%04X:%04X [%02X] ", debugInfo.getCs(), debugInfo.getIp(), debugInfo.getOpcode()) + debugInfo.getCode();
        if (debugInfo.getOperands() != null) {
            debugString += " (" + debugInfo.getOperands() + ")";
        }
        debugFile.println(debugString);
        debugFile.flush();
    }
}
