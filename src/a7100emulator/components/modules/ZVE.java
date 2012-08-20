/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.modules;

import a7100emulator.Tools.AddressSpace;
import a7100emulator.Tools.Memory;
import a7100emulator.components.ic.*;
import a7100emulator.components.system.SystemClock;
import a7100emulator.components.system.SystemMemory;
import a7100emulator.components.system.SystemPorts;
import java.io.File;

/**
 *
 * @author Dirk
 */
public final class ZVE implements PortModule, MemoryModule, ClockModule {

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
    private final KR580WW55A ppi = new KR580WW55A();
    private final KR580WI53 pti = new KR580WI53();
    private final KR580WM51A usart = new KR580WM51A();
    private final K580WN59A pic=new K580WN59A();
    private final Memory memory = new Memory(32768);

    public ZVE() {
        init();
    }

    @Override
    public void init() {
        cpu.reset();
        registerPorts();
        registerMemory();
        registerClocks();
        initEPROMS();
    }

    @Override
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

    @Override
    public void writePort_Byte(int port, int data) {
        //System.out.println("OUT Byte " + Integer.toHexString(data) + "(" + Integer.toBinaryString(data) + ")" + " to port " + Integer.toHexString(port));
        switch (port) {
            case PORT_ZVE_8259A_1:
                pic.writePort0(data);
                break;
            case PORT_ZVE_8259A_2:
                pic.writePort1(data);
                break;
            case PORT_ZVE_8255A_PORT_A:
                ppi.writePortA(data);
                break;
            case PORT_ZVE_8255A_PORT_B:
                ppi.writePortB(data);
                break;
            case PORT_ZVE_8255A_PORT_C:
                ppi.writePortC(data);
                break;
            case PORT_ZVE_8255A_INIT:
                ppi.writeInit(data);
                break;
            case PORT_ZVE_8253_COUNTER0:
                pti.writeCounter(0, data);
                break;
            case PORT_ZVE_8253_COUNTER1:
                pti.writeCounter(1, data);
                break;
            case PORT_ZVE_8253_COUNTER2:
                pti.writeCounter(2, data);
                break;
            case PORT_ZVE_8253_INIT:
                pti.writeInit(data);
                break;
            case PORT_ZVE_8251A_DATA:
                usart.writeDataToDevice(data);
                break;
            case PORT_ZVE_8251A_COMMAND:
                usart.writeCommand(data);
                break;
        }
    }

    @Override
    public void writePort_Word(int port, int data) {
//        System.out.println("OUT Word " + Integer.toHexString(data) + " to port " + Integer.toHexString(port));
        switch (port) {
            case PORT_ZVE_8259A_1:
                break;
            case PORT_ZVE_8259A_2:
                break;
            case PORT_ZVE_8255A_PORT_A:
                break;
            case PORT_ZVE_8255A_PORT_B:
                break;
            case PORT_ZVE_8255A_PORT_C:
                break;
            case PORT_ZVE_8255A_INIT:
                break;
            case PORT_ZVE_8253_COUNTER0:
                break;
            case PORT_ZVE_8253_COUNTER1:
                break;
            case PORT_ZVE_8253_COUNTER2:
                break;
            case PORT_ZVE_8253_INIT:
                break;
            case PORT_ZVE_8251A_DATA:
                break;
            case PORT_ZVE_8251A_COMMAND:
                break;
        }
    }

    @Override
    public int readPort_Byte(int port) {
        //System.out.println("IN Byte from port " + Integer.toHexString(port));
        switch (port) {
            case PORT_ZVE_8259A_1:
                return pic.readStatus();
            case PORT_ZVE_8259A_2:
                return pic.readOCW();
            case PORT_ZVE_8255A_PORT_A:
                return ppi.readPortA();
            case PORT_ZVE_8255A_PORT_B:
                return ppi.readPortB();
            case PORT_ZVE_8255A_PORT_C:
                return ppi.readPortC();
            case PORT_ZVE_8253_COUNTER0:
                return pti.readCounter(0);
            case PORT_ZVE_8253_COUNTER1:
                return pti.readCounter(1);
            case PORT_ZVE_8253_COUNTER2:
                return pti.readCounter(2);
            case PORT_ZVE_8251A_DATA:
                return usart.readFromDevice();
            case PORT_ZVE_8251A_COMMAND:
                return usart.readStatus();
            case PORT_ZVE_8255A_INIT:
            case PORT_ZVE_8253_INIT:
                throw new IllegalArgumentException("Cannot read from PORT:" + Integer.toHexString(port));
        }
        return 0;
    }

    @Override
    public int readPort_Word(int port) {
        System.out.println("IN Word from port " + Integer.toHexString(port));
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
                throw new IllegalArgumentException("Cannot read from PORT:" + Integer.toHexString(port));
        }
        return 0;
    }

    @Override
    public int readByte(int address) {
        return memory.readByte(address - 0xF8000);
    }

    @Override
    public int readWord(int address) {
        return memory.readWord(address - 0xF8000);
    }

    @Override
    public void registerMemory() {
        AddressSpace addressSpace = new AddressSpace(0xF8000, 0xFFFFF);
        SystemMemory.getInstance().registerMemorySpace(addressSpace, this);
    }

    @Override
    public void writeByte(int address, int data) {
        throw new IllegalArgumentException("Cannot Write to ZVE-ROM");
    }

    @Override
    public void writeWord(int address, int data) {
        throw new IllegalArgumentException("Cannot Write to ZVE-ROM");
    }

    private void initEPROMS() {
        // A7100
        File AHCL = new File("./eproms/259.rom");
        File AWCL = new File("./eproms/260.rom");
        File BOCL = new File("./eproms/261.rom");
        File CGCL = new File("./eproms/262.rom");

        // ACT 2.1
//        File AHCL = new File("./eproms/265.bin");
//        File AWCL = new File("./eproms/266.bin");
//        File BOCL = new File("./eproms/267.bin");
//        File CGCL = new File("./eproms/268.bin");
        // ACT2.2
//        File AHCL = new File("./eproms/269.bin");
//        File AWCL = new File("./eproms/270.bin");
//        File BOCL = new File("./eproms/271.bin");
//        File CGCL = new File("./eproms/272.bin");
        // ACT 2.3
//        File AHCL = new File("./eproms/273.ROM");
//        File AWCL = new File("./eproms/274.ROM");
//        File BOCL = new File("./eproms/275.ROM");
//        File CGCL = new File("./eproms/276.ROM");

        memory.loadFile(0xF8000 - 0xF8000, CGCL, Memory.FileLoadMode.LOW_BYTE_ONLY);
        memory.loadFile(0xF8000 - 0xF8000, AWCL, Memory.FileLoadMode.HIGH_BYTE_ONLY);
        memory.loadFile(0xFC000 - 0xF8000, BOCL, Memory.FileLoadMode.LOW_BYTE_ONLY);
        memory.loadFile(0xFC000 - 0xF8000, AHCL, Memory.FileLoadMode.HIGH_BYTE_ONLY);

    }

    public void start() {
        Thread cpuThread=new Thread(cpu);
        cpuThread.start();
    }

    @Override
    public void registerClocks() {
        SystemClock.getInstance().registerClock(this);
    }

    @Override
    public void clockUpdate(int amount) {
        pti.updateClock(amount);
    }
}
