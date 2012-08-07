/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.system;

import java.util.LinkedList;

/**
 *
 * @author Dirk
 */
public class InterruptSystem {

    /**
     * Interrupt Anforderungen
     */
    private LinkedList<Integer> interrupts = new LinkedList<Integer>();
    private boolean interruptEnable;
    private boolean parityNMIEnable = true;
    private static InterruptSystem instance;
    
    private InterruptSystem() {
    }
    
    public static InterruptSystem getInstance() {
        if (instance == null) {
            instance = new InterruptSystem();
        }
        return instance;
    }
    
    public void addParityNMI() {
        if (parityNMIEnable) {
            interrupts.add(0x02);
        }
    }
    
    public void addInterrupt(int interruptID) {
        if (interruptEnable) {
            interrupts.add(interruptID);
        }
    }
    
    public int getNextInterrupt() {
        if (interrupts.size() > 0) {
            return interrupts.poll();
        } else {
            return -1;
        }
    }
    
    public void enableInterrupts() {
        interruptEnable = true;
    }
    
    public void disableInterrupts() {
        interruptEnable = false;
    }
    
    public void enableParityNMI() {
        parityNMIEnable = true;
    }
    
    public void disableParityNMI() {
        parityNMIEnable = false;
    }
}
