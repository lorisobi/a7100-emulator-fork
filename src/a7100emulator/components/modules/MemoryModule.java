/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package a7100emulator.components.modules;

/**
 *
 * @author Dirk
 */
public interface MemoryModule extends Module {

    /**
     * 
     * @param address
     * @return
     */
    int readByte(int address);

    /**
     * 
     * @param address
     * @return
     */
    int readWord(int address);

    /**
     * 
     */
    void registerMemory();

    /**
     * 
     * @param address
     * @param data
     */
    void writeByte(int address, int data);

    /**
     * 
     * @param address
     * @param data
     */
    void writeWord(int address, int data);

}
