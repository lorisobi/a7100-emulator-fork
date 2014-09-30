/*
 * ABG_new.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   09.08.2014 Erstellt aus ABG.java
 *
 */
package a7100emulator.components.modules;

import a7100emulator.Tools.Memory;
import a7100emulator.components.system.MMS16Bus;
import a7100emulator.components.system.Screen;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Klasse zur Abbildung der ABG (Anschlußsteuerung für grafischen Bildschirm)
 * <p>
 * TODO: Diese Klasse ist die Neuimplementierung von ABG.java und soll diese
 * vollständig ersetzen
 *
 * @author Dirk Bräuer
 */
public final class ABG implements Module, ClockModule {
    /**
     * Enum zur Abbildung der möglichen Cursordarstellungen
     */
    public enum CursorMode {

        /**
         * Unsichtbarer Cursor
         */
        CURSOR_INVISIBLE,
        /**
         * Blinkender Unterstrich
         */
        CURSOR_BLINK_LINE,
        /**
         * Blinkender Block
         */
        CURSOR_BLINK_BLOCK,
        /**
         * Permanenter Block
         */
        CURSOR_STATIC_BLOCK;
    }

    /**
     * Port Funktionsregister
     */
    private static final int LOCAL_PORT_FUNCTION_REGISTER = 0x22;
    /**
     * Port Splitregister
     */
    private static final int LOCAL_PORT_SPLIT_REGISTER = 0x23;
    /**
     * Port Adresszähler Low Byte
     */
    private static final int LOCAL_PORT_ADDRESS_COUNTER_LOW = 0x20;
    /**
     * Port Adresszähler High Byte
     */
    private static final int LOCAL_PORT_ADDRESS_COUNTER_HIGH = 0x21;
    /**
     * Port Palettenregister 0
     */
    private static final int LOCAL_PORT_PALETTE_0 = 0x30;
    /**
     * Port Palettenregister 1
     */
    private static final int LOCAL_PORT_PALETTE_1 = 0x31;
    /**
     * Port Palettenregister 2
     */
    private static final int LOCAL_PORT_PALETTE_2 = 0x32;
    /**
     * Port Palettenregister 3
     */
    private static final int LOCAL_PORT_PALETTE_3 = 0x33;
    /**
     * Port Palettenregister 4
     */
    private static final int LOCAL_PORT_PALETTE_4 = 0x34;
    /**
     * Port Palettenregister 5
     */
    private static final int LOCAL_PORT_PALETTE_5 = 0x35;
    /**
     * Port Palettenregister 6
     */
    private static final int LOCAL_PORT_PALETTE_6 = 0x36;
    /**
     * Port Palettenregister 7
     */
    private static final int LOCAL_PORT_PALETTE_7 = 0x37;
    /**
     * Port Palettenregister 8
     */
    private static final int LOCAL_PORT_PALETTE_8 = 0x38;
    /**
     * Port Palettenregister 9
     */
    private static final int LOCAL_PORT_PALETTE_9 = 0x39;
    /**
     * Port Palettenregister A
     */
    private static final int LOCAL_PORT_PALETTE_A = 0x3A;
    /**
     * Port Palettenregister B
     */
    private static final int LOCAL_PORT_PALETTE_B = 0x3B;
    /**
     * Port Palettenregister C
     */
    private static final int LOCAL_PORT_PALETTE_C = 0x3C;
    /**
     * Port Palettenregister D
     */
    private static final int LOCAL_PORT_PALETTE_D = 0x3D;
    /**
     * Port Palettenregister E
     */
    private static final int LOCAL_PORT_PALETTE_E = 0x3E;
    /**
     * Port Palettenregister F
     */
    private static final int LOCAL_PORT_PALETTE_F = 0x3F;
    /**
     * Attribut Blinkende Darstellung
     */
    private static final int ATTRIBUTE_BLINK = 0x02;
    /**
     * Attribut inverse Darstellung
     */
    private static final int ATTRIBUTE_INVERSE = 0x04;
    /**
     * Attribut intensive Darstellung
     */
    private static final int ATTRIBUTE_INTENSE = 0x08;
    /**
     * Attribute Cursor
     */
    private static final int ATTRIBUTE_CURSOR = 0x10;
    /**
     * Farbe dunkles Grün
     */
    private static final int DARK_GREEN = new Color(0, 100, 0).getRGB();
    /**
     * Farbe normales Grün
     */
    private static final int GREEN = new Color(0, 150, 0).getRGB();
    /**
     * Farbe intensives Grün
     */
    private static final int INTENSE_GREEN = new Color(0, 255, 0).getRGB();
    /**
     * Farbe Schwarz
     */
    private static final int BLACK = new Color(0, 0, 0).getRGB();
    /**
     * Alphanumerik-Bildschirm
     */
    private BufferedImage alphanumericScreen = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
    /**
     * Alphanumerik-Bildschirm blinkend
     */
    private final BufferedImage alphanumericScreenBlink = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
    /**
     * Grafikbildschirm
     */
    private final BufferedImage graphicsScreen = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
    /**
     * aktuell Dargestellter Bildschirm
     */
    private BufferedImage screenImage = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
    /**
     * Aktueller Zustand für blinkenden Text / Cursor 0 - Nicht dargestellt / 1
     * - dargestellt
     */
    private int blinkState = 0;
    /**
     * Zähler für Wechsel des Zustands blinken
     */
    private int blinkClock = 0;
    /**
     * Zähler für Bildschirmupdates
     */
    private int localClock = 0;
    /**
     * Aktueller Cursor
     */
    private CursorMode cursorMode = CursorMode.CURSOR_BLINK_LINE;
    /**
     * Aktuelle Zeile des Cursors
     */
    private int cursorRow = 1;
    /**
     * Aktuelle Spalte des Cursors
     */
    private int cursorColumn = 1;
    /**
     * Grafischer Bildwiederholspeicher
     */
    private Memory[] graphicMemory = new Memory[2];
    /**
     * Alphanumerischer Bildiwederholspeicher
     */
    private Memory[] alphanumericMemory = new Memory[2];
    /**
     * Palettenregister
     */
    private int[] palette_register = new int[16];
    /**
     * Adresszähler
     */
    private int address_counter = 0;
    /**
     * Funktionsregister
     */
    private int function_register;
    /**
     * Splitregister
     */
    private int split_register;
    /**
     * Verweis auf KGS
     */
    private KGS kgs;

    /**
     * Erstellt eine neue ABG und initialisiert diese
     */
    public ABG(KGS kgs) {
        this.kgs = kgs;
        init();
    }

    /**
     * Intitialisiert die ABG
     */
    @Override
    public void init() {
        for (int i = 0; i < 2; i++) {
            graphicMemory[i] = new Memory(0x8000);
            alphanumericMemory[i] = new Memory(0x8000);
        }

        Graphics g = alphanumericScreen.getGraphics();
        g.setColor(new Color(BLACK));
        g.fillRect(0, 0, 640, 400);
        g = graphicsScreen.getGraphics();
        g.setColor(new Color(BLACK));
        g.fillRect(0, 0, 640, 400);
        g = alphanumericScreenBlink.getGraphics();
        g.setColor(new Color(BLACK));
        g.fillRect(0, 0, 640, 400);
        screenImage = alphanumericScreen;
        Screen.getInstance().setImage(screenImage);
        registerClocks();
    }

    /**
     * Gibt den Verweis auf den Alphanumerik-Bildschirm zurück
     *
     * @return Alphanumerik Bildschirm
     */
    BufferedImage getAlphanumericScreen() {
        return alphanumericScreen;
    }

    /**
     * Gibt den Verweis auf den Grafikbildschirm zurück
     *
     * @return Grafikbildschirm
     */
    BufferedImage getGraphicsScreen() {
        return graphicsScreen;
    }

    /**
     * Zeichnet die Komponente neu und aktualisiert damit die Ansicht
     */
    private void repaintScreen() {
        Screen.getInstance().repaint();
    }

    /**
     * Registriert das Modul für Änderungen der Systemzeit
     */
    @Override
    public void registerClocks() {
        MMS16Bus.getInstance().registerClockModule(this);
    }

    /**
     * Verarbeitet die geänderte Systemzeit
     *
     * @param amount Anzahl der Ticks
     */
    @Override
    public void clockUpdate(int amount) {
        localClock += amount;
        if (localClock > 98300) {
            localClock = 0;
            updateScreen();
            kgs.requestNMI();
        }
    }

    /**
     * Setzt den aktuellen Cursor-Modus
     *
     * @param cursorMode the cursorMode to set
     */
    void setCursorMode(CursorMode cursorMode) {
        this.cursorMode = cursorMode;
    }

    void writeWord(int msel, int address, int data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void writeByte(int msel, int address, int data) {
        switch (msel) {
            case 0x01:
                // Grafik 1
                graphicMemory[0].writeByte(address, data);
                break;
            case 0x03:
                // Alphanumerik 1
                alphanumericMemory[0].writeByte(address, data);
                break;
            case 0x04:
                // Grafik 2
                graphicMemory[1].writeByte(address, data);
                break;
            case 0x05:
                // Grafik 1 + 2
                graphicMemory[0].writeByte(address, data);
                graphicMemory[1].writeByte(address, data);
                break;
            case 0x0C:
                // Alphanumerik 2
                alphanumericMemory[1].writeByte(address, data);
                break;
            case 0x0F:
                // Alphanumerik 1+2
                alphanumericMemory[0].writeByte(address, data);
                alphanumericMemory[1].writeByte(address, data);
                break;
            default:
                System.out.println("Nicht definiertes MSEL Register Schreiben " + Integer.toBinaryString(msel) + " für ABG");
                break;
        }
    }

    int readWord(int msel, int address) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    int readByte(int msel, int address) {
        switch (msel) {
            case 0x01:
                // Grafik 1
                return graphicMemory[0].readByte(address);
            case 0x03:
                // Alphanumerik 1
                return alphanumericMemory[0].readByte(address);
            case 0x04:
                // Grafik 2
                return graphicMemory[1].readByte(address);
            case 0x0C:
                // Alphanumerik 2
                return alphanumericMemory[1].readByte(address);
            default:
                System.out.println("Nicht definiertes MSEL Register Lesen " + Integer.toBinaryString(msel) + " für ABG");
                break;
        }
        return 0;
    }

    public int readLocalPort(int port) {
        switch (port) {
            case LOCAL_PORT_FUNCTION_REGISTER:
                throw new IllegalArgumentException("Lesen Funktionsregister nicht erlaubt!");
            case LOCAL_PORT_SPLIT_REGISTER:
                throw new IllegalArgumentException("Lesen Splitregister nicht erlaubt!");
            case LOCAL_PORT_ADDRESS_COUNTER_LOW:
            case LOCAL_PORT_ADDRESS_COUNTER_HIGH:
                throw new IllegalArgumentException("Lesen Adresszähler nicht erlaubt!");
            case LOCAL_PORT_PALETTE_0:
            case LOCAL_PORT_PALETTE_1:
            case LOCAL_PORT_PALETTE_2:
            case LOCAL_PORT_PALETTE_3:
            case LOCAL_PORT_PALETTE_4:
            case LOCAL_PORT_PALETTE_5:
            case LOCAL_PORT_PALETTE_6:
            case LOCAL_PORT_PALETTE_7:
            case LOCAL_PORT_PALETTE_8:
            case LOCAL_PORT_PALETTE_9:
            case LOCAL_PORT_PALETTE_A:
            case LOCAL_PORT_PALETTE_B:
            case LOCAL_PORT_PALETTE_C:
            case LOCAL_PORT_PALETTE_D:
            case LOCAL_PORT_PALETTE_E:
            case LOCAL_PORT_PALETTE_F:
                throw new IllegalArgumentException("Lesen Palettenregister nicht erlaubt!");
        }
        return 0;
    }

    public void writeLocalPort(int port, int data) {
        switch (port) {
            case LOCAL_PORT_FUNCTION_REGISTER:
                function_register = data & 0xFF;
                //System.out.println("Function Register:" + String.format("%02X", function_register) + " " + Integer.toBinaryString(data));
                break;
            case LOCAL_PORT_SPLIT_REGISTER:
                split_register = data & 0xFF;
                if (split_register != 0xFF) {
                    System.out.println("Split Register:" + String.format("%02X", split_register));
                }
                break;
            case LOCAL_PORT_ADDRESS_COUNTER_LOW:
                address_counter = (address_counter & 0x00FF) | ((data & 0xFF) << 8);
                break;
            case LOCAL_PORT_ADDRESS_COUNTER_HIGH:
                address_counter = (address_counter & 0xFF00) | (data & 0xFF);
                break;
            case LOCAL_PORT_PALETTE_0:
            case LOCAL_PORT_PALETTE_1:
            case LOCAL_PORT_PALETTE_2:
            case LOCAL_PORT_PALETTE_3:
            case LOCAL_PORT_PALETTE_4:
            case LOCAL_PORT_PALETTE_5:
            case LOCAL_PORT_PALETTE_6:
            case LOCAL_PORT_PALETTE_7:
            case LOCAL_PORT_PALETTE_8:
            case LOCAL_PORT_PALETTE_9:
            case LOCAL_PORT_PALETTE_A:
            case LOCAL_PORT_PALETTE_B:
            case LOCAL_PORT_PALETTE_C:
            case LOCAL_PORT_PALETTE_D:
            case LOCAL_PORT_PALETTE_E:
            case LOCAL_PORT_PALETTE_F:
                palette_register[port - 0x30] = data & 0xFF;
                break;
        }
    }

    private void updateScreen() {
        int address = ~address_counter & 0x7FFF;

        int splitline = (split_register == 0xFF) ? 0 : ((split_register == 0xFE) ? 400 : (split_register * 2 - 1));
        for (int line = 0; line < 400; line++) {
            for (int column = 0; column < 640; column += 8) {
                if (line < splitline) {
                    updateGraphicsScreen(address, column, line);
                } else {
                    updateAlphanumericScreen(address, column, line);
                }
                address = (address - 1) & 0x7FFF;
            }
        }
        Screen.getInstance().repaint();
    }

    private void updateAlphanumericScreen(int address, int column, int line) {
        int data = alphanumericMemory[0].readByte(address);
        int attribute = alphanumericMemory[1].readByte(address);
        boolean intense = (attribute & ATTRIBUTE_INTENSE) != 0;
        boolean inverse = (attribute & ATTRIBUTE_INVERSE) != 0;
        boolean blink = ((attribute & ATTRIBUTE_BLINK) != 0);
        boolean blink_fnct = getBit(function_register, 3);

        boolean cursor = (attribute & ATTRIBUTE_CURSOR) != 0;
        for (int pixel = 0; pixel < 8; pixel++) {
            boolean b1 = getBit(data, pixel);
            if (cursor) {
                // TODO
                if (blink_fnct) {
                    screenImage.setRGB(column + 7 - pixel, line, intense ? INTENSE_GREEN : GREEN);
                } else {
                    screenImage.setRGB(column + 7 - pixel, line, BLACK);
                }
            } else {
                if (b1 ^ inverse) {
                    screenImage.setRGB(column + 7 - pixel, line, intense ? INTENSE_GREEN : GREEN);
                } else {
                    screenImage.setRGB(column + 7 - pixel, line, BLACK);
                }
            }
        }
    }

    private void updateGraphicsScreen(int address, int column, int line) {
        int data1 = graphicMemory[0].readByte(address);
        int data2 = graphicMemory[1].readByte(address);
        for (int pixel = 0; pixel < 8; pixel++) {
            boolean b1 = getBit(data1, pixel);
            boolean b2 = getBit(data2, pixel);
            if (b1 && !b2) {
                screenImage.setRGB(column + 7 - pixel, line, DARK_GREEN);
            } else if (!b1 && b2) {
                screenImage.setRGB(column + 7 - pixel, line, GREEN);
            } else if (b1 && b2) {
                screenImage.setRGB(column + 7 - pixel, line, INTENSE_GREEN);
            } else {
                screenImage.setRGB(column + 7 - pixel, line, BLACK);
            }
        }
    }

    /**
     * Prüft ob ein Bit des Operanden gesetzt ist
     *
     * @param op Operand
     * @param i Nummer des Bits
     * @return true - wenn das Bit gesetzt ist, false - sonst
     */
    private boolean getBit(int op, int i) {
        return (((op >> i) & 0x1) == 0x1);
    }

    /**
     * Speichert den Zustand der ABG in eine Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn das Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        dos.writeInt(blinkState);
        dos.writeInt(blinkClock);
        dos.writeUTF(cursorMode.name());
        dos.writeInt(cursorRow);
        dos.writeInt(cursorColumn);
    }

    /**
     * Lädt den Zustand der ABG aus einer Datei
     *
     * @param dis Stream der Datei
     * @throws IOException Wenn das Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        blinkState = dis.readInt();
        blinkClock = dis.readInt();
        cursorMode = CursorMode.valueOf(dis.readUTF());
        cursorRow = dis.readInt();
        cursorColumn = dis.readInt();
    }

}
