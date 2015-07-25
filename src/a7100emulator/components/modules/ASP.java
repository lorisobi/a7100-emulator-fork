/*
 * ASP.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2015 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   02.04.2014 - Kommentare vervollständigt
 *   09.08.2014 - Zugriffe auf SystemPorts durch MMS16Bus ersetzt
 */
package a7100emulator.components.modules;

import a7100emulator.components.system.MMS16Bus;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Klasse zur Abbildung der ASP (Anschlußsteuerung seriell/parallel)
 * <p>
 * TODO: Diese Klasse ist noch nicht vollständig implementiert.
 *
 * @author Dirk Bräuer
 */
public final class ASP implements IOModule {

    /**
     * Anzahl der ASP im System
     */
    public static int asp_count = 0;
    /**
     * 1. ASP - Port aktiviere Interrupt
     */
    private final static int PORT_ASP_1_ENABLE_INTERRUPT = 0x300;
    /**
     * 1. ASP - BOS1810 spezifischer Port
     */
    private final static int PORT_ASP_1_BOS1810 = 0x301;
    /**
     * 1. ASP - Port Kommando RETI
     */
    private final static int PORT_ASP_1_RETI = 0x302;
    /**
     * 1. ASP - Port Testbetrieb
     */
    private final static int PORT_ASP_1_TEST = 0x304;
    /**
     * 1. ASP - Port Steuersignale IFSP
     */
    private final static int PORT_ASP_1_CONTROL = 0x306;
    /**
     * 1. ASP - Port PIO Port A - Daten
     */
    private final static int PORT_ASP_1_U855_PORT_A_DATA = 0x308;
    /**
     * 1. ASP - Port PIO Port B - Daten
     */
    private final static int PORT_ASP_1_U855_PORT_B_DATA = 0x30A;
    /**
     * 1. ASP - Port PIO Port A - Initialisierung
     */
    private final static int PORT_ASP_1_U855_PORT_A_INIT = 0x30C;
    /**
     * 1. ASP - Port PIO Port B - Initialisierung
     */
    private final static int PORT_ASP_1_U855_PORT_B_INIT = 0x30E;
    /**
     * 1. ASP - Port CTC - Kanal 0
     */
    private final static int PORT_ASP_1_U857_TIMER_0 = 0x310;
    /**
     * 1. ASP - Port CTC - Kanal 1
     */
    private final static int PORT_ASP_1_U857_TIMER_1 = 0x312;
    /**
     * 1. ASP - Port CTC - Kanal 2
     */
    private final static int PORT_ASP_1_U857_TIMER_2 = 0x314;
    /**
     * 1. ASP - Port CTC - Kanal 3
     */
    private final static int PORT_ASP_1_U857_TIMER_3 = 0x316;
    /**
     * 1. ASP - Port SIO - Daten Kanal A
     */
    private final static int PORT_ASP_1_U856_DATA_V24 = 0x318;
    /**
     * 1. ASP - Port SIO - Daten Kanal B
     */
    private final static int PORT_ASP_1_U856_DATA_IFSS = 0x31A;
    /**
     * 1. ASP - Port SIO - Steuerung Kanal A
     */
    private final static int PORT_ASP_1_U856_CONTROL_V24 = 0x31C;
    /**
     * 1. ASP - Port SIO - Steuerung Kanal B
     */
    private final static int PORT_ASP_1_U856_CONTROL_IFSS_V24 = 0x31E;
    /**
     * 2. ASP - Port aktiviere Interrupt
     */
    private final static int PORT_ASP_2_ENABLE_INTERRUPT = 0x320;
    /**
     * 2. ASP - BOS1810 spezifischer Port
     */
    private final static int PORT_ASP_2_BOS1810 = 0x321;
    /**
     * 2. ASP - Port Kommando RETI
     */
    private final static int PORT_ASP_2_RETI = 0x322;
    /**
     * 2. ASP - Port Testbetrieb
     */
    private final static int PORT_ASP_2_TEST = 0x324;
    /**
     * 2. ASP - Port Steuersignale IFSP
     */
    private final static int PORT_ASP_2_CONTROL = 0x326;
    /**
     * 2. ASP - Port PIO Port A - Daten
     */
    private final static int PORT_ASP_2_U855_PORT_A_DATA = 0x328;
    /**
     * 2. ASP - Port PIO Port B - Daten
     */
    private final static int PORT_ASP_2_U855_PORT_B_DATA = 0x32A;
    /**
     * 2. ASP - Port PIO Port A - Initialisierung
     */
    private final static int PORT_ASP_2_U855_PORT_A_INIT = 0x32C;
    /**
     * 2. ASP - Port PIO Port B - Initialisierung
     */
    private final static int PORT_ASP_2_U855_PORT_B_INIT = 0x32E;
    /**
     * 2. ASP - Port CTC - Kanal 0
     */
    private final static int PORT_ASP_2_U857_TIMER_0 = 0x330;
    /**
     * 2. ASP - Port CTC - Kanal 1
     */
    private final static int PORT_ASP_2_U857_TIMER_1 = 0x332;
    /**
     * 2. ASP - Port CTC - Kanal 2
     */
    private final static int PORT_ASP_2_U857_TIMER_2 = 0x334;
    /**
     * 2. ASP - Port CTC - Kanal 3
     */
    private final static int PORT_ASP_2_U857_TIMER_3 = 0x336;
    /**
     * 2. ASP - Port SIO - Daten Kanal A
     */
    private final static int PORT_ASP_2_U856_DATA_V24 = 0x338;
    /**
     * 2. ASP - Port SIO - Daten Kanal B
     */
    private final static int PORT_ASP_2_U856_DATA_IFSS = 0x33A;
    /**
     * 2. ASP - Port SIO - Steuerung Kanal A
     */
    private final static int PORT_ASP_2_U856_CONTROL_V24 = 0x33C;
    /**
     * 2. ASP - Port SIO - Steuerung Kanal B
     */
    private final static int PORT_ASP_2_U856_CONTROL_IFSS_V24 = 0x33E;
    /**
     * ID der ASP
     */
    private final int asp_id;

    /**
     * Erstellt eine neue Anschlußsteuerung seriell/parallel
     */
    public ASP() {
        asp_id = asp_count++;
        init();
    }

    /**
     * Registriert die Ports am Systembus
     */
    @Override
    public void registerPorts() {

        switch (asp_id) {
            case 1:
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_1_ENABLE_INTERRUPT);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_1_BOS1810);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_1_RETI);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_1_TEST);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_1_CONTROL);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_1_U855_PORT_A_DATA);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_1_U855_PORT_B_DATA);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_1_U855_PORT_A_INIT);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_1_U855_PORT_B_INIT);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_1_U857_TIMER_0);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_1_U857_TIMER_1);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_1_U857_TIMER_2);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_1_U857_TIMER_3);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_1_U856_DATA_V24);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_1_U856_DATA_IFSS);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_1_U856_CONTROL_V24);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_1_U856_CONTROL_IFSS_V24);
                break;
            case 2:
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_2_ENABLE_INTERRUPT);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_2_BOS1810);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_2_RETI);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_2_TEST);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_2_CONTROL);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_2_U855_PORT_A_DATA);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_2_U855_PORT_B_DATA);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_2_U855_PORT_A_INIT);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_2_U855_PORT_B_INIT);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_2_U857_TIMER_0);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_2_U857_TIMER_1);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_2_U857_TIMER_2);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_2_U857_TIMER_3);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_2_U856_DATA_V24);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_2_U856_DATA_IFSS);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_2_U856_CONTROL_V24);
                MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_2_U856_CONTROL_IFSS_V24);
                break;
        }
    }

    /**
     * Gibt ein Byte auf einem Systemport aus
     *
     * @param port Port
     * @param data Daten
     */
    @Override
    public void writePortByte(int port, int data) {
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
     * Gibt ein Wort auf einem Port aus
     *
     * @param port Port
     * @param data Daten
     */
    @Override
    public void writePortWord(int port, int data) {
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
     * Liest ein Byte von einem Port
     *
     * @param port Port
     * @return gelesenes Byte
     */
    @Override
    public int readPortByte(int port) {
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
     * Liest ein Wort von einem Port
     *
     * @param port Port
     * @return gelesenes Wort
     */
    @Override
    public int readPortWord(int port) {
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
     * Initialisiert die ASP
     */
    @Override
    public void init() {
        registerPorts();
    }

    /**
     * Speichert den Zustand der ASP in einer Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Liest den Zustand der ASP aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
