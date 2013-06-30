/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.modules;

import a7100emulator.Tools.AddressSpace;
import a7100emulator.Tools.Memory;
import a7100emulator.components.system.InterruptSystem;
import a7100emulator.components.system.SystemMemory;
import a7100emulator.components.system.SystemPorts;
import java.io.Serializable;

/**
 *
 * @author Dirk
 */
public final class OPS implements PortModule, MemoryModule, Serializable  {

    enum Parity {

        EVEN, ODD;
    }
    private static int ops_count = 0;
    private final static int PORT_OPS_1_PES = 0x00;
    private final static int PORT_OPS_2_PES = 0x02;
    private final static int PORT_OPS_3_PES = 0x40;
    private final static int PORT_OPS_4_PES = 0x42;
    private final int ops_id;
    private final Memory memory = new Memory(262144);
    private final byte[] parityBits = new byte[262144];
    private int ops_offset = 0;
    private Parity parity;
    private int state = 0x0F;

    public OPS() {
        ops_id = ops_count++;
        init();
    }

    @Override
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

    @Override
    public void writePort_Byte(int port, int data) {
        if (data == 0) {
            parity = Parity.EVEN;
        } else {
            parity = Parity.ODD;
        }
        state = 0x0F;
    }

    @Override
    public void writePort_Word(int port, int data) {
        //System.out.println("OUT Word " + Integer.toHexString(data) + " to port " + Integer.toHexString(port));
        //System.out.println("write Word auf OPS Port nicht implementiert");
    }

    @Override
    public int readPort_Byte(int port) {
        //System.out.println("Lese Status OPS Port " + Integer.toHexString(port) + ": " + Integer.toBinaryString(state));
        return state;
    }

    @Override
    public int readPort_Word(int port) {
        //System.out.println("read Word auf OPS Port nicht implementiert");
        return 0;
    }

    @Override
    public void init() {
        registerPorts();
        registerMemory();
    }

    @Override
    public void registerMemory() {
        ops_offset = (ZPS.zps_count == 1) ? 0x20000 : 0;

        switch (ops_id) {
            case 0:
                ops_offset += 0;
                break;
            case 1:
                ops_offset += 0x40000;
                break;
            case 2:
                ops_offset += 0x80000;
                break;
            case 3:
                ops_offset += 0xC0000;
                break;
        }
        SystemMemory.getInstance().registerMemorySpace(new AddressSpace(ops_offset, ops_offset + 0x3FFFF), this);
    }

    @Override
    public int readByte(int address) {
        // Parity Hack für A C T
        if ((address - ops_offset) == 0x20000) {
            int par = (byte) checkParity(address - ops_offset);
            if (par != parityBits[address - ops_offset]) {
                state &= ~0x07;
                InterruptSystem.getInstance().addParityNMI();
            }
        }
        return memory.readByte(address - ops_offset);
    }

    @Override
    public int readWord(int address) {
        // Parity Hack für A C T
        if ((address - ops_offset) == 0x20000) {
            int par1 = (byte) checkParity(memory.readByte(address - ops_offset));
            int par2 = (byte) checkParity(memory.readByte(address - ops_offset + 1));
            if (par1 != parityBits[address - ops_offset] || par2 != parityBits[address - ops_offset + 1]) {
                state &= ~0x07;
                InterruptSystem.getInstance().addParityNMI();
            }
        }
        return memory.readWord(address - ops_offset);
    }

    @Override
    public void writeByte(int address, int data) {
        memory.writeByte(address - ops_offset, data);
        // Parity Hack für A C T
        if ((address - ops_offset) == 0x20000) {
            parityBits[address - ops_offset] = (byte) checkParity(data);
        }
    }

    @Override
    public void writeWord(int address, int data) {
        memory.writeWord(address - ops_offset, data);
        // Parity Hack für A C T
        if ((address - ops_offset) == 0x20000) {
            parityBits[address - ops_offset] = (byte) checkParity(data & 0xFF);
            parityBits[address - ops_offset + 1] = (byte) checkParity((data >> 8) & 0xFF);
        }
    }

    private int checkParity(int data) {
        int par = (parity == Parity.EVEN) ? 0x00 : 0x01;
        for (int i = 0; i < 8; i++) {
            par ^= (0x01 & (data >> i));
        }
        return par;
    }
}
