/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.modules;

import a7100emulator.components.system.SystemPorts;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 * @author Dirk
 */
public class ABS implements PortModule {

    private final static int PORT_ABS_STATE = 0x200;
    private final static int PORT_ABS_DATA = 0x202;

    /**
     * 
     */
    @Override
    public void registerPorts() {
        SystemPorts.getInstance().registerPort(this, PORT_ABS_STATE);
        SystemPorts.getInstance().registerPort(this, PORT_ABS_DATA);
    }

    /**
     * 
     * @param port
     * @param data
     */
    @Override
    public void writePort_Byte(int port, int data) {
        switch (port) {
            case PORT_ABS_STATE:
                break;
            case PORT_ABS_DATA:
                break;
        }
    }

    /**
     * 
     * @param port
     * @param data
     */
    @Override
    public void writePort_Word(int port, int data) {
        switch (port) {
            case PORT_ABS_STATE:
                break;
            case PORT_ABS_DATA:
                break;
        }
    }

    /**
     * 
     * @param port
     * @return
     */
    @Override
    public int readPort_Byte(int port) {
        switch (port) {
            case PORT_ABS_STATE:
                break;
            case PORT_ABS_DATA:
                break;
        }
        return 0;
    }

    /**
     * 
     * @param port
     * @return
     */
    @Override
    public int readPort_Word(int port) {
        switch (port) {
            case PORT_ABS_STATE:
                break;
            case PORT_ABS_DATA:
                break;
        }
        return 0;
    }

    /**
     * 
     */
    @Override
    public void init() {
        registerPorts();
    }

    /**
     * 
     * @param dos
     * @throws IOException
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * 
     * @param dis
     * @throws IOException
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
