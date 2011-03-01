/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.modules;

import a7100emulator.Tools.AddressSpace;
import a7100emulator.Tools.Memory;
import a7100emulator.components.SystemMemory;
import a7100emulator.components.SystemPorts;
import a7100emulator.components.cpu.K1810WM86;
import java.io.File;

/**
 *
 * @author Dirk
 */
public final class ZVE implements PortModule, MemoryModule {

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
    /**
     * Komponenten
     */
    private final K1810WM86 cpu = new K1810WM86();
    private final Memory memory=new Memory(32768);

    public ZVE() {
        init();
    }

    public void init() {
        cpu.reset();
        registerPorts();
        registerMemory();
        initEPROMS();
    }

    public void registerPorts() {
        SystemPorts.getInstance().registerPort(this, PORT_ZVE_8259A_1);
        SystemPorts.getInstance().registerPort(this, PORT_ZVE_8259A_2);
        SystemPorts.getInstance().registerPort(this, PORT_ZVE_8255A_PORT_A);
        SystemPorts.getInstance().registerPort(this, PORT_ZVE_8255A_PORT_B);
        SystemPorts.getInstance().registerPort(this, PORT_ZVE_8255A_PORT_C);
        SystemPorts.getInstance().registerPort(this, PORT_ZVE_8255A_INIT);
        SystemPorts.getInstance().registerPort(this, PORT_ZVE_8253_COUNTER0);
        SystemPorts.getInstance().registerPort(this, PORT_ZVE_8253_COUNTER1);
        SystemPorts.getInstance().registerPort(this, PORT_ZVE_8253_COUNTER2);
        SystemPorts.getInstance().registerPort(this, PORT_ZVE_8253_INIT);
        SystemPorts.getInstance().registerPort(this, PORT_ZVE_8251A_DATA);
        SystemPorts.getInstance().registerPort(this, PORT_ZVE_8251A_COMMAND);
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

    public int readByte(int address) {
        return memory.readByte(address-0xF8000);
    }

    public int readWord(int address) {
        return memory.readWord(address-0xF8000);
    }

    public void registerMemory() {
        AddressSpace addressSpace = new AddressSpace(0xF8000, 0xFFFFF);
        SystemMemory.getInstance().registerMemorySpace(addressSpace, this);
    }

    public void writeByte(int address, int data) {
        throw new IllegalArgumentException("Cannot Write to ZVE-ROM");
    }

    public void writeWord(int address, int data) {
        throw new IllegalArgumentException("Cannot Write to ZVE-ROM");
    }

    private void initEPROMS() {
        File AHCL=new File("./eproms/273.ROM");
        File AWCL=new File("./eproms/274.ROM");
        File BOCL=new File("./eproms/275.ROM");
        File CGCL=new File("./eproms/276.ROM");

        memory.loadFile(0xF8000-0xF8000, CGCL, Memory.FileLoadMode.LOW_BYTE_ONLY);
        memory.loadFile(0xF8000-0xF8000, AWCL, Memory.FileLoadMode.HIGH_BYTE_ONLY);
        memory.loadFile(0xFC000-0xF8000, BOCL, Memory.FileLoadMode.LOW_BYTE_ONLY);
        memory.loadFile(0xFC000-0xF8000, AHCL, Memory.FileLoadMode.HIGH_BYTE_ONLY);
    }

    public static void main(String[] args) {
        new ZVE();
    }
}
