/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package a7100emulator.components.modules;

import a7100emulator.Tools.AddressSpace;
import a7100emulator.Tools.Memory;
import a7100emulator.components.system.SystemMemory;

/**
 *
 * @author Dirk
 */
public final class ZPS implements MemoryModule {

    public static int zps_count=0;
    private final Memory memory = new Memory(131072);

    public ZPS() {
        zps_count++;
        init();
    }

    public void init() {
        registerMemory();
    }

    public void registerMemory() {
        SystemMemory.getInstance().registerMemorySpace(new AddressSpace(0x00000, 0x1FFFF), this);
    }

    public int readByte(int address) {
        return memory.readByte(address);
    }

    public int readWord(int address) {
        return memory.readWord(address);
    }

    public void writeByte(int address, int data) {
        memory.writeByte(address, data);
    }

    public void writeWord(int address, int data) {
        memory.writeWord(address, data);
    }
}