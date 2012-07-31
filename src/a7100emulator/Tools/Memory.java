/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.Tools;

import a7100emulator.components.system.SystemMemory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Dirk
 */
public class Memory {

    public enum MemoryType {

        RAM,
        ROM;
    }

    public enum FileLoadMode {

        LOW_BYTE_ONLY,
        HIGH_BYTE_ONLY,
        LOW_AND_HIGH_BYTE;
    }
    private byte[] memory;

    public Memory(int size) {
        memory = new byte[size];
    }

    public Memory(byte[] memory) {
        this.memory = memory;
    }

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
        //dump();
    }

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
            Logger.getLogger(SystemMemory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

//
//    public static void main(String[] args) {
//
//        try {
//            File file=new File("DUMP.ROM");
//            FileInputStream fin = new FileInputStream(file);
//            FileOutputStream fos1 = new FileOutputStream("259.rom");
//            FileOutputStream fos2 = new FileOutputStream("260.rom");
//            FileOutputStream fos3 = new FileOutputStream("261.rom");
//            FileOutputStream fos4 = new FileOutputStream("262.rom");
//
//            int offset1=0x4000;
//
//            byte[] ba1=new byte[(int) file.length()/4];
//            byte[] ba2=new byte[(int) file.length()/4];
//            byte[] ba3=new byte[(int) file.length()/4];
//            byte[] ba4=new byte[(int) file.length()/4];
//
//            for (int i=0;i<0x2000;i++) {
//                ba1[i]=(byte)fin.read();
//                ba2[i]=(byte)fin.read();
//            }
//            for (int i=0;i<0x2000;i++) {
//                ba3[i]=(byte)fin.read();
//                ba4[i]=(byte)fin.read();
//            }
//
//            fos1.write(ba4);
//            fos2.write(ba2);
//            fos3.write(ba3);
//            fos4.write(ba1);
//
//
//            fos1.close();
//            fos2.close();
//            fos3.close();
//            fos4.close();
//        } catch (Exception ex) {
//        }
//    }
}
