/*
 * KES.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * Copyright (c) 2011-2018 Dirk Bräuer
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
 *   25.07.2016 - Erkennen von fehlerhafter Diskettenposition beim Daten lesen
 *   07.08.2016 - Logger hinzugefügt und Ausgaben umgeleitet
 *   19.12.2024 - Lese beim ersten START_OPERATION nur den WUB, ohne jedoch
 *                eine Operation auszufuehren
 *              - Ausbau der IRQ Verzoegerung ueber interruptClock, da dies
 *                zu Problemen unter SCP in Zusammenhang den Erweiterungen
 *                der PIC Emulation gefuehrt hat
 */
package a7100emulator.components.modules;

import a7100emulator.Tools.BitTest;
import a7100emulator.Tools.Memory;
import a7100emulator.components.system.FloppyDrive;
import a7100emulator.components.system.GlobalClock;
import a7100emulator.components.system.MMS16Bus;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasse zur Abbildung des KES (Kontroller für Externspeicher)
 * <p>
 * TODO: Lesen von einseitigen Disketten funktioniert nicht.
 *
 * @author Dirk Bräuer
 */
public final class KES implements IOModule, ClockModule {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(KES.class.getName());

    /**
     * Anzahl der im System vorhandenen KES-Module
     */
    public static int kes_count = 0;
    /**
     * Startadresse des Wake-Up-Blocks
     */
    private final int INIT_WUB_ADDRESS = 0x01000;
    /**
     * Lesen des Wake-Up-Blocks notwendig
     */
    private boolean readWUB = true;
    /**
     * Adresse des 1. Wake-Up E/A-Ports
     */
    private final static int[] PORT_KES_WAKEUP_1 = new int[]{0x100, 0x102};
    /**
     * Adresse des 2. Wake-Up E/A-Ports
     */
    private final static int[] PORT_KES_WAKEUP_2 = new int[]{0x101, 0x103};
    /**
     * Nummer des KES-Moduls
     */
    private final int kes_id;
    /**
     * Verbindung zum MMS16 Systembus
     */
    private final MMS16Bus mms16 = MMS16Bus.getInstance();
    /**
     * Adresse des Channel-Control-Blocks
     */
    private int ccbAddress = 0;
    /**
     * Speicher des Channel Control Blocks
     */
    private final byte[] ccb = new byte[16];
    /**
     * Speicher des Controller Invocation Blocks
     */
    private final byte[] cib = new byte[16];
    /**
     * Speicher des I/O Parameter Blocks
     */
    private final byte[] iopb = new byte[30];
    /**
     * KES Speciher
     */
    private final Memory sram = new Memory(0x4000);
    /**
     * Referenz auf angeschlossene AFS (Anschlußsteuerung für Folienspeicher)
     */
    private final AFS afs = new AFS();
    /**
     * Gibt an ob auf einen Interrupt der KES gewartet wird
     */
    private boolean interruptWaiting = false;

    /**
     * Erstellt ein neues KES-Modul
     */
    public KES() {
        kes_id = kes_count++;
        init();
    }

    /**
     * Registriert die E/A Ports der KES
     */
    @Override
    public void registerPorts() {
        MMS16Bus.getInstance().registerIOPort(this, PORT_KES_WAKEUP_1[kes_id]);
        MMS16Bus.getInstance().registerIOPort(this, PORT_KES_WAKEUP_2[kes_id]);
    }

    /**
     * Gibt ein Byte an einem Port aus
     *
     * @param port Port
     * @param data Daten
     */
    @Override
    public void writePortByte(int port, int data) {
        writePortWord(port, data);
    }

    /**
     * Gibt ein Wort an einem Port aus
     *
     * @param port Port
     * @param data Daten
     */
    @Override
    public void writePortWord(int port, int data) {
        if (port == PORT_KES_WAKEUP_1[kes_id]) {
            switch (data) {
                case 0x00:
                    // RESET_OFF
//                        System.out.println("RESET OFF");
                    interruptWaiting = false;
                    break;
                case 0x01:
                    // START_OPERATION
//                        System.out.println("START OPERATION");
					if (readWUB) {
						// erster Aufruf von START_OPERATION nach RESET_OFF
						int seg = mms16.readMemoryWord(INIT_WUB_ADDRESS + 4);
						int off = mms16.readMemoryWord(INIT_WUB_ADDRESS + 2);
						ccbAddress = (seg << 4) + off;
						readWUB = false;
					} else {
						// nachfolgende Aufrufe von START_OPERATION
						startOperation();
					}
					// setze Busy Byte zurueck
					mms16.writeMemoryByte(ccbAddress + 0x01, 0x00);
					interruptWaiting = true;
                    break;
                case 0x02:
                    // RESET
//                        System.out.println("RESET");
					readWUB = true;
                    break;
                default:
                    throw new IllegalArgumentException("Illegal Command:" + Integer.toHexString(data));
            }
        } else if (port == PORT_KES_WAKEUP_2[kes_id]) {
            LOG.log(Level.WARNING, "Schreiben auf Kanal 2 nicht implementiert!", String.format("0x%02X", port));
        } else {
            LOG.log(Level.FINE, "Schreiben auf nicht definiertem Port {0}!", String.format("0x%02X", port));
        }
    }

    /**
     * Liest ein Byte von einem Port
     *
     * @param port Port
     * @return gelesene Daten
     */
    @Override
    public int readPortByte(int port) {
        return readPortWord(port);
    }

    /**
     * Liest ein Wort von einem Port
     *
     * @param port Port
     * @return gelesene Daten
     */
    @Override
    public int readPortWord(int port) {
        if (port == PORT_KES_WAKEUP_1[kes_id] || port == PORT_KES_WAKEUP_2[kes_id]) {
            LOG.log(Level.FINE, "Lesen von Port {0} nicht erlaubt!", String.format("0x%02X", port));
        } else {
            LOG.log(Level.FINE, "Lesen von undefiniertem Port {0}!", String.format("0x%02X", port));
        }
        return 0;
    }

    /**
     * Initialisiert die KES
     */
    @Override
    public void init() {
        registerPorts();
        registerClocks();
    }

    /**
     * Startet ausgelöst durch einen Interrupt die Bearbeitung durch die KES
     */
    private void startOperation() {
        for (int i = 0; i < 30; i++) {
            if (i < 16) {
                ccb[i] = (byte) mms16.readMemoryByte(ccbAddress + i);
                cib[i] = (byte) mms16.readMemoryByte(ccbAddress + i + 0x10);
            }
            iopb[i] = (byte) mms16.readMemoryByte(ccbAddress + i + 0x20);
        }
        checkIOPB();
    }

    /**
     * Führt die Operationen entsprechend den Daten des I/O Parameter Blocks
     * durch.
     * <p>
     * TODO: Modifizierung ergänzen, fehlende Funktionen implementieren
     */
    private void checkIOPB() {
        // TODO: Festplatten Laufwerke überall prüfen
//        System.out.println("20-23 (leer): " + Integer.toHexString(iopb[0]) + "," + Integer.toHexString(iopb[1]) + "," + Integer.toHexString(iopb[2]) + "," + Integer.toHexString(iopb[3]));
//        System.out.println("24-25 (Zähler/Firmware): " + Integer.toHexString(iopb[4]) + "," + Integer.toHexString(iopb[5]));
//        System.out.println("26-27 (Leer/Firmware): " + Integer.toHexString(iopb[6]) + "," + Integer.toHexString(iopb[7]));
//        System.out.println("28-29 (Gerätecode): " + Integer.toHexString(iopb[8]) + "," + Integer.toHexString(iopb[9]));
//        System.out.println("2A (Laufwerk-Nr.): " + Integer.toHexString(iopb[10]));
//        System.out.println("2B (Funktionscode): " + Integer.toHexString(iopb[11]));
//        System.out.println("2C-2D (Modifizierung): " + Integer.toHexString(iopb[12]) + "," + Integer.toHexString(iopb[13]));
//        System.out.println("2E-2F (Zylindernummer): " + Integer.toHexString(iopb[14]) + "," + Integer.toHexString(iopb[15]));
//        System.out.println("30 (Kopfnummer): " + Integer.toHexString(iopb[16]));
//        System.out.println("31 (Sektornummer): " + Integer.toHexString(iopb[17]));
//        System.out.println("32-35 (Datenpufferadresse): " + Integer.toHexString(iopb[18]) + "," + Integer.toHexString(iopb[19]) + "," + Integer.toHexString(iopb[20]) + "," + Integer.toHexString(iopb[21]));
//        System.out.println("36-37 (Anzahl Bytes): " + Integer.toHexString(iopb[22]) + "," + Integer.toHexString(iopb[23]));
//        System.out.println("38-39 (ignorieren): " + Integer.toHexString(iopb[24]) + "," + Integer.toHexString(iopb[25]));
//        System.out.println("3A-3D (Allgemeiner Adresspointer): " + Integer.toHexString(iopb[26]) + "," + Integer.toHexString(iopb[27]) + "," + Integer.toHexString(iopb[28]) + "," + Integer.toHexString(iopb[29]));
        switch (iopb[0x0B]) {
            case 0x00: {
                // Intialisierung
                int driveNr = mms16.readMemoryByte(ccbAddress + 0x2A);
                int deviceCode = mms16.readMemoryByte(ccbAddress + 0x28);
                int status = 0;
                int memAddr = (mms16.readMemoryWord(ccbAddress + 0x34) << 4) + mms16.readMemoryWord(ccbAddress + 0x32);
                int cylinder = mms16.readMemoryWord(memAddr);
                int fixedHeads = mms16.readMemoryByte(memAddr + 2);
                int moveHeads = mms16.readMemoryByte(memAddr + 3);
                int sectorsPerTrack = mms16.readMemoryByte(memAddr + 4);
                int bytesPerSector = mms16.readMemoryWord(memAddr + 5);
                int replaceCylinders = mms16.readMemoryByte(memAddr + 7);

//                System.out.println("cyl:" + cylinder);
//                System.out.println("fixedHead:" + fixedHeads);
//                System.out.println("moveHead:" + moveHeads);
//                System.out.println("sectorsPerTrack:" + sectorsPerTrack);
//                System.out.println("bytesPerSector:" + bytesPerSector);
//                System.out.println("replaceCylinders:"+replaceCylinders);
//                System.out.println("Initialisiere Laufwerk " + deviceCode);
                switch (deviceCode) {
                    case 0x00:
                    case 0x02:
                        // Festplatte
                        status = 0x01;
                        status |= 0xC0;
                        break;
                    case 0x03:
                        // 5.25" Floppy
                        FloppyDrive drive = afs.getFloppy(driveNr & 0x03);
                        status = 0x01;
                        // Typ Floppy
                        status |= 0x08;
                        // Laufwerksnummer
                        status |= (driveNr & 0x03) << 4;
                        status |= (drive.isDiskInsert()) ? 0x00 : 0xC0;

                        drive.setCylinder(cylinder & 0x7FFF);
                        drive.setDoubleStep(BitTest.getBit(cylinder, 15));
                        drive.setPrecompensation(fixedHeads & 0x3);
                        drive.setReduceCurrent((fixedHeads >> 2) & 0x3);
                        drive.setHeadSink((fixedHeads >> 6) & 0x1);
                        drive.setHeads(moveHeads);
                        drive.setSectorsPerTrack(sectorsPerTrack);
                        drive.setBytesPerSector(bytesPerSector);
                        drive.setMfmMode(BitTest.getBit(replaceCylinders, 0));
                        drive.setStepTime((replaceCylinders >> 1) & 0x7);
                        drive.setHeadTime((replaceCylinders >> 4) & 0x15);
                        break;
                }
                mms16.writeMemoryByte(ccbAddress + 0x13, 0xFF);
                mms16.writeMemoryByte(ccbAddress + 0x11, status);
            }
            break;
            case 0x01: {
                // Statusabfrage
                int driveNr = mms16.readMemoryByte(ccbAddress + 0x2A);
                int memAddr = (mms16.readMemoryWord(ccbAddress + 0x34) << 4) + mms16.readMemoryWord(ccbAddress + 0x32);
                int status = 0x01;
                if (BitTest.getBit(driveNr, 4)) {
                    FloppyDrive drive = afs.getFloppy(driveNr & 0x03);
                    // Typ Floppy
                    status |= 0x08;
                    // Laufwerksnummer
                    status |= (driveNr & 0x03) << 4;
                    // Hard Error
                    mms16.writeMemoryByte(memAddr + 0x0, 0);
                    mms16.writeMemoryByte(memAddr + 0x1, (drive.isDiskInsert()) ? 0x00 : 0x40);
                    status |= (drive.isDiskInsert()) ? 0x00 : 0xC0;
                    // Soft Error
                    mms16.writeMemoryByte(memAddr + 0x2, 0);
                    // Verlangter Zylinder
                    mms16.writeMemoryByte(memAddr + 0x3, 0);
                    mms16.writeMemoryByte(memAddr + 0x4, 0);
                    // Verlangter Kopf
                    mms16.writeMemoryByte(memAddr + 0x5, 0);
                    // Verlangter Sektor
                    mms16.writeMemoryByte(memAddr + 0x6, 0);
                    // Aktueller Zylinder
                    mms16.writeMemoryByte(memAddr + 0x7, 0);
                    mms16.writeMemoryByte(memAddr + 0x8, 0);
                    // Aktueller Kopf
                    mms16.writeMemoryByte(memAddr + 0x9, 0);
                    // Aktueller Sektor
                    mms16.writeMemoryByte(memAddr + 0xA, 0);
                    // Anzahl der durchgeführten Wiederholungen
                    mms16.writeMemoryByte(memAddr + 0xB, 0);

                    // Anzahl der Bytes
                    mms16.writeMemoryByte(ccbAddress + 0x36, 0xB);
                }
                mms16.writeMemoryByte(ccbAddress + 0x13, 0xFF);
                mms16.writeMemoryByte(ccbAddress + 0x11, status);
            }
            break;
            case 0x02: {
                // Formatieren
                int driveNr = mms16.readMemoryByte(ccbAddress + 0x2A);
                int memSeg = mms16.readMemoryWord(ccbAddress + 0x34);
                int memOff = mms16.readMemoryWord(ccbAddress + 0x32);
                int memAddr = (memSeg << 4) + memOff;
                int cylinder = mms16.readMemoryWord(ccbAddress + 0x2E);
                int head = mms16.readMemoryByte(ccbAddress + 0x30);
                int mod = mms16.readMemoryByte(ccbAddress + 0x2C);
                int[] data = new int[]{mms16.readMemoryByte(memAddr + 1), mms16.readMemoryByte(memAddr + 2), mms16.readMemoryByte(memAddr + 3), mms16.readMemoryByte(memAddr + 4)};
                int interleave = mms16.readMemoryByte(memAddr + 5);

                FloppyDrive drive = afs.getFloppy(driveNr & 0x03);
//                System.out.println("Formatiere Laufwerk " + (driveNr & 0x03) + " C/H " + cylinder + "/" + head);
//                System.out.println("Datenbytes: " + String.format("%02X %02X %02X %02X", mms16.readMemoryByte(memAddr + 1), mms16.readMemoryByte(memAddr + 2), mms16.readMemoryByte(memAddr + 3), mms16.readMemoryByte(memAddr + 4)) + " Interleave: " + String.format("%02X", mms16.readMemoryByte(memAddr + 5)));
//                System.out.println("Modifizierung: " + Integer.toBinaryString(mod));
                drive.format(cylinder, head, mod, data, interleave);
                mms16.writeMemoryByte(ccbAddress + 0x13, 0xFF);
                mms16.writeMemoryByte(ccbAddress + 0x11, 0x01);
            }
            break;
            case 0x03: {
                // Lesen des Sektor-ID-Feldes
                int driveNr = mms16.readMemoryByte(ccbAddress + 0x2A);
                int deviceCode = mms16.readMemoryByte(ccbAddress + 0x28);
                int mod = mms16.readMemoryByte(ccbAddress + 0x2C);
                int memSeg = mms16.readMemoryWord(ccbAddress + 0x34);
                int memOff = mms16.readMemoryWord(ccbAddress + 0x32);
                int memAddr = (memSeg << 4) + memOff;
                int status = 0;

                switch (deviceCode) {
                    case 0x00:
                    case 0x02:
                        status = 0x01;
                        status |= 0xC0;
                        break;
                    case 0x03:
                        FloppyDrive drive = afs.getFloppy(driveNr & 0x03);
                        byte[] sectorID = drive.readSectorID();
                        for (int i = 0; i < sectorID.length; i++) {
                            mms16.writeMemoryWord(memAddr + i, sectorID[i]);
                        }
                        status = 0x01;
                        break;
                }
                mms16.writeMemoryByte(ccbAddress + 0x13, 0xFF);
                mms16.writeMemoryByte(ccbAddress + 0x11, status);
            }
            break;
            case 0x04: {
                // Daten lesen
                int deviceCode = mms16.readMemoryByte(ccbAddress + 0x28);
                int driveNr = mms16.readMemoryByte(ccbAddress + 0x2A);
                int memSeg = mms16.readMemoryWord(ccbAddress + 0x34);
                int memOff = mms16.readMemoryWord(ccbAddress + 0x32);
                int memAddr = (memSeg << 4) + memOff;
                int cylinder = mms16.readMemoryWord(ccbAddress + 0x2E);
                int sector = mms16.readMemoryByte(ccbAddress + 0x31);
                int head = mms16.readMemoryByte(ccbAddress + 0x30);
                int byteCnt = mms16.readMemoryWord(ccbAddress + 0x36);
                int status = 0;
//                System.out.println("Lese " + byteCnt + " Bytes von Laufwerk " + (driveNr & 0x03) + " C/H/S " + cylinder + "/" + head + "/" + sector + " nach " + String.format("%04X:%04X", memSeg, memOff));

                switch (deviceCode) {
                    case 0x00:
                    case 0x02:
                        status = 0x01;
                        status |= 0xC0;
                        break;
                    case 0x03:
                        FloppyDrive drive = afs.getFloppy(driveNr & 0x03);
                        byte[] data = drive.readData(cylinder, head, sector, byteCnt);
//                        String ascii = "";
                        if (data != null) {
                            for (int i = 0; i < data.length; i++) {
                                mms16.writeMemoryByte(memAddr + i, data[i]);
//                            System.out.print(String.format("%02X", data[i] & 0xFF) + " ");
//                            ascii += ((data[i] < 0x20) || (data[i] == 127)) ? '.' : (char) (data[i] & 0xFF);
//                            if ((i + 1) % 16 == 0) {
//                                System.out.println(" " + ascii);
//                                ascii = "";
                            }
                        }
//                        }
//                        System.out.println();
                        status = 0x01;
                        break;
                }
                mms16.writeMemoryByte(ccbAddress + 0x13, 0xFF);
                mms16.writeMemoryByte(ccbAddress + 0x11, status);
            }
            break;
            case 0x05:
                // Daten zum KES-Puffer lesen
                LOG.log(Level.WARNING, "Daten zum KES-Puffer noch nicht implementiert");
                break;
            case 0x06: {
                // Daten schreiben
                int driveNr = mms16.readMemoryByte(ccbAddress + 0x2A);
                int memSeg = mms16.readMemoryWord(ccbAddress + 0x34);
                int memOff = mms16.readMemoryWord(ccbAddress + 0x32);
                int memAddr = (memSeg << 4) + memOff;
                int cylinder = mms16.readMemoryWord(ccbAddress + 0x2E);
                int sector = mms16.readMemoryByte(ccbAddress + 0x31);
                int head = mms16.readMemoryByte(ccbAddress + 0x30);
                int byteCnt = mms16.readMemoryWord(ccbAddress + 0x36);
                FloppyDrive drive = afs.getFloppy(driveNr & 0x03);
                byte[] data = new byte[byteCnt];
                for (int i = 0; i < data.length; i++) {
                    data[i] = (byte) mms16.readMemoryByte(memAddr + i);
                }
                drive.writeData(cylinder, head, sector, data);
                mms16.writeMemoryByte(ccbAddress + 0x13, 0xFF);
                mms16.writeMemoryByte(ccbAddress + 0x11, 0x01);
            }
            break;
            case 0x07:
                // Daten aus KES-Puffer Schreiben
                LOG.log(Level.WARNING, "Daten von KES-Puffer noch nicht implementiert");
                break;
            case 0x08: {
                // Spurpositionierung einschalten
                int driveNr = mms16.readMemoryByte(ccbAddress + 0x2A);
                int deviceCode = mms16.readMemoryByte(ccbAddress + 0x28);
                int mod = mms16.readMemoryByte(ccbAddress + 0x2C);
                int cylinder = mms16.readMemoryWord(ccbAddress + 0x2E);
                int head = mms16.readMemoryByte(ccbAddress + 0x30);
                int status = 0;

                switch (deviceCode) {
                    case 0x00:
                    case 0x02:
                        status = 0x01;
                        status |= 0xC0;
                        break;
                    case 0x03:
                        FloppyDrive drive = afs.getFloppy(driveNr & 0x03);
                        drive.setTrackPosition(cylinder, head);
                        // Status für erfolgreiches Positionieren
                        // TODO: Implementieren, dass erst Operation beendet und dann Positionierung beendet gemeldet wird
                        status = 0x02;
                        break;
                }
                mms16.writeMemoryByte(ccbAddress + 0x13, 0xFF);
                mms16.writeMemoryByte(ccbAddress + 0x11, status);
            }
            break;
            case 0x0C:
                // Start UA880-Programm
                LOG.log(Level.WARNING, "UA880 noch nicht implementiert");
                break;
            case 0x0D:
                // DMA-Transfer zwischen Systemspeicher und UA-880-Subsystem Port
                LOG.log(Level.WARNING, "DMA-Transfer noch nicht implementiert");
                break;
            case 0x0E: {
                // KES-Puffer Ein-/Ausgabe
                int kesAddr = mms16.readMemoryWord(ccbAddress + 0x2E);
                int memAddr = (mms16.readMemoryWord(ccbAddress + 0x34) << 4) + mms16.readMemoryWord(ccbAddress + 0x32);
                int cnt = mms16.readMemoryWord(ccbAddress + 0x36);
                boolean toKES = mms16.readMemoryByte(ccbAddress + 0x30) == 0xFF;
                for (int i = 0; i < cnt; i++) {
                    if (toKES) {
                        sram.writeByte(kesAddr + i, mms16.readMemoryByte(memAddr + i));
                    } else {
                        mms16.writeMemoryByte(memAddr + i, sram.readByte(kesAddr + i));
                    }
                }
                mms16.writeMemoryByte(ccbAddress + 0x13, 0xFF);
                mms16.writeMemoryByte(ccbAddress + 0x11, 0x01);
            }
            break;
            case 0x0F:
                // Diagnose
                mms16.writeMemoryByte(ccbAddress + 0x13, 0xFF);
                mms16.writeMemoryByte(ccbAddress + 0x11, 0x01);
                break;
        }
    }

    /**
     * Registiert das Modul für Änderungen der Systemzeit
     */
    @Override
    public void registerClocks() {
        //MMS16Bus.getInstance().registerClockModule(this);
        GlobalClock.getInstance().registerModule(this);
    }

    /**
     * Verarbeitet die geänderte Systemzeit
     *
     * @param amount Anzahl der Ticks
     */
    @Override
    public void clockUpdate(int amount) {
        if (interruptWaiting) {
			interruptWaiting = false;
			MMS16Bus.getInstance().requestInterrupt(5);
        }
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
     * Schreibt den Zustand der KES in eine Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        dos.writeInt(ccbAddress);
        dos.writeBoolean(readWUB);
        dos.write(ccb);
        dos.write(cib);
        dos.write(iopb);
        sram.saveMemory(dos);
        afs.saveState(dos);
        dos.writeBoolean(interruptWaiting);
    }

    /**
     * Liest den Zustand der KES aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        ccbAddress = dis.readInt();
        readWUB = dis.readBoolean();
        dis.read(ccb);
        dis.read(cib);
        dis.read(iopb);
        sram.loadMemory(dis);
        afs.loadState(dis);
        interruptWaiting = dis.readBoolean();
    }
}
