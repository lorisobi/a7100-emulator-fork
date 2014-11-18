/*
 * KR580WW55A.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   03.04.2014 - Kommentare vervollständigt
 *   18.11.2014 - getBit durch BitTest.getBit ersetzt
 *
 */
package a7100emulator.components.ic;

import a7100emulator.Tools.BitTest;
import a7100emulator.components.system.InterruptSystem;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Klasse zur Abbildung des Parallel-E/A-Schaltkreises PPI
 *
 * @author Dirk Bräuer
 */
public class KR580WW55A {

    /**
     * Aktueller Modus des jeweiligen Ports
     */
    enum In_Out {

        /**
         * Eingabe Modus
         */
        INPUT,
        /**
         * Ausgabe Modus
         */
        OUTPUT;
    }

    /**
     * Modus Gruppe A
     */
    private int group_a_mode = 0;

    /**
     * Modus Gruppe B
     */
    private int group_b_mode = 0;

    /**
     * Modus Port C unteres Nibble
     */
    private In_Out port_c_lower_in_out = In_Out.INPUT;

    /**
     * Modus Port C oberes Nibble
     */
    private In_Out port_c_higher_in_out = In_Out.INPUT;

    /**
     * Modus Port B
     */
    private In_Out port_b_in_out = In_Out.INPUT;

    /**
     * Modus Port A
     */
    private In_Out port_a_in_out = In_Out.INPUT;

    /**
     * Aktuelle Bitkonfiguration
     */
    private int bits = 0;

    /**
     * Daten des Ports B
     */
    private int dataB = 0;

    /**
     * Setzt den Zustand des PPI
     *
     * @param control Control-Wort
     */
    public void writeInit(int control) {
//        System.out.println("Out Control: " + Integer.toHexString(control)+"/"+Integer.toBinaryString(control));
        if (BitTest.getBit(control, 7)) {
            // Configure Mode
            group_a_mode = BitTest.getBit(control, 6) ? 2 : (BitTest.getBit(control, 5) ? 1 : 0);
            group_b_mode = BitTest.getBit(control, 2) ? 1 : 0;
            port_a_in_out = BitTest.getBit(control, 4) ? In_Out.INPUT : In_Out.OUTPUT;
            port_b_in_out = BitTest.getBit(control, 1) ? In_Out.INPUT : In_Out.OUTPUT;
            port_c_lower_in_out = BitTest.getBit(control, 0) ? In_Out.INPUT : In_Out.OUTPUT;
            port_c_higher_in_out = BitTest.getBit(control, 3) ? In_Out.INPUT : In_Out.OUTPUT;
        } else {
            // Bit set Mode
            int bit = 0 + (BitTest.getBit(control, 1) ? 1 : 0) + (BitTest.getBit(control, 2) ? 2 : 0) + (BitTest.getBit(control, 3) ? 4 : 0);
            boolean oldState = BitTest.getBit(bits, bit);
            boolean newState = BitTest.getBit(control, 0);

            if (BitTest.getBit(control, 0)) {
                bits = bits | (0xFF & (0x01 << bit));
            } else {
                bits = bits & (0xFF & (0x00 << bit));
            }

            switch (bit) {
                case 0: // PB-INTR
                    //System.out.println("PB-INTR:" + (newState ? "ON" : "OFF"));
                    break;
                case 1: // SB-INTR-OUT
                    //System.out.println("SB-INTR-OUT:" + (newState ? "ON" : "OFF"));
                    break;
                case 2: // ACK
                    //System.out.println("ACK:" + (newState ? "ON" : "OFF"));
                    break;
                case 3: // STROBE
                    //System.out.println("STROBE:" + (newState ? "ON" : "OFF"));
                    break;
                case 4: // OVERRIDE
                    //System.out.println("OVERRIDE:" + (newState ? "ON" : "OFF"));
                    break;
                case 5: // SET-DC-OFF
                    //System.out.println("DC-OFF:" + (newState ? "ON" : "OFF"));
                    break;
                case 6: // TONE
                    if (!oldState && newState) {
                        //beep.play();
                        //Toolkit.getDefaultToolkit().beep();
                        //SystemMemory.getInstance().dump();
                        //System.out.println("!-B-E-E-P-!");
                    }
                    break;
                case 7: // NMI-MASK
                    //System.out.println("NMI-MASK:" + (newState ? "ON" : "OFF"));
                    if (newState) {
                        InterruptSystem.getInstance().enableParityNMI();
                    } else {
                        InterruptSystem.getInstance().disableParityNMI();
                    }
                    break;
            }
            // System.out.println("Control:" + Integer.toBinaryString(control) + " Bit:" + bit + " Bits:" + Integer.toBinaryString(bits));
        }
    }

    /**
     * Gibt ein Zeichen auf Port A aus
     *
     * @param data
     */
    public void writePortA(int data) {
    }

    /**
     * Gibt ein Zeichen auf Port B aus
     *
     * @param data Daten
     */
    public void writePortB(int data) {
        dataB = data;
    }

    /**
     * Gibt ein Zeichen auf Port C aus
     *
     * @param data Daten
     */
    public void writePortC(int data) {
    }

    /**
     * Liest ein Zeichen vom Port A
     *
     * @return
     */
    public int readPortA() {
        return 0xC0;
    }

    /**
     * Liest ein Zeichen vom Port B
     *
     * @return
     */
    public int readPortB() {
        return dataB;
    }

    /**
     * Liest ein Zeichen vom Port C
     *
     * @return
     */
    public int readPortC() {
        return 0;
    }

    /**
     * Schreibt den Zustand des PPI in eine Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    public void saveState(DataOutputStream dos) throws IOException {
        dos.writeInt(group_a_mode);
        dos.writeInt(group_b_mode);
        dos.writeUTF(port_c_lower_in_out.name());
        dos.writeUTF(port_c_higher_in_out.name());
        dos.writeUTF(port_b_in_out.name());
        dos.writeUTF(port_a_in_out.name());
        dos.writeInt(bits);
        dos.writeInt(dataB);
    }

    /**
     * Liest den Zustand des PPI aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    public void loadState(DataInputStream dis) throws IOException {
        group_a_mode = dis.readInt();
        group_b_mode = dis.readInt();
        port_c_lower_in_out = In_Out.valueOf(dis.readUTF());
        port_c_higher_in_out = In_Out.valueOf(dis.readUTF());
        port_b_in_out = In_Out.valueOf(dis.readUTF());
        port_a_in_out = In_Out.valueOf(dis.readUTF());
        bits = dis.readInt();
        dataB = dis.readInt();
    }
}
