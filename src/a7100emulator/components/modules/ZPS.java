/*
 * ZPS.java
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
 *   09.08.2014 - Zugriff auf SystemMemory durch MMS16Bus ersetzt
 *   31.07.2016 - Parity Hack hinzugefügt
 *              - Zugriff auf ZVE ergänzt
 *   09.08.2016 - Logger hinzugefügt
 */
package a7100emulator.components.modules;

import a7100emulator.Tools.AddressSpace;
import a7100emulator.Tools.BitTest;
import a7100emulator.Tools.Memory;
import a7100emulator.Tools.Parity;
import a7100emulator.components.system.MMS16Bus;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Klasse zur Abbildung der ZPS (Zweiportspeicher).
 * <p>
 * TODO: - Zugriff auf ZVE ggf. neu gestalten, Control Byte vollständig
 * implementieren
 *
 * @author Dirk Bräuer
 */
public final class ZPS implements MemoryModule {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(ZPS.class.getName());

    /**
     * Anzahl der ZPS Module im System
     */
    public static int zps_count = 0;
    /**
     * Speicher der ZPS
     */
    private final Memory memory = new Memory(131072);
    /**
     * Array mit Paritäts-Bits
     */
    private final byte[] parityBits = new byte[131072];
    /**
     * Aktuell gesetzte Parität
     */
    private Parity parity = Parity.ODD;
    /**
     * Verweis auf ZVE zum setzen der Staussignale
     */
    private final ZVE zve;

    /**
     * Erstellt eine neue ZPS
     *
     * @param zve
     */
    public ZPS(ZVE zve) {
        zps_count++;
        this.zve = zve;
        init();
    }

    /**
     * Initialisiert die ZPS
     */
    @Override
    public void init() {
        registerMemory();
    }

    /**
     * Registriert den 128kB Speicherberei der ZPS im Systemspeicher
     */
    @Override
    public void registerMemory() {
        MMS16Bus.getInstance().registerMemoryModule(new AddressSpace(0x00000, 0x1FFFF), this);
    }

    /**
     * Liest ein Byte von der Angegebenen Adresse
     *
     * @param address Adresse
     * @return Daten
     */
    @Override
    public int readByte(int address) {
        return memory.readByte(address);
    }

    /**
     * Liest ein Wort von der angegebenen Adresse
     *
     * @param address Adresse
     * @return Daten
     */
    @Override
    public int readWord(int address) {
        if (address == 0x10000) {
            // Parity Hack für A C T
            int par1 = (byte) Parity.calculateParityBit(memory.readByte(address), parity);
            int par2 = (byte) Parity.calculateParityBit(memory.readByte(address + 1), parity);
            if (par1 != parityBits[address] || par2 != parityBits[address + 1]) {
                zve.getPPI().writePortA(((par2 != parityBits[address + 1]) ? 0x00 : 0x40) | ((par1 != parityBits[address]) ? 0x00 : 0x40));
                MMS16Bus.getInstance().requestInterrupt(0);
            }
        }
        return memory.readWord(address);
    }

    /**
     * Schreibt ein Byte an die Angegebene Adresse
     *
     * @param address Adresse
     * @param data Daten
     */
    @Override
    public void writeByte(int address, int data) {
        if (address == 0x400) {
            // Control-Byte wird geschrieben
            parity = BitTest.getBit(data, 0) ? Parity.EVEN : Parity.ODD;
            if (!BitTest.getBit(data, 1)) {
                zve.getPPI().writePortA(0xC0);
            }
        }
        if (address == 0x10000) {
            // Parity Hack für A C T
            parityBits[address] = (byte) Parity.calculateParityBit(data & 0xFF, parity);
            parityBits[address + 1] = (byte) Parity.calculateParityBit((data >> 8) & 0xFF, parity);
        }
        memory.writeByte(address, data);
    }

    /**
     * Schreibt ein Wort an die angegebene Adresse
     *
     * @param address Adresse
     * @param data Wort
     */
    @Override
    public void writeWord(int address, int data) {
        memory.writeWord(address, data);
    }

    /**
     * Speichert den Zustand der ZPS in einer Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        memory.saveMemory(dos);
        dos.write(parityBits);
        dos.writeUTF(parity.name());
    }

    /**
     * Liest den Zustand der ZPS aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        memory.loadMemory(dis);
        dis.read(parityBits);
        parity = Parity.valueOf(dis.readUTF());
    }
}
