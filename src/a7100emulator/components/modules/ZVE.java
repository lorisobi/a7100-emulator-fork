/*
 * ZVE.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * Copyright (c) 2011-2016 Dirk Bräuer
 *
 * Der A7100 Emulator ist Freie Software: Sie können ihn unter den Bedingungen
 * der GNU General Public License, wie von der Free Software Foundation,
 * Version 3 der Lizenz oder jeder späteren veröffentlichten Version, 
 * weiterverbreiten und/oder modifizieren.
 *
 * Der A7100 Emulator wird in der Hoffnung, dass er nützlich sein wird, aber
 * OHNE JEDE GEWÄHRLEISTUNG, bereitgestellt; sogar ohne die implizite
 * Gewährleistung der MARKTFÄHIGKEIT oder EIGNUNG FÜR EINEN BESTIMMTEN ZWECK.
 * Siehe die GNU General Public License für weitere Details.
 *
 * Sie sollten eine Kopie der GNU General Public License zusammen mit diesem
 * Programm erhalten haben. Wenn nicht, siehe <http://www.gnu.org/licenses/>.
 * 
 * Letzte Änderungen:
 *   02.04.2014 - Kommentare vervollständigt
 *   23.07.2014 - Aktualisierung Systemzeit an USART weitergeleitet
 *   09.08.2014 - Zugriffe auf SystemMemory, SystemPorts und SystemClock durch
 *                MMS16Bus ersetzt
 *   23.07.2016 - Methoden für CPU- Pausieren und Anhalten entfernt
 *   24.07.2016 - Speichern Quartz Zustand
 *   28.07.2016 - Methode getDecoder() hinzugefügt
 *   31.07.2016 - Methode getPPI() hinzugefügt
 *   07.08.2016 - Doppelte USART Ports hinzugefügt
 *              - Logger hinzugefügt und Ausgaben umgeleitet
 *   09.08.2016 - Fehler beim Laden der EPROMS abgefangen
 */
package a7100emulator.components.modules;

import a7100emulator.Debug.Decoder;
import a7100emulator.Tools.AddressSpace;
import a7100emulator.Tools.Memory;
import a7100emulator.components.ic.K1810WM86;
import a7100emulator.components.ic.K580WN59A;
import a7100emulator.components.ic.KR580WI53;
import a7100emulator.components.ic.KR580WM51A;
import a7100emulator.components.ic.KR580WW55A;
import a7100emulator.components.system.GlobalClock;
import a7100emulator.components.system.MMS16Bus;
import a7100emulator.components.system.QuartzCrystal;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * Klasse zur Abbildung der ZVE (Zentrale Verarbeitungeinheit).
 *
 * @author Dirk Bräuer
 */
public final class ZVE implements IOModule, MemoryModule, ClockModule {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(ZVE.class.getName());

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
     * Datenleitung des USART-Schaltkreises
     */
    private final static int PORT_ZVE_8251A_DATA_1 = 0xD8;

    /**
     * Datenleitung des USART-Schaltkreises - Diese nicht dokumentierte zweite
     * Adresse ergibt sich daraus, dass die Leitung AB(2) auf der ZVE nicht mit
     * dem USART verbunden ist.
     */
    private final static int PORT_ZVE_8251A_DATA_2 = 0xDC;

    /**
     * Commandleitung des USART-Schaltkreises
     */
    private final static int PORT_ZVE_8251A_COMMAND_1 = 0xDA;

    /**
     * Commandleitung des USART-Schaltkreises - Diese nicht dokumentierte zweite
     * Adresse ergibt sich daraus, dass die Leitung AB(2) auf der ZVE nicht mit
     * dem USART verbunden ist.
     */
    private final static int PORT_ZVE_8251A_COMMAND_2 = 0xDE;

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
     * Quartz CPU-Takt
     */
    private final QuartzCrystal cpuClock = new QuartzCrystal(4.9152);

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
        MMS16Bus.getInstance().registerIOPort(this, PORT_ZVE_8251A_DATA_1);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ZVE_8251A_DATA_2);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ZVE_8251A_COMMAND_1);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ZVE_8251A_COMMAND_2);
    }

    /**
     * Gibt ein Byte auf einem Port aus
     *
     * @param port Port
     * @param data Daten
     */
    @Override
    public void writePortByte(int port, int data) {
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
            case PORT_ZVE_8251A_DATA_1:
            case PORT_ZVE_8251A_DATA_2:
                usart.writeDataToDevice(data);
                break;
            case PORT_ZVE_8251A_COMMAND_1:
            case PORT_ZVE_8251A_COMMAND_2:
                usart.writeCommand(data);
                break;
            default:
                LOG.log(Level.FINE, "Schreiben auf nicht definiertem Port {0}!", String.format("0x%02X", port));
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
    public void writePortWord(int port, int data) {
        //System.out.println("OUT Word " + Integer.toHexString(data) + " to port " + Integer.toHexString(port));
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
            case PORT_ZVE_8251A_DATA_1:
            case PORT_ZVE_8251A_DATA_2:
                break;
            case PORT_ZVE_8251A_COMMAND_1:
            case PORT_ZVE_8251A_COMMAND_2:
                break;
        }
        LOG.log(Level.FINE, "Schreiben von Wörtern auf Port {0} noch nicht implementiert!", String.format("0x%02X", port));
    }

    /**
     * Liest ein Byte von einem Port
     *
     * @param port Port
     * @return gelesenes Byte
     */
    @Override
    public int readPortByte(int port) {
//        System.out.println("IN Byte from port " + Integer.toHexString(port));
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
            case PORT_ZVE_8251A_DATA_1:
            case PORT_ZVE_8251A_DATA_2:
                return usart.readFromDevice();
            case PORT_ZVE_8251A_COMMAND_1:
            case PORT_ZVE_8251A_COMMAND_2:
                return usart.readStatus();
            case PORT_ZVE_8255A_INIT:
                LOG.log(Level.FINER, "Lesen 8255A-INIT (Port {0}) nicht erlaubt!", String.format("0x%02X", port));
                break;
            case PORT_ZVE_8253_INIT:
                LOG.log(Level.FINER, "Lesen 8253-INIT (Port {0}) nicht erlaubt!", String.format("0x%02X", port));
                break;
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
    public int readPortWord(int port) {
        //System.out.println("IN Byte from port " + Integer.toHexString(port));
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
            case PORT_ZVE_8253_COUNTER0:
                break;
            case PORT_ZVE_8253_COUNTER1:
                break;
            case PORT_ZVE_8253_COUNTER2:
                break;
            case PORT_ZVE_8251A_DATA_1:
            case PORT_ZVE_8251A_DATA_2:
                break;
            case PORT_ZVE_8251A_COMMAND_1:
            case PORT_ZVE_8251A_COMMAND_2:
                break;
            case PORT_ZVE_8255A_INIT:
                LOG.log(Level.FINER, "Lesen 8255A-INIT (Port {0}) nicht erlaubt!", String.format("0x%02X", port));
                break;
            case PORT_ZVE_8253_INIT:
                LOG.log(Level.FINER, "Lesen 8253-INIT (Port {0}) nicht erlaubt!", String.format("0x%02X", port));
                break;
        }
        LOG.log(Level.FINE, "Lesen von Wörtern auf Port {0} noch nicht implementiert!", String.format("0x%02X", port));
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
        // TODO: Diese Exceptions durch richtige Fehlerbehandlung ersetzen
        LOG.log(Level.FINE, "Schreiben auf ZVE-ROM-Bereich (Adresse {0}) nicht erlaubt!", String.format("0x%05X", address));
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
        // TODO: Diese Exceptions durch richtige Fehlerbehandlung ersetzen
        LOG.log(Level.FINE, "Schreiben auf ZVE-ROM-Bereich (Adresse {0}) nicht erlaubt!", String.format("0x%05X", address));
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
            LOG.log(Level.SEVERE, "ZVE-EPROM {0} nicht gefunden!", AHCL.getPath());
            JOptionPane.showMessageDialog(null, "Eprom: " + AHCL.getName() + " nicht gefunden!", "Eprom nicht gefunden", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        File AWCL = new File("./eproms/ZVE-K2771.10-260.rom");
        if (!AWCL.exists()) {
            LOG.log(Level.SEVERE, "ZVE-EPROM {0} nicht gefunden!", AWCL.getPath());
            JOptionPane.showMessageDialog(null, "Eprom: " + AWCL.getName() + " nicht gefunden!", "Eprom nicht gefunden", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        File BOCL = new File("./eproms/ZVE-K2771.10-261.rom");
        if (!BOCL.exists()) {
            LOG.log(Level.SEVERE, "ZVE-EPROM {0} nicht gefunden!", BOCL.getPath());
            JOptionPane.showMessageDialog(null, "Eprom: " + BOCL.getName() + " nicht gefunden!", "Eprom nicht gefunden", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        File CGCL = new File("./eproms/ZVE-K2771.10-262.rom");
        if (!CGCL.exists()) {
            LOG.log(Level.SEVERE, "ZVE-EPROM {0} nicht gefunden!", CGCL.getPath());
            JOptionPane.showMessageDialog(null, "Eprom: " + CGCL.getName() + " nicht gefunden!", "Eprom nicht gefunden", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        try {
            memory.loadFile(0xF8000 - 0xF8000, CGCL, Memory.FileLoadMode.LOW_BYTE_ONLY);
            memory.loadFile(0xF8000 - 0xF8000, AWCL, Memory.FileLoadMode.HIGH_BYTE_ONLY);
            memory.loadFile(0xFC000 - 0xF8000, BOCL, Memory.FileLoadMode.LOW_BYTE_ONLY);
            memory.loadFile(0xFC000 - 0xF8000, AHCL, Memory.FileLoadMode.HIGH_BYTE_ONLY);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Fehler beim Laden der ZVE-EPROMS!", ex);
            System.exit(0);
        }
    }

    /**
     * Registriert das Modul für Änderungen der Systemzeit
     */
    @Override
    public void registerClocks() {
        GlobalClock.getInstance().registerModule(this);
    }

    /**
     * Verarbeitet die geänderte Systemzeit
     *
     * @param micros Zeitdauer in Mikrosekunden
     */
    @Override
    public void clockUpdate(int micros) {
        int cycles = cpuClock.getCycles(micros);

        //TODO: Ein und Ausgabe zwischen Bausteinen synchronisieren
        cpu.executeCycles(cycles);
        pti.updateClock(cycles);
        usart.updateClock(cycles);
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
        cpuClock.saveState(dos);
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
        cpuClock.loadState(dis);
    }

    /**
     * Aktiviert oder Deaktiviert den Debugger
     *
     * @param debug true - wenn Debugger aktiviert wird, false - sonst
     */
    public void setDebug(boolean debug) {
        cpu.setDebug(debug);
        if (debug) {
            getDecoder().clear();
        }
    }

    /**
     * Gibt die Instanz des CPU Decoders zurück.
     *
     * @return Decoderinstanz oder <code>null</code> wenn kein Decoder
     * initialisiert ist.
     */
    public Decoder getDecoder() {
        return cpu.getDecoder();
    }

    /**
     * Gibt den auf dem Modul enthaltenen Parallel E/A-Schaltkreis zurück. Dies
     * wird von dem ZPS zum setzen von Statussignalen benötigt.
     *
     * @return Referenz auf PPI
     */
    KR580WW55A getPPI() {
        return ppi;
    }
}
