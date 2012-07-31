/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.modules;

import a7100emulator.components.system.SystemPorts;

/**
 *
 * @author Dirk
 */
public class ABS implements PortModule {

    private final static int PORT_ABS_STATE = 0x200;
    private final static int PORT_ABS_DATA = 0x202;

    public void registerPorts() {
        SystemPorts.getInstance().registerPort(this, PORT_ABS_STATE);
        SystemPorts.getInstance().registerPort(this, PORT_ABS_DATA);
    }

    public void writePort_Byte(int port, int data) {
        switch (port) {
            case PORT_ABS_STATE:
                break;
            case PORT_ABS_DATA:
                break;
        }
    }

    public void writePort_Word(int port, int data) {
        switch (port) {
            case PORT_ABS_STATE:
                break;
            case PORT_ABS_DATA:
                break;
        }
    }

    public int readPort_Byte(int port) {
        switch (port) {
            case PORT_ABS_STATE:
                break;
            case PORT_ABS_DATA:
                break;
        }
        return 0;
    }

    public int readPort_Word(int port) {
        switch (port) {
            case PORT_ABS_STATE:
                break;
            case PORT_ABS_DATA:
                break;
        }
        return 0;
    }

    public void init() {
        registerPorts();
    }
}
