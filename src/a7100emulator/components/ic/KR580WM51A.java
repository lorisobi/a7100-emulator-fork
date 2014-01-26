/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.ic;

import a7100emulator.components.system.InterruptSystem;
import a7100emulator.components.system.Keyboard;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;

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
    private LinkedList<Byte> deviceBuffer = new LinkedList<Byte>();

    /**
     *
     */
    public KR580WM51A() {
        Keyboard.getInstance().registerController(this);
    }

    /**
     *
     * @param command
     */
    public void writeCommand(int command) {
        if (modeInstruction) {
            this.mode = command;
            modeInstruction = false;
//            System.out.print("Setze Modus:");
//            System.out.print(" Stop-Bits:" + (getBit(command, 7) ? (getBit(command, 6) ? 2 : 1.5) : (getBit(command, 6) ? 1 : -1)));
//            System.out.print(" Parität:" + (getBit(command, 4) ? (getBit(command, 5) ? "ungerade" : "gerade") : "keine"));
//            System.out.print(" Databits:" + (getBit(command, 3) ? (getBit(command, 2) ? 8 : 7) : (getBit(command, 2) ? 6 : 5)));
//            System.out.println(" Baudrate:" + (getBit(command, 1) ? (getBit(command, 0) ? "64x" : "16x") : (getBit(command, 0) ? "1x" : "Synchron")));
        } else {
            this.command = command;
//            System.out.print("Kommando:");
//            System.out.print(" Hunt-Mode:" + getBit(command, 7));
//            System.out.print(" Reset:" + getBit(command, 6));
//            System.out.print(" RTS:" + getBit(command, 5));
//            System.out.print(" Error-Reset:" + getBit(command, 4));
//            System.out.print(" Break:" + getBit(command, 3));
//            System.out.print(" Receive-Enable:" + getBit(command, 2));
//            System.out.print(" DTR:" + getBit(command, 1));
//            System.out.println(" Transmit-Enable:" + getBit(command, 0));
            if (getBit(command,6)) { // Reset
                modeInstruction = true;
                deviceBuffer.clear();
                //deviceBuffer.add((byte) 0);
                //state |= STATE_RXRDY;
                Keyboard.getInstance().receiveByte(0x00);
                //writeDataToSystem(0);
            } else {
            }
        }
        //System.out.println("Out Command " + Integer.toHexString(command) + "/" + Integer.toBinaryString(command));
    }

    /**
     *
     * @param data
     */
    public void writeDataToDevice(int data) {
//        System.out.println("Sende an Tastatur:" + String.format("%02X", data));
        Keyboard.getInstance().receiveByte(data);
        //state = 7;
    }

    /**
     *
     * @param deviceData
     */
    public void writeDataToSystem(int deviceData) {
        deviceBuffer.add((byte) (deviceData & 0xFF));
        state |= STATE_RXRDY;
        InterruptSystem.getInstance().getPIC().requestInterrupt(6);
    }

    /**
     *
     * @return
     */
    public int readStatus() {
        // XX0001XX
        //System.out.println("Lese Status " + Integer.toHexString(state) + "/" + Integer.toBinaryString(state));
        return state;
    }

    /**
     *
     * @return
     */
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
//        System.out.println("Lese Daten von USART:" + String.format("%02X",value&0xFF));
//        printBuffer();
        return value;
    }

    private void printBuffer() {
        System.out.print("Puffergröße: " + deviceBuffer.size() + "( ");
        for (byte b : deviceBuffer) {
            System.out.print(String.format("%02X ",b));
        }
        System.out.println(")");
    }

    /**
     *
     * @param bytes
     */
    public void writeDataToSystem(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            deviceBuffer.add(bytes[i]);
        }
        state |= STATE_RXRDY;
        InterruptSystem.getInstance().getPIC().requestInterrupt(6);
    }

    private boolean getBit(int op1, int i) {
        return (((op1 >> i) & 0x1) == 0x1);
    }

    /**
     *
     * @param dos
     * @throws IOException
     */
    public void saveState(DataOutputStream dos) throws IOException {
        dos.writeInt(command);
        dos.writeInt(mode);
        dos.writeInt(state);
        dos.writeBoolean(modeInstruction);
        dos.writeInt(deviceBuffer.size());
        for (int i = 0; i < deviceBuffer.size(); i++) {
            dos.writeByte(deviceBuffer.get(i));
        }
    }

    /**
     *
     * @param dis
     * @throws IOException
     */
    public void loadState(DataInputStream dis) throws IOException {
        command = dis.readInt();
        mode = dis.readInt();
        state = dis.readInt();
        modeInstruction = dis.readBoolean();
        deviceBuffer.clear();
        int size = dis.readInt();
        for (int i = 0; i < size; i++) {
            deviceBuffer.add(dis.readByte());
        }
    }
}
