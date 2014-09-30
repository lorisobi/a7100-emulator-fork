/*
 * ZVE.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   02.04.2014 Kommentare vervollständigt
 *   23.07.2014 Aktualisierung Systemzeit an USART weitergeleitet
 *   09.08.2014 Zugriffe auf SystemMemory, SystemPorts und SystemClock durch MMS16Bus ersetzt
 *
 */
package a7100emulator.components.modules;

import a7100emulator.Tools.AddressSpace;
import a7100emulator.Tools.Memory;
import a7100emulator.components.ic.K1810WM86;
import a7100emulator.components.ic.K580WN59A;
import a7100emulator.components.ic.KR580WI53;
import a7100emulator.components.ic.KR580WM51A;
import a7100emulator.components.ic.KR580WW55A;
import a7100emulator.components.system.MMS16Bus;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import javax.swing.JOptionPane;

/**
 * Klasse zur Abbildung der ZVE (Zentrale Verarbeitungeinheit)
 *
 * @author Dirk
 */
public final class ZVE implements IOModule, MemoryModule, ClockModule {

    /**
     * Port 1 des Interruptcontrollers
     */
    private final static int PORT_ZVE_8259A_1 = 0xC0;

    /**
     * Port 2 des Interruptcontrollers
     */
    private final static int PORT_ZVE_8259A_2 = 0xC2;

    /**
     * Port 1 des Parallel-E/A-Schaltkreises
     */
    private final static int PORT_ZVE_8255A_PORT_A = 0xC8;

    /**
     * Port 2 des Parallel-E/A-Schaltkreises
     */
    private final static int PORT_ZVE_8255A_PORT_B = 0xCA;

    /**
     * Port 3 des Parallel-E/A-Schaltkreises
     */
    private final static int PORT_ZVE_8255A_PORT_C = 0xCC;

    /**
     * Port 4 des Parallel-E/A-Schaltkreises
     */
    private final static int PORT_ZVE_8255A_INIT = 0xCE;

    /**
     * Port 1 des Timer-Schaltkreises
     */
    private final static int PORT_ZVE_8253_COUNTER0 = 0xD0;

    /**
     * Port 2 des Timer-Schaltkreises
     */
    private final static int PORT_ZVE_8253_COUNTER1 = 0xD2;

    /**
     * Port 3 des Timer-Schaltkreises
     */
    private final static int PORT_ZVE_8253_COUNTER2 = 0xD4;

    /**
     * Port 4 des Timer-Schaltkreises
     */
    private final static int PORT_ZVE_8253_INIT = 0xD6;

    /**
     * Port 1 des USART-Schaltkreises
     */
    private final static int PORT_ZVE_8251A_DATA = 0xD8;

    /**
     * Port 2 des USART-Schaltkreises
     */
    private final static int PORT_ZVE_8251A_COMMAND = 0xDA;
    /**
     * Interruptcontroller
     */
    private final K580WN59A pic = new K580WN59A();

    /**
     * 8086 CPU
     */
    private final K1810WM86 cpu = new K1810WM86();

    /**
     * Parallel-E/A-Schaltkreis
     */
    private final KR580WW55A ppi = new KR580WW55A();

    /**
     * Timer-Schaltkreis
     */
    private final KR580WI53 pti = new KR580WI53();

    /**
     * USART-Schaltkreis
     */
    private final KR580WM51A usart = new KR580WM51A();

    /**
     * Speicher der EPROMS
     */
    private final Memory memory = new Memory(32768);

    /**
     * Erstellt eine neue ZVE
     */
    public ZVE() {
        init();
    }

    /**
     * Initialisiert die ZVE
     */
    @Override
    public void init() {
        cpu.reset();
        registerPorts();
        registerMemory();
        registerClocks();
        initEPROMS();
    }

    /**
     * Registriert die Ports der ZVE
     */
    @Override
    public void registerPorts() {
        MMS16Bus.getInstance().registerIOPort(this, PORT_ZVE_8259A_1);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ZVE_8259A_2);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ZVE_8255A_PORT_A);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ZVE_8255A_PORT_B);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ZVE_8255A_PORT_C);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ZVE_8255A_INIT);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ZVE_8253_COUNTER0);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ZVE_8253_COUNTER1);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ZVE_8253_COUNTER2);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ZVE_8253_INIT);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ZVE_8251A_DATA);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ZVE_8251A_COMMAND);
    }

    /**
     * Gibt ein Byte auf einem Port aus
     *
     * @param port Port
     * @param data Daten
     */
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

    /**
     * Gibt ein Wort auf einem Port aus
     *
     * @param port Port
     * @param data Daten
     */
    @Override
    public void writePort_Word(int port, int data) {
        System.out.println("OUT Word " + Integer.toHexString(data) + " to port " + Integer.toHexString(port));
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

    /**
     * Liest ein Byte von einem Port
     *
     * @param port Port
     * @return gelesenes Byte
     */
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

    /**
     * Liest ein Wort von einem Port
     *
     * @param port Port
     * @return gelesenes Wort
     */
    @Override
    public int readPort_Word(int port) {
        System.out.println("IN Byte from port " + Integer.toHexString(port));
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

    /**
     * Liest ein Byte aus dem Adressbereich der ZVE
     *
     * @param address Adresse
     * @return gelesenes Byte
     */
    @Override
    public int readByte(int address) {
        return memory.readByte(address - 0xF8000);
    }

    /**
     * Liest ein Wort aus dem Adressbereich der ZVE
     *
     * @param address Adresse
     * @return gelesenes Wort
     */
    @Override
    public int readWord(int address) {
        return memory.readWord(address - 0xF8000);
    }

    /**
     * Registriert den Speicherbereich im Arbeitsspeicher
     */
    @Override
    public void registerMemory() {
        AddressSpace addressSpace = new AddressSpace(0xF8000, 0xFFFFF);
        MMS16Bus.getInstance().registerMemoryModule(addressSpace, this);
    }

    /**
     * Schreibt ein Byte in den Speicher der ZVE. Diese Funktion führt zu einem
     * Fehler, da die ZVE EPROMS nicht beschreibbar sind.
     *
     * @param address Adresse
     * @param data Daten
     */
    @Override
    public void writeByte(int address, int data) {
        throw new IllegalArgumentException("Cannot Write to ZVE-ROM");
    }

    /**
     * Schreibt ein Wort in den Speicher der ZVE. Diese Funktion führt zu einem
     * Fehler, da die ZVE EPROMS nicht beschreibbar sind.
     *
     * @param address Adresse
     * @param data Daten
     */
    @Override
    public void writeWord(int address, int data) {
        throw new IllegalArgumentException("Cannot Write to ZVE-ROM");
    }

    /**
     * Initialisiert die EPROMS und lädt die entsprechenden Daten von der
     * Festplatte
     */
    private void initEPROMS() {
        // A7150
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

        // A7100
        File AHCL = new File("./eproms/ZVE-K2771.10-259.rom");
        if (!AHCL.exists()) {
            JOptionPane.showMessageDialog(null, "Eprom: " + AHCL.getName() + " nicht gefunden!", "Eprom nicht gefunden", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        File AWCL = new File("./eproms/ZVE-K2771.10-260.rom");
        if (!AWCL.exists()) {
            JOptionPane.showMessageDialog(null, "Eprom: " + AWCL.getName() + " nicht gefunden!", "Eprom nicht gefunden", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        File BOCL = new File("./eproms/ZVE-K2771.10-261.rom");
        if (!BOCL.exists()) {
            JOptionPane.showMessageDialog(null, "Eprom: " + BOCL.getName() + " nicht gefunden!", "Eprom nicht gefunden", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        File CGCL = new File("./eproms/ZVE-K2771.10-262.rom");
        if (!CGCL.exists()) {
            JOptionPane.showMessageDialog(null, "Eprom: " + CGCL.getName() + " nicht gefunden!", "Eprom nicht gefunden", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        memory.loadFile(0xF8000 - 0xF8000, CGCL, Memory.FileLoadMode.LOW_BYTE_ONLY);
        memory.loadFile(0xF8000 - 0xF8000, AWCL, Memory.FileLoadMode.HIGH_BYTE_ONLY);
        memory.loadFile(0xFC000 - 0xF8000, BOCL, Memory.FileLoadMode.LOW_BYTE_ONLY);
        memory.loadFile(0xFC000 - 0xF8000, AHCL, Memory.FileLoadMode.HIGH_BYTE_ONLY);
    }

    /**
     * Startet die CPU
     */
    public void start() {
        Thread cpuThread = new Thread(cpu, "K1810WM86");
        cpuThread.start();
    }

    /**
     * Registriert das Modul für Änderungen der Systemzeit
     */
    @Override
    public void registerClocks() {
        MMS16Bus.getInstance().registerClockModule(this);
    }

    /**
     * Verarbeitet die geänderte Systemzeit
     *
     * @param amount Anzahl der Ticks
     */
    @Override
    public void clockUpdate(int amount) {
        pti.updateClock(amount);
        usart.updateClock(amount);
    }

    /**
     * Versetz die CPU in den Pause-Modus
     */
    public void pause() {
        cpu.setSuspend(true);
    }

    /**
     * Holt die CPU aus dem Pausemodus
     */
    public void resume() {
        synchronized (cpu) {
            cpu.setSuspend(false);
            cpu.notify();
        }
    }

    /**
     * Veranlasst die CPU den nächsten Befehl abzuarbeiten
     */
    public void singleStep() {
        synchronized (cpu) {
            cpu.notify();
        }
    }

    /**
     * Schreibt den Zustand der ZVE in eine Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        memory.saveMemory(dos);
        pic.saveState(dos);
        cpu.saveState(dos);
        ppi.saveState(dos);
        pti.saveState(dos);
        usart.saveState(dos);
    }

    /**
     * Liest den Zustand der ZVE aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        memory.loadMemory(dis);
        pic.loadState(dis);
        cpu.loadState(dis);
        ppi.loadState(dis);
        pti.loadState(dis);
        usart.loadState(dis);
    }

    /**
     * Hält die CPU an
     */
    public void stopCPU() {
        cpu.stop();
    }
}
