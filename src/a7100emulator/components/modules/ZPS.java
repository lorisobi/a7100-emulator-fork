/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.modules;

import a7100emulator.Tools.AddressSpace;
import a7100emulator.Tools.Memory;
import a7100emulator.components.system.SystemMemory;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 * @author Dirk
 */
public final class ZPS implements MemoryModule {

    /**
     *
     */
    public static int zps_count = 0;
    private final Memory memory = new Memory(131072);

    /**
     *
     */
    public ZPS() {
        zps_count++;
        init();
    }

    /**
     *
     */
    @Override
    public void init() {
        registerMemory();
    }

    /**
     *
     */
    @Override
    public void registerMemory() {
        SystemMemory.getInstance().registerMemorySpace(new AddressSpace(0x00000, 0x1FFFF), this);
    }

    /**
     *
     * @param address
     * @return
     */
    @Override
    public int readByte(int address) {
        return memory.readByte(address);
    }

    /**
     *
     * @param address
     * @return
     */
    @Override
    public int readWord(int address) {
        return memory.readWord(address);
    }

    /**
     *
     * @param address
     * @param data
     */
    @Override
    public void writeByte(int address, int data) {
        memory.writeByte(address, data);
    }

    /**
     *
     * @param address
     * @param data
     */
    @Override
    public void writeWord(int address, int data) {
        memory.writeWord(address, data);
    }

    /**
     *
     * @param dos
     * @throws IOException
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     *
     * @param dis
     * @throws IOException
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
