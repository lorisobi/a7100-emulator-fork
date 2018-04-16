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
 *   23.03.2018 - Zugriff auf Arbeitsspeicher implementiert
 *              - Laden der EPROMS implentiert
 *              - CPU hinzugefügt
 *              - Debugging-Methoden ergänzt
 */
package a7100emulator.components.modules;

import a7100emulator.Debug.Decoder;
import a7100emulator.Debug.MemoryAnalyzer;
import a7100emulator.Tools.BitmapGenerator;
import a7100emulator.Tools.ConfigurationManager;
import a7100emulator.Tools.Memory;
import a7100emulator.components.ic.KR580WG75;
import a7100emulator.components.ic.UA880;
import a7100emulator.components.system.GlobalClock;
import a7100emulator.components.system.MMS16Bus;
import a7100emulator.components.system.QuartzCrystal;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JFrame;
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
     * Lokaler Port DMA - Kanal 0 - Adresse
     */
    private final static int LOCAL_PORT_DMA_CH0_ADDRESS = 0x00;
    /**
     * Lokaler Port DMA - Kanal 0 - Zähler
     */
    private final static int LOCAL_PORT_DMA_CH0_COUNT = 0x01;
    /**
     * Lokaler Port DMA - Kanal 1 - Adresse
     */
    private final static int LOCAL_PORT_DMA_CH1_ADDRESS = 0x02;
    /**
     * Lokaler Port DMA - Kanal 1 - Zähler
     */
    private final static int LOCAL_PORT_DMA_CH1_COUNT = 0x03;
    /**
     * Lokaler Port DMA - Kanal 2 - Adresse
     */
    private final static int LOCAL_PORT_DMA_CH2_ADDRESS = 0x04;
    /**
     * Lokaler Port DMA - Kanal 2 - Zähler
     */
    private final static int LOCAL_PORT_DMA_CH2_COUNT = 0x05;
    /**
     * Lokaler Port DMA - Kanal 3 - Adresse
     */
    private final static int LOCAL_PORT_DMA_CH3_ADDRESS = 0x06;
    /**
     * Lokaler Port DMA - Kanal 3 - Zähler
     */
    private final static int LOCAL_PORT_DMA_CH3_COUNT = 0x07;
    /**
     * Lokaler Port DMA - Modus / Status
     */
    private final static int LOCAL_PORT_DMA_MODE_STATUS = 0x08;
    /**
     * Lokaler Port CRT-Parameter
     */
    private final static int LOCAL_PORT_CRT_PARAMETER = 0x20;
    /**
     * Lokaler Port CRT-Parameter
     */
    private final static int LOCAL_PORT_CRT_COMMAND = 0x21;
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
    private Memory ram = new Memory(0x2000);
    /**
     * Zeichensatz - Rom
     */
    private Memory charRom = new Memory(0x800);
    /**
     * Steuerwerk - Rom
     */
    private Memory ctrlRom = new Memory(0x800);
    /**
     * UA880 CPU der ABS
     */
    private final UA880 cpu = new UA880(this, "ABS");
    /**
     * CRT-Controller KR580WG75
     */
    private final KR580WG75 crt = new KR580WG75();
    /**
     * Quarz-CPU Takt
     */
    private final QuartzCrystal cpuClock = new QuartzCrystal(2.67);

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
        int cycles = cpuClock.getCycles(micros);

        cpu.executeCycles(cycles);
    }

    /**
     * Lädt die EPROMS der ABS
     */
    private void initEPROMS() {
        String directory = ConfigurationManager.getInstance().readString("directories", "eproms", "./eproms/");

        final File ctrlRomFile = new File(directory + "ABS-K7071-Q208.rom");
        if (!ctrlRomFile.exists()) {
            LOG.log(Level.SEVERE, "ABS-EPROM {0} nicht gefunden!", ctrlRomFile.getPath());
            JOptionPane.showMessageDialog(null, "Eprom: " + ctrlRomFile.getName() + " nicht gefunden!", "Eprom nicht gefunden", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        final File absRom1 = new File(directory + "ABS-K7071-Q209.rom");
        if (!absRom1.exists()) {
            LOG.log(Level.SEVERE, "ABS-EPROM {0} nicht gefunden!", absRom1.getPath());
            JOptionPane.showMessageDialog(null, "Eprom: " + absRom1.getName() + " nicht gefunden!", "Eprom nicht gefunden", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        final File absRom2 = new File(directory + "ABS-K7071-Q210.rom");
        if (!absRom1.exists()) {
            LOG.log(Level.SEVERE, "ABS-EPROM {0} nicht gefunden!", absRom1.getPath());
            JOptionPane.showMessageDialog(null, "Eprom: " + absRom1.getName() + " nicht gefunden!", "Eprom nicht gefunden", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        final File charRomFile = new File(directory + "ABS-K7071-Q211.rom");
        if (!charRomFile.exists()) {
            LOG.log(Level.SEVERE, "ABS-EPROM {0} nicht gefunden!", charRomFile.getPath());
            JOptionPane.showMessageDialog(null, "Eprom: " + charRomFile.getName() + " nicht gefunden!", "Eprom nicht gefunden", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        try {
            ctrlRom.loadFile(0x0000, ctrlRomFile, Memory.FileLoadMode.LOW_AND_HIGH_BYTE);
            ram.loadFile(0x0000, absRom1, Memory.FileLoadMode.LOW_AND_HIGH_BYTE);
            ram.loadFile(0x0800, absRom2, Memory.FileLoadMode.LOW_AND_HIGH_BYTE);
            charRom.loadFile(0x0000, charRomFile, Memory.FileLoadMode.LOW_AND_HIGH_BYTE);
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
                System.out.println("Daten zur ABS:" + String.format("%02X", data));
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
                result = state;
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
            case LOCAL_PORT_DMA_CH0_ADDRESS:
                break;
            case LOCAL_PORT_DMA_CH0_COUNT:
                break;
            case LOCAL_PORT_DMA_CH1_ADDRESS:
                break;
            case LOCAL_PORT_DMA_CH1_COUNT:
                break;
            case LOCAL_PORT_DMA_CH2_ADDRESS:
                break;
            case LOCAL_PORT_DMA_CH2_COUNT:
                break;
            case LOCAL_PORT_DMA_CH3_ADDRESS:
                break;
            case LOCAL_PORT_DMA_CH3_COUNT:
                break;
            case LOCAL_PORT_DMA_MODE_STATUS:
                break;
            case LOCAL_PORT_CRT_PARAMETER:
                break;
            case LOCAL_PORT_CRT_COMMAND:
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
                LOG.log(Level.WARNING, "Lesen von nicht definiertem Port {0}!", String.format("0x%02X", port));
                break;
        }
        return 0;
    }

    @Override
    public void writeLocalPort(int port, int data) {
        switch (port) {
            case LOCAL_PORT_DMA_CH0_ADDRESS:
                break;
            case LOCAL_PORT_DMA_CH0_COUNT:
                break;
            case LOCAL_PORT_DMA_CH1_ADDRESS:
                break;
            case LOCAL_PORT_DMA_CH1_COUNT:
                break;
            case LOCAL_PORT_DMA_CH2_ADDRESS:
                break;
            case LOCAL_PORT_DMA_CH2_COUNT:
                break;
            case LOCAL_PORT_DMA_CH3_ADDRESS:
                break;
            case LOCAL_PORT_DMA_CH3_COUNT:
                break;
            case LOCAL_PORT_DMA_MODE_STATUS:
                break;
            case LOCAL_PORT_CRT_PARAMETER:
                break;
            case LOCAL_PORT_CRT_COMMAND:
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
                LOG.log(Level.WARNING, "Schreiben auf nicht definiertem Port {0}!", String.format("0x%02X", port));
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
        address &= 0x3FFF;
        return ram.readByte(address);
    }

    /**
     * Liest ein Wort von der angegebenen Adresse aus dem lokalen Speicher.
     *
     * @param address Adresse
     * @return gelesenes Wort
     */
    @Override
    public int readLocalWord(int address) {
        address &= 0x3FFF;
        return ram.readWord(address);
    }

    /**
     * Schreibt ein Byte an die angegebene Adresse im lokalen Speicher.
     *
     * @param address Adresse
     * @param data Zu schreibendes Byte
     */
    @Override
    public void writeLocalByte(int address, int data) {
        address &= 0x3FFF;
        ram.writeByte(address, data);
    }

    /**
     * Schreibt ein Wort an die angegebene Adresse im lokalen Speicher.
     *
     * @param address Adresse
     * @param data Zu schreibendes Wort
     */
    @Override
    public void writeLocalWord(int address, int data) {
        address &= 0x3FFF;
        ram.writeWord(address, data);
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

    }

    @Override
    public void requestInterrupt(int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Zeigt den aktuellen Zeichensatz in einem separaten Fenster an TODO: Auf
     * RAM erweitern
     */
    public void showCharacters() {
        final BufferedImage characterImage = new BufferedImage(512, 384, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < 128; i++) {
            int x = (i / 16) * 32 + 24;
            int y = (i % 16) * 24;
            // Darstellbares Zeichen
            byte[] linecode = new byte[16];
            for (byte line = 0; line < 16; line++) {
                linecode[line] = (byte) (charRom.readByte((i << 4) + line) & 0xFF);
            }
            BufferedImage character = BitmapGenerator.generateBitmapFromLineCode(linecode, false, false, false, false);
            characterImage.getGraphics().drawImage(character, x, y, null);
            characterImage.getGraphics().drawString(String.format("%02X", (byte) i), x - 20, y + 10);
        }
        JFrame frame = new JFrame("Zeichentabelle");
        frame.setResizable(false);

        JComponent component = new JComponent() {

            @Override
            public void paint(Graphics g) {
                g.drawImage(characterImage, 0, 0, null);
            }
        };
        component.setMinimumSize(new Dimension(512, 384));
        component.setPreferredSize(new Dimension(512, 384));
        frame.add(component);

        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            LOG.log(Level.FINEST, null, ex);
        }
        frame.setVisible(true);
        frame.pack();
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
    }

    /**
     * Zeigt den Speicher der ABS an.
     */
    public void showMemory() {
        (new MemoryAnalyzer(ram, "ABS-Speicher")).show();
    }

    /**
     * Schreibt den Inhalt des ABS-RAM in eine Datei.
     *
     * @param filename Dateiname
     * @throws java.io.IOException Wenn das Speichern des Speicherninhaltes auf
     * dem Datenträger nicht erfolgreich war
     */
    public void dumpLocalMemory(String filename) throws IOException {
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(filename));
        ram.saveMemory(dos);
        dos.close();
    }

    /**
     * Aktiviert oder deaktiviert den Debugger der CPU.
     *
     * @param debug <code>true</code> - zum Aktivieren des Debuggers,
     * <code>false</code> - Zum Deaktivieren des Debuggers
     */
    public void setDebug(boolean debug) {
        LOG.log(Level.CONFIG, "Debugger des ABS {0}", new String[]{(debug ? "aktiviert" : "deaktiviert")});
        cpu.setDebug(debug);
    }

    /**
     * Gibt an ob der Debugger aktiviert ist.
     *
     * @return <code>true</code> - wenn Debugger aktiviert ist,
     * <code>false</code> - sonst
     */
    public boolean isDebug() {
        return cpu.isDebug();
    }

    /**
     * Gibt die Instanz des CPU-Decoders zurück.
     *
     * @return Decoderinstanz oder <code>null</code> wenn kein Decoder
     * initialisiert ist.
     */
    public Decoder getDecoder() {
        return cpu.getDecoder();
    }
}
