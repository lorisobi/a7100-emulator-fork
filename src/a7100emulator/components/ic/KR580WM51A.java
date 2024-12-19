/*
 * KR580WW51A.java
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
 *   03.04.2014 - Kommentare vervollständigt
 *   23.07.2014 - Aktualisierung Systemzeit an Tastatur weiterreichen
 *   24.07.2014 - Methoden ausgelagert
 *              - Abfrage ob RTS gesetzt
 *              - Konstanten erstellt
 *   18.11.2014 - getBit durch BitTest.getBit ersetzt
 *              - Interface IC implementiert
 *   18.12.2014 - Keyboard Reset Hack hinzugefügt
 *   07.08.2016 - Puffer behält Wert bis er überschrieben wird
 *   09.08.2016 - Logger hinzugefügt
 *   18.03.2018 - Loggen des Reset-Hacks implementiert
 *   19.12.2024 - Unterdruecke Interrupt, falls empfangenes Zeichen 0xff ist
 */
package a7100emulator.components.ic;

import a7100emulator.Tools.BitTest;
import a7100emulator.components.system.Keyboard;
import a7100emulator.components.system.MMS16Bus;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasse zur Abbildung des USART-Schaltkreises
 * <p>
 * TODO: Overrun Error implementieren
 *
 * @author Dirk Bräuer
 */
public class KR580WM51A implements IC {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(KR580WM51A.class.getName());

    /**
     * Statusbit Transmitter Ready
     */
    private final int STATE_TXRDY = 0x01;

    /**
     * Statusbit Receive Ready
     */
    private final int STATE_RXRDY = 0x02;

    /**
     * Statusbit Transmitter Empty
     */
    private final int STATE_TXE = 0x04;

    /**
     * Statusbit Parity Error
     */
    private final int STATE_PE = 0x08;

    /**
     * Statusbit Overrun Error
     */
    private final int STATE_OE = 0x10;

    /**
     * Statusbit Framing Error
     */
    private final int STATE_FE = 0x20;

    /**
     * Statusbit SYNC Detect / Break Detect
     */
    private final int STATE_SYNDET_BRKDET = 0x40;

    /**
     * Statusbit Data Set Ready
     */
    private final int STATE_DSR = 0x80;

    /**
     * Kommando
     */
    private int command;

    /**
     * Aktueller Betriebsmodus
     */
    private int mode;

    /**
     * Aktueller Status
     */
    private int state = STATE_TXE | STATE_TXRDY;

    /**
     * Gibt an ob bereits ein Mode Word empfangen wurde
     */
    private boolean modeInstruction = false;

    /**
     * Recieve Buffer
     */
    private int receiveBuffer;

    /**
     * Tastatur-Reset Hack
     */
    private static boolean keyboardResetHack;

    /**
     * Erstellt einen neuen USART Schaltkreis und registriert ihn bei der
     * Tastatur
     */
    public KR580WM51A() {
        registerAtKeyboard();
    }

    /**
     * Registriert den USART bei der Tastatur
     */
    private void registerAtKeyboard() {
        Keyboard.getInstance().registerController(this);
    }

    /**
     * Verarbeitet ein ankommendes Kommandu und konfiguriert den USART
     * entsprechend
     *
     * @param newCommand Kommando
     */
    public void writeCommand(int newCommand) {
//        System.out.println("Out Command " + Integer.toHexString(newCommand) + "/" + Integer.toBinaryString(newCommand));
        if (modeInstruction) {
            mode = newCommand;
            modeInstruction = false;
//            System.out.print("Setze Modus:");
//            System.out.print(" Stop-Bits:" + (getBit(command, 7) ? (getBit(command, 6) ? 2 : 1.5) : (getBit(command, 6) ? 1 : -1)));
//            System.out.print(" Parität:" + (getBit(command, 4) ? (getBit(command, 5) ? "ungerade" : "gerade") : "keine"));
//            System.out.print(" Databits:" + (getBit(command, 3) ? (getBit(command, 2) ? 8 : 7) : (getBit(command, 2) ? 6 : 5)));
//            System.out.println(" Baudrate:" + (getBit(command, 1) ? (getBit(command, 0) ? "64x" : "16x") : (getBit(command, 0) ? "1x" : "Synchron")));
        } else {
//            System.out.print("Kommando:");
//            System.out.print(" Hunt-Mode:" + getBit(newCommand, 7));
//            System.out.print(" Reset:" + getBit(newCommand, 6));
//            System.out.print(" RTS:" + getBit(newCommand, 5));
//            System.out.print(" Error-Reset:" + getBit(newCommand, 4));
//            System.out.print(" Break:" + getBit(newCommand, 3));
//            System.out.print(" Receive-Enable:" + getBit(newCommand, 2));
//            System.out.print(" DTR:" + getBit(newCommand, 1));
//            System.out.println(" Transmit-Enable:" + getBit(newCommand, 0));
            if (BitTest.getBit(newCommand, 6)) {
                reset();
            }
            if (BitTest.getBit(newCommand, 4)) {
                errorReset();
            }
            if (BitTest.getBit(newCommand, 0) && !BitTest.getBit(newCommand, 5)) {
                if (!keyboardResetHack) {
                    writeDataToDevice(0x00);
                }
            }
            command = newCommand;
        }
    }

    /**
     * Setzt den USART-Controller in den Initialzustand
     */
    private void reset() {
        modeInstruction = true;
        receiveBuffer = 0x00;
        state = STATE_TXE | STATE_TXRDY;
    }

    /**
     * Löscht die Fehlerbits im Statusbyte
     */
    private void errorReset() {
        state &= ~(STATE_PE | STATE_OE | STATE_FE);
    }

    /**
     * Gibt ein Zeichen an die angeschlossene Tastatur aus
     *
     * @param data Daten
     */
    public void writeDataToDevice(int data) {
        // Prüfe ob RTS nicht gesetzt
        if (!BitTest.getBit(command, 5)) {
//            System.out.println("Sende an Tastatur:" + String.format("%02X", data));
            Keyboard.getInstance().receiveByte(data);
        }
    }

    /**
     * Liest den Status des USART-Schaltkreises
     *
     * @return Status-Byte
     */
    public int readStatus() {
        // Status: DSR SYNDET_BRKDET FE OE PE TxEMPTY RxRDY TxRDY
        //System.out.println("Lese Status " + Integer.toHexString(state) + "/" + Integer.toBinaryString(state));
        return state;
    }

    /**
     * Liest ein Byte aus dem Puffer (vom angeschlossenen Gerät)
     *
     * @return gelesenes Byte
     */
    public int readFromDevice() {
        int value = receiveBuffer;
//        System.out.println("Lese Daten von USART:" + String.format("%02X", value & 0xFF));
        // Leere Puffer
        // receiveBuffer = 0x00;
        // Lösche RxRDY Status
        state &= ~STATE_RXRDY;

        return value;
    }

    /**
     * Schreibt ein Byte in den Puffer des USART
     *
     * @param data Zu speicherndes Byte
     */
    public void receiveData(int data) {
//        System.out.println("Empfange von Tastatur:" + String.format("%02X", data & 0xFF));
        // Prüfe, dass RTS nicht gesetzt
//        if (!BitTest.getBit(command, 5)) {
        // Wenn Puffer noch nicht leer -> Overrun Fehler
//            if (receiveBuffer != 0x00) {
//                state |= STATE_OE;
//            }
        receiveBuffer = data;
        state |= STATE_RXRDY;
		if ((data & 0xff) != 0xff) {
			MMS16Bus.getInstance().requestInterrupt(6);
		}
//        }
    }

    /**
     * Leitet die geänderte Systemzeit an die Tastatur weiter
     *
     * @param amount Anzahl der Ticks
     */
    public void updateClock(int amount) {
        Keyboard.getInstance().updateClock(amount);
    }

    /**
     * Speichert den Zustand des USART in eine Datei
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(final DataOutputStream dos) throws IOException {
        dos.writeInt(command);
        dos.writeInt(mode);
        dos.writeInt(state);
        dos.writeBoolean(modeInstruction);
        dos.writeInt(receiveBuffer);
        dos.writeBoolean(keyboardResetHack);
    }

    /**
     * Lädt den Zustand des USART aus einer Datei
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Laden nicht erfolgreich war
     */
    @Override
    public void loadState(final DataInputStream dis) throws IOException {
        command = dis.readInt();
        mode = dis.readInt();
        state = dis.readInt();
        modeInstruction = dis.readBoolean();
        receiveBuffer = dis.readInt();
        KR580WM51A.keyboardResetHack = dis.readBoolean();
    }

    /**
     * Aktiviert oder Deaktiviert den Tastatur-Reset-Hack.
     *
     * @param keyboardResetHack <code>true</code>um den Hack zu aktivieren,
     * <code>false</code> sonst
     */
    public static void setKeyboardResetHack(boolean keyboardResetHack) {
        LOG.log(Level.CONFIG, "Tastatur-Reset-Hack ist {0}", new String[]{(keyboardResetHack ? "aktiviert" : "deaktiviert")});
        KR580WM51A.keyboardResetHack = keyboardResetHack;
    }

    /**
     * Gibt an, ob der Tastatur-Reset-Hack aktiv ist
     *
     * @return <code>true</code>wenn Hack aktiv ist, <code>false</code> sonst
     */
    public static boolean isKeyboardResetHack() {
        return KR580WM51A.keyboardResetHack;
    }
}
