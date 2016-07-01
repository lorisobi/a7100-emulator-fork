/*
 * Keyboard.java
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
 *   05.04.2014 - Kommentare vervollständigt
 *   25.07.2014 - Puffer aus USART in Keyboard ausgelagert
 *              - Speichern und Laden des Puffers
 *              - Systemzeit implementiert
 *   18.11.2014 - Interface StateSavable implementiert
 *   22.02.2015 - Grafische Kommandos implementiert
 *              - Alt implementiert
 *   27.02.2015 - CE, Pfeil nach unten - implementiert
 *   15.07.2015 - Fehlendes break ergänzt
 *   22.07.2015 - ERASE INP, ERASE EOF, FM, DUP, INS LINE, DEL LINE ergänzt
 *   24.07.2015 - RESET, CLEAR ergänzt
 *              - Fehler in Steuerfolgen behoben 0 durch O ersetzt
 *   25.07.2016 - keyboardClock und selfTest in Reset,Laden und Speichern
 *              - Kommentare überarbeitet
 */
package a7100emulator.components.system;

import a7100emulator.Tools.StateSavable;
import a7100emulator.components.ic.KR580WM51A;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Klasse zur Realisierung einer A7100 Tastatur. Die Klasse nimmt Tastaturevents
 * entgegen und übersetzt diese in die jeweilige Tastenfolge der A7100 Tastatur.
 * Die Tasten der PC-tastatur werden wie folgt übersetzt:
 * <p>
 * 1:1 Umsetzung:
 * <ul>
 * <li>Alphanumerik: A-Z, a-z, 1-0</li>
 * <li>Sondertasten: Backspace, ESC, Enter, Tabulator, DEL, Ctrl, Alt</li>
 * <li>Navigation : Pfeile</li>
 * <li>Symbole : ! &quot; § $ % &amp; / ( ) = &lt; &gt; , ; . : - _ # + * ? ´
 * `</li>
 * </ul>
 * <p>
 * Andere Umsetzungen:
 * <ul>
 * <li>^ &rarr; = (Ziffernblock)</li>
 * <li>' &rarr; ^</li>
 * <li>ö &rarr; \</li>
 * <li>Ö &rarr; |</li>
 * <li>ä &rarr; [</li>
 * <li>Ä &rarr; {</li>
 * <li>ü &rarr; ]</li>
 * <li>Ü &rarr; }</li>
 * <li>ß &rarr; Überstrich</li>
 * <li>F1-F12 &rarr; PF1-PF12</li>
 * <li>Pause &rarr; Break</li>
 * <li>Shift+Insert &rarr; Insert Line</li>
 * <li>Shift+Delete &rarr; Delete Line</li>
 * <li>Home &rarr; PA1</li>
 * <li>Shift+Home &rarr; PA2</li>
 * <li>End &rarr; PA3</li>
 * <li>Shift+End &rarr; Pfeil Links-Oben</li>
 * <li>PageUp &rarr; Erase EOF</li>
 * <li>Shift+PageUp &rarr; Erase INP</li>
 * <li>PageDn &rarr; DUP</li>
 * <li>Shift+PageDn &rarr; FM</li>
 * <li>Print &rarr; Line Feed</li>
 * <li>Shift+Print &rarr; Clear</li>
 * <li>Shift+Esc &rarr; RESET</li>
 * </ul>
 * Fehlende Umsetzungen:
 * <ul>
 * <li>Insert &rarr; INS Mode</li>
 * <li>Rollen &rarr; MOD 2</li>
 * <li>Steuerzeichen : 1B, 1C, 1D, 1E, 1F</li>
 * <li>Alternativzeichen : DE, DF, E0</li>
 * <li>CAPS-LOCK</li>
 * </ul>
 * <p>
 * TODO:
 * <ul>
 * <li>Tastaturtyp K7672 noch nicht implementiert</li>
 * <li>Wenige Tasten noch nicht implementiert</li>
 * <li>Selbsttest-Verzögerung überarbeiten</li>
 * <li>Break funktioniert im SCP nicht</li>
 * </ul>
 *
 * @author Dirk Bräuer
 */
public class Keyboard implements KeyListener, StateSavable {

    /**
     * Enum der Tastaturtypen
     */
    private enum KeyboardType {

        /**
         * Tastaturtyp K7637
         */
        K7637,
        /**
         * Tastaturtyp K7672
         */
        K7672;
    }
    /**
     * USART Controller
     */
    private KR580WM51A ifssController = null;
    /**
     * Gibt an, ob der ALT-Modus aktiv ist
     */
    private boolean alt = false;
    /**
     * Gibt an, ob CAPS aktiv ist
     */
    private boolean caps = false;
    /**
     * Gibt an, MODE 2 aktiv ist
     */
    private boolean mode2 = false;
    /**
     * Instanz
     */
    private static Keyboard instance = null;
    /**
     * Puffergröße
     */
    private int byteCnt = 0;
    /**
     * Puffer mit Zeichen von der Tastatur
     */
    private byte[] commands = new byte[10];
    /**
     * Tastaturtyp
     */
    private KeyboardType kbdType = KeyboardType.K7637;
    /**
     * Sendepuffer
     */
    private final LinkedList<Byte> sendBuffer = new LinkedList();
    /**
     * Counter für Sendepuffer
     */
    private long keyboardClock = 0;
    /**
     * Gibt an ob ein Selbsttest durchgeführt wird (längere Wartezeit)
     */
    private boolean selfTest = false;

    /**
     * Erstellt eine neue Tastatur
     */
    private Keyboard() {
    }

    /**
     * Gibt die Instanz der Tastatur zurück
     *
     * @return Instanz
     */
    public static Keyboard getInstance() {
        if (instance == null) {
            instance = new Keyboard();
        }
        return instance;
    }

    /**
     * Registriert den USART Controller bei der Tastatur.
     *
     * @param controller USART-Controller
     */
    public void registerController(KR580WM51A controller) {
        ifssController = controller;
    }

    /**
     * Verarbeitet das Drücken einer Taste. Dieses Event wird vom Emulator nicht
     * ausgewertet.
     *
     * @param e Event für das Drücken einer Taste
     */
    @Override
    public void keyTyped(KeyEvent e) {
    }

    /**
     * Verarbeitet das Drücken und Loslassen einer Taste. Dieses Event wird vom
     * Emulator nicht ausgewertet.
     *
     * @param e Event für das Drücken und Loslassen einer Taste
     */
    @Override
    public void keyPressed(KeyEvent e) {
    }

    /**
     * Verarbeitet das Loslassen einer Taste. Das Kommando für die gedrückte
     * Taste wird entsprechend des ausgewählten Tastaturtyps übersetzt. Die
     * einzelnen Tastencodes werden in den Sendepuffer übertragen und können
     * dort vom Controller abgerufen werden.
     * <p>
     * TODO:
     * <ul>
     * <li>Grafiktasten vollständig implementieren</li>
     * <li>MOD 2 implementieren</li>
     * <li>INS MOD implementieren</li>
     * <li>CAPS implementieren</li>
     * <li>ALT als Zustand implementieren</li>
     * <li>CTRL+ALT implementieren</li>
     * </ul>
     *
     * @param e Event für das Loslassen einer Taste
     */
    @Override
    public void keyReleased(KeyEvent e) {
        boolean keyShift = e.isShiftDown();
        boolean keyCtrl = e.isControlDown();
        boolean keyAlt = e.isAltDown();

//        boolean keyAltGr = e.isAltGraphDown();
//        System.out.println(Integer.toHexString(e.getKeyCode())+" "+e.getKeyChar());
//        if (e.isAltDown()) {
//            System.out.println("Alt");
//        }
//        if (e.isControlDown()) {
//            System.out.println("Ctrl");
//        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_0:
                sendByte(keyCtrl ? 0x8A : keyShift ? 0x3D : 0x30);
                break;
            case KeyEvent.VK_1:
                sendByte(keyCtrl ? 0x97 : keyShift ? 0x21 : 0x31);
                break;
            case KeyEvent.VK_2:
                sendByte(keyCtrl ? 0x96 : keyShift ? 0x22 : 0x32);
                break;
            case KeyEvent.VK_3:
                sendByte(keyShift ? (keyAlt ? 0xC0 : 0x40) : 0x33);
                break;
            case KeyEvent.VK_4:
                sendByte(keyCtrl ? 0x84 : keyShift ? 0x24 : 0x34);
                break;
            case KeyEvent.VK_5:
                sendByte(keyCtrl ? 0x82 : keyShift ? 0x25 : 0x35);
                break;
            case KeyEvent.VK_6:
                sendByte(keyCtrl ? 0x80 : keyShift ? 0x26 : 0x36);
                break;
            case KeyEvent.VK_7:
                sendByte(keyCtrl ? 0x81 : keyShift ? 0x2F : 0x37);
                break;
            case KeyEvent.VK_8:
                sendByte(keyCtrl ? 0x83 : keyShift ? 0x28 : 0x38);
                break;
            case KeyEvent.VK_9:
                sendByte(keyCtrl ? 0x85 : keyShift ? 0x29 : 0x39);
                break;
            case KeyEvent.VK_A:
                sendByte(keyCtrl ? 0x01 : keyShift ? (keyAlt ? 0xC1 : 0x41) : (keyAlt ? 0xE1 : 0x61));
                break;
            case KeyEvent.VK_B:
                sendByte(keyCtrl ? 0x02 : keyShift ? (keyAlt ? 0xC2 : 0x42) : (keyAlt ? 0xE2 : 0x62));
                break;
            case KeyEvent.VK_C:
                sendByte(keyCtrl ? 0x03 : keyShift ? (keyAlt ? 0xC3 : 0x43) : (keyAlt ? 0xE3 : 0x63));
                break;
            case KeyEvent.VK_D:
                sendByte(keyCtrl ? 0x04 : keyShift ? (keyAlt ? 0xC4 : 0x44) : (keyAlt ? 0xE4 : 0x64));
                break;
            case KeyEvent.VK_E:
                sendByte(keyCtrl ? 0x05 : keyShift ? (keyAlt ? 0xC5 : 0x45) : (keyAlt ? 0xE5 : 0x65));
                break;
            case KeyEvent.VK_F:
                sendByte(keyCtrl ? 0x06 : keyShift ? (keyAlt ? 0xC6 : 0x46) : (keyAlt ? 0xE6 : 0x66));
                break;
            case KeyEvent.VK_G:
                sendByte(keyCtrl ? 0x07 : keyShift ? (keyAlt ? 0xC7 : 0x47) : (keyAlt ? 0xE7 : 0x67));
                break;
            case KeyEvent.VK_H:
                sendByte(keyCtrl ? 0x08 : keyShift ? (keyAlt ? 0xC8 : 0x48) : (keyAlt ? 0xE8 : 0x68));
                break;
            case KeyEvent.VK_I:
                sendByte(keyCtrl ? 0x09 : keyShift ? (keyAlt ? 0xC9 : 0x49) : (keyAlt ? 0xE9 : 0x69));
                break;
            case KeyEvent.VK_J:
                sendByte(keyCtrl ? 0x0A : keyShift ? (keyAlt ? 0xCA : 0x4A) : (keyAlt ? 0xEA : 0x6A));
                break;
            case KeyEvent.VK_K:
                sendByte(keyCtrl ? 0x0B : keyShift ? (keyAlt ? 0xCB : 0x4B) : (keyAlt ? 0xEB : 0x6B));
                break;
            case KeyEvent.VK_L:
                sendByte(keyCtrl ? 0x0C : keyShift ? (keyAlt ? 0xCC : 0x4C) : (keyAlt ? 0xEC : 0x6C));
                break;
            case KeyEvent.VK_M:
                sendByte(keyCtrl ? 0x0D : keyShift ? (keyAlt ? 0xCD : 0x4D) : (keyAlt ? 0xED : 0x6D));
                break;
            case KeyEvent.VK_N:
                sendByte(keyCtrl ? 0x0E : keyShift ? (keyAlt ? 0xCE : 0x4E) : (keyAlt ? 0xEE : 0x6E));
                break;
            case KeyEvent.VK_O:
                sendByte(keyCtrl ? 0x0F : keyShift ? (keyAlt ? 0xCF : 0x4F) : (keyAlt ? 0xEF : 0x6F));
                break;
            case KeyEvent.VK_P:
                sendByte(keyCtrl ? 0x10 : keyShift ? (keyAlt ? 0xD0 : 0x50) : (keyAlt ? 0xF0 : 0x70));
                break;
            case KeyEvent.VK_Q:
                sendByte(keyCtrl ? 0x11 : keyShift ? (keyAlt ? 0xD1 : 0x51) : (keyAlt ? 0xF1 : 0x71));
                break;
            case KeyEvent.VK_R:
                sendByte(keyCtrl ? 0x12 : keyShift ? (keyAlt ? 0xD2 : 0x52) : (keyAlt ? 0xF2 : 0x72));
                break;
            case KeyEvent.VK_S:
                sendByte(keyCtrl ? 0x13 : keyShift ? (keyAlt ? 0xD3 : 0x53) : (keyAlt ? 0xF3 : 0x73));
                break;
            case KeyEvent.VK_T:
                sendByte(keyCtrl ? 0x14 : keyShift ? (keyAlt ? 0xD4 : 0x54) : (keyAlt ? 0xF4 : 0x74));
                break;
            case KeyEvent.VK_U:
                sendByte(keyCtrl ? 0x15 : keyShift ? (keyAlt ? 0xD5 : 0x55) : (keyAlt ? 0xF5 : 0x75));
                break;
            case KeyEvent.VK_V:
                sendByte(keyCtrl ? 0x16 : keyShift ? (keyAlt ? 0xD6 : 0x56) : (keyAlt ? 0xF6 : 0x76));
                break;
            case KeyEvent.VK_W:
                sendByte(keyCtrl ? 0x17 : keyShift ? (keyAlt ? 0xD7 : 0x57) : (keyAlt ? 0xF7 : 0x77));
                break;
            case KeyEvent.VK_X:
                sendByte(keyCtrl ? 0x18 : keyShift ? (keyAlt ? 0xD8 : 0x58) : (keyAlt ? 0xF8 : 0x78));
                break;
            case KeyEvent.VK_Y:
                sendByte(keyCtrl ? 0x19 : keyShift ? (keyAlt ? 0xD9 : 0x59) : (keyAlt ? 0xF9 : 0x79));
                break;
            case KeyEvent.VK_Z:
                sendByte(keyCtrl ? 0x1A : keyShift ? (keyAlt ? 0xDA : 0x5A) : (keyAlt ? 0xFA : 0x7A));
                break;
            case KeyEvent.VK_F1:    // PF1
                sendBytes(new byte[]{0x1B, 0x4F, 0x50});
                break;
            case KeyEvent.VK_F2:    // PF2
                sendBytes(new byte[]{0x1B, 0x4F, 0x51});
                break;
            case KeyEvent.VK_F3:    // PF3
                sendBytes(new byte[]{0x1B, 0x4F, 0x52});
                break;
            case KeyEvent.VK_F4:    // PF4
                sendBytes(new byte[]{0x1B, 0x4F, 0x53});
                break;
            case KeyEvent.VK_F5:    // PF5
                sendBytes(new byte[]{0x1B, 0x4F, 0x70});
                break;
            case KeyEvent.VK_F6:    // PF6
                sendBytes(new byte[]{0x1B, 0x4F, 0x71});
                break;
            case KeyEvent.VK_F7:    // PF7
                sendBytes(new byte[]{0x1B, 0x4F, 0x72});
                break;
            case KeyEvent.VK_F8:    // PF8
                sendBytes(new byte[]{0x1B, 0x4F, 0x73});
                break;
            case KeyEvent.VK_F9:    // PF9
                sendBytes(new byte[]{0x1B, 0x4F, 0x74});
                break;
            case KeyEvent.VK_F10:   // PF10
                sendBytes(new byte[]{0x1B, 0x4F, 0x75});
                break;
            case KeyEvent.VK_F11:   // PF11
                sendBytes(new byte[]{0x1B, 0x4F, 0x76});
                break;
            case KeyEvent.VK_F12:   // PF12
                sendBytes(new byte[]{0x1B, 0x4F, 0x77});
                break;
            case KeyEvent.VK_UP:    // Up
                sendBytes(keyCtrl ? new byte[]{(byte) 0x9B} : new byte[]{0x1B, 0x5B, 0x41});
                break;
            case KeyEvent.VK_DOWN:  // Down
                sendBytes(keyCtrl ? new byte[]{(byte) 0x9F} : new byte[]{0x1B, 0x5B, 0x42});
                break;
            case KeyEvent.VK_RIGHT: // Right
                sendBytes(keyCtrl ? new byte[]{(byte) 0x9D} : new byte[]{0x1B, 0x5B, 0x43});
                break;
            case KeyEvent.VK_LEFT:  // Left
                sendBytes(keyCtrl ? new byte[]{(byte) 0x8D} : new byte[]{0x1B, 0x5B, 0x44});
                break;
            case KeyEvent.VK_HOME:
                // PA2 / PA1
                sendBytes(keyShift ? new byte[]{0x1B, 0x4F, 0x79} : new byte[]{0x1B, 0x4F, 0x78});
                break;
            case KeyEvent.VK_END:
                // Links Oben / PA3
                sendBytes(keyShift ? new byte[]{0x1B, 0x5B, 0x48} : new byte[]{0x1B, 0x4F, 0x7A});
                break;
            // Sondertasten
            case KeyEvent.VK_ESCAPE:
                sendBytes(keyShift ? new byte[]{0x1B, 0x63} : new byte[]{0x1B});
                break;
            case KeyEvent.VK_ENTER:
                sendBytes(keyCtrl ? new byte[]{0x1B, 0x4F, 0x4D} : new byte[]{0x0D});
                break;
            case KeyEvent.VK_TAB:
                sendBytes(keyShift ? new byte[]{0x1B, 0x5B, 0x5A} : new byte[]{0x09});
                break;
            case KeyEvent.VK_BACK_SPACE:
                sendByte(keyCtrl ? 0x88 : keyShift ? 0x18 : 0x08);
                break;
            case KeyEvent.VK_DELETE:
                sendBytes(keyShift ? new byte[]{0x1B, 0x5B, 0x4D} : new byte[]{0x7F});
                break;
            case KeyEvent.VK_PAUSE:
                System.out.println("Break");
                MMS16Bus.getInstance().requestInterrupt(1);
                break;
            case KeyEvent.VK_PRINTSCREEN:
                sendBytes(keyCtrl ? new byte[]{(byte) 0x99} : keyShift ? new byte[]{0x1B, 0x5B, 0x32, 0x4A} : new byte[]{0x0A});
                break;
            case KeyEvent.VK_PAGE_UP:
                sendBytes(keyShift ? new byte[]{0x1B, 0x5B, 0x4F} : new byte[]{0x1B, 0x5B, 0x4E});
                break;
            case KeyEvent.VK_PAGE_DOWN:
                sendBytes(keyShift ? new byte[]{0x1B, 0x4F, 0x4F} : new byte[]{0x1B, 0x4F, 0x4E});
                break;
            case KeyEvent.VK_INSERT:
                // TODO: INS MODE
                sendBytes(keyShift ? new byte[]{0x1B, 0x5B, 0x4C} : new byte[]{});
                break;
            case KeyEvent.VK_SCROLL_LOCK:
                // TODO: MOD 2
                break;
            // Sonderzeichen
            case KeyEvent.VK_SPACE:
                sendByte(0x20);
                break;
            case KeyEvent.VK_PLUS:
                sendByte(keyShift ? 0x2A : 0x2B);
                break;
            case KeyEvent.VK_COMMA:
                sendByte(keyShift ? 0x3B : 0x2C);
                break;
            case KeyEvent.VK_MINUS:
                sendByte(keyShift ? 0x5F : 0x2D);
                break;
            case KeyEvent.VK_NUMBER_SIGN:
                sendByte(keyShift ? 0x5E : 0x23);
                break;
            case KeyEvent.VK_LESS:
                sendByte(keyShift ? 0x3E : 0x3C);
                break;
            case KeyEvent.VK_PERIOD:
                sendByte(keyShift ? 0x3A : 0x2E);
                break;
            case KeyEvent.VK_DEAD_ACUTE:
                sendByte(keyCtrl ? 0x89 : keyShift ? 0x60 : 0x27);
                break;
            case KeyEvent.VK_DEAD_CIRCUMFLEX:
                sendByte(keyCtrl ? 0x9A : 0x3D);
                break;
            // Ziffernblock
            case KeyEvent.VK_NUMPAD0:
                sendByte(keyCtrl ? 0x8E : 0x30);
                break;
            case KeyEvent.VK_NUMPAD1:
                sendByte(keyCtrl ? 0x8C : 0x31);
                break;
            case KeyEvent.VK_NUMPAD2:
                sendByte(keyCtrl ? 0x9C : 0x32);
                break;
            case KeyEvent.VK_NUMPAD3:
                sendByte(keyCtrl ? 0x87 : 0x33);
                break;
            case KeyEvent.VK_NUMPAD4:
                sendByte(keyCtrl ? 0x94 : 0x34);
                break;
            case KeyEvent.VK_NUMPAD5:
                sendByte(keyCtrl ? 0x92 : 0x35);
                break;
            case KeyEvent.VK_NUMPAD6:
                sendByte(keyCtrl ? 0x90 : 0x36);
                break;
            case KeyEvent.VK_NUMPAD7:
                sendByte(keyCtrl ? 0x91 : 0x37);
                break;
            case KeyEvent.VK_NUMPAD8:
                sendByte(keyCtrl ? 0x93 : 0x38);
                break;
            case KeyEvent.VK_NUMPAD9:
                sendByte(keyCtrl ? 0x95 : 0x39);
                break;
            case KeyEvent.VK_DECIMAL:
                sendByte(keyCtrl ? 0x86 : 0x2C);
                break;
            case KeyEvent.VK_ADD:
                sendBytes(keyCtrl ? new byte[]{0x1B, 0x5B, 0x69} : new byte[]{0x2B});
                break;
            case KeyEvent.VK_DIVIDE:
                sendByte(keyCtrl ? 0x8B : 0x2F);
                break;
            case KeyEvent.VK_MULTIPLY:
                sendByte(keyCtrl ? 0x8F : 0x2A);
                break;
            case KeyEvent.VK_SUBTRACT:
                sendByte(keyCtrl ? 0x9E : 0x2D);
                break;
            // Sonstige
            case 0:
                switch (e.getKeyChar()) {
                    case 'ß':
                    case '\\':
                    case '?':
                        sendByte(keyCtrl ? 0x98 : keyShift ? 0x3F : keyAlt ? 0xFE : 0x7E);
                        break;
                    case 'ä':
                    case 'Ä':
                        sendByte(keyShift ? (keyAlt ? 0xDB : 0x7B) : (keyAlt ? 0xFB : 0x5B));
                        break;
                    case 'ö':
                    case 'Ö':
                        sendByte(keyShift ? (keyAlt ? 0xDC : 0x7C) : (keyAlt ? 0xFC : 0x5C));
                        break;
                    case 'ü':
                    case 'Ü':
                        sendByte(keyShift ? (keyAlt ? 0xDD : 0x7D) : (keyAlt ? 0xFD : 0x5D));
                        break;
                }
                break;
        }
    }

    /**
     * Fügt mehrere Bytes zum Sendepuffer hinzu
     *
     * @param bytes Daten
     */
    public void sendBytes(byte[] bytes) {
//        System.out.print("Sende Sequenz: ");
        for (byte b : bytes) {
            sendBuffer.offer(b);
//            System.out.print(String.format("%02X", b));
        }
//        System.out.println();
    }

    /**
     * Fügt ein Byte zum Sendepuffer hinzu
     *
     * @param b Daten
     */
    private void sendByte(int b) {
//        System.out.println(String.format("Sende Byte: %02X", b));
        sendBuffer.offer((byte) (b & 0xFF));
    }

    /**
     * Aktualisiert den Taktzähler und sendet ggf. Daten an USART.
     *
     * @param amount Anzahl der Takte
     */
    public void updateClock(int amount) {
        keyboardClock += amount;
        if (selfTest && (sendBuffer.peek() == 0x11)) {
            // Tastatur Selbsttest, nächstes Zeichen ist 0x11 -> Längere Verzögerung
            if (keyboardClock >= 330000) {
                ifssController.receiveData(sendBuffer.poll());
                selfTest = false;
                keyboardClock = 0;
            }
        } else {
            if (keyboardClock >= 1000) {
                if (!sendBuffer.isEmpty()) {
                    //System.out.println("Sende Byte: "+sendBuffer.peek());
                    ifssController.receiveData(sendBuffer.poll());
                }
                keyboardClock = 0;
            }
        }
    }

    /**
     * Empfängt und verarbeitet ein Byte vom USART-Controller
     *
     * @param b Daten
     */
    public void receiveByte(int b) {
        commands[byteCnt++] = (byte) (b & 0xFF);
        if (kbdType.equals(KeyboardType.K7637)) {
            //System.out.println("Empfange Byte:"+b);
            if (b == 0x00) {
                byteCnt = 0;
                sendBuffer.clear();
                sendBytes(new byte[]{(byte) 0xFF, 0x11});
                selfTest = true;
            } else {
                sendByte(0xFF);
            }
        }
        checkCommands();
    }

    /**
     * Prüft die vom Controller empfangenen Zeichen und verarbeitet diese
     */
    public void checkCommands() {
        switch (byteCnt) {
            case 1:
                switch (commands[0]) {
                    case 0x20:
                        //XON
                        byteCnt = 0;
                        break;
                    case 0x44:
                        //XOFF
                        byteCnt = 0;
                        break;
                    case 0x52:
                        //Status
                        byteCnt = 0;
                        sendBytes(new byte[]{0x1B, 0x5B, 0x30, 0x6E});
                        break;
                }
                break;
            case 2:
                switch (commands[1]) {
                    case 0x20:
                        // ALT-LED ein
                        byteCnt = 0;
                        break;
                    case 0x44:
                        // MOD2-LED ein
                        byteCnt = 0;
                        break;
                    case 0x52:
                        // INS-MODE-LED ein
                        byteCnt = 0;
                        break;
                    case 0x63:
                        // Reset
                        if (commands[0] == 0x1b) {
                            byteCnt = 0;
                            sendByte(0x11);
                        }
                        break;
                }
                break;
            case 3:
                switch (commands[2]) {
                    case 0x20:
                        // ALT-LED aus
                        byteCnt = 0;
                        break;
                    case 0x44:
                        // MOD2-LED aus
                        byteCnt = 0;
                        break;
                    case 0x52:
                        // INS-MODE-LED aus
                        byteCnt = 0;
                        break;
                    case 0x55:
                        // RESET
                        byteCnt = 0;
                        sendBuffer.clear();
                        sendByte(0x11);
                        break;
                }
                break;
            case 4:
                switch (commands[3]) {
                    case 0x6E:
                        // Status
                        if (commands[0] == 0x1b) {
                            byteCnt = 0;
                            sendBytes(new byte[]{0x5B, 0x30, 0x6E});
                        }
                        break;
                }
                break;
            default:
                break;
        }
    }

    /**
     * Schreibt den Zustand der Tastatur in eine Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(final DataOutputStream dos) throws IOException {
        dos.writeBoolean(alt);
        dos.writeBoolean(caps);
        dos.writeBoolean(mode2);
        dos.writeByte(byteCnt);
        for (int i = 0; i < 10; i++) {
            dos.writeByte(commands[i]);
        }
        dos.writeInt(sendBuffer.size());
        for (Byte b : sendBuffer) {
            dos.writeByte(b);
        }
        dos.writeUTF(kbdType.name());
        dos.writeLong(keyboardClock);
        dos.writeBoolean(selfTest);
    }

    /**
     * Liest den Zustand der Tastatur aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Laden nicht erfolgreich war
     */
    @Override
    public void loadState(final DataInputStream dis) throws IOException {
        alt = dis.readBoolean();
        caps = dis.readBoolean();
        mode2 = dis.readBoolean();
        byteCnt = dis.readByte();
        for (int i = 0; i < 10; i++) {
            commands[i] = dis.readByte();
        }
        sendBuffer.clear();
        int size = dis.readInt();
        for (int i = 0; i < size; i++) {
            sendBuffer.add(dis.readByte());
        }
        kbdType = KeyboardType.valueOf(dis.readUTF());
        keyboardClock = dis.readLong();
        selfTest = dis.readBoolean();
    }

    /**
     * Setzt die Tastatur in den Grundzustand
     */
    public void reset() {
        ifssController = null;
        alt = false;
        caps = false;
        mode2 = false;
        byteCnt = 0;
        commands = new byte[10];
        sendBuffer.clear();
        keyboardClock = 0;
        selfTest = false;
    }
}
