/*
 * SystemPorts.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   05.04.2014 Kommentare vervollständigt
 *
 */
package a7100emulator.components.system;

import a7100emulator.components.modules.PortModule;
import java.util.HashMap;

/**
 * Singleton-Klasse zur Abbildung der E/A-Ports
 * @author Dirk Bräuer
 */
public class SystemPorts {

    /**
     * Instanz
     */
    private static SystemPorts instance;
    /**
     * Liste der Module mit E/A Ressourcen
     */
    private final HashMap<Integer, PortModule> portModules = new HashMap<Integer, PortModule>();
    
    /**
     * Gibt die Instanz der SystemPorts zurück
     * @return Instanz
     */
    public static SystemPorts getInstance() {
        if (instance == null) {
            instance = new SystemPorts();
        }
        return instance;
    }
    
    /**
     * Registriert ein Modul für einen Port
     * @param module Modul
     * @param port Port
     */
    public void registerPort(PortModule module, int port) {
        portModules.put(port, module);
    }

    /**
     * Gibt ein Byte auf einem Port aus
     * @param address Port
     * @param value Daten
     */
    public void writeByte(int address, int value) {
        PortModule module = portModules.get(address);
//        System.out.println("Schreibe Byte auf " + Integer.toHexString(address));
        if (module == null) {
            SystemClock.getInstance().updateClock(0xC000);
//            System.out.println("Kein Modul für Port " + Integer.toHexString(address));
        } else {
            module.writePort_Byte(address, 0xFF & value);
        }
    }

    /**
     * Liest ein Byte von einem Port
     * @param address Port
     * @return gelesenes Byte
     */
    public int readByte(int address) {
        PortModule module = portModules.get(address);
//        System.out.println("Lese Byte von " + Integer.toHexString(address));
        if (module == null) {
//            System.out.println("Kein Modul für Port " + Integer.toHexString(address));
            SystemClock.getInstance().updateClock(0xC000);
            return 0x00;
        } else {
            return module.readPort_Byte(address);
        }
    }

    /**
     * Gibt ein Wort auf einem Port aus
     * @param address Port
     * @param value Daten
     */
    public void writeWord(int address, int value) {
        PortModule module = portModules.get(address);
//        System.out.println("Schreibe Wort auf " + Integer.toHexString(address));
        if (module == null) {
            SystemClock.getInstance().updateClock(0xC000);
//            System.out.println("Kein Modul für Port " + Integer.toHexString(address));
        } else {
            module.writePort_Word(address, 0xFFFF & value);
        }
    }

    /**
     * Liest ein Wort von einem Port
     * @param address Port
     * @return gelesenes Wort
     */
    public int readWord(int address) {
        PortModule module = portModules.get(address);
//        System.out.println("Lese Wort von " + Integer.toHexString(address));
        if (module == null) {
            SystemClock.getInstance().updateClock(0xC000);
//            System.out.println("Kein Modul für Port " + Integer.toHexString(address));
            return 0xFF;
        }
        return module.readPort_Word(address);
    }

    /**
     * Löscht die Liste der Module
     */
    public void reset() {
        portModules.clear();
    }
}
