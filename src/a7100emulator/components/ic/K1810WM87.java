/*
 * KR1810WM87.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *
 */
package a7100emulator.components.ic;

import a7100emulator.components.system.SystemMemory;
import a7100emulator.components.system.SystemPorts;

/**
 * Klasse zur Realisierung des numerischen Coprozessors K1810WM87
 * <p>
 * TODO: Diese Klasse ist noch nicht vollständig implementiert
 *
 * @author Dirk Bräuer
 */
public class K1810WM87 implements Runnable {

    private class StackRegister {

        int sign;
        int exponent;
        long mantissa;
    }

    /**
     * Speicher
     */
    private final SystemMemory memory = SystemMemory.getInstance();
    /**
     * E/A Ports
     */
    private final SystemPorts ports = SystemPorts.getInstance();
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
     *
     */
    public K1810WM87() {
    }

    private void executeNextInstruction() {
    }

    private boolean getBit(int op1, int i) {
        return (((op1 >> i) & 0x1) == 0x1);
    }

    @Override
    public void run() {
        while (true) {
            executeNextInstruction();
        }
    }
}
