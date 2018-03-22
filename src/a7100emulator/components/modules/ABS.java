/*
 * ABS.java
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
 *   09.08.2016 - Logger hinzugefügt
 *   19.08.2016 - Speicher, Ports und Methoden ergänzt
 */
package a7100emulator.components.modules;

import a7100emulator.Tools.ConfigurationManager;
import a7100emulator.Tools.Memory;
import a7100emulator.components.system.GlobalClock;
import a7100emulator.components.system.MMS16Bus;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

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
     * Error-Bit
     */
    private static final int ERR_BIT = 0x80;
    /**
     * Interrupt-Bit
     */
    private static final int INT_BIT = 0x04;
    /**
     * Input-Buffer-Full-Bit
     */
    private static final int IBF_BIT = 0x02;
    /**
     * Output-Buffer-Full-Bit
     */
    private static final int OBF_BIT = 0x01;

    /**
     * Statusregister
     */
    private int state = 0x00;
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

    /**
     * Lädt die EPROMS der ABS
     */
    private void initEPROMS() {
        String directory = ConfigurationManager.getInstance().readString("directories", "eproms", "./eproms/");

        final File absRom1 = new File(directory + "ABS-K7071-.rom");
        if (!absRom1.exists()) {
            LOG.log(Level.SEVERE, "ABS-EPROM {0} nicht gefunden!", absRom1.getPath());
            JOptionPane.showMessageDialog(null, "Eprom: " + absRom1.getName() + " nicht gefunden!", "Eprom nicht gefunden", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        final File absRom2 = new File(directory + "ABS-K7071-.rom");
        if (!absRom1.exists()) {
            LOG.log(Level.SEVERE, "ABS-EPROM {0} nicht gefunden!", absRom1.getPath());
            JOptionPane.showMessageDialog(null, "Eprom: " + absRom1.getName() + " nicht gefunden!", "Eprom nicht gefunden", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        final File charRom1 = new File(directory + "ABS-K7071-.rom");
        if (!charRom1.exists()) {
            LOG.log(Level.SEVERE, "ABS-EPROM {0} nicht gefunden!", charRom1.getPath());
            JOptionPane.showMessageDialog(null, "Eprom: " + charRom1.getName() + " nicht gefunden!", "Eprom nicht gefunden", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        final File charRom2 = new File(directory + "ABS-K7071-.rom");
        if (!charRom2.exists()) {
            LOG.log(Level.SEVERE, "ABS-EPROM {0} nicht gefunden!", charRom2.getPath());
            JOptionPane.showMessageDialog(null, "Eprom: " + charRom2.getName() + " nicht gefunden!", "Eprom nicht gefunden", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        try {
            ram.loadFile(0x0000, absRom1, Memory.FileLoadMode.LOW_AND_HIGH_BYTE);
            ram.loadFile(0x0800, absRom2, Memory.FileLoadMode.LOW_AND_HIGH_BYTE);
            charRom.loadFile(0x0000, charRom1, Memory.FileLoadMode.LOW_AND_HIGH_BYTE);
            charRom.loadFile(0x0800, charRom2, Memory.FileLoadMode.LOW_AND_HIGH_BYTE);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Fehler beim Laden der ABS-ROMS!", ex);
            System.exit(0);
        }
    }

    /**
     * Gibt ein Byte auf einem Port aus
     *
     * @param port Port
     * @param data Daten
     */
    @Override
    public void writePortByte(int port, int data) {
        // TODO: Ggf. zusätzliche Adresse 0xYYX0,0xYYX4,0xYYX8,0xYYXC ergänzen
        switch (port) {
            case PORT_ABS_STATE:
                clearBit(INT_BIT);
                clearBit(ERR_BIT);
                break;
            case PORT_ABS_DATA:
                dataIn = data;
                setBit(IBF_BIT);
                break;
            default:
                LOG.log(Level.FINE, "Schreiben auf nicht definiertem Port {0}!", String.format("0x%02X", port));
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
        writePortByte(port, data);
    }

    /**
     * Liest ein byte von einem Port
     *
     * @param port Port
     * @return gelesenes Byte
     */
    @Override
    public int readPortByte(int port) {
        // TODO: Ggf. zusätzliche Adresse 0xYYX0,0xYYX4,0xYYX8,0xYYXC ergänzen
        int result = 0;
        switch (port) {
            case PORT_ABS_STATE:
                result= state;
                break;
            case PORT_ABS_DATA:
                result = dataOut;
                clearBit(OBF_BIT);
                clearBit(INT_BIT);
                break;
            default:
                LOG.log(Level.FINE, "Lesen von nicht definiertem Port {0}!", String.format("0x%02X", port));
                break;
        }
        return result;
    }

    /**
     * Liest ein Wort von einem Port
     *
     * @param port Port
     * @return gelesenes Wort
     */
    @Override
    public int readPortWord(int port) {
        return readPortByte(port);
    }

    @Override
    public int readLocalPort(int port) {
        switch (port) {
            case LOCAL_PORT_DMA:
                break;
            case LOCAL_PORT_CRT:
                break;
            case LOCAL_PORT_MATRIX:
                break;
            case LOCAL_PORT_LINE:
                break;
            case LOCAL_PORT_INT:
                break;
            case LOCAL_PORT_ERR:
                break;
            case LOCAL_PORT_STATUS:
                break;
            case LOCAL_PORT_EA:
                break;
            default:
                LOG.log(Level.FINE, "Lesen von nicht definiertem Port {0}!", String.format("0x%02X", port));
                break;
        }
        return 0;
    }

    @Override
    public void writeLocalPort(int port, int data) {
        switch (port) {
            case LOCAL_PORT_DMA:
                break;
            case LOCAL_PORT_CRT:
                break;
            case LOCAL_PORT_MATRIX:
                break;
            case LOCAL_PORT_LINE:
                break;
            case LOCAL_PORT_INT:
                break;
            case LOCAL_PORT_ERR:
                break;
            case LOCAL_PORT_STATUS:
                break;
            case LOCAL_PORT_EA:
                break;
            default:
                LOG.log(Level.FINE, "Schreiben auf nicht definiertem Port {0}!", String.format("0x%02X", port));
                break;
        }
    }

    /**
     * Liest ein Byte von der angegebenen Adresse aus dem lokalen Speicher.
     *
     * @param address Adresse
     * @return gelesenes Byte
     */
    @Override
    public int readLocalByte(int address) {
        if (address < 0x1000) {
            // Unterer Adressbereich normal
            return ram.readByte(address);
        } else if (address >= 0x1400) {
            // Überspringe Lücke zwischen 0x1000 und 0x1400
            return ram.readByte(address - 0x400);
        } else {
            // Zugriff auf nicht definierten Speicher zwischen 0x1000 und 0x1400
            LOG.log(Level.FINER, "Lesen von nicht definierter Speicheradresse {0}!", new String[]{String.format("%04X", address)});
            return 0;
        }
    }

    /**
     * Liest ein Wort von der angegebenen Adresse aus dem lokalen Speicher.
     *
     * @param address Adresse
     * @return gelesenes Wort
     */
    @Override
    public int readLocalWord(int address) {
        if (address < 0x1000) {
            // Unterer Adressbereich normal
            return ram.readWord(address);
        } else if (address >= 0x1400) {
            // Überspringe Lücke zwischen 0x1000 und 0x1400
            return ram.readWord(address - 0x400);
        } else {
            // Zugriff auf nicht definierten Speicher zwischen 0x1000 und 0x1400
            LOG.log(Level.FINER, "Lesen von nicht definierter Speicheradresse {0}!", new String[]{String.format("%04X", address)});
            return 0;
        }
    }

    /**
     * Schreibt ein Byte an die angegebene Adresse im lokalen Speicher.
     *
     * @param address Adresse
     * @param data Zu schreibendes Byte
     */
    @Override
    public void writeLocalByte(int address, int data) {
        if (address < 0x1000) {
            // Unterer Adressbereich normal
            ram.writeByte(address, data);
        } else if (address >= 0x1400) {
            // Überspringe Lücke zwischen 0x1000 und 0x1400
            ram.writeByte(address - 0x400, data);
        } else {
            // Zugriff auf nicht definierten Speicher zwischen 0x1000 und 0x1400
            LOG.log(Level.FINER, "Schreiben auf nicht definierter Speicheradresse {0}!", new String[]{String.format("%04X", address)});
        }
    }

    /**
     * Schreibt ein Wort an die angegebene Adresse im lokalen Speicher.
     *
     * @param address Adresse
     * @param data Zu schreibendes Wort
     */
    @Override
    public void writeLocalWord(int address, int data) {
        if (address < 0x1000) {
            // Unterer Adressbereich normal
            ram.writeWord(address, data);
        } else if (address >= 0x1400) {
            // Überspringe Lücke zwischen 0x1000 und 0x1400
            ram.writeWord(address - 0x400, data);
        } else {
            // Zugriff auf nicht definierten Speicher zwischen 0x1000 und 0x1400
            LOG.log(Level.FINER, "Schreiben auf nicht definierter Speicheradresse {0}!", new String[]{String.format("%04X", address)});
        }
    }

    /**
     * Setzt ein Bit im Statusbyte
     *
     * @param bit zu setzendes Bit
     */
    private void setBit(int bit) {
        state |= bit;
    }

    /**
     * Löscht ein Bit im Satusbyte
     *
     * @param bit Zu löschendes Bit
     */
    private void clearBit(int bit) {
        state &= ~bit;
    }

    @Override
    public void localClockUpdate(int cycles) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void requestInterrupt(int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Schreibt den Zustand der ABS in eine Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        dos.writeInt(state);
        dos.writeInt(dataIn);
        dos.writeInt(dataOut);
        ram.saveMemory(dos);
        charRom.saveMemory(dos);
    }

    /**
     * Liest den Zustand der ABS aus einer Datei.
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        state = dis.readInt();
        dataIn = dis.readInt();
        dataOut = dis.readInt();
        ram.loadMemory(dis);
        charRom.loadMemory(dis);
    }
}
