/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.ic;

/**
 *
 * @author Dirk
 */
public class K580WN59A {

    private int state = 0;
    private int icw1 = 0;
    private int icw2 = 0;
    private int icw3 = 0;
    private int icw4 = 0;
    private int ocw1 = 0;
    private int ocw2 = 0;
    private int ocw3 = 0;
    private boolean icw1Send = false;
    private boolean icw2Send = false;
    private boolean icw3Send = false;

    public K580WN59A() {
    }

    public int readStatus() {
        return state;
    }

    public int readOCW() {
        return ocw1;
    }

    public void writePort0(int data) {
        if (getBit(data, 4)) {
            // ICW1
            icw1 = data;
            icw1Send = true;
//            System.out.println("Setze ICW1 " + Integer.toBinaryString(icw1));
        } else {
            if (getBit(data, 3)) {
                // OCW3
                ocw3 = data;
//                System.out.println("Setze OCW3 " + Integer.toBinaryString(ocw3));
            } else {
                // OCW2
                ocw2 = data;
//                System.out.println("Setze OCW2 " + Integer.toBinaryString(ocw2));
            }
        }
    }

    public void writePort1(int data) {
        if (icw1Send) {
            //ICW2
            icw2 = data;
            icw2Send = true;
            icw1Send = false;
//            System.out.println("Setze ICW2 " + Integer.toBinaryString(icw2));
        } else if (icw2Send && getBit(icw1, 1)) {
            // ICW3
            icw3 = data;
            icw3Send = true;
            icw2Send = false;
//            System.out.println("Setze ICW3 " + Integer.toBinaryString(icw3));
        } else if (getBit(icw1, 0)&&(icw3Send||icw2Send)) {
            // ICW4
            icw4 = data;
            icw2Send = false;
            icw3Send=false;
//            System.out.println("Setze ICW4 " + Integer.toBinaryString(icw4));
        } else {
            // OCW1    
            ocw1 = data;
//            System.out.println("Setze OCW1 " + Integer.toBinaryString(ocw1));
        }
    }

    private boolean getBit(int op1, int i) {
        return (((op1 >> i) & 0x1) == 0x1);
    }
}
