/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.modules;

import a7100emulator.components.Ports;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Dirk
 */
public class ZVE implements Module {

    private final static int PORT_ZVE_8259A_1 = 0xC0;
    private final static int PORT_ZVE_8259A_2 = 0xC2;
    private final static int PORT_ZVE_8255A_PORT_A = 0xC8;
    private final static int PORT_ZVE_8255A_PORT_B = 0xCA;
    private final static int PORT_ZVE_8255A_PORT_C = 0xCC;
    private final static int PORT_ZVE_8255A_INIT = 0xCE;
    private final static int PORT_ZVE_8253_COUNTER0 = 0xD0;
    private final static int PORT_ZVE_8253_COUNTER1 = 0xD2;
    private final static int PORT_ZVE_8253_COUNTER2 = 0xD4;
    private final static int PORT_ZVE_8253_INIT = 0xD6;
    private final static int PORT_ZVE_8251A_DATA = 0xD8;
    private final static int PORT_ZVE_8251A_COMMAND = 0xDA;

    private RandomAccessFile prom1;
    private RandomAccessFile prom2;
    private RandomAccessFile prom3;
    private RandomAccessFile prom4;

    public ZVE() {
        try {
            prom1 = new RandomAccessFile("./eproms/273.ROM", "r");
            prom2 = new RandomAccessFile("./eproms/274.ROM", "r");
            prom3 = new RandomAccessFile("./eproms/275.ROM", "r");
            prom4 = new RandomAccessFile("./eproms/276.ROM", "r");

            FileOutputStream fos1 = new FileOutputStream("1.hex");
            FileOutputStream fos2 = new FileOutputStream("2.hex");

            for (int i = 0; i < prom1.length(); i++) {
                fos1.write(prom3.readByte());
                fos1.write(prom1.readByte());
                fos2.write(prom4.readByte());
                fos2.write(prom2.readByte());
            }

            fos1.close();
            fos2.close();

        } catch (Exception ex) {
            Logger.getLogger(ZVE.class.getName()).log(Level.SEVERE, null, ex);
        }


    }

    public static void main(String[] args) {
        new ZVE();
    }

    public void registerPorts() {
        Ports.getInstance().registerPort(this, PORT_ZVE_8259A_1);
        Ports.getInstance().registerPort(this, PORT_ZVE_8259A_2);
        Ports.getInstance().registerPort(this, PORT_ZVE_8255A_PORT_A);
        Ports.getInstance().registerPort(this, PORT_ZVE_8255A_PORT_B);
        Ports.getInstance().registerPort(this, PORT_ZVE_8255A_PORT_C);
        Ports.getInstance().registerPort(this, PORT_ZVE_8255A_INIT);
        Ports.getInstance().registerPort(this, PORT_ZVE_8253_COUNTER0);
        Ports.getInstance().registerPort(this, PORT_ZVE_8253_COUNTER1);
        Ports.getInstance().registerPort(this, PORT_ZVE_8253_COUNTER2);
        Ports.getInstance().registerPort(this, PORT_ZVE_8253_INIT);
        Ports.getInstance().registerPort(this, PORT_ZVE_8251A_DATA);
        Ports.getInstance().registerPort(this, PORT_ZVE_8251A_COMMAND);
    }

    public void writePort_Byte(int port, int data) {
        switch (port) {
            case PORT_ZVE_8259A_1:
            case PORT_ZVE_8259A_2:
            case PORT_ZVE_8255A_PORT_A:
            case PORT_ZVE_8255A_PORT_B:
            case PORT_ZVE_8255A_PORT_C:
                break;
            case PORT_ZVE_8253_COUNTER0:
            case PORT_ZVE_8253_COUNTER1:
            case PORT_ZVE_8253_COUNTER2:
                break;
            case PORT_ZVE_8251A_DATA:
            case PORT_ZVE_8251A_COMMAND:
                break;
            case PORT_ZVE_8255A_INIT:
            case PORT_ZVE_8253_INIT:
                throw new IllegalArgumentException("Cannot Write to PORT:" + Integer.toHexString(port));
        }
    }

    public void writePort_Word(int port, int data) {
        switch (port) {
            case PORT_ZVE_8259A_1:
            case PORT_ZVE_8259A_2:
            case PORT_ZVE_8255A_PORT_A:
            case PORT_ZVE_8255A_PORT_B:
            case PORT_ZVE_8255A_PORT_C:
                break;
            case PORT_ZVE_8253_COUNTER0:
            case PORT_ZVE_8253_COUNTER1:
            case PORT_ZVE_8253_COUNTER2:
                break;
            case PORT_ZVE_8251A_DATA:
            case PORT_ZVE_8251A_COMMAND:
                break;
            case PORT_ZVE_8255A_INIT:
            case PORT_ZVE_8253_INIT:
                throw new IllegalArgumentException("Cannot Write to PORT:" + Integer.toHexString(port));
        }
    }

    public int readPort_Byte(int port) {
        switch (port) {
            case PORT_ZVE_8259A_1:
            case PORT_ZVE_8259A_2:
            case PORT_ZVE_8255A_PORT_A:
            case PORT_ZVE_8255A_PORT_B:
            case PORT_ZVE_8255A_PORT_C:
            case PORT_ZVE_8255A_INIT:
                break;
            case PORT_ZVE_8253_COUNTER0:
            case PORT_ZVE_8253_COUNTER1:
            case PORT_ZVE_8253_COUNTER2:
            case PORT_ZVE_8253_INIT:
                break;
            case PORT_ZVE_8251A_DATA:
            case PORT_ZVE_8251A_COMMAND:
                break;
        }
        return 0;
    }

    public int readPort_Word(int port) {
        switch (port) {
            case PORT_ZVE_8259A_1:
            case PORT_ZVE_8259A_2:
            case PORT_ZVE_8255A_PORT_A:
            case PORT_ZVE_8255A_PORT_B:
            case PORT_ZVE_8255A_PORT_C:
            case PORT_ZVE_8255A_INIT:
                break;
            case PORT_ZVE_8253_COUNTER0:
            case PORT_ZVE_8253_COUNTER1:
            case PORT_ZVE_8253_COUNTER2:
            case PORT_ZVE_8253_INIT:
                break;
            case PORT_ZVE_8251A_DATA:
            case PORT_ZVE_8251A_COMMAND:
                break;
        }
        return 0;
    }
}
