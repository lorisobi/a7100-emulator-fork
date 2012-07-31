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

    public static SystemMemory getInstance() {
        if (instance == null) {
            instance = new SystemMemory();
        }
        return instance;
    }

    public void registerMemorySpace(AddressSpace addressSpace, MemoryModule module) {
        memoryModules.put(addressSpace, module);
    }

    private MemoryModule getModuleForAddress(int address) {
        for (AddressSpace addressSpace : memoryModules.keySet()) {
            if (address >= addressSpace.getLowerAddress() && address <= addressSpace.getHigherAddress()) {
                return memoryModules.get(addressSpace);
            }
        }
        //System.out.println("No module for memory address " + Integer.toHexString(address));
        return null;
    }

    public void writeByte(int address, int value) {
        MemoryModule module = getModuleForAddress(address);
        if (module == null) {
            return;
        }
//        if (address==0x80) {
//            System.out.println("Schreibe an 0x80");
//            this.dump();
//            System.exit(0);
//        }
        module.writeByte(address, value);
    }

    public int readByte(int address) {
        MemoryModule module = getModuleForAddress(address);
        if (module == null) {
            return 0;
        }
        return module.readByte(address);
    }

    public void writeWord(int address, int value) {
        MemoryModule module = getModuleForAddress(address);
        if (module == null) {
            return;
        }
//        if (address==0x80) {
//            System.out.println("Schreibe an 0x80");
//            this.dump();
//            System.exit(0);
//        }
        module.writeWord(address, value);
    }

    public int readWord(int address) {
        MemoryModule module = getModuleForAddress(address);
        if (module == null) {
            return 0;
        }
        return module.readWord(address);
    }

    public void dump() {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream("dump.hex");
            for (int i = 0; i <= MAX_ADDRESS; i++) {
                fos.write(readByte(i));
            }
            fos.close();
            //System.exit(0);
        } catch (Exception ex) {
            Logger.getLogger(SystemMemory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
