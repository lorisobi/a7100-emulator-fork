/*
 * ASP.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * Copyright (c) 2011-2018 Dirk Bräuer
 *
 * Der A7100 Emulator ist Freie Software: Sie können ihn unter den Bedingungen
 * der GNU General Public License, wie von der Free Software Foundation,
 * Version 3 der Lizenz oder jeder späteren veröffentlichten Version, 
 * weiterverbreiten und/oder modifizieren.
 *
 * Der A7100 Emulator wird in der Hoffnung, dass er nützlich sein wird, aber
 * OHNE JEDE GEWÄHRLEISTUNG, bereitgestellt; sogar ohne die implizite
 * Gewährleistung der MARKTFÄHIGKEIT oder EIGNUNG FÜR EINEN BESTIMMTEN ZWECK.
 * Siehe die GNU General Public License für weitere Details.
 *
 * Sie sollten eine Kopie der GNU General Public License zusammen mit diesem
 * Programm erhalten haben. Wenn nicht, siehe <http://www.gnu.org/licenses/>.
 * 
 * Letzte Änderungen:
 *   02.04.2014 - Kommentare vervollständigt
 *   09.08.2014 - Zugriffe auf SystemPorts durch MMS16Bus ersetzt
 *   31.07.2016 - Ports für alle Module zusammengefasst
 *   09.08.2016 - Logger hinzugefügt
 *   17.03.2018 - Beenden des Emulators bei Initialisierung
 */
package a7100emulator.components.modules;

import a7100emulator.components.system.MMS16Bus;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * Klasse zur Abbildung der ASP (Anschlußsteuerung seriell/parallel)
 * <p>
 * TODO: Diese Klasse ist noch nicht vollständig implementiert.
 *
 * @author Dirk Bräuer
 */
public final class ASP implements IOModule {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(ASP.class.getName());

    /**
     * Anzahl der ASP im System
     */
    public static int asp_count = 0;
    /**
     * ASP - Ports aktiviere Interrupt
     */
    private final static int[] PORT_ASP_ENABLE_INTERRUPT = new int[]{0x300, 0x320};
    /**
     * ASP - BOS1810 spezifische Ports
     */
    private final static int[] PORT_ASP_BOS1810 = new int[]{0x301, 0x321};
    /**
     * ASP - Ports Kommando RETI
     */
    private final static int[] PORT_ASP_RETI = new int[]{0x302, 0x322};
    /**
     * ASP - Ports Testbetrieb
     */
    private final static int[] PORT_ASP_TEST = new int[]{0x304, 0x324};
    /**
     * ASP - Ports Steuersignale IFSP
     */
    private final static int[] PORT_ASP_CONTROL = new int[]{0x306, 0x326};
    /**
     * ASP - Ports PIO Port A - Daten
     */
    private final static int[] PORT_ASP_U855_PORT_A_DATA = new int[]{0x308, 0x328};
    /**
     * ASP - Ports PIO Port B - Daten
     */
    private final static int[] PORT_ASP_U855_PORT_B_DATA = new int[]{0x30A, 0x32A};
    /**
     * ASP - Ports PIO Port A - Initialisierung
     */
    private final static int[] PORT_ASP_U855_PORT_A_INIT = new int[]{0x30C, 0x32C};
    /**
     * ASP - Ports PIO Port B - Initialisierung
     */
    private final static int[] PORT_ASP_U855_PORT_B_INIT = new int[]{0x30E, 0x32E};
    /**
     * ASP - Ports CTC - Kanal 0
     */
    private final static int[] PORT_ASP_U857_TIMER_0 = new int[]{0x310, 0x330};
    /**
     * ASP - Ports CTC - Kanal 1
     */
    private final static int[] PORT_ASP_U857_TIMER_1 = new int[]{0x312, 0x332};
    /**
     * ASP - Ports CTC - Kanal 2
     */
    private final static int[] PORT_ASP_U857_TIMER_2 = new int[]{0x314, 0x334};
    /**
     * ASP - Ports CTC - Kanal 3
     */
    private final static int[] PORT_ASP_U857_TIMER_3 = new int[]{0x316, 0x336};
    /**
     * ASP - Ports SIO - Daten Kanal A
     */
    private final static int[] PORT_ASP_U856_DATA_V24 = new int[]{0x318, 0x338};
    /**
     * ASP - Ports SIO - Daten Kanal B
     */
    private final static int[] PORT_ASP_U856_DATA_IFSS = new int[]{0x31A, 0x33A};
    /**
     * ASP - Ports SIO - Steuerung Kanal A
     */
    private final static int[] PORT_ASP_U856_CONTROL_V24 = new int[]{0x31C, 0x33C};
    /**
     * ASP - Ports SIO - Steuerung Kanal B
     */
    private final static int[] PORT_ASP_U856_CONTROL_IFSS_V24 = new int[]{0x31E, 0x33E};
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
        MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_ENABLE_INTERRUPT[asp_id]);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_BOS1810[asp_id]);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_RETI[asp_id]);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_TEST[asp_id]);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_CONTROL[asp_id]);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_U855_PORT_A_DATA[asp_id]);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_U855_PORT_B_DATA[asp_id]);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_U855_PORT_A_INIT[asp_id]);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_U855_PORT_B_INIT[asp_id]);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_U857_TIMER_0[asp_id]);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_U857_TIMER_1[asp_id]);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_U857_TIMER_2[asp_id]);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_U857_TIMER_3[asp_id]);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_U856_DATA_V24[asp_id]);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_U856_DATA_IFSS[asp_id]);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_U856_CONTROL_V24[asp_id]);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ASP_U856_CONTROL_IFSS_V24[asp_id]);
    }

    /**
     * Gibt ein Byte auf einem Systemport aus
     *
     * @param port Port
     * @param data Daten
     */
    @Override
    public void writePortByte(int port, int data) {
        if (port == PORT_ASP_ENABLE_INTERRUPT[asp_id]) {
        } else if (port == PORT_ASP_BOS1810[asp_id]) {
        } else if (port == PORT_ASP_RETI[asp_id]) {
        } else if (port == PORT_ASP_TEST[asp_id]) {
        } else if (port == PORT_ASP_CONTROL[asp_id]) {
        } else if (port == PORT_ASP_U855_PORT_A_DATA[asp_id]) {
        } else if (port == PORT_ASP_U855_PORT_B_DATA[asp_id]) {
        } else if (port == PORT_ASP_U855_PORT_A_INIT[asp_id]) {
        } else if (port == PORT_ASP_U855_PORT_B_INIT[asp_id]) {
        } else if (port == PORT_ASP_U857_TIMER_0[asp_id]) {
        } else if (port == PORT_ASP_U857_TIMER_1[asp_id]) {
        } else if (port == PORT_ASP_U857_TIMER_2[asp_id]) {
        } else if (port == PORT_ASP_U857_TIMER_3[asp_id]) {
        } else if (port == PORT_ASP_U856_DATA_V24[asp_id]) {
        } else if (port == PORT_ASP_U856_DATA_IFSS[asp_id]) {
        } else if (port == PORT_ASP_U856_CONTROL_V24[asp_id]) {
        } else if (port == PORT_ASP_U856_CONTROL_IFSS_V24[asp_id]) {
        } else {
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
        if (port == PORT_ASP_ENABLE_INTERRUPT[asp_id]) {
        } else if (port == PORT_ASP_BOS1810[asp_id]) {
        } else if (port == PORT_ASP_RETI[asp_id]) {
        } else if (port == PORT_ASP_TEST[asp_id]) {
        } else if (port == PORT_ASP_CONTROL[asp_id]) {
        } else if (port == PORT_ASP_U855_PORT_A_DATA[asp_id]) {
        } else if (port == PORT_ASP_U855_PORT_B_DATA[asp_id]) {
        } else if (port == PORT_ASP_U855_PORT_A_INIT[asp_id]) {
        } else if (port == PORT_ASP_U855_PORT_B_INIT[asp_id]) {
        } else if (port == PORT_ASP_U857_TIMER_0[asp_id]) {
        } else if (port == PORT_ASP_U857_TIMER_1[asp_id]) {
        } else if (port == PORT_ASP_U857_TIMER_2[asp_id]) {
        } else if (port == PORT_ASP_U857_TIMER_3[asp_id]) {
        } else if (port == PORT_ASP_U856_DATA_V24[asp_id]) {
        } else if (port == PORT_ASP_U856_DATA_IFSS[asp_id]) {
        } else if (port == PORT_ASP_U856_CONTROL_V24[asp_id]) {
        } else if (port == PORT_ASP_U856_CONTROL_IFSS_V24[asp_id]) {
        } else {
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
        if (port == PORT_ASP_ENABLE_INTERRUPT[asp_id]) {
        } else if (port == PORT_ASP_BOS1810[asp_id]) {
        } else if (port == PORT_ASP_RETI[asp_id]) {
        } else if (port == PORT_ASP_TEST[asp_id]) {
        } else if (port == PORT_ASP_CONTROL[asp_id]) {
        } else if (port == PORT_ASP_U855_PORT_A_DATA[asp_id]) {
        } else if (port == PORT_ASP_U855_PORT_B_DATA[asp_id]) {
        } else if (port == PORT_ASP_U855_PORT_A_INIT[asp_id]) {
        } else if (port == PORT_ASP_U855_PORT_B_INIT[asp_id]) {
        } else if (port == PORT_ASP_U857_TIMER_0[asp_id]) {
        } else if (port == PORT_ASP_U857_TIMER_1[asp_id]) {
        } else if (port == PORT_ASP_U857_TIMER_2[asp_id]) {
        } else if (port == PORT_ASP_U857_TIMER_3[asp_id]) {
        } else if (port == PORT_ASP_U856_DATA_V24[asp_id]) {
        } else if (port == PORT_ASP_U856_DATA_IFSS[asp_id]) {
        } else if (port == PORT_ASP_U856_CONTROL_V24[asp_id]) {
        } else if (port == PORT_ASP_U856_CONTROL_IFSS_V24[asp_id]) {
        } else {
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
        if (port == PORT_ASP_ENABLE_INTERRUPT[asp_id]) {
        } else if (port == PORT_ASP_BOS1810[asp_id]) {
        } else if (port == PORT_ASP_RETI[asp_id]) {
        } else if (port == PORT_ASP_TEST[asp_id]) {
        } else if (port == PORT_ASP_CONTROL[asp_id]) {
        } else if (port == PORT_ASP_U855_PORT_A_DATA[asp_id]) {
        } else if (port == PORT_ASP_U855_PORT_B_DATA[asp_id]) {
        } else if (port == PORT_ASP_U855_PORT_A_INIT[asp_id]) {
        } else if (port == PORT_ASP_U855_PORT_B_INIT[asp_id]) {
        } else if (port == PORT_ASP_U857_TIMER_0[asp_id]) {
        } else if (port == PORT_ASP_U857_TIMER_1[asp_id]) {
        } else if (port == PORT_ASP_U857_TIMER_2[asp_id]) {
        } else if (port == PORT_ASP_U857_TIMER_3[asp_id]) {
        } else if (port == PORT_ASP_U856_DATA_V24[asp_id]) {
        } else if (port == PORT_ASP_U856_DATA_IFSS[asp_id]) {
        } else if (port == PORT_ASP_U856_CONTROL_V24[asp_id]) {
        } else if (port == PORT_ASP_U856_CONTROL_IFSS_V24[asp_id]) {
        } else {
        }
        return 0;
    }

    /**
     * Initialisiert die ASP
     */
    @Override
    public void init() {
        registerPorts();

        LOG.log(Level.SEVERE, "Emulation der ASP noch nicht implementiert!");
        JOptionPane.showMessageDialog(null, "Die Emulation der ASP wird gegenwärtig noch nicht unterstützt!", "ASP nicht unterstützt", JOptionPane.ERROR_MESSAGE);
        System.exit(0);
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
