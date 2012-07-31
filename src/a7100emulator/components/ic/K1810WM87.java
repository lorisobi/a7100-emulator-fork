/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.ic;

import a7100emulator.components.system.SystemMemory;
import a7100emulator.components.system.SystemPorts;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Dirk
 */
public class K1810WM87 implements Runnable {

    /**
     * Speicher
     */
    private SystemMemory memory = SystemMemory.getInstance();
    /**
     * E/A Ports
     */
    private SystemPorts ports = SystemPorts.getInstance();
       /**
     * Debugger
     */
    private PrintStream debugFile = null;
    private final boolean debug = true;

    public K1810WM87() {
        if (debug) {
            try {
                debugFile = new PrintStream(new FileOutputStream("K1810WM87.log"));
            } catch (Exception ex) {
                Logger.getLogger(K1810WM87.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void executeNextInstruction() {
    }


    private boolean getBit(int op1, int i) {
        return (((op1 >> i) & 0x1) == 0x1);
    }

    public void run() {
        while (true) {
            executeNextInstruction();
        }
    }
}
