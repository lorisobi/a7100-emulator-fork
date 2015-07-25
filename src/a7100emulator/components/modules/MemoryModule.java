/*
 * MemoryModule.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2015 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   02.04.2014 Kommentare vervollständigt
 */
package a7100emulator.components.modules;

/**
 * Interface für Module welche Arbeitsspeicher zur Verfügung stellen
 *
 * @author Dirk Bräuer
 */
public interface MemoryModule extends Module {

    /**
     * Registriert das Modul im Systemspeicher
     */
    void registerMemory();

    /**
     * Liest ein Byte von der angegebenen Adresse
     *
     * @param address Adresse
     * @return gelesenes Byte
     */
    int readByte(int address);

    /**
     * Liest ein Wort von der angegebenen Adresse
     *
     * @param address Adresse
     * @return gelesenes Wort
     */
    int readWord(int address);

    /**
     * Schreibt ein Byte an die angegebene Adresse
     *
     * @param address Adresse
     * @param data Daten
     */
    void writeByte(int address, int data);

    /**
     * Schreibt ein Wort an die angegebene Adresse
     *
     * @param address Adresse
     * @param data Daten
     */
    void writeWord(int address, int data);
}
