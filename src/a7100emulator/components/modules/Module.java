/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.modules;

/**
 *
 * @author Dirk
 */
public interface Module {

    public void registerPorts();

    public void writePort_Byte(int port, int data);

    public void writePort_Word(int port, int data);

    public int readPort_Byte(int port);

    public int readPort_Word(int port);
}
