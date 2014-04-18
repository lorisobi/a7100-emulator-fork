/*
 * ASP.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   02.04.2014 Kommentare vervollständigt
 *
 */
package a7100emulator.components.modules;

import a7100emulator.components.system.SystemPorts;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Klasse zur Abbildung der ASP (Anschlußsteuerung seriell/parallel)
 * <p>
 * TODO: Diese Klasse ist noch nicht vollständig implementiert
 *
 * @author Dirk Bräuer
 */
public final class ASP implements PortModule {

    /**
     *
     */
    public static int asp_count = 0;
    private final static int PORT_ASP_1_ENABLE_INTERRUPT = 0x300;
    private final static int PORT_ASP_1_BOS1810 = 0x301;
    private final static int PORT_ASP_1_RETI = 0x302;
    private final static int PORT_ASP_1_TEST = 0x304;
    private final static int PORT_ASP_1_CONTROL = 0x306;
    private final static int PORT_ASP_1_U855_PORT_A_DATA = 0x308;
    private final static int PORT_ASP_1_U855_PORT_B_DATA = 0x30A;
    private final static int PORT_ASP_1_U855_PORT_A_INIT = 0x30C;
    private final static int PORT_ASP_1_U855_PORT_B_INIT = 0x30E;
    private final static int PORT_ASP_1_U857_TIMER_0 = 0x310;
    private final static int PORT_ASP_1_U857_TIMER_1 = 0x312;
    private final static int PORT_ASP_1_U857_TIMER_2 = 0x314;
    private final static int PORT_ASP_1_U857_TIMER_3 = 0x316;
    private final static int PORT_ASP_1_U856_DATA_V24 = 0x318;
    private final static int PORT_ASP_1_U856_DATA_IFSS = 0x31A;
    private final static int PORT_ASP_1_U856_CONTROL_V24 = 0x31C;
    private final static int PORT_ASP_1_U856_CONTROL_IFSS_V24 = 0x31E;
    private final static int PORT_ASP_2_ENABLE_INTERRUPT = 0x320;
    private final static int PORT_ASP_2_BOS1810 = 0x321;
    private final static int PORT_ASP_2_RETI = 0x322;
    private final static int PORT_ASP_2_TEST = 0x324;
    private final static int PORT_ASP_2_CONTROL = 0x326;
    private final static int PORT_ASP_2_U855_PORT_A_DATA = 0x328;
    private final static int PORT_ASP_2_U855_PORT_B_DATA = 0x32A;
    private final static int PORT_ASP_2_U855_PORT_A_INIT = 0x32C;
    private final static int PORT_ASP_2_U855_PORT_B_INIT = 0x32E;
    private final static int PORT_ASP_2_U857_TIMER_0 = 0x330;
    private final static int PORT_ASP_2_U857_TIMER_1 = 0x332;
    private final static int PORT_ASP_2_U857_TIMER_2 = 0x334;
    private final static int PORT_ASP_2_U857_TIMER_3 = 0x336;
    private final static int PORT_ASP_2_U856_DATA_V24 = 0x338;
    private final static int PORT_ASP_2_U856_DATA_IFSS = 0x33A;
    private final static int PORT_ASP_2_U856_CONTROL_V24 = 0x33C;
    private final static int PORT_ASP_2_U856_CONTROL_IFSS_V24 = 0x33E;
    private final int asp_id;

    /**
     * Erstellt eine neue Anschlußsteuerung seriell/parallel
     */
    public ASP() {
        asp_id = asp_count++;
        init();
    }

    /**
     *
     */
    @Override
    public void registerPorts() {

        switch (asp_id) {
            case 1:
                SystemPorts.getInstance().registerPort(this, PORT_ASP_1_ENABLE_INTERRUPT);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_1_BOS1810);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_1_RETI);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_1_TEST);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_1_CONTROL);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_1_U855_PORT_A_DATA);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_1_U855_PORT_B_DATA);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_1_U855_PORT_A_INIT);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_1_U855_PORT_B_INIT);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_1_U857_TIMER_0);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_1_U857_TIMER_1);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_1_U857_TIMER_2);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_1_U857_TIMER_3);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_1_U856_DATA_V24);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_1_U856_DATA_IFSS);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_1_U856_CONTROL_V24);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_1_U856_CONTROL_IFSS_V24);
                break;
            case 2:
                SystemPorts.getInstance().registerPort(this, PORT_ASP_2_ENABLE_INTERRUPT);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_2_BOS1810);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_2_RETI);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_2_TEST);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_2_CONTROL);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_2_U855_PORT_A_DATA);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_2_U855_PORT_B_DATA);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_2_U855_PORT_A_INIT);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_2_U855_PORT_B_INIT);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_2_U857_TIMER_0);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_2_U857_TIMER_1);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_2_U857_TIMER_2);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_2_U857_TIMER_3);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_2_U856_DATA_V24);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_2_U856_DATA_IFSS);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_2_U856_CONTROL_V24);
                SystemPorts.getInstance().registerPort(this, PORT_ASP_2_U856_CONTROL_IFSS_V24);
                break;
        }
    }

    /**
     *
     * @param port
     * @param data
     */
    @Override
    public void writePort_Byte(int port, int data) {
        switch (port) {
            case PORT_ASP_1_ENABLE_INTERRUPT:
                break;
            case PORT_ASP_1_BOS1810:
                break;
            case PORT_ASP_1_RETI:
                break;
            case PORT_ASP_1_TEST:
                break;
            case PORT_ASP_1_CONTROL:
                break;
            case PORT_ASP_1_U855_PORT_A_DATA:
                break;
            case PORT_ASP_1_U855_PORT_B_DATA:
                break;
            case PORT_ASP_1_U855_PORT_A_INIT:
                break;
            case PORT_ASP_1_U855_PORT_B_INIT:
                break;
            case PORT_ASP_1_U857_TIMER_0:
                break;
            case PORT_ASP_1_U857_TIMER_1:
                break;
            case PORT_ASP_1_U857_TIMER_2:
                break;
            case PORT_ASP_1_U857_TIMER_3:
                break;
            case PORT_ASP_1_U856_DATA_V24:
                break;
            case PORT_ASP_1_U856_DATA_IFSS:
                break;
            case PORT_ASP_1_U856_CONTROL_V24:
                break;
            case PORT_ASP_1_U856_CONTROL_IFSS_V24:
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
            case PORT_ASP_1_ENABLE_INTERRUPT:
                break;
            case PORT_ASP_1_BOS1810:
                break;
            case PORT_ASP_1_RETI:
                break;
            case PORT_ASP_1_TEST:
                break;
            case PORT_ASP_1_CONTROL:
                break;
            case PORT_ASP_1_U855_PORT_A_DATA:
                break;
            case PORT_ASP_1_U855_PORT_B_DATA:
                break;
            case PORT_ASP_1_U855_PORT_A_INIT:
                break;
            case PORT_ASP_1_U855_PORT_B_INIT:
                break;
            case PORT_ASP_1_U857_TIMER_0:
                break;
            case PORT_ASP_1_U857_TIMER_1:
                break;
            case PORT_ASP_1_U857_TIMER_2:
                break;
            case PORT_ASP_1_U857_TIMER_3:
                break;
            case PORT_ASP_1_U856_DATA_V24:
                break;
            case PORT_ASP_1_U856_DATA_IFSS:
                break;
            case PORT_ASP_1_U856_CONTROL_V24:
                break;
            case PORT_ASP_1_U856_CONTROL_IFSS_V24:
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
            case PORT_ASP_1_ENABLE_INTERRUPT:
                break;
            case PORT_ASP_1_BOS1810:
                break;
            case PORT_ASP_1_RETI:
                break;
            case PORT_ASP_1_TEST:
                break;
            case PORT_ASP_1_CONTROL:
                break;
            case PORT_ASP_1_U855_PORT_A_DATA:
                break;
            case PORT_ASP_1_U855_PORT_B_DATA:
                break;
            case PORT_ASP_1_U855_PORT_A_INIT:
                break;
            case PORT_ASP_1_U855_PORT_B_INIT:
                break;
            case PORT_ASP_1_U857_TIMER_0:
                break;
            case PORT_ASP_1_U857_TIMER_1:
                break;
            case PORT_ASP_1_U857_TIMER_2:
                break;
            case PORT_ASP_1_U857_TIMER_3:
                break;
            case PORT_ASP_1_U856_DATA_V24:
                break;
            case PORT_ASP_1_U856_DATA_IFSS:
                break;
            case PORT_ASP_1_U856_CONTROL_V24:
                break;
            case PORT_ASP_1_U856_CONTROL_IFSS_V24:
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
            case PORT_ASP_1_ENABLE_INTERRUPT:
                break;
            case PORT_ASP_1_BOS1810:
                break;
            case PORT_ASP_1_RETI:
                break;
            case PORT_ASP_1_TEST:
                break;
            case PORT_ASP_1_CONTROL:
                break;
            case PORT_ASP_1_U855_PORT_A_DATA:
                break;
            case PORT_ASP_1_U855_PORT_B_DATA:
                break;
            case PORT_ASP_1_U855_PORT_A_INIT:
                break;
            case PORT_ASP_1_U855_PORT_B_INIT:
                break;
            case PORT_ASP_1_U857_TIMER_0:
                break;
            case PORT_ASP_1_U857_TIMER_1:
                break;
            case PORT_ASP_1_U857_TIMER_2:
                break;
            case PORT_ASP_1_U857_TIMER_3:
                break;
            case PORT_ASP_1_U856_DATA_V24:
                break;
            case PORT_ASP_1_U856_DATA_IFSS:
                break;
            case PORT_ASP_1_U856_CONTROL_V24:
                break;
            case PORT_ASP_1_U856_CONTROL_IFSS_V24:
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
