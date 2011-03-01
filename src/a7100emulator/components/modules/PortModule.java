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

    int readPort_Byte(int port);

    int readPort_Word(int port);

    void registerPorts();

    void writePort_Byte(int port, int data);

    void writePort_Word(int port, int data);

}
