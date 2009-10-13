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
public class Ports {

    private byte[] ports=new byte[65536];

    public void writeByte(int address, byte value) {
        ports[address] = value;
    }

    public byte readByte(int address) {
        return ports[address];
    }

    public void writeWord(int address, short value) {
        byte hb = (byte) (value >> 8);
        byte lb = (byte) value;
        ports[address] = lb;
        ports[address + 1] = hb;
    }

    public short readWord(int address) {
        short result = 0;
        byte lb = ports[address];
        byte hb = ports[address + 1];
        result = (short) (((short) hb << 8) | lb);
        return result;
    }

    public void dump() {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream("dump_ports.hex");
            fos.write(ports);
            fos.close();
        } catch (Exception ex) {
            Logger.getLogger(Memory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }



}
