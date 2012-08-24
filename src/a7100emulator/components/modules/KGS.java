/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.modules;

import a7100emulator.Tools.BitmapGenerator;
import a7100emulator.Tools.Memory;
import a7100emulator.components.system.InterruptSystem;
import a7100emulator.components.system.SystemClock;
import a7100emulator.components.system.SystemPorts;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 *
 * @author Dirk
 */
public final class KGS implements PortModule, ClockModule {

    // Zeichensätze
    private Memory characterCodes = new Memory(4096);
    // Zeichencodes
    private final static int CODE_NUL = 0x00;
    private final static int CODE_SOH = 0x01;
    private final static int CODE_STX = 0x02;
    private final static int CODE_ETX = 0x03;
    private final static int CODE_EOT = 0x04;
    private final static int CODE_ENQ = 0x05;
    private final static int CODE_ACK = 0x06;
    private final static int CODE_BEL = 0x07;
    private final static int CODE_BS = 0x08;
    private final static int CODE_HT = 0x09;
    private final static int CODE_LF = 0x0A;
    private final static int CODE_VT = 0x0B;
    private final static int CODE_FF = 0x0C;
    private final static int CODE_CR = 0x0D;
    private final static int CODE_SO = 0x0E;
    private final static int CODE_SI = 0x0F;
    private final static int CODE_DLE = 0x10;
    private final static int CODE_DC1 = 0x11;
    private final static int CODE_DC2 = 0x12;
    private final static int CODE_DC3 = 0x13;
    private final static int CODE_DC4 = 0x14;
    private final static int CODE_NAK = 0x15;
    private final static int CODE_SYN = 0x16;
    private final static int CODE_ETB = 0x17;
    private final static int CODE_CAN = 0x18;
    private final static int CODE_EM = 0x19;
    private final static int CODE_SUB = 0x1A;
    private final static int CODE_ESC = 0x1B;
    private final static int CODE_FS = 0x1C;
    private final static int CODE_GS = 0x1D;
    private final static int CODE_RS = 0x1E;
    private final static int CODE_US = 0x1F;
    private final static int CODE_DEL = 0x7F;
    // Ports
    private final static int PORT_KGS_STATE = 0x200;
    private final static int PORT_KGS_DATA = 0x202;
    // Status Bits
    private static final int ERR_BIT = 0x80;
    private static final int INT_BIT = 0x04;
    private static final int IBF_BIT = 0x02;
    private static final int OBF_BIT = 0x01;
    private int state = 1;
    private int cursorRow = 1;
    private int cursorColumn = 1;
    private boolean receiveESC = false;
    private byte[] ESCBytes = new byte[256];
    private byte ESCPosition = 0;
    private boolean[] hTabs = new boolean[80];
    private boolean[] vTabs = new boolean[25];
    private ABG abg;
    private boolean disableGraphics = false;
    private boolean initialized = false;
    private int output = 0;
    private long interruptClock = 0;
    private boolean interruptWaiting = false;

    public KGS() {
        init();
    }

    @Override
    public void registerPorts() {
        SystemPorts.getInstance().registerPort(this, PORT_KGS_STATE);
        SystemPorts.getInstance().registerPort(this, PORT_KGS_DATA);
    }

    @Override
    public void writePort_Byte(int port, int data) {
        //System.out.println("OUT Byte " + Integer.toHexString(data) + "(" + Integer.toBinaryString(data) + ")" + " to port " + Integer.toHexString(port));
        switch (port) {
            case PORT_KGS_STATE:
                clearBit(INT_BIT);
                clearBit(ERR_BIT);
                if (!initialized) {
                    output = 0x00;
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

    @Override
    public int readPort_Byte(int port) {
        //System.out.println("IN Byte from port " + Integer.toHexString(port));
        int result = 0;
        switch (port) {
            case PORT_KGS_STATE:
                result = state;
                break;
            case PORT_KGS_DATA:
                result = output;
                clearBit(OBF_BIT);
                break;
        }
        return result;
    }

    @Override
    public int readPort_Word(int port) {
        int result = 0;
        switch (port) {
            case PORT_KGS_STATE:
                result = state;
                break;
            case PORT_KGS_DATA:
                break;
        }
        return result;
    }

    @Override
    public void init() {
        characterCodes.loadFile(0x00, new File("./eproms/CHARGEN.bin"), Memory.FileLoadMode.LOW_AND_HIGH_BYTE);
        initTabs();
        abg = new ABG();
        registerPorts();
        registerClocks();
    }

    private void initTabs() {
        for (int i = 0; i < 73; i = i + 8) {
            hTabs[i] = true;
        }
        for (int i = 0; i < 25; i++) {
            vTabs[i] = true;
        }
    }

    private void setBit(int bit) {
        state |= bit;
    }

    private void clearBit(int bit) {
        state &= ~bit;
    }

    private boolean getBit(int bit) {
        return (state & bit) != 0;
    }

    private void dataReceived(int data) {
        if (initialized) {
            if (receiveESC) {
                ESCBytes[ESCPosition++] = (byte) (data & 0xFF);
                checkESC();
            } else {
                if (data >= 0x20 && data != CODE_DEL) {
                    // Darstellbares Zeichen
                    byte[] linecode = new byte[16];
                    for (byte line = 0; line < 16; line++) {
                        linecode[line] = (byte) (characterCodes.readByte(16 * data + line) & 0xFF);
                    }
                    BufferedImage character = BitmapGenerator.generateBitmapFromLineCode(linecode, false, false, false, false);
                    abg.updateAlphanumericScreenRectangle((cursorColumn - 1) * 8, (cursorRow - 1) * 16, character);
                    cursorColumn++;
                    if (cursorColumn == 81) {
                        cursorColumn = 1;
                        if (cursorRow != 25) {
                            cursorRow++;
                        } else {
                            abg.rollAlphanumericScreen();
                        }
                    }
                } else {
                    // Steuerzeichen
                    switch (data) {
                        case CODE_NUL:
                            // Nichts
                            break;
                        case CODE_SOH:
                            // Start Grafiktastencode
                            break;
                        case CODE_STX:
                            // Start Grafik-Kommando
                            break;
                        case CODE_ETX:
                            // Ende Grafikkommando
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
                            // TODO
                            break;
                        case CODE_SI:
                            // Shift In
                            // TODO
                            break;
                        case CODE_DLE:
                            // Data Link Escape
                            // TODO
                            break;
                        case CODE_CAN:
                            // Cancel ESC
                            break;
                        case CODE_ESC:
                            receiveESC = true;
                            ESCPosition = 0;
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
        } else {
            // TODO Initialisierung
            if (data == 0xAA) {
                output = 0x55;
                setBit(OBF_BIT);
            } else if (data == 0x55) {
                setBit(INT_BIT);
                setBit(ERR_BIT);
            }
        }
    }

    private void checkESC() {
        switch (ESCBytes[ESCPosition - 1]) {
            case 0x41:
            case 0x6B:
                // [ Pn A / [ Pn k Cursor nach oben
                cursorRow -= getParameter();
                if (cursorRow < 1) {
                    cursorRow = 1;
                }
                receiveESC = false;
                break;
            case 0x42:
            case 0x65:
                // [ Pn B / [ Pn e Cursor nach unten
                cursorRow += getParameter();
                if (cursorRow > 25) {
                    cursorRow = 25;
                }
                receiveESC = false;
                break;
            case 0x43:
            case 0x61:
                // [ Pn C / [ Pn a Cursor nach rechts
                cursorColumn += getParameter();
                if (cursorRow > 80) {
                    cursorColumn = 80;
                }
                receiveESC = false;
                break;
            case 0x44:
                // [ Pn D Cursor nach links
                // D Zeilenschaltung
                if (ESCPosition == 1) {
                    cursorRow++;
                    if (cursorRow > 25) {
                        abg.rollAlphanumericScreen();
                        cursorRow = 25;
                    }
                    receiveESC = false;
                } else {
                    cursorColumn -= getParameter();
                    if (cursorRow < 1) {
                        cursorColumn = 1;
                    }
                    receiveESC = false;
                }
                break;
            case 0x6A:
                // [ Pn j Cursor nach links
                cursorColumn -= getParameter();
                if (cursorRow < 1) {
                    cursorColumn = 1;
                }
                receiveESC = false;
                break;
            case 0x47:
            case 0x60:
                // [ Pn G / [ Pn ` Horizontalposition absolut
                cursorColumn = getParameter();
                if (cursorColumn > 80) {
                    cursorColumn = 80;
                }
                receiveESC = false;
                break;
            case 0x64:
                // [ Pn d Vertikalposition absolut
                cursorRow = getParameter();
                if (cursorRow > 25) {
                    cursorRow = 25;
                }
                receiveESC = false;
                break;
            case 0x46:
                // [ Pn F Cursor nach oben an Zeilenanfang
                cursorColumn = 1;
                cursorRow -= getParameter();
                if (cursorRow < 1) {
                    cursorRow = 1;
                }
                receiveESC = false;
                break;
            case 0x45:
                // [ Pn E Cursor nach unten an Zeilenanfang
                // E Neue Zeile
                if (ESCPosition == 1) {
                    cursorColumn = 1;
                    cursorRow++;
                    if (cursorRow > 25) {
                        abg.rollAlphanumericScreen();
                        cursorRow = 25;
                    }
                    receiveESC = false;
                } else {
                    cursorColumn = 1;
                    cursorRow += getParameter();
                    if (cursorRow > 25) {
                        cursorRow = 25;
                    }
                    receiveESC = false;
                }
                break;
            case 0x49:
                // [ Pn I Horizontaltabulator vorwärts
                System.out.println("ESC Folge [ Pn I noch nicht implementiert");
                receiveESC = false;
                break;
            case 0x5A:
                // [ Pn Z Horizontaltabulator rückwärts
                System.out.println("ESC Folge [ Pn Z noch nicht implementiert");
                receiveESC = false;
                break;
            case 0x59:
                // [ Pn Y Vertikaltabulator vorwärts
                System.out.println("ESC Folge [ Pn Y noch nicht implementiert");
                receiveESC = false;
                break;
            case 0x48:
                // [ Pn ; Pm H Cursor-Direktpositionierung
                // H Setzen Horizontal-Tabulatorstop
                System.out.println("ESC Folge [ Pn;Pm H noch nicht implementiert");
                receiveESC = false;
                break;
            case 0x66:
                // [ Pn ; Pm f Cursor-Direktpositionierung
                System.out.println("ESC Folge [ Pn;Pm f noch nicht implementiert");
                receiveESC = false;
                break;
            case 0x4D:
                // M Cursor eine Zeile nach oben
                System.out.println("ESC Folge M noch nicht implementiert");
                receiveESC = false;
                break;
            case 0x4A:
                // J Setzen Vertikal-Tabulatorstop
                // [ Ps; Ps; ... ;Ps J Löschen eines Zeichenbereiches des Bildschirms
                System.out.println("ESC Folge [ Ps;Ps;... J noch nicht implementiert");
                receiveESC = false;
                break;
            case 0x4E:
                // [ Pn1; Pn2; ... ;Pns <SP> N Setzen Horizontal-Tabulatorstops
                System.out.println("ESC Folge [ Pn1; Pn2 <SP> N noch nicht implementiert");
                receiveESC = false;
                break;
            case 0x67:
                // [ Ps; Ps; ... ;Ps g Löschen Tabulatorstops
                System.out.println("ESC Folge [ Ps; Ps; ... g noch nicht implementiert");
                receiveESC = false;
                break;
            case 0x57:
                // [ Ps; Ps; ... ;Ps W Tabulator-Steuerung
                System.out.println("ESC Folge [ Ps; Ps; ... W noch nicht implementiert");
                receiveESC = false;
                break;
            case 0x68:
                // [ Ps; Ps; ... ;Ps h Setzen Modus
                System.out.println("ESC Folge [ Ps; Ps; ... ;Ps h noch nicht implementiert");
                receiveESC = false;
                break;
            case 0x6C:
                // [ Ps; Ps; ... ;Ps l Rücksetzen Modus
                System.out.println("ESC Folge [ Ps; Ps; ... ;Ps l noch nicht implementiert");
                receiveESC = false;
                break;
            case 0x4B:
                // [ Ps; Ps; ... ;Ps K Löschen eines Zeichenbereiches in der aktiven Zeile
                System.out.println("ESC Folge [ Ps; Ps; ... ;Ps K noch nicht implementiert");
                receiveESC = false;
                break;
            case 0x75:
                // [ Pn1; Pnm1; Pn2; Pm2 <SP> u Löschen eines Zeichenbereiches von Anfangs- bis Endposition
                System.out.println("ESC Folge [ Pn1; Pnm1; Pn2; Pm2 <SP> u noch nicht implementiert");
                receiveESC = false;
                break;
            case 0x6D:
                // [ Ps; Ps; ... ;Ps m Ein-Ausschalten von Attributen
                System.out.println("ESC Folge [ Ps; PS; ... m noch nicht implementiert");
                receiveESC = false;
                break;
            case 0x6E:
                // [ Ps; Ps; ... ;Ps n Anforderung zur Übertragung des KGS-Status an die ZVE und Rückmeldung
                System.out.println("ESC Folge [ Ps; Ps; ... n noch nicht implementiert");
                receiveESC = false;
                break;
            case 0x52:
                // [ Pn; Pm R Übertragung der Cursorposition vom KGS an die ZVE als Antwort auf eine Anforderung der ZVE
                System.out.println("ESC Folge [ Pn;Pm R noch nicht implementiert");
                receiveESC = false;
                break;
            case 0x63:
                // c Rücksetzen des KGS
                // [ Ps c Anforderung zur Übertragung der KGS Gerätekennung
                System.out.println("ESC Folge [ Ps c noch nicht implementiert");
                receiveESC = false;
                break;
            case 0x37:
                // 7 Retten Cursorposition
                System.out.println("ESC Folge 7 noch nicht implementiert");
                if (ESCPosition == 1) {
                    receiveESC = false;
                }
                break;
            case 0x38:
                // 8 Rücklesen Cursorposition
                System.out.println("ESC Folge 8 noch nicht implementiert");
                if (ESCPosition == 1) {
                    receiveESC = false;
                }
                break;
            case 0x5D:
                // ] Anforderung zur Übertragung des Diagnosefiles
                System.out.println("ESC Folge ] noch nicht implementiert");
                receiveESC = false;
                break;
            case 0x5E:
                // ^ Aufruf Testbild
                System.out.println("ESC Folge ^ noch nicht implementiert");
                receiveESC = false;
                break;
            case 0x50:
                // P BCL BCH LAL LAH Byte1 ... ByteN Laden der ladbaren Firmware
                System.out.println("ESC Folge P BCL BCH LAL LAH noch nicht implementiert");
                receiveESC = false;
                break;
            case 0x5F:
                // _ SAL SAR Start Programm
                System.out.println("ESC Folge _ noch nicht implementiert");
                receiveESC = false;
                break;
            case 0x70:
                // [ p Unterdrücken Grafikanzeige
                if (ESCPosition == 2 && ESCBytes[0] == 0x5B) {
                    disableGraphics = true;
                    System.out.println("Grafikmodus abgeschaltet");
                    receiveESC = false;
                }
                break;
            case 0x73:
                // [ s Erlauben Grafikanzeige
                if (ESCPosition == 2 && ESCBytes[0] == 0x5B) {
                    disableGraphics = false;
                    System.out.println("Grafikmodus erlaubt");
                    receiveESC = false;
                }
                break;
        }
    }

    private int getParameter() {
        if (ESCBytes.length == 4) {
            return (ESCBytes[1] - 0x30) * 10 + (ESCBytes[2] - 0x30);
        } else if (ESCBytes.length == 3) {
            return (ESCBytes[1] - 0x30);
        }
        throw new IllegalArgumentException("Falsche ESC-Sequenz");
    }

    @Override
    public void registerClocks() {
        SystemClock.getInstance().registerClock(this);
    }

    @Override
    public void clockUpdate(int amount) {
        if (interruptWaiting) {
            interruptClock += amount;
            if (interruptClock > 20) {
                interruptWaiting = false;
                InterruptSystem.getInstance().addIRInterrupt(7);
            }
        }
    }
}
