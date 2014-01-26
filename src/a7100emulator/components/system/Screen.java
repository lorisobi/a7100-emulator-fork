/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package a7100emulator.components.system;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 *
 * @author Dirk
 */
public class Screen extends JComponent {

    private static Screen instance;
    private BufferedImage screenImage;

    private Screen() {
        setMinimumSize(new Dimension(640, 400));
        setPreferredSize(new Dimension(640, 400));
    }

    /**
     * 
     * @return
     */
    public static Screen getInstance() {
        if (instance == null) {
            instance = new Screen();
        }
        return instance;
    }

    /**
     * 
     * @param screenImage
     */
    public void setImage(BufferedImage screenImage) {
        this.screenImage = screenImage;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.drawImage(screenImage, 0, 0, this);
    }
}
