/*
 * SystemMemory.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   05.04.2014 Kommentare vervollständigt
 *
 */
package a7100emulator.components.system;

import a7100emulator.Tools.AddressSpace;
import a7100emulator.components.modules.MemoryModule;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton-Klasse zur Abbildung des Systemspeichers
 * @author Dirk Bräuer
 */
public class SystemMemory {

    /**
     * Module mit Anteil am Systemspeicher
     */
    private final HashMap<AddressSpace, MemoryModule> memoryModules = new HashMap<AddressSpace, MemoryModule>();
    /**
     * Instanz
     */
    private static SystemMemory instance;
    /**
     * Größte mögliche Speicheradresse
     */
    private static final int MAX_ADDRESS = 0xFFFFF;

    /**
     * Erstellt den Systemspeicher
     */
    private SystemMemory() {
    }

    /**
     * Gibt die Instanz des Systemspeichers zurück
     * @return Instanz
     */
    public static SystemMemory getInstance() {
        if (instance == null) {
            instance = new SystemMemory();
        }
        return instance;
    }

    /**
     * Registriert den Speicherbereich eines Moduls im Systemspeicher
     * @param addressSpace Adressbereich
     * @param module Modul
     */
    public void registerMemorySpace(AddressSpace addressSpace, MemoryModule module) {
        memoryModules.put(addressSpace, module);
    }

    /**
     * Liefert ein Modul für den angegebenen Speicherbereich
     * @param address Adresse
     * @return Modul oder null, wenn kein Modul für diesen Speicherbereich vorhanden ist
     */
    private MemoryModule getModuleForAddress(int address) {
        for (AddressSpace addressSpace : memoryModules.keySet()) {
            if (address >= addressSpace.getLowerAddress() && address <= addressSpace.getHigherAddress()) {
                return memoryModules.get(addressSpace);
            }
        }
        SystemClock.getInstance().updateClock(0xC000);
        return null;
    }

    /**
     * Schreibt ein Byte in den Systemspeicher
     * @param address Adresse
     * @param value Daten
     */
    public void writeByte(int address, int value) {
        MemoryModule module = getModuleForAddress(address);
        if (module == null) {
            //System.out.println("Zugriff auf nicht vorhandenen Speicher");            
            return;
        }
        module.writeByte(address, value);
    }

    /**
     * Liest ein Byte aus dem Systemspeicher
     * @param address Adresse
     * @return gelesenes Byte
     */
    public int readByte(int address) {
        MemoryModule module = getModuleForAddress(address);
        if (module == null) {
            //System.out.println("Zugriff auf nicht vorhandenen Speicher");
            return 0;
        }
        return module.readByte(address);
    }

    /**
     * Schreibt ein Wort in den Systemspeicher
     * @param address Adresse
     * @param value Daten
     */
    public void writeWord(int address, int value) {
        MemoryModule module = getModuleForAddress(address);
        if (module == null) {
            //System.out.println("Zugriff auf nicht vorhandenen Speicher");
            return;
        }
        module.writeWord(address, value);
    }

    /**
     * Liest ein Wort aus dem Systemspeicher
     * @param address Adresse
     * @return gelesenes Wort
     */
    public int readWord(int address) {
        MemoryModule module = getModuleForAddress(address);
        if (module == null) {
            //System.out.println("Zugriff auf nicht vorhandenen Speicher");
            return 0;
        }
        return module.readWord(address);
    }

    /**
     * Schreibt den Inhalt des Speichers in eine Datei
     * @param filename Dateiname
     */
    public void dump(String filename) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(filename);
            for (int i = 0; i <= MAX_ADDRESS; i++) {
                fos.write(readByte(i));
            }
            fos.close();
        } catch (IOException ex) {
            Logger.getLogger(SystemMemory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Löscht die Liste der Module
     */
    public void reset() {
        memoryModules.clear();
    }
}
