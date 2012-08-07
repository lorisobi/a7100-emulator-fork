/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.ic;

import a7100emulator.components.system.Keyboard;

/**
 *
 * @author Dirk
 */
public class KR580WM51A {

    private final int STATE_TXRDY = 0x01;
    private final int STATE_RXRDY = 0x02;
    private final int STATE_TXE = 0x04;
    private int command;
    private int mode;
    private int state = 0x05;
    private boolean modeInstruction = false;
    private byte[] deviceBuffer = new byte[10];
    private int bufferPosition = 0;

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
                bufferPosition=1;
                deviceBuffer[0]=0x00;
                Keyboard.getInstance().receiveByte(0x00);
            } else {
            }
        }
//        System.out.println("Out Command " + Integer.toHexString(command) + "/" + Integer.toBinaryString(command));
    }

    public void writeDataToDevice(int data) {
        //System.out.println("Out Data " + Integer.toHexString(data) + "/" + Integer.toBinaryString(data));
        Keyboard.getInstance().receiveByte(data);
        //state = 7;
    }

    public void writeDataToSystem(byte deviceData) {
        deviceBuffer[bufferPosition++] = deviceData;
        state |= STATE_RXRDY;
    }

    public int readStatus() {
        // XX0001XX
        //System.out.println("Lese Status " + Integer.toHexString(state) + "/" + Integer.toBinaryString(state));
        return state;
    }

    public int readFromDevice() {
        int value = 0;
        if (bufferPosition != 0) {
            value = deviceBuffer[0];
            System.arraycopy(deviceBuffer, 1, deviceBuffer, 0, 9);
            bufferPosition--;
            if (bufferPosition == 0) {
                state &= ~STATE_RXRDY;
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
            deviceBuffer[bufferPosition++] = bytes[i];
        }
        state |= STATE_RXRDY;
    }
}
