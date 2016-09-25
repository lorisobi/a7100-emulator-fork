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
 *   09.08.2016 - Logger hinzugefügt
 *   19.08.2016 - Speicher, Ports und Methoden ergänzt
 */
package a7100emulator.components.modules;

import a7100emulator.Tools.Memory;
import a7100emulator.components.system.GlobalClock;
import a7100emulator.components.system.MMS16Bus;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Klasse zur Abbildung der ABS (Alphanumerische Bildschirmsteuerung)
 * <p>
 * TODO: Diese Klasse ist noch nicht vollständig implementiert.
 *
 * @author Dirk Bräuer
 */
public final class ABS implements IOModule, ClockModule, SubsystemModule {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(ABS.class.getName());

    /**
     * Port ABS-Zustand
     */
    private final static int PORT_ABS_STATE = 0x200;
    /**
     * Port ABS-Daten
     */
    private final static int PORT_ABS_DATA = 0x202;

    /**
     * Lokaler Port DMA
     */
    private final static int LOCAL_PORT_DMA = 0x00;
    /**
     * Lokaler Port CRT
     */
    private final static int LOCAL_PORT_CRT = 0x20;
    /**
     * Lokaler Port Matrixregister
     */
    private final static int LOCAL_PORT_MATRIX = 0x30;
    /**
     * Lokaler Port Zeilenzähler
     */
    private final static int LOCAL_PORT_LINE = 0x40;
    /**
     * Lokaler Port INT-FF
     */
    private final static int LOCAL_PORT_INT = 0x50;
    /**
     * Lokaler Port ERR-FF
     */
    private final static int LOCAL_PORT_ERR = 0x60;
    /**
     * Lokaler Port Status
     */
    private final static int LOCAL_PORT_STATUS = 0x70;
    /**
     * Lokaler Port E/A Register
     */
    private final static int LOCAL_PORT_EA = 0x71;

    /**
     * Statusregister
     */
    private int status = 0x00;
    /**
     * Datenregister für Eingabe
     */
    private int dataIn = 0x00;
    /**
     * Datenregister für Ausgabe
     */
    private int dataOut = 0x00;

    /**
     * Programmspeicher + Arbeitsspeicher
     */
    private Memory ram = new Memory(0x1C00);
    /**
     * Zeichensatz - Rom
     */
    private Memory charRom = new Memory(0x1000);
    

    /**
     * Erstellt eine neue ABS
     */
    public ABS() {
        init();
    }

    /**
     * Initialisiert die ABS
     */
    @Override
    public void init() {
        registerPorts();
        registerClocks();
        initEPROMS();
    }

    /**
     * Registriert die Ports am Systembus
     */
    @Override
    public void registerPorts() {
        MMS16Bus.getInstance().registerIOPort(this, PORT_ABS_STATE);
        MMS16Bus.getInstance().registerIOPort(this, PORT_ABS_DATA);
    }

    /**
     * Registriert das Modul für Änderungen der Systemzeit
     */
    @Override
    public void registerClocks() {
        GlobalClock.getInstance().registerModule(this);
    }

    /**
     * Verarbeitet Änderungen der Systemzeit. Diese Funktion lässt den UA880
     * Prozessor Befehle abarbeiten. Die Anzahl der Befehle hängt von der
     * übergebenen Anzahl an Mikrosekunden ab. Die Anderen vom Taktgeber
     * abhängigen Komponenten werden ebenfalls benachrichtigt.
     *
     * @param micros Zeitdauer in Mikrosekunden
     */
    @Override
    public void clockUpdate(int micros) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void initEPROMS() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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

    @Override
    public int readLocalPort(int port) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeLocalPort(int port, int data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int readLocalByte(int address) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int readLocalWord(int address) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeLocalByte(int address, int data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeLocalWord(int address, int data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void localClockUpdate(int cycles) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void requestInterrupt(int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
