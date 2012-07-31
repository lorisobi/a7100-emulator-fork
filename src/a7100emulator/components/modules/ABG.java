/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.modules;

import a7100emulator.components.system.Screen;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 *
 * @author Dirk
 */
public class ABG implements Module {
    // Farben
    private static final int GREEN = new Color(0, 150, 0).getRGB();
    private static final int INTENSE_GREEN = new Color(0, 255, 0).getRGB();
    private static final int DARK_GREEN = new Color(0, 75, 0).getRGB();
    private static final int BLACK = new Color(0, 0, 0).getRGB();
    // Bilder
    private BufferedImage alphanumericScreen = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
    private BufferedImage graphicsScreen = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
    private BufferedImage screenImage = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
    // Puffer
    private byte[][] alphanumericBuffer = new byte[640][400];
    private byte[][] graphicsBuffer = new byte[640][400];

    public ABG() {
        init();
    }

    public void init() {
        Graphics g=alphanumericScreen.getGraphics();
        g.setColor(new Color(BLACK));
        g.fillRect(0, 0, 640, 400);
        g=graphicsScreen.getGraphics();
        g.setColor(new Color(BLACK));
        g.fillRect(0, 0, 640, 400);
        screenImage=alphanumericScreen;
        Screen.getInstance().setImage(screenImage);
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

    public void updateAlphanumericScreenRectangle(int x, int y, BufferedImage character) {
        alphanumericScreen.getGraphics().drawImage(character, x, y, null);
        updateScreen();
    }

    private void updateScreen() {
        Screen.getInstance().repaint();
    }

    void rollAlphanumericScreen() {
        BufferedImage newScreen = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
        newScreen.getGraphics().drawImage(alphanumericScreen.getSubimage(0, 16, 640, 384),0,0,null);
        alphanumericScreen=newScreen;
        screenImage=alphanumericScreen;
        Screen.getInstance().setImage(screenImage);
    }
}
