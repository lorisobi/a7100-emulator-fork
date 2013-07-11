/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.system;

import a7100emulator.components.ic.K580WN59A;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 * @author Dirk
 */
public class InterruptSystem {

    /**
     * Interrupt Anforderungen
     */
    private boolean parityNMIEnable = true;
    private static InterruptSystem instance;
    private boolean nmi = false;
    private K580WN59A pic;

    private InterruptSystem() {
    }

    public static InterruptSystem getInstance() {
        if (instance == null) {
            instance = new InterruptSystem();
        }
        return instance;
    }

    public void setPIC(K580WN59A pic) {
        this.pic = pic;
    }

    public K580WN59A getPIC() {
        return pic;
    }

    public boolean getNMI() {
        if (nmi) {
            nmi = false;
            return true;
        }
        return false;
    }

    public void addParityNMI() {
        if (parityNMIEnable) {
            nmi = true;
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

    public void saveState(DataOutputStream dos) throws IOException {
        dos.writeBoolean(parityNMIEnable);
        dos.writeBoolean(nmi);
    }

    public void loadState(DataInputStream dis) throws IOException {
        parityNMIEnable = dis.readBoolean();
        nmi = dis.readBoolean();
    }

    public void reset() {
        parityNMIEnable = false;
        nmi = false;
    }
}
