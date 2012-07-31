/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.ic;

/**
 *
 * @author Dirk
 */
public class KR580WW55A {

    enum In_Out {

        INPUT, OUTPUT;
    }
    private int group_a_mode = 0;
    private int group_b_mode = 0;
    private In_Out port_c_lower_in_out = In_Out.INPUT;
    private In_Out port_c_higher_in_out = In_Out.INPUT;
    private In_Out port_b_in_out = In_Out.INPUT;
    private In_Out port_a_in_out = In_Out.INPUT;
    private int bits = 0;
    //private Beep beep=new Beep();

    public void writeInit(int control) {
        if (getBit(control, 7)) {
            // Configure Mode
            group_a_mode = getBit(control, 6) ? 2 : (getBit(control, 5) ? 1 : 0);
            group_b_mode = getBit(control, 2) ? 1 : 0;
            port_a_in_out = getBit(control, 4) ? In_Out.INPUT : In_Out.OUTPUT;
            port_b_in_out = getBit(control, 1) ? In_Out.INPUT : In_Out.OUTPUT;
            port_c_lower_in_out = getBit(control, 0) ? In_Out.INPUT : In_Out.OUTPUT;
            port_c_higher_in_out = getBit(control, 3) ? In_Out.INPUT : In_Out.OUTPUT;
        } else {
            // Bit set Mode
            int bit = 0 + (getBit(control, 1) ? 1 : 0) + (getBit(control, 2) ? 2 : 0) + (getBit(control, 3) ? 4 : 0);
            boolean oldState = getBit(bits, bit);
            boolean newState = getBit(control, 0);

            if (getBit(control, 0)) {
                bits = bits | (0xFF & (0x01 << bit));
            } else {
                bits = bits & (0xFF & (0x00 << bit));
            }

            switch (bit) {
                case 0: // PB-INTR
                    System.out.println("PB-INTR:" + (newState ? "ON" : "OFF"));
                    break;
                case 1: // SB-INTR-OUT
                    System.out.println("SB-INTR-OUT:" + (newState ? "ON" : "OFF"));
                    break;
                case 2: // ACK
                    System.out.println("ACK:" + (newState ? "ON" : "OFF"));
                    break;
                case 3: // STROBE
                    System.out.println("STROBE:" + (newState ? "ON" : "OFF"));
                    break;
                case 4: // OVERRIDE
                    System.out.println("OVERRIDE:" + (newState ? "ON" : "OFF"));
                    break;
                case 5: // SET-DC-OFF
                    System.out.println("DC-OFF:" + (newState ? "ON" : "OFF"));
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
                    System.out.println("NMI-MASK:" + (newState ? "ON" : "OFF"));
                    break;
            }
            // System.out.println("Control:" + Integer.toBinaryString(control) + " Bit:" + bit + " Bits:" + Integer.toBinaryString(bits));
        }
    }

    public void writePortA(int data) {
    }

    public void writePortB(int data) {
    }

    public void writePortC(int data) {
    }

    public int readPortA() {
        return 0;
    }

    public int readPortB() {
        return 0;
    }

    public int readPortC() {
        return 0;
    }

    private boolean getBit(int op1, int i) {
        return (((op1 >> i) & 0x1) == 0x1);
    }
}
