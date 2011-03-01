/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.modules;

import a7100emulator.components.SystemPorts;

/**
 *
 * @author Dirk
 */
public final class OPS implements PortModule,MemoryModule {

    private static int ops_count=0;

    private final static int PORT_OPS_1_PES = 0x00;
    private final static int PORT_OPS_2_PES = 0x02;
    private final static int PORT_OPS_3_PES = 0x40;
    private final static int PORT_OPS_4_PES = 0x41;
    private final int ops_id;

    public OPS() {
        ops_id=ops_count++;
        this.registerPorts();
    }

    public void registerPorts() {
        switch (ops_id) {
            case 0:
                SystemPorts.getInstance().registerPort(this, PORT_OPS_1_PES);
                break;
            case 1:
                SystemPorts.getInstance().registerPort(this, PORT_OPS_2_PES);
                break;
            case 2:
                SystemPorts.getInstance().registerPort(this, PORT_OPS_3_PES);
                break;
            case 3:
                SystemPorts.getInstance().registerPort(this, PORT_OPS_4_PES);
                break;
        }
    }

    public void writePort_Byte(int port, int data) {
        System.out.println("write Word auf OPS Port nicht implementiert");
    }

    public void writePort_Word(int port, int data) {
        System.out.println("write Byte auf OPS Port nicht implementiert");
    }

    public int readPort_Byte(int port) {
        System.out.println("read Byte auf OPS Port nicht implementiert");
        return 0;
    }

    public int readPort_Word(int port) {
        System.out.println("read Word auf OPS Port nicht implementiert");
        return 0;
    }

    public void init() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int readByte(int address) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int readWord(int address) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void registerMemory() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void writeByte(int address, int data) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void writeWord(int address, int data) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
