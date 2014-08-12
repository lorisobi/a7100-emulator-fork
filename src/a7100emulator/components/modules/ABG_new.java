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
import a7100emulator.components.modules.ABG.CursorMode;
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
public final class ABG_new implements Module, ClockModule {

    /**
     * Attribut intensive Darstellung
     */
    private static final int ATTRIBUTE_INTENSE = 0x01;
    /**
     * Attribut Blinkende Darstellung
     */
    private static final int ATTRIBUTE_FLASH = 0x02;
    /**
     * Attribute Cursor
     */
    private static final int ATTRIBUTE_CURSOR = 0x04;
    /**
     * Attribut unterstrichene Darstellung
     */
    private static final int ATTRIBUTE_UNDERLINE = 0x08;
    /**
     * Attribut inverse Darstellung
     */
    private static final int ATTRIBUTE_INVERSE = 0x10;
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
     * Erstellt eine neue ABG und initialisiert diese
     */
    public ABG_new() {
        init();
    }

    /**
     * Intitialisiert die ABG
     */
    @Override
    public void init() {
        for (Memory mem : graphicMemory) {
            mem = new Memory(0x8000);
        }
        for (Memory mem : alphanumericMemory) {
            mem = new Memory(0x8000);
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

    public void writeMemory(int address, int data, int msel) {
        if (getBit(msel, 0)) {
            alphanumericMemory[0].writeByte(address - 0x8000, data);
        }
        if (getBit(msel, 1)) {
            alphanumericMemory[1].writeByte(address - 0x8000, data);
        }
        if (getBit(msel, 2)) {
            graphicMemory[0].writeByte(address - 0x8000, data);
        }
        if (getBit(msel, 0)) {
            graphicMemory[1].writeByte(address - 0x8000, data);
        }
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
    private void updateScreen() {
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
        blinkClock += amount;
        if (blinkClock > 2457500) {
            blinkClock = 0;
            if (blinkState == 0) {
                blinkState = 1;

            } else {
                blinkState = 0;
            }
            updateAlphanumericScreen(cursorRow, cursorColumn);
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
     * Aktualisiert ein Zeichen des Alphanumerik-Bildschirms
     *
     * @param row Zeile
     * @param column Spalte
     */
    private void updateAlphanumericScreen(int row, int column) {
        updateScreen();
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
     * Lädt den Zustand der ABG aus einer datei
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
        generateAlphanumericScreen();
    }

    /**
     * Erstellt den Alphanumerik-Bildschirm neu
     */
    private void generateAlphanumericScreen() {
        for (int row = 0; row < 25; row++) {
            for (int column = 0; column < 80; column++) {
                updateAlphanumericScreen(row, column);
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
}
