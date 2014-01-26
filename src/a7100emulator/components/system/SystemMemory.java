/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.system;

import a7100emulator.Tools.AddressSpace;
import a7100emulator.components.modules.MemoryModule;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Dirk
 */
public class SystemMemory {

    private HashMap<AddressSpace, MemoryModule> memoryModules = new HashMap<AddressSpace, MemoryModule>();
    private static SystemMemory instance;
    private static int MAX_ADDRESS = 0xFFFFF;

    private SystemMemory() {
    }

    /**
     * 
     * @return
     */
    public static SystemMemory getInstance() {
        if (instance == null) {
            instance = new SystemMemory();
        }
        return instance;
    }

    /**
     * 
     * @param addressSpace
     * @param module
     */
    public void registerMemorySpace(AddressSpace addressSpace, MemoryModule module) {
        memoryModules.put(addressSpace, module);
    }

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
     * 
     * @param address
     * @param value
     */
    public void writeByte(int address, int value) {
        MemoryModule module = getModuleForAddress(address);
        if (module == null) {
            return;
        }
        module.writeByte(address, value);
    }

    /**
     * 
     * @param address
     * @return
     */
    public int readByte(int address) {
        MemoryModule module = getModuleForAddress(address);
        if (module == null) {
            return 0;
        }
        return module.readByte(address);
    }

    /**
     * 
     * @param address
     * @param value
     */
    public void writeWord(int address, int value) {
        MemoryModule module = getModuleForAddress(address);
        if (module == null) {
            return;
        }
        module.writeWord(address, value);
    }

    /**
     * 
     * @param address
     * @return
     */
    public int readWord(int address) {
        MemoryModule module = getModuleForAddress(address);
        if (module == null) {
            return 0;
        }
        return module.readWord(address);
    }

    /**
     * 
     * @param filename
     */
    public void dump(String filename) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(filename);
            for (int i = 0; i <= MAX_ADDRESS; i++) {
                fos.write(readByte(i));
            }
            fos.close();
        } catch (Exception ex) {
            Logger.getLogger(SystemMemory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * 
     */
    public void reset() {
        memoryModules.clear();
    }
}
