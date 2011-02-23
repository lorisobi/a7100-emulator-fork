/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components;

import java.io.FileOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Dirk
 */
public class Memory {

    private byte[] memory = new byte[1048576];

    public void writeByte(int address, int value) {
        memory[address] = (byte)value;
    }

    public int readByte(int address) {
        return memory[address]&0xFF;
    }

    public void writeWord(int address, int value) {
        byte hb = (byte) (value >> 8);
        byte lb = (byte) value;
        memory[address] = lb;
        memory[address + 1] = hb;
    }

    public int readWord(int address) {
        short result = 0;
        byte lb = memory[address];
        byte hb = memory[address + 1];
        result = (short) (((short) hb << 8) | lb);
        return result&0xFFFF;
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
