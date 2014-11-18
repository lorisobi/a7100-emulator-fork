/*
 * ABG.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   09.08.2014 - Erstellt aus ABG.java
 *   30.09.2014 - Umbenannt in ABG, Kommentare vervollständigt, Darstellung Funktionstüchtig, Laden und Speichern implemetiert
 *              
 */
package a7100emulator.components.modules;

import a7100emulator.Tools.BitTest;
import a7100emulator.Tools.Memory;
import a7100emulator.components.system.GlobalClock;
import a7100emulator.components.system.Screen;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Klasse zur Abbildung der ABG (Anschlußsteuerung für grafischen Bildschirm)
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
     * aktuell Dargestellter Bildschirm
     */
    private BufferedImage screenImage = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
    /**
     * Zähler für Bildschirmupdates
     */
    private int localClock = 0;
    /**
     * Aktueller Cursor
     */
    private CursorMode cursorMode = CursorMode.CURSOR_BLINK_LINE;
    /**
     * Grafischer Bildwiederholspeicher
     */
    private final Memory[] graphicMemory = new Memory[2];
    /**
     * Alphanumerischer Bildiwederholspeicher
     */
    private final Memory[] alphanumericMemory = new Memory[2];
    /**
     * Palettenregister
     */
    private final int[] palette_register = new int[16];
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
    private final KGS kgs;

    /**
     * Erstellt eine neue ABG und initialisiert diese
     *
     * @param kgs Zeiger auf KGS Modul
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

        // Löschen des Bildschirmes
        Graphics g = screenImage.getGraphics();
        g.setColor(new Color(BLACK));
        g.fillRect(0, 0, 640, 400);
        Screen.getInstance().setImage(screenImage);

        // Modul für Änderungen der Systemzeit registrieren
        registerClocks();
    }

    /**
     * Registriert das Modul für Änderungen der Systemzeit
     */
    @Override
    public void registerClocks() {
        //MMS16Bus.getInstance().registerClockModule(this);
        GlobalClock.getInstance().registerModule(this);
    }

    /**
     * Verarbeitet die geänderte Systemzeit
     *
     * @param amount Anzahl der Ticks
     */
    @Override
    public void clockUpdate(int amount) {
        localClock += amount;
        if (localClock > 20) {
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

    /**
     * Schreibt ein Wort in den Bildiwederholspeicher
     *
     * @param msel Memory-Select Register
     * @param address Adresse
     * @param data Daten
     */
    void writeWord(int msel, int address, int data) {
        switch (msel) {
            case 0x01:
                // Grafik 1
                graphicMemory[0].writeWord(address, data);
                break;
            case 0x03:
                // Alphanumerik 1
                alphanumericMemory[0].writeWord(address, data);
                break;
            case 0x04:
                // Grafik 2
                graphicMemory[1].writeWord(address, data);
                break;
            case 0x05:
                // Grafik 1 + 2
                graphicMemory[0].writeWord(address, data);
                graphicMemory[1].writeWord(address, data);
                break;
            case 0x0C:
                // Alphanumerik 2
                alphanumericMemory[1].writeWord(address, data);
                break;
            case 0x0F:
                // Alphanumerik 1+2
                alphanumericMemory[0].writeWord(address, data);
                alphanumericMemory[1].writeWord(address, data);
                break;
            default:
                System.out.println("Nicht definiertes MSEL Register Schreiben " + Integer.toBinaryString(msel) + " für ABG");
                break;
        }
    }

    /**
     * Schreibt ein Byte in den Bildiwederholspeicher
     * <p>
     * TODO: Diese Funktion ist noch nicht implementiert
     *
     * @param msel Memory-Select Register
     * @param address Adresse
     * @param data Daten
     */
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

    /**
     * Liest ein Wort aus dem Bildiwederholspeicher
     * <p>
     * TODO: Diese Funktion ist noch nicht implementiert
     *
     * @param msel Memory-Select Register
     * @param address Adresse
     * @return Daten
     */
    int readWord(int msel, int address) {
        switch (msel) {
            case 0x01:
                // Grafik 1
                return graphicMemory[0].readWord(address);
            case 0x03:
                // Alphanumerik 1
                return alphanumericMemory[0].readWord(address);
            case 0x04:
                // Grafik 2
                return graphicMemory[1].readWord(address);
            case 0x0C:
                // Alphanumerik 2
                return alphanumericMemory[1].readWord(address);
            default:
                System.out.println("Nicht definiertes MSEL Register Lesen " + Integer.toBinaryString(msel) + " für ABG");
                break;
        }
        return 0;
    }

    /**
     * Liest ein Byte aus dem Bildiwederholspeicher
     *
     * @param msel Memory-Select Register
     * @param address Adresse
     * @return Daten
     */
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

    /**
     * Liest Daten von einem lokalen Port
     *
     * @param port Port
     * @return Gelesenes Byte
     */
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

    /**
     * Gibt Daten auf einem lokalen Port aus
     *
     * @param port Port
     * @param data Daten
     */
    public void writeLocalPort(int port, int data) {
        switch (port) {
            case LOCAL_PORT_FUNCTION_REGISTER:
                function_register = data & 0xFF;
                //System.out.println("Function Register:" + String.format("%02X", function_register) + " " + Integer.toBinaryString(data));
                break;
            case LOCAL_PORT_SPLIT_REGISTER:
                split_register = data & 0xFF;
//                if (split_register != 0xFF) {
//                    System.out.println("Split Register:" + String.format("%02X", split_register));
//                }
                break;
            case LOCAL_PORT_ADDRESS_COUNTER_LOW:
                address_counter = (address_counter & 0x00FF) | ((data & 0xFF) << 8);
                //System.out.println("address_counter: " + String.format("%04X", address_counter));
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

    /**
     * Aktualisiert die Bildschirmanzeige
     */
    private void updateScreen() {
        int address_an = ~address_counter & 0x7FFF;
        int address_gr = 0x7FFF;

        int splitline = (split_register == 0xFF) ? 0 : ((split_register == 0xFE) ? 400 : (split_register * 2 - 1));
        for (int line = 0; line < 400; line++) {
            for (int column = 0; column < 640; column += 8) {
                if (line < splitline) {
                    updateGraphicsScreen(address_gr, column, line);
                    address_gr = (address_gr - 1) & 0x7FFF;
                } else {
                    if (line == splitline) {
                        kgs.requestNMI();
                    }

                    updateAlphanumericScreen(address_an, column, line);
                    address_an = (address_an - 1) & 0x7FFF;
                }
                //address = (address - 1) & 0x7FFF;
            }
        }
        Screen.getInstance().repaint();
    }

    /**
     * Aktualisiert den Alphanumerikbildschirm für eine Speicherzelle
     *
     * @param address Speicherzelle
     * @param column Spalte auf Bildschirm
     * @param line Zeile auf Bildschirm
     */
    private void updateAlphanumericScreen(int address, int column, int line) {
        int data = alphanumericMemory[0].readByte(address);
        int attribute = alphanumericMemory[1].readByte(address);
        boolean intense = (attribute & ATTRIBUTE_INTENSE) != 0;
        boolean inverse = (attribute & ATTRIBUTE_INVERSE) != 0;
        boolean blink = ((attribute & ATTRIBUTE_BLINK) != 0);
        boolean blink_fnct = BitTest.getBit(function_register, 3);

        boolean cursor = (attribute & ATTRIBUTE_CURSOR) != 0;
        for (int pixel = 0; pixel < 8; pixel++) {
            boolean b1 = BitTest.getBit(data, pixel);
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

    /**
     * Aktualisiert den Grafikbildschirm für eine Speicherzelle
     *
     * @param address Speicherzelle
     * @param column Spalte auf Bildschirm
     * @param line Zeile auf Bildschirm
     */
    private void updateGraphicsScreen(int address, int column, int line) {
        int data1 = graphicMemory[0].readByte(address);
        int data2 = graphicMemory[1].readByte(address);
        for (int pixel = 0; pixel < 8; pixel++) {
            boolean b1 = BitTest.getBit(data1, pixel);
            boolean b2 = BitTest.getBit(data2, pixel);
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
     * Speichert den Zustand der ABG in eine Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn das Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        dos.writeInt(localClock);
        dos.writeUTF(cursorMode.name());
        for (int i = 0; i < 2; i++) {
            graphicMemory[i].saveMemory(dos);
            alphanumericMemory[i].saveMemory(dos);
        }
        for (int i = 0; i < 16; i++) {
            dos.writeInt(palette_register[i]);
        }
        dos.writeInt(address_counter);
        dos.writeInt(function_register);
        dos.writeInt(split_register);
    }

    /**
     * Lädt den Zustand der ABG aus einer Datei
     *
     * @param dis Stream der Datei
     * @throws IOException Wenn das Lesen nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        localClock = dis.readInt();
        cursorMode = CursorMode.valueOf(dis.readUTF());
        for (int i = 0; i < 2; i++) {
            graphicMemory[i].loadMemory(dis);
            alphanumericMemory[i].loadMemory(dis);
        }
        for (int i = 0; i < 16; i++) {
            palette_register[i] = dis.readInt();
        }
        address_counter = dis.readInt();
        function_register = dis.readInt();
        split_register = dis.readInt();
    }
}
