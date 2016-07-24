/*
 * UA858.java
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
 *   01.12.2015 - Erstellt
 *   06.03.2016 - Lesen Register und Zähler ergänzt
 *   14.03.2016 - Implementierung der Übertragung
 *   25.03.2016 - Speichern und Lesen des Zustands implementiert
 */
package a7100emulator.components.ic;

import a7100emulator.Tools.BitTest;
import a7100emulator.components.modules.SubsystemModule;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Klasse zur Realisierung des UA858 DMA
 *
 * @author Dirk Bräuer
 */
public class UA858 implements IC {

    /**
     * Registersatz Schreiben Haupt Register: WR0:0, WR1:5, WR2:7, WR3:9,
     * WR4:12, WR5:18, WR6:19
     * <pre>
     * 00 - WR0
     * 01 - WR0 - Port A Adresse LOW
     * 02 - WR0 - Port A Adresse HIGH
     * 03 - WR0 - Block Length LOW
     * 04 - WR0 - Block Length HIGH
     * 05 - WR1
     * 06 - WR1 - Port A Timing
     * 07 - WR2
     * 08 - WR2 - Port B Timing
     * 09 - WR3
     * 10 - WR3 - MASK Byte
     * 11 - WR3 - Match Byte
     * 12 - WR4
     * 13 - WR4 - Port B Adresse LOW
     * 14 - WR4 - Port B Adresse HIGH
     * 15 - WR4 - Interrupt Control Byte
     * 16 - WR4 - Pulse Control Byte
     * 17 - WR4 - Interrupt Vector
     * 18 - WR5
     * 19 - WR6
     * 20 - WR6 - Read Mask
     * </pre>
     */
    private int[] writeRegister = new int[21];
    /**
     * Nächste zu schreibende Register
     */
    private LinkedList<Integer> nextWriteRegister = new LinkedList<>();
    /**
     * Nächste zu lesende Register
     */
    private LinkedList<Integer> nextReadRegister = new LinkedList<>();
    /**
     * Anzahl der ausstehenden Verarbeitungszyklen
     */
    private int buffer = 0;
    /**
     * Port A - Adresse
     */
    private int addressPortA = 0;
    /**
     * Port B - Adresse
     */
    private int addressPortB = 0;
    /**
     * Bytezähler
     */
    private int bytecounter = 0;
    /**
     * Statusbyte
     */
    private int status = 0;
    /**
     * DMA aktiviert
     */
    private boolean enableDMA = false;
    /**
     * DMA Interrupts aktiviert
     */
    private boolean enableInterrupts = false;
    /**
     * DMA fordert Bus
     */
    private boolean busRequest = false;
    /**
     * FORCE READY Bedingung
     */
    private boolean forceReady = false;
    /**
     * Ready Line
     */
    private boolean rdy = false;
    /**
     * Bus Acknowledge In - Leitung
     */
    private boolean bai = false;
    /**
     * Auto-Restart Funktion
     */
    private boolean autoRestart = false;
    /**
     * Verweis auf UA880 Modul
     */
    private final SubsystemModule module;

    /**
     * Erzeugt einen neuen UA858 DMA-Controller
     *
     * @param module Modul mit Controllereinsatz
     */
    public UA858(SubsystemModule module) {
        this.module = module;
    }

    /**
     * Schreibt ein Control-byte in den DMA.
     * <p>
     * TODO: Befehle implementieren
     *
     * @param data Control-Byte
     */
    public void writeControl(int data) {
        // Befehle deaktivieren DMA
        enableDMA = false;

        // Nur wenn gerade kein anderes Register erwartet wird
        if (nextWriteRegister.isEmpty()) {
            if (!BitTest.getBit(data, 7)) {
                if (BitTest.getBit(data, 0) || BitTest.getBit(data, 1)) {
                    // WR0 0xxxxxyy (yy nicht 00)
                    writeRegister[0] = data;
                    // Prüfe Bits für folgende Register
                    for (int i = 3; i < 7; i++) {
                        if (BitTest.getBit(data, i)) {
                            nextWriteRegister.add(1 + (i - 3));
                        }
                    }
                    System.out.print(String.format("WR0=%02X : ", data));
                    System.out.print(((data & 0x03) == 0x01) ? "TRANSFER - " : (((data & 0x03) == 0x02) ? "SEARCH - " : "SEARCH/TRANSFER"));
                    System.out.println(BitTest.getBit(data, 3) ? "A->B" : "B->A");
                } else {
                    if (BitTest.getBit(data, 2)) {
                        // WR1 0xxxx100
                        writeRegister[5] = data;
                        // Prüfe Bit für folgendes Register
                        if (BitTest.getBit(data, 6)) {
                            nextWriteRegister.add(6);
                        }
                        System.out.print(String.format("WR1=%02X : ", data));
                        System.out.print(BitTest.getBit(data, 3) ? " A is I/O - " : "A is MEMORY - ");
                        System.out.println(((data & 0x30) == 0x00) ? "A DECREMENTS" : (((data & 0x30) == 0x10) ? "A INCREMENTS" : "A FIXED"));
                    } else {
                        // WR2 0xxxx000
                        writeRegister[7] = data;
                        // Prüfe Bit für folgendes Register
                        if (BitTest.getBit(data, 6)) {
                            nextWriteRegister.add(8);
                        }
                        System.out.print(String.format("WR2=%02X : ", data));
                        System.out.print(BitTest.getBit(data, 3) ? " B is I/O - " : "B is MEMORY - ");
                        System.out.println(((data & 0x30) == 0x00) ? "B DECREMENTS" : (((data & 0x30) == 0x10) ? "B INCREMENTS" : "B FIXED"));
                    }
                }
            } else {
                if (!BitTest.getBit(data, 0) && !BitTest.getBit(data, 1)) {
                    // WR3 1xxxxx00
                    writeRegister[9] = data;
                    // Prüfe Bits für folgende Register
                    for (int i = 3; i < 5; i++) {
                        if (BitTest.getBit(data, i)) {
                            nextWriteRegister.add(10 + (i - 3));
                        }
                    }
                    System.out.print(String.format("WR3=%02X : ", data));
                    System.out.print(BitTest.getBit(data, 2) ? "STOP ON MATCH - " : "NO STOP ON MATCH - ");
                    System.out.print(BitTest.getBit(data, 5) ? "INT ENABLE - " : "INT DISABLE - ");
                    System.out.println(BitTest.getBit(data, 6) ? "DMA ENABLE" : "DMA DISABLE");
                    enableDMA = BitTest.getBit(data, 6);
                    enableInterrupts = BitTest.getBit(data, 5);
                } else if (BitTest.getBit(data, 0) && !BitTest.getBit(data, 1)) {
                    // WR4 1xxxxx01
                    writeRegister[12] = data;
                    // Prüfe Bits für folgende Register
                    for (int i = 2; i < 5; i++) {
                        if (BitTest.getBit(data, i)) {
                            nextWriteRegister.add(13 + (i - 2));
                        }
                    }
                    System.out.print(String.format("WR4=%02X : ", data));
                    System.out.println(((data & 0x60) == 0x00) ? "BYTE" : (((data & 0x60) == 0x20) ? "CONTINOUS" : "BURST"));
                } else if (!BitTest.getBit(data, 0) && BitTest.getBit(data, 1) && !BitTest.getBit(data, 2) && !BitTest.getBit(data, 6)) {
                    // WR5 10xxx010
                    writeRegister[18] = data;
                    autoRestart = BitTest.getBit(data, 5);
                    System.out.print(String.format("WR5=%02X : ", data));
                    System.out.print(BitTest.getBit(data, 3) ? "RDY AL - " : "RDY AH - ");
                    System.out.print(BitTest.getBit(data, 4) ? "/CE - " : "/CE+/WAIT - ");
                    System.out.println(BitTest.getBit(data, 5) ? "STOP ON EOB" : "RESTART ON EOB");
                } else if (BitTest.getBit(data, 0) && BitTest.getBit(data, 1)) {
                    // WR6 1xxxxx11
                    switch (data) {
                        case 0xC3:
                            // Reset
                            // System.out.println("DMA Reset");
                            bytecounter = 0;
                            enableInterrupts = false;
                            busRequest = false;
                            forceReady = false;
                            autoRestart = false;
                            // TODO:
                            //  - Reset interrupt latches
                            //  - Reset Wait Function
                            //  - Reset Timing
                            break;
                        case 0xC7:
                            // TODO: Reset Port A Timing
                            break;
                        case 0xCB:
                            // TODO: Reset Port B Timing
                            break;
                        case 0xCF:
                            // Load
                            forceReady = false;
                            bytecounter = 0;
                            if (BitTest.getBit(writeRegister[0], 2)) {
                                addressPortA = (writeRegister[2] << 8) | writeRegister[1];
                                System.out.println(String.format("Lade Port A %04X", addressPortA));
                            } else {
                                addressPortB = (writeRegister[14] << 8) | writeRegister[13];
                                System.out.println(String.format("Lade Port B %04X", addressPortB));
                            }
                            break;
                        case 0xD3:
                            // Continue
                            bytecounter = 0;
                            break;
                        case 0xAF:
                            // Disable Interrupts
                            System.out.println("DMA Disable Interrupts");
                            enableInterrupts = false;
                            break;
                        case 0xAB:
                            // Enable Interrupts
                            System.out.println("DMA Enable Interrupts");
                            enableInterrupts = true;
                            break;
                        case 0xA3:
                            // Reset and Disable Interrupts
                            forceReady = false;
                            enableInterrupts = false;
                            break;
                        case 0xB7:
                            // Enable after Reti
                            break;
                        case 0xBF:
                            // Read Status Byte
                            nextReadRegister.add(0);
                            break;
                        case 0x8B:
                            // Reinitialize Status Byte
                            status |= 0x30;
                            break;
                        case 0xA7:
                            // Initiate Read Sequence
                            for (int i = 0; i < 7; i++) {
                                if (BitTest.getBit(writeRegister[20], i)) {
                                    nextReadRegister.add(i);
                                }
                            }
                            break;
                        case 0xB3:
                            // Force Ready
                            forceReady = true;
                            break;
                        case 0x87:
                            // Enable DMA
                            System.out.println("DMA Enable");
                            enableDMA = true;
                            break;
                        case 0x83:
                            // Disable DMA
                            System.out.println("DMA Disable");
                            enableDMA = false;
                            break;
                        case 0xBB:
                            // Read Mask follows
                            nextWriteRegister.add(20);
                            break;
                    }
                }
            }
        } else {
            // Lese nächstes geplantes Register
            int nextRegister = nextWriteRegister.remove();
            switch (nextRegister) {
                case 6:
                    System.out.print(String.format("PORT A-TIMING: %02X ", data));
                    System.out.print(((data & 0x03) == 0x00) ? "CLEN=4 - " : (((data & 0x03) == 0x01) ? "CLEN=3 - " : "CLEN=2 - "));
                    System.out.print(BitTest.getBit(data, 2) ? "/IORQ NORMAL - " : "/IORQ 1/2 CYL - ");
                    System.out.print(BitTest.getBit(data, 3) ? "/MREQ NORMAL - " : "/MREQ 1/2 CYL - ");
                    System.out.print(BitTest.getBit(data, 6) ? "/RD NORMAL - " : "/RD 1/2 CYL - ");
                    System.out.println(BitTest.getBit(data, 7) ? "/WR NORMAL" : "/WR 1/2 CYL");
                    break;
                case 8:
                    System.out.print(String.format("PORT B-TIMING: %02X ", data));
                    System.out.print(((data & 0x03) == 0x00) ? "CLEN=4 - " : (((data & 0x03) == 0x01) ? "CLEN=3 - " : "CLEN=2 - "));
                    System.out.print(BitTest.getBit(data, 2) ? "/IORQ NORMAL - " : "/IORQ 1/2 CYL - ");
                    System.out.print(BitTest.getBit(data, 3) ? "/MREQ NORMAL - " : "/MREQ 1/2 CYL - ");
                    System.out.print(BitTest.getBit(data, 6) ? "/RD NORMAL - " : "/RD 1/2 CYL - ");
                    System.out.println(BitTest.getBit(data, 7) ? "/WR NORMAL" : "/WR 1/2 CYL");
                    break;
                case 10:
                    System.out.println(String.format("MASK: %02X ", data));
                    break;
                case 11:
                    System.out.println(String.format("MATCH: %02X ", data));
                    break;
                case 15:
                    System.out.print(String.format("INT CTRL: %02X ", data));
                    System.out.print(((data & 0x03) == 0x00) ? "CLEN=4 - " : (((data & 0x03) == 0x01) ? "CLEN=3 - " : "CLEN=2 - "));
                    System.out.print(BitTest.getBit(data, 0) ? "INT ON MATCH - " : "NO INT ON MATCH - ");
                    System.out.print(BitTest.getBit(data, 1) ? "INT ON EOB - " : "NO INT ON EOB - ");
                    System.out.print(BitTest.getBit(data, 2) ? "PULSE - " : "NO PULSE - ");
                    System.out.print(BitTest.getBit(data, 5) ? "STATUS AFFECTS VECT - " : "STATUS NOT AFFECTS VECT - ");
                    System.out.println(BitTest.getBit(data, 6) ? "INT ON RDY" : "NO INT ON RDY");
                    break;
            }

            writeRegister[nextRegister] = data;
            if (nextRegister == 15) {
                for (int i = 3; i < 5; i++) {
                    if (BitTest.getBit(data, i)) {
                        nextWriteRegister.add(16 + (i - 3));
                    }
                }
            } else if (nextRegister == 20) {
                for (int i = 0; i < 7; i++) {
                    if (BitTest.getBit(data, i)) {
                        nextReadRegister.add(i);
                    }
                }
            }
        }
    }

    /**
     * Liest ein Statusbyte vom DMA.
     *
     * @return Statusbyte
     */
    public int readStatus() {
        if (nextReadRegister.isEmpty()) {
            return status;
        } else {
            switch (nextReadRegister.remove()) {
                case 0:
                    return status;
                case 1:
                    return bytecounter & (0xFF);
                case 2:
                    return (bytecounter >> 8) & (0xFF);
                case 3:
                    return addressPortA & (0xFF);
                case 4:
                    return (addressPortA >> 8) & (0xFF);
                case 5:
                    return addressPortB & (0xFF);
                case 6:
                    return (addressPortB >> 8) & (0xFF);
                default:
                    throw new IllegalStateException("Unbekanntes Leseregister in UA858");
            }
        }
    }

    /**
     * TODO: implementieren
     *
     * @param cycles
     */
    public void updateClock(int cycles) {
        // Prüfe ob DMA aktiv ist
        if (enableDMA) {
            // Addiere Anzahl der Takte
            // TODO: Maschinentakte, Takte
            //buffer += cycles * 1000;
            buffer += cycles;
            do {
                startOperation();
            } while (buffer > 0 && enableDMA);
        }
    }

    /**
     * Führt die DMA-Operationen aus.
     */
    private void startOperation() {
        if (rdy || forceReady) {
            // RDY Line aktiv gesetzt
            busRequest = true;
            module.requestBus(true);
            // Übertrage ein Byte
            transferOneByte();
            if (bytecounter > ((writeRegister[4] << 8) | writeRegister[3])) {
                System.out.println("End of Block");
                // End of Block
                // Status Bit 5 löschen
                status &= 0xDF;
                if (BitTest.getBit(writeRegister[15], 1)) {
                    module.requestInterrupt(writeRegister[17]);
                }
                if (autoRestart) {
                    System.out.println("Auto Repeat");
                    // Lade Register und setze Zähler zurück
                    bytecounter = 0;
                    addressPortA = (writeRegister[2] << 8) | writeRegister[1];
                    addressPortB = (writeRegister[14] << 8) | writeRegister[13];
                } else {
                    module.requestBus(false);
                    enableDMA = false;
                }
            } else {
                // Kein End of Block
                // Prüfe Übertragungsmodus
                switch (writeRegister[12] & 0x60) {
                    case 0x00:
                        // Byte
                        busRequest = false;
                        System.out.println("Mode: Byte");
                        break;
                    case 0x20:
                        // Continous Mode
                        // TODO: Prüfe Ready
                        System.out.println("Mode: Continous");
                        break;
                    case 0x40:
                        // Burst
                        // TODO: Prüfe Ready
                        // TODO: Release Bus
                        System.out.println("Mode: Burst");
                        break;
                }
            }
        }
    }

    /**
     * Überträgt ein Byte mittels DMA gemäß den aktuellen Einstellungen.
     */
    private void transferOneByte() {
        int source;
        // Inkrement / Decrement nur nach dem ersten Byte ausführen
        if (bytecounter != 0) {
            switch (writeRegister[5] & 0x30) {
                case 0x00:
                    // PortA Decrements
                    addressPortA--;
                    break;
                case 0x10:
                    // PortA Increments
                    addressPortA++;
                    break;
            }
        }
        if (bytecounter != 0) {
            switch (writeRegister[7] & 0x30) {
                case 0x00:
                    // PortB Decrements
                    addressPortB--;
                    break;
                case 0x10:
                    // PortB Increments
                    addressPortB++;
                    break;
            }
        }
        if (BitTest.getBit(writeRegister[0], 2)) {
            // PortA->PortB
            if (BitTest.getBit(writeRegister[5], 3)) {
                // PortA ist I/O
                source = module.readLocalPort(addressPortA);
                System.out.println(String.format("Lese Port A I/O %04X", addressPortA));
                buffer -= 4;
            } else {
                // PortA ist Speicher
                source = module.readLocalByte(addressPortA);
                System.out.println(String.format("Lese Port A Speicher %04X", addressPortA));
                buffer -= 3;
            }
            if (BitTest.getBit(writeRegister[7], 3)) {
                // PortB ist I/O
                module.writeLocalPort(addressPortB, source);
                System.out.println(String.format("Schreibe Port B I/O %04X", addressPortB));
                buffer -= 4;
            } else {
                // PortB ist Speicher
                module.writeLocalByte(addressPortB, source);
                System.out.println(String.format("Schreibe Port B Speicher %04X", addressPortB));
                buffer -= 3;
            }
        } else {
            // PortB->PortA
            if (BitTest.getBit(writeRegister[7], 3)) {
                // PortB ist I/O
                source = module.readLocalPort(addressPortB);
                System.out.println(String.format("Lese Port B I/O %04X", addressPortB));
                buffer -= 4;
            } else {
                // PortB ist Speicher
                source = module.readLocalByte(addressPortB);
                System.out.println(String.format("Lese Port B Speicher %04X", addressPortB));
                buffer -= 3;
            }
            if (BitTest.getBit(writeRegister[5], 3)) {
                // PortA ist I/O
                module.writeLocalPort(addressPortA, source);
                System.out.println(String.format("Schreibe Port A I/O %04X", addressPortA));
                buffer -= 4;
            } else {
                // PortA ist Speicher
                module.writeLocalByte(addressPortA, source);
                System.out.println(String.format("Schreibe Port A Speicher %04X", addressPortA));
                buffer -= 3;
            }
        }
        if (BitTest.getBit(writeRegister[0], 1)) {
            // Search
            System.out.println("DMA Search noch nicht implementiert!");
            if ((source & writeRegister[10]) == (writeRegister[11] & writeRegister[10])) {
                // Match
                // Status Bit 4 löschen
                status &= 0xEF;
                if (BitTest.getBit(writeRegister[9], 2)) {
                    // TODO: Stop On Match
                }
                if (BitTest.getBit(writeRegister[15], 0)) {
                    // TODO: Interrupt On Match
                }
            }
        } else {
            bytecounter++;
        }
    }

    /**
     * Setzt die RDY-Leitung des DMA.
     *
     * @param rdy Zustand der RDY-Leitung
     */
    public void setRDY(boolean rdy) {
        this.rdy = rdy;
    }

    /**
     * Setzt die BAI-Leitung des DMA.
     *
     * @param bai Zusand der BAI Leitung
     */
    public void setBAI(boolean bai) {
        this.bai = bai;
    }

    /**
     * Speichert den Zustand des DMA in einer Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        for (int wr : writeRegister) {
            dos.writeInt(wr);
        }
        dos.writeInt(nextWriteRegister.size());
        for (Integer nwr : nextWriteRegister) {
            dos.writeInt(nwr);
        }
        dos.writeInt(nextReadRegister.size());
        for (Integer nrr : nextReadRegister) {
            dos.writeInt(nrr);
        }
        dos.writeInt(buffer);
        dos.writeInt(addressPortA);
        dos.writeInt(addressPortB);
        dos.writeInt(bytecounter);
        dos.writeInt(status);
        dos.writeBoolean(enableDMA);
        dos.writeBoolean(enableInterrupts);
    }

    /**
     * Lädt den Zustand des DMA aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        for (int i = 0; i < writeRegister.length; i++) {
            writeRegister[i] = dis.readInt();
        }
        nextWriteRegister.clear();
        int size_nwr = dis.readInt();
        for (int i = 0; i < size_nwr; i++) {
            nextWriteRegister.add(dis.readInt());
        }
        nextReadRegister.clear();
        int size_nrr = dis.readInt();
        for (int i = 0; i < size_nrr; i++) {
            nextReadRegister.add(dis.readInt());
        }
        buffer = dis.readInt();
        addressPortA = dis.readInt();
        addressPortB = dis.readInt();
        bytecounter = dis.readInt();
        buffer = dis.readInt();
        status = dis.readInt();
        enableDMA = dis.readBoolean();
        enableInterrupts = dis.readBoolean();
    }
}
