/*
 * ZPS.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   02.04.2014 Kommentare vervollständigt
 *   09.08.2014 Zugriff auf SystemMemory durch MMS16 Bus ersetzt
 *
 */
package a7100emulator.components.modules;

import a7100emulator.Tools.AddressSpace;
import a7100emulator.Tools.Memory;
import a7100emulator.components.system.MMS16Bus;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Klasse zur Abbildung der ZPS (Zweiportspeicher)
 *
 * @author Dirk Bräuer
 */
public final class ZPS implements MemoryModule {

    /**
     * Anzahl der ZPS Module im System
     */
    public static int zps_count = 0;
    /**
     * Speicher der ZPS
     */
    private final Memory memory = new Memory(131072);

    /**
     * Erstellt eine neue ZPS
     */
    public ZPS() {
        zps_count++;
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
    }
}
