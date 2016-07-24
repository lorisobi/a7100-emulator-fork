/*
 * KES.java
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
 *   12.04.2014 - Verarbeitung der Init Parameter
 *   09.08.2014 - Zugriffe auf SystemMemory, SystemPorts und SystemClock durch
 *                MMS16Bus ersetzt
 *   30.07.2015 - Spurpositionierung und Lesen Sektor Identifikationsfeld
 *                implementiert
 *   16.08.2015 - Geänderte Parameterreihenfolge in FloppyDrive
 *   29.11.2015 - Lokale DMA und CTC Ports ergänzt
 *   30.11.2015 - Lokale Ports vervollständigt
 *   01.12.2015 - Von SubsystemModule abgeleitet
 *              - Methoden für Port lesen/schreiben implementiert
 *              - Arbeitsspeicher geändert, Lesen von Eproms implementiert
 *   05.12.2015 - requestInterrupt hinzugefügt
 *              - Speicherzugriffe hinzugefügt
 *              - AFP hinzugefügt
 *   14.02.2016 - Alte Implementierungen entfernt
 *              - CPU hinzugefügt
 *   27.02.2016 - FlipFlop und Interrupt Ports implementiert
 *              - Lesen von Systemspeicher ermöglicht
 *   24.03.2016 - NMI Sperre ergänzt
 *   26.03.2016 - Speichern und Laden vervollständigt
 *   23.07.2016 - Quartz hinzugefügt
 */
package a7100emulator.components.modules;

import a7100emulator.Debug.MemoryAnalyzer;
import a7100emulator.Tools.BitTest;
import a7100emulator.Tools.Memory;
import a7100emulator.components.ic.UA857;
import a7100emulator.components.ic.UA858;
import a7100emulator.components.ic.UA880;
import a7100emulator.components.system.GlobalClock;
import a7100emulator.components.system.MMS16Bus;
import a7100emulator.components.system.QuartzCrystal;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * Klasse zur Abbildung des KES (Kontroller für Externspeicher)
 * <p>
 * TODO: Lesen von einseitigen Disketten funktioniert nicht.
 *
 * @author Dirk Bräuer
 */
public final class KES implements IOModule, ClockModule, SubsystemModule {

    /**
     * Anzahl der im System vorhandenen KES-Module
     */
    public static int kes_count = 0;
    /**
     * Nummer des KES-Moduls
     */
    private final int kes_id;

    /**
     * KES Speicher-Eproms
     */
    private final Memory eprom = new Memory(0x2000);
    /**
     * KES Speicher-Sram
     */
    private final Memory sram = new Memory(0x4000);

    /**
     * Adresse des 1. Wake-Up E/A-Ports 1. KES
     */
    private final static int PORT_KES_1_WAKEUP_1 = 0x100;
    /**
     * Adresse des 2. Wake-Up E/A-Ports 1. KES
     */
    private final static int PORT_KES_1_WAKEUP_2 = 0x101;
    /**
     * Adresse des 1. Wake-Up E/A-Ports 2. KES
     */
    private final static int PORT_KES_2_WAKEUP_1 = 0x102;
    /**
     * Adresse des 2. Wake-Up E/A-Ports 2. KES
     */
    private final static int PORT_KES_2_WAKEUP_2 = 0x103;

    /**
     * Lokaler Ports DMA
     */
    private final static int LOCAL_PORT_DMA_0 = 0x00;
    private final static int LOCAL_PORT_DMA_1 = 0x01;
    private final static int LOCAL_PORT_DMA_2 = 0x02;
    private final static int LOCAL_PORT_DMA_3 = 0x03;
    /**
     * Lokaler Port CTC Kanal 0
     */
    private final static int LOCAL_PORT_CTC_CHANNEL_0 = 0x04;
    /**
     * Lokaler Port CTC Kanal 1
     */
    private final static int LOCAL_PORT_CTC_CHANNEL_1 = 0x05;
    /**
     * Lokaler Port CTC Kanal 2
     */
    private final static int LOCAL_PORT_CTC_CHANNEL_2 = 0x06;
    /**
     * Lokaler Port CTC Kanal 3
     */
    private final static int LOCAL_PORT_CTC_CHANNEL_3 = 0x07;
    /**
     * Lokaler Ports Reset CA1-FlipFlop
     */
    private final static int LOCAL_PORT_RESETCA1_0 = 0x08;
    private final static int LOCAL_PORT_RESETCA1_1 = 0x09;
    private final static int LOCAL_PORT_RESETCA1_2 = 0x0A;
    private final static int LOCAL_PORT_RESETCA1_3 = 0x0B;
    /**
     * Lokaler Ports Reset CA2-FlipFlop
     */
    private final static int LOCAL_PORT_RESETCA2_0 = 0x0C;
    private final static int LOCAL_PORT_RESETCA2_1 = 0x0D;
    private final static int LOCAL_PORT_RESETCA2_2 = 0x0E;
    private final static int LOCAL_PORT_RESETCA2_3 = 0x0F;
    /**
     * Lokaler Ports Lesen Wake-Up-I/O-Port-Solladresse Low
     */
    private final static int LOCAL_PORT_WAKEUPL_0 = 0x10;
    private final static int LOCAL_PORT_WAKEUPL_1 = 0x11;
    private final static int LOCAL_PORT_WAKEUPL_2 = 0x12;
    private final static int LOCAL_PORT_WAKEUPL_3 = 0x13;
    /**
     * Lokaler Ports Lesen Wake-Up-I/O-Port-Solladresse High
     */
    private final static int LOCAL_PORT_WAKEUPH_0 = 0x14;
    private final static int LOCAL_PORT_WAKEUPH_1 = 0x15;
    private final static int LOCAL_PORT_WAKEUPH_2 = 0x16;
    private final static int LOCAL_PORT_WAKEUPH_3 = 0x17;
    /**
     * Lokaler Ports MMS16-Interrupt 1
     */
    private final static int LOCAL_PORT_SETINT1_0 = 0x18;
    private final static int LOCAL_PORT_SETINT1_1 = 0x19;
    /**
     * Lokaler Ports MMS16-Interrupt 2
     */
    private final static int LOCAL_PORT_SETINT2_0 = 0x1A;
    private final static int LOCAL_PORT_SETINT2_1 = 0x1B;
    /**
     * Lokaler Ports NMI und MAP
     */
    private final static int LOCAL_PORT_NMI_MAP_0 = 0x1C;
    private final static int LOCAL_PORT_NMI_MAP_1 = 0x1D;
    private final static int LOCAL_PORT_NMI_MAP_2 = 0x1E;
    private final static int LOCAL_PORT_NMI_MAP_3 = 0x1F;
    /**
     * Wake-Up-Adresse
     * <p>
     * TODO: Eventuell doppelt mit obigen WAKE_UP_PORTS
     */
    private final int WAKEUP_ADDRESS = 0x0100;

    /**
     * Verbindung zum MMS16 Systembus
     */
    private final MMS16Bus mms16 = MMS16Bus.getInstance();
    /**
     * Referenz auf angeschlossene AFS (Anschlußsteuerung für Folienspeicher)
     */
    private AFS afs;
    /**
     * Referenz auf angeschlossene AFP (Anschlußsteuerung für
     * Festplattenspeicher)
     */
    private AFP afp;
    /**
     * UA880 CPU des KES
     */
    private final UA880 cpu = new UA880(this, "KES");
    /**
     * UA857 CTC des KES
     */
    private UA857 ctc;
    /**
     * UA858 DMA des KES
     */
    private UA858 dma;
    /**
     * Kanal 1 Flip Flop
     */
    private boolean ca1ff = false;
    /**
     * Kanal 2 Flip Flop
     */
    private boolean ca2ff = false;
    /**
     * DNMI/MAP Register
     */
    private int dnmi_map = 0x80;
    /**
     * NMI ausstehend
     */
    private boolean nmiRequest = false;
        /**
     * Quarz-CPU Takt
     */
    private QuartzCrystal cpuClock = new QuartzCrystal(3.6864);

    /**
     * Erstellt ein neues KES-Modul
     */
    public KES() {
        kes_id = kes_count++;
        init();

        // TODO: entfernen
        //this.setDebug(true);
    }

    /**
     * Registriert die E/A Ports der KES
     */
    @Override
    public void registerPorts() {
        switch (kes_id) {
            case 0:
                MMS16Bus.getInstance().registerIOPort(this, PORT_KES_1_WAKEUP_1);
                MMS16Bus.getInstance().registerIOPort(this, PORT_KES_1_WAKEUP_2);
                break;
            case 1:
                MMS16Bus.getInstance().registerIOPort(this, PORT_KES_2_WAKEUP_1);
                MMS16Bus.getInstance().registerIOPort(this, PORT_KES_2_WAKEUP_2);
                break;
        }
    }

    /**
     * Gibt ein Byte an einem Port aus
     *
     * @param port Port
     * @param data Daten
     */
    @Override
    public void writePortByte(int port, int data) {
        switch (port) {
            case PORT_KES_1_WAKEUP_1:
                switch (data) {
                    case 0x00:
                        // RESET_OFF
                        System.out.println("RESET OFF");
                        // TODO:
                        break;
                    case 0x01:
                        // START_OPERATION
                        System.out.println("START OPERATION");
                        ca1ff = true;
                        nmiRequest = true;
                        break;
                    case 0x02:
                        // RESET
                        System.out.println("RESET ON");
                        cpu.reset();
                        dnmi_map = 0x80;
                        break;
                    default:
                        throw new IllegalArgumentException("Illegal Command:" + Integer.toHexString(data));
                }
                break;
            case PORT_KES_1_WAKEUP_2:
                break;
        }
    }

    /**
     * Gibt ein Wort an einem Port aus
     *
     * @param port Port
     * @param data Daten
     */
    @Override
    public void writePortWord(int port, int data) {
        writePortByte(port, data);
    }

    /**
     * Liest ein Byte von einem Port.
     *
     * @param port Port
     * @return gelesene Daten
     */
    @Override
    public int readPortByte(int port) {
        switch (port) {
            case PORT_KES_1_WAKEUP_1:
            case PORT_KES_1_WAKEUP_2:
                throw new IllegalArgumentException("Cannot read from PORT:" + Integer.toHexString(port));
        }
        return 0;
    }

    /**
     * Liest ein Wort von einem Port.
     *
     * @param port Port
     * @return gelesene Daten
     */
    @Override
    public int readPortWord(int port) {
        return readPortByte(port);
    }

    /**
     * Initialisiert den KES
     */
    @Override
    public void init() {
        final File kesRom1 = new File("./eproms/KES-K5170.10-P854.rom");
        if (!kesRom1.exists()) {
            JOptionPane.showMessageDialog(null, "Eprom: " + kesRom1.getName() + " nicht gefunden!", "Eprom nicht gefunden", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        final File kesRom2 = new File("./eproms/KES-K5170.10-P855.rom");
        if (!kesRom2.exists()) {
            JOptionPane.showMessageDialog(null, "Eprom: " + kesRom2.getName() + " nicht gefunden!", "Eprom nicht gefunden", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        eprom.loadFile(0x0000, kesRom1, Memory.FileLoadMode.LOW_AND_HIGH_BYTE);
        eprom.loadFile(0x0800, kesRom2, Memory.FileLoadMode.LOW_AND_HIGH_BYTE);

        afs = new AFS();
        // afp=new AFP();
        ctc = new UA857(this);
        dma = new UA858(this);

        registerPorts();
        registerClocks();
    }

    /**
     * Registiert das Modul für Änderungen der Systemzeit
     */
    @Override
    public void registerClocks() {
        GlobalClock.getInstance().registerModule(this);
    }

    /**
     * Verarbeitet Änderungen der Systemzeit. Diese Funktion lässt den UA880
     * Prozessor Befehle abarbeiten. Die Anzahl der Befehle hängt von der Anzahl
     * der ausgeführten Befehle der Haupt-CPU ab. Andere Komponenten des Systems
     * werden nicht benachrichtigt.
     *
     * @param amount Anzahl der Ticks
     */
    @Override
    public void clockUpdate(int amount) {
        int cycles = cpuClock.getCycles(amount);    
        
        // Prüfe ob NMI erlaubt und Anfrage besteht
        if (nmiRequest && !BitTest.getBit(dnmi_map, 7)) {
            nmiRequest = false;
            cpu.requestNMI();
        }
        cpu.executeCycles(cycles);
    }

    /**
     * Liest ein Byte von einem Lokalen Port
     *
     * @param port Port
     * @return gelesenes Byte
     */
    @Override
    public int readLocalPort(int port) {
        switch (port) {
            case LOCAL_PORT_DMA_0:
            case LOCAL_PORT_DMA_1:
            case LOCAL_PORT_DMA_2:
            case LOCAL_PORT_DMA_3:
                return dma.readStatus();
            case LOCAL_PORT_CTC_CHANNEL_0:
                return ctc.readChannel(0);
            case LOCAL_PORT_CTC_CHANNEL_1:
                return ctc.readChannel(1);
            case LOCAL_PORT_CTC_CHANNEL_2:
                return ctc.readChannel(2);
            case LOCAL_PORT_CTC_CHANNEL_3:
                int result = ctc.readChannel(3);
                System.out.println("Lese von Zähler 3:" + result);
                return ctc.readChannel(3);
            case LOCAL_PORT_RESETCA1_0:
            case LOCAL_PORT_RESETCA1_1:
            case LOCAL_PORT_RESETCA1_2:
            case LOCAL_PORT_RESETCA1_3:
                throw new IllegalArgumentException("Lesen von RESETCA1 Port nicht erlaubt");
            case LOCAL_PORT_RESETCA2_0:
            case LOCAL_PORT_RESETCA2_1:
            case LOCAL_PORT_RESETCA2_2:
            case LOCAL_PORT_RESETCA2_3:
                throw new IllegalArgumentException("Lesen von RESETCA2 Port nicht erlaubt");
            case LOCAL_PORT_WAKEUPL_0:
            case LOCAL_PORT_WAKEUPL_1:
            case LOCAL_PORT_WAKEUPL_2:
            case LOCAL_PORT_WAKEUPL_3:
                return (WAKEUP_ADDRESS | (ca1ff ? 0x01 : 0x00)) & 0xFF;
            case LOCAL_PORT_WAKEUPH_0:
            case LOCAL_PORT_WAKEUPH_1:
            case LOCAL_PORT_WAKEUPH_2:
            case LOCAL_PORT_WAKEUPH_3:
                return (WAKEUP_ADDRESS >> 8) & 0xFF;
            case LOCAL_PORT_SETINT1_0:
            case LOCAL_PORT_SETINT1_1:
                throw new IllegalArgumentException("Lesen von SETINT1 Port nicht erlaubt");
            case LOCAL_PORT_SETINT2_0:
            case LOCAL_PORT_SETINT2_1:
                throw new IllegalArgumentException("Lesen von SETINT2 Port nicht erlaubt");
            case LOCAL_PORT_NMI_MAP_0:
            case LOCAL_PORT_NMI_MAP_1:
            case LOCAL_PORT_NMI_MAP_2:
            case LOCAL_PORT_NMI_MAP_3:
                throw new IllegalArgumentException("Lesen von DNMI/MAP Port nicht erlaubt");
            case 0x90:
            case 0x91:
            case 0x92:
            case 0x93:
            case 0x94:
            case 0x95:
            case 0x96:
            case 0x97:
            case 0x98:
            case 0x99:
            case 0x9A:
            case 0x9B:
            case 0x9C:
            case 0x9D:
            case 0x9E:
            case 0x9F:
                return afs.readLocalPort(port);
        }
        return 0;
    }

    /**
     * Gibt ein Byte auf einem lokalen Port aus
     *
     * @param port Port
     * @param data Daten
     */
    @Override
    public void writeLocalPort(int port, int data) {
        switch (port) {
            case LOCAL_PORT_DMA_0:
            case LOCAL_PORT_DMA_1:
            case LOCAL_PORT_DMA_2:
            case LOCAL_PORT_DMA_3:
                System.out.println(String.format("Ausgabe an DMA: %02X",data));
                dma.writeControl(data);
                break;
            case LOCAL_PORT_CTC_CHANNEL_0:
//                System.out.println(String.format("Ausgabe an CTC0: %02X",data));
                ctc.writeChannel(0, data);
                break;
            case LOCAL_PORT_CTC_CHANNEL_1:
//                System.out.println(String.format("Ausgabe an CTC1: %02X",data));
                ctc.writeChannel(1, data);
                break;
            case LOCAL_PORT_CTC_CHANNEL_2:
//                System.out.println(String.format("Ausgabe an CTC2: %02X",data));
                ctc.writeChannel(2, data);
                break;
            case LOCAL_PORT_CTC_CHANNEL_3:
//                System.out.println(String.format("Ausgabe an CTC3: %02X",data));
                ctc.writeChannel(3, data);
                break;
            case LOCAL_PORT_RESETCA1_0:
            case LOCAL_PORT_RESETCA1_1:
            case LOCAL_PORT_RESETCA1_2:
            case LOCAL_PORT_RESETCA1_3:
                ca1ff = false;
                break;
            case LOCAL_PORT_RESETCA2_0:
            case LOCAL_PORT_RESETCA2_1:
            case LOCAL_PORT_RESETCA2_2:
            case LOCAL_PORT_RESETCA2_3:
                ca2ff = false;
                break;
            case LOCAL_PORT_WAKEUPL_0:
            case LOCAL_PORT_WAKEUPL_1:
            case LOCAL_PORT_WAKEUPL_2:
            case LOCAL_PORT_WAKEUPL_3:
                throw new IllegalArgumentException("Schreiben von WAKEUPL Port nicht erlaubt");
            case LOCAL_PORT_WAKEUPH_0:
            case LOCAL_PORT_WAKEUPH_1:
            case LOCAL_PORT_WAKEUPH_2:
            case LOCAL_PORT_WAKEUPH_3:
                throw new IllegalArgumentException("Schreiben von WAKEUPH Port nicht erlaubt");
            case LOCAL_PORT_SETINT1_0:
            case LOCAL_PORT_SETINT1_1:
                mms16.requestInterrupt(5);
                System.out.println("Interrupt Kanal 1");
                break;
            case LOCAL_PORT_SETINT2_0:
            case LOCAL_PORT_SETINT2_1:
                //mms16.requestInterrupt(3);
                System.out.println("Interrupt Kanal 2");
                break;
            case LOCAL_PORT_NMI_MAP_0:
            case LOCAL_PORT_NMI_MAP_1:
            case LOCAL_PORT_NMI_MAP_2:
            case LOCAL_PORT_NMI_MAP_3:
                dnmi_map = data;
                break;
            case 0x90:
            case 0x91:
            case 0x92:
            case 0x93:
            case 0x94:
            case 0x95:
            case 0x96:
            case 0x97:
            case 0x98:
            case 0x99:
            case 0x9A:
            case 0x9B:
            case 0x9C:
            case 0x9D:
            case 0x9E:
            case 0x9F:
                afs.writeLocalPort(port, data);
                break;
        }
    }

    /**
     * Liest ein Byte aus dem Arbeitsspeicher des KES.
     *
     * @param address Adresse
     * @return gelesenes Byte
     */
    @Override
    public int readLocalByte(int address) {
        if (address < 0x2000) {
            return eprom.readByte(address);
        } else if (address < 0x4000) {
            return sram.readByte(address - 0x2000);
        } else if (address < 0x5000) {
            return afs.readByte(address);
        } else if (address < 0x6000) {
            if (afp != null) {
                return afp.readByte(address);
            } else {
                return 0;
            }
        } else if (address < 0xC000) {
            System.out.println("Lesen aus nichtgenutzem KES Adressraum!");
            return 0;
        } else {
            return mms16.readMemoryByte(((dnmi_map & 0x3F) << 14) | (address & 0x3FFF));
        }
    }

    /**
     * Liest ein Wort aus dem Arbeitsspeicher des KES.
     * <p>
     * TODO: Lesen über Modulgrenzen berücksichtigen
     *
     * @param address Adresse
     * @return gelesenes Wort
     */
    @Override
    public int readLocalWord(int address) {
        if (address < 0x2000) {
            return eprom.readWord(address);
        } else if (address < 0x4000) {
            return sram.readWord(address - 0x2000);
        } else if (address < 0x5000) {
            return afs.readWord(address);
        } else if (address < 0x6000) {
            if (afp != null) {
                return afp.readWord(address);
            } else {
                return 0;
            }
        } else if (address < 0xC000) {
            System.out.println("Lesen aus nichtgenutzem KES Adressraum!");
            return 0;
        } else {
            return mms16.readMemoryWord(((dnmi_map & 0x3F) << 14) | (address & 0x3FFF));
        }
    }

    /**
     * Schreibt ein Byte in den Arbeitsspeicher des KES.
     *
     * @param address Adresse
     * @param data Daten
     */
    @Override
    public void writeLocalByte(int address, int data) {
        if (address < 0x2000) {
            System.out.println("Schreiben auf KES Eproms nicht erlaubt!");
        } else if (address < 0x4000) {
            sram.writeByte(address - 0x2000, data);
        } else if (address < 0x5000) {
            System.out.println("Schreiben auf AFS Eproms nicht erlaubt!");
        } else if (address < 0x6000) {
            System.out.println("Schreiben auf AFP Eproms nicht erlaubt!");
        } else if (address < 0xC000) {
            System.out.println("Schreiben in nicht belegten KES Adressraum!");
        } else {
            mms16.writeMemoryByte(((dnmi_map & 0x3F) << 14) | (address & 0x3FFF), data);
        }
    }

    /**
     * Schreibt ein Wort in den Arbeitsspeicher des KES.
     *
     * @param address Adresse
     * @param data Daten
     */
    @Override
    public void writeLocalWord(int address, int data) {
        if (address < 0x2000) {
            System.out.println("Schreiben auf KES Eproms nicht erlaubt!");
        } else if (address < 0x4000) {
            sram.writeWord(address - 0x2000, data);
        } else if (address < 0x5000) {
            System.out.println("Schreiben auf AFS Eproms nicht erlaubt!");
        } else if (address < 0x6000) {
            System.out.println("Schreiben auf AFP Eproms nicht erlaubt!");
        } else if (address < 0xC000) {
            System.out.println("Schreiben in nicht belegten KES Adressraum!");
        } else {
            mms16.writeMemoryWord(((dnmi_map & 0x3F) << 14) | (address & 0x3FFF), data);
        }
    }

    /**
     * Aktualisiert die Systemzeit der Komponenten des KES auf Basis der
     * Taktzyklen des UA880.
     *
     * @param cycles Anzahl der Takte
     */
    @Override
    public void localClockUpdate(int cycles) {
        ctc.updateClock(cycles);
        dma.updateClock(cycles);
        afs.updateClock(cycles);
        //afp.clockUpdate(cycles);
    }

    /**
     * Fordert einen lokalen Interrupt an
     *
     * @param i Interrupt Nummer
     */
    @Override
    public void requestInterrupt(int i) {
        cpu.requestInterrupt(i);
    }

    /**
     * Aktiviert oder deaktiviert den Debugger der CPU
     *
     * @param debug true - Aktivieren, false - Deaktivieren
     */
    public void setDebug(boolean debug) {
        cpu.setDebug(debug);
    }

    /**
     * Gibt die Referenz auf das angeschlossene AFS-Modul zurück
     *
     * @return AFS-Modul
     */
    public AFS getAFS() {
        return afs;
    }

    /**
     * Gibt die Referenz auf das angeschlossene AFP-Modul zurück
     *
     * @return AFP-Modul oder <code>null</code> wenn keine AFP vorhanden ist
     */
    public AFP getAFP() {
        return afp;
    }

    /**
     * Zeigt den Speicher des KES an.
     * <p>
     * TODO: Auch ROM und Systemfensteranzeige implementieren
     */
    public void showMemory() {
        (new MemoryAnalyzer(this, "KES Speicherbereich")).show();
    }

    /**
     * Schreibt den Inhalt des KES-RAM in eine Datei.
     * <p>
     * TODO: Auch Submodule einbeziehen
     *
     * @param filename Dateiname
     */
    public void dumpLocalMemory(String filename) {
        DataOutputStream dos;
        try {
            dos = new DataOutputStream(new FileOutputStream(filename));
            eprom.saveMemory(dos);
            sram.saveMemory(dos);
            dos.close();
        } catch (IOException ex) {
            Logger.getLogger(KES.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Leitet einen Bus Request an die CPU weiter
     * <p>
     * TODO: Überarbeiten
     *
     * @param request <code>true</code> wenn eine Anforderung vorlieg,
     * <code>false</code> sonst
     */
    @Override
    public void requestBus(boolean request) {
        cpu.requestBus(request);
        mms16.setBusRequest(request);
    }

    /**
     * Schreibt den Zustand der KES in eine Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        eprom.saveMemory(dos);
        sram.saveMemory(dos);
        afs.saveState(dos);
        if (afp != null) {
            afp.saveState(dos);
        }
        cpu.saveState(dos);
        ctc.saveState(dos);
        dma.saveState(dos);
        dos.writeBoolean(ca1ff);
        dos.writeBoolean(ca2ff);
        dos.writeInt(dnmi_map);
        dos.writeBoolean(nmiRequest);
    }

    /**
     * Liest den Zustand der KES aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        eprom.loadMemory(dis);
        sram.loadMemory(dis);
        afs.loadState(dis);
        if (afp != null) {
            afp.loadState(dis);
        }
        cpu.loadState(dis);
        ctc.loadState(dis);
        dma.loadState(dis);
        ca1ff = dis.readBoolean();
        ca2ff = dis.readBoolean();
        dnmi_map = dis.readInt();
        nmiRequest = dis.readBoolean();
    }
}
