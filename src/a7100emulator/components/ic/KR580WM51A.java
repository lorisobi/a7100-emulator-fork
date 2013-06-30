/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.ic;

import a7100emulator.components.system.InterruptSystem;
import a7100emulator.components.system.Keyboard;
import java.io.Serializable;
import java.util.LinkedList;

/**
 *
 * @author Dirk
 */
public class KR580WM51A implements Serializable {

    private final int STATE_TXRDY = 0x01;
    private final int STATE_RXRDY = 0x02;
    private final int STATE_TXE = 0x04;
    private int command;
    private int mode;
    private int state = 0x05;
    private boolean modeInstruction = false;
    private LinkedList<Byte> deviceBuffer = new LinkedList<Byte>();

    public KR580WM51A() {
        Keyboard.getInstance().registerController(this);
    }

    public void writeCommand(int command) {
        if (modeInstruction) {
            this.mode = command;
            modeInstruction = false;
        } else {
            this.command = command;
            if (command == 0x40) { // Reset
                modeInstruction = true;
                deviceBuffer.clear();
                deviceBuffer.add((byte) 0);
                Keyboard.getInstance().receiveByte(0x00);
            } else {
            }
        }
        //System.out.println("Out Command " + Integer.toHexString(command) + "/" + Integer.toBinaryString(command));
    }

    public void writeDataToDevice(int data) {
        //System.out.println("Out Data " + Integer.toHexString(data) + "/" + Integer.toBinaryString(data));
        Keyboard.getInstance().receiveByte(data);
        //state = 7;
    }

    public void writeDataToSystem(byte deviceData) {
        deviceBuffer.add(deviceData);
        state |= STATE_RXRDY;
        InterruptSystem.getInstance().getPIC().requestInterrupt(6);
    }

    public int readStatus() {
        // XX0001XX
        //System.out.println("Lese Status " + Integer.toHexString(state) + "/" + Integer.toBinaryString(state));
        return state;
    }

    public int readFromDevice() {
        int value = 0;
        if (!deviceBuffer.isEmpty()) {
            value = deviceBuffer.poll();
            if (deviceBuffer.isEmpty()) {
                state &= ~STATE_RXRDY;
            } else {
                InterruptSystem.getInstance().getPIC().requestInterrupt(6);
            }
        }
        //System.out.println("Lese Daten " + Integer.toHexString((byte) value) + "/" + Integer.toBinaryString((byte) value));
        return value;
    }

    private boolean getBit(int op1, int i) {
        return (((op1 >> i) & 0x1) == 0x1);
    }

    public void writeDataToSystem(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            deviceBuffer.add(bytes[i]);
        }
        state |= STATE_RXRDY;
        InterruptSystem.getInstance().getPIC().requestInterrupt(6);
    }
}
