/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components;

import a7100emulator.Tools.AddressSpace;
import a7100emulator.components.modules.Module;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Dirk
 */
public class Memory {

    private HashMap<AddressSpace, Module> memoryModules = new HashMap<AddressSpace, Module>();
    private static Memory instance;

    private Memory() {
    }

    public static Memory getInstance() {
        if (instance == null) {
            instance = new Memory();
        }
        return instance;
    }

    public void registerMemorySpace(AddressSpace addressSpace, Module module) {
        memoryModules.put(addressSpace, module);
    }

    private Module getModuleForAddress(int address) {
        for (AddressSpace addressSpace : memoryModules.keySet()) {
            if (address >= addressSpace.getLowerAddress() && address <= addressSpace.getHigherAddress()) {
                return memoryModules.get(addressSpace);
            }
        }
        return null;
    }



    private byte[] memory = new byte[1048576];

    public void writeByte(int address, int value) {
        memory[address] = (byte) value;
    }

    public int readByte(int address) {
        return memory[address] & 0xFF;
    }

    public void writeWord(int address, int value) {
        byte hb = (byte) (value >> 8);
        byte lb = (byte) value;
        memory[address] = lb;
        memory[address + 1] = hb;
    }

    public int readWord(int address) {
        int result = 0;
        int lb = memory[address];
        int hb = memory[address + 1];
        result = ((hb << 8) | (lb & 0xFF));
        return result & 0xFFFF;
    }

    public void dump() {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream("dump_memory.hex");
            fos.write(memory);
            fos.close();
        } catch (Exception ex) {
            Logger.getLogger(Memory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
