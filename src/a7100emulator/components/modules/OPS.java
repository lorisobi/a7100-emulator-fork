/*
 * OPS.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   01.04.2014 Kommentare vervollständigt
 *
 */
package a7100emulator.components.modules;

import a7100emulator.Tools.AddressSpace;
import a7100emulator.Tools.Memory;
import a7100emulator.components.system.InterruptSystem;
import a7100emulator.components.system.SystemMemory;
import a7100emulator.components.system.SystemPorts;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Klasse zur Abbildung der OPS (Operativspeicher)
 * @author Dirk Bräuer
 */
public final class OPS implements PortModule, MemoryModule {

    /**
     * Enum für verwendete Paritäten
     */
    enum Parity {

        /**
         * Gerade Parität
         */
        EVEN,

        /**
         * Ungerade Parität
         */
        ODD;
    }
    /**
     * Gesamtzahl der vorhandenen OPS-Module
     */
    public static int ops_count = 0;

    /**
     * Port der 1. OPS
     */
    private final static int PORT_OPS_1_PES = 0x00;

    /**
     * Port der 2. OPS
     */
    private final static int PORT_OPS_2_PES = 0x02;

    /**
     * Port der 3. OPS
     */
    private final static int PORT_OPS_3_PES = 0x40;

    /**
     * Port der 4. OPS
     */
    private final static int PORT_OPS_4_PES = 0x42;

    /**
     * Nummer der OPS
     */
    private final int ops_id;

    /**
     * Speicher
     */
    private final Memory memory = new Memory(262144);

    /**
     * Array mit Paritäts-Bits
     */
    private final byte[] parityBits = new byte[262144];

    /**
     * Offset zum Speicherbereich der OPS
     */
    private int ops_offset = 0;

    /**
     * Aktuell gesetzte Parität
     */
    private Parity parity;

    /**
     * Status der OPS
     */
    private int state = 0x0F;

    /**
     * Erstellt eine neue OPS
     */
    public OPS() {
        ops_id = ops_count++;
        init();
    }

    /**
     * Registriert die Ports im System
     */
    @Override
    public void registerPorts() {
        switch (ops_id) {
            case 0:
                SystemPorts.getInstance().registerPort(this, PORT_OPS_1_PES);
                break;
            case 1:
                SystemPorts.getInstance().registerPort(this, PORT_OPS_2_PES);
                break;
            case 2:
                SystemPorts.getInstance().registerPort(this, PORT_OPS_3_PES);
                break;
            case 3:
                SystemPorts.getInstance().registerPort(this, PORT_OPS_4_PES);
                break;
        }
    }

    /**
     * Gibt ein Byte auf einem Port aus
     * @param port Port
     * @param data Daten
     */
    @Override
    public void writePort_Byte(int port, int data) {
        if (data == 0) {
            parity = Parity.EVEN;
        } else {
            parity = Parity.ODD;
        }
        state = 0x0F;
    }

    /**
     * Gibt ein Wort auf einem Port aus
     * @param port Port
     * @param data Daten
     */
    @Override
    public void writePort_Word(int port, int data) {
        //System.out.println("write Word auf OPS Port nicht implementiert");
    }

    /**
     * Liest ein Byte von einem Port
     * @param port Port
     * @return gelesenes Byte
     */
    @Override
    public int readPort_Byte(int port) {
        return state;
    }

    /**
     * Liest ein Wort von einem Port
     * @param port Port
     * @return gelesenes Wort
     */
    @Override
    public int readPort_Word(int port) {
        //System.out.println("read Word auf OPS Port nicht implementiert");
        return 0;
    }

    /**
     * Initialisiert die OPS
     */
    @Override
    public void init() {
        registerPorts();
        registerMemory();
    }

    /**
     * Registriert den Speicherbereich im Systemspeicher
     */
    @Override
    public void registerMemory() {
        ops_offset = (ZPS.zps_count == 1) ? 0x20000 : 0;

        switch (ops_id) {
            case 0:
                ops_offset += 0;
                break;
            case 1:
                ops_offset += 0x40000;
                break;
            case 2:
                ops_offset += 0x80000;
                break;
            case 3:
                ops_offset += 0xC0000;
                break;
        }
        SystemMemory.getInstance().registerMemorySpace(new AddressSpace(ops_offset, ops_offset + 0x3FFFF), this);
    }

    /**
     * Liest ein Byte aus dem Speicher
     * @param address Adresse
     * @return gelesenes Byte
     */
    @Override
    public int readByte(int address) {
        // Parity Hack für A C T
        if ((address - ops_offset) == 0x20000) {
            int par = (byte) checkParity(address - ops_offset);
            if (par != parityBits[address - ops_offset]) {
                state &= ~0x07;
                InterruptSystem.getInstance().addParityNMI();
            }
        }
        return memory.readByte(address - ops_offset);
    }

    /**
     * Liest ein Wort aus dem Speicher
     * @param address Adresse
     * @return gelesenes Wort
     */
    @Override
    public int readWord(int address) {
        // Parity Hack für A C T
        if ((address - ops_offset) == 0x20000) {
            int par1 = (byte) checkParity(memory.readByte(address - ops_offset));
            int par2 = (byte) checkParity(memory.readByte(address - ops_offset + 1));
            if (par1 != parityBits[address - ops_offset] || par2 != parityBits[address - ops_offset + 1]) {
                state &= ~0x07;
                InterruptSystem.getInstance().addParityNMI();
            }
        }
        return memory.readWord(address - ops_offset);
    }

    /**
     * Schreibt ein Byte in den Speicher
     * @param address Adresse
     * @param data Daten
     */
    @Override
    public void writeByte(int address, int data) {
        memory.writeByte(address - ops_offset, data);
        // Parity Hack für A C T
        if ((address - ops_offset) == 0x20000) {
            parityBits[address - ops_offset] = (byte) checkParity(data);
        }
    }

    /**
     * Schreibt ein Wort in den Speicher
     * @param address Adresse
     * @param data Daten
     */
    @Override
    public void writeWord(int address, int data) {
        memory.writeWord(address - ops_offset, data);
        // Parity Hack für A C T
        if ((address - ops_offset) == 0x20000) {
            parityBits[address - ops_offset] = (byte) checkParity(data & 0xFF);
            parityBits[address - ops_offset + 1] = (byte) checkParity((data >> 8) & 0xFF);
        }
    }

    /**
     * Prüft die parität eines Bytes
     * @param data Daten
     * @return Parität (0-gerade / 1-ungerade)
     */
    private int checkParity(int data) {
        int par = (parity == Parity.EVEN) ? 0x00 : 0x01;
        for (int i = 0; i < 8; i++) {
            par ^= (0x01 & (data >> i));
        }
        return par;
    }

    /**
     * Schreibt den Zustand der OPS in eine Datei
     * @param dos Stream der Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        memory.saveMemory(dos);
        dos.write(parityBits);
        dos.writeInt(ops_offset);
        dos.writeUTF(parity.name());
        dos.writeInt(state);
    }
    
    /**
     * Liest den Zustand der OPS aus einer Datei
     * @param dis Stream der Datei
     * @throws IOException Wenn Lesen nicht erfolreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        memory.loadMemory(dis);
        dis.read(parityBits);
        ops_offset=dis.readInt();
        parity=Parity.valueOf(dis.readUTF());
        state=dis.readInt();
    }
}
