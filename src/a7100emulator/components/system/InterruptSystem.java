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
    private int irMask = 0;
    private boolean[] irRequest = new boolean[8];
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

    public void addIRInterrupt(int ir) {
//        System.out.println("MASK: " + Integer.toBinaryString(irMask) + " IR:" + ir + " enable:" + interruptEnable);
        if (!getBit(irMask, ir)) {
            if (interruptEnable) {
                addInterrupt(32 + ir);
//                System.out.println("Füge Interrupt hinzu:" + (32 + ir));
            } else {
                irRequest[ir] = true;
            }
        }
    }

    public void setIRMask(int mask) {
        this.irMask = mask;
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
        if (!interruptEnable) {
            interruptEnable = true;
            for (int i = 0; i < 8; i++) {
                if (irRequest[i] && !getBit(irMask, i)) {
                    interrupts.add(32 + i);
                }
                irRequest[i] = false;
            }
        }
    }

    public void disableInterrupts() {
        interruptEnable = false;
        for (int i = 0; i < 8; i++) {
                irRequest[i] = false;
            }
    }

    public void enableParityNMI() {
        parityNMIEnable = true;
    }

    public void disableParityNMI() {
        parityNMIEnable = false;
    }

    private boolean getBit(int op1, int i) {
        return (((op1 >> i) & 0x1) == 0x1);
    }
}
