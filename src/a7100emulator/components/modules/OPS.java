/*
 * OPS.java
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
 *   01.04.2014 - Kommentare vervollständigt
 *   09.08.2014 - Zugriffe auf SystemMemory, SystemPorts durch MMS16Bus ersetzt
 *              - Interrupts auf MMS16 umgeleitet
 *   18.12.2014 - Parity Hack ergänzt
 *   03.01.2015 - Parität setzen durch Modulo Rechnung überarbeitet
 *              - Fehler Paritätsprüfung behoben und Parity Hack entfernt
 *   37.07.2016 - Ports für alle Module in Array zusammengefasst
 */
package a7100emulator.components.modules;

import a7100emulator.Tools.AddressSpace;
import a7100emulator.Tools.Memory;
import a7100emulator.components.system.MMS16Bus;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Klasse zur Abbildung der OPS (Operativspeicher).
 * <p>
 * TODO: - Paritätsprüfung global durchführen (Weniger Performance), Schreiben
 * von Wörtern auf Ports noch nicht definiert, Setzen des Paritätsfehlers noch
 * nicht vollständig
 *
 * @author Dirk Bräuer
 */
public final class OPS implements IOModule, MemoryModule {

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
     * Ports für die vier möglichen OPS-Module
     */
    private final static int[] PORT_OPS_PES = new int[]{0x00, 0x02, 0x40, 0x42};

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
    private Parity parity = Parity.ODD;

    /**
     * Status der OPS
     */
    private int state = 0x0F;

    /**
     * Erstellt ein neues OPS-Modul. Die Gesamtanzahl der OPS-Module wird erhöht
     * und das entsprechende Modul angelegt.
     */
    public OPS() {
        ops_id = ops_count++;
        init();
    }

    /**
     * Registriert die Ports des OPS im System. Für jede OPS wird im
     * E/A-Adressraum nur ein Port registriert. Über diesen Port kann die
     * Parität des Moduls festgelegt werden.
     */
    @Override
    public void registerPorts() {
        MMS16Bus.getInstance().registerIOPort(this, PORT_OPS_PES[ops_id]);
    }

    /**
     * Gibt ein Byte auf einem Port des OPS aus.
     *
     * @param port Port
     * @param data Daten
     */
    @Override
    public void writePortByte(int port, int data) {
        if (port == PORT_OPS_PES[ops_id]) {
            if ((data % 2) == 0) {
                parity = Parity.EVEN;
            } else {
                parity = Parity.ODD;
            }
            state = 0x0F;
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
        //System.out.println("write Word auf OPS Port nicht implementiert");
    }

    /**
     * Liest ein Byte von einem Port
     *
     * @param port Port
     * @return gelesenes Byte
     */
    @Override
    public int readPortByte(int port) {
        if (port == PORT_OPS_PES[ops_id]) {
            return state;
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
        MMS16Bus.getInstance().registerMemoryModule(new AddressSpace(ops_offset, ops_offset + 0x3FFFF), this);
    }

    /**
     * Liest ein Byte aus dem Speicher
     *
     * @param address Adresse
     * @return gelesenes Byte
     */
    @Override
    public int readByte(int address) {
        // Parity Hack für A C T
        if ((address - ops_offset) == 0x20000) {
            int par = (byte) checkParity(memory.readByte(address - ops_offset));
            if (par != parityBits[address - ops_offset]) {
                state &= ~0x07;
                MMS16Bus.getInstance().requestInterrupt(0);
            }
        }
        return memory.readByte(address - ops_offset);
    }

    /**
     * Liest ein Wort aus dem Speicher
     *
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
                MMS16Bus.getInstance().requestInterrupt(0);
            }
        }
        return memory.readWord(address - ops_offset);
    }

    /**
     * Schreibt ein Byte in den Speicher
     *
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
     *
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
     * Prüft die Parität eines Bytes
     *
     * @param data Daten
     * @return Parität (0-gerade / 1-ungerade)
     */
    private int checkParity(int data) {
        int par = (parity.equals(Parity.EVEN)) ? 0x00 : 0x01;
        for (int i = 0; i < 8; i++) {
            par ^= (0x01 & (data >> i));
        }
        return par;
    }

    /**
     * Schreibt den Zustand der OPS in eine Datei
     *
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
     *
     * @param dis Stream der Datei
     * @throws IOException Wenn Lesen nicht erfolreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        memory.loadMemory(dis);
        dis.read(parityBits);
        ops_offset = dis.readInt();
        parity = Parity.valueOf(dis.readUTF());
        state = dis.readInt();
    }
}
