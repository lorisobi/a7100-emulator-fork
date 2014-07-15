/*
 * KR1810WM87.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   15.07.2014 Kommentare aktualisiert
 */
package a7100emulator.components.ic;

import a7100emulator.components.system.SystemMemory;
import a7100emulator.components.system.SystemPorts;

/**
 * Klasse zur Realisierung des numerischen Coprozessors K1810WM87
 * <p>
 * TODO: Diese Klasse ist noch nicht vollständig implementiert und wird nur für
 * die Emulation des A7150 benötigt
 *
 * @author Dirk Bräuer
 */
public class K1810WM87 implements Runnable {

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
     * Startet den CPU-Thread
     */
    @Override
    public void run() {
        while (true) {
            executeNextInstruction();
        }
    }

    /**
     * Prüft ob ein Bit des Operanden gesetzt ist
     *
     * @param op Operand
     * @param i Nummer des Bits
     * @return true - wenn das Bit gesetzt ist, false - sonst
     */
    private boolean getBit(int op, int i) {
        return (((op >> i) & 0x1) == 0x1);
    }
}
