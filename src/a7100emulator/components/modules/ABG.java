/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.modules;

import a7100emulator.Tools.BitmapGenerator;
import a7100emulator.components.system.Screen;
import a7100emulator.components.system.SystemClock;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 * @author Dirk
 */
public final class ABG implements Module, ClockModule {

    /**
     * @param cursorMode the cursorMode to set
     */
    public void setCursorMode(CursorMode cursorMode) {
        this.cursorMode = cursorMode;
    }

    public enum CursorMode {

        CURSOR_INVISIBLE, CURSOR_BLINK_LINE, CURSOR_BLINK_BLOCK, CURSOR_STATIC_BLOCK;
    }
    // Attribute
    private static final int ATTRIBUTE_INTENSE = 0x01;
    private static final int ATTRIBUTE_FLASH = 0x02;
    private static final int ATTRIBUTE_CURSOR = 0x04;
    private static final int ATTRIBUTE_UNDERLINE = 0x08;
    private static final int ATTRIBUTE_INVERSE = 0x10;
    // Farben
    private static final int GREEN = new Color(0, 150, 0).getRGB();
    private static final int INTENSE_GREEN = new Color(0, 255, 0).getRGB();
    private static final int DARK_GREEN = new Color(0, 75, 0).getRGB();
    private static final int BLACK = new Color(0, 0, 0).getRGB();
    // Bilder
    private volatile BufferedImage alphanumericScreen = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
    private volatile BufferedImage alphanumericScreenBlink = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
    private volatile BufferedImage graphicsScreen = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
    private volatile BufferedImage screenImage = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
    // Puffer
    private byte[][][] alphanumericBuffer = new byte[80][25][16];
    private byte[][] attributeBuffer = new byte[80][25];
    private byte[][] graphicsBuffer = new byte[640][400];
    private int blinkState = 0;
    private int blinkClock = 0;
    private CursorMode cursorMode = CursorMode.CURSOR_BLINK_LINE;
    private int cursorRow = 1;
    private int cursorColumn = 1;

    public ABG() {
        init();
    }

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
     * @return the alphanumericScreen
     */
    public BufferedImage getAlphanumericScreen() {
        return alphanumericScreen;
    }

    /**
     * @return the graphicsScreen
     */
    public BufferedImage getGraphicsScreen() {
        return graphicsScreen;
    }

    public BufferedImage Screen() {
        return alphanumericScreen;
    }

    public void setLineCodes(int row, int column, byte[] linecodes, boolean intense, boolean flash, boolean cursor, boolean underline, boolean inverse) {
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
        updateAlphanumericScreen(column, row);
    }

    private void generateAlphanumericScreen() {
        BufferedImage newScreen = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
        Graphics g = newScreen.getGraphics();
        for (int row = 0; row < 25; row++) {
            for (int column = 0; column < 80; column++) {
                updateAlphanumericScreen(column, row);
            }
        }
        //alphanumericScreen.getGraphics().drawImage(newScreen, 0, 0, null);
    }

    public void updateScreen() {
        Screen.getInstance().repaint();
    }

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

    @Override
    public void registerClocks() {
        SystemClock.getInstance().registerClock(this);
    }

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
            updateAlphanumericScreen(cursorColumn, cursorRow);
        }
    }

    void removeCursor(int cursorRow, int cursorColumn) {
        attributeBuffer[(cursorColumn == 80) ? 79 : cursorColumn][cursorRow] &= ~ATTRIBUTE_CURSOR;
        updateAlphanumericScreen((cursorColumn == 80) ? 79 : cursorColumn, cursorRow);
    }

    void setCursor(int cursorRow, int cursorColumn) {
        attributeBuffer[(cursorColumn == 80) ? 79 : cursorColumn][cursorRow] |= ATTRIBUTE_CURSOR;
        updateAlphanumericScreen((cursorColumn == 80) ? 79 : cursorColumn, cursorRow);
        this.cursorColumn = (cursorColumn == 80) ? 79 : cursorColumn;
        this.cursorRow = cursorRow;

    }

    private void updateAlphanumericScreen(int column, int row) {
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
        this.updateScreen();
    }

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

    @Override
    public void loadState(DataInputStream dis) throws IOException {
        for (int i = 0; i < 80; i++) {
            for (int j = 0; j < 25; j++) {
                for (int k = 0; k < 16; k++) {
                    alphanumericBuffer[i][j][k]=dis.readByte();
                }
            }
        }
        for (int i = 0; i < 80; i++) {
            for (int j = 0; j < 25; j++) {
                attributeBuffer[i][j]=dis.readByte();
            }
        }
        for (int i = 0; i < 640; i++) {
            for (int j = 0; j < 400; j++) {
                graphicsBuffer[i][j]=dis.readByte();
            }
        }
        blinkState=dis.readInt();
        blinkClock=dis.readInt();
        cursorMode=CursorMode.valueOf(dis.readUTF());
        cursorRow=dis.readInt();
        cursorColumn=dis.readInt();
        generateAlphanumericScreen();
    }
}
