/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.modules;

import a7100emulator.Tools.Memory;
import a7100emulator.components.system.*;

/**
 *
 * @author Dirk
 */
public final class KES implements PortModule, ClockModule {

    private final int INIT_WUB_ADDRESS = 0x01000;
    private static int kes_count = 0;
    private final static int PORT_KES_1_WAKEUP_1 = 0x100;
    private final static int PORT_KES_1_WAKEUP_2 = 0x101;
    private final static int PORT_KES_2_WAKEUP_1 = 0x102;
    private final static int PORT_KES_2_WAKEUP_2 = 0x103;
    private final int kes_id;
    private int ccbAddress = 0;
    private boolean readWUB = true;
    private SystemMemory memory = SystemMemory.getInstance();
    private byte[] ccb = new byte[16];
    private byte[] cib = new byte[16];
    private byte[] iopb = new byte[30];
    private Memory sram = new Memory(0x4000);
    private AFS afs = new AFS();
    private long interruptClock = 0;
    private boolean interruptWaiting = false;

    public KES() {
        kes_id = kes_count++;
        registerPorts();
        registerClocks();
    }

    @Override
    public void registerPorts() {
        switch (kes_id) {
            case 0:
                SystemPorts.getInstance().registerPort(this, PORT_KES_1_WAKEUP_1);
                SystemPorts.getInstance().registerPort(this, PORT_KES_1_WAKEUP_2);
                break;
            case 1:
                SystemPorts.getInstance().registerPort(this, PORT_KES_2_WAKEUP_1);
                SystemPorts.getInstance().registerPort(this, PORT_KES_2_WAKEUP_2);
                break;
        }
    }

    @Override
    public void writePort_Byte(int port, int data) {
        switch (port) {
            case PORT_KES_1_WAKEUP_1:
                switch (data) {
                    case 0x00:
                        // RESET_OFF
                        System.out.println("RESET OFF");
                        readWUB = true;
                        break;
                    case 0x01:
                        // START_OPERATION
                        System.out.println("START OPERATION");
                        startOperation();
                        break;
                    case 0x02:
                        // RESET
                        System.out.println("RESET");
                        break;
                    default:
                        throw new IllegalArgumentException("Illegal Command:" + Integer.toHexString(data));
                }
                break;
            case PORT_KES_1_WAKEUP_2:
                break;
        }
    }

    @Override
    public void writePort_Word(int port, int data) {
        switch (port) {
            case PORT_KES_1_WAKEUP_1:
                break;
            case PORT_KES_1_WAKEUP_2:
                break;
        }
    }

    @Override
    public int readPort_Byte(int port) {
        switch (port) {
            case PORT_KES_1_WAKEUP_1:
            case PORT_KES_1_WAKEUP_2:
                throw new IllegalArgumentException("Cannot read from PORT:" + Integer.toHexString(port));
        }
        return 0;
    }

    @Override
    public int readPort_Word(int port) {
        switch (port) {
            case PORT_KES_1_WAKEUP_1:
            case PORT_KES_1_WAKEUP_2:
                throw new IllegalArgumentException("Cannot read from PORT:" + Integer.toHexString(port));
        }
        return 0;
    }

    @Override
    public void init() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void startOperation() {
        if (readWUB) {
            int seg = memory.readWord(INIT_WUB_ADDRESS + 4);
            int off = memory.readWord(INIT_WUB_ADDRESS + 2);
            System.out.println("WUB: " + Integer.toHexString(seg) + ":" + Integer.toHexString(off));
            ccbAddress = seg * 16 + off;
            readWUB = false;
        }
        for (int i = 0; i < 30; i++) {
            if (i < 16) {
                ccb[i] = (byte) memory.readByte(ccbAddress + i);
                cib[i] = (byte) memory.readByte(ccbAddress + i + 0x10);
            }
            iopb[i] = (byte) memory.readByte(ccbAddress + i + 0x20);
        }
        checkIOPB();
        memory.writeByte(ccbAddress + 0x01, 0x00);

        interruptWaiting = true;
        interruptClock = 0;
    }

    private void checkIOPB() {
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
                int driveNr = memory.readByte(ccbAddress + 0x2A);
                FloppyDrive drive = afs.getFloppy(driveNr & 0x03);
                int status = 0x01;
                // Typ Floppy
                status |= 0x08;
                // Laufwerksnummer
                status |= (driveNr & 0x03) << 4;
                status |= (drive.getDiskInsert()) ? 0x00 : 0xC0;
                memory.writeByte(ccbAddress + 0x13, 0xFF);
                memory.writeByte(ccbAddress + 0x11, status);
            }
            break;
            case 0x01: {
                // Statusabfrage
                int driveNr = memory.readByte(ccbAddress + 0x2A);
                int memAddr = memory.readWord(ccbAddress + 0x34) * 16 + memory.readWord(ccbAddress + 0x32);
                int status = 0x01;
                if (getBit(driveNr, 4)) {
                    FloppyDrive drive = afs.getFloppy(driveNr & 0x03);
                    // Typ Floppy
                    status |= 0x08;
                    // Laufwerksnummer
                    status |= (driveNr & 0x03) << 4;

                    // Hard Error
                    memory.writeByte(memAddr + 0x0, 0);
                    memory.writeByte(memAddr + 0x1, (drive.getDiskInsert()) ? 0x00 : 0x40);
                    status |= (drive.getDiskInsert()) ? 0x00 : 0xC0;
                    // Soft Error
                    memory.writeByte(memAddr + 0x2, 0);
                    // Verlangter Zylinder
                    memory.writeByte(memAddr + 0x3, 0);
                    memory.writeByte(memAddr + 0x4, 0);
                    // Verlangter Kopf
                    memory.writeByte(memAddr + 0x5, 0);
                    // Verlangter Sektor
                    memory.writeByte(memAddr + 0x6, 0);
                    // Aktueller Zylinder
                    memory.writeByte(memAddr + 0x7, 0);
                    memory.writeByte(memAddr + 0x8, 0);
                    // Aktueller Kopf
                    memory.writeByte(memAddr + 0x9, 0);
                    // Aktueller Sektor
                    memory.writeByte(memAddr + 0xA, 0);
                    // Anzahl der durchgeführten Wiederholungen
                    memory.writeByte(memAddr + 0xB, 0);

                    // Anzahl der Bytes
                    memory.writeByte(ccbAddress + 0x36, 0xB);
                }
                memory.writeByte(ccbAddress + 0x13, 0xFF);
                memory.writeByte(ccbAddress + 0x11, status);
            }
            break;
            case 0x02:
                // Formatieren
                break;
            case 0x03:
                // Lesen des Sektor-ID-Feldes
                break;
            case 0x04: {
                // Daten lesen
                int driveNr = memory.readByte(ccbAddress + 0x2A);
                int memSeg = memory.readWord(ccbAddress + 0x34);
                int memOff = memory.readWord(ccbAddress + 0x32);
                int memAddr = memSeg * 16 + memOff;
                int cylinder = memory.readWord(ccbAddress + 0x2E);
                int sector = memory.readByte(ccbAddress + 0x31);
                int head = memory.readByte(ccbAddress + 0x30);
                int byteCnt = memory.readWord(ccbAddress + 0x36);
                System.out.println("Lese " + byteCnt + " Bytes von Laufwerk " + (driveNr & 0x03) + " C/H/S " + cylinder + "/" + head + "/" + sector + " nach " + String.format("%04X:%04X", memSeg, memOff));
                FloppyDrive drive = afs.getFloppy(driveNr & 0x03);
                byte[] data = drive.readData(cylinder, sector, head, byteCnt);
                for (int i = 0; i < data.length; i++) {
                    memory.writeByte(memAddr + i, data[i]);
                    System.out.print(String.format("%02X", data[i] & 0xFF) + " ");
                    if ((i + 1) % 16 == 0) {
                        System.out.println();
                    }
                }
                System.out.println();
                memory.writeByte(ccbAddress + 0x13, 0xFF);
                memory.writeByte(ccbAddress + 0x11, 0x01);
            }
            break;
            case 0x05:
                // Daten zum KES-Puffer lesen
                break;
            case 0x06:
                // Daten schreiben

                break;
            case 0x07:
                // Daten aus KES-Puffer Schreiben
                break;
            case 0x08:
                // Spurpositionierung einschalten
                break;
            case 0x0C:
                // Start UA880-Programm
                break;
            case 0x0D:
                // DMA-Transfer zwischen Systemspeicher und UA-880-Subsystem Port
                break;
            case 0x0E: {
                // KES-Puffer Ein-/Ausgabe
                int kesAddr = memory.readWord(ccbAddress + 0x2E);
                int memAddr = memory.readWord(ccbAddress + 0x34) * 16 + memory.readWord(ccbAddress + 0x32);
                int cnt = memory.readWord(ccbAddress + 0x36);
                boolean toKES = memory.readByte(ccbAddress + 0x30) == 0xFF;
                for (int i = 0; i < cnt; i++) {
                    if (toKES) {
                        sram.writeByte(kesAddr + i, memory.readByte(memAddr + i));
                    } else {
                        memory.writeByte(memAddr + i, sram.readByte(kesAddr + i));
                    }
                }
                memory.writeByte(ccbAddress + 0x13, 0xFF);
                memory.writeByte(ccbAddress + 0x11, 0x01);
            }
            break;
            case 0x0F:
                // Diagnose
                memory.writeByte(ccbAddress + 0x13, 0xFF);
                memory.writeByte(ccbAddress + 0x11, 0x01);
                break;
        }
    }

    private boolean getBit(int op1, int i) {
        return (((op1 >> i) & 0x1) == 0x1);
    }

    @Override
    public void registerClocks() {
        SystemClock.getInstance().registerClock(this);
    }

    @Override
    public void clockUpdate(int amount) {
        if (interruptWaiting) {
            interruptClock += amount;
            if (interruptClock > 20) {
                interruptWaiting = false;
                InterruptSystem.getInstance().addIRInterrupt(5);
            }
        }
    }
}
