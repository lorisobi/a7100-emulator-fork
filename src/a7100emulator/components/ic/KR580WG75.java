/*
 * KR580WG75.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * Copyright (c) 2011-2020 Dirk Bräuer
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
 *   30.03.2018 - Erste Version
 *   01.06.2018 - Register hinzugefügt
 *   04.01.2020 - Auswerten Start Display
 */
package a7100emulator.components.ic;

import a7100emulator.components.modules.ABS;
import a7100emulator.components.system.Screen;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasse zur Realisierung des CRT-Controllers.
 *
 * @author Dirk Bräuer
 */
public class KR580WG75 implements IC {

    /**
     * Logger Instanz
     */
    private static final Logger LOG = Logger.getLogger(ABS.class.getName());

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
     * Bit Interrupt Enable
     */
    private static final int IE_BIT = 0x40;
    /**
     * Bit Interrupt Request
     */
    private static final int IR_BIT = 0x20;
    /**
     * Bit Light Pen
     */
    private static final int LP_BIT = 0x10;
    /**
     * Bit Improper Command
     */
    private static final int IC_BIT = 0x08;
    /**
     * Bit Video Enable
     */
    private static final int VE_BIT = 0x04;
    /**
     * Bit DMA Underrun
     */
    private static final int DU_BIT = 0x02;
    /**
     * Bit FIFO Overrun
     */
    private static final int FO_BIT = 0x01;
    /**
     * Aktuell Dargestellter Bildschirm
     */
    private final BufferedImage screenImage = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
    /**
     * Parameterregister
     */
    private int preg;
    /**
     * Statusregister
     */
    private int sreg;
    /**
     * Command-Register
     */
    private int creg;
    /**
     * Nummer des nächsten erwarteten Parameters
     */
    private int nextParam = 0;
    /**
     * Screen-Composition parameter
     */
    private int[] screenComposition = new int[4];
    /**
     * Cursor-Position-Register
     */
    private int[] cursorPosition = new int[2];
    /**
     * Light-Pen-Position-Register
     */
    private int[] lightPenPosition = new int[2];
    /**
     * Anzahl der Zeichentakte zwischen DMA Anfragen
     */
    private int clocksBetweenDMA = 0;
    /**
     * Anzahl der DMA Zyklen pro Burst
     */
    private int dmaCyclesPerBurst = 0;
    /**
     * Zeichenzähler
     */
    private int characterCounter = 0;
    /**
     * Linienzähler
     */
    private int lineCounter = 0;
    /**
     * Zeilenpuffer
     */
    private int[][] rowBuffer = new int[2][80];
    /**
     * Position Eingangspuffer
     */
    private int rowInputPosition = 0;

    /**
     * Erstellt einen neuen CRT-Controller und initialisiert Ihn
     */
    public KR580WG75() {
        init();
    }

    /**
     * Initialisiert den CRT-Controller
     */
    private void init() {
        // Löschen des Bildschirmes
        Graphics g = screenImage.getGraphics();
        g.setColor(new Color(BLACK));
        g.fillRect(0, 0, 640, 400);
        Screen.getInstance().setImage(screenImage);
    }

    /**
     * Liest das Parameterregister des CRT-Controllers.
     *
     * @return Parameterregisterinhalt
     */
    public int readParameter() {
        int result = 0;
        if (nextParam != -1) {
            // Wähle Kommandoregister
            switch (creg) {
                case 0x60:
                    result = lightPenPosition[nextParam++];
                    if (nextParam == 2) {
                        nextParam = -1;
                    }
                    break;
            }
        }
        return result;
    }

    /**
     * Liest das Statusregister des CRT-Controllers.
     *
     * @return Statusregisterinhalt
     */
    public int readStatus() {
        return sreg;
    }

    /**
     * Schreibt einen Parameter in das Parameterregister des CRT-Controllers.
     *
     * @param data Parameterwert
     */
    public void writeParameter(int data) {
        if (nextParam != -1) {
            // Wähle Kommandoregister
            switch (creg) {
                case 0x00:
                    // Reset
                    System.out.println("CRT: Setze Screencomposition " + nextParam + "=" + Integer.toBinaryString(data));
                    screenComposition[nextParam++] = data;
                    if (nextParam == 4) {
                        nextParam = -1;
                    }

                    break;
                case 0x80:
                    // Load Cursor Position
                    System.out.println("CRT: Setze Cursorposition " + nextParam + "=" + Integer.toBinaryString(data));
                    cursorPosition[nextParam++] = data;
                    if (nextParam == 2) {
                        nextParam = -1;
                    }
                    break;
            }
        }
    }

    /**
     * Übergibt ein Kommando an den CRT Controller.
     *
     * @param command Kommando
     */
    public void writeCommand(int command) {
        if (nextParam != -1) {
            // Setze Improper Command
            setBit(IC_BIT);
        }
        // Speichere Kommando
        creg = command;
        nextParam = -1;

        if (command == 0x00) {
            // Reset
            // Auf den Befehl folgen 4 Screen Composition Bytes
            nextParam = 0;
            clearBit(IE_BIT);
            clearBit(VE_BIT);
            //LOG.log(Level.WARNING, "CRT Kommando Reset noch nicht vollständig implementiert!");
        } else if (command >= 0x20 && command < 0x40) {
            // Start Display
            // Burst Space Code auswerten
            int burstSpaceCode = (command & 0x1C) >> 2;
            if (burstSpaceCode == 0) {
                clocksBetweenDMA = 0;
            } else {
                clocksBetweenDMA = burstSpaceCode * 8 - 1;
            }
            int burstCountCode = command & 0x03;
            this.dmaCyclesPerBurst = 0x01 << burstCountCode;
            System.out.println("Setze Takte zwischen DMA: " + clocksBetweenDMA);
            System.out.println("Setze DMA Zyklen pro Burst: " + dmaCyclesPerBurst);
            setBit(IE_BIT);
            setBit(VE_BIT);
            //LOG.log(Level.WARNING, "CRT Kommando Start Display noch nicht vollständig implementiert!");
        } else if (command == 0x40) {
            // Stop Display
            clearBit(VE_BIT);
            //LOG.log(Level.WARNING, "CRT Kommando Stop Display noch nicht vollständig implementiert!");
        } else if (command == 0x60) {
            // Read Light Pen
            // Nach dem Befehl können die 2 Light Pen Register gelesen werden
            nextParam = 0;
            //LOG.log(Level.WARNING, "CRT Kommando Read Light Pen noch nicht vollständig implementiert!");
        } else if (command == 0x80) {
            // Load Cursor Position
            nextParam = 0;
            //LOG.log(Level.WARNING, "CRT Kommando Load Cursor Position noch nicht vollständig implementiert!");
        } else if (command == 0xA0) {
            // Enable Interrupt
            setBit(IE_BIT);
            //LOG.log(Level.WARNING, "CRT Kommando Enable Interrupt noch nicht vollständig implementiert!");
        } else if (command == 0xC0) {
            // Disable Interrupt
            clearBit(IE_BIT);
            //LOG.log(Level.WARNING, "CRT Kommando Disable Interrupt noch nicht vollständig implementiert!");
        } else if (command == 0xE0) {
            // Preset Counters
            LOG.log(Level.WARNING, "CRT Kommando Preset Counters noch nicht vollständig implementiert!");
        } else {
            LOG.log(Level.FINE, "Unbekanntes CRT Kommando {0}!", String.format("0x%02X", command));
        }
    }

    /**
     * Schreibt Zeichencodes in den Eingangszeilenpuffer.
     *
     * @param data Zeichcodes
     */
    public void fillRowBuffer(int[] dataIn) {
        for (int ch : dataIn) {
            rowBuffer[1][rowInputPosition++] = ch;
        }
    }
    
    /**
     * Gibt die Zeichencodes aus dem en Ausgangspuffer zurück.
     * 
     * @return Inhalt Ausgangspuffer
     */
    public int[] getOutputRowBuffer() {
        return rowBuffer[0];
    }

    /**
     * Setzt ein Bit im Statusbyte
     *
     * @param bit zu setzendes Bit
     */
    private void setBit(int bit) {
        sreg |= bit;
    }

    /**
     * Löscht ein Bit im Satusbyte
     *
     * @param bit Zu löschendes Bit
     */
    private void clearBit(int bit) {
        sreg &= ~bit;
    }

    /**
     * Speichert den Zustand des CRT-Controllers in einer Datei.
     *
     * @param dos Stream zur Datei
     * @throws IOException Wenn Schreiben nicht erfolgreich war
     */
    @Override
    public void saveState(DataOutputStream dos) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Lädt den Zustand des CRT-Controllers aus einer Datei.
     *
     * @param dis Stream zur Datei
     * @throws IOException Wenn Laden nicht erfolgreich war
     */
    @Override
    public void loadState(DataInputStream dis) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
