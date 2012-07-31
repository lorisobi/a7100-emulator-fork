/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.system;

import a7100emulator.components.ic.KR580WM51A;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 *
 * @author Dirk
 */
public class Keyboard implements KeyListener {

    public void reset() {
        byteCnt=0;
    }

    enum KeyboardType {

        K7637, K7672;
    }
    private KR580WM51A ifssController = null;
    // 1:1
    // Alphanumerik: A-Z, a-z, 1-0
    // Sondertasten: Backspace, ESC, Enter, Tabulator, Caps Lock, DEL, Ctrl, Alt
    // Navigation  : Pfeile
    // Symbole     : ! " § $ % & / ( ) = < > , ; . : - _ # + * ? ´ `
    // Umsetzungen:
    // PF1-PF12 --> F1-F12
    // ^        --> '
    // ö        --> \
    // Ö        --> |
    // ä        --> [
    // Ä        --> {
    // ü        --> ]
    // Ü        --> }
    // ß        --> Überstrich
    // Pause    --> Break
    // Insert   --> INS Mode
    // Shift+Insert --> Insert Line
    // Shift+Delete --> Delete Line
    // Home         --> PA1
    // Shift+Home   --> PA2
    // End          --> PA3
    // Shift+End    --> Pfeil Links-Oben
    // PageUp       --> Erase EOF
    // Shift+PageUp --> Erase INP
    // PageDn       --> DUP
    // Shift+PageDn --> FM
    private boolean alt = false;
    private boolean caps = false;
    private boolean mode2 = false;
    private static Keyboard instance = null;
    private int byteCnt = 0;
    private byte[] commands = new byte[10];
    private KeyboardType kbdType = KeyboardType.K7672;

    private Keyboard() {
    }

    public static Keyboard getInstance() {
        if (instance == null) {
            instance = new Keyboard();
        }
        return instance;
    }

    public void registerController(KR580WM51A controller) {
        ifssController = controller;
        sendBytes(new byte[]{(byte)0xFF, 0x11});
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
        System.out.println("KeyReleased: " + Integer.toHexString(e.getKeyCode()) + " " + (int) e.getKeyChar());

        int code = 0x00;
        boolean shift = e.isShiftDown();

        //System.out.println(Integer.toHexString(code)+" "+e.getKeyChar());

        if (e.isShiftDown()) {
            System.out.println("Shift+");
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_0:
                sendByte(shift ? 0x3D : 0x30);
                break;
            case KeyEvent.VK_1:
                sendByte(shift ? 0x21 : 0x31);
                break;
            case KeyEvent.VK_2:
                sendByte(shift ? 0x22 : 0x32);
                break;
            case KeyEvent.VK_3:
                sendByte(shift ? 0x40 : 0x33);
                break;
            case KeyEvent.VK_4:
                sendByte(shift ? 0x24 : 0x34);
                break;
            case KeyEvent.VK_5:
                sendByte(shift ? 0x25 : 0x35);
                break;
            case KeyEvent.VK_6:
                sendByte(shift ? 0x26 : 0x36);
                break;
            case KeyEvent.VK_7:
                sendByte(shift ? 0x2F : 0x37);
                break;
            case KeyEvent.VK_8:
                sendByte(shift ? 0x28 : 0x38);
                break;
            case KeyEvent.VK_9:
                sendByte(shift ? 0x29 : 0x39);
                break;
            case KeyEvent.VK_A:
                sendByte(shift ? 0x41 : 0x61);
                break;
            case KeyEvent.VK_B:
                sendByte(shift ? 0x42 : 0x62);
                break;
            case KeyEvent.VK_C:
                sendByte(shift ? 0x43 : 0x63);
                break;
            case KeyEvent.VK_D:
                sendByte(shift ? 0x44 : 0x64);
                break;
            case KeyEvent.VK_E:
                sendByte(shift ? 0x45 : 0x65);
                break;
            case KeyEvent.VK_F:
                sendByte(shift ? 0x46 : 0x66);
                break;
            case KeyEvent.VK_G:
                sendByte(shift ? 0x47 : 0x67);
                break;
            case KeyEvent.VK_H:
                sendByte(shift ? 0x48 : 0x68);
                break;
            case KeyEvent.VK_I:
                sendByte(shift ? 0x49 : 0x69);
                break;
            case KeyEvent.VK_J:
                sendByte(shift ? 0x4A : 0x6A);
                break;
            case KeyEvent.VK_K:
                sendByte(shift ? 0x4B : 0x6B);
                break;
            case KeyEvent.VK_L:
                sendByte(shift ? 0x4C : 0x6C);
                break;
            case KeyEvent.VK_M:
                sendByte(shift ? 0x4D : 0x6D);
                break;
            case KeyEvent.VK_N:
                sendByte(shift ? 0x4E : 0x6E);
                break;
            case KeyEvent.VK_O:
                sendByte(shift ? 0x4F : 0x6F);
                break;
            case KeyEvent.VK_P:
                sendByte(shift ? 0x50 : 0x70);
                break;
            case KeyEvent.VK_Q:
                sendByte(shift ? 0x51 : 0x71);
                break;
            case KeyEvent.VK_R:
                sendByte(shift ? 0x52 : 0x72);
                break;
            case KeyEvent.VK_S:
                sendByte(shift ? 0x53 : 0x73);
                break;
            case KeyEvent.VK_T:
                sendByte(shift ? 0x54 : 0x74);
                break;
            case KeyEvent.VK_U:
                sendByte(shift ? 0x55 : 0x75);
                break;
            case KeyEvent.VK_V:
                sendByte(shift ? 0x56 : 0x76);
                break;
            case KeyEvent.VK_W:
                sendByte(shift ? 0x57 : 0x77);
                break;
            case KeyEvent.VK_X:
                sendByte(shift ? 0x58 : 0x78);
                break;
            case KeyEvent.VK_Y:
                sendByte(shift ? 0x59 : 0x79);
                break;
            case KeyEvent.VK_Z:
                sendByte(shift ? 0x5A : 0x7A);
                break;
            case KeyEvent.VK_F1:    // PF1
                sendBytes(new byte[]{0x27, 0x30, 0x50});
                break;
            case KeyEvent.VK_F2:    // PF2
                sendBytes(new byte[]{0x27, 0x30, 0x51});
                break;
            case KeyEvent.VK_F3:    // PF3
                sendBytes(new byte[]{0x27, 0x30, 0x52});
                break;
            case KeyEvent.VK_F4:    // PF4
                sendBytes(new byte[]{0x27, 0x30, 0x53});
                break;
            case KeyEvent.VK_F5:    // PF5
                sendBytes(new byte[]{0x27, 0x30, 0x70});
                break;
            case KeyEvent.VK_F6:    // PF6
                sendBytes(new byte[]{0x27, 0x30, 0x71});
                break;
            case KeyEvent.VK_F7:    // PF7
                sendBytes(new byte[]{0x27, 0x30, 0x72});
                break;
            case KeyEvent.VK_F8:    // PF8
                sendBytes(new byte[]{0x27, 0x30, 0x73});
                break;
            case KeyEvent.VK_F9:    // PF9
                sendBytes(new byte[]{0x27, 0x30, 0x74});
                break;
            case KeyEvent.VK_F10:   // PF10
                sendBytes(new byte[]{0x27, 0x30, 0x75});
                break;
            case KeyEvent.VK_F11:   // PF11
                sendBytes(new byte[]{0x27, 0x30, 0x76});
                break;
            case KeyEvent.VK_F12:   // PF12
                sendBytes(new byte[]{0x27, 0x30, 0x77});
                break;
            case KeyEvent.VK_UP:    // Up
                sendBytes(new byte[]{0x27, 0x5B, 0x41});
                break;
            case KeyEvent.VK_DOWN:  // Down
                sendBytes(new byte[]{0x27, 0x5B, 0x42});
                break;
            case KeyEvent.VK_RIGHT: // Right
                sendBytes(new byte[]{0x27, 0x5B, 0x43});
                break;
            case KeyEvent.VK_LEFT:  // Left
                sendBytes(new byte[]{0x27, 0x5B, 0x44});
                break;
            case KeyEvent.VK_HOME:
                if (e.isShiftDown()) // PA2
                {
                    sendBytes(new byte[]{0x27, 0x30, 0x79});
                } else // PA1
                {
                    sendBytes(new byte[]{0x27, 0x30, 0x78});
                }
                break;
            case KeyEvent.VK_END:
                if (e.isShiftDown()) // Links Oben
                {
                    sendBytes(new byte[]{0x27, 0x5B, 0x48});
                } else {            // PA3
                    sendBytes(new byte[]{0x27, 0x30, 0x7A});
                }
                break;

            case KeyEvent.VK_ESCAPE:
                sendByte(0x27);
                break;
            case KeyEvent.VK_ENTER:
                sendByte(0x0D);
                break;
            case KeyEvent.VK_TAB:
                sendByte(0x09);
                break;
            case KeyEvent.VK_BACK_SPACE:
                sendByte(0x08);
                break;
            case KeyEvent.VK_SPACE:
                sendByte(0x20);
                break;
            case KeyEvent.VK_PLUS:
                sendByte(shift ? 0x29 : 0x2A);
                break;
            case KeyEvent.VK_COMMA:
                sendByte(shift ? 0x3B : 0x2C);
                break;
            case KeyEvent.VK_MINUS:
                sendByte(shift ? 0x5F : 0x2D);
                break;
            case KeyEvent.VK_NUMBER_SIGN:
                sendByte(shift ? 0x5E : 0x23);
                break;
            case KeyEvent.VK_LESS:
                sendByte(shift ? 0x3E : 0x3C);
                break;
            case KeyEvent.VK_PERIOD:
                sendByte(shift ? 0x3A : 0x2E);
                break;

            //throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    public void sendBytes(byte[] bytes) {
        ifssController.writeDataToSystem(bytes);
    }

    private void sendByte(int b) {
        ifssController.writeDataToSystem((byte) (b & 0xFF));
    }

    public void receiveByte(int b) {
        commands[byteCnt++] = (byte) (b & 0xFF);
        sendByte(0xFF);
        checkCommands();
    }

    public void checkCommands() {
        System.out.println("Cnt:" + byteCnt);

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
                        System.out.println("Anfrage Status");
                        byteCnt = 0;
                        sendBytes(new byte[]{0x1B, 0x5B, 0x30, 0x6E});
                        break;
                    case 0x55:
                        // Reset Manuell
                        //byteCnt=0;
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
                        System.out.println("Anfrage Reset");
                        byteCnt = 0;
                        sendByte(0x11);
                        break;
                }
                break;
            case 4:
                switch (commands[3]) {
                    case 0x6E:
                        // Status
                        if (commands[0] == 0x1b) {
                            System.out.println("Anfrage Status");
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
}
