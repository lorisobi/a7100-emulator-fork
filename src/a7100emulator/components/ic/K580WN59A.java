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
    
    private int state=0;
    private int ocw1=0;

    public K580WN59A() {
    }
    
    public void writeCommand(int command) {
    }

    public void writeData(int data) {
    }

    public int readStatus() {
        return state;
    }

    public int readOCW() {
        return ocw1;
    }

    private boolean getBit(int op1, int i) {
        return (((op1 >> i) & 0x1) == 0x1);
    }
}
