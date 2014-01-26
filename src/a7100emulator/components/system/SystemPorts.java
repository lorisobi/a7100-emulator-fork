/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.system;

import a7100emulator.components.modules.PortModule;
import java.util.HashMap;

/**
 *
 * @author Dirk
 */
public class SystemPorts {

    private static SystemPorts instance;
    private HashMap<Integer, PortModule> portModules = new HashMap<Integer, PortModule>();
    
    /**
     * 
     * @return
     */
    public static SystemPorts getInstance() {
        if (instance == null) {
            instance = new SystemPorts();
        }
        return instance;
    }
    
    /**
     * 
     * @param module
     * @param port
     */
    public void registerPort(PortModule module, int port) {
        portModules.put(port, module);
    }

    /**
     * 
     * @param address
     * @param value
     */
    public void writeByte(int address, int value) {
        PortModule module = portModules.get(address);
        if (module == null) {
            SystemClock.getInstance().updateClock(0xC000);
//            System.out.println("Kein Modul für Port " + Integer.toHexString(address));
        } else {
            module.writePort_Byte(address, 0xFF & value);
        }
    }

    /**
     * 
     * @param address
     * @return
     */
    public int readByte(int address) {
        PortModule module = portModules.get(address);
        if (module == null) {
//            System.out.println("Kein Modul für Port " + Integer.toHexString(address));
            SystemClock.getInstance().updateClock(0xC000);
            return 0x00;
        } else {
            return module.readPort_Byte(address);
        }
    }

    /**
     * 
     * @param address
     * @param value
     */
    public void writeWord(int address, int value) {
        PortModule module = portModules.get(address);
        if (module == null) {
            SystemClock.getInstance().updateClock(0xC000);
//            System.out.println("Kein Modul für Port " + Integer.toHexString(address));
        } else {
            module.writePort_Word(address, 0xFFFF & value);
        }
    }

    /**
     * 
     * @param address
     * @return
     */
    public int readWord(int address) {
        PortModule module = portModules.get(address);
        if (module == null) {
            SystemClock.getInstance().updateClock(0xC000);
//            System.out.println("Kein Modul für Port " + Integer.toHexString(address));
            return 0xFF;
        }
        return module.readPort_Word(address);
    }

    /**
     * 
     */
    public void reset() {
        portModules.clear();
    }
}
