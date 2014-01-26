/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package a7100emulator.components.modules;

/**
 *
 * @author Dirk
 */
public interface PortModule extends Module {

    /**
     * 
     * @param port
     * @return
     */
    int readPort_Byte(int port);

    /**
     * 
     * @param port
     * @return
     */
    int readPort_Word(int port);

    /**
     * 
     */
    void registerPorts();

    /**
     * 
     * @param port
     * @param data
     */
    void writePort_Byte(int port, int data);

    /**
     * 
     * @param port
     * @param data
     */
    void writePort_Word(int port, int data);

}
