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

    int readByte(int address);

    int readWord(int address);

    void registerMemory();

    void writeByte(int address, int data);

    void writeWord(int address, int data);

}
