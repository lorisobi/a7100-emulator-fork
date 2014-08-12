/*
 * ABG.java
 * 
 * Diese Datei gehört zum Projekt A7100 Emulator 
 * (c) 2011-2014 Dirk Bräuer
 * 
 * Letzte Änderungen:
 *   01.04.2014 Kommentare vervollständigt
 *   09.08.2014 Zugriffe auf SystemClock durch MMS16Bus ersetzt
 *
 */
package a7100emulator.components.modules;

import a7100emulator.Tools.BitmapGenerator;
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
     * Puffer für darstellbare Zeichen
     */
    private final byte[][][] alphanumericBuffer = new byte[80][25][16];
    /**
     * Puffer für Attribute
     */
    private final byte[][] attributeBuffer = new byte[80][25];
    /**
     * Puffer für grafischen Bildschirm
     */
    private final byte[][] graphicsBuffer = new byte[640][400];
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
     * Erstellt eine neue ABG und initialisiert diese
     */
    public ABG() {
        init();
    }

    /**
     * Intitialisiert die ABG
     */
    @Override
    public void init() {
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
     * Setzt den Liniencode sowie die Attribute für ein Zeichen des
     * Alphanumerik-Bildschirms
     *
     * @param row Zeilennummer
     * @param column Spaltennummer
     * @param linecodes Liniencode des Zeichens
     * @param intense Intensive Darstellung
     * @param flash Blinkende Darstellung
     * @param cursor Darstellung des Cursors
     * @param underline Unterstrichene Darstellung
     * @param inverse Inverse Darstellung
     */
    void setLineCodes(int row, int column, byte[] linecodes, boolean intense, boolean flash, boolean cursor, boolean underline, boolean inverse) {
        if (column > 79) {
            column = 79;
        }

        alphanumericBuffer[column][row] = linecodes;
        byte attribute = 0;
        if (intense) {
            attribute |= ATTRIBUTE_INTENSE;
        }
        if (flash) {
            attribute |= ATTRIBUTE_FLASH;
        }
        if (cursor) {
            attribute |= ATTRIBUTE_CURSOR;
        }
        if (underline) {
            attribute |= ATTRIBUTE_UNDERLINE;
        }
        if (inverse) {
            attribute |= ATTRIBUTE_INVERSE;
        }
        attributeBuffer[column][row] = attribute;
        updateAlphanumericScreen(row, column);
    }

    /**
     * Zeichnet die Komponente neu und aktualisiert damit die Ansicht
     */
    private void updateScreen() {
        Screen.getInstance().repaint();
    }

    /**
     * Schiebt den Alphanumerik-Bildschirm eine Zeile nach oben
     */
    void rollAlphanumericScreen() {
        for (int column = 0; column < 80; column++) {
            System.arraycopy(alphanumericBuffer[column], 1, alphanumericBuffer[column], 0, 24);
            System.arraycopy(attributeBuffer[column], 1, attributeBuffer[column], 0, 24);
            alphanumericBuffer[column][24] = new byte[16];
        }

        BufferedImage newScreen = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
        newScreen.getGraphics().drawImage(alphanumericScreen.getSubimage(0, 16, 640, 384), 0, 0, null);
        alphanumericScreen = newScreen;
        screenImage = alphanumericScreen;
        Screen.getInstance().setImage(screenImage);
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
     * Entfernt den Cursor von der angegebenen Position
     *
     * @param cursorRow Zeile
     * @param cursorColumn Spalte
     */
    void removeCursor(int cursorRow, int cursorColumn) {
        attributeBuffer[(cursorColumn >= 80) ? 79 : cursorColumn][cursorRow] &= ~ATTRIBUTE_CURSOR;
        updateAlphanumericScreen(cursorRow, (cursorColumn >= 80) ? 79 : cursorColumn);
    }

    /**
     * Setzt den Cursor auf die angegebene Position
     *
     * @param newCursorRow Zeile
     * @param newCursorColumn Spalte
     */
    void setCursor(int newCursorRow, int newCursorColumn) {
        cursorColumn = (newCursorColumn >= 80) ? 79 : newCursorColumn;
        cursorRow = newCursorRow;
        attributeBuffer[cursorColumn][cursorRow] |= ATTRIBUTE_CURSOR;
        updateAlphanumericScreen(cursorRow, cursorColumn);
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
        Graphics g = alphanumericScreen.getGraphics();

        boolean flash = (attributeBuffer[column][row] & ATTRIBUTE_FLASH) == ATTRIBUTE_FLASH;
        boolean cursor = (attributeBuffer[column][row] & ATTRIBUTE_CURSOR) == ATTRIBUTE_CURSOR;
        boolean intense = (attributeBuffer[column][row] & ATTRIBUTE_INTENSE) == ATTRIBUTE_INTENSE;
        boolean inverse = (attributeBuffer[column][row] & ATTRIBUTE_INVERSE) == ATTRIBUTE_INVERSE;
        boolean underline = (attributeBuffer[column][row] & ATTRIBUTE_UNDERLINE) == ATTRIBUTE_UNDERLINE;
        byte[] linecode = new byte[16];
        System.arraycopy(alphanumericBuffer[column][row], 0, linecode, 0, 16);

        if (flash && blinkState == 0) {
            linecode = new byte[16];
        }

        if (cursor && !cursorMode.equals(CursorMode.CURSOR_INVISIBLE)) {
            switch (cursorMode) {
                case CURSOR_BLINK_LINE:
                    if (blinkState == 1) {
                        linecode[14] = (byte) ~linecode[14];
                        linecode[15] = (byte) ~linecode[15];
                    }
                    break;
                case CURSOR_BLINK_BLOCK: {
                    if (blinkState == 1) {
                        for (int i = 0; i < 16; i++) {
                            linecode[i] = (byte) ~linecode[i];
                        }
                    }
                }
                break;
                case CURSOR_STATIC_BLOCK: {
                    for (int i = 0; i < 16; i++) {
                        linecode[i] = (byte) ~linecode[i];
                    }
                }
                break;
            }
        }

        BufferedImage character = BitmapGenerator.generateBitmapFromLineCode(linecode, intense, inverse, underline, flash);
        g.drawImage(character, column * 8, row * 16, null);
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
        for (int i = 0; i < 80; i++) {
            for (int j = 0; j < 25; j++) {
                for (int k = 0; k < 16; k++) {
                    dos.writeByte(alphanumericBuffer[i][j][k]);
                }
            }
        }
        for (int i = 0; i < 80; i++) {
            for (int j = 0; j < 25; j++) {
                dos.writeByte(attributeBuffer[i][j]);
            }
        }
        for (int i = 0; i < 640; i++) {
            for (int j = 0; j < 400; j++) {
                dos.writeByte(graphicsBuffer[i][j]);
            }
        }
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
        for (int i = 0; i < 80; i++) {
            for (int j = 0; j < 25; j++) {
                for (int k = 0; k < 16; k++) {
                    alphanumericBuffer[i][j][k] = dis.readByte();
                }
            }
        }
        for (int i = 0; i < 80; i++) {
            for (int j = 0; j < 25; j++) {
                attributeBuffer[i][j] = dis.readByte();
            }
        }
        for (int i = 0; i < 640; i++) {
            for (int j = 0; j < 400; j++) {
                graphicsBuffer[i][j] = dis.readByte();
            }
        }
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
}
