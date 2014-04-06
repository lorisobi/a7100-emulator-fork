/*
 * KGS.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   02.04.2014 Kommentare vervollständigt
 *
 */
package a7100emulator.components.modules;

import a7100emulator.Tools.BitmapGenerator;
import a7100emulator.Tools.Memory;
import a7100emulator.components.system.InterruptSystem;
import a7100emulator.components.system.SystemClock;
import a7100emulator.components.system.SystemPorts;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Klasse zur Abbildung der KGS (Kontroller für grafisches Subsytem)
 *
 * @author Dirk Bräuer
 */
public final class KGS implements PortModule, ClockModule {

    /**
     * Arbeistspeicher der KGS
     */
    private final Memory ram = new Memory(0x8000);
    /**
     * Zeichencode Null
     */
    private final static int CODE_NUL = 0x00;

    /**
     * Zeichencode Start Head
     */
    private final static int CODE_SOH = 0x01;

    /**
     * Zeichncode Start Text
     */
    private final static int CODE_STX = 0x02;

    /**
     * Zeichencode End Text
     */
    private final static int CODE_ETX = 0x03;

    /**
     * Zeichencode EOT
     */
    private final static int CODE_EOT = 0x04;

    /**
     * Zeichencode ENQ
     */
    private final static int CODE_ENQ = 0x05;

    /**
     * Zeichencode ACK
     */
    private final static int CODE_ACK = 0x06;

    /**
     * Zeichencode BEL
     */
    private final static int CODE_BEL = 0x07;

    /**
     * Zeichencode Backspace
     */
    private final static int CODE_BS = 0x08;

    /**
     * Zeichencode Horizontaltabulator
     */
    private final static int CODE_HT = 0x09;

    /**
     * Zeichencode Line Feed
     */
    private final static int CODE_LF = 0x0A;

    /**
     * Zeichencode Vertikaltabulator
     */
    private final static int CODE_VT = 0x0B;

    /**
     * Zeichencode Form Feed
     */
    private final static int CODE_FF = 0x0C;

    /**
     * Zeichncode Carriage Return
     */
    private final static int CODE_CR = 0x0D;

    /**
     * Zeichencode Shift Out
     */
    private final static int CODE_SO = 0x0E;

    /**
     * Zeichencode Shift in
     */
    private final static int CODE_SI = 0x0F;

    /**
     * Zeichencode Data Link Escape
     */
    private final static int CODE_DLE = 0x10;

    /**
     * Zeichencode DC1
     */
    private final static int CODE_DC1 = 0x11;

    /**
     * Zeichencode DC2
     */
    private final static int CODE_DC2 = 0x12;

    /**
     * Zeichencode DC3
     */
    private final static int CODE_DC3 = 0x13;

    /**
     * Zeichencode DC4
     */
    private final static int CODE_DC4 = 0x14;

    /**
     * Zeichencode NAK
     */
    private final static int CODE_NAK = 0x15;

    /**
     * Zeichencode SYN
     */
    private final static int CODE_SYN = 0x16;

    /**
     * Zeichencode ETB
     */
    private final static int CODE_ETB = 0x17;

    /**
     * Zeichencode Cancel
     */
    private final static int CODE_CAN = 0x18;

    /**
     * Zeichencode EM
     */
    private final static int CODE_EM = 0x19;

    /**
     * Zeichencode SUB
     */
    private final static int CODE_SUB = 0x1A;

    /**
     * Zeichencode Escape
     */
    private final static int CODE_ESC = 0x1B;

    /**
     * Zeichencode FS
     */
    private final static int CODE_FS = 0x1C;

    /**
     * Zeichencode GS
     */
    private final static int CODE_GS = 0x1D;

    /**
     * Zeichencode Record Separator
     */
    private final static int CODE_RS = 0x1E;

    /**
     * Zeichencode US
     */
    private final static int CODE_US = 0x1F;

    /**
     * Zeichencode Delete
     */
    private final static int CODE_DEL = 0x7F;

    /**
     * Port KGS-Zustand
     */
    private final static int PORT_KGS_STATE = 0x200;

    /**
     * Port KGS-Daten
     */
    private final static int PORT_KGS_DATA = 0x202;

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
    private int state = 0x01;

    /**
     * Aktuelle Zeile des Cursors
     */
    private int cursorRow = 1;

    /**
     * Aktuelle Spalte des Cursors
     */
    private int cursorColumn = 1;

    /**
     * Gibt an ob gerade eine ESC-Steuerfolge empfangen wird
     */
    private boolean receiveSequence = false;

    /**
     * Zeichen der ESC-Steuerfolge
     */
    private final LinkedList<Byte> escSequence = new LinkedList<Byte>();

    /**
     * Postitionen der Horizontaltabulatoren
     */
    private final boolean[] hTabs = new boolean[80];

    /**
     * Positionen der Vertikaltabulatoren
     */
    private final boolean[] vTabs = new boolean[25];

    /**
     * Referenz auf die ABG (Anschlußsteuerung für grafischen Bildschirm)
     */
    private ABG abg;

    /**
     * Gibt an ob der Grafikbildschirm unterdrückt ist
     */
    private boolean disableGraphics = false;

    /**
     * Gibt an ob die KGS bereits initialisiert ist
     */
    private boolean initialized = false;

    /**
     * Gibt den aktuellen Zähler für Interrupt-Weietrgabe an
     */
    private long interruptClock = 0;

    /**
     * Gibt an ob auf einen Interrupt der KGS gewartet wird
     */
    private boolean interruptWaiting = false;

    /**
     * Puffer für die Übertragung von Daten zur ZVE
     */
    private final byte[] deviceBuffer = new byte[100];

    /**
     * Aktuelle Position im Puffer
     */
    private int bufferPosition = 0;

    /**
     * Gespeicherte Cursorzeile
     */
    private int cursorRowSave = 1;

    /**
     * Gespeicherte Cursorspalte
     */
    private int cursorColumnSave = 1;

    /**
     * Darstellung intensiver Zeichen
     */
    private boolean intense = false;

    /**
     * Darstellung inverser Zeichen
     */
    private boolean inverse = false;

    /**
     * Darstellung blinkender Zeichen
     */
    private boolean flash = false;

    /**
     * darstellung unterstrichener Zeichen
     */
    private boolean underline = false;

    /**
     * Status des Wraparound Modus
     */
    private boolean wraparound = false;

    /**
     * Zweite Implementierung des Puffers zur ZVE TODO: Puffer zusammenführen
     */
    private final LinkedList<Byte> deviceBuffer2 = new LinkedList<Byte>();

    /**
     * Funktionskennzeichen AALP (Anzeige Splitgrenze)
     */
    private boolean gsx_AALP = true;

    /**
     * Funktionskennzeichen ALOC (Sichtbarkeit grafischer Cursor)
     */
    private boolean gsx_ALOC = false;

    /**
     * Funktionskennzeichen AZUG (Dunkeltastung Bildschirm)
     */
    private boolean gsx_AZUG = false;

    /**
     * Linienoption Linientyp
     */
    private int[] gsx_linetype = {1, 1, 1};

    /**
     * Linienoption Linienstärke
     */
    private int[] gsx_linewidth = {1, 1, 1};

    /**
     * Linienoption Speicherebene
     */
    private int[] gsx_linememory = {2, 2, 2};

    /**
     * Markeroption Markertyp
     */
    private int[] gsx_markertype = {3, 2};

    /**
     * Markeroption Speicherebene
     */
    private int[] gsx_markermemory = {2, 2};

    /**
     * Schreibtyp
     */
    private int gsx_writetype = 1;

    /**
     * Palettenregister
     */
    private int gsx_paletteregister = 1;

    /**
     * Grenzen Bildausschnitt
     */
    private int[] gsx_window = {0, 0, 639, 399};

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
        SystemPorts.getInstance().registerPort(this, PORT_KGS_STATE);
        SystemPorts.getInstance().registerPort(this, PORT_KGS_DATA);
    }

    /**
     * Gibt ein Byte auf einem Port aus
     *
     * @param port Port
     * @param data Daten
     */
    @Override
    public void writePort_Byte(int port, int data) {
        //System.out.println("OUT Byte " + Integer.toHexString(data) + "(" + Integer.toBinaryString(data) + ")" + " to port " + Integer.toHexString(port));
        switch (port) {
            case PORT_KGS_STATE:
                clearBit(INT_BIT);
                clearBit(ERR_BIT);
                if (!initialized) {
                    deviceBuffer[0] = 0x00;
                    bufferPosition = 1;
                    deviceBuffer2.add((byte) 0x00);
                    setBit(OBF_BIT);
                    initialized = true;
                }
                break;
            case PORT_KGS_DATA:
                dataReceived(data);
                setBit(INT_BIT);
                interruptWaiting = true;
                interruptClock = 0;
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
        //System.out.println("OUT Word " + Integer.toHexString(data) + "(" + Integer.toBinaryString(data) + ")" + " to port " + Integer.toHexString(port));
        switch (port) {
            case PORT_KGS_STATE:
                break;
            case PORT_KGS_DATA:
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
    public int readPort_Byte(int port) {
        int result = 0;
        switch (port) {
            case PORT_KGS_STATE:
                result = state;
                break;
            case PORT_KGS_DATA:
                result = deviceBuffer[0];
                if (initialized && bufferPosition != 0) {
                    System.arraycopy(deviceBuffer, 1, deviceBuffer, 0, 99);
                    bufferPosition--;
                    if (bufferPosition == 0) {
                        clearBit(OBF_BIT);
                    }
                } else //                result = output;
                {
                    clearBit(OBF_BIT);
                }
                //            System.out.println("Lese KGS Daten: " + Integer.toHexString(result)+ " Buffer Position:"+bufferPosition+" OBF:"+getBit(OBF_BIT));
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
    public int readPort_Word(int port) {
        int result = 0;
        switch (port) {
            case PORT_KGS_STATE:
                result = state;
                break;
            case PORT_KGS_DATA:
                result = deviceBuffer[0];
                if (initialized && bufferPosition != 0) {
                    System.arraycopy(deviceBuffer, 1, deviceBuffer, 0, 99);
                    bufferPosition--;
                    if (bufferPosition == 0) {
                        clearBit(OBF_BIT);
                    }
                } else //                result = output;
                {
                    clearBit(OBF_BIT);
                }
                //System.out.println("Lese KGS Daten: " + Integer.toHexString(result)+ " Buffer Position:"+bufferPosition+" OBF:"+getBit(OBF_BIT));
                break;
        }
        return result;
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
        initTabs();
        abg = new ABG();
        registerPorts();
        registerClocks();
    }

    /**
     * Gibt ein Zeichen auf dem Bildschirm aus
     *
     * @param data Code des Zeichens
     * @param row Zeile für Ausgabe
     * @param column Spalte für Ausgabe
     */
    private void drawCharacter(int data, int row, int column) {
        // Darstellbares Zeichen
        byte[] linecode = new byte[16];
        for (byte line = 0; line < 16; line++) {
            linecode[line] = (byte) (ram.readByte(0x1800 + (data << 4) + line) & 0xFF);
        }
        abg.setLineCodes(row - 1, column - 1, linecode, intense, flash, false, underline, inverse);
    }

    /**
     * Initialisiert die Tabulatoren auf Default-Werte
     */
    private void initTabs() {
        for (int i = 0; i < 73; i = i + 8) {
            hTabs[i] = true;
        }
        for (int i = 0; i < 25; i++) {
            vTabs[i] = true;
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

    /**
     * Verarbeitet von der ZVE empfangene Daten
     *
     * @param data Daten
     */
    private void dataReceived(int data) {
        if (initialized) {
            abg.removeCursor(cursorRow - 1, cursorColumn - 1);
            if (receiveSequence) {
                escSequence.add((byte) (data & 0xFF));
                //System.out.print((char) data);
                checkESC();
            } else {
                if (data >= 0x20 && data != CODE_DEL) {
                    if (wraparound) {
                        if (cursorColumn == 81) {
                            cursorColumn = 1;
                            if (cursorRow != 25) {
                                cursorRow++;
                            } else {
                                abg.rollAlphanumericScreen();
                            }
                        }
                    }
                    //System.out.println((char)data);
                    drawCharacter(data, cursorRow, cursorColumn++);
                    /*if (cursorColumn == 81) {
                     if (!wraparound) {
                     cursorColumn = 1;
                     if (cursorRow != 25) {
                     cursorRow++;
                     } else {
                     abg.rollAlphanumericScreen();
                     }
                     }
                     }*/
                } else {
                    // Steuerzeichen
                    switch (data) {
                        case CODE_NUL:
                            // Nichts
                            break;
                        case CODE_SOH:
                            // Start Grafiktastencode
                            //System.out.println("Starte Grafik-Tastencode");
                            receiveSequence = true;
                            escSequence.clear();
                            escSequence.add((byte) 0x01);
                            break;
                        case CODE_STX:
                            // Start Grafik-Kommando
                            //System.out.println("Starte Grafik-Kommando");
                            receiveSequence = true;
                            escSequence.clear();
                            escSequence.add((byte) 0x02);
                            break;
                        case CODE_ETX:
                            // Ende Grafikkommando
                            //System.out.println("Ende Grafik-Kommando");
                            setBit(INT_BIT);
                            receiveSequence = false;
                            break;
                        case CODE_BS:
                            // Cursor nach links
                            if (cursorColumn != 1) {
                                cursorColumn--;
                            }
                            break;
                        case CODE_HT:
                            // Tabulator Horizontal
                            if (cursorColumn != 80) {
                                do {
                                    cursorColumn++;
                                } while (!hTabs[cursorColumn - 1] && cursorColumn != 80);
                            }
                            break;
                        case CODE_LF:
                            // Cursor nach unten
                            if (cursorRow != 25) {
                                cursorRow++;
                            } else {
                                abg.rollAlphanumericScreen();
                            }
                            break;
                        case CODE_VT:
                            // Vertikaler Tabulator
                            if (cursorRow != 25) {
                                do {
                                    cursorRow++;
                                } while (!vTabs[cursorRow - 1] && cursorRow != 25);
                            }
                            break;
                        case CODE_FF:
                            // Form Feed
                            if (cursorRow != 25) {
                                cursorRow++;
                            } else {
                                abg.rollAlphanumericScreen();
                            }
                            break;
                        case CODE_CR:
                            // Carriage return
                            cursorColumn = 1;
                            break;
                        case CODE_SO:
                            // Shift Out
                            //System.out.println("Shift OUT");
                            // TODO
                            break;
                        case CODE_SI:
                            // Shift In
                            //System.out.println("Shift In");
                            // TODO
                            break;
                        case CODE_DLE:
                            // Data Link Escape
                            receiveSequence = true;
                            escSequence.clear();
                            escSequence.add((byte) 0x10);
//                            ESCPosition = 0;
                            // TODO
                            break;
                        case CODE_CAN:
                            // Cancel ESC
                            receiveSequence = false;
                            break;
                        case CODE_ESC:
                            receiveSequence = true;
                            escSequence.clear();
                            escSequence.add((byte) 0x1B);
                            //System.out.print("ESC ");
                            break;
                        case CODE_RS:
                            cursorColumn = 1;
                            cursorRow++;
                            // TODO Rollen
                            break;
                        case CODE_DEL:
                            // Delete
                            // Ignorieren
                            break;
                        case CODE_EOT:
                        case CODE_ENQ:
                        case CODE_ACK:
                        case CODE_BEL:
                        case CODE_DC1:
                        case CODE_DC2:
                        case CODE_DC3:
                        case CODE_DC4:
                        case CODE_NAK:
                        case CODE_SYN:
                        case CODE_ETB:
                        case CODE_EM:
                        case CODE_SUB:
                        case CODE_FS:
                        case CODE_GS:
                        case CODE_US:
                            state &= ERR_BIT;
                            break;
                    }
                }
            }
            abg.setCursor(cursorRow - 1, cursorColumn - 1);
            //abg.updateScreen();
        } else {
            // TODO Initialisierung
            if (data == 0xAA) {
                //deviceBuffer2.add((byte) 0x55);
                deviceBuffer[0] = 0x55;
                bufferPosition = 1;
                setBit(OBF_BIT);
            } else if (data == 0x55) {
                setBit(INT_BIT);
                setBit(ERR_BIT);
            }
        }
    }

    /**
     * Prüft die aktuelle ESC Sequenz auf Gültigkeit und führt diese ggf. aus
     */
    private void checkESC() {
        if (escSequence.peekFirst() == 0x10) {
            int code = escSequence.get(1) & 0xFF;
            if (escSequence.size() == 18) {
                code |= 0x80;
                for (byte line = 0; line < 16; line++) {
                    ram.writeByte(0x1800 + (code << 4) + line, escSequence.get(2 + line) & 0xFF);
                }
                receiveSequence = false;
            }
        } else if (escSequence.peekFirst() == 0x01) {
            if (escSequence.size() == 2) {
                int code = escSequence.peekLast() & 0xFF;
                //System.out.println(String.format("Tastaturkommando: %02X", code));
                receiveSequence = false;
            }
        } else if (escSequence.peekFirst() == 0x02) {
            if (escSequence.size() >= 3) {
                int byteCnt = (escSequence.get(2) << 8) + escSequence.get(1);
                if (escSequence.size() >= byteCnt + 3) {
                    //  System.out.print("Anzahl der Bytes: " + byteCnt+ "(");
                    //for (byte b : escSequence) {
                    //    System.out.print(String.format(" %03d", b));
                    //}
                    //System.out.println(" )");
                    executeGraphicsBuffer();
                    receiveSequence = false;
                }
            }
        } else {
            switch (escSequence.peekLast()) {
                case 0x41:
                case 0x6B:
                    // [ Pn A / [ Pn k Cursor nach oben
                    cursorRow -= getParameter();
                    if (cursorRow < 1) {
                        cursorRow = 1;
                    }
                    receiveSequence = false;
                    break;
                case 0x42:
                case 0x65:
                    // [ Pn B / [ Pn e Cursor nach unten
                    cursorRow += getParameter();
                    if (cursorRow > 25) {
                        cursorRow = 25;
                    }
                    receiveSequence = false;
                    break;
                case 0x43:
                case 0x61:
                    // [ Pn C / [ Pn a Cursor nach rechts
                    cursorColumn += getParameter();
                    if (cursorRow > 80) {
                        cursorColumn = 80;
                    }
                    receiveSequence = false;
                    break;
                case 0x44:
                    // [ Pn D Cursor nach links
                    // D Zeilenschaltung
                    if (escSequence.size() == 2) {
                        cursorRow++;
                        if (cursorRow > 25) {
                            abg.rollAlphanumericScreen();
                            cursorRow = 25;
                        }
                        receiveSequence = false;
                    } else {
                        cursorColumn -= getParameter();
                        if (cursorColumn < 1) {
                            cursorColumn = 1;
                        }
                        receiveSequence = false;
                    }
                    break;
                case 0x6A:
                    // [ Pn j Cursor nach links
                    cursorColumn -= getParameter();
                    if (cursorColumn < 1) {
                        cursorColumn = 1;
                    }
                    receiveSequence = false;
                    break;
                case 0x47:
                case 0x60:
                    // [ Pn G / [ Pn ` Horizontalposition absolut
                    cursorColumn = getParameter();
                    if (cursorColumn > 80) {
                        cursorColumn = 80;
                    }
                    receiveSequence = false;
                    break;
                case 0x64:
                    // [ Pn d Vertikalposition absolut
                    cursorRow = getParameter();
                    if (cursorRow > 25) {
                        cursorRow = 25;
                    }
                    receiveSequence = false;
                    break;
                case 0x46:
                    // [ Pn F Cursor nach oben an Zeilenanfang
                    cursorColumn = 1;
                    cursorRow -= getParameter();
                    if (cursorRow < 1) {
                        cursorRow = 1;
                    }
                    receiveSequence = false;
                    break;
                case 0x45:
                    // [ Pn E Cursor nach unten an Zeilenanfang
                    // E Neue Zeile
                    if (escSequence.size() == 2) {
                        cursorColumn = 1;
                        cursorRow++;
                        if (cursorRow > 25) {
                            abg.rollAlphanumericScreen();
                            cursorRow = 25;
                        }
                        receiveSequence = false;
                    } else {
                        cursorColumn = 1;
                        cursorRow += getParameter();
                        if (cursorRow > 25) {
                            cursorRow = 25;
                        }
                        receiveSequence = false;
                    }
                    break;
                case 0x49:
                    // [ Pn I Horizontaltabulator vorwärts
                    System.out.println("ESC Folge [ Pn I noch nicht implementiert");
                    receiveSequence = false;
                    break;
                case 0x5A:
                    // [ Pn Z Horizontaltabulator rückwärts
                    System.out.println("ESC Folge [ Pn Z noch nicht implementiert");
                    receiveSequence = false;
                    break;
                case 0x59:
                    // [ Pn Y Vertikaltabulator vorwärts
                    System.out.println("ESC Folge [ Pn Y noch nicht implementiert");
                    receiveSequence = false;
                    break;
                case 0x48:
                    // [ Pn ; Pm H Cursor-Direktpositionierung
                    // H Setzen Horizontal-Tabulatorstop
                    if (escSequence.size() == 2) {
                        receiveSequence = false;
                    } else {
                        int[] params = getParameters();
                        cursorRow = params[0] == -1 ? 1 : params[0];
                        cursorColumn = params.length < 2 || params[1] == -1 ? 1 : params[1];
                        if (cursorRow < 1) {
                            cursorRow = 1;
                        }
                        if (cursorRow > 25) {
                            cursorRow = 25;
                        }
                        if (cursorColumn < 1) {
                            cursorColumn = 1;
                        }
                        if (cursorColumn > 80) {
                            cursorColumn = 80;
                        }
                        receiveSequence = false;
                    }
                    break;
                case 0x66: {
                    // [ Pn ; Pm f Cursor-Direktpositionierung
                    //System.out.println("ESC Folge [ Pn;Pm f noch nicht implementiert");
                    int[] params = getParameters();
                    cursorRow = params.length < 1 || params[0] == -1 ? 1 : params[0];
                    cursorColumn = params.length < 2 || params[1] == -1 ? 1 : params[1];
                    if (cursorRow < 1) {
                        cursorRow = 1;
                    }
                    if (cursorRow > 25) {
                        cursorRow = 25;
                    }
                    if (cursorColumn < 1) {
                        cursorColumn = 1;
                    }
                    if (cursorColumn > 80) {
                        cursorColumn = 80;
                    }

                    receiveSequence = false;
                }
                break;
                case 0x4D:
                    // M Cursor eine Zeile nach oben
                    System.out.println("ESC Folge M noch nicht implementiert");
                    receiveSequence = false;
                    break;
                case 0x4A:
                    if (escSequence.size() == 2) {
                        // J Setzen Vertikal-Tabulatorstop
                        receiveSequence = false;
                    } else {
                        // [ Ps; Ps; ... ;Ps J Löschen eines Zeichenbereiches des Bildschirms
                        int[] params = getParameters();
                        if (params[0] <= 0) {
                            for (int i = cursorColumn; i <= 80; i++) {
                                drawCharacter(0x20, cursorRow, i);
                            }
                            for (int i = cursorRow + 1; i <= 25; i++) {
                                for (int j = 1; j <= 80; j++) {
                                    drawCharacter(0x20, i, j);
                                }
                            }
                        } else if (params[0] == 1) {
                            for (int i = 1; i < cursorRow; i++) {
                                for (int j = 1; j <= 80; j++) {
                                    drawCharacter(0x20, i, j);
                                }
                            }
                            for (int i = 1; i <= cursorColumn; i++) {
                                drawCharacter(0x20, cursorRow, i);
                            }
                        } else if (params[0] == 2) {
                            for (int i = 1; i <= 25; i++) {
                                for (int j = 1; j <= 80; j++) {
                                    drawCharacter(0x20, i, j);
                                }
                            }
                        }
                        receiveSequence = false;
                    }
                    break;
                case 0x4E:
                    // [ Pn1; Pn2; ... ;Pns <SP> N Setzen Horizontal-Tabulatorstops
                    System.out.println("ESC Folge [ Pn1; Pn2 <SP> N noch nicht implementiert");
                    receiveSequence = false;
                    break;
                case 0x67:
                    // [ Ps; Ps; ... ;Ps g Löschen Tabulatorstops
                    System.out.println("ESC Folge [ Ps; Ps; ... g noch nicht implementiert");
                    receiveSequence = false;
                    break;
                case 0x57:
                    // [ Ps; Ps; ... ;Ps W Tabulator-Steuerung
                    System.out.println("ESC Folge [ Ps; Ps; ... W noch nicht implementiert");
                    receiveSequence = false;
                    break;
                case 0x68: {
                    // [ Ps; Ps; ... ;Ps h Setzen Modus
                    int[] params = getParameters();
                    for (int p : params) {
                        switch (p) {
                            case 4:
                                //System.out.println("Weiches Rollen Bildlinienweise");
                                break;
                            case 7:
                                //System.out.println("Wraparound");
                                wraparound = true;
                                break;
                            case 10:
                                abg.setCursorMode(ABG.CursorMode.CURSOR_BLINK_LINE);
                                //System.out.println("Cursor blinkender Unterstrich");
                                break;
                            case 14:
                                //System.out.println("Cursor sichtbar");
                                abg.setCursorMode(ABG.CursorMode.CURSOR_BLINK_LINE);
                                break;
                            case 16:
                                //System.out.println("Weiches Rollen normal");
                                break;
                        }
                    }
                    receiveSequence = false;
                }
                break;
                case 0x6C: {
                    // [ Ps; Ps; ... ;Ps l Rücksetzen Modus
                    int[] params = getParameters();
                    for (int p : params) {
                        //System.out.println("" + p);
                        switch (p) {
                            case 2:
                                //System.out.println("Modus 2");
                                break;
                            case 4:
                                //System.out.println("Hartes Rollen Bildlinienweise");
                                break;
                            case 7:
                                //System.out.println("kein Wraparound");
                                wraparound = false;
                                break;
                            case 10:
                                //System.out.println("Cursor nicht blinkender Block");
                                abg.setCursorMode(ABG.CursorMode.CURSOR_STATIC_BLOCK);
                                break;
                            case 14:
                                //System.out.println("Cursor unsichtbar");
                                abg.setCursorMode(ABG.CursorMode.CURSOR_INVISIBLE);
                                break;
                            case 16:
                                //System.out.println("Schnelles Weiches Rollen");
                                break;
                        }
                    }
                    receiveSequence = false;
                }
                break;
                case 0x4B: {
                    // [ Ps; Ps; ... ;Ps K Löschen eines Zeichenbereiches in der aktiven Zeile
                    int[] params = getParameters();
                    switch (params[0] == -1 ? 0 : params[0]) {
                        case 0:
                            for (int i = cursorColumn; i <= 80; i++) {
                                drawCharacter(0x20, cursorRow, i);
                            }
                            break;
                        case 1:
                            for (int i = 1; i <= cursorColumn; i++) {
                                drawCharacter(0x20, cursorRow, i);
                            }
                            break;
                        case 2:
                            for (int i = 1; i <= 80; i++) {
                                drawCharacter(0x20, cursorRow, i);
                            }
                            break;
                    }
                    receiveSequence = false;
                }
                break;
                case 0x75: {
                    // [ Pn1; Pnm1; Pn2; Pm2 <SP> u Löschen eines Zeichenbereiches von Anfangs- bis Endposition
                    int[] params = getParameters();
                    for (int i = params[0]; i <= params[2]; i++) {
                        for (int j = params[1]; j <= params[3]; j++) {
                            drawCharacter(0x20, i, j);
                        }
                    }
                    receiveSequence = false;
                }
                break;
                case 0x6D: {
                    // [ Ps; Ps; ... ;Ps m Ein-Ausschalten von Attributen
                    int[] params = getParameters();
                    for (int p : params) {
                        switch (p) {
                            case 0:
                                intense = false;
                                inverse = false;
                                flash = false;
                                underline = false;
                                break;
                            case 1:
                                intense = true;
                                break;
                            case 4:
                                underline = true;
                                break;
                            case 5:
                                flash = true;
                                break;
                            case 7:
                                inverse = true;
                                break;
                            case 22:
                                intense = false;
                                break;
                            case 24:
                                underline = false;
                                break;
                            case 25:
                                flash = false;
                                break;
                            case 27:
                                inverse = false;
                                break;
                        }
                    }
                    receiveSequence = false;
                }
                break;
                case 0x6E:
                    // [ Ps; Ps; ... ;Ps n Anforderung zur Übertragung des KGS-Status an die ZVE und Rückmeldung
                    System.out.println("ESC Folge [ Ps; Ps; ... n noch nicht implementiert");
                    receiveSequence = false;
                    break;
                case 0x52:
                    // [ Pn; Pm R Übertragung der Cursorposition vom KGS an die ZVE als Antwort auf eine Anforderung der ZVE
                    System.out.println("ESC Folge [ Pn;Pm R noch nicht implementiert");
                    receiveSequence = false;
                    break;
                case 0x63:
                    if (escSequence.size() == 1) {
                        // c Rücksetzen des KGS
                        receiveSequence = false;
                    } else {
                        // [ Ps c Anforderung zur Übertragung der KGS Gerätekennung (ESC[?2;3c)
                        writeOutputBuffer(new byte[]{0x1B, 0x5B, 0x3F, 0x32, 0x3B, 0x33, 0x63});
                        //System.out.println("ESC Folge [ Ps c noch nicht implementiert");
                        receiveSequence = false;
                    }
                    break;
                case 0x37:
                    // 7 Retten Cursorposition
                    if (escSequence.size() == 2) {
                        cursorColumnSave = cursorColumn;
                        cursorRowSave = cursorRow;
                        receiveSequence = false;
                    }
                    break;
                case 0x38:
                    // 8 Rücklesen Cursorposition
                    if (escSequence.size() == 2) {
                        cursorColumn = cursorColumnSave;
                        cursorRow = cursorRowSave;
                        receiveSequence = false;
                    }
                    break;
                case 0x5D:
                    // ] Anforderung zur Übertragung des Diagnosefiles
                    System.out.println("ESC Folge ] noch nicht implementiert");
                    receiveSequence = false;
                    break;
                case 0x5E:
                    // ^ Aufruf Testbild
                    System.out.println("ESC Folge ^ noch nicht implementiert");
                    receiveSequence = false;
                    break;
                case 0x50:
                    // P BCL BCH LAL LAH Byte1 ... ByteN Laden der ladbaren Firmware
                    System.out.println("ESC Folge P BCL BCH LAL LAH noch nicht implementiert");
                    receiveSequence = false;
                    break;
                case 0x5F:
                    // _ SAL SAR Start Programm
                    System.out.println("ESC Folge _ noch nicht implementiert");
                    receiveSequence = false;
                    break;
                case 0x70:
                    // [ p Unterdrücken Grafikanzeige
                    if (escSequence.size() == 3 && escSequence.get(1) == 0x5B) {
                        disableGraphics = true;
                        //System.out.println("Grafikmodus abgeschaltet");
                        receiveSequence = false;
                    }
                    break;
                case 0x73:
                    // [ s Erlauben Grafikanzeige
                    if (escSequence.size() == 3 && escSequence.get(1) == 0x5B) {
                        disableGraphics = false;
                        //System.out.println("Grafikmodus erlaubt");
                        receiveSequence = false;
                    }
                    break;
            }
        }
        if (!receiveSequence) {
            //System.out.println();
        }
    }

    /**
     * Liest einen Parameter aus der ESC-Steuerfolge
     *
     * @return Parameter
     */
    private int getParameter() {
        escSequence.removeFirst();
        if (escSequence.size() == 4) {
            return (escSequence.get(1) - 0x30) * 10 + (escSequence.get(2) - 0x30);
        } else if (escSequence.size() == 3) {
            return (escSequence.get(1) - 0x30);
        } else if (escSequence.size() == 2) {
            return 1;
        }
        throw new IllegalArgumentException("Falsche ESC-Sequenz" + escSequence.size());
    }

    /**
     * Liest mehrere Parameter aus der ESC-Steuerfolge
     *
     * @return Parameter
     */
    private int[] getParameters() {
        String escString = "";
        escSequence.removeFirst();
        escSequence.removeFirst();
        escSequence.removeLast();
        for (byte b : escSequence) {
            escString += (char) b;
        }
        escString = escString.trim();
        String[] params2 = escString.replaceAll("\\?", "").split(";", -1);

        int[] result = new int[params2.length];
        for (int i = 0; i < params2.length; i++) {
            String str = params2[i];
            if (str.isEmpty()) {
                result[i] = -1;
            } else {
                result[i] = Integer.parseInt(str);
            }
        }
        return result;
    }

    /**
     * Registriert das Modul für Änderungen der Systemzeit
     */
    @Override
    public void registerClocks() {
        SystemClock.getInstance().registerClock(this);
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
                InterruptSystem.getInstance().getPIC().requestInterrupt(7);
            }
        }
    }

    /**
     * Schreibt mehrere Bytes in den Ausgabe Puffer zur ZVE
     *
     * @param bytes zu schreibende Bytes
     */
    private void writeOutputBuffer(byte[] bytes) {
        deviceBuffer2.clear();
        bufferPosition = 0;
        for (int i = 0; i < bytes.length; i++) {
            deviceBuffer[bufferPosition++] = bytes[i];
        }
        setBit(OBF_BIT);
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
            BufferedImage character = BitmapGenerator.generateBitmapFromLineCode(linecode, intense, inverse, underline, false);
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
     * Führt die Anweisungen aus dem Grafik-Puffer aus
     */
    private void executeGraphicsBuffer() {
//        int byteCnt = (escSequence.get(2) << 8) + escSequence.get(1);
        int pos = 3;
        do {
            switch (escSequence.get(pos++)) {
                case 1: {
                    // Setze Funktionskennzeichen
                    int fk = escSequence.get(pos++);
                    System.out.println("Setze Funktionskennzeichen fk=" + fk);
                }
                break;
                case 2: {
                    // Setze Register des grafischen Cursors
                    int x = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    int y = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    System.out.println("Setze Register grafischer Kursor x=" + x + ",y=" + y);
                }
                break;
                case 4: {
                    // Setze Splitgrenze
                    int z = escSequence.get(pos++);
                    System.out.println("Setze Splitgrenze z=" + z);
                }
                break;
                case 7: {
                    // Initialisiere Speicher
                    int im = escSequence.get(pos++);
                    int s = escSequence.get(pos++);
                    System.out.println("Initialisiere Speicher im=" + im + ",s=" + s);
                }
                break;
                case 8: {
                    // Initialisiere Speicherabschnitt 
                    int im = escSequence.get(pos++);
                    int s = escSequence.get(pos++);
                    int ya = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    int ye = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    System.out.println("Initialisiere Speicherabschnitt im=" + im + ",s=" + s + ",ya=" + ya + ",ye=" + ye);
                }
                break;
                case 9: {
                    // Initialisiere Speicherausschnitt
                    int im = escSequence.get(pos++);
                    int s = escSequence.get(pos++);
                    int xa = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    int ya = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    int xe = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    int ye = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    System.out.println("Initialisiere Speicherausschnitt im=" + im + ",s=" + s + ",xa=" + xa + ",xe=" + xe + ",ya=" + ya + ",ye=" + ye);
                }
                break;
                case 10: {
                    // Übernehme Palettenregisterbelegung
                    int vi = escSequence.get(pos++);
                    int e1 = escSequence.get(pos++);
                    int e2 = escSequence.get(pos++);
                    int e3 = escSequence.get(pos++);
                    int e4 = escSequence.get(pos++);
                    System.out.println("Übernehme Palettenregisterbelegung vi=" + vi + ",e1=" + e1 + ",e2=" + e2 + ",e3=" + e3 + ",e4=" + e4);
                }
                break;
                case 11: {
                    // Aktiviere Palettenregisterbelegung
                    int vi = escSequence.get(pos++);
                    System.out.println("Aktiviere Palettenregisterbelegung vi=" + vi);
                }
                break;
                case 13: {
                    // Übernehme Musterbox
                    pos++;
                    int pfx = escSequence.get(pos++);
                    int pfy = escSequence.get(pos++);
                    int nz = escSequence.get(pos++);
                    int ns = escSequence.get(pos++);
                    System.out.println("Übernehme Musterbox pfx=" + pfx + ",pfy=" + pfy + ",nz=" + nz + ",ns=" + ns);
                }
                break;
                case 14: {
                    // Setze Window
                    int x1 = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    int y1 = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    int x2 = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    int y2 = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    System.out.println("Setze Window x1=" + x1 + ",y1=" + y1 + ",x2=" + x2 + ",y2=" + y2);
                }
                break;
                case 15: {
                    // Windowdarstellung
                    int s = escSequence.get(pos++);
                    System.out.println("Windowsdarstellung s=" + s);
                }
                break;
                case 16: {
                    // Setze Startpunkt für Liniengenerierung
                    int li = escSequence.get(pos++);
                    int x = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    int y = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    System.out.println("Setze Startpunkt für Liniengenerierung li=" + li + ",x=" + x + ",y=" + y);
                }
                break;
                case 17: {
                    // Generiere Linie
                    int x = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    int y = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    System.out.println("Generiere Linie x=" + x + ",y=" + y);
                }
                break;
                case 18: {
                    // Setze Marker
                    int x = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    int y = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    System.out.println("Setze Marker x=" + x + ",y=" + y);
                }
                break;
                case 19: {
                    // Setze Schreibposition
                    int x = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    int y = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    System.out.println("Setze Schreibposition x=" + x + ",y=" + y);
                }
                break;
                case 20: {
                    // Generiere Text
                    int n = escSequence.get(pos++);
                    int[] c = new int[n];
                    for (int i = 0; i < n; i++) {
                        c[i] = escSequence.get(pos++);
                    }
                    System.out.println("Generiere Text Länge n=" + n);
                }
                break;
                case 21: {
                    // Setze Linientyp bzw. Speicherebenenauswahl fuer Text
                    int li = escSequence.get(pos++);
                    int lt = escSequence.get(pos++);
                    int ls = escSequence.get(pos++);
                    int s = escSequence.get(pos++);
                    System.out.println("Setze Linientyp/Speicherebenenauswahl li=" + li + ",lt=" + lt + ",ls=" + ls + ",s=" + s);
                }
                break;
                case 22: {
                    // Setze Markertyp
                    int mi = escSequence.get(pos++);
                    int mt = escSequence.get(pos++);
                    int s = escSequence.get(pos++);
                    System.out.println("Setze Markertyp mi=" + mi + ",mt=" + mt + ",s=" + s);
                }
                break;
                case 23: {
                    // Setze Schreibtyp
                    int st = escSequence.get(pos++);
                    System.out.println("Setze Schreibtyp st=" + st);
                }
                break;
                case 24: {
                    // Eckpunkt Typ 0 einer zu fuellenden Fläche
                    int x = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    int y = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    System.out.println("Setze Eckpunkt Typ 0 füllende Fläche x=" + x + ",y=" + y);
                }
                break;
                case 25: {
                    // Eckpunkt Typ 1 einer zu fuellenden Fläche
                    int x = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    int y = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    System.out.println("Setze Eckpunkt Typ 1 füllende Fläche x=" + x + ",y=" + y);
                }
                break;
                case 26: {
                    // Setze Parameter für FILL AREA
                    int ls = escSequence.get(pos++);
                    int fa = escSequence.get(pos++);
                    switch (fa) {
                        case 0: {
                            int s = escSequence.get(pos++);
                            System.out.println("Setze Parameter für FILL AREA ls=" + ls + ",fa=" + fa + ",s=" + s);
                        }
                        break;
                        case 1: {
                            int s = escSequence.get(pos++);
                            System.out.println("Setze Parameter für FILL AREA ls=" + ls + ",fa=" + fa + ",s=" + s);
                        }
                        break;
                        case 2: {
                            int prx = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                            pos = pos + 2;
                            int pry = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                            pos = pos + 2;
                            System.out.println("Setze Parameter für FILL AREA ls=" + ls + ",fa=" + fa + ",prx=" + prx + ",pry=" + pry);
                        }
                        break;
                        case 3: {
                            int i = escSequence.get(pos++);
                            int s = escSequence.get(pos++);
                            System.out.println("Setze Parameter für FILL AREA ls=" + ls + ",fa=" + fa + ",i=" + i + ",s=" + s);
                        }
                        break;
                    }
                }
                break;
                case 27: {
                    // Starte Fuellen
                    int kr = escSequence.get(pos++);
                    System.out.println("Starte Füllen kr=" + kr);
                }
                break;
                case 30: {
                    // Unbekannt
                    int param1 = escSequence.get(pos++);
                    System.out.println("Unbekanntes Kommando param1=" + param1);
                }
                break;
                case 31: {
                    // Setze Request Modus
                    int rm = escSequence.get(pos++);
                    System.out.println("Setze Request Modus rm=" + rm);
                }
                break;
                case 33: {
                    // Setze Anfangswert für LOCATOR
                    int sw = escSequence.get(pos++);
                    if (sw != 0) {
                        int x = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                        pos = pos + 2;
                        int y = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                        pos = pos + 2;
                        System.out.println("Setze Anfangswert für Locator sw=" + sw + ",x=" + x + ",y=" + y);
                    }
                    System.out.println("Setze Anfangswert für Locator sw=" + sw);
                }
                break;
                case 40: {
                    // Initialisiere GSX
                    int lt = escSequence.get(pos++);
                    int s1 = escSequence.get(pos++);
                    int mt = escSequence.get(pos++);
                    int s2 = escSequence.get(pos++);
                    System.out.println("Initialisiere GSX lt=" + lt + ",s1=" + s1 + ",mt=" + mt + ",s2=" + s2);
                }
                break;
                case 41: {
                    // Generiere Kreisbogen
                    int xm = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    int ym = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    int xa = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    int ya = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    int xe = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    int ye = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    int r = (escSequence.get(pos) << 8) + escSequence.get(pos + 1);
                    pos = pos + 2;
                    System.out.println("Generiere Kreisbogen xm=" + xm + ",ym=" + ym + ",xa=" + xa + ",ya=" + ya + ",xe=" + xe + ",ye=" + ye + ",r=" + r);
                }
                break;
                case 42: {
                    // Setze Textfont
                    int tf = escSequence.get(pos++);
                    System.out.println("Setze Textfont tf=" + tf);
                }
                break;
            }
        } while (pos < escSequence.size());
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
        dos.writeInt(cursorRow);
        dos.writeInt(cursorColumn);
        dos.writeBoolean(receiveSequence);
        dos.writeInt(escSequence.size());
        for (int i = 0; i < escSequence.size(); i++) {
            dos.writeByte(escSequence.get(i));
        }
        for (int i = 0; i < 80; i++) {
            dos.writeBoolean(hTabs[i]);
        }
        for (int i = 0; i < 25; i++) {
            dos.writeBoolean(vTabs[i]);
        }
        dos.writeBoolean(disableGraphics);
        dos.writeBoolean(initialized);
        dos.writeLong(interruptClock);
        dos.writeBoolean(interruptWaiting);
        dos.write(deviceBuffer);
        dos.writeInt(bufferPosition);
        dos.writeInt(cursorRowSave);
        dos.writeInt(cursorColumnSave);
        dos.writeBoolean(intense);
        dos.writeBoolean(inverse);
        dos.writeBoolean(flash);
        dos.writeBoolean(underline);
        dos.writeBoolean(wraparound);
        dos.writeInt(deviceBuffer2.size());
        for (int i = 0; i < deviceBuffer2.size(); i++) {
            dos.writeByte(deviceBuffer2.get(i));
        }
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
        cursorRow = dis.readInt();
        cursorColumn = dis.readInt();
        receiveSequence = dis.readBoolean();
        int escSize = dis.readInt();
        for (int i = 0; i < escSize; i++) {
            escSequence.add(dis.readByte());
        }
        for (int i = 0; i < 80; i++) {
            hTabs[i] = dis.readBoolean();
        }
        for (int i = 0; i < 25; i++) {
            vTabs[i] = dis.readBoolean();
        }
        disableGraphics = dis.readBoolean();
        initialized = dis.readBoolean();
        interruptClock = dis.readLong();
        interruptWaiting = dis.readBoolean();
        dis.read(deviceBuffer);
        bufferPosition = dis.readInt();
        cursorRowSave = dis.readInt();
        cursorColumnSave = dis.readInt();
        intense = dis.readBoolean();
        inverse = dis.readBoolean();
        flash = dis.readBoolean();
        underline = dis.readBoolean();
        wraparound = dis.readBoolean();

        int size = dis.readInt();
        deviceBuffer2.clear();
        for (int i = 0; i < size; i++) {
            deviceBuffer2.add(dis.readByte());
        }
        ram.loadMemory(dis);
        abg.loadState(dis);
    }
}
