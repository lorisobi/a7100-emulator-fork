/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.Debug;

import a7100emulator.components.ic.K1810WM86;
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
    //private int debugstart = 0xF800 * 16 + 0x3224;
    private int debugstart = -1;
    private static Debugger instance;

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
        this.debug = debug;
        if (debug) {
            try {
                debugFile = new PrintStream(new FileOutputStream("K1810WM86.log"));
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Debugger.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * @return the debugstart
     */
    public int getDebugstart() {
        return debugstart;
    }

    public void addLine(String decoderString) {
        debugFile.println(decoderString);
        debugFile.flush();
    }
}
