/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.modules;

import a7100emulator.components.Ports;

/**
 *
 * @author Dirk
 */
public final class KES implements Module {

    private static int kes_count = 0;
    private final static int PORT_KES_1_WAKEUP_1 = 0x100;
    private final static int PORT_KES_1_WAKEUP_2 = 0x101;
    private final static int PORT_KES_2_WAKEUP_1 = 0x102;
    private final static int PORT_KES_2_WAKEUP_2 = 0x103;
    private final int kes_id;

    public KES() {
        kes_id = kes_count++;
        registerPorts();
    }

    public void registerPorts() {
        switch (kes_id) {
            case 0:
                Ports.getInstance().registerPort(this, PORT_KES_1_WAKEUP_1);
                Ports.getInstance().registerPort(this, PORT_KES_1_WAKEUP_2);
                break;
            case 1:
                Ports.getInstance().registerPort(this, PORT_KES_2_WAKEUP_1);
                Ports.getInstance().registerPort(this, PORT_KES_2_WAKEUP_2);
                break;
        }
    }

    public void writePort_Byte(int port, int data) {
        switch (port) {
            case PORT_KES_1_WAKEUP_1:
                break;
            case PORT_KES_1_WAKEUP_2:
                break;
        }
    }

    public void writePort_Word(int port, int data) {
        switch (port) {
            case PORT_KES_1_WAKEUP_1:
                break;
            case PORT_KES_1_WAKEUP_2:
                break;
        }
    }

    public int readPort_Byte(int port) {
        switch (port) {
            case PORT_KES_1_WAKEUP_1:
            case PORT_KES_1_WAKEUP_2:
                throw new IllegalArgumentException("Cannot read from PORT:" + Integer.toHexString(port));
        }
        return 0;
    }

    public int readPort_Word(int port) {
        switch (port) {
            case PORT_KES_1_WAKEUP_1:
            case PORT_KES_1_WAKEUP_2:
                throw new IllegalArgumentException("Cannot read from PORT:" + Integer.toHexString(port));
        }
        return 0;
    }
}
