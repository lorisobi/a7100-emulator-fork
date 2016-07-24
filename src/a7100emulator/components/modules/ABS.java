/*
 * ABS.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * Copyright (c) 2011-2016 Dirk Bräuer
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
 */
package a7100emulator.components.modules;

import a7100emulator.components.system.MMS16Bus;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Klasse zur Abbildung der ABS (Alphanumerische Bildschirmsteuerung)
 * <p>
 * TODO: Diese Klasse ist noch nicht vollständig implementiert.
 *
 * @author Dirk Bräuer
 */
public class ABS implements IOModule {

    /**
     * Port ABS-Zustand
     */
    private final static int PORT_ABS_STATE = 0x200;
    /**
     * Port ABS-Daten
     */
    private final static int PORT_ABS_DATA = 0x202;

    /**
     * Registriert die Ports am Systembus
     */
    @Override
    public void registerPorts() {
        MMS16Bus.getInstance().registerIOPort(this, PORT_ABS_STATE);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ABS_DATA);
    }

    /**
     * Gibt ein Byte auf einem Port aus
     *
     * @param port Port
     * @param data Daten
     */
    @Override
    public void writePortByte(int port, int data) {
        switch (port) {
            case PORT_ABS_STATE:
                break;
            case PORT_ABS_DATA:
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
            case PORT_ABS_STATE:
                break;
            case PORT_ABS_DATA:
                break;
        }
    }

    /**
     * Liest ein byte von einem Port
     *
     * @param port Port
     * @return gelesenes Byte
     */
    @Override
    public int readPortByte(int port) {
        switch (port) {
            case PORT_ABS_STATE:
                break;
            case PORT_ABS_DATA:
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
            case PORT_ABS_STATE:
                break;
            case PORT_ABS_DATA:
                break;
        }
        return 0;
    }

    /**
     * Initialisiert die ABS
     */
    @Override
    public void init() {
        registerPorts();
    }

    /**
     * Schreibt den Zustand der ABS in eine Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Liest den Zustand der ABS aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
