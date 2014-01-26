/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.Tools;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Dirk
 */
public class Memory {

    /**
     * 
     */
    public enum MemoryType {

        /**
         * 
         */
        RAM,
        /**
         * 
         */
        ROM;
    }

    /**
     * 
     */
    public enum FileLoadMode {

        /**
         * 
         */
        LOW_BYTE_ONLY,
        /**
         * 
         */
        HIGH_BYTE_ONLY,
        /**
         * 
         */
        LOW_AND_HIGH_BYTE;
    }
    private final byte[] memory;

    /**
     * 
     * @param size
     */
    public Memory(int size) {
        memory = new byte[size];
    }

    /**
     * 
     * @param memory
     */
    public Memory(byte[] memory) {
        this.memory = memory;
    }

    /**
     * 
     * @param baseAddress
     * @param file
     * @param loadMode
     */
    public void loadFile(int baseAddress, File file, FileLoadMode loadMode) {
        InputStream in = null;
        try {
            byte[] buffer = new byte[(int) file.length()];
            in = new FileInputStream(file);
            in.read(buffer);
            in.close();

            int address = baseAddress + (loadMode.equals(FileLoadMode.HIGH_BYTE_ONLY) ? 1 : 0);
            for (byte b : buffer) {
                memory[address++] = b;
                if (!loadMode.equals(FileLoadMode.LOW_AND_HIGH_BYTE)) {
                    address++;
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(Memory.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                Logger.getLogger(Memory.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * 
     * @param address
     * @param value
     */
    public void writeByte(int address, int value) {
        memory[address] = (byte) value;
    }

    /**
     * 
     * @param address
     * @return
     */
    public int readByte(int address) {
        return memory[address] & 0xFF;
    }

    /**
     * 
     * @param address
     * @param value
     */
    public void writeWord(int address, int value) {
        byte hb = (byte) (value >> 8);
        byte lb = (byte) value;
        memory[address] = lb;
        memory[address + 1] = hb;
    }

    /**
     * 
     * @param address
     * @return
     */
    public int readWord(int address) {
        int result;
        int lb = memory[address];
        int hb = memory[address + 1];
        result = ((hb << 8) | (lb & 0xFF));
        return result & 0xFFFF;
    }
    
    /**
     * 
     * @param dos
     * @throws IOException
     */
    public void saveMemory(DataOutputStream dos) throws IOException {
        dos.write(memory);
    }
    
    /**
     * 
     * @param dis
     * @throws IOException
     */
    public void loadMemory(DataInputStream dis) throws IOException {
        dis.read(memory);
    }
}
