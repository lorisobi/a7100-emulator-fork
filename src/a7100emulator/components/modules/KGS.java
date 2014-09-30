/*
 * KGS_new.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   07.08.2014 Erste Version aus KGS.java kopiert
 *   09.08.2014 Zugriffe auf SystemMemory, SystemPorts und SystemClock durch MMS16Bus ersetzt
 *
 */
package a7100emulator.components.modules;

import a7100emulator.Tools.BitmapGenerator;
import a7100emulator.Tools.Memory;
import a7100emulator.components.ic.UA856;
import a7100emulator.components.ic.UA857;
import a7100emulator.components.ic.UA880;
import a7100emulator.components.system.MMS16Bus;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Klasse zur Abbildung der KGS (Kontroller für grafisches Subsytem)
 * <p>
 * TODO: Diese Klasse ist die Neuimplementierung von KGS.java und soll diese
 * vollständig ersetzen
 *
 * @author Dirk Bräuer
 */
public final class KGS implements IOModule, ClockModule {

    /**
     * Arbeistspeicher der KGS
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
     * Gibt den aktuellen Zähler für Interrupt-Weietrgabe an
     */
    private long interruptClock = 0;
    /**
     * Gibt an ob auf einen Interrupt der KGS gewartet wird
     */
    private boolean interruptWaiting = false;
    /**
     * Select Byte 0
     */
    private final int selectByte0 = 0x03;
    /**
     * Select Byte 1
     */
    private final int selectByte1 = 0x1C;

    /**
     * UA880 CPU der KGS
     */
    private UA880 cpu = new UA880(this);
    /**
     * UA856 SIO der KGS
     */
    private UA856 sio = new UA856();
    /**
     * UA857 CTC der KGS
     */
    private UA857 ctc = new UA857();
    private int msel;

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
     * Gibt ein Byte auf einem Port aus
     *
     * @param port Port
     * @param data Daten
     */
    @Override
    public void writePort_Byte(int port, int data) {
        //System.out.println("OUT Byte " + Integer.toHexString(data) + "(" + Integer.toBinaryString(data) + ","+((data>=0x20)?(char)data:"-")+")" + " to port " + Integer.toHexString(port));
        switch (port) {
            case PORT_KGS_STATE:
                clearBit(INT_BIT);
                clearBit(ERR_BIT);
                break;
            case PORT_KGS_DATA:
                dataIn = data;
                setBit(IBF_BIT);
                System.out.println("Ausgabe "+String.format("%02X",data)+" ("+((data>0x20)?(char)data:" ")+") Speicher:"+String.format("%04X",ram.readWord(0x2805)));
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
    public void writePort_Word(int port, int data) {
        writePort_Byte(port, data);
    }

    /**
     * Liest ein Byte von einem Port
     *
     * @param port Port
     * @return gelesenes Byte
     */
    @Override
    public int readPort_Byte(int port) {
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
        //System.out.println("IN Byte from port " + Integer.toHexString(port)+": "+Integer.toHexString(result));
        return result;
    }

    /**
     * Liest ein Wort von einem Port
     *
     * @param port Port
     * @return gelesenes Wort
     */
    @Override
    public int readPort_Word(int port) {
        return readPort_Byte(port);
    }

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
                //System.out.println("Lese Status lokal "+Integer.toBinaryString(state));
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
                break;
            case LOCAL_PORT_SIO_CONTROL_A:
                sio.writeControl(0, data);
                break;
            case LOCAL_PORT_SIO_DATA_B:
                sio.writeData(1, data);
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
//                System.out.println("Setze INT Bit");
                break;
            case LOCAL_PORT_ERR_FLAG:
                setBit(ERR_BIT);
//                System.out.println("Setze ERR Bit");
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

    public void writeMemoryWord(int address, int data) {
        // Zugriffe auf lokalen KGS-Ram
        if (address <= 0x7FFF || msel == 0) {
            ram.writeWord(address, data);
        } else {
            abg.writeWord(msel, ~address & 0x7FFF, data);
        }
    }

    public void writeMemoryByte(int address, int data) {
        // Zugriffe auf lokalen KGS-Ram
        if (address <= 0x7FFF || msel == 0) {
            ram.writeByte(address, data);
        } else {
            abg.writeByte(msel, ~address & 0x7FFF, data);
        }
    }

    public int readMemoryWord(int address) {
        // Zugriffe auf lokalen KGS-Ram
        if (address <= 0x7FFF || msel == 0) {
            return ram.readWord(address);
        } else {
            return abg.readWord(msel, ~address & 0x7FFF);
        }
    }

    public int readMemoryByte(int address) {
        // Zugriffe auf lokalen KGS-Ram
        if (address <= 0x7FFF || msel == 0) {
            return ram.readByte(address);
        } else {
            return abg.readByte(msel, ~address & 0x7FFF);
        }
    }

    void requestNMI() {
        if (!sio.isDiagnose()) {
            cpu.requestNMI();
        }
    }

    /**
     * Initialisiert die KGS
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
        MMS16Bus.getInstance().registerClockModule(this);
    }

    /**
     * Verarbeitet Änderungen der Systemzeit
     *
     * @param amount Anzahl der Ticks
     */
    @Override
    public void clockUpdate(int amount) {
        if (interruptWaiting) {
            interruptClock += amount;
            if (interruptClock > 20) {
                interruptWaiting = false;
                MMS16Bus.getInstance().requestInterrupt(7);
            }
        }
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
            Logger.getLogger(BitmapGenerator.class.getName()).log(Level.SEVERE, null, ex);
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
        dos.writeInt(state);
        dos.writeLong(interruptClock);
        dos.writeBoolean(interruptWaiting);
        ram.saveMemory(dos);
        abg.saveState(dos);
    }

    /**
     * Liest den Zustand der KGS aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        state = dis.readInt();
        interruptClock = dis.readLong();
        interruptWaiting = dis.readBoolean();
        ram.loadMemory(dis);
        abg.loadState(dis);
    }

    public void start() {
        Thread cpuThread = new Thread(cpu, "UA880 KGS");
        cpuThread.start();
    }
}
