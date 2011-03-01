/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components;

import a7100emulator.components.modules.PortModule;
import java.util.HashMap;

/**
 *
 * @author Dirk
 */
public class SystemPorts {

    private static SystemPorts instance;

    public static SystemPorts getInstance() {
        if (instance == null) {
            instance = new SystemPorts();
        }
        return instance;
    }
    private HashMap<Integer, PortModule> portModules = new HashMap<Integer, PortModule>();
    //private byte[] ports = new byte[65536];

    public void registerPort(PortModule module, int port) {
        portModules.put(port, module);
    }

    public void writeByte(int address, byte value) {
        portModules.get(address).writePort_Byte(address, value);
//        ports[address] = value;
    }

    public int readByte(int address) {
        return portModules.get(address).readPort_Byte(address);
    }

    public void writeWord(int address, short value) {
        portModules.get(address).writePort_Word(address, value);

//        byte hb = (byte) (value >> 8);
//        byte lb = (byte) value;
//        ports[address] = lb;
//        ports[address + 1] = hb;
    }

    public int readWord(int address) {
        return portModules.get(address).readPort_Word(address);
//        short result = 0;
//        byte lb = ports[address];
//        byte hb = ports[address + 1];
//        result = (short) (((short) hb << 8) | lb);
//        return result;
    }
//    public void dump() {
//        FileOutputStream fos = null;
//        try {
//            fos = new FileOutputStream("dump_ports.hex");
//            fos.write(ports);
//            fos.close();
//        } catch (Exception ex) {
//            Logger.getLogger(Memory.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
}
