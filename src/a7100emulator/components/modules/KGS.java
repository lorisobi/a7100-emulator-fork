/*
 * KGS.java
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
 *   07.08.2014 - Erste Version aus KGS.java kopiert
 *   09.08.2014 - Zugriffe auf SystemMemory, SystemPorts und SystemClock 
 *                durch MMS16Bus ersetzt
 *   30.09.2014 - Umbenannt in KGS
 *              - Kommentare vervollständigt
 *              - Darstellung funktionstüchtig
 *   19.11.2014 - Trennung Systemtakt / CPU-Takt
 *   04.12.2014 - Dump RAM implementiert
 *   30.11.2015 - Speicherzugriffsmethoden umbenannt
 *   01.12.2015 - Kommentare korrigiert
 *   23.07.2016 - Quartz hinzugefügt
 *   24.07.2016 - Parameter umbenannt
 *              - localClockUpdate() bis auf CTC ohne Funktion
 *              - Speichern von Quartz-Informationen 
 *   28.07.2016 - Methode getDecoder() hinzugefügt
 *   29.07.2016 - IOException beim Speichern des KGS-Rams hinzugefügt
 */
package a7100emulator.components.modules;

import a7100emulator.Debug.Decoder;
import a7100emulator.Debug.MemoryAnalyzer;
import a7100emulator.Tools.BitmapGenerator;
import a7100emulator.Tools.Memory;
import a7100emulator.components.ic.UA856;
import a7100emulator.components.ic.UA857;
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
 * Klasse zur Abbildung des KGS (Kontroller für grafisches Subsytem)
 *
 * @author Dirk Bräuer
 */
public final class KGS implements IOModule, ClockModule, SubsystemModule {

    /**
     * Arbeitspeicher des KGS
     */
    private final Memory ram = new Memory(0x10000);

    /**
     * Port KGS-Zustand
     */
    private final static int PORT_KGS_STATE = 0x200;
    /**
     * Port KGS-Daten
     */
    private final static int PORT_KGS_DATA = 0x202;

    /**
     * Lokaler Port CTC Kanal 0
     */
    private final static int LOCAL_PORT_CTC_CHANNEL_0 = 0x00;
    /**
     * Lokaler Port CTC Kanal 1
     */
    private final static int LOCAL_PORT_CTC_CHANNEL_1 = 0x01;
    /**
     * Lokaler Port CTC Kanal 2
     */
    private final static int LOCAL_PORT_CTC_CHANNEL_2 = 0x02;
    /**
     * Lokaler Port CTC Kanal 3
     */
    private final static int LOCAL_PORT_CTC_CHANNEL_3 = 0x03;
    /**
     * Lokaler Port SIO Daten Kanal A
     */
    private final static int LOCAL_PORT_SIO_DATA_A = 0x08;
    /**
     * Lokaler Port SIO Control Kanal A
     */
    private final static int LOCAL_PORT_SIO_CONTROL_A = 0x09;
    /**
     * Lokaler Port SIO Daten Kanal B
     */
    private final static int LOCAL_PORT_SIO_DATA_B = 0x0A;
    /**
     * Lokaler Port SIO Control Kanal B
     */
    private final static int LOCAL_PORT_SIO_CONTROL_B = 0x0B;
    /**
     * Lokaler Port Eingaberegister
     */
    private final static int LOCAL_PORT_INPUT = 0x10;
    /**
     * Lokaler Port Ausgaberegister
     */
    private final static int LOCAL_PORT_OUTPUT = 0x11;
    /**
     * Lokaler Port Statusregister
     */
    private final static int LOCAL_PORT_STATE = 0x12;
    /**
     * Lokaler Port INT-Flag
     */
    private final static int LOCAL_PORT_INT_FLAG = 0x13;
    /**
     * Lokaler Port ERR-Flag
     */
    private final static int LOCAL_PORT_ERR_FLAG = 0x14;
    /**
     * Lokaler Port Memory Select Register
     */
    private final static int LOCAL_PORT_MSEL = 0x15;
    /**
     * Lokaler Port Select-Byte 0
     */
    private final static int LOCAL_PORT_SELECT_BYTE_0 = 0x16;
    /**
     * Lokaler Port Select-Byte 1
     */
    private final static int LOCAL_PORT_SELECT_BYTE_1 = 0x17;

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
     * Statusbyte
     */
    private int state = 0x03;
    /**
     * Datenbyte Eingabe
     */
    private int dataIn = 0x00;
    /**
     * Datenbyte Ausgabe
     */
    private int dataOut = 0x00;
    /**
     * Referenz auf die ABG (Anschlußsteuerung für grafischen Bildschirm)
     */
    private ABG abg;
    /**
     * Select Byte 0
     */
    private int selectByte0 = 0x03;
    /**
     * Select Byte 1
     */
    private int selectByte1 = 0x1C;
    /**
     * UA880 CPU der KGS
     */
    private final UA880 cpu = new UA880(this, "KGS");
    /**
     * UA856 SIO der KGS
     */
    private final UA856 sio = new UA856();
    /**
     * UA857 CTC der KGS
     */
    private final UA857 ctc = new UA857(this);
    /**
     * Memory-Select-Register
     */
    private int msel;
    /**
     * Quarz-CPU Takt
     */
    private final QuartzCrystal cpuClock = new QuartzCrystal(4.0);

    /**
     * Erstellt eine neue KGS
     */
    public KGS() {
        init();
    }

    /**
     * Registriert die Ports der KGS
     */
    @Override
    public void registerPorts() {
        MMS16Bus.getInstance().registerIOPort(this, PORT_KGS_STATE);
        MMS16Bus.getInstance().registerIOPort(this, PORT_KGS_DATA);
    }

    /**
     * Gibt ein Byte auf einem Systemport aus.
     *
     * @param port Port
     * @param data Daten
     */
    @Override
    public void writePortByte(int port, int data) {
        switch (port) {
            case PORT_KGS_STATE:
                clearBit(INT_BIT);
                clearBit(ERR_BIT);
                break;
            case PORT_KGS_DATA:
                dataIn = data;
                setBit(IBF_BIT);
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
     * Liest ein Byte von einem Port
     *
     * @param port Port
     * @return gelesenes Byte
     */
    @Override
    public int readPortByte(int port) {
        int result = 0;
        switch (port) {
            case PORT_KGS_STATE:
                result = state;
                break;
            case PORT_KGS_DATA:
                result = dataOut;
                clearBit(OBF_BIT);
                clearBit(INT_BIT);
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

    /**
     * Liest ein Byte von einem Lokalen Port
     *
     * @param port Port
     * @return gelesenes Byte
     */
    @Override
    public int readLocalPort(int port) {
        switch (port) {
            case LOCAL_PORT_CTC_CHANNEL_0:
                return ctc.readChannel(0);
            case LOCAL_PORT_CTC_CHANNEL_1:
                return ctc.readChannel(1);
            case LOCAL_PORT_CTC_CHANNEL_2:
                return ctc.readChannel(2);
            case LOCAL_PORT_CTC_CHANNEL_3:
                return ctc.readChannel(3);
            case LOCAL_PORT_SIO_DATA_A:
                return sio.readData(0);
            case LOCAL_PORT_SIO_CONTROL_A:
                return sio.readControl(0);
            case LOCAL_PORT_SIO_DATA_B:
                return sio.readData(1);
            case LOCAL_PORT_SIO_CONTROL_B:
                return sio.readControl(1);
            case LOCAL_PORT_INPUT:
                clearBit(IBF_BIT);
                return dataIn;
            case LOCAL_PORT_OUTPUT:
                throw new IllegalArgumentException("Lesen von OUTPUT Port nicht erlaubt");
            case LOCAL_PORT_STATE:
                return state;
            case LOCAL_PORT_INT_FLAG:
                System.out.println("Lesen von INT-Flag noch nicht implementiert");
                break;
            case LOCAL_PORT_ERR_FLAG:
                System.out.println("Lesen von ERR-Flag noch nicht implementiert");
                break;
            case LOCAL_PORT_MSEL:
                throw new IllegalArgumentException("Lesen von MSEL Port nicht erlaubt");
            case LOCAL_PORT_SELECT_BYTE_0:
                return selectByte0;
            case LOCAL_PORT_SELECT_BYTE_1:
                return selectByte1;
        }
        return 0;
    }

    /**
     * Gibt ein Byte auf einem lokalen Port aus
     *
     * @param port Port
     * @param data Daten
     */
    @Override
    public void writeLocalPort(int port, int data) {
        switch (port) {
            case LOCAL_PORT_CTC_CHANNEL_0:
                ctc.writeChannel(0, data);
                break;
            case LOCAL_PORT_CTC_CHANNEL_1:
                ctc.writeChannel(1, data);
                break;
            case LOCAL_PORT_CTC_CHANNEL_2:
                ctc.writeChannel(2, data);
                break;
            case LOCAL_PORT_CTC_CHANNEL_3:
                ctc.writeChannel(3, data);
                break;
            case LOCAL_PORT_SIO_DATA_A:
                sio.writeData(0, data);
                //System.out.println("Schreibe Kanal A:" + String.format("%02X", data));
                break;
            case LOCAL_PORT_SIO_CONTROL_A:
                sio.writeControl(0, data);
                break;
            case LOCAL_PORT_SIO_DATA_B:
                sio.writeData(1, data);
                //System.out.println("Schreibe Kanal B:" + String.format("%02X", data));
                break;
            case LOCAL_PORT_SIO_CONTROL_B:
                sio.writeControl(1, data);
                break;
            case LOCAL_PORT_INPUT:
                throw new IllegalArgumentException("Schreiben auf INPUT Port nicht erlaubt");
            case LOCAL_PORT_OUTPUT:
                dataOut = data;
                setBit(OBF_BIT);
                break;
            case LOCAL_PORT_STATE:
                throw new IllegalArgumentException("Schreiben auf STATUS Port nicht erlaubt");
            case LOCAL_PORT_INT_FLAG:
                setBit(INT_BIT);
                MMS16Bus.getInstance().requestInterrupt(7);
                break;
            case LOCAL_PORT_ERR_FLAG:
                setBit(ERR_BIT);
                break;
            case LOCAL_PORT_MSEL:
                msel = data;
                break;
            case LOCAL_PORT_SELECT_BYTE_0:
                throw new IllegalArgumentException("Schreiben von DSEL0 Port nicht erlaubt");
            case LOCAL_PORT_SELECT_BYTE_1:
                throw new IllegalArgumentException("Schreiben von DSEL1 Port nicht erlaubt");
            case 0x20:
            case 0x21:
            case 0x22:
            case 0x23:
                abg.writeLocalPort(port, data);
                break;
        }
    }

    /**
     * Schreibt ein Wort in den Arbeitsspeicher der KGS/ABG. Abhängig vom
     * eingestellten Memory-Select-Register werden die Daten in den lokalen RAM
     * des KGS oder in den Bildwiederholspeicher der ABG geschrieben.
     *
     * @param address Adresse
     * @param data Daten
     */
    @Override
    public void writeLocalWord(int address, int data) {
        // Zugriffe auf lokalen KGS-Ram
        if (address <= 0x7FFF || msel == 0) {
            ram.writeWord(address, data);
        } else {
            abg.writeWord(msel, ~address & 0x7FFF, data);
        }
    }

    /**
     * Schreibt ein Byte in den Arbeitsspeicher der KGS/ABG. Abhängig vom
     * eingestellten Memory-Select-Register werden die Daten in den lokalen RAM
     * des KGS oder in den Bildwiederholspeicher der ABG geschrieben.
     *
     * @param address Adresse
     * @param data Daten
     */
    @Override
    public void writeLocalByte(int address, int data) {
        // Zugriffe auf lokalen KGS-Ram
        if (address <= 0x7FFF || msel == 0) {
            ram.writeByte(address, data);
        } else {
            abg.writeByte(msel, ~address & 0x7FFF, data);
        }
    }

    /**
     * Liest ein Wort aus dem Arbeitsspeicher der KGS/ABG. Abhängig vom
     * eingestellten Memory-Select-Register werden die Daten aus dem lokalen RAM
     * des KGS oder aus dem Bildwiederholspeicher der ABG gelesen.
     *
     * @param address Adresse
     * @return gelesenes Wort
     */
    @Override
    public int readLocalWord(int address) {
        // Zugriffe auf lokalen KGS-Ram
        if (address <= 0x7FFF || msel == 0) {
            return ram.readWord(address);
        } else {
            return abg.readWord(msel, ~address & 0x7FFF);
        }
    }

    /**
     * Liest ein Byte aus dem Arbeitsspeicher der KGS/ABG. Abhängig vom
     * eingestellten Memory-Select-Register werden die Daten aus dem lokalen RAM
     * des KGS oder aus dem Bildwiederholspeicher der ABG gelesen.
     *
     * @param address Adresse
     * @return gelesenes Byte
     */
    @Override
    public int readLocalByte(int address) {
        // Zugriffe auf lokalen KGS-Ram
        if (address <= 0x7FFF || msel == 0) {
            return ram.readByte(address);
        } else {
            return abg.readByte(msel, ~address & 0x7FFF);
        }
    }

    /**
     * Schreibt den Inhalt des KGS-RAM in eine Datei.
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
     * Reicht die Anforderung einen nichtmaskierbaren Interrupt zu verarbeiten
     * an die CPU weiter
     */
    void requestNMI() {
        if (!sio.isRTS(1)) {
            cpu.requestNMI();
        }
    }

    /**
     * Initialisiert den KGS
     */
    @Override
    public void init() {
        final File kgsRom = new File("./eproms/KGS-K7070-152.rom");
        if (!kgsRom.exists()) {
            JOptionPane.showMessageDialog(null, "Eprom: " + kgsRom.getName() + " nicht gefunden!", "Eprom nicht gefunden", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        ram.loadFile(0x00, kgsRom, Memory.FileLoadMode.LOW_AND_HIGH_BYTE);
        abg = new ABG(this);
        registerPorts();
        registerClocks();
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
     * übergebenen Anzahl an Mikrosekunden ab. Andere Komponenten des Systems
     * werden nicht benachrichtigt.
     *
     * @param micros Zeitdauer in Mikrosekunden
     */
    @Override
    public void clockUpdate(int micros) {
        int cycles = cpuClock.getCycles(micros);

        cpu.executeCycles(cycles);

        // TODO: CTC hierher verlagern, bisher Probleme mit letztem CTC Interrupt
//        ctc.updateClock(cycles);
        sio.updateClock(cycles);
        abg.updateClock(cycles);
    }

    /**
     * Zeigt den aktuellen Zeichensatz in einem separaten Fenster an
     */
    public void showCharacters() {
        final BufferedImage characterImage = new BufferedImage(512, 384, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < 256; i++) {
            int x = (i / 16) * 32 + 24;
            int y = (i % 16) * 24;
            // Darstellbares Zeichen
            byte[] linecode = new byte[16];
            for (byte line = 0; line < 16; line++) {
                linecode[line] = (byte) (ram.readByte(0x1800 + (i << 4) + line) & 0xFF);
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
            Logger.getLogger(KGS.class.getName()).log(Level.SEVERE, null, ex);
        }
        frame.setVisible(true);
        frame.pack();
    }

    /**
     * Speichert den Zustand der KGS in einer Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        ram.saveMemory(dos);
        dos.writeInt(state);
        dos.writeInt(dataIn);
        dos.writeInt(dataOut);
        dos.writeInt(selectByte0);
        dos.writeInt(selectByte1);
        dos.writeInt(msel);
        cpu.saveState(dos);
        sio.saveState(dos);
        ctc.saveState(dos);
        abg.saveState(dos);
        cpuClock.saveState(dos);
    }

    /**
     * Liest den Zustand der KGS aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        ram.loadMemory(dis);
        state = dis.readInt();
        dataIn = dis.readInt();
        dataOut = dis.readInt();
        selectByte0 = dis.readInt();
        selectByte1 = dis.readInt();
        msel = dis.readInt();
        cpu.loadState(dis);
        sio.loadState(dis);
        ctc.loadState(dis);
        abg.loadState(dis);
        cpuClock.loadState(dis);
    }

    /**
     * Leitet die Anfrage der Interruptbehandlung an die CPU weiter
     *
     * @param i Interruptnummer
     */
    @Override
    public void requestInterrupt(int i) {
        //System.out.println("Interrupt " + i + " auf KGS!");
        cpu.requestInterrupt(i);
    }

    /**
     * Aktiviert oder deaktiviert den Debugger der CPU
     *
     * @param debug true - Aktivieren, false - Deaktivieren
     */
    public void setDebug(boolean debug) {
        cpu.setDebug(debug);
    }

    /**
     * Aktualisiert die Systemzeit der Komponenten des KGS auf Basis der
     * Taktzyklen des UA880.
     *
     * @param cycles Anzahl der Takte
     */
    @Override
    public void localClockUpdate(int cycles) {
        // TODO: CTC hier entfernen, bisher Probleme mit letztem CTC Interrupt
        ctc.updateClock(cycles);
    }

    /**
     * Zeigt den Speicher des KGS an.
     */
    public void showMemory() {
        (new MemoryAnalyzer(ram, "KGS-Speicher")).show();
    }

    /**
     * Gibt die Referenz auf die ABG zurück.
     *
     * @return ABG
     */
    public ABG getABG() {
        return abg;
    }

    /**
     * Gibt an, ob noch ein NMI ansteht bzw. gerade ein NMI bearbeitet wird.
     *
     * @return <code>true</code> wenn ein NMI in Bearbeitung ist oder ansteht,
     * <code>false</code> sonst
     */
    boolean isNMIInProgress() {
        return cpu.isNmiInProgress();
    }

    /**
     * Gibt die Instanz des CPU Decoders zurück.
     *
     * @return Decoderinstanz oder <code>null</code> wenn kein Decoder
     * initialisiert ist.
     */
    public Decoder getDecoder() {
        return cpu.getDecoder();
    }
}
